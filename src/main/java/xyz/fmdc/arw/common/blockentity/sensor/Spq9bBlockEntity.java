package xyz.fmdc.arw.common.blockentity.sensor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import xyz.fmdc.arw.api.sensor.RadarScanRange;
import xyz.fmdc.arw.registry.ModBlocks;

/**
 * AN/SPQ-9B 対水上・低空目標捜索用パルスドップラーレーダー
 * 主な用途: シースキマー/対空迎撃、5インチ砲FCS連携
 */
public class Spq9bBlockEntity extends HorizontalRadarBlockEntity {

    // --- レーダー諸元パラメータ ---
    public static final double RANGE = 300.0;              // 最大探知半径 (ブロック)
    public static final float H_FOV = 360.0f;              // 水平視野角 (全周レーダーとして常時360°カバー)
    public static final float INSTANTANEOUS_BEAM_H_FOV = 1.5f; // 瞬時ビーム幅 (1.5°)
    public static final float MIN_PITCH = -10.0f;          // 最小仰角 (度)
    public static final float MAX_PITCH = 50.0f;           // 最大仰角 (度)
    public static final float V_FOV = MAX_PITCH - MIN_PITCH; // 垂直視野角 (60.0°)
    public static final float RPM = 30.0f;                 // 回転速度 (30 RPM = 9.0° / tick, 2秒で1周)
    public static final float ROTATION_SPEED = RPM * 360.0f / (60.0f * 20.0f); // 9.0度/tick

    private final float rotationSpeed = ROTATION_SPEED;

    public Spq9bBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.SPQ9B.getBEType(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, Spq9bBlockEntity be) {
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
        return RadarScanRange.SPQ9B;
    }

    @Override
    public float getInstantaneousBeamHorizontal() {
        return INSTANTANEOUS_BEAM_H_FOV; // 狭ビーム幅 (1.5度)
    }

    @Override
    public void performScan() {
        this.currentScanAngle = (this.currentScanAngle + rotationSpeed) % 360.0f;
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(this.worldPosition).inflate(3.0);
    }
}
