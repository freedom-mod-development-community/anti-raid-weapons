package xyz.fmdc.arw.spq9b

import xyz.fmdc.arw.ARWMod
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalBlockContainer

class SPQ9BBlock : ModelNormalBlockContainer(tileEntityClass = SPQ9BTile::class.java) {
    init {
        setBlockName("spq_9b")
        setBlockTextureName(ARWMod.DOMAIN + ":spq_9b")
        setBlockBoundsSize(1.75f, 2.5f)
        setSelectedBoundSize(1.75, 2.5)
    }
}
