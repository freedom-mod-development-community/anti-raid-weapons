package xyz.fmdc.arw.api.fcs;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface IFcsNetworkNode {
    UUID getNetworkId();
    boolean isConnectedToFcs();
    void setFcsConnected(boolean connected);

    @Nullable
    default UUID getLinkedFcsCoreUuid() { return null; }
    default void setLinkedFcsCoreUuid(@Nullable UUID uuid) {}

    @Nullable
    default BlockPos getLinkedFcsCorePos() { return null; }
    default void setLinkedFcsCorePos(@Nullable BlockPos pos) {}
}
