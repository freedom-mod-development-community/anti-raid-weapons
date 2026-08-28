package xyz.fmdc.arw.common.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import xyz.fmdc.arw.api.control.IRemoteControllableWeapon;
import xyz.fmdc.arw.registry.ModBlocks;

import java.util.UUID;

public class Mk45Mod4BlockEntity extends IntegratedWeaponBlockEntity implements IRemoteControllableWeapon {

    // Mk 45 Mod 4 固有のパラメータ設定
    private static final float YAW_TURN_SPEED = 4.0f;   // 1Tickあたり4度（高速旋回）
    private static final float PITCH_TURN_SPEED = 3.0f; // 1Tickあたり3度
    private static final float MIN_YAW = -150.0f;
    private static final float MAX_YAW = 150.0f;
    private static final float MIN_PITCH = -65.0f;     // 仰角（上向き）
    private static final float MAX_PITCH = 15.0f;      // 俯角（下向き）

    public static final float FIRE_ANIM_DURATION = 0.8f;
    public static final float RELOAD_ANIM_DURATION = 2.0f;

    private UUID controllerPlayerUUID = null;
    private int cooldownTicks = 0;

    public Mk45Mod4BlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.MK45_MOD4.getBEType(), pos, state);
        animationDurations.put("fire", FIRE_ANIM_DURATION);
        animationDurations.put("reload", RELOAD_ANIM_DURATION);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, Mk45Mod4BlockEntity be) {
        // 共通の武器旋回・アニメーション処理を実行
        be.tickWeapon();

        if (be.cooldownTicks > 0) {
            be.cooldownTicks--;
        }

        // 遠隔操作中ではなく、かつFCS未接続時のテスト・デフォルト挙動（任意で記述）
        if (!be.isBeingRemoteControlled() && !be.isConnectedToFcs()) {
            // Standby状態の維持など
        }
    }

    @Override
    public void fire() {
        if (!canFire()) return;
        playAnimation("fire", FIRE_ANIM_DURATION);
        playAnimation("reload", RELOAD_ANIM_DURATION);
        this.cooldownTicks = 40; // 発射間隔（例: 2秒＝20rpm）
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
}