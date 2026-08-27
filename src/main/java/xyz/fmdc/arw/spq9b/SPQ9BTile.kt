package xyz.fmdc.arw.spq9b

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalYawRotatableTileEntity
import xyz.fmdc.arw.registry.RegistryBlock

class SPQ9BTile(pos: BlockPos, state: BlockState) :
    ModelNormalYawRotatableTileEntity(RegistryBlock.blockEntityType(SPQ9BTile::class.java), pos, state) {
}
