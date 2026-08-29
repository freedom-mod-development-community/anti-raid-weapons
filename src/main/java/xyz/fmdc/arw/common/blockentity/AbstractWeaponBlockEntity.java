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

import java.util.HashMap;
import java.util.Map;

/**
 * 全ての火器の基底クラス. ここから主砲や小口径火器等にFCSへの接続の有無等を加味して分岐.描画インターフェースはここには実装しない.
 */
public abstract class AbstractWeaponBlockEntity extends AbstractARWBlockEntity {

    protected float currentYaw = 0.0f;
    protected float prevYaw = 0.0f;
    protected float currentPitch = 0.0f;
    protected float prevPitch = 0.0f;

    protected float targetYaw = 0.0f;
    protected float targetPitch = 0.0f;

    protected final Map<String, Long> runningAnimations = new HashMap<>();
    protected final Map<String, Float> animationDurations = new HashMap<>();

    public AbstractWeaponBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected abstract float getYawTurnSpeed();
    protected abstract float getPitchTurnSpeed();
    protected abstract float getMinYaw();
    protected abstract float getMaxYaw();
    protected abstract float getMinPitch();
    protected abstract float getMaxPitch();

    public void tickWeapon() {
        this.prevYaw = this.currentYaw;
        this.prevPitch = this.currentPitch;

        updateRotation();
        cleanUpAnimations();
    }

    protected void updateRotation() {
        float yawDiff = Mth.wrapDegrees(this.targetYaw - this.currentYaw);
        float yawStep = Mth.clamp(yawDiff, -getYawTurnSpeed(), getYawTurnSpeed());
        this.currentYaw = Mth.clamp(this.currentYaw + yawStep, getMinYaw(), getMaxYaw());

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

    public void setTargetYaw(float yaw) {
        this.targetYaw = Mth.clamp(yaw, getMinYaw(), getMaxYaw());
    }

    public void setTargetPitch(float pitch) {
        this.targetPitch = Mth.clamp(pitch, getMinPitch(), getMaxPitch());
    }

    public void playAnimation(String animName, float durationSeconds) {
        if (this.level != null) {
            this.animationDurations.put(animName, durationSeconds);
            this.runningAnimations.put(animName, this.level.getGameTime());
            if (!this.level.isClientSide) {
                syncToClient();
            }
        }
    }

    public abstract void fire();

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putFloat("Yaw", this.currentYaw);
        tag.putFloat("Pitch", this.currentPitch);
        tag.putFloat("TargetYaw", this.targetYaw);
        tag.putFloat("TargetPitch", this.targetPitch);

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
                    if (this.level != null) {
                        this.runningAnimations.put(animTag.getString("Name"), this.level.getGameTime());
                        this.animationDurations.put(animTag.getString("Name"), animTag.getFloat("Duration"));
                    }
                }
            }
        }
    }
}