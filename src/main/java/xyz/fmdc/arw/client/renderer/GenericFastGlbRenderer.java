package xyz.fmdc.arw.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import xyz.fmdc.arw.client.util.CustomRenderTypes;
import xyz.fmdc.arw.client.util.FastGlbModel;
import xyz.fmdc.arw.client.util.GlbLoader;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GenericFastGlbRenderer {

    @Nullable
    public Matrix4f getGlobalTransform(String nodeName) {
        Matrix4f matrix = globalTransforms.get(nodeName);
        return matrix != null ? new Matrix4f(matrix) : null;
    }

    @FunctionalInterface
    public interface BoneTransformCallback {
        void apply(String boneName, Vector3f translation, Quaternionf rotation, Vector3f scale);
    }

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

    // 計算用インスタンスの再利用 (GC 発生防止)
    private final Vector3f animTranslation = new Vector3f();
    private final Quaternionf animRotation = new Quaternionf();
    private final Vector3f animScale = new Vector3f();

    private final Quaternionf qStart = new Quaternionf();
    private final Quaternionf qEnd = new Quaternionf();

    // トランスフォーム保持用 Map
    private final Map<String, Matrix4f> globalTransforms = new HashMap<>();
    private final Map<String, Matrix4f> localTransforms = new HashMap<>();

    private final Matrix4f tempNodeMatrix = new Matrix4f();
    private final Vector4f skinPos = new Vector4f();
    private final Vector3f skinNorm = new Vector3f();
    private final Vector4f tempPos = new Vector4f();
    private final Vector3f tempNorm = new Vector3f();

    // --- render のオーバーロード ---

    public void render(FastGlbModel fastModel, PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay, float partialTick,
                       List<ActiveAnimation> activeAnimations,
                       @Nullable NodeTransformCallback callback) {
        render(fastModel, poseStack, bufferSource, packedLight, packedOverlay, partialTick, activeAnimations, callback, null, null, true);
    }

    public void render(FastGlbModel fastModel, PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay, float partialTick,
                       List<ActiveAnimation> activeAnimations,
                       @Nullable NodeTransformCallback callback,
                       boolean centerBlockOffset) {
        render(fastModel, poseStack, bufferSource, packedLight, packedOverlay, partialTick, activeAnimations, callback, null, null, centerBlockOffset);
    }

    public void render(FastGlbModel fastModel, PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay, float partialTick,
                       List<ActiveAnimation> activeAnimations,
                       @Nullable NodeTransformCallback callback,
                       @Nullable NodePostRenderCallback postRenderCallback,
                       boolean centerBlockOffset) {
        render(fastModel, poseStack, bufferSource, packedLight, packedOverlay, partialTick, activeAnimations, callback, postRenderCallback, null, centerBlockOffset);
    }

    public void render(FastGlbModel fastModel, PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay, float partialTick,
                       List<ActiveAnimation> activeAnimations,
                       @Nullable NodeTransformCallback callback,
                       @Nullable NodePostRenderCallback postRenderCallback,
                       @Nullable BoneTransformCallback boneCallback,
                       boolean centerBlockOffset) {

        if (fastModel == null || fastModel.rootNode == null) return;

        // 1. Pass 1: 各ノードのローカルおよびグローバル行列を一括計算
        globalTransforms.clear();
        localTransforms.clear();
        computeGlobalTransforms(fastModel.rootNode, fastModel.rawData, new Matrix4f(), activeAnimations, boneCallback);

        poseStack.pushPose();
        if (centerBlockOffset) {
            poseStack.translate(0.5, 0.0, 0.5);
        }

        // 2. Pass 2: Pass 1 の計算結果を使って描画
        renderNode(fastModel.rootNode, fastModel.rawData, poseStack, bufferSource,
                packedLight, packedOverlay, partialTick, callback, postRenderCallback);

        poseStack.popPose();

        // 【修正点 1】VertexBuffer.unbind() を削除 (OpenGL/RenderTypeのステート破壊防止)
    }

    /**
     * Pass 1: 各ノード・ボーンのローカル行列およびグローバル行列を計算・保持する
     */
    private void computeGlobalTransforms(FastGlbModel.FastNode node, GlbLoader.GlbModelData rawData,
                                         Matrix4f parentTransform, List<ActiveAnimation> activeAnimations,
                                         @Nullable BoneTransformCallback boneCallback) {

        animTranslation.set(node.defaultTranslation());
        animRotation.set(node.defaultRotation());
        animScale.set(node.defaultScale());

        if (activeAnimations != null && !activeAnimations.isEmpty()) {
            for (ActiveAnimation activeAnim : activeAnimations) {
                GlbLoader.GlbAnimation anim = rawData.animations.get(activeAnim.name());
                if (anim != null) {
                    applyAnimationToNode(node.name(), anim, activeAnim.timeSeconds(), activeAnim.loop());
                }
            }
        }

        // ボーン操作コールバック
        if (boneCallback != null) {
            boneCallback.apply(node.name(), animTranslation, animRotation, animScale);
        }

        // ローカル変換行列を生成
        tempNodeMatrix.translationRotateScale(
                animTranslation.x(), animTranslation.y(), animTranslation.z(),
                animRotation.x(), animRotation.y(), animRotation.z(), animRotation.w(),
                animScale.x(), animScale.y(), animScale.z()
        );

        localTransforms.put(node.name(), new Matrix4f(tempNodeMatrix));

        // グローバル変換行列の計算・保存
        Matrix4f currentGlobal = new Matrix4f(parentTransform).mul(tempNodeMatrix);
        globalTransforms.put(node.name(), currentGlobal);

        for (FastGlbModel.FastNode child : node.children()) {
            computeGlobalTransforms(child, rawData, currentGlobal, activeAnimations, boneCallback);
        }
    }

    /**
     * Pass 2: Pass 1 で確定した行列を用いてメッシュを描画
     */
    private void renderNode(FastGlbModel.FastNode node, GlbLoader.GlbModelData rawData, PoseStack poseStack,
                            MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                            float partialTick,
                            @Nullable NodeTransformCallback callback,
                            @Nullable NodePostRenderCallback postRenderCallback) {

        poseStack.pushPose();

        // Pass 1 で計算したローカル変換を PoseStack に適用
        Matrix4f localMat = localTransforms.get(node.name());
        if (localMat != null) {
            poseStack.last().pose().mul(localMat);
        }

        if (callback != null) {
            callback.apply(node.name(), poseStack, partialTick);
        }

        // メッシュ描画
        for (FastGlbModel.FastMeshPart part : node.meshParts()) {
            if (part.isSkinned()) {
                // 【修正点 2】スキンメッシュは BufferSource 経由で描画（VBO書き換えを防止）
                renderSkinnedMeshPart(part, poseStack, bufferSource, packedLight, packedOverlay);
            } else {
                renderMeshPartVbo(part, poseStack, packedLight, packedOverlay);
            }
        }

        if (postRenderCallback != null) {
            postRenderCallback.render(node.name(), poseStack, bufferSource, packedLight, packedOverlay, partialTick);
        }

        // 子ノードの描画
        for (FastGlbModel.FastNode child : node.children()) {
            renderNode(child, rawData, poseStack, bufferSource, packedLight, packedOverlay,
                    partialTick, callback, postRenderCallback);
        }

        poseStack.popPose();
    }

    /**
     * スキンメッシュの描画（MultiBufferSource / VertexConsumer 経由）
     */
    private void renderSkinnedMeshPart(FastGlbModel.FastMeshPart part, PoseStack poseStack,
                                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        GlbLoader.MeshPart raw = part.rawPart();
        if (raw == null || raw.skin == null) return;

        GlbLoader.GlbSkin skin = raw.skin;
        Matrix4f[] jointMatrices = new Matrix4f[skin.jointNodeNames.size()];

        // 各ボーンの JointMatrix 計算
        for (int i = 0; i < skin.jointNodeNames.size(); i++) {
            String jointName = skin.jointNodeNames.get(i);
            Matrix4f globalJoint = globalTransforms.getOrDefault(jointName, new Matrix4f());
            Matrix4f invBind = skin.inverseBindMatrices.get(i);

            jointMatrices[i] = new Matrix4f(globalJoint).mul(invBind);
        }

        VertexConsumer consumer = bufferSource.getBuffer(part.renderType());
        PoseStack.Pose lastPose = poseStack.last();
        Matrix4f poseMatrix = lastPose.pose();

        float r = raw.baseColorFactor[0];
        float g = raw.baseColorFactor[1];
        float b = raw.baseColorFactor[2];
        float a = raw.baseColorFactor[3];

        for (int idx : raw.indices) {
            int posIdx = idx * 3;
            float vx = raw.positions[posIdx];
            float vy = raw.positions[posIdx + 1];
            float vz = raw.positions[posIdx + 2];

            float nx = 0.0f, ny = 1.0f, nz = 0.0f;
            if (raw.normals != null && (idx * 3 + 2) < raw.normals.length) {
                nx = raw.normals[idx * 3];
                ny = raw.normals[idx * 3 + 1];
                nz = raw.normals[idx * 3 + 2];
            }

            int jIdx = idx * 4;
            int j0 = raw.joints[jIdx];
            int j1 = raw.joints[jIdx + 1];
            int j2 = raw.joints[jIdx + 2];
            int j3 = raw.joints[jIdx + 3];

            float w0 = raw.weights[jIdx];
            float w1 = raw.weights[jIdx + 1];
            float w2 = raw.weights[jIdx + 2];
            float w3 = raw.weights[jIdx + 3];

            skinPos.set(0, 0, 0, 0);
            skinNorm.set(0, 0, 0);

            applyJointWeight(j0, w0, vx, vy, vz, nx, ny, nz, jointMatrices);
            applyJointWeight(j1, w1, vx, vy, vz, nx, ny, nz, jointMatrices);
            applyJointWeight(j2, w2, vx, vy, vz, nx, ny, nz, jointMatrices);
            applyJointWeight(j3, w3, vx, vy, vz, nx, ny, nz, jointMatrices);

            float u = 0.0f, v = 0.0f;
            if (raw.uvs != null && (idx * 2 + 1) < raw.uvs.length) {
                u = raw.uvs[idx * 2];
                v = raw.uvs[idx * 2 + 1];
            }

            // 計算した頂点を直接 BufferSource へ送る
            consumer.vertex(poseMatrix, skinPos.x(), skinPos.y(), skinPos.z())
                    .color(r, g, b, a)
                    .uv(u, v)
                    .overlayCoords(packedOverlay)
                    .uv2(packedLight)
                    .normal(lastPose.normal(), skinNorm.x(), skinNorm.y(), skinNorm.z())
                    .endVertex();
        }
    }

    private void applyJointWeight(int jointIdx, float weight, float vx, float vy, float vz,
                                  float nx, float ny, float nz, Matrix4f[] jointMatrices) {
        if (weight <= 0.0001f || jointIdx < 0 || jointIdx >= jointMatrices.length) return;

        Matrix4f jMat = jointMatrices[jointIdx];

        tempPos.set(vx, vy, vz, 1.0f);
        tempPos.mul(jMat);
        skinPos.add(tempPos.x() * weight, tempPos.y() * weight, tempPos.z() * weight, 0);

        tempNorm.set(nx, ny, nz);
        tempNorm.mulDirection(jMat);
        skinNorm.add(tempNorm.x() * weight, tempNorm.y() * weight, tempNorm.z() * weight);
    }

    /**
     * 非スキンメッシュ（静的VBO）の描画
     */
    private void renderMeshPartVbo(FastGlbModel.FastMeshPart part, PoseStack poseStack, int packedLight, int packedOverlay) {
        if (part.vbo() == null) return;

        RenderType renderType = part.renderType();

        // 【修正点 4】ステートの強制変更を整理し、安全にシェーダーとVBOを適用
        renderType.setupRenderState();

        Matrix4f modelViewMatrix = poseStack.last().pose();
        Matrix4f projectionMatrix = RenderSystem.getProjectionMatrix();

        ShaderInstance shader = RenderSystem.getShader();
        if (shader != null) {
            if (shader.COLOR_MODULATOR != null) {
                shader.COLOR_MODULATOR.set(1.0f, 1.0f, 1.0f, 1.0f);
            }

            if (shader.LIGHT0_DIRECTION != null) {
                RenderSystem.setShaderLights(
                        new Vector3f(0.2F, 1.0F, -0.7F).normalize(),
                        new Vector3f(-0.2F, -1.0F, 0.7F).normalize()
                );
            }

            Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();
            shader.apply();
        }

        part.vbo().bind();
        part.vbo().drawWithShader(modelViewMatrix, projectionMatrix, shader);

        Minecraft.getInstance().gameRenderer.lightTexture().turnOffLightLayer();
        renderType.clearRenderState();
    }

    private void applyAnimationToNode(String nodeName, GlbLoader.GlbAnimation anim, float time, boolean loop) {
        float animTime = (loop && anim.maxTime > 0.0f) ? (time % anim.maxTime) : Math.min(time, anim.maxTime);

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

        // 【修正点 5】単一キーフレーム対応
        if (len == 1 || time <= times[0]) {
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

        if (len == 1 || time <= times[0]) {
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

        qStart.set(values[q0], values[q0 + 1], values[q0 + 2], values[q0 + 3]);
        qEnd.set(values[q1], values[q1 + 1], values[q1 + 2], values[q1 + 3]);

        // JOMLの slerp は内部で自動的に最短経路（Shortest Path）補間を行って dest に結果を格納してくれます
        qStart.slerp(qEnd, factor, dest);
    }

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