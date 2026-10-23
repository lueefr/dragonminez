package com.dragonminez;

import com.dragonminez.common.DMZCommon;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class DragonMineZ implements ModInitializer {
    @Override
    public void onInitialize() {
        checkIncompatibility("legendarytooltips", "Legendary Tooltips");
        checkIncompatibility("epicfight", "Epic Fight");
        checkIncompatibility("bettercombat", "Better Combat");
        LogUtil.info(Env.COMMON, "Initializing DragonMineZ...");
        DMZCommon.init();
        LogUtil.info(Env.COMMON, "DragonMineZ common initialization complete");
    }
    private static void checkIncompatibility(String modId, String modName) {
        FabricLoader loader = FabricLoader.getInstance();
        if (!loader.isModLoaded(modId)) return;
        String jarName = loader.getModContainer(modId).map(container -> container.getOrigin().getPaths().stream()
                .findFirst().map(path -> path.getFileName().toString()).orElse(modId + ".jar")).orElse(modId + ".jar");
        throw new IllegalStateException("DragonMineZ cannot be loaded alongside " + modName
                + ". Remove " + jarName + " from the mods folder to continue.");
    }
}
