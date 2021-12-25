package xyz.fmdc.arw.nora1c

import net.minecraft.util.ResourceLocation
import xyz.fmdc.arw.ARWMod.DOMAIN
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalBlockContainer

object NORA1CBlock : ModelNormalBlockContainer(tileEntityClass = NORA1CTile::class.java) {
    init {
        registryName = ResourceLocation(DOMAIN, "nora_1c")
        translationKey = "nora_1c"
        setBlockBoundsSize(1.75f, 2.5f)
    }
}
