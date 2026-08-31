package xyz.fmdc.arw.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import xyz.fmdc.arw.common.blockentity.fcs.AbstractFcsCoreBlockEntity;

import java.util.UUID;
import java.util.function.Supplier;

public class ServerboundFcsCoreUnregisterPacket {
    private final BlockPos corePos;
    private final UUID targetUuid;

    public ServerboundFcsCoreUnregisterPacket(BlockPos corePos, UUID targetUuid) {
        this.corePos = corePos;
        this.targetUuid = targetUuid;
    }

    public ServerboundFcsCoreUnregisterPacket(FriendlyByteBuf buf) {
        this.corePos = buf.readBlockPos();
        this.targetUuid = buf.readUUID();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.corePos);
        buf.writeUUID(this.targetUuid);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null && player.level().isLoaded(corePos)) {
                BlockEntity be = player.level().getBlockEntity(corePos);
                if (be instanceof AbstractFcsCoreBlockEntity fcsCore) {
                    fcsCore.unregisterDevice(targetUuid);
                }
            }
        });
        return true;
    }
}
