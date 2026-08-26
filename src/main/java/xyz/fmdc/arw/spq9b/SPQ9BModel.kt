package xyz.fmdc.arw.spq9b

import net.minecraft.resources.ResourceLocation
import xyz.fmdc.arw.ARWMod
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalRotatableModelBaseFixedTexture

class SPQ9BModel : ModelNormalRotatableModelBaseFixedTexture<SPQ9BTile>() {
    override val modelPath = ResourceLocation(ARWMod.DOMAIN, "models/spq_9b_yukikaze.obj")
    override val texture = ResourceLocation(ARWMod.DOMAIN, "textures/models/wgb.png")
}
