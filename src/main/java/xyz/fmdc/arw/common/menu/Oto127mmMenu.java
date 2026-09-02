package xyz.fmdc.arw.common.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import xyz.fmdc.arw.common.blockentity.weapon.singlegun.Oto127mmBlockEntity;
import xyz.fmdc.arw.registry.ModMenuTypes;

public class Oto127mmMenu extends AbstractWeaponMenu<Oto127mmBlockEntity> {

    // クライアント側コンストラクタ
    public Oto127mmMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, getBlockEntityFromBuf(playerInventory, extraData));
    }

    // 共通コンストラクタ
    public Oto127mmMenu(int containerId, Inventory playerInventory, BlockEntity blockEntity) {
        super(ModMenuTypes.OTO127MM_MENU.get(), containerId, playerInventory, (Oto127mmBlockEntity) blockEntity);
    }
}
