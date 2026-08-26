package xyz.fmdc.arw.network

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.network.NetworkEvent
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalTileEntity
import java.util.function.Supplier

class TileEntityMessage(
    val pos: BlockPos,
    val tag: CompoundTag
) {

    /**
     * BlockEntity の現在状態（またはカスタムタグ）からパケットを生成するセカンダリコンストラクタ
     */
    constructor(tile: BlockEntity, tag: CompoundTag = tile.saveWithoutMetadata()) : this(
        pos = tile.blockPos,
        tag = tag
    )

    /**
     * デコード（ByteBuf -> Message）
     */
    constructor(buf: FriendlyByteBuf) : this(
        pos = buf.readBlockPos(),
        tag = buf.readNbt() ?: CompoundTag()
    )

    /**
     * エンコード（Message -> ByteBuf）
     */
    fun encode(buf: FriendlyByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeNbt(tag)
    }

    /**
     * クライアント側受信ハンドラ
     */
    fun handle(ctxSupplier: Supplier<NetworkEvent.Context>) {
        val ctx = ctxSupplier.get()
        ctx.enqueueWork {
            val level = Minecraft.getInstance().level ?: return@enqueueWork
            val blockEntity = level.getBlockEntity(pos) ?: return@enqueueWork

            // NBT データを BlockEntity に反映
            blockEntity.load(tag)

            // ModelNormalTileEntity の場合は再描画等のフックを実行
            if (blockEntity is ModelNormalTileEntity) {
                blockEntity.setChanged()
            }
        }
        ctx.packetHandled = true
    }
}