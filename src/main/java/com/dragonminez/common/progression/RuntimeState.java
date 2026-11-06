package com.dragonminez.common.progression;

import com.dragonminez.api.runtime.*;
import com.dragonminez.common.config.*;
import com.dragonminez.common.stats.StatsData;
import com.google.gson.*;
import net.minecraft.nbt.*;
import java.util.*;

/** Character-owned meter state. Disk snapshots preserve only character-scoped values. */
public final class RuntimeState {
    private final ReactionWindow reactions = new ReactionWindow();
    public boolean attempt(java.util.UUID projectile, String rule, long tick, int interval) { bind(); return reactions.attempt(projectile, rule, tick, interval); }
    private final AttackState attack = new AttackState();
    private boolean strikeCharge, observed, observing;
    private RuntimeConfig config;
    private MeterState meters;
    private CompoundTag saved = new CompoundTag(), visible = new CompoundTag();
    private boolean failed, dispatching;
    private int combatLeft, formTicks, stackTicks;
    private String form = "", stack = "";

    private void bind() {
        var next = ConfigManager.getRuntime();
        if (config == next) return;
        if (meters != null) store();
        config = next; reactions.clear(); attack.cancel(); meters = next.rules().create(); strikeCharge = false; failed = false; combatLeft = 0; formTicks = 0; stackTicks = 0;
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
        if (failed || dispatching) return;
        observe(data);
        dispatching = true;
        var before = meters.snapshot();
        Map<String, Double> inputs = new HashMap<>(numbers(data)); inputs.putAll(values);
        boolean committed=false;
        try {
            var commands=config.scriptBatch(event,Map.copyOf(inputs),config.queries(data));
            var rewards=new ArrayList<>(config.actions(event,data));rewards.addAll(commands.rewards());
            if(rewards.size()>64)throw new IllegalArgumentException("At most 64 gameplay actions per event");
            for(var reward:rewards)com.dragonminez.api.rules.Rewards.validate(reward);
            config.rules().dispatch(meters, new EventRules.Context(event, queries(values), config.flags(data)));
            config.rules().adjust(meters,commands.meters());
            committed=true;
            com.dragonminez.api.rules.Rewards.apply((net.minecraft.server.level.ServerPlayer)data.getPlayer(),data,event,rewards);
        } catch (RuntimeException ex) {
            if(!committed)meters.restore(before);
            failed = true;
            com.dragonminez.LogUtil.error(com.dragonminez.Env.COMMON, "Runtime event {} failed for {} (meter commit={}): {}. Rules disabled for this character until reload.", event, data.getPlayer().getUUID(), committed, ex.getMessage());
        } finally { dispatching = false; }
    }
    private Map<String, Double> queries(Map<String, Double> values) {
        Map<String, Double> result = new HashMap<>(attack.numbers()); result.putAll(values); return result;
    }
    public void startAttack(StatsData data, String id) {
        if (!(data.getPlayer() instanceof net.minecraft.server.level.ServerPlayer)) return;
        bind();
        if (attack.phase() == AttackState.Phase.CHARGING && attack.technique().equals(id)) return;
        if (attack.phase() == AttackState.Phase.CHARGING) dispatch(data, "dmz:attack_cancelled", Map.of());
        strikeCharge = false; attack.start(id); dispatch(data, "dmz:attack_started", Map.of());
    }
    public void releaseAttack(StatsData data, String id, double charge) {
        if (!(data.getPlayer() instanceof net.minecraft.server.level.ServerPlayer)) return;
        bind(); var timing = config.behaviors().attack(id);
        strikeCharge = false; attack.release(id, charge, timing.active(), timing.recovery()); dispatch(data, "dmz:attack_released", Map.of());
    }
    public boolean permits(String action) {
        bind();
        return config.behaviors().attack(attack.technique()).permits(attack.phase().name().toLowerCase(java.util.Locale.ROOT), action);
    }
    public void startStrike(StatsData data, String id) {
        startAttack(data, id); strikeCharge = true;
    }
    private void tickAttack(StatsData data) {
        var techniques = data.getTechniques(); String id = techniques.getChargingTechniqueId();
        boolean charging = techniques.isTechniqueCharging() && id != null && !id.isBlank();
        if (charging && (attack.phase() != AttackState.Phase.CHARGING || !attack.technique().equals(id))) startAttack(data, id);
        if (strikeCharge && attack.phase() == AttackState.Phase.CHARGING && data.getPlayer() instanceof net.minecraft.server.level.ServerPlayer player) {
            id = attack.technique();
            charging = com.dragonminez.server.events.players.combat.StrikeAttackHandler.isRushing(player);
            if (!charging && com.dragonminez.server.events.players.combat.StrikeAttackHandler.ownsStrike(player)) releaseAttack(data,id,100);
        }
        var previous = attack.phase();
        if (attack.tick(charging, id, techniques.getTechniqueChargePercent())) {
            String event = previous == AttackState.Phase.CHARGING ? "dmz:attack_cancelled" : attack.phase() == AttackState.Phase.RECOVERY ? "dmz:attack_recovery" : "dmz:attack_finished";
            dispatch(data, event, Map.of());
        }
    }
    public void contact(StatsData data) {
        bind();
        if (combatLeft == 0) dispatch(data, "dmz:combat_enter", Map.of());
        combatLeft = config.combatTicks();
    }
    public void tick(StatsData data) {
        if (!(data.getPlayer() instanceof net.minecraft.server.level.ServerPlayer)) return;
        bind();
        observe(data);
        tickAttack(data);
        formTicks = Math.min(Integer.MAX_VALUE - 1, formTicks) + 1;
        stackTicks = Math.min(Integer.MAX_VALUE - 1, stackTicks) + 1;
        maintain(data, false); maintain(data, true);
        if (!config.active()) return;
        if (combatLeft > 0 && --combatLeft == 0) {
            dispatch(data, "dmz:combat_exit", Map.of()); meters.reset(MeterState.Scope.COMBAT);
        }
        meters.tick(); dispatch(data, "dmz:tick", Map.of());
    }
    public void changed(StatsData data) {
        if (!dispatching && data != null && data.getPlayer() instanceof net.minecraft.server.level.ServerPlayer) { bind(); observe(data); }
    }
    public void prime(StatsData data) {
        if(observed||data==null||!(data.getPlayer() instanceof net.minecraft.server.level.ServerPlayer))return;
        bind();var character=data.getCharacter();
        form=character.getActiveFormGroup()+"."+character.getActiveForm();
        stack=character.getActiveStackFormGroup()+"."+character.getActiveStackForm();observed=true;
    }
    private void transition(StatsData data,String reference,boolean enter){
        if(failed||!data.getStatus().isHasCreatedCharacter())return;
        var rule=config.behaviors().lifetime(reference);if(rule==null)return;
        var actions=enter?rule.onEnter():rule.onExit();if(actions.isEmpty())return;
        dispatching=true;
        try{
            for(var action:actions)com.dragonminez.api.rules.Rewards.validate(action);
            com.dragonminez.api.rules.Rewards.apply((net.minecraft.server.level.ServerPlayer)data.getPlayer(),data,reference,actions);
        }catch(RuntimeException ex){failed=true;com.dragonminez.LogUtil.error(com.dragonminez.Env.COMMON,"Form transition {} failed (enter={}): {}. Rules disabled until reload.",reference,enter,ex.getMessage());}
        finally{dispatching=false;}
    }
    private void observe(StatsData data) {
        if(observing)return;
        if(!observed){prime(data);return;}
        var character = data.getCharacter();
        String current = character.getActiveFormGroup() + "." + character.getActiveForm();
        String extra = character.getActiveStackFormGroup() + "." + character.getActiveStackForm();
        if (!form.equals(current) || !stack.equals(extra)) {
            if (!form.equals(current)) formTicks = 0;
            if (!stack.equals(extra)) stackTicks = 0;
            String previous=form, previousStack=stack;
            meters.reset(MeterState.Scope.FORM); form = current; stack = extra;
            observing=true;
            try{
                if(!previous.equals(current))transition(data,previous,false);
                if(!previousStack.equals(extra))transition(data,previousStack,false);
                if(!previous.equals(current))transition(data,current,true);
                if(!previousStack.equals(extra))transition(data,extra,true);
                dispatch(data,"dmz:form_changed",Map.of());
            }finally{observing=false;}
        }
    }
    public Map<String, Double> numbers(StatsData data) {
        bind(); Map<String, Double> values = new HashMap<>(attack.numbers());
        meters.snapshot().forEach((id, value) -> values.put("meter:" + id, value));
        values.put("state.formTicks", (double) formTicks); values.put("state.stackTicks", (double) stackTicks);
        var player = data.getPlayer();
        values.put("self.healthPercent", player == null || player.getMaxHealth() <= 0 ? 0 : (double) player.getHealth() * 100 / player.getMaxHealth());
        return Map.copyOf(values);
    }
    private void maintain(StatsData data, boolean stacked) {
        var character = data.getCharacter(); String reference = stacked ? stack : form;
        var lifetime = config.behaviors().lifetime(reference);
        if (lifetime == null) return;
        int elapsed = stacked ? stackTicks : formTicks;
        if ((lifetime.duration() < 0 || elapsed < lifetime.duration()) && com.dragonminez.api.rules.Conditions.test(new com.dragonminez.api.rules.Conditions.Context(data, reference), lifetime.when())) return;
        data.getCooldowns().setCooldown("RuntimeForm:" + reference, lifetime.cooldown());
        if (stacked) character.clearActiveStackForm(); else character.clearActiveForm();
        if (!lifetime.fallback().isEmpty()) {
            var parts = com.dragonminez.api.rules.Conditions.form(lifetime.fallback());
            if (stacked) character.setActiveStackForm(parts[0], parts[1]); else character.setActiveForm(parts[0], parts[1]);
        }
    }
    private void store() {
        var json = meters.save();
        json.entrySet().forEach(entry -> saved.putDouble(entry.getKey(), entry.getValue().getAsDouble()));
        for (String id : config.rules().meters()) if (!json.has(id)) saved.remove(id);
    }
    public CompoundTag save() { bind(); store(); return saved.copy(); }
    public void load(CompoundTag tag) {
        saved = tag.copy(); observed=false; observing=false; reactions.clear(); config = null; meters = null; failed = false; combatLeft = 0; formTicks = 0; stackTicks = 0; form = ""; stack = "";
        if (!visible.isEmpty()) view(visible);
    }
    public CompoundTag view() {
        bind(); var tag = new CompoundTag(); meters.snapshot().forEach(tag::putDouble);
        attack.numbers().forEach((key, value) -> tag.putDouble("$" + key, value));
        tag.putInt("$formTicks", formTicks); tag.putInt("$stackTicks", stackTicks); return tag;
    }
    public void view(CompoundTag tag) {
        visible = tag.copy();
        bind();
        formTicks = Math.max(0, tag.getInt("$formTicks")); stackTicks = Math.max(0, tag.getInt("$stackTicks"));
        Map<String, Double> queries = new HashMap<>(); attack.numbers().keySet().forEach(key -> queries.put(key, tag.getDouble("$" + key))); attack.view(queries);
        for (String id : config.rules().meters()) if (tag.contains(id, Tag.TAG_ANY_NUMERIC) && Double.isFinite(tag.getDouble(id))) meters.set(id, tag.getDouble(id));
    }
}
