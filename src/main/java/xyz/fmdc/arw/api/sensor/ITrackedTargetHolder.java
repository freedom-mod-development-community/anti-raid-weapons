package xyz.fmdc.arw.api.sensor;

import xyz.fmdc.arw.api.TrackedTarget;
import xyz.fmdc.arw.network.S2CSyncRadarTargetsPacket;

import java.util.Collections;
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
    default Map<UUID, TrackedTarget> getTrackedTargets() {
        return Collections.emptyMap();
    }

    /**
     * クライアント側でパケット受信時に追尾目標データを更新する
     */
    default void updateClientTrackedTargets(List<S2CSyncRadarTargetsPacket.TargetData> dataList) {}

    /**
     * センサー自身の探索範囲・視野角パラメータを取得
     */
    default RadarScanRange getScanRange() {
        return RadarScanRange.DEFAULT;
    }
}
