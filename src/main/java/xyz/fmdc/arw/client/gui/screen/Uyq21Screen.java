package xyz.fmdc.arw.client.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import xyz.fmdc.arw.api.TargetAffiliation;
import xyz.fmdc.arw.api.TrackedTarget;
import xyz.fmdc.arw.api.fcs.IFcsControllableWeapon;
import xyz.fmdc.arw.api.sensor.RadarScanRange;
import xyz.fmdc.arw.client.gui.EmptyMenu;
import xyz.fmdc.arw.common.blockentity.console.Uyq21BlockEntity;
import xyz.fmdc.arw.common.blockentity.fcs.AbstractFcsCoreBlockEntity;
import xyz.fmdc.arw.common.blockentity.vls.VlsBlockEntity;
import xyz.fmdc.arw.common.blockentity.weapon.ARWCIWSBlockEntity;
import xyz.fmdc.arw.common.blockentity.weapon.AbstractMissileLauncherBlockEntity;
import xyz.fmdc.arw.common.blockentity.weapon.AbstractSingleGunBlockEntity;
import xyz.fmdc.arw.common.blockentity.weapon.singlegun.Oto127mmBlockEntity;
import xyz.fmdc.arw.common.item.projectile.FiveInchShellItem;
import xyz.fmdc.arw.network.PacketHandler;
import xyz.fmdc.arw.network.ServerboundSetTargetAffiliationPacket;
import xyz.fmdc.arw.network.ServerboundWeaponControlPacket;

import java.util.*;
import java.util.function.Consumer;

/**
 * AN/UYQ-21(V) 戦術情報表示コンソールGUI
 * - 背景色: #C5A05A
 * - 中心の表示部分は常に4:3（ボタン表示領域を除いた幅に追従）
 * - 中心のレーダー/兵装画面は1:1（高さ方向を1とする正方形）
 * - 左右のボタン横に「< ラベル」や「ラベル >」のインジケーターを2行・縮小フォントで表示
 * - 兵装選択ページ（WEAPONS）: MCDU風の2段組テキスト情報設計、Line Select Keyによる管制
 * - レーダー表示は北が上（North-Up）
 */
public class Uyq21Screen extends AbstractContainerScreen<EmptyMenu> {

    // ボタン総数（各サイド10個）
    public static final int BUTTON_COUNT_PER_SIDE = 10;

    // 背景色: #C5A05A
    public static final int BACKGROUND_COLOR = 0xFFC5A05A;

    // 画面モード（ページ）
    public enum PageMode {
        RADAR("RADAR"),
        WEAPONS("WEAPONS");

