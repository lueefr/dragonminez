package com.dragonminez.client.util;

import com.dragonminez.api.lang.ConfigTranslations;
import com.dragonminez.common.util.DMZTextPlaceholders;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class LocalizationUtil {
    private LocalizationUtil() {}
    public static Component localizedOrReadable(String key) {
        if(key==null||key.isBlank())return Component.empty();
        return DMZTextPlaceholders.apply(ConfigTranslations.text(key),Minecraft.getInstance().player);
    }
    public static String localizedOrReadableText(String key){return localizedOrReadable(key).getString();}
}
