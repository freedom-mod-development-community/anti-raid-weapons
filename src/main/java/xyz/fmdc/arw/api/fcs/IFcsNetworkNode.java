package xyz.fmdc.arw.api.fcs;

import java.util.UUID;

public interface IFcsNetworkNode {
    UUID getNetworkId();
    boolean isConnectedToFcs();
    void setFcsConnected(boolean connected);
}