        private final String label;
        PageMode(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private PageMode currentPage = PageMode.RADAR;

    // 射撃承認モード（R1用）
    public enum FireApprovalMode {
        AUTO("AUTO"),
        SEMI("SEMI"),
        MANUAL("MANU");

        private final String label;
        FireApprovalMode(String label) { this.label = label; }
        public String getLabel() { return label; }
        public FireApprovalMode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    // 斉射モード（R7用）
    public enum SalvoMode {
        SALVO_1("SALVO 1", 1),
        SALVO_2("SALVO 2", 2),
        BURST("BURST", 5);

        private final String label;
        private final int count;
        SalvoMode(String label, int count) { this.label = label; this.count = count; }
        public String getLabel() { return label; }
        public int getCount() { return count; }
        public SalvoMode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    // レンジプリセット一覧
    private static final float[] RANGE_PRESETS = { 64.0f, 128.0f, 256.0f, 512.0f, 1024.0f, 2048.0f, 4096.0f };
    private int rangePresetIndex = 3; // デフォルト: 512m

    // レーダー画面で選択（フック）したターゲットUUID
    private UUID selectedTargetUuid = null;

    // レーダー画面上のプロット目標（クリック判定用）
    private record GuiRadarTarget(UUID uuid, int x, int y, TrackedTarget target) {}
    private final List<GuiRadarTarget> guiRadarTargets = new ArrayList<>();

    // 兵装エントリー（兵装選択ページ用）
    public record WeaponDisplayEntry(UUID uuid, String name, BlockPos pos, String category, BlockEntity blockEntity) {}
    private final List<WeaponDisplayEntry> connectedWeapons = new ArrayList<>();
    private int selectedWeaponIndex = 0; // 現在選択中の兵装インデックス

    // 兵装ごとの制御ステート保持
    private final Map<UUID, UUID> weaponAssignedTargets = new HashMap<>(); // 兵装UUID -> 割り当て目標UUID
    private final Map<UUID, FireApprovalMode> weaponApprovalModes = new HashMap<>();
    private final Map<UUID, SalvoMode> weaponSalvoModes = new HashMap<>();
    private final Map<UUID, Boolean> weaponHoldFire = new HashMap<>();

    // 兵装一覧のページング（1ページ4スロット: L1〜L4）
    private int weaponPage = 0;
    private static final int WEAPONS_PER_PAGE = 4;

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

    // レスポンシブ座標・寸法（4:3表示部分、1:1レーダー/兵装画面、左右ボーダー、正方形ボタン）
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

    public Uyq21Screen(EmptyMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        Arrays.fill(leftFunctionLabels, "TEMP");
        Arrays.fill(rightFunctionLabels, "TEMP");
    }

    public void setLeftButton(int index, String label, Consumer<Integer> action) {
        if (index >= 0 && index < BUTTON_COUNT_PER_SIDE) {
            leftFunctionLabels[index] = label;
            leftActions[index] = action;
        }
    }

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

        this.imageWidth = this.width;
        this.imageHeight = this.height;
        this.leftPos = 0;
        this.topPos = 0;

        computeLayout();

        // 左右各10個の正方形ボタンを縦1列に配置
        for (int i = 0; i < BUTTON_COUNT_PER_SIDE; i++) {
            final int btnIdx = i;
            int btnY = buttonsStartY + i * (btnSize + btnSpacing);

            leftButtons[i] = this.addRenderableWidget(Button.builder(
                    Component.empty(),
                    btn -> onButtonClicked(SelectedSide.LEFT, btnIdx)
            ).bounds(leftBtnX, btnY, btnSize, btnSize).build());

            rightButtons[i] = this.addRenderableWidget(Button.builder(
                    Component.empty(),
                    btn -> onButtonClicked(SelectedSide.RIGHT, btnIdx)
            ).bounds(rightBtnX, btnY, btnSize, btnSize).build());
        }

        refreshWeaponsList();
        setupPageButtons();
    }

    /**
     * 画面サイズに応じたレスポンシブレイアウトの計算
     */
    private void computeLayout() {
        int margin = Math.max(4, (int) (this.width * 0.015f));
        int gap = Math.max(3, (int) (this.width * 0.01f));
        int vertMargin = Math.max(4, (int) (this.height * 0.02f));

        int approxSpacing = Math.max(1, (int) ((this.height - vertMargin * 2) * 0.012f));
        int calcDisplayW = (int) (((this.width - 2 * (margin + gap)) + 1.8f * approxSpacing) * 20.0f / 23.0f);
        int calcDisplayH = (calcDisplayW * 3) / 4;

        int maxAvailableH = this.height - vertMargin * 2;
        if (calcDisplayH > maxAvailableH) {
            calcDisplayH = maxAvailableH;
            calcDisplayW = (calcDisplayH * 4) / 3;
        }

        this.displayW = Math.max(40, calcDisplayW);
        this.displayH = Math.max(30, (this.displayW * 3) / 4);

        this.displayX = (this.width - this.displayW) / 2;
        this.displayY = (this.height - this.displayH) / 2;

        this.btnSpacing = Math.max(1, Math.min(4, (this.displayH - BUTTON_COUNT_PER_SIDE * 10) / (BUTTON_COUNT_PER_SIDE - 1)));
        this.btnSize = Math.max(8, (this.displayH - this.btnSpacing * (BUTTON_COUNT_PER_SIDE - 1)) / BUTTON_COUNT_PER_SIDE);

        int totalButtonsH = btnSize * BUTTON_COUNT_PER_SIDE + btnSpacing * (BUTTON_COUNT_PER_SIDE - 1);
        this.buttonsStartY = displayY + (displayH - totalButtonsH) / 2;

        this.leftBtnX = Math.max(2, displayX - gap - btnSize);
        this.rightBtnX = Math.min(this.width - btnSize - 2, displayX + displayW + gap);

        this.radarSize = this.displayH;
        this.radarX = this.displayX + (this.displayW - this.radarSize) / 2;
        this.radarY = this.displayY;
    }

    /**
     * ページごとのボタン定義とアクションのバインド
     */
    private void setupPageButtons() {
        Arrays.fill(leftFunctionLabels, "TEMP");
        Arrays.fill(rightFunctionLabels, "TEMP");
        Arrays.fill(leftActions, null);
        Arrays.fill(rightActions, null);

        if (currentPage == PageMode.RADAR) {
            // L0: ページ切替（WEAPONSへ）
            setLeftButton(0, "PAGE\n[WPN]", idx -> switchPage(PageMode.WEAPONS));

            // L1〜L4: 目標識別変更
            setLeftButton(1, "ID\nUNK", idx -> changeSelectedTargetAffiliation(TargetAffiliation.UNKNOWN));
            setLeftButton(2, "ID\nFRND", idx -> changeSelectedTargetAffiliation(TargetAffiliation.FRIENDLY));
            setLeftButton(3, "ID\nHOST", idx -> changeSelectedTargetAffiliation(TargetAffiliation.HOSTILE));
            setLeftButton(4, "ID\nCYCL", idx -> cycleSelectedTargetAffiliation());

            // L5: 選択解除
            setLeftButton(5, "CLR\nLOCK", idx -> this.selectedTargetUuid = null);

            // L6〜L8: レンジ切替
            setLeftButton(6, "RNG\n+", idx -> adjustRange(1));
            setLeftButton(7, "RNG\n-", idx -> adjustRange(-1));
            setLeftButton(8, String.format("RNG\n%.0fm", RANGE_PRESETS[rangePresetIndex]), idx -> adjustRange(1));

            // L9: 予備
            setLeftButton(9, "RADAR\nPAGE", idx -> {});

            // R0: モード表示
            setRightButton(0, "MODE\nRADAR", idx -> {});

            // R1〜R9: 動的目標情報サマリー表示
            setRightButton(1, "TGT\nNONE", idx -> {});
            setRightButton(2, "TYPE\n--", idx -> {});
            setRightButton(3, "DIST\n--", idx -> {});
            setRightButton(4, "ALT\n--", idx -> {});
            setRightButton(5, "SPD\n--", idx -> {});
            setRightButton(6, "IFF\n--", idx -> {});
            setRightButton(7, "TEMP", idx -> {});
            setRightButton(8, "TEMP", idx -> {});
            setRightButton(9, "TEMP", idx -> {});

        } else if (currentPage == PageMode.WEAPONS) {
            // 兵装画面ボタンの初期構築
            updateWeaponsPageButtonsAndLabels();
        }
    }

    /**
     * 兵装ページの左右ボタン定義およびラベルを動的更新
     */
    private void updateWeaponsPageButtonsAndLabels() {
        if (currentPage != PageMode.WEAPONS) return;

        int totalWeapons = connectedWeapons.size();
        int totalPages = Math.max(1, (totalWeapons + WEAPONS_PER_PAGE - 1) / WEAPONS_PER_PAGE);
        weaponPage = Math.max(0, Math.min(weaponPage, totalPages - 1));

        WeaponDisplayEntry activeWpn = (selectedWeaponIndex >= 0 && selectedWeaponIndex < connectedWeapons.size())
                ? connectedWeapons.get(selectedWeaponIndex) : null;
        UUID activeWpnUuid = activeWpn != null ? activeWpn.uuid() : null;

        // 目標情報と射撃諸元計算
        UUID assignedTargetUuid = activeWpnUuid != null ? weaponAssignedTargets.get(activeWpnUuid) : null;
        TrackedTarget assignedTarget = null;
        if (assignedTargetUuid != null && this.menu.getBlockEntity() instanceof Uyq21BlockEntity uyqBE) {
            assignedTarget = uyqBE.getTrackedTargets().get(assignedTargetUuid);
        }

        boolean hasTarget = assignedTarget != null && assignedTarget.getLastKnownPos() != null;
        double distH = 0.0, dist3D = 0.0, azimDeg = 0.0;
        boolean inRange = false;
        if (hasTarget && activeWpn != null) {
            Vec3 tPos = assignedTarget.getLastKnownPos();
            BlockPos wPos = activeWpn.pos();
            double dx = tPos.x - (wPos.getX() + 0.5);
            double dy = tPos.y - (wPos.getY() + 0.5);
            double dz = tPos.z - (wPos.getZ() + 0.5);
            distH = Math.sqrt(dx * dx + dz * dz);
            dist3D = Math.sqrt(dx * dx + dy * dy + dz * dz);
            azimDeg = Math.toDegrees(Math.atan2(dx, -dz));
            if (azimDeg < 0) azimDeg += 360.0;
            float maxR = getMaxWeaponRange(activeWpn.blockEntity());
            inRange = dist3D <= maxR;
        }

        // --- 左サイドボタン（L1〜L10: index 0〜9） ---
        // L1〜L4: 兵装スロット選択
        for (int i = 0; i < WEAPONS_PER_PAGE; i++) {
            final int slotIdx = i;
            final int wpnIdx = weaponPage * WEAPONS_PER_PAGE + slotIdx;
            if (wpnIdx < totalWeapons) {
                WeaponDisplayEntry w = connectedWeapons.get(wpnIdx);
                String label = String.format("< WPN %d:\n  %s", wpnIdx + 1, truncateString(w.name(), 9));
                setLeftButton(i, label, idx -> selectedWeaponIndex = wpnIdx);
            } else {
                setLeftButton(i, "< [EMPTY]", idx -> {});
            }
        }

        // L5: 空き / 拡張
        setLeftButton(4, "<", idx -> {});

        // L6: 区切り線
        setLeftButton(5, "-------------", idx -> {});

        // L7: 目標割り当て（ASSIGN TO HOOKED）
        setLeftButton(6, "< ASSIGN TO\n  HOOKED", idx -> {
            if (activeWpnUuid != null && this.selectedTargetUuid != null) {
                weaponAssignedTargets.put(activeWpnUuid, this.selectedTargetUuid);
            }
        });

        // L8: 目標解除（DESELECT TARGET）
        setLeftButton(7, "< DESELECT\n  TARGET", idx -> {
            if (activeWpnUuid != null) {
                weaponAssignedTargets.remove(activeWpnUuid);
            }
        });

        // L9: ページ送り（PREV PAGE）
        String prevLabel = String.format("< PREV PAGE\n  (%d/%d)", weaponPage + 1, totalPages);
        setLeftButton(8, prevLabel, idx -> {
            weaponPage = (weaponPage - 1 + totalPages) % totalPages;
            updateWeaponsPageButtonsAndLabels();
        });

        // L10: レーダー画面に戻る（RETURN TO RADAR）
        setLeftButton(9, "< RETURN TO\n  RADAR", idx -> switchPage(PageMode.RADAR));


        // --- 右サイドボタン（R1〜R10: index 0〜9） ---
        // R1: 射撃承認モード（STATUS: RDY / SEMI / AUTO）
        FireApprovalMode appMode = activeWpnUuid != null ? weaponApprovalModes.getOrDefault(activeWpnUuid, FireApprovalMode.AUTO) : FireApprovalMode.AUTO;
        String statusLabel = String.format("STATUS: %s\n(%s) >", inRange ? "RDY" : "STBY", appMode.getLabel());
        setRightButton(0, statusLabel, idx -> {
            if (activeWpnUuid != null) {
                weaponApprovalModes.put(activeWpnUuid, appMode.next());
            }
        });

        // R2: 割り当て目標情報（TARGET: TNxxx）
        if (hasTarget) {
            String tn = getTrackNumber(assignedTargetUuid);
            String affilStr = assignedTarget.getAffiliation().getShortLabel();
            setRightButton(1, String.format("TARGET: %s\n[%s] >", tn, affilStr), idx -> {});
        } else {
            setRightButton(1, "TARGET: NONE\n[NO TRK] >", idx -> {});
        }

        // R3: 方位・距離（BRG/RNG）
        if (hasTarget) {
            setRightButton(2, String.format("BRG/RNG: %03.0f°\n%.0fm >", azimDeg, distH), idx -> {});
        } else {
            setRightButton(2, "BRG/RNG: ---\n--- >", idx -> {});
        }

        // R4: 射撃諸元ステータス（SOL）
        if (hasTarget) {
            String solLabel = inRange ? "SOL: READY\nINTERCEPT OK >" : "SOL: NO SOL\nOUT OF RNG >";
            setRightButton(3, solLabel, idx -> {});
        } else {
            setRightButton(3, "SOL: IDLE\nNO TARGET >", idx -> {});
        }

        // R5: 装填弾種・残弾数（AMMO）
        AmmoInfo ammo = getWeaponAmmoInfo(activeWpn != null ? activeWpn.blockEntity() : null);
        String ammoLabel = String.format("AMMO: %s\n[%3d] >", truncateString(ammo.nextAmmo(), 6), ammo.totalCount());
        setRightButton(4, ammoLabel, idx -> {});

        // R6: 区切り線
        setRightButton(5, "-------------", idx -> {});

        // R7: 斉射モード（FIRE MODE: SALVO 1/2/BURST）
        SalvoMode salvo = activeWpnUuid != null ? weaponSalvoModes.getOrDefault(activeWpnUuid, SalvoMode.SALVO_1) : SalvoMode.SALVO_1;
        setRightButton(6, String.format("FIRE MODE:\n%s >", salvo.getLabel()), idx -> {
            if (activeWpnUuid != null) {
                weaponSalvoModes.put(activeWpnUuid, salvo.next());
            }
        });

        // R8: 射撃保留（HOLD FIRE）
        boolean isHold = activeWpnUuid != null && weaponHoldFire.getOrDefault(activeWpnUuid, false);
        String holdLabel = isHold ? "FREE FIRE\n[HOLDING] >" : "HOLD FIRE\n[FREE] >";
        setRightButton(7, holdLabel, idx -> {
            if (activeWpnUuid != null) {
                weaponHoldFire.put(activeWpnUuid, !isHold);
            }
        });

        // R9: 次ページ送り（NEXT PAGE）
        String nextLabel = String.format("NEXT PAGE\n(%d/%d) >", weaponPage + 1, totalPages);
        setRightButton(8, nextLabel, idx -> {
            weaponPage = (weaponPage + 1) % totalPages;
            updateWeaponsPageButtonsAndLabels();
        });

        // R10: 交戦承認・発射キー（ENGAGE / FIRE >>>）
        setRightButton(9, "ENGAGE / FIRE\n>>> >", idx -> executeEngageFire());
    }

    /**
     * 兵装発射命令の実行
     */
    private void executeEngageFire() {
        if (connectedWeapons.isEmpty()) return;
        if (selectedWeaponIndex < 0 || selectedWeaponIndex >= connectedWeapons.size()) return;
        WeaponDisplayEntry wpn = connectedWeapons.get(selectedWeaponIndex);
        if (wpn == null) return;

        // HOLD FIRE 中は発射禁止
        if (weaponHoldFire.getOrDefault(wpn.uuid(), false)) return;

        UUID tgtUuid = weaponAssignedTargets.get(wpn.uuid());
        if (tgtUuid == null) return;

        if (this.menu.getBlockEntity() instanceof Uyq21BlockEntity uyqBE) {
            TrackedTarget tgt = uyqBE.getTrackedTargets().get(tgtUuid);
            if (tgt == null || tgt.getLastKnownPos() == null) return;

            Vec3 tPos = tgt.getLastKnownPos();
            BlockPos wPos = wpn.pos();
            double dx = tPos.x - (wPos.getX() + 0.5);
            double dy = tPos.y - (wPos.getY() + 0.5);
            double dz = tPos.z - (wPos.getZ() + 0.5);
            double distH = Math.sqrt(dx * dx + dz * dz);

            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float pitch = (float) -Math.toDegrees(Math.atan2(dy, distH));

            // サーバーへ発射制御パケットを送信
            PacketHandler.sendToServer(new ServerboundWeaponControlPacket(wPos, yaw, pitch, true));
        }
    }

    private void switchPage(PageMode newPage) {
        this.currentPage = newPage;
        if (newPage == PageMode.WEAPONS) {
            refreshWeaponsList();
        }
        setupPageButtons();
    }

    private void adjustRange(int delta) {
        rangePresetIndex = (rangePresetIndex + delta + RANGE_PRESETS.length) % RANGE_PRESETS.length;
        if (currentPage == PageMode.RADAR) {
            setLeftButton(8, String.format("RNG\n%.0fm", RANGE_PRESETS[rangePresetIndex]), idx -> adjustRange(1));
        }
    }

    private float getCurrentRadarRange() {
        return RANGE_PRESETS[rangePresetIndex];
    }

    /**
     * 選択中ターゲットの識別を変更し、サーバーへパケット送信
     */
    private void changeSelectedTargetAffiliation(TargetAffiliation affiliation) {
        if (this.selectedTargetUuid == null || affiliation == null) return;
        if (this.menu.getBlockEntity() instanceof Uyq21BlockEntity uyqBE) {
            AbstractFcsCoreBlockEntity core = uyqBE.getLinkedFcsCore();
            BlockPos corePos = core != null ? core.getBlockPos() : uyqBE.getLinkedFcsCorePos();
            if (corePos != null) {
                // クライアント側即時反映プレビュー
                TrackedTarget target = uyqBE.getTrackedTargets().get(this.selectedTargetUuid);
                if (target != null) {
                    target.setAffiliation(affiliation);
                }
                // サーバーへ更新パケット送信
                PacketHandler.sendToServer(new ServerboundSetTargetAffiliationPacket(corePos, this.selectedTargetUuid, affiliation));
            }
        }
    }

    private void cycleSelectedTargetAffiliation() {
        if (this.selectedTargetUuid == null) return;
        if (this.menu.getBlockEntity() instanceof Uyq21BlockEntity uyqBE) {
            TrackedTarget target = uyqBE.getTrackedTargets().get(this.selectedTargetUuid);
            if (target != null) {
                changeSelectedTargetAffiliation(target.getAffiliation().next());
            }
        }
    }

    private void refreshWeaponsList() {
        connectedWeapons.clear();
        if (this.menu.getBlockEntity() instanceof Uyq21BlockEntity uyqBE) {
            AbstractFcsCoreBlockEntity core = uyqBE.getLinkedFcsCore();
            if (core != null && this.minecraft != null && this.minecraft.level != null) {
                Level level = this.minecraft.level;
                Map<UUID, BlockPos> positions = core.getNodePositions();
                for (UUID uuid : core.getConnectedNodeUuids()) {
                    BlockPos pos = positions.get(uuid);
                    if (pos == null || !level.isLoaded(pos)) continue;
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof IFcsControllableWeapon) {
                        String name = level.getBlockState(pos).getBlock().getName().getString();
                        String category = categorizeWeapon(be);
                        connectedWeapons.add(new WeaponDisplayEntry(uuid, name, pos, category, be));
                    }
                }
            }
        }
        if (selectedWeaponIndex >= connectedWeapons.size()) {
            selectedWeaponIndex = Math.max(0, connectedWeapons.size() - 1);
        }
    }

    private String categorizeWeapon(BlockEntity be) {
        if (be instanceof AbstractSingleGunBlockEntity) return "GUN";
        if (be instanceof AbstractMissileLauncherBlockEntity) return "GMLS";
        if (be instanceof ARWCIWSBlockEntity) return "CIWS";
        if (be instanceof VlsBlockEntity) return "VLS";
        return "WPN";
    }

    private float getMaxWeaponRange(BlockEntity be) {
        if (be instanceof AbstractSingleGunBlockEntity) return 1500.0f;
        if (be instanceof ARWCIWSBlockEntity) return 350.0f;
        if (be instanceof AbstractMissileLauncherBlockEntity || be instanceof VlsBlockEntity) return 3000.0f;
        return 1000.0f;
    }

    private String getTrackNumber(UUID uuid) {
        if (uuid == null) return "TN---";
        int num = Math.abs(uuid.hashCode()) % 1000;
        return String.format("TN%03d", num);
    }

    private String truncateString(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }

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
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
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

        // 2. 中心の表示部分（常に4:3、純黒背景）
        renderCenterDisplay(guiGraphics, partialTick);
    }

