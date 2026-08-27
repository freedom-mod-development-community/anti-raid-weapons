package xyz.fmdc.arw.ansps49

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalYawPitchRotatableTileEntity
import xyz.fmdc.arw.registry.RegistryBlock

class ANSPS49Tile(pos: BlockPos, state: BlockState) :
    ModelNormalYawPitchRotatableTileEntity(RegistryBlock.blockEntityType(ANSPS49Tile::class.java), pos, state) {

    override fun getDefaultPitchDeg(): Double = 15.0
}
