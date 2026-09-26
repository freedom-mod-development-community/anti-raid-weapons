package xyz.fmdc.arw.api.projectile;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 飛翔体（砲弾・ミサイル等）の物理挙動を計算するステートレスな外弾道物理計算エンジン.
 * <p>
 * マイクラ特有の圧縮・縮小スケールではなく、現実世界（1 block = 1 m）と等身大の1:1物理スケールを採用。
 * 高度による国際標準大気（ISA）モデル、実地球スケールハイト（H ≈ 8500m）、音速・マッハ数に応じた超音速造波抗力、
 * 弾体姿勢と進行方向のズレ（迎角）に応じた円柱投影面積および抗力・揚力、
 * ウェザーベーン効果（進行方向への弾軸姿勢復元）、ロケット推力、地球標準重力（-9.80665 m/s^2）を統合して計算します。
 */
public final class BallisticsEngine {

    /** Minecraft標準の海面高度（基準空気密度点） */
    public static final double SEA_LEVEL_Y = 63.0;

    /** 海面基準空気密度 \rho_0 [kg/m^3] (国際標準大気 ISA: 15℃, 1013.25hPa) */
    public static final double RHO_SEA_LEVEL = 1.225;

    /**
     * 実地球の標準大気スケールハイト H ≈ 8500.0 [m].
     * マイクラ空間を縮小するのではなく、現実世界の1:1等身大スケールに基づいた指数関数的減衰を計算します。
     */
    public static final double SCALE_HEIGHT = 8500.0;

    /** 重力加速度ベクトル [m/s^2] (Minecraft 1 block = 1 m, 地球標準重力 9.80665 m/s^2) */
    public static final Vec3 GRAVITY = new Vec3(0, -9.80665, 0);

    /** 海面基準音速 a_0 [m/s] (ISA標準大気: 15℃, 288.15K) */
    public static final double SPEED_OF_SOUND_SEA_LEVEL = 340.29;

    private BallisticsEngine() {}

    /**
     * 高度 y [m] における空気密度 \rho(y) [kg/m^3] を算出します。
     * 国際標準大気（ISA）指数関数減衰モデル: \rho(y) = \rho_0 * \exp(- (y - y_0) / H)
     *
     * @param altitudeY MinecraftのY座標 [m]
     * @return 空気密度 [kg/m^3]
     */
    public static double calculateAirDensity(double altitudeY) {
        double relativeAltitude = Math.max(0.0, altitudeY - SEA_LEVEL_Y);
        return RHO_SEA_LEVEL * Math.exp(-relativeAltitude / SCALE_HEIGHT);
    }

    /**
     * 高度 y [m] における音速 a(y) [m/s] を算出します。
     * 国際標準大気（ISA）の対流圏気温減率 L = 0.0065 K/m に準拠。
     *
     * @param altitudeY MinecraftのY座標 [m]
     * @return 音速 [m/s]
     */
    public static double calculateSpeedOfSound(double altitudeY) {
        double relativeAltitude = Math.max(0.0, altitudeY - SEA_LEVEL_Y);
        // 対流圏気温 T(h) = T0 - L * h （圏界面 11,000m / 216.65K で下限クランプ）
        double tempK = Math.max(216.65, 288.15 - 0.0065 * relativeAltitude);
        // 音速 a = sqrt(\gamma * R * T) ≈ 20.0468 * sqrt(T)
        return 20.0468 * Math.sqrt(tempK);
    }

    /**
     * マッハ数 M に応じた外弾道学的な波抗力（造波抵抗）倍率を算出します。
     * 亜音速から遷音速（音速の壁）、超音速域に至る実測抗力曲線（G1/G7弾道モデル準拠）を再現。
     *
     * @param mach マッハ数 (v / a)
     * @return 抗力係数倍率 (1.0以上)
     */
    public static double calculateMachDragMultiplier(double mach) {
        if (mach < 0.8) {
            return 1.0;
        } else if (mach < 1.05) {
            // 遷音速急増域 (0.8 -> 1.05 で 1.0 -> 2.25 へ滑らかに上昇)
            double t = (mach - 0.8) / 0.25;
            double smooth = Math.sin(t * (Math.PI * 0.5));
            return 1.0 + 1.25 * (smooth * smooth);
        } else if (mach < 1.4) {
            // 超音速直後ピーク減衰 (1.05 -> 1.4 で 2.25 -> 1.70 へ下降)
            double t = (mach - 1.05) / 0.35;
            return 2.25 - 0.55 * t;
        } else {
            // 高超音速漸近域 (マッハ数増加に伴い 1.0 に漸近)
            return 1.0 + 1.1 / Math.sqrt(mach * mach - 0.75);
        }
    }

