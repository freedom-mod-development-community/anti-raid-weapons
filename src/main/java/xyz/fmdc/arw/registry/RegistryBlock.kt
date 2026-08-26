package xyz.fmdc.arw.registry

import com.mojang.datafixers.DSL
import net.minecraft.core.BlockPos
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.MapColor
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject
import xyz.fmdc.arw.ARWMod
import xyz.fmdc.arw.ansps49.ANSPS49Block
import xyz.fmdc.arw.ansps49.ANSPS49Tile
import xyz.fmdc.arw.anuyh3.ANUYH3Block
import xyz.fmdc.arw.anuyh3.ANUYH3Tile
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalBlockContainer
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalTileEntity
import xyz.fmdc.arw.cicelectric.CICElectricBlock
import xyz.fmdc.arw.cicelectric.CICElectricTile
import xyz.fmdc.arw.nora1c.NORA1CBlock
import xyz.fmdc.arw.nora1c.NORA1CTile
import xyz.fmdc.arw.norq1.NORQ1Block
import xyz.fmdc.arw.norq1.NORQ1Tile
import xyz.fmdc.arw.ops39.OPS39Block
import xyz.fmdc.arw.ops39.OPS39Tile
import xyz.fmdc.arw.orn6e.ORN6EBlock
import xyz.fmdc.arw.orn6e.ORN6ETile
import xyz.fmdc.arw.spg62.SPG62Block
import xyz.fmdc.arw.spg62.SPG62Tile
import xyz.fmdc.arw.spq9b.SPQ9BBlock
import xyz.fmdc.arw.spq9b.SPQ9BTile
import xyz.fmdc.arw.usc42.USC42Block
import xyz.fmdc.arw.usc42.USC42Tile
import java.util.function.Supplier

object RegistryBlock {
    // 1. レジストリ定義
    val BLOCKS: DeferredRegister<Block> = DeferredRegister.create(ForgeRegistries.BLOCKS, ARWMod.DOMAIN)
    val ITEMS: DeferredRegister<Item> = DeferredRegister.create(ForgeRegistries.ITEMS, ARWMod.DOMAIN)
    val BLOCK_ENTITIES: DeferredRegister<BlockEntityType<*>> = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ARWMod.DOMAIN)

    val BLOCK_ENTITIES_MAP = mutableMapOf<Class<out ModelNormalTileEntity>, RegistryObject<out BlockEntityType<*>>>()
    val BLOCKS_MAP = mutableMapOf<String, RegistryObject<out Block>>()
    val ITEMS_MAP = mutableMapOf<Class<out ModelNormalBlockContainer>, RegistryObject<Item>>()

    // 2. 汎用プロパティ定義
    val DEFAULT_PROPERTIES: BlockBehaviour.Properties = BlockBehaviour.Properties.of()
        .mapColor(MapColor.METAL)
        .strength(3.0f, 6.0f)
        .sound(SoundType.METAL)
        .noOcclusion()

    // 3. 各ブロックの遅延登録定義 (関数参照 ::BlockName で渡す)
    val AN_SPS_49 = registerBlock("an_sps_49", ::ANSPS49Block, ANSPS49Tile::class.java)
    val AN_UYH_3 = registerBlock("an_uyh_3", ::ANUYH3Block, ANUYH3Tile::class.java)
    val CIC_ELECTRIC = registerBlock("cic_electric", ::CICElectricBlock, CICElectricTile::class.java)
    val NORA_1C = registerBlock("nora_1c", ::NORA1CBlock, NORA1CTile::class.java)
    val NORQ_1 = registerBlock("norq_1", ::NORQ1Block, NORQ1Tile::class.java)
    val OPS_39 = registerBlock("ops_39", ::OPS39Block, OPS39Tile::class.java)
    val ORN_6E = registerBlock("orn_6e", ::ORN6EBlock, ORN6ETile::class.java)
    val SPG_62 = registerBlock("spg_62", ::SPG62Block, SPG62Tile::class.java)
    val SPQ_9B = registerBlock("spq_9b", ::SPQ9BBlock, SPQ9BTile::class.java)
    val USC_42 = registerBlock("usc_42", ::USC42Block, USC42Tile::class.java)

    /**
     * ブロックインスタンスを遅延生成（Supplier経由）して一括登録する汎用関数
     */
    fun <B : ModelNormalBlockContainer, T : ModelNormalTileEntity> registerBlock(
        name: String,
        blockSupplier: Supplier<B>,
        tileEntityClass: Class<T>
    ): RegistryObject<B> {
        // 1. Block の遅延登録
        val blockObj: RegistryObject<B> = BLOCKS.register(name, blockSupplier)
        BLOCKS_MAP[name] = blockObj

        // 2. BlockItem の遅延登録
        val itemObj: RegistryObject<Item> = ITEMS.register(name) {
            BlockItem(blockObj.get(), Item.Properties())
        }

        // 3. BlockEntityType の遅延登録
        val tileObj: RegistryObject<BlockEntityType<T>> = BLOCK_ENTITIES.register(name) {
            BlockEntityType.Builder.of(
                { pos: BlockPos, state: BlockState ->
                    tileEntityClass.getConstructor(BlockPos::class.java, BlockState::class.java)
                        .newInstance(pos, state)
                },
                blockObj.get()
            ).build(DSL.remainderType())
        }

        BLOCK_ENTITIES_MAP[tileEntityClass] = tileObj
        return blockObj
    }

    /**
     * ブロッククラスから対応する BlockItem の ItemStack を安全に取得
     */
    fun getItemStack(blockClass: Class<out ModelNormalBlockContainer>): ItemStack {
        val item = ITEMS_MAP[blockClass]?.get() ?: Items.AIR
        return ItemStack(item)
    }
}
