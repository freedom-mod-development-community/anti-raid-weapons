package xyz.fmdc.arw.common.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import xyz.fmdc.arw.api.fcs.FiringSolution;
import xyz.fmdc.arw.api.fcs.IFcsControllableWeapon;
import xyz.fmdc.arw.client.renderer.GenericGlbRenderer;
import xyz.fmdc.arw.client.util.IYawPitchAnimatableModel;

import java.util.*;

//FCS対応近代兵装の基底.
public abstract class AbstractSingleGunBlockEntity extends AbstractARWBlockEntity implements IYawPitchAnimatableModel,IFcsControllableWeapon {

    protected UUID networkId = UUID.randomUUID();
    protected boolean isFcsConnected = false;
    protected float currentYaw = 0.0f;
    protected float prevYaw = 0.0f;
    protected float currentPitch = 0.0f;
    protected float prevPitch = 0.0f;
    protected boolean limitYaw = false;

    protected float targetYaw = 0.0f;
    protected float targetPitch = 0.0f;

    protected abstract float getYawTurnSpeed();
    protected abstract float getPitchTurnSpeed();
    protected abstract float getMinYaw();
    protected abstract float getMaxYaw();
    protected abstract float getMinPitch();
    protected abstract float getMaxPitch();

    public void tickSingleGun() {
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

    public abstract void fire();


    public AbstractSingleGunBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
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
    public List<GenericGlbRenderer.ActiveAnimation> getActiveAnimations(float partialTick) {
        List<GenericGlbRenderer.ActiveAnimation> list = new ArrayList<>();
        if (this.level == null) return list;

        long currentGameTime = this.level.getGameTime();
        for (Map.Entry<String, Long> entry : this.runningAnimations.entrySet()) {
            String name = entry.getKey();
            long startTime = entry.getValue();
            float elapsedTicks = (float) (currentGameTime - startTime) + partialTick;
            float elapsedSeconds = Math.max(0.0f, elapsedTicks / 20.0f);
            list.add(new GenericGlbRenderer.ActiveAnimation(name, elapsedSeconds));
        }
        return list;
    }

    @Override
    public UUID getNetworkId() {
        return this.networkId;
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
        tag.putUUID("NetworkId", this.networkId);
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
        if (tag.hasUUID("NetworkId")) this.networkId = tag.getUUID("NetworkId");
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
                    // ★修正：NBTから "Start" を取得する
                    this.runningAnimations.put(animTag.getString("Name"), animTag.getLong("Start"));
                    this.animationDurations.put(animTag.getString("Name"), animTag.getFloat("Duration"));
                }
            }
        }
    }
}
