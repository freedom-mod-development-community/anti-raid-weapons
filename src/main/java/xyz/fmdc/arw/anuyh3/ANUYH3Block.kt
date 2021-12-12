package xyz.fmdc.arw.anuyh3

import xyz.fmdc.arw.ARWMod
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalBlockContainer

class ANUYH3Block : ModelNormalBlockContainer(tileEntityClass = ANUYH3Tile::class.java) {
    init {
        setBlockName("an_uyh_3")
        setBlockTextureName(ARWMod.DOMAIN + ":an_uyh_3")
        setBlockBoundsSize(0.7f, 2.05f)
        setSelectedBoundSize(0.7, 2.05)
    }
}
