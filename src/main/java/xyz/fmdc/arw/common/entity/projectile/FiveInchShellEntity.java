package xyz.fmdc.arw.common.entity.projectile;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class FiveInchShellEntity extends ThrowableItemProjectile {

    private static final EntityDataAccessor<Integer> AMMO_ORDINAL =
            SynchedEntityData.defineId(FiveInchShellEntity.class, EntityDataSerializers.INT);

    public FiveInchShellEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
    }

    public void setAmmoType(FiveInchAmmoType ammoType) {
        this.entityData.set(AMMO_ORDINAL, ammoType.ordinal());
    }

    public FiveInchAmmoType getAmmoType() {
        int ord = this.entityData.get(AMMO_ORDINAL);
        FiveInchAmmoType[] values = FiveInchAmmoType.values();
        return (ord >= 0 && ord < values.length) ? values[ord] : FiveInchAmmoType.MK80_HE_PD;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(AMMO_ORDINAL, FiveInchAmmoType.MK80_HE_PD.ordinal());
    }

    @Override
    protected Item getDefaultItem() {
        return Items.FIRE_CHARGE; // 仮の描画用アイテム（ModItemがあれば差し替え）
    }

    @Override
    public void tick() {
        super.tick();

        // 飛翔中のトレイル（煙・炎）エフェクト
        if (this.level().isClientSide) {
            Vec3 pos = this.position();
            this.level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.x, pos.y, pos.z, 0, 0.02, 0);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            FiveInchAmmoType type = getAmmoType();

            // 砲弾タイプごとの着弾処理分岐
            switch (type.getCategory()) {
                case HE_POINT_DETONATING:
                case HE_PROXIMITY:
                case HE_INFRARED:
                    // 炸薬量に応じた爆発威力（3.7kg -> 爆発半径換算）
                    float power = 3.0F + (type.getExplosiveFillerKg() * 0.5F);
                    this.level().explode(this, this.getX(), this.getY(), this.getZ(), power, Level.ExplosionInteraction.TNT);
                    break;
                case ILLUMINATION:
                    // 照明弾の処理（空中で発光パーティクル散布など）
                    break;
                case CARGO_SUBMUNITION:
                    // 子弾散布処理
                    break;
            }
            this.discard();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("AmmoType", this.entityData.get(AMMO_ORDINAL));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("AmmoType")) {
            this.entityData.set(AMMO_ORDINAL, tag.getInt("AmmoType"));
        }
    }
}