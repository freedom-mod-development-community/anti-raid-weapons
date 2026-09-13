package xyz.fmdc.arw.api;

import net.minecraft.world.entity.Entity;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RadarTargetManager {
    public static final RadarTargetManager INSTANCE = new RadarTargetManager();

    // 探知対象となるEntityのセット
    private final Set<Entity> globalTargets = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public void registerEntity(Entity entity) {
        // ミサイルや特定のインターフェースを持つEntityのみを登録対象にする
        if (isRadarDetectable(entity)) {
            globalTargets.add(entity);
        }
    }

    public void unregisterEntity(Entity entity) {
        globalTargets.remove(entity);
    }

    public Set<Entity> getGlobalTargets() {
        return globalTargets;
    }

    private boolean isRadarDetectable(Entity entity) {
        // 判定条件 (例: LivingEntity, または自作のMissileEntityなど)
        return true;
        //return entity instanceof IMissile || entity instanceof Player;
    }
}
