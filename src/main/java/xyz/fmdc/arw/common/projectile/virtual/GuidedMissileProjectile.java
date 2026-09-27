package xyz.fmdc.arw.common.projectile.virtual;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import xyz.fmdc.arw.AntiRaidWeapons;
import xyz.fmdc.arw.common.projectile.telemetry.FlightTelemetryLogger;
import xyz.fmdc.arw.common.projectile.virtual.util.ProjectileRaycastHelper;
import xyz.fmdc.arw.common.projectile.virtual.util.SafeExplosionHelper;
import xyz.fmdc.arw.network.PacketHandler;
import xyz.fmdc.arw.network.S2CDestroyVirtualProjectilePacket;

import java.util.List;
import java.util.UUID;

/**
 * 超長距離艦対空ミサイル（SM-2 / RIM-66M-2等・射程30km）用の仮想飛翔体。
 * 比例航法（Proportional Navigation）による高度な迎撃誘導計算を行い、
 * 目標の異次元移動や未ロード化、NPEを完全防御した慣性直進フォールバック機構を内蔵しています。
 */
public class GuidedMissileProjectile extends VirtualProjectile {

    @Nullable
    private UUID targetEntityUuid;
    private Vec3 lastKnownTargetPos = null;
    private boolean isTargetLost = false;
    private boolean sessionStarted = false;

    // ミサイル運動・誘導パラメータ
    private final float maxSpeedMps;        // 最高速度 [m/s] (例: 1200 m/s = 60m/tick)
    private final float motorAcceleration;  // ロケット加速度 [m/s^2]
    private final int motorBurnTicks;       // ロケット燃焼時間 [ticks]
    private final float turnRate;           // 最大旋回レート (rad/tick)
    private final double proximityFuseRadius;
    private final float explosionPower;
    private final float directDamage;

    // 比例航法（PN）ゲイン
    private static final double PN_GAIN = 4.0;
    private Vec3 prevLosVector = null;

    public GuidedMissileProjectile(
            UUID projectileId,
            ResourceKey<Level> dimension,
            @Nullable UUID ownerUuid,
            @Nullable UUID targetEntityUuid,
            Vec3 initialPosition,
            Vec3 initialVelocity,
            float maxSpeedMps,
            float motorAcceleration,
            int motorBurnTicks,
            int maxAgeTicks,
            float turnRate,
            double proximityFuseRadius,
            float explosionPower,
            float directDamage
    ) {
        super(projectileId, dimension, ownerUuid, initialPosition, initialVelocity, maxAgeTicks);
        this.targetEntityUuid = targetEntityUuid;
        this.maxSpeedMps = maxSpeedMps;
        this.motorAcceleration = motorAcceleration;
        this.motorBurnTicks = motorBurnTicks;
        this.turnRate = turnRate;
        this.proximityFuseRadius = proximityFuseRadius;
        this.explosionPower = explosionPower;
        this.directDamage = directDamage;
    }

    public static GuidedMissileProjectile createRim66M2(
            ResourceKey<Level> dimension,
            @Nullable UUID ownerUuid,
            @Nullable UUID targetEntityUuid,
            Vec3 initialPosition,
            Vec3 initialDirection
    ) {
        Vec3 dir = initialDirection.normalize();
        Vec3 initVel = dir.scale(100.0); // 初速 100m/s

        return new GuidedMissileProjectile(
                UUID.randomUUID(),
                dimension,
                ownerUuid,
                targetEntityUuid,
                initialPosition,
                initVel,
                1200.0F, // 1200 m/s (60 blocks/tick)
                200.0F,  // 加速度 200 m/s^2
                120,     // 6秒間燃焼
                2400,    // 最大寿命 120秒
                0.15F,   // 旋回性能
                6.0,     // 近接信管作動半径 6m
                8.0F,    // 爆発威力
                200.0F   // 直撃ダメージ
        );
    }

    @Override
    public byte getProjectileTypeId() {
        return 2; // MISSILE
    }

    @Nullable
    public UUID getTargetEntityUuid() {
        return this.targetEntityUuid;
    }

    public boolean isTargetLost() {
        return this.isTargetLost;
    }

