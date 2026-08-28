package xyz.fmdc.arw.common.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import xyz.fmdc.arw.api.fcs.FiringSolution;
import xyz.fmdc.arw.api.fcs.IFcsControllableWeapon;

import java.util.UUID;

//FCS対応近代兵装の基底.
public abstract class IntegratedWeaponBlockEntity extends AbstractWeaponBlockEntity implements IFcsControllableWeapon {

    protected UUID networkId = UUID.randomUUID();
    protected boolean isFcsConnected = false;

    public IntegratedWeaponBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public UUID getNetworkId() {
        return this.networkId;
    }

    @Override
    public boolean isConnectedToFcs() {
        return this.isFcsConnected;
    }

    @Override
    public void setFcsConnected(boolean connected) {
        this.isFcsConnected = connected;
    }

    @Override
    public void applyFiringSolution(FiringSolution solution) {
        if (solution == null) return;
        setTargetYaw(solution.targetYaw());
        setTargetPitch(solution.targetPitch());

        if (solution.allowFire() && canFire()) {
            fire();
        }
    }

    protected abstract boolean canFire();

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putUUID("NetworkId", this.networkId);
        tag.putBoolean("FcsConnected", this.isFcsConnected);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID("NetworkId")) this.networkId = tag.getUUID("NetworkId");
        this.isFcsConnected = tag.getBoolean("FcsConnected");
    }
}