    private void renderCenterDisplay(GuiGraphics guiGraphics, float partialTick) {
        if (displayW <= 0 || displayH <= 0) return;

        // 1. 中心の表示部分全体（4:3、純黒背景 0xFF000000）
        guiGraphics.fill(displayX, displayY, displayX + displayW, displayY + displayH, 0xFF000000);

        // 外枠ベゼル
        guiGraphics.renderOutline(displayX, displayY, displayW, displayH, 0xFF1A1A1A);
        if (displayW > 4 && displayH > 4) {
            guiGraphics.renderOutline(displayX + 1, displayY + 1, displayW - 2, displayH - 2, 0xFF2A2A2A);
        }

        // 2. 左右ボーダーと1:1中央領域の仕切り線
        int borderLineCol = 0xFF223344;
        guiGraphics.vLine(radarX, displayY, displayY + displayH, borderLineCol);
        guiGraphics.vLine(radarX + radarSize, displayY, displayY + displayH, borderLineCol);

        // 3. ページ別の中央表示（1:1領域）
        guiGraphics.enableScissor(radarX, radarY, radarX + radarSize, radarY + radarSize);
        if (currentPage == PageMode.RADAR) {
            renderRadarPage(guiGraphics, partialTick);
            updateRightTelemetryLabels();
        } else {
            updateWeaponsPageButtonsAndLabels();
            renderWeaponsPage(guiGraphics);
        }
        guiGraphics.disableScissor();

        // 4. 左右ボーダー内にボタン機能名を2行・縮小フォントで描画
        renderFunctionLabels(guiGraphics);
    }

