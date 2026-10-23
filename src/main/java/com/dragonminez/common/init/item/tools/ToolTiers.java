package com.dragonminez.common.init.item.tools;

import com.dragonminez.common.init.item.weapons.BlankWeaponTier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ToolMaterial;

public final class ToolTiers {
    private ToolTiers() {}
    public static final ToolMaterial BLANK_WEAPON_TIER = BlankWeaponTier.create();
    public static final ToolMaterial GETE_TIER = new ToolMaterial(
            TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("dragonminez", "incorrect_for_gete_tool")),
            3250, 12, 6, 25,
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("dragonminez", "repairs_gete_tool")));
}
