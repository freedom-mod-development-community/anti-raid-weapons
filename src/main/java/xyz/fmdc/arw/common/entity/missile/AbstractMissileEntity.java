package xyz.fmdc.arw.common.entity.missile;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import xyz.fmdc.arw.AntiRaidWeapons;
import xyz.fmdc.arw.api.projectile.telemetry.FlightTelemetryLogger;
import xyz.fmdc.arw.common.entity.AbstractBallisticProjectileEntity;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * ミサイル（誘導弾・ロケット推進弾）の基底抽象クラス.
 * 外弾道物理エンジン {@link xyz.fmdc.arw.api.projectile.BallisticsEngine} に基づき、
 * ロケットモーター推進、目標追尾誘導（迎角空力誘導）、近接信管、着弾爆発を管理します。
 */
public abstract class AbstractMissileEntity extends AbstractBallisticProjectileEntity {

    private static final EntityDataAccessor<Boolean> IS_MOTOR_BURNING =
            SynchedEntityData.defineId(AbstractMissileEntity.class, EntityDataSerializers.BOOLEAN);

    /** ミサイル用チャンクロードチケット定義（タイムアウト60ticks = 3秒のセーフティ付き） */
    public static final TicketType<UUID> MISSILE_CHUNK_TICKET =
            TicketType.create("arw_missile", UUID::compareTo, 60);
    private static final int CHUNK_LOAD_RADIUS = 2; // 半径2 -> 中心チャンクのチケットレベル31（ENTITY_TICKING）

    protected boolean chunkLoadingEnabled = true;
    private final Set<ChunkPos> activeChunkTickets = new HashSet<>();

    protected float explosionPower = 6.0F;
    protected float directDamage = 100.0F;

    /** 1tickあたりの最大移動速度 [blocks/tick] */
    protected float maxSpeed = 60.0F;

    /** 1tickあたりのロケット推力加速度（後方互換・推力換算用） [blocks/tick^2] */
    protected float acceleration = 0.8F;

    /** ロケットモーター推力 [N] (未設定時は acceleration と mass から自動計算) */
    protected double motorThrustNewtons = 0.0;

    // --- IBallisticProjectile 諸元デフォルト (SM-2 MR相当) ---
    protected double diameter = 0.34;           // [m]
    protected double length = 4.72;             // [m]
    protected double mass = 708.0;              // [kg]
    protected double dragCoefficientZero = 0.25; // ゼロ迎角時Cd0
    protected double sideDragCoefficient = 1.20; // 側面Cd
    protected double liftSlope = 2.5;           // 迎角による動翼揚力効果
    protected double stabilityFactor = 4.0;     // 復元安定係数

    /** 目標への旋回追従係数（0.0: 直進 〜 1.0: 即座に目標方向へ姿勢を向ける） */
    protected float turnRate = 0.08F;

    /** ロケットモーターの燃焼持続Tick数（例: 100 ticks = 5秒） */
    protected int motorBurnTicks = 100;

    /** 近接信管半径（ブロック単位。この半径内に目標が入れば起爆。0以下の場合は無効） */
    protected double proximityFuseRadius = 2.5D;

    /** 近接信管の起爆遅延Tick（発射直後の誤爆防止） */
    protected int proximityFuseArmTicks = 15;

    protected int lifeTicks = 0;
    protected int maxLifeTicks = 300; // 15秒で燃料切れまたは自爆

    @Nullable
    protected Vec3 targetPos = null;

    @Nullable
    protected UUID targetEntityUuid = null;

    @Nullable
    protected Entity cachedTargetEntity = null;

    public AbstractMissileEntity(EntityType<? extends ThrowableProjectile> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(IS_MOTOR_BURNING, true);
    }

    // --- IBallisticProjectile 実装 ---

    @Override
    public double getDiameter() {
        return this.diameter;
    }

    @Override
    public double getLength() {
        return this.length;
    }

    @Override
    public double getMass() {
        return this.mass;
    }

    @Override
    public double getDragCoefficientZero() {
        return this.dragCoefficientZero;
    }

    @Override
    public double getSideDragCoefficient() {
        return this.sideDragCoefficient;
    }

