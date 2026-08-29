package xyz.fmdc.arw;

import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import xyz.fmdc.arw.network.PacketHandler;
import xyz.fmdc.arw.registry.ModBlockEntities;
import xyz.fmdc.arw.registry.ModBlocks;
import xyz.fmdc.arw.registry.ModCreativeTabs;
import xyz.fmdc.arw.registry.ModItems;

@Mod(AntiRaidWeapons.MOD_ID)
public class AntiRaidWeapons {
    public static final String MOD_ID = "arw";
    public static final Logger LOGGER = LogUtils.getLogger();

    // コンストラクタの引数で FMLJavaModLoadingContext を受け取る
    public AntiRaidWeapons(FMLJavaModLoadingContext context) {
        // インジェクションされた context から IEventBus を取得
        IEventBus modEventBus = context.getModEventBus();

        // 各レジストリの登録
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus); // ← これが呼ばれているか＆順番が正しいか確認
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // パケット登録はマルチスレッド実行時の競合を防ぐため enqueueWork 内で行う
        event.enqueueWork(PacketHandler::register);
    }
}