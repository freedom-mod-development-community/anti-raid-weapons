package xyz.fmdc.arw.spg62

import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import xyz.fmdc.arw.ARWMod
import xyz.fmdc.arw.modcore.baseclass.modelblock.ModelNormalRotatableModelBase

class SPG62Model : ModelNormalRotatableModelBase<SPG62Tile>() {
    override val modelName = ResourceLocation(ARWMod.DOMAIN, "models/spg_62.obj")
    override val texture = ResourceLocation(ARWMod.DOMAIN, "textures/models/wgb.png")
    override fun offsetPitch() {
        GL11.glTranslated(0.0, 2.5862, 0.0751)
    }
}