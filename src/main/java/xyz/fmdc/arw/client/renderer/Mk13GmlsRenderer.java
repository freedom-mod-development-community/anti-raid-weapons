package xyz.fmdc.arw.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.jetbrains.annotations.NotNull;
import xyz.fmdc.arw.client.GlbModelManager;
import xyz.fmdc.arw.client.util.FastGlbModel;
import xyz.fmdc.arw.common.blockentity.weapon.Mk13GmlsBlockEntity;

import java.util.List;

/**
 * Mk 13 GMLS (Guided Missile Launching System) 専用 BlockEntity レンダラー.
 * 単装ミサイルランチャー（1発装填）の中央発射レール上に RIM-66M-2 モデルを描画します。
 * 発射時・リロード中はミサイルを非表示にし、リロード完了時にインベントリに存在する場合のみ描画します。
 */
public class Mk13GmlsRenderer extends BaseNavalGunRenderer<Mk13GmlsBlockEntity> {

    public static final double MISSILE_CENTER_X = Mk13GmlsBlockEntity.MOUNTED_MISSILE_X;
    public static final double MISSILE_CENTER_Y = Mk13GmlsBlockEntity.MOUNTED_MISSILE_Y;
    public static final double MISSILE_CENTER_Z = Mk13GmlsBlockEntity.MOUNTED_MISSILE_Z;

    private final GenericFastGlbRenderer missileRenderer = new GenericFastGlbRenderer();

    public Mk13GmlsRenderer(BlockEntityRendererProvider.Context context) {
        super(context, be -> GlbModelManager.INSTANCE.getFastModel(GlbModelManager.MK13GMLS_ID));
    }

    @Override
    public void render(@NotNull Mk13GmlsBlockEntity blockEntity, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        FastGlbModel launcherModel = GlbModelManager.INSTANCE.getFastModel(GlbModelManager.MK13GMLS_ID);
        if (launcherModel == null) return;

        List<GenericFastGlbRenderer.ActiveAnimation> activeAnimations = blockEntity.getActiveAnimations(partialTick);

        glbRenderer.render(
                launcherModel, poseStack, bufferSource, packedLight, packedOverlay, partialTick,
                activeAnimations,
                (nodeName, stack, pTick) -> {
                    if ("yaw".equalsIgnoreCase(nodeName)) {
                        stack.mulPose(Axis.YP.rotationDegrees(-blockEntity.getRenderTargetYaw(pTick)));
                    } else if ("pitch".equalsIgnoreCase(nodeName)) {
                        stack.mulPose(Axis.XP.rotationDegrees(blockEntity.getRenderTargetPitch(pTick)));
                    }
                },
                (nodeName, stack, bufSource, light, overlay, pTick) -> {
                    if ("pitch".equalsIgnoreCase(nodeName) && shouldRenderMissile(blockEntity, activeAnimations)) {
                        renderMountedMissile(blockEntity, stack, bufSource, light, overlay, pTick);
                    }
                },
                true
        );
    }

    /**
     * 発射時・リロード中（クールダウン中または発射/リロードアニメーション再生中）はミサイルを描画せず、
     * リロードが完了し、インベントリ内に RIM-66M-2 が存在する場合にのみミサイルを描画します。
     */
    private boolean shouldRenderMissile(Mk13GmlsBlockEntity blockEntity,
                                        List<GenericFastGlbRenderer.ActiveAnimation> activeAnimations) {
        if (!blockEntity.shouldRenderMissile()) {
            return false;
        }
        if (activeAnimations != null) {
            for (GenericFastGlbRenderer.ActiveAnimation anim : activeAnimations) {
                if ("fire".equalsIgnoreCase(anim.name()) || "reload".equalsIgnoreCase(anim.name())) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * ランチャー中央の単装レールに RIM-66M-2 ミサイルモデルを描画します。
     */
    private void renderMountedMissile(Mk13GmlsBlockEntity blockEntity, PoseStack poseStack,
                                      MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                      float partialTick) {

        FastGlbModel missileModel = GlbModelManager.INSTANCE.getFastModel(GlbModelManager.RIM66M2_ID);
        if (missileModel == null) return;

        poseStack.pushPose();
        poseStack.translate(MISSILE_CENTER_X, MISSILE_CENTER_Y, MISSILE_CENTER_Z);
        poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
        missileRenderer.render(missileModel, poseStack, bufferSource, packedLight, packedOverlay,
                partialTick, null, null, false);
        poseStack.popPose();
    }
}
