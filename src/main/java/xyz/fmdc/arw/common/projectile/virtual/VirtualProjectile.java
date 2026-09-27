package xyz.fmdc.arw.common.projectile.virtual;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import xyz.fmdc.arw.api.projectile.virtual.IVirtualProjectile;
import xyz.fmdc.arw.common.projectile.virtual.util.ProjectileRaycastHelper;

import java.util.UUID;
import java.util.function.Predicate;

/**
 * 仮想飛翔体（Virtual Projectile）の基底抽象クラス。
 * サーバーのEntity管理外で動作し、純粋な物理演算とレイキャストによって目標への衝突判定を行います。
 */
public abstract class VirtualProjectile implements IVirtualProjectile {

    protected final UUID projectileId;
    protected final ResourceKey<Level> dimension;
    @Nullable
    protected final UUID ownerUuid;

    protected Vec3 position;
    protected Vec3 prevPosition;
    protected Vec3 velocity; // [m/s]

    protected int ageTicks = 0;
    protected int maxAgeTicks;
    protected boolean isDead = false;

    public VirtualProjectile(
            UUID projectileId,
            ResourceKey<Level> dimension,
            @Nullable UUID ownerUuid,
            Vec3 initialPosition,
            Vec3 initialVelocity,
            int maxAgeTicks
    ) {
        this.projectileId = projectileId != null ? projectileId : UUID.randomUUID();
        this.dimension = dimension;
        this.ownerUuid = ownerUuid;
        this.position = initialPosition;
        this.prevPosition = initialPosition;
        this.velocity = initialVelocity;
        this.maxAgeTicks = maxAgeTicks;
    }

    @Override
    public UUID getProjectileId() {
        return this.projectileId;
    }

    @Override
    public ResourceKey<Level> getDimension() {
        return this.dimension;
    }

    @Nullable
    public UUID getOwnerUuid() {
        return this.ownerUuid;
    }

    @Override
    public Vec3 getPosition() {
        return this.position;
    }

    public void setPosition(Vec3 position) {
        this.position = position;
    }

    @Override
    public Vec3 getPrevPosition() {
        return this.prevPosition;
    }

    @Override
    public Vec3 getVelocity() {
        return this.velocity;
    }

    public void setVelocity(Vec3 velocity) {
        this.velocity = velocity;
    }

    @Override
    public int getAgeTicks() {
        return this.ageTicks;
    }

    @Override
    public int getMaxAgeTicks() {
        return this.maxAgeTicks;
    }

    @Override
    public boolean isDead() {
        return this.isDead;
    }

    @Override
    public void markDead() {
        this.isDead = true;
    }

    @Override
    public void tick(ServerLevel level) {
        if (this.isDead) return;

        this.prevPosition = this.position;

        // 1. 弾道・運動方程式の更新 (サブクラス実装)
        updateMotion(level);

        // 2. 衝突判定 (移動区間 prevPosition -> position)
        if (!this.isDead) {
            checkCollisions(level, this.prevPosition, this.position);
        }

        // 3. 寿命判定
        this.ageTicks++;
        if (this.ageTicks >= this.maxAgeTicks) {
            onExpired(level);
            markDead();
        }
    }

    /**
     * 各兵器固有の運動計算（直進、放物線外弾道、誘導航法など）を実行します。
     */
    protected abstract void updateMotion(ServerLevel level);

    /**
     * 前Tickから現在Tickまでの移動線分について衝突判定を実行します。
     */
    protected void checkCollisions(ServerLevel level, Vec3 from, Vec3 to) {
        HitResult hit = ProjectileRaycastHelper.performRaycast(
                level,
                from,
                to,
                this.ownerUuid,
                getEntityFilter()
        );

        if (hit != null && hit.getType() != HitResult.Type.MISS) {
            onHit(level, hit);
            markDead();
        }
    }

    /** エンティティ判定時の追加フィルタ */
    protected Predicate<Entity> getEntityFilter() {
        return entity -> true;
    }

    /** 寿命到来時のコールバック（空中自爆や安全消失など） */
    protected void onExpired(ServerLevel level) {
        // デフォルトでは何もしない
    }

    /** サーバー停止やアンロード時に母艦へロスト通知を送るためのフック */
    public void onServerStopping() {
        markDead();
    }
}
