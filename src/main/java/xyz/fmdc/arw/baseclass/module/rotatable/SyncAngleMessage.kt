package xyz.fmdc.arw.network

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraftforge.network.NetworkEvent
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalYawPitchRotatableTileEntity
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalYawRotatableTileEntity
import java.util.function.Supplier

class SyncAngleMessage(
    val pos: BlockPos,
    val yaw: Float,
    val pitch: Float
) {

    /**
     * デコード（バイト列からメッセージオブジェクトを生成）
     */
    constructor(buf: FriendlyByteBuf) : this(
        pos = buf.readBlockPos(),
        yaw = buf.readFloat(),
        pitch = buf.readFloat()
    )

    /**
     * エンコード（メッセージオブジェクトをバイト列へ書き込み）
     */
    fun encode(buf: FriendlyByteBuf) {
        buf.writeBlockPos(pos)
        buf.writeFloat(yaw)
        buf.writeFloat(pitch)
    }

    /**
     * 受信時のハンドラ処理（クライアント側で実行）
     */
    fun handle(ctxSupplier: Supplier<NetworkEvent.Context>) {
        val ctx = ctxSupplier.get()
        ctx.enqueueWork {
            // クライアントのワールドから対象の BlockEntity を取得して角度を同期
            val level = Minecraft.getInstance().level ?: return@enqueueWork
            val blockEntity = level.getBlockEntity(pos)

            when (blockEntity) {
                is ModelNormalYawPitchRotatableTileEntity -> {
                    blockEntity.currentYaw = yaw
                    blockEntity.currentPitch = pitch
                }
                is ModelNormalYawRotatableTileEntity -> {
                    blockEntity.currentYaw = yaw
                }
            }
        }
        ctx.packetHandled = true
    }
}