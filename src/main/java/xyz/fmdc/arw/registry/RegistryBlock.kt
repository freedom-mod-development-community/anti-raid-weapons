package xyz.fmdc.arw.registry

import cpw.mods.fml.common.registry.GameRegistry
import net.minecraft.block.Block
import net.minecraft.tileentity.TileEntity
import xyz.fmdc.arw.cicelectric.CICElectricBlock
import xyz.fmdc.arw.cicelectric.CICElectricTile
import xyz.fmdc.arw.nora1c.NORA1CBlock
import xyz.fmdc.arw.nora1c.NORA1CTile
import xyz.fmdc.arw.spg62.SPG62Block
import xyz.fmdc.arw.spg62.SPG62Tile

object RegistryBlock {
    fun registerBlock() {
        registerBlock(NORA1CBlock(), NORA1CTile::class.java)
        registerBlock(SPG62Block(), SPG62Tile::class.java)
        registerBlock(CICElectricBlock(), CICElectricTile::class.java)
    }

    private fun registerBlock(block: Block, tileEntityClass: Class<out TileEntity>) {
        GameRegistry.registerBlock(block, block.unlocalizedName)
        GameRegistry.registerTileEntity(tileEntityClass, block.unlocalizedName)
    }
}
