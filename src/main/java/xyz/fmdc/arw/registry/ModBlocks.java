package xyz.fmdc.arw.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import xyz.fmdc.arw.AntiRaidWeapons;
import xyz.fmdc.arw.common.blockentity.Mk45Mod4BlockEntity;
import xyz.fmdc.arw.emmi.EmmiBlock;
import xyz.fmdc.arw.emmi.EmmiBlockEntity;
import xyz.fmdc.arw.mk45mod4.Mk45mod4Block;
import xyz.fmdc.arw.ops39.Ops39Block;
import xyz.fmdc.arw.ops39.Ops39BlockEntity;
import xyz.fmdc.arw.oto127mm.Oto127mmBlock;
import xyz.fmdc.arw.oto127mm.Oto127mmBlockEntity;
import xyz.fmdc.arw.radardisplay.RadarDisplayBlock;
import xyz.fmdc.arw.radardisplay.RadarDisplayBlockEntity;
import xyz.fmdc.arw.spq9b.Spq9bBlock;
import xyz.fmdc.arw.spq9b.Spq9bBlockEntity;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, AntiRaidWeapons.MOD_ID);

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

    // Block + Item + BlockEntity を1行で一括登録
    public static final BlockEntry<Ops39Block, Ops39BlockEntity> OPS39 =
            new BlockEntry<>("ops39", () -> new Ops39Block(defaultProps()), Ops39BlockEntity::new);

    public static final BlockEntry<Spq9bBlock, Spq9bBlockEntity> SPQ9B =
            new BlockEntry<>("spq9b", () -> new Spq9bBlock(defaultProps()), Spq9bBlockEntity::new);

    public static final BlockEntry<EmmiBlock, EmmiBlockEntity> EMMI =
            new BlockEntry<>("emmision-test", () -> new EmmiBlock(defaultProps()), EmmiBlockEntity::new);

    public static final BlockEntry<Oto127mmBlock, Oto127mmBlockEntity> OTO127MM =
            new BlockEntry<>("oto127mm", () -> new Oto127mmBlock(defaultProps()), Oto127mmBlockEntity::new);

    public static final BlockEntry<Mk45mod4Block, Mk45Mod4BlockEntity> MK45_MOD4 =
            new BlockEntry<>("mk45mod4", () -> new Mk45mod4Block(defaultProps()), Mk45Mod4BlockEntity::new);

    public static final BlockEntry<RadarDisplayBlock, RadarDisplayBlockEntity> RADAR_DISPLAY =
            new BlockEntry<>("radar_display", () -> new RadarDisplayBlock(stoneProps()), RadarDisplayBlockEntity::new);


    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
