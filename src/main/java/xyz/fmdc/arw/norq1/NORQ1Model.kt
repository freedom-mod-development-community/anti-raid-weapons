package xyz.fmdc.arw.norq1

import net.minecraft.util.ResourceLocation
import xyz.fmdc.arw.ARWMod
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalModelBaseFixedTexture

class NORQ1Model : ModelNormalModelBaseFixedTexture<NORQ1Tile>() {
    override val modelName = ResourceLocation(ARWMod.DOMAIN, "models/norq_1_yukikaze.obj")
    override val texture = ResourceLocation(ARWMod.DOMAIN, "textures/models/wgb.png")
}