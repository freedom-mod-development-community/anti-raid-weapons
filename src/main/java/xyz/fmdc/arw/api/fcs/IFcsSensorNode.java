package xyz.fmdc.arw.api.fcs;

import net.minecraft.world.entity.Entity;
import xyz.fmdc.arw.api.sensor.RadarScanRange;

import java.util.Collections;
import java.util.List;

/**
 * FCSネットワークに目標データ（TargetTrack）を提供するセンサー用インターフェース
 */
public interface IFcsSensorNode extends IFcsNetworkNode {

    default List<TargetTrack> getDetectedTargets() {
        return Collections.emptyList();
    }

    default TargetTrack getPrimaryLockedTarget() {
        return null;
    }

    /**
     * センサーの探索範囲・視野角パラメータを取得
     */
    default RadarScanRange getScanRange() {
        return RadarScanRange.DEFAULT;
    }

    /**
     * センサーアンテナの現在のワールド絶対方位角（Yaw: 度）を取得
     */
    default float getAntennaYaw() {
        return 0.0f;
    }

    /**
     * センサーアンテナの現在の仰角（Pitch: 度）を取得
     */
    default float getAntennaPitch() {
        return 0.0f;
    }

    /**
     * センサーの電源がONかどうか
     */
    default boolean isPowered() {
        return true;
    }

    /**
     * センサーの電源状態を設定
     */
    default void setPowered(boolean powered) {}

    /**
     * @deprecated FCSコアによる統合判定アーキテクチャへの移行に伴い非推奨
     */
    @Deprecated
    default void filterAndScan(List<Entity> candidates) {}
}
