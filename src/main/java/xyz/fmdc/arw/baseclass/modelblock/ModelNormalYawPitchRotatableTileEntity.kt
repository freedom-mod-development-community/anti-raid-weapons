package xyz.fmdc.arw.baseclass.modelblock

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.Mth
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import xyz.fmdc.arw.baseclass.module.rotatable.IPitchRotatable
import xyz.fmdc.arw.baseclass.module.rotatable.IYawRotatable
import xyz.fmdc.arw.baseclass.module.rotatable.ModulePitchRotatable
import xyz.fmdc.arw.baseclass.module.rotatable.ModuleYawRotatable
import xyz.fmdc.arw.registry.RegistryBlock

open class ModelNormalYawPitchRotatableTileEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : ModelNormalTileEntity(type, pos, state), IYawRotatable, IPitchRotatable {

    constructor(pos: BlockPos, state: BlockState) : this(
        RegistryBlock.BLOCK_ENTITIES_MAP[ModelNormalYawPitchRotatableTileEntity::class.java]?.get()
            ?: throw IllegalStateException("BlockEntityType not registered for ${state.block}"),
        pos,
        state
    )

    // 引数なしコンストラクタ（1.7.10 互換）
    constructor() : this(BlockPos.ZERO, Blocks.AIR.defaultBlockState())

    override val moduleYawRotatable = ModuleYawRotatable()
    override val modulePitchRotatable = ModulePitchRotatable()

    var prevYaw: Float = 0.0f
    var currentYaw: Float = 0.0f
    var targetYaw: Float = 0.0f
    var rotSpeedYaw: Float = 5.0f

    var prevPitch: Float = 0.0f
    var currentPitch: Float = 0.0f
    var targetPitch: Float = 0.0f
    var rotSpeedPitch: Float = 3.0f

    var minPitch: Float = -10.0f
    var maxPitch: Float = 85.0f

    private val strYaw = "yawAngDeg"
    private val strPitch = "pitchAngDeg"
    private val strTargetYaw = "targetYaw"
    private val strTargetPitch = "targetPitch"
    private val strRotSpeedYaw = "rotSpeedYaw"
    private val strRotSpeedPitch = "rotSpeedPitch"

    /**
     * IYawRotatable と IPitchRotatable の重複メソッドを明示的にオーバーライド
     */
    override fun syncAngleToClient() {
        // 必要に応じて両方のインターフェースの super を呼ぶか、TileEntityの同期処理を実行
        super<IYawRotatable>.syncAngleToClient()
        super<IPitchRotatable>.syncAngleToClient()
    }

    override fun getYawAngle(partialTicks: Float): Float {
        return Mth.rotLerp(partialTicks, this.prevYaw, this.currentYaw)
    }

    override fun getPitchAngle(partialTicks: Float): Float {
        return Mth.lerp(partialTicks, this.prevPitch, this.currentPitch)
    }

    override fun tick(level: Level, pos: BlockPos, state: BlockState) {
        super.tick(level, pos, state)
        this.prevYaw = this.currentYaw
        this.prevPitch = this.currentPitch

        val diffYaw = Mth.degreesDifference(this.currentYaw, this.targetYaw)
        if (Mth.abs(diffYaw) > 0.001f) {
            val stepYaw = Mth.clamp(diffYaw, -rotSpeedYaw, rotSpeedYaw)
            this.currentYaw = Mth.wrapDegrees(this.currentYaw + stepYaw)
        } else {
            this.currentYaw = this.targetYaw
        }

        val clampedTargetPitch = Mth.clamp(this.targetPitch, minPitch, maxPitch)
        val diffPitch = clampedTargetPitch - this.currentPitch
        if (Mth.abs(diffPitch) > 0.001f) {
            val stepPitch = Mth.clamp(diffPitch, -rotSpeedPitch, rotSpeedPitch)
            this.currentPitch = Mth.clamp(this.currentPitch + stepPitch, minPitch, maxPitch)
        } else {
            this.currentPitch = clampedTargetPitch
        }
    }

    fun setTargetAngles(yaw: Float, pitch: Float) {
        this.targetYaw = yaw
        this.targetPitch = pitch
        setChanged()
    }

    override fun load(nbt: CompoundTag) {
        super.load(nbt)
        if (nbt.contains(strYaw)) {
            this.currentYaw = nbt.getFloat(strYaw)
            this.prevYaw = this.currentYaw
        }
        if (nbt.contains(strPitch)) {
            this.currentPitch = nbt.getFloat(strPitch)
            this.prevPitch = this.currentPitch
        }
        if (nbt.contains(strTargetYaw)) {
            this.targetYaw = nbt.getFloat(strTargetYaw)
        }
        if (nbt.contains(strTargetPitch)) {
            this.targetPitch = nbt.getFloat(strTargetPitch)
        }
        if (nbt.contains(strRotSpeedYaw)) {
            this.rotSpeedYaw = nbt.getFloat(strRotSpeedYaw)
        }
        if (nbt.contains(strRotSpeedPitch)) {
            this.rotSpeedPitch = nbt.getFloat(strRotSpeedPitch)
        }
    }

    override fun saveAdditional(nbt: CompoundTag) {
        super.saveAdditional(nbt)
        nbt.putFloat(strYaw, this.currentYaw)
        nbt.putFloat(strPitch, this.currentPitch)
        nbt.putFloat(strTargetYaw, this.targetYaw)
        nbt.putFloat(strTargetPitch, this.targetPitch)
        nbt.putFloat(strRotSpeedYaw, this.rotSpeedYaw)
        nbt.putFloat(strRotSpeedPitch, this.rotSpeedPitch)
    }
}