package xyz.fmdc.arw.registry

import cpw.mods.fml.client.registry.ClientRegistry
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import net.minecraft.tileentity.TileEntity
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
import java.util.*
import java.util.concurrent.Executors

@SideOnly(Side.CLIENT)
object RegistryRenderer {
    private var modelLoadList: ArrayList<IParallelModelLoad> = ArrayList()

    fun registerRenderer() {
        //register
        registerNormalRenderer(NORA1CTile::class.java, NORA1CModel())
        registerNormalRenderer(NORQ1Tile::class.java, NORQ1Model())
        registerNormalRenderer(CICElectricTile::class.java, CICElectricModel())
        registerNormalRenderer(NORA1CTile::class.java, NORA1CModel())
        registerNormalRenderer(OPS39Tile::class.java, OPS39Model())
        registerNormalRenderer(ORN6ETile::class.java, ORN6EModel())
        registerNormalRenderer(SPG62Tile::class.java, SPG62Model())
        registerNormalRenderer(SPQ9BTile::class.java, SPQ9BModel())

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

    private fun registerNormalRenderer(tileClass: Class<out TileEntity>, model: ModelNormalModelBase<*>) {
        modelLoadList.add(model)
        ClientRegistry.bindTileEntitySpecialRenderer(tileClass, ModelNormalRenderer(model))
    }
}
