package xyz.fmdc.arw.nora1c

import xyz.fmdc.arw.ARWMod
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalBlockContainer

object NORA1CBlock : ModelNormalBlockContainer(tileEntityClass = NORA1CTile::class.java) {
    init {
        setBlockName("nora_1c")
        setBlockTextureName(ARWMod.DOMAIN + ":nora_1c")
        setBlockBoundsSize(1.75f, 2.5f)
        setSelectedBoundSize(1.75, 2.5)
    }
}