    /**
     * 外弾道計算の入力パラメータ（不変値）
     */
    public record BallisticsParams(
            double diameter,
            double length,
            double mass,
            double cd0,
            double cdSide,
            double clAlpha,
            double stabilityFactor,
            double thrust
    ) {
        public static BallisticsParams from(IBallisticProjectile projectile) {
            return new BallisticsParams(
                    projectile.getDiameter(),
                    projectile.getLength(),
                    projectile.getMass(),
                    projectile.getDragCoefficientZero(),
                    projectile.getSideDragCoefficient(),
                    projectile.getLiftSlope(),
                    projectile.getStabilityFactor(),
                    projectile.getThrustNewtons()
            );
        }
    }

    /**
     * 飛翔体の物理状態（不変値）
     */
    public record BallisticsState(
            Vec3 position,
            Vec3 velocity,
            Vec3 orientation
    ) {}

    /**
     * 1ステップ（dt）の計算結果詳細（テレメトリ・解析用）
     */
    public record StepResult(
            BallisticsState state,
            double dragNewtons,
            double liftNewtons,
            double aoaDegrees,
            double airDensity,
            double thrustNewtons,
            double machNumber,
            double speedOfSound
    ) {
        // 後方互換用コンストラクタ（6引数）
        public StepResult(
                BallisticsState state,
                double dragNewtons,
                double liftNewtons,
                double aoaDegrees,
                double airDensity,
                double thrustNewtons
        ) {
            this(state, dragNewtons, liftNewtons, aoaDegrees, airDensity, thrustNewtons, 0.0, SPEED_OF_SOUND_SEA_LEVEL);
        }
    }

