package xyz.fmdc.arw.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class RadarTest extends Screen {

    private final BlockPos radarPos; // レーダーの中心座標（BlockEntityの座標等）

    // 描画設定パラメータ
    private double currentRange = 100.0; // 現在表示中の半径（m）
    private static final double MIN_RANGE = 20.0;
    private static final double MAX_RANGE = 500.0;

    private int radarRadiusPx = 0;

    // 画面上で検出されたエンティティの情報（クリック判定用）
    private final List<RadarTarget> trackedTargets = new ArrayList<>();
    private Entity selectedEntity = null;

    public RadarTest(BlockPos radarPos) {
        super(Component.literal("Tactical Radar"));
        this.radarPos = radarPos;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        // GUI描画領域の半径（ピクセル）
        radarRadiusPx = (int) (Math.min(this.width, this.height) * 0.40f);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // 1. レーダー背景とフレームの描画
        drawRadarBackground(guiGraphics, centerX, centerY);

        // 2. グリッド（十字線・同心円・距離テキスト）の描画
        drawRadarGrid(guiGraphics, centerX, centerY);

        // 3. エンティティの取得とプロット描画
        updateAndDrawTargets(guiGraphics, centerX, centerY);

        // 4. 選択されたエンティティの詳細情報（HUD）描画
        if (this.selectedEntity != null && this.selectedEntity.isAlive()) {
            drawEntityDetails(guiGraphics, this.selectedEntity);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * レーダーの背景（黒色の円）
     */
    private void drawRadarBackground(GuiGraphics guiGraphics, int centerX, int centerY) {
        // 簡易的な黒丸背景（または四角枠）
        guiGraphics.fill(centerX - radarRadiusPx, centerY - radarRadiusPx,
                centerX + radarRadiusPx, centerY + radarRadiusPx, 0xDD001100); // ダークグリーン透過
    }

    /**
     * 十字線、同心円、距離テキストの描画
     */
    private void drawRadarGrid(GuiGraphics guiGraphics, int centerX, int centerY) {
        int colorGrid = 0xFF00FF00; // 蛍光グリーン
        int colorText = 0xFF88FF88;

        // 十字線の描画
        guiGraphics.hLine(centerX - radarRadiusPx, centerX + radarRadiusPx, centerY, colorGrid);
        guiGraphics.vLine(centerX, centerY - radarRadiusPx, centerY + radarRadiusPx, colorGrid);

        double stepMeters = calculateStepMeters(currentRange);

        // 同心円と交点への距離テキスト描画
        for (double dist = stepMeters; dist < currentRange; dist += stepMeters) {
            int radiusPx = (int) ((dist / currentRange) * radarRadiusPx);

            RadarRenderUtils.drawSmoothCircle(guiGraphics, centerX, centerY, radiusPx, 32, 0xFFFFFFFF);
            // 十字と円が交わる点（上下左右の4箇所）に距離を表示
            String distText = String.format("%dm", (int) dist);
            // 上の交点
            guiGraphics.drawString(this.font, distText, centerX + 2, centerY - radiusPx - 4, colorText, false);
            // 右の交点
            guiGraphics.drawString(this.font, distText, centerX + radiusPx + 2, centerY - 4, colorText, false);
        }

        // 現在の最外殻レンジ表示
        guiGraphics.drawString(this.font, String.format("RANGE: %dm", (int) currentRange),
                centerX - radarRadiusPx, centerY - radarRadiusPx - 12, 0xFFFFFFFF, false);
    }

    private static double calculateStepMeters(double range) {
        if (range <= 0) return 1.0;
        // rangeの等しい桁数で最も小さい値
        double magnitude = Math.pow(10, Math.floor(Math.log10(range)));

        // 1〜10の範囲に正規化
        double normalized = range / magnitude;

        // 桁数を掛け直して最終的な stepMeters を算出
        return Math.floor(normalized+0.5) * magnitude / 4;
    }

    /**
     * 周囲のEntityを取得してプロット（描画）
     */
    private void updateAndDrawTargets(GuiGraphics guiGraphics, int centerX, int centerY) {
        if (this.minecraft == null || this.minecraft.level == null) return;

        this.trackedTargets.clear();
        Vec3 centerVec = Vec3.atCenterOf(this.radarPos);

        // 500m以内のAABB検索（実際の最大値で検索）
        AABB searchBox = new AABB(this.radarPos).inflate(MAX_RANGE);
        List<Entity> entities = this.minecraft.level.getEntities((Entity) null, searchBox, e -> e != this.minecraft.player);

        for (Entity entity : entities) {
            double dx = entity.getX() - centerVec.x();
            double dz = entity.getZ() - centerVec.z();
            double distanceSq = dx * dx + dz * dz;

            // 表示レンジ外ならスキップ
            if (distanceSq > currentRange * currentRange) continue;

            // ワールド座標(X, Z) から レーダー画面相対座標(Px) への変換
            // マイクラのZ軸は南（下）方向なので、ScreenのY座標に対応
            int pixelX = centerX + (int) ((dx / currentRange) * radarRadiusPx);
            int pixelY = centerY + (int) ((dz / currentRange) * radarRadiusPx);

            // 画面上のクリック判定用にリストに保持
            this.trackedTargets.add(new RadarTarget(entity, pixelX, pixelY));

            // プロットの描画（選択中なら赤、それ以外は黄色）
            int color = (entity == this.selectedEntity) ? 0xFFFF0000 : 0xFFFFFF00;
            guiGraphics.fill(pixelX - 2, pixelY - 2, pixelX + 2, pixelY + 2, color);
        }
    }

    /**
     * 左クリックされた選択エンティティの詳細情報表示（画面右上に固定表示）
     */
    private void drawEntityDetails(GuiGraphics guiGraphics, Entity entity) {
        int infoX = 10;
        int infoY = 10;
        int color = 0xFFFFFFFF;

        Vec3 centerVec = Vec3.atCenterOf(this.radarPos);
        double dist = entity.position().distanceTo(centerVec);

        guiGraphics.fill(infoX - 5, infoY - 5, infoX + 150, infoY + 60, 0xCC000000);
        guiGraphics.renderOutline(infoX - 5, infoY - 5, 155, 65, 0xFF00FF00);

        guiGraphics.drawString(this.font, "TARGET INFO", infoX, infoY, 0xFF00FF00, false);
        guiGraphics.drawString(this.font, "Type: " + entity.getType().getDescription().getString(), infoX, infoY + 12, color, false);
        guiGraphics.drawString(this.font, String.format("Dist: %.1fm", dist), infoX, infoY + 24, color, false);
        guiGraphics.drawString(this.font, String.format("Pos : %.0f, %.0f, %.0f", entity.getX(), entity.getY(), entity.getZ()), infoX, infoY + 36, color, false);
    }

    /**
     * 要件1：マウスホイールによるズーム処理
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta > 0) {
            // 拡大（表示レンジを小さく）
            this.currentRange = Math.max(MIN_RANGE, this.currentRange - 10.0);
        } else if (delta < 0) {
            // 縮小（表示レンジを大きく）
            this.currentRange = Math.min(MAX_RANGE, this.currentRange + 10.0);
        }
        return true;
    }

    /**
     * 要件3：クリック判定によるエンティティ選択
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // 左クリック
            for (RadarTarget target : this.trackedTargets) {
                // プロットを中心に 6x6 ピクセルの判定領域
                if (Math.abs(mouseX - target.screenX) <= 3 && Math.abs(mouseY - target.screenY) <= 3) {
                    this.selectedEntity = target.entity;
                    return true;
                }
            }
            // 空白部分をクリックしたら選択解除
            this.selectedEntity = null;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // 内部データ構造
    private record RadarTarget(Entity entity, int screenX, int screenY) {}
}