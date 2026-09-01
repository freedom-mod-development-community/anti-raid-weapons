package xyz.fmdc.arw.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public class TestConsoleClientHandler {
    public static void openScreen(BlockPos pos) {
        Minecraft.getInstance().setScreen(new TestConsoleScreen(pos));
    }
}