    @Override
    protected void updateMotion(ServerLevel level) {
        double currentSpeed = this.velocity.length();
        Vec3 forward = (currentSpeed > 1.0E-4) ? this.velocity.scale(1.0 / currentSpeed) : new Vec3(0, 1, 0);

        double pitch = Math.toDegrees(Math.asin(-forward.y));
        double yaw = Math.toDegrees(Math.atan2(-forward.x, forward.z));

        // 初回Tick: テレメトリセッション開始 & 発射ログ
        if (!this.sessionStarted) {
            this.sessionStarted = true;
            FlightTelemetryLogger.startSession(
                    this.projectileId,
                    "GuidedMissile_RIM66M2",
                    this.position,
                    this.velocity,
                    forward,
                    (float) pitch,
                    (float) yaw
            );
            AntiRaidWeapons.LOGGER.info(
                    "[VirtualProjectile] Missile [{}] launched towards target [{}] at ({}, {}, {}) with initial velocity {} m/s",
                    this.projectileId, this.targetEntityUuid, this.position.x, this.position.y, this.position.z, currentSpeed
            );
        }

        // 1. ロケット推力による加速 (燃焼期間中)
        double thrustN = 0.0;
        if (this.ageTicks < this.motorBurnTicks) {
            currentSpeed = Math.min(this.maxSpeedMps, currentSpeed + this.motorAcceleration * 0.05);
            thrustN = this.motorAcceleration * 700.0; // 概算質量700kg
        }

        // 2. 誘導計算（目標探索 & フェイルセーフ）
        Vec3 desiredDirection = forward;
        String event = "";
        if (!this.isTargetLost && this.targetEntityUuid != null) {
            Entity target = findTargetSafe(level, this.targetEntityUuid);

            if (target != null && target.isAlive()) {
                // 目標の有効性を確認
                Vec3 targetPos = target.position().add(0, target.getEyeHeight() * 0.5, 0);
                this.lastKnownTargetPos = targetPos;

                // 比例航法 (PN: Proportional Navigation)
                Vec3 losVector = targetPos.subtract(this.position);
                double range = losVector.length();

                if (range > 1.0E-3) {
                    Vec3 losUnit = losVector.scale(1.0 / range);

                    if (this.prevLosVector != null) {
                        // 視線角速度 (LOS Rate) の計算
                        Vec3 losDelta = losUnit.subtract(this.prevLosVector).scale(20.0); // dLOS/dt
                        // 指令加速度 a_cmd = N * V_closing * dLOS/dt
                        Vec3 pAccel = losDelta.scale(PN_GAIN * currentSpeed);
                        Vec3 targetVel = forward.scale(currentSpeed).add(pAccel.scale(0.05));
                        if (targetVel.lengthSqr() > 1.0E-4) {
                            desiredDirection = targetVel.normalize();
                        }
                    } else {
                        desiredDirection = losUnit;
                    }
                    this.prevLosVector = losUnit;
                }
            } else {
                // 目標ロスト（異次元移動、死亡、または未ロード領域へ離脱）
                // フェイルセーフ: NPEを防ぎ、慣性直進モードへ安全に移行
                this.isTargetLost = true;
                this.targetEntityUuid = null;
                event = "TARGET_LOST";
                AntiRaidWeapons.LOGGER.info("[VirtualProjectile] Missile [{}] lost target entity. Switching to ballistic inertial straight flight.", this.projectileId);
            }
        }

        // 3. 旋回制限（最大旋回レート turnRate による滑らかな姿勢追従）
        Vec3 currentDir = forward;
        double dot = Math.max(-1.0, Math.min(1.0, currentDir.dot(desiredDirection)));
        double angle = Math.acos(dot);
        if (angle > 1.0E-4) {
            double step = Math.min(angle, this.turnRate);
            Vec3 rotationAxis = currentDir.cross(desiredDirection);
            if (rotationAxis.lengthSqr() < 1.0E-6) {
                rotationAxis = new Vec3(0, 1, 0);
            }
            rotationAxis = rotationAxis.normalize();

            // ロドリゲスの回転公式
            Vec3 v = currentDir;
            Vec3 k = rotationAxis;
            double cosTheta = Math.cos(step);
            double sinTheta = Math.sin(step);
            forward = v.scale(cosTheta)
                    .add(k.cross(v).scale(sinTheta))
                    .add(k.scale(k.dot(v) * (1.0 - cosTheta)))
                    .normalize();
        }

        this.velocity = forward.scale(currentSpeed);

        // 4. 位置更新
        this.position = this.position.add(this.velocity.scale(1.0 / 20.0));

        // 1Tickテレメトリデータの記録
        FlightTelemetryLogger.recordTick(
                this.projectileId,
                this.ageTicks,
                this.position,
                this.velocity,
                forward,
                (float) pitch,
                (float) yaw,
                1.225,
                thrustN,
                0.0,
                0.0,
                event
        );

        // 5. 近接信管（Proximity Fuse）判定
        checkProximityFuse(level);
    }

    /**
     * 異次元移動・未ロード状態を安全にガードしながら目標Entityを検索します。
     */
    @Nullable
    private Entity findTargetSafe(ServerLevel level, UUID uuid) {
        Entity entity = level.getEntity(uuid);
        if (entity == null) {
            return null;
        }

        // フェイルセーフ3: 目標が別ディメンションへ移動していないか
        if (entity.level().dimension() != this.dimension) {
            return null;
        }

        // フェイルセーフ2: 目標の位置がロード済みチャンク内にあるか
        int cx = entity.getBlockX() >> 4;
        int cz = entity.getBlockZ() >> 4;
        if (!ProjectileRaycastHelper.isChunkSafeAndTicking(level, cx, cz)) {
            return null;
        }

        return entity;
    }

    /**
     * 近接信管の判定。ロード済みチャンク内のLivingEntityが信管作動半径に入った場合、起爆します。
     */
    private void checkProximityFuse(ServerLevel level) {
        int cx = (int) Math.floor(this.position.x) >> 4;
        int cz = (int) Math.floor(this.position.z) >> 4;
        if (!ProjectileRaycastHelper.isChunkSafeAndTicking(level, cx, cz)) {
            return;
        }

        // 発射後しばらくは近接信管を不作動（アーム時間 15 ticks）
        if (this.ageTicks < 15) return;

        AABB box = new AABB(this.position, this.position).inflate(this.proximityFuseRadius);
        List<LivingEntity> nearby = level.getEntitiesOfClass(
                LivingEntity.class,
                box,
                e -> e.isAlive() && !e.isSpectator() && (this.ownerUuid == null || !this.ownerUuid.equals(e.getUUID()))
        );

        if (!nearby.isEmpty()) {
            LivingEntity target = nearby.get(0);
            AntiRaidWeapons.LOGGER.info("[VirtualProjectile] Missile [{}] proximity fuse triggered near [{}] at ({}, {}, {})",
                    this.projectileId, target.getName().getString(), this.position.x, this.position.y, this.position.z);
            explodeAtPosition(level, this.position, "PROXIMITY_FUSE");
        }
    }

    @Override
    public void onHit(ServerLevel level, HitResult hitResult) {
        explodeAtPosition(level, hitResult.getLocation(), hitResult.getType().name());
    }

    private void explodeAtPosition(ServerLevel level, Vec3 hitPos, String reason) {
        AntiRaidWeapons.LOGGER.info("[VirtualProjectile] Missile [{}] detonated (reason: {}) at ({}, {}, {})",
                this.projectileId, reason, hitPos.x, hitPos.y, hitPos.z);
        FlightTelemetryLogger.endSession(this.projectileId, reason, hitPos);

        // カスケード防止安全爆発
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

        // クライアントへ爆発パケットを同期
        PacketHandler.sendToNear(
                level,
                hitPos,
                384.0,
                new S2CDestroyVirtualProjectilePacket(this.projectileId, hitPos, (byte) 0)
        );

        markDead();
    }

    @Override
    protected void onExpired(ServerLevel level) {
        AntiRaidWeapons.LOGGER.info("[VirtualProjectile] Missile [{}] reached max lifetime ({} ticks). Discarding at ({}, {}, {})",
                this.projectileId, this.maxAgeTicks, this.position.x, this.position.y, this.position.z);
        FlightTelemetryLogger.endSession(this.projectileId, "TIMEOUT", this.position);
        super.onExpired(level);
    }

    @Override
    public void onServerStopping() {
        FlightTelemetryLogger.endSession(this.projectileId, "SERVER_STOP", this.position);
        super.onServerStopping();
        // フェイルセーフ5: 母艦FCS等へロスト通知を発行（非同期デッドロック防止）
        AntiRaidWeapons.LOGGER.info("[VirtualProjectile] Missile [{}] purged on server stop/level unload.", this.projectileId);
    }
}
