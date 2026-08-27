package xyz.fmdc.arw

import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import thedarkcolour.kotlinforforge.forge.MOD_BUS
import xyz.fmdc.arw.network.PacketHandlerARW
import xyz.fmdc.arw.registry.RegistryBlock

@Mod(ARWMod.DOMAIN)
object ARWMod {
    const val DOMAIN = "arw"
    const val ModName = "AntiRaidWeaponMod"

    init {
        // 1. 各ブロック・アイテム・TileEntity を DeferredRegister にキューイング

        // 2. レジストリ群を Mod イベントバスへ紐づけ
        RegistryBlock.BLOCKS.register(MOD_BUS)
        RegistryBlock.ITEMS.register(MOD_BUS)
        RegistryBlock.BLOCK_ENTITIES.register(MOD_BUS)
        ARWCreativeTab.CREATIVE_MODE_TABS.register(MOD_BUS)

        // ライフサイクルイベントリスナー
        MOD_BUS.addListener(::onCommonSetup)
    }

    private fun onCommonSetup(event: FMLCommonSetupEvent) {
        event.enqueueWork {
            PacketHandlerARW.register()
        }
    }
}
