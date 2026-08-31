package xyz.fmdc.arw.common.blockentity.sensor;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import xyz.fmdc.arw.client.util.IYawModel;
import xyz.fmdc.arw.common.blockentity.AbstractARWBlockEntity;
import xyz.fmdc.arw.registry.ModBlocks;

public class Spq9bBlockEntity extends AbstractARWBlockEntity implements IYawModel {

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
    public AABB getRenderBoundingBox() {
        return new AABB(this.worldPosition).inflate(3.0);
    }

    @Override
    public float getTargetYaw(float partialTick) {
        // 直前の tick と現在の tick の角度を補間してスムーズに描画
        // (360度から0度への跨ぎ時の飛躍を防ぐため補間計算)
        float diff = this.currentYaw - this.prevYaw;
        if (diff < -180.0f) diff += 360.0f;
        if (diff > 180.0f) diff -= 360.0f;

        return this.prevYaw + diff * partialTick;
    }
}
