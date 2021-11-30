package xyz.fmdc.arw.registry

import cpw.mods.fml.common.registry.GameRegistry
import net.minecraft.block.Block
import net.minecraft.tileentity.TileEntity
import xyz.fmdc.arw.spg62.SPG62Block
import xyz.fmdc.arw.spg62.SPG62Tile

object RegistryBlock {
    fun registerBlock() {
        registerBlock(SPG62Block(), SPG62Tile::class.java)
    }

    private fun registerBlock(block: Block, tileEntityClass: Class<out TileEntity>) {
        GameRegistry.registerBlock(block, block.unlocalizedName)
        GameRegistry.registerTileEntity(tileEntityClass, block.unlocalizedName)
    }
}
