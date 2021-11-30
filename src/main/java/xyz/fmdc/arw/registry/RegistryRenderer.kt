package xyz.fmdc.arw.registry

import cpw.mods.fml.client.registry.ClientRegistry
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import net.minecraft.tileentity.TileEntity
import xyz.fmdc.arw.baseclass.IParallelModelLoad
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalModelBase
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalRenderer
import xyz.fmdc.arw.spg62.SPG62Model
import xyz.fmdc.arw.spg62.SPG62Tile
import java.util.concurrent.Executors

@SideOnly(Side.CLIENT)
object RegistryRenderer {
    private var rendererList: ArrayList<IParallelModelLoad> = ArrayList()

    fun registerRenderer() {
        //register
        registerNormalRenderer(SPG62Tile::class.java, SPG62Model())

        //Model Loading
        val exec = Executors.newCachedThreadPool()
        rendererList.forEach {
            exec.submit { it.loadModel() }
        }
    }

    private fun registerNormalRenderer(tileClass: Class<out TileEntity>, model: ModelNormalModelBase<*>) {
        rendererList.add(model)
        ClientRegistry.bindTileEntitySpecialRenderer(tileClass, ModelNormalRenderer(model))
    }
}
