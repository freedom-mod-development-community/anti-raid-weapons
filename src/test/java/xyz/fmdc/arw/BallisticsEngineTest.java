package xyz.fmdc.arw;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import xyz.fmdc.arw.api.projectile.BallisticsEngine;

import static org.junit.jupiter.api.Assertions.*;

public class BallisticsEngineTest {

    @Test
    public void testAirDensityDecreasesWithAltitude() {
        double rhoSea = BallisticsEngine.calculateAirDensity(63.0);
        double rhoHigh = BallisticsEngine.calculateAirDensity(300.0);
        double rhoSpace = BallisticsEngine.calculateAirDensity(2000.0);

        assertEquals(BallisticsEngine.RHO_SEA_LEVEL, rhoSea, 1e-4);
        assertTrue(rhoHigh < rhoSea, "高高度では空気密度が低下すること");
        assertTrue(rhoSpace < rhoHigh, "超高高度ではさらに空気密度が低下すること");
    }

    @Test
    public void testAoADragAndLift() {
        // 5インチ砲弾相当 (直径0.127m, 全長0.8m, 質量31.75kg)
        BallisticsEngine.BallisticsParams params = new BallisticsEngine.BallisticsParams(
                0.127, 0.8, 31.75, 0.20, 1.15, 1.8, 5.0, 0.0
        );

        Vec3 pos = new Vec3(0, 100, 0);
        Vec3 vel = new Vec3(160, 0, 0); // 160 m/s (水平X方向)

        // 迎角0度 (弾軸も水平X方向)
        BallisticsEngine.BallisticsState stateZeroAoA = new BallisticsEngine.BallisticsState(
                pos, vel, new Vec3(1, 0, 0)
        );
        BallisticsEngine.BallisticsState nextZero = BallisticsEngine.step(stateZeroAoA, params, 0.05);

        // 迎角15度 (弾軸を仰角15度に向ける)
        double rad15 = Math.toRadians(15);
        Vec3 oriAoA = new Vec3(Math.cos(rad15), Math.sin(rad15), 0);
        BallisticsEngine.BallisticsState stateAoA = new BallisticsEngine.BallisticsState(
                pos, vel, oriAoA
        );
        BallisticsEngine.BallisticsState nextAoA = BallisticsEngine.step(stateAoA, params, 0.05);

        // 迎角がある場合、抗力が増加するためX方向速度の減少量が大きくなるはず
        double velLossZero = vel.x - nextZero.velocity().x;
        double velLossAoA = vel.x - nextAoA.velocity().x;
        assertTrue(velLossAoA > velLossZero, "迎角発生時は誘導抗力・側面投影面積増加により速度低下が大きくなること");

        // 迎角がある場合、上向きの揚力が発生するためY方向加速度が重力(-9.81)を緩和するはず
        assertTrue(nextAoA.velocity().y > nextZero.velocity().y, "迎角発生時は上向き揚力が発生すること");
    }

    @Test
    public void testWeathervaneEffect() {
        // 安定係数 10.0
        BallisticsEngine.BallisticsParams params = new BallisticsEngine.BallisticsParams(
                0.127, 0.8, 31.75, 0.20, 1.15, 1.8, 10.0, 0.0
        );

        Vec3 pos = new Vec3(0, 100, 0);
        Vec3 vel = new Vec3(100, 0, 0); // 水平X進行
        Vec3 initialOri = new Vec3(0, 1, 0); // 弾軸は垂直上向き (迎角90度)

        BallisticsEngine.BallisticsState state = new BallisticsEngine.BallisticsState(pos, vel, initialOri);
        BallisticsEngine.BallisticsState next = BallisticsEngine.step(state, params, 0.05);

        // 弾軸姿勢が進行方向 (X方向) に引き戻されているか
        assertTrue(next.orientation().x > initialOri.x, "ウェザーベーン効果により進行方向へ弾軸が復元すること");
    }

    @Test
    public void testMissileThrustAccelerates() {
        // RIM-66M-2 相当 (直径0.34m, 全長4.72m, 質量708kg, 推力28000N)
        BallisticsEngine.BallisticsParams params = new BallisticsEngine.BallisticsParams(
                0.34, 4.72, 708.0, 0.22, 1.15, 3.2, 3.5, 28000.0
        );

        Vec3 pos = new Vec3(0, 100, 0);
        Vec3 vel = new Vec3(50, 0, 0);
        Vec3 ori = new Vec3(1, 0, 0);

        BallisticsEngine.BallisticsState state = new BallisticsEngine.BallisticsState(pos, vel, ori);
        BallisticsEngine.BallisticsState next = BallisticsEngine.step(state, params, 0.05);

        // 推力28000N / 708kg ≈ 39.5 m/s^2 加速度
        // 0.05s で +1.9 m/s 以上加速するはず
        assertTrue(next.velocity().x > vel.x, "ロケット推力により前進加速すること");
    }
}
