package xyz.fmdc.arw.common.blockentity.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import xyz.fmdc.arw.api.blockentity.IDirectionalBlockEntity;
import xyz.fmdc.arw.api.blockentity.IYawPitchAnimatableModel;
import xyz.fmdc.arw.api.fcs.FiringSolution;
import xyz.fmdc.arw.api.fcs.IFcsControllableWeapon;
import xyz.fmdc.arw.api.projectile.virtual.BallisticShellProjectile;
import xyz.fmdc.arw.client.renderer.GenericFastGlbRenderer;
import xyz.fmdc.arw.common.blockentity.AbstractARWBlockEntity;
import xyz.fmdc.arw.common.entity.projectile.FiveInchAmmoType;
import xyz.fmdc.arw.common.entity.projectile.FiveInchShellEntity;
import xyz.fmdc.arw.common.projectile.virtual.VirtualProjectileManager;
import xyz.fmdc.arw.network.PacketHandler;
import xyz.fmdc.arw.network.S2CSpawnVirtualProjectilePacket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// FCS対応近代兵装の基底.
public abstract class AbstractSingleGunBlockEntity extends AbstractARWBlockEntity
        implements IYawPitchAnimatableModel, IFcsControllableWeapon, IDirectionalBlockEntity {

    protected boolean isFcsConnected = false;
    protected float currentYaw = 0.0f;
    protected float prevYaw = 0.0f;
    protected float currentPitch = 0.0f;
    protected float prevPitch = 0.0f;
    protected boolean limitYaw = false;

    protected float targetYaw = 0.0f;
    protected float targetPitch = 0.0f;
    protected int cooldownTicks = 0;

    protected final Map<String, Float> animationDurations = new HashMap<>();
    protected final Map<String, Long> runningAnimations = new HashMap<>();

    protected abstract float getYawTurnSpeed();
    protected abstract float getPitchTurnSpeed();
    protected abstract float getMinYaw();
    protected abstract float getMaxYaw();
    protected abstract float getMinPitch();
    protected abstract float getMaxPitch();

    public void tickSingleGun() {
        if (cooldownTicks > 0) {
            cooldownTicks--;
        }

        this.prevYaw = this.currentYaw;
        this.prevPitch = this.currentPitch;

        // 目標角への旋回（Yaw）
        float yawDiff = Mth.wrapDegrees(this.targetYaw - this.currentYaw);
        float maxTurn = getYawTurnSpeed();
        if (Math.abs(yawDiff) <= maxTurn) {
            this.currentYaw = this.targetYaw;
        } else {
            this.currentYaw += Math.signum(yawDiff) * maxTurn;
        }
        if (this.limitYaw) {
            this.currentYaw = Mth.clamp(this.currentYaw, getMinYaw(), getMaxYaw());
        }

        // 目標角への仰俯角変更（Pitch）
        float pitchDiff = this.targetPitch - this.currentPitch;
        float maxPitch = getPitchTurnSpeed();
        if (Math.abs(pitchDiff) <= maxPitch) {
            this.currentPitch = this.targetPitch;
        } else {
            this.currentPitch += Math.signum(pitchDiff) * maxPitch;
        }
        this.currentPitch = Mth.clamp(this.currentPitch, getMinPitch(), getMaxPitch());
    }

    public abstract Vec3 getFiringDirection();
    public abstract FiveInchAmmoType getSelectedAmmoType();
    public abstract EntityType<FiveInchShellEntity> getShellEntityType();
    protected abstract boolean canFire();

    /** ブロックの中心から砲口（マズル）までの相対位置オフセット */
    public abstract Vec3 getMuzzleOffset();

    /** 再装填時間（Tick単位 / 20ticks = 1秒） */
    public abstract int getMaxCooldownTicks();

    /** 発射時の効果音（デフォルトは汎重大爆発音） */
    public SoundEvent getFireSound() {
        return SoundEvents.GENERIC_EXPLODE;
    }

    /** 初速パラメータ [blocks/tick] */
    public float getMuzzleVelocity() {
        return 8.0F;
    }

    public abstract void fire();

    public void playAnimation(String animName, float durationSeconds) {
        if (this.level != null) {
            this.animationDurations.put(animName, durationSeconds);
            this.runningAnimations.put(animName, this.level.getGameTime());
            if (!this.level.isClientSide) {
                syncToClient();
            }
        }
    }

    /**
     * 主砲発射メソッド（子クラスからはこれを呼び出すだけ）
     * 仮想飛翔体（Virtual Projectile）方式に完全統合され、チャンクロード要求を行わずに長距離砲戦を実現します。
     */
    public void fireProcess() {
        if (this.level == null || this.cooldownTicks > 0) {
            return;
        }
        // クールダウン開始
        this.cooldownTicks = getMaxCooldownTicks();

        Vec3 direction = getFiringDirection().normalize();
        Vec3 muzzlePos = Vec3.atBottomCenterOf(this.worldPosition).add(getMuzzleOffset());

        // 1. サウンド再生
        this.level.playSound(
                null,
                BlockPos.containing(muzzlePos),
                getFireSound(),
                SoundSource.BLOCKS,
                10.0F,
                0.5F
        );

        // 2. 仮想飛翔体の生成 & サーバー側マネージャー登録
        if (this.level instanceof ServerLevel serverLevel) {
            float muzzleVelocityMps = getMuzzleVelocity() * 20.0F; // blocks/tick -> m/s
            BallisticShellProjectile shell = BallisticShellProjectile.create5Inch(
                    serverLevel.dimension(),
                    this.uuid,
                    muzzlePos,
                    direction,
                    muzzleVelocityMps,
                    getSelectedAmmoType()
            );

            // サーバー側マネージャーへ登録（インメモリ計算）
            VirtualProjectileManager.getInstance().register(shell);

            // クライアント側描画用ダミーEntity生成パケットを周辺プレイヤーへ送信
            PacketHandler.sendToNear(
                    serverLevel,
                    muzzlePos,
                    256.0,
                    new S2CSpawnVirtualProjectilePacket(
                            shell.getProjectileId(),
                            shell.getProjectileTypeId(),
                            muzzlePos,
                            shell.getVelocity(),
                            shell.getMaxAgeTicks(),
                            new CompoundTag()
                    )
            );

            // マズルフラッシュと大爆煙のエフェクト
            spawnMuzzleEffects(serverLevel, muzzlePos, direction);
        }

        this.setChanged();
    }

    /** 砲口エフェクト（炎と重厚な白煙） */
    protected void spawnMuzzleEffects(ServerLevel serverLevel, Vec3 muzzlePos, Vec3 direction) {
        // マズルフラッシュ（炎）
        serverLevel.sendParticles(
                ParticleTypes.FLAME,
                muzzlePos.x, muzzlePos.y, muzzlePos.z,
                25, 0.4, 0.4, 0.4, 0.25
        );

        // 大爆煙
        for (int i = 0; i < 40; i++) {
            double rx = (serverLevel.random.nextDouble() - 0.5) * 2.0;
            double ry = (serverLevel.random.nextDouble() - 0.5) * 2.0;
            double rz = (serverLevel.random.nextDouble() - 0.5) * 2.0;

            serverLevel.sendParticles(
                    ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    muzzlePos.x + direction.x * 1.5,
                    muzzlePos.y + direction.y * 1.5,
                    muzzlePos.z + direction.z * 1.5,
                    1,
                    rx * 0.2 + direction.x * 0.6,
                    ry * 0.2 + direction.y * 0.6,
                    rz * 0.2 + direction.z * 0.6,
                    0.15
            );
        }
    }

    public AbstractSingleGunBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public Direction getFacing() {
        return this.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public float getRenderTargetYaw(float partialTick) {
        return Mth.rotLerp(partialTick, prevYaw, currentYaw);
    }

    @Override
    public float getRenderTargetPitch(float partialTick) {
        return Mth.rotLerp(partialTick, prevPitch, currentPitch);
    }

    @Override
    public List<GenericFastGlbRenderer.ActiveAnimation> getActiveAnimations(float partialTick) {
        List<GenericFastGlbRenderer.ActiveAnimation> list = new ArrayList<>();
        if (this.level == null) return list;

        long currentGameTime = this.level.getGameTime();
        for (Map.Entry<String, Long> entry : this.runningAnimations.entrySet()) {
            String name = entry.getKey();
            long startTime = entry.getValue();
            float elapsedTicks = (float) (currentGameTime - startTime) + partialTick;
            float elapsedSeconds = Math.max(0.0f, elapsedTicks / 20.0f);
            list.add(new GenericFastGlbRenderer.ActiveAnimation(name, elapsedSeconds));
        }
        return list;
    }

    @Override
    public UUID getNetworkId() {
        return this.uuid;
    }

    @Override
    public boolean isConnectedToFcs() {
        return this.isFcsConnected;
    }

    @Override
    public void setFcsConnected(boolean connected) {
        this.isFcsConnected = connected;
    }

    @Override
    public void applyFiringSolution(FiringSolution solution) {
        if (solution == null) return;
        setTargetYaw((float) solution.targetYaw());
        setTargetPitch((float) solution.targetPitch());
    }

    public void setTargetYaw(float targetYaw) {
        this.targetYaw = targetYaw;
    }

    public void setTargetPitch(float targetPitch) {
        this.targetPitch = targetPitch;
    }
}
