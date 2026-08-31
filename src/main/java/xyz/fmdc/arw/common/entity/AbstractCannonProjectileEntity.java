package xyz.fmdc.arw.common.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public abstract class AbstractCannonProjectileEntity extends ThrowableProjectile {

    protected float explosionPower = 4.0f;
    protected float directDamage = 50.0f;

    public AbstractCannonProjectileEntity(EntityType<? extends ThrowableProjectile> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
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
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.explosionPower = tag.getFloat("ExplosionPower");
        this.directDamage = tag.getFloat("DirectDamage");
    }
}