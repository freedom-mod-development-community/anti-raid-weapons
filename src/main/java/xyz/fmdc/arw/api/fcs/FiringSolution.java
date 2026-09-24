package xyz.fmdc.arw.api.fcs;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import xyz.fmdc.arw.api.TrackedTarget;
import xyz.fmdc.arw.common.blockentity.vls.VlsBlockEntity;
import xyz.fmdc.arw.common.blockentity.weapon.ARWCIWSBlockEntity;
import xyz.fmdc.arw.common.blockentity.weapon.AbstractMissileLauncherBlockEntity;
import xyz.fmdc.arw.common.blockentity.weapon.AbstractSingleGunBlockEntity;

/**
 * 火器管制システム（FCS）における射撃諸元および弾道・会合計算データ
 *
 * 静止目標から超音速ミサイル（Mach 3〜5+）などの高速飛翔体までカバーし、
 * 砲弾（重力放物線弾道）およびミサイル（直進・誘導巡航）の両方に対応した
 * 数学的厳密解（2次会合方程式）および高精度反復法（Newton-Raphson法）による弾道ソルバーを内蔵。
 */
public record FiringSolution(
        float targetYaw,            // 指向方位角 (度: 0=北, 90=東, 180=南, 270=西)
        float targetPitch,          // 指向仰角 (度: 水平0, 見上げ+90, 見下ろし-90)
        boolean allowFire,          // 発射承認 (トリガー許可)
        boolean isTargetLocked,     // 諸元成立・目標ロック完了
        double timeOfFlightTicks,   // 弾丸/ミサイルの到達所要時間 (Ticks)
        Vec3 interceptPos,          // 未来の会合着弾予測座標
        double slantRange,          // 砲口から会合点までの直線距離 (m)
        boolean inRange,            // 兵装の最大射程内か
        boolean solutionValid       // 幾何学的に迎撃解が存在するか
) {
    public static final FiringSolution IDLE = new FiringSolution(
            0.0f, 0.0f, false, false, 0.0, Vec3.ZERO, 0.0, false, false
    );

    // 既存コードとの完全な後方互換用コンストラクタ（4引数）
    public FiringSolution(float targetYaw, float targetPitch, boolean allowFire, boolean isTargetLocked) {
        this(targetYaw, targetPitch, allowFire, isTargetLocked, 0.0, Vec3.ZERO, 0.0, false, isTargetLocked);
    }

    /**
     * 到達所要時間（秒）を取得
     */
    public double getTimeOfFlightSeconds() {
        return timeOfFlightTicks / 20.0;
    }

    /**
     * 発射体の弾道・推進特性種別
     */
    public enum TrajectoryType {
        BALLISTIC_SHELL, // 重力落差の影響を受ける放物線弾道（戦道砲弾、CIWS機銃弾など）
        CRUISE_MISSILE,  // 推力により重力を相殺して飛翔する直進・誘導体（ミサイル）
        DIRECT_ENERGY    // 即時着弾（ビーム・レーザー）
    }

    /**
     * 汎用射撃諸元ソルバー
     *
     * @param muzzlePos        発射位置（砲口またはランチャー位置）
     * @param targetPos        目標の現在座標
     * @param targetVelPerTick 目標の速度ベクトル（blocks/tick）
     * @param speedPerTick     発射体の初速/平均速度（blocks/tick）
     * @param gravityPerTick2  発射体の重力加速度（blocks/tick^2、通常砲弾は0.03、ミサイルは0.0）
     * @param trajectoryType   弾道タイプ
     * @param maxRange         兵装の最大有効射程 (m)
     * @param minPitchDeg      兵装の最小仰角リミット (度)
     * @param maxPitchDeg      兵装の最大仰角リミット (度)
     * @return 計算された射撃諸元
     */
    public static FiringSolution calculate(
            Vec3 muzzlePos,
            Vec3 targetPos,
            Vec3 targetVelPerTick,
            double speedPerTick,
            double gravityPerTick2,
            TrajectoryType trajectoryType,
            float maxRange,
            float minPitchDeg,
            float maxPitchDeg
    ) {
        if (speedPerTick <= 0.001) {
            return IDLE;
        }

        Vec3 D = targetPos.subtract(muzzlePos); // 発射位置から目標への相対ベクトル
        Vec3 V = (targetVelPerTick != null) ? targetVelPerTick : Vec3.ZERO;

        // 1. 直線会合方程式（2次方程式）による初期会合時間 t_0 の算出
        // |D + V * t|^2 = (speed * t)^2
        // a * t^2 + b * t + c = 0
        double a = V.dot(V) - speedPerTick * speedPerTick;
        double b = 2.0 * D.dot(V);
        double c = D.dot(D);

        double t = -1.0;

        if (Math.abs(a) < 1e-7) {
            // 目標速度と弾速が等しい特異ケース
            if (Math.abs(b) > 1e-7) {
                double tLinear = -c / b;
                if (tLinear > 0) t = tLinear;
            }
        } else {
            double discriminant = b * b - 4.0 * a * c;
            if (discriminant >= 0.0) {
                double sqrtDisc = Math.sqrt(discriminant);
                double t1 = (-b - sqrtDisc) / (2.0 * a);
                double t2 = (-b + sqrtDisc) / (2.0 * a);

                // 正の解で最も早い会合時間を採用
                if (t1 > 0.0 && t2 > 0.0) {
                    t = Math.min(t1, t2);
                } else if (t1 > 0.0) {
                    t = t1;
                } else if (t2 > 0.0) {
                    t = t2;
                }
            }
        }

        // 解が見つからない（目標が弾速より速く逃げ去っている等）場合
        if (t <= 0.0) {
            // 直視線方向での暫定諸元を出力（ロック不可）
            double dist = D.length();
            float fallbackYaw = (float) Math.toDegrees(Math.atan2(-D.x, D.z));
            float fallbackPitch = (float) -Math.toDegrees(Math.atan2(D.y, Math.sqrt(D.x * D.x + D.z * D.z)));
            return new FiringSolution(fallbackYaw, fallbackPitch, false, false, dist / speedPerTick, targetPos, dist, dist <= maxRange, false);
        }

        // 2. 弾道タイプごとの処理
        Vec3 aimPoint;
        Vec3 interceptPoint = targetPos.add(V.scale(t));

        if (trajectoryType == TrajectoryType.CRUISE_MISSILE || trajectoryType == TrajectoryType.DIRECT_ENERGY || gravityPerTick2 <= 1e-6) {
            // ミサイルまたは重力なし：上記で求めた2次方程式の解が厳密解
            aimPoint = interceptPoint;
        } else {
            // 砲弾（重力落下あり）：Newton-Raphson法で収束計算
            // F(t) = |D + V*t + 0.5*g*t^2| - speed*t = 0
            Vec3 gVec = new Vec3(0.0, 0.5 * gravityPerTick2, 0.0);

            for (int iter = 0; iter < 8; iter++) {
                Vec3 predAim = D.add(V.scale(t)).add(gVec.scale(t * t));
                double dist = predAim.length();
                double f = dist - speedPerTick * t;

                if (Math.abs(f) < 1e-4) {
                    break; // 十分な精度で収束
                }

                // 微分 F'(t) = (predAim dot (V + gVec*2*t)) / dist - speed
                Vec3 dPred = V.add(gVec.scale(2.0 * t));
                double fPrime = (dist > 1e-6 ? predAim.dot(dPred) / dist : 0.0) - speedPerTick;

                if (Math.abs(fPrime) < 1e-7) {
                    break;
                }

                double nextT = t - f / fPrime;
                if (nextT <= 0.0) break;
                t = nextT;
            }

            interceptPoint = targetPos.add(V.scale(t));
            aimPoint = interceptPoint.add(0.0, 0.5 * gravityPerTick2 * t * t, 0.0);
        }

        // 3. 照準ベクトルから射撃角度（Yaw / Pitch）を計算
        Vec3 aimDir = aimPoint.subtract(muzzlePos);
        double distH = Math.sqrt(aimDir.x * aimDir.x + aimDir.z * aimDir.z);
        double slantRange = aimDir.length();

        // Minecraft座標系: Yaw 0=北(Z-), 90=東(X+), 180=南(Z+), 270=西(X-)
        float targetYaw = (float) Math.toDegrees(Math.atan2(-aimDir.x, aimDir.z));
        // Pitch: 見上げがマイナス、見下ろしがプラス
        float targetPitch = (float) -Math.toDegrees(Math.atan2(aimDir.y, distH));

        // 4. エンベロープ（射程・射角制約）の判定
        boolean inRange = slantRange <= maxRange;
        boolean withinPitchLimits = (targetPitch >= minPitchDeg && targetPitch <= maxPitchDeg);
        boolean solutionValid = inRange && withinPitchLimits;

        return new FiringSolution(
                targetYaw,
                targetPitch,
                solutionValid, // 解が成立していれば発射許可
                solutionValid, // 目標ロック成立
                t,
                interceptPoint,
                slantRange,
                inRange,
                true
        );
    }

    /**
     * 兵装ブロックエンティティと目標情報から自動的に射撃諸元を計算する便利メソッド
     *
     * @param weaponEntity 兵装のBlockEntity
     * @param target       追尾目標データ
     * @return 射撃諸元
     */
    public static FiringSolution calculateForWeapon(BlockEntity weaponEntity, TrackedTarget target) {
        if (weaponEntity == null || target == null || target.getLastKnownPos() == null) {
            return IDLE;
        }

        BlockPos pos = weaponEntity.getBlockPos();
        Vec3 muzzlePos = Vec3.atCenterOf(pos);
        Vec3 targetPos = target.getLastKnownPos();
        Vec3 targetVel = (target.getLastKnownVelocity() != null) ? target.getLastKnownVelocity() : Vec3.ZERO;

        double speed = 40.0; // デフォルト初速: 40 blocks/tick (800m/s)
        double gravity = 0.03; // デフォルト重力: 0.03 blocks/tick^2
        TrajectoryType trajectory = TrajectoryType.BALLISTIC_SHELL;
        float maxRange = 1500.0f;
        float minPitch = -15.0f;
        float maxPitch = 85.0f;

        if (weaponEntity instanceof AbstractSingleGunBlockEntity gun) {
            muzzlePos = Vec3.atBottomCenterOf(pos).add(gun.getMuzzleOffset());
            speed = gun.getMuzzleVelocity();
            gravity = 0.03;
            trajectory = TrajectoryType.BALLISTIC_SHELL;
            maxRange = 1500.0f;
            minPitch = -15.0f;
            maxPitch = 85.0f;
        } else if (weaponEntity instanceof ARWCIWSBlockEntity) {
            speed = 55.0; // 1100m/s
            gravity = 0.015;
            trajectory = TrajectoryType.BALLISTIC_SHELL;
            maxRange = 400.0f;
            minPitch = -20.0f;
            maxPitch = 88.0f;
        } else if (weaponEntity instanceof AbstractMissileLauncherBlockEntity || weaponEntity instanceof VlsBlockEntity) {
            speed = 45.0; // 900m/s (Mach 2.6)
            gravity = 0.0;
            trajectory = TrajectoryType.CRUISE_MISSILE;
            maxRange = 3500.0f;
            minPitch = -5.0f;
            maxPitch = 90.0f;
        }

        return calculate(
                muzzlePos,
                targetPos,
                targetVel,
                speed,
                gravity,
                trajectory,
                maxRange,
                minPitch,
                maxPitch
        );
    }
}
