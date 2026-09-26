package xyz.fmdc.arw.common.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import xyz.fmdc.arw.api.projectile.BallisticsEngine;
import xyz.fmdc.arw.api.projectile.IBallisticProjectile;
import xyz.fmdc.arw.api.projectile.telemetry.FlightTelemetryLogger;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

/**
 * {@link IBallisticProjectile} を実装し、{@link BallisticsEngine} による現実的な外弾道計算を行う
 * 全ての飛翔体（砲弾・ミサイル等）の基底抽象Entity。
 * 1Tick刻みのフライトテレメトリCSV出力機能（{@link FlightTelemetryLogger}）および
 * 未ロードチャンク突入時のフリーズを防ぐ動的チャンクロード機構を統合しています。
 */
public abstract class AbstractBallisticProjectileEntity extends ThrowableProjectile implements IBallisticProjectile {

    /** 弾道飛翔体用チャンクロードチケット定義（タイムアウト60ticks = 3秒のセーフティ付き） */
    public static final TicketType<UUID> PROJECTILE_CHUNK_TICKET =
            TicketType.create("arw_ballistic_projectile", UUID::compareTo, 60);
    protected static final int CHUNK_LOAD_RADIUS = 2; // 半径2 -> 中心チャンクのチケットレベル31（ENTITY_TICKING）

    protected boolean chunkLoadingEnabled = true;
    private final Set<ChunkPos> activeChunkTickets = new HashSet<>();

