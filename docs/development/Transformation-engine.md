# Transformation engine implementation

The default path remains JSON. Optional Lua uses the same operations as JSON; direct writes into internal objects are not the extension contract. Gameplay is authoritative on the server.

| Branch | Scope | Dependency |
| --- | --- | --- |
| `chore/documentation-layout` | Organize documentation and repair links; published to main | None |
| `feature/transformation-runtime` | Compiled values, events, meters, state policies and deterministic evaluation | None |
| `feature/attack-phases` | Attack lifecycle queries and configurable active/recovery windows | Lua/runtime stack |
| `feature/power-snapshots` | Persistent captured attribute inheritance with activation policies | Runtime/main |
| `feature/form-behaviors` | Maintenance, timed states, attack phases and live/snapshot inheritance | Runtime |
| `feature/combat-reactions` | Force-relative hit chance, automatic reactions and combat capabilities | Behaviors |
| `feature/aura-layers` | Independent identified layers, scale, transitions and migration | Existing renderer; runtime for dynamic parameters |
| `feature/lua-extension` | Optional runtime, controlled bindings, reload, diagnostics and content registration | Stable engine operations |
| `feature/flight-physics` | Shared simulation, prediction and server reconciliation | Runtime and existing flight integration |

Create implementation branches when their work starts. Merge validated increments individually; the remaining scope of a module must stay explicitly pending.

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

## Flight authority and body collision

The flight branch uses Do a Barrel Roll as a control/orientation reference (reference checkout
`15125e45927a167c078eb6b7a1c93132f5bc7980`). The server owns motion and body orientation;
clients submit bounded controls, never authoritative positions or hitbox dimensions. Pitch,
yaw and roll must affect the physical body and remote rendering as well as the local camera.
Form scale remains the source of body dimensions.

Implemented on the flight branch: shared fixed-tick quaternion simulation, server-owned sessions
and bounded controls, client prediction with acknowledgement/replay, remote poses, swept body
collision, form-resolved dimensions, oriented targeting, prone eye/camera origins and combat
knockback. Vanilla position packets cannot override an active authoritative session. Vanilla
teleport acknowledgements remain active; disconnects, cloning and dimension changes clear the
session. Search flight uses the new engine by default; combat flight and scripted strike
movement retain their existing handlers.

Server settings live in `progression/runtime.json`, synchronized through the existing config
channel. An example with the default motion values:

```json
{
  "flight": {
    "enabled": true,
    "when": {},
    "thrust": 0.055,
    "drag": 0.04,
    "braking": 0.3,
    "gravity": 0,
    "lift": 0,
    "speed": 0.492,
    "boost": 1.75,
    "turn": 0.15,
    "smoothing": 0.35,
    "banking": 0.2,
    "cost": 0,
    "turnCost": 0
  }
}
```

Speed is blocks/tick before flight skill, attributes, temporary speed effects and the player's
speed limiter. Default normal/boosted caps preserve the existing search-flight base speeds.
Turn is radians/tick; drag/braking/smoothing are fractions. Cost and turnCost are additional
energy per tick, disabled by default; existing flight drain still runs once through its native
handler. `enabled: false` selects the native search-flight handler. Conditions use the shared
vocabulary, including declared runtime meters. W/S provide thrust/braking/reverse, A/D yaw,
mouse X roll, mouse Y pitch, jump/sneak local lift and sprint boost. Menus submit braking
controls. Missing controls time out after ten server ticks.

Validated with 60 unit tests, 60 required server GameTests and the packaged-client harness
with Sodium/Iris/Complementary. The server test uses the real vanilla movement packet handler
rather than FakePlayer's no-op override. Equal entity IDs on client/server remain isolated by
weak object-identity stores. The client run covers A/D direction, authoritative movement/roll,
prediction/reconciliation, teleport session resets, remote pose reception/cleanup, native eye
queries and existing aura/Ki/crosshair regressions. Evidence is in the runtime validation note.

Conditional profiles and separate axis rates are implemented in the following increment.
Broader latency simulation, remote-pose interpolation and automatic obstacle sliding remain
separate enhancements; collision currently stops motion at the last clear pose. This does not
complete the broader engine plan.

## Flight profile increment

`flight.profiles` accepts up to 32 ordered overrides. The first matching shared `when` condition
selects the entire profile; omitted values inherit the root flight settings. Without a match,
the root settings apply. Profiles do not inherit values from earlier profiles and cannot nest
or override `enabled`. The root `when` continues to select eligibility for the shared engine;
when it fails, native search flight handles movement.

```json
{
  "flight": {
    "speed": 0.492,
    "turn": 0.15,
    "profiles": [
      {
        "when": {"all": [{"activeForm": "example.focus"}, {"not": {"content": "example:exhausted"}}]},
        "drag": 0.02,
        "yawRate": 0.08,
        "rollRate": 0.2,
        "cost": 1
      },
      {
        "when": {"content": "example:heavy_flight"},
        "thrust": 0.04,
        "gravity": 0.01,
        "banking": 0.1
      }
    ]
  }
}
```

