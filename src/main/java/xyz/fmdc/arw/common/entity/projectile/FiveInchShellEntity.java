package xyz.fmdc.arw.common.entity.projectile;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import xyz.fmdc.arw.AntiRaidWeapons;
import xyz.fmdc.arw.common.entity.AbstractCannonProjectileEntity;
import xyz.fmdc.arw.registry.ModEntities;
import xyz.fmdc.arw.registry.auto.ModItems;

public class FiveInchShellEntity extends AbstractCannonProjectileEntity implements ItemSupplier {

    private static final EntityDataAccessor<Integer> AMMO_ORDINAL =
            SynchedEntityData.defineId(FiveInchShellEntity.class, EntityDataSerializers.INT);

    private int lifeTicks = 0;
    private static final int MAX_LIFE_TICKS = 200; // 10秒で消滅

    public FiveInchShellEntity(EntityType<? extends FiveInchShellEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public FiveInchShellEntity(Level level, double x, double y, double z) {
        this(ModEntities.FIVE_INCH_SHELL.get(), level);
        this.setPos(x, y, z);
    }

    public void setAmmoType(FiveInchAmmoType ammoType) {
        this.entityData.set(AMMO_ORDINAL, ammoType.ordinal());
        updateAmmoProperties(ammoType);
    }

    public FiveInchAmmoType getAmmoType() {
        int ord = this.entityData.get(AMMO_ORDINAL);
        FiveInchAmmoType[] values = FiveInchAmmoType.values();
        return (ord >= 0 && ord < values.length) ? values[ord] : FiveInchAmmoType.MK80_HE_PD;
    }

    private void updateAmmoProperties(FiveInchAmmoType type) {
        this.explosionPower = 3.0F + (type.getExplosiveFillerKg() * 0.5F);
        this.directDamage = 50.0F;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(AMMO_ORDINAL, FiveInchAmmoType.MK80_HE_PD.ordinal());
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(ModItems.getFiveInchShell(getAmmoType()));
    }

    @Override
    public void tick() {
        super.tick();

        this.lifeTicks++;
        if (this.lifeTicks >= MAX_LIFE_TICKS) {
            this.discard();
            return;
        }

        // 飛翔中のトレイル（煙・炎）エフェクト
        if (this.level().isClientSide) {
            Vec3 pos = this.position();
            this.level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.x, pos.y, pos.z, 0, 0.02, 0);
            this.level().addParticle(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 0, 0.01, 0);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide) {
            Entity target = result.getEntity();
            Vec3 hitPos = result.getLocation();
            AntiRaidWeapons.LOGGER.info("FiveInchShell hit entity '{}' (Type: {}) at ({}, {}, {})",
                    target.getName().getString(),
                    target.getType().getDescription().getString(),
                    hitPos.x, hitPos.y, hitPos.z);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide) {
            BlockState blockState = this.level().getBlockState(result.getBlockPos());
            Vec3 hitPos = result.getLocation();
            AntiRaidWeapons.LOGGER.info("FiveInchShell hit block '{}' at ({}, {}, {}) [BlockPos: {}]",
                    blockState.getBlock().getName().getString(),
                    hitPos.x, hitPos.y, hitPos.z,
                    result.getBlockPos());
        }
    }

    @Override
    protected void onHit(HitResult result) {
        if (!this.level().isClientSide) {
            FiveInchAmmoType type = getAmmoType();

            // 砲弾タイプごとの着弾処理分岐
            switch (type.getCategory()) {
                case HE_POINT_DETONATING:
                case HE_PROXIMITY:
                case HE_INFRARED:
                    float power = 3.0F + (type.getExplosiveFillerKg() * 0.5F);
                    this.level().explode(this, this.getX(), this.getY(), this.getZ(), power, Level.ExplosionInteraction.TNT);
                    break;
                case ILLUMINATION:
                    // 照明弾の処理
                    break;
                case CARGO_SUBMUNITION:
                    // 子弾散布処理
                    break;
            }
            this.discard();
        }
        // ログ出力等のため onHitBlock / onHitEntity をトリガーする super.onHit を呼び出す
        // ※ AbstractCannonProjectileEntity.onHit は二重爆発防止のため除外して ThrowableProjectile.onHit の動作を委譲
        if (result.getType() == HitResult.Type.ENTITY) {
            this.onHitEntity((EntityHitResult) result);
        } else if (result.getType() == HitResult.Type.BLOCK) {
            this.onHitBlock((BlockHitResult) result);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("AmmoType", this.entityData.get(AMMO_ORDINAL));
        tag.putInt("LifeTicks", this.lifeTicks);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("AmmoType")) {
            int ammoOrdinal = tag.getInt("AmmoType");
            this.entityData.set(AMMO_ORDINAL, ammoOrdinal);
            FiveInchAmmoType[] values = FiveInchAmmoType.values();
            if (ammoOrdinal >= 0 && ammoOrdinal < values.length) {
                updateAmmoProperties(values[ammoOrdinal]);
            }
        }
        this.lifeTicks = tag.getInt("LifeTicks");
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
