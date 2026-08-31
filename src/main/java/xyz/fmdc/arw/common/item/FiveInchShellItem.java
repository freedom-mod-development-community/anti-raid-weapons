package xyz.fmdc.arw.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import xyz.fmdc.arw.common.entity.projectile.FiveInchAmmoType;

import java.util.List;

public class FiveInchShellItem extends AmmunitionItem {

    private final FiveInchAmmoType ammoType;

    public FiveInchShellItem(Properties properties, FiveInchAmmoType ammoType) {
        super(properties, "127mm");
        this.ammoType = ammoType;
    }

    public FiveInchAmmoType getAmmoType() {
        return this.ammoType;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);

        tooltipComponents.add(Component.translatable("tooltip.arw.caliber", getCaliber())
                .withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tooltip.arw.ammo_type", ammoType.getName())
                .withStyle(ChatFormatting.YELLOW));
        tooltipComponents.add(Component.translatable("tooltip.arw.ammo_category." + ammoType.getCategory().getKey())
                .withStyle(ChatFormatting.AQUA));
        tooltipComponents.add(Component.translatable("tooltip.arw.weight", String.format("%.1f", ammoType.getWeightKg()))
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltipComponents.add(Component.translatable("tooltip.arw.length", String.format("%.3f", ammoType.getLengthMeters()))
                .withStyle(ChatFormatting.DARK_GRAY));

        if (ammoType.getExplosiveFillerKg() > 0) {
            tooltipComponents.add(Component.translatable("tooltip.arw.explosive_filler", String.format("%.1f", ammoType.getExplosiveFillerKg()))
                    .withStyle(ChatFormatting.RED));
        }
    }
}
