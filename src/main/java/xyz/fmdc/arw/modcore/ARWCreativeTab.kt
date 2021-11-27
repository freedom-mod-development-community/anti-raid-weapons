package xyz.fmdc.arw.modcore

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.Item
import xyz.fmdc.arw.ARWMod

class ARWCreativeTab : CreativeTabs(ARWMod.ModName) {

    @SideOnly(Side.CLIENT)
    override fun getTabIconItem(): Item {
        //TODO
        return Item()
    }

    @SideOnly(Side.CLIENT)
    override fun getTranslatedTabLabel(): String {
        return ARWMod.ModName
    }

}
