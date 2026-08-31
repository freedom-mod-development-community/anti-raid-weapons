package xyz.fmdc.arw.common.blockentity.vls;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import xyz.fmdc.arw.common.blockentity.AbstractARWBlockEntity;
import xyz.fmdc.arw.registry.ModBlocks;

/**
 * 物理的な砲塔旋回（Yaw/Pitch）を持たず、FCSからの発射承認・目標座標指示のみでミサイルを出射する完全自動垂直発射機
 */
public class VlsBlockEntity extends AbstractARWBlockEntity {

    public void tickVLS() {
    }

    public VlsBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.VLS_BLOCK.getBEType(), pos, state);
    }
}