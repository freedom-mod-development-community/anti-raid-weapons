package xyz.fmdc.arw.api.fcs;

import java.util.List;

/**
 * FCSネットワークに目標データ（TargetTrack）を提供するセンサー用インターフェース
 */
public interface IFcsSensorNode extends IFcsNetworkNode {
    List<TargetTrack> getDetectedTargets();
    TargetTrack getPrimaryLockedTarget();
}