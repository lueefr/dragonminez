package com.dragonminez.platform.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.Equippable;

/** Component-based armor access, including vanilla and other mods' items. */
public final class ArmorData {
    private ArmorData() {}

    public static boolean isArmor(DataComponentGetter stack) {
        Equippable equipment = stack.get(DataComponents.EQUIPPABLE);
        if (equipment == null) return false;
        return stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
                .modifiers().stream().anyMatch(entry -> entry.attribute().equals(Attributes.ARMOR));
    }

    public static double defense(DataComponentGetter stack) {
        Equippable equipment = stack.get(DataComponents.EQUIPPABLE);
        if (equipment == null) return 0;
        return stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
                .compute(Attributes.ARMOR, 0, equipment.slot());
    }

    public static Identifier asset(DataComponentGetter stack) {
        Equippable equipment = stack.get(DataComponents.EQUIPPABLE);
        if (equipment == null) throw new IllegalArgumentException("Item has no equipment component: " + stack);
        return equipment.assetId().orElseThrow(() -> new IllegalArgumentException("Item has no equipment asset: " + stack)).identifier();
    }
}
