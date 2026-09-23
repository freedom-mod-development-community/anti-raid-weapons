package xyz.fmdc.arw.common.blockentity.sensor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import xyz.fmdc.arw.api.RadarMathUtil;
import xyz.fmdc.arw.api.TrackedTarget;
import xyz.fmdc.arw.api.blockentity.IDirectionalBlockEntity;
import xyz.fmdc.arw.api.blockentity.IYawModel;
import xyz.fmdc.arw.api.fcs.IFcsSensorNode;
import xyz.fmdc.arw.api.fcs.TargetTrack;
import xyz.fmdc.arw.api.sensor.ITrackedTargetHolder;
import xyz.fmdc.arw.api.sensor.RadarScanRange;
import xyz.fmdc.arw.common.blockentity.AbstractARWBlockEntity;
import xyz.fmdc.arw.common.blockentity.fcs.AbstractFcsCoreBlockEntity;
import xyz.fmdc.arw.network.PacketHandler;
import xyz.fmdc.arw.network.S2CSyncRadarTargetsPacket;

import java.util.*;

/**
 * ターゲットスキャンと方位計算を行う全センサーの基底クラス
 */
public abstract class HorizontalRadarBlockEntity extends AbstractARWBlockEntity
        implements IYawModel, IFcsSensorNode, IDirectionalBlockEntity, ITrackedTargetHolder {

    protected boolean isFcsConnected = false;
    protected boolean isPowered = true;
    protected final List<TargetTrack> detectedTargets = new ArrayList<>();
    protected TargetTrack primaryLockedTarget = null;

    // アンテナ回転状態
    protected float currentYaw = 0.0f;
    protected float prevYaw = 0.0f;
    protected float currentScanAngle = 0.0f;

    // 追尾中の目標リスト (UUID -> TrackedTarget)
    protected final Map<UUID, TrackedTarget> trackedTargets = new HashMap<>();
    // 記憶保持時間 (40 Tick = 2秒間、アンテナが回転して戻るまで記憶を維持)
    protected static final long TARGET_TIMEOUT_TICKS = 40L;

    public HorizontalRadarBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public abstract RadarScanRange getScanRange();

    /**
     * 最大探知距離の取得ヘルパー
     */
    public float getMaxScanRange() {
        return getScanRange().maxRange();
    }

    /**
     * 土台の設置方角（Facing）とアンテナ回転角（currentYaw）を合成した
     * ワールド座標系におけるアンテナの絶対方位角 (0〜360度) を取得
     */
    public float getWorldFacingYaw() {
        float baseFacingRot = getFacing().toYRot();
        float yaw = (this.currentYaw + baseFacingRot) % 360.0f;
        if (yaw < 0.0f) {
            yaw += 360.0f;
        }
        return yaw;
    }

    /**
     * アンテナの瞬時水平ビーム幅（度）
     * 360度全周捜索型の場合はスキャン中のビーム幅（例: 25度）、扇形・指向型の場合はその水平FOVを返す
     */
    public float getInstantaneousBeamHorizontal() {
        RadarScanRange range = getScanRange();
        return range.isOmni() ? 25.0f : range.horizontalFov();
    }

    /**
     * アンテナの瞬時垂直ビーム幅（度）
     */
    public float getInstantaneousBeamVertical() {
        RadarScanRange range = getScanRange();
        return range.maxPitch() - range.minPitch();
    }

    /**
     * アンテナの現在のPitch角（見上げ/見下ろし角度）
     */
    public float getRadarPitch() {
        return 0.0f;
    }

    public abstract void performScan();

    public void tickSensor() {
        if (this.level != null && !this.level.isClientSide && isPowered()) {
            performScan();
        }
    }

    /**
     * FCSコア等から渡されたターゲット候補エンティティを
     * アンテナの現在の方位角とビーム幅でフィルタリングし、追尾目標を更新する
     */
    @Override
    public void filterAndScan(List<Entity> candidates) {
        if (!isPowered() || this.level == null || this.level.isClientSide) return;

        Vector3f radarPos = new Vector3f(
                this.worldPosition.getX() + 0.5f,
                this.worldPosition.getY() + 1.0f,
                this.worldPosition.getZ() + 0.5f
        );

        float antennaWorldYaw = getWorldFacingYaw();
        float antennaPitch = getRadarPitch();
        RadarScanRange range = getScanRange();
        float maxRange = range.maxRange();
        float beamHoriz = getInstantaneousBeamHorizontal();
        float beamVert = getInstantaneousBeamVertical();

        List<Entity> detectedThisFrame = new ArrayList<>();
        for (Entity target : candidates) {
            if (!target.isAlive() || target.level() != this.level) continue;

            if (RadarMathUtil.isEntityInRadarFOV(
                    radarPos, antennaWorldYaw, antennaPitch,
                    target, maxRange, beamHoriz, beamVert
            )) {
                detectedThisFrame.add(target);
            }
        }

        updateTrackedTargets(detectedThisFrame);
        syncToClients();
    }

    /**
     * 探知したエンティティリストを元に内部の追尾記憶（trackedTargets）を更新する
     */
    protected void updateTrackedTargets(List<Entity> detectedThisFrame) {
        if (this.level == null) return;
        long currentGameTime = this.level.getGameTime();

        // 1. 今回ビーム内に入ったエンティティの記憶を新規登録 / 更新
        for (Entity entity : detectedThisFrame) {
            UUID uuid = entity.getUUID();
            if (trackedTargets.containsKey(uuid)) {
                trackedTargets.get(uuid).update(entity, currentGameTime);
            } else {
                trackedTargets.put(uuid, new TrackedTarget(entity, currentGameTime));
                this.onTargetDiscovered(entity);
            }
        }

        // 2. タイムアウトした古い目標を削除
        trackedTargets.values().removeIf(target -> {
            boolean expired = target.isExpired(currentGameTime, TARGET_TIMEOUT_TICKS) || !target.getEntity().isAlive();
            if (expired) {
                this.onTargetLost(target);
            }
            return expired;
        });

        // FCS用 TargetTrack リストも更新
        this.detectedTargets.clear();
        for (TrackedTarget tt : trackedTargets.values()) {
            this.detectedTargets.add(new TargetTrack(
                    tt.getEntityId(),
                    tt.getLastKnownPos(),
                    tt.getLastKnownVelocity(),
                    tt.getLastSeenGameTime(),
                    false
            ));
        }
    }

    protected void onTargetDiscovered(Entity entity) {}
    protected void onTargetLost(TrackedTarget target) {}

    @Override
    public Map<UUID, TrackedTarget> getTrackedTargets() {
        return this.trackedTargets;
    }

    @Override
    public void updateClientTrackedTargets(List<S2CSyncRadarTargetsPacket.TargetData> dataList) {
        if (this.level == null) return;
        long currentGameTime = this.level.getGameTime();

        this.trackedTargets.clear();
        for (S2CSyncRadarTargetsPacket.TargetData data : dataList) {
            this.trackedTargets.put(
                    data.uuid(),
                    new TrackedTarget(data.uuid(), data.name(), data.pos(), data.vel(), currentGameTime)
            );
        }
    }

    protected void syncToClients() {
        if (this.level == null || this.level.isClientSide) return;
        List<S2CSyncRadarTargetsPacket.TargetData> packetList = new ArrayList<>();
        for (TrackedTarget target : this.trackedTargets.values()) {
            String name = target.getEntity() != null ? target.getEntity().getType().getDescription().getString() : "Unknown";
            packetList.add(new S2CSyncRadarTargetsPacket.TargetData(
                    target.getEntityId(),
                    name,
                    target.getLastKnownPos(),
                    target.getLastKnownVelocity() != null ? target.getLastKnownVelocity() : Vec3.ZERO
            ));
        }
        PacketHandler.INSTANCE.send(
                PacketDistributor.TRACKING_CHUNK.with(() -> this.level.getChunkAt(this.worldPosition)),
                new S2CSyncRadarTargetsPacket(this.worldPosition, packetList)
        );
    }

    @Override
    public Direction getFacing() {
        return this.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public UUID getNetworkId() {
        return this.uuid;
    }

    @Override
    public boolean isConnectedToFcs() {
        return this.isFcsConnected;
    }

    @Override
    public void setFcsConnected(boolean connected) {
        this.isFcsConnected = connected;
        if (connected) {
            notifyScanRangeToCore();
        }
        syncToClient();
    }

    @Override
    public boolean isPowered() {
        return this.isPowered;
    }

    @Override
    public void setPowered(boolean powered) {
        if (this.isPowered != powered) {
            this.isPowered = powered;
            onPowerChanged(powered);
            syncToClient();
            setChanged();
        }
    }

    protected void onPowerChanged(boolean powered) {
        if (!powered) {
            this.trackedTargets.clear();
            this.detectedTargets.clear();
            if (this.level != null && !this.level.isClientSide) {
                syncToClients();
            }
        }
    }

    public void notifyScanRangeToCore() {
        if (this.level == null || this.level.isClientSide) return;
        if (this.linkedFcsCorePos != null && this.linkedFcsCoreUuid != null) {
            if (this.level.isLoaded(this.linkedFcsCorePos)) {
                BlockEntity be = this.level.getBlockEntity(this.linkedFcsCorePos);
                if (be instanceof AbstractFcsCoreBlockEntity fcsCore && fcsCore.getUuid().equals(this.linkedFcsCoreUuid)) {
                    fcsCore.updateSensorScanRange(this.uuid, getScanRange());
                }
            }
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide && this.isFcsConnected) {
            notifyScanRangeToCore();
        }
    }

    @Override
    public List<TargetTrack> getDetectedTargets() {
        return this.detectedTargets;
    }

    @Override
    public TargetTrack getPrimaryLockedTarget() {
        return this.primaryLockedTarget;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("FcsConnected", this.isFcsConnected);
        tag.putBoolean("Powered", this.isPowered);
        tag.putFloat("CurrentYaw", this.currentYaw);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        this.isFcsConnected = tag.getBoolean("FcsConnected");
        if (tag.contains("Powered")) {
            this.isPowered = tag.getBoolean("Powered");
        }
        if (tag.contains("CurrentYaw")) {
            this.currentYaw = tag.getFloat("CurrentYaw");
            this.prevYaw = this.currentYaw;
        }
    }

    @Override
    public float getTargetYaw(float partialTick) {
        float diff = this.currentYaw - this.prevYaw;
        if (diff < -180.0f) diff += 360.0f;
        if (diff > 180.0f) diff -= 360.0f;
        return this.prevYaw + diff * partialTick;
    }
}
