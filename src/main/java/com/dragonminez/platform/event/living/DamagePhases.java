package com.dragonminez.platform.event.living;

import com.dragonminez.platform.event.GameEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import java.util.ArrayDeque;

/** Reentrant ownership of the mitigated damage applied by each native damage call. */
public final class DamagePhases {
    private static final ThreadLocal<ArrayDeque<Frame>> FRAMES = ThreadLocal.withInitial(ArrayDeque::new);
    private static final class Frame {
        final LivingEntity entity;
        final DamageSource source;
        float damage;
        boolean seen;
        Frame(LivingEntity entity, DamageSource source) { this.entity = entity; this.source = source; }
    }
    private DamagePhases() {}
    public static void around(LivingEntity entity, DamageSource source, Runnable nativeDamage) {
        var stack = FRAMES.get();
        Frame frame = new Frame(entity, source);
        stack.push(frame);
        try { nativeDamage.run(); }
        finally {
            if (stack.pop() != frame) throw new IllegalStateException("Damage phase stack corrupted");
            if (stack.isEmpty()) FRAMES.remove();
        }
        if (frame.seen) GameEvents.BUS.post(new LivingDamageEvent.Post(entity, source, frame.damage));
    }
    /** Called after native absorption bookkeeping and before the zero-damage guard. */
    public static float beforeApplied(LivingEntity entity, DamageSource source, float amount) {
        Frame frame = FRAMES.get().peek();
        if (frame == null || frame.entity != entity || frame.source != source) throw new IllegalStateException("Unowned native damage phase");
        var event = GameEvents.BUS.post(new LivingDamageEvent.Pre(entity, source, amount));
        frame.damage = event.getNewDamage();
        frame.seen = true;
        return frame.damage;
    }
}
