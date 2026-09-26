package xyz.fmdc.arw.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import xyz.fmdc.arw.client.ClientVirtualProjectileHandler;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 仮想飛翔体の着弾・爆発・消滅をクライアントへ通知するパケット。
 */
public class S2CDestroyVirtualProjectilePacket {

    private final UUID projectileId;
    private final Vec3 hitPosition;
    private final byte destroyReason; // 0: 着弾爆発, 1: 空中自爆, 2: 寿命消失

    public S2CDestroyVirtualProjectilePacket(UUID projectileId, Vec3 hitPosition, byte destroyReason) {
        this.projectileId = projectileId;
        this.hitPosition = hitPosition;
        this.destroyReason = destroyReason;
    }

    public S2CDestroyVirtualProjectilePacket(FriendlyByteBuf buf) {
        this.projectileId = buf.readUUID();
        this.hitPosition = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        this.destroyReason = buf.readByte();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(this.projectileId);
        buf.writeDouble(this.hitPosition.x);
        buf.writeDouble(this.hitPosition.y);
        buf.writeDouble(this.hitPosition.z);
        buf.writeByte(this.destroyReason);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientVirtualProjectileHandler.handleDestroy(
                        this.projectileId,
                        this.hitPosition,
                        this.destroyReason
                );
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
