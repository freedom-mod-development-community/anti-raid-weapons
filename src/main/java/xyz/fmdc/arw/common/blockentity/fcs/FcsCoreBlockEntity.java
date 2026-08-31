package xyz.fmdc.arw.common.blockentity.fcs;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import xyz.fmdc.arw.registry.ModBlocks;

public class FcsCoreBlockEntity extends AbstractFcsCoreBlockEntity {

    public FcsCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.FCS_CORE_BLOCK.getBEType(), pos, state);
    }
}