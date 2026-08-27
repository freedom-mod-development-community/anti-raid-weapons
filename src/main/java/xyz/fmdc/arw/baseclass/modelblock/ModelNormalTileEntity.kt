package xyz.fmdc.arw.baseclass.modelblock

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.Connection
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import xyz.fmdc.arw.baseclass.module.direction.IDirection
import xyz.fmdc.arw.baseclass.module.direction.ModuleDirection
import xyz.fmdc.arw.loadTo

open class ModelNormalTileEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state), IDirection {

    override val moduleDirection = ModuleDirection()

    // =========================================================================
    // Yaw / Pitch ゲッター (Tick間補間対応)
    // 回転しないブロックでは常に 0。回転可能なサブクラス側でオーバーライドする。
    // =========================================================================

    open fun getYawAngle(partialTicks: Float): Float = 0.0f

    open fun getPitchAngle(partialTicks: Float): Float = 0.0f

    // =========================================================================
    // ライフサイクル & NBT
    // =========================================================================

    open fun tick(level: Level, pos: BlockPos, state: BlockState) {
    }

    override fun load(nbt: CompoundTag) {
        super.load(nbt)
        if (nbt.contains(KEY_DIRECTION)) {
            this.saveDirectionData(nbt.getDouble(KEY_DIRECTION))
        }
    }

    override fun saveAdditional(nbt: CompoundTag) {
        super.saveAdditional(nbt)
        nbt.putDouble(KEY_DIRECTION, this.getDirectionAngle())
    }

    /**
     * OBJ モデルはブロック 1 個分より大きいため、ブロックの当たり判定サイズから
     * 描画用バウンディングボックスを広げてカリング落ちを防ぐ。
     */
    override fun getRenderBoundingBox(): AABB {
        val block = this.blockState.block
        if (block !is ModelNormalBlockContainer) {
            return super.getRenderBoundingBox()
        }

        val halfWide = block.selectBoundsHalfWide + 1.0
        val height = block.selectBoundsHeight + 1.0

        val x = this.blockPos.x.toDouble()
        val y = this.blockPos.y.toDouble()
        val z = this.blockPos.z.toDouble()

        return AABB(
            x - halfWide + 0.5,
            y,
            z - halfWide + 0.5,
            x + halfWide + 0.5,
            y + height,
            z + halfWide + 0.5
        )
    }

    override fun getUpdatePacket(): Packet<ClientGamePacketListener>? =
        ClientboundBlockEntityDataPacket.create(this)

    override fun onDataPacket(net: Connection, pkt: ClientboundBlockEntityDataPacket) {
        pkt.loadTo(this)
    }

    override fun getUpdateTag(): CompoundTag = saveWithoutMetadata()

    private companion object {
        const val KEY_DIRECTION = "directionAngDeg"
    }
}
