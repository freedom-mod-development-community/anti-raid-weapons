package xyz.fmdc.arw.common.entity.missile;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import xyz.fmdc.arw.registry.ModEntities;

/**
 * RIM-66M-2 ミサイルの旧Entity。
 *
 * @deprecated 仮想飛翔体（Virtual Projectile）への完全移行に伴い廃止予定です。
 * 既存セーブデータの読み込み時にクラッシュを防ぐプレースホルダーとして機能し、スポーン/ロード直後に discard() されます。
 */
@Deprecated(forRemoval = true)
public class Rim66M2 extends AbstractMissileEntity {

    public Rim66M2(EntityType<? extends Rim66M2> type, Level level) {
        super(type, level);
    }

    public Rim66M2(Level level, double x, double y, double z) {
        this(ModEntities.RIM_66M2.get(), level);
        this.setPos(x, y, z);
    }

    public Rim66M2(Level level, double x, double y, double z, Vec3 motion) {
        this(level, x, y, z);
    }

    @Override
    public void tick() {
        super.tick();
        this.discard();
    }
}
