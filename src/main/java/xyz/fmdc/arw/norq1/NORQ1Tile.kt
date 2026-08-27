package xyz.fmdc.arw.norq1

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalTileEntity
import xyz.fmdc.arw.registry.RegistryBlock

class NORQ1Tile(pos: BlockPos, state: BlockState) :
    ModelNormalTileEntity(RegistryBlock.blockEntityType(NORQ1Tile::class.java), pos, state) {
}
