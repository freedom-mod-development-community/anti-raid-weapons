package xyz.fmdc.arw

import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

class ARWCreativeTab : CreativeTabs(ARWMod.ModName) {

    @SideOnly(Side.CLIENT)
    override fun createIcon(): ItemStack {
        //TODO
        return ItemStack.EMPTY
    }
}