    /**
     * レーダー画面時：選択中ターゲットの情報を右側ファンクションキーラベルに反映
     */
    private void updateRightTelemetryLabels() {
        if (this.menu.getBlockEntity() instanceof Uyq21BlockEntity uyqBE) {
            TrackedTarget selected = (this.selectedTargetUuid != null)
                    ? uyqBE.getTrackedTargets().get(this.selectedTargetUuid)
                    : null;

            if (selected != null) {
                BlockPos centerPos = uyqBE.getBlockPos();
                Vec3 pos = selected.getLastKnownPos();
                double dx = pos.x - (centerPos.getX() + 0.5);
                double dy = pos.y - centerPos.getY();
                double dz = pos.z - (centerPos.getZ() + 0.5);
                double dist = Math.sqrt(dx * dx + dz * dz);
                double speed = selected.getLastKnownVelocity() != null ? selected.getLastKnownVelocity().length() * 20.0 : 0.0;

                rightFunctionLabels[1] = "TGT\nLOCK";
                rightFunctionLabels[2] = "TYPE\n" + truncateString(selected.getEntityTypeName(), 7);
                rightFunctionLabels[3] = String.format("DIST\n%.0fm", dist);
                rightFunctionLabels[4] = String.format("ALT\n%+.0fm", dy);
                rightFunctionLabels[5] = String.format("SPD\n%.0fm/s", speed);
                rightFunctionLabels[6] = "IFF\n" + selected.getAffiliation().getShortLabel();
            } else {
                rightFunctionLabels[1] = "TGT\nNONE";
                rightFunctionLabels[2] = "TYPE\n--";
                rightFunctionLabels[3] = "DIST\n--";
                rightFunctionLabels[4] = "ALT\n--";
                rightFunctionLabels[5] = "SPD\n--";
                rightFunctionLabels[6] = "IFF\n--";
            }
        }
    }

