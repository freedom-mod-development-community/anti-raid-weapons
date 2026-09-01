package xyz.fmdc.arw.registry;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import xyz.fmdc.arw.AntiRaidWeapons;
import xyz.fmdc.arw.common.menu.Mk13GmlsMenu;
import xyz.fmdc.arw.common.menu.Oto127mmMenu;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, AntiRaidWeapons.MOD_ID);

    public static final RegistryObject<MenuType<Oto127mmMenu>> OTO127MM_MENU =
            MENUS.register("oto127mm_menu", () -> IForgeMenuType.create(Oto127mmMenu::new));

    public static final RegistryObject<MenuType<Mk13GmlsMenu>> MK13_GMLS_MENU =
            MENUS.register("mk13_gmls_menu", () -> IForgeMenuType.create(Mk13GmlsMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
