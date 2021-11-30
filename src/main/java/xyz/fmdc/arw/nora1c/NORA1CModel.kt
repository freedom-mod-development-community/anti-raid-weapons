package xyz.fmdc.arw.nora1c

import net.minecraft.util.ResourceLocation
import xyz.fmdc.arw.ARWMod
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalModelBase

class NORA1CModel : ModelNormalModelBase<NORA1CTile>() {
    override val modelName = ResourceLocation(ARWMod.DOMAIN, "models/nora_1c_yukikaze.obj")
    override val texture = ResourceLocation(ARWMod.DOMAIN, "textures/models/wgb.png")
}