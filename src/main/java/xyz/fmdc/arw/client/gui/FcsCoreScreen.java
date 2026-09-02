package xyz.fmdc.arw.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import xyz.fmdc.arw.api.fcs.IFcsControllableWeapon;
import xyz.fmdc.arw.api.fcs.IFcsNetworkNode;
import xyz.fmdc.arw.api.fcs.IFcsSensorNode;
import xyz.fmdc.arw.common.blockentity.AbstractARWBlockEntity;
import xyz.fmdc.arw.common.blockentity.AbstractSingleGunBlockEntity;
import xyz.fmdc.arw.common.blockentity.console.TestConsoleBlockEntity;
import xyz.fmdc.arw.common.blockentity.fcs.AbstractFcsCoreBlockEntity;
import xyz.fmdc.arw.common.blockentity.sensor.HorizontalRadarBlockEntity;
import xyz.fmdc.arw.common.blockentity.sensor.Spq9bBlockEntity;
import xyz.fmdc.arw.common.blockentity.vls.VlsBlockEntity;
import xyz.fmdc.arw.common.blockentity.weapon.ARWCIWSBlockEntity;
import xyz.fmdc.arw.common.blockentity.weapon.singlegun.Mk45Mod4BlockEntity;
import xyz.fmdc.arw.common.blockentity.weapon.singlegun.Oto127mmBlockEntity;
import xyz.fmdc.arw.common.blockentity.weapon.PhalanxBlockEntity;
import xyz.fmdc.arw.common.item.FiveInchShellItem;
import xyz.fmdc.arw.network.PacketHandler;
import xyz.fmdc.arw.network.ServerboundFcsCoreUnregisterPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FcsCoreScreen extends Screen {

    private final BlockPos corePos;
    private UUID coreUuid = null;

    // UI配置定数
    private static final int MARGIN = 12;
    private static final int HEADER_HEIGHT = 36;
    private static final int FOOTER_HEIGHT = 28;
    private static final int GAP = 10;
    private static final int ITEM_HEIGHT = 24;
    private static final int ITEM_SPACING = 3;

    // デバイス情報レコード（UUID、名前、座標を保持）
    public record DeviceEntry(UUID uuid, String name, BlockPos pos) {}
    public record AmmoInfo(int totalCount, String nextAmmoName) {}

    private final List<DeviceEntry> connectedDevices = new ArrayList<>();
    private DeviceEntry selectedDevice = null;
    private int hoveredIndex = -1;
    private Button deleteButton;

    // スクロール制御
    private int scrollOffset = 0;
    private boolean isDraggingScrollBar = false;

    public FcsCoreScreen(BlockPos corePos) {
        super(Component.translatable("gui.arw.fcs_core.title"));
        this.corePos = corePos;

        // FCS Core の接続機器の初期化
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            BlockEntity be = mc.level.getBlockEntity(corePos);
            if (be instanceof AbstractFcsCoreBlockEntity fcsCore) {
                this.coreUuid = fcsCore.getUuid();
                initConnectedDevices(mc.level, fcsCore);
            }
        }
    }

    private void initConnectedDevices(Level level, AbstractFcsCoreBlockEntity fcsCore) {
        connectedDevices.clear();
        Map<UUID, BlockPos> nodePositions = fcsCore.getNodePositions();
        for (UUID uuid : fcsCore.getConnectedNodeUuids()) {
            BlockPos pos = nodePositions.get(uuid);
            String name = (pos != null && level != null && level.isLoaded(pos))
                    ? level.getBlockState(pos).getBlock().getName().getString()
                    : "Unknown Device";
            connectedDevices.add(new DeviceEntry(uuid, name, pos));
        }

        if (!connectedDevices.isEmpty()) {
            selectedDevice = connectedDevices.get(0);
        } else {
            selectedDevice = null;
        }
    }

    @Override
    protected void init() {
        super.init();

        int contentY = HEADER_HEIGHT + MARGIN;
        int contentHeight = this.height - contentY - FOOTER_HEIGHT - MARGIN;

        int totalContentWidth = this.width - MARGIN * 2;
        int listWidth = (int) (totalContentWidth * 0.45f);
        int listX = MARGIN;

        int detailX = listX + listWidth + GAP;
        int detailWidth = this.width - MARGIN - detailX;

        // 詳細情報タブ内の下側に削除ボタンを配置
        int deleteBtnWidth = Math.min(130, detailWidth - 20);
        int deleteBtnHeight = 20;
        int deleteBtnX = detailX + 10;
        int deleteBtnY = contentY + contentHeight - deleteBtnHeight - 8;

        this.deleteButton = this.addRenderableWidget(Button.builder(Component.literal("UNREGISTER / DELETE"), button -> {
            if (selectedDevice != null) {
                // サーバーへ登録解除パケットを送信
                PacketHandler.sendToServer(new ServerboundFcsCoreUnregisterPacket(corePos, selectedDevice.uuid()));
                connectedDevices.remove(selectedDevice);
                selectedDevice = connectedDevices.isEmpty() ? null : connectedDevices.get(0);
                clampScroll();
            }
        }).bounds(deleteBtnX, deleteBtnY, deleteBtnWidth, deleteBtnHeight).build());
    }

    private void clampScroll() {
        int contentY = HEADER_HEIGHT + MARGIN;
        int contentHeight = this.height - contentY - FOOTER_HEIGHT - MARGIN;
        int visibleHeight = contentHeight - 30;
        int totalHeight = connectedDevices.size() * (ITEM_HEIGHT + ITEM_SPACING);
        int maxScroll = Math.max(0, totalHeight - visibleHeight);
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);
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

        // タイトル & サブタイトル（FCSコアの位置およびUUIDを表示）
        guiGraphics.drawString(this.font, "◆ FCS CORE - NETWORK CONTROL MANAGER ◆", MARGIN, 8, 0xFF4DEEEA, false);
        String subTitle = "Core Pos: [" + corePos.toShortString() + "]  |  Core UUID: " + (coreUuid != null ? coreUuid.toString() : "Unknown");
        guiGraphics.drawString(this.font, subTitle, MARGIN, 22, 0xFF88AACC, false);

        // 3. メインコンテンツ領域（左: 接続機器リスト / 右: 機器詳細）
        int contentY = HEADER_HEIGHT + MARGIN;
        int contentHeight = this.height - contentY - FOOTER_HEIGHT - MARGIN;

        int totalContentWidth = this.width - MARGIN * 2;
        int listWidth = (int) (totalContentWidth * 0.45f);
        int listX = MARGIN;

        int detailX = listX + listWidth + GAP;
        int detailWidth = this.width - MARGIN - detailX;

        // 左側：接続機器リスト描画（機器の名前のみ表示、スクロール対応）
        drawDeviceList(guiGraphics, listX, contentY, listWidth, contentHeight, mouseX, mouseY);

        // 右側：機器詳細情報描画（名前と種類に合わせた詳細情報を表示、UUID非表示）
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
        guiGraphics.drawString(this.font, "CONNECTED DEVICES", x + 8, y + 8, 0xFFE0E0E0, false);
        guiGraphics.hLine(x + 4, x + width - 4, y + 22, 0xFF1F3554);

        hoveredIndex = -1;

        if (connectedDevices.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, "NO CONNECTED DEVICES", x + width / 2, y + height / 2 - 4, 0xFF556677);
            return;
        }

        int listTop = y + 24;
        int listBottom = y + height - 4;
        int visibleHeight = listBottom - listTop;
        int totalHeight = connectedDevices.size() * (ITEM_HEIGHT + ITEM_SPACING);
        int maxScroll = Math.max(0, totalHeight - visibleHeight);

        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);

        int scrollbarWidth = (maxScroll > 0) ? 6 : 0;
        int itemWidth = width - 8 - scrollbarWidth;

        // シザー（描画領域の切り抜き）を有効化してはみ出しを防止
        guiGraphics.enableScissor(x + 4, listTop, x + width - 4, listBottom);

        for (int i = 0; i < connectedDevices.size(); i++) {
            int itemY = listTop + i * (ITEM_HEIGHT + ITEM_SPACING) - scrollOffset;

            // 表示範囲外ならスキップ
            if (itemY + ITEM_HEIGHT < listTop || itemY > listBottom) continue;

            DeviceEntry dev = connectedDevices.get(i);
            boolean isSelected = (dev == selectedDevice);
            boolean isHovered = mouseX >= x + 4 && mouseX <= x + 4 + itemWidth &&
                    mouseY >= Math.max(listTop, itemY) && mouseY <= Math.min(listBottom, itemY + ITEM_HEIGHT);

            if (isHovered) hoveredIndex = i;

            // アイテム背景
            int bgColor = isSelected ? 0xFF1C3454 : (isHovered ? 0xFF14243B : 0xFF0E1722);
            int borderColor = isSelected ? 0xFF4DEEEA : (isHovered ? 0xFF355B8C : 0xFF16253A);
            guiGraphics.fill(x + 4, itemY, x + 4 + itemWidth, itemY + ITEM_HEIGHT, bgColor);
            guiGraphics.renderOutline(x + 4, itemY, itemWidth, ITEM_HEIGHT, borderColor);

            // 表示項目：機器の名前のみを表示
            int textColor = isSelected ? 0xFFFFFFFF : 0xFFCCDDEE;
            guiGraphics.drawString(this.font, dev.name(), x + 8, itemY + 8, textColor, false);
        }

        guiGraphics.disableScissor();

        // スクロールバー描画
        if (maxScroll > 0) {
            int scrollTrackX = x + width - 8;
            guiGraphics.fill(scrollTrackX, listTop, scrollTrackX + 4, listBottom, 0xFF0A1422);

            int thumbHeight = Math.max(16, (int) ((float) visibleHeight / totalHeight * visibleHeight));
            int thumbY = listTop + (int) ((float) scrollOffset / maxScroll * (visibleHeight - thumbHeight));
            guiGraphics.fill(scrollTrackX, thumbY, scrollTrackX + 4, thumbY + thumbHeight, 0xFF355B8C);
        }
    }

    private AmmoInfo getAmmoInfo(BlockEntity be) {
        IItemHandler handler = null;
        if (be instanceof Oto127mmBlockEntity oto) {
            handler = oto.getInventory();
        } else if (be != null) {
            handler = be.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
        }

        if (handler != null) {
            int count = 0;
            String nextAmmo = null;
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    count += stack.getCount();
                    if (nextAmmo == null) {
                        if (stack.getItem() instanceof FiveInchShellItem shellItem) {
                            nextAmmo = shellItem.getAmmoType().getName();
                        } else {
                            nextAmmo = stack.getHoverName().getString();
                        }
                    }
                }
            }
            if (count > 0 && nextAmmo != null) {
                return new AmmoInfo(count, nextAmmo);
            }
        }
        return new AmmoInfo(0, "NONE");
    }

    private void drawDeviceDetails(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        // 詳細パネル外枠・背景
        guiGraphics.fill(x, y, x + width, y + height, 0xFF060B12);
        guiGraphics.renderOutline(x, y, width, height, 0xFF1F3554);

        // ヘッダーテキスト
        guiGraphics.drawString(this.font, "DEVICE DETAILS & TELEMETRY", x + 8, y + 8, 0xFFE0E0E0, false);
        guiGraphics.hLine(x + 4, x + width - 4, y + 22, 0xFF1F3554);

        if (selectedDevice == null) {
            guiGraphics.drawCenteredString(this.font, "NO DEVICE SELECTED", x + width / 2, y + height / 2 - 8, 0xFF667788);
            guiGraphics.drawCenteredString(this.font, "Select a device from the list", x + width / 2, y + height / 2 + 4, 0xFF445566);
            return;
        }

        int textY = y + 28;
        int lineGap = 15;

        // 1. 機器の名前
        drawDetailRow(guiGraphics, "DEVICE NAME:", selectedDevice.name(), x + 10, textY, 0xFF6C8EA4, 0xFFFFFFFF);
        textY += lineGap;

        // 2. 座標
        String posStr = (selectedDevice.pos() != null) ? selectedDevice.pos().toShortString() : "Unknown";
        drawDetailRow(guiGraphics, "POSITION:", posStr, x + 10, textY, 0xFF6C8EA4, 0xFFCCDDEE);
        textY += lineGap;

        // 区切り線
        guiGraphics.hLine(x + 8, x + width - 8, textY + 2, 0xFF1A2A3E);
        textY += 8;

        // 3. 各機器の種類に応じた詳細情報（分類のみ表示）
        Level level = (this.minecraft != null) ? this.minecraft.level : null;
        BlockEntity be = (level != null && selectedDevice.pos() != null && level.isLoaded(selectedDevice.pos()))
                ? level.getBlockEntity(selectedDevice.pos())
                : null;

        if (be instanceof Oto127mmBlockEntity oto) {
            drawDetailRow(guiGraphics, "CATEGORY:", "Gun", x + 10, textY, 0xFF4DEEEA, 0xFFFFFFFF);
            textY += lineGap;

            AmmoInfo ammo = getAmmoInfo(oto);
            String ammoCountText = ammo.totalCount() > 0 ? ammo.totalCount() + " rounds" : "0 rounds (EMPTY)";
            int ammoColor = ammo.totalCount() > 0 ? 0xFF00FF88 : 0xFFFF4444;
            drawDetailRow(guiGraphics, "AMMO COUNT:", ammoCountText, x + 10, textY, 0xFF6C8EA4, ammoColor);
            textY += lineGap;

            drawDetailRow(guiGraphics, "AMMO TYPE:", ammo.nextAmmoName(), x + 10, textY, 0xFF6C8EA4, ammo.totalCount() > 0 ? 0xFFCCDDEE : 0xFF888888);
            textY += lineGap;

            String angleStr = String.format("Yaw: %.1f°  |  Pitch: %.1f°", oto.getRenderTargetYaw(1.0f), oto.getRenderTargetPitch(1.0f));
            drawDetailRow(guiGraphics, "AIM ANGLE:", angleStr, x + 10, textY, 0xFF6C8EA4, 0xFFCCDDEE);
            textY += lineGap;

            drawDetailRow(guiGraphics, "STATUS:", oto.isConnectedToFcs() ? "ONLINE / LINKED" : "OFFLINE", x + 10, textY, 0xFF6C8EA4, 0xFF00FF88);

        } else if (be instanceof Mk45Mod4BlockEntity mk45) {
            drawDetailRow(guiGraphics, "CATEGORY:", "Gun", x + 10, textY, 0xFF4DEEEA, 0xFFFFFFFF);
            textY += lineGap;

            AmmoInfo ammo = getAmmoInfo(mk45);
            String ammoCountText = ammo.totalCount() > 0 ? ammo.totalCount() + " rounds" : "0 rounds (EMPTY)";
            int ammoColor = ammo.totalCount() > 0 ? 0xFF00FF88 : 0xFFFF4444;
            drawDetailRow(guiGraphics, "AMMO COUNT:", ammoCountText, x + 10, textY, 0xFF6C8EA4, ammoColor);
            textY += lineGap;

            drawDetailRow(guiGraphics, "AMMO TYPE:", ammo.nextAmmoName(), x + 10, textY, 0xFF6C8EA4, ammo.totalCount() > 0 ? 0xFFCCDDEE : 0xFF888888);
            textY += lineGap;

            String angleStr = String.format("Yaw: %.1f°  |  Pitch: %.1f°", mk45.getRenderTargetYaw(1.0f), mk45.getRenderTargetPitch(1.0f));
            drawDetailRow(guiGraphics, "AIM ANGLE:", angleStr, x + 10, textY, 0xFF6C8EA4, 0xFFCCDDEE);
            textY += lineGap;

            drawDetailRow(guiGraphics, "STATUS:", mk45.isConnectedToFcs() ? "ONLINE / LINKED" : "OFFLINE", x + 10, textY, 0xFF6C8EA4, 0xFF00FF88);

        } else if (be instanceof PhalanxBlockEntity phalanx) {
            drawDetailRow(guiGraphics, "CATEGORY:", "CIWS", x + 10, textY, 0xFF4DEEEA, 0xFFFFFFFF);
            textY += lineGap;

            drawDetailRow(guiGraphics, "CALIBER:", "20mm M61A1 Vulcan", x + 10, textY, 0xFF6C8EA4, 0xFFCCDDEE);
            textY += lineGap;

            drawDetailRow(guiGraphics, "FIRING STATE:", phalanx.isFiring() ? "FIRING / ACTIVE" : "STANDBY", x + 10, textY, 0xFF6C8EA4, phalanx.isFiring() ? 0xFFFF4444 : 0xFF00FF88);
            textY += lineGap;

            boolean linked = phalanx.getLinkedFcsCoreUuid() != null;
            drawDetailRow(guiGraphics, "STATUS:", linked ? "ONLINE / LINKED" : "OFFLINE", x + 10, textY, 0xFF6C8EA4, 0xFF00FF88);

        } else if (be instanceof ARWCIWSBlockEntity ciws) {
            drawDetailRow(guiGraphics, "CATEGORY:", "CIWS", x + 10, textY, 0xFF4DEEEA, 0xFFFFFFFF);
            textY += lineGap;

            drawDetailRow(guiGraphics, "FIRING STATE:", ciws.isFiring() ? "FIRING" : "STANDBY", x + 10, textY, 0xFF6C8EA4, ciws.isFiring() ? 0xFFFF4444 : 0xFF00FF88);
            textY += lineGap;

            boolean linked = ciws.getLinkedFcsCoreUuid() != null;
            drawDetailRow(guiGraphics, "STATUS:", linked ? "ONLINE / LINKED" : "OFFLINE", x + 10, textY, 0xFF6C8EA4, 0xFF00FF88);

        } else if (be instanceof Spq9bBlockEntity) {
            drawDetailRow(guiGraphics, "CATEGORY:", "Radar", x + 10, textY, 0xFF4DEEEA, 0xFFFFFFFF);
            textY += lineGap;

            drawDetailRow(guiGraphics, "SCAN SPEED:", "30 RPM (Continuous Rotation)", x + 10, textY, 0xFF6C8EA4, 0xFFCCDDEE);
            textY += lineGap;

            drawDetailRow(guiGraphics, "STATUS:", "ACTIVE / SCANNING", x + 10, textY, 0xFF6C8EA4, 0xFF00FF88);

        } else if (be instanceof HorizontalRadarBlockEntity radar) {
            drawDetailRow(guiGraphics, "CATEGORY:", "Radar", x + 10, textY, 0xFF4DEEEA, 0xFFFFFFFF);
            textY += lineGap;

            drawDetailRow(guiGraphics, "SCAN RANGE:", (int) radar.getScanRange() + " blocks", x + 10, textY, 0xFF6C8EA4, 0xFFCCDDEE);
            textY += lineGap;

            drawDetailRow(guiGraphics, "DETECTED TARGETS:", radar.getDetectedTargets().size() + " tracks", x + 10, textY, 0xFF6C8EA4, 0xFFCCDDEE);
            textY += lineGap;

            drawDetailRow(guiGraphics, "STATUS:", radar.isConnectedToFcs() ? "ONLINE / SCANNING" : "OFFLINE", x + 10, textY, 0xFF6C8EA4, 0xFF00FF88);

        } else if (be instanceof VlsBlockEntity vls) {
            drawDetailRow(guiGraphics, "CATEGORY:", "VLS", x + 10, textY, 0xFF4DEEEA, 0xFFFFFFFF);
            textY += lineGap;

            boolean linked = vls.getLinkedFcsCoreUuid() != null;
            drawDetailRow(guiGraphics, "STATUS:", linked ? "ONLINE / LINKED" : "OFFLINE", x + 10, textY, 0xFF6C8EA4, 0xFF00FF88);

        } else if (be instanceof AbstractSingleGunBlockEntity singleGun) {
            drawDetailRow(guiGraphics, "CATEGORY:", "Gun", x + 10, textY, 0xFF4DEEEA, 0xFFFFFFFF);
            textY += lineGap;

            AmmoInfo ammo = getAmmoInfo(singleGun);
            String ammoCountText = ammo.totalCount() > 0 ? ammo.totalCount() + " rounds" : "0 rounds (EMPTY)";
            int ammoColor = ammo.totalCount() > 0 ? 0xFF00FF88 : 0xFFFF4444;
            drawDetailRow(guiGraphics, "AMMO COUNT:", ammoCountText, x + 10, textY, 0xFF6C8EA4, ammoColor);
            textY += lineGap;

            drawDetailRow(guiGraphics, "AMMO TYPE:", ammo.nextAmmoName(), x + 10, textY, 0xFF6C8EA4, ammo.totalCount() > 0 ? 0xFFCCDDEE : 0xFF888888);
            textY += lineGap;

            String angleStr = String.format("Yaw: %.1f°  |  Pitch: %.1f°", singleGun.getRenderTargetYaw(1.0f), singleGun.getRenderTargetPitch(1.0f));
            drawDetailRow(guiGraphics, "AIM ANGLE:", angleStr, x + 10, textY, 0xFF6C8EA4, 0xFFCCDDEE);
            textY += lineGap;

            drawDetailRow(guiGraphics, "STATUS:", singleGun.isConnectedToFcs() ? "ONLINE / LINKED" : "OFFLINE", x + 10, textY, 0xFF6C8EA4, 0xFF00FF88);

        } else if (be instanceof TestConsoleBlockEntity console) {
            drawDetailRow(guiGraphics, "CATEGORY:", "Console", x + 10, textY, 0xFF4DEEEA, 0xFFFFFFFF);
            textY += lineGap;

            drawDetailRow(guiGraphics, "STATUS:", console.isConnectedToFcs() ? "ONLINE / LINKED" : "OFFLINE", x + 10, textY, 0xFF6C8EA4, 0xFF00FF88);
            textY += lineGap;

            drawDetailRow(guiGraphics, "DETECTED WEAPONS:", console.getConnectedWeapons().size() + " units", x + 10, textY, 0xFF6C8EA4, 0xFFCCDDEE);

        } else if (be instanceof IFcsSensorNode sensorNode) {
            drawDetailRow(guiGraphics, "CATEGORY:", "Radar", x + 10, textY, 0xFF4DEEEA, 0xFFFFFFFF);
            textY += lineGap;

            drawDetailRow(guiGraphics, "DETECTED TARGETS:", sensorNode.getDetectedTargets().size() + " tracks", x + 10, textY, 0xFF6C8EA4, 0xFFCCDDEE);
            textY += lineGap;

            drawDetailRow(guiGraphics, "STATUS:", sensorNode.isConnectedToFcs() ? "ONLINE / LINKED" : "OFFLINE", x + 10, textY, 0xFF6C8EA4, 0xFF00FF88);

        } else if (be instanceof IFcsControllableWeapon weaponNode) {
            drawDetailRow(guiGraphics, "CATEGORY:", "Gun", x + 10, textY, 0xFF4DEEEA, 0xFFFFFFFF);
            textY += lineGap;

            drawDetailRow(guiGraphics, "STATUS:", weaponNode.isConnectedToFcs() ? "ONLINE / LINKED" : "OFFLINE", x + 10, textY, 0xFF6C8EA4, 0xFF00FF88);

        } else if (be instanceof IFcsNetworkNode node) {
            drawDetailRow(guiGraphics, "CATEGORY:", "Device", x + 10, textY, 0xFF4DEEEA, 0xFFFFFFFF);
            textY += lineGap;

            drawDetailRow(guiGraphics, "STATUS:", node.isConnectedToFcs() ? "ONLINE / LINKED" : "OFFLINE", x + 10, textY, 0xFF6C8EA4, 0xFF00FF88);

        } else if (be instanceof AbstractARWBlockEntity) {
            drawDetailRow(guiGraphics, "CATEGORY:", "Device", x + 10, textY, 0xFF4DEEEA, 0xFFFFFFFF);
            textY += lineGap;

            drawDetailRow(guiGraphics, "STATUS:", "CONNECTED", x + 10, textY, 0xFF6C8EA4, 0xFF00FF88);

        } else {
            drawDetailRow(guiGraphics, "CATEGORY:", "Device", x + 10, textY, 0xFF4DEEEA, 0xFFFFFFFF);
            textY += lineGap;

            drawDetailRow(guiGraphics, "STATUS:", "REMOTE / ONLINE", x + 10, textY, 0xFF6C8EA4, 0xFF88AACC);
        }
    }

    private void drawDetailRow(GuiGraphics guiGraphics, String label, String value, int x, int y, int labelColor, int valueColor) {
        guiGraphics.drawString(this.font, label, x, y, labelColor, false);
        int labelWidth = this.font.width(label + " ");
        guiGraphics.drawString(this.font, value, x + labelWidth, y, valueColor, false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta != 0) {
            int contentY = HEADER_HEIGHT + MARGIN;
            int contentHeight = this.height - contentY - FOOTER_HEIGHT - MARGIN;
            int listTop = contentY + 24;
            int listBottom = contentY + contentHeight - 4;
            int visibleHeight = listBottom - listTop;
            int totalHeight = connectedDevices.size() * (ITEM_HEIGHT + ITEM_SPACING);
            int maxScroll = Math.max(0, totalHeight - visibleHeight);

            if (maxScroll > 0) {
                scrollOffset = (int) Mth.clamp(scrollOffset - delta * (ITEM_HEIGHT + ITEM_SPACING), 0, maxScroll);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int contentY = HEADER_HEIGHT + MARGIN;
            int contentHeight = this.height - contentY - FOOTER_HEIGHT - MARGIN;
            int totalContentWidth = this.width - MARGIN * 2;
            int listWidth = (int) (totalContentWidth * 0.45f);
            int listX = MARGIN;
            int listTop = contentY + 24;
            int listBottom = contentY + contentHeight - 4;

            // スクロールバークリック判定
            int scrollTrackX = listX + listWidth - 8;
            if (mouseX >= scrollTrackX && mouseX <= scrollTrackX + 6 && mouseY >= listTop && mouseY <= listBottom) {
                isDraggingScrollBar = true;
                return true;
            }

            if (hoveredIndex >= 0 && hoveredIndex < connectedDevices.size()) {
                selectedDevice = connectedDevices.get(hoveredIndex);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isDraggingScrollBar = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDraggingScrollBar && button == 0) {
            int contentY = HEADER_HEIGHT + MARGIN;
            int contentHeight = this.height - contentY - FOOTER_HEIGHT - MARGIN;
            int listTop = contentY + 24;
            int listBottom = contentY + contentHeight - 4;
            int visibleHeight = listBottom - listTop;
            int totalHeight = connectedDevices.size() * (ITEM_HEIGHT + ITEM_SPACING);
            int maxScroll = Math.max(0, totalHeight - visibleHeight);

            if (maxScroll > 0) {
                float ratio = (float) (mouseY - listTop) / (float) visibleHeight;
                scrollOffset = (int) Mth.clamp(ratio * maxScroll, 0, maxScroll);
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
