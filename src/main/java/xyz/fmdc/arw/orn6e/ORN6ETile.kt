package xyz.fmdc.arw.orn6e

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalTileEntity
import xyz.fmdc.arw.registry.RegistryBlock

class ORN6ETile(pos: BlockPos, state: BlockState) :
    ModelNormalTileEntity(RegistryBlock.blockEntityType(ORN6ETile::class.java), pos, state) {
}
