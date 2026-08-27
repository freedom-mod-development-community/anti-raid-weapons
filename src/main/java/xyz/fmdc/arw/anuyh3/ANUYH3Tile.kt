package xyz.fmdc.arw.anuyh3

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalTileEntity
import xyz.fmdc.arw.registry.RegistryBlock

class ANUYH3Tile(pos: BlockPos, state: BlockState) :
    ModelNormalTileEntity(RegistryBlock.blockEntityType(ANUYH3Tile::class.java), pos, state) {
}
