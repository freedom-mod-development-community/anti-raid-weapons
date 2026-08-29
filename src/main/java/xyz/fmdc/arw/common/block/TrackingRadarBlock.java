package xyz.fmdc.arw.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import xyz.fmdc.arw.common.blockentity.sensor.AbstractHRadarBlockEntity;
import xyz.fmdc.arw.common.blockentity.sensor.TrackingRadarBlockEntity;

public class TrackingRadarBlock extends BaseEntityBlock {
    public TrackingRadarBlock(Properties properties) { super(properties); }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TrackingRadarBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return (lvl, pos, st, be) -> {
            if (be instanceof AbstractHRadarBlockEntity sensor) sensor.tickSensor();
        };
    }
}

// OpticalSightBlock, TrackingRadarBlock も上記と同様に newBlockEntity の戻り値をそれぞれの BlockEntity に差し替えて作成します。