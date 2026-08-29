package xyz.fmdc.arw.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import xyz.fmdc.arw.AntiRaidWeapons;
import xyz.fmdc.arw.registry.ModEntities;

public class NavalShellEntity extends Projectile {

    private static final EntityDataAccessor<Float> DATA_EXPLOSION_POWER =
            SynchedEntityData.defineId(NavalShellEntity.class, EntityDataSerializers.FLOAT);

    private int lifeTicks = 0;
    private static final int MAX_LIFE_TICKS = 200; // 10秒で消滅

    private float directDamage = 50.0f;
    private float gravity = 0.03f;
    private float drag = 0.99f;

    public NavalShellEntity(EntityType<? extends NavalShellEntity> entityType, Level level) {
        super(entityType, level);
        this.noCulling = true;
    }

    public NavalShellEntity(Level level, double x, double y, double z) {
        this(ModEntities.NAVAL_SHELL.get(), level);
        this.setPos(x, y, z);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_EXPLOSION_POWER, 4.0f);
    }

    public void setExplosionPower(float power) {
        this.entityData.set(DATA_EXPLOSION_POWER, power);
    }

    public float getExplosionPower() {
        return this.entityData.get(DATA_EXPLOSION_POWER);
    }

    public void setDirectDamage(float damage) {
        this.directDamage = damage;
    }

    public void setGravity(float gravity) {
        this.gravity = gravity;
    }

    /**
     * 砲口から初速を与えて発射する
     * @param velocity (m/tick)
     */
    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        Vec3 dir = (new Vec3(x, y, z)).normalize().add(
                this.random.triangle(0.0, 0.0172275 * (double) inaccuracy),
                this.random.triangle(0.0, 0.0172275 * (double) inaccuracy),
                this.random.triangle(0.0, 0.0172275 * (double) inaccuracy)
        ).scale(velocity);

        this.setDeltaMovement(dir);
        double horizontalDist = dir.horizontalDistance();
        this.setYRot((float) (Mth.atan2(dir.x, dir.z) * (180F / (float) Math.PI)));
        this.setXRot((float) (Mth.atan2(dir.y, horizontalDist) * (180F / (float) Math.PI)));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    @Override
    public void tick() {
        super.tick();

        this.lifeTicks++;
        if (this.lifeTicks >= MAX_LIFE_TICKS) {
            this.discard();
            return;
        }

        Vec3 movement = this.getDeltaMovement();
        Vec3 currentPos = this.position();
        Vec3 nextPos = currentPos.add(movement);

        // 衝突判定（レイキャスト）
        HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hitResult.getType() != HitResult.Type.MISS) {
            this.onHit(hitResult);
            return;
        }

        // 位置の更新
        this.setPos(nextPos.x, nextPos.y, nextPos.z);

        // 姿勢（回転）の更新
        double horizontalDist = movement.horizontalDistance();
        this.setYRot((float) (Mth.atan2(movement.x, movement.z) * (180F / (float) Math.PI)));
        this.setXRot((float) (Mth.atan2(movement.y, horizontalDist) * (180F / (float) Math.PI)));

        // 曳光・煙パーティクル演出（クライアント側）
        if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            this.level().addParticle(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
        }

        // 重力と空気抵抗の適用
        float currentGravity = this.isInWater() ? this.gravity * 3.0f : this.gravity;
        float currentDrag = this.isInWater() ? 0.8f : this.drag;
        this.setDeltaMovement(movement.scale(currentDrag).subtract(0.0, currentGravity, 0.0));
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide) {
            Vec3 hitPos = result.getLocation();
            Entity target = result.getEntity();
            AntiRaidWeapons.LOGGER.info("NavalShell hit entity '{}' at ({}, {}, {})", target.getName().getString(), hitPos.x, hitPos.y, hitPos.z);

            Entity owner = this.getOwner();
            DamageSource damageSource = owner != null
                    ? this.damageSources().indirectMagic(this, owner)
                    : this.damageSources().generic();

            target.hurt(damageSource, this.directDamage);
            explode(hitPos);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide) {
            Vec3 hitPos = result.getLocation();
            AntiRaidWeapons.LOGGER.info("NavalShell hit block at ({}, {}, {}) [BlockPos: {}]", hitPos.x, hitPos.y, hitPos.z, result.getBlockPos());
            explode(hitPos);
        }
    }

    private void explode(Vec3 hitPos) {
        if (!this.level().isClientSide) {
            float power = getExplosionPower();
            this.level().explode(
                    this,
                    hitPos.x,
                    hitPos.y,
                    hitPos.z,
                    power,
                    Level.ExplosionInteraction.MOB
            );
            this.discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        if (!target.canBeHitByProjectile()) {
            return false;
        }
        Entity owner = this.getOwner();
        return owner == null || !target.isPassengerOfSameVehicle(owner);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("LifeTicks", this.lifeTicks);
        tag.putFloat("ExplosionPower", getExplosionPower());
        tag.putFloat("DirectDamage", this.directDamage);
        tag.putFloat("Gravity", this.gravity);
        tag.putFloat("Drag", this.drag);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.lifeTicks = tag.getInt("LifeTicks");
        if (tag.contains("ExplosionPower")) {
            setExplosionPower(tag.getFloat("ExplosionPower"));
        }
        if (tag.contains("DirectDamage")) {
            this.directDamage = tag.getFloat("DirectDamage");
        }
        if (tag.contains("Gravity")) {
            this.gravity = tag.getFloat("Gravity");
        }
        if (tag.contains("Drag")) {
            this.drag = tag.getFloat("Drag");
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
