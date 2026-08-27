package xyz.fmdc.arw.baseclass.modelblock

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import xyz.fmdc.arw.baseclass.module.rotatable.IYawRotatable
import xyz.fmdc.arw.baseclass.module.rotatable.ModuleYawRotatable

/**
 * 水平旋回のみを行う TileEntity。回転状態は [ModuleYawRotatable] が単一の管理元となる。
 */
open class ModelNormalYawRotatableTileEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : ModelNormalTileEntity(type, pos, state), IYawRotatable {

    override val moduleYawRotatable = ModuleYawRotatable().apply {
        // レーダー類は既定で連続回転
        isContinuousRotating = true
    }

    /** [xyz.fmdc.arw.network.SyncAngleMessage] からの直接同期用 */
    var currentYaw: Float
        get() = moduleYawRotatable.yawAngleDeg
        set(value) {
            moduleYawRotatable.yawAngleDeg = value
        }

    override fun getYawAngle(partialTicks: Float): Float =
        moduleYawRotatable.getInterpolatedYaw(partialTicks)

    override fun tick(level: Level, pos: BlockPos, state: BlockState) {
        super.tick(level, pos, state)
        moduleYawRotatable.tick()
    }

    /** 目標角へ向けた旋回に切り替える */
    fun setTargetYawAngle(yaw: Float) {
        moduleYawRotatable.setTargetYaw(yaw)
        setChanged()
    }

    /** 連続回転に切り替える */
    fun enableContinuousRotation(speed: Float = moduleYawRotatable.rotSpeedYaw) {
        moduleYawRotatable.isContinuousRotating = true
        moduleYawRotatable.rotSpeedYaw = speed
        setChanged()
    }

    override fun load(nbt: CompoundTag) {
        super.load(nbt)
        moduleYawRotatable.readFromNBT(nbt)
    }

    override fun saveAdditional(nbt: CompoundTag) {
        super.saveAdditional(nbt)
        moduleYawRotatable.writeToNBT(nbt)
    }
}
