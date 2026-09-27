package xyz.fmdc.arw.common.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import xyz.fmdc.arw.api.projectile.IBallisticProjectile;

/**
 * 外弾道シミュレーション用エンティティの旧基底クラス。
 *
 * @deprecated 仮想飛翔体（Virtual Projectile）への完全移行に伴い廃止予定です。
 * 既存セーブデータの読み込み時にクラッシュを防ぐプレースホルダーとして機能し、スポーン/ロード直後に discard() されます。
 */
@Deprecated(forRemoval = true)
public abstract class AbstractBallisticProjectileEntity extends ThrowableProjectile implements IBallisticProjectile {

    public AbstractBallisticProjectileEntity(EntityType<? extends ThrowableProjectile> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    @Override
    protected void defineSynchedData() {
        // セーフガードプレースホルダーのため同期データは定義しない
    }

    @Override
    public void tick() {
        // スポーン/ロード直後に安全に消滅
        this.discard();
    }

    // --- IBallisticProjectile のダミー実装（安全なデフォルト値を返却） ---

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
        return Vec3.ZERO;
    }

    @Override
    public void setVelocityMetersPerSecond(Vec3 velocity) {
        this.setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public Vec3 getOrientation() {
        return new Vec3(0, 0, 1);
    }

    @Override
    public void setOrientation(Vec3 orientation) {
    }

    public void setInitialMovement(Vec3 motion) {
        this.setDeltaMovement(motion);
    }

    public void clearChunkTickets() {
        // チケット発行は行わないため何もしない
    }

    public boolean isChunkLoadingEnabled() {
        return false;
    }

    public void setChunkLoadingEnabled(boolean enabled) {
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
