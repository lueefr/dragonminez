package com.dragonminez.common.wish.wishes;

import net.minecraft.world.item.equipment.ArmorType;
import com.dragonminez.common.util.types.items.GenericItemDTO;
import com.dragonminez.common.wish.Wish;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import com.dragonminez.platform.registry.DeferredHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Grants a list of items, each of which may carry enchantments, potion data or a trim.
 */
public class ItemListWish extends Wish {

	public static final String WISH_TYPE = "item_list_wish";

	private final List<GenericItemDTO> items;

	public ItemListWish(String name, String description, List<GenericItemDTO> items) {
		super(name, description, WISH_TYPE);
		this.items = items;
	}

	public ItemListWish(String name, String description, DeferredHolder<net.minecraft.world.item.Item, ? extends Item> item) {
		this(name, description, item, 1);
	}

	public ItemListWish(String name, String description, DeferredHolder<net.minecraft.world.item.Item, ? extends Item> item, int count) {
		super(name, description, WISH_TYPE);
		this.items = List.of(new GenericItemDTO(item.getId(), count));
	}

	/**
	 * Grants a whole armour set, in {@link ArmorType} order. The sets in
	 * {@code MainItems} are {@code HashMap}s keyed by an enum, whose iteration order
	 * varies between JVM runs; datagen output has to be byte-stable, so the order is
	 * pinned here.
	 */
	public ItemListWish(String name, String description, Map<ArmorType, DeferredHolder<net.minecraft.world.item.Item, ? extends Item>> armorSet) {
		super(name, description, WISH_TYPE);
		List<GenericItemDTO> pieces = new ArrayList<>();
		for (ArmorType type : ArmorType.values()) {
			DeferredHolder<net.minecraft.world.item.Item, ? extends Item> piece = armorSet.get(type);
			if (piece != null) {
				pieces.add(new GenericItemDTO(piece.getId(), 1));
			}
		}
		this.items = pieces;
	}

	@Override
	public void grant(ServerPlayer player) {
		if (items == null) {
			return;
		}
		for (GenericItemDTO itemInfo : items) {
			if (itemInfo == null) {
				continue;
			}
			ItemStack stack = itemInfo.getItemStack();
			if (!stack.isEmpty()) {
				giveOrDrop(player, stack);
			}
		}
	}

	private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
		player.getInventory().add(stack);
		if (!stack.isEmpty()) {
			ItemEntity drop = player.drop(stack, false);
			if (drop != null) drop.setNoPickUpDelay();
		}
	}
}
