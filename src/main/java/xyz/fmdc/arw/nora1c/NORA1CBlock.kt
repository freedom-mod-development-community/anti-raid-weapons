package xyz.fmdc.arw.nora1c

import xyz.fmdc.arw.ARWMod
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalBlockContainer

class NORA1CBlock : ModelNormalBlockContainer(tileEntityClass = NORA1CTile::class.java) {
    init {
        setBlockName("nora_1c")
        setBlockTextureName(ARWMod.DOMAIN + ":nora_1c")
        setBlockBoundsSize(2f, 3f)
        setSelectedBoundSize(2.0, 3.0)
    }
}
