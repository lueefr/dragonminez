package com.dragonminez.server.events.players.actionmode;

import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.init.MainSounds;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.util.FusionForms;
import com.dragonminez.common.util.TransformationItemCostHelper;
import com.dragonminez.common.util.TransformationsHelper;
import com.dragonminez.common.util.lists.StackForms;
import com.dragonminez.server.events.players.IActionModeHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;

public class StackFormModeHandler implements IActionModeHandler {
	@Override
	public boolean canCharge(ServerPlayer player, StatsData data) {
		FormConfig.FormData nextForm = TransformationsHelper.getNextAvailableStackForm(data);
		if (nextForm == null) return false;

		if (data.getCharacter().hasActiveForm()) {
			FormConfig.FormData activeFormData = data.getCharacter().getActiveFormData();
			if (activeFormData != null) {
				if (!activeFormData.getFormStackable() || !nextForm.getFormStackable()) return false;
				String stackGroup = data.getCharacter().hasActiveStackForm() ? data.getCharacter().getActiveStackFormGroup() : data.getCharacter().getSelectedStackFormGroup();
				if (!TransformationsHelper.areFormsCompatible(activeFormData, data.getCharacter().getActiveFormGroup(), nextForm, stackGroup)) return false;
			}
		}
		return true;
	}

	@Override
	public int handleActionCharge(ServerPlayer player, StatsData data) {
		if (!canCharge(player, data)) return 0;

		FormConfig.FormData nextForm = TransformationsHelper.getNextAvailableStackForm(data);
		String group = data.getCharacter().hasActiveStackForm() ? data.getCharacter().getActiveStackFormGroup() : data.getCharacter().getSelectedStackFormGroup();

		int mastery = (int) data.getStackFormChargeMastery(group, nextForm.getName());
		return TransformationsHelper.formChargeStep(mastery);
	}

	@Override
	public boolean performAction(ServerPlayer player, StatsData data) {
		attemptTransform(player, data);
		return true;
	}

