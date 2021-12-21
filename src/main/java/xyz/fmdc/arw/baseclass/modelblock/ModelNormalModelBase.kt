package xyz.fmdc.arw.baseclass.modelblock

import net.minecraft.client.model.ModelBase
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.client.FMLClientHandler
import org.lwjgl.opengl.GL11
import xyz.fmdc.arw.baseclass.IParallelModelLoad
import xyz.fmdc.arw.baseclass.module.direction.IDirection
import xyz.fmdc.arw.modelloder.WavefrontObject


abstract class ModelNormalModelBase<T : TileEntity> : ModelBase(), IParallelModelLoad {
    abstract val modelName: ResourceLocation

    open fun renderBase() {
        model?.renderAll()
    }

    /**
     * @param light light value (0~240)
     * @see IGlowingModel.getLight()
     */
    open fun renderBaseWithGlowing(light: Int) {
        model?.renderPart("base")
        model?.renderPart("baselight", light)
    }

    protected open var model: WavefrontObject? = null

    override fun loadModel() {
        model = WavefrontObject(modelName)
    }

    abstract fun getTexture(tile: T): ResourceLocation

    open fun render(tile: T, x: Double, y: Double, z: Double) {
        GL11.glPushMatrix()
        GL11.glTranslated(x + 0.5, y, z + 0.5)

        FMLClientHandler.instance().client.renderEngine.bindTexture(getTexture(tile))

        if (tile is IDirection) {
            val directionYaw = tile.getDirectionAngle()
            GL11.glRotated(-directionYaw, 0.0, 1.0, 0.0)
            if (tile is IGlowingModel) {
                renderBaseWithGlowing(tile.getLight())
            } else {
                renderBase()
            }
            GL11.glRotated(+directionYaw, 0.0, 1.0, 0.0)
        } else {
            renderBase()
        }

        GL11.glPopMatrix()
    }
}
