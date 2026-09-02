package xyz.fmdc.arw.common.blockentity;

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
import xyz.fmdc.arw.api.fcs.FiringSolution;
import xyz.fmdc.arw.api.fcs.IFcsControllableWeapon;
import xyz.fmdc.arw.client.renderer.GenericFastGlbRenderer;
import xyz.fmdc.arw.client.util.IDirectionalBlockEntity;
import xyz.fmdc.arw.client.util.IYawPitchAnimatableModel;
import xyz.fmdc.arw.common.entity.projectile.FiveInchAmmoType;
import xyz.fmdc.arw.common.entity.projectile.FiveInchShellEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

//FCS対応近代兵装の基底.
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

        updateRotation();
        cleanUpAnimations();
    }

    protected void updateRotation() {
        float yawDiff = Mth.wrapDegrees(this.targetYaw - this.currentYaw);
        float yawStep = Mth.clamp(yawDiff, -getYawTurnSpeed(), getYawTurnSpeed());
        this.currentYaw = (this.limitYaw)?Mth.clamp(this.currentYaw + yawStep, getMinYaw(), getMaxYaw()):
                Mth.wrapDegrees(this.currentYaw + yawStep);

        float pitchDiff = Mth.wrapDegrees(this.targetPitch - this.currentPitch);
        float pitchStep = Mth.clamp(pitchDiff, -getPitchTurnSpeed(), getPitchTurnSpeed());
        this.currentPitch = Mth.clamp(this.currentPitch + pitchStep, getMinPitch(), getMaxPitch());
    }

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
        this.targetYaw = Mth.clamp(yaw, getMinYaw(), getMaxYaw());
    }

    public void setTargetPitch(float pitch) {
        this.targetPitch = Mth.clamp(pitch, getMinPitch(), getMaxPitch());
    }

    // --- 子クラスで定義・オーバーライドする抽象メソッド群 ---

    /** 使用する砲弾の Enum（重量や爆薬量などのパラメータ保持用） */
    public abstract FiveInchAmmoType getSelectedAmmoType();

    /** 生成する砲弾エンティティの EntityType */
    public abstract EntityType<FiveInchShellEntity> getShellEntityType();

    /** 砲身の向きベクトル（正規化済み） */
    public abstract Vec3 getFiringDirection();

    /** ブロックの中心から砲口（マズル）までの相対位置オフセット */
    public abstract Vec3 getMuzzleOffset();

    /** 再装填時間（Tick単位 / 20ticks = 1秒） */
    public abstract int getMaxCooldownTicks();

    /** 発射時の効果音（デフォルトは汎重大爆発音） */
    public SoundEvent getFireSound() {
        return SoundEvents.GENERIC_EXPLODE;
    }

    /** 初速パラメータ */
    public float getMuzzleVelocity() {
        return 8.0F;
    }

    public abstract void fire();
    /**
     * 主砲発射メソッド（子クラスからはこれを呼び出すだけ）
     */
    public void fireProcess() {
        if (this.level == null || this.cooldownTicks > 0) {
            return;
        }
        // クールダウン開始
        this.cooldownTicks = getMaxCooldownTicks();

        Vec3 direction = getFiringDirection();
        Vec3 muzzlePos = Vec3.atBottomCenterOf(this.worldPosition).add(getMuzzleOffset());
        System.out.println(muzzlePos);

        // 1. サウンド再生
        this.level.playSound(
                null,
                BlockPos.containing(muzzlePos),
                getFireSound(),
                SoundSource.BLOCKS,
                10.0F,
                0.5F
        );

        // 2. 弾丸エンティティ生成 & エフェクト（サーバー側）
        if (!this.level.isClientSide) {
            FiveInchShellEntity shell = new FiveInchShellEntity(getShellEntityType(), this.level);
            shell.setPos(muzzlePos.x, muzzlePos.y, muzzlePos.z);
            shell.setAmmoType(getSelectedAmmoType());
            shell.setDeltaMovement(direction.scale(getMuzzleVelocity()));

            this.level.addFreshEntity(shell);

            // マズルフラッシュと大爆煙の同期発生
            if (this.level instanceof ServerLevel serverLevel) {
                spawnMuzzleEffects(serverLevel, muzzlePos, direction);
            }
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
    public Direction getFacing(){
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
        setTargetYaw(solution.targetYaw());
        setTargetPitch(solution.targetPitch());

        if (solution.allowFire() && canFire()) {
            fire();
        }
    }

    protected abstract boolean canFire();

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putFloat("Yaw", this.currentYaw);
        tag.putFloat("Pitch", this.currentPitch);
        tag.putFloat("TargetYaw", this.targetYaw);
        tag.putFloat("TargetPitch", this.targetPitch);
        tag.putBoolean("FcsConnected", this.isFcsConnected);

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

        this.runningAnimations.clear();
        if (tag.contains("RunningAnims", Tag.TAG_LIST)) {
            ListTag animList = tag.getList("RunningAnims", Tag.TAG_COMPOUND);
            for (int i = 0; i < animList.size(); i++) {
                CompoundTag animTag = animList.getCompound(i);
                this.runningAnimations.put(animTag.getString("Name"), animTag.getLong("Start"));
                this.animationDurations.put(animTag.getString("Name"), animTag.getFloat("Duration"));
            }
        }
        this.isFcsConnected = tag.getBoolean("FcsConnected");
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            if (tag.contains("TargetYaw")) this.targetYaw = tag.getFloat("TargetYaw");
            if (tag.contains("TargetPitch")) this.targetPitch = tag.getFloat("TargetPitch");

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
