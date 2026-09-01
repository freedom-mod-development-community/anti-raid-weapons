package xyz.fmdc.arw.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import xyz.fmdc.arw.AntiRaidWeapons;
import xyz.fmdc.arw.client.GlbModelManager;
import xyz.fmdc.arw.client.renderer.GenericFastGlbRenderer;
import xyz.fmdc.arw.client.util.FastGlbModel;
import xyz.fmdc.arw.common.entity.missile.Rim66M2;

import java.util.Collections;

/**
 * RIM-66M-2 ミサイルのエンティティ描画クラス。
 * GLBモデルとテクスチャを用いて進行方向へ高速飛行するミサイルを描画します。
 */
public class Rim66M2Renderer extends EntityRenderer<Rim66M2> {

    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(AntiRaidWeapons.MOD_ID, "textures/entity/rim66m2.png");
    private final GenericFastGlbRenderer glbRenderer = new GenericFastGlbRenderer();

    public Rim66M2Renderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(Rim66M2 entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        FastGlbModel model = GlbModelManager.INSTANCE.getFastModel(GlbModelManager.RIM66M2_ID);
        if (model == null) {
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
            return;
        }

        poseStack.pushPose();

        // 姿勢の補間計算 (Yaw / Pitch)
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());

        // GLBモデルは+Y方向が弾頭(先端)なので、進行方向(Yaw / Pitch)へ回転させる
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F - pitch));

        // ブロックオフセット (0.5, 0, 0.5) なしで描画
        glbRenderer.render(model, poseStack, bufferSource, packedLight, 0, partialTick, Collections.emptyList(), null, false);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(Rim66M2 entity) {
        return TEXTURE;
    }
}
