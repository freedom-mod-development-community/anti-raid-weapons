package xyz.fmdc.arw.spg62

import net.minecraft.util.ResourceLocation
import xyz.fmdc.arw.ARWMod.DOMAIN
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalBlockContainer

object SPG62Block : ModelNormalBlockContainer(tileEntityClass = SPG62Tile::class.java) {
    init {
        registryName = ResourceLocation(DOMAIN, "spg_62")
        setBlockBoundsSize(2f, 3f)
    }
}
