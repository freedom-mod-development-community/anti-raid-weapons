package xyz.fmdc.arw.common.blockentity.sensor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import xyz.fmdc.arw.api.RadarTargetManager;
import xyz.fmdc.arw.api.sensor.RadarScanRange;
import xyz.fmdc.arw.registry.ModBlocks;

import java.util.ArrayList;

/**
 * AN/SPQ-9B 対水上・低空目標捜索用パルスドップラーレーダー
 */
public class Spq9bBlockEntity extends HorizontalRadarBlockEntity {

    private final float RPM = 30f;
    private final float rotationSpeed = RPM * 360 / (60 * 20); // 毎Tick回転する速度 (30 RPM = 9度/tick)

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

        // FCS未接続時は自前でグローバルリストからスキャンを実行
        if (!level.isClientSide && !be.isConnectedToFcs()) {
            be.filterAndScan(new ArrayList<>(RadarTargetManager.INSTANCE.getGlobalTargets()));
        }
    }

    @Override
    public RadarScanRange getScanRange() {
        return RadarScanRange.omni(500.0f);
    }

    @Override
    public float getInstantaneousBeamHorizontal() {
        return 15.0f; // 高解像度・狭ビーム (15度)
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
