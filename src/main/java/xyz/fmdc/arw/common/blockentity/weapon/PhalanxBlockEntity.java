package xyz.fmdc.arw.common.blockentity.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.state.BlockState;
import xyz.fmdc.arw.client.util.IDirectionalBlockEntity;
import xyz.fmdc.arw.common.entity.projectile.FiveInchAmmoType;
import xyz.fmdc.arw.common.entity.projectile.FiveInchShellEntity;
import xyz.fmdc.arw.registry.ModBlocks;
import xyz.fmdc.arw.registry.ModEntities;

/**
 * Phalanx (CIWS) 専用の BlockEntity。
 * ARWCIWSBlockEntity を継承し、ファランクス特有のパラメーターや動作を定義します。
 */
public class PhalanxBlockEntity extends ARWCIWSBlockEntity  implements IDirectionalBlockEntity {

    public PhalanxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.PHALANX.getBEType(), pos, state);
    }
    /* ------------------------------------------------------------------
     * 必要に応じて親クラスのプロパティや動作をオーバーライドする例
     * ------------------------------------------------------------------ */

    /**
     * 例: 発射アニメーション名やループの設定
     */
    public String getFiringAnimationName() {
        return "phalanx_fire";
    }

    @Override
    public EntityType<FiveInchShellEntity> getShellEntityType() {
        return ModEntities.FIVE_INCH_SHELL.get();
    }

    @Override
    public FiveInchAmmoType getSelectedAmmoType() {
        return FiveInchAmmoType.MK91_ILLUM_MT;
    }

    @Override
    public float getAnimationDuration(String animName) {
        return switch (animName) {
            case "start_fire" -> 0.5f; // 例: 0.5秒
            case "firing" -> 1.0f;     // 例: 1.0秒
            case "end_fire" -> 0.5f;   // 例: 0.5秒
            default -> 1.0f;
        };
    }

    @Override
    public float getRenderTargetYaw(float partialTick) {
        return currentYaw;
    }

    @Override
    public float getRenderTargetPitch(float partialTick) {
        return currentPitch;
    }

    @Override
    public float getRenderBarrelAng(float partialTick) {
        return this.barrelAngle;
    }
}
