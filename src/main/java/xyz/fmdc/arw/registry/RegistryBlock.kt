package xyz.fmdc.arw.registry

import net.minecraft.block.Block
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.registry.GameRegistry
import xyz.fmdc.arw.ansps49.ANSPS49Block
import xyz.fmdc.arw.ansps49.ANSPS49Tile
import xyz.fmdc.arw.anuyh3.ANUYH3Block
import xyz.fmdc.arw.anuyh3.ANUYH3Tile
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
import xyz.fmdc.arw.usc42.USC42Block
import xyz.fmdc.arw.usc42.USC42Tile

object RegistryBlock {
    @Suppress("unused")
    @SubscribeEvent
    fun onRegister(e: RegistryEvent.Register<Block>) {
        e.registerBlock(ANSPS49Block, ANSPS49Tile::class.java)
        e.registerBlock(ANUYH3Block, ANUYH3Tile::class.java)
        e.registerBlock(CICElectricBlock, CICElectricTile::class.java)
        e.registerBlock(NORA1CBlock, NORA1CTile::class.java)
        e.registerBlock(NORQ1Block, NORQ1Tile::class.java)
        e.registerBlock(OPS39Block, OPS39Tile::class.java)
        e.registerBlock(ORN6EBlock, ORN6ETile::class.java)
        e.registerBlock(SPG62Block, SPG62Tile::class.java)
        e.registerBlock(SPQ9BBlock, SPQ9BTile::class.java)
        e.registerBlock(USC42Block, USC42Tile::class.java)
    }

    private fun RegistryEvent.Register<Block>.registerBlock(block: Block, tileEntityClass: Class<out TileEntity>) {
        registry.register(block)
        GameRegistry.registerTileEntity(tileEntityClass, block.registryName)
    }
}
