package com.dragonminez.client.text;

import net.minecraft.network.chat.Style;

/** Client-only transient rendering metadata. Never uses click/hover/insertion fields. */
public interface FormNameStyle {
    Integer dragonminez$formColor();
    void dragonminez$setColor(Integer color);
    Style dragonminez$copyColor(Integer color);
}
