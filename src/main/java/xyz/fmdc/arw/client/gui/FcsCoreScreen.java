package xyz.fmdc.arw.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import xyz.fmdc.arw.common.blockentity.fcs.AbstractFcsCoreBlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FcsCoreScreen extends Screen {

    private final BlockPos corePos;
    private UUID coreUuid = null;

    // UI配置定数
    private static final int MARGIN = 12;
    private static final int HEADER_HEIGHT = 36;
    private static final int FOOTER_HEIGHT = 28;
    private static final int GAP = 10;

    // デバイス情報レコード（名前のみ保持）
    public record DeviceEntry(String name) {}

    private final List<DeviceEntry> connectedDevices = new ArrayList<>();
    private DeviceEntry selectedDevice = null;
    private int hoveredIndex = -1;
    private Button deleteButton;

    public FcsCoreScreen(BlockPos corePos) {
        super(Component.translatable("gui.arw.fcs_core.title"));
        this.corePos = corePos;

        // FCS Core の UUID 取得
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            BlockEntity be = mc.level.getBlockEntity(corePos);
            if (be instanceof AbstractFcsCoreBlockEntity fcsCore) {
                this.coreUuid = fcsCore.getUuid();
            }
        }

        // サンプルの機器データを1つ追加
        initSampleDevice();
    }

    private void initSampleDevice() {
        connectedDevices.clear();
        connectedDevices.add(new DeviceEntry("OTO 127mm Naval Gun"));

        if (!connectedDevices.isEmpty()) {
            selectedDevice = connectedDevices.get(0);
        }
    }

    @Override
    protected void init() {
        super.init();

        int contentY = HEADER_HEIGHT + MARGIN;
        int contentHeight = this.height - contentY - FOOTER_HEIGHT - MARGIN;

        int totalContentWidth = this.width - MARGIN * 2;
        int listWidth = (int) (totalContentWidth * 0.55f);
        int listX = MARGIN;

        int detailX = listX + listWidth + GAP;
        int detailWidth = this.width - MARGIN - detailX;

        // 詳細情報タブ内の下側に削除ボタンを配置
        int deleteBtnWidth = Math.min(100, detailWidth - 20);
        int deleteBtnHeight = 20;
        int deleteBtnX = detailX + 10;
        int deleteBtnY = contentY + contentHeight - deleteBtnHeight - 8;

        this.deleteButton = this.addRenderableWidget(Button.builder(Component.literal("DELETE"), button -> {
            if (selectedDevice != null) {
                connectedDevices.remove(selectedDevice);
                selectedDevice = connectedDevices.isEmpty() ? null : connectedDevices.get(0);
            }
        }).bounds(deleteBtnX, deleteBtnY, deleteBtnWidth, deleteBtnHeight).build());
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 削除ボタンの活性・非活性状態を更新
        if (this.deleteButton != null) {
            this.deleteButton.active = (this.selectedDevice != null);
        }

        // 背景の半透明オーバーレイ
        this.renderBackground(guiGraphics);

        // 1. 全画面背景
        guiGraphics.fill(0, 0, this.width, this.height, 0xF00A0F18);

        // 2. ヘッダーエリア
        guiGraphics.fill(0, 0, this.width, HEADER_HEIGHT, 0xFF121B28);
        guiGraphics.hLine(0, this.width, HEADER_HEIGHT, 0xFF2B4C7E);

        // タイトル & サブタイトル
        guiGraphics.drawString(this.font, "◆ FCS CORE - NETWORK CONTROL MANAGER ◆", MARGIN, 8, 0xFF4DEEEA, false);
        String subTitle = "Core Pos: [" + corePos.toShortString() + "]" +
                (coreUuid != null ? "  |  UUID: " + coreUuid : "");
        guiGraphics.drawString(this.font, subTitle, MARGIN, 22, 0xFF88AACC, false);

        // 3. メインコンテンツ領域（左: 接続機器リスト / 右: 機器詳細）
        int contentY = HEADER_HEIGHT + MARGIN;
        int contentHeight = this.height - contentY - FOOTER_HEIGHT - MARGIN;

        int totalContentWidth = this.width - MARGIN * 2;
        int listWidth = (int) (totalContentWidth * 0.55f);
        int listX = MARGIN;

        int detailX = listX + listWidth + GAP;
        int detailWidth = this.width - MARGIN - detailX;

        // 左側：接続機器リスト描画（機器の名前のみ表示）
        drawDeviceList(guiGraphics, listX, contentY, listWidth, contentHeight, mouseX, mouseY);

        // 右側：機器詳細情報描画（機器の名前のみ表示）
        drawDeviceDetails(guiGraphics, detailX, contentY, detailWidth, contentHeight);

        // 4. フッターエリア
        int footerY = this.height - FOOTER_HEIGHT;
        guiGraphics.fill(0, footerY, this.width, this.height, 0xFF121B28);
        guiGraphics.hLine(0, this.width, footerY, 0xFF2B4C7E);

        String deviceCountText = "CONNECTED NODES: " + connectedDevices.size();
        guiGraphics.drawString(this.font, deviceCountText, MARGIN, footerY + 10, 0xFF00FF88, false);

        String escHintText = "[ESC] TO EXIT";
        int escTextWidth = this.font.width(escHintText);
        guiGraphics.drawString(this.font, escHintText, this.width - MARGIN - escTextWidth, footerY + 10, 0xFF88AACC, false);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void drawDeviceList(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY) {
        // リスト外枠・背景
        guiGraphics.fill(x, y, x + width, y + height, 0xFF060B12);
        guiGraphics.renderOutline(x, y, width, height, 0xFF1F3554);

        // ヘッダーテキスト
        guiGraphics.drawString(this.font, "CONNECTED DEVICES LIST", x + 8, y + 8, 0xFFE0E0E0, false);
        guiGraphics.hLine(x + 4, x + width - 4, y + 22, 0xFF1F3554);

        hoveredIndex = -1;

        if (connectedDevices.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, "NO CONNECTED DEVICES", x + width / 2, y + height / 2 - 4, 0xFF556677);
            return;
        }

        int itemStartY = y + 26;
        int itemHeight = 24;
        int itemSpacing = 3;

        for (int i = 0; i < connectedDevices.size(); i++) {
            int itemY = itemStartY + i * (itemHeight + itemSpacing);
            if (itemY + itemHeight > y + height - 4) break;

            DeviceEntry dev = connectedDevices.get(i);
            boolean isSelected = (dev == selectedDevice);
            boolean isHovered = mouseX >= x + 4 && mouseX <= x + width - 4 && mouseY >= itemY && mouseY <= itemY + itemHeight;

            if (isHovered) hoveredIndex = i;

            // アイテム背景
            int bgColor = isSelected ? 0xFF1C3454 : (isHovered ? 0xFF14243B : 0xFF0E1722);
            int borderColor = isSelected ? 0xFF4DEEEA : (isHovered ? 0xFF355B8C : 0xFF16253A);
            guiGraphics.fill(x + 4, itemY, x + width - 4, itemY + itemHeight, bgColor);
            guiGraphics.renderOutline(x + 4, itemY, width - 8, itemHeight, borderColor);

            // 表示項目：機器の名前のみ表示
            int textColor = isSelected ? 0xFFFFFFFF : 0xFFCCDDEE;
            guiGraphics.drawString(this.font, dev.name(), x + 8, itemY + 8, textColor, false);
        }
    }

    private void drawDeviceDetails(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        // 詳細パネル外枠・背景
        guiGraphics.fill(x, y, x + width, y + height, 0xFF060B12);
        guiGraphics.renderOutline(x, y, width, height, 0xFF1F3554);

        // ヘッダーテキスト
        guiGraphics.drawString(this.font, "NODE DETAILS", x + 8, y + 8, 0xFFE0E0E0, false);
        guiGraphics.hLine(x + 4, x + width - 4, y + 22, 0xFF1F3554);

        if (selectedDevice == null) {
            guiGraphics.drawCenteredString(this.font, "Select a device to view details", x + width / 2, y + height / 2 - 4, 0xFF556677);
            return;
        }

        int textY = y + 28;

        // 表示項目：名前のみ表示
        guiGraphics.drawString(this.font, "NAME:", x + 10, textY, 0xFF6C8EA4, false);
        guiGraphics.drawString(this.font, selectedDevice.name(), x + 10, textY + 12, 0xFFFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && hoveredIndex >= 0 && hoveredIndex < connectedDevices.size()) {
            selectedDevice = connectedDevices.get(hoveredIndex);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
