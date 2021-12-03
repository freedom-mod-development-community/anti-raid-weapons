package xyz.fmdc.arw.registry

import cpw.mods.fml.common.registry.GameRegistry
import net.minecraft.block.Block
import net.minecraft.tileentity.TileEntity
import xyz.fmdc.arw.cicelectric.CICElectricBlock
import xyz.fmdc.arw.cicelectric.CICElectricTile
import xyz.fmdc.arw.nora1c.NORA1CBlock
import xyz.fmdc.arw.nora1c.NORA1CTile
import xyz.fmdc.arw.norq1.NORQ1Block
import xyz.fmdc.arw.norq1.NORQ1Tile
import xyz.fmdc.arw.ops39.OPS39Block
import xyz.fmdc.arw.ops39.OPS39Tile
import xyz.fmdc.arw.orn6e.ORN6EBlock
import xyz.fmdc.arw.orn6e.ORN6ETile
import xyz.fmdc.arw.spg62.SPG62Block
import xyz.fmdc.arw.spg62.SPG62Tile
import xyz.fmdc.arw.spq9b.SPQ9BBlock
import xyz.fmdc.arw.spq9b.SPQ9BTile

object RegistryBlock {
    fun registerBlock() {
        registerBlock(NORA1CBlock(), NORA1CTile::class.java)
        registerBlock(NORQ1Block(), NORQ1Tile::class.java)
        registerBlock(CICElectricBlock(), CICElectricTile::class.java)
        registerBlock(OPS39Block(), OPS39Tile::class.java)
        registerBlock(ORN6EBlock(), ORN6ETile::class.java)
        registerBlock(SPG62Block(), SPG62Tile::class.java)
        registerBlock(SPQ9BBlock(), SPQ9BTile::class.java)
    }

    private fun registerBlock(block: Block, tileEntityClass: Class<out TileEntity>) {
        GameRegistry.registerBlock(block, block.unlocalizedName)
        GameRegistry.registerTileEntity(tileEntityClass, block.unlocalizedName)
    }
}
