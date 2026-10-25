package com.dragonminez.gametest;

import com.dragonminez.common.util.GroundProjection;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class GroundProjectionGameTests {
    @GameTest(template = "port_vertical_empty")
    public static void followsFeetAcrossSurfacesAndStopsAboveTwoBlocks(GameTestHelper helper) {
        BlockPos block = helper.absolutePos(new BlockPos(1, 2, 1));
        var level = helper.getLevel();
        level.setBlockAndUpdate(block, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(block.east(), Blocks.STONE_SLAB.defaultBlockState());
        var entity = EntityType.ARMOR_STAND.create(level);
        Vec3 ground = Vec3.atBottomCenterOf(block.above());
        for (double height : new double[]{0, .5, 1.99, 2}) {
            var result = GroundProjection.below(entity, ground.add(0, height, 0));
            helper.assertTrue(result.isPresent() && result.get().distanceTo(ground) < 1e-6,
                    "Ground projection moved vertically or disappeared within two blocks: " + height + " feet=" + ground + " hit=" + result + " block=" + level.getBlockState(block));
        }
        helper.assertTrue(GroundProjection.below(entity, ground.add(0, 2.01, 0)).isEmpty(), "Effect exceeded two blocks");
        Vec3 slab = ground.add(1, -.5, 0);
        var moved = GroundProjection.below(entity, slab.add(0, 1, 0));
        helper.assertTrue(moved.isPresent() && moved.get().distanceTo(slab) < 1e-6, "Projection did not follow X/Z onto the slab surface");
        helper.assertTrue(GroundProjection.below(entity, ground.add(0, 1, -1)).isEmpty(), "Projection survived leaving the ground below it");
        helper.succeed();
    }
}
