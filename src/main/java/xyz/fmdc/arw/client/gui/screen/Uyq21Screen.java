package xyz.fmdc.arw.client.gui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import xyz.fmdc.arw.api.TrackedTarget;
import xyz.fmdc.arw.client.gui.EmptyMenu;
import xyz.fmdc.arw.common.blockentity.console.Uyq21BlockEntity;
import xyz.fmdc.arw.common.blockentity.fcs.AbstractFcsCoreBlockEntity;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * AN/UYQ-21(V) 戦術ディスプレイコンソールGUI
 * - 背景色: #C5A05A
 * - 中心の表示部分は常に4:3（ボタン表示領域を除いた幅に追従）
 * - 中心のレーダーは1:1（高さ方向を1とする正方形）
 * - レーダー左右のボーダーに各正方形ボタンの機能名を表示
 * - 文字情報はボタン機能名以外すべて非表示
 * - レーダー表示は北が上（North-Up）
 */
public class Uyq21Screen extends AbstractContainerScreen<EmptyMenu> {

    // ボタン総数（各サイド10個）
    public static final int BUTTON_COUNT_PER_SIDE = 10;

    // 背景色: #C5A05A
    public static final int BACKGROUND_COLOR = 0xFFC5A05A;

    // 左右正方形ボタンの配列
    private final Button[] leftButtons = new Button[BUTTON_COUNT_PER_SIDE];
    private final Button[] rightButtons = new Button[BUTTON_COUNT_PER_SIDE];

    // ボタンの機能名テキスト（デフォルト: "TEMP"）
    private final String[] leftFunctionLabels = new String[BUTTON_COUNT_PER_SIDE];
    private final String[] rightFunctionLabels = new String[BUTTON_COUNT_PER_SIDE];

    // ボタンのコールバック
    @SuppressWarnings("unchecked")
    private final Consumer<Integer>[] leftActions = new Consumer[BUTTON_COUNT_PER_SIDE];
    @SuppressWarnings("unchecked")
    private final Consumer<Integer>[] rightActions = new Consumer[BUTTON_COUNT_PER_SIDE];

    // 選択中のボタン状態
    public enum SelectedSide { NONE, LEFT, RIGHT }
    private SelectedSide selectedSide = SelectedSide.NONE;
    private int selectedIndex = -1;

    // レスポンシブ座標・寸法（4:3表示部分、1:1レーダー、左右ボーダー、正方形ボタン）
    private int displayX;
    private int displayY;
    private int displayW;
    private int displayH;

    private int radarX;
    private int radarY;
    private int radarSize; // 1:1（高さ方向を1とする = displayH）

    private int btnSize;
    private int btnSpacing;
    private int buttonsStartY;
    private int leftBtnX;
    private int rightBtnX;

    // レーダー表示レンジ (デフォルト: 512ブロック)
    private float radarRange = 512.0f;

    public Uyq21Screen(EmptyMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);

