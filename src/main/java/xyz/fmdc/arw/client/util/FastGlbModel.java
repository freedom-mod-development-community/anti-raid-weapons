package xyz.fmdc.arw.client.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import xyz.fmdc.arw.client.renderer.GenericFastGlbRenderer;

import java.util.ArrayList;
import java.util.List;

public class FastGlbModel implements AutoCloseable {

    public final GlbLoader.GlbModelData rawData;
    public final FastNode rootNode;

    public FastGlbModel(GlbLoader.GlbModelData rawData) {
        this.rawData = rawData;
        this.rootNode = buildFastNode(rawData.rootNode);
    }

    private FastNode buildFastNode(GlbLoader.GlbNode rawNode) {
        if (rawNode == null) return null;

        List<FastMeshPart> parts = new ArrayList<>();
        for (GlbLoader.MeshPart rawPart : rawNode.meshParts) {
            if (rawPart.positions == null || rawPart.indices == null) continue;

            // VBO の構築 (メインスレッドで実行)
            VertexBuffer vbo = new VertexBuffer(VertexBuffer.Usage.STATIC);
            RenderType renderType = GenericFastGlbRenderer.getRenderType(rawPart.material);

            BufferBuilder builder = Tesselator.getInstance().getBuilder();
            builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.NEW_ENTITY);

            float r = rawPart.baseColorFactor[0];
            float g = rawPart.baseColorFactor[1];
            float b = rawPart.baseColorFactor[2];
            float a = rawPart.baseColorFactor[3];

            Matrix4f identity = new Matrix4f();

            for (int idx : rawPart.indices) {
                int posIdx = idx * 3;
                float vx = rawPart.positions[posIdx];
                float vy = rawPart.positions[posIdx + 1];
                float vz = rawPart.positions[posIdx + 2];

                float nx = 0.0f, ny = 1.0f, nz = 0.0f;
                if (rawPart.normals != null && (idx * 3 + 2) < rawPart.normals.length) {
                    nx = rawPart.normals[idx * 3];
                    ny = rawPart.normals[idx * 3 + 1];
                    nz = rawPart.normals[idx * 3 + 2];
                }

                float u = 0.0f, v = 0.0f;
                if (rawPart.uvs != null && (idx * 2 + 1) < rawPart.uvs.length) {
                    u = rawPart.uvs[idx * 2];
                    v = rawPart.uvs[idx * 2 + 1];
                }

                builder.vertex(identity, vx, vy, vz)
                        .color(r, g, b, a)
                        .uv(u, v)
                        .overlayCoords(0, 10) // デフォルト Overlay
                        .uv2(240)             // デフォルト Light
                        .normal(nx, ny, nz)
                        .endVertex();
            }

            BufferBuilder.RenderedBuffer renderedBuffer = builder.end();
            vbo.bind();
            vbo.upload(renderedBuffer);
            VertexBuffer.unbind();

            parts.add(new FastMeshPart(vbo, renderType, rawPart.material));
        }

        List<FastNode> children = new ArrayList<>();
        for (GlbLoader.GlbNode child : rawNode.children) {
            children.add(buildFastNode(child));
        }

        return new FastNode(rawNode.name, rawNode.translation, rawNode.rotation, rawNode.scale, parts, children);
    }

    @Override
    public void close() {
        RenderSystem.assertOnRenderThread();
        if (rootNode != null) {
            rootNode.close();
        }
    }

    public record FastMeshPart(VertexBuffer vbo, RenderType renderType, GlbLoader.MaterialInfo material) implements AutoCloseable {
        @Override
        public void close() {
            vbo.close();
        }
    }

    public record FastNode(
            String name,
            org.joml.Vector3f defaultTranslation,
            org.joml.Quaternionf defaultRotation,
            org.joml.Vector3f defaultScale,
            List<FastMeshPart> meshParts,
            List<FastNode> children
    ) implements AutoCloseable {
        @Override
        public void close() {
            for (FastMeshPart part : meshParts) part.close();
            for (FastNode child : children) child.close();
        }
    }
}