`pitchRate`, `yawRate` and `rollRate` are independent radians/tick values in 0..pi/4, each
falling back to `turn` when omitted. Zero disables rotation on that axis. Existing uniform-turn
JSON and Java constructors retain their behavior. Shared conditions can reference forms,
content, mastery and declared meters; unknown meter references are rejected at load.

Client prediction and the server resolve the same synchronized profile. The server alone pays
additional motion costs; known resource restrictions also suppress client prediction and are
retained when replaying unacknowledged inputs. Additional costs are charged during unrestricted
simulation; native flight drain still runs through its existing handler. Boost drain now reads
the applied server boost state rather than the vanilla sprint flag. Network revision 3 requires
matching updated clients for these prediction semantics; existing JSON configVersion values
are unchanged. Server GameTests and packaged-client evidence are in the runtime validation note.

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

`behaviors.forms` maps group.form to `when`, `durationTicks` (-1 unlimited), `cooldownTicks` and optional `fallback`. Maintenance runs on the server. Failure clears the form/stack, starts its persistent existing cooldown and attempts the fallback through ordinary access checks. Age counters synchronize in the live view. Attack phases are implemented separately on `feature/attack-phases`; captured attribute inheritance is implemented separately on `feature/power-snapshots`; additional transition actions remain pending.

## Aura list branch

Forms and race character configs accept `aura.layers`. A missing list keeps the old main/extra behavior. A supplied list (including an empty one) replaces that owner's legacy aura; stack lists append their own layers. IDs are stable within the base/stack owner namespace and remain separate from drawing order. Up to 16 layers per owner are validated.

Each layer accepts id, type2d, type3d, color, uniform scale (default 1), opacity, order, when and a 3D style object. 3D style size controls per-axis geometry. Explicit scale disables the legacy index-based expansion in world, GUI and pulse rendering. Existing files are not rewritten and configVersion is unchanged. Shared predicates may use meters. Conditional removal uses the existing type fade; whole-aura sealing and mesh transitions remain supported. Layers now accept `sealedWhen` and `sealSeconds` (0.01..10 seconds, default 0.4). Their own reversible absorption contracts each shell into the body, with an initially sealed shell absent on its first frame. World and GUI rendering preserve stable identities and the existing mesh blending; whole-aura sealing remains an outer gate. Owner-qualified `operation`/`target` entries now apply in base-then-stack file order, supporting conditional add/replace/remove. Missing targets are harmless; replacement adds its own shell regardless. Pure list composition is shared with server GameTests. Network revision 4 requires updated clients for the new aura semantics. Per-layer emitter configuration remains pending.

## Combat reaction branch

`reactions` is an ordered list in runtime.json. Entries accept namespaced id, shared when, attacks (melee/projectile), hitChance expression, powerSource (battlePower/STR/PWR/SPEED), distance, fractional energyCost/staminaCost and cooldownTicks. First matching rule replaces native speed dodge for that attack category, avoiding two independent rolls. Environmental damage is not eligible.

Hit chance expressions may read event.powerRatio and configured meters. Equal zero powers yield ratio 1; positive attacker versus zero defender yields the maximum finite ratio. Probability clamps to 0..1. Reactions require sufficient resources, cooldown readiness, a loaded/bounded collision-free destination and a clear swept path. The server moves the defender, synchronizes position/animation, consumes costs and emits dmz:evaded. No free damage cancellation occurs when movement is blocked. Runtime expression errors disable that reaction until reload. Successful reactions also enter combat. Native NPC battle power is used when available; other living entities currently use max health as a fallback score.

Swept displacement, probability endpoints, cooldown and actual movement are covered by a server GameTest. Weighted power and per-projectile attempt intervals are implemented on `feature/reaction-policies`; configurable blocking/counters and additional capabilities remain pending.

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


## Attack phase branch

Ki cast/release events now feed character-owned transient phases: charging, active and recovery. Charging cancellation is detected on the server tick, including direct technique setters. New charges supersede previous windows. Reload, character load and clone reset phase state; live views synchronize it for client-side visual queries. No phase data is stored as persistent meters.

Configure timing per technique in `progression/runtime.json`:

```json
{
  "behaviors": {
    "attacks": {"example:beam": {"activeTicks": 2, "recoveryTicks": 12}},
    "modifiers": [{"factors": {"PWR": {"add": [1, {"source": "attack.charging"}]}}}]
  }
}
```

Timing defaults to one active tick and zero recovery ticks; active accepts 1..72000 and recovery accepts 0..72000. These are rule windows, not projectile lifetimes or additional input locks. Native cooldown, cost and projectile behavior remain authoritative. Technique IDs use the existing technique registry keys, including existing unnamespaced keys.

