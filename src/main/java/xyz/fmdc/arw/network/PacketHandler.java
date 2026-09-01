package xyz.fmdc.arw.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import xyz.fmdc.arw.AntiRaidWeapons;

import java.util.Optional;

public class PacketHandler {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(AntiRaidWeapons.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    public static void register() {
        INSTANCE.registerMessage(
                id(),
                ServerboundWeaponControlPacket.class,
                ServerboundWeaponControlPacket::encode,
                ServerboundWeaponControlPacket::new,
                ServerboundWeaponControlPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        INSTANCE.registerMessage(
                id(),
                Mk45PacketTest.class,
                Mk45PacketTest::encode,
                Mk45PacketTest::new,
                Mk45PacketTest::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        INSTANCE.messageBuilder(UpdateRadarDisplayConfigPacket.class, id())
                .encoder(UpdateRadarDisplayConfigPacket::toBytes)
                .decoder(UpdateRadarDisplayConfigPacket::new)
                .consumerMainThread(UpdateRadarDisplayConfigPacket::handle)
                .add();
        INSTANCE.messageBuilder(ServerboundFcsCoreUnregisterPacket.class, id())
                .encoder(ServerboundFcsCoreUnregisterPacket::toBytes)
                .decoder(ServerboundFcsCoreUnregisterPacket::new)
                .consumerMainThread(ServerboundFcsCoreUnregisterPacket::handle)
                .add();
        INSTANCE.messageBuilder(ServerboundRemoteControlSessionPacket.class, id())
                .encoder(ServerboundRemoteControlSessionPacket::toBytes)
                .decoder(ServerboundRemoteControlSessionPacket::new)
                .consumerMainThread(ServerboundRemoteControlSessionPacket::handle)
                .add();
    }

    // クライアントからのパケット送信ヘルパー
    public static void sendToServer(Object message) {
        INSTANCE.sendToServer(message);
    }
}
