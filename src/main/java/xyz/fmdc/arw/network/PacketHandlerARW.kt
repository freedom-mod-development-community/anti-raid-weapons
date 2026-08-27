package xyz.fmdc.arw.network

import net.minecraft.resources.ResourceLocation
import net.minecraftforge.network.NetworkRegistry
import net.minecraftforge.network.simple.SimpleChannel
import xyz.fmdc.arw.ARWMod

object PacketHandlerARW {
    private const val PROTOCOL_VERSION = "1"

    val CHANNEL: SimpleChannel = NetworkRegistry.newSimpleChannel(
        ResourceLocation(ARWMod.DOMAIN, "main"),
        { PROTOCOL_VERSION },
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    )

    private var packetId = 0

    fun register() {
        CHANNEL.registerMessage(
            packetId++,
            SyncAngleMessage::class.java,
            SyncAngleMessage::encode,
            ::SyncAngleMessage,
            SyncAngleMessage::handle
        )
    }
}