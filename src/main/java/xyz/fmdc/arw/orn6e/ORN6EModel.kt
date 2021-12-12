package xyz.fmdc.arw.orn6e

import net.minecraft.util.ResourceLocation
import xyz.fmdc.arw.ARWMod
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalModelBaseFixedTexture

class ORN6EModel : ModelNormalModelBaseFixedTexture<ORN6ETile>() {
    override val modelName = ResourceLocation(ARWMod.DOMAIN, "models/orn_6e_yukikaze.obj")
    override val texture = ResourceLocation(ARWMod.DOMAIN, "textures/models/wgb.png")
}
