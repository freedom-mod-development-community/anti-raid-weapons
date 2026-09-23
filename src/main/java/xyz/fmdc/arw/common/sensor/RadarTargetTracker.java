package xyz.fmdc.arw.common.sensor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import xyz.fmdc.arw.api.RadarTargetManager;
import xyz.fmdc.arw.api.TrackedTarget;
import xyz.fmdc.arw.network.PacketHandler;
import xyz.fmdc.arw.network.S2CSyncRadarTargetsPacket;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * レーダーの探知・追尾・タイムアウト・パケット同期ロジックをカプセル化するコンポーネントクラス。
 * 内部探索はEntityIDを用い、外部公開・追尾データ管理はUUIDをキーとして扱う。
 */
public class RadarTargetTracker {

    // 外部公開・保持用の追尾中目標マップ (UUID -> TrackedTarget)
    private final Map<UUID, TrackedTarget> trackedTargets = new HashMap<>();

    // 記憶保持時間 (デフォルト: 40 ticks = 2秒)
    private long timeoutTicks;

    // イベントリスナー（新規捕捉・ロスト時）
    private Consumer<Entity> onDiscoveredListener = null;
    private Consumer<TrackedTarget> onLostListener = null;

    public RadarTargetTracker() {
        this(40L);
    }

    public RadarTargetTracker(long timeoutTicks) {
        this.timeoutTicks = timeoutTicks;
    }

    public void setTimeoutTicks(long timeoutTicks) {
        this.timeoutTicks = timeoutTicks;
    }

    public long getTimeoutTicks() {
        return this.timeoutTicks;
    }

    public void setOnDiscoveredListener(Consumer<Entity> listener) {
        this.onDiscoveredListener = listener;
    }

    public void setOnLostListener(Consumer<TrackedTarget> listener) {
        this.onLostListener = listener;
    }

    public Map<UUID, TrackedTarget> getTrackedTargets() {
        return this.trackedTargets;
    }

    /**
     * 内部探索処理 (EntityID ベース)
     * RadarTargetManager の登録目標から、中心座標と最大距離、任意のフィルターを満たす目標を探索する
     */
    public List<Entity> scanEntities(Level level, Vec3 centerPos, float maxRange, Predicate<Entity> additionalFilter) {
        List<Entity> detectedList = new ArrayList<>();
        double maxRangeSqr = (double) maxRange * maxRange;

        // 内部探索: EntityIDのセットを反復
        for (int entityId : RadarTargetManager.INSTANCE.getTargetEntityIds()) {
            Entity target = RadarTargetManager.INSTANCE.getEntityById(entityId);
            if (target == null) continue;

            // 共通探知チェック: 生存状態、ディメンション一致
            if (!target.isAlive() || target.level() != level) {
                continue;
            }

            // 距離判定
            if (target.position().distanceToSqr(centerPos) > maxRangeSqr) {
                continue;
            }

            // 追加条件判定（視野角など、nullの場合は全方位）
            if (additionalFilter != null && !additionalFilter.test(target)) {
                continue;
            }

            detectedList.add(target);
        }

        return detectedList;
    }

    /**
     * 全方位探知（フィルターなし）の探索
     */
    public List<Entity> scanEntities(Level level, Vec3 centerPos, float maxRange) {
        return scanEntities(level, centerPos, maxRange, null);
    }

    /**
     * 探知したエンティティリストを元に、内部の追尾マップ（UUID -> TrackedTarget）を更新する
     */
    public void updateTrackedTargets(Level level, List<Entity> detectedThisFrame) {
        if (level == null) return;
        long currentGameTime = level.getGameTime();

        // 1. 今回探知されたエンティティを登録・更新
        for (Entity entity : detectedThisFrame) {
            UUID uuid = entity.getUUID();
            TrackedTarget existing = trackedTargets.get(uuid);
            if (existing != null) {
                existing.update(entity, currentGameTime);
            } else {
                trackedTargets.put(uuid, new TrackedTarget(entity, currentGameTime));
                if (onDiscoveredListener != null) {
                    onDiscoveredListener.accept(entity);
                }
            }
        }

        // 2. タイムアウトまたは死亡した目標を削除
        trackedTargets.values().removeIf(target -> {
            boolean expired = target.isExpired(currentGameTime, timeoutTicks)
                    || (target.getEntity() != null && !target.getEntity().isAlive());
            if (expired && onLostListener != null) {
                onLostListener.accept(target);
            }
            return expired;
        });
    }

    /**
     * 追尾データをブロック周辺（チャンク）のクライアントへパケット送信する
     */
    public void syncToClients(Level level, BlockPos pos) {
        if (level == null || level.isClientSide) return;

        List<S2CSyncRadarTargetsPacket.TargetData> packetList = new ArrayList<>(this.trackedTargets.size());
        for (TrackedTarget target : this.trackedTargets.values()) {
            String name = target.getEntity() != null
                    ? target.getEntity().getType().getDescription().getString()
                    : target.getEntityTypeName();
            packetList.add(new S2CSyncRadarTargetsPacket.TargetData(
                    target.getEntityId(),
                    name != null ? name : "Unknown",
                    target.getLastKnownPos(),
                    target.getLastKnownVelocity() != null ? target.getLastKnownVelocity() : Vec3.ZERO
            ));
        }

        PacketHandler.INSTANCE.send(
                PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(pos)),
                new S2CSyncRadarTargetsPacket(pos, packetList)
        );
    }

    /**
     * クライアント側でパケット受信時に追尾マップを更新する
     */
    public void updateClientTrackedTargets(Level level, List<S2CSyncRadarTargetsPacket.TargetData> dataList) {
        if (level == null) return;
        long currentGameTime = level.getGameTime();

        this.trackedTargets.clear();
        for (S2CSyncRadarTargetsPacket.TargetData data : dataList) {
            this.trackedTargets.put(
                    data.uuid(),
                    new TrackedTarget(data.uuid(), data.name(), data.pos(), data.vel(), currentGameTime)
            );
        }
    }
}
