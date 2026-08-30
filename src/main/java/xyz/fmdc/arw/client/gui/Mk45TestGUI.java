package xyz.fmdc.arw.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import xyz.fmdc.arw.common.blockentity.AbstractSingleGunBlockEntity;
import xyz.fmdc.arw.network.Mk45PacketTest;
import xyz.fmdc.arw.network.PacketHandler;

public class Mk45TestGUI extends Screen {

    private final BlockPos pos;

    public Mk45TestGUI(BlockPos pos) {
        super(Component.literal("Yaw Control"));
        this.pos = pos;
    }

    @Override
    protected void init() {
        super.init();

        int buttonWidth = 80;
        int buttonHeight = 20;
        int centerX = this.width / 2 - buttonWidth / 2;
        int centerY = this.height / 2;

        // -170度 ボタン
        this.addRenderableWidget(Button.builder(
                Component.literal("-160°"),
                button -> sendAngPacket(-160.0f, 0)
        ).bounds(centerX - 90, centerY - 10, buttonWidth, buttonHeight).build());

        // 0度 ボタン
        this.addRenderableWidget(Button.builder(
                Component.literal("0°"),
                button -> sendAngPacket(0.0f,-20 )
        ).bounds(centerX, centerY - 10, buttonWidth, buttonHeight).build());

        // 175度 ボタン
        this.addRenderableWidget(Button.builder(
                Component.literal("165°"),
                button -> sendAngPacket(165.0f, 10)
        ).bounds(centerX + 90, centerY - 10, buttonWidth, buttonHeight).build());

        // 発射ボタン（中央下部に配置）
        this.addRenderableWidget(Button.builder(
                Component.literal("FIRE").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                button -> {
                    // サーバーへ発射命令パケットを送信
                    PacketHandler.sendToServer(new Mk45PacketTest(this.pos, true));
                    // 押し連打を考慮して画面を閉じないか、閉じるかはお好みで
                }
        ).bounds(centerX - 40, centerY + 30, 80, 20).build());
    }

    private void sendAngPacket(float yaw, float pitch) {
        // サーバーへ角度変更パケットを送信
        PacketHandler.sendToServer(new Mk45PacketTest(this.pos, yaw, pitch, false));
        BlockEntity be = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getBlockEntity(this.pos) : null;
        if (be instanceof AbstractSingleGunBlockEntity gun){
            gun.setTargetYaw(yaw);
            gun.setTargetPitch(pitch);
        }
        // ボタン押下後にGUIを閉じる
        this.onClose();
    }

    private void sendFire(){

    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 背景を薄暗くする（バニラの標準背景 overlay）
        this.renderBackground(guiGraphics);

        // タイトルテキストを中央表示
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 40, 0xFFFFFF);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false; // シングルプレイ時もゲームを停止しない
    }
}