    /**
     * レーダー画面（PPI）の描画
     */
    private void renderRadarPage(GuiGraphics guiGraphics, float partialTick) {
        drawTacticalGrid(guiGraphics, radarX, radarY, radarSize, radarSize);

        int radarCenterX = radarX + radarSize / 2;
        int radarCenterY = radarY + radarSize / 2;
        int maxRadius = radarSize / 2 - 4;
        if (maxRadius > 10) {
            drawRadarReticle(guiGraphics, radarCenterX, radarCenterY, maxRadius);
            drawRadarTargets(guiGraphics, radarCenterX, radarCenterY, maxRadius, partialTick);
        }
    }

    /**
     * 兵装選択・管制ページの描画（中央部: MCDU風2段組テキスト情報設計）
     */
    private void renderWeaponsPage(GuiGraphics guiGraphics) {
        // 1. 背景の薄い戦術グリッド
        drawTacticalGrid(guiGraphics, radarX, radarY, radarSize, radarSize);

        int pad = 8;
        int curX = radarX + pad;
        int curY = radarY + pad;
        int curW = radarSize - pad * 2;
        int lineHeight = 11;

        // ヘッダー
        String header = "------------------ WEAPON CONTROL ------------------";
        guiGraphics.drawCenteredString(this.font, header, radarX + radarSize / 2, curY, 0xFF00FF88);
        curY += lineHeight + 3;

        WeaponDisplayEntry wpn = (selectedWeaponIndex >= 0 && selectedWeaponIndex < connectedWeapons.size())
                ? connectedWeapons.get(selectedWeaponIndex) : null;

        if (wpn == null) {
            guiGraphics.drawCenteredString(this.font, "NO WEAPONS LINKED TO FCS", radarX + radarSize / 2, radarY + radarSize / 2, 0xFF88AACC);
            return;
        }

        // SELECTED: [MK45 MOD4 5-INCH GUN]      LINK: FCS-CORE
        String selPrefix = "SELECTED: ";
        String selName = "[" + wpn.name() + "]";
        String linkStr = "LINK: FCS-CORE";
        guiGraphics.drawString(this.font, selPrefix, curX, curY, 0xFF00FFFF, false);
        guiGraphics.drawString(this.font, selName, curX + this.font.width(selPrefix), curY, 0xFFFFFFFF, false);
        guiGraphics.drawString(this.font, linkStr, curX + curW - this.font.width(linkStr), curY, 0xFF00FF66, false);
        curY += lineHeight + 4;

        // 割り当て目標の取得
        UUID assignedUuid = weaponAssignedTargets.get(wpn.uuid());
        TrackedTarget assignedTgt = null;
        if (assignedUuid != null && this.menu.getBlockEntity() instanceof Uyq21BlockEntity uyqBE) {
            assignedTgt = uyqBE.getTrackedTargets().get(assignedUuid);
        }

        if (assignedTgt != null && assignedTgt.getLastKnownPos() != null) {
            Vec3 tPos = assignedTgt.getLastKnownPos();
            BlockPos wPos = wpn.pos();
            double dx = tPos.x - (wPos.getX() + 0.5);
            double dy = tPos.y - (wPos.getY() + 0.5);
            double dz = tPos.z - (wPos.getZ() + 0.5);
            double distH = Math.sqrt(dx * dx + dz * dz);
            double dist3D = Math.sqrt(dx * dx + dy * dy + dz * dz);

            double azimDeg = Math.toDegrees(Math.atan2(dx, -dz));
            if (azimDeg < 0) azimDeg += 360.0;
            double elevDeg = Math.toDegrees(Math.atan2(dy, distH));

            double closingSpd = 0.0;
            if (assignedTgt.getLastKnownVelocity() != null) {
                Vec3 vel = assignedTgt.getLastKnownVelocity().scale(20.0);
                double dot = vel.x * dx + vel.y * dy + vel.z * dz;
                closingSpd = -(dot / (dist3D > 0.001 ? dist3D : 1.0));
            }

            double muzzleVel = 400.0;
            if (wpn.blockEntity() instanceof AbstractSingleGunBlockEntity gun) {
                muzzleVel = gun.getMuzzleVelocity() * 20.0;
            }
            double effectiveSpd = muzzleVel + closingSpd;
            double timeToGo = effectiveSpd > 1.0 ? dist3D / effectiveSpd : 99.0;

            float maxRange = getMaxWeaponRange(wpn.blockEntity());
            boolean inRange = dist3D <= maxRange;
            int envelopePct = (int) Math.round((dist3D / maxRange) * 100.0);

            // TARGET TRACK : #042 [AIR - HOSTILE]
            String tn = getTrackNumber(assignedUuid);
            String domain = dy > 10.0 ? "AIR" : (dy < -10.0 ? "SUB" : "SURFACE");
            String affil = assignedTgt.getAffiliation().name();
            guiGraphics.drawString(this.font, "TARGET TRACK : ", curX, curY, 0xFF00FFFF, false);
            guiGraphics.drawString(this.font, "#" + tn.replace("TN", "") + " ", curX + this.font.width("TARGET TRACK : "), curY, 0xFFFFFFFF, false);
            int affilCol = assignedTgt.getAffiliation().getColor();
            guiGraphics.drawString(this.font, "[" + domain + " - " + affil + "]", curX + this.font.width("TARGET TRACK : #" + tn.replace("TN", "") + " "), curY, affilCol, false);
            curY += lineHeight;

            // POSITION     : X:+245  Y:+68  Z:-812
            String posStr = String.format("POSITION     : X:%+d  Y:%+d  Z:%+d", (int) Math.round(tPos.x), (int) Math.round(tPos.y), (int) Math.round(tPos.z));
            guiGraphics.drawString(this.font, posStr, curX, curY, 0xFFCCDDEE, false);
            curY += lineHeight;

            // CLOSING SPD  : 18.4 m/s (MACH 0.05)
            double mach = closingSpd / 340.0;
            String spdStr = String.format("CLOSING SPD  : %.1f m/s (MACH %.2f)", closingSpd, Math.max(0.0, mach));
            guiGraphics.drawString(this.font, spdStr, curX, curY, 0xFFCCDDEE, false);
            curY += lineHeight;

            // TIME TO GO   : 00:08 SEC
            String ttgStr = String.format("TIME TO GO   : %02d:%02d SEC", (int) (timeToGo / 60), (int) (timeToGo % 60));
            guiGraphics.drawString(this.font, ttgStr, curX, curY, inRange ? 0xFF00FF66 : 0xFFFFAA00, false);
            curY += lineHeight + 4;

            // SOLUTION     : ELEV +14.2° / AZIM 048.5°
            String solStr = String.format("SOLUTION     : ELEV %+05.1f° / AZIM %05.1f°", elevDeg, azimDeg);
            guiGraphics.drawString(this.font, solStr, curX, curY, inRange ? 0xFF00FF66 : 0xFFFFAA00, false);
            curY += lineHeight;

            // ENGAGE ENVELOPE: IN RANGE [==========|    ] 72%
            int barLength = 14;
            int filled = Math.max(0, Math.min(barLength, (int) Math.round(((double) envelopePct / 100.0) * barLength)));
            StringBuilder bar = new StringBuilder("[");
            for (int b = 0; b < barLength; b++) {
                if (b < filled) bar.append("=");
                else if (b == filled) bar.append("|");
                else bar.append(" ");
            }
            bar.append("]");
            String envStatus = inRange ? "IN RANGE" : "OUT OF RNG";
            String envStr = String.format("ENGAGE ENVELOPE: %s %s %d%%", envStatus, bar.toString(), envelopePct);
            guiGraphics.drawString(this.font, envStr, curX, curY, inRange ? 0xFF00FF66 : 0xFFFF3333, false);
            curY += lineHeight + 4;

            // 区切り線
            guiGraphics.drawCenteredString(this.font, "----------------------------------------------------", radarX + radarSize / 2, curY, 0xFF888888);
            curY += lineHeight;

            // システムメッセージ
            boolean hold = weaponHoldFire.getOrDefault(wpn.uuid(), false);
            if (hold) {
                guiGraphics.drawString(this.font, "SYSTEM MSG: HOLD FIRE ACTIVE. WEAPON INHIBITED.", curX, curY, 0xFFFF3333, false);
            } else if (inRange) {
                guiGraphics.drawString(this.font, "SYSTEM MSG: FIRING SOLUTION CALCULATED. READY.", curX, curY, 0xFF00FF66, false);
            } else {
                guiGraphics.drawString(this.font, "SYSTEM MSG: TARGET BEYOND ENGAGEMENT ENVELOPE.", curX, curY, 0xFFFFAA00, false);
            }
        } else {
            // 未割り当て時
            guiGraphics.drawString(this.font, "TARGET TRACK : [NO TARGET ASSIGNED]", curX, curY, 0xFFFFAA00, false);
            curY += lineHeight;

            if (this.selectedTargetUuid != null) {
                String hookTn = getTrackNumber(this.selectedTargetUuid);
                guiGraphics.drawString(this.font, "HOOK STATUS  : HOOKED (#" + hookTn.replace("TN", "") + ") -> PRESS [ASSIGN]", curX, curY, 0xFF00FF66, false);
            } else {
                guiGraphics.drawString(this.font, "HOOK STATUS  : NO RADAR TARGET HOOKED", curX, curY, 0xFF888888, false);
            }
            curY += lineHeight;

            guiGraphics.drawString(this.font, "POSITION     : ---", curX, curY, 0xFF666666, false);
            curY += lineHeight;
            guiGraphics.drawString(this.font, "CLOSING SPD  : ---", curX, curY, 0xFF666666, false);
            curY += lineHeight;
            guiGraphics.drawString(this.font, "TIME TO GO   : ---", curX, curY, 0xFF666666, false);
            curY += lineHeight + 4;

            guiGraphics.drawString(this.font, "SOLUTION     : STANDBY / NO TARGET", curX, curY, 0xFF888888, false);
            curY += lineHeight;
            guiGraphics.drawString(this.font, "ENGAGE ENVELOPE: [              ] 0%", curX, curY, 0xFF666666, false);
            curY += lineHeight + 4;

            guiGraphics.drawCenteredString(this.font, "----------------------------------------------------", radarX + radarSize / 2, curY, 0xFF888888);
            curY += lineHeight;

            guiGraphics.drawString(this.font, "SYSTEM MSG: SELECT TARGET ON RADAR & ASSIGN TO WPN.", curX, curY, 0xFFCCDDEE, false);
        }
    }

