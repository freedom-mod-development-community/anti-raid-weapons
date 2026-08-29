package xyz.fmdc.arw.common.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import xyz.fmdc.arw.api.fcs.FiringSolution;
import xyz.fmdc.arw.api.fcs.IFcsControllableWeapon;
import xyz.fmdc.arw.client.renderer.GenericGlbRenderer;
import xyz.fmdc.arw.client.util.IYawPitchAnimatableModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

//FCS対応近代兵装の基底.
public abstract class AbstractGunBlockEntity extends AbstractWeaponBlockEntity implements IYawPitchAnimatableModel,IFcsControllableWeapon {

    protected UUID networkId = UUID.randomUUID();
    protected boolean isFcsConnected = false;

    public AbstractGunBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public float getRenderTargetYaw(float partialTick) {
        return Mth.rotLerp(partialTick, prevYaw, currentYaw);
    }

    @Override
    public float getRenderTargetPitch(float partialTick) {
        return Mth.rotLerp(partialTick, prevPitch, currentPitch);
    }

    @Override
    public List<GenericGlbRenderer.ActiveAnimation> getActiveAnimations(float partialTick) {
        List<GenericGlbRenderer.ActiveAnimation> list = new ArrayList<>();
        if (this.level == null) return list;

        long currentGameTime = this.level.getGameTime();
        for (Map.Entry<String, Long> entry : this.runningAnimations.entrySet()) {
            String name = entry.getKey();
            long startTime = entry.getValue();
            float elapsedTicks = (float) (currentGameTime - startTime) + partialTick;
            float elapsedSeconds = Math.max(0.0f, elapsedTicks / 20.0f);
            list.add(new GenericGlbRenderer.ActiveAnimation(name, elapsedSeconds));
        }
        return list;
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
