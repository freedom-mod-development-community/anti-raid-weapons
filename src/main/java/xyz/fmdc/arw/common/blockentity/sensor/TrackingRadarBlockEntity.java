package xyz.fmdc.arw.common.blockentity.sensor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import xyz.fmdc.arw.api.fcs.TargetTrack;
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
    public float getScanRange() {
        return 384.0f;
    }

    public boolean isActiveRadar() {
        return true;
    }

    @Override
    public void performScan() {
        if (lockedTarget != null) {
            // ロックオン目標の追従旋回および位置の精密更新処理（スケルトン）
            this.primaryLockedTarget = lockedTarget;
        }
    }

    public void setLockTarget(TargetTrack target) {
        this.lockedTarget = target;
    }
}