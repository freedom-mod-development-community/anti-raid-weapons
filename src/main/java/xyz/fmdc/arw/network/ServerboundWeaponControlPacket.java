package xyz.fmdc.arw.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import xyz.fmdc.arw.api.control.IDirectMannedWeapon;
import xyz.fmdc.arw.api.control.IRemoteControllableWeapon;

import java.util.function.Supplier;

public class ServerboundWeaponControlPacket {

    private final BlockPos targetPos;
    private final float yawInput;
    private final float pitchInput;
    private final boolean triggerFire;

    public ServerboundWeaponControlPacket(BlockPos targetPos, float yawInput, float pitchInput, boolean triggerFire) {
        this.targetPos = targetPos;
        this.yawInput = yawInput;
        this.pitchInput = pitchInput;
        this.triggerFire = triggerFire;
    }

    // デコード
    public ServerboundWeaponControlPacket(FriendlyByteBuf buf) {
        this.targetPos = buf.readBlockPos();
        this.yawInput = buf.readFloat();
        this.pitchInput = buf.readFloat();
        this.triggerFire = buf.readBoolean();
    }

    // エンコード
    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.targetPos);
        buf.writeFloat(this.yawInput);
        buf.writeFloat(this.pitchInput);
        buf.writeBoolean(this.triggerFire);
    }

    // 1.20.1 用ハンドラー
    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            BlockEntity be = player.level().getBlockEntity(this.targetPos);

            if (be instanceof IRemoteControllableWeapon remoteWeapon) {
                remoteWeapon.handleRemoteInput(this.yawInput, this.pitchInput, this.triggerFire);
            } else if (be instanceof IDirectMannedWeapon mannedWeapon) {
                mannedWeapon.handleMannedInput(this.yawInput, this.pitchInput, this.triggerFire);
            }
        });
        ctx.setPacketHandled(true);
    }
}
