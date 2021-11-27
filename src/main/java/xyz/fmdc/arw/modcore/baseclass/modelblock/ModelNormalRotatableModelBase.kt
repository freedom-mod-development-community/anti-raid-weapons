package xyz.fmdc.arw.modcore.baseclass.modelblock

import cpw.mods.fml.client.FMLClientHandler
import net.minecraft.tileentity.TileEntity
import org.lwjgl.opengl.GL11
import xyz.fmdc.arw.modcore.baseclass.module.direction.IDirection
import xyz.fmdc.arw.modcore.baseclass.module.rotatable.IRotatablePitch
import xyz.fmdc.arw.modcore.baseclass.module.rotatable.IRotatableYaw

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
        //GL11.glScaled(100.0, 100.0, 100.0)

        FMLClientHandler.instance().client.renderEngine.bindTexture(texture)

        if (tile is IDirection) {
            val directionYaw = tile.getDirectionAngle()
            GL11.glRotated(-directionYaw, 0.0, 1.0, 0.0)
            renderBase()
            GL11.glRotated(+directionYaw, 0.0, 1.0, 0.0)
        } else {
            renderBase()
        }

        offsetYaw()

        if (tile is IRotatableYaw) {
            val yawDeg = tile.yawDeg
            GL11.glRotated(-yawDeg, 0.0, 1.0, 0.0)
            renderYaw()
        }

        offsetPitch()

        if (tile is IRotatablePitch) {
            val pitchDeg = tile.pitchDeg
            GL11.glRotated(-pitchDeg, 1.0, 0.0, 0.0)
            renderPitch()
        }

        GL11.glPopMatrix()
    }
}
