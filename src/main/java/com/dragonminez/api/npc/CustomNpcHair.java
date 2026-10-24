package com.dragonminez.api.npc;

import com.dragonminez.common.hair.CustomHair;
import com.dragonminez.common.hair.HairCodec;
import com.dragonminez.common.hair.HairStyleSlot;
import java.util.regex.Pattern;

/** Resolves local Hair Salon exports; no web request or numeric preset lookup is involved. */
public final class CustomNpcHair {
    private static final Pattern CLIPBOARD_SEPARATORS = Pattern.compile("[\\s\u200B\uFEFF]+", Pattern.UNICODE_CHARACTER_CLASS);

    private CustomNpcHair() {}

    /** Match Hair Salon copy/paste cleanup, including invisible clipboard separators. */
    public static String normalizeCode(String code) {
        return code == null ? null : CLIPBOARD_SEPARATORS.matcher(code).replaceAll("");
    }

    /** Full sets use the requested slot; single-style exports are used directly. */
    public static CustomHair resolveCode(String code, String hairType) {
        String normalized = normalizeCode(code);
        if (normalized == null || normalized.isEmpty()) return null;
        HairStyleSlot slot = "none".equals(hairType) ? HairStyleSlot.BASE : HairStyleSlot.byHairType(hairType);
        if (slot == null) return null;
        if (HairCodec.isFullSetCode(normalized)) {
            var styles = HairCodec.fromFullSetCode(normalized);
            return styles == null ? null : styles.get(slot);
        }
        return HairCodec.fromCode(normalized);
    }
}
