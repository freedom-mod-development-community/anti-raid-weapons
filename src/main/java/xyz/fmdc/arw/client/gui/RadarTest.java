package xyz.fmdc.arw.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import xyz.fmdc.arw.api.TrackedTarget;
import xyz.fmdc.arw.common.blockentity.sensor.SearchRadarBlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RadarTest extends Screen {

    private final BlockPos radarPos;
    private final SearchRadarBlockEntity radarBlockEntity; // BEへの参照

    private double currentRange = 100.0;
    private static final double MIN_RANGE = 20.0;
    private static final double MAX_RANGE = 500.0;

    private int radarRadiusPx = 0;

    // GUI上でプロットされたターゲット（クリック判定用）
    private final List<GuiRadarTarget> guiTargets = new ArrayList<>();
    private UUID selectedTargetId = null; // Entity参照ではなくUUIDで選択状態を保持

    public RadarTest(SearchRadarBlockEntity radarBlockEntity) {
        super(Component.literal("Tactical Radar"));
        this.radarBlockEntity = radarBlockEntity;
        this.radarPos = radarBlockEntity.getBlockPos();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        radarRadiusPx = (int) (Math.min(this.width, this.height) * 0.40f);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        drawRadarBackground(guiGraphics, centerX, centerY);
        drawRadarGrid(guiGraphics, centerX, centerY);

        // 検索を行わず、BlockEntityが保持している記憶リストを描画
        drawTrackedTargets(guiGraphics, centerX, centerY);

        // 選択されたターゲットの詳細描画
        if (this.selectedTargetId != null) {
            TrackedTarget selected = this.radarBlockEntity.getTrackedTargets().get(this.selectedTargetId);
            if (selected != null) {
                drawTargetDetails(guiGraphics, selected);
            } else {
                this.selectedTargetId = null; // ロストした場合は選択解除
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void drawRadarBackground(GuiGraphics guiGraphics, int centerX, int centerY) {
        guiGraphics.fill(centerX - radarRadiusPx, centerY - radarRadiusPx,
                centerX + radarRadiusPx, centerY + radarRadiusPx, 0xDD001100);
    }

    private void drawRadarGrid(GuiGraphics guiGraphics, int centerX, int centerY) {
        int colorGrid = 0xFF00FF00;
        int colorText = 0xFF88FF88;

        guiGraphics.hLine(centerX - radarRadiusPx, centerX + radarRadiusPx, centerY, colorGrid);
        guiGraphics.vLine(centerX, centerY - radarRadiusPx, centerY + radarRadiusPx, colorGrid);

        double stepMeters = calculateStepMeters(currentRange);

        for (double dist = stepMeters; dist < currentRange; dist += stepMeters) {
            int radiusPx = (int) ((dist / currentRange) * radarRadiusPx);
            RadarRenderUtils.drawSmoothCircle(guiGraphics, centerX, centerY, radiusPx, 32, 0xFFFFFFFF);

            String distText = String.format("%dm", (int) dist);
            guiGraphics.drawString(this.font, distText, centerX + 2, centerY - radiusPx - 4, colorText, false);
            guiGraphics.drawString(this.font, distText, centerX + radiusPx + 2, centerY - 4, colorText, false);
        }

        guiGraphics.drawString(this.font, String.format("RANGE: %dm", (int) currentRange),
                centerX - radarRadiusPx, centerY - radarRadiusPx - 12, 0xFFFFFFFF, false);
    }

    private static double calculateStepMeters(double range) {
        if (range <= 0) return 1.0;
        double magnitude = Math.pow(10, Math.floor(Math.log10(range)));
        double normalized = range / magnitude;
        return Math.floor(normalized + 0.5) * magnitude / 4;
    }

    /**
     * 重いAABB検索を行わず、BE内の trackedTargets を元に描画する
     */
    private void drawTrackedTargets(GuiGraphics guiGraphics, int centerX, int centerY) {
        this.guiTargets.clear();
        if (this.radarBlockEntity == null) return;

        Vec3 centerVec = Vec3.atCenterOf(this.radarPos);

        // BlockEntityが記憶している追尾目標を取得
        for (TrackedTarget target : this.radarBlockEntity.getTrackedTargets().values()) {
            Vec3 targetPos = target.getLastKnownPos();

            double dx = targetPos.x() - centerVec.x();
            double dz = targetPos.z() - centerVec.z();
            double distanceSq = dx * dx + dz * dz;

            // ズームレンジ外なら表示しない
            if (distanceSq > currentRange * currentRange) continue;

            // スクリーン座標への計算
            int pixelX = centerX + (int) ((dx / currentRange) * radarRadiusPx);
            int pixelY = centerY + (int) ((dz / currentRange) * radarRadiusPx);

            // クリック判定用リストに記録
            this.guiTargets.add(new GuiRadarTarget(target.getEntityId(), pixelX, pixelY));

            // 選択中かどうかの色分け
            boolean isSelected = target.getEntityId().equals(this.selectedTargetId);
            int color = isSelected ? 0xFFFF0000 : 0xFFFFFF00;

            // プロット描画
            guiGraphics.fill(pixelX - 2, pixelY - 2, pixelX + 2, pixelY + 2, color);
        }
    }

    /**
     * 選択中ターゲットの詳細情報HUD
     */
    private void drawTargetDetails(GuiGraphics guiGraphics, TrackedTarget target) {
        int infoX = 10;
        int infoY = 10;
        int color = 0xFFFFFFFF;

        Vec3 centerVec = Vec3.atCenterOf(this.radarPos);
        Vec3 pos = target.getLastKnownPos();
        double dist = pos.distanceTo(centerVec);

        guiGraphics.fill(infoX - 5, infoY - 5, infoX + 160, infoY + 65, 0xCC000000);
        guiGraphics.renderOutline(infoX - 5, infoY - 5, 165, 70, 0xFF00FF00);

        String entityTypeName = target.getEntity() != null
                ? target.getEntity().getType().getDescription().getString()
                : "Unknown";

        guiGraphics.drawString(this.font, "TRACKED TARGET", infoX, infoY, 0xFF00FF00, false);
        guiGraphics.drawString(this.font, "Type: " + entityTypeName, infoX, infoY + 12, color, false);
        guiGraphics.drawString(this.font, String.format("Dist: %.1fm", dist), infoX, infoY + 24, color, false);
        guiGraphics.drawString(this.font, String.format("Pos : %.0f, %.0f, %.0f", pos.x, pos.y, pos.z), infoX, infoY + 36, color, false);

        //// 残り記憶時間や速度なども表示可能
        Vec3 vel = target.getLastKnownVelocity();
        if (vel != null) {
            double speed = vel.length() * 20.0; // m/s換算
            guiGraphics.drawString(this.font, String.format("Spd : %.1fm/s", speed), infoX, infoY + 48, 0xFF88FF88, false);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta > 0) {
            this.currentRange = Math.max(MIN_RANGE, this.currentRange - 10.0);
        } else if (delta < 0) {
            this.currentRange = Math.min(MAX_RANGE, this.currentRange + 10.0);
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (GuiRadarTarget target : this.guiTargets) {
                if (Math.abs(mouseX - target.screenX) <= 3 && Math.abs(mouseY - target.screenY) <= 3) {
                    this.selectedTargetId = target.targetId;
                    return true;
                }
            }
            this.selectedTargetId = null;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // GUI内での判定用データ構造
    private record GuiRadarTarget(UUID targetId, int screenX, int screenY) {}
}