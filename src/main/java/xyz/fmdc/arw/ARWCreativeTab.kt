package xyz.fmdc.arw

import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.RegistryObject
import xyz.fmdc.arw.registry.RegistryBlock

object ARWCreativeTab {
    // クリエイティブタブ用のDeferredRegister
    val CREATIVE_MODE_TABS: DeferredRegister<CreativeModeTab> =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ARWMod.DOMAIN)

    // タブの登録定義
    val ARW_TAB: RegistryObject<CreativeModeTab> = CREATIVE_MODE_TABS.register("arw_tab") {
        CreativeModeTab.builder()
            // タブの表示名（言語ファイル ja_jp.json / en_us.json に対応）
            .title(Component.translatable("itemGroup.${ARWMod.DOMAIN}.main_tab"))
            // タブのアイコン（代表的なアイテム/ブロックのItemStackを指定）
            .icon { ItemStack(RegistryBlock.AN_SPS_49.get()) }
            // タブ内に並べるアイテムの登録
            .displayItems { _, output ->
                RegistryBlock.ITEMS.entries.forEach { itemRegistryObject ->
                    output.accept(itemRegistryObject.get())
                }
            }
            .build()
    }
}
