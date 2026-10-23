package com.dragonminez.common.init.armor;

import com.dragonminez.Reference;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import java.util.Map;

/** Armor materials are component templates in Minecraft 26, rather than registry entries. */
public final class ModArmorMaterials {
    // Upstream 2.2 doubles vanilla slot durability bases (26/30/32/22).
    public static final Holder<ArmorMaterial> BASIC = Holder.direct(new ArmorMaterial(
            15 * 2, Map.of(ArmorType.BOOTS, 1, ArmorType.LEGGINGS, 2,
            ArmorType.CHESTPLATE, 3, ArmorType.HELMET, 1, ArmorType.BODY, 3),
            10, SoundEvents.ARMOR_EQUIP_IRON, 0, 0,
            TagKey.create(Registries.ITEM, id("repairs_basic_armor")),
            ResourceKey.create(EquipmentAssets.ROOT_ID, id("basic"))));
    public static final Holder<ArmorMaterial> KIKONO = Holder.direct(new ArmorMaterial(
            37 * 2, Map.of(ArmorType.BOOTS, 16, ArmorType.LEGGINGS, 26,
            ArmorType.CHESTPLATE, 35, ArmorType.HELMET, 2, ArmorType.BODY, 35),
            25, SoundEvents.ARMOR_EQUIP_NETHERITE, 5, 0.1F,
            TagKey.create(Registries.ITEM, id("repairs_kikono_armor")),
            ResourceKey.create(EquipmentAssets.ROOT_ID, id("kikono"))));
    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath(Reference.MOD_ID, path); }
    private ModArmorMaterials() {}
}
