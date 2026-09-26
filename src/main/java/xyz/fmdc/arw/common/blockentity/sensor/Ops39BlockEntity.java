package xyz.fmdc.arw.common.blockentity.sensor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import xyz.fmdc.arw.api.sensor.RadarScanRange;
import xyz.fmdc.arw.registry.ModBlocks;

/**
 * OPS-39 対水上捜索レーダー
 * 主な用途: 水上目標・ボート・沿岸哨戒
 */
public class Ops39BlockEntity extends HorizontalRadarBlockEntity {

    // --- レーダー諸元パラメータ ---
    public static final double RANGE = 450.0;              // 最大探知半径 (ブロック)
    public static final float H_FOV = 360.0f;              // 水平視野角 (全周レーダーとして常時360°カバー)
    public static final float INSTANTANEOUS_BEAM_H_FOV = 1.5f; // 瞬時ビーム幅 (1.5°)
    public static final float MIN_PITCH = -5.0f;           // 最小仰角 (度)
    public static final float MAX_PITCH = 15.0f;           // 最大仰角 (度)
    public static final float V_FOV = MAX_PITCH - MIN_PITCH; // 垂直視野角 (20.0°)
    public static final float RPM = 24.0f;                 // 回転速度 (15〜24 rpm, 24 rpm = 7.2° / tick)
    public static final float ROTATION_SPEED = RPM * 360.0f / (60.0f * 20.0f); // 7.2度/tick

    private final float rotationSpeed = ROTATION_SPEED;

    public Ops39BlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.OPS39.getBEType(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, Ops39BlockEntity be) {
        if (!be.isPowered()) {
            be.prevYaw = be.currentYaw;
            return;
        }
        be.tickSensor();
        be.prevYaw = be.currentYaw;
        be.currentYaw = (be.currentYaw + be.rotationSpeed) % 360.0f;
    }

    @Override
    public RadarScanRange getScanRange() {
        return RadarScanRange.OPS39;
    }

    @Override
    public float getInstantaneousBeamHorizontal() {
        return INSTANTANEOUS_BEAM_H_FOV; // 水平ビーム幅 (1.5度)
    }

    @Override
    public void performScan() {
        this.currentScanAngle = (this.currentScanAngle + rotationSpeed) % 360.0f;
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(this.worldPosition).inflate(2.0);
    }
}
