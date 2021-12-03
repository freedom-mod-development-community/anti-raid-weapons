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
import xyz.fmdc.arw.spg62.SPG62Model
import xyz.fmdc.arw.spg62.SPG62Tile
import java.util.*
import java.util.concurrent.Executors

@SideOnly(Side.CLIENT)
object RegistryRenderer {
    private var modelLoadList: ArrayList<IParallelModelLoad> = ArrayList()

    fun registerRenderer() {
        //register
        registerNormalRenderer(NORA1CTile::class.java, NORA1CModel())
        registerNormalRenderer(NORQ1Tile::class.java, NORQ1Model())
        registerNormalRenderer(SPG62Tile::class.java, SPG62Model())
        registerNormalRenderer(CICElectricTile::class.java, CICElectricModel())
        registerNormalRenderer(NORA1CTile::class.java, NORA1CModel())

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
