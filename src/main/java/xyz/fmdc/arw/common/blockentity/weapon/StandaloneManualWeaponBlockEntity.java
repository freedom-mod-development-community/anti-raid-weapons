package xyz.fmdc.arw.common.blockentity.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import xyz.fmdc.arw.common.blockentity.AbstractWeaponBlockEntity;

/**
 * FCSネットワークに接続しない、手動・旧式兵装の抽象クラス
 */
public abstract class StandaloneManualWeaponBlockEntity extends AbstractWeaponBlockEntity {

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
