package xyz.fmdc.arw.api.control;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public interface IRemoteControllableWeapon {
    Vec3 getCameraPosition();
    boolean isBeingRemoteControlled();
    void startRemoteControl(Player player);
    void stopRemoteControl(Player player);
    void handleRemoteInput(float yawInput, float pitchInput, boolean triggerFire);
}
