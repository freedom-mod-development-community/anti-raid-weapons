package xyz.fmdc.arw.common.blockentity.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import xyz.fmdc.arw.client.renderer.GenericGlbRenderer;
import xyz.fmdc.arw.client.util.IYawPitchAnimatableModel;
import xyz.fmdc.arw.common.entity.projectile.NavalShellEntity;
import xyz.fmdc.arw.registry.ModBlocks;
import xyz.fmdc.arw.registry.ModSounds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Oto127mmBlockEntity extends BlockEntity implements IYawPitchAnimatableModel {

    // 描画用の現在角度・過去角度
    private float currentYaw = 0.0f;
    private float prevYaw = 0.0f;
    private float currentPitch = 0.0f;
    private float prevPitch = 0.0f;

    // 目標角度（パケット同期や視線追従の入力先）
    private float targetYaw = 0.0f;
    private float targetPitch = 0.0f;

    // 旋回性能（1Tickあたりに回転できる最大角度）
    private static final float YAW_TURN_SPEED = 3.0f;   // 1Tickあたり最大3度
    private static final float PITCH_TURN_SPEED = 2.0f; // 1Tickあたり最大2度

    // 可動域制限
    private static final float MIN_YAW = -45.0f;
    private static final float MAX_YAW = 45.0f;
    private static final float MIN_PITCH = -65.0f; // マイナスが仰角（上向き）の場合
    private static final float MAX_PITCH = 15.0f;

    public static final float FIRE_ANIM_DURATION = 1.0f;
    public static final float RELOAD_ANIM_DURATION = 2.33f;

    private int tickCounter = 0;

    private final Map<String, Long> runningAnimations = new HashMap<>();
    private final Map<String, Float> animationDurations = new HashMap<>();

    public Oto127mmBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.OTO127MM.getBEType(), pos, state);
        animationDurations.put("fire", FIRE_ANIM_DURATION);
        animationDurations.put("reload", RELOAD_ANIM_DURATION);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, Oto127mmBlockEntity be) {
        // 1. 補間用の前Tick角度を保存
        be.prevYaw = be.currentYaw;
        be.prevPitch = be.currentPitch;

        be.tickCounter++;

//        // 2. 目標角度の更新テスト（将来的にここを「視線追従」や「AIの照準」に差し替える）
//        // ※例としてサイン波で目標角度（target）を動かしてみる
//        be.setTargetYaw(Mth.sin(be.tickCounter * 0.03f) * 45.0f);
//        float rawTargetPitch = Mth.sin(be.tickCounter * 0.05f) * 40.0f + 25.0f;
//        be.setTargetPitch(-rawTargetPitch);
//
//        // 3. current を target に向かってぬるっと旋回（イージング処理）
//        be.updateRotation();
//
        // 4. アニメーションクリーンアップ
        if (!be.runningAnimations.isEmpty()) {
            long currentTime = level.getGameTime();
            be.runningAnimations.entrySet().removeIf(entry -> {
                String animName = entry.getKey();
                long startTime = entry.getValue();
                float duration = be.animationDurations.getOrDefault(animName, 1.0f);
                float elapsedSeconds = (currentTime - startTime) / 20.0f;
                return elapsedSeconds >= duration;
            });
        }

        // テスト用：発射処理
        if (!level.isClientSide) {
            if (be.tickCounter % 120 == 0) {
                be.fire();
            }
        }
    }

    /**
     * 現在角度（current）を目標角度（target）へスムーズに近づける処理
     */
    private void updateRotation() {
        // --- Yaw 旋回 ---
        float yawDiff = Mth.wrapDegrees(this.targetYaw - this.currentYaw);
        float yawStep = Mth.clamp(yawDiff, -YAW_TURN_SPEED, YAW_TURN_SPEED);
        this.currentYaw = Mth.clamp(this.currentYaw + yawStep, MIN_YAW, MAX_YAW);

        // --- Pitch 旋回 ---
        float pitchDiff = Mth.wrapDegrees(this.targetPitch - this.currentPitch);
        float pitchStep = Mth.clamp(pitchDiff, -PITCH_TURN_SPEED, PITCH_TURN_SPEED);
        this.currentPitch = Mth.clamp(this.currentPitch + pitchStep, MIN_PITCH, MAX_PITCH);
    }

    /**
     * 目標角度（Yaw）を設定（自動で可動域内に制限）
     */
    public void setTargetYaw(float yaw) {
        this.targetYaw = Mth.clamp(yaw, MIN_YAW, MAX_YAW);
    }

    /**
     * 目標角度（Pitch）を設定（自動で可動域内に制限）
     */
    public void setTargetPitch(float pitch) {
        this.targetPitch = Mth.clamp(pitch, MIN_PITCH, MAX_PITCH);
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

    public void fire() {
        playAnimation("fire", FIRE_ANIM_DURATION);

        if (this.level != null && !this.level.isClientSide) {
            Vec3 pivot = Vec3.atCenterOf(this.worldPosition).add(0.0, 1.8, 0.0);
            Vec3 direction = Vec3.directionFromRotation(this.currentPitch, this.currentYaw);
            Vec3 muzzlePos = pivot.add(direction.scale(8.0));

            NavalShellEntity shell = new NavalShellEntity(this.level, muzzlePos.x, muzzlePos.y, muzzlePos.z);
            shell.setExplosionPower(4.0f);
            shell.setDirectDamage(50.0f);
            shell.shoot(direction.x, direction.y, direction.z, 40.4f, 0.1f);

            this.level.addFreshEntity(shell);

            // サウンド再生（発射音）
            this.level.playSound(
                    null,
                    this.worldPosition.getX() + 0.5,
                    this.worldPosition.getY() + 0.5,
                    this.worldPosition.getZ() + 0.5,
                    ModSounds.OTO127_FIRE.get(),
                    SoundSource.BLOCKS,
                    4.0f,
                    1.0f
            );
        }

        playAnimation("reload", RELOAD_ANIM_DURATION);
    }

    private void syncToClient() {
        setChanged();
        if (this.level != null) {
            BlockState state = getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, 3);
        }
    }

    // --- INavalGun の実装 ---

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

    // --- NBT & Sync ---

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
                String name = animTag.getString("Name");
                long start = animTag.getLong("Start");
                float duration = animTag.getFloat("Duration");

                this.runningAnimations.put(name, start);
                this.animationDurations.put(name, duration);
            }
        }
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            // ★ パケット受信時は target のみ更新し、current の滑らかな旋回を妨げない
            if (tag.contains("TargetYaw")) this.targetYaw = tag.getFloat("TargetYaw");
            if (tag.contains("TargetPitch")) this.targetPitch = tag.getFloat("TargetPitch");

            // アニメーション情報の読み込み（クライアント時間で再生開始）
            this.runningAnimations.clear();
            if (tag.contains("RunningAnims", Tag.TAG_LIST)) {
                ListTag animList = tag.getList("RunningAnims", Tag.TAG_COMPOUND);
                for (int i = 0; i < animList.size(); i++) {
                    CompoundTag animTag = animList.getCompound(i);
                    String name = animTag.getString("Name");
                    float duration = animTag.getFloat("Duration");

                    if (this.level != null) {
                        this.runningAnimations.put(name, this.level.getGameTime());
                        this.animationDurations.put(name, duration);
                    }
                }
            }
        }
    }
}
