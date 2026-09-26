package xyz.fmdc.arw.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import xyz.fmdc.arw.api.TargetAffiliation;
import xyz.fmdc.arw.common.blockentity.fcs.AbstractFcsCoreBlockEntity;

import java.util.UUID;
import java.util.function.Supplier;

public class ServerboundSetTargetAffiliationPacket {
    private final BlockPos corePos;
    private final UUID targetUuid;
    private final TargetAffiliation affiliation;

    public ServerboundSetTargetAffiliationPacket(BlockPos corePos, UUID targetUuid, TargetAffiliation affiliation) {
        this.corePos = corePos;
        this.targetUuid = targetUuid;
        this.affiliation = affiliation != null ? affiliation : TargetAffiliation.UNKNOWN;
    }

    public ServerboundSetTargetAffiliationPacket(FriendlyByteBuf buf) {
        this.corePos = buf.readBlockPos();
        this.targetUuid = buf.readUUID();
        this.affiliation = TargetAffiliation.fromOrdinal(buf.readByte());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.corePos);
        buf.writeUUID(this.targetUuid);
        buf.writeByte(this.affiliation.ordinal());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            Level level = player.level();
            if (level.isLoaded(this.corePos)) {
                BlockEntity be = level.getBlockEntity(this.corePos);
                if (be instanceof AbstractFcsCoreBlockEntity fcsCore) {
                    fcsCore.setTargetAffiliation(this.targetUuid, this.affiliation);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
