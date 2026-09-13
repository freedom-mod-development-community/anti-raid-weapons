package xyz.fmdc.arw.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import xyz.fmdc.arw.common.blockentity.sensor.SearchRadarBlockEntity;

import java.util.List;

public class ClientRadarDataHandler {
    public static void handleTargetSync(BlockPos pos, List<S2CSyncRadarTargetsPacket.TargetData> targets) {
        if (Minecraft.getInstance().level != null) {
            BlockEntity be = Minecraft.getInstance().level.getBlockEntity(pos);
            if (be instanceof SearchRadarBlockEntity radarBe) {
                // 統一した trackedTargets を直接更新
                radarBe.updateClientTrackedTargets(targets);
            }
        }
    }
}