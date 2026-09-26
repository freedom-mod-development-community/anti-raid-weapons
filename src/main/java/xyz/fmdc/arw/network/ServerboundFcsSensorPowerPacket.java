package xyz.fmdc.arw.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import xyz.fmdc.arw.common.blockentity.fcs.AbstractFcsCoreBlockEntity;

import java.util.UUID;
import java.util.function.Supplier;

public class ServerboundFcsSensorPowerPacket {
    private final BlockPos corePos;
    private final UUID sensorUuid;
    private final boolean power;

    public ServerboundFcsSensorPowerPacket(BlockPos corePos, UUID sensorUuid, boolean power) {
        this.corePos = corePos;
        this.sensorUuid = sensorUuid;
        this.power = power;
    }

    public ServerboundFcsSensorPowerPacket(FriendlyByteBuf buf) {
        this.corePos = buf.readBlockPos();
        this.sensorUuid = buf.readUUID();
        this.power = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.corePos);
        buf.writeUUID(this.sensorUuid);
        buf.writeBoolean(this.power);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null && player.level().isLoaded(corePos)) {
                BlockEntity be = player.level().getBlockEntity(corePos);
                if (be instanceof AbstractFcsCoreBlockEntity fcsCore) {
                    fcsCore.setSensorPower(sensorUuid, power);
                }
            }
        });
        return true;
    }
}
