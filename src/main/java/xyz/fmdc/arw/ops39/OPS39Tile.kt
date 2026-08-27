package xyz.fmdc.arw.ops39

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalYawRotatableTileEntity
import xyz.fmdc.arw.registry.RegistryBlock

class OPS39Tile(pos: BlockPos, state: BlockState) :
    ModelNormalYawRotatableTileEntity(RegistryBlock.blockEntityType(OPS39Tile::class.java), pos, state) {
}
