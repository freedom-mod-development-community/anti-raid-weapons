package xyz.fmdc.arw.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class S2CSyncRadarTargetsPacket {
    private final BlockPos pos;
    private final List<TargetData> targets;

    public record TargetData(UUID uuid, String name, Vec3 pos, Vec3 vel) {}

    public S2CSyncRadarTargetsPacket(BlockPos pos, List<TargetData> targets) {
        this.pos = pos;
        this.targets = targets;
    }

    public S2CSyncRadarTargetsPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        int size = buf.readVarInt();
        this.targets = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            UUID uuid = buf.readUUID();
            String name = buf.readUtf();
            Vec3 targetPos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
            Vec3 targetVel = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
            this.targets.add(new TargetData(uuid, name, targetPos, targetVel));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeVarInt(this.targets.size());
        for (TargetData data : this.targets) {
            buf.writeUUID(data.uuid());
            buf.writeUtf(data.name());
            buf.writeDouble(data.pos().x);
            buf.writeDouble(data.pos().y);
            buf.writeDouble(data.pos().z);
            buf.writeDouble(data.vel().x);
            buf.writeDouble(data.vel().y);
            buf.writeDouble(data.vel().z);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        //mainスレッドに戻してclientのBEが保持するデータを更新するハンドラへ渡す.
        ctx.get().enqueueWork(() -> {
            ClientRadarDataHandler.handleTargetSync(this.pos, this.targets);
        });
        ctx.get().setPacketHandled(true);
    }
}
