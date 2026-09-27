package xyz.fmdc.arw.common.entity.missile;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import xyz.fmdc.arw.common.entity.AbstractBallisticProjectileEntity;

/**
 * ミサイルの旧基底抽象クラス。
 *
 * @deprecated 仮想飛翔体（Virtual Projectile）への完全移行に伴い廃止予定です。
 * 既存セーブデータの読み込み時にクラッシュを防ぐプレースホルダーとして機能し、スポーン/ロード直後に discard() されます。
 */
@Deprecated(forRemoval = true)
public abstract class AbstractMissileEntity extends AbstractBallisticProjectileEntity {

    protected float explosionPower = 6.0F;
    protected float directDamage = 100.0F;
    protected float maxSpeed = 60.0F;
    protected float acceleration = 0.8F;
    protected double motorThrustNewtons = 0.0;
    protected double diameter = 0.34;
    protected double length = 4.72;
    protected double mass = 708.0;
    protected double dragCoefficientZero = 0.25;
    protected double sideDragCoefficient = 1.20;
    protected double liftSlope = 2.5;
    protected double stabilityFactor = 4.0;
    protected float turnRate = 0.08F;
    protected int motorBurnTicks = 100;
    protected double proximityFuseRadius = 2.5D;
    protected int proximityFuseArmTicks = 15;
    protected int lifeTicks = 0;
    protected int maxLifeTicks = 300;

    public AbstractMissileEntity(EntityType<? extends ThrowableProjectile> type, Level level) {
        super(type, level);
        this.noCulling = true;
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
        return 0.0;
    }

    public boolean isMotorBurning() {
        return false;
    }

    public void setMotorBurning(boolean burning) {
    }

    public void setTargetPos(@Nullable Vec3 targetPos) {
    }

    @Nullable
    public Vec3 getTargetPos() {
        return null;
    }

    public void setTargetEntity(@Nullable Entity target) {
    }

    @Nullable
    public Entity getTargetEntity() {
        return null;
    }

    @Override
    public void tick() {
        super.tick();
        this.discard();
    }

    public void explode() {
        this.discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        // NBTデータは保存しない
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // 既存のセーブデータNBTは安全に無視
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
