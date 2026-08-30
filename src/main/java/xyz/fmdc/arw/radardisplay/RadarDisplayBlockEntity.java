package xyz.fmdc.arw.radardisplay;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import xyz.fmdc.arw.registry.ModBlocks;

public class RadarDisplayBlockEntity extends BlockEntity {
    public RadarDisplayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.RADAR_DISPLAY.getBEType(), pos, state);
    }
}
