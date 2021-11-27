package xyz.fmdc.arw

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.SidedProxy
import cpw.mods.fml.common.event.FMLInitializationEvent
import cpw.mods.fml.common.event.FMLPreInitializationEvent
import cpw.mods.fml.common.event.FMLServerStartingEvent
import cpw.mods.fml.relauncher.Side
import net.minecraft.creativetab.CreativeTabs
import xyz.fmdc.arw.ARWMod.Companion.DOMAIN
import xyz.fmdc.arw.ARWMod.Companion.ModName
import xyz.fmdc.arw.modcore.ARWCreativeTab
import xyz.fmdc.arw.modcore.WorldEventListener
import xyz.fmdc.arw.modcore.proxy.AWMProxy
import xyz.fmdc.arw.registry.RegistryBlockAndTileEntity

@Mod(modid = DOMAIN, name = ModName)
class ARWMod {
    companion object {
        const val DOMAIN = "arw"
        const val ModName = "AntiRaidWeaponMod"

        @Mod.Instance(ModName)
        lateinit var instance: ARWMod

        @SidedProxy(
            clientSide = "xyz.fmdc.arw.modcore.proxy.AWMClientProxy",
            serverSide = "xyz.fmdc.arw.modcore.proxy.AWMCommonProxy",
        )
        lateinit var proxy: AWMProxy

        /**
         * クリエイティブタブ
         * */
        val arwTabs: CreativeTabs = ARWCreativeTab()
    }

    /**
     * ブロック類の登録     *
     * TileEntity類の登録
     */
    @Mod.EventHandler
    fun preInit(event: FMLPreInitializationEvent) {
        RegistryBlockAndTileEntity.registerBlockAndTileEntity()
    }

    /**
     */
    @Mod.EventHandler
    @Suppress("UNUSED_PARAMETER")
    fun init(event: FMLInitializationEvent) {
        if (FMLCommonHandler.instance().side == Side.CLIENT) {
            proxy.callRegisterRenderer()
        }

        FMLCommonHandler.instance().bus().register(WorldEventListener)
    }

    /**
     * コマンド登録
     */
    @Mod.EventHandler
    fun serverLoad(event: FMLServerStartingEvent) {
    }
}
