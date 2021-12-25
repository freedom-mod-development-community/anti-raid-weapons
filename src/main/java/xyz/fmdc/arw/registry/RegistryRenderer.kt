package xyz.fmdc.arw.registry

import net.minecraft.tileentity.TileEntity
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import xyz.fmdc.arw.ansps49.ANSPS49Model
import xyz.fmdc.arw.ansps49.ANSPS49Tile
import xyz.fmdc.arw.anuyh3.ANUYH3Model
import xyz.fmdc.arw.anuyh3.ANUYH3Tile
import xyz.fmdc.arw.baseclass.IParallelModelLoad
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalModelBase
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalRenderer
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
import java.util.*
import java.util.concurrent.Executors

@SideOnly(Side.CLIENT)
object RegistryRenderer {
    private var modelLoadList: ArrayList<IParallelModelLoad> = ArrayList()

    fun registerRenderer() {
        //register
        registerNormalRenderer(ANUYH3Tile::class.java, ANUYH3Model())
        registerNormalRenderer(ANSPS49Tile::class.java, ANSPS49Model())
        registerNormalRenderer(CICElectricTile::class.java, CICElectricModel())
        registerNormalRenderer(NORA1CTile::class.java, NORA1CModel())
        registerNormalRenderer(NORQ1Tile::class.java, NORQ1Model())
        registerNormalRenderer(NORA1CTile::class.java, NORA1CModel())
        registerNormalRenderer(OPS39Tile::class.java, OPS39Model())
        registerNormalRenderer(ORN6ETile::class.java, ORN6EModel())
        registerNormalRenderer(SPG62Tile::class.java, SPG62Model())
        registerNormalRenderer(SPQ9BTile::class.java, SPQ9BModel())
        registerNormalRenderer(USC42Tile::class.java, USC42Model())

        //Model Loading
        val exec = Executors.newWorkStealingPool()
        try {
            modelLoadList.forEach {
                exec.submit { it.loadModel() }
            }
        } finally {
            exec.shutdown()
        }
    }

    private fun <T : TileEntity> registerNormalRenderer(tileClass: Class<T>, model: ModelNormalModelBase<T>) {
        modelLoadList.add(model)
        ClientRegistry.bindTileEntitySpecialRenderer(tileClass, ModelNormalRenderer(model))
    }
}
