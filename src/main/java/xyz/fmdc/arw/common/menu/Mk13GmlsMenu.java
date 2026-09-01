package xyz.fmdc.arw.common.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import xyz.fmdc.arw.common.blockentity.weapon.Mk13GmlsBlockEntity;
import xyz.fmdc.arw.registry.ModMenuTypes;

/**
 * Mk 13 GMLS 用のコンテナメニュー.
 */
public class Mk13GmlsMenu extends AbstractWeaponMenu<Mk13GmlsBlockEntity> {

    // クライアント側コンストラクタ
    public Mk13GmlsMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, getBlockEntityFromBuf(playerInventory, extraData));
    }

    // 共通コンストラクタ
    public Mk13GmlsMenu(int containerId, Inventory playerInventory, BlockEntity blockEntity) {
        super(ModMenuTypes.MK13_GMLS_MENU.get(), containerId, playerInventory, (Mk13GmlsBlockEntity) blockEntity);
    }
}
