package com.dragonminez.common.init.entities.ai;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import java.util.EnumSet;

/** Continuous attention within five blocks, without looking through walls. */
public final class PlayerLookGoal extends Goal {
    private final Mob mob;
    private Player player;

    public PlayerLookGoal(Mob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.LOOK));
    }

    public boolean canUse() {
        player = mob.level().getEntitiesOfClass(Player.class, mob.getBoundingBox().inflate(5), this::visible)
                .stream().min(java.util.Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
        return player != null;
    }

    private boolean visible(Player target) {
        return target.isAlive() && !target.isSpectator() && mob.distanceToSqr(target) <= 25
                && mob.getSensing().hasLineOfSight(target);
    }

    public boolean canContinueToUse() { return player != null && visible(player); }
    public boolean requiresUpdateEveryTick() { return true; }
    public void tick() { mob.getLookControl().setLookAt(player, 30, mob.getMaxHeadXRot()); }
    public void stop() { player = null; }
}
