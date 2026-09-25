package xyz.fmdc.arw.api.projectile;

import net.minecraft.world.phys.Vec3;

/**
 * 外弾道物理シミュレーションの対象となる飛翔体（砲弾・ミサイル等）のインターフェース。
 * 単位系はSI単位系（m, m/s, kg, N, s）に準拠します。
 */
public interface IBallisticProjectile {

    /**
     * 弾体直径（口径） [m] (例: 5インチ砲弾 = 0.127m)
     */
    double getDiameter();

    /**
     * 弾体全長 [m] (例: 5インチ砲弾 = 0.8m, SM-2ミサイル = 4.72m)
     */
    double getLength();

    /**
     * 弾体質量 [kg] (例: 5インチ砲弾 = 31.75kg, SM-2ミサイル = 708kg)
     */
    double getMass();

    /**
     * ゼロ迎角時における弾軸方向の基本抗力係数 (Cd0)
     */
    default double getDragCoefficientZero() {
        return 0.20;
    }

    /**
     * 側面（迎角90度時）の円柱横流抗力係数 (Cd_side)
     */
    default double getSideDragCoefficient() {
        return 1.15;
    }

    /**
     * 迎角に対する揚力傾斜係数 (Cl_alpha)
     */
    default double getLiftSlope() {
        return 1.8;
    }

    /**
     * ウェザーベーン効果（弾軸を進行方向へ追従・復元させる安定係数 [1/s]）
     */
    default double getStabilityFactor() {
        return 5.0;
    }

    /**
     * 現在の位置 [m] (Minecraftワールド座標)
     */
    Vec3 getPositionMeters();

    /**
     * 位置の設定 [m]
     */
    void setPositionMeters(Vec3 position);

    /**
     * 現在の速度ベクトル [m/s]
     */
    Vec3 getVelocityMetersPerSecond();

    /**
     * 速度ベクトルの設定 [m/s]
     */
    void setVelocityMetersPerSecond(Vec3 velocity);

    /**
     * 弾軸姿勢単位ベクトル（弾頭が向いている方向、長さ1.0）
     */
    Vec3 getOrientation();

    /**
     * 弾軸姿勢単位ベクトルの設定
     */
    void setOrientation(Vec3 orientation);

    /**
     * 現在の推力 [N] (ロケットモーター燃焼中など、砲弾は 0.0)
     */
    default double getThrustNewtons() {
        return 0.0;
    }
}
