package xyz.fmdc.arw.api.projectile;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 飛翔体（砲弾・ミサイル等）の物理挙動を計算するステートレスな外弾道物理計算エンジン.
 * <p>
 * 高度による空気密度変化、弾体姿勢と進行方向のズレ（迎角）に応じた円柱投影面積および抗力・揚力、
 * ウェザーベーン効果（進行方向への弾軸姿勢復元）、ロケット推力、重力を統合して計算します。
 */
public final class BallisticsEngine {

    /** Minecraft標準の海面高度（基準空気密度点） */
    public static final double SEA_LEVEL_Y = 63.0;

    /** 海面基準空気密度 \rho_0 [kg/m^3] */
    public static final double RHO_SEA_LEVEL = 1.225;

    /**
     * Minecraftの高度限界（約320m）に合わせた実用スケールハイト H [m].
     * 地上〜成層圏の空気抵抗変化をゲーム内で体感できるよう、実地球（約8500m）より圧縮した値を使用。
     */
    public static final double SCALE_HEIGHT = 2000.0;

    /** 重力加速度ベクトル [m/s^2] (Minecraft 1 block = 1 m) */
    public static final Vec3 GRAVITY = new Vec3(0, -9.80665, 0);

    private BallisticsEngine() {}

    /**
     * 高度 y [m] における空気密度 \rho(y) [kg/m^3] を算出します。
     * 指数関数的減衰モデル: \rho(y) = \rho_0 * \exp(- (y - y_0) / H)
     *
     * @param altitudeY MinecraftのY座標
     * @return 空気密度 [kg/m^3]
     */
    public static double calculateAirDensity(double altitudeY) {
        double relativeAltitude = Math.max(0.0, altitudeY - SEA_LEVEL_Y);
        return RHO_SEA_LEVEL * Math.exp(-relativeAltitude / SCALE_HEIGHT);
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
            double thrustNewtons
    ) {}

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

            // 合成抗力係数 Cd(alpha)
            double cd = params.cd0() * (cosAlpha * cosAlpha) + params.cdSide() * (sinAlpha * sinAlpha);

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
        return new StepResult(nextState, dragNewtons, liftNewtons, aoaDegrees, rho, thrustNewtons);
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
