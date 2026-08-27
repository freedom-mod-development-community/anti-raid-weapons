package xyz.fmdc.arw.spg62

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalYawPitchRotatableTileEntity
import xyz.fmdc.arw.registry.RegistryBlock

class SPG62Tile(pos: BlockPos, state: BlockState) :
    ModelNormalYawPitchRotatableTileEntity(RegistryBlock.blockEntityType(SPG62Tile::class.java), pos, state) {

    override fun getDefaultPitchDeg(): Double = 45.0
}
