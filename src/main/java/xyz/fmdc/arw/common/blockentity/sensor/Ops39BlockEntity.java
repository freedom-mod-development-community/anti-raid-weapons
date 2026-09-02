package xyz.fmdc.arw.common.blockentity.sensor;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import xyz.fmdc.arw.registry.ModBlocks;

public class Ops39BlockEntity extends HorizontalRadarBlockEntity {

    private float currentYaw = 0.0f;
    private float prevYaw = 0.0f;
    private final float RPM = 30f;
    private final float rotationSpeed = RPM * 360 / (60*20) ; // 毎Tick回転する速度

    public Ops39BlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.OPS39.getBEType(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, Ops39BlockEntity be) {
        be.tickSensor();
        be.prevYaw = be.currentYaw;
        be.currentYaw = (be.currentYaw + be.rotationSpeed) % 360.0f;
    }

    @Override
    public float getScanRange() {
        return 500;
    }

    @Override
    public void performScan() {
    }

    @Override
    public float getTargetYaw(float partialTick) {
        return Mth.rotLerp(partialTick, this.prevYaw, this.currentYaw);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(this.worldPosition).inflate(2.0);
    }
}
