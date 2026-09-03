package xyz.fmdc.arw.common.block.weapon.launcher;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.fmdc.arw.common.block.ARWBaseEntityBlock;
import xyz.fmdc.arw.common.blockentity.weapon.BonedMissileLauncherBlockEntity;
import xyz.fmdc.arw.registry.ModBlocks;

public class BonedMissileLauncherBlock extends ARWBaseEntityBlock {

    public BonedMissileLauncherBlock(Properties properties) {
        super(properties);
    }

    @Override public @NotNull RenderShape getRenderShape(@NotNull BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new BonedMissileLauncherBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlocks.MK13BONE.getBEType(), BonedMissileLauncherBlockEntity::tick);
    }
}
