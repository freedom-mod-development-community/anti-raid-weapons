package xyz.fmdc.arw.api.fcs;

import net.minecraft.world.entity.Entity;
import xyz.fmdc.arw.api.sensor.RadarScanRange;

import java.util.List;

/**
 * FCSネットワークに目標データ（TargetTrack）を提供するセンサー用インターフェース
 */
public interface IFcsSensorNode extends IFcsNetworkNode {
    List<TargetTrack> getDetectedTargets();
    TargetTrack getPrimaryLockedTarget();

    /**
     * センサーの探索範囲・視野角パラメータを取得
     */
    default RadarScanRange getScanRange() {
        return RadarScanRange.DEFAULT;
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
     * FCSコア等から一次スクリーニングされた候補エンティティを受け取り、
     * センサー自身の向きや視野角（ビームFOV）で二次フィルタリングして追尾目標を更新する
     *
     * @param candidates 一次スクリーニングされたターゲット候補エンティティ
     */
    default void filterAndScan(List<Entity> candidates) {}
}
