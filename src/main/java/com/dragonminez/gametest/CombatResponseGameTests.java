package com.dragonminez.gametest;

import com.dragonminez.common.config.*;
import com.dragonminez.common.stats.*;
import com.dragonminez.server.events.players.combat.ReactionEvents;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("dragonminez")
@PrefixGameTestTemplate(false)
public final class CombatResponseGameTests {
    @GameTest(template = "port_empty")
    public static void defenseResponses(GameTestHelper helper) throws Exception {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "CombatResponses"));
        var data = StatsProvider.get(StatsCapability.INSTANCE, player).orElseThrow(IllegalStateException::new);
        var field = ConfigManager.class.getDeclaredField("runtime"); field.setAccessible(true); var original = field.get(null);
        try {
            data.getStatus().setHasCreatedCharacter(true); player.setPos(helper.absoluteVec(new Vec3(1.5, 2, 1.5)));
            var attacker = new Zombie(helper.getLevel()); attacker.setPos(player.position().add(1, 0, 0));
            var block = RuntimeConfig.parse("""
                {"meters":{"example:blocked":{"scope":"character"}},
                 "rules":[{"event":"dmz:blocked","actions":[{"meter":"example:blocked","operation":"add","value":{"source":"event.blockedDamage"}}]}],
                 "reactions":[{"id":"example:block","response":"block","hitChance":0,"damageFactor":0.25,"staminaCost":0,"cooldownTicks":0}]}
                """);
            field.set(null, block); Vec3 before = player.position();
            var event = new LivingIncomingDamageEvent(player, new DamageContainer(player.damageSources().mobAttack(attacker), 8));
            ReactionEvents.react(event);
            helper.assertTrue(event.getAmount() == 8 && !event.isCanceled(), "Block ran before native damage calculation");
            ReactionEvents.defend(event);
            helper.assertTrue(event.getAmount() == 2 && !event.isCanceled(), "Block did not preserve configured residual damage");
            helper.assertTrue(player.position().equals(before), "Automatic block displaced the player");
            helper.assertTrue(data.getRuntime().value("example:blocked") == 6, "Blocked-damage event lost measured inputs");
            field.set(null, RuntimeConfig.parse("{\"reactions\":[{\"id\":\"example:counter\",\"response\":\"counter\",\"hitChance\":0,\"counterFactor\":0.5,\"counterRange\":3,\"staminaCost\":0,\"cooldownTicks\":10}]}"));
            float health = attacker.getHealth();
            var counter = new LivingIncomingDamageEvent(player, new DamageContainer(player.damageSources().mobAttack(attacker), 8));
            ReactionEvents.defend(counter);
            helper.assertTrue(counter.isCanceled() && attacker.getHealth() < health, "Counter did not block and retaliate through native damage");
            var repeated = new LivingIncomingDamageEvent(player, new DamageContainer(player.damageSources().mobAttack(attacker), 8)); ReactionEvents.defend(repeated);
            helper.assertTrue(!repeated.isCanceled(), "Counter ignored cooldown");
            data.getCooldowns().clearCooldowns(); attacker.setPos(player.position().add(10, 0, 0));
            var distant = new LivingIncomingDamageEvent(player, new DamageContainer(player.damageSources().mobAttack(attacker), 8)); ReactionEvents.defend(distant);
            helper.assertTrue(!distant.isCanceled(), "Counter triggered outside configured range");
            field.set(null, RuntimeConfig.parse("{\"reactions\":[{\"id\":\"example:recursive\",\"response\":\"counter\",\"hitChance\":0,\"staminaCost\":0,\"cooldownTicks\":0}]}"));
            var calls = new java.util.concurrent.atomic.AtomicInteger();
            var responsive = new Zombie(helper.getLevel()) {
                @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
                    if (calls.incrementAndGet() > 2) throw new AssertionError("Recursive counter chain");
                    var nested = new LivingIncomingDamageEvent(player, new DamageContainer(player.damageSources().mobAttack(this), 8));
                    ReactionEvents.defend(nested);
                    helper.assertTrue(!nested.isCanceled(), "Nested retaliation triggered another configured response");
                    return super.hurt(source, amount);
                }
            };
            responsive.setPos(player.position().add(1, 0, 0));
            var recursive = new LivingIncomingDamageEvent(player, new DamageContainer(player.damageSources().mobAttack(responsive), 8)); ReactionEvents.defend(recursive);
            helper.assertTrue(recursive.isCanceled() && calls.get() == 1, "Counter guard failed with no cooldown");
            helper.succeed();
        } finally { field.set(null, original); player.discard(); }
    }
}
