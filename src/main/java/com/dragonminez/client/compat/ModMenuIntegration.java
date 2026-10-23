package com.dragonminez.client.compat;

import com.dragonminez.client.gui.config.DMZModConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Optional config-screen entry; Mod Menu loads it only when installed. */
public final class ModMenuIntegration implements ModMenuApi {
    @Override public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return DMZModConfigScreen::new;
    }
}
