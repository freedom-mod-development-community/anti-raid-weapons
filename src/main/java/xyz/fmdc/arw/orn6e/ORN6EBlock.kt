package xyz.fmdc.arw.orn6e

import net.minecraft.util.ResourceLocation
import xyz.fmdc.arw.ARWMod.DOMAIN
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalBlockContainer

object ORN6EBlock : ModelNormalBlockContainer(tileEntityClass = ORN6ETile::class.java) {
    init {
        registryName = ResourceLocation(DOMAIN, "orn_6e")
        translationKey = "orn_6e"
        setBlockBoundsSize(1.5f, 2.0f)
    }
}
