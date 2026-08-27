package xyz.fmdc.arw.cicelectric

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState
import xyz.fmdc.arw.baseclass.modelblock.IGlowingModel
import xyz.fmdc.arw.baseclass.modelblock.ModelNormalTileEntity
import xyz.fmdc.arw.registry.RegistryBlock

class CICElectricTile(pos: BlockPos, state: BlockState) :
    ModelNormalTileEntity(RegistryBlock.blockEntityType(CICElectricTile::class.java), pos, state), IGlowingModel {

    override fun getLight(): Int = IGlowingModel.MAX_LIGHT
}
