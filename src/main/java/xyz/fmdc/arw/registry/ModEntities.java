package xyz.fmdc.arw.registry;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import xyz.fmdc.arw.AntiRaidWeapons;
import xyz.fmdc.arw.entity.NavalShellEntity;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, AntiRaidWeapons.MOD_ID);

    public static final RegistryObject<EntityType<NavalShellEntity>> NAVAL_SHELL =
            ENTITY_TYPES.register("naval_shell", () ->
                    EntityType.Builder.<NavalShellEntity>of(NavalShellEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(16)
                            .updateInterval(1)
                            .build("naval_shell")
            );

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
