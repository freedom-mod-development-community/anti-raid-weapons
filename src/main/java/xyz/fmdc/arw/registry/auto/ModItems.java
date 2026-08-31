package xyz.fmdc.arw.registry.auto;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import xyz.fmdc.arw.AntiRaidWeapons;
import xyz.fmdc.arw.common.item.FcsConnectorItem;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, AntiRaidWeapons.MOD_ID);

    // FCS接続用アイテム（木の棒の外見）
    public static final RegistryObject<Item> FCS_CONNECTOR =
            ITEMS.register("fcs_connector", () -> new FcsConnectorItem(new Item.Properties().stacksTo(1)));

    // メインModクラスから呼び出す登録用メソッド
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
