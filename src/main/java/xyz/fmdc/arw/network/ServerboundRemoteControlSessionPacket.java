package xyz.fmdc.arw.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import xyz.fmdc.arw.api.control.IRemoteControllableWeapon;

import java.util.function.Supplier;

public class ServerboundRemoteControlSessionPacket {

    private final BlockPos targetPos;
    private final boolean start;

    public ServerboundRemoteControlSessionPacket(BlockPos targetPos, boolean start) {
        this.targetPos = targetPos;
        this.start = start;
    }

    public ServerboundRemoteControlSessionPacket(FriendlyByteBuf buf) {
        this.targetPos = buf.readBlockPos();
        this.start = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.targetPos);
        buf.writeBoolean(this.start);
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            BlockEntity be = player.level().getBlockEntity(this.targetPos);
            if (be instanceof IRemoteControllableWeapon remoteWeapon) {
                if (this.start) {
                    remoteWeapon.startRemoteControl(player);
                } else {
                    remoteWeapon.stopRemoteControl(player);
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
