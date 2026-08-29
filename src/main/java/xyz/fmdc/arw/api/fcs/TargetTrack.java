package xyz.fmdc.arw.api.fcs;

import net.minecraft.world.phys.Vec3;
import java.util.UUID;

public record TargetTrack(
        UUID targetEntityUniqueId,
        Vec3 position,
        Vec3 velocity,
        long lastUpdatedTick,
        boolean isLocked
) {}
