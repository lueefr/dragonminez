# Transformation engine implementation

The default path remains JSON. Optional Lua uses the same operations as JSON; direct writes into internal objects are not the extension contract. Gameplay is authoritative on the server.

| Branch | Scope | Dependency |
| --- | --- | --- |
| `chore/documentation-layout` | Organize documentation and repair links; published to main | None |
| `feature/transformation-runtime` | Compiled values, events, meters, state policies and deterministic evaluation | None |
| `feature/form-behaviors` | Maintenance, timed states, attack phases and live/snapshot inheritance | Runtime |
| `feature/combat-reactions` | Force-relative hit chance, automatic reactions and combat capabilities | Behaviors |
| `feature/aura-layers` | Independent identified layers, scale, transitions and migration | Existing renderer; runtime for dynamic parameters |
| `feature/lua-extension` | Optional runtime, controlled bindings, reload, diagnostics and content registration | Stable engine operations |
| `feature/flight-physics` | Shared simulation, prediction and server reconciliation | Runtime and existing flight integration |

Create implementation branches when their work starts. Do not merge unfinished gameplay modules into main.

## Runtime contract

- Queries have no side effects. Events evaluate conditions and values against one snapshot, then commit meter actions in declaration order.
- Meter identifiers are namespaced. Definitions specify bounds, initial value, scope and decay per tick.
- Scopes: character (persistent), combat and form (temporary). State serialization includes only persistent meters.
- Dynamic numbers compile once from JSON into immutable expressions. Dependencies are explicit, numbers must remain finite, expression depth and size are bounded.
- Curves use strictly increasing input points with clamped endpoints; linear and smooth interpolation are explicit.
- Missing context values are errors, not silent zeroes. Conditions remain a separate predicate adapter for the existing shared vocabulary.
- Runtime errors must not partially commit an event. No content-specific form names or flags are built into the engine.

## Integration direction

Damage events, StatsData persistence, shared conditions and full/delta synchronization are integrated on the runtime branch and covered by server GameTests. Attack phases, maintenance, visible HUD widgets and meter-driven stats remain subsequent modules. No automatic UI/Ultra Ego behavior is installed by the foundation.

Force-relative UI curves describe hit probability, not dodge probability. Eligibility, resource availability, cooldown, collision and attack identity are evaluated separately. Continuous attacks require an explicit reaction interval.

Aura migration converts old implicit layer size to explicit scale. New layer order never changes size. Lua interpreter selection requires execution-budget/isolation validation before adding a dependency. Flight uses client prediction and server reconciliation, not position smoothing alone.

## Foundation implemented on the runtime branch

`DynamicValue.compile` accepts constants, context sources, add/multiply/min/max operations and clamped linear/smooth point curves. `EventRules.compile` accepts meter declarations and event actions, with a predicate compiler supplied by the integration. `MeterState` owns values and serializes character-scope meters; it does not perform file I/O.

The following is an engine example, not an automatically loaded server config yet:

```json
{
  "meters": {
    "example:spirit": { "scope": "combat", "min": 0, "max": 100, "decayPerTick": 0.05 }
  },
  "rules": [
    {
      "event": "example:damage",
      "actions": [
        { "meter": "example:spirit", "operation": "add", "value": { "source": "event.damage" } }
      ]
    }
  ]
}
```

Meter numeric sources use `meter:<namespaced-id>`. Event inputs cannot overwrite meter sources. Conditions and value sources observe pre-event values; actions commit in declaration order and clamp after each action. Unknown inputs and nonfinite results reject the event without committing any actions. The integration adapter must supply measured server event data and condition evaluation; this module does not trust client-supplied damage.

## Server integration

The runtime branch now loads `config/dragonminez/progression/runtime.json` and includes it in config synchronization. Missing files generate an empty ruleset. Invalid files remain unchanged and disable runtime rules until corrected/reloaded.

Native events currently available:

| Event | Timing / numeric inputs |
| --- | --- |
| `dmz:damage_received` | NeoForge post-damage event on the recipient |
| `dmz:damage_dealt` | Same event for the responsible player; self damage excluded |
| `dmz:combat_enter` | First committed damaging contact after being out of combat |
| `dmz:combat_exit` | `combatTicks` without another damaging contact |
| `dmz:form_changed` | Main/stack form changes; form meters reset first |
| `dmz:tick` | Created server character tick, after decay |

Damage events expose `event.damage` (NeoForge final damage), `event.originalDamage` and `event.blockedDamage`. Other native events have no event numeric inputs yet. Unknown native events, unavailable numeric inputs and unknown meter references in runtime conditions fail validation. Shared `when` conditions evaluate before meter actions, including `meter` conditions of the form `{"meter":{"id":"example:spirit","min":10}}`.

`combatTicks` defaults to 200 and accepts 1..72000. Combat currently means actual damaging contact, not proximity or merely attempting an attack. Changing either main or stack form resets all form-scope meters. Character-scope meters survive save/load and cloning; form/combat scopes do not. Reload preserves and clamps character values and resets temporary scopes. Removed meter IDs remain in saved data so re-adding a definition can recover character progress.

Live meter values use the existing full/delta stat synchronization. Disk snapshots contain only persistent meters; network snapshots add a separate live view. The runtime does not add meter HUD widgets or stat modifiers yet. No new packet registration or configVersion change is needed.

