package xyz.fmdc.arw;

import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import xyz.fmdc.arw.network.ModPacketHandler;
import xyz.fmdc.arw.registry.ModBlockEntities;
import xyz.fmdc.arw.registry.ModBlocks;
import xyz.fmdc.arw.registry.ModCreativeTabs;
import xyz.fmdc.arw.registry.ModEntities;
import xyz.fmdc.arw.registry.ModItems;
import xyz.fmdc.arw.registry.ModSounds;

@Mod(AntiRaidWeapons.MOD_ID)
public class AntiRaidWeapons {
    public static final String MOD_ID = "arw";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AntiRaidWeapons(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        ModSounds.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        ModPacketHandler.register();
    }
}
