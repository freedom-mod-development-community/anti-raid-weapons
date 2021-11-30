package xyz.fmdc.arw

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.SidedProxy
import cpw.mods.fml.common.event.FMLInitializationEvent
import cpw.mods.fml.common.event.FMLPreInitializationEvent
import cpw.mods.fml.common.event.FMLServerStartingEvent
import cpw.mods.fml.relauncher.Side
import net.minecraft.creativetab.CreativeTabs
import xyz.fmdc.arw.ARWMod.DOMAIN
import xyz.fmdc.arw.ARWMod.ModName
import xyz.fmdc.arw.network.PacketHandlerARW
import xyz.fmdc.arw.proxy.AWMProxy
import xyz.fmdc.arw.registry.RegistryBlock

@Mod(modid = DOMAIN, name = ModName)
object ARWMod {
    const val DOMAIN = "arw"
    const val ModName = "AntiRaidWeaponMod"

    @Mod.InstanceFactory
    @JvmStatic
    fun instance() = this

    @SidedProxy(
        clientSide = "xyz.fmdc.arw.proxy.AWMClientProxy",
        serverSide = "xyz.fmdc.arw.proxy.AWMCommonProxy",
    )
    lateinit var proxy: AWMProxy

    /**
     * クリエイティブタブ
     * */
    val arwTabs: CreativeTabs = ARWCreativeTab()

    /**
     * ブロック類の登録     *
     * TileEntity類の登録
     */
    @Mod.EventHandler
    @Suppress("UNUSED_PARAMETER")
    fun preInit(event: FMLPreInitializationEvent) {
        RegistryBlock.registerBlock()
        PacketHandlerARW.init()
    }

    /**
     */
    @Mod.EventHandler
    @Suppress("UNUSED_PARAMETER")
    fun init(event: FMLInitializationEvent) {
        if (FMLCommonHandler.instance().side == Side.CLIENT) {
            proxy.callRegisterRenderer()
        }
    }

    /**
     * コマンド登録
     */
    @Mod.EventHandler
    @Suppress("UNUSED_PARAMETER")
    fun serverLoad(event: FMLServerStartingEvent) {
    }
}
