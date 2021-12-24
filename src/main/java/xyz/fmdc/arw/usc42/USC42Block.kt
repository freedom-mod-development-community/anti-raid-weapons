package xyz.fmdc.arw.usc42

import net.minecraft.util.ResourceLocation
import xyz.fmdc.arw.ARWMod
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalBlockContainer

object USC42Block : ModelNormalBlockContainer(tileEntityClass = USC42Tile::class.java) {
    init {
        registryName = ResourceLocation(ARWMod.DOMAIN, "usc_42")
        translationKey = "usc_42"
        setBlockBoundsSize(1.75f, 2.5f)
    }
}
