package xyz.fmdc.arw.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import xyz.fmdc.arw.AntiRaidWeapons;
import xyz.fmdc.arw.common.entity.projectile.ClientDummyProjectileEntity;

/**
 * クライアント側ダミー飛翔体の描画クラス。
 * パーティクルやトレイルエフェクトを主体とし、モデルが必要な場合はここで描画します。
 */
public class ClientDummyProjectileRenderer extends EntityRenderer<ClientDummyProjectileEntity> {

    private static final ResourceLocation DUMMY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AntiRaidWeapons.MOD_ID, "textures/entity/projectiles/bullet.png");

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
        // CIWS弾などは超高速かつ多数のため、実体ポリゴンを描画せずパーティクルのみで描画負荷を極小化
        // 必要に応じてミサイル（type == 2）時にGLBモデルを描画可能
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ClientDummyProjectileEntity entity) {
        return DUMMY_TEXTURE;
    }
}
