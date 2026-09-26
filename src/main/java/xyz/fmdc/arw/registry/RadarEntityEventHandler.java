package xyz.fmdc.arw.registry;

import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.fmdc.arw.AntiRaidWeapons;
import xyz.fmdc.arw.api.RadarTargetManager;

@Mod.EventBusSubscriber(modid = AntiRaidWeapons.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RadarEntityEventHandler {

    /**
     * エンティティがワールドに出現した時 (スポーン・チャンクロード時)
     */
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        // サーバー側の処理のみを対象とする
        if (!event.getLevel().isClientSide()) {
            Entity entity = event.getEntity();
            RadarTargetManager.INSTANCE.registerEntity(entity);
        }
    }

    /**
     * エンティティがワールドから消出された時 (死亡・デスポーン・チャンクアンロード時)
     */
    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        // サーバー側の処理のみを対象とする
        if (!event.getLevel().isClientSide()) {
            Entity entity = event.getEntity();
            RadarTargetManager.INSTANCE.unregisterEntity(entity);
        }
    }

    /**
     * サーバー停止時の完全解放
     */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        RadarTargetManager.INSTANCE.clear();
    }

    /**
     * ワールドアンロード時の解放
     */
    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (!event.getLevel().isClientSide()) {
            RadarTargetManager.INSTANCE.clear();
        }
    }
}
