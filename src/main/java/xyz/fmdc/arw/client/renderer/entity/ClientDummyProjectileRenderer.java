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
import xyz.fmdc.arw.common.entity.projectile.ClientDummyProjectileEntity;

import java.util.Collections;

/**
 * クライアント側ダミー飛翔体の描画クラス。
 * CIWS弾・5インチ通常砲弾は高密度パーティクル・トレーサーにより数千発でもゼロ描画負荷を実現。
 * ミサイルの場合はGLB 3Dモデルを進行方向に合わせてレンダリングします。
 */
public class ClientDummyProjectileRenderer extends EntityRenderer<ClientDummyProjectileEntity> {

    private static final ResourceLocation DUMMY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AntiRaidWeapons.MOD_ID, "textures/entity/projectiles/bullet.png");

    private final GenericFastGlbRenderer glbRenderer = new GenericFastGlbRenderer();

    public ClientDummyProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(
            ClientDummyProjectileEntity entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        // ミサイル（type == 2）の場合は GLB 3Dモデルを描画
        if (entity.getProjectileType() == 2) {
            FastGlbModel model = GlbModelManager.INSTANCE.getFastModel(GlbModelManager.RIM66M2_ID);
            if (model != null) {
                poseStack.pushPose();

                // 姿勢補間（Yaw / Pitch）
                float yaw = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
                float pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());

                // 進行方向へ回転
                poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
                poseStack.mulPose(Axis.XP.rotationDegrees(90.0F - pitch));

                glbRenderer.render(model, poseStack, buffer, packedLight, 0, partialTicks, Collections.emptyList(), null, false);

                poseStack.popPose();
            }
        }

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ClientDummyProjectileEntity entity) {
        return DUMMY_TEXTURE;
    }
}
