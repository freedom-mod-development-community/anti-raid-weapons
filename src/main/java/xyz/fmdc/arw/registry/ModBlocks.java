package xyz.fmdc.arw.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import xyz.fmdc.arw.AntiRaidWeapons;
import xyz.fmdc.arw.common.block.console.TestConsoleBlock;
import xyz.fmdc.arw.common.block.console.Uyq21Block;
import xyz.fmdc.arw.common.block.decoration.AtagoBlock;
import xyz.fmdc.arw.common.block.decoration.EmmiBlock;
import xyz.fmdc.arw.common.block.fcs.FcsCoreBlock;
import xyz.fmdc.arw.common.block.sensor.*;
import xyz.fmdc.arw.common.block.target.TargetBlock;
import xyz.fmdc.arw.common.block.vls.VlsBlock;
import xyz.fmdc.arw.common.block.weapon.ciws.phalanxBlock;
import xyz.fmdc.arw.common.block.weapon.launcher.BonedMissileLauncherBlock;
import xyz.fmdc.arw.common.block.weapon.launcher.Mk13GmlsBlock;
import xyz.fmdc.arw.common.block.weapon.singlegun.*;
import xyz.fmdc.arw.common.blockentity.console.TestConsoleBlockEntity;
import xyz.fmdc.arw.common.blockentity.console.Uyq21BlockEntity;
import xyz.fmdc.arw.common.blockentity.decoration.AtagoBlockEntity;
import xyz.fmdc.arw.common.blockentity.decoration.EmmiBlockEntity;
import xyz.fmdc.arw.common.blockentity.fcs.FcsCoreBlockEntity;
import xyz.fmdc.arw.common.blockentity.sensor.Ops39BlockEntity;
import xyz.fmdc.arw.common.blockentity.sensor.SearchRadarBlockEntity;
import xyz.fmdc.arw.common.blockentity.sensor.Spq9bBlockEntity;
import xyz.fmdc.arw.common.blockentity.sensor.TrackingRadarBlockEntity;
import xyz.fmdc.arw.common.blockentity.sensor.optics.OpticalSightBlockEntity;
import xyz.fmdc.arw.common.blockentity.target.TargetBlockEntity;
import xyz.fmdc.arw.common.blockentity.vls.VlsBlockEntity;
import xyz.fmdc.arw.common.blockentity.weapon.ciws.PhalanxBlockEntity;
import xyz.fmdc.arw.common.blockentity.weapon.launcher.BonedMissileLauncherBlockEntity;
import xyz.fmdc.arw.common.blockentity.weapon.launcher.Mk13GmlsBlockEntity;
import xyz.fmdc.arw.common.blockentity.weapon.singlegun.*;
import xyz.fmdc.arw.registry.auto.BlockEntry;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, AntiRaidWeapons.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, AntiRaidWeapons.MOD_ID);

    // 共通プロパティの事前定義
    public static BlockBehaviour.Properties defaultProps() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(5.0F, 6.0F)
                .noOcclusion();
    }

    public static BlockBehaviour.Properties stoneProps() {
        return BlockBehaviour.Properties.copy(Blocks.STONE);
    }

    public static BlockBehaviour.Properties emeraldProps() {
        return BlockBehaviour.Properties.copy(Blocks.EMERALD_BLOCK);
    }

    public static BlockBehaviour.Properties targetProps() {
        return BlockBehaviour.Properties.copy(Blocks.TARGET)
                .strength(1.5F, 1.5F);
    }

    //yukky section
    public static final BlockEntry<BonedMissileLauncherBlock, BonedMissileLauncherBlockEntity> MK13BONE =
            new BlockEntry<>("mk13bone", () -> new BonedMissileLauncherBlock(defaultProps()), BonedMissileLauncherBlockEntity::new);
    //

    // Block + Item + BlockEntity を1行で一括登録
    public static final BlockEntry<Ops39Block, Ops39BlockEntity> OPS39 =
            new BlockEntry<>("ops39", () -> new Ops39Block(defaultProps()), Ops39BlockEntity::new);
    public static final BlockEntry<Spq9bBlock, Spq9bBlockEntity> SPQ9B =
            new BlockEntry<>("spq9b", () -> new Spq9bBlock(defaultProps()), Spq9bBlockEntity::new);
    public static final BlockEntry<EmmiBlock, EmmiBlockEntity> EMMI =
            new BlockEntry<>("emmision-test", () -> new EmmiBlock(defaultProps()), EmmiBlockEntity::new);
    public static final BlockEntry<AtagoBlock, AtagoBlockEntity> ATAGO =
            new BlockEntry<>("atago", () -> new AtagoBlock(defaultProps()), AtagoBlockEntity::new);
    public static final BlockEntry<Oto127mmBlock, Oto127mmBlockEntity> OTO127MM =
            new BlockEntry<>("oto127mm", () -> new Oto127mmBlock(defaultProps()), Oto127mmBlockEntity::new);
    public static final BlockEntry<Mk45mod4Block, Mk45Mod4BlockEntity> MK45_MOD4 =
            new BlockEntry<>("mk45mod4", () -> new Mk45mod4Block(defaultProps()), Mk45Mod4BlockEntity::new);
    public static final BlockEntry<phalanxBlock, PhalanxBlockEntity> PHALANX =
            new BlockEntry<>("phalanx", () -> new phalanxBlock(defaultProps()), PhalanxBlockEntity::new);

    // --- FCS ---
    public static final BlockEntry<FcsCoreBlock, FcsCoreBlockEntity> FCS_CORE_BLOCK =
            new BlockEntry<>("fcs_core", () -> new FcsCoreBlock(defaultProps()), FcsCoreBlockEntity::new);
    public static final BlockEntry<Uyq21Block, Uyq21BlockEntity> UYQ21 =
            new BlockEntry<>("uyq21", () -> new Uyq21Block(defaultProps()), Uyq21BlockEntity::new);

    // --- Sensors ---
    public static final BlockEntry<OpticalSightBlock, OpticalSightBlockEntity> OPTICAL_SIGHT_BLOCK =
            new BlockEntry<>("optical_sight", () -> new OpticalSightBlock(defaultProps()), OpticalSightBlockEntity::new);

    public static final BlockEntry<SearchRadarBlock, SearchRadarBlockEntity> SEARCH_RADAR_BLOCK =
            new BlockEntry<>("search_radar", () -> new SearchRadarBlock(defaultProps()), SearchRadarBlockEntity::new);

    public static final BlockEntry<TrackingRadarBlock, TrackingRadarBlockEntity> TRACKING_RADAR_BLOCK =
            new BlockEntry<>("tracking_radar", () -> new TrackingRadarBlock(defaultProps()), TrackingRadarBlockEntity::new);

    // --- Weapons ---
    public static final BlockEntry<WWIIAntiAircraftGunBlock, WWIIAntiAircraftGunBlockEntity> WW2_AA_GUN_BLOCK =
            new BlockEntry<>("ww2_aa_gun", () -> new WWIIAntiAircraftGunBlock(defaultProps()), WWIIAntiAircraftGunBlockEntity::new);

    public static final BlockEntry<ManualRwsGunBlock, ManualRwsGunBlockEntity> MANUAL_RWS_GUN_BLOCK =
            new BlockEntry<>("manual_rws_gun", () -> new ManualRwsGunBlock(defaultProps()), ManualRwsGunBlockEntity::new);

    public static final BlockEntry<VlsBlock, VlsBlockEntity> VLS_BLOCK =
            new BlockEntry<>("vls", () -> new VlsBlock(defaultProps()), VlsBlockEntity::new);

    public static final BlockEntry<MannedTankTurretBlock, MannedTankTurretBlockEntity> MANNED_TANK_TURRET_BLOCK =
            new BlockEntry<>("manned_tank_turret", () -> new MannedTankTurretBlock(defaultProps()), MannedTankTurretBlockEntity::new);

    public static final BlockEntry<Mk13GmlsBlock, Mk13GmlsBlockEntity> MK13_GMLS_BLOCK =
            new BlockEntry<>("mk13-gmls", () -> new Mk13GmlsBlock(defaultProps()), Mk13GmlsBlockEntity::new);

    // --- Target Blocks ---
    public static final BlockEntry<TargetBlock, TargetBlockEntity> TARGET_BLOCK =
            new BlockEntry<>("target_block", () -> new TargetBlock(targetProps()), TargetBlockEntity::new);

    // --- Consoles ---
    public static final BlockEntry<TestConsoleBlock, TestConsoleBlockEntity> TEST_CONSOLE =
            new BlockEntry<>("test_console", () -> new TestConsoleBlock(emeraldProps()), TestConsoleBlockEntity::new);

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
