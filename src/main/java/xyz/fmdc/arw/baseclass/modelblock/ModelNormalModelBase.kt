package xyz.fmdc.arw.baseclass.modelblock

import cpw.mods.fml.client.FMLClientHandler
import net.minecraft.client.model.ModelBase
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.ResourceLocation
import net.minecraftforge.client.model.IModelCustom
import org.lwjgl.opengl.GL11
import xyz.fmdc.arw.baseclass.IParallelModelLoad
import xyz.fmdc.arw.baseclass.module.direction.IDirection
import xyz.fmdc.arw.modelloder.WavefrontObject

abstract class ModelNormalModelBase<T : TileEntity> : ModelBase(), IParallelModelLoad {
    abstract val modelName: ResourceLocation
    abstract val texture: ResourceLocation

    open fun renderBase() {
        model?.renderAll()
    }

    protected var model: IModelCustom? = null

    override fun loadModel() {
        model = WavefrontObject(modelName)
    }

    open fun render(tile: T, x: Double, y: Double, z: Double) {
        GL11.glPushMatrix()
        GL11.glTranslated(x + 0.5, y, z + 0.5)

        FMLClientHandler.instance().client.renderEngine.bindTexture(texture)

        if (tile is IDirection) {
            val directionYaw = tile.getDirectionAngle()
            GL11.glRotated(-directionYaw, 0.0, 1.0, 0.0)
            renderBase()
            GL11.glRotated(+directionYaw, 0.0, 1.0, 0.0)
        } else {
            renderBase()
        }

        GL11.glPopMatrix()
    }
}
