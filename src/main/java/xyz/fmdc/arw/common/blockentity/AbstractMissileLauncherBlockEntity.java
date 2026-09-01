package xyz.fmdc.arw.common.blockentity;

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
import xyz.fmdc.arw.api.control.IRemoteControllableWeapon;
import xyz.fmdc.arw.api.fcs.FiringSolution;
import xyz.fmdc.arw.api.fcs.IFcsControllableWeapon;
import xyz.fmdc.arw.client.renderer.GenericFastGlbRenderer;
import xyz.fmdc.arw.client.util.IYawPitchAnimatableModel;
import xyz.fmdc.arw.common.entity.AbstractMissileEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 旋回・俯仰制御可能なミサイルランチャー（誘導弾発射機）の基底クラス.
 * FCS連携、遠隔操作、アニメーション管理、ミサイルエンティティ生成、発射制御などの共通ロジックを提供します。
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

    public AbstractMissileLauncherBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // --- 旋回性能パラメータ（子クラスで実装） ---

    /** 1Tickあたりに回転できる最大Yaw角度（度/tick） */
    protected abstract float getYawTurnSpeed();

    /** 1Tickあたりに回転できる最大Pitch角度（度/tick） */
    protected abstract float getPitchTurnSpeed();

    /** 最小Yaw角度（度） */
    protected abstract float getMinYaw();

    /** 最大Yaw角度（度） */
    protected abstract float getMaxYaw();

    /** 最小Pitch角度（度、負の値が仰角/上向き） */
    protected abstract float getMinPitch();

    /** 最大Pitch角度（度、正の値が俯角/下向き） */
    protected abstract float getMaxPitch();

    // --- クールダウン & 発射定義（子クラスで実装） ---

    /** 再装填/発射間隔時間（Tick単位 / 20ticks = 1秒） */
    public abstract int getMaxCooldownTicks();

    /** 発射可能条件（装填完了、弾薬装填状態など） */
    protected abstract boolean canFire();

    /** 発射アクション実行（アニメーション再生やlaunchMissile()呼び出し） */
    public abstract void fire();

    /**
     * ブロック中心からミサイル発射点（レール/セル先端）までの相対位置オフセット
     */
    public abstract Vec3 getLaunchOffset();

    // --- ランチャー共通更新処理 ---

    /**
     * 毎Tick呼び出されるランチャー共通の更新処理
     */
    public void tickMissileLauncher() {
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
        }
        this.prevYaw = this.currentYaw;
        this.prevPitch = this.currentPitch;

        updateRotation();
        cleanUpAnimations();
    }

    /**
     * 目標角へのスムーズな旋回処理
     */
    protected void updateRotation() {
        float yawDiff = Mth.wrapDegrees(this.targetYaw - this.currentYaw);
        float yawStep = Mth.clamp(yawDiff, -getYawTurnSpeed(), getYawTurnSpeed());
        this.currentYaw = (this.limitYaw)
                ? Mth.clamp(this.currentYaw + yawStep, getMinYaw(), getMaxYaw())
                : Mth.wrapDegrees(this.currentYaw + yawStep);

        float pitchDiff = Mth.wrapDegrees(this.targetPitch - this.currentPitch);
        float pitchStep = Mth.clamp(pitchDiff, -getPitchTurnSpeed(), getPitchTurnSpeed());
        this.currentPitch = Mth.clamp(this.currentPitch + pitchStep, getMinPitch(), getMaxPitch());
    }

    /**
     * 終了したアニメーションの自動クリーンアップ
     */
    protected void cleanUpAnimations() {
        if (this.level != null && !this.runningAnimations.isEmpty()) {
            long currentTime = this.level.getGameTime();
            this.runningAnimations.entrySet().removeIf(entry -> {
                String animName = entry.getKey();
                long startTime = entry.getValue();
                float duration = this.animationDurations.getOrDefault(animName, 1.0f);
                float elapsedSeconds = (currentTime - startTime) / 20.0f;
                return elapsedSeconds >= duration;
            });
        }
    }

    /**
     * アニメーションの再生開始
     */
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

    public int getCooldownTicks() {
        return this.cooldownTicks;
    }

    public void setCooldownTicks(int cooldownTicks) {
        this.cooldownTicks = cooldownTicks;
    }

    /**
     * 発射方向ベクトル（正規化済み）
     */
    public Vec3 getFiringDirection() {
        return Vec3.directionFromRotation(this.currentPitch, this.currentYaw);
    }

    /**
     * 発射時の効果音（デフォルトは爆発音）
     */
    public SoundEvent getLaunchSound() {
        return SoundEvents.GENERIC_EXPLODE;
    }

    /**
     * ミサイル発射時の初期初速（ブロック単位/tick、デフォルト: 1.0）
     */
    protected float getInitialLaunchVelocity() {
        return 1.0F;
    }

    /**
     * 発射するミサイルエンティティの生成（子クラスでオーバーライド可能）
     */
    @Nullable
    protected AbstractMissileEntity createMissileEntity(Level level, Vec3 launchPos, Vec3 direction) {
        EntityType<? extends AbstractMissileEntity> entityType = getMissileEntityType();
        if (entityType != null) {
            return entityType.create(level);
        }
        return null;
    }

    /**
     * 発射するミサイルのEntityType（createMissileEntityをオーバーライドしない場合に使用）
     */
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
        Vec3 launchPos = Vec3.atCenterOf(this.worldPosition).add(getLaunchOffset());

        // 1. サウンド再生
        this.level.playSound(
                null,
                BlockPos.containing(launchPos),
                getLaunchSound(),
                SoundSource.BLOCKS,
                10.0F,
                0.8F
        );

        // 2. サーバー側エフェクト & エンティティ生成
        if (!this.level.isClientSide) {
            if (this.level instanceof ServerLevel serverLevel) {
                spawnLaunchEffects(serverLevel, launchPos, direction);
            }
            spawnMissileEntity(launchPos, direction);
        }

        this.setChanged();
    }

    /**
     * ミサイルエンティティの生成とワールドへのスポーン
     */
    protected void spawnMissileEntity(Vec3 launchPos, Vec3 direction) {
        if (this.level instanceof ServerLevel serverLevel) {
            AbstractMissileEntity missile = createMissileEntity(serverLevel, launchPos, direction);
            if (missile != null) {
                missile.setPos(launchPos.x, launchPos.y, launchPos.z);
                missile.setDeltaMovement(direction.scale(getInitialLaunchVelocity()));
                serverLevel.addFreshEntity(missile);
            }
        }
    }

    /**
     * ミサイル発射特有のエフェクト（ロケット点火の炎・バックブラスト煙）
     */
    protected void spawnLaunchEffects(ServerLevel serverLevel, Vec3 launchPos, Vec3 direction) {
        // 発射点周辺のロケット炎
        serverLevel.sendParticles(
                ParticleTypes.FLAME,
                launchPos.x, launchPos.y, launchPos.z,
                25, 0.4, 0.4, 0.4, 0.2
        );

        // ロケット後方・周囲への濃い煙（バックブラスト）
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
        return Mth.rotLerp(partialTick, this.prevYaw, this.currentYaw);
    }

    @Override
    public float getRenderTargetPitch(float partialTick) {
        return Mth.rotLerp(partialTick, this.prevPitch, this.currentPitch);
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

    // --- NBT 保存 & 同期 ---

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putFloat("Yaw", this.currentYaw);
        tag.putFloat("Pitch", this.currentPitch);
        tag.putFloat("TargetYaw", this.targetYaw);
        tag.putFloat("TargetPitch", this.targetPitch);
        tag.putBoolean("FcsConnected", this.isFcsConnected);
        tag.putInt("CooldownTicks", this.cooldownTicks);
        if (this.controllerPlayerUUID != null) {
            tag.putUUID("ControllerPlayerUUID", this.controllerPlayerUUID);
        }

        ListTag animList = new ListTag();
        for (Map.Entry<String, Long> entry : this.runningAnimations.entrySet()) {
            CompoundTag animTag = new CompoundTag();
            animTag.putString("Name", entry.getKey());
            animTag.putLong("Start", entry.getValue());
            animTag.putFloat("Duration", this.animationDurations.getOrDefault(entry.getKey(), 1.0f));
            animList.add(animTag);
        }
        tag.put("RunningAnims", animList);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        this.currentYaw = tag.getFloat("Yaw");
        this.currentPitch = tag.getFloat("Pitch");
        this.targetYaw = tag.getFloat("TargetYaw");
        this.targetPitch = tag.getFloat("TargetPitch");
        this.isFcsConnected = tag.getBoolean("FcsConnected");
        this.cooldownTicks = tag.getInt("CooldownTicks");
        if (tag.hasUUID("ControllerPlayerUUID")) {
            this.controllerPlayerUUID = tag.getUUID("ControllerPlayerUUID");
        } else {
            this.controllerPlayerUUID = null;
        }

        this.runningAnimations.clear();
        if (tag.contains("RunningAnims", Tag.TAG_LIST)) {
            ListTag animList = tag.getList("RunningAnims", Tag.TAG_COMPOUND);
            for (int i = 0; i < animList.size(); i++) {
                CompoundTag animTag = animList.getCompound(i);
                this.runningAnimations.put(animTag.getString("Name"), animTag.getLong("Start"));
                this.animationDurations.put(animTag.getString("Name"), animTag.getFloat("Duration"));
            }
        }
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            if (tag.contains("TargetYaw")) this.targetYaw = tag.getFloat("TargetYaw");
            if (tag.contains("TargetPitch")) this.targetPitch = tag.getFloat("TargetPitch");
            if (tag.contains("CooldownTicks")) this.cooldownTicks = tag.getInt("CooldownTicks");

            this.runningAnimations.clear();
            if (tag.contains("RunningAnims", Tag.TAG_LIST)) {
                ListTag animList = tag.getList("RunningAnims", Tag.TAG_COMPOUND);
                for (int i = 0; i < animList.size(); i++) {
                    CompoundTag animTag = animList.getCompound(i);
                    this.runningAnimations.put(animTag.getString("Name"), animTag.getLong("Start"));
                    this.animationDurations.put(animTag.getString("Name"), animTag.getFloat("Duration"));
                }
            }
        }
    }
}
