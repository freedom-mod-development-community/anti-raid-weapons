package xyz.fmdc.arw.ansps49

import net.minecraft.util.ResourceLocation
import xyz.fmdc.arw.ARWMod.DOMAIN
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalBlockContainer

object ANSPS49Block : ModelNormalBlockContainer(tileEntityClass = ANSPS49Tile::class.java) {
    init {
        registryName = ResourceLocation(DOMAIN, "an_sps_49")
        translationKey = "an_sps_49"
        setBlockBoundsSize(3.0f, 5.0f)
    }
}
