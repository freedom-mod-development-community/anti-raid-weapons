package xyz.fmdc.arw.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

/**
 * 低レイヤーAPIを使用して、滑らかな同心円を描く方法
 */
public class RadarRenderUtils {
    /**
     * Minecraft 1.20.1 Forge 用の滑らかな円描画処理
     */
    public static void drawSmoothCircle(GuiGraphics guiGraphics, float centerX, float centerY, float radius, int segments, int argbColor) {
        // ARGBカラーの分解
        float a = (float)(argbColor >> 24 & 255) / 255.0F;
        float r = (float)(argbColor >> 16 & 255) / 255.0F;
        float g = (float)(argbColor >> 8 & 255) / 255.0F;
        float b = (float)(argbColor & 255) / 255.0F;

        // レンダリング状態のセットアップ
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f matrix = guiGraphics.pose().last().pose();

        // 1.20.1 形式の Tessellator と BufferBuilder の呼び出し
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuilder();

        // 1.20.1 では begin() でフォーマットと描画モードを指定
        bufferBuilder.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i <= segments; i++) {
            double theta = 2.0 * Math.PI * i / segments;
            float x = (float) (centerX + radius * Math.cos(theta));
            float y = (float) (centerY + radius * Math.sin(theta));

            // 1.20.1 の頂点追加記法 (.vertex -> .color -> .endVertex)
            bufferBuilder.vertex(matrix, x, y, 0.0F)
                    .color(r, g, b, a)
                    .endVertex();
        }

        // 1.20.1 では tessellator.end() で描画を実行
        tessellator.end();

        RenderSystem.disableBlend();
    }
}
