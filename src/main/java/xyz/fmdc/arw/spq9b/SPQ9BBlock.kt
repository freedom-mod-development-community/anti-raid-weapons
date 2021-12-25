package xyz.fmdc.arw.spq9b

import net.minecraft.util.ResourceLocation
import xyz.fmdc.arw.ARWMod.DOMAIN
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalBlockContainer

object SPQ9BBlock : ModelNormalBlockContainer(tileEntityClass = SPQ9BTile::class.java) {
    init {
        registryName = ResourceLocation(DOMAIN, "spq_9b")
        translationKey = "spq_9b"
        setBlockBoundsSize(1.75f, 2.5f)
    }
}
