package xyz.fmdc.arw.radardisplay.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

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
            this.addRenderableWidget(Button.builder(Component.literal(modeName), button -> {
                this.selectedTopMode = modeName;
            }).bounds(bLeft, MARGIN, topBtnWidth, TOP_BTN_HEIGHT).build());
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
            this.addRenderableWidget(Button.builder(Component.literal(String.valueOf(range)), button -> {
                this.selectedRange = range;
            }).bounds(rightBtnLeft, bTop, rightBtnWidth, rightBtnHeight).build());
        }

        // 1つの空ボタン
        int emptyBtnTop = mainY + RANGES.length * (rightBtnHeight + rightBtnSpacing);
        this.addRenderableWidget(Button.builder(Component.literal(""), button -> {
            // 空ボタンのアクション（将来の拡張用）
        }).bounds(rightBtnLeft, emptyBtnTop, rightBtnWidth, rightBtnHeight).build());
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

        // 左側：レーダー画面エリア（全画面に対する3:1比率、暫定的に空白領域を描画）
        guiGraphics.fill(radarLeft, mainY, radarLeft + radarWidth, mainY + mainHeight, 0xFF03070E);
        guiGraphics.renderOutline(radarLeft, mainY, radarWidth, mainHeight, 0xFF1E3A5F);

        // レーダーエリア内のステータス表示
        guiGraphics.drawString(this.font, "RANGE: " + this.selectedRange + "m", radarLeft + 8, mainY + 8, 0x55AAFF);
        guiGraphics.drawString(this.font, "MODE: " + this.selectedTopMode, radarLeft + 8, mainY + 22, 0x88CC88);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
