package xyz.fmdc.arw.client.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import xyz.fmdc.arw.common.menu.Oto127mmMenu;

public class Oto127mmScreen extends AbstractWeaponScreen<Oto127mmMenu> {

    public Oto127mmScreen(Oto127mmMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }
}
