package xyz.fmdc.arw.common.blockentity.console;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.fmdc.arw.api.blockentity.IDirectionalBlockEntity;
import xyz.fmdc.arw.api.fcs.IFcsNetworkNode;
import xyz.fmdc.arw.client.gui.EmptyMenu;
import xyz.fmdc.arw.common.blockentity.AbstractARWBlockEntity;
import xyz.fmdc.arw.registry.ModBlocks;

import java.util.UUID;

public class Uyq21BlockEntity extends AbstractARWBlockEntity
        implements IDirectionalBlockEntity, IFcsNetworkNode, MenuProvider {

    public Uyq21BlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.UYQ21.getBEType(), pos, state);
    }
    private boolean fcsConnected = false;

    @Override
    public Direction getFacing(){
        return this.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public AABB getRenderBoundingBox() {
        Vec3 center = Vec3.atCenterOf(this.worldPosition);
        return AABB.ofSize(center, 1, 2, 1);
    }

    //IFCSNetowrkNode
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

    //Menu関係
    @Override
    public @NotNull Component getDisplayName() {
        return Component.literal("AN/UYQ-21");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new EmptyMenu(id, playerInventory, this);
    }
}
