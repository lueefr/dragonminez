package com.dragonminez.client.compat;

import com.dragonminez.Reference;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.init.entities.ki.AbstractKiProjectile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.lang.reflect.Method;
import java.util.Optional;

/** Optional Wakes 1.21.1 bridge. Reflection keeps the mod entirely optional. */
@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class WakesBridge {
    private static boolean checked;
    private static Method instance, trail, insert;

    private static void resolve() {
        if (checked) return;
        checked = true;
        if (!ModList.get().isLoaded("wakes")) return;
        try {
            Class<?> handler = Class.forName("com.leclowndu93150.wakes.simulation.WakeHandler");
            Class<?> node = Class.forName("com.leclowndu93150.wakes.simulation.WakeNode");
            Class<?> factory = Class.forName("com.leclowndu93150.wakes.simulation.WakeNode$Factory");
            instance = handler.getMethod("getInstance", Level.class);
            insert = handler.getMethod("insert", node);
            trail = factory.getMethod("nodeTrail", double.class, double.class, double.class, double.class,
                    int.class, float.class, double.class);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            instance = null;
            org.slf4j.LoggerFactory.getLogger("DragonMineZ").warn("Unsupported Wakes API; water effects disabled", error);
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) return;
        resolve();
        if (instance == null) return;
        try {
            Object handler = ((Optional<?>) instance.invoke(null, mc.level)).orElse(null);
            if (handler == null) return;
            for (Entity entity : mc.level.entitiesForRendering()) {
                boolean flying = entity instanceof AbstractClientPlayer player && !player.onGround()
                        && (player.getAbilities().flying || StatsProvider.get(StatsCapability.INSTANCE, player).map(stats -> {
                            var skill = stats.getSkills().getSkill("fly");
                            return skill != null && skill.isActive();
                        }).orElse(false));
                if (!(entity instanceof AbstractKiProjectile) && !flying) continue;
                if (entity.distanceToSqr(mc.player) > 96.0 * 96.0) continue;
                ripple(entity, handler);
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            instance = null;
            org.slf4j.LoggerFactory.getLogger("DragonMineZ").warn("Wakes integration failed; disabled for this session", error);
        }
    }

    private static void ripple(Entity entity, Object handler) throws ReflectiveOperationException {
        BlockPos origin = entity.blockPosition();
        for (int depth = 0; depth <= 4; depth++) {
            BlockPos pos = origin.below(depth);
            var fluid = entity.level().getFluidState(pos);
            if (!fluid.is(FluidTags.WATER)) {
                if (!entity.level().getBlockState(pos).getCollisionShape(entity.level(), pos).isEmpty()) return;
                continue;
            }
            if (!fluid.isSource()) continue;
            if (!entity.level().getFluidState(pos.above()).isEmpty()) continue;
            double surface = pos.getY() + fluid.getHeight(entity.level(), pos);
            double gap = entity.getY() - surface;
            if (gap < -0.5 || gap > 4.0) return;
            // Wakes stores integer simulation amplitudes, not normalized alpha values.
            // Even the minimum hover speed must survive its (int)(strength * velocity) conversion.
            float strength = (float) (40.0 * (1.0 - Math.max(0.0, gap) / 5.0));
            double speed = Math.min(2.0, Math.max(0.2, entity.getDeltaMovement().length()));
            Iterable<?> nodes = (Iterable<?>) trail.invoke(null, entity.xo, entity.zo,
                    entity.getX(), entity.getZ(), pos.getY(), strength, speed);
            for (Object node : nodes) insert.invoke(handler, node);
            return;
        }
    }
}
