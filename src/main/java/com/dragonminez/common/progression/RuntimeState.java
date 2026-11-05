package com.dragonminez.common.progression;

import com.dragonminez.api.runtime.*;
import com.dragonminez.common.config.*;
import com.dragonminez.common.stats.StatsData;
import com.google.gson.*;
import net.minecraft.nbt.*;
import java.util.*;

/** Character-owned meter state. Disk snapshots preserve only character-scoped values. */
public final class RuntimeState {
    private RuntimeConfig config;
    private MeterState meters;
    private CompoundTag saved = new CompoundTag(), visible = new CompoundTag();
    private boolean failed, dispatching;
    private int combatLeft;
    private String form = "", stack = "";

    private void bind() {
        var next = ConfigManager.getRuntime();
        if (config == next) return;
        if (meters != null) store();
        config = next; meters = next.rules().create(); failed = false; combatLeft = 0;
        var json = new JsonObject();
        for (String id : saved.getAllKeys()) if (next.rules().meters().contains(id) && saved.contains(id, Tag.TAG_ANY_NUMERIC)) {
            double value = saved.getDouble(id); if (Double.isFinite(value)) json.addProperty(id, value);
        }
        meters.load(json);
    }
    public boolean has(String id) { bind(); return config.rules().meters().contains(id); }
    public double value(String id) {
        bind();
        if (config.rules().meters().contains(id)) return meters.value(id);
        return visible.contains(id, Tag.TAG_ANY_NUMERIC) ? visible.getDouble(id) : 0;
    }
    public void dispatch(StatsData data, String event, Map<String, Double> values) {
        if (!(data.getPlayer() instanceof net.minecraft.server.level.ServerPlayer)) return;
        bind();
        if (failed || dispatching || config.rules().empty()) return;
        observe(data);
        dispatching = true;
        try {
            config.rules().dispatch(meters, new EventRules.Context(event, values, config.flags(data)));
        } catch (RuntimeException ex) {
            failed = true;
            com.dragonminez.LogUtil.error(com.dragonminez.Env.COMMON, "Runtime event {} failed for {}: {}. Rules disabled for this character until reload.", event, data.getPlayer().getUUID(), ex.getMessage());
        } finally { dispatching = false; }
    }
    public void contact(StatsData data) {
        bind();
        if (combatLeft == 0) dispatch(data, "dmz:combat_enter", Map.of());
        combatLeft = config.combatTicks();
    }
    public void tick(StatsData data) {
        if (!(data.getPlayer() instanceof net.minecraft.server.level.ServerPlayer)) return;
        bind();
        if (config.rules().empty()) return;
        observe(data);
        if (combatLeft > 0 && --combatLeft == 0) {
            dispatch(data, "dmz:combat_exit", Map.of()); meters.reset(MeterState.Scope.COMBAT);
        }
        meters.tick(); dispatch(data, "dmz:tick", Map.of());
    }
    public void changed(StatsData data) {
        if (data != null && data.getPlayer() instanceof net.minecraft.server.level.ServerPlayer) { bind(); observe(data); }
    }
    private void observe(StatsData data) {
        var character = data.getCharacter();
        String current = character.getActiveFormGroup() + "." + character.getActiveForm();
        String extra = character.getActiveStackFormGroup() + "." + character.getActiveStackForm();
        if (!form.equals(current) || !stack.equals(extra)) {
            meters.reset(MeterState.Scope.FORM); form = current; stack = extra;
            dispatch(data, "dmz:form_changed", Map.of());
        }
    }
    private void store() {
        var json = meters.save();
        json.entrySet().forEach(entry -> saved.putDouble(entry.getKey(), entry.getValue().getAsDouble()));
        for (String id : config.rules().meters()) if (!json.has(id)) saved.remove(id);
    }
    public CompoundTag save() { bind(); store(); return saved.copy(); }
    public void load(CompoundTag tag) {
        saved = tag.copy(); config = null; meters = null; failed = false; combatLeft = 0; form = ""; stack = "";
        if (!visible.isEmpty()) view(visible);
    }
    public CompoundTag view() {
        bind(); var tag = new CompoundTag(); meters.snapshot().forEach(tag::putDouble); return tag;
    }
    public void view(CompoundTag tag) {
        visible = tag.copy();
        bind();
        for (String id : config.rules().meters()) if (tag.contains(id, Tag.TAG_ANY_NUMERIC) && Double.isFinite(tag.getDouble(id))) meters.set(id, tag.getDouble(id));
    }
}
