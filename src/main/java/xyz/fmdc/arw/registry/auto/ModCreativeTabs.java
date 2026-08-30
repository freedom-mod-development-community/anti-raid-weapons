package xyz.fmdc.arw.registry.auto;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import xyz.fmdc.arw.AntiRaidWeapons;
import xyz.fmdc.arw.registry.ModBlocks;

public class ModCreativeTabs {
    // CreativeModeTab 用の DeferredRegister
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AntiRaidWeapons.MOD_ID);

    // 自作タブの登録
    public static final RegistryObject<CreativeModeTab> MAIN_TAB =
            CREATIVE_MODE_TABS.register("main_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("creativetab." + AntiRaidWeapons.MOD_ID + ".main"))
                    .icon(() -> new ItemStack(ModBlocks.OPS39.getBlock())) // タブのアイコン
                    .displayItems((parameters, output) -> {
                        // 1. ModBlocks に登録されている全ブロック（BlockItem）を自動追加
                        ModBlocks.BLOCKS.getEntries().stream()
                                .map(RegistryObject::get)
                                .forEach(output::accept);

                        // 2. ModItems に登録されている全アイテムを自動追加
                        ModItems.ITEMS.getEntries().stream()
                                .map(RegistryObject::get)
                                .forEach(output::accept);
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}