package xyz.fmdc.arw.norq1

import net.minecraft.util.ResourceLocation
import xyz.fmdc.arw.ARWMod
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalModelBase

class NORQ1Model : ModelNormalModelBase<NORQ1Tile>() {
    override val modelName = ResourceLocation(ARWMod.DOMAIN, "models/norq_1_yukikaze.obj")
    //override val modelName = ResourceLocation(ARWMod.DOMAIN, "models/nora_1c_yukikaze.obj")
    override val texture = ResourceLocation(ARWMod.DOMAIN, "textures/models/wgb.png")
}