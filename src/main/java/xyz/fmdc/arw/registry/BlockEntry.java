package xyz.fmdc.arw.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class BlockEntry<B extends Block, E extends BlockEntity> {
    public final RegistryObject<B> block;
    public final RegistryObject<Item> item;
    public final RegistryObject<BlockEntityType<E>> blockEntity;

    public BlockEntry(String name, Supplier<B> blockFactory, BlockEntityType.BlockEntitySupplier<E> beFactory) {
        // 1. Block の登録
        this.block = ModBlocks.BLOCKS.register(name, blockFactory);
        // 2. BlockItem の自動登録
        this.item = ModItems.ITEMS.register(name, () -> new BlockItem(this.block.get(), new Item.Properties()));
        // 3. BlockEntity の自動登録 ("_be" サフィックスを付与)
        this.blockEntity = ModBlockEntities.BLOCK_ENTITIES.register(name + "_be",
                () -> BlockEntityType.Builder.of(beFactory, this.block.get()).build(null)
        );
    }

    public B getBlock() { return block.get(); }
    public BlockEntityType<E> getBEType() { return blockEntity.get(); }
}