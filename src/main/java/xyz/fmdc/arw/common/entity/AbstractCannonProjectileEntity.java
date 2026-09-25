package xyz.fmdc.arw.common.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

/**
 * 艦砲・野砲等の通常砲弾エンティティの基底抽象クラス。
 * 飛翔中の弾道計算、アンロード領域通過時のチャンクロード管理、着弾爆発、ダメージ処理を統括します。
 */
public abstract class AbstractCannonProjectileEntity extends ThrowableProjectile {

    /** 砲弾用チャンクロードチケット定義（タイムアウト60ticks = 3秒の自動失効セーフティ付き） */
    public static final TicketType<UUID> SHELL_CHUNK_TICKET =
            TicketType.create("arw_cannon_shell", UUID::compareTo, 60);

    /** ロード半径（半径2 -> 中心チャンクのチケットレベル31：ENTITY_TICKINGでエンティティの更新を維持） */
    protected static final int CHUNK_LOAD_RADIUS = 2;

    protected boolean chunkLoadingEnabled = true;
    private final Set<ChunkPos> activeChunkTickets = new HashSet<>();

    protected float explosionPower = 4.0f;
    protected float directDamage = 50.0f;

    public AbstractCannonProjectileEntity(EntityType<? extends ThrowableProjectile> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    public void tick() {
        super.tick();

        // サーバー側での飛翔中チャンクロード管理
        if (!this.level().isClientSide) {
            updateChunkLoading();
        }
    }

    /**
     * 砲弾周辺および進行方向先読みチャンクの動的チケット管理。
     * 未ロード領域突入による弾丸の空中停止・消失を防止しつつ、通過済みのチャンクは即座に解放します。
     */
    protected void updateChunkLoading() {
        if (!this.chunkLoadingEnabled || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        ChunkPos currentChunk = new ChunkPos(this.blockPosition());
        Vec3 motion = this.getDeltaMovement();
        // 飛翔方向の先読みチャンク（高速移動時に突入先を事前ロード）
        ChunkPos leadChunk = new ChunkPos(BlockPos.containing(this.position().add(motion.scale(8.0))));

        Set<ChunkPos> desiredChunks = new HashSet<>();
        desiredChunks.add(currentChunk);
        desiredChunks.add(leadChunk);

        // 不要になった過去のチャンクチケットを解除
        Iterator<ChunkPos> it = this.activeChunkTickets.iterator();
        while (it.hasNext()) {
            ChunkPos pos = it.next();
            if (!desiredChunks.contains(pos)) {
                serverLevel.getChunkSource().removeRegionTicket(SHELL_CHUNK_TICKET, pos, CHUNK_LOAD_RADIUS, this.getUUID());
                it.remove();
            }
        }

        // 必要なチャンクにチケットを追加・更新（20tickごと、または新規チャンク突入時）
        boolean periodicRefresh = (this.tickCount % 20 == 0);
        for (ChunkPos pos : desiredChunks) {
            if (periodicRefresh || !this.activeChunkTickets.contains(pos)) {
                serverLevel.getChunkSource().addRegionTicket(SHELL_CHUNK_TICKET, pos, CHUNK_LOAD_RADIUS, this.getUUID());
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
                serverLevel.getChunkSource().removeRegionTicket(SHELL_CHUNK_TICKET, pos, CHUNK_LOAD_RADIUS, this.getUUID());
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

    public Set<ChunkPos> getActiveChunkTickets() {
        return Collections.unmodifiableSet(this.activeChunkTickets);
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

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            clearChunkTickets();
            // 爆発処理およびダメージ付与
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), this.explosionPower, Level.ExplosionInteraction.TNT);
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult entityHitResult) {
        super.onHitEntity(entityHitResult);
        if (!this.level().isClientSide) {
            entityHitResult.getEntity().hurt(this.damageSources().thrown(this, getOwner()), this.directDamage);
        }
    }

    @Override
    protected float getGravity() {
        return 0.03f; // 砲弾の放物線描画用重力
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("ExplosionPower", this.explosionPower);
        tag.putFloat("DirectDamage", this.directDamage);
        tag.putBoolean("ChunkLoadingEnabled", this.chunkLoadingEnabled);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.explosionPower = tag.getFloat("ExplosionPower");
        this.directDamage = tag.getFloat("DirectDamage");
        if (tag.contains("ChunkLoadingEnabled")) {
            this.chunkLoadingEnabled = tag.getBoolean("ChunkLoadingEnabled");
        }
    }
}
