package xyz.fmdc.arw.common.block.sensor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.fmdc.arw.client.gui.RadarTest;
import xyz.fmdc.arw.common.block.ARWBaseEntityBlock;
import xyz.fmdc.arw.common.blockentity.sensor.SearchRadarBlockEntity;
import xyz.fmdc.arw.common.item.FcsConnectorItem;
import xyz.fmdc.arw.registry.ModBlocks;

public class SearchRadarBlock extends ARWBaseEntityBlock {
    public SearchRadarBlock(Properties properties) { super(properties); }

    @Override public @NotNull RenderShape getRenderShape(@NotNull BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }

    @Nullable @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new SearchRadarBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        // BlockEntityType が一致しているか検証して Ticker を返す
        return createTickerHelper(blockEntityType, ModBlocks.SEARCH_RADAR_BLOCK.getBEType(), SearchRadarBlockEntity::tick);
    }

    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player,
                                          @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() instanceof FcsConnectorItem) {
            return InteractionResult.PASS;
        }

        // クライアント側（描画側）でのみGUIを開く
        if (level.isClientSide) {
            // 1. BlockEntity を取得
            BlockEntity be = level.getBlockEntity(pos);

// 2. 目的の BlockEntity 型（SearchRadarBlockEntity）にキャストして処理
            if (be instanceof SearchRadarBlockEntity radarBe) {
                // 取得成功：GUIを開く、または処理を行う
                openControlScreen(radarBe);
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private void openControlScreen(SearchRadarBlockEntity be) {
        net.minecraft.client.Minecraft.getInstance().setScreen(new RadarTest(be));
    }
}
