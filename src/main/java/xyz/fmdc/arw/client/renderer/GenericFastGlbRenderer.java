package xyz.fmdc.arw.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.Minecraft;
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

    @FunctionalInterface
    public interface NodePostRenderCallback {
        void render(String nodeName, PoseStack poseStack, MultiBufferSource bufferSource,
                    int packedLight, int packedOverlay, float partialTick);
    }

    public record ActiveAnimation(String name, float timeSeconds, boolean loop) {
        public ActiveAnimation(String name, float timeSeconds) {
            this(name, timeSeconds, false);
        }
    }

    // 計算用インスタンスの再利用 (GC 発生の完全防止)
    private final Vector3f animTranslation = new Vector3f();
    private final Quaternionf animRotation = new Quaternionf();
    private final Vector3f animScale = new Vector3f();

    // Quaternion 補間計算用の作業用インスタンス
    private final Quaternionf qStart = new Quaternionf();
    private final Quaternionf qEnd = new Quaternionf();

    public void render(FastGlbModel fastModel, PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay, float partialTick,
                       List<ActiveAnimation> activeAnimations,
                       @Nullable NodeTransformCallback callback) {
        render(fastModel, poseStack, bufferSource, packedLight, packedOverlay, partialTick, activeAnimations, callback, null, true);
    }

    public void render(FastGlbModel fastModel, PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay, float partialTick,
                       List<ActiveAnimation> activeAnimations,
                       @Nullable NodeTransformCallback callback,
                       boolean centerBlockOffset) {
        render(fastModel, poseStack, bufferSource, packedLight, packedOverlay, partialTick, activeAnimations, callback, null, centerBlockOffset);
    }

    public void render(FastGlbModel fastModel, PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay, float partialTick,
                       List<ActiveAnimation> activeAnimations,
                       @Nullable NodeTransformCallback callback,
                       @Nullable NodePostRenderCallback postRenderCallback,
                       boolean centerBlockOffset) {

        if (fastModel == null || fastModel.rootNode == null) return;

        poseStack.pushPose();
        if (centerBlockOffset) {
            poseStack.translate(0.5, 0.0, 0.5);
        }

        renderNode(fastModel.rootNode, fastModel.rawData, poseStack, bufferSource,
                packedLight, packedOverlay, partialTick, activeAnimations, callback, postRenderCallback);

        poseStack.popPose();

        // 最後に1度だけ VBO のバインドを解除
        VertexBuffer.unbind();
    }

    private void renderNode(FastGlbModel.FastNode node, GlbLoader.GlbModelData rawData, PoseStack poseStack,
                            MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                            float partialTick, List<ActiveAnimation> activeAnimations,
                            @Nullable NodeTransformCallback callback,
                            @Nullable NodePostRenderCallback postRenderCallback) {

        poseStack.pushPose();

        animTranslation.set(node.defaultTranslation());
        animRotation.set(node.defaultRotation());
        animScale.set(node.defaultScale());

        if (activeAnimations != null && !activeAnimations.isEmpty()) {
            for (int i = 0; i < activeAnimations.size(); i++) {
                ActiveAnimation activeAnim = activeAnimations.get(i);
                GlbLoader.GlbAnimation anim = rawData.animations.get(activeAnim.name());
                if (anim != null) {
                    applyAnimationToNode(node.name(), anim, activeAnim.timeSeconds(), activeAnim.loop());
                }
            }
        }

        poseStack.translate(animTranslation.x(), animTranslation.y(), animTranslation.z());
        poseStack.mulPose(animRotation);

        if (callback != null) {
            callback.apply(node.name(), poseStack, partialTick);
        }

        poseStack.scale(animScale.x(), animScale.y(), animScale.z());

        // メッシュパーツ描画
        for (FastGlbModel.FastMeshPart part : node.meshParts()) {
            renderMeshPartVbo(part, poseStack, packedLight, packedOverlay);
        }

        // ノード描画後の追加処理（アタッチメントパーツやミサイル等の描画）
        if (postRenderCallback != null) {
            postRenderCallback.render(node.name(), poseStack, bufferSource, packedLight, packedOverlay, partialTick);
        }

        // 子ノードの再帰
        for (FastGlbModel.FastNode child : node.children()) {
            renderNode(child, rawData, poseStack, bufferSource, packedLight, packedOverlay,
                    partialTick, activeAnimations, callback, postRenderCallback);
        }

        poseStack.popPose();
    }

    private void renderMeshPartVbo(FastGlbModel.FastMeshPart part, PoseStack poseStack, int packedLight, int packedOverlay) {
        if (part.vbo() == null) return;

        RenderType renderType = part.renderType();

        // 深度テストと深度書き込みの有効化
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);

        // フォグの距離を上書きするコードを削除する（水中のフォグ効果を生かす）
        // RenderSystem.setShaderFogStart(10000.0f); <-- 削除
        // RenderSystem.setShaderFogEnd(20000.0f);   <-- 削除

        renderType.setupRenderState();

        Matrix4f modelViewMatrix = poseStack.last().pose();
        Matrix4f projectionMatrix = RenderSystem.getProjectionMatrix();

        ShaderInstance shader = RenderSystem.getShader();
        if (shader != null) {
            if (shader.COLOR_MODULATOR != null) {
                shader.COLOR_MODULATOR.set(1.0f, 1.0f, 1.0f, 1.0f);
            }

            int light = (part.material() != null && part.material().isEmissive)
                    ? net.minecraft.client.renderer.LightTexture.FULL_BRIGHT
                    : packedLight;

            // 【修正2】PackedLight（明暗情報）および Overlay をシェーダーに設定
            // バニラシェーダーの Sampler / Uniform にライトマップテクスチャをバインドします
            if (shader.LIGHT0_DIRECTION != null) {
                RenderSystem.setShaderLights(
                        new Vector3f(0.2F, 1.0F, -0.7F).normalize(),
                        new Vector3f(-0.2F, -1.0F, 0.7F).normalize()
                );
            }

            // 1.20.1 等の標準シェーダーに対してライトテクスチャを有効化する
            Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();

            shader.apply();
        }

        // VBO の描画
        part.vbo().bind();
        part.vbo().drawWithShader(modelViewMatrix, projectionMatrix, shader);

        // 【修正3】描画完了後にライトレイヤーを消去・ステートクリア
        Minecraft.getInstance().gameRenderer.lightTexture().turnOffLightLayer();
        renderType.clearRenderState();
    }

    private void applyAnimationToNode(String nodeName, GlbLoader.GlbAnimation anim, float time, boolean loop) {
        float animTime = (loop && anim.maxTime > 0.0f) ? (time % anim.maxTime) : Math.min(time, anim.maxTime);

        // 拡張ポイント: 事前に nodeName -> channels の Map を構築しておけば検索をさらに高速化可能
        List<GlbLoader.AnimationChannel> channels = anim.channels;
        for (int i = 0; i < channels.size(); i++) {
            GlbLoader.AnimationChannel ch = channels.get(i);
            if (!ch.targetNodeName.equals(nodeName)) continue;

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
        int len = times.length;
        if (len == 0 || values.length < 3) return;

        if (time <= times[0]) {
            dest.set(values[0], values[1], values[2]);
            return;
        }
        if (time >= times[len - 1]) {
            int last = (len - 1) * 3;
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
        int len = times.length;
        if (len == 0 || values.length < 4) return;

        if (time <= times[0]) {
            dest.set(values[0], values[1], values[2], values[3]);
            return;
        }
        if (time >= times[len - 1]) {
            int last = (len - 1) * 4;
            dest.set(values[last], values[last + 1], values[last + 2], values[last + 3]);
            return;
        }

        int idx = findTimeIndex(times, time);
        float factor = (time - times[idx]) / (times[idx + 1] - times[idx]);
        int q0 = idx * 4;
        int q1 = (idx + 1) * 4;

        // GC 発生を回避するため再利用フィールドへセット
        qStart.set(values[q0], values[q0 + 1], values[q0 + 2], values[q0 + 3]);
        qEnd.set(values[q1], values[q1 + 1], values[q1 + 2], values[q1 + 3]);

        dest.set(qStart.slerp(qEnd, factor));
    }

    // 二分探索 (Binary Search) による高速キーフレームインデックス検索
    private int findTimeIndex(float[] times, float time) {
        int low = 0;
        int high = times.length - 2;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (time < times[mid]) {
                high = mid - 1;
            } else if (time >= times[mid + 1]) {
                low = mid + 1;
            } else {
                return mid;
            }
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