	private static void attemptTransform(ServerPlayer player, StatsData data) {
		FormConfig.FormData nextForm = TransformationsHelper.getNextAvailableStackForm(data);
		if (nextForm == null) return;

        String useGroup=data.getCharacter().hasActiveStackForm()?data.getCharacter().getActiveStackFormGroup():data.getCharacter().getSelectedStackFormGroup();
        if (!com.dragonminez.api.forms.FormAccess.allow(data,useGroup,nextForm.getName(),true,false)) return;

		if (data.getCharacter().hasActiveForm()) {
			FormConfig.FormData activeFormData = data.getCharacter().getActiveFormData();
			if (activeFormData != null) {
				if (!activeFormData.getFormStackable() || !nextForm.getFormStackable()) {
					com.dragonminez.api.forms.FormAccess.notice(player,Component.translatable("message.dragonminez.form.not_stackable"),null);
					return;
				}

				String stackGroup = data.getCharacter().hasActiveStackForm() ? data.getCharacter().getActiveStackFormGroup() : data.getCharacter().getSelectedStackFormGroup();
				if (!TransformationsHelper.areFormsCompatible(activeFormData, data.getCharacter().getActiveFormGroup(), nextForm, stackGroup)) {
					com.dragonminez.api.forms.FormAccess.notice(player,Component.translatable("message.dragonminez.form.not_stackable"),null);
					return;
				}

				String nextGroup = data.getCharacter().hasActiveStackForm() ? data.getCharacter().getActiveStackFormGroup() : data.getCharacter().getSelectedStackFormGroup();
				double baseMastery = data.getCharacter().getFormMasteries().getMastery(data.getCharacter().getActiveFormGroup(), data.getCharacter().getActiveForm());
				double stackMastery = data.getCharacter().getStackFormMasteries().getMastery(nextGroup, nextForm.getName());
				if (baseMastery < activeFormData.getStackOnMastery() || stackMastery < nextForm.getStackOnMastery()) {
					com.dragonminez.api.forms.FormAccess.notice(player,Component.translatable("message.dragonminez.form.not_stackable"),null);
					return;
				}
			}
		}

		int energyCost = (int) (data.getMaxEnergy() * nextForm.getEnergyDrain());
		int staminaCost = (int) (data.getMaxStamina() * nextForm.getStaminaDrain());
		int healthCost = (int) (data.getMaxHealth() * nextForm.getHealthDrain());

		boolean hasEnoughEnergy = data.getResources().getCurrentEnergy() >= energyCost;
		boolean hasEnoughStamina = data.getResources().getCurrentStamina() >= staminaCost;
		boolean hasEnoughHealth = data.getPlayer().getHealth() >= healthCost;

		if (!hasEnoughEnergy) {
			com.dragonminez.api.forms.FormAccess.notice(player,Component.translatable("message.dragonminez.form.no_ki", energyCost),null);
		}

		if (!hasEnoughStamina) {
			com.dragonminez.api.forms.FormAccess.notice(player,Component.translatable("message.dragonminez.form.no_stamina", staminaCost),null);
		}

		if (!hasEnoughHealth) {
			com.dragonminez.api.forms.FormAccess.notice(player,Component.translatable("message.dragonminez.form.no_health", healthCost),null);
		}

		if (hasEnoughEnergy && hasEnoughStamina && hasEnoughHealth) {
			if (!TransformationItemCostHelper.canAffordAndHandleTriggerCost(player, nextForm)) {
				com.dragonminez.api.forms.FormAccess.notice(player,Component.translatable("message.dragonminez.form.no_trigger_item"),null);
				return;
			}
			String group = data.getCharacter().hasActiveStackForm() ?
					data.getCharacter().getActiveStackFormGroup() :
					data.getCharacter().getSelectedStackFormGroup();

			if (!data.getCharacter().getStackFormsUsedBefore().getFormGroup(group).contains(nextForm.getName())) {
				data.getCharacter().getStackFormsUsedBefore().putForm(group, nextForm.getName());
			}
			float[] resourceSnapshot = data.snapshotMultiplierResources();
			data.getCharacter().recordPreviousStackForm();
			data.getCharacter().setActiveStackForm(group, nextForm.getName());
			data.restoreMultiplierGains(player, resourceSnapshot);
			TransformationItemCostHelper.clearStackFormDurationSecondsRemaining(player);
			player.refreshDimensions();

			player.level().playSound(null, player.getX(), player.getY(), player.getZ(), MainSounds.TRANSFORM_ON.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

			if (StackForms.GROUP_KAIOKEN.equalsIgnoreCase(group)) {
				player.level().playSound(null, player.getX(), player.getY(), player.getZ(), MainSounds.STACK_FORM.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
			}

			Component translatedStackFormGroup = Component.translatable("race.dragonminez.stack.group." + data.getCharacter().getSelectedStackFormGroup());
			Component translatedStackFormName = Component.translatable("race.dragonminez.stack.form." + data.getCharacter().getSelectedStackFormGroup() + "." + nextForm.getName());
			Component fullFormName;

			if (data.getCharacter().getActiveForm() != null && !data.getCharacter().getActiveForm().isEmpty()) {
				Component translatedFormName = Component.translatable(FusionForms.formTranslationKey(data.getCharacter().getRaceName(), data.getCharacter().getActiveFormGroup(), data.getCharacter().getActiveForm()));
				fullFormName = Component.empty()
						.append(translatedFormName)
						.append(net.minecraft.network.chat.Component.translatable("text.dragonminez.stackformmodehandler.fc191eafd9"))
						.append(translatedStackFormGroup)
						.append(net.minecraft.network.chat.Component.translatable("text.dragonminez.stackformmodehandler.36a9e7f1c9"))
						.append(translatedStackFormName);
			} else {
				fullFormName = Component.empty()
						.append(translatedStackFormGroup)
						.append(net.minecraft.network.chat.Component.translatable("text.dragonminez.stackformmodehandler.36a9e7f1c9"))
						.append(translatedStackFormName);
			}

			player.sendSystemMessage(Component.translatable("message.dragonminez.transformation", fullFormName), true);

			if (!player.hasEffect(MainEffects.STACK_TRANSFORMED)) {
				player.addEffect(new MobEffectInstance(MainEffects.STACK_TRANSFORMED, -1, 0, false, false, true));
			}
			player.refreshDimensions();
		}
	}
}