package xyz.fmdc.arw

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity

/**
 * BlockEntity (TileEntity) から更新パケットを生成する拡張関数 (旧 newPacketUpdateTileEntity)
 */
fun BlockEntity.newPacketUpdateBlockEntity(): Packet<ClientGamePacketListener>? {
    return ClientboundBlockEntityDataPacket.create(this)
}

/**
 * 受信したパケットの NBT データを BlockEntity にロードする拡張関数 (旧 pkt.loadTo(this))
 */
fun ClientboundBlockEntityDataPacket.loadTo(blockEntity: BlockEntity) {
    val compoundTag = this.tag
    compoundTag?.let { blockEntity.load(it) }
}

/**
 * サーバー側で BlockEntity の変更をマークし、周囲のクライアントへ同期・再描画を通知するヘルパー
 */
fun BlockEntity.syncToClient() {
    val lvl = this.level ?: return
    if (!lvl.isClientSide) {
        this.setChanged()
        // worldPosition ではなく blockPos を使用
        lvl.sendBlockUpdated(this.blockPos, this.blockState, this.blockState, Block.UPDATE_ALL)
    }
}

/**
 * CompoundTag への安全な double / float 取得用拡張関数
 */
fun CompoundTag.getDoubleOrDefault(key: String, default: Double = 0.0): Double {
    return if (this.contains(key)) this.getDouble(key) else default
}

fun CompoundTag.getFloatOrDefault(key: String, default: Float = 0.0f): Float {
    return if (this.contains(key)) this.getFloat(key) else default
}