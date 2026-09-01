package xyz.fmdc.arw.common.blockentity.console;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.fmdc.arw.api.fcs.IFcsControllableWeapon;
import xyz.fmdc.arw.api.fcs.IFcsNetworkNode;
import xyz.fmdc.arw.common.blockentity.AbstractARWBlockEntity;
import xyz.fmdc.arw.common.blockentity.AbstractMissileLauncherBlockEntity;
import xyz.fmdc.arw.common.blockentity.fcs.AbstractFcsCoreBlockEntity;
import xyz.fmdc.arw.registry.ModBlocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 動作確認用コンソールの BlockEntity.
 * FCSネットワークに接続し、リンクされた兵装（ミサイルランチャー等）の確認・制御を行う基盤となります。
 */
public class TestConsoleBlockEntity extends AbstractARWBlockEntity implements IFcsNetworkNode {

    private boolean fcsConnected = false;

    public TestConsoleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.TEST_CONSOLE.getBEType(), pos, state);
    }

    @Override
    public UUID getNetworkId() {
        return this.uuid;
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

    /**
     * リンク先の FCS Core を取得します（ロードされている場合）。
     */
    @Nullable
    public AbstractFcsCoreBlockEntity getLinkedFcsCore() {
        if (this.level != null && this.linkedFcsCorePos != null && this.level.isLoaded(this.linkedFcsCorePos)) {
            BlockEntity be = this.level.getBlockEntity(this.linkedFcsCorePos);
            if (be instanceof AbstractFcsCoreBlockEntity fcsCore) {
                if (this.linkedFcsCoreUuid == null || this.linkedFcsCoreUuid.equals(fcsCore.getUuid())) {
                    return fcsCore;
                }
            }
        }
        return null;
    }

    /**
     * リンクされた FCS Core に接続されているすべての兵装を取得します。
     */
    public List<IFcsControllableWeapon> getConnectedWeapons() {
        AbstractFcsCoreBlockEntity core = getLinkedFcsCore();
        if (core != null) {
            return core.getConnectedWeapons();
        }
        return Collections.emptyList();
    }

    /**
     * リンクされた FCS Core に接続されているミサイルランチャー（AbstractMissileLauncherBlockEntity）を取得します。
     */
    public List<AbstractMissileLauncherBlockEntity> getConnectedMissileLaunchers() {
        List<AbstractMissileLauncherBlockEntity> launchers = new ArrayList<>();
        for (IFcsControllableWeapon weapon : getConnectedWeapons()) {
            if (weapon instanceof AbstractMissileLauncherBlockEntity launcher) {
                launchers.add(launcher);
            }
        }
        return launchers;
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
            if (tag.contains("FcsConnected")) {
                this.fcsConnected = tag.getBoolean("FcsConnected");
            }
        }
    }
}
