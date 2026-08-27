package xyz.fmdc.arw.usc42

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalTileEntity
import xyz.fmdc.arw.registry.RegistryBlock

class USC42Tile(pos: BlockPos, state: BlockState) :
    ModelNormalTileEntity(RegistryBlock.blockEntityType(USC42Tile::class.java), pos, state) {
}
