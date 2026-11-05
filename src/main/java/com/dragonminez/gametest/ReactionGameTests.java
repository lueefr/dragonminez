package com.dragonminez.gametest;

import com.dragonminez.common.config.*;
import com.dragonminez.common.stats.*;
import com.dragonminez.server.events.players.combat.ReactionEvents;
import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class ReactionGameTests {
    @GameTest(template="port_empty")
    public static void hitProbability(GameTestHelper helper) throws Exception {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "ReactionTest"));
        var data = StatsProvider.get(StatsCapability.INSTANCE, player).orElseThrow(IllegalStateException::new);
        var json = JsonParser.parseString("""
            [{"id":"example:reaction","hitChance":{"curve":{"input":{"source":"event.powerRatio"},"interpolation":"smooth","points":[[0.5,0],[1,0.5],[2,1]]}}}]
            """);
        var config = ReactionConfig.parse(json, Set.of()); var rule = config.select(data, "melee");
        helper.assertTrue(config.chance(rule, data, 1, 2) == 0 && config.chance(rule, data, 2, 1) == 1 && config.chance(rule, data, 1, 1) == 0.5, "Hit probability is inverted or misses configured endpoints");
        helper.assertTrue(ReactionConfig.ratio(0, 0) == 1 && ReactionConfig.ratio(1, 0) == Double.MAX_VALUE, "Zero-force ratio is undefined");
        var field = ConfigManager.class.getDeclaredField("runtime"); field.setAccessible(true); var original = field.get(null);
        try {
            field.set(null, RuntimeConfig.parse("{\"reactions\":[{\"id\":\"example:dodge\",\"hitChance\":0,\"staminaCost\":0,\"distance\":1}]}"));
            data.getStatus().setHasCreatedCharacter(true);
            player.setPos(helper.absoluteVec(new Vec3(1.5, 2, 1.5)));
            var attacker = new net.minecraft.world.entity.monster.Zombie(helper.getLevel()); attacker.setPos(player.position().add(-1,0,0));
            helper.setBlock(new BlockPos(2,2,1), Blocks.AIR); helper.setBlock(new BlockPos(2,3,1), Blocks.AIR);
            var destination = ReactionEvents.destination(player, new Vec3(1,0,0));
            helper.assertTrue(destination != null, "Open reaction displacement was rejected");
            helper.setBlock(new BlockPos(2,2,1), Blocks.STONE); helper.setBlock(new BlockPos(2,3,1), Blocks.STONE);
            helper.assertTrue(ReactionEvents.destination(player, new Vec3(1,0,0)) == null, "Dodge passed through a solid wall");
            helper.setBlock(new BlockPos(2,2,1), Blocks.AIR); helper.setBlock(new BlockPos(2,3,1), Blocks.AIR);
            var event = new LivingIncomingDamageEvent(player, new DamageContainer(player.damageSources().mobAttack(attacker), 5));
            Vec3 before = player.position(); ReactionEvents.react(event);
            helper.assertTrue(event.isCanceled() && player.position().distanceToSqr(before) > 0.5, "Zero hit chance did not perform an actual server dodge");
            helper.assertTrue(data.getCooldowns().hasCooldown("RuntimeReaction:example:dodge"), "Dodge did not start cooldown");
            var next = new LivingIncomingDamageEvent(player, new DamageContainer(player.damageSources().mobAttack(attacker), 5)); ReactionEvents.react(next);
            helper.assertTrue(!next.isCanceled(), "Reaction ignored cooldown");
            helper.assertTrue(ReactionConfig.weighted(Map.of("STR", 3d, "PWR", 1d), source -> source.equals("STR") ? 20 : 100) == 40, "Weighted power is not normalized");
            helper.assertTrue(Double.isFinite(ReactionConfig.weighted(Map.of("STR", 1d, "PWR", 1d), source -> Double.MAX_VALUE)), "Weighted power overflowed finite scores");
            field.set(null, RuntimeConfig.parse("{\"reactions\":[{\"id\":\"example:continuous\",\"attacks\":[\"projectile\"],\"powerWeights\":{\"STR\":3,\"PWR\":1},\"hitChance\":0,\"staminaCost\":0,\"cooldownTicks\":0,\"reactionTicks\":10}]}"));
            var arrow = new net.minecraft.world.entity.projectile.Arrow(net.minecraft.world.entity.EntityType.ARROW, helper.getLevel()); arrow.setOwner(attacker);
            var source = new net.minecraft.world.damagesource.DamageSource(helper.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(net.minecraft.world.damagesource.DamageTypes.ARROW), arrow, attacker);
            player.setPos(before);
            var first = new LivingIncomingDamageEvent(player, new DamageContainer(source, 5)); ReactionEvents.react(first);
            helper.assertTrue(first.isCanceled(), "First projectile reaction failed");
            var repeat = new LivingIncomingDamageEvent(player, new DamageContainer(source, 5)); ReactionEvents.react(repeat);
            helper.assertTrue(!repeat.isCanceled(), "Continuous projectile retried within its reaction interval");
            var alternate = new net.minecraft.world.entity.projectile.Arrow(net.minecraft.world.entity.EntityType.ARROW, helper.getLevel()); alternate.setOwner(attacker);
            var otherSource = new net.minecraft.world.damagesource.DamageSource(source.typeHolder(), alternate, attacker);
            player.setPos(before);
            var independent = new LivingIncomingDamageEvent(player, new DamageContainer(otherSource, 5)); ReactionEvents.react(independent);
            helper.assertTrue(independent.isCanceled(), "One projectile suppressed another projectile's reaction");
            helper.succeed();
        } finally { field.set(null, original); player.discard(); }
    }
}
