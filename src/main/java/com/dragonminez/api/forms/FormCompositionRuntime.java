package com.dragonminez.api.forms;

import com.dragonminez.common.config.*;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.skills.Skills;
import com.dragonminez.common.util.*;
import java.util.*;
import static com.dragonminez.api.forms.FormCompositionResolver.*;

/** Player-owned bridge between form JSON, existing unlock rules and the pure resolver. */
public final class FormCompositionRuntime {
    private final FormCompositionResolver resolver = new FormCompositionResolver();
    private final Map<String, FormConfig.FormData> forms = new LinkedHashMap<>();
    private final Map<String, String> groups = new HashMap<>();
    private long generation = -1, stateRevision;
    private Input previous;
    private record Input(String race, String current, String currentAnchor, Skills skills, long skillRevision,
                         long masteryRevision, long stackMasteryRevision, long partnerSkillRevision, long contentRevision, boolean tail,
                         boolean android, boolean rage, boolean fullRage, boolean mutant, Object fusion) {}

    public static boolean usesCurrent(StatsData data, String group, String form) {
        Set<String> visited = new HashSet<>();
        while (group != null && form != null && visited.add(group + "." + form) && visited.size() <= 256) {
            FormConfig config = ConfigManager.getFormGroup(data.getCharacter().getRaceName(), group);
            if (config == null) return false;
            FormConfig.FormData value = config.getForm(form);
            if (value == null) return false;
            String reference = value.getInheritsMultipliersFrom();
            if (reference == null || reference.isBlank()) reference = value.getBaseForm();
            if ("current".equals(reference)) return true;
            if (reference == null || reference.isBlank() || "best".equals(reference)) return false;
            int dot = reference.indexOf('.');
            if (dot <= 0 || dot == reference.length() - 1) return false;
            group = FusionForms.qualifyLinkedGroup(group, reference.substring(0, dot));
            form = reference.substring(dot + 1);
        }
        return false;
    }

    public static boolean isComposed(FormConfig.FormData form) {
        return form != null && ((form.getBaseForm() != null && !form.getBaseForm().isBlank())
                || (form.getInheritsMultipliersFrom() != null && !form.getInheritsMultipliersFrom().isBlank()));
    }

    public double multiplier(StatsData data, String group, FormConfig.FormData form, String stat) {
        var character = data.getCharacter();
        Input input = new Input(character.getRaceName(), character.getActiveFormGroup() + "." + character.getActiveForm(),
                character.getCompositionBaseFormGroup() + "." + character.getCompositionBaseForm(),
                data.getSkills(), data.getSkills().getProgressionRevision(), character.getFormMasteries().getProgressionRevision(),
                character.getStackFormMasteries().getProgressionRevision(),
                data.getFusedData() != null ? data.getFusedData().getPartnerFormSkills().getProgressionRevision() : 0,
                data.getContentProgress().revision(), character.isHasSaiyanTail(), data.getStatus().isAndroidUpgraded(),
                data.getStatus().isRageActive(), data.getResources().isRageFull(), FusionForms.hasAnyMutant(data), data.getFusedData());
        long config = ConfigManager.getFormConfigGeneration();
        if (generation != config || previous == null || !Objects.equals(previous.race, input.race) || previous.fusion != input.fusion) {
            forms.clear(); groups.clear();
            List<Definition> definitions = new ArrayList<>();
            FusionForms.allFormGroups(data).forEach((groupId, groupConfig) -> groupConfig.getForms().values().forEach(value -> {
                String id = groupId + "." + value.getName();
                forms.put(id, value); groups.put(id, groupId);
                definitions.add(definition(groupId, value));
            }));
            ConfigManager.getAllStackForms().forEach((groupId, groupConfig) -> groupConfig.getForms().values().forEach(value -> {
                String id = groupId + "." + value.getName();
                forms.put(id, value); groups.put(id, groupId);
                definitions.add(definition(groupId, value));
            }));
            resolver.reload(definitions);
            generation = config;
            previous = null;
        }
        if (!input.equals(previous)) {
            Set<String> unlocked = new TreeSet<>();
            FusionForms.allFormGroups(data).forEach((groupId, groupConfig) ->
                    TransformationsHelper.getUnlockedForms(data, input.race, groupId).forEach(value -> unlocked.add(groupId + "." + value.getName())));
            Map<String, Multipliers> local = new HashMap<>();
            forms.forEach((id, value) -> {
                double mastery = character.getFormMasteries().getMastery(groups.get(id), value.getName());
                if (ConfigManager.getStackFormGroup(groups.get(id)) != null) {
                    mastery = character.getStackFormMasteries().getMastery(groups.get(id), value.getName());
                }
                double factor = value.getMaxMastery() > 0 ? 1 + Math.min(1, Math.max(0, mastery) / value.getMaxMastery()) * (value.getMaxStatsMultiplier()-1) : 1;
                Multipliers raw = raw(value);
                local.put(id, new Multipliers(adjust(raw.str(),factor),adjust(raw.skp(),factor),adjust(raw.stm(),factor),adjust(raw.def(),factor),
                        adjust(raw.vit(),factor),adjust(raw.pwr(),factor),adjust(raw.ene(),factor),adjust(raw.speed(),factor)));
            });
            resolver.updatePlayer(data.getPlayer().getUUID(), new PlayerState(++stateRevision, input.current, input.currentAnchor, unlocked, local));
            previous = input;
        }
        Multipliers value = resolver.resolve(data.getPlayer().getUUID(), group + "." + form.getName());
        return switch (stat.toUpperCase(Locale.ROOT)) {
            case "STR" -> value.str(); case "SKP" -> value.skp(); case "STM" -> value.stm();
            case "DEF" -> value.def(); case "RES" -> (value.def()+value.stm())/2;
            case "VIT" -> value.vit(); case "PWR" -> value.pwr(); case "ENE" -> value.ene(); case "SPEED" -> value.speed();
            default -> 1;
        };
    }
    public static Definition definition(String group, FormConfig.FormData form) {
        return new Definition(group + "." + form.getName(), qualify(group, form.getBaseForm()), qualify(group, form.getInheritsMultipliersFrom()), raw(form));
    }
    private static String qualify(String group, String reference) {
        if (reference == null || reference.isBlank() || reference.equals("best") || reference.equals("current")) return reference;
        int dot=reference.indexOf('.');
        if (dot<=0 || dot==reference.length()-1) throw new IllegalArgumentException("Expected group.form: " + reference);
        return FusionForms.qualifyLinkedGroup(group, reference.substring(0,dot)) + reference.substring(dot);
    }
    private static double adjust(double value, double factor) { return value>1 ? value*factor : value; }
    private static Multipliers raw(FormConfig.FormData form) {
        return new Multipliers(form.getStrMultiplier(),form.getSkpMultiplier(),form.getStmMultiplier(),form.getDefMultiplier(),
                form.getVitMultiplier(),form.getPwrMultiplier(),form.getEneMultiplier(),form.getSpeedMultiplier());
    }
}
