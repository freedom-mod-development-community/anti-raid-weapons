package xyz.fmdc.arw.spg62

import net.minecraft.resources.ResourceLocation
import org.lwjgl.opengl.GL11
import xyz.fmdc.arw.ARWMod
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalRotatableModelBaseFixedTexture

class SPG62Model : ModelNormalRotatableModelBaseFixedTexture<SPG62Tile>() {
    override val modelPath = ResourceLocation(ARWMod.DOMAIN, "models/spg_62.obj")
    override val texture = ResourceLocation(ARWMod.DOMAIN, "textures/models/wgb.png")
    fun offsetPitch() {
        GL11.glTranslated(0.0, 2.5862, 0.0751)
    }
}