        // すべてのボタンの機能表示を仮として "TEMP" に初期化
        Arrays.fill(leftFunctionLabels, "TEMP");
        Arrays.fill(rightFunctionLabels, "TEMP");
    }

    /**
     * 左側ボタンの機能表示テキストとアクションを設定
     */
    public void setLeftButton(int index, String label, Consumer<Integer> action) {
        if (index >= 0 && index < BUTTON_COUNT_PER_SIDE) {
            leftFunctionLabels[index] = label;
            leftActions[index] = action;
        }
    }

    /**
     * 右側ボタンの機能表示テキストとアクションを設定
     */
    public void setRightButton(int index, String label, Consumer<Integer> action) {
        if (index >= 0 && index < BUTTON_COUNT_PER_SIDE) {
            rightFunctionLabels[index] = label;
            rightActions[index] = action;
        }
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();

        // 画面全体の寸法に合わせて全画面コンテナ化
        this.imageWidth = this.width;
        this.imageHeight = this.height;
        this.leftPos = 0;
        this.topPos = 0;

        // レスポンシブ寸法の計算（4:3表示部、1:1レーダー、正方形ボタン）
        computeLayout();

        // 左右各10個の正方形ボタンを縦1列に配置（ボタン内のテキストはなし）
        for (int i = 0; i < BUTTON_COUNT_PER_SIDE; i++) {
            final int btnIdx = i;
            int btnY = buttonsStartY + i * (btnSize + btnSpacing);

            // 左側正方形ボタン
            leftButtons[i] = this.addRenderableWidget(Button.builder(
                    Component.empty(),
                    btn -> onButtonClicked(SelectedSide.LEFT, btnIdx)
            ).bounds(leftBtnX, btnY, btnSize, btnSize).build());

            // 右側正方形ボタン
            rightButtons[i] = this.addRenderableWidget(Button.builder(
                    Component.empty(),
                    btn -> onButtonClicked(SelectedSide.RIGHT, btnIdx)
            ).bounds(rightBtnX, btnY, btnSize, btnSize).build());
        }
    }

    /**
     * 画面サイズに応じたレスポンシブレイアウトの計算
     * - 中心の表示部分は常に4:3、ボタン表示領域を除いた幅に合わせる
     * - 中心のレーダーは1:1（高さ方向を1とする）
     * - ボタンは正方形
     */
    private void computeLayout() {
        int margin = Math.max(4, (int) (this.width * 0.015f));
        int gap = Math.max(3, (int) (this.width * 0.01f));
        int vertMargin = Math.max(4, (int) (this.height * 0.02f));

        int approxSpacing = Math.max(1, (int) ((this.height - vertMargin * 2) * 0.012f));
        int calcDisplayW = (int) (((this.width - 2 * (margin + gap)) + 1.8f * approxSpacing) * 20.0f / 23.0f);
        int calcDisplayH = (calcDisplayW * 3) / 4;

        // 画面の上下高さを超えないようにクランプ（常に4:3を維持）
        int maxAvailableH = this.height - vertMargin * 2;
        if (calcDisplayH > maxAvailableH) {
            calcDisplayH = maxAvailableH;
            calcDisplayW = (calcDisplayH * 4) / 3;
        }

        this.displayW = Math.max(40, calcDisplayW);
        this.displayH = Math.max(30, (this.displayW * 3) / 4);

        // 表示部分の位置（画面中央にセンタリング）
        this.displayX = (this.width - this.displayW) / 2;
        this.displayY = (this.height - this.displayH) / 2;

        // ボタンのサイズ・配置計算（正方形ボタン）
        this.btnSpacing = Math.max(1, Math.min(4, (this.displayH - BUTTON_COUNT_PER_SIDE * 10) / (BUTTON_COUNT_PER_SIDE - 1)));
        this.btnSize = Math.max(8, (this.displayH - this.btnSpacing * (BUTTON_COUNT_PER_SIDE - 1)) / BUTTON_COUNT_PER_SIDE);

        int totalButtonsH = btnSize * BUTTON_COUNT_PER_SIDE + btnSpacing * (BUTTON_COUNT_PER_SIDE - 1);
        this.buttonsStartY = displayY + (displayH - totalButtonsH) / 2;

        this.leftBtnX = Math.max(2, displayX - gap - btnSize);
        this.rightBtnX = Math.min(this.width - btnSize - 2, displayX + displayW + gap);

        // 中心のレーダー（1:1、高さ方向を1とする -> サイズは displayH x displayH）
        this.radarSize = this.displayH;
        this.radarX = this.displayX + (this.displayW - this.radarSize) / 2;
        this.radarY = this.displayY;
    }

    /**
     * ボタン押下時のハンドラ
     */
    private void onButtonClicked(SelectedSide side, int index) {
        this.selectedSide = side;
        this.selectedIndex = index;

        if (side == SelectedSide.LEFT && leftActions[index] != null) {
            leftActions[index].accept(index);
        } else if (side == SelectedSide.RIGHT && rightActions[index] != null) {
            rightActions[index].accept(index);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 標準の暗転背景描画
        this.renderBackground(guiGraphics);

        // コンテナの背景・全画面UI要素の描画
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // ツールチップの描画
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 文字情報はボタンの機能表示以外すべて非表示
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // 1. 全画面背景 (#C5A05A)
        guiGraphics.fill(0, 0, this.width, this.height, BACKGROUND_COLOR);

        // 2. 中心の表示部分（常に4:3、黒背景）、1:1レーダー、および左右ボーダー内の機能名
        renderCenterDisplay(guiGraphics);
    }

    /**
     * 中心の表示部分の描画
     * - 常に4:3の黒背景スクリーン
     * - 中心のレーダーは1:1（高さ方向を1とする）
     * - レーダー左右のボーダーに各ボタンの機能名を表示
     */
    private void renderCenterDisplay(GuiGraphics guiGraphics) {
        if (displayW <= 0 || displayH <= 0) return;

        // 1. 中心の表示部分全体（4:3、純黒背景 0xFF000000）
        guiGraphics.fill(displayX, displayY, displayX + displayW, displayY + displayH, 0xFF000000);

        // 外枠ベゼル
        guiGraphics.renderOutline(displayX, displayY, displayW, displayH, 0xFF1A1A1A);
        if (displayW > 4 && displayH > 4) {
            guiGraphics.renderOutline(displayX + 1, displayY + 1, displayW - 2, displayH - 2, 0xFF2A2A2A);
        }

        // 2. 左右ボーダーと1:1レーダーの仕切り線
        int borderLineCol = 0xFF223344;
        guiGraphics.vLine(radarX, displayY, displayY + displayH, borderLineCol);
        guiGraphics.vLine(radarX + radarSize, displayY, displayY + displayH, borderLineCol);

        // 3. レーダー領域（1:1、正方形、高さ方向1）の描画
        guiGraphics.enableScissor(radarX, radarY, radarX + radarSize, radarY + radarSize);
        drawTacticalGrid(guiGraphics, radarX, radarY, radarSize, radarSize);

        int radarCenterX = radarX + radarSize / 2;
        int radarCenterY = radarY + radarSize / 2;
        int maxRadius = radarSize / 2 - 4;
        if (maxRadius > 10) {
            drawRadarReticle(guiGraphics, radarCenterX, radarCenterY, maxRadius);
            drawRadarTargets(guiGraphics, radarCenterX, radarCenterY, maxRadius);
        }
        guiGraphics.disableScissor();

        // 4. 中心のレーダー左右のボーダーにボタンの機能名を表示
        int leftBorderW = radarX - displayX;
        int rightBorderW = (displayX + displayW) - (radarX + radarSize);

        // 左側ボーダー内の機能名表示
        if (leftBorderW > 0) {
            guiGraphics.enableScissor(displayX, displayY, radarX, displayY + displayH);
            for (int i = 0; i < BUTTON_COUNT_PER_SIDE; i++) {
                int btnY = buttonsStartY + i * (btnSize + btnSpacing);
                int textY = btnY + (btnSize - this.font.lineHeight) / 2;
                String label = leftFunctionLabels[i] != null ? leftFunctionLabels[i] : "TEMP";
                int textW = this.font.width(label);
                int textX = displayX + (leftBorderW - textW) / 2;
                int color = (selectedSide == SelectedSide.LEFT && selectedIndex == i) ? 0xFFFFAA00 : 0xFF00FF88;
                guiGraphics.drawString(this.font, label, textX, textY, color, false);
            }
            guiGraphics.disableScissor();
        }

        // 右側ボーダー内の機能名表示
        if (rightBorderW > 0) {
            guiGraphics.enableScissor(radarX + radarSize, displayY, displayX + displayW, displayY + displayH);
            for (int i = 0; i < BUTTON_COUNT_PER_SIDE; i++) {
                int btnY = buttonsStartY + i * (btnSize + btnSpacing);
                int textY = btnY + (btnSize - this.font.lineHeight) / 2;
                String label = rightFunctionLabels[i] != null ? rightFunctionLabels[i] : "TEMP";
                int textW = this.font.width(label);
                int textX = (radarX + radarSize) + (rightBorderW - textW) / 2;
                int color = (selectedSide == SelectedSide.RIGHT && selectedIndex == i) ? 0xFFFFAA00 : 0xFF00FF88;
                guiGraphics.drawString(this.font, label, textX, textY, color, false);
            }
            guiGraphics.disableScissor();
        }
    }

    /**
     * 薄い戦術グリッド線の描画（図形のみ）
     */
    private void drawTacticalGrid(GuiGraphics guiGraphics, int x, int y, int w, int h) {
        int gridStep = 32;
        int gridColor = 0x0A00FF88;

        for (int gx = x + (w % gridStep) / 2; gx < x + w; gx += gridStep) {
            guiGraphics.vLine(gx, y, y + h, gridColor);
        }
        for (int gy = y + (h % gridStep) / 2; gy < y + h; gy += gridStep) {
            guiGraphics.hLine(x, x + w, gy, gridColor);
        }
    }

    /**
     * レーダーHUDレティクル（十字線・同心円・方位記号）の描画
     */
    private void drawRadarReticle(GuiGraphics guiGraphics, int cx, int cy, int maxRadius) {
        int reticleColor = 0x2400FF88;
        int axisColor = 0x3300FF88;

        // 十字軸線
        guiGraphics.hLine(cx - maxRadius, cx + maxRadius, cy, axisColor);
        guiGraphics.vLine(cx, cy - maxRadius, cy + maxRadius, axisColor);

        // 同心円（内側・外側）
        drawCircle(guiGraphics, cx, cy, maxRadius, reticleColor);
        if (maxRadius > 40) {
            drawCircle(guiGraphics, cx, cy, (int) (maxRadius * 0.66f), reticleColor);
            drawCircle(guiGraphics, cx, cy, (int) (maxRadius * 0.33f), reticleColor);
        }

        // 方位記号（北が画面真上: North-Up）
        int headingColor = 0x8800FF88;
        guiGraphics.drawString(this.font, "N", cx - this.font.width("N") / 2, cy - maxRadius + 2, headingColor, false);
        guiGraphics.drawString(this.font, "S", cx - this.font.width("S") / 2, cy + maxRadius - this.font.lineHeight - 1, headingColor, false);
        guiGraphics.drawString(this.font, "E", cx + maxRadius - this.font.width("E") - 2, cy - this.font.lineHeight / 2, headingColor, false);
        guiGraphics.drawString(this.font, "W", cx - maxRadius + 3, cy - this.font.lineHeight / 2, headingColor, false);
    }

    /**
     * レーダー画面上にターゲットのドットと速度ベクトルラインを描画（北が上）
     */
    private void drawRadarTargets(GuiGraphics guiGraphics, int cx, int cy, int maxRadius) {
        if (this.menu.getBlockEntity() instanceof Uyq21BlockEntity uyqBE) {
            Map<UUID, TrackedTarget> targets = uyqBE.getTrackedTargets();
            if (targets.isEmpty()) return;

            BlockPos centerPos = uyqBE.getBlockPos();
            double originX = centerPos.getX() + 0.5;
            double originZ = centerPos.getZ() + 0.5;

            // 表示レンジ（FCSコアが接続されている場合はそのアクティブ最大探知距離を優先、なければデフォルト512m）
            float currentRange = this.radarRange;
            AbstractFcsCoreBlockEntity core = uyqBE.getLinkedFcsCore();
            if (core != null) {
                float coreMaxRange = core.getMaxActiveDetectionRange();
                if (coreMaxRange > 0.0f) {
                    currentRange = coreMaxRange;
                }
            }

            for (TrackedTarget target : targets.values()) {
                Vec3 pos = target.getLastKnownPos();
                if (pos == null) continue;

                // ワールド座標差分（北が上: -Zが上、+Xが右）
                double dX = pos.x - originX;
                double dZ = pos.z - originZ;

                // スクリーンピクセル座標への投影
                double screenRelX = (dX / currentRange) * maxRadius;
                double screenRelY = (dZ / currentRange) * maxRadius;

                // レーダー円外判定
                double distSqr = screenRelX * screenRelX + screenRelY * screenRelY;
                if (distSqr > (double) maxRadius * maxRadius) {
                    continue;
                }

                int px = (int) Math.round(cx + screenRelX);
                int py = (int) Math.round(cy + screenRelY);

                // 1. ベクトルライン（速度ベクトル）の描画
                Vec3 vel = target.getLastKnownVelocity();
                if (vel != null) {
                    // 2秒間（40 ticks）の予想移動ベクトル
                    double vxWorld = vel.x * 40.0;
                    double vzWorld = vel.z * 40.0;
                    double vxPix = (vxWorld / currentRange) * maxRadius;
                    double vzPix = (vzWorld / currentRange) * maxRadius;

                    if (vxPix * vxPix + vzPix * vzPix >= 1.0) {
                        int endX = (int) Math.round(px + vxPix);
                        int endY = (int) Math.round(py + vzPix);
                        drawLine(guiGraphics, px, py, endX, endY, 0xCC00FF88);
                    }
                }

                // 2. ターゲットのドット描画（3x3正方形、中心白）
                guiGraphics.fill(px - 1, py - 1, px + 2, py + 2, 0xFF00FF88);
                guiGraphics.fill(px, py, px + 1, py + 1, 0xFFFFFFFF);
            }

            // レンジ表示（画面左上に小さく表示）
            String rangeText = String.format("RNG: %.0fm", currentRange);
            guiGraphics.drawString(this.font, rangeText, radarX + 4, radarY + 4, 0x8800FF88, false);
        }
    }

    /**
     * 円の簡易描画（折れ線近似）
     */
    private void drawCircle(GuiGraphics guiGraphics, int cx, int cy, int radius, int color) {
        if (radius <= 0) return;
        int steps = 36;
        int prevX = cx + radius;
        int prevY = cy;

        for (int i = 1; i <= steps; i++) {
            double angle = i * (2.0 * Math.PI / steps);
            int curX = cx + (int) Math.round(Math.cos(angle) * radius);
            int curY = cy + (int) Math.round(Math.sin(angle) * radius);
            drawLine(guiGraphics, prevX, prevY, curX, curY, color);
            prevX = curX;
            prevY = curY;
        }
    }

    /**
     * Bresenhamのアルゴリズムによる直線描画
     */
    private void drawLine(GuiGraphics guiGraphics, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        int x = x0;
        int y = y0;
        while (true) {
            guiGraphics.fill(x, y, x + 1, y + 1, color);
            if (x == x1 && y == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
