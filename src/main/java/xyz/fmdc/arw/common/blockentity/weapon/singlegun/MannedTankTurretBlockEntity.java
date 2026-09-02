package xyz.fmdc.arw.common.blockentity.weapon.singlegun;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import xyz.fmdc.arw.api.control.IDirectMannedWeapon;
import xyz.fmdc.arw.common.blockentity.AbstractSingleGunBlockEntity;
import xyz.fmdc.arw.common.entity.projectile.FiveInchAmmoType;
import xyz.fmdc.arw.common.entity.projectile.FiveInchShellEntity;
import xyz.fmdc.arw.registry.ModBlocks;
import xyz.fmdc.arw.registry.ModEntities;

/**
 * 砲手として砲塔内に乗り込みつつ、FCSからリアルタイムに偏差計算アシスト（FiringSolution）を受けて射撃する近代戦車砲
 */
public class MannedTankTurretBlockEntity extends AbstractSingleGunBlockEntity implements IDirectMannedWeapon {

    private Player mountedPlayer = null;
    private FiveInchAmmoType currentAmmo = FiveInchAmmoType.MK80_HE_PD;

    public MannedTankTurretBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.MANNED_TANK_TURRET_BLOCK.getBEType(), pos, state);
    }

    @Override protected float getYawTurnSpeed() { return 6.0f; }
    @Override protected float getPitchTurnSpeed() { return 4.0f; }
    @Override protected float getMinYaw() { return -180.0f; }
    @Override protected float getMaxYaw() { return 180.0f; }
    @Override protected float getMinPitch() { return -10.0f; }
    @Override protected float getMaxPitch() { return 20.0f; }

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
        return true;
    }

    // --- IDirectMannedWeapon の実装 ---
    @Override public boolean isManned() { return this.mountedPlayer != null; }
    @Override public Player getControllingPlayer() { return this.mountedPlayer; }

    @Override
    public void mountPlayer(Player player) {
        this.mountedPlayer = player;
    }

    @Override
    public void dismountPlayer() {
        this.mountedPlayer = null;
    }

    @Override
    public void handleMannedInput(float yawDelta, float pitchDelta, boolean triggerFire) {
        // FCSの自動追従に対して手動で補正・微調整を加える処理（スケルトン）
        setTargetYaw(this.targetYaw + yawDelta);
        setTargetPitch(this.targetPitch + pitchDelta);
        if (triggerFire && canFire()) {
            fire();
        }
    }
}
