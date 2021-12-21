package xyz.fmdc.arw.ops39

import net.minecraft.util.ResourceLocation
import xyz.fmdc.arw.ARWMod.DOMAIN
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalBlockContainer

class OPS39Block : ModelNormalBlockContainer(tileEntityClass = OPS39Tile::class.java) {
    init {
        registryName = ResourceLocation(DOMAIN, "ops_39")
        setBlockBoundsSize(1.5f, 2.5f)
    }
}
