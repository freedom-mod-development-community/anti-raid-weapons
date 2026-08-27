package xyz.fmdc.arw.spg62

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.Vec3
import xyz.fmdc.arw.ARWMod
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalRotatableModelBaseFixedTexture

class SPG62Model : ModelNormalRotatableModelBaseFixedTexture<SPG62Tile>() {
    override val modelPath = ResourceLocation(ARWMod.DOMAIN, "models/spg_62.obj")
    override val texture = ResourceLocation(ARWMod.DOMAIN, "textures/models/wgb.png")
    override val pitchPivot = Vec3(0.0, 2.5862, 0.0751)
}
