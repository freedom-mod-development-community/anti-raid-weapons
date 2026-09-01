package xyz.fmdc.arw.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import xyz.fmdc.arw.client.util.CustomRenderTypes;
import xyz.fmdc.arw.client.util.FastGlbModel;
import xyz.fmdc.arw.client.util.GlbLoader;

import javax.annotation.Nullable;
import java.util.List;

public class GenericFastGlbRenderer {

    @FunctionalInterface
    public interface NodeTransformCallback {
        void apply(String nodeName, PoseStack poseStack, float partialTick);
    }

    public record ActiveAnimation(String name, float timeSeconds, boolean loop) {
        public ActiveAnimation(String name, float timeSeconds) {
            this(name, timeSeconds, false);
        }
    }

    // 計算用インスタンスの再利用 (GC 発生の防止)
    private final Vector3f animTranslation = new Vector3f();
    private final Quaternionf animRotation = new Quaternionf();
    private final Vector3f animScale = new Vector3f();

    public void render(FastGlbModel fastModel, PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay, float partialTick,
                       List<ActiveAnimation> activeAnimations,
                       @Nullable NodeTransformCallback callback) {

        if (fastModel == null || fastModel.rootNode == null) return;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5); // 0.5ブロックの補正

        renderNode(fastModel.rootNode, fastModel.rawData, poseStack, bufferSource,
                packedLight, packedOverlay, partialTick, activeAnimations, callback);

