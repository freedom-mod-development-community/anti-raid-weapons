package xyz.fmdc.arw.radardisplay.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import xyz.fmdc.arw.network.PacketHandler;
import xyz.fmdc.arw.network.UpdateRadarDisplayConfigPacket;
import xyz.fmdc.arw.radardisplay.RadarDisplayBlockEntity;

import java.util.Objects;
import java.util.function.BooleanSupplier;

public class RadarDisplayScreen extends Screen {
    private final BlockPos pos;

    private static final int MARGIN = 10;
    private static final int GAP = 10;
    private static final int TOP_BTN_HEIGHT = 20;
    private static final int TOP_BAR_GAP = 8;

    private static final int[] RANGES = {50000, 10000, 5000, 1000};
    private int selectedRange = 50000;

    // 上部ボタンのラベル一覧
    private static final String[] TOP_MODES = {"Radar Mode", "TgtEnemySet", "TgtCoordSet", "SysModeSet"};
    private String selectedTopMode = "Radar Mode";

    public RadarDisplayScreen(BlockPos pos) {
        super(Component.translatable("gui.arw.radar_display.title"));
        this.pos = pos;

        // BlockEntity から保存されている設定値をロード
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            BlockEntity be = mc.level.getBlockEntity(pos);
            if (be instanceof RadarDisplayBlockEntity radarBE) {
                this.selectedRange = radarBE.getSelectedRange();
                this.selectedTopMode = radarBE.getSelectedTopMode();
            }
        }
    }

    private void updateConfig(int newRange, String newTopMode) {
        this.selectedRange = newRange;
        this.selectedTopMode = newTopMode;

        // クライアント側 BlockEntity を即時更新
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            BlockEntity be = mc.level.getBlockEntity(pos);
            if (be instanceof RadarDisplayBlockEntity radarBE) {
                radarBE.setConfig(this.selectedRange, this.selectedTopMode);
            }
        }

        // サーバーへ同期パケットを送信して保存
        PacketHandler.sendToServer(new UpdateRadarDisplayConfigPacket(this.pos, this.selectedRange, this.selectedTopMode));
    }

    @Override
    protected void init() {
        super.init();

        int totalWidth = this.width - MARGIN * 2;
        int topBtnCount = TOP_MODES.length;
        int topBtnSpacing = 6;
        int topBtnWidth = (totalWidth - (topBtnCount - 1) * topBtnSpacing) / topBtnCount;

        // 1. 上部ボタン 4つを画面上部に横1列配置
        for (int i = 0; i < topBtnCount; i++) {
            final String modeName = TOP_MODES[i];
            int bLeft = MARGIN + i * (topBtnWidth + topBtnSpacing);
            this.addRenderableWidget(new FlatGrayButton(
                    bLeft, MARGIN, topBtnWidth, TOP_BTN_HEIGHT,
                    Component.literal(modeName),
                    button -> updateConfig(this.selectedRange, modeName),
                    () -> Objects.equals(this.selectedTopMode, modeName)
            ));
        }

        // 2. メイン領域の寸法計算
        int mainY = MARGIN + TOP_BTN_HEIGHT + TOP_BAR_GAP;
        int mainHeight = this.height - mainY - MARGIN;

        int contentWidth = totalWidth - GAP;
        int radarWidth = (contentWidth * 3) / 4; // 3:1比率
        int rightBtnWidth = contentWidth - radarWidth;
        int rightBtnLeft = MARGIN + radarWidth + GAP;

        // 右側ボタン 5つ（レンジボタン4つ + 空ボタン1つ）
        int rightBtnCount = 5;
        int rightBtnHeight = 20;
        int rightBtnSpacing = rightBtnCount > 1
                ? Math.max(4, (mainHeight - rightBtnCount * rightBtnHeight) / (rightBtnCount - 1))
                : 4;

        // 4つのレンジボタン [50000, 10000, 5000, 1000]
        for (int i = 0; i < RANGES.length; i++) {
            final int range = RANGES[i];
            int bTop = mainY + i * (rightBtnHeight + rightBtnSpacing);
            this.addRenderableWidget(new FlatGrayButton(
                    rightBtnLeft, bTop, rightBtnWidth, rightBtnHeight,
                    Component.literal(String.valueOf(range)),
                    button -> updateConfig(range, this.selectedTopMode),
                    () -> this.selectedRange == range
            ));
        }

        // 1つの空ボタン
        int emptyBtnTop = mainY + RANGES.length * (rightBtnHeight + rightBtnSpacing);
        this.addRenderableWidget(new FlatGrayButton(
                rightBtnLeft, emptyBtnTop, rightBtnWidth, rightBtnHeight,
                Component.literal(""),
                button -> {
                    // 空ボタンのアクション（将来の拡張用）
                },
                () -> false
        ));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        // 全画面背景
        guiGraphics.fill(0, 0, this.width, this.height, 0xEE080C14);

        int totalWidth = this.width - MARGIN * 2;
        int mainY = MARGIN + TOP_BTN_HEIGHT + TOP_BAR_GAP;
        int mainHeight = this.height - mainY - MARGIN;

        int contentWidth = totalWidth - GAP;
        int radarWidth = (contentWidth * 3) / 4;
        int radarLeft = MARGIN;

        // 左側：レーダー画面エリア（全画面に対する3:1比率、空白領域を描画）
        guiGraphics.fill(radarLeft, mainY, radarLeft + radarWidth, mainY + mainHeight, 0xFF03070E);
        guiGraphics.renderOutline(radarLeft, mainY, radarWidth, mainHeight, 0xFF1E3A5F);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * 単色の灰色背景と状態に応じたテキストカラーを持つカスタムボタン
     */
    private static class FlatGrayButton extends Button {
        private final BooleanSupplier isSelectedSupplier;

        public FlatGrayButton(int x, int y, int width, int height, Component message, OnPress onPress, BooleanSupplier isSelectedSupplier) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
            this.isSelectedSupplier = isSelectedSupplier;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            Minecraft mc = Minecraft.getInstance();
            boolean selected = isSelectedSupplier != null && isSelectedSupplier.getAsBoolean();
            boolean hovered = this.isHoveredOrFocused();

            // 単色灰色背景と枠線色の決定
            int bgColor;
            int borderColor;
            if (selected) {
                bgColor = hovered ? 0xFF3D434A : 0xFF32363C;
                borderColor = 0xFF55FF55;
            } else if (hovered) {
                bgColor = 0xFF3E4348;
                borderColor = 0xFF656B73;
            } else {
                bgColor = 0xFF2A2D32;
                borderColor = 0xFF42464D;
            }

            // 単色灰色の矩形描画
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bgColor);
            guiGraphics.renderOutline(this.getX(), this.getY(), this.width, this.height, borderColor);

            // テキスト色：選択中は緑(0x55FF55)、非選択時は白/薄灰色
            int textColor = selected ? 0x55FF55 : (hovered ? 0xFFFFFF : 0xCCCCCC);

            int textX = this.getX() + this.width / 2;
            int textY = this.getY() + (this.height - 8) / 2;
            guiGraphics.drawCenteredString(mc.font, this.getMessage(), textX, textY, textColor);
        }
    }
}
