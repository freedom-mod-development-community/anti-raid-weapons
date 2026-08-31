package xyz.fmdc.arw.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import xyz.fmdc.arw.AntiRaidWeapons;
import xyz.fmdc.arw.common.entity.projectile.FiveInchShellEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import xyz.fmdc.arw.common.entity.projectile.NavalShellEntity;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, AntiRaidWeapons.MOD_ID);

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AntiRaidWeapons.MOD_ID);

    // 1. 5インチ砲弾エンティティの登録
    public static final RegistryObject<EntityType<FiveInchShellEntity>> FIVE_INCH_SHELL =
            ENTITY_TYPES.register("5inch_shell", () ->
                    EntityType.Builder.<FiveInchShellEntity>of(FiveInchShellEntity::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F) // ヒットボックスの大きさ（幅, 高さ）
                            .clientTrackingRange(8)  // 描画更新の追尾範囲
                            .updateInterval(1)       // パケット更新間隔（1tick毎で滑らかに飛翔）
                            .build("5inch_shell")
            );

    // 2. oto用の砲弾エンティティの登録
    public static final RegistryObject<EntityType<NavalShellEntity>> NAVAL_SHELL =
            ENTITY_TYPES.register("naval_shell", () ->
                    EntityType.Builder.<NavalShellEntity>of(NavalShellEntity::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F) // ヒットボックスの大きさ（幅, 高さ）
                            .clientTrackingRange(8)  // 描画更新の追尾範囲
                            .updateInterval(1)       // パケット更新間隔（1tick毎で滑らかに飛翔）
                            .build("naval_shell")
            );

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}
