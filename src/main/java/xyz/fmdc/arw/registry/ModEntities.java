package xyz.fmdc.arw.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import xyz.fmdc.arw.AntiRaidWeapons;
import xyz.fmdc.arw.common.entity.missile.Rim66M2;
import xyz.fmdc.arw.common.entity.projectile.ClientDummyProjectileEntity;
import xyz.fmdc.arw.common.entity.projectile.FiveInchShellEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, AntiRaidWeapons.MOD_ID);

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AntiRaidWeapons.MOD_ID);

    // 1. 5インチ砲弾エンティティの登録（旧セーブデータ互換プレースホルダー）
    @Deprecated(forRemoval = true)
    public static final RegistryObject<EntityType<FiveInchShellEntity>> FIVE_INCH_SHELL =
            ENTITY_TYPES.register("5inch_shell", () ->
                    EntityType.Builder.<FiveInchShellEntity>of(FiveInchShellEntity::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(8)
                            .updateInterval(1)
                            .build("5inch_shell")
            );

    // 2. RIM-66M-2 (SM-2 Block III) ミサイルエンティティの登録（旧セーブデータ互換プレースホルダー）
    @Deprecated(forRemoval = true)
    public static final RegistryObject<EntityType<Rim66M2>> RIM_66M2 =
            ENTITY_TYPES.register("rim_66m2", () ->
                    EntityType.Builder.<Rim66M2>of(Rim66M2::new, MobCategory.MISC)
                            .sized(0.34F, 4.72F)
                            .clientTrackingRange(32)
                            .updateInterval(1)
                            .build("rim_66m2")
            );

    // 3. クライアント描画専用ダミー飛翔体（Virtual Projectile Dummy）
    public static final RegistryObject<EntityType<ClientDummyProjectileEntity>> CLIENT_DUMMY_PROJECTILE =
            ENTITY_TYPES.register("client_dummy_projectile", () ->
                    EntityType.Builder.<ClientDummyProjectileEntity>of(ClientDummyProjectileEntity::new, MobCategory.MISC)
                            .sized(0.2F, 0.2F)
                            .noSave()
                            .fireImmune()
                            .clientTrackingRange(16)
                            .updateInterval(1)
                            .build("client_dummy_projectile")
            );

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}
