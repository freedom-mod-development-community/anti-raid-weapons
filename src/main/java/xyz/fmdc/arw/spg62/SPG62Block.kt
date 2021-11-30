package xyz.fmdc.arw.spg62

import xyz.fmdc.arw.ARWMod
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalBlockContainer

class SPG62Block : ModelNormalBlockContainer(tileEntityClass = SPG62Tile::class.java) {
    init {
        setBlockName("spg62")
        setBlockTextureName(ARWMod.DOMAIN + ":spg_62")
        setBlockBoundsSize(2f, 3f)
        setSelectedBoundSize(2.0, 3.0)
    }
}
