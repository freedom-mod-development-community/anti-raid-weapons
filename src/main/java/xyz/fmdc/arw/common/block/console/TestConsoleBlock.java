package xyz.fmdc.arw.common.block.console;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.fmdc.arw.client.gui.screen.TestConsoleScreen;
import xyz.fmdc.arw.common.blockentity.console.TestConsoleBlockEntity;
import xyz.fmdc.arw.common.item.tool.FcsConnectorItem;

/**
 * 動作確認用コンソールのブロック.
 * 外観および基本プロパティはエメラルドブロックに準拠します。
 */
public class TestConsoleBlock extends BaseEntityBlock {

    public TestConsoleBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new TestConsoleBlockEntity(pos, state);
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                                         @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        // FCS コネクターアイテムを手prefixに持っている場合は接続処理を優先
        if (player.getItemInHand(hand).getItem() instanceof FcsConnectorItem) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> openControlScreen(pos));
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @OnlyIn(Dist.CLIENT)
    private void openControlScreen(BlockPos pos) {
        net.minecraft.client.Minecraft.getInstance().setScreen(new TestConsoleScreen(pos));
    }
}
