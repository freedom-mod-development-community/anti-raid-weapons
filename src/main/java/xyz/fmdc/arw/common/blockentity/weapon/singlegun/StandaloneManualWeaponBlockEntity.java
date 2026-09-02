package xyz.fmdc.arw.common.blockentity.weapon.singlegun;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import xyz.fmdc.arw.common.blockentity.AbstractSingleGunBlockEntity;

/**
 * FCSネットワークに接続しない、手動単装砲.
 */
public abstract class StandaloneManualWeaponBlockEntity extends AbstractSingleGunBlockEntity {

    public StandaloneManualWeaponBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * 手動操作による目標旋回角の直接入力
     */
    public void addManualInput(float yawDelta, float pitchDelta) {
        setTargetYaw(this.targetYaw + yawDelta);
        setTargetPitch(this.targetPitch + pitchDelta);
    }
}
