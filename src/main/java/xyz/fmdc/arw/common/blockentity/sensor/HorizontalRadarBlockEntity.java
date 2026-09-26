package xyz.fmdc.arw.common.blockentity.sensor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.NotNull;
import xyz.fmdc.arw.api.TrackedTarget;
import xyz.fmdc.arw.api.blockentity.IDirectionalBlockEntity;
import xyz.fmdc.arw.api.blockentity.IYawModel;
import xyz.fmdc.arw.api.fcs.IFcsSensorNode;
import xyz.fmdc.arw.api.fcs.TargetTrack;
import xyz.fmdc.arw.api.sensor.ITrackedTargetHolder;
import xyz.fmdc.arw.api.sensor.RadarScanRange;
import xyz.fmdc.arw.common.blockentity.AbstractARWBlockEntity;
import xyz.fmdc.arw.common.blockentity.fcs.AbstractFcsCoreBlockEntity;
import xyz.fmdc.arw.network.S2CSyncRadarTargetsPacket;

import java.util.*;

/**
 * センサーアンテナの姿勢制御およびパラメータ提供を行う基底クラス。
 * 目標の探知・追尾状態は自前では保持せず、FCSコアへ一元化される。
 */
public abstract class HorizontalRadarBlockEntity extends AbstractARWBlockEntity
        implements IYawModel, IFcsSensorNode, IDirectionalBlockEntity, ITrackedTargetHolder {

    protected boolean isFcsConnected = false;
    protected boolean isPowered = true;

    // アンテナ回転状態
    protected float currentYaw = 0.0f;
    protected float prevYaw = 0.0f;
    protected float currentScanAngle = 0.0f;

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
     * 土台の設置方位（Facing）とアンテナ回転角（currentYaw）を合成した
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

    @Override
    public float getAntennaYaw() {
        return getWorldFacingYaw();
    }

    @Override
    public float getAntennaPitch() {
        return getRadarPitch();
    }

    /**
     * アンテナの瞬時水平ビーム幅（度）
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
     * FCSコア等からのレガシー呼び出しに対する空実装（判定はFCSコア側へ集約）
     */
    @Override
    public void filterAndScan(List<Entity> candidates) {
        // レーダーBE側では重い走査・ターゲット保持を行わない
    }

    @Override
    public Map<UUID, TrackedTarget> getTrackedTargets() {
        // リンクされたFCSコアがあればそこから委譲、なければ空
        if (this.level != null && this.linkedFcsCorePos != null && this.level.isLoaded(this.linkedFcsCorePos)) {
            BlockEntity be = this.level.getBlockEntity(this.linkedFcsCorePos);
            if (be instanceof AbstractFcsCoreBlockEntity core) {
                return core.getTrackedTargets();
            }
        }
        return Collections.emptyMap();
    }

    @Override
    public void updateClientTrackedTargets(List<S2CSyncRadarTargetsPacket.TargetData> dataList) {
        // レーダーBE側では個別同期を行わない
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
            syncToClient();
            setChanged();
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
        return Collections.emptyList();
    }

    @Override
    public TargetTrack getPrimaryLockedTarget() {
        return null;
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
