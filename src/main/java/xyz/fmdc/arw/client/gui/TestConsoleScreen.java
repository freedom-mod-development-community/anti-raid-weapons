package xyz.fmdc.arw.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import xyz.fmdc.arw.api.control.IRemoteControllableWeapon;
import xyz.fmdc.arw.api.fcs.IFcsControllableWeapon;
import xyz.fmdc.arw.common.blockentity.AbstractMissileLauncherBlockEntity;
import xyz.fmdc.arw.common.blockentity.AbstractSingleGunBlockEntity;
import xyz.fmdc.arw.common.blockentity.console.TestConsoleBlockEntity;
import xyz.fmdc.arw.common.blockentity.fcs.AbstractFcsCoreBlockEntity;
import xyz.fmdc.arw.common.blockentity.vls.VlsBlockEntity;
import xyz.fmdc.arw.common.blockentity.weapon.ARWCIWSBlockEntity;
import xyz.fmdc.arw.common.blockentity.weapon.Mk13GmlsBlockEntity;
import xyz.fmdc.arw.common.blockentity.weapon.singlegun.Oto127mmBlockEntity;
import xyz.fmdc.arw.network.PacketHandler;
import xyz.fmdc.arw.network.ServerboundRemoteControlSessionPacket;
import xyz.fmdc.arw.network.ServerboundWeaponControlPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TestConsoleScreen extends Screen {

    private final BlockPos consolePos;
    private UUID linkedCoreUuid = null;
    private BlockPos linkedCorePos = null;

    // ページ定義 (1: 兵器一覧選択, 2: 選択兵器の遠隔操作)
    private int currentPage = 1;

    // 兵器カテゴリ
    public enum WeaponCategory {
        MISSILE_LAUNCHER("Missile Launcher", 0xFFFF9900),
        NAVAL_GUN("Naval Gun", 0xFF00DDFF),
        CIWS("CIWS", 0xFFFF4466),
        VLS("VLS", 0xFFFFAA33),
        GENERIC("Weapon System", 0xFF00FF88);

        private final String labelEn;
        private final int color;

        WeaponCategory(String labelEn, int color) {
            this.labelEn = labelEn;
            this.color = color;
        }

        public String getLabel() {
            return labelEn;
        }

        public int getColor() {
            return color;
        }
    }

    // 兵器エントリ
    public record WeaponEntry(
            UUID uuid,
            String name,
            BlockPos pos,
            WeaponCategory category,
            String typeName
    ) {}

    private final List<WeaponEntry> weaponList = new ArrayList<>();
    private WeaponEntry selectedWeapon = null;
    private int hoveredIndex = -1;
    private int listScrollOffset = 0;
    private boolean isDraggingListScroll = false;

    // 遠隔操作用状態 (ページ2)
    private float targetYaw = 0.0f;
    private float targetPitch = 0.0f;
    private boolean isArmed = true;

    // ページ1 ボタン
    private Button startControlButton;
    private Button reloadDevicesButton;

    // ページ2 コンポーネント (テキストボックス & アクションボタン)
    private Button backButton;
    private Button armToggleButton;
    private Button fireButton;

    private EditBox yawEditBox;
    private EditBox pitchEditBox;
    private Button applyAnglesButton;
    private Button readCurrentAnglesButton;
    private Button centerAimButton;

    // UI定数（コンパクト化）
    private static final int MARGIN = 8;
    private static final int HEADER_HEIGHT = 26;
    private static final int FOOTER_HEIGHT = 20;
    private static final int GAP = 8;
    private static final int ITEM_HEIGHT = 24;
    private static final int ITEM_SPACING = 2;

    public TestConsoleScreen(BlockPos consolePos) {
        super(Component.literal("ARW Test Console"));
        this.consolePos = consolePos;
        refreshWeapons();
    }

    private void refreshWeapons() {
        weaponList.clear();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        BlockEntity be = mc.level.getBlockEntity(consolePos);
        if (!(be instanceof TestConsoleBlockEntity console)) return;

        this.linkedCoreUuid = console.getLinkedFcsCoreUuid();
        this.linkedCorePos = console.getLinkedFcsCorePos();

        AbstractFcsCoreBlockEntity fcsCore = console.getLinkedFcsCore();
        if (fcsCore == null) return;

        Map<UUID, BlockPos> nodePositions = fcsCore.getNodePositions();
        for (UUID uuid : fcsCore.getConnectedNodeUuids()) {
            BlockPos pos = nodePositions.get(uuid);
            if (pos == null || !mc.level.isLoaded(pos)) continue;

            BlockEntity nodeBe = mc.level.getBlockEntity(pos);
            if (nodeBe == null) continue;

            // 兵器ノードのみを抽出
            if (nodeBe instanceof IFcsControllableWeapon || nodeBe instanceof IRemoteControllableWeapon) {
                String name = mc.level.getBlockState(pos).getBlock().getName().getString();
                WeaponCategory category = categorizeWeapon(nodeBe);
                String typeName = nodeBe.getClass().getSimpleName();
                weaponList.add(new WeaponEntry(uuid, name, pos, category, typeName));
            }
        }

        if (!weaponList.isEmpty()) {
            // 選択中の兵器がリストに存在するか確認し、なければ先頭を選択
            if (selectedWeapon == null || weaponList.stream().noneMatch(w -> w.uuid().equals(selectedWeapon.uuid()))) {
                selectedWeapon = weaponList.get(0);
            }
        } else {
            selectedWeapon = null;
        }
    }

    private WeaponCategory categorizeWeapon(BlockEntity be) {
        if (be instanceof AbstractMissileLauncherBlockEntity || be instanceof Mk13GmlsBlockEntity) {
            return WeaponCategory.MISSILE_LAUNCHER;
        } else if (be instanceof AbstractSingleGunBlockEntity) {
            return WeaponCategory.NAVAL_GUN;
        } else if (be instanceof ARWCIWSBlockEntity) {
            return WeaponCategory.CIWS;
        } else if (be instanceof VlsBlockEntity) {
            return WeaponCategory.VLS;
        }
        return WeaponCategory.GENERIC;
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();

        int contentY = HEADER_HEIGHT + MARGIN;
        int contentHeight = this.height - contentY - FOOTER_HEIGHT - MARGIN;
        int totalContentWidth = this.width - MARGIN * 2;

        if (currentPage == 1) {
            initPage1(contentY, contentHeight, totalContentWidth);
        } else if (currentPage == 2) {
            initPage2(contentY, contentHeight, totalContentWidth);
        }
    }

    private void initPage1(int contentY, int contentHeight, int totalContentWidth) {
        // 右上 デバイスリロードボタン
        int reloadBtnW = 75;
        int reloadBtnX = this.width - MARGIN - reloadBtnW;
        reloadDevicesButton = this.addRenderableWidget(Button.builder(
                Component.literal("⟳ RELOAD"),
                button -> {
                    refreshWeapons();
                    init();
                }
        ).bounds(reloadBtnX, 3, reloadBtnW, 20).build());

        int listWidth = (int) (totalContentWidth * 0.48f);
        int listX = MARGIN;
        int detailX = listX + listWidth + GAP;
        int detailWidth = this.width - MARGIN - detailX;

        int btnWidth = Math.min(160, detailWidth - 16);
        int btnHeight = 22;
        int btnX = detailX + (detailWidth - btnWidth) / 2;
        int btnY = contentY + contentHeight - btnHeight - 8;

        startControlButton = this.addRenderableWidget(Button.builder(
                Component.literal("▶ CONTROL WEAPON"),
                button -> enterPage2()
        ).bounds(btnX, btnY, btnWidth, btnHeight).build());

        startControlButton.active = (selectedWeapon != null);
    }

    private void initPage2(int contentY, int contentHeight, int totalContentWidth) {
        // 左上 戻るボタン
        backButton = this.addRenderableWidget(Button.builder(
                Component.literal("◀ BACK"),
                button -> exitPage2()
        ).bounds(MARGIN, 3, 70, 20).build());

        // 右上 ARMED/SAFE トグルボタン
        int armBtnWidth = 90;
        int armBtnX = this.width - MARGIN - armBtnWidth;
        armToggleButton = this.addRenderableWidget(Button.builder(
                getArmedButtonText(),
                button -> {
                    isArmed = !isArmed;
                    button.setMessage(getArmedButtonText());
                }
        ).bounds(armBtnX, 3, armBtnWidth, 20).build());

        int panelCenterX = this.width / 2;

        // 角度入力ボックスの配置
        int boxW = 70;
        int boxH = 18;
        int inputStartY = contentY + 46;

        // Yaw 入力
        yawEditBox = new EditBox(this.font, panelCenterX - 30, inputStartY, boxW, boxH, Component.literal("Yaw"));
        yawEditBox.setValue(String.format("%.1f", targetYaw));
        yawEditBox.setFilter(s -> s.isEmpty() || s.equals("-") || s.matches("^-?\\d*(\\.\\d*)?$"));
        yawEditBox.setMaxLength(7);
        this.addRenderableWidget(yawEditBox);

        // Pitch 入力
        pitchEditBox = new EditBox(this.font, panelCenterX - 30, inputStartY + boxH + 6, boxW, boxH, Component.literal("Pitch"));
        pitchEditBox.setValue(String.format("%.1f", targetPitch));
        pitchEditBox.setFilter(s -> s.isEmpty() || s.equals("-") || s.matches("^-?\\d*(\\.\\d*)?$"));
        pitchEditBox.setMaxLength(7);
        this.addRenderableWidget(pitchEditBox);

        // 操作ボタン群 (横1列に3つのボタンを並べる)
        int actionBtnY = inputStartY + (boxH + 6) * 2 + 6;
        int btnH = 20;
        int applyW = 90;
        int readW = 90;
        int centerW = 75;
        int spacing = 6;
        int totalBtnW = applyW + readW + centerW + (spacing * 2);
        int btnStartX = panelCenterX - totalBtnW / 2;

        applyAnglesButton = this.addRenderableWidget(Button.builder(
                Component.literal("✔ SET [Enter]"),
                b -> applyAnglesFromEditBoxes()
        ).bounds(btnStartX, actionBtnY, applyW, btnH).build());

        readCurrentAnglesButton = this.addRenderableWidget(Button.builder(
                Component.literal("⟳ READ"),
                b -> readCurrentAnglesFromWeapon()
        ).bounds(btnStartX + applyW + spacing, actionBtnY, readW, btnH).build());

        centerAimButton = this.addRenderableWidget(Button.builder(
                Component.literal("✛ CENTER"),
                b -> {
                    targetYaw = 0.0f;
                    targetPitch = 0.0f;
                    updateEditBoxValues();
                    sendControlPacket(false);
                }
        ).bounds(btnStartX + applyW + readW + (spacing * 2), actionBtnY, centerW, btnH).build());

        // 発射ボタン（最下部にコンパクトに配置）
        int fireBtnW = Math.min(260, totalContentWidth - 40);
        int fireBtnH = 24;
        int fireBtnX = panelCenterX - fireBtnW / 2;
        int fireBtnY = contentY + contentHeight - fireBtnH - 6;

        fireButton = this.addRenderableWidget(Button.builder(
                getFireButtonLabel(),
                button -> triggerWeaponFire()
        ).bounds(fireBtnX, fireBtnY, fireBtnW, fireBtnH).build());
    }

    private void updateEditBoxValues() {
        if (yawEditBox != null) {
            yawEditBox.setValue(String.format("%.1f", targetYaw));
        }
        if (pitchEditBox != null) {
            pitchEditBox.setValue(String.format("%.1f", targetPitch));
        }
    }

    private void applyAnglesFromEditBoxes() {
        if (yawEditBox != null) {
            String val = yawEditBox.getValue().trim();
            if (!val.isEmpty() && !val.equals("-")) {
                try {
                    targetYaw = Mth.wrapDegrees(Float.parseFloat(val));
                } catch (NumberFormatException ignored) {}
            }
        }
        if (pitchEditBox != null) {
            String val = pitchEditBox.getValue().trim();
            if (!val.isEmpty() && !val.equals("-")) {
                try {
                    targetPitch = Mth.clamp(Float.parseFloat(val), -85.0f, 85.0f);
                } catch (NumberFormatException ignored) {}
            }
        }
        updateEditBoxValues();
        sendControlPacket(false);
    }

    private void readCurrentAnglesFromWeapon() {
        if (selectedWeapon == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.isLoaded(selectedWeapon.pos())) {
            BlockEntity be = mc.level.getBlockEntity(selectedWeapon.pos());
            if (be instanceof AbstractMissileLauncherBlockEntity launcher) {
                this.targetYaw = launcher.getCurrentYaw();
                this.targetPitch = launcher.getCurrentPitch();
            } else if (be instanceof AbstractSingleGunBlockEntity gun) {
                this.targetYaw = gun.getRenderTargetYaw(1.0f);
                this.targetPitch = gun.getRenderTargetPitch(1.0f);
            }
            updateEditBoxValues();
            sendControlPacket(false);
        }
    }

    private Component getArmedButtonText() {
        return isArmed ? Component.literal("● ARMED") : Component.literal("○ SAFE");
    }

    private Component getFireButtonLabel() {
        if (selectedWeapon == null) return Component.literal("FIRE / LAUNCH");
        return switch (selectedWeapon.category()) {
            case MISSILE_LAUNCHER -> Component.literal("▲ LAUNCH MISSILE");
            case NAVAL_GUN -> Component.literal("● FIRE CANNON");
            case CIWS -> Component.literal("■ ENGAGE TARGET");
            case VLS -> Component.literal("▲ LAUNCH VLS");
            default -> Component.literal("● FIRE");
        };
    }

    private void enterPage2() {
        if (selectedWeapon == null) return;
        currentPage = 2;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.isLoaded(selectedWeapon.pos())) {
            BlockEntity be = mc.level.getBlockEntity(selectedWeapon.pos());
            if (be instanceof AbstractMissileLauncherBlockEntity launcher) {
                this.targetYaw = launcher.getCurrentYaw();
                this.targetPitch = launcher.getCurrentPitch();
            } else if (be instanceof AbstractSingleGunBlockEntity gun) {
                this.targetYaw = gun.getRenderTargetYaw(1.0f);
                this.targetPitch = gun.getRenderTargetPitch(1.0f);
            }
        }

        PacketHandler.sendToServer(new ServerboundRemoteControlSessionPacket(selectedWeapon.pos(), true));
        init();
    }

    private void exitPage2() {
        if (selectedWeapon != null) {
            PacketHandler.sendToServer(new ServerboundRemoteControlSessionPacket(selectedWeapon.pos(), false));
        }
        currentPage = 1;
        refreshWeapons();
        init();
    }

    @Override
    public void onClose() {
        if (currentPage == 2 && selectedWeapon != null) {
            PacketHandler.sendToServer(new ServerboundRemoteControlSessionPacket(selectedWeapon.pos(), false));
        }
        super.onClose();
    }

    private void triggerWeaponFire() {
        if (!isArmed || selectedWeapon == null) return;
        sendControlPacket(true);
    }

    private void sendControlPacket(boolean fire) {
        if (selectedWeapon == null) return;
        PacketHandler.sendToServer(new ServerboundWeaponControlPacket(
                selectedWeapon.pos(),
                targetYaw,
                targetPitch,
                fire
        ));
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        // 全画面ダーク背景
        guiGraphics.fill(0, 0, this.width, this.height, 0xF2080E16);

        // ヘッダーエリア
        guiGraphics.fill(0, 0, this.width, HEADER_HEIGHT, 0xFF101926);
        guiGraphics.hLine(0, this.width, HEADER_HEIGHT, 0xFF1D385C);

        int contentY = HEADER_HEIGHT + MARGIN;
        int contentHeight = this.height - contentY - FOOTER_HEIGHT - MARGIN;
        int totalContentWidth = this.width - MARGIN * 2;

        if (currentPage == 1) {
            renderPage1(guiGraphics, contentY, contentHeight, totalContentWidth, mouseX, mouseY);
        } else {
            renderPage2(guiGraphics, contentY, contentHeight, totalContentWidth, mouseX, mouseY, partialTick);
        }

        // フッターエリア
        int footerY = this.height - FOOTER_HEIGHT;
        guiGraphics.fill(0, footerY, this.width, this.height, 0xFF101926);
        guiGraphics.hLine(0, this.width, footerY, 0xFF1D385C);

        String pageInfo = currentPage == 1 ? "PAGE 1/2: WEAPON SELECT" : "PAGE 2/2: REMOTE CONTROL";
        guiGraphics.drawString(this.font, pageInfo, MARGIN, footerY + 6, 0xFF00FF88, false);

        String escHint = "[ESC] EXIT";
        int escW = this.font.width(escHint);
        guiGraphics.drawString(this.font, escHint, this.width - MARGIN - escW, footerY + 6, 0xFF88AACC, false);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    // ==========================================
    // ページ 1: 兵器一覧・選択画面
    // ==========================================
    private void renderPage1(GuiGraphics guiGraphics, int contentY, int contentHeight, int totalContentWidth, int mouseX, int mouseY) {
        // ヘッダータイトル
        guiGraphics.drawString(this.font, "ARW FCS TEST CONSOLE", MARGIN + 4, 9, 0xFF4DEEEA, false);

        int listWidth = (int) (totalContentWidth * 0.48f);
        int listX = MARGIN;
        int detailX = listX + listWidth + GAP;
        int detailWidth = this.width - MARGIN - detailX;

        drawWeaponList(guiGraphics, listX, contentY, listWidth, contentHeight, mouseX, mouseY);
        drawWeaponPreview(guiGraphics, detailX, contentY, detailWidth, contentHeight);

        if (startControlButton != null) {
            startControlButton.active = (selectedWeapon != null);
        }
    }

    private void drawWeaponList(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY) {
        guiGraphics.fill(x, y, x + width, y + height, 0xFF060B12);
        guiGraphics.renderOutline(x, y, width, height, 0xFF1F3554);

        guiGraphics.drawString(this.font, "WEAPONS (" + weaponList.size() + ")", x + 6, y + 6, 0xFFE0E0E0, false);
        guiGraphics.hLine(x + 2, x + width - 2, y + 18, 0xFF1F3554);

        hoveredIndex = -1;

        if (weaponList.isEmpty()) {
            String msg = (linkedCorePos == null) ? "CORE NOT LINKED" : "NO WEAPONS LINKED";
            guiGraphics.drawCenteredString(this.font, msg, x + width / 2, y + height / 2 - 4, 0xFFFF6666);
            return;
        }

        int listTop = y + 20;
        int listBottom = y + height - 2;
        int visibleHeight = listBottom - listTop;
        int totalHeight = weaponList.size() * (ITEM_HEIGHT + ITEM_SPACING);
        int maxScroll = Math.max(0, totalHeight - visibleHeight);

        listScrollOffset = Mth.clamp(listScrollOffset, 0, maxScroll);

        int scrollbarWidth = (maxScroll > 0) ? 4 : 0;
        int itemWidth = width - 6 - scrollbarWidth;

        guiGraphics.enableScissor(x + 2, listTop, x + width - 2, listBottom);

        for (int i = 0; i < weaponList.size(); i++) {
            int itemY = listTop + i * (ITEM_HEIGHT + ITEM_SPACING) - listScrollOffset;
            if (itemY + ITEM_HEIGHT < listTop || itemY > listBottom) continue;

            WeaponEntry w = weaponList.get(i);
            boolean isSelected = (w == selectedWeapon);
            boolean isHovered = mouseX >= x + 3 && mouseX <= x + 3 + itemWidth &&
                    mouseY >= Math.max(listTop, itemY) && mouseY <= Math.min(listBottom, itemY + ITEM_HEIGHT);

            if (isHovered) hoveredIndex = i;

            int bgColor = isSelected ? 0xFF1C3454 : (isHovered ? 0xFF14243B : 0xFF0E1722);
            int borderColor = isSelected ? 0xFF4DEEEA : (isHovered ? 0xFF355B8C : 0xFF16253A);
            guiGraphics.fill(x + 3, itemY, x + 3 + itemWidth, itemY + ITEM_HEIGHT, bgColor);
            guiGraphics.renderOutline(x + 3, itemY, itemWidth, ITEM_HEIGHT, borderColor);

            // インジケータ
            guiGraphics.fill(x + 4, itemY + 2, x + 6, itemY + ITEM_HEIGHT - 2, w.category().getColor());

            // 兵器名
            int textColor = isSelected ? 0xFFFFFFFF : 0xFFCCDDEE;
            guiGraphics.drawString(this.font, w.name(), x + 9, itemY + 3, textColor, false);

            // カテゴリ
            guiGraphics.drawString(this.font, w.category().getLabel(), x + 9, itemY + 13, 0xFF6C8EA4, false);
        }

        guiGraphics.disableScissor();

        // スクロールバー
        if (maxScroll > 0) {
            int scrollTrackX = x + width - 5;
            guiGraphics.fill(scrollTrackX, listTop, scrollTrackX + 3, listBottom, 0xFF0A1422);

            int thumbHeight = Math.max(12, (int) ((float) visibleHeight / totalHeight * visibleHeight));
            int thumbY = listTop + (int) ((float) listScrollOffset / maxScroll * (visibleHeight - thumbHeight));
            guiGraphics.fill(scrollTrackX, thumbY, scrollTrackX + 3, thumbY + thumbHeight, 0xFF355B8C);
        }
    }

    private void drawWeaponPreview(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, 0xFF060B12);
        guiGraphics.renderOutline(x, y, width, height, 0xFF1F3554);

        guiGraphics.drawString(this.font, "DETAILS", x + 6, y + 6, 0xFFE0E0E0, false);
        guiGraphics.hLine(x + 2, x + width - 2, y + 18, 0xFF1F3554);

        if (selectedWeapon == null) {
            guiGraphics.drawCenteredString(this.font, "SELECT A WEAPON", x + width / 2, y + height / 2 - 4, 0xFF667788);
            return;
        }

        int textY = y + 24;
        int lineGap = 13;

        drawDetailRow(guiGraphics, "NAME:", selectedWeapon.name(), x + 8, textY, 0xFF6C8EA4, 0xFFFFFFFF);
        textY += lineGap;

        drawDetailRow(guiGraphics, "TYPE:", selectedWeapon.category().getLabel(), x + 8, textY, 0xFF6C8EA4, selectedWeapon.category().getColor());
        textY += lineGap;

        drawDetailRow(guiGraphics, "POS:", "[" + selectedWeapon.pos().toShortString() + "]", x + 8, textY, 0xFF6C8EA4, 0xFFCCDDEE);
        textY += lineGap;

        Minecraft mc = Minecraft.getInstance();
        BlockEntity be = (mc.level != null && mc.level.isLoaded(selectedWeapon.pos()))
                ? mc.level.getBlockEntity(selectedWeapon.pos())
                : null;

        if (be instanceof AbstractMissileLauncherBlockEntity launcher) {
            int cd = launcher.getCooldownTicks();
            String cdStr = cd > 0 ? "RELOAD (" + cd + "t)" : "READY";
            drawDetailRow(guiGraphics, "STATUS:", cdStr, x + 8, textY, 0xFF6C8EA4, cd > 0 ? 0xFFFF8800 : 0xFF00FF88);
        } else if (be instanceof AbstractSingleGunBlockEntity gun) {
            if (gun instanceof Oto127mmBlockEntity oto) {
                IItemHandler handler = oto.getInventory();
                int ammo = 0;
                for (int s = 0; s < handler.getSlots(); s++) {
                    ammo += handler.getStackInSlot(s).getCount();
                }
                drawDetailRow(guiGraphics, "AMMO:", ammo + " rds", x + 8, textY, 0xFF6C8EA4, ammo > 0 ? 0xFF00FF88 : 0xFFFF4444);
            } else {
                drawDetailRow(guiGraphics, "STATUS:", "READY", x + 8, textY, 0xFF6C8EA4, 0xFF00FF88);
            }
        } else {
            drawDetailRow(guiGraphics, "STATUS:", "CONNECTED", x + 8, textY, 0xFF6C8EA4, 0xFF00FF88);
        }
    }

    // ==========================================
    // ページ 2: 遠隔操作画面（スリム＆コンパクト設計）
    // ==========================================
    private void renderPage2(GuiGraphics guiGraphics, int contentY, int contentHeight, int totalContentWidth, int mouseX, int mouseY, float partialTick) {
        if (selectedWeapon == null) {
            guiGraphics.drawCenteredString(this.font, "NO WEAPON SELECTED", this.width / 2, this.height / 2, 0xFFFF4444);
            return;
        }

        // ヘッダー部タイトル（兵器名表示）
        String title = "REMOTE: " + selectedWeapon.name();
        guiGraphics.drawString(this.font, title, MARGIN + 76, 9, selectedWeapon.category().getColor(), false);

        int panelX = MARGIN;
        int panelY = contentY;
        int panelW = totalContentWidth;
        int panelH = contentHeight;

        drawSlimControlPanel(guiGraphics, panelX, panelY, panelW, panelH);

        if (fireButton != null) {
            fireButton.active = isArmed;
        }
    }

    private void drawSlimControlPanel(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        // パネル背景 & 枠
        guiGraphics.fill(x, y, x + width, y + height, 0xFF060B12);
        guiGraphics.renderOutline(x, y, width, height, 0xFF1F3554);

        Minecraft mc = Minecraft.getInstance();
        BlockEntity be = (mc.level != null && mc.level.isLoaded(selectedWeapon.pos()))
                ? mc.level.getBlockEntity(selectedWeapon.pos())
                : null;

        // -------------------------------------------------------------
        // 上段：兵器ステータス（2行でスリムに表示）
        // -------------------------------------------------------------
        int textY = y + 8;
        int lineGap = 13;
        int col1X = x + 12;
        int col2X = x + width / 2 + 10;

        // 行1
        drawDetailRow(guiGraphics, "NAME:", selectedWeapon.name(), col1X, textY, 0xFF6C8EA4, 0xFFFFFFFF);
        drawDetailRow(guiGraphics, "POS:", "[" + selectedWeapon.pos().toShortString() + "]", col2X, textY, 0xFF6C8EA4, 0xFFCCDDEE);
        textY += lineGap;

        // 行2
        drawDetailRow(guiGraphics, "TYPE:", selectedWeapon.category().getLabel(), col1X, textY, 0xFF6C8EA4, selectedWeapon.category().getColor());

        String statusStr = "READY";
        int statusCol = 0xFF00FF88;
        if (selectedWeapon.category() == WeaponCategory.MISSILE_LAUNCHER && be instanceof AbstractMissileLauncherBlockEntity launcher) {
            int cd = launcher.getCooldownTicks();
            if (cd > 0) {
                statusStr = "RELOADING (" + cd + "t)";
                statusCol = 0xFFFF9900;
            }
        } else if (be instanceof Oto127mmBlockEntity oto) {
            IItemHandler h = oto.getInventory();
            int rds = 0;
            for (int i = 0; i < h.getSlots(); i++) rds += h.getStackInSlot(i).getCount();
            statusStr = rds + " rds loaded";
            statusCol = rds > 0 ? 0xFF00FF88 : 0xFFFF4444;
        }
        drawDetailRow(guiGraphics, "STATUS:", statusStr, col2X, textY, 0xFF6C8EA4, statusCol);

        // 区切り線
        guiGraphics.hLine(x + 4, x + width - 4, textY + 13, 0xFF1A2A3E);

        // -------------------------------------------------------------
        // 中段：現在の兵器角度 & 目標角度入力
        // -------------------------------------------------------------
        float curYaw = 0, curPitch = 0;
        if (be instanceof AbstractMissileLauncherBlockEntity launcher) {
            curYaw = launcher.getCurrentYaw();
            curPitch = launcher.getCurrentPitch();
        } else if (be instanceof AbstractSingleGunBlockEntity gun) {
            curYaw = gun.getRenderTargetYaw(1.0f);
            curPitch = gun.getRenderTargetPitch(1.0f);
        }

        int panelCenterX = x + width / 2;
        int inputStartY = y + 46;

        // Yaw 行 ラベル & 現在値
        String yawLabel = "TARGET YAW:";
        int yawLabelW = this.font.width(yawLabel);
        guiGraphics.drawString(this.font, yawLabel, panelCenterX - 36 - yawLabelW, inputStartY + 5, 0xFFCCDDEE, false);

        String curYawText = String.format("(Cur: %+.1f°)", curYaw);
        guiGraphics.drawString(this.font, curYawText, panelCenterX + 46, inputStartY + 5, 0xFFFFAA00, false);

        // Pitch 行 ラベル & 現在値
        String pitchLabel = "TARGET PITCH:";
        int pitchLabelW = this.font.width(pitchLabel);
        guiGraphics.drawString(this.font, pitchLabel, panelCenterX - 36 - pitchLabelW, inputStartY + 24 + 5, 0xFFCCDDEE, false);

        String curPitchText = String.format("(Cur: %+.1f°)", curPitch);
        guiGraphics.drawString(this.font, curPitchText, panelCenterX + 46, inputStartY + 24 + 5, 0xFFFFAA00, false);

        // 最下段操作ヒント
        int hintY = y + height - 46;
        guiGraphics.drawCenteredString(this.font, "[ENTER] Apply Angles  |  [SPACE] Fire", panelCenterX, hintY, 0xFF556677);
    }

    private void drawDetailRow(GuiGraphics guiGraphics, String label, String value, int x, int y, int labelColor, int valueColor) {
        guiGraphics.drawString(this.font, label, x, y, labelColor, false);
        int labelWidth = this.font.width(label + " ");
        guiGraphics.drawString(this.font, value, x + labelWidth, y, valueColor, false);
    }

    // ==========================================
    // 入力イベント（マウス・キーボード・スクロール）
    // ==========================================

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (currentPage == 2) {
            boolean boxFocused = (yawEditBox != null && yawEditBox.isFocused()) ||
                    (pitchEditBox != null && pitchEditBox.isFocused());

            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                applyAnglesFromEditBoxes();
                if (yawEditBox != null) yawEditBox.setFocused(false);
                if (pitchEditBox != null) pitchEditBox.setFocused(false);
                return true;
            }

            if (!boxFocused && keyCode == GLFW.GLFW_KEY_SPACE) {
                triggerWeaponFire();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && currentPage == 1) {
            int contentY = HEADER_HEIGHT + MARGIN;
            int contentHeight = this.height - contentY - FOOTER_HEIGHT - MARGIN;
            int totalContentWidth = this.width - MARGIN * 2;

            int listWidth = (int) (totalContentWidth * 0.48f);
            int listX = MARGIN;
            int listTop = contentY + 20;
            int listBottom = contentY + contentHeight - 2;

            // スクロールバー
            int scrollTrackX = listX + listWidth - 5;
            if (mouseX >= scrollTrackX && mouseX <= scrollTrackX + 5 && mouseY >= listTop && mouseY <= listBottom) {
                isDraggingListScroll = true;
                return true;
            }

            // 兵器リストクリック
            if (hoveredIndex >= 0 && hoveredIndex < weaponList.size()) {
                boolean doubleClick = (selectedWeapon == weaponList.get(hoveredIndex));
                selectedWeapon = weaponList.get(hoveredIndex);
                if (doubleClick) {
                    enterPage2();
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isDraggingListScroll = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && currentPage == 1 && isDraggingListScroll) {
            int contentY = HEADER_HEIGHT + MARGIN;
            int contentHeight = this.height - contentY - FOOTER_HEIGHT - MARGIN;
            int listTop = contentY + 20;
            int listBottom = contentY + contentHeight - 2;
            int visibleHeight = listBottom - listTop;
            int totalHeight = weaponList.size() * (ITEM_HEIGHT + ITEM_SPACING);
            int maxScroll = Math.max(0, totalHeight - visibleHeight);

            if (maxScroll > 0) {
                float ratio = (float) (mouseY - listTop) / (float) visibleHeight;
                listScrollOffset = (int) Mth.clamp(ratio * maxScroll, 0, maxScroll);
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta != 0 && currentPage == 1) {
            int contentY = HEADER_HEIGHT + MARGIN;
            int contentHeight = this.height - contentY - FOOTER_HEIGHT - MARGIN;
            int listTop = contentY + 20;
            int listBottom = contentY + contentHeight - 2;
            int visibleHeight = listBottom - listTop;
            int totalHeight = weaponList.size() * (ITEM_HEIGHT + ITEM_SPACING);
            int maxScroll = Math.max(0, totalHeight - visibleHeight);

            if (maxScroll > 0) {
                listScrollOffset = (int) Mth.clamp(listScrollOffset - delta * (ITEM_HEIGHT + ITEM_SPACING), 0, maxScroll);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
