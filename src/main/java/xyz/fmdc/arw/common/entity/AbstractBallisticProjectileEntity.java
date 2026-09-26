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
import xyz.fmdc.arw.api.projectile.virtual.VirtualProjectile;
import xyz.fmdc.arw.common.projectile.virtual.VirtualProjectileManager;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

/**
 * {@link IBallisticProjectile} を実装し、{@link BallisticsEngine} による現実的な外弾道計算を行う
 * 全ての飛翔体（砲弾・ミサイル等）の基底抽象Entity。
 *
 * @deprecated サーバー負荷軽減および強制チャンクロードクラッシュの根本解決のため、
 * 新アーキテクチャ {@link VirtualProjectile} および {@link VirtualProjectileManager} への移行が推奨されます。
 * 本クラスでの強制チャンクロード（Ticket発行）はデフォルトで無効化されています。
 */
@Deprecated
public abstract class AbstractBallisticProjectileEntity extends ThrowableProjectile implements IBallisticProjectile {

    /** 弾道飛翔体用チャンクロードチケット定義（タイムアウト60ticks = 3秒のセーフティ付き） */
    public static final TicketType<UUID> PROJECTILE_CHUNK_TICKET =
            TicketType.create("arw_ballistic_projectile", UUID::compareTo, 60);
    protected static final int CHUNK_LOAD_RADIUS = 2; // 半径2 -> 中心チャンクのチケットレベル31（ENTITY_TICKING）

    /** 旧方式の強制チャンクロードはクラッシュ防止のためデフォルト無効 */
    protected boolean chunkLoadingEnabled = false;
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
        // DeltaMovement は blocks/tick なので、20倍して m/s に換算
        return this.getDeltaMovement().scale(20.0);
    }

    @Override
    public void setVelocityMetersPerSecond(Vec3 velocity) {
        // m/s を blocks/tick に換算して DeltaMovement に設定
        this.setDeltaMovement(velocity.scale(1.0 / 20.0));
    }

    @Override
    public Vec3 getOrientation() {
        return new Vec3(
                this.entityData.get(ORIENTATION_X),
                this.entityData.get(ORIENTATION_Y),
                this.entityData.get(ORIENTATION_Z)
        );
    }

    @Override
    public void setOrientation(Vec3 orientation) {
        Vec3 norm = orientation.normalize();
        this.entityData.set(ORIENTATION_X, (float) norm.x);
        this.entityData.set(ORIENTATION_Y, (float) norm.y);
        this.entityData.set(ORIENTATION_Z, (float) norm.z);

        // 姿勢ベクトルからピッチとヨーを計算して同期
        double pitch = Math.toDegrees(Math.asin(-norm.y));
        double yaw = Math.toDegrees(Math.atan2(-norm.x, norm.z));
        this.setXRot((float) pitch);
        this.setYRot((float) yaw);
        this.xRotO = (float) pitch;
        this.yRotO = (float) yaw;
    }

    public void setInitialMovement(Vec3 motion) {
        this.setDeltaMovement(motion);
        if (motion.lengthSqr() > 1.0E-6) {
            setOrientation(motion.normalize());
        }
    }

    /**
     * 進行方向前方のチャンクを動的にロードし、未ロード領域突入によるフリーズ・デッドロックを防止します。
     */
    protected void updateChunkLoading() {
        if (!this.chunkLoadingEnabled || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 pos = this.position();
        Vec3 vel = this.getDeltaMovement();

        // 現在位置と次Tickの予測位置のチャンク座標を計算
        ChunkPos currentChunk = new ChunkPos(BlockPos.containing(pos));
        ChunkPos nextChunk = new ChunkPos(BlockPos.containing(pos.add(vel)));

        Set<ChunkPos> desiredChunks = new HashSet<>();
        desiredChunks.add(currentChunk);
        desiredChunks.add(nextChunk);

        // 新規に必要なチャンクにチケットを発行
        for (ChunkPos target : desiredChunks) {
            if (!this.activeChunkTickets.contains(target)) {
                serverLevel.getChunkSource().addRegionTicket(
                        PROJECTILE_CHUNK_TICKET,
                        target,
                        CHUNK_LOAD_RADIUS,
                        this.getUUID()
                );
                this.activeChunkTickets.add(target);
            }
        }

        // 不要になった古いチャンクチケットを返還
        Iterator<ChunkPos> it = this.activeChunkTickets.iterator();
        while (it.hasNext()) {
            ChunkPos posInSet = it.next();
            if (!desiredChunks.contains(posInSet)) {
                serverLevel.getChunkSource().removeRegionTicket(
                        PROJECTILE_CHUNK_TICKET,
                        posInSet,
                        CHUNK_LOAD_RADIUS,
                        this.getUUID()
                );
                it.remove();
            }
        }
    }

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
            }
        }

        // 姿勢の補間更新
        this.updateRotation();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            // テレメトリセッションの終了
            FlightTelemetryLogger.endSession(this.getUUID(), result.getType().name(), result.getLocation());
            clearChunkTickets();
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        clearChunkTickets();
        super.remove(reason);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("FlightTicks", this.flightTicks);
        tag.putDouble("OrientationX", this.entityData.get(ORIENTATION_X));
        tag.putDouble("OrientationY", this.entityData.get(ORIENTATION_Y));
        tag.putDouble("OrientationZ", this.entityData.get(ORIENTATION_Z));
        tag.putBoolean("ChunkLoadingEnabled", this.chunkLoadingEnabled);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.flightTicks = tag.getInt("FlightTicks");
        if (tag.contains("OrientationX")) {
            this.entityData.set(ORIENTATION_X, (float) tag.getDouble("OrientationX"));
            this.entityData.set(ORIENTATION_Y, (float) tag.getDouble("OrientationY"));
            this.entityData.set(ORIENTATION_Z, (float) tag.getDouble("OrientationZ"));
        }
        if (tag.contains("ChunkLoadingEnabled")) {
            this.chunkLoadingEnabled = tag.getBoolean("ChunkLoadingEnabled");
        }
    }
}
