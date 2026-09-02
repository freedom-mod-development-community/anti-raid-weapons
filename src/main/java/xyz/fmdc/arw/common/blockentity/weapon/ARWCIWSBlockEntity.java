package xyz.fmdc.arw.common.blockentity.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import xyz.fmdc.arw.client.renderer.GenericFastGlbRenderer;
import xyz.fmdc.arw.client.util.IDirectionalBlockEntity;
import xyz.fmdc.arw.client.util.IYawPitchBarrelAnimatableModel;
import xyz.fmdc.arw.common.blockentity.AbstractARWBlockEntity;
import xyz.fmdc.arw.common.entity.projectile.FiveInchAmmoType;
import xyz.fmdc.arw.common.entity.projectile.FiveInchShellEntity;

import java.util.ArrayList;
import java.util.List;

public abstract class ARWCIWSBlockEntity extends AbstractARWBlockEntity implements IYawPitchBarrelAnimatableModel, IDirectionalBlockEntity {

    // CIWS アニメーションステート
    public enum FiringState {
        IDLE,
        START_FIRE,
        FIRING,
        END_FIRE
    }

    private FiringState currentState = FiringState.IDLE;
    private long stateStartTime = 0;
    private boolean isFiringTarget = false; // 発射フラグ（外部/サーバーから切り替え）

    private float currentSpinSpeed = 0.0f; // 現在の回転速度 (deg/sec)
    protected float barrelAngle = 0.0f;       // 現在の回転角度 (deg)

    private final float MAX_SPIN_SPEED = 3600.0f; // 最高回転速度 (例: 1秒間に10回転)
    private final float ACCEL = 1800.0f;          // 加速度 (deg/s^2)
    private final float DECEL = 1200.0f;          // 減速加速度 (deg/s^2)

    // 角度（Yaw / Pitch）
    protected float currentYaw = 0.0f;
    protected float currentPitch = 0.0f;

    private int firingTimer = 0; // 残り発砲時間（Tick単位: 20 Tick = 1秒）

    public ARWCIWSBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * 右クリックなどで発砲を一定時間開始させるメソッド
     * @param ticks 発砲を継続するTick数（5秒 = 100）
     */
    public void startFiringFor(int ticks) {
        this.firingTimer = ticks;
        if (!this.isFiringTarget) {
            setFiring(true); // 発砲フラグを立ててクライアントと同期
        }
    }

    // --- 抽象プロパティ設定（子クラス側で指定） ---

    /** 生成する砲弾の EntityType */
    public abstract EntityType<FiveInchShellEntity> getShellEntityType();

    /** 使用する砲弾 Enum */
    public abstract FiveInchAmmoType getSelectedAmmoType();

    /** 各アニメーションの再生時間（秒）を取得 */
    public abstract float getAnimationDuration(String animName);

    /** 初速パラメータ */
    public float getMuzzleVelocity() {
        return 12.0F; // CIWS用に高速化
    }

    /** 射撃時の効果音 */
    public SoundEvent getFireSound() {
        return SoundEvents.GENERIC_EXPLODE;
    }

    // --- 角度・位置ベクトルの計算 ---

    @Override
    public Direction getFacing(){
        return this.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
    }

    public Vec3 getFiringDirection() {
        // ブロックの回転オフセットを加算
        float effectiveYaw = this.currentYaw + getFacing().toYRot();
        return Vec3.directionFromRotation(this.currentPitch, effectiveYaw);
    }

    public Vec3 getMuzzleOffset() {
        float barrelLength = 1.7f;
        double pivotHeight = 1.1749 - 0.5;

        Vec3 dir = getFiringDirection();
        return new Vec3(
                dir.x * barrelLength,
                dir.y * barrelLength + pivotHeight,
                dir.z * barrelLength
        );
    }

    // --- 発射制御メソッド ---

    public void setFiring(boolean firing) {
        if (this.isFiringTarget != firing) {
            this.isFiringTarget = firing;
            syncToClient();
        }
    }

    public boolean isFiring() {
        return this.isFiringTarget;
    }

    // --- Tick 処理（ステート遷移 & 毎Tick発射） ---

