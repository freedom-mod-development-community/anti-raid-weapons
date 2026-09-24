package xyz.fmdc.arw.common.blockentity.sensor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import xyz.fmdc.arw.api.fcs.TargetTrack;
import xyz.fmdc.arw.api.sensor.RadarScanRange;
import xyz.fmdc.arw.registry.ModBlocks;

/**
 * 特定の1目標に旋回追従・電波照射し高精度ロックオンデータを出力する照射レーダー（STIR等）
 */
public class TrackingRadarBlockEntity extends HorizontalRadarBlockEntity {

    private TargetTrack lockedTarget = null;

    public TrackingRadarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.TRACKING_RADAR_BLOCK.getBEType(), pos, state);
    }

    @Override
    public RadarScanRange getScanRange() {
        return RadarScanRange.directional(384.0f, 60.0f, -10.0f, 85.0f);
    }

    public boolean isActiveRadar() {
        return isPowered();
    }

    @Override
    public void performScan() {
        if (!isPowered()) return;
        if (lockedTarget != null) {
            // ロックオン目標の追従旋回および位置の精密更新処理（スケルトン）
        }
    }

    public void setLockTarget(TargetTrack target) {
        this.lockedTarget = target;
    }

    @Override
    public TargetTrack getPrimaryLockedTarget() {
        return this.lockedTarget;
    }
}
