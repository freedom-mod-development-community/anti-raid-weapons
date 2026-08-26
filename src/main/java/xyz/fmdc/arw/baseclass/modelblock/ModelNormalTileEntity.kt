package xyz.fmdc.arw.baseclass.modelblock

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.Connection
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import xyz.fmdc.arw.baseclass.module.direction.IDirection
import xyz.fmdc.arw.baseclass.module.direction.ModuleDirection
import xyz.fmdc.arw.baseclass.module.rotatable.IPitchRotatable
import xyz.fmdc.arw.baseclass.module.rotatable.IYawRotatable
import xyz.fmdc.arw.loadTo
import xyz.fmdc.arw.registry.RegistryBlock

open class ModelNormalTileEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state), IDirection {

    // (BlockPos, BlockState) 標準コンストラクタ
    constructor(pos: BlockPos, state: BlockState) : this(
        RegistryBlock.BLOCK_ENTITIES_MAP[ModelNormalTileEntity::class.java]?.get()
            ?: throw IllegalStateException("BlockEntityType not registered for ${state.block}"),
        pos,
        state
    )

    // 引数なしコンストラクタ (1.7.10 互換)
    constructor() : this(BlockPos.ZERO, Blocks.AIR.defaultBlockState())

    override val moduleDirection = ModuleDirection()
    private val strDirectionAngDeg = "directionAngDeg"

    // =========================================================================
    // Yaw / Pitch ゲッター (Tick間補間対応)
    // =========================================================================

    /**
     * 描画用の補間された Yaw 角度（度数法）を取得
     */
    open fun getYawAngle(partialTicks: Float = 1.0f): Float {
        return if (this is IYawRotatable) {
            this.getYawAngle(partialTicks)
        } else {
            0.0f
        }
    }

    /**
     * 描画用の補間された Pitch 角度（度数法）を取得
     */
    open fun getPitchAngle(partialTicks: Float = 1.0f): Float {
        return if (this is IPitchRotatable) {
            this.getPitchAngle(partialTicks)
        } else {
            0.0f
        }
    }

    // =========================================================================
    // ライフサイクル & NBT
    // =========================================================================

    open fun tick(level: Level, pos: BlockPos, state: BlockState) {
    }

    override fun load(nbt: CompoundTag) {
        super.load(nbt)
        if (nbt.contains(strDirectionAngDeg)) {
            this.saveDirectionData(nbt.getDouble(strDirectionAngDeg))
        }
    }

    override fun saveAdditional(nbt: CompoundTag) {
        super.saveAdditional(nbt)
        nbt.putDouble(strDirectionAngDeg, this.getDirectionAngle())
    }

    override fun getRenderBoundingBox(): AABB {
        val block = this.blockState.block
        if (block !is ModelNormalBlockContainer) {
            return super.getRenderBoundingBox()
        }

        val selectBoundsHalfWide = block.selectBoundsHalfWide + 1.0
        val selectBoundsHeight = block.selectBoundsHeight + 1.0

        val x = this.blockPos.x.toDouble()
        val y = this.blockPos.y.toDouble()
        val z = this.blockPos.z.toDouble()

        return AABB(
            x - selectBoundsHalfWide + 0.5,
            y,
            z - selectBoundsHalfWide + 0.5,
            x + selectBoundsHalfWide + 0.5,
            y + selectBoundsHeight,
            z + selectBoundsHalfWide + 0.5
        )
    }

    override fun getUpdatePacket(): Packet<ClientGamePacketListener>? {
        return ClientboundBlockEntityDataPacket.create(this)
    }

    override fun onDataPacket(net: Connection, pkt: ClientboundBlockEntityDataPacket) {
        pkt.loadTo(this)
    }

    override fun getUpdateTag(): CompoundTag {
        return saveWithoutMetadata()
    }

    fun getViewDistance(): Double {
        return 65536.0
    }
}