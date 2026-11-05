package com.dragonminez.client.gui.character.util;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.util.TransformationsHelper;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class FormPreview {
	private static final List<String> TYPE_ORDER = List.of("superforms", "androidforms", "legendaryforms", "godforms");

	public record Option(String group, String form) {
		public boolean isBase() {
			return group == null || group.isEmpty() || form == null || form.isEmpty();
		}
	}

	public record Snapshot(String formGroup, String form, String stackGroup, String stackForm, boolean androidUpgraded) {}

	private final String race;
	private final List<Option> options = new ArrayList<>();
	private int index;

	public FormPreview(String race) {
		this.race = race != null ? race : "human";
		options.add(new Option("", ""));
		for (TransformationsHelper.OrderedFormEntry entry : TransformationsHelper.orderedForms(com.dragonminez.client.text.FormNameText.viewer(),this.race,TYPE_ORDER)) {
			if (entry == null || entry.getFormData() == null) continue;
			options.add(new Option(entry.getGroupName(), entry.getFormData().getName()));
		}
	}

	public int size() {
		return options.size();
	}

	public int index() {
		return index;
	}

	public boolean hasPrevious() {
		return index > 0;
	}

	public boolean hasNext() {
		return index < options.size() - 1;
	}

	public void select(int newIndex) {
		if (options.isEmpty()) return;
		index = Math.floorMod(newIndex, options.size());
	}

	public void cycle(int delta) {
		select(index + delta);
	}

	public Option current() {
		return options.get(index);
	}

	public String displayName() {return displayNameComponent().getString();}

	public net.minecraft.network.chat.Component displayNameComponent() {
		Option option = current();
		if (option.isBase()) return net.minecraft.network.chat.Component.translatable("forms.dragonminez.base");
		String key = "race.dragonminez." + race.toLowerCase(Locale.ROOT) + ".form." + option.group() + "." + option.form();
		return net.minecraft.network.chat.Component.translatable(key);
	}

	public Snapshot apply(LivingEntity player) {
		Snapshot[] snapshot = new Snapshot[1];
		Option option = current();
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
			Character character = stats.getCharacter();
			snapshot[0] = new Snapshot(character.slotGroup(), character.slotForm(), character.stackGroup(), character.stackForm(), stats.getStatus().isAndroidUpgraded());
			character.clearActiveForm();
			character.clearActiveStackForm();
			if (option.isBase()) return;
			if (ConfigManager.getStackFormGroup(option.group()) != null) {
				character.setActiveStackForm(option.group(), option.form());
			} else {
				character.setActiveForm(option.group(), option.form());
				if ("androidforms".equalsIgnoreCase(option.group())) stats.getStatus().setAndroidUpgraded(true);
			}
		});
		return snapshot[0];
	}

	public void restore(LivingEntity player, Snapshot snapshot) {
		if (snapshot == null) return;
		StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(stats -> {
			Character character = stats.getCharacter();
			character.setActiveForm(snapshot.formGroup(), snapshot.form());
			character.setActiveStackForm(snapshot.stackGroup(), snapshot.stackForm());
			stats.getStatus().setAndroidUpgraded(snapshot.androidUpgraded());
		});
	}
}
