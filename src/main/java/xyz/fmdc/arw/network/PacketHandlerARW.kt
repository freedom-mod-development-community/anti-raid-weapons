package xyz.fmdc.arw.network

import cpw.mods.fml.common.network.NetworkRegistry
import cpw.mods.fml.common.network.simpleimpl.IMessage
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.relauncher.Side
import net.minecraft.entity.player.EntityPlayerMP
import xyz.fmdc.arw.ARWMod
import xyz.fmdc.arw.modcore.baseclass.module.rotatable.SyncAngleMessage

object PacketHandlerARW {
    private val INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(ARWMod.DOMAIN)


    fun init() {
        registerMessage(SyncAngleMessage.Companion, SyncAngleMessage::class.java, 0, Side.SERVER)
        registerMessage(SyncAngleMessage.Companion, SyncAngleMessage::class.java, 0, Side.CLIENT)
    }

    private fun <REQ : IMessage, REPLY : IMessage?> registerMessage(
        messageHandler: IMessageHandler<REQ, REPLY>,
        requestMessageType: Class<REQ>,
        discriminator: Int,
        sendTo: Side,
    ) {
        INSTANCE.registerMessage(messageHandler, requestMessageType, discriminator, sendTo)
    }

    @JvmStatic
    fun sendPacketServer(message: IMessage) {
        INSTANCE.sendToServer(message)
    }

    @JvmStatic
    fun sendPacketAll(message: IMessage) {
        INSTANCE.sendToAll(message)
    }

    @JvmStatic
    fun sendPacketEPM(message: IMessage, EPM: EntityPlayerMP) {
        INSTANCE.sendTo(message, EPM)
    }

    @JvmStatic
    fun sendPacketAround(message: IMessage, dimension: Int, x: Double, y: Double, z: Double, range: Double) {
        INSTANCE.sendToAllAround(message, NetworkRegistry.TargetPoint(dimension, x, y, z, range))
    }


}
