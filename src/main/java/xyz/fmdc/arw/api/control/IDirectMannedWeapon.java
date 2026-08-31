package xyz.fmdc.arw.api.control;

import net.minecraft.world.entity.player.Player;

/**
 * プレイヤーが座席や砲塔に「直接乗り込んで」手動操作する兵装用インターフェース
 */
public interface IDirectMannedWeapon {
    boolean isManned();
    Player getControllingPlayer();
    void mountPlayer(Player player);
    void dismountPlayer();
    void handleMannedInput(float yawDelta, float pitchDelta, boolean triggerFire);
}