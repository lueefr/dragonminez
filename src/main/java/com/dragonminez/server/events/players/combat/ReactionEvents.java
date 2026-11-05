package com.dragonminez.server.events.players.combat;

import com.dragonminez.Reference;
import com.dragonminez.common.config.*;
import com.dragonminez.common.stats.*;
import com.dragonminez.common.init.entities.IBattlePower;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import java.util.Map;

/** A successful reaction requires a valid displacement before damage is cancelled. */
@EventBusSubscriber(modid = Reference.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class ReactionEvents {
    private ReactionEvents() {}
    public static String category(DamageSource source) {
        return source.getDirectEntity() != source.getEntity() ? "projectile" : "melee";
    }
    public static boolean handles(StatsData data, DamageSource source) {
        return source.getEntity() instanceof LivingEntity && ConfigManager.getRuntime().reactions().select(data, category(source)) != null;
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void react(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getSource().getEntity() instanceof LivingEntity attacker) || attacker == player) return;
        var data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
        if (data == null || !data.getStatus().isHasCreatedCharacter() || data.getStatus().isStunned() || player.isSpectator()) return;
        var rule = ConfigManager.getRuntime().reactions().select(data, category(event.getSource()));
        if (rule == null || data.getCooldowns().hasCooldown("RuntimeReaction:" + rule.id())) return;
        double energy = data.getMaxEnergy() * rule.energy(), stamina = data.getMaxStamina() * rule.stamina();
        if (data.getResources().getCurrentEnergy() < energy || data.getResources().getCurrentStamina() < stamina) return;
        double attackerPower = power(attacker, rule.power()), defenderPower = power(player, rule.power());
        double chance = ConfigManager.getRuntime().reactions().chance(rule, data, attackerPower, defenderPower);
        if (player.getRandom().nextDouble() < chance) return;
        Vec3 away = player.position().subtract(attacker.position()).multiply(1, 0, 1);
        if (away.lengthSqr() < 0.0001) away = player.getLookAngle().multiply(1, 0, 1);
        if (away.lengthSqr() < 0.0001) return;
        Vec3 offset = away.normalize().scale(rule.distance());
        Vec3 destination = destination(player, offset);
        if (destination == null) destination = destination(player, new Vec3(-offset.z, 0, offset.x));
        if (destination == null) destination = destination(player, new Vec3(offset.z, 0, -offset.x));
        if (destination == null) return;
        player.setPos(destination);
        player.connection.teleport(destination.x, destination.y, destination.z, player.getYRot(), player.getXRot());
        data.getResources().removeEnergy((float) energy, false); data.getResources().removeStamina((float) stamina);
        data.getCooldowns().setCooldown("RuntimeReaction:" + rule.id(), rule.cooldown());
        event.setCanceled(true);
        com.dragonminez.common.network.NetworkHandler.sendToTrackingEntityAndSelf(new com.dragonminez.common.network.S2C.TriggerAnimationS2C(player.getUUID(), com.dragonminez.common.network.S2C.TriggerAnimationS2C.AnimationType.EVASION, 1), player);
        data.getRuntime().contact(data);
        data.getRuntime().dispatch(data, "dmz:evaded", Map.of());
    }
    public static Vec3 destination(ServerPlayer player, Vec3 offset) {
        var target = player.position().add(offset);
        if (!player.level().hasChunkAt(net.minecraft.core.BlockPos.containing(target)) || !player.level().getWorldBorder().isWithinBounds(net.minecraft.core.BlockPos.containing(target))) return null;
        if (!player.level().noCollision(player, player.getBoundingBox().expandTowards(offset))) return null;
        var hit = player.level().clip(new ClipContext(player.getEyePosition(), player.getEyePosition().add(offset), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? target : null;
    }
    private static double power(LivingEntity entity, String source) {
        if (entity instanceof net.minecraft.world.entity.player.Player player) {
            var data = StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
            if (data != null && data.getStatus().isHasCreatedCharacter()) return Math.max(0, switch (source) {
                case "STR" -> data.getStats().getStrength() * data.getTotalMultiplier("STR");
                case "PWR" -> data.getStats().getKiPower() * data.getTotalMultiplier("PWR");
                case "SPEED" -> data.getSpeed();
                default -> data.getBattlePower();
            });
        }
        return entity instanceof IBattlePower battle ? Math.max(0, battle.getBattlePower()) : Math.max(0, entity.getMaxHealth());
    }
}