    private record AmmoInfo(int totalCount, String nextAmmo) {}

    private AmmoInfo getWeaponAmmoInfo(BlockEntity be) {
        if (be == null) return new AmmoInfo(0, "NONE");
        IItemHandler handler = null;
        if (be instanceof Oto127mmBlockEntity oto) {
            handler = oto.getInventory();
        } else {
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

    /**
     * 薄い戦術グリッド線の描画
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
     * レーダーHUDレティクルの描画
     */
    private void drawRadarReticle(GuiGraphics guiGraphics, int cx, int cy, int maxRadius) {
        int reticleColor = 0x2400FF88;
        int axisColor = 0x3300FF88;

        // 十字軸線
        guiGraphics.hLine(cx - maxRadius, cx + maxRadius, cy, axisColor);
        guiGraphics.vLine(cx, cy - maxRadius, cy + maxRadius, axisColor);

        // 同心円
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
     * レーダー目標の描画
     */
    private void drawRadarTargets(GuiGraphics guiGraphics, int cx, int cy, int maxRadius, float partialTick) {
        this.guiRadarTargets.clear();

        if (this.menu.getBlockEntity() instanceof Uyq21BlockEntity uyqBE) {
            Map<UUID, TrackedTarget> targets = uyqBE.getTrackedTargets();
            if (targets.isEmpty()) return;

            BlockPos centerPos = uyqBE.getBlockPos();
            double originX = centerPos.getX() + 0.5;
            double originY = centerPos.getY();
            double originZ = centerPos.getZ() + 0.5;

            float currentRange = getCurrentRadarRange();

            long currentClientGameTime = Minecraft.getInstance().level != null
                    ? Minecraft.getInstance().level.getGameTime()
                    : 0L;

            // センサー最大レンジ円の表示
            AbstractFcsCoreBlockEntity core = uyqBE.getLinkedFcsCore();
            if (core != null) {
                float maxSensorRange = core.getMaxActiveDetectionRange();
                if (maxSensorRange > 0.0f && maxSensorRange < currentRange) {
                    int limitRadiusPx = (int) Math.round((maxSensorRange / currentRange) * maxRadius);
                    drawCircle(guiGraphics, cx, cy, limitRadiusPx, 0x22FFAA00);
                }
            }

            for (TrackedTarget target : targets.values()) {
                Vec3 basePos = target.getLastKnownPos();
                if (basePos == null) continue;

                Vec3 vel = target.getLastKnownVelocity();

                // デッドレコニング（予測補間）
                double interpolatedX = basePos.x;
                double interpolatedZ = basePos.z;

                if (vel != null && currentClientGameTime > 0L) {
                    long elapsedTicks = Math.max(0, currentClientGameTime - target.getLastSeenGameTime());
                    if (elapsedTicks < 40) {
                        double totalElapsed = (double) elapsedTicks + partialTick;
                        interpolatedX += vel.x * totalElapsed;
                        interpolatedZ += vel.z * totalElapsed;
                    }
                }

                // ワールド座標差分
                double dX = interpolatedX - originX;
                double dZ = interpolatedZ - originZ;

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

                // クリック判定用に登録
                this.guiRadarTargets.add(new GuiRadarTarget(target.getEntityId(), px, py, target));

                // 識別カラー
                TargetAffiliation affiliation = target.getAffiliation();
                int iffColor = affiliation.getColor();
                boolean isSelected = target.getEntityId().equals(this.selectedTargetUuid);

                // 1. ベクトルライン
                if (vel != null) {
                    double vxWorld = vel.x * 40.0;
                    double vzWorld = vel.z * 40.0;
                    double vxPix = (vxWorld / currentRange) * maxRadius;
                    double vzPix = (vzWorld / currentRange) * maxRadius;

                    if (vxPix * vxPix + vzPix * vzPix >= 1.0) {
                        int endX = (int) Math.round(px + vxPix);
                        int endY = (int) Math.round(py + vzPix);
                        int vectorColor = (iffColor & 0x00FFFFFF) | 0x99000000;
                        drawLine(guiGraphics, px, py, endX, endY, vectorColor);
                    }
                }

                // 2. 高度別アイコン
                double relY = basePos.y - originY;
                drawAltitudeSymbol(guiGraphics, px, py, relY, iffColor, isSelected);

                // 3. 選択ハイライト
                if (isSelected) {
                    drawSelectionBracket(guiGraphics, px, py, 0xFFFFAA00);
                }
            }

            // レンジ情報表示
            String rangeText = String.format("RNG: %.0fm", currentRange);
            guiGraphics.drawString(this.font, rangeText, radarX + 4, radarY + 4, 0xFF00FF88, false);

            if (selectedTargetUuid != null) {
                guiGraphics.drawString(this.font, "TRK: HOOKED", radarX + 4, radarY + 14, 0xFFFFAA00, false);
            }
        }
    }

    /**
     * 相対高度に応じた戦術アイコンの描画
     */
    private void drawAltitudeSymbol(GuiGraphics guiGraphics, int px, int py, double relY, int color, boolean isSelected) {
        if (relY > 10.0) {
            drawLine(guiGraphics, px - 3, py + 2, px, py - 3, color);
            drawLine(guiGraphics, px, py - 3, px + 3, py + 2, color);
            guiGraphics.fill(px, py, px + 1, py + 1, 0xFFFFFFFF);
        } else if (relY < -10.0) {
            drawLine(guiGraphics, px - 3, py - 2, px, py + 3, color);
            drawLine(guiGraphics, px, py + 3, px + 3, py - 2, color);
            guiGraphics.fill(px, py, px + 1, py + 1, 0xFFFFFFFF);
        } else {
            guiGraphics.renderOutline(px - 2, py - 2, 5, 5, color);
            guiGraphics.fill(px, py, px + 1, py + 1, 0xFFFFFFFF);
        }
    }

    /**
     * 選択ターゲット用ブラケット枠 [ ] の描画
     */
    private void drawSelectionBracket(GuiGraphics guiGraphics, int px, int py, int color) {
        int r = 5;
        guiGraphics.hLine(px - r, px - r + 2, py - r, color);
        guiGraphics.vLine(px - r, py - r, py - r + 2, color);
        guiGraphics.hLine(px + r - 2, px + r, py - r, color);
        guiGraphics.vLine(px + r, py - r, py - r + 2, color);
        guiGraphics.hLine(px - r, px - r + 2, py + r, color);
        guiGraphics.vLine(px - r, py + r - 2, py + r, color);
        guiGraphics.hLine(px + r - 2, px + r, py + r, color);
        guiGraphics.vLine(px + r, py + r - 2, py + r, color);
    }

    /**
     * 左右ボーダー内にボタン機能名を2行・縮小フォントで描画
     */
    private void renderFunctionLabels(GuiGraphics guiGraphics) {
        int leftBorderW = radarX - displayX;
        int rightBorderW = (displayX + displayW) - (radarX + radarSize);

        // 左側ボーダー
        if (leftBorderW > 0) {
            guiGraphics.enableScissor(displayX, displayY, radarX, displayY + displayH);
            for (int i = 0; i < BUTTON_COUNT_PER_SIDE; i++) {
                int btnY = buttonsStartY + i * (btnSize + btnSpacing);
                String label = leftFunctionLabels[i] != null ? leftFunctionLabels[i] : "TEMP";
                int color = getLeftButtonColor(i);
                drawTwoLineLabel(guiGraphics, label, displayX, btnY, leftBorderW, btnSize, color, true);
            }
            guiGraphics.disableScissor();
        }

        // 右側ボーダー
        if (rightBorderW > 0) {
            guiGraphics.enableScissor(radarX + radarSize, displayY, displayX + displayW, displayY + displayH);
            for (int i = 0; i < BUTTON_COUNT_PER_SIDE; i++) {
                int btnY = buttonsStartY + i * (btnSize + btnSpacing);
                String label = rightFunctionLabels[i] != null ? rightFunctionLabels[i] : "TEMP";
                int color = getRightButtonColor(i);
                drawTwoLineLabel(guiGraphics, label, radarX + radarSize, btnY, rightBorderW, btnSize, color, false);
            }
            guiGraphics.disableScissor();
        }
    }

    private int getLeftButtonColor(int i) {
        if (selectedSide == SelectedSide.LEFT && selectedIndex == i) {
            return 0xFFFFAA00;
        }
        if (currentPage == PageMode.WEAPONS) {
            // L1〜L4: 選択中の兵装スロットはシアンハイライト
            if (i < WEAPONS_PER_PAGE) {
                int wpnIdx = weaponPage * WEAPONS_PER_PAGE + i;
                if (wpnIdx < connectedWeapons.size()) {
                    return (wpnIdx == selectedWeaponIndex) ? 0xFF00FFFF : 0xFF00FF88;
                }
                return 0xFF666666;
            }
            if (i == 4) return 0xFF666666; // L5: 空き
            if (i == 5) return 0xFF888888; // L6: 区切り線
            if (i == 6) return (this.selectedTargetUuid != null) ? 0xFF00FF66 : 0xFF888888; // L7: ASSIGN
            if (i == 7) return 0xFFFFAA00; // L8: DESELECT
            if (i == 8) return 0xFF00FF88; // L9: PREV
            if (i == 9) return 0xFF00E5FF; // L10: RETURN TO RADAR
        }
        return 0xFF00FF88;
    }

    private int getRightButtonColor(int i) {
        if (selectedSide == SelectedSide.RIGHT && selectedIndex == i) {
            return 0xFFFFAA00;
        }
        if (currentPage == PageMode.WEAPONS) {
            WeaponDisplayEntry activeWpn = (selectedWeaponIndex >= 0 && selectedWeaponIndex < connectedWeapons.size())
                    ? connectedWeapons.get(selectedWeaponIndex) : null;
            UUID activeWpnUuid = activeWpn != null ? activeWpn.uuid() : null;

            if (i == 0) return 0xFF00FF66; // R1: STATUS
            if (i == 1) { // R2: TARGET
                if (activeWpnUuid != null && weaponAssignedTargets.containsKey(activeWpnUuid)) {
                    return 0xFFFF3333;
                }
                return 0xFF888888;
            }
            if (i == 2) return 0xFFCCDDEE; // R3: BRG/RNG
            if (i == 3) return 0xFF00FF66; // R4: SOL
            if (i == 4) return 0xFF00FF88; // R5: AMMO
            if (i == 5) return 0xFF888888; // R6: 区切り線
            if (i == 6) return 0xFFCCDDEE; // R7: SALVO
            if (i == 7) { // R8: HOLD FIRE
                boolean hold = activeWpnUuid != null && weaponHoldFire.getOrDefault(activeWpnUuid, false);
                return hold ? 0xFFFF3333 : 0xFF00FF88;
            }
            if (i == 8) return 0xFF00FF88; // R9: NEXT PAGE
            if (i == 9) { // R10: ENGAGE / FIRE
                // 諸元成立時に点滅
                boolean hold = activeWpnUuid != null && weaponHoldFire.getOrDefault(activeWpnUuid, false);
                boolean hasTarget = activeWpnUuid != null && weaponAssignedTargets.containsKey(activeWpnUuid);
                if (hasTarget && !hold) {
                    boolean blink = (Minecraft.getInstance().level != null && (Minecraft.getInstance().level.getGameTime() / 6) % 2 == 0);
                    return blink ? 0xFFFF3333 : 0xFFFFAA00;
                }
                return 0xFF666666;
            }
        }
        return 0xFF00FF88;
    }

    /**
     * ボタン横の領域に2行対応で縮小フォントを用いてラベルを描画
     */
    private void drawTwoLineLabel(GuiGraphics guiGraphics, String label, int boxX, int boxY, int boxW, int boxH, int color, boolean isLeft) {
        if (label == null || label.isEmpty()) return;

        String[] lines;
        if (label.contains("\n")) {
            lines = label.split("\n", 2);
        } else if (label.contains(": ") && label.length() > 8) {
            lines = label.split(": ", 2);
        } else {
            lines = new String[] { label };
        }

        float scale = 0.70f;
        float scaledLineHeight = (this.font.lineHeight - 1) * scale;
        float lineSpacing = 1.0f;
        float totalH = lines.length == 1 ? scaledLineHeight : (scaledLineHeight * 2 + lineSpacing);

        float startY = boxY + (boxH - totalH) / 2.0f;

        for (int l = 0; l < lines.length; l++) {
            String line = lines[l].trim();
            float lineW = this.font.width(line) * scale;
            float startX;
            if (currentPage == PageMode.RADAR) {
                // レーダー画面時はボーダー枠の中央揃え
                startX = boxX + (boxW - lineW) / 2.0f;
            } else if (isLeft) {
                // 兵装画面（左側）は左ボタンのすぐ横（左寄せ）
                startX = boxX + 4.0f;
            } else {
                // 兵装画面（右側）は右ボタンのすぐ横（右寄せ）
                startX = (boxX + boxW) - lineW - 4.0f;
            }

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(startX, startY + l * (scaledLineHeight + lineSpacing), 0.0f);
            guiGraphics.pose().scale(scale, scale, 1.0f);
            guiGraphics.drawString(this.font, line, 0, 0, color, false);
            guiGraphics.pose().popPose();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // 左クリック
            if (currentPage == PageMode.RADAR) {
                // レーダー画面内クリックで目標選択
                if (mouseX >= radarX && mouseX <= radarX + radarSize &&
                    mouseY >= radarY && mouseY <= radarY + radarSize) {

                    GuiRadarTarget clickedTarget = null;
                    double closestDistSq = 64.0; // 許容半径8ピクセル

                    for (GuiRadarTarget gt : this.guiRadarTargets) {
                        double dx = mouseX - gt.x();
                        double dy = mouseY - gt.y();
                        double distSq = dx * dx + dy * dy;
                        if (distSq <= closestDistSq) {
                            closestDistSq = distSq;
                            clickedTarget = gt;
                        }
                    }

                    if (clickedTarget != null) {
                        this.selectedTargetUuid = clickedTarget.uuid();
                        return true;
                    } else {
                        this.selectedTargetUuid = null;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
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
