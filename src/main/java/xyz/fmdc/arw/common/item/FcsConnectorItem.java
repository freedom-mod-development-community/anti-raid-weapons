package xyz.fmdc.arw.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.fmdc.arw.AntiRaidWeapons;
import xyz.fmdc.arw.api.fcs.IFcsNetworkNode;
import xyz.fmdc.arw.common.blockentity.AbstractARWBlockEntity;
import xyz.fmdc.arw.common.blockentity.fcs.AbstractFcsCoreBlockEntity;

import java.util.List;
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
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        // 1. FCS Coreブロックがクリックされた場合 -> リンク先としてCoreのUUIDおよび座標を記憶
        if (blockEntity instanceof AbstractFcsCoreBlockEntity fcsCore) {
            if (!level.isClientSide) {
                UUID coreUuid = fcsCore.getUuid();
                CompoundTag tag = stack.getOrCreateTag();
                tag.putUUID("CoreUUID", coreUuid);
                tag.put("CorePos", NbtUtils.writeBlockPos(pos));
                tag.putString("Dimension", level.dimension().location().toString());

                AntiRaidWeapons.LOGGER.info("[ARW] Selected FCS Core UUID: {} at {}", coreUuid, pos);

                if (player != null) {
                    player.sendSystemMessage(
                            Component.translatable("message.arw.fcs_connector.core_selected", coreUuid.toString())
                                    .withStyle(ChatFormatting.GREEN)
                    );
                    level.playSound(null, pos, SoundEvents.NOTE_BLOCK_PLING.get(), SoundSource.PLAYERS, 1.0F, 1.5F);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // 2. 接続対象の機器（ARW BlockEntity / IFcsNetworkNode 等）がクリックされた場合
        if (blockEntity instanceof AbstractARWBlockEntity || blockEntity instanceof IFcsNetworkNode) {
            if (!level.isClientSide) {
                CompoundTag tag = stack.getTag();
                if (tag == null || !tag.hasUUID("CoreUUID") || !tag.contains("CorePos")) {
                    if (player != null) {
                        player.sendSystemMessage(
                                Component.translatable("message.arw.fcs_connector.no_core_linked")
                                        .withStyle(ChatFormatting.RED)
                        );
                    }
                    return InteractionResult.sidedSuccess(false);
                }

                UUID coreUuid = tag.getUUID("CoreUUID");
                BlockPos corePos = NbtUtils.readBlockPos(tag.getCompound("CorePos"));
                String dimension = tag.getString("Dimension");

                if (!dimension.isEmpty() && !dimension.equals(level.dimension().location().toString())) {
                    if (player != null) {
                        player.sendSystemMessage(
                                Component.translatable("message.arw.fcs_connector.different_dimension")
                                        .withStyle(ChatFormatting.RED)
                        );
                    }
                    return InteractionResult.sidedSuccess(false);
                }

                BlockEntity targetCoreBE = level.getBlockEntity(corePos);
                if (!(targetCoreBE instanceof AbstractFcsCoreBlockEntity fcsCore) || !fcsCore.getUuid().equals(coreUuid)) {
                    if (player != null) {
                        player.sendSystemMessage(
                                Component.translatable("message.arw.fcs_connector.core_not_found", coreUuid.toString())
                                        .withStyle(ChatFormatting.RED)
                        );
                    }
                    return InteractionResult.sidedSuccess(false);
                }

                UUID targetUuid = (blockEntity instanceof AbstractARWBlockEntity arw) ? arw.getUuid() : ((IFcsNetworkNode) blockEntity).getNetworkId();
                UUID linkedCoreUuid = null;
                if (blockEntity instanceof IFcsNetworkNode networkNode) {
                    linkedCoreUuid = networkNode.getLinkedFcsCoreUuid();
                } else if (blockEntity instanceof AbstractARWBlockEntity arw) {
                    linkedCoreUuid = arw.getLinkedFcsCoreUuid();
                }

                BlockState state = level.getBlockState(pos);
                Block block = state.getBlock();
                String blockDisplayName = block.getName().getString();

                if (player != null && player.isShiftKeyDown()) {
                    // スニーク右クリックで登録解除
                    // 他のFCSコアにリンクされている機器を解除しようとした場合は拒否
                    if (linkedCoreUuid != null && !linkedCoreUuid.equals(coreUuid)) {
                        player.sendSystemMessage(
                                Component.translatable("message.arw.fcs_connector.cannot_unlink_other_core", linkedCoreUuid.toString())
                                        .withStyle(ChatFormatting.RED)
                        );
                        return InteractionResult.sidedSuccess(false);
                    }

                    if (fcsCore.isDeviceRegistered(targetUuid) || (linkedCoreUuid != null && linkedCoreUuid.equals(coreUuid))) {
                        fcsCore.unregisterDevice(targetUuid);
                        player.sendSystemMessage(
                                Component.translatable("message.arw.fcs_connector.unregistered", blockDisplayName, targetUuid.toString())
                                        .withStyle(ChatFormatting.YELLOW)
                        );
                        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BASS.get(), SoundSource.PLAYERS, 1.0F, 0.8F);
                    } else {
                        player.sendSystemMessage(
                                Component.translatable("message.arw.fcs_connector.not_registered", blockDisplayName, targetUuid.toString())
                                        .withStyle(ChatFormatting.RED)
                        );
                    }
                } else {
                    // 通常右クリックで登録
                    // 既に別のFCSコアにリンクされている場合は上書き禁止
                    if (linkedCoreUuid != null && !linkedCoreUuid.equals(coreUuid)) {
                        player.sendSystemMessage(
                                Component.translatable("message.arw.fcs_connector.already_linked_to_other_core", linkedCoreUuid.toString())
                                        .withStyle(ChatFormatting.RED)
                        );
                        return InteractionResult.sidedSuccess(false);
                    }

                    if (fcsCore.isDeviceRegistered(targetUuid)) {
                        player.sendSystemMessage(
                                Component.translatable("message.arw.fcs_connector.already_registered", blockDisplayName, targetUuid.toString())
                                        .withStyle(ChatFormatting.YELLOW)
                        );
                    } else {
                        boolean success = fcsCore.registerDevice(blockEntity);
                        if (success) {
                            AntiRaidWeapons.LOGGER.info("[ARW] Registered device '{}' (UUID: {}) at {} to FCS Core (UUID: {})",
                                    blockDisplayName, targetUuid, pos, coreUuid);
                            player.sendSystemMessage(
                                    Component.translatable("message.arw.fcs_connector.registered", blockDisplayName, targetUuid.toString())
                                            .withStyle(ChatFormatting.GREEN)
                            );
                            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.get(), SoundSource.PLAYERS, 1.0F, 1.2F);
                        } else {
                            player.sendSystemMessage(
                                    Component.translatable("message.arw.fcs_connector.register_failed", blockDisplayName)
                                            .withStyle(ChatFormatting.RED)
                            );
                        }
                    }
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return super.useOn(context);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        if (stack.hasTag() && stack.getTag().hasUUID("CoreUUID")) {
            UUID coreUuid = stack.getTag().getUUID("CoreUUID");
            tooltip.add(Component.translatable("tooltip.arw.fcs_connector.linked_core_uuid", coreUuid.toString())
                    .withStyle(ChatFormatting.AQUA));
        } else {
            tooltip.add(Component.translatable("tooltip.arw.fcs_connector.no_core")
                    .withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
