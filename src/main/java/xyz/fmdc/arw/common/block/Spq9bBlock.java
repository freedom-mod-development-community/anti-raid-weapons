package xyz.fmdc.arw.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.fmdc.arw.registry.ModBlocks;
import xyz.fmdc.arw.common.blockentity.sensor.Spq9bBlockEntity;

public class Spq9bBlock extends BaseEntityBlock {

    public Spq9bBlock(Properties properties) {
        super(properties);
    }

    // BlockEntity（データ・回転処理）の生成
    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new Spq9bBlockEntity(pos, state);
    }

    // 毎Tickの回転処理（tickメソッド）を呼ぶための設定
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlocks.SPQ9B.getBEType(), Spq9bBlockEntity::tick);
    }

    // バニラのブロックレンダラー（キューブ描画）を無効化し、BER (Ops39Renderer) のみで描画させる
    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }
}