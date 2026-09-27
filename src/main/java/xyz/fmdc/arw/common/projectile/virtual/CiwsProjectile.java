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
import xyz.fmdc.arw.common.projectile.telemetry.FlightTelemetryLogger;
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
    private boolean sessionStarted = false;

    public CiwsProjectile(
            UUID projectileId,
            ResourceKey<Level> dimension,
            @Nullable UUID ownerUuid,
            Vec3 initialPosition,
            Vec3 initialVelocity,
            int maxAgeTicks,
            float directDamage
    ) {
        super(projectileId, dimension, ownerUuid, initialPosition, initialVelocity, maxAgeTicks);
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
                directDamage
        );
    }

    @Override
    public byte getProjectileTypeId() {
        return 0; // CIWS
    }

    @Override
    protected void updateMotion(ServerLevel level) {
        double currentSpeed = this.velocity.length();
        Vec3 forward = (currentSpeed > 1.0E-4) ? this.velocity.scale(1.0 / currentSpeed) : new Vec3(0, 0, 1);
        double pitch = Math.toDegrees(Math.asin(-forward.y));
        double yaw = Math.toDegrees(Math.atan2(-forward.x, forward.z));

        if (!this.sessionStarted) {
            this.sessionStarted = true;
            FlightTelemetryLogger.startSession(
                    this.projectileId,
                    "Ciws_Bullet",
                    this.position,
                    this.velocity,
                    forward,
                    (float) pitch,
                    (float) yaw
            );
            AntiRaidWeapons.LOGGER.debug(
                    "[VirtualProjectile] CIWS bullet [{}] fired at ({}, {}, {}) with velocity {} m/s",
                    this.projectileId, this.position.x, this.position.y, this.position.z, currentSpeed
            );
        }

        // 直進移動 (m/s -> 1Tickあたりの移動量)
        Vec3 step = this.velocity.scale(1.0 / 20.0);
        this.position = this.position.add(step);

        FlightTelemetryLogger.recordTick(
                this.projectileId,
                this.ageTicks,
                this.position,
                this.velocity,
                forward,
                (float) pitch,
                (float) yaw,
                1.225,
                0.0,
                0.0,
                0.0,
                ""
        );

        // ロード領域外（未ロードチャンク）に出た場合は即座に消滅
        int chunkX = (int) Math.floor(this.position.x) >> 4;
        int chunkZ = (int) Math.floor(this.position.z) >> 4;
        if (!ProjectileRaycastHelper.isChunkSafeAndTicking(level, chunkX, chunkZ)) {
            FlightTelemetryLogger.endSession(this.projectileId, "UNLOADED_CHUNK", this.position);
            markDead();
        }
    }

    @Override
    public void onHit(ServerLevel level, HitResult hitResult) {
        Vec3 hitPos = hitResult.getLocation();

        if (hitResult instanceof EntityHitResult entityHit) {
            Entity target = entityHit.getEntity();
            DamageSource damageSource = level.damageSources().thrown(null, null);
            target.hurt(damageSource, this.directDamage);

            // 被弾エフェクト・金属打撃音
            level.playSound(
                    null,
                    BlockPos.containing(hitPos),
                    SoundEvents.ITEM_BREAK,
                    SoundSource.NEUTRAL,
                    1.5F,
                    1.8F
            );
            AntiRaidWeapons.LOGGER.info("[VirtualProjectile] CIWS bullet [{}] hit entity '{}' at ({}, {}, {})",
                    this.projectileId, target.getName().getString(), hitPos.x, hitPos.y, hitPos.z);
        } else if (hitResult instanceof BlockHitResult blockHit) {
            // 地形への着弾音とパーティクル
            level.playSound(
                    null,
                    blockHit.getBlockPos(),
                    SoundEvents.STONE_HIT,
                    SoundSource.NEUTRAL,
                    1.0F,
                    1.5F
            );
            AntiRaidWeapons.LOGGER.debug("[VirtualProjectile] CIWS bullet [{}] hit block at ({}, {}, {})",
                    this.projectileId, hitPos.x, hitPos.y, hitPos.z);
        }

        FlightTelemetryLogger.endSession(this.projectileId, hitResult.getType().name(), hitPos);

        // 周囲のクライアントへ着弾破棄パケットを同期待ちなしで送信
        PacketHandler.sendToNear(
                level,
                hitPos,
                128.0,
                new S2CDestroyVirtualProjectilePacket(this.projectileId, hitPos, (byte) 0)
        );
    }

    @Override
    protected void onExpired(ServerLevel level) {
        FlightTelemetryLogger.endSession(this.projectileId, "TIMEOUT", this.position);
        super.onExpired(level);
    }

    @Override
    public void onServerStopping() {
        FlightTelemetryLogger.endSession(this.projectileId, "SERVER_STOP", this.position);
        super.onServerStopping();
    }
}
