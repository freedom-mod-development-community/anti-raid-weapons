package xyz.fmdc.arw.common.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.fmdc.arw.common.blockentity.fcs.AbstractFcsCoreBlockEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 本modにおける Block entity 最上位クラス.
 */
public abstract class AbstractARWBlockEntity extends BlockEntity{

    protected UUID uuid = UUID.randomUUID();
    protected UUID linkedFcsCoreUuid = null;
    protected BlockPos linkedFcsCorePos = null;
    protected final Map<String, Long> runningAnimations = new HashMap<>();
    protected final Map<String, Float> animationDurations = new HashMap<>();

    public AbstractARWBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    @Nullable
    public UUID getLinkedFcsCoreUuid() {
        return this.linkedFcsCoreUuid;
    }

    public void setLinkedFcsCoreUuid(@Nullable UUID uuid) {
        this.linkedFcsCoreUuid = uuid;
    }

    @Nullable
    public BlockPos getLinkedFcsCorePos() {
        return this.linkedFcsCorePos;
    }

    public void setLinkedFcsCorePos(@Nullable BlockPos pos) {
        this.linkedFcsCorePos = pos;
    }

    public void syncToClient() {
        setChanged();
        if (this.level != null) {
            BlockState state = getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, 3);
        }
    }

    @Override
    public void setRemoved() {
        if (this.linkedFcsCorePos != null && this.linkedFcsCoreUuid != null && this.level != null && !this.level.isClientSide) {
            if (this.level.isLoaded(this.linkedFcsCorePos)) {
                BlockEntity coreBE = this.level.getBlockEntity(this.linkedFcsCorePos);
                if (coreBE instanceof AbstractFcsCoreBlockEntity fcsCore && fcsCore.getUuid().equals(this.linkedFcsCoreUuid)) {
                    fcsCore.unregisterDevice(this.uuid);
                }
            }
        }
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putUUID("UUID", this.uuid);
        if (this.linkedFcsCoreUuid != null) {
            tag.putUUID("LinkedFcsCoreUuid", this.linkedFcsCoreUuid);
        }
        if (this.linkedFcsCorePos != null) {
            tag.put("LinkedFcsCorePos", NbtUtils.writeBlockPos(this.linkedFcsCorePos));
        }
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID("UUID")) {
            this.uuid = tag.getUUID("UUID");
        } else if (tag.hasUUID("NetworkId")) {
            this.uuid = tag.getUUID("NetworkId");
        }

        if (tag.hasUUID("LinkedFcsCoreUuid")) {
            this.linkedFcsCoreUuid = tag.getUUID("LinkedFcsCoreUuid");
        } else {
            this.linkedFcsCoreUuid = null;
        }

        if (tag.contains("LinkedFcsCorePos")) {
            this.linkedFcsCorePos = NbtUtils.readBlockPos(tag.getCompound("LinkedFcsCorePos"));
        } else {
            this.linkedFcsCorePos = null;
        }
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
