package xyz.fmdc.arw.api.projectile.virtual;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import xyz.fmdc.arw.api.projectile.BallisticsEngine;
import xyz.fmdc.arw.common.entity.projectile.FiveInchAmmoType;
import xyz.fmdc.arw.network.PacketHandler;
import xyz.fmdc.arw.network.S2CDestroyVirtualProjectilePacket;

import java.util.UUID;

/**
 * 5インチ砲などの通常砲弾用の仮想飛翔体。
 * BallisticsEngine による大気減衰・超音速抗力・重力を含む外弾道計算を行い、
 * 未ロードチャンクをノー計算（レイキャストなし）で飛び越え、ロード済みチャンクへの突入・着弾時に安全に爆発します。
 */
public class BallisticShellProjectile extends VirtualProjectile {

    private final BallisticsEngine.BallisticsParams params;
    private Vec3 orientation;
    private final FiveInchAmmoType ammoType;
    private final float explosionPower;
    private final float directDamage;

    public BallisticShellProjectile(
            UUID projectileId,
            ResourceKey<Level> dimension,
            @Nullable UUID ownerUuid,
            Vec3 initialPosition,
            Vec3 initialVelocity,
            Vec3 initialOrientation,
            BallisticsEngine.BallisticsParams params,
            FiveInchAmmoType ammoType,
            float explosionPower,
            float directDamage,
            int maxAgeTicks
    ) {
        super(projectileId, dimension, ownerUuid, initialPosition, initialVelocity, maxAgeTicks);
        this.orientation = initialOrientation.lengthSqr() > 1.0E-6 ? initialOrientation.normalize() : initialVelocity.normalize();
        this.params = params;
        this.ammoType = ammoType;
        this.explosionPower = explosionPower;
        this.directDamage = directDamage;
    }

    public static BallisticShellProjectile create5Inch(
            ResourceKey<Level> dimension,
            @Nullable UUID ownerUuid,
            Vec3 initialPosition,
            Vec3 initialDirection,
            float muzzleVelocityMps,
            FiveInchAmmoType ammoType
    ) {
        Vec3 dir = initialDirection.normalize();
        Vec3 vel = dir.scale(muzzleVelocityMps);

        // 127mm 諸元
        BallisticsEngine.BallisticsParams params = new BallisticsEngine.BallisticsParams(
                0.127,  // diameter [m]
                0.8,    // length [m]
                31.75,  // mass [kg]
                0.18,   // cd0 (流線型低抵抗)
                1.15,   // cdSide
                0.05,   // clAlpha
                2.5,    // stabilityFactor
                0.0     // thrust
        );

        float power = 4.0F;
        float damage = 60.0F;
        if (ammoType == FiveInchAmmoType.MK80_HE_PD || ammoType == FiveInchAmmoType.MK116_HE_VT) {
            power = 5.0F;
            damage = 80.0F;
        }

        return new BallisticShellProjectile(
                UUID.randomUUID(),
                dimension,
                ownerUuid,
                initialPosition,
                vel,
                dir,
                params,
                ammoType,
                power,
                damage,
                1200 // 60秒 (1200 ticks)
        );
    }

    @Override
    public byte getProjectileTypeId() {
        return 1; // 5-INCH SHELL
    }

    public Vec3 getOrientation() {
        return this.orientation;
    }

    public FiveInchAmmoType getAmmoType() {
        return this.ammoType;
    }

    @Override
    protected void updateMotion(ServerLevel level) {
        // 1Tick (0.05秒) の弾道力学計算
        BallisticsEngine.BallisticsState currentState = new BallisticsEngine.BallisticsState(
                this.position,
                this.velocity,
                this.orientation
        );

        BallisticsEngine.StepResult result = BallisticsEngine.stepResult(currentState, this.params, 0.05);

        this.position = result.state().position();
        this.velocity = result.state().velocity();
        this.orientation = result.state().orientation();

        // 世界の底（Void）へ落下した場合の破棄
        if (this.position.y < level.getMinBuildHeight() - 64) {
            markDead();
        }
    }

    @Override
    public void onHit(ServerLevel level, HitResult hitResult) {
        Vec3 hitPos = hitResult.getLocation();

        // 1. 直撃エンティティへのダメージ適用
        if (hitResult instanceof EntityHitResult entityHit) {
            Entity target = entityHit.getEntity();
            target.hurt(level.damageSources().thrown(null, null), this.directDamage);
        }

        // 2. カスケード地形生成を防止する安全な爆発（フェイルセーフ4）
        SafeExplosionHelper.explodeSafe(
                level,
                null,
                level.damageSources().explosion(null, null),
                hitPos.x,
                hitPos.y,
                hitPos.z,
                this.explosionPower,
                false,
                Level.ExplosionInteraction.MOB
        );

        // 3. クライアントへの同期パケット送出
        PacketHandler.sendToNear(
                level,
                hitPos,
                256.0,
                new S2CDestroyVirtualProjectilePacket(this.projectileId, hitPos, (byte) 0)
        );
    }

    @Override
    protected void onExpired(ServerLevel level) {
        // 寿命到達時は安全に破棄
        PacketHandler.sendToNear(
                level,
                this.position,
                128.0,
                new S2CDestroyVirtualProjectilePacket(this.projectileId, this.position, (byte) 2)
        );
    }
}
