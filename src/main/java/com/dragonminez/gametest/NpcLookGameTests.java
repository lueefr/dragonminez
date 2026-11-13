package com.dragonminez.gametest;

import com.dragonminez.common.init.entities.ai.PlayerLookGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class NpcLookGameTests {
    @GameTest(template="port_empty")
    public static void visiblePlayer(GameTestHelper helper) {
        var mob = helper.spawn(EntityType.VILLAGER, new BlockPos(1, 2, 1));
        mob.setNoAi(true); mob.setNoGravity(true);
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(helper.getLevel(), new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "NpcLook"));
        player.setNoGravity(true);
        player.setPos(mob.getX() + 4, mob.getY(), mob.getZ());
        helper.getLevel().addNewPlayer(player);
        // Clear the test structure's boundary blocks from the full sight corridor.
        for (int x = 0; x <= 6; x++) for (int y = 0; y <= 3; y++) for (int z = -1; z <= 1; z++)
            helper.getLevel().setBlockAndUpdate(mob.blockPosition().offset(x,y,z), Blocks.AIR.defaultBlockState());
        mob.getSensing().tick();
        var goal = new PlayerLookGoal(mob);
        helper.assertTrue(goal.canUse(), "Nearby player ignored: alive=" + player.isAlive() + " spectator=" + player.isSpectator() + " distance=" + mob.distanceToSqr(player) + " visible=" + mob.getSensing().hasLineOfSight(player) + " count=" + helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.player.Player.class, mob.getBoundingBox().inflate(5)).size());
        for (int i = 0; i < 100; i++) helper.assertTrue(goal.canContinueToUse(), "Attention expired while the player was visible");
        player.setPos(mob.getX() + 6, mob.getY(), mob.getZ());
        helper.assertTrue(!goal.canContinueToUse(), "NPC kept following beyond five blocks");
        player.setPos(mob.getX() + 4, mob.getY(), mob.getZ());
        var wall = mob.blockPosition().east(2);
        for (int y = -1; y <= 3; y++) for (int z = -1; z <= 1; z++)
            helper.getLevel().setBlockAndUpdate(wall.offset(0,y,z), Blocks.STONE.defaultBlockState());
        mob.getSensing().tick();
        helper.assertTrue(!goal.canUse(), "NPC looked through a solid wall");
        player.discard(); mob.discard(); helper.succeed();
    }
    @GameTest(template="port_empty")
    public static void demonResidentsUseContinuousAttention(GameTestHelper helper) throws Exception {
        var field = net.minecraft.world.entity.Mob.class.getDeclaredField("goalSelector");
        field.setAccessible(true);
        for (var type : java.util.List.of(com.dragonminez.common.init.MainEntities.DEMON_VILLAGER.get(),
                com.dragonminez.common.init.MainEntities.TIME_PATROLLER.get())) {
            var mob = type.create(helper.getLevel());
            try {
                var goals = (net.minecraft.world.entity.ai.goal.GoalSelector) field.get(mob);
                helper.assertTrue(mob != null && goals.getAvailableGoals().stream()
                        .anyMatch(goal -> goal.getGoal() instanceof PlayerLookGoal),
                        "Demon resident lacks continuous, occlusion-aware attention: " + type);
            } finally { if (mob != null) mob.discard(); }
        }
        helper.succeed();
    }

}
