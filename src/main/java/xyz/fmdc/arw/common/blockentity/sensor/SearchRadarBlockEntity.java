package xyz.fmdc.arw.common.blockentity.sensor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import xyz.fmdc.arw.api.sensor.RadarScanRange;
import xyz.fmdc.arw.registry.ModBlocks;

/**
 * 広域を周回スキャンし、探知パラメータを提供する広域捜索レーダー（OPS-39等）
 */
public class SearchRadarBlockEntity extends HorizontalRadarBlockEntity {

    private final float RPM = 30f;
    private final float rotationSpeed = RPM * 360 / (60 * 20); // 毎Tick回転する速度

    public SearchRadarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.SEARCH_RADAR_BLOCK.getBEType(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SearchRadarBlockEntity be) {
        // 電源OFFの場合はアンテナ回転を停止
        if (!be.isPowered()) {
            be.prevYaw = be.currentYaw;
            return;
        }

        be.tickSensor();
        be.prevYaw = be.currentYaw;
        be.currentYaw = (be.currentYaw + be.rotationSpeed) % 360.0f;
    }

    @Override
    public RadarScanRange getScanRange() {
        return RadarScanRange.omni(512.0f);
    }

    @Override
    public float getInstantaneousBeamHorizontal() {
        return 25.0f; // 瞬時水平ビーム幅25度
    }

    @Override
    public void performScan() {
        this.currentScanAngle = (this.currentScanAngle + rotationSpeed) % 360.0f;
    }
}
