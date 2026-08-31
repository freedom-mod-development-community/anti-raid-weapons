package xyz.fmdc.arw.common.blockentity.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import xyz.fmdc.arw.api.control.IRemoteControllableWeapon;
import xyz.fmdc.arw.common.blockentity.AbstractSingleGunBlockEntity;
import xyz.fmdc.arw.common.entity.projectile.FiveInchAmmoType;
import xyz.fmdc.arw.common.entity.projectile.FiveInchShellEntity;
import xyz.fmdc.arw.registry.ModBlocks;
import xyz.fmdc.arw.registry.ModEntities;

/**
 * FCSの自動追従機能を持たず、遠隔カメラ映像越しにプレイヤーが直接マウス等で動かす小口径RWS
 */
public class ManualRwsGunBlockEntity extends AbstractSingleGunBlockEntity implements IRemoteControllableWeapon {

    private Player controllingPlayer = null;
    private FiveInchAmmoType currentAmmo = FiveInchAmmoType.MK80_HE_PD;

    public ManualRwsGunBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.MANUAL_RWS_GUN_BLOCK.getBEType(), pos, state);
    }

    @Override protected float getYawTurnSpeed() { return 12.0f; }
    @Override protected float getPitchTurnSpeed() { return 10.0f; }
    @Override protected float getMinYaw() { return -180.0f; }
    @Override protected float getMaxYaw() { return 180.0f; }
    @Override protected float getMinPitch() { return -20.0f; }
    @Override protected float getMaxPitch() { return 60.0f; }

    @Override
    public void fire() {
        fireProcess();
        this.cooldownTicks = 60; // 発射間隔（例: 2秒＝20rpm）
    }

    @Override
    public FiveInchAmmoType getSelectedAmmoType() {
        return this.currentAmmo;
    }

    @Override
    public EntityType<FiveInchShellEntity> getShellEntityType() {
        // 登録済みの 5インチ砲弾 ModEntities.FIVE_INCH_SHELL.get() など
        return ModEntities.FIVE_INCH_SHELL.get();
    }

    @Override
    public Vec3 getFiringDirection() {
        // 砲塔の現在方位角（Pitch/Yaw）からベクトルを計算、あるいはブロックの向き
        return new Vec3(0, 0.2, 1);
    }

    @Override
    public Vec3 getMuzzleOffset() {
        // Mk45の砲塔中心から長砲身先端までのオフセット（例: 前方に3.5m, 高さに1.2m）
        return new Vec3(0.0, 1.2, 3.5);
    }

    @Override
    public int getMaxCooldownTicks() {
        // Mk45 (Mod 4) 連射速度: 約20発/分 ➔ 1発あたり 3秒 (60 ticks)
        return 60;
    }

    @Override
    protected boolean canFire() {
        return false;
    }

    // --- IRemoteControllableWeapon の実装 ---
    @Override
    public Vec3 getCameraPosition() {
        return Vec3.atCenterOf(this.worldPosition).add(0.0, 1.2, 0.0);
    }

    @Override public boolean isBeingRemoteControlled() { return this.controllingPlayer != null; }
    @Override public void startRemoteControl(Player player) { this.controllingPlayer = player; }
    @Override public void stopRemoteControl(Player player) { this.controllingPlayer = null; }

    @Override
    public void handleRemoteInput(float yawInput, float pitchInput, boolean triggerFire) {
        setTargetYaw(yawInput);
        setTargetPitch(pitchInput);
        if (triggerFire) {
            fire();
        }
    }
}
