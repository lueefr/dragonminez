package com.dragonminez.client.render.cloth;

import com.dragonminez.client.animation.IPlayerAnimatable;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import java.util.WeakHashMap;

/** Per-wearer articulated cloth springs, integrated at 60 Hz in game time. */
public final class CapeMotion {
    private static final WeakHashMap<Entity, State[]> STATES = new WeakHashMap<>();
    private CapeMotion() {}
    private static final class State {
        double time = -1, pending;
        final float[] angle = {.18f, 0, 0}, speed = new float[3];
        float side, sway;
        Vec3 position;
    }
    public static float[] sample(Entity entity, float partial, boolean heavy, int segment) {
        if (!(entity instanceof LivingEntity living)) return new float[]{0, 0};
        State state = STATES.computeIfAbsent(entity, key -> new State[]{new State(), new State()})[heavy ? 1 : 0];
        double now = entity.level().getGameTime() + partial;
        if (state.time < 0 || now < state.time || now-state.time > 10 || state.position != null && state.position.distanceToSqr(entity.position()) > 256) {
            state.time = now;
            state.pending = 0;
            java.util.Arrays.fill(state.speed, 0);
        }
        double elapsed = (now-state.time)/20;
        state.time = now;
        state.position = entity.position();
        Vec3 movement = entity.getDeltaMovement();
        Vec3 forward = Vec3.directionFromRotation(0, living.yBodyRot);
        float wind = Mth.clamp((float)(entity.getY()-96)/160, 0, 1);
        boolean power = entity instanceof IPlayerAnimatable player && !player.dragonminez$getCurrentKiAnimation().isEmpty();
        float target = Mth.clamp(.16f + (float)Math.max(0, movement.dot(forward))*1.6f + (float)Math.abs(movement.y)*.35f + wind*.18f + (power ? .75f : 0), .08f, 1.4f);
        if (entity.isInWater()) target *= .4f;
        float side = Mth.clamp((float)movement.dot(forward.cross(new Vec3(0,1,0))), -.5f,.5f) + wind * .12f * (float)forward.x;
        state.pending += elapsed;
        final float step = 1f/60;
        while (state.pending >= step) {
            for (int i=0;i<3;i++) {
                float flutter = Mth.sin((float)now*.15f-i*.8f) * (.025f+wind*.035f);
                float rest = i==0 ? target : (state.angle[i-1]-.16f)*.25f+flutter;
                float stiffness = heavy ? 34 : 48;
                state.speed[i] += ((rest-state.angle[i])*stiffness-state.speed[i]*(heavy?12:9))*step;
                state.angle[i] = Mth.clamp(state.angle[i]+state.speed[i]*step, i==0?.04f:-.25f, i==0?1.45f:.45f);
            }
            state.sway += (side-state.side)*24*step-state.sway*8*step;
            state.side = Mth.clamp(state.side+state.sway*step,-.35f,.35f);
            float sum=0;
            for (int i=0;i<3;i++) { state.angle[i]=Math.max(state.angle[i],.025f-sum); sum+=state.angle[i]; }
            state.pending -= step;
        }
        return rotation(state.angle, state.side, segment);
    }
    /** Gecko's cape bones use negative X for motion away from the wearer's back. */
    public static float[] rotation(float[] angles, float side, int segment) {
        // Keep the shoulder seam attached. Rolling that seam swings its wide corners
        // through the torso even when the centre of the cape points backwards.
        float roll = segment == 0 ? 0 : Mth.clamp(side * segment / 3, -.12f, .12f);
        return new float[]{-angles[segment], roll};
    }

}
