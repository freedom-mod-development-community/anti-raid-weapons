package xyz.fmdc.arw.common.blockentity.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import xyz.fmdc.arw.common.blockentity.AbstractGunBlockEntity;
import xyz.fmdc.arw.registry.ModBlocks;

/**
 * 物理的な砲塔旋回（Yaw/Pitch）を持たず、FCSからの発射承認・目標座標指示のみでミサイルを出射する完全自動垂直発射機
 */
public class VlsBlockEntity extends AbstractGunBlockEntity {

    private int reloadCooldown = 0;

    public VlsBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.VLS_BLOCK.getBEType(), pos, state);
    }

    @Override protected float getYawTurnSpeed() { return 0.0f; } // 旋回なし
    @Override protected float getPitchTurnSpeed() { return 0.0f; }
    @Override protected float getMinYaw() { return 0.0f; }
    @Override protected float getMaxYaw() { return 0.0f; }
    @Override protected float getMinPitch() { return 90.0f; } // 常に真上固定
    @Override protected float getMaxPitch() { return 90.0f; }

    @Override
    public void fire() {
        if (!canFire()) return;
        playAnimation("hatch_open", 1.0f);
        playAnimation("launch", 0.5f);
        this.reloadCooldown = 100;
    }

    @Override
    protected boolean canFire() {
        return this.reloadCooldown <= 0;
    }
}