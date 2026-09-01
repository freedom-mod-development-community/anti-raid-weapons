package xyz.fmdc.arw.common.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 各兵器ブロック用メニュー（コンテナ）の基底抽象クラス.
 * 兵器側のスロット、プレイヤーインベントリ、クイック移動（Shiftクリック）、
 * 有効性判定（stillValid）などの共通処理を提供します。
 *
 * @param <T> 兵器のBlockEntity型
 */
public abstract class AbstractWeaponMenu<T extends BlockEntity> extends AbstractContainerMenu {

    public static final int DEFAULT_WEAPON_SLOT_COUNT = 9;
    public static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    public static final int PLAYER_INVENTORY_COL_COUNT = 9;
    public static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_ROW_COUNT * PLAYER_INVENTORY_COL_COUNT; // 27
    public static final int PLAYER_HOTBAR_SLOT_COUNT = 9;
    public static final int TOTAL_PLAYER_SLOT_COUNT = PLAYER_INVENTORY_SLOT_COUNT + PLAYER_HOTBAR_SLOT_COUNT; // 36

    protected final T blockEntity;
    protected final ContainerLevelAccess levelAccess;
    protected final int weaponSlotCount;

    protected AbstractWeaponMenu(@Nullable MenuType<?> menuType, int containerId, Inventory playerInventory, @Nullable T blockEntity) {
        this(menuType, containerId, playerInventory, blockEntity, DEFAULT_WEAPON_SLOT_COUNT);
    }

    protected AbstractWeaponMenu(@Nullable MenuType<?> menuType, int containerId, Inventory playerInventory, @Nullable T blockEntity, int weaponSlotCount) {
        super(menuType, containerId);
        this.blockEntity = blockEntity;
        this.weaponSlotCount = weaponSlotCount;

        if (blockEntity != null && blockEntity.getLevel() != null) {
            this.levelAccess = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        } else {
            this.levelAccess = ContainerLevelAccess.NULL;
        }

        addWeaponSlots(playerInventory);
        addPlayerInventorySlots(playerInventory);
        addPlayerHotbarSlots(playerInventory);
    }

    /**
     * FriendlyByteBuf から BlockPos を読み取り、該当する BlockEntity を取得するヘルパーメソッド（クライアント側コンストラクタ用）
     */
    @SuppressWarnings("unchecked")
    protected static <BE extends BlockEntity> BE getBlockEntityFromBuf(Inventory playerInventory, FriendlyByteBuf extraData) {
        BlockPos pos = extraData.readBlockPos();
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return (BE) be;
    }

    /**
     * 兵器側のスロット（デフォルトでは3x3、中央配置の9スロット）を登録します。
     * 必要に応じて子クラスでオーバーライド可能です。
     */
    protected void addWeaponSlots(Inventory playerInventory) {
        if (this.blockEntity != null) {
            this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(this::addWeaponItemHandlerSlots);
        }
    }

    /**
     * IItemHandler からスロットを配置する処理（デフォルトはディスペンサー風の 3x3 配置）
     */
    protected void addWeaponItemHandlerSlots(IItemHandler handler) {
        int rows = 3;
        int cols = 3;
        int startX = 62;
        int startY = 17;
        int slotIndex = 0;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if (slotIndex < this.weaponSlotCount && slotIndex < handler.getSlots()) {
                    this.addSlot(new SlotItemHandler(handler, slotIndex, startX + col * 18, startY + row * 18));
                    slotIndex++;
                }
            }
        }
    }

    /**
     * プレイヤーのメインインベントリスロット（3行×9列 = 27スロット）を登録します。
     */
    protected void addPlayerInventorySlots(Inventory playerInventory) {
        int startX = 8;
        int startY = 84;
        for (int row = 0; row < PLAYER_INVENTORY_ROW_COUNT; row++) {
            for (int col = 0; col < PLAYER_INVENTORY_COL_COUNT; col++) {
                this.addSlot(new Slot(playerInventory, col + row * PLAYER_INVENTORY_COL_COUNT + 9, startX + col * 18, startY + row * 18));
            }
        }
    }

    /**
     * プレイヤーのホットバースロット（9スロット）を登録します。
     */
    protected void addPlayerHotbarSlots(Inventory playerInventory) {
        int startX = 8;
        int startY = 142;
        for (int col = 0; col < PLAYER_HOTBAR_SLOT_COUNT; col++) {
            this.addSlot(new Slot(playerInventory, col, startX + col * 18, startY));
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot sourceSlot = this.slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyStack = sourceStack.copy();

        int playerInventoryStart = this.weaponSlotCount;
        int playerInventoryEnd = this.weaponSlotCount + TOTAL_PLAYER_SLOT_COUNT;

        // 兵器インベントリからプレイヤーインベントリへ
        if (index < this.weaponSlotCount) {
            if (!this.moveItemStackTo(sourceStack, playerInventoryStart, playerInventoryEnd, true)) {
                return ItemStack.EMPTY;
            }
        }
        // プレイヤーインベントリから兵器インベントリへ
        else {
            if (!this.moveItemStackTo(sourceStack, 0, this.weaponSlotCount, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (sourceStack.isEmpty()) {
            sourceSlot.setByPlayer(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }

        sourceSlot.onTake(player, sourceStack);
        return copyStack;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        if (this.blockEntity == null || this.blockEntity.isRemoved()) {
            return false;
        }
        Block validBlock = getValidBlock();
        if (validBlock == null) {
            return false;
        }
        return stillValid(this.levelAccess, player, validBlock);
    }

    /**
     * メニューを開き続けるために有効なブロックを取得します。
     * デフォルトでは BlockEntity の現在のブロックを返します。
     */
    @Nullable
    protected Block getValidBlock() {
        return this.blockEntity != null ? this.blockEntity.getBlockState().getBlock() : null;
    }

    @Nullable
    public T getBlockEntity() {
        return this.blockEntity;
    }

    public int getWeaponSlotCount() {
        return this.weaponSlotCount;
    }

    public ContainerLevelAccess getLevelAccess() {
        return this.levelAccess;
    }
}
