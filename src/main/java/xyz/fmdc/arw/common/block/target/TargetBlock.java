package xyz.fmdc.arw.common.block.target;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.fmdc.arw.api.RadarTargetManager;
import xyz.fmdc.arw.common.blockentity.target.TargetBlockEntity;

/**
 * レーダー探知可能な標的用ブロック。
 * /setblock arw:target_block 等のコマンドでも配置可能。
 * レーダー探知対象（TrackedTarget）として機能し、破壊時に座標・破壊者の詳細ログを出力する。
 */
public class TargetBlock extends BaseEntityBlock {

    public TargetBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new TargetBlockEntity(pos, state);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide && !state.is(oldState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TargetBlockEntity targetBE) {
                RadarTargetManager.INSTANCE.registerTargetBlock(targetBE);
            }
        }
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TargetBlockEntity targetBE) {
                targetBE.onPlayerDestroy(player);
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TargetBlockEntity targetBE) {
                targetBE.onExplosionDestroy(explosion);
            }
        }
        super.onBlockExploded(state, level, pos, explosion);
    }

    @Override
    public void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        super.onProjectileHit(level, state, hit, projectile);
        if (!level.isClientSide) {
            BlockPos pos = hit.getBlockPos();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TargetBlockEntity targetBE) {
                targetBE.onProjectileDestroy(projectile, hit);
            }
            level.destroyBlock(pos, false);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof TargetBlockEntity targetBE) {
                    targetBE.onGenericDestroy();
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}
