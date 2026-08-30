package xyz.fmdc.arw.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import xyz.fmdc.arw.radardisplay.RadarDisplayBlockEntity;

import java.util.function.Supplier;

public class UpdateRadarDisplayConfigPacket {
    private final BlockPos pos;
    private final int selectedRange;
    private final String selectedTopMode;

    public UpdateRadarDisplayConfigPacket(BlockPos pos, int selectedRange, String selectedTopMode) {
        this.pos = pos;
        this.selectedRange = selectedRange;
        this.selectedTopMode = selectedTopMode;
    }

    public UpdateRadarDisplayConfigPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.selectedRange = buf.readInt();
        this.selectedTopMode = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeInt(this.selectedRange);
        buf.writeUtf(this.selectedTopMode);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null && player.level().isLoaded(pos)) {
                BlockEntity be = player.level().getBlockEntity(pos);
                if (be instanceof RadarDisplayBlockEntity radarBE) {
                    radarBE.setConfig(selectedRange, selectedTopMode);
                }
            }
        });
        return true;
    }
}
