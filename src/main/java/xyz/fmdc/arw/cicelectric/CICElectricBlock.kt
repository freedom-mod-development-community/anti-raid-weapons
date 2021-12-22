package xyz.fmdc.arw.cicelectric

import net.minecraft.util.ResourceLocation
import xyz.fmdc.arw.ARWMod.DOMAIN
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalBlockContainer

object CICElectricBlock : ModelNormalBlockContainer(tileEntityClass = CICElectricTile::class.java) {
    init {
        registryName = ResourceLocation(DOMAIN, "cic_electric")
        setBlockBoundsSize(1f, 1.5f)
    }
}
