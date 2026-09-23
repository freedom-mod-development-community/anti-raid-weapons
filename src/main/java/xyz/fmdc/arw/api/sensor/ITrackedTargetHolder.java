package xyz.fmdc.arw.api.sensor;

import xyz.fmdc.arw.api.TrackedTarget;
import xyz.fmdc.arw.network.S2CSyncRadarTargetsPacket;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * レーダー追尾目標を保持・クライアント同期するBlockEntity共通のインターフェース
 */
public interface ITrackedTargetHolder {

    /**
     * 外部（FCS・GUI・通信等）から現在追尾中の全目標を取得する
     */
    Map<UUID, TrackedTarget> getTrackedTargets();

    /**
     * クライアント側でパケット受信時に追尾目標データを更新する
     */
    void updateClientTrackedTargets(List<S2CSyncRadarTargetsPacket.TargetData> dataList);
}
