package xyz.fmdc.arw.common.sensor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import xyz.fmdc.arw.api.TrackedTarget;
import xyz.fmdc.arw.network.PacketHandler;
import xyz.fmdc.arw.network.S2CSyncRadarTargetsPacket;

import java.util.*;
import java.util.function.Consumer;

/**
 * レーダーの追尾目標リストの管理、タイムアウト判定、パケット送受信を共通化するクラス
 */
public class RadarTargetTracker {

    // 追尾タイムアウト（Tick数：2秒＝40Tick）
    private static final long TIMEOUT_TICKS = 40;

    // 外部公開・保持用の追尾中目標マップ (UUID -> TrackedTarget)
    private final Map<UUID, TrackedTarget> trackedTargets = new HashMap<>();

    // 内部管理用：前フレームで探知された目標のUUIDセット
    private final Set<UUID> detectedUuidsThisTick = new HashSet<>();

    // 目標喪失時のコールバックリスナー（必要に応じて設定）
    private Consumer<TrackedTarget> onLostListener = null;

    public RadarTargetTracker() {}

    /**
     * 目標喪失時のコールバックを設定
     */
    public void setOnLostListener(Consumer<TrackedTarget> listener) {
        this.onLostListener = listener;
    }

    public Map<UUID, TrackedTarget> getTrackedTargets() {
        return this.trackedTargets;
    }

    /**
     * 探知したエンティティリストを元に、内部の追尾マップ（UUID -> TrackedTarget）を更新する
     */
    public void updateTrackedTargets(Level level, List<Entity> detectedThisFrame) {
        if (level == null) return;
        long currentGameTime = level.getGameTime();

        detectedUuidsThisTick.clear();

        for (Entity entity : detectedThisFrame) {
            UUID uuid = entity.getUUID();
            detectedUuidsThisTick.add(uuid);

            TrackedTarget existing = trackedTargets.get(uuid);
            if (existing != null) {
                existing.update(entity, currentGameTime);
            } else {
                trackedTargets.put(uuid, new TrackedTarget(entity, currentGameTime));
            }
        }

        trackedTargets.values().removeIf(target -> {
            boolean expired = target.isExpired(currentGameTime, TIMEOUT_TICKS);
            Entity entity = target.getEntity();
            boolean dead = (entity != null && !entity.isAlive());

            if (expired || dead) {
                if (onLostListener != null) {
                    onLostListener.accept(target);
                }
                return true;
            }
            return false;
        });
    }

    /**
     * 追尾データをブロック周辺（チャンク）のクライアントへパケット送信する
     */
    public void syncToClients(Level level, BlockPos pos) {
        if (level == null || level.isClientSide) return;

        List<S2CSyncRadarTargetsPacket.TargetData> packetList = new ArrayList<>(this.trackedTargets.size());
        for (TrackedTarget target : this.trackedTargets.values()) {
            String name = target.getEntityTypeName();
            packetList.add(new S2CSyncRadarTargetsPacket.TargetData(
                    target.getEntityId(),
                    name != null ? name : "Unknown",
                    target.getLastKnownPos(),
                    target.getLastKnownVelocity() != null ? target.getLastKnownVelocity() : Vec3.ZERO,
                    target.getAffiliation()
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
                    new TrackedTarget(data.uuid(), data.name(), data.pos(), data.vel(), currentGameTime, data.affiliation())
            );
        }
    }
}
