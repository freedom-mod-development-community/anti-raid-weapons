package xyz.fmdc.arw.cicelectric

import net.minecraft.util.ResourceLocation
import xyz.fmdc.arw.ARWMod
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalModelBaseFixTexture

class CICElectricModel: ModelNormalModelBaseFixTexture<CICElectricTile>() {
    override val modelName = ResourceLocation(ARWMod.DOMAIN, "models/cic_electric_control_kiukiki.obj")
    override val texture = ResourceLocation(ARWMod.DOMAIN, "textures/models/monitor01_on.png")
}
