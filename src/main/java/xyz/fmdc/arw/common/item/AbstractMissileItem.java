package xyz.fmdc.arw.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import xyz.fmdc.arw.common.entity.AbstractMissileEntity;

import java.util.List;
import java.util.function.Supplier;

/**
 * ミサイル（誘導弾）アイテムの基底抽象クラス.
 * AmmunitionItem を継承し、各種諸元（口径/直径、全長、重量、炸薬量、最高速度等）
 * およびツールチップ情報、発射対象のミサイルEntityTypeを管理します。
 */
public abstract class AbstractMissileItem extends AmmunitionItem {

    private final Supplier<? extends EntityType<? extends AbstractMissileEntity>> entityTypeSupplier;
    private final double lengthMeters;
    private final double weightKg;
    private final double explosiveFillerKg;
    private final float maxSpeed;

    public AbstractMissileItem(
            Properties properties,
            String caliber,
            Supplier<? extends EntityType<? extends AbstractMissileEntity>> entityTypeSupplier,
            double lengthMeters,
            double weightKg,
            double explosiveFillerKg,
            float maxSpeed
    ) {
        super(properties, caliber);
        this.entityTypeSupplier = entityTypeSupplier;
        this.lengthMeters = lengthMeters;
        this.weightKg = weightKg;
        this.explosiveFillerKg = explosiveFillerKg;
        this.maxSpeed = maxSpeed;
    }

    public EntityType<? extends AbstractMissileEntity> getMissileEntityType() {
        return this.entityTypeSupplier.get();
    }

    public double getLengthMeters() {
        return this.lengthMeters;
    }

    public double getWeightKg() {
        return this.weightKg;
    }

    public double getExplosiveFillerKg() {
        return this.explosiveFillerKg;
    }

    public float getMaxSpeed() {
        return this.maxSpeed;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);

        tooltipComponents.add(Component.translatable("tooltip.arw.caliber", getCaliber())
                .withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tooltip.arw.weight", String.format("%.1f", getWeightKg()))
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltipComponents.add(Component.translatable("tooltip.arw.length", String.format("%.2f", getLengthMeters()))
                .withStyle(ChatFormatting.DARK_GRAY));

        if (getExplosiveFillerKg() > 0) {
            tooltipComponents.add(Component.translatable("tooltip.arw.explosive_filler", String.format("%.1f", getExplosiveFillerKg()))
                    .withStyle(ChatFormatting.RED));
        }

        tooltipComponents.add(Component.translatable("tooltip.arw.max_speed", String.format("%.1f", getMaxSpeed()))
                .withStyle(ChatFormatting.YELLOW));
    }
}
