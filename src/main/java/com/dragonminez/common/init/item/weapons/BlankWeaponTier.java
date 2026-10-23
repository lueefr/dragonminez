package com.dragonminez.common.init.item.weapons;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ToolMaterial;

/** Shared native sword material; individual weapons supply damage, durability and enchantability. */
public final class BlankWeaponTier {
    private BlankWeaponTier() {}
    public static ToolMaterial create() {
        return new ToolMaterial(BlockTags.INCORRECT_FOR_WOODEN_TOOL, 1, 0, -1, 15,
                TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("dragonminez", "repairs_blank_weapon")));
    }
}
