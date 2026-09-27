package xyz.fmdc.arw.common.entity.projectile;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import xyz.fmdc.arw.common.entity.AbstractBallisticProjectileEntity;

/**
 * 砲弾の旧基底抽象Entity。
 *
 * @deprecated 仮想飛翔体（Virtual Projectile）への完全移行に伴い廃止予定です。
 * 既存セーブデータの読み込み時にクラッシュを防ぐプレースホルダーとして機能し、スポーン/ロード直後に discard() されます。
 */
@Deprecated(forRemoval = true)
public abstract class AbstractCannonProjectileEntity extends AbstractBallisticProjectileEntity {

    protected float explosionPower = 4.0f;
    protected float directDamage = 50.0f;
    protected double diameter = 0.127;
    protected double length = 0.8;
    protected double mass = 31.75;

    public AbstractCannonProjectileEntity(EntityType<? extends ThrowableProjectile> type, Level level) {
        super(type, level);
    }

    // --- IBallisticProjectile ダミー実装 ---

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
        return 0.18;
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
        return 12.0;
    }

    @Override
    public double getThrustNewtons() {
        return 0.0;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        // NBTデータは保存しない
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // 既存のセーブデータNBTは安全に無視
    }
}
