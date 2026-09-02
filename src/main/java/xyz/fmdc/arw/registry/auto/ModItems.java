package xyz.fmdc.arw.registry.auto;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import xyz.fmdc.arw.AntiRaidWeapons;
import xyz.fmdc.arw.common.entity.projectile.FiveInchAmmoType;
import xyz.fmdc.arw.common.item.FcsConnectorItem;
import xyz.fmdc.arw.common.item.FiveInchShellItem;
import xyz.fmdc.arw.common.item.Rim66m2Item;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, AntiRaidWeapons.MOD_ID);

    // FCS接続用アイテム（木の棒の外見）
    public static final RegistryObject<Item> FCS_CONNECTOR =
            ITEMS.register("fcs_connector", () -> new FcsConnectorItem(new Item.Properties().stacksTo(1)));

    // RIM-66M-2 ミサイルアイテム
    public static final RegistryObject<Item> RIM_66M2 =
            ITEMS.register("rim_66m2", () -> new Rim66m2Item(new Item.Properties().stacksTo(16)));

    // 5インチ砲弾アイテム（各種）
    public static final RegistryObject<Item> FIVE_INCH_SHELL_MK68_HE_CVT =
            ITEMS.register("five_inch_shell_mk68_he_cvt", () -> new FiveInchShellItem(new Item.Properties(), FiveInchAmmoType.MK68_HE_CVT));

    public static final RegistryObject<Item> FIVE_INCH_SHELL_MK80_HE_PD =
            ITEMS.register("five_inch_shell_mk80_he_pd", () -> new FiveInchShellItem(new Item.Properties(), FiveInchAmmoType.MK80_HE_PD));

    public static final RegistryObject<Item> FIVE_INCH_SHELL_MK91_ILLUM_MT =
            ITEMS.register("five_inch_shell_mk91_illum_mt", () -> new FiveInchShellItem(new Item.Properties(), FiveInchAmmoType.MK91_ILLUM_MT));

    public static final RegistryObject<Item> FIVE_INCH_SHELL_MK116_HE_VT =
            ITEMS.register("five_inch_shell_mk116_he_vt", () -> new FiveInchShellItem(new Item.Properties(), FiveInchAmmoType.MK116_HE_VT));

    public static final RegistryObject<Item> FIVE_INCH_SHELL_MK127_HE_CVT =
            ITEMS.register("five_inch_shell_mk127_he_cvt", () -> new FiveInchShellItem(new Item.Properties(), FiveInchAmmoType.MK127_HE_CVT));

    public static final RegistryObject<Item> FIVE_INCH_SHELL_MK156_HE_IR =
            ITEMS.register("five_inch_shell_mk156_he_ir", () -> new FiveInchShellItem(new Item.Properties(), FiveInchAmmoType.MK156_HE_IR));

    public static final RegistryObject<Item> FIVE_INCH_SHELL_MK172_HE_ICM =
            ITEMS.register("five_inch_shell_mk172_he_icm", () -> new FiveInchShellItem(new Item.Properties(), FiveInchAmmoType.MK172_HE_ICM));

    private static final Map<FiveInchAmmoType, RegistryObject<Item>> FIVE_INCH_SHELLS_MAP;

    static {
        Map<FiveInchAmmoType, RegistryObject<Item>> map = new EnumMap<>(FiveInchAmmoType.class);
        map.put(FiveInchAmmoType.MK68_HE_CVT, FIVE_INCH_SHELL_MK68_HE_CVT);
        map.put(FiveInchAmmoType.MK80_HE_PD, FIVE_INCH_SHELL_MK80_HE_PD);
        map.put(FiveInchAmmoType.MK91_ILLUM_MT, FIVE_INCH_SHELL_MK91_ILLUM_MT);
        map.put(FiveInchAmmoType.MK116_HE_VT, FIVE_INCH_SHELL_MK116_HE_VT);
        map.put(FiveInchAmmoType.MK127_HE_CVT, FIVE_INCH_SHELL_MK127_HE_CVT);
        map.put(FiveInchAmmoType.MK156_HE_IR, FIVE_INCH_SHELL_MK156_HE_IR);
        map.put(FiveInchAmmoType.MK172_HE_ICM, FIVE_INCH_SHELL_MK172_HE_ICM);
        FIVE_INCH_SHELLS_MAP = Collections.unmodifiableMap(map);
    }

    public static RegistryObject<Item> getFiveInchShellRegistryObject(FiveInchAmmoType ammoType) {
        return FIVE_INCH_SHELLS_MAP.getOrDefault(ammoType, FIVE_INCH_SHELL_MK80_HE_PD);
    }

    public static Item getFiveInchShell(FiveInchAmmoType ammoType) {
        RegistryObject<Item> regObj = FIVE_INCH_SHELLS_MAP.get(ammoType);
        return regObj != null ? regObj.get() : FIVE_INCH_SHELL_MK80_HE_PD.get();
    }

    // メインModクラスから呼び出す登録用メソッド
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
