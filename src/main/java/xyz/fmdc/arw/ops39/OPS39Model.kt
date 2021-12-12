package xyz.fmdc.arw.ops39

import net.minecraft.util.ResourceLocation
import xyz.fmdc.arw.ARWMod
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalModelBaseFixedTexture

class OPS39Model : ModelNormalModelBaseFixedTexture<OPS39Tile>() {
    override val modelName = ResourceLocation(ARWMod.DOMAIN, "models/ops_39_yukikaze.obj")
    override val texture = ResourceLocation(ARWMod.DOMAIN, "textures/models/wgb.png")
}
