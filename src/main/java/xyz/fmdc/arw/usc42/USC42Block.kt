package xyz.fmdc.arw.usc42

import xyz.fmdc.arw.ARWMod
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalBlockContainer

class USC42Block : ModelNormalBlockContainer(tileEntityClass = USC42Tile::class.java) {
    init {
        setBlockName("usc_42")
        setBlockTextureName(ARWMod.DOMAIN + ":usc_42")
        setBlockBoundsSize(1.75f, 2.5f)
        setSelectedBoundSize(1.75, 2.5)
    }
}