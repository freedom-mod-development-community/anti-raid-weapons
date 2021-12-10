package xyz.fmdc.arw.baseclass.modelblock

import cpw.mods.fml.client.FMLClientHandler
import net.minecraft.tileentity.TileEntity
import org.lwjgl.opengl.GL11
import xyz.fmdc.arw.baseclass.module.direction.IDirection
import xyz.fmdc.arw.baseclass.module.rotatable.IPitchRotatable
import xyz.fmdc.arw.baseclass.module.rotatable.IYawRotatable
import xyz.fmdc.arw.baseclass.module.rotatable.pitchDeg
import xyz.fmdc.arw.baseclass.module.rotatable.yawDeg

abstract class ModelNormalRotatableModelBase<T : TileEntity> : ModelNormalModelBase<T>() {
    override fun renderBase() {
        model?.renderPart("base")
    }

    open fun renderYaw() {
        model?.renderPart("yaw")
    }

    open fun renderPitch() {
        model?.renderPart("pitch")
    }

    open fun offsetYaw() {}

    open fun offsetPitch() {}

    override fun render(tile: T, x: Double, y: Double, z: Double) {
        GL11.glPushMatrix()
        GL11.glTranslated(x + 0.5, y, z + 0.5)

        FMLClientHandler.instance().client.renderEngine.bindTexture(getTexture(tile))

        if (tile is IDirection) {
            val directionYaw = tile.getDirectionAngle()
            GL11.glRotated(-directionYaw, 0.0, 1.0, 0.0)
            renderBase()
            GL11.glRotated(+directionYaw, 0.0, 1.0, 0.0)
        } else {
            renderBase()
        }

        offsetYaw()

        if (tile is IYawRotatable) {
            val yawDeg = tile.yawDeg
            GL11.glRotated(-yawDeg, 0.0, 1.0, 0.0)
            renderYaw()
        }

        offsetPitch()

        if (tile is IPitchRotatable) {
            val pitchDeg = tile.pitchDeg
            GL11.glRotated(-pitchDeg, 1.0, 0.0, 0.0)
            renderPitch()
        }

        GL11.glPopMatrix()
    }
}
