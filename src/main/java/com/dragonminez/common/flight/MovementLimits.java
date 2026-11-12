package com.dragonminez.common.flight;

import com.dragonminez.Reference;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Shared movement limits: the server owns preferences; both sides predict the same physics. */
@EventBusSubscriber(modid = Reference.MOD_ID)
public final class MovementLimits {
    private static final ResourceLocation SPRINT_CAP = ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "sprint_limit");
    private MovementLimits() {}

    public static double jumpScale(int percent) { return Math.sqrt(Math.max(5, Math.min(100, percent)) / 100.0); }

    /** Discrete vanilla gravity plus the existing held-jump skill boosts. */
    public static double jumpHeight(int level, double gravity) {
        double velocity = 0.42 + (1.0 + level * 0.1 - 1.25) * 0.18 * gravity;
        double height = 0;
        for (int tick = 0; tick < 1000 && velocity > 0; tick++) {
            height += velocity;
            velocity = (velocity - 0.08) * 0.98;
            if (tick < level) velocity += 0.11;
        }
        return Math.max(0, height);
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            var attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (attribute == null) return;
            boolean enabled = data.getStatus().isHasCreatedCharacter() && player.isSprinting()
                    && data.getSkills().isSkillActive("sprint") && !data.getSkills().isSkillActive("fly");
            double amount = enabled ? data.getResources().getSprintLimit() / 100.0 - 1.0 : 0;
            var previous = attribute.getModifier(SPRINT_CAP);
            if (previous != null && previous.amount() == amount) return;
            if (previous != null) attribute.removeModifier(SPRINT_CAP);
            if (amount != 0) attribute.addTransientModifier(new AttributeModifier(SPRINT_CAP, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        });
    }

    @SubscribeEvent
    public static void jump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
            if (!data.getStatus().isHasCreatedCharacter() || !data.getSkills().isSkillActive("jump")
                    || data.getSkills().isSkillActive("fly")) return;
            var motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x, motion.y * jumpScale(data.getResources().getJumpLimit()), motion.z);
        });
    }
}
