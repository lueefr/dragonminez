package com.dragonminez.common.init.armor;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;

/** Installs native equipment, attribute, durability, enchantment and repair components. */
public class NativeArmorItem extends Item {
    private final Holder<ArmorMaterial> material;
    private final ArmorType type;

    protected NativeArmorItem(Holder<ArmorMaterial> material, ArmorType type, Properties properties) {
        super(properties.humanoidArmor(material.value(), type));
        this.material = material;
        this.type = type;
    }

    public Holder<ArmorMaterial> getMaterial() { return material; }
    public ArmorType getType() { return type; }
    public EquipmentSlot getEquipmentSlot() { return type.getSlot(); }
    public int getDefense() { return material.value().defense().getOrDefault(type, 0); }
    public float getToughness() { return material.value().toughness(); }
}
