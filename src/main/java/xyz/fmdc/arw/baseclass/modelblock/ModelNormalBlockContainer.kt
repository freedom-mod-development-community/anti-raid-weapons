package xyz.fmdc.arw.baseclass.modelblock

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import net.minecraft.block.BlockContainer
import net.minecraft.block.material.Material
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.AxisAlignedBB
import net.minecraft.world.World
import xyz.fmdc.arw.ARWMod
import xyz.fmdc.arw.baseclass.module.direction.IDirection
import xyz.fmdc.arw.baseclass.module.rotatable.IPitchRotatable
import xyz.fmdc.arw.baseclass.module.rotatable.IYawRotatable
import xyz.fmdc.arw.baseclass.module.rotatable.pitchDeg
import xyz.fmdc.arw.baseclass.module.rotatable.yawDeg

open class ModelNormalBlockContainer(
    material: Material = Material.rock,
    private val tileEntityClass: Class<out ModelNormalTileEntity>,
) : BlockContainer(material) {
    init {
        setCreativeTab(ARWMod.arwTabs)
    }


    override fun onBlockPlacedBy(
        world: World,
        x: Int,
        y: Int,
        z: Int,
        entity: EntityLivingBase,
        itemStack: ItemStack?,
    ) {
        super.onBlockPlacedBy(world, x, y, z, entity, itemStack)
        val tile = world.getTileEntity(x, y, z)
        if (tile is IDirection) {
            tile.saveReversDirectionData(entity.rotationYaw)
            if (tile is IYawRotatable) {
                tile.yawDeg = tile.getDefaultYaw(tile.getDirectionAngle())
                tile.markDirty()
            }
        }
        if (tile is IPitchRotatable) {
            tile.pitchDeg = tile.getDefaultPitchDeg()
        }
    }

    fun setBlockBoundsSize(wide: Float, height: Float) {
        val halfWide = wide / 2
        setBlockBounds(-halfWide + 0.5f, 0f, -halfWide + 0.5f, halfWide + 0.5f, height, halfWide + 0.5f)
    }

    var selectBoundsHalfWide: Double = 1.0
    var selectBoundsHeight: Double = 1.0
    fun setSelectedBoundSize(wide: Double, height: Double) {
        selectBoundsHalfWide = wide / 2
        selectBoundsHeight = height
    }

    @SideOnly(Side.CLIENT)
    override fun getSelectedBoundingBoxFromPool(
        world: World?,
        x: Int,
        y: Int,
        z: Int,
    ): AxisAlignedBB {
        return AxisAlignedBB.getBoundingBox(
            x.toDouble() - selectBoundsHalfWide + 0.5,
            y.toDouble(),
            z.toDouble() - selectBoundsHalfWide + 0.5,
            x.toDouble() + selectBoundsHalfWide + 0.5,
            y.toDouble() + selectBoundsHeight,
            z.toDouble() + selectBoundsHalfWide + 0.5)
    }

    override fun createNewTileEntity(p_149915_1_: World?, p_149915_2_: Int): TileEntity {
        return tileEntityClass.newInstance()
    }

    override fun renderAsNormalBlock(): Boolean {
        return false
    }

    override fun getRenderType(): Int {
        return -1
    }

    override fun isOpaqueCube(): Boolean {
        return false
    }
}
