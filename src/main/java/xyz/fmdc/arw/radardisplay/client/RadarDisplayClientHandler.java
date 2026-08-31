package xyz.fmdc.arw.radardisplay.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public class RadarDisplayClientHandler {
    public static void openScreen(BlockPos pos) {
        Minecraft.getInstance().setScreen(new RadarDisplayScreen(pos));
    }
}
