package xyz.fmdc.arw.client.gui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import xyz.fmdc.arw.client.gui.EmptyMenu;

public class Uyq21Screen extends AbstractContainerScreen<EmptyMenu> {

    public Uyq21Screen(EmptyMenu menu, Inventory playerInventory, Component title){
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {

    }
}
