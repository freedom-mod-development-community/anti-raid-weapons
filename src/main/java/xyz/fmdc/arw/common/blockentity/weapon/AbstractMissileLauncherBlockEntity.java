package xyz.fmdc.arw.common.blockentity.weapon;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.fmdc.arw.api.blockentity.IYawPitchAnimatableModel;
import xyz.fmdc.arw.api.control.IRemoteControllableWeapon;
import xyz.fmdc.arw.api.fcs.FiringSolution;
import xyz.fmdc.arw.api.fcs.IFcsControllableWeapon;
import xyz.fmdc.arw.api.projectile.virtual.GuidedMissileProjectile;
import xyz.fmdc.arw.client.renderer.GenericFastGlbRenderer;
import xyz.fmdc.arw.common.blockentity.AbstractARWBlockEntity;
import xyz.fmdc.arw.common.entity.missile.AbstractMissileEntity;
import xyz.fmdc.arw.common.projectile.virtual.VirtualProjectileManager;
import xyz.fmdc.arw.network.PacketHandler;
import xyz.fmdc.arw.network.S2CSpawnVirtualProjectilePacket;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 旋回・俯仰制御可能なミサイルランチャー（誘導弾発射機）の基底クラス.
 * Virtual Projectile方式により、30km先への超長距離射撃を未ロード領域を飛び越えて実現します。
 */
