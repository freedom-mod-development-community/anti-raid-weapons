package xyz.fmdc.arw.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import xyz.fmdc.arw.client.GlbModelManager;
import xyz.fmdc.arw.client.util.FastGlbModel;
import xyz.fmdc.arw.api.blockentity.IMissileLauncherBlockEntity;
import xyz.fmdc.arw.common.blockentity.weapon.launcher.MissileSlot;

import java.util.List;

public class MissileLauncherRenderer<T extends BlockEntity & IMissileLauncherBlockEntity> implements BlockEntityRenderer<T> {

    public MissileLauncherRenderer(BlockEntityRendererProvider.Context context) {
    }

    protected final GenericFastGlbRenderer launcherRenderer = new GenericFastGlbRenderer();
    protected final GenericFastGlbRenderer missileRenderer = new GenericFastGlbRenderer();

    @Override
    public void render(T blockEntity, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        // 1. ランチャー本体のモデルを取得
        FastGlbModel launcherModel = GlbModelManager.INSTANCE.getFastModel(blockEntity.getLauncherModelId());
        if (launcherModel == null) return;

        poseStack.pushPose();

        poseStack.translate(0.5, 0.0, 0.5);
        // ブロックの中心配置 & Facing（方角）適用
        if (blockEntity.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            Direction facing = blockEntity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-facing.toYRot()));
        }

        float yaw = blockEntity.getRenderTargetYaw(partialTick);
        float pitch = blockEntity.getRenderTargetPitch(partialTick);

        List<GenericFastGlbRenderer.ActiveAnimation> activeAnimations = blockEntity.getActiveAnimations(partialTick);

        // 2. ランチャー本体のレンダリング
        // (glbRenderer は自作の GenericFastGlbRenderer インスタンスと仮定)
        launcherRenderer.render(
                launcherModel,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                partialTick,
                activeAnimations, // アニメーション一覧があれば渡す
                null,
                null,
                // BoneTransformCallback: Yaw/Pitchボーンのダイナミック回転
                (boneName, translation, rotation, scale) -> {
                    if ("yaw".equalsIgnoreCase(boneName)) {
                        rotation.rotateY((float) Math.toRadians(-yaw));
                    } else if ("pitch".equalsIgnoreCase(boneName)) {
                        rotation.rotateX((float) Math.toRadians(pitch));
                    }
                },
                false
        );

        // 3. 各スロット（ダミーボーン）の位置を計算してミサイルを描画
        for (MissileSlot slot : blockEntity.getLoadedMissileSlots()) {
            if (!slot.isLoaded()) break;

            // GLTF内のダミーボーン (例: "mount_point_1") のグローバル変換行列を取得
            Matrix4f mountMatrix = launcherRenderer.getGlobalTransform(slot.slotBoneName());
            if (mountMatrix == null) continue;

            FastGlbModel missileModel = GlbModelManager.INSTANCE.getFastModel(slot.missileModelId());
            if (missileModel == null) continue;

            poseStack.pushPose();

            // 行列を直接 PoseStack に乗算する
            // mountMatrix は「モデル原点(0,0,0)」からの相対グローバル行列のため、
            // 既に translate(0.5, 0, 0.5) と facing 回転が適用されている現在の poseStack に直接乗算すれば追従
            poseStack.last().pose().mul(mountMatrix);

            // ミサイル本体の描画
            missileRenderer.render(
                    missileModel, poseStack, bufferSource, packedLight, packedOverlay, partialTick,
                    null, null, null, false
            );

            poseStack.popPose();
        }

        poseStack.popPose();
    }
}