package xyz.fmdc.arw.ops39

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.resources.ResourceLocation
import xyz.fmdc.arw.ARWMod
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalRotatableModelBaseFixedTexture

/**
 * [OPS39Tile] は水平旋回する対水上レーダーのため、旋回対応の基底クラスを使用する。
 * この OBJ は `base` / `yaw` ではなく独自のグループ名を持つため、描画対象を上書きしている。
 */
class OPS39Model : ModelNormalRotatableModelBaseFixedTexture<OPS39Tile>() {
    override val modelPath = ResourceLocation(ARWMod.DOMAIN, "models/ops_39_yukikaze.obj")
    override val texture = ResourceLocation(ARWMod.DOMAIN, "textures/models/wgb.png")

    override fun renderBaseParts(
        poseStack: PoseStack,
        consumer: VertexConsumer,
        packedLight: Int,
        packedOverlay: Int
    ) {
        model.renderPart(PART_BODY, poseStack, consumer, packedLight, packedOverlay)
    }

    override fun renderYawParts(
        poseStack: PoseStack,
        consumer: VertexConsumer,
        packedLight: Int,
        packedOverlay: Int
    ) {
        model.renderPart(PART_RADAR, poseStack, consumer, packedLight, packedOverlay)
    }

    private companion object {
        const val PART_BODY = "body_ops_39"
        const val PART_RADAR = "radar_ops_39.001"
    }
}
