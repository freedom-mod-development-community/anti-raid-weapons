package xyz.fmdc.arw.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import xyz.fmdc.arw.AntiRaidWeapons;
import xyz.fmdc.arw.entity.NavalShellEntity;

public class NavalShellRenderer extends EntityRenderer<NavalShellEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AntiRaidWeapons.MOD_ID, "textures/entity/naval_shell.png");

    public NavalShellRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(NavalShellEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        // 飛翔方向への回転
        float yaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90.0f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch));

        // 砲弾メッシュのレンダリング（シンプルな弾頭形状）
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutout(getTextureLocation(entity)));
        PoseStack.Pose last = poseStack.last();
        Matrix4f pose = last.pose();
        Matrix3f normal = last.normal();

        float length = 0.6f;
        float radius = 0.12f;

        // 弾頭コーン・円筒の描画
        int segments = 8;
        for (int i = 0; i < segments; i++) {
            float angle1 = (float) (i * 2 * Math.PI / segments);
            float angle2 = (float) ((i + 1) * 2 * Math.PI / segments);

            float cos1 = (float) Math.cos(angle1) * radius;
            float sin1 = (float) Math.sin(angle1) * radius;
            float cos2 = (float) Math.cos(angle2) * radius;
            float sin2 = (float) Math.sin(angle2) * radius;

            // 胴体 (Body)
            addQuad(consumer, pose, normal, packedLight,
                    -length * 0.5f, cos1, sin1,
                    -length * 0.5f, cos2, sin2,
                     length * 0.2f, cos2, sin2,
                     length * 0.2f, cos1, sin1
            );

            // 弾頭部 (Nose cone)
            addTriangle(consumer, pose, normal, packedLight,
                    length * 0.2f, cos1, sin1,
                    length * 0.2f, cos2, sin2,
                    length * 0.5f, 0.0f, 0.0f
            );

            // 底部 (Base cap)
            addTriangle(consumer, pose, normal, packedLight,
                    -length * 0.5f, cos2, sin2,
                    -length * 0.5f, cos1, sin1,
                    -length * 0.5f, 0.0f, 0.0f
            );
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private void addQuad(VertexConsumer consumer, Matrix4f pose, Matrix3f normal, int light,
                         float x1, float y1, float z1,
                         float x2, float y2, float z2,
                         float x3, float y3, float z3,
                         float x4, float y4, float z4) {
        addVertex(consumer, pose, normal, light, x1, y1, z1, 0, 0);
        addVertex(consumer, pose, normal, light, x2, y2, z2, 1, 0);
        addVertex(consumer, pose, normal, light, x3, y3, z3, 1, 1);
        addVertex(consumer, pose, normal, light, x4, y4, z4, 0, 1);
    }

    private void addTriangle(VertexConsumer consumer, Matrix4f pose, Matrix3f normal, int light,
                            float x1, float y1, float z1,
                            float x2, float y2, float z2,
                            float x3, float y3, float z3) {
        addVertex(consumer, pose, normal, light, x1, y1, z1, 0, 0);
        addVertex(consumer, pose, normal, light, x2, y2, z2, 1, 0);
        addVertex(consumer, pose, normal, light, x3, y3, z3, 0.5f, 1);
        addVertex(consumer, pose, normal, light, x3, y3, z3, 0.5f, 1); // 4頂点目（Quads互換）
    }

    private void addVertex(VertexConsumer consumer, Matrix4f pose, Matrix3f normal, int light,
                           float x, float y, float z, float u, float v) {
        consumer.vertex(pose, x, y, z)
                .color(200, 200, 210, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normal, 0.0f, 1.0f, 0.0f)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(NavalShellEntity entity) {
        return TEXTURE;
    }
}
