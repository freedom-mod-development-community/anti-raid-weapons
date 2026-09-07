package xyz.fmdc.arw.common.blockentity.weapon.launcher;

import net.minecraft.resources.ResourceLocation;

/**
 * ランチャー上のミサイル装填スロット情報
 * @param slotBoneName GLTFモデル側のダミーボーン名 (例: "mount_point_1", "mount_point_2")
 * @param missileModelId 描画するミサイルのモデルID (nullの場合は未装填/非表示)
 */
public record MissileSlot(String slotBoneName, ResourceLocation missileModelId) {
    public boolean isLoaded() {
        return missileModelId != null;
    }
}
