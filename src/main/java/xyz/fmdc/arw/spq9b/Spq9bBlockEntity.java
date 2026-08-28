package xyz.fmdc.arw.spq9b;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Quaternionf;
import xyz.fmdc.arw.client.util.INodeRotatableModel;
import xyz.fmdc.arw.client.util.IRadar;
import xyz.fmdc.arw.registry.ModBlockEntities;
import xyz.fmdc.arw.registry.ModBlocks;

public class Spq9bBlockEntity extends BlockEntity implements INodeRotatableModel, IRadar {

    private float currentYaw = 0.0f;
    private float prevYaw = 0.0f;
    private final float RPM = 30f;
    private final float rotationSpeed = RPM * 360 / (60*20) ; // 毎Tick回転する速度

    public Spq9bBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.SPQ9B.getBEType(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, Spq9bBlockEntity be) {
        be.prevYaw = be.currentYaw;
        be.currentYaw = (be.currentYaw + be.rotationSpeed) % 360.0f;
    }

    /**
     * 補間済みの現在角度 (0.0 ～ 360.0度) を取得
     */
    public float getInterpolatedYaw(float partialTick) {
        float interpolated = this.prevYaw + Mth.wrapDegrees(this.currentYaw - this.prevYaw) * partialTick;
        if (interpolated < 0) interpolated += 360.0f;
        return interpolated % 360.0f;
    }

    /**
     * 【重要】指定されたアニメーションの総再生時間(maxTime)に対し、
     * 現在の角度(Yaw)から「再生すべき時間(Time)」を割り当てて返します。
     */
    public float getAnimationTimeByYaw(float maxTime, float partialTick) {
        float yawProgress = getInterpolatedYaw(partialTick) / 360.0f; // 0.0 ～ 1.0 に正規化
        return yawProgress * maxTime; // アニメーション時間上の再生位置を決定
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putFloat("Yaw", this.currentYaw);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.currentYaw = tag.getFloat("Yaw");
        this.prevYaw = this.currentYaw;
    }

    @Override
    public Quaternionf getNodeRotation(String nodeName, float partialTick) {
        return null;
    }

    @Override
    public AABB getRenderBoundingBox() {
        // ブロックの現在位置を中心に、描画判定領域を上下左右に広げる
        // 例: 上下に3ブロック、東西南北に3ブロック分領域を拡張
        return new AABB(this.worldPosition).inflate(3.0);
    }

    @Override
    public float getRotationYaw(float partialTick) {
        // 直前の tick と現在の tick の角度を補間してスムーズに描画
        // (360度から0度への跨ぎ時の飛躍を防ぐため補間計算)
        float diff = this.currentYaw - this.prevYaw;
        if (diff < -180.0f) diff += 360.0f;
        if (diff > 180.0f) diff -= 360.0f;

        return this.prevYaw + diff * partialTick;
    }
}