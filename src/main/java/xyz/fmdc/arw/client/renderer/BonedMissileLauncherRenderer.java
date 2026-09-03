package xyz.fmdc.arw.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import xyz.fmdc.arw.client.GlbModelManager;
import xyz.fmdc.arw.client.util.FastGlbModel;
import xyz.fmdc.arw.client.util.IDirectionalBlockEntity;
import xyz.fmdc.arw.client.util.IYawPitchAnimatableModel;
import xyz.fmdc.arw.common.blockentity.weapon.BonedMissileLauncherBlockEntity;

import java.util.List;
import java.util.function.Function;

public class BonedMissileLauncherRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {

    protected final GenericFastGlbRenderer glbRenderer = new GenericFastGlbRenderer();
    protected final GenericFastGlbRenderer missileRenderer = new GenericFastGlbRenderer();
    private final Function<T, FastGlbModel> modelProvider;

    public BonedMissileLauncherRenderer(BlockEntityRendererProvider.Context context, Function<T, FastGlbModel> modelProvider) {
        this.modelProvider = modelProvider;
    }

    @Override
    public void render(@NotNull T blockEntity, float partialTick, @NotNull PoseStack poseStack,
                       @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        FastGlbModel modelData = this.modelProvider.apply(blockEntity);
        if (modelData == null) return;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        Direction facing = getDirectionFromBlockEntity(blockEntity);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));

        // アニメーションおよび旋回角度の取得
        List<GenericFastGlbRenderer.ActiveAnimation> activeAnimations = getAnimationsFromBlockEntity(blockEntity, partialTick);

        final float targetYaw = getYawFromBlockEntity(blockEntity, partialTick);
        final float targetPitch = getPitchFromBlockEntity(blockEntity, partialTick);

        // 1. ランチャー本体（親モデル）のレンダリング
        glbRenderer.render(
                modelData,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                partialTick,
                activeAnimations,
                null, // NodeTransformCallback
                null, // NodePostRenderCallback (使わない)

                // BoneTransformCallback (Yaw/Pitchボーンの回転操作)
                (boneName, translation, rotation, scale) -> {
                    if ("root".equalsIgnoreCase(boneName) || "yaw".equalsIgnoreCase(boneName)) {
                        rotation.rotateY((float) Math.toRadians(-targetYaw));
                    } else if ("bone_pitch".equalsIgnoreCase(boneName) || "pitch".equalsIgnoreCase(boneName)) {
                        rotation.rotateX((float) Math.toRadians(targetPitch));
                    }
                },
                false
        );

        // 2. Pitchボーンのグローバル変換行列を取得してミサイルを描画
        if (blockEntity instanceof BonedMissileLauncherBlockEntity launcherBE) {
            if (shouldRenderMissile(launcherBE)) {
                // "bone_pitch" または "pitch" ノードの計算済みグローバル行列を取得
                Matrix4f pitchGlobalMatrix = glbRenderer.getGlobalTransform("bone_pitch");
                if (pitchGlobalMatrix == null) {
                    pitchGlobalMatrix = glbRenderer.getGlobalTransform("pitch");
                }

                if (pitchGlobalMatrix != null) {
                    poseStack.pushPose();

                    // ★修正箇所: 行列を直接乗算せず、位置・回転・スケールに分解して安全に適用する
                    Vector3f translation = new Vector3f();
                    Quaternionf rotation = new Quaternionf();
                    Vector3f scale = new Vector3f();

                    pitchGlobalMatrix.getTranslation(translation);
                    pitchGlobalMatrix.getUnnormalizedRotation(rotation);
                    pitchGlobalMatrix.getScale(scale);

                    // PoseStackの正規メソッド経由で適用（これで法線行列も正しく計算される）
                    poseStack.translate(translation.x, translation.y, translation.z);
                    //poseStack.mulPose(rotation);
                    poseStack.scale(scale.x, scale.y, scale.z);

                    // Pitchボーンローカル位置からのオフセット（ミサイルの取り付け位置）
                    poseStack.translate(
                            BonedMissileLauncherBlockEntity.MOUNTED_MISSILE_X,
                            BonedMissileLauncherBlockEntity.MOUNTED_MISSILE_Y,
                            BonedMissileLauncherBlockEntity.MOUNTED_MISSILE_Z
                    );

                    // ミサイルのロール軸調整が必要な場合はここで適用
                    // poseStack.mulPose(Axis.ZP.rotationDegrees(45.0f));

                    // ミサイル本体のレンダリング
                    missileRenderer.render(
                            GlbModelManager.INSTANCE.getFastModel(GlbModelManager.RIM66M2_ID),
                            poseStack, bufferSource, packedLight, packedOverlay,
                            partialTick, null, null, null, false
                    );

                    poseStack.popPose();
                }
            }
        }

        poseStack.popPose();
    }

    private boolean shouldRenderMissile(BonedMissileLauncherBlockEntity blockEntity) {
        return blockEntity.shouldRenderMissile();
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    public Direction getDirectionFromBlockEntity(BlockEntity blockEntity) {
        if (blockEntity instanceof IDirectionalBlockEntity directional) {
            return directional.getFacing();
        }
        return Direction.SOUTH;
    }

    private float getYawFromBlockEntity(BlockEntity blockEntity, float partialTick) {
        if (blockEntity instanceof IYawPitchAnimatableModel be) {
            return be.getRenderTargetYaw(partialTick);
        }
        return 0.0f;
    }

    private float getPitchFromBlockEntity(BlockEntity blockEntity, float partialTick) {
        if (blockEntity instanceof IYawPitchAnimatableModel be) {
            return be.getRenderTargetPitch(partialTick);
        }
        return 0.0f;
    }

    @SuppressWarnings("unchecked")
    private List<GenericFastGlbRenderer.ActiveAnimation> getAnimationsFromBlockEntity(T blockEntity, float partialTick) {
        if (blockEntity instanceof IYawPitchAnimatableModel animatable) {
            return animatable.getActiveAnimations(partialTick);
        }
        return List.of();
    }
}