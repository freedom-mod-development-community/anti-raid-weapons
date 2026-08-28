package xyz.fmdc.arw.registry;

import net.minecraftforge.eventbus.api.IEventBus;
import xyz.fmdc.arw.AntiRaidWeapons;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, AntiRaidWeapons.MOD_ID);

    // メインModクラスから呼び出す登録用メソッド
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
