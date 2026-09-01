package xyz.fmdc.arw.client.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import xyz.fmdc.arw.common.menu.Mk13GmlsMenu;

public class Mk13GmlsScreen extends AbstractWeaponScreen<Mk13GmlsMenu> {

    public Mk13GmlsScreen(Mk13GmlsMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }
}