    @Override
    public double getLiftSlope() {
        return this.liftSlope;
    }

    @Override
    public double getStabilityFactor() {
        return this.stabilityFactor;
    }

    @Override
    public double getThrustNewtons() {
        if (!isMotorBurning()) {
            return 0.0;
        }
        if (this.motorThrustNewtons > 0.0) {
            return this.motorThrustNewtons;
        }
        // acceleration (blocks/tick^2) から推力 [N] を換算: 1 block/tick^2 = 400 m/s^2
        return this.mass * (this.acceleration * 400.0);
    }

    public boolean isMotorBurning() {
        return this.entityData.get(IS_MOTOR_BURNING);
    }

    public void setMotorBurning(boolean burning) {
        this.entityData.set(IS_MOTOR_BURNING, burning);
    }

    public void setTargetPos(@Nullable Vec3 targetPos) {
        this.targetPos = targetPos;
    }

    @Nullable
    public Vec3 getTargetPos() {
        return this.targetPos;
    }

    public void setTargetEntity(@Nullable Entity target) {
        this.cachedTargetEntity = target;
        this.targetEntityUuid = (target != null) ? target.getUUID() : null;
    }

    @Nullable
    public Entity getTargetEntity() {
        if (this.cachedTargetEntity != null && this.cachedTargetEntity.isAlive()) {
            return this.cachedTargetEntity;
        }
        if (this.targetEntityUuid != null && this.level() instanceof ServerLevel serverLevel) {
            this.cachedTargetEntity = serverLevel.getEntity(this.targetEntityUuid);
            return this.cachedTargetEntity;
        }
        return null;
    }

    @Override
    public void tick() {
        this.lifeTicks++;

        // 寿命到来時の自爆
        if (!this.level().isClientSide && this.lifeTicks >= this.maxLifeTicks) {
            FlightTelemetryLogger.endSession(this.getUUID(), "MAX_LIFE_TIMEOUT", this.position());
            explode();
            return;
        }

        // 飛行・推進制御（サーバー側）
        if (!this.level().isClientSide) {
            updateChunkLoading();
            boolean motorActive = this.lifeTicks <= this.motorBurnTicks;
            if (isMotorBurning() != motorActive) {
                setMotorBurning(motorActive);
            }

            // 目標誘導（弾軸姿勢の旋回制御）
            if (motorActive || this.lifeTicks < this.maxLifeTicks) {
                applyGuidance();
            }

            // 近接信管チェック
            if (this.lifeTicks >= this.proximityFuseArmTicks && this.proximityFuseRadius > 0.0D) {
                checkProximityFuse();
            }
        }

        // 基底クラスの外弾道物理計算およびレイキャスト衝突判定を実行
        super.tick();

        // 速度上限の適用
        Vec3 motion = this.getDeltaMovement();
        if (motion.length() > this.maxSpeed) {
            this.setDeltaMovement(motion.normalize().scale(this.maxSpeed));
        }

        // 推進エフェクト（クライアント側）
        if (this.level().isClientSide && isMotorBurning()) {
            spawnFlightParticles();
        }
    }

    /**
     * 空力誘導制御: 目標方向に向けてミサイルの弾軸姿勢（orientation）を旋回させます。
     * <p>
     * 弾軸姿勢が目標へ旋回すると、進行方向との間に「迎角（Angle of Attack）」が発生し、
     * 外弾道エンジン {@link xyz.fmdc.arw.api.projectile.BallisticsEngine} の揚力によって旋回軌道を描きます。
     * 同時に迎角増大により抗力が増大し、旋回時の自然な減速（誘導抗力）が発生します。
     */
    protected void applyGuidance() {
        Vec3 target = null;
        Entity tgtEntity = getTargetEntity();
        if (tgtEntity != null && tgtEntity.isAlive()) {
            target = tgtEntity.getBoundingBox().getCenter();
        } else if (this.targetPos != null) {
            target = this.targetPos;
        }

        if (target == null) return;

        Vec3 currentPos = this.position();
        Vec3 toTarget = target.subtract(currentPos);
        double dist = toTarget.length();
        if (dist < 1.0E-3) return;

        Vec3 desiredDir = toTarget.normalize();
        Vec3 currentOri = this.getOrientation();

        // 目標方向への弾軸姿勢の補間（フィン旋回）
        Vec3 newOri = currentOri.scale(1.0 - this.turnRate).add(desiredDir.scale(this.turnRate));
        if (newOri.lengthSqr() > 1.0E-6) {
            this.setOrientation(newOri.normalize());
        }
    }