    /**
     * 状態とパラメータ、時間ステップ dt から次の物理状態および詳細な空力結果を計算します。
     *
     * @param state  現在の物理状態
     * @param params 空力・物理パラメータ
     * @param dt     時間ステップ [s]
     * @return 計算結果レコード {@link StepResult}
     */
    public static StepResult stepResult(BallisticsState state, BallisticsParams params, double dt) {
        Vec3 pos = state.position();
        Vec3 vel = state.velocity();
        Vec3 u = state.orientation().lengthSqr() > 1.0E-6 ? state.orientation().normalize() : new Vec3(0, 0, 1);

        double speed = vel.length();
        double rho = calculateAirDensity(pos.y);
        double soundSpeed = calculateSpeedOfSound(pos.y);
        double mach = (soundSpeed > 1.0E-4) ? (speed / soundSpeed) : 0.0;

        // 投影面積の計算 (円柱モデル)
        double radius = params.diameter() * 0.5;
        double aFront = Math.PI * radius * radius;
        double aSide = params.diameter() * params.length();

        Vec3 fDrag = Vec3.ZERO;
        Vec3 fLift = Vec3.ZERO;
        double aoaDegrees = 0.0;
        double dragNewtons = 0.0;
        double liftNewtons = 0.0;

        if (speed > 1.0E-4) {
            Vec3 vHat = vel.scale(1.0 / speed);

            // 迎角 alpha の余弦・正弦
            double cosAlpha = Mth.clamp(u.dot(vHat), -1.0, 1.0);
            double sinAlpha = Math.sqrt(Math.max(0.0, 1.0 - cosAlpha * cosAlpha));
            aoaDegrees = Math.toDegrees(Math.acos(cosAlpha));

            // 実効投影面積 A_eff(alpha)
            double aEff = aFront * Math.abs(cosAlpha) + aSide * sinAlpha;

            // 実世界スケールのマッハ数造波抗力補正
            double machDragMult = calculateMachDragMultiplier(mach);
            double baseCd = params.cd0() * machDragMult;

            // 合成抗力係数 Cd(alpha, Mach)
            double cd = baseCd * (cosAlpha * cosAlpha) + params.cdSide() * (sinAlpha * sinAlpha);

            // 動圧 q = 0.5 * rho * v^2
            double q = 0.5 * rho * speed * speed;

            // 抗力 F_drag (進行方向と逆向き)
            dragNewtons = q * cd * aEff;
            fDrag = vHat.scale(-dragNewtons);

            // 揚力方向ベクトル (弾軸を含み進行方向に直交するベクトル)
            Vec3 uPerp = u.subtract(vHat.scale(cosAlpha));
            double uPerpLen = uPerp.length();
            if (uPerpLen > 1.0E-5) {
                Vec3 nLift = uPerp.scale(1.0 / uPerpLen);
                // 揚力係数 Cl = 2 * Cl_alpha * sin(alpha) * cos(alpha)
                double cl = 2.0 * params.clAlpha() * sinAlpha * cosAlpha;
                liftNewtons = q * cl * aFront;
                fLift = nLift.scale(liftNewtons);
            }
        }

        // 推力 F_thrust (弾軸方向)
        double thrustNewtons = Math.max(0.0, params.thrust());
        Vec3 fThrust = (thrustNewtons > 0.0) ? u.scale(thrustNewtons) : Vec3.ZERO;

        // 重力 F_gravity
        Vec3 fGravity = GRAVITY.scale(params.mass());

        // 合力 F_total
        Vec3 fTotal = fDrag.add(fLift).add(fGravity).add(fThrust);

        // 加速度 a = F / m
        Vec3 accel = fTotal.scale(1.0 / params.mass());

        // 新速度 v_next = v + a * dt
        Vec3 nextVel = vel.add(accel.scale(dt));

        // 新位置 p_next = p + nextVel * dt
        Vec3 nextPos = pos.add(nextVel.scale(dt));

        // ウェザーベーン効果（空力復元モーメントによる弾軸追従）
        Vec3 nextOrientation = u;
        double nextSpeed = nextVel.length();
        if (nextSpeed > 1.0E-3 && params.stabilityFactor() > 0.0) {
            Vec3 nextVHat = nextVel.scale(1.0 / nextSpeed);
            double alignFactor = 1.0 - Math.exp(-params.stabilityFactor() * dt);
            Vec3 blended = u.add(nextVHat.subtract(u).scale(alignFactor));
            if (blended.lengthSqr() > 1.0E-6) {
                nextOrientation = blended.normalize();
            }
        }

        BallisticsState nextState = new BallisticsState(nextPos, nextVel, nextOrientation);
        return new StepResult(nextState, dragNewtons, liftNewtons, aoaDegrees, rho, thrustNewtons, mach, soundSpeed);
    }

    /**
     * 状態とパラメータ、時間ステップ dt から次の物理状態を純粋計算します（ステートレス）。
     *
     * @param state  現在の物理状態
     * @param params 空力・物理パラメータ
     * @param dt     時間ステップ [s]
     * @return 次の物理状態
     */
    public static BallisticsState step(BallisticsState state, BallisticsParams params, double dt) {
        return stepResult(state, params, dt).state();
    }

    /**
     * {@link IBallisticProjectile} の現在状態から 1 tick 分の物理シミュレーションを実行し、
     * 計算結果をプロジェクタイルの状態に反映します。
     *
     * @param projectile シミュレーション対象の飛翔体
     * @param dt         時間ステップ [s]
     * @return 計算結果詳細 {@link StepResult}
     */
    public static StepResult updateFlight(IBallisticProjectile projectile, double dt) {
        BallisticsState currentState = new BallisticsState(
                projectile.getPositionMeters(),
                projectile.getVelocityMetersPerSecond(),
                projectile.getOrientation()
        );
        BallisticsParams params = BallisticsParams.from(projectile);

        StepResult result = stepResult(currentState, params, dt);
        BallisticsState nextState = result.state();

        projectile.setPositionMeters(nextState.position());
        projectile.setVelocityMetersPerSecond(nextState.velocity());
        projectile.setOrientation(nextState.orientation());

        return result;
    }
}