public abstract class AbstractMissileLauncherBlockEntity extends AbstractARWBlockEntity
        implements IYawPitchAnimatableModel, IFcsControllableWeapon, IRemoteControllableWeapon {

    protected boolean isFcsConnected = false;
    protected float currentYaw = 0.0f;
    protected float prevYaw = 0.0f;
    protected float currentPitch = 0.0f;
    protected float prevPitch = 0.0f;
    protected boolean limitYaw = false;

    protected float targetYaw = 0.0f;
    protected float targetPitch = 0.0f;
    protected int cooldownTicks = 0;

    protected UUID controllerPlayerUUID = null;
    @Nullable
    protected UUID lockedTargetUuid = null;

    public AbstractMissileLauncherBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // --- 旋回性能パラメータ（子クラスで実装） ---
    protected abstract float getYawTurnSpeed();
    protected abstract float getPitchTurnSpeed();
    protected abstract float getMinYaw();
    protected abstract float getMaxYaw();
    protected abstract float getMinPitch();
    protected abstract float getMaxPitch();

    public abstract Vec3 getLaunchPosition();
    public abstract Vec3 getLaunchOffset();
    public abstract int getMaxCooldownTicks();

    public float getPitchModelOffset() {
        return 0.0f;
    }

    public void tickMissileLauncher() {
        tickLauncher();
    }

    public void tickLauncher() {
        if (cooldownTicks > 0) {
            cooldownTicks--;
        }

        this.prevYaw = this.currentYaw;
        this.prevPitch = this.currentPitch;

        float yawDiff = Mth.wrapDegrees(this.targetYaw - this.currentYaw);
        float maxYawTurn = getYawTurnSpeed();
        if (Math.abs(yawDiff) <= maxYawTurn) {
            this.currentYaw = this.targetYaw;
        } else {
            this.currentYaw += Math.signum(yawDiff) * maxYawTurn;
        }
        if (this.limitYaw) {
            this.currentYaw = Mth.clamp(this.currentYaw, getMinYaw(), getMaxYaw());
        }

        float pitchDiff = this.targetPitch - this.currentPitch;
        float maxPitchTurn = getPitchTurnSpeed();
        if (Math.abs(pitchDiff) <= maxPitchTurn) {
            this.currentPitch = this.targetPitch;
        } else {
            this.currentPitch += Math.signum(pitchDiff) * maxPitchTurn;
        }
        this.currentPitch = Mth.clamp(this.currentPitch, getMinPitch(), getMaxPitch());
    }

    protected abstract boolean canFire();

    public abstract void fire();

    protected void playAnimation(String animName, float durationSeconds) {
        if (this.level != null) {
            this.animationDurations.put(animName, durationSeconds);
            this.runningAnimations.put(animName, this.level.getGameTime());
            if (!this.level.isClientSide) {
                syncToClient();
            }
        }
    }

    public void setTargetYaw(float yaw) {
        this.targetYaw = (this.limitYaw) ? Mth.clamp(yaw, getMinYaw(), getMaxYaw()) : yaw;
    }

    public void setTargetPitch(float pitch) {
        this.targetPitch = Mth.clamp(pitch, getMinPitch(), getMaxPitch());
    }

    public float getCurrentYaw() {
        return this.currentYaw;
    }

    public float getCurrentPitch() {
        return this.currentPitch;
    }

    public float getTargetYaw() {
        return this.targetYaw;
    }

    public float getTargetPitch() {
        return this.targetPitch;
    }

    public boolean isAimAligned(float tolerance) {
        float yawDiff = Math.abs(Mth.wrapDegrees(this.targetYaw - this.currentYaw));
        float pitchDiff = Math.abs(Mth.wrapDegrees(this.targetPitch - this.currentPitch));
        return yawDiff <= tolerance && pitchDiff <= tolerance;
    }

    public boolean isAimAligned() {
        return isAimAligned(1.0f);
    }

    public int getCooldownTicks() {
        return this.cooldownTicks;
    }

    public void setCooldownTicks(int cooldownTicks) {
        this.cooldownTicks = cooldownTicks;
    }

    public Vec3 getFiringDirection() {
        return Vec3.directionFromRotation(this.currentPitch, this.currentYaw);
    }

    public SoundEvent getLaunchSound() {
        return SoundEvents.GENERIC_EXPLODE;
    }

    protected float getInitialLaunchVelocity() {
        return 1.0F;
    }

    @Nullable
    protected AbstractMissileEntity createMissileEntity(Level level, Vec3 launchPos, Vec3 direction) {
        EntityType<? extends AbstractMissileEntity> entityType = getMissileEntityType();
        if (entityType != null) {
            return entityType.create(level);
        }
        return null;
    }

    @Nullable
    protected EntityType<? extends AbstractMissileEntity> getMissileEntityType() {
        return null;
    }

    /**
     * ミサイル発射プロセスの標準実装
     */
    public void launchMissile() {
        if (this.level == null || this.cooldownTicks > 0) {
            return;
        }
        this.cooldownTicks = getMaxCooldownTicks();

        Vec3 direction = getFiringDirection().normalize();
        Vec3 launchPos = getLaunchPosition();

        // 1. サウンド再生
        this.level.playSound(
                null,
                BlockPos.containing(launchPos),
                getLaunchSound(),
                SoundSource.BLOCKS,
                10.0F,
                0.8F
        );

        // 2. サーバー側エフェクト & 仮想ミサイル登録 & クライアント同期
        if (!this.level.isClientSide && this.level instanceof ServerLevel serverLevel) {
            spawnLaunchEffects(serverLevel, launchPos, direction);
            spawnMissileEntity(launchPos, direction);
            syncToClient();
        }

        this.setChanged();
    }

    /**
     * 仮想飛翔体（Virtual Projectile）方式でのミサイル登録
     */
    protected void spawnMissileEntity(Vec3 launchPos, Vec3 direction) {
        if (this.level instanceof ServerLevel serverLevel) {
            // Virtual Projectile として RIM-66M-2 ミサイルを生成
            GuidedMissileProjectile missile = GuidedMissileProjectile.createRim66M2(
                    serverLevel.dimension(),
                    this.uuid,
                    this.lockedTargetUuid,
                    launchPos,
                    direction
            );

            // サーバー側マネージャーへ登録
            VirtualProjectileManager.getInstance().register(missile);

            // クライアント側描画パケット送出
            PacketHandler.sendToNear(
                    serverLevel,
                    launchPos,
                    256.0,
                    new S2CSpawnVirtualProjectilePacket(
                            missile.getProjectileId(),
                            missile.getProjectileTypeId(),
                            launchPos,
                            missile.getVelocity(),
                            missile.getMaxAgeTicks(),
                            new CompoundTag()
                    )
            );
        }
    }

    /**
     * ミサイル発射特有のエフェクト（ロケット点火の炎・バックブラスト煙）
     */
    protected void spawnLaunchEffects(ServerLevel serverLevel, Vec3 launchPos, Vec3 direction) {
        serverLevel.sendParticles(
                ParticleTypes.FLAME,
                launchPos.x, launchPos.y, launchPos.z,
                25, 0.4, 0.4, 0.4, 0.2
        );

        Vec3 backDir = direction.scale(-1.0);
        for (int i = 0; i < 40; i++) {
            double rx = (serverLevel.random.nextDouble() - 0.5) * 1.5;
            double ry = (serverLevel.random.nextDouble() - 0.5) * 1.5;
            double rz = (serverLevel.random.nextDouble() - 0.5) * 1.5;

            serverLevel.sendParticles(
                    ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    launchPos.x + backDir.x * 1.2 + rx,
                    launchPos.y + backDir.y * 1.2 + ry,
                    launchPos.z + backDir.z * 1.2 + rz,
                    1,
                    backDir.x * 0.3 + rx * 0.1,
                    backDir.y * 0.3 + ry * 0.1,
                    backDir.z * 0.3 + rz * 0.1,
                    0.05
            );
        }
    }

    // --- IYawPitchAnimatableModel の実装 ---

    @Override
    public float getRenderTargetYaw(float partialTick) {
        return currentYaw;
    }

    @Override
    public float getRenderTargetPitch(float partialTick) {
        return currentPitch;
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

    // --- IFcsControllableWeapon の実装 ---

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
        setTargetYaw(solution.targetYaw());
        setTargetPitch(solution.targetPitch());

        if (solution.allowFire() && canFire()) {
            fire();
        }
    }

    public void setLockedTargetUuid(@Nullable UUID targetUuid) {
        this.lockedTargetUuid = targetUuid;
    }

    // --- IRemoteControllableWeapon の実装 ---

    @Override
    public Vec3 getCameraPosition() {
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
        setTargetYaw(yawInput);
        setTargetPitch(pitchInput);
        if (triggerFire && canFire()) {
            fire();
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(this.worldPosition).inflate(5.0);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putFloat("CurrentYaw", this.currentYaw);
        tag.putFloat("CurrentPitch", this.currentPitch);
        tag.putFloat("TargetYaw", this.targetYaw);
        tag.putFloat("TargetPitch", this.targetPitch);
        tag.putInt("CooldownTicks", this.cooldownTicks);
        if (this.lockedTargetUuid != null) {
            tag.putUUID("LockedTargetUuid", this.lockedTargetUuid);
        }
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        this.currentYaw = tag.getFloat("CurrentYaw");
        this.currentPitch = tag.getFloat("CurrentPitch");
        this.targetYaw = tag.getFloat("TargetYaw");
        this.targetPitch = tag.getFloat("TargetPitch");
        this.cooldownTicks = tag.getInt("CooldownTicks");
        if (tag.hasUUID("LockedTargetUuid")) {
            this.lockedTargetUuid = tag.getUUID("LockedTargetUuid");
        }
    }
}