A usable server configuration:

```json
{
  "combatTicks": 200,
  "meters": {
    "example:spirit": { "scope": "combat", "min": 0, "max": 100, "decayPerTick": 0.05 }
  },
  "rules": [
    {
      "event": "dmz:damage_received",
      "when": { "content": "example:enabled" },
      "actions": [
        { "meter": "example:spirit", "operation": "add", "value": { "source": "event.damage" } }
      ]
    }
  ]
}
```

## Form behavior branch

`behaviors.modifiers` in runtime.json accepts `when` and `factors`; stat targets are STR/SKP/RES/DEF/STM/VIT/PWR/ENE/SPEED and cost targets are energyDrain/staminaDrain/healthDrain. Numeric expressions can read meters, state.formTicks, state.stackTicks and self.healthPercent. Factors multiply outside the existing form/stack calculation. Queries are guarded against recursive stat conditions; invalid computed factors disable modifiers until reload.

`behaviors.forms` maps group.form to `when`, `durationTicks` (-1 unlimited), `cooldownTicks` and optional `fallback`. Maintenance runs on the server. Failure clears the form/stack, starts its persistent existing cooldown and attempts the fallback through ordinary access checks. Age counters synchronize in the live view. Attack-specific phases, captured inheritance and additional transition actions remain pending.

## Aura list branch

Forms and race character configs accept `aura.layers`. A missing list keeps the old main/extra behavior. A supplied list (including an empty one) replaces that owner's legacy aura; stack lists append their own layers. IDs are stable within the base/stack owner namespace and remain separate from drawing order. Up to 16 layers per owner are validated.

Each layer accepts id, type2d, type3d, color, uniform scale (default 1), opacity, order, when and a 3D style object. 3D style size controls per-axis geometry. Explicit scale disables the legacy index-based expansion in world, GUI and pulse rendering. Existing files are not rewritten and configVersion is unchanged. Shared predicates may use meters. Conditional removal uses the existing type fade; whole-aura sealing and mesh transitions remain supported. Per-layer sealing/absorption, replacement operations across owners and emitter configuration remain pending.

## Combat reaction branch

`reactions` is an ordered list in runtime.json. Entries accept namespaced id, shared when, attacks (melee/projectile), hitChance expression, powerSource (battlePower/STR/PWR/SPEED), distance, fractional energyCost/staminaCost and cooldownTicks. First matching rule replaces native speed dodge for that attack category, avoiding two independent rolls. Environmental damage is not eligible.

Hit chance expressions may read event.powerRatio and configured meters. Equal zero powers yield ratio 1; positive attacker versus zero defender yields the maximum finite ratio. Probability clamps to 0..1. Reactions require sufficient resources, cooldown readiness, a loaded/bounded collision-free destination and a clear swept path. The server moves the defender, synchronizes position/animation, consumes costs and emits dmz:evaded. No free damage cancellation occurs when movement is blocked. Runtime expression errors disable that reaction until reload. Successful reactions also enter combat. Native NPC battle power is used when available; other living entities currently use max health as a fallback score.

Swept displacement, probability endpoints, cooldown and actual movement are covered by a server GameTest. Configurable blocking/counters, power weights, attack-specific intervals and additional capabilities remain pending.

## Lua branch: server event scripts

Optional LuaJ scripts subscribe to runtime events in `progression/runtime.json`:

```json
{
  "meters": {"example:spirit": {"scope": "character", "min": 0, "max": 100}},
  "scripts": [{"id": "example:spirit", "events": ["dmz:damage_received"]}]
}
```

Create `config/dragonminez/scripts/example/spirit.lua`:

```lua
return {
  onEvent = function(ctx)
    return {{meter = 'example:spirit', operation = 'add', value = ctx.values['event.damage']}}
  end
}
```

The server loads source files; clients receive subscription metadata, never source. `ctx.event` contains the event identifier; `ctx.values` contains the pre-event numeric snapshot, including `meter:<id>`, `state.formTicks`, `state.stackTicks` and `self.healthPercent`. Scripts return `add`/`set` meter commands. JSON actions run first, followed by script commands in subscription order, against the same pre-event inputs. A failure rolls back both sets of meter changes and disables this character's event rules until reload.

`/dmzreload scripts` recompiles this configuration and its scripts without reloading storage or quests. Invalid source keeps the previously loaded configuration and leaves the files untouched. Sources must stay inside the scripts directory, including after symlink resolution. Each source is limited to 64 KiB, with at most 32 subscriptions and 64 combined returned commands per event.

Each invocation has a fresh environment. Persistent state belongs in declared meters. Only base language operations and deterministic math are exposed; filesystem, OS, Java objects, dynamic loading, coroutines, debug API and random functions are absent. Execution limits interrupt instruction loops, excessive recursion and oversized string concatenation, including loops inside `pcall`. These are safeguards for trusted local server scripts, not a process-isolated sandbox for hostile code.

Implemented: event subscriptions, numeric inputs, transactional meter commands, server-only loading, reload and bounded execution. Still pending: content registration, broader gameplay operations, editor autocomplete/diagnostics, rendering/model/camera/HUD bindings and the remaining extension APIs. Lua support alone does not complete those modules.
