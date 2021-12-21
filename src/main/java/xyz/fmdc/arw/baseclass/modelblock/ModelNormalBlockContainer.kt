package xyz.fmdc.arw.baseclass.modelblock

import net.minecraft.block.BlockContainer
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import xyz.fmdc.arw.ARWMod
import xyz.fmdc.arw.baseclass.module.direction.IDirection
import xyz.fmdc.arw.baseclass.module.rotatable.IPitchRotatable
import xyz.fmdc.arw.baseclass.module.rotatable.IYawRotatable
import xyz.fmdc.arw.baseclass.module.rotatable.pitchDeg
import xyz.fmdc.arw.baseclass.module.rotatable.yawDeg

open class ModelNormalBlockContainer(
    material: Material = Material.ROCK,
    private val tileEntityClass: Class<out ModelNormalTileEntity>,
) : BlockContainer(material) {
    init {
        creativeTab = ARWMod.arwTabs
    }


    override fun onBlockPlacedBy(
        worldIn: World,
        pos: BlockPos,
        state: IBlockState,
        placer: EntityLivingBase,
        stack: ItemStack,
    ) {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack)
        val tile = worldIn.getTileEntity(pos)
        if (tile is IDirection) {
            tile.saveReversDirectionData(placer.rotationYaw)
            if (tile is IYawRotatable) {
                tile.yawDeg = tile.getDefaultYaw(tile.getDirectionAngle())
                tile.markDirty()
            }
        }
        if (tile is IPitchRotatable) {
            tile.pitchDeg = tile.getDefaultPitchDeg()
        }
    }

    private lateinit var boundingBox: AxisAlignedBB
    fun setBlockBoundsSize(wide: Float, height: Float) {
        val halfWide = wide / 2
        boundingBox = AxisAlignedBB(
            -halfWide + 0.5, 0.0, -halfWide + 0.5,
            +halfWide + 0.5, +height.toDouble(), +halfWide + 0.5)
    }

    override fun getBoundingBox(state: IBlockState, source: IBlockAccess, pos: BlockPos): AxisAlignedBB {
        return boundingBox
    }

    override fun createNewTileEntity(p_149915_1_: World?, p_149915_2_: Int): TileEntity {
        return tileEntityClass.newInstance()
    }

    override fun isOpaqueCube(state: IBlockState): Boolean {
        return false
    }
}
