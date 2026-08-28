package xyz.fmdc.arw.api.fcs;

public interface IFcsControllableWeapon extends IFcsNetworkNode {
    void applyFiringSolution(FiringSolution solution);
}
