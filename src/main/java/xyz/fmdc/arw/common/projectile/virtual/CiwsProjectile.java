package xyz.fmdc.arw.common.projectile.virtual;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import xyz.fmdc.arw.AntiRaidWeapons;
import xyz.fmdc.arw.api.TargetAffiliation;
import xyz.fmdc.arw.common.projectile.virtual.util.ProjectileRaycastHelper;
import xyz.fmdc.arw.network.PacketHandler;
import xyz.fmdc.arw.network.S2CDestroyVirtualProjectilePacket;

import java.util.UUID;

/**
 * CIWS (Phalanx等) / 高レート対空機関砲用の超高速・超短寿命仮想飛翔体。
 * 弾速 900m/s (45ブロック/Tick)、寿命 10 Ticks。
 * 未ロードチャンクへ突入した場合は即座に消滅します。
 */
public class CiwsProjectile extends VirtualProjectile {

    private final float directDamage;
    private boolean initialLogged = false;

    public CiwsProjectile(
            UUID projectileId,
            ResourceKey<Level> dimension,
            @Nullable UUID ownerUuid,
            Vec3 initialPosition,
            Vec3 initialVelocity,
            int maxAgeTicks,
            float directDamage
    ) {
        this(projectileId, dimension, ownerUuid, initialPosition, initialVelocity, maxAgeTicks, directDamage, TargetAffiliation.FRIENDLY);
    }

    public CiwsProjectile(
            UUID projectileId,
            ResourceKey<Level> dimension,
            @Nullable UUID ownerUuid,
            Vec3 initialPosition,
            Vec3 initialVelocity,
            int maxAgeTicks,
            float directDamage,
            TargetAffiliation affiliation
    ) {
        super(projectileId, dimension, ownerUuid, initialPosition, initialVelocity, maxAgeTicks, affiliation);
        this.directDamage = directDamage;
    }

    public CiwsProjectile(
            ResourceKey<Level> dimension,
            @Nullable UUID ownerUuid,
            Vec3 initialPosition,
            Vec3 direction,
            float speedMps,
            int maxAgeTicks,
            float directDamage
    ) {
        this(
                UUID.randomUUID(),
                dimension,
                ownerUuid,
                initialPosition,
                direction.normalize().scale(speedMps),
                maxAgeTicks,
                directDamage,
                TargetAffiliation.FRIENDLY
        );
    }

    public CiwsProjectile(
            ResourceKey<Level> dimension,
            @Nullable UUID ownerUuid,
            Vec3 initialPosition,
            Vec3 direction,
            float speedMps,
            int maxAgeTicks,
            float directDamage,
            TargetAffiliation affiliation
    ) {
        this(
                UUID.randomUUID(),
                dimension,
                ownerUuid,
                initialPosition,
                direction.normalize().scale(speedMps),
                maxAgeTicks,
                directDamage,
                affiliation
        );
    }

    @Override
    public byte getProjectileTypeId() {
        return 0; // CIWS
    }

    @Override
    public boolean isRadarDetectable() {
        return false; // CIWS弾はレーダー探知除外
    }

    @Override
    protected void updateMotion(ServerLevel level) {
        if (!this.initialLogged) {
            this.initialLogged = true;
            AntiRaidWeapons.LOGGER.debug(
                    "[VirtualProjectile] CIWS bullet [{}] fired at ({}, {}, {}) with velocity {} m/s",
                    this.projectileId, this.position.x, this.position.y, this.position.z, this.velocity.length()
            );
        }

        // 等速直線運動 (重力・空気抵抗は近距離のため無視)
        Vec3 step = this.velocity.scale(1.0 / 20.0);
        this.position = this.position.add(step);

        // ロード領域外（未ロードチャンク）に出た場合は即座に消滅
        int chunkX = (int) Math.floor(this.position.x) >> 4;
        int chunkZ = (int) Math.floor(this.position.z) >> 4;
        if (!ProjectileRaycastHelper.isChunkSafeAndTicking(level, chunkX, chunkZ)) {
            markDead();
        }
    }

    @Override
    public void onHit(ServerLevel level, HitResult hitResult) {
        Vec3 hitPos = hitResult.getLocation();

        // 1. ブロック命中時の効果
        if (hitResult instanceof BlockHitResult blockHit) {
            BlockPos bpos = blockHit.getBlockPos();
            level.playSound(
                    null,
                    bpos,
                    SoundEvents.STONE_HIT,
                    SoundSource.BLOCKS,
                    1.0F,
                    1.2F
            );
        }

        // 2. エンティティ命中時のダメージ処理
        if (hitResult instanceof EntityHitResult entityHit && entityHit.getEntity() != null) {
            Entity target = entityHit.getEntity();
            DamageSource ds = level.damageSources().generic();
            target.hurt(ds, this.directDamage);
            AntiRaidWeapons.LOGGER.debug(
                    "[VirtualProjectile] CIWS bullet [{}] hit entity [{}] for {} damage at ({}, {}, {})",
                    this.projectileId, target.getName().getString(), this.directDamage, hitPos.x, hitPos.y, hitPos.z
            );
        } else {
            AntiRaidWeapons.LOGGER.debug(
                    "[VirtualProjectile] CIWS bullet [{}] hit terrain at ({}, {}, {})",
                    this.projectileId, hitPos.x, hitPos.y, hitPos.z);
        }

        // 周囲のクライアントへ着弾破棄パケットを同期待ちなしで送信
        PacketHandler.sendToNear(
                level,
                hitPos,
                128.0,
                new S2CDestroyVirtualProjectilePacket(this.projectileId, hitPos, (byte) 0)
        );
    }
}
