package xyz.fmdc.arw.ops39;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import xyz.fmdc.arw.client.util.INodeRotatableModel;
import xyz.fmdc.arw.client.util.IRadar;
import xyz.fmdc.arw.registry.ModBlocks;

public class Ops39BlockEntity extends BlockEntity implements INodeRotatableModel, IRadar {

    private float currentYaw = 0.0f;
    private float prevYaw = 0.0f;

    private static final float RPM = 20.0f;
    // 毎Tick回転する速度 (20 RPM * 360 deg / (60s * 20ticks) = 6.0 deg/tick)
    private static final float ROTATION_SPEED = RPM * 360.0f / (60.0f * 20.0f);

    public Ops39BlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.OPS39.getBEType(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, Ops39BlockEntity be) {
        // 1. 補間用の前Tick角度を保持
        be.prevYaw = be.currentYaw;

        // 2. 角度の加算 (0.0 ～ 360.0度)
        be.currentYaw = (be.currentYaw + ROTATION_SPEED) % 360.0f;

        // 3. サーバー側での定期同期 (例: 100Tick = 5秒ごとに位相ズレをクライアントへ同期)
        if (!level.isClientSide && be.currentYaw < be.prevYaw) { // 1周(360度)回転するごとに同期する例
            be.syncToClient();
        }
    }

    /**
     * 描画用の滑らかな回転角度を取得 (360度跨ぎ対応)
     */
    @Override
    public float getRotationYaw(float partialTick) {
        return Mth.rotLerp(partialTick, this.prevYaw, this.currentYaw);
    }

    /**
     * 補間済みの現在角度 (0.0 ～ 360.0度) を取得
     */
    public float getInterpolatedYaw(float partialTick) {
        float yaw = getRotationYaw(partialTick) % 360.0f;
        return yaw < 0.0f ? yaw + 360.0f : yaw;
    }

    /**
     * 指定されたアニメーション時間へのマッピング
     */
    public float getAnimationTimeByYaw(float maxTime, float partialTick) {
        float yawProgress = getInterpolatedYaw(partialTick) / 360.0f;
        return yawProgress * maxTime;
    }

    private void syncToClient() {
        setChanged();
        if (this.level != null) {
            BlockState state = getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, 3);
        }
    }

    // --- NBT & Sync ---

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putFloat("Yaw", this.currentYaw);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        this.currentYaw = tag.getFloat("Yaw");
        this.prevYaw = this.currentYaw;
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null && this.level != null && this.level.isClientSide) {
            if (tag.contains("Yaw")) {
                float serverYaw = tag.getFloat("Yaw");

                // クライアントとサーバーの位相差（角度差）を計算
                float diff = Mth.wrapDegrees(serverYaw - this.currentYaw);

                // ズレが大きく開いている場合のみ補正（同期時のカクつきを防止）
                if (Math.abs(diff) > 15.0f) {
                    this.currentYaw = serverYaw;
                    this.prevYaw = serverYaw;
                } else {
                    // わずかなズレなら、少しずつ追いつかせる（イージング）
                    this.currentYaw = (this.currentYaw + diff * 0.2f) % 360.0f;
                }
            }
        }
    }

    @Override
    public Quaternionf getNodeRotation(String nodeName, float partialTick) {
        return null;
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(this.worldPosition).inflate(3.0);
    }
}