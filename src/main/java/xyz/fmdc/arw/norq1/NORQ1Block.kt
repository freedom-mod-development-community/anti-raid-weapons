package xyz.fmdc.arw.norq1

import xyz.fmdc.arw.ARWMod
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalBlockContainer

class NORQ1Block : ModelNormalBlockContainer(tileEntityClass = NORQ1Tile::class.java) {
    init {
        setBlockName("norq_1")
        setBlockTextureName(ARWMod.DOMAIN + ":norq_1")
        setBlockBoundsSize(1.75f, 2.5f)
        setSelectedBoundSize(1.75, 2.5)
    }
}
