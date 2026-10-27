package xyz.fmdc.arw;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import xyz.fmdc.arw.api.RadarTargetManager;
import xyz.fmdc.arw.api.TargetAffiliation;
import xyz.fmdc.arw.api.TargetType;
import xyz.fmdc.arw.api.TrackedTarget;
import xyz.fmdc.arw.api.projectile.virtual.IVirtualProjectile;
import xyz.fmdc.arw.common.entity.projectile.FiveInchAmmoType;
import xyz.fmdc.arw.common.projectile.virtual.BallisticShellProjectile;
import xyz.fmdc.arw.common.projectile.virtual.CiwsProjectile;
import xyz.fmdc.arw.common.projectile.virtual.GuidedMissileProjectile;
import xyz.fmdc.arw.common.projectile.virtual.VirtualProjectile;
import xyz.fmdc.arw.common.projectile.virtual.VirtualProjectileManager;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 仮想飛翔体（Virtual Projectile）とレーダー探知パイプライン（TrackedTarget, RadarTargetManager, FCS）
 * の統合単体テスト。
 */
public class VirtualProjectileRadarIntegrationTest {

    @SuppressWarnings("unchecked")
    private static ResourceKey<Level> createDimensionKey(String name) {
        try {
            Constructor<?> constructor = ResourceKey.class.getDeclaredConstructor(ResourceLocation.class, ResourceLocation.class);
            constructor.setAccessible(true);
            return (ResourceKey<Level>) constructor.newInstance(
                    new ResourceLocation("minecraft", "dimension"),
                    new ResourceLocation("minecraft", name)
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final ResourceKey<Level> OVERWORLD = createDimensionKey("overworld");

    @BeforeEach
    public void setUp() {
        VirtualProjectileManager.getInstance().clearAndNotifyAll();
        RadarTargetManager.INSTANCE.clear();
    }

    @Test
    @DisplayName("仮想飛翔体の探知フラグ（isRadarDetectable）の個別挙動検証")
    public void testRadarDetectability() {
        UUID ownerId = UUID.randomUUID();

        // 1. 誘導ミサイル (RIM-66) -> 探知対象 (true)
        GuidedMissileProjectile missile = GuidedMissileProjectile.createRim66M2(
                OVERWORLD,
                ownerId,
                null,
                new Vec3(0, 50, 0),
                new Vec3(1, 0, 0)
        );
        assertTrue(missile.isRadarDetectable(), "RIM-66 ミサイルはレーダー探知可能であること");

        // 2. 5インチ大型砲弾 -> 探知対象 (true)
        BallisticShellProjectile shell = BallisticShellProjectile.create5Inch(
                OVERWORLD,
                ownerId,
                new Vec3(0, 50, 0),
                new Vec3(1, 0, 0),
                800.0F,
                FiveInchAmmoType.MK80_HE_PD,
                2400
        );
        assertTrue(shell.isRadarDetectable(), "5インチ砲弾はレーダー探知可能であること");

        // 3. CIWS 機関砲弾 -> 探知除外 (false)
        CiwsProjectile ciws = new CiwsProjectile(
                OVERWORLD,
                ownerId,
                new Vec3(0, 50, 0),
                new Vec3(1, 0, 0),
                900.0F,
                10,
                25.0F
        );
        assertFalse(ciws.isRadarDetectable(), "CIWS 弾はレーダー探知除外（目標溢れ防止）であること");

        // RadarTargetManager の判定メソッド
        assertTrue(RadarTargetManager.INSTANCE.isRadarDetectable((IVirtualProjectile) missile));
        assertTrue(RadarTargetManager.INSTANCE.isRadarDetectable((IVirtualProjectile) shell));
        assertFalse(RadarTargetManager.INSTANCE.isRadarDetectable((IVirtualProjectile) ciws));
    }

    @Test
    @DisplayName("VirtualProjectile からの TrackedTarget 生成と属性抽出検証")
    public void testTrackedTargetCreationFromVirtualProjectile() {
        long gameTime = 12345L;
        UUID ownerId = UUID.randomUUID();

        // ミサイルの生成と TrackedTarget 変換
        GuidedMissileProjectile missile = GuidedMissileProjectile.createRim66M2(
                OVERWORLD,
                ownerId,
                null,
                new Vec3(100, 60, 200),
                new Vec3(0, 0, 1),
                TargetAffiliation.HOSTILE
        );
        TrackedTarget missileTrack = new TrackedTarget(missile, gameTime);

        assertEquals(missile.getProjectileId(), missileTrack.getEntityId());
        assertEquals(TargetType.MISSILE, missileTrack.getTargetType());
        assertEquals("Missile", missileTrack.getEntityTypeName());
        assertEquals(new Vec3(100, 60, 200), missileTrack.getLastKnownPos());
        assertEquals(missile.getVelocity(), missileTrack.getLastKnownVelocity());
        assertEquals(gameTime, missileTrack.getLastSeenGameTime());
        assertEquals(TargetAffiliation.HOSTILE, missileTrack.getAffiliation());
        assertTrue(missileTrack.isVirtualProjectile(), "仮想飛翔体フラグが true であること");

        // 砲弾の生成と TrackedTarget 変換
        BallisticShellProjectile shell = BallisticShellProjectile.create5Inch(
                OVERWORLD,
                ownerId,
                new Vec3(50, 70, 50),
                new Vec3(1, 1, 0),
                800.0F,
                FiveInchAmmoType.MK80_HE_PD,
                1200,
                TargetAffiliation.FRIENDLY
        );
        TrackedTarget shellTrack = new TrackedTarget(shell, gameTime);

        assertEquals(shell.getProjectileId(), shellTrack.getEntityId());
        assertEquals(TargetType.SHELL, shellTrack.getTargetType());
        assertEquals("Shell", shellTrack.getEntityTypeName());
        assertEquals(TargetAffiliation.FRIENDLY, shellTrack.getAffiliation());
        assertTrue(shellTrack.isVirtualProjectile());

        // 姿勢ベクトル（Orientation）抽出
        assertNotNull(shell.getOrientation());
        assertTrue(shell.getOrientation().length() > 0.99 && shell.getOrientation().length() < 1.01);
    }

    @Test
    @DisplayName("TrackedTarget の更新 (update) とタイムアウト (isExpired) 検証")
    public void testTrackedTargetUpdateAndExpiry() {
        GuidedMissileProjectile missile = GuidedMissileProjectile.createRim66M2(
                OVERWORLD,
                null,
                null,
                new Vec3(0, 0, 0),
                new Vec3(1, 0, 0)
        );

        TrackedTarget target = new TrackedTarget(missile, 100L);
        assertFalse(target.isExpired(100L, 40L));
        assertFalse(target.isExpired(140L, 40L));
        assertTrue(target.isExpired(141L, 40L), "40 ticks を超えたらタイムアウト（ロスト）すること");

        // 移動後の update
        missile.setPosition(new Vec3(50, 10, 0));
        target.update(missile, 130L);
        assertEquals(new Vec3(50, 10, 0), target.getLastKnownPos());
        assertEquals(130L, target.getLastSeenGameTime());
        assertFalse(target.isExpired(150L, 40L), "更新されたため新基準時間で生存していること");
        assertTrue(target.isExpired(171L, 40L));
    }

    @Test
    @DisplayName("VirtualProjectileManager のディメンション別飛翔体取得検証")
    public void testVirtualProjectileManagerGetProjectiles() {
        VirtualProjectileManager manager = VirtualProjectileManager.getInstance();

        GuidedMissileProjectile missile = GuidedMissileProjectile.createRim66M2(
                OVERWORLD,
                null,
                null,
                new Vec3(0, 0, 0),
                new Vec3(1, 0, 0)
        );
        CiwsProjectile ciws = new CiwsProjectile(
                OVERWORLD,
                null,
                new Vec3(0, 0, 0),
                new Vec3(0, 1, 0),
                900.0F,
                10,
                25.0F
        );

        manager.register(missile);
        manager.register(ciws);

        Map<UUID, VirtualProjectile> projs = manager.getProjectiles(OVERWORLD);
        assertNotNull(projs);
        assertEquals(2, projs.size(), "保留中 (pending) の飛翔体も含めて即座に取得できること");
        assertTrue(projs.containsKey(missile.getProjectileId()));
        assertTrue(projs.containsKey(ciws.getProjectileId()));
    }

    @Test
    @DisplayName("AABB範囲走査におけるCIWS除外・探知対象包含・死亡時除外の検証")
    public void testAabbCandidateFiltering() {
        VirtualProjectileManager manager = VirtualProjectileManager.getInstance();

        // 1. AABB 内部にミサイル (探知対象)
        GuidedMissileProjectile missileInside = GuidedMissileProjectile.createRim66M2(
                OVERWORLD,
                null,
                null,
                new Vec3(50, 50, 50),
                new Vec3(1, 0, 0)
        );
        manager.register(missileInside);

        // 2. AABB 外部にミサイル (範囲外)
        GuidedMissileProjectile missileOutside = GuidedMissileProjectile.createRim66M2(
                OVERWORLD,
                null,
                null,
                new Vec3(500, 50, 50),
                new Vec3(1, 0, 0)
        );
        manager.register(missileOutside);

        // 3. AABB 内部にCIWS弾 (探知除外)
        CiwsProjectile ciwsInside = new CiwsProjectile(
                OVERWORLD,
                null,
                new Vec3(50, 50, 50),
                new Vec3(1, 0, 0),
                900.0F,
                10,
                25.0F
        );
        manager.register(ciwsInside);

        // 4. AABB 内部だが消滅済み (isDead) のミサイル
        GuidedMissileProjectile missileDead = GuidedMissileProjectile.createRim66M2(
                OVERWORLD,
                null,
                null,
                new Vec3(50, 50, 50),
                new Vec3(1, 0, 0)
        );
        missileDead.markDead();
        manager.register(missileDead);

        AABB testAabb = new AABB(0, 0, 0, 100, 100, 100);
        Map<UUID, VirtualProjectile> projs = manager.getProjectiles(OVERWORLD);

        // RadarTargetManager.queryCandidatesInAABB と同等のロジック検証
        int candidatesCount = 0;
        TrackedTarget detected = null;
        for (VirtualProjectile proj : projs.values()) {
            if (proj.isDead() || !proj.isRadarDetectable()) continue;
            Vec3 pos = proj.getPosition();
            if (pos.x >= testAabb.minX && pos.x <= testAabb.maxX &&
                pos.y >= testAabb.minY && pos.y <= testAabb.maxY &&
                pos.z >= testAabb.minZ && pos.z <= testAabb.maxZ) {
                candidatesCount++;
                detected = new TrackedTarget(proj, 100L);
            }
        }

        assertEquals(1, candidatesCount, "CIWS弾、範囲外ミサイル、死亡ミサイルが除外され、範囲内の生存ミサイル1件のみ探知されること");
        assertNotNull(detected);
        assertEquals(missileInside.getProjectileId(), detected.getEntityId());
        assertEquals(TargetType.MISSILE, detected.getTargetType());
    }
}
