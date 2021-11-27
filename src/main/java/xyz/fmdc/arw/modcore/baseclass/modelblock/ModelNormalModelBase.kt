package xyz.fmdc.arw.modcore.baseclass.modelblock

import cpw.mods.fml.client.FMLClientHandler
import net.minecraft.client.model.ModelBase
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.ResourceLocation
import net.minecraftforge.client.model.AdvancedModelLoader
import net.minecraftforge.client.model.IModelCustom
import org.lwjgl.opengl.GL11
import xyz.fmdc.arw.modcore.baseclass.IParallelModelLoad

abstract class ModelNormalModelBase<T : TileEntity> : ModelBase(), IParallelModelLoad {
    abstract val modelName: ResourceLocation
    abstract val texture: ResourceLocation

    open fun renderBase() {
        model?.renderAll()
    }

    protected var model: IModelCustom? = null

    @Synchronized
    override fun loadModel() {
        model = AdvancedModelLoader.loadModel(modelName)
    }

    open fun render(tile: T, x: Double, y: Double, z: Double) {
        GL11.glPushMatrix()
        GL11.glTranslated(x + 0.5, y, z + 0.5)

        FMLClientHandler.instance().client.renderEngine.bindTexture(texture)
        renderBase()

        GL11.glPopMatrix()
    }
}