    public static void tick(Level level, BlockPos pos, BlockState state, ARWCIWSBlockEntity be) {

        if (!level.isClientSide && be.firingTimer > 0) {
            be.firingTimer--;
            if (be.firingTimer <= 0) {
                be.setFiring(false); // 5秒経過したら発砲停止
            }
        }

        // 射撃中なら加速、止まったら慣性で減速
        if (be.isFiringTarget) {
            be.currentSpinSpeed = Math.min(be.MAX_SPIN_SPEED, be.currentSpinSpeed + be.ACCEL);
        } else {
            be.currentSpinSpeed = Math.max(0.0f, be.currentSpinSpeed - be.DECEL);
        }

        // 角度の更新
        be.barrelAngle = (be.barrelAngle + be.currentSpinSpeed) % 360.0f;

        // 2. 実射撃処理（FIRING ステートかつ 毎Tick 発射）
        if (be.isFiring()) {
            be.executeTickFire(level);
        }
    }

    private void transitionTo(FiringState newState, long gameTime) {
        this.currentState = newState;
        this.stateStartTime = gameTime;
        this.setChanged();
    }

    /** 毎Tick実行される1発ずつの発射処理 */
    protected void executeTickFire(Level level) {
        Vec3 direction = getFiringDirection().normalize();
        Vec3 muzzlePos = Vec3.atCenterOf(this.worldPosition).add(getMuzzleOffset());

        // サウンド再生（連射用にピッチに微小なゆらぎを付与）
        float pitchVariance = 0.9F + level.random.nextFloat() * 0.2F;
        level.playSound(
                null,
                BlockPos.containing(muzzlePos),
                getFireSound(),
                SoundSource.BLOCKS,
                6.0F,
                pitchVariance
        );

        if (!level.isClientSide) {
            // 弾丸エンティティ生成
            FiveInchShellEntity shell = new FiveInchShellEntity(getShellEntityType(), level);
            shell.setPos(muzzlePos.x, muzzlePos.y, muzzlePos.z);
            shell.setAmmoType(getSelectedAmmoType());
            shell.setDeltaMovement(direction.scale(getMuzzleVelocity()));
            level.addFreshEntity(shell);

            // マズルフラッシュ・軽度の煙エフェクト
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        ParticleTypes.FLAME,
                        muzzlePos.x, muzzlePos.y, muzzlePos.z,
                        5, 0.2, 0.2, 0.2, 0.1
                );
                serverLevel.sendParticles(
                        ParticleTypes.SMOKE,
                        muzzlePos.x + direction.x * 0.5,
                        muzzlePos.y + direction.y * 0.5,
                        muzzlePos.z + direction.z * 0.5,
                        3, 0.1, 0.1, 0.1, 0.05
                );
            }
        }
    }

    // --- BaseNavalGunRenderer 連携用のアニメーション情報共有 ---

    /**
     * Rendererの render() から毎フレーム呼び出され、現在再生すべき Glb アニメーションを返す
     */
    public List<GenericFastGlbRenderer.ActiveAnimation> getActiveAnimations(float partialTick) {
        List<GenericFastGlbRenderer.ActiveAnimation> activeAnims = new ArrayList<>();
        if (this.level == null) return activeAnims;

        long currentTime = this.level.getGameTime();
        float elapsedSeconds = (currentTime - this.stateStartTime + partialTick) / 20.0f;

        switch (this.currentState) {
            case START_FIRE:
                activeAnims.add(new GenericFastGlbRenderer.ActiveAnimation("start_fire", elapsedSeconds, false));
                break;

            case FIRING:
                // firing アニメーションをループ再生
                float firingDuration = getAnimationDuration("firing");
                float loopTime = (firingDuration > 0) ? (elapsedSeconds % firingDuration) : elapsedSeconds;
                activeAnims.add(new GenericFastGlbRenderer.ActiveAnimation("firing", loopTime, true));
                break;

            case END_FIRE:
                activeAnims.add(new GenericFastGlbRenderer.ActiveAnimation("end_fire", elapsedSeconds, false));
                break;

            case IDLE:
            default:
                break;
        }

        return activeAnims;
    }

    // --- NBT 保存・同期 ---

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("IsFiringTarget", this.isFiringTarget);
        tag.putInt("FiringState", this.currentState.ordinal());
        tag.putFloat("CurrentYaw", this.currentYaw);
        tag.putFloat("CurrentPitch", this.currentPitch);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        this.isFiringTarget = tag.getBoolean("IsFiringTarget");
        this.currentState = FiringState.values()[tag.getInt("FiringState")];
        this.currentYaw = tag.getFloat("CurrentYaw");
        this.currentPitch = tag.getFloat("CurrentPitch");
    }
}