    private static final EntityDataAccessor<Float> ORIENTATION_X =
            SynchedEntityData.defineId(AbstractBallisticProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ORIENTATION_Y =
            SynchedEntityData.defineId(AbstractBallisticProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ORIENTATION_Z =
            SynchedEntityData.defineId(AbstractBallisticProjectileEntity.class, EntityDataSerializers.FLOAT);

    /** サーバー側でのフライトTick数 */
    protected int flightTicks = 0;

    public AbstractBallisticProjectileEntity(EntityType<? extends ThrowableProjectile> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(ORIENTATION_X, 0.0F);
        this.entityData.define(ORIENTATION_Y, 0.0F);
        this.entityData.define(ORIENTATION_Z, 1.0F);
    }

    // --- IBallisticProjectile の実装 ---

    @Override
    public Vec3 getPositionMeters() {
        return this.position();
    }

    @Override
    public void setPositionMeters(Vec3 position) {
        this.setPos(position.x, position.y, position.z);
    }

    @Override
    public Vec3 getVelocityMetersPerSecond() {
        return this.getDeltaMovement().scale(20.0);
    }

    @Override
    public void setVelocityMetersPerSecond(Vec3 velocity) {
        this.setDeltaMovement(velocity.scale(1.0 / 20.0));
    }

    @Override
    public Vec3 getOrientation() {
        float x = this.entityData.get(ORIENTATION_X);
        float y = this.entityData.get(ORIENTATION_Y);
        float z = this.entityData.get(ORIENTATION_Z);
        double lenSq = x * x + y * y + z * z;
        if (lenSq < 1.0E-6) {
            Vec3 rotDir = Vec3.directionFromRotation(this.getXRot(), this.getYRot());
            return rotDir.lengthSqr() > 1.0E-6 ? rotDir.normalize() : new Vec3(0, 0, 1);
        }
        return new Vec3(x, y, z).normalize();
    }

    @Override
    public void setOrientation(Vec3 orientation) {
        Vec3 norm = orientation.lengthSqr() > 1.0E-6 ? orientation.normalize() : new Vec3(0, 0, 1);
        this.entityData.set(ORIENTATION_X, (float) norm.x);
        this.entityData.set(ORIENTATION_Y, (float) norm.y);
        this.entityData.set(ORIENTATION_Z, (float) norm.z);
        syncRotationFromOrientation(norm);
    }

    /**
     * 発射時の初速と弾軸姿勢を初期化します。
     *
     * @param motion 初速ベクトル [blocks/tick]
     */
    public void setInitialMovement(Vec3 motion) {
        this.setDeltaMovement(motion);
        if (motion.lengthSqr() > 1.0E-6) {
            setOrientation(motion.normalize());
        }
    }

    @Override
    public void setDeltaMovement(Vec3 motion) {
        super.setDeltaMovement(motion);
        // 初期状態など orientation が未設定の場合は初速ベクトルに合わせる
        float ox = this.entityData.get(ORIENTATION_X);
        float oy = this.entityData.get(ORIENTATION_Y);
        float oz = this.entityData.get(ORIENTATION_Z);
        if (ox * ox + oy * oy + oz * oz < 1.0E-6 && motion.lengthSqr() > 1.0E-6) {
            setOrientation(motion.normalize());
        }
    }

    /**
     * 弾軸姿勢ベクトルから Minecraft の Yaw / Pitch 回転角を同期します。
     */
    protected void syncRotationFromOrientation(Vec3 dir) {
        double horizDist = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        float targetYaw = (float) (Mth.atan2(dir.x, dir.z) * (180.0 / Math.PI));
        float targetPitch = (float) (Mth.atan2(dir.y, horizDist) * (180.0 / Math.PI));
        this.setYRot(targetYaw);
        this.setXRot(targetPitch);
    }

    /**
     * 進行方向先読みチャンク計算用の速度スケール係数を返します。
     */
    protected double getChunkLeadFactor() {
        return 8.0;
    }

    /**
     * 飛翔体周辺および進行方向先読みチャンクの動的チケット管理（未ロード領域突入によるフリーズ防止）
     */
    protected void updateChunkLoading() {
        if (!this.chunkLoadingEnabled || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        ChunkPos currentChunk = new ChunkPos(this.blockPosition());
        Vec3 motion = this.getDeltaMovement();
        ChunkPos leadChunk = new ChunkPos(BlockPos.containing(this.position().add(motion.scale(getChunkLeadFactor()))));

        Set<ChunkPos> desiredChunks = new HashSet<>();
        desiredChunks.add(currentChunk);
        desiredChunks.add(leadChunk);

        Iterator<ChunkPos> it = this.activeChunkTickets.iterator();
        while (it.hasNext()) {
            ChunkPos pos = it.next();
            if (!desiredChunks.contains(pos)) {
                serverLevel.getChunkSource().removeRegionTicket(PROJECTILE_CHUNK_TICKET, pos, CHUNK_LOAD_RADIUS, this.getUUID());
                it.remove();
            }
        }

        boolean periodicRefresh = (this.flightTicks % 20 == 0);
        for (ChunkPos pos : desiredChunks) {
            if (periodicRefresh || !this.activeChunkTickets.contains(pos)) {
                serverLevel.getChunkSource().addRegionTicket(PROJECTILE_CHUNK_TICKET, pos, CHUNK_LOAD_RADIUS, this.getUUID());
                this.activeChunkTickets.add(pos);
            }
        }
    }

    /**
     * 付与した全てのチャンクチケットを確実に解放・クリーンアップします。
     */
    public void clearChunkTickets() {
        if (!this.activeChunkTickets.isEmpty() && this.level() instanceof ServerLevel serverLevel) {
            for (ChunkPos pos : this.activeChunkTickets) {
                serverLevel.getChunkSource().removeRegionTicket(PROJECTILE_CHUNK_TICKET, pos, CHUNK_LOAD_RADIUS, this.getUUID());
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
    public void tick() {
        this.baseTick();

        if (!this.level().isClientSide) {
            updateChunkLoading();

            // 初回Tick: テレメトリセッション開始
            if (this.flightTicks == 0) {
                FlightTelemetryLogger.startSession(
                        this,
                        this.position(),
                        this.getVelocityMetersPerSecond(),
                        this.getOrientation(),
                        this.getXRot(),
                        this.getYRot()
                );
            }

            // サーバー側: BallisticsEngine による外弾道物理シミュレーション (1tick = 0.05s)
            BallisticsEngine.StepResult result = BallisticsEngine.updateFlight(this, 0.05);
            this.flightTicks++;

            // 1Tickテレメトリデータの記録
            FlightTelemetryLogger.recordTick(
                    this.getUUID(),
                    this.flightTicks,
                    this.position(),
                    this.getVelocityMetersPerSecond(),
                    this.getOrientation(),
                    this.getXRot(),
                    this.getYRot(),
                    result.airDensity(),
                    result.thrustNewtons(),
                    result.dragNewtons(),
                    result.liftNewtons(),
                    ""
            );

            // 移動ベクトルに沿った衝突判定（レイキャスト）
            HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hitResult.getType() != HitResult.Type.MISS) {
                this.onHit(hitResult);
                return;
            }
        } else {
            // クライアント側: サーバー同期速度による移動と回転補間
            Vec3 motion = this.getDeltaMovement();
            this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
            syncRotationFromOrientation(getOrientation());
        }

        this.checkInsideBlocks();
        this.hasImpulse = true;

        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide) {
            String targetName = result.getEntity().getName().getString();
            FlightTelemetryLogger.endSession(
                    this.getUUID(),
                    "HIT_ENTITY [" + targetName + "]",
                    result.getLocation()
            );
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide) {
            String blockInfo = result.getBlockPos().toShortString();
            FlightTelemetryLogger.endSession(
                    this.getUUID(),
                    "HIT_BLOCK [" + blockInfo + "]",
                    result.getLocation()
            );
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        clearChunkTickets();
        if (!this.level().isClientSide) {
            FlightTelemetryLogger.endSession(this.getUUID(), "REMOVED_" + reason.name(), this.position());
        }
        super.remove(reason);
    }

    @Override
    public void onRemovedFromWorld() {
        clearChunkTickets();
        if (!this.level().isClientSide) {
            FlightTelemetryLogger.endSession(this.getUUID(), "REMOVED_FROM_WORLD", this.position());
        }
        super.onRemovedFromWorld();
    }

    @Override
    protected float getGravity() {
        // 重力加速度は BallisticsEngine 側で計算するため Vanilla の重力二重加算を防止
        return 0.0F;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        Vec3 ori = getOrientation();
        tag.putDouble("OriX", ori.x);
        tag.putDouble("OriY", ori.y);
        tag.putDouble("OriZ", ori.z);
        tag.putInt("FlightTicks", this.flightTicks);
        tag.putBoolean("ChunkLoadingEnabled", this.chunkLoadingEnabled);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("OriX") && tag.contains("OriY") && tag.contains("OriZ")) {
            setOrientation(new Vec3(tag.getDouble("OriX"), tag.getDouble("OriY"), tag.getDouble("OriZ")));
        }
        this.flightTicks = tag.getInt("FlightTicks");
        if (tag.contains("ChunkLoadingEnabled")) {
            this.chunkLoadingEnabled = tag.getBoolean("ChunkLoadingEnabled");
        }
    }
}
