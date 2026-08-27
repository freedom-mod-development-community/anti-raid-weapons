package xyz.fmdc.arw.network

import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.fml.DistExecutor
import net.minecraftforge.network.NetworkEvent
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
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT) {
                Runnable { ClientSyncAngleHandler.applyAngle(pos, yaw, pitch) }
            }
        }
        ctx.packetHandled = true
    }
}