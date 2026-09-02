package xyz.fmdc.arw.common.blockentity.sensor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.NotNull;
import xyz.fmdc.arw.api.fcs.IFcsSensorNode;
import xyz.fmdc.arw.api.fcs.TargetTrack;
import xyz.fmdc.arw.client.util.IDirectionalBlockEntity;
import xyz.fmdc.arw.client.util.IYawModel;
import xyz.fmdc.arw.common.blockentity.AbstractARWBlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ターゲットスキャンと方位計算を行う全センサーの基底クラス
 */
public abstract class HorizontalRadarBlockEntity extends AbstractARWBlockEntity
        implements IYawModel, IFcsSensorNode, IDirectionalBlockEntity {

    protected boolean isFcsConnected = false;
    protected final List<TargetTrack> detectedTargets = new ArrayList<>();
    protected TargetTrack primaryLockedTarget = null;

    protected float currentYaw = 0.0f;
    protected float prevYaw = 0.0f;

    protected float currentScanAngle = 0.0f;

    public HorizontalRadarBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public abstract float getScanRange();
    public abstract void performScan();

    public void tickSensor() {
        if (this.level != null && !this.level.isClientSide) {
            performScan();
        }
    }
    @Override
    public Direction getFacing(){
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
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        this.isFcsConnected = tag.getBoolean("FcsConnected");
    }

    @Override
    public float getTargetYaw(float partialTick) {
        return Mth.rotLerp(partialTick, this.prevYaw, this.currentYaw);
    }
}
