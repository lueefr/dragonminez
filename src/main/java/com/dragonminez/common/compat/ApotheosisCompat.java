package com.dragonminez.common.compat;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Copies the selected Apotheosis 1.21 item components without making it a required mod. */
public final class ApotheosisCompat {
    public static final String MOD_ID = "apotheosis";
    private ApotheosisCompat() {}

    public static boolean isLoaded() { return net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(MOD_ID); }

    public static void copyRarity(ItemStack from, ItemStack to) { copy(from, to, "rarity"); }
    public static void copyAffixes(ItemStack from, ItemStack to) {
        copy(from, to, "affixes");
        copy(from, to, "affix_name");
    }
    public static void copySockets(ItemStack from, ItemStack to) { copy(from, to, "sockets"); }
    public static void copyGems(ItemStack from, ItemStack to) { copy(from, to, "socketed_gems"); }

    private static void copy(ItemStack from, ItemStack to, String component) {
        if (!isLoaded()) return;
        BuiltInRegistries.DATA_COMPONENT_TYPE.getOptional(Identifier.fromNamespaceAndPath(MOD_ID, component))
                .ifPresent(type -> copyComponent(from, to, type));
    }

    private static <T> void copyComponent(ItemStack from, ItemStack to, DataComponentType<T> type) {
        T value = from.get(type);
        if (value != null) to.set(type, value);
    }
}
