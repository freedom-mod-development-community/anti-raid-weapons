package xyz.fmdc.arw.common.entity.projectile;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import xyz.fmdc.arw.common.entity.AbstractBallisticProjectileEntity;

/**
 * 砲弾の基底抽象Entity。
 * 外弾道物理シミュレーションおよびチャンクロード機能は親クラス {@link AbstractBallisticProjectileEntity} により管理されます。
 */
public abstract class AbstractCannonProjectileEntity extends AbstractBallisticProjectileEntity {

    protected float explosionPower = 4.0f;
    protected float directDamage = 50.0f;

    // 基本諸元デフォルト（127mm 砲弾相当）
    protected double diameter = 0.127; // [m]
    protected double length = 0.8;      // [m]
    protected double mass = 31.75;     // [kg]

    public AbstractCannonProjectileEntity(EntityType<? extends ThrowableProjectile> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
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
        return 0.18; // 流線型砲弾の低Cd0
    }

    @Override
    public double getSideDragCoefficient() {
        return 1.15;
    }

    @Override
    public double getLiftSlope() {
        return 1.5;
    }

    @Override
    public double getStabilityFactor() {
        return 12.0; // 高スピン/高安定性により弾軸が素早く進行方向に一致
    }

    @Override
    public double getThrustNewtons() {
        return 0.0; // 砲弾は推力なし
    }

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
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("ExplosionPower", this.explosionPower);
        tag.putFloat("DirectDamage", this.directDamage);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("ExplosionPower")) {
            this.explosionPower = tag.getFloat("ExplosionPower");
        }
        if (tag.contains("DirectDamage")) {
            this.directDamage = tag.getFloat("DirectDamage");
        }
    }
}
