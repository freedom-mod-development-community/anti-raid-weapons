package xyz.fmdc.arw.registry;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import xyz.fmdc.arw.AntiRaidWeapons;
import xyz.fmdc.arw.emmi.EmmiBlockEntity;
import xyz.fmdc.arw.ops39.Ops39BlockEntity;
import xyz.fmdc.arw.spq9b.Spq9bBlockEntity;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, AntiRaidWeapons.MOD_ID);

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
