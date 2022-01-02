package xyz.fmdc.arw.ansps49

import xyz.fmdc.arw.ARWMod
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalBlockContainer

object ANSPS49Block : ModelNormalBlockContainer(tileEntityClass = ANSPS49Tile::class.java) {
    init {
        setBlockName("an_sps_49")
        setBlockTextureName(ARWMod.DOMAIN + ":an_sps_49")
        setBlockBoundsSize(3.0f, 5.0f)
        setSelectedBoundSize(3.0, 5.0)
    }
}
