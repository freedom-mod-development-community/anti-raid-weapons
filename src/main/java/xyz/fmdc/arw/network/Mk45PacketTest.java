package xyz.fmdc.arw.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import xyz.fmdc.arw.common.blockentity.AbstractSingleGunBlockEntity; // ※適切なインターフェースまたはBlockEntityクラスを指定

import java.util.function.Supplier;

public class Mk45PacketTest {

    private final BlockPos pos;
    private final float targetYaw;
    private final float targetPitch;
    private final boolean fire;

    //fire
    public Mk45PacketTest(BlockPos pos, boolean fire){
        this(pos, Float.NaN, Float.NaN, true);
    }

    //角度だけ
    public Mk45PacketTest(BlockPos pos, float targetYaw, float targetPitch){
        this(pos,targetYaw, targetPitch, false);
    }

    /**
     * フル引数
     */
    public Mk45PacketTest(BlockPos pos, float targetYaw, float targetPitch, boolean fire){
        this.pos = pos;
        this.targetYaw = targetYaw;
        this.targetPitch = targetPitch;
        this.fire = fire;
    }

    public Mk45PacketTest(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.targetYaw = buf.readFloat();
        this.targetPitch = buf.readFloat();
        this.fire = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeFloat(this.targetYaw);
        buf.writeFloat(this.targetPitch);
        buf.writeBoolean(this.fire);
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            // プレイヤーのいるワールドから対象座標の BlockEntity を取得
            BlockEntity be = player.level().getBlockEntity(this.pos);

            // 角度を設定する処理を実行（使用しているクラスやインターフェースに合わせて調整してください）
            if (be instanceof AbstractSingleGunBlockEntity gun) {
                if(!Float.isNaN(this.targetYaw) && !Float.isNaN(this.targetPitch)){
                    gun.setTargetYaw(this.targetYaw);
                    gun.setTargetPitch(this.targetPitch);
                }
                if(this.fire)gun.fire();
                be.setChanged(); // 保存フラグを立てる
            }
        });
        ctx.setPacketHandled(true);
    }
}