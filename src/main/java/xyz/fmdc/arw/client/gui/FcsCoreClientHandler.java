package xyz.fmdc.arw.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import xyz.fmdc.arw.client.gui.screen.FcsCoreScreen;

public class FcsCoreClientHandler {
    public static void openScreen(BlockPos pos) {
        Minecraft.getInstance().setScreen(new FcsCoreScreen(pos));
    }
}
