package com.dragonminez.common.init.item.weapons;

import com.dragonminez.common.init.item.tools.ToolTiers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class WeaponItem extends Item {
	private final String tag;
	private final int enchantability;

	public WeaponItem(int damageBase, float attackSpeed, int durability, int enchantability, String tag) {
		super(weaponProperties(damageBase, attackSpeed, durability, enchantability));
		this.tag = tag;
		this.enchantability = enchantability;
	}

    private static Item.Properties weaponProperties(int damage, float speed, int durability, int enchantability) {
        var properties = com.dragonminez.platform.registry.RegistrationScope.itemProperties()
                .sword(ToolTiers.BLANK_WEAPON_TIER, damage, speed).fireResistant().enchantable(enchantability);
        if (durability > 0) return properties.durability(durability);
        return properties.component(DataComponents.MAX_DAMAGE, null).component(DataComponents.DAMAGE, null).stacksTo(1);
    }

	@Override
	public @NotNull Component getName(@NotNull ItemStack pStack) {
		return Component.translatable("item.dragonminez." + this.tag);
	}

	@Override
	public void appendHoverText(ItemStack pStack, Item.TooltipContext context, net.minecraft.world.item.component.TooltipDisplay dmz$display, java.util.function.Consumer<Component> dmz$consumer, TooltipFlag pIsAdvanced) {
        java.util.List<Component> pTooltipComponents = new java.util.ArrayList<>();
        try {
		pTooltipComponents.add(Component.translatable("item.dragonminez." + this.tag + ".tooltip").withStyle(ChatFormatting.GRAY));

        } finally { pTooltipComponents.forEach(dmz$consumer); }
    }
}
