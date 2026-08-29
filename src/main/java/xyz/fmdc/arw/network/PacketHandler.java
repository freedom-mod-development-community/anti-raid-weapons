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
    }

    // クライアントからのパケット送信ヘルパー
    public static void sendToServer(Object message) {
        INSTANCE.sendToServer(message);
    }
}