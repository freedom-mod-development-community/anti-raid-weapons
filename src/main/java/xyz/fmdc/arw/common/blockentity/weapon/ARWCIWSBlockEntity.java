package xyz.fmdc.arw.common.blockentity.weapon;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.phys.Vec3;
import xyz.fmdc.arw.client.renderer.GenericFastGlbRenderer;
import xyz.fmdc.arw.client.util.IYawPitchAnimatableModel;
import xyz.fmdc.arw.common.blockentity.AbstractARWBlockEntity;
import xyz.fmdc.arw.common.entity.projectile.FiveInchAmmoType;
import xyz.fmdc.arw.common.entity.projectile.FiveInchShellEntity;

import java.util.ArrayList;
import java.util.List;

public abstract class ARWCIWSBlockEntity extends AbstractARWBlockEntity implements IYawPitchAnimatableModel {

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

    public Vec3 getFiringDirection() {
        return Vec3.directionFromRotation(this.currentPitch, this.currentYaw);
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
        long gameTime = level.getGameTime();

        if (!level.isClientSide && be.firingTimer > 0) {
            be.firingTimer--;
            if (be.firingTimer <= 0) {
                be.setFiring(false); // 5秒経過したら発砲停止
            }
        }

        // 1. ステートマシンの更新
        be.updateFiringState(gameTime);

        // 2. 実射撃処理（FIRING ステートかつ 毎Tick 発射）
        if (be.currentState == FiringState.FIRING) {
            be.executeTickFire(level);
        }
    }

    private void updateFiringState(long gameTime) {
        float elapsedSeconds = (gameTime - this.stateStartTime) / 20.0f;

        switch (this.currentState) {
            case IDLE:
                if (this.isFiringTarget) {
                    transitionTo(FiringState.START_FIRE, gameTime);
                }
                break;

            case START_FIRE:
                // start_fire アニメーションが完了したら FIRING へ移行
                if (elapsedSeconds >= getAnimationDuration("start_fire")) {
                    transitionTo(FiringState.FIRING, gameTime);
                }
                break;

            case FIRING:
                // トリガーが離されたら end_fire へ移行
                if (!this.isFiringTarget) {
                    transitionTo(FiringState.END_FIRE, gameTime);
                }
                break;

            case END_FIRE:
                // end_fire アニメーションが完了したら IDLE へ戻る
                if (elapsedSeconds >= getAnimationDuration("end_fire")) {
                    transitionTo(FiringState.IDLE, gameTime);
                }
                break;
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
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("IsFiringTarget", this.isFiringTarget);
        tag.putInt("FiringState", this.currentState.ordinal());
        tag.putFloat("CurrentYaw", this.currentYaw);
        tag.putFloat("CurrentPitch", this.currentPitch);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.isFiringTarget = tag.getBoolean("IsFiringTarget");
        this.currentState = FiringState.values()[tag.getInt("FiringState")];
        this.currentYaw = tag.getFloat("CurrentYaw");
        this.currentPitch = tag.getFloat("CurrentPitch");
    }
}