        poseStack.popPose();
    }

    private void renderNode(FastGlbModel.FastNode node, GlbLoader.GlbModelData rawData, PoseStack poseStack,
                            MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                            float partialTick, List<ActiveAnimation> activeAnimations,
                            @Nullable NodeTransformCallback callback) {

        poseStack.pushPose();

        animTranslation.set(node.defaultTranslation());
        animRotation.set(node.defaultRotation());
        animScale.set(node.defaultScale());

        // アニメーションの合成
        if (activeAnimations != null && !activeAnimations.isEmpty()) {
            for (ActiveAnimation activeAnim : activeAnimations) {
                if (rawData.animations.containsKey(activeAnim.name())) {
                    GlbLoader.GlbAnimation anim = rawData.animations.get(activeAnim.name());
                    applyAnimationToNode(node.name(), anim, activeAnim.timeSeconds(), activeAnim.loop());
                }
            }
        }

        poseStack.translate(animTranslation.x(), animTranslation.y(), animTranslation.z());
        poseStack.mulPose(animRotation);

        // Yaw / Pitch 旋回等のコールバック
        if (callback != null) {
            callback.apply(node.name(), poseStack, partialTick);
        }

        poseStack.scale(animScale.x(), animScale.y(), animScale.z());

        // VBO の超高速描画処理
        for (FastGlbModel.FastMeshPart part : node.meshParts()) {
            renderMeshPartVbo(part, poseStack, packedLight, packedOverlay);
        }

        // 子ノードの再帰
        for (FastGlbModel.FastNode child : node.children()) {
            renderNode(child, rawData, poseStack, bufferSource, packedLight, packedOverlay,
                    partialTick, activeAnimations, callback);
        }

        poseStack.popPose();
    }

    private void renderMeshPartVbo(FastGlbModel.FastMeshPart part, PoseStack poseStack, int packedLight, int packedOverlay) {
        RenderType renderType = part.renderType();

        // RenderType のセットアップ
        renderType.setupRenderState();

        Matrix4f modelViewMatrix = poseStack.last().pose();
        Matrix4f projectionMatrix = RenderSystem.getProjectionMatrix();

        ShaderInstance shader = RenderSystem.getShader();
        if (shader != null) {
            // 1.20.1 では setDefaultUniforms は存在しないため呼び出しを削除

            // ライティングの調整 (必要に応じて Shader 内の Uniform や RenderSystem へ適用)
            int light = (part.material() != null && part.material().isEmissive)
                    ? LightTexture.FULL_BRIGHT
                    : packedLight;

            // カラーモジュレータのセット
            if (shader.COLOR_MODULATOR != null) {
                shader.COLOR_MODULATOR.set(1.0f, 1.0f, 1.0f, 1.0f);
            }

            // シェーダーの変更を確定
            shader.apply();
        }

        // GPU 上の VBO を描画 (CPUループなし)
        if (part.vbo() != null) {
            part.vbo().bind();
            part.vbo().drawWithShader(modelViewMatrix, projectionMatrix, shader);
            VertexBuffer.unbind(); // com.mojang.blaze3d.vertex.VertexBuffer の静的メソッド
        }

        renderType.clearRenderState();
    }

    private void applyAnimationToNode(String nodeName, GlbLoader.GlbAnimation anim, float time, boolean loop) {
        float animTime = (loop && anim.maxTime > 0.0f) ? (time % anim.maxTime) : Math.min(time, anim.maxTime);

        for (GlbLoader.AnimationChannel ch : anim.channels) {
            if (!ch.targetNodeName.equalsIgnoreCase(nodeName)) continue;

            switch (ch.path) {
                case "translation" -> sampleVector3(ch, animTime, animTranslation);
                case "rotation" -> sampleQuaternion(ch, animTime, animRotation);
                case "scale" -> sampleVector3(ch, animTime, animScale);
            }
        }
    }

    private void sampleVector3(GlbLoader.AnimationChannel ch, float time, Vector3f dest) {
        float[] times = ch.keyframeTimes;
        float[] values = ch.keyframeValues;
        if (times.length == 0 || values.length < 3) return;

        if (time <= times[0]) {
            dest.set(values[0], values[1], values[2]);
            return;
        }
        if (time >= times[times.length - 1]) {
            int last = (times.length - 1) * 3;
            dest.set(values[last], values[last + 1], values[last + 2]);
            return;
        }

        int idx = findTimeIndex(times, time);
        float factor = (time - times[idx]) / (times[idx + 1] - times[idx]);
        int v0 = idx * 3;
        int v1 = (idx + 1) * 3;

        dest.set(
                values[v0] + factor * (values[v1] - values[v0]),
                values[v0 + 1] + factor * (values[v1 + 1] - values[v0 + 1]),
                values[v0 + 2] + factor * (values[v1 + 2] - values[v0 + 2])
        );
    }

    private void sampleQuaternion(GlbLoader.AnimationChannel ch, float time, Quaternionf dest) {
        float[] times = ch.keyframeTimes;
        float[] values = ch.keyframeValues;
        if (times.length == 0 || values.length < 4) return;

        if (time <= times[0]) {
            dest.set(values[0], values[1], values[2], values[3]);
            return;
        }
        if (time >= times[times.length - 1]) {
            int last = (times.length - 1) * 4;
            dest.set(values[last], values[last + 1], values[last + 2], values[last + 3]);
            return;
        }

        int idx = findTimeIndex(times, time);
        float factor = (time - times[idx]) / (times[idx + 1] - times[idx]);
        int q0 = idx * 4;
        int q1 = (idx + 1) * 4;

        Quaternionf qStart = new Quaternionf(values[q0], values[q0 + 1], values[q0 + 2], values[q0 + 3]);
        Quaternionf qEnd = new Quaternionf(values[q1], values[q1 + 1], values[q1 + 2], values[q1 + 3]);

        dest.set(qStart.slerp(qEnd, factor));
    }

    private int findTimeIndex(float[] times, float time) {
        for (int i = 0; i < times.length - 1; i++) {
            if (time >= times[i] && time <= times[i + 1]) return i;
        }
        return 0;
    }

    private static final ResourceLocation DUMMY_WHITE_TEXTURE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    public static RenderType getRenderType(@Nullable GlbLoader.MaterialInfo mat) {
        ResourceLocation tex = (mat != null && mat.textureLocation != null) ? mat.textureLocation : DUMMY_WHITE_TEXTURE;

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
}
