package xyz.fmdc.arw.common.blockentity.console;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.fmdc.arw.api.TrackedTarget;
import xyz.fmdc.arw.api.blockentity.IDirectionalBlockEntity;
import xyz.fmdc.arw.api.fcs.IFcsNetworkNode;
import xyz.fmdc.arw.api.sensor.ITrackedTargetHolder;
import xyz.fmdc.arw.client.gui.EmptyMenu;
import xyz.fmdc.arw.common.blockentity.AbstractARWBlockEntity;
import xyz.fmdc.arw.common.blockentity.fcs.AbstractFcsCoreBlockEntity;
import xyz.fmdc.arw.registry.ModBlocks;

import java.util.*;

/**
 * AN/UYQ-21 射撃指揮コンソールのBlockEntity。
 * 自身ではターゲットリストを直接重複保持せず、リンクされたFCSコアへ問い合わせるステートレスなプロキシとして動作する。
 */
public class Uyq21BlockEntity extends AbstractARWBlockEntity
        implements IDirectionalBlockEntity, IFcsNetworkNode, MenuProvider, ITrackedTargetHolder {

    private boolean fcsConnected = false;

    public Uyq21BlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.UYQ21.getBEType(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, Uyq21BlockEntity be) {
        // FCSコアによる一元管理アーキテクチャのため、コンソールBEでの独自Tick走査・同期パケット送信は不要
    }

    /**
     * リンク先の FCS Core を取得（ロードされている場合）
     */
    @Nullable
    public AbstractFcsCoreBlockEntity getLinkedFcsCore() {
        if (this.level != null && this.linkedFcsCorePos != null && this.level.isLoaded(this.linkedFcsCorePos)) {
            BlockEntity be = this.level.getBlockEntity(this.linkedFcsCorePos);
            if (be instanceof AbstractFcsCoreBlockEntity fcsCore) {
                if (this.linkedFcsCoreUuid == null || this.linkedFcsCoreUuid.equals(fcsCore.getUuid()) || this.level.isClientSide) {
                    return fcsCore;
                }
            }
        }
        return null;
    }

    @Override
    public Map<UUID, TrackedTarget> getTrackedTargets() {
        AbstractFcsCoreBlockEntity core = getLinkedFcsCore();
        if (core != null) {
            return core.getTrackedTargets();
        }
        return Collections.emptyMap();
    }

    @Override
    public Direction getFacing() {
        return this.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public AABB getRenderBoundingBox() {
        Vec3 center = Vec3.atCenterOf(this.worldPosition);
        return AABB.ofSize(center, 1, 2, 1);
    }

    // IFcsNetworkNode
    @Override
    public UUID getNetworkId() {
        return this.getUuid();
    }

    @Override
    public boolean isConnectedToFcs() {
        return this.fcsConnected;
    }

    @Override
    public void setFcsConnected(boolean connected) {
        this.fcsConnected = connected;
        syncToClient();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("FcsConnected", this.fcsConnected);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        if (tag.contains("FcsConnected")) {
            this.fcsConnected = tag.getBoolean("FcsConnected");
        }
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt) {
        super.onDataPacket(net, pkt);
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            load(tag);
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        if (tag != null) {
            load(tag);
        }
    }

    // Menu関係
    @Override
    public @NotNull Component getDisplayName() {
        return Component.literal("AN/UYQ-21");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new EmptyMenu(id, playerInventory, this);
    }
}
