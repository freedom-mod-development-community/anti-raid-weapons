package xyz.fmdc.arw.baseclass.modelblock

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import xyz.fmdc.arw.baseclass.module.rotatable.IYawPitchRotatable
import xyz.fmdc.arw.baseclass.module.rotatable.ModulePitchRotatable
import xyz.fmdc.arw.baseclass.module.rotatable.ModuleYawRotatable

/**
 * 水平旋回 + 俯仰を行う TileEntity。
 * Yaw / Pitch の同期処理の衝突は [IYawPitchRotatable] 側で解決済み。
 */
open class ModelNormalYawPitchRotatableTileEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : ModelNormalTileEntity(type, pos, state), IYawPitchRotatable {

    override val moduleYawRotatable = ModuleYawRotatable()
    override val modulePitchRotatable = ModulePitchRotatable()

    init {
        // サブクラスが指定した既定俯仰角を初期値として反映する
        val defaultPitch = getDefaultPitchDeg().toFloat()
        modulePitchRotatable.pitchAngleDeg = defaultPitch
        modulePitchRotatable.prevPitchAngleDeg = defaultPitch
        modulePitchRotatable.targetPitchAngleDeg = defaultPitch
    }

    /** [xyz.fmdc.arw.network.SyncAngleMessage] からの直接同期用 */
    var currentYaw: Float
        get() = moduleYawRotatable.yawAngleDeg
        set(value) {
            moduleYawRotatable.yawAngleDeg = value
        }

    /** [xyz.fmdc.arw.network.SyncAngleMessage] からの直接同期用 */
    var currentPitch: Float
        get() = modulePitchRotatable.pitchAngleDeg
        set(value) {
            modulePitchRotatable.pitchAngleDeg = value
        }

    override fun getYawAngle(partialTicks: Float): Float =
        moduleYawRotatable.getInterpolatedYaw(partialTicks)

    override fun getPitchAngle(partialTicks: Float): Float =
        modulePitchRotatable.getInterpolatedPitch(partialTicks)

    override fun tick(level: Level, pos: BlockPos, state: BlockState) {
        super.tick(level, pos, state)
        moduleYawRotatable.tick()
        modulePitchRotatable.tick()
    }

    /** Yaw / Pitch の目標角をまとめて設定する */
    fun setTargetAngles(yaw: Float, pitch: Float) {
        moduleYawRotatable.setTargetYaw(yaw)
        modulePitchRotatable.setTargetPitch(pitch)
        setChanged()
    }

    override fun load(nbt: CompoundTag) {
        super.load(nbt)
        moduleYawRotatable.readFromNBT(nbt)
        modulePitchRotatable.readFromNBT(nbt)
    }

    override fun saveAdditional(nbt: CompoundTag) {
        super.saveAdditional(nbt)
        moduleYawRotatable.writeToNBT(nbt)
        modulePitchRotatable.writeToNBT(nbt)
    }
}
