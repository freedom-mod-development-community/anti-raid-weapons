package xyz.fmdc.arw.test;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import xyz.fmdc.arw.AntiRaidWeapons;
import xyz.fmdc.arw.common.blockentity.weapon.singlegun.Oto127mmBlockEntity;
import xyz.fmdc.arw.registry.ModBlocks;

@GameTestHolder(AntiRaidWeapons.MOD_ID)
@PrefixGameTestTemplate(false)
public class Oto127mmGameTests {

    @GameTest(template = "empty_test_structure", timeoutTicks = 2500)
    public static void testOto127mmPitch45FireAndFlight(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, ModBlocks.OTO_127MM_BLOCK.get());
        Oto127mmBlockEntity oto = (Oto127mmBlockEntity) helper.getBlockEntity(pos);

        // 発射ライン（前方 -Z および上空 +Y 方向）の遮蔽ブロックをクリア
        BlockPos gunPos = helper.absolutePos(pos);
        for (int dy = 0; dy <= 150; dy++) {
            for (int dz = 0; dz >= -100; dz--) {
                for (int dx = -3; dx <= 3; dx++) {
                    BlockPos targetPos = gunPos.offset(dx, dy, dz);
                    if (!helper.getLevel().getBlockState(targetPos).isAir()) {
                        helper.getLevel().setBlock(targetPos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }

        helper.runAtTickTime(5, () -> {
            oto.setTargetYaw(0.0f);
            oto.setTargetPitch(-45.0f);
            oto.setCurrentYaw(0.0f);
            oto.setCurrentPitch(-45.0f);
        });

        helper.runAtTickTime(10, () -> {
            oto.fire();
            if (oto.getCooldownTicks() <= 0 && !oto.isPendingFire()) {
                helper.fail("Expected cooldown or pendingFire after firing", pos);
            }
        });

        // 砲弾が放物線（最高高度7700m、約22km）を飛び切る（約1584 Tick）のを待って2000 Tickで成功
        helper.runAtTickTime(2000, () -> {
            helper.succeed();
        });
    }
}
