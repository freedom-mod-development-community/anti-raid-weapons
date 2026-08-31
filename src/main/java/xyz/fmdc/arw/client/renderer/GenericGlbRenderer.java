package xyz.fmdc.arw.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import xyz.fmdc.arw.client.util.CustomRenderTypes;
import xyz.fmdc.arw.client.util.GlbLoader;

import javax.annotation.Nullable;
import java.util.List;

public class GenericGlbRenderer {

    @FunctionalInterface
    public interface NodeTransformCallback {
        void apply(String nodeName, PoseStack poseStack, float partialTick);
    }

    /**
     * 再生中アニメーションの情報を保持するレコード
     * @param name アニメーション名
     * @param timeSeconds 経過時間（秒）
     * @param loop ループ再生するかどうか（trueで繰り返す、falseで最終フレームで固定）
     */
    public record ActiveAnimation(String name, float timeSeconds, boolean loop) {
        // 後方互換用のコンストラクタ（デフォルトは単発再生 = false）
        public ActiveAnimation(String name, float timeSeconds) {
            this(name, timeSeconds, false);
        }
    }

    /**
     * GLBモデルを描画します。
     *
     * @param activeAnimations 現在再生中のアニメーション情報のリスト（複数指定で合成）
     */
    public void render(GlbLoader.GlbModelData modelData, PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay, float partialTick,
                       List<ActiveAnimation> activeAnimations,
                       @Nullable NodeTransformCallback callback) {

        if (modelData == null || modelData.rootNode == null) return;

        poseStack.pushPose();

        // 0.5ブロック分の補正
        poseStack.translate(0.5, 0.0, 0.5);

        renderNode(modelData.rootNode, modelData, poseStack, bufferSource,
                packedLight, packedOverlay, partialTick, activeAnimations, callback);

        poseStack.popPose();
    }

    private void renderNode(GlbLoader.GlbNode node, GlbLoader.GlbModelData modelData, PoseStack poseStack,
                            MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                            float partialTick, List<ActiveAnimation> activeAnimations,
                            @Nullable NodeTransformCallback callback) {

        poseStack.pushPose();

        Vector3f translation = new Vector3f(node.translation);
        Quaternionf rotation = new Quaternionf(node.rotation);
        Vector3f scale = new Vector3f(node.scale);

        // 1. 再生中リストにある全アニメーションを重層適用（マルチトラック合成）
        if (activeAnimations != null && !activeAnimations.isEmpty()) {
            for (ActiveAnimation activeAnim : activeAnimations) {
                if (modelData.animations.containsKey(activeAnim.name())) {
                    GlbLoader.GlbAnimation anim = modelData.animations.get(activeAnim.name());
                    applyAnimationToNode(node.name, anim, activeAnim.timeSeconds(), activeAnim.loop(), translation, rotation, scale);
                }
            }
        }

        // 2. 基本トランスフォームの適用
        poseStack.translate(translation.x(), translation.y(), translation.z());
        poseStack.mulPose(rotation);

        // 3. 外部カスタムトランスフォーム（Yaw / Pitch 旋回など）
        if (callback != null) {
            callback.apply(node.name, poseStack, partialTick);
        }

        // 4. スケール適用
        poseStack.scale(scale.x(), scale.y(), scale.z());

        // 5. メッシュの描画
        for (GlbLoader.MeshPart part : node.meshParts) {
            renderMeshPart(part, poseStack, bufferSource, packedLight, packedOverlay);
        }

        // 6. 子ノードの再帰描画
        for (GlbLoader.GlbNode child : node.children) {
            renderNode(child, modelData, poseStack, bufferSource, packedLight, packedOverlay,
                    partialTick, activeAnimations, callback);
        }

        poseStack.popPose();
    }

    private void applyAnimationToNode(String nodeName, GlbLoader.GlbAnimation anim, float time, boolean loop,
                                      Vector3f translation, Quaternionf rotation, Vector3f scale) {

        // 時間の計算：ループ設定に応じて余剰（余り）を取るかクランプするかを決定
        float animTime;
        if (loop && anim.maxTime > 0.0f) {
            animTime = time % anim.maxTime;
        } else {
            animTime = Math.min(time, anim.maxTime);
        }

        for (GlbLoader.AnimationChannel ch : anim.channels) {
            if (!ch.targetNodeName.equalsIgnoreCase(nodeName)) continue;

            switch (ch.path) {
                case "translation" -> {
                    Vector3f animVec = sampleVector3(ch, animTime);
                    if (animVec != null) translation.set(animVec);
                }
                case "rotation" -> {
                    Quaternionf q = sampleQuaternion(ch, animTime);
                    if (q != null) rotation.set(q);
                }
                case "scale" -> {
                    Vector3f s = sampleVector3(ch, animTime);
                    if (s != null) scale.set(s);
                }
            }
        }
    }

