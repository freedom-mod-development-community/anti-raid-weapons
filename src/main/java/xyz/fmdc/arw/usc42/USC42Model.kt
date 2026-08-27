package xyz.fmdc.arw.usc42

import net.minecraft.resources.ResourceLocation
import xyz.fmdc.arw.ARWMod
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalModelBaseFixedTexture

/** [USC42Tile] は回転しない固定ブロックのため、旋回しない基底クラスを使用する。 */
class USC42Model : ModelNormalModelBaseFixedTexture<USC42Tile>() {
    override val modelName = ResourceLocation(ARWMod.DOMAIN, "models/usc_42_yukikaze.obj")
    override val texture = ResourceLocation(ARWMod.DOMAIN, "textures/models/wgb.png")
}
