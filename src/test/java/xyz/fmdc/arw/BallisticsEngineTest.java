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
        assertEquals(8500.0, BallisticsEngine.SCALE_HEIGHT, 1e-4, "実世界の地球標準大気スケールハイト8500mであること");
    }

    @Test
    public void testSpeedOfSoundAndMachDrag() {
        // 海面高度 (63.0) での音速が約 340.29 m/s
        double speedSoundSea = BallisticsEngine.calculateSpeedOfSound(63.0);
        assertEquals(BallisticsEngine.SPEED_OF_SOUND_SEA_LEVEL, speedSoundSea, 0.5);

        // 高度が高くなると気温低下に伴い音速が低下すること
        double speedSoundHigh = BallisticsEngine.calculateSpeedOfSound(5000.0);
        assertTrue(speedSoundHigh < speedSoundSea, "高高度では気温低下により音速が下がること");

        // 亜音速 (M=0.5) では抗力倍率は1.0
        assertEquals(1.0, BallisticsEngine.calculateMachDragMultiplier(0.5), 1e-4);

        // 遷音速〜音速付近 (M=1.05) で波抗力ピーク (2.0倍以上)
        double peakDrag = BallisticsEngine.calculateMachDragMultiplier(1.05);
        assertTrue(peakDrag >= 2.0, "音速突破時に波抗力が激増すること");

        // 超音速域 (M=2.5) ではピークより減少するが1.0より高いこと
        double superDrag = BallisticsEngine.calculateMachDragMultiplier(2.5);
        assertTrue(superDrag < peakDrag, "超音速域ではピーク時より抗力倍率が減少すること");
        assertTrue(superDrag > 1.0, "超音速域でも波抗力により亜音速より高いこと");
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
