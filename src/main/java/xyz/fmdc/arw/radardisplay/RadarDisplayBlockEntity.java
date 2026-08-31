package xyz.fmdc.arw.radardisplay;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import xyz.fmdc.arw.common.blockentity.AbstractARWBlockEntity;
import xyz.fmdc.arw.registry.ModBlocks;

public class RadarDisplayBlockEntity extends AbstractARWBlockEntity {
    private int selectedRange = 50000;
    private String selectedTopMode = "Radar Mode";

    public RadarDisplayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.RADAR_DISPLAY.getBEType(), pos, state);
    }

    public int getSelectedRange() {
        return selectedRange;
    }

    public String getSelectedTopMode() {
        return selectedTopMode;
    }

    public void setConfig(int selectedRange, String selectedTopMode) {
        this.selectedRange = selectedRange;
        this.selectedTopMode = selectedTopMode;
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            BlockState state = getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, 3);
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("selectedRange", selectedRange);
        tag.putString("selectedTopMode", selectedTopMode);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        if (tag.contains("selectedRange")) {
            this.selectedRange = tag.getInt("selectedRange");
        }
        if (tag.contains("selectedTopMode")) {
            this.selectedTopMode = tag.getString("selectedTopMode");
        }
    }
}
