package xyz.fmdc.arw.common.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import xyz.fmdc.arw.AntiRaidWeapons;
import xyz.fmdc.arw.common.blockentity.AbstractARWBlockEntity;

import java.util.UUID;

/**
 * FCS Coreと各種センサー・兵装・機器を接続・リンクするためのアイテム
 */
public class FcsConnectorItem extends Item {

    public FcsConnectorItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockEntity blockEntity = level.getBlockEntity(pos);

        // ARWModのブロック（AbstractARWBlockEntityを持つブロック）のみを対象とする
        if (blockEntity instanceof AbstractARWBlockEntity arwBlockEntity) {
            if (!level.isClientSide) {
                UUID uuid = arwBlockEntity.getUuid();
                BlockState state = level.getBlockState(pos);
                Block block = state.getBlock();
                ResourceLocation blockRegistryName = ForgeRegistries.BLOCKS.getKey(block);
                String blockDisplayName = block.getName().getString();

                AntiRaidWeapons.LOGGER.info("[ARW] Clicked ARW Block: '{}' ({}) at {} | UUID: {}",
                        blockDisplayName, blockRegistryName, pos, uuid);

                if (context.getPlayer() != null) {
                    context.getPlayer().sendSystemMessage(
                            Component.literal(String.format("[ARW] Block: %s (%s)\nUUID: %s",
                                    blockDisplayName, blockRegistryName, uuid))
                    );
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return super.useOn(context);
    }
}
