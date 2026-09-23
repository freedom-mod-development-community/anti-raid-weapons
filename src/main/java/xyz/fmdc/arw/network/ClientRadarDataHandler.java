package xyz.fmdc.arw.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import xyz.fmdc.arw.api.sensor.ITrackedTargetHolder;
import xyz.fmdc.arw.common.blockentity.sensor.SearchRadarBlockEntity;

import java.util.List;

public class ClientRadarDataHandler {
    public static void handleTargetSync(BlockPos pos, List<S2CSyncRadarTargetsPacket.TargetData> targets) {
        if (Minecraft.getInstance().level != null) {
            BlockEntity be = Minecraft.getInstance().level.getBlockEntity(pos);
            if (be instanceof ITrackedTargetHolder holder) {
                holder.updateClientTrackedTargets(targets);
            } else if (be instanceof SearchRadarBlockEntity radarBe) {
                // 既存のSearchRadarBlockEntityに対するフォールバック
                radarBe.updateClientTrackedTargets(targets);
            }
        }
    }
}
