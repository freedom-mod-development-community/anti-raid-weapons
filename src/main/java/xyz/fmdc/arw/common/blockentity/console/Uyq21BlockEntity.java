package xyz.fmdc.arw.common.blockentity.console;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
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
import xyz.fmdc.arw.common.sensor.RadarTargetTracker;
import xyz.fmdc.arw.network.S2CSyncRadarTargetsPacket;
import xyz.fmdc.arw.registry.ModBlocks;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Uyq21BlockEntity extends AbstractARWBlockEntity
        implements IDirectionalBlockEntity, IFcsNetworkNode, MenuProvider, ITrackedTargetHolder {

    // レーダー探知・追尾管理コンポーネント（内部探索はEntityIDを使用、外部公開はUUIDを使用）
    private final RadarTargetTracker radarTracker = new RadarTargetTracker();

    // 探知範囲 (ブロック)
    private static final float SCAN_RANGE = 512.0f;

    private boolean fcsConnected = false;

    public Uyq21BlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.UYQ21.getBEType(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, Uyq21BlockEntity be) {
        if (!level.isClientSide) {
            be.serverTick();
        }
    }

    public void serverTick() {
        if (this.level == null) return;

        Vec3 centerPos = Vec3.atCenterOf(this.worldPosition);

        // 内部探索（EntityIDベース）による周囲目標の走査
        List<Entity> detected = this.radarTracker.scanEntities(this.level, centerPos, SCAN_RANGE);

        // 追尾マップの更新（タイムアウト処理を含む）
        this.radarTracker.updateTrackedTargets(this.level, detected);

        // クライアント同期パケットの送信
        this.radarTracker.syncToClients(this.level, this.worldPosition);
    }

    public RadarTargetTracker getRadarTracker() {
        return this.radarTracker;
    }

    @Override
    public Map<UUID, TrackedTarget> getTrackedTargets() {
        return this.radarTracker.getTrackedTargets();
    }

    @Override
    public void updateClientTrackedTargets(List<S2CSyncRadarTargetsPacket.TargetData> dataList) {
        this.radarTracker.updateClientTrackedTargets(this.level, dataList);
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
