package xyz.fmdc.arw.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public class FcsCoreClientHandler {
    public static void openScreen(BlockPos pos) {
        Minecraft.getInstance().setScreen(new FcsCoreScreen(pos));
    }
}
