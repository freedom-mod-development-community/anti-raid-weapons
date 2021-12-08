package xyz.fmdc.arw.anuyh3

import net.minecraft.util.ResourceLocation
import xyz.fmdc.arw.ARWMod
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalModelBase

class ANUYH3Model : ModelNormalModelBase<ANUYH3Tile>() {
    override val modelName = ResourceLocation(ARWMod.DOMAIN, "models/an_uyh_3_kukiki.obj")
    override val texture = ResourceLocation(ARWMod.DOMAIN, "textures/models/wgb.png")
}