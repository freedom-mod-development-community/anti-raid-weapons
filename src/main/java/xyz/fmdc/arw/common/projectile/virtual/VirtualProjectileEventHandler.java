package xyz.fmdc.arw.common.projectile.virtual;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.fmdc.arw.AntiRaidWeapons;

/**
 * サーバー側で VirtualProjectileManager を駆動し、停止時・アンロード時の安全処理をフックするイベントハンドラー。
 */
@Mod.EventBusSubscriber(modid = AntiRaidWeapons.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class VirtualProjectileEventHandler {

    /**
     * 毎Tickの末尾で全ディメンションの仮想飛翔体を一括更新
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            VirtualProjectileManager.getInstance().tick(event.getServer());
        }
    }

    /**
     * サーバー停止時の完全パージ処理
     */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        VirtualProjectileManager.getInstance().clearAndNotifyAll();
    }

    /**
     * ディメンションアンロード時の飛翔体解放
     */
    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (!event.getLevel().isClientSide() && event.getLevel() instanceof ServerLevel serverLevel) {
            VirtualProjectileManager.getInstance().onLevelUnload(serverLevel.dimension());
        }
    }
}
