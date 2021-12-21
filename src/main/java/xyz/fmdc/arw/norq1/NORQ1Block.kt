package xyz.fmdc.arw.norq1

import net.minecraft.util.ResourceLocation
import xyz.fmdc.arw.ARWMod.DOMAIN
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalBlockContainer

class NORQ1Block : ModelNormalBlockContainer(tileEntityClass = NORQ1Tile::class.java) {
    init {
        registryName = ResourceLocation(DOMAIN, "norq_1")
        setBlockBoundsSize(1.5f, 1.75f)
    }
}
