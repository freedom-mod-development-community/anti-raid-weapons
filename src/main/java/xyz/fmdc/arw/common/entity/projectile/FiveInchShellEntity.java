package xyz.fmdc.arw.common.entity.projectile;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import xyz.fmdc.arw.registry.ModEntities;
import xyz.fmdc.arw.registry.ModItems;

/**
 * 5インチ砲弾の旧Entity。
 *
 * @deprecated 仮想飛翔体（Virtual Projectile）への完全移行に伴い廃止予定です。
 * 既存セーブデータの読み込み時にクラッシュを防ぐプレースホルダーとして機能し、スポーン/ロード直後に discard() されます。
 */
@Deprecated(forRemoval = true)
public class FiveInchShellEntity extends AbstractCannonProjectileEntity implements ItemSupplier {

    public FiveInchShellEntity(EntityType<? extends FiveInchShellEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public FiveInchShellEntity(Level level, double x, double y, double z) {
        this(ModEntities.FIVE_INCH_SHELL.get(), level);
        this.setPos(x, y, z);
    }

    public void setAmmoType(FiveInchAmmoType ammoType) {
        // ダミー実装
    }

    public FiveInchAmmoType getAmmoType() {
        return FiveInchAmmoType.MK80_HE_PD;
    }

    @Override
    public @NotNull ItemStack getItem() {
        return new ItemStack(ModItems.FIVE_INCH_SHELL_MK80_HE_PD.get());
    }

    @Override
    public void tick() {
        super.tick();
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
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
