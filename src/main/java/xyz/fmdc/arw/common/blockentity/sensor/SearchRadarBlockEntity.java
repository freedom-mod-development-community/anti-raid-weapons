package xyz.fmdc.arw.common.blockentity.sensor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import xyz.fmdc.arw.registry.ModBlocks;

/**
 * 広域を周回/首振りスキャンし、複数目標（List<TargetTrack>）を出力する広域捜索レーダー（OPS-39等）
 */
public class SearchRadarBlockEntity extends FcsRadarBlockEntity {

    private float rotationSpeed = 12.0f; // 毎tickの周回回転角

    public SearchRadarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.SEARCH_RADAR_BLOCK.getBEType(), pos, state);
    }

    @Override
    public float getScanRange() {
        return 512.0f;
    }

    @Override
    public boolean isActiveRadar() {
        return true;
    }

    @Override
    public void performScan() {
        this.currentScanAngle = (this.currentScanAngle + rotationSpeed) % 360.0f;
        // 周囲360度の広域エンティティ探知および detectedTargets への追加処理（スケルトン）
    }
}
