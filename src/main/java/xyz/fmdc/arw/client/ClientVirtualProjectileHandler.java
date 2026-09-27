package xyz.fmdc.arw.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import xyz.fmdc.arw.AntiRaidWeapons;
import xyz.fmdc.arw.common.entity.projectile.ClientDummyProjectileEntity;
import xyz.fmdc.arw.registry.ModEntities;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * クライアント側で受信した仮想飛翔体のパケットを処理し、ダミーEntityのスポーンや着弾エフェクトを実行するハンドラー。
 */
public final class ClientVirtualProjectileHandler {

    private static final Map<UUID, ClientDummyProjectileEntity> ACTIVE_CLIENT_PROJECTILES = new ConcurrentHashMap<>();

    private ClientVirtualProjectileHandler() {}

    /**
     * 飛翔体のスポーンパケット処理
     */
    public static void handleSpawn(
            UUID projectileId,
            byte projectileType,
            Vec3 initialPos,
            Vec3 initialVelocity,
            int maxAgeTicks,
            CompoundTag extraData
    ) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        AntiRaidWeapons.LOGGER.info(
                "[ClientVirtualProjectileHandler] Received S2CSpawnVirtualProjectilePacket: id={}, type={}, pos=({}, {}, {})",
                projectileId,
                ClientDummyProjectileEntity.getTypeName(projectileType),
                initialPos.x, initialPos.y, initialPos.z
        );

        ClientDummyProjectileEntity dummy = new ClientDummyProjectileEntity(
                ModEntities.CLIENT_DUMMY_PROJECTILE.get(),
                level
        );
        dummy.initData(projectileId, projectileType, initialPos, initialVelocity, maxAgeTicks, extraData);

        level.putNonPlayerEntity(dummy.getId(), dummy);
        ACTIVE_CLIENT_PROJECTILES.put(projectileId, dummy);
    }

    /**
     * 飛翔体の着弾・消滅パケット処理
     */
    public static void handleDestroy(UUID projectileId, Vec3 hitPosition, byte destroyReason) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;

        String reasonStr = switch (destroyReason) {
            case 0 -> "HIT";
            case 1 -> "AIR_BURST";
            case 2 -> "TIMEOUT";
            default -> "REASON_" + destroyReason;
        };

        ClientDummyProjectileEntity dummy = ACTIVE_CLIENT_PROJECTILES.remove(projectileId);
        AntiRaidWeapons.LOGGER.info(
                "[ClientVirtualProjectileHandler] Received S2CDestroyVirtualProjectilePacket: id={}, reason={}, hitPos=({}, {}, {}), dummyPresent={}",
                projectileId, reasonStr, hitPosition.x, hitPosition.y, hitPosition.z, (dummy != null)
        );

        if (dummy != null) {
            dummy.onImpact(hitPosition, destroyReason);
        }

        if (level == null) return;

        // 破壊理由に応じた視覚・音響エフェクト
        if (destroyReason == 0) {
            // 着弾爆発エフェクト
            for (int i = 0; i < 20; i++) {
                double rx = (level.random.nextDouble() - 0.5) * 1.5;
                double ry = (level.random.nextDouble() - 0.5) * 1.5;
                double rz = (level.random.nextDouble() - 0.5) * 1.5;
                level.addParticle(
                        ParticleTypes.EXPLOSION,
                        hitPosition.x + rx, hitPosition.y + ry, hitPosition.z + rz,
                        0, 0, 0
                );
            }
            level.playLocalSound(
                    hitPosition.x, hitPosition.y, hitPosition.z,
                    SoundEvents.GENERIC_EXPLODE,
                    SoundSource.BLOCKS,
                    4.0F,
                    (1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F) * 0.9F,
                    false
            );
        } else if (destroyReason == 1) {
            // 空中自爆エフェクト
            level.addParticle(ParticleTypes.EXPLOSION_EMITTER, hitPosition.x, hitPosition.y, hitPosition.z, 0, 0, 0);
            level.playLocalSound(
                    hitPosition.x, hitPosition.y, hitPosition.z,
                    SoundEvents.GENERIC_EXPLODE,
                    SoundSource.BLOCKS,
                    2.0F,
                    1.2F,
                    false
            );
        }
    }

    /**
     * クライアント切断やワールド切り替え時のマップクリア
     */
    public static void clear() {
        ACTIVE_CLIENT_PROJECTILES.clear();
    }
}
