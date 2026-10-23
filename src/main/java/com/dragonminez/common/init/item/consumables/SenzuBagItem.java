package com.dragonminez.common.init.item.consumables;

import com.dragonminez.common.init.MainTags;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import com.dragonminez.platform.event.living.ItemUseHooks;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class SenzuBagItem extends Item {

	private static final String TAG_ITEMS = "Items";
	public static final int MAX_BEANS = 64;
	private static final int BAR_COLOR = Mth.color(0.4F, 0.8F, 0.4F);

	public SenzuBagItem(Properties properties) {
		super(properties);
	}

	public static boolean isAllowed(ItemStack stack) {
		return !stack.isEmpty() && stack.is(MainTags.Items.SENZU_BEANS);
	}

	public static int getContentCount(ItemStack bag) {
		return getContents(bag).mapToInt(ItemStack::getCount).sum();
	}

    public static Stream<ItemStack> getContents(ItemStack bag) {
        return bag.getOrDefault(net.minecraft.core.component.DataComponents.CONTAINER, net.minecraft.world.item.component.ItemContainerContents.EMPTY).stream();
    }

    private static void setContents(ItemStack bag, List<ItemStack> items) {
        bag.set(net.minecraft.core.component.DataComponents.CONTAINER, net.minecraft.world.item.component.ItemContainerContents.fromItems(items));
    }

    private static int add(ItemStack bag, ItemStack inserted) {
        if (!isAllowed(inserted)) return 0;
        int moved = Math.min(MAX_BEANS - getContentCount(bag), inserted.getCount());
        if (moved <= 0) return 0;
        List<ItemStack> items = new ArrayList<>(getContents(bag).map(ItemStack::copy).toList());
        if (!items.isEmpty() && !inserted.isDamaged() && ItemStack.isSameItemSameComponents(items.getFirst(), inserted)) items.getFirst().grow(moved);
        else items.addFirst(inserted.copyWithCount(moved));
        setContents(bag, items);
        return moved;
    }

    private static Optional<ItemStack> removeOne(ItemStack bag) {
        ItemStack stored = getContents(bag).findFirst().orElse(ItemStack.EMPTY);
        if (stored.isEmpty()) return Optional.empty();
        int taken = Math.min(stored.getCount(), stored.getMaxStackSize());
        ItemStack out = stored.copyWithCount(taken);
        shrinkEntry(bag, 0, taken);
        return Optional.of(out);
    }

    private static void shrinkEntry(ItemStack bag, int index, int amount) {
        List<ItemStack> items = new ArrayList<>(getContents(bag).map(ItemStack::copy).toList());
        if (index < 0 || index >= items.size()) return;
        items.get(index).shrink(amount);
        if (items.get(index).isEmpty()) items.remove(index);
        setContents(bag, items);
    }

    private static boolean hasContents(ItemStack bag) { return getContents(bag).findAny().isPresent(); }

    private static boolean unloadLatestStack(ItemStack bag, Player player) {
        ItemStack stored = getContents(bag).findFirst().orElse(ItemStack.EMPTY);
        if (stored.isEmpty()) return false;
        ItemStack out = stored.copyWithCount(Math.min(stored.getCount(), stored.getMaxStackSize()));
        int moved = moveToInventory(player.getInventory(), out);
        if (moved <= 0) return false;
        shrinkEntry(bag, 0, moved);
        return true;
    }

	private static int moveToInventory(Inventory inventory, ItemStack stack) {
		int start = stack.getCount();
		while (!stack.isEmpty()) {
			int slot = inventory.getSlotWithRemainingSpace(stack);
			if (slot < 0) slot = inventory.getFreeSlot();
			if (slot < 0) break;
			int before = stack.getCount();
			inventory.add(slot, stack);
			if (stack.getCount() >= before) break;
		}
		return start - stack.getCount();
	}

    private static int findEdibleIndex(ItemStack bag, Player player, boolean fireUseStart) {
        List<ItemStack> items = getContents(bag).toList();
        for (int i = 0; i < items.size(); i++) {
            ItemStack stored = items.get(i);
            FoodProperties food = stored.getFoodProperties(player);
            if (food == null || !player.canEat(food.canAlwaysEat())) continue;
            if (player.getCooldowns().isOnCooldown(stored.getItem())) continue;
            if (fireUseStart) {
                ItemStack probe = stored.copyWithCount(1);
                if (ItemUseHooks.start(player, probe, probe.getUseDuration(player)) < 0) continue;
            }
            return i;
        }
        return -1;
    }

    private static void eatEntry(ItemStack bag, int index, Level level, Player player) {
        List<ItemStack> items = getContents(bag).toList();
        if (index < 0 || index >= items.size()) return;
        ItemStack bean = items.get(index).copyWithCount(1);
        shrinkEntry(bag, index, 1);
        ItemStack eaten = bean.copy();
        ItemUseHooks.finish(player, eaten, 0, bean.finishUsingItem(level, player));
    }

	@Override
	public boolean overrideStackedOnOther(@NotNull ItemStack bag, @NotNull Slot slot, @NotNull ClickAction action, @NotNull Player player) {
		if (action != ClickAction.SECONDARY) return false;

		ItemStack other = slot.getItem();
		if (other.isEmpty()) {
			removeOne(bag).ifPresent(stack -> {
				playRemoveOneSound(player);
				add(bag, slot.safeInsert(stack));
			});
			return true;
		}
		if (isAllowed(other)) {
			int space = MAX_BEANS - getContentCount(bag);
			if (space > 0 && add(bag, slot.safeTake(other.getCount(), space, player)) > 0) {
				playInsertSound(player);
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean overrideOtherStackedOnMe(@NotNull ItemStack bag, @NotNull ItemStack other, @NotNull Slot slot, @NotNull ClickAction action, @NotNull Player player, @NotNull net.minecraft.world.entity.SlotAccess access) {
		if (action != ClickAction.SECONDARY || !slot.allowModification(player)) return false;

		if (other.isEmpty()) {
			removeOne(bag).ifPresent(stack -> {
				playRemoveOneSound(player);
				access.set(stack);
			});
			return true;
		}
		if (isAllowed(other)) {
			int moved = add(bag, other);
			if (moved > 0) {
				playInsertSound(player);
				other.shrink(moved);
			}
			return true;
		}
		return false;
	}

	@Override
	public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
		ItemStack bag = player.getItemInHand(hand);
		if (player.isShiftKeyDown()) {
			if (level.isClientSide()) {
				return findEdibleIndex(bag, player, false) >= 0 ? InteractionResultHolder.success(bag) : InteractionResultHolder.fail(bag);
			}
			int index = findEdibleIndex(bag, player, true);
			if (index < 0) return InteractionResultHolder.fail(bag);
			eatEntry(bag, index, level, player);
			player.awardStat(Stats.ITEM_USED.get(this));
			return InteractionResultHolder.consume(bag);
		}

		if (level.isClientSide()) {
			return hasContents(bag) ? InteractionResultHolder.success(bag) : InteractionResultHolder.fail(bag);
		}
		if (unloadLatestStack(bag, player)) {
			playUnloadSound(player);
			player.awardStat(Stats.ITEM_USED.get(this));
			return InteractionResultHolder.consume(bag);
		}
		return InteractionResultHolder.fail(bag);
	}

	@Override
	public boolean isBarVisible(@NotNull ItemStack stack) {
		return getContentCount(stack) > 0;
	}

	@Override
	public int getBarWidth(@NotNull ItemStack stack) {
		return Math.min(1 + 12 * getContentCount(stack) / MAX_BEANS, 13);
	}

	@Override
	public int getBarColor(@NotNull ItemStack stack) {
		return BAR_COLOR;
	}

	@Override
	public boolean isEnchantable(@NotNull ItemStack stack) {
		return false;
	}

	@Override
	public void onDestroyed(@NotNull net.minecraft.world.entity.item.ItemEntity itemEntity) {
		net.minecraft.world.item.ItemUtils.onContainerDestroyed(itemEntity, getContents(itemEntity.getItem()).toList());
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, net.minecraft.world.item.component.TooltipDisplay dmz$display, java.util.function.Consumer<Component> dmz$consumer, TooltipFlag flag) {
        java.util.List<Component> tooltip = new java.util.ArrayList<>();
        try {
		List<ItemStack> contents = getContents(stack).toList();
		if (contents.isEmpty()) {
			tooltip.add(Component.translatable("item.dragonminez.senzu_bean_bag.empty").withStyle(ChatFormatting.GRAY));
		} else {
			for (ItemStack stored : new ArrayList<>(contents)) {
				tooltip.add(Component.translatable("item.dragonminez.senzu_bean_bag.entry",
						stored.getHoverName(), stored.getCount()).withStyle(ChatFormatting.GRAY));
			}
		}
		tooltip.add(Component.translatable("item.dragonminez.senzu_bean_bag.fullness",
				getContentCount(stack), MAX_BEANS).withStyle(ChatFormatting.GRAY));

        } finally { tooltip.forEach(dmz$consumer); }
    }

	private void playRemoveOneSound(Player player) {
		player.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
	}

	private void playInsertSound(Player player) {
		player.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
	}

	private void playUnloadSound(Player player) {
		player.level().playSound(null, player.blockPosition(), SoundEvents.BUNDLE_REMOVE_ONE,
				SoundSource.PLAYERS, 0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
	}
}
