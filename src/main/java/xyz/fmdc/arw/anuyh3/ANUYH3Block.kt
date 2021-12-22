package xyz.fmdc.arw.anuyh3

import net.minecraft.util.ResourceLocation
import xyz.fmdc.arw.ARWMod
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalBlockContainer

object ANUYH3Block : ModelNormalBlockContainer(tileEntityClass = ANUYH3Tile::class.java) {
    init {
        registryName = ResourceLocation(ARWMod.DOMAIN, "an_uyh_3")
        setBlockBoundsSize(0.7f, 2.05f)
    }
}
