package xyz.fmdc.arw.cicelectric

import xyz.fmdc.arw.ARWMod
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalBlockContainer

object CICElectricBlock : ModelNormalBlockContainer(tileEntityClass = CICElectricTile::class.java) {
    init {
        setBlockName("cic_electric")
        setBlockTextureName(ARWMod.DOMAIN + ":cic_electric")
        setBlockBoundsSize(1f, 1.5f)
        setSelectedBoundSize(1.0, 1.5)
    }
}
