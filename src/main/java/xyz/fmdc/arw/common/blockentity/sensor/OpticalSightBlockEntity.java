package xyz.fmdc.arw.common.blockentity.sensor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import xyz.fmdc.arw.common.blockentity.weapon.StandaloneManualWeaponBlockEntity;
import xyz.fmdc.arw.registry.ModBlocks;

/**
 * FCSを介さず、アナログ高角砲等へ1対1で直結して照準方位角を送る光学測遠機
 */
public class OpticalSightBlockEntity extends AbstractHRadarBlockEntity {

    private StandaloneManualWeaponBlockEntity connectedWeapon;
    private float sightYaw = 0.0f;
    private float sightPitch = 0.0f;

    public OpticalSightBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.OPTICAL_SIGHT_BLOCK.getBEType(), pos, state);
    }

    @Override
    public float getScanRange() {
        return 128.0f; // 目視限界距離
    }

    @Override
    public void performScan() {
        // 目視範囲内のエンティティ計算ロジック（スケルトン）
        if (connectedWeapon != null) {
            connectedWeapon.setTargetYaw(this.sightYaw);
            connectedWeapon.setTargetPitch(this.sightPitch);
        }
    }

    public void setConnectedWeapon(StandaloneManualWeaponBlockEntity weapon) {
        this.connectedWeapon = weapon;
    }

    public void setManualSightRotation(float yaw, float pitch) {
        this.sightYaw = yaw;
        this.sightPitch = pitch;
    }
}