Numeric sources `attack.charging`, `attack.active` and `attack.recovery` are 0/1; `attack.ticks` is elapsed ticks in the current phase and `attack.charge` is charge percent (0..200). Sources are available in JSON event actions, dynamic stat factors and Lua snapshots. Events: `dmz:attack_started`, `dmz:attack_released`, `dmz:attack_cancelled`, `dmz:attack_recovery` and `dmz:attack_finished`. Events observe the new phase; cancellation/finish observe idle. Successful releases retain their charge through active/recovery windows.

This integration covers the native charged Ki technique lifecycle. Strike/basic-blast phase adapters, per-projectile continuous-reaction intervals and configurable attack input restrictions remain subsequent work.

## Captured power inheritance

`progression/attributes.json` form sources now accept `mode: "live"` (the existing default) or `mode: "snapshot"`. Snapshot sources require a stable namespaced `id`, unique inside their attribute. Capture uses the source form's already resolved form composition and mastery, before the attribute's own `factor` and stat selection. This avoids multiplying an attribute back into its own source.

```json
{
  "attributes": {
    "example:absorbed_power": {
      "when": {"content": "example:absorbed_power"},
      "composition": {"inherits": [{
        "id": "example:retained_power",
        "form": "example.source",
        "mode": "snapshot",
        "capture": "once",
        "factor": 1,
        "stats": ["STR", "PWR", "DEF", "STM"]
      }]}
    }
  }
}
```

`capture: "once"` stores the first eligible resolved factors. Later mastery, source edits and race changes do not replace the captured values. `capture: "activation"` captures again after the attribute becomes ineligible and then eligible again; loading/reconnecting while already active does not count as a new activation. The source `factor` and stat selection remain live JSON settings. Changing the stable source ID creates a new capture. Java API `getSnapshots().clear(attributeId + "/" + sourceId)` explicitly removes a capture.

The server captures after committed content completion and on created-character ticks, supporting conditions that change independently of wishes. Stat queries only read. Before the first server capture, that source contributes a neutral factor; missing source forms log once and retry when available. Invalid resolved factors disable that source's captures until config reload without replacing its existing snapshot. Snapshots remain saved when their definition is temporarily removed, survive cloning/reconnection, and use full/delta synchronization. Existing JSONs retain live behavior; configVersion is unchanged.

The validated runtime, behavior factors/maintenance, aura lists, dodge reactions, Lua meter scripts and charged-Ki phase increments were merged into main at `c5c8e74a`. This does not mark the remaining scopes in their sections complete.


## Reaction policy increment

`powerWeights` replaces `powerSource` with a normalized mean of selected native scores, using scaled arithmetic to avoid overflowing finite inputs. `reactionTicks` limits attempts per defender/projectile/rule, including unsuccessful rolls and blocked displacements. It grants no damage immunity. Attempts are stored by UUID in a bounded 256-entry character-owned window and never persisted or synchronized; configuration reload and character load clear them. Defaults preserve prior reactions. See the wiki Runtime-rules page for examples.

## Combat responses

`feature/combat-responses` adds `response` selection (`dodge` default, `block`, `counter`) to the same validated reaction definitions. Block/counter processing occurs at incoming-damage LOWEST, after the mod's HIGH-priority native damage replacement. Dodge remains HIGHEST. This avoids reducing a vanilla input that native melee calculation subsequently overwrites. Costs, selection, weighted probability and projectile windows remain shared.

Counters require range/line of sight and use the existing generic strike damage source, retaining native damage/protection handling. A thread-local guard prevents recursive reaction chains during retaliation; it is removed in finally. No combat state is persisted. `dmz:blocked`/`dmz:countered` expose stage-specific measured damage inputs. See the wiki Runtime-rules guide for configuration.


## Configurable form dimensions

Implemented on `feature/form-dimensions` after authorization on 2026-11-05.
`appearance.size` supplies multiplier, height, width, eyeRatio and an optional
registered entity reference. It extends the existing per-axis body scale and
main/stack transition pipeline. Explicit height overrides reference height;
multiplier applies last. New Oozaru defaults reference the hostile NPC type.
Existing owner files remain unchanged. See the Content-configuration wiki page.
The following acceptance scope guides validation:

- Expose one consistent size configuration for every transformation, including
  per-axis visual scale, physical height/width and eye position with safe bounds.
- Resolve dimensions on the server and synchronize the result so rendering,
  movement, flight geometry, targeting and collision agree. Reuse the current
  form-size pipeline rather than introducing a second independent multiplier.
- Default the Oozaru transformation to the same standing height as the hostile
  Oozaru NPC. Derive that default from the NPC dimensions; allow server owners
  to reduce or override it in the transformation JSON. Other transformations
  retain their existing dimensions unless configured.
- Audit existing scaling fields and document precedence/migration without
  changing configVersion or discarding owner-created JSON.
- Verify transitions without size jumps, third-person rendering, ceiling
  clearance, eye position and authoritative flight hitboxes for small/large
  forms, reconnect and character cloning.
