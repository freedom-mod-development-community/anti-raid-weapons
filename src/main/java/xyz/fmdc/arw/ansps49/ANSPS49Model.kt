package xyz.fmdc.arw.ansps49

import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import xyz.fmdc.arw.ARWMod
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalRotatableModelBase

class ANSPS49Model : ModelNormalRotatableModelBase<ANSPS49Tile>() {
    override val modelName = ResourceLocation(ARWMod.DOMAIN, "models/an_sps_49_kukiki.obj")
    override val texture = ResourceLocation(ARWMod.DOMAIN, "textures/models/wgb.png")
    override fun offsetPitch() {
        GL11.glTranslated(0.0, 7.24, 0.0)
    }
}
