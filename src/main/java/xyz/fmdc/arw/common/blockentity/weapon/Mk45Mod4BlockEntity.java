package xyz.fmdc.arw.common.blockentity.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xyz.fmdc.arw.api.control.IRemoteControllableWeapon;
import xyz.fmdc.arw.common.blockentity.AbstractSingleGunBlockEntity;
import xyz.fmdc.arw.common.entity.projectile.FiveInchAmmoType;
import xyz.fmdc.arw.common.entity.projectile.FiveInchShellEntity;
import xyz.fmdc.arw.registry.ModBlocks;
import xyz.fmdc.arw.registry.ModEntities;

import java.util.UUID;

/**
 * MK45Mod4
 */
public class Mk45Mod4BlockEntity extends AbstractSingleGunBlockEntity implements IRemoteControllableWeapon {

    // Mk 45 Mod 4 固有のパラメータ設定
    private static final float YAW_TURN_SPEED = 4.0f;   // 1Tickあたり4度（高速旋回）
    private static final float PITCH_TURN_SPEED = 3.0f; // 1Tickあたり3度
    private static final float MIN_YAW = -190.0f;
    private static final float MAX_YAW = 190.0f;
    private static final float MIN_PITCH = -65.0f;     // 仰角（上向き）
    private static final float MAX_PITCH = 15.0f;      // 俯角（下向き）

    public static final float FIRE_ANIM_DURATION = 0.8f;
    public static final float RELOAD_ANIM_DURATION = 2.0f;

    private int tickCounter = 0;

    private FiveInchAmmoType currentAmmo = FiveInchAmmoType.MK80_HE_PD;


    private UUID controllerPlayerUUID = null;

    public Mk45Mod4BlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.MK45_MOD4.getBEType(), pos, state);
        animationDurations.put("fire", FIRE_ANIM_DURATION);
        animationDurations.put("reload", RELOAD_ANIM_DURATION);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, Mk45Mod4BlockEntity be) {
        // 共通の武器旋回・アニメーション処理を実行
        be.tickSingleGun();

        if (be.cooldownTicks > 0) {
            be.cooldownTicks--;
        }

        // 遠隔操作中ではなく、かつFCS未接続時のテスト・デフォルト挙動（任意で記述）
        if (!be.isBeingRemoteControlled() && !be.isConnectedToFcs()) {
            // Standby状態の維持など
        }
        // テスト用：発射処理
        if (!level.isClientSide) {
            if (be.tickCounter % 120 == 0) {
                be.fire();
            }
        }
        be.tickCounter++;
    }

    @Override
    public void fire() {
        if (!canFire()) return;
        playAnimation("fire", FIRE_ANIM_DURATION);
        playAnimation("reload", RELOAD_ANIM_DURATION);
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
        return Vec3.directionFromRotation(this.currentPitch, this.currentYaw);
    }

    @Override
    public Vec3 getMuzzleOffset() {
        float barrelLength = 7.7f; // 砲身の長さ
        double pivotHeight = 2.1749 - 0.5; // 旋回軸の高さ（ブロック底部または中心からのYオフセット）

        Vec3 dir = getFiringDirection();

        // 射撃方向に砲身長さを掛け、旋回軸の基準高さを加算
        return new Vec3(
                dir.x * barrelLength,
                dir.y * barrelLength + pivotHeight,
                dir.z * barrelLength
        );
    }

    @Override
    public int getMaxCooldownTicks() {
        // Mk45 (Mod 4) 連射速度: 約20発/分 ➔ 1発あたり 3秒 (60 ticks)
        return 60;
    }

    @Override
    protected boolean canFire() {
        return this.cooldownTicks <= 0;
    }

    // --- 旋回性能定義のオーバーライド ---
    @Override protected float getYawTurnSpeed() { return YAW_TURN_SPEED; }
    @Override protected float getPitchTurnSpeed() { return PITCH_TURN_SPEED; }
    @Override protected float getMinYaw() { return MIN_YAW; }
    @Override protected float getMaxYaw() { return MAX_YAW; }
    @Override protected float getMinPitch() { return MIN_PITCH; }
    @Override protected float getMaxPitch() { return MAX_PITCH; }

    // --- IRemoteControllableWeapon の実装 ---
    @Override
    public Vec3 getCameraPosition() {
        // 砲頭カメラの位置オフセット（例: ブロック中心から上方1.8m、前方0.5m）
        return Vec3.atCenterOf(this.worldPosition).add(0.0, 1.8, 0.5);
    }

    @Override
    public boolean isBeingRemoteControlled() {
        return this.controllerPlayerUUID != null;
    }

    @Override
    public void startRemoteControl(Player player) {
        this.controllerPlayerUUID = player.getUUID();
        syncToClient();
    }

    @Override
    public void stopRemoteControl(Player player) {
        this.controllerPlayerUUID = null;
        syncToClient();
    }

    @Override
    public void handleRemoteInput(float yawInput, float pitchInput, boolean triggerFire) {
        // FCSの命令をオーバーライドして、プレイヤー操作を優先設定
        setTargetYaw(yawInput);
        setTargetPitch(pitchInput);

        if (triggerFire && canFire()) {
            fire();
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        // 例: 上下に3ブロック、東西南北に3ブロック分領域を拡張
        return new AABB(this.worldPosition).inflate(5.0);
    }
}