    /**
     * 近接信管：指定半径内にエンティティがいれば起爆
     */
    protected void checkProximityFuse() {
        AABB searchBox = this.getBoundingBox().inflate(this.proximityFuseRadius);
        List<LivingEntity> nearby = this.level().getEntitiesOfClass(
                LivingEntity.class,
                searchBox,
                e -> e.isAlive() && !e.isSpectator() && !e.equals(getOwner()) && !this.isPassengerOfSameVehicle(e)
        );

        if (!nearby.isEmpty()) {
            LivingEntity target = nearby.get(0);
            Vec3 hitPos = this.position();
            AntiRaidWeapons.LOGGER.info("{} proximity fuse triggered near entity '{}' (Type: {}) at ({}, {}, {})",
                    this.getType().getDescription().getString(),
                    target.getName().getString(),
                    target.getType().getDescription().getString(),
                    hitPos.x, hitPos.y, hitPos.z);
            FlightTelemetryLogger.endSession(
                    this.getUUID(),
                    "PROXIMITY_FUSE [" + target.getName().getString() + "]",
                    hitPos
            );
            explode();
        }
    }

    /**
     * 飛行中の排気煙・ロケット炎パーティクル
     */
    protected void spawnFlightParticles() {
        Vec3 pos = this.position();
        Vec3 motion = this.getDeltaMovement();
        if (motion.lengthSqr() < 1.0E-6) return;
        Vec3 back = motion.normalize().scale(-0.8);

        this.level().addParticle(
                ParticleTypes.CAMPFIRE_COSY_SMOKE,
                pos.x + back.x, pos.y + back.y, pos.z + back.z,
                back.x * 0.05, back.y * 0.05, back.z * 0.05
        );
        this.level().addParticle(
                ParticleTypes.FLAME,
                pos.x + back.x * 0.5, pos.y + back.y * 0.5, pos.z + back.z * 0.5,
                0, 0, 0
        );
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide) {
            Entity target = result.getEntity();
            Vec3 hitPos = result.getLocation();
            AntiRaidWeapons.LOGGER.info("{} hit entity '{}' (Type: {}) at ({}, {}, {})",
                    this.getType().getDescription().getString(),
                    target.getName().getString(),
                    target.getType().getDescription().getString(),
                    hitPos.x, hitPos.y, hitPos.z);
            target.hurt(this.damageSources().thrown(this, getOwner()), this.directDamage);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide) {
            BlockState blockState = this.level().getBlockState(result.getBlockPos());
            Vec3 hitPos = result.getLocation();
            AntiRaidWeapons.LOGGER.info("{} hit block '{}' at ({}, {}, {}) [BlockPos: {}]",
                    this.getType().getDescription().getString(),
                    blockState.getBlock().getName().getString(),
                    hitPos.x, hitPos.y, hitPos.z,
                    result.getBlockPos());
        }
    }

    @Override
    protected void onHit(HitResult result) {
        if (!this.level().isClientSide) {
            if (result.getType() == HitResult.Type.ENTITY) {
                this.onHitEntity((EntityHitResult) result);
            } else if (result.getType() == HitResult.Type.BLOCK) {
                this.onHitBlock((BlockHitResult) result);
            }
            explode();
        }
    }

    /**
     * ミサイル周辺および進行方向先読みチャンクの動的チケット管理（未ロード領域突入によるフリーズ防止）
     */
    protected void updateChunkLoading() {
        if (!this.chunkLoadingEnabled || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        ChunkPos currentChunk = new ChunkPos(this.blockPosition());
        Vec3 motion = this.getDeltaMovement();
        ChunkPos leadChunk = new ChunkPos(BlockPos.containing(this.position().add(motion.scale(8.0))));

        Set<ChunkPos> desiredChunks = new HashSet<>();
        desiredChunks.add(currentChunk);
        desiredChunks.add(leadChunk);

        Iterator<ChunkPos> it = this.activeChunkTickets.iterator();
        while (it.hasNext()) {
            ChunkPos pos = it.next();
            if (!desiredChunks.contains(pos)) {
                serverLevel.getChunkSource().removeRegionTicket(MISSILE_CHUNK_TICKET, pos, CHUNK_LOAD_RADIUS, this.getUUID());
                it.remove();
            }
        }

        boolean periodicRefresh = (this.lifeTicks % 20 == 0);
        for (ChunkPos pos : desiredChunks) {
            if (periodicRefresh || !this.activeChunkTickets.contains(pos)) {
                serverLevel.getChunkSource().addRegionTicket(MISSILE_CHUNK_TICKET, pos, CHUNK_LOAD_RADIUS, this.getUUID());
                this.activeChunkTickets.add(pos);
            }
        }
    }

    /**
     * 付与した全てのチャンクチケットを確実に解放・クリーンアップします。
     */
    protected void clearChunkTickets() {
        if (!this.activeChunkTickets.isEmpty() && this.level() instanceof ServerLevel serverLevel) {
            for (ChunkPos pos : this.activeChunkTickets) {
                serverLevel.getChunkSource().removeRegionTicket(MISSILE_CHUNK_TICKET, pos, CHUNK_LOAD_RADIUS, this.getUUID());
            }
            this.activeChunkTickets.clear();
        }
    }

    public boolean isChunkLoadingEnabled() {
        return this.chunkLoadingEnabled;
    }

    public void setChunkLoadingEnabled(boolean enabled) {
        this.chunkLoadingEnabled = enabled;
        if (!enabled) {
            clearChunkTickets();
        }
    }

    @Override
    public void onRemovedFromWorld() {
        clearChunkTickets();
        super.onRemovedFromWorld();
    }

    @Override
    public void remove(RemovalReason reason) {
        clearChunkTickets();
        super.remove(reason);
    }

    /**
     * 起爆処理（爆発の発生およびエンティティ消滅）
     */
    public void explode() {
        if (!this.level().isClientSide) {
            clearChunkTickets();
            this.level().explode(
                    this,
                    this.getX(), this.getY(), this.getZ(),
                    this.explosionPower,
                    Level.ExplosionInteraction.TNT
            );
            this.discard();
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("LifeTicks", this.lifeTicks);
        tag.putBoolean("ChunkLoadingEnabled", this.chunkLoadingEnabled);
        tag.putBoolean("MotorBurning", isMotorBurning());
        tag.putFloat("ExplosionPower", this.explosionPower);
        tag.putFloat("DirectDamage", this.directDamage);
        if (this.targetPos != null) {
            tag.putDouble("TargetX", this.targetPos.x);
            tag.putDouble("TargetY", this.targetPos.y);
            tag.putDouble("TargetZ", this.targetPos.z);
        }
        if (this.targetEntityUuid != null) {
            tag.putUUID("TargetEntityUuid", this.targetEntityUuid);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.lifeTicks = tag.getInt("LifeTicks");
        if (tag.contains("ChunkLoadingEnabled")) {
            this.chunkLoadingEnabled = tag.getBoolean("ChunkLoadingEnabled");
        }
        if (tag.contains("MotorBurning")) {
            setMotorBurning(tag.getBoolean("MotorBurning"));
        }
        if (tag.contains("ExplosionPower")) {
            this.explosionPower = tag.getFloat("ExplosionPower");
        }
        if (tag.contains("DirectDamage")) {
            this.directDamage = tag.getFloat("DirectDamage");
        }
        if (tag.contains("TargetX") && tag.contains("TargetY") && tag.contains("TargetZ")) {
            this.targetPos = new Vec3(tag.getDouble("TargetX"), tag.getDouble("TargetY"), tag.getDouble("TargetZ"));
        }
        if (tag.hasUUID("TargetEntityUuid")) {
            this.targetEntityUuid = tag.getUUID("TargetEntityUuid");
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
