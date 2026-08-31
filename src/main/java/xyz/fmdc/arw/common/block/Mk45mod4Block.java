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
import xyz.fmdc.arw.client.gui.Mk45TestGUI;
import xyz.fmdc.arw.common.blockentity.weapon.Mk45Mod4BlockEntity;
import xyz.fmdc.arw.common.item.FcsConnectorItem;
import xyz.fmdc.arw.registry.ModBlocks;

public class Mk45mod4Block extends BaseEntityBlock {

    public Mk45mod4Block(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new Mk45Mod4BlockEntity(pos, state);
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlocks.MK45_MOD4.getBEType(), Mk45Mod4BlockEntity::tick);
    }

    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player,
                                          @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() instanceof FcsConnectorItem) {
            return InteractionResult.PASS;
        }

        // クライアント側（描画側）でのみGUIを開く
        if (level.isClientSide) {
            openControlScreen(pos);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // @OnlyIn(Dist.CLIENT) 相当の呼び出し分離（サーバー側でのクラスロードエラー防止）
    private void openControlScreen(BlockPos pos) {
        net.minecraft.client.Minecraft.getInstance().setScreen(new Mk45TestGUI(pos));
    }
}
