package xyz.fmdc.arw

import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.level.block.entity.BlockEntity

/**
 * 受信したパケットの NBT データを BlockEntity にロードする拡張関数
 */
fun ClientboundBlockEntityDataPacket.loadTo(blockEntity: BlockEntity) {
    this.tag?.let { blockEntity.load(it) }
}
