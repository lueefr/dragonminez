package com.dragonminez.platform.event.living;
import com.dragonminez.platform.event.GameEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
public final class LivingLifecycleBridge {
    private static boolean installed;
    private LivingLifecycleBridge() {}
    public static synchronized void register() {
        if (installed) return;
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> !GameEvents.BUS.post(new LivingDeathEvent(entity, source)).isCanceled());
        installed = true;
    }
}
