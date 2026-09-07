package xyz.fmdc.arw.client.util;

import net.minecraft.resources.ResourceLocation;
import xyz.fmdc.arw.client.renderer.GenericFastGlbRenderer;
import xyz.fmdc.arw.common.blockentity.weapon.launcher.MissileSlot;

import java.util.List;

public interface IMissileLauncherBlockEntity {
    /**
     * ランチャー本体のGLBモデルIDを取得
     */
    ResourceLocation getLauncherModelId();

    /**
     * 現在のYaw/Pitch角度を取得 (度数法)
     */
    float getRenderTargetYaw(float partialTick);
    float getRenderTargetPitch(float partialTick);

    /**
     * 現在装填されているミサイルスロットの一覧を取得
     */
    List<MissileSlot> getLoadedMissileSlots();
    List<GenericFastGlbRenderer.ActiveAnimation> getActiveAnimations(float partialTick);

}
