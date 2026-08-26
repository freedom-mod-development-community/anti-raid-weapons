package xyz.fmdc.arw.registry

import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.EntityRenderersEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import xyz.fmdc.arw.ARWMod
import xyz.fmdc.arw.ansps49.ANSPS49Model
import xyz.fmdc.arw.ansps49.ANSPS49Tile
import xyz.fmdc.arw.anuyh3.ANUYH3Model
import xyz.fmdc.arw.anuyh3.ANUYH3Tile
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalModelBase
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalRenderer
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalTileEntity
import xyz.fmdc.arw.cicelectric.CICElectricModel
import xyz.fmdc.arw.cicelectric.CICElectricTile
import xyz.fmdc.arw.nora1c.NORA1CModel
import xyz.fmdc.arw.nora1c.NORA1CTile
import xyz.fmdc.arw.norq1.NORQ1Model
import xyz.fmdc.arw.norq1.NORQ1Tile
import xyz.fmdc.arw.ops39.OPS39Model
import xyz.fmdc.arw.ops39.OPS39Tile
import xyz.fmdc.arw.orn6e.ORN6EModel
import xyz.fmdc.arw.orn6e.ORN6ETile
import xyz.fmdc.arw.spg62.SPG62Model
import xyz.fmdc.arw.spg62.SPG62Tile
import xyz.fmdc.arw.spq9b.SPQ9BModel
import xyz.fmdc.arw.spq9b.SPQ9BTile
import xyz.fmdc.arw.usc42.USC42Model
import xyz.fmdc.arw.usc42.USC42Tile

@Mod.EventBusSubscriber(modid = ARWMod.DOMAIN, bus = Mod.EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
object RegistryRenderer {

    @SubscribeEvent
    fun registerRenderers(event: EntityRenderersEvent.RegisterRenderers) {
        registerRenderer(event, ANSPS49Tile::class.java, ANSPS49Model())
        registerRenderer(event, ANUYH3Tile::class.java, ANUYH3Model())
        registerRenderer(event, CICElectricTile::class.java, CICElectricModel())
        registerRenderer(event, NORA1CTile::class.java, NORA1CModel())
        registerRenderer(event, NORQ1Tile::class.java, NORQ1Model())
        registerRenderer(event, OPS39Tile::class.java, OPS39Model())
        registerRenderer(event, ORN6ETile::class.java, ORN6EModel())
        registerRenderer(event, SPG62Tile::class.java, SPG62Model())
        registerRenderer(event, SPQ9BTile::class.java, SPQ9BModel())
        registerRenderer(event, USC42Tile::class.java, USC42Model())
    }

    /**
     * TileEntity クラスと Model インスタンスを受け取り、BER を登録するヘルパー関数
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T : ModelNormalTileEntity> registerRenderer(
        event: EntityRenderersEvent.RegisterRenderers,
        tileClass: Class<T>,
        model: ModelNormalModelBase
    ) {
        val tileRegistryObj = RegistryBlock.BLOCK_ENTITIES_MAP[tileClass] as? net.minecraftforge.registries.RegistryObject<BlockEntityType<T>>
        tileRegistryObj?.ifPresent { tileType ->
            event.registerBlockEntityRenderer(tileType) { ModelNormalRenderer(model) }
        }
    }
}