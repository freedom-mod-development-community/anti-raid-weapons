package xyz.fmdc.arw.client.gui;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import xyz.fmdc.arw.common.blockentity.AbstractARWBlockEntity;
import xyz.fmdc.arw.registry.ModMenuTypes;

public class EmptyMenu extends AbstractContainerMenu {

    private final AbstractARWBlockEntity blockEntity;

    // クライアント側用コンストラクタ（NetworkHooksから呼ばれる）
    public EmptyMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, (AbstractARWBlockEntity) playerInventory.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    // サーバー側用コンストラクタ
    public EmptyMenu(int containerId, Inventory playerInventory, AbstractARWBlockEntity blockEntity) {
        super(ModMenuTypes.UYQ21_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        // ※ addSlot(...) を一切呼ばない（インベントリなし）
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        return ItemStack.EMPTY; // スロットがないので空を返す
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return this.blockEntity != null && !this.blockEntity.isRemoved();
    }
}
