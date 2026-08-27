package xyz.fmdc.arw.orn6e

import xyz.fmdc.arw.ARWMod
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalBlockContainer

class ORN6EBlock : ModelNormalBlockContainer(tileEntityClass = ORN6ETile::class.java) {
    init {
        setBlockName("orn_6e")
        setBlockTextureName(ARWMod.DOMAIN + ":orn_6e")
        setBlockBoundsSize(1.5f, 2.0f)
        setSelectedBoundSize(1.5, 2.0)
    }
}
