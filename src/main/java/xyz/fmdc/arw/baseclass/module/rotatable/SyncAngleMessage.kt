package xyz.fmdc.arw.baseclass.module.rotatable

import io.netty.buffer.ByteBuf
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext
import xyz.fmdc.arw.network.PacketHandlerARW
import xyz.fmdc.arw.network.TileEntityMessage

class SyncAngleMessage(var yawDeg: Double, var pitchDeg: Double) : TileEntityMessage() {

    override fun write(buf: ByteBuf) {
        buf.writeDouble(yawDeg)
        buf.writeDouble(pitchDeg)
    }

    override fun read(buf: ByteBuf) {
        yawDeg = buf.readDouble()
        pitchDeg = buf.readDouble()
    }

    companion object : IMessageHandler<SyncAngleMessage, IMessage?> {
        override fun onMessage(message: SyncAngleMessage, ctx: MessageContext): IMessage? {
            val tile = message.getTileEntity(ctx)
            if (tile is IYawRotatable) {
                tile.yawDeg = message.yawDeg
            }
            if (tile is IYawPitchRotatable) {
                tile.pitchDeg = message.pitchDeg
            }
            if (ctx.side.isServer) {
                PacketHandlerARW.sendPacketAll(message)
            }
            return null
        }
    }
}
