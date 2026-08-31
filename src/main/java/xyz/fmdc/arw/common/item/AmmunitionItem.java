package xyz.fmdc.arw.common.item;

import net.minecraft.world.item.Item;

/**
 * 各種砲弾（127mm砲弾、機関砲弾、ミサイルセル等）のベースアイテムクラス
 */
public class AmmunitionItem extends Item {

    private final String caliber; // "127mm", "20mm", "VLS_Cell" など

    public AmmunitionItem(Properties properties, String caliber) {
        super(properties);
        this.caliber = caliber;
    }

    public String getCaliber() {
        return this.caliber;
    }
}