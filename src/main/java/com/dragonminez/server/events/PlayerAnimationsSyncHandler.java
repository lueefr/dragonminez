package com.dragonminez.server.events;

import com.dragonminez.Reference;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.PlayerAnimationsSync;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import com.dragonminez.platform.event.tick.PlayerTickEvent;
import com.dragonminez.platform.event.SubscribeEvent;
import com.dragonminez.platform.event.EventBusSubscriber;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = Reference.MOD_ID)
public class PlayerAnimationsSyncHandler {

    private static final Map<UUID, Boolean> lastFlyingState = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer)) return;
        if (!event.getEntity().level().isClientSide()) return;

        Player player = event.getEntity();
        UUID uuid = player.getUUID();

        boolean isCurrentlyFlying = player.getAbilities().flying || player.isFallFlying();

        Boolean lastState = lastFlyingState.get(uuid);
        if (lastState == null || lastState != isCurrentlyFlying) {
            lastFlyingState.put(uuid, isCurrentlyFlying);
            NetworkHandler.sendToTrackingEntityAndSelf(new PlayerAnimationsSync(uuid, isCurrentlyFlying), player);
        }
    }
}

