package xyz.fmdc.arw.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import xyz.fmdc.arw.registry.ModEntities;

import java.util.List;

/**
 * RIM-66M-2 (SM-2MR Block III) ミサイルアイテム.
 * <p>
 * Mk 13 GMLS 等から装填・発射される中距離艦対空ミサイル。
 */
public class Rim66m2Item extends AbstractMissileItem {

    public Rim66m2Item(Properties properties) {
        super(
                properties,
                "343mm",                   // 直径 / 胴体直径（約13.5インチ = 343mm）
                ModEntities.RIM_66M2::get, // 生成ミサイルエンティティ
                4.72D,                     // 全長: 4.72m
                635.0D,                    // 重量: 約635kg
                62.0D,                     // 炸薬量: 約62kg（Mk 115 爆風破片弾頭相当）
                60.0F                      // 最高速度: 60.0m/tick
        );
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);

        tooltipComponents.add(Component.translatable("tooltip.arw.warhead", "Mk 115 Blast-Fragmentation (HE)")
                .withStyle(ChatFormatting.AQUA));
        tooltipComponents.add(Component.translatable("tooltip.arw.guidance", Component.translatable("tooltip.arw.guidance.sarh"))
                .withStyle(ChatFormatting.GREEN));
    }
}
