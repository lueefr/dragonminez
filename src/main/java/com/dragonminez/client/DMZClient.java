package com.dragonminez.client;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.client.gui.config.DMZModConfigScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ClientModInitializer;

@Environment(EnvType.CLIENT)
public class DMZClient implements ClientModInitializer {
    @Override public void onInitializeClient() { init(); }

    public static void init() {
        LogUtil.info(Env.CLIENT, "Initializing DragonMineZ Client...");
        com.dragonminez.compat.network.simple.SimpleChannel.registerClientReceivers();
        com.dragonminez.platform.network.ClientFluidRendering.register();
        com.dragonminez.platform.event.tick.ClientTickSubscribers.register();
        com.dragonminez.platform.event.tick.ClientTickBridge.register();
        com.dragonminez.platform.event.client.InputSubscribers.register();
        com.dragonminez.platform.event.client.ClientNetworkBridge.register();
        com.dragonminez.platform.event.client.HudSubscribers.register();
        com.dragonminez.platform.event.client.ClientHudBridge.register();
        com.dragonminez.client.events.ModClientEvents.registerGuiOverlays();
        com.dragonminez.platform.event.ClientCustomEventSubscribers.register();
        com.dragonminez.platform.event.GameEvents.BUS.addListener(
                com.dragonminez.platform.resource.AddPackFindersEvent.class, com.dragonminez.platform.event.EventPriority.NORMAL, false,
                com.dragonminez.client.events.ModClientEvents::onAddPackFinders);

    }
}
