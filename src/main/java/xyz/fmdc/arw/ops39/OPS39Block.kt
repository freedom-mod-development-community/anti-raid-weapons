package xyz.fmdc.arw.ops39

import xyz.fmdc.arw.ARWMod
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalBlockContainer

class OPS39Block : ModelNormalBlockContainer(tileEntityClass = OPS39Tile::class.java) {
    init {
        setBlockName("ops_39")
        setBlockTextureName(ARWMod.DOMAIN + ":ops_39")
        setBlockBoundsSize(1.5f, 2.5f)
        setSelectedBoundSize(1.5, 2.5)
    }
}