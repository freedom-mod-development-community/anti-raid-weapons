package xyz.fmdc.arw.registry

import com.mojang.datafixers.DSL
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
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
    val BLOCK_ENTITIES: DeferredRegister<BlockEntityType<*>> =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ARWMod.DOMAIN)

    /** TileEntity 実装クラス -> 登録済み BlockEntityType の逆引き表 */
    private val BLOCK_ENTITY_TYPES = mutableMapOf<Class<out ModelNormalTileEntity>, RegistryObject<out BlockEntityType<*>>>()

    // 2. 各ブロックの遅延登録定義
    val AN_SPS_49 = registerBlock("an_sps_49", ::ANSPS49Block, ANSPS49Tile::class.java, ::ANSPS49Tile)
    val AN_UYH_3 = registerBlock("an_uyh_3", ::ANUYH3Block, ANUYH3Tile::class.java, ::ANUYH3Tile)
    val CIC_ELECTRIC = registerBlock("cic_electric", ::CICElectricBlock, CICElectricTile::class.java, ::CICElectricTile)
    val NORA_1C = registerBlock("nora_1c", ::NORA1CBlock, NORA1CTile::class.java, ::NORA1CTile)
    val NORQ_1 = registerBlock("norq_1", ::NORQ1Block, NORQ1Tile::class.java, ::NORQ1Tile)
    val OPS_39 = registerBlock("ops_39", ::OPS39Block, OPS39Tile::class.java, ::OPS39Tile)
    val ORN_6E = registerBlock("orn_6e", ::ORN6EBlock, ORN6ETile::class.java, ::ORN6ETile)
    val SPG_62 = registerBlock("spg_62", ::SPG62Block, SPG62Tile::class.java, ::SPG62Tile)
    val SPQ_9B = registerBlock("spq_9b", ::SPQ9BBlock, SPQ9BTile::class.java, ::SPQ9BTile)
    val USC_42 = registerBlock("usc_42", ::USC42Block, USC42Tile::class.java, ::USC42Tile)

    /**
     * Block / BlockItem / BlockEntityType をまとめて遅延登録する。
     *
     * [tileFactory] は `::XxxTile` の形でコンストラクタ参照を渡す。リフレクションを使わないため、
     * TileEntity 側のコンストラクタシグネチャ変更はコンパイル時に検出される。
     */
    private fun <B : ModelNormalBlockContainer, T : ModelNormalTileEntity> registerBlock(
        name: String,
        blockSupplier: Supplier<B>,
        tileEntityClass: Class<T>,
        tileFactory: BlockEntityType.BlockEntitySupplier<T>
    ): RegistryObject<B> {
        val blockObj: RegistryObject<B> = BLOCKS.register(name, blockSupplier)

        ITEMS.register(name) { BlockItem(blockObj.get(), Item.Properties()) }

        BLOCK_ENTITY_TYPES[tileEntityClass] = BLOCK_ENTITIES.register(name) {
            BlockEntityType.Builder.of(tileFactory, blockObj.get()).build(DSL.remainderType())
        }

        return blockObj
    }

    /**
     * TileEntity 実装クラスから登録済みの [BlockEntityType] を取得する。
     * 各 TileEntity のコンストラクタおよび BER 登録から呼ばれる。
     */
    fun blockEntityType(tileEntityClass: Class<out ModelNormalTileEntity>): BlockEntityType<*> =
        BLOCK_ENTITY_TYPES[tileEntityClass]?.get()
            ?: throw IllegalStateException("BlockEntityType is not registered for $tileEntityClass")
}