    private void renderMeshPart(GlbLoader.MeshPart part, PoseStack poseStack, MultiBufferSource bufferSource,
                                int packedLight, int packedOverlay) {
        if (part.positions == null || part.indices == null) return;

        RenderType renderType = getRenderType(part.material);
        VertexConsumer consumer = bufferSource.getBuffer(renderType);

        int light = (part.material != null && part.material.isEmissive)
                ? LightTexture.FULL_BRIGHT
                : packedLight;

        PoseStack.Pose lastPose = poseStack.last();
        Matrix4f poseMatrix = lastPose.pose();
        Matrix3f normalMatrix = lastPose.normal();

        float r = part.baseColorFactor[0];
        float g = part.baseColorFactor[1];
        float b = part.baseColorFactor[2];
        float a = part.baseColorFactor[3];

        for (int idx : part.indices) {
            int posIdx = idx * 3;
            float vx = part.positions[posIdx];
            float vy = part.positions[posIdx + 1];
            float vz = part.positions[posIdx + 2];

            float nx = 0.0f, ny = 1.0f, nz = 0.0f;
            if (part.normals != null && (idx * 3 + 2) < part.normals.length) {
                nx = part.normals[idx * 3];
                ny = part.normals[idx * 3 + 1];
                nz = part.normals[idx * 3 + 2];
            }

            float u = 0.0f, v = 0.0f;
            if (part.uvs != null && (idx * 2 + 1) < part.uvs.length) {
                u = part.uvs[idx * 2];
                v = part.uvs[idx * 2 + 1];
            }

            consumer.vertex(poseMatrix, vx, vy, vz)
                    .color(r, g, b, a)
                    .uv(u, v)
                    .overlayCoords(packedOverlay)
                    .uv2(light)
                    .normal(normalMatrix, nx, ny, nz)
                    .endVertex();
        }
    }

    private static final ResourceLocation DUMMY_WHITE_TEXTURE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    private RenderType getRenderType(@Nullable GlbLoader.MaterialInfo mat) {
        ResourceLocation tex = (mat != null && mat.textureLocation != null)
                ? mat.textureLocation
                : DUMMY_WHITE_TEXTURE;

        if (mat != null) {
            if (mat.isEmissive) {
                return CustomRenderTypes.emissiveOpaque(tex);
            }
            if (mat.alphaMode == GlbLoader.AlphaMode.BLEND) {
                return RenderType.entityTranslucent(tex);
            } else if (mat.alphaMode == GlbLoader.AlphaMode.MASK) {
                return RenderType.entityCutout(tex);
            }
        }
        return CustomRenderTypes.entityCutoutTriangles(tex);
    }

    private Vector3f sampleVector3(GlbLoader.AnimationChannel ch, float time) {
        float[] times = ch.keyframeTimes;
        float[] values = ch.keyframeValues;
        if (times.length == 0 || values.length < 3) return null;

        if (time <= times[0]) return new Vector3f(values[0], values[1], values[2]);
        if (time >= times[times.length - 1]) {
            int last = (times.length - 1) * 3;
            return new Vector3f(values[last], values[last + 1], values[last + 2]);
        }

        int idx = findTimeIndex(times, time);
        float t0 = times[idx];
        float t1 = times[idx + 1];
        float factor = (time - t0) / (t1 - t0);

        int v0 = idx * 3;
        int v1 = (idx + 1) * 3;

        return new Vector3f(
                values[v0] + factor * (values[v1] - values[v0]),
                values[v0 + 1] + factor * (values[v1 + 1] - values[v0 + 1]),
                values[v0 + 2] + factor * (values[v1 + 2] - values[v0 + 2])
        );
    }

    private Quaternionf sampleQuaternion(GlbLoader.AnimationChannel ch, float time) {
        float[] times = ch.keyframeTimes;
        float[] values = ch.keyframeValues;
        if (times.length == 0 || values.length < 4) return null;

        if (time <= times[0]) return new Quaternionf(values[0], values[1], values[2], values[3]);
        if (time >= times[times.length - 1]) {
            int last = (times.length - 1) * 4;
            return new Quaternionf(values[last], values[last + 1], values[last + 2], values[last + 3]);
        }

        int idx = findTimeIndex(times, time);
        float t0 = times[idx];
        float t1 = times[idx + 1];
        float factor = (time - t0) / (t1 - t0);

        int q0 = idx * 4;
        int q1 = (idx + 1) * 4;

        Quaternionf qStart = new Quaternionf(values[q0], values[q0 + 1], values[q0 + 2], values[q0 + 3]);
        Quaternionf qEnd = new Quaternionf(values[q1], values[q1 + 1], values[q1 + 2], values[q1 + 3]);

        return qStart.slerp(qEnd, factor);
    }

    private int findTimeIndex(float[] times, float time) {
        for (int i = 0; i < times.length - 1; i++) {
            if (time >= times[i] && time <= times[i + 1]) {
                return i;
            }
        }
        return 0;
    }
}