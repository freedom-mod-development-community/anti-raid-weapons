package xyz.fmdc.arw.ansps49

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.Vec3
import xyz.fmdc.arw.ARWMod
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalRotatableModelBaseFixedTexture

class ANSPS49Model : ModelNormalRotatableModelBaseFixedTexture<ANSPS49Tile>() {
    override val modelPath = ResourceLocation(ARWMod.DOMAIN, "models/an_sps_49_kukiki.obj")
    override val texture = ResourceLocation(ARWMod.DOMAIN, "textures/models/wgb.png")
    override val pitchPivot = Vec3(0.0, 7.24, 0.0)
}
