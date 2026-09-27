package xyz.fmdc.arw.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
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
        INSTANCE.messageBuilder(S2CSyncRadarTargetsPacket.class, id())
                .encoder(S2CSyncRadarTargetsPacket::encode)
                .decoder(S2CSyncRadarTargetsPacket::new)
                .consumerMainThread(S2CSyncRadarTargetsPacket::handle)
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
        INSTANCE.messageBuilder(ServerboundFcsSensorPowerPacket.class, id())
                .encoder(ServerboundFcsSensorPowerPacket::toBytes)
                .decoder(ServerboundFcsSensorPowerPacket::new)
                .consumerMainThread(ServerboundFcsSensorPowerPacket::handle)
                .add();
        INSTANCE.messageBuilder(ServerboundSetTargetAffiliationPacket.class, id())
                .encoder(ServerboundSetTargetAffiliationPacket::toBytes)
                .decoder(ServerboundSetTargetAffiliationPacket::new)
                .consumerMainThread(ServerboundSetTargetAffiliationPacket::handle)
                .add();

        // 仮想飛翔体（Virtual Projectile）用S2Cパケット
        INSTANCE.messageBuilder(S2CSpawnVirtualProjectilePacket.class, id())
                .encoder(S2CSpawnVirtualProjectilePacket::encode)
                .decoder(S2CSpawnVirtualProjectilePacket::new)
                .consumerMainThread(S2CSpawnVirtualProjectilePacket::handle)
                .add();
        INSTANCE.messageBuilder(S2CDestroyVirtualProjectilePacket.class, id())
                .encoder(S2CDestroyVirtualProjectilePacket::encode)
                .decoder(S2CDestroyVirtualProjectilePacket::new)
                .consumerMainThread(S2CDestroyVirtualProjectilePacket::handle)
                .add();
    }

    // クライアントからのパケット送信ヘルパー
    public static void sendToServer(Object message) {
        INSTANCE.sendToServer(message);
    }

    // サーバーから特定プレイヤーへの送信
    public static void sendToPlayer(ServerPlayer player, Object message) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    // サーバーから特定ディメンション内の全プレイヤーへの送信
    public static void sendToDimension(ServerLevel level, Object message) {
        INSTANCE.send(PacketDistributor.DIMENSION.with(level::dimension), message);
    }

    // サーバーから特定座標周辺のプレイヤーへの送信
    public static void sendToNear(ServerLevel level, Vec3 pos, double radius, Object message) {
        INSTANCE.send(
                PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                        pos.x, pos.y, pos.z, radius, level.dimension()
                )),
                message
        );
    }
}
