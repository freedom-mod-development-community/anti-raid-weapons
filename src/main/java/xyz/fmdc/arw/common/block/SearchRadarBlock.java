package xyz.fmdc.arw.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.fmdc.arw.client.gui.RadarTest;
import xyz.fmdc.arw.common.blockentity.sensor.HorizontalRadarBlockEntity;
import xyz.fmdc.arw.common.blockentity.sensor.SearchRadarBlockEntity;

public class SearchRadarBlock extends BaseEntityBlock {
    public SearchRadarBlock(Properties properties) { super(properties); }

    @Override public @NotNull RenderShape getRenderShape(@NotNull BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }

    @Nullable @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new SearchRadarBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        return (lvl, pos, st, be) -> {
            if (be instanceof HorizontalRadarBlockEntity sensor) sensor.tickSensor();
        };
    }

    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player,
                                          @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {

        // クライアント側（描画側）でのみGUIを開く
        if (level.isClientSide) {
            openControlScreen(pos);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private void openControlScreen(BlockPos pos) {
        net.minecraft.client.Minecraft.getInstance().setScreen(new RadarTest(pos));
    }
}