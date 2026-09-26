package xyz.fmdc.arw.api.projectile.telemetry;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 飛翔体の外弾道物理挙動を1Tick刻みでCSV形式として記録・出力するテレメトリロガー.
 * <p>
 * サーバーTickへの負荷を回避するため、飛翔中はオンメモリに蓄積し、
 * 着弾・消滅時に専用のバックグラウンドスレッドで非同期に一括書き出しを行います。
 */
public final class FlightTelemetryLogger {

    private static final Logger LOGGER = LogManager.getLogger(FlightTelemetryLogger.class);

    /** テレメトリ記録のグローバル有効/無効フラグ */
    public static boolean ENABLED = true;

    /** 孤児セッションと判定する最大生存Tick数（約5分 = 6000 ticks） */
    public static final int MAX_ORPHAN_TICKS = 6000;

    /** ファイルI/Oによるメインサーバースレッドのブロックを防止する非同期Executor */
    private static final ExecutorService IO_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ARW-FlightTelemetry-IO");
        t.setDaemon(true);
        return t;
    });

    /** CSVヘッダー行 */
    private static final String CSV_HEADER =
            "tick,time_s,pos_x,pos_y,pos_z,vel_x,vel_y,vel_z,speed_mps,ori_x,ori_y,ori_z,aoa_deg,pitch_deg,yaw_deg,air_density,thrust_n,drag_n,lift_n,event";

    /** 進行中の飛翔セッションマップ (UUID -> FlightSession) */
    private static final Map<UUID, FlightSession> ACTIVE_SESSIONS = new ConcurrentHashMap<>();

    private FlightTelemetryLogger() {}

    /**
     * ログ出力先ディレクトリパスを取得します。
     * Forgeローダー環境では FMLPaths.GAMEDIR/arw/flight_logs、テスト環境では run/arw/flight_logs にフォールバック。
     */
    public static Path getLogDirectory() {
        try {
            return FMLPaths.GAMEDIR.get().resolve("arw/flight_logs");
        } catch (Throwable t) {
            return Paths.get("run/arw/flight_logs");
        }
    }

    /**
     * 1Tickあたりのテレメトリデータ行
     */
    public record TelemetryRow(
            int tick,
            double timeSec,
            Vec3 pos,
            Vec3 vel,
            double speedMps,
            Vec3 ori,
            double aoaDeg,
            float pitchDeg,
            float yawDeg,
            double airDensity,
            double thrustN,
            double dragN,
            double liftN,
            String event
    ) {
        public String toCsvLine() {
            return String.format(Locale.ROOT,
                    "%d,%.2f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.2f,%.4f,%.4f,%.4f,%.2f,%.2f,%.2f,%.4f,%.2f,%.2f,%.2f,%s",
                    tick,
                    timeSec,
                    pos.x, pos.y, pos.z,
                    vel.x, vel.y, vel.z,
                    speedMps,
                    ori.x, ori.y, ori.z,
                    aoaDeg,
                    pitchDeg,
                    yawDeg,
                    airDensity,
                    thrustN,
                    dragN,
                    liftN,
                    event != null ? event : ""
            );
        }
    }

    /**
     * 単一飛翔体の飛行セッション
     */
    public static class FlightSession {
        private final UUID uuid;
        private final String entityTypeName;
        private final long startTimeMillis;
        private final List<TelemetryRow> rows = new ArrayList<>();
        private boolean ended = false;
        private int lastTick = 0;

        public FlightSession(UUID uuid, String entityTypeName) {
            this.uuid = uuid;
            this.entityTypeName = entityTypeName;
            this.startTimeMillis = System.currentTimeMillis();
        }

        public synchronized void addRow(TelemetryRow row) {
            if (this.ended) return;
            this.rows.add(row);
            this.lastTick = row.tick();
        }

        public synchronized boolean isEnded() {
            return this.ended;
        }

        public synchronized List<TelemetryRow> snapshotAndEnd() {
            this.ended = true;
            return new ArrayList<>(this.rows);
        }
    }

    /**
     * 飛翔体の射出時にテレメトリセッションを開始します。
     *
     * @param entity 飛翔体Entity
     * @param pos    初期位置
     * @param velMps 初期速度 [m/s]
     * @param ori    初期弾軸姿勢
     * @param pitch  初期Pitch
     * @param yaw    初期Yaw
     */
    public static void startSession(Entity entity, Vec3 pos, Vec3 velMps, Vec3 ori, float pitch, float yaw) {
        if (!ENABLED || entity.level().isClientSide) return;

        UUID uuid = entity.getUUID();
        String typeName = entity.getType().getDescription().getString().replaceAll("[^a-zA-Z0-9_-]", "");
        if (typeName.isEmpty()) {
            typeName = entity.getClass().getSimpleName();
        }

        FlightSession session = new FlightSession(uuid, typeName);
        ACTIVE_SESSIONS.put(uuid, session);

        // 初期発射レコード (Tick 0, LAUNCH)
        double speed = velMps.length();
        TelemetryRow initialRow = new TelemetryRow(
                0, 0.0,
                pos, velMps, speed, ori, 0.0, pitch, yaw,
                1.225, 0.0, 0.0, 0.0, "LAUNCH"
        );
        session.addRow(initialRow);

        cleanupStaleSessions();
    }

    /**
     * 飛翔中の1Tickデータを記録します。
     */
    public static void recordTick(
            UUID uuid,
            int tick,
            Vec3 pos,
            Vec3 velMps,
            Vec3 ori,
            float pitch,
            float yaw,
            double airDensity,
            double thrustN,
            double dragN,
            double liftN,
            String event
    ) {
        if (!ENABLED) return;

        FlightSession session = ACTIVE_SESSIONS.get(uuid);
        if (session == null || session.isEnded()) return;

        double speed = velMps.length();
        Vec3 u = ori.lengthSqr() > 1.0E-6 ? ori.normalize() : new Vec3(0, 0, 1);
        double aoaDeg = 0.0;
        if (speed > 1.0E-4) {
            Vec3 vHat = velMps.scale(1.0 / speed);
            double cosAlpha = Math.max(-1.0, Math.min(1.0, u.dot(vHat)));
            aoaDeg = Math.toDegrees(Math.acos(cosAlpha));
        }

        double timeSec = tick * 0.05;
        TelemetryRow row = new TelemetryRow(
                tick, timeSec,
                pos, velMps, speed, u, aoaDeg, pitch, yaw,
                airDensity, thrustN, dragN, liftN, event
        );
        session.addRow(row);
    }

    /**
     * 着弾・消滅・自爆時にセッションを終了し、非同期でCSVファイルへ書き出します。
     *
     * @param uuid      飛翔体のUUID
     * @param endReason 消滅理由（HIT_BLOCK, HIT_ENTITY, TIMEOUT, DISCARD等）
     * @param finalPos  最終座標（nullの場合は直前座標）
     */
    public static void endSession(UUID uuid, String endReason, Vec3 finalPos) {
        if (!ENABLED) return;

        FlightSession session = ACTIVE_SESSIONS.remove(uuid);
        if (session == null || session.isEnded()) return;

        List<TelemetryRow> snapshot = session.snapshotAndEnd();
        if (snapshot.isEmpty()) return;

        // 最終行に着弾イベントを追加
        TelemetryRow last = snapshot.get(snapshot.size() - 1);
        Vec3 pos = (finalPos != null) ? finalPos : last.pos();
        TelemetryRow endRow = new TelemetryRow(
                last.tick() + 1,
                (last.tick() + 1) * 0.05,
                pos,
                last.vel(),
                last.speedMps(),
                last.ori(),
                last.aoaDeg(),
                last.pitchDeg(),
                last.yawDeg(),
                last.airDensity(),
                0.0,
                0.0,
                0.0,
                endReason != null ? endReason : "END"
        );
        snapshot.add(endRow);

        // 非同期スレッドでファイル書き込み
        CompletableFuture.runAsync(() -> writeCsvFile(session.entityTypeName, session.uuid, snapshot), IO_EXECUTOR);
    }

    /**
     * CSVファイルへの実書き出し処理（非同期スレッドで実行）
     */
    private static void writeCsvFile(String typeName, UUID uuid, List<TelemetryRow> rows) {
        try {
            Path dir = getLogDirectory();
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            String filename = String.format("%s_%s.csv", typeName, uuid.toString());
            Path filePath = dir.resolve(filename);

            try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
                writer.write(CSV_HEADER);
                writer.newLine();
                for (TelemetryRow row : rows) {
                    writer.write(row.toCsvLine());
                    writer.newLine();
                }
            }
            LOGGER.debug("Flight telemetry saved to {}", filePath);
        } catch (IOException e) {
            LOGGER.error("Failed to write flight telemetry CSV for {}", uuid, e);
        }
    }

    /**
     * 孤児セッション（未ロードチャンク消失や予期せぬ破棄）の定期クリーンアップ
     */
    private static void cleanupStaleSessions() {
        if (ACTIVE_SESSIONS.size() > 50) {
            long now = System.currentTimeMillis();
            ACTIVE_SESSIONS.entrySet().removeIf(entry -> {
                FlightSession s = entry.getValue();
                // 5分以上経過したセッションは破棄してファイル書き出し
                if (now - s.startTimeMillis > 300_000L || s.lastTick > MAX_ORPHAN_TICKS) {
                    List<TelemetryRow> snapshot = s.snapshotAndEnd();
                    CompletableFuture.runAsync(() -> writeCsvFile(s.entityTypeName, s.uuid, snapshot), IO_EXECUTOR);
                    return true;
                }
                return false;
            });
        }
    }
}
