package xyz.fmdc.arw.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import xyz.fmdc.arw.client.ClientVirtualProjectileHandler;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * サーバー側で生成された仮想飛翔体の描画用ダミーEntity生成パケット。
 */
public class S2CSpawnVirtualProjectilePacket {

    private final UUID projectileId;
    private final byte projectileType; // 0: CIWS, 1: 5-INCH SHELL, 2: MISSILE
    private final Vec3 position;
    private final Vec3 velocity;
    private final int maxAgeTicks;
    private final CompoundTag extraData;

    public S2CSpawnVirtualProjectilePacket(
            UUID projectileId,
            byte projectileType,
            Vec3 position,
            Vec3 velocity,
            int maxAgeTicks,
            CompoundTag extraData
    ) {
        this.projectileId = projectileId;
        this.projectileType = projectileType;
        this.position = position;
        this.velocity = velocity;
        this.maxAgeTicks = maxAgeTicks;
        this.extraData = extraData != null ? extraData : new CompoundTag();
    }

    public S2CSpawnVirtualProjectilePacket(FriendlyByteBuf buf) {
        this.projectileId = buf.readUUID();
        this.projectileType = buf.readByte();
        this.position = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        this.velocity = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        this.maxAgeTicks = buf.readVarInt();
        this.extraData = buf.readNbt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(this.projectileId);
        buf.writeByte(this.projectileType);
        buf.writeDouble(this.position.x);
        buf.writeDouble(this.position.y);
        buf.writeDouble(this.position.z);
        buf.writeDouble(this.velocity.x);
        buf.writeDouble(this.velocity.y);
        buf.writeDouble(this.velocity.z);
        buf.writeVarInt(this.maxAgeTicks);
        buf.writeNbt(this.extraData);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientVirtualProjectileHandler.handleSpawn(
                        this.projectileId,
                        this.projectileType,
                        this.position,
                        this.velocity,
                        this.maxAgeTicks,
                        this.extraData
                );
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
