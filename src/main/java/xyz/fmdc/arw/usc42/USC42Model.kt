package xyz.fmdc.arw.usc42

import net.minecraft.util.ResourceLocation
import xyz.fmdc.arw.ARWMod
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalRotatableModelBaseFixedTexture

class USC42Model : ModelNormalRotatableModelBaseFixedTexture<USC42Tile>() {
    override val modelName = ResourceLocation(ARWMod.DOMAIN, "models/usc_42_yukikaze.obj")
    override val texture = ResourceLocation(ARWMod.DOMAIN, "textures/models/wgb.png")
}
