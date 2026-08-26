package xyz.fmdc.arw.baseclass.modelblock

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.Mth
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import xyz.fmdc.arw.baseclass.module.rotatable.IYawRotatable
import xyz.fmdc.arw.baseclass.module.rotatable.ModuleYawRotatable
import xyz.fmdc.arw.registry.RegistryBlock

open class ModelNormalYawRotatableTileEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : ModelNormalTileEntity(type, pos, state), IYawRotatable {

    constructor(pos: BlockPos, state: BlockState) : this(
        RegistryBlock.BLOCK_ENTITIES_MAP[ModelNormalYawRotatableTileEntity::class.java]?.get()
            ?: throw IllegalStateException("BlockEntityType not registered for ${state.block}"),
        pos,
        state
    )

    // 引数なしコンストラクタ（1.7.10 互換）
    constructor() : this(BlockPos.ZERO, Blocks.AIR.defaultBlockState())

    override val moduleYawRotatable = ModuleYawRotatable()

    var prevYaw: Float = 0.0f
    var currentYaw: Float = 0.0f
    var targetYaw: Float = 0.0f
    var rotSpeedYaw: Float = 6.0f
    var isContinuousRotating: Boolean = true

    private val strYaw = "yawAngDeg"
    private val strTargetYaw = "targetYaw"
    private val strRotSpeedYaw = "rotSpeedYaw"
    private val strIsContinuous = "isContinuousRotating"

    override fun getYawAngle(partialTicks: Float): Float {
        return Mth.rotLerp(partialTicks, this.prevYaw, this.currentYaw)
    }

    override fun tick(level: Level, pos: BlockPos, state: BlockState) {
        super.tick(level, pos, state)
        this.prevYaw = this.currentYaw

        if (isContinuousRotating) {
            this.currentYaw = Mth.wrapDegrees(this.currentYaw + rotSpeedYaw)
        } else {
            val diffYaw = Mth.degreesDifference(this.currentYaw, this.targetYaw)
            if (Mth.abs(diffYaw) > 0.001f) {
                val stepYaw = Mth.clamp(diffYaw, -rotSpeedYaw, rotSpeedYaw)
                this.currentYaw = Mth.wrapDegrees(this.currentYaw + stepYaw)
            } else {
                this.currentYaw = this.targetYaw
            }
        }
    }

    fun setTargetYawAngle(yaw: Float) {
        this.isContinuousRotating = false
        this.targetYaw = yaw
        setChanged()
    }

    fun enableContinuousRotation(speed: Float = this.rotSpeedYaw) {
        this.isContinuousRotating = true
        this.rotSpeedYaw = speed
        setChanged()
    }

    override fun load(nbt: CompoundTag) {
        super.load(nbt)
        if (nbt.contains(strYaw)) {
            this.currentYaw = nbt.getFloat(strYaw)
            this.prevYaw = this.currentYaw
        }
        if (nbt.contains(strTargetYaw)) {
            this.targetYaw = nbt.getFloat(strTargetYaw)
        }
        if (nbt.contains(strRotSpeedYaw)) {
            this.rotSpeedYaw = nbt.getFloat(strRotSpeedYaw)
        }
        if (nbt.contains(strIsContinuous)) {
            this.isContinuousRotating = nbt.getBoolean(strIsContinuous)
        }
    }

    override fun saveAdditional(nbt: CompoundTag) {
        super.saveAdditional(nbt)
        nbt.putFloat(strYaw, this.currentYaw)
        nbt.putFloat(strTargetYaw, this.targetYaw)
        nbt.putFloat(strRotSpeedYaw, this.rotSpeedYaw)
        nbt.putBoolean(strIsContinuous, this.isContinuousRotating)
    }
}