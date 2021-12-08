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
import xyz.fmdc.arw.spg62.SPG62Model
import xyz.fmdc.arw.spg62.SPG62Tile
import java.util.concurrent.Executors

@SideOnly(Side.CLIENT)
object RegistryRenderer {
    private var modelLoadList: ArrayList<IParallelModelLoad> = ArrayList()

    fun registerRenderer() {
        //register
        registerNormalRenderer(SPG62Tile::class.java, SPG62Model())
        registerNormalRenderer(CICElectricTile::class.java, CICElectricModel())

        //Model Loading
        val exec = Executors.newCachedThreadPool()
        modelLoadList.forEach {
            exec.submit { it.loadModel() }
        }
    }

    private fun registerNormalRenderer(tileClass: Class<out TileEntity>, model: ModelNormalModelBase<*>) {
        modelLoadList.add(model)
        ClientRegistry.bindTileEntitySpecialRenderer(tileClass, ModelNormalRenderer(model))
    }
}
