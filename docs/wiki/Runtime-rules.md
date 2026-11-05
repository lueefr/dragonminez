# Runtime meters, form behavior and reactions

Edit `config/dragonminez/progression/runtime.json` on the server. The generated default is empty: no particular transformation or combat mechanic is enabled automatically. Apply changes with `/dmzreload scripts` (runtime configuration and referenced scripts), `/dmzreload config`, or restart. Administrative permissions are required. Clients receive configuration and live values through normal synchronization.

## Accumulate a combat meter

```json
{
  "combatTicks": 200,
  "meters": {
    "example:resolve": {"scope": "combat", "initial": 0, "min": 0, "max": 100, "decayPerTick": 0.05}
  },
  "rules": [{
    "event": "dmz:damage_received",
    "when": {"content": "example:training"},
    "actions": [{"meter": "example:resolve", "operation": "add", "value": {"source": "event.damage"}}]
  }]
}
```

Meter IDs are namespaced. `character` scope survives saving, reconnecting and cloning; `combat` resets after the configured time without damaging contact; `form` resets when either the main or stack form changes. `combatTicks` accepts 1..72000 ticks. A tick is 1/20 second at the normal server rate. Only persistent character meters survive reload; they are clamped to updated bounds. Removed IDs stay saved so restoring a definition can recover their values.

`decayPerTick` moves a meter toward its initial value. Changes clamp to its bounds. Actions accept `add` or `set`. All conditions and expressions in an event read the same pre-event state; writes commit in declaration order. Invalid event processing rolls back its meter changes and disables that character's runtime rules until reload. Read the server log for the cause.

Use a meter in any shared condition:

```json
{"meter": {"id": "example:resolve", "min": 50, "max": 100}}
```

Add translations for meter IDs used in requirement descriptions. Compose conditions with `all`, `any` and `not` as usual.

## Numeric expressions

Expressions accept numbers, `{"source":"meter:example:resolve"}`, and `add`, `multiply`, `min`, `max` arrays. Curves map an input to configured points:

```json
{"curve": {"input": {"source": "meter:example:resolve"}, "points": [[0, 1], [100, 1.5]], "interpolation": "smooth"}}
```

Points must have increasing inputs; endpoints clamp outside their range. Interpolation is `linear` (default) or `smooth`. Source availability depends on the consumer; missing sources are errors.

## Stat factors and form maintenance

Add a `behaviors` object to the same file:

```json
{
  "behaviors": {
    "modifiers": [{
      "when": {"content": "example:training"},
      "factors": {"PWR": {"curve": {"input": {"source": "meter:example:resolve"}, "points": [[0, 1], [100, 1.5]]}}}
    }],
    "forms": {
      "path.focused": {"when": {"energyPercent": {"min": 10}}, "durationTicks": 600, "cooldownTicks": 100, "fallback": "path.previous"}
    }
  }
}
```

Replace form IDs with existing forms. Factors multiply the normal result; supported targets are `STR`, `SKP`, `RES`, `DEF`, `STM`, `VIT`, `PWR`, `ENE`, `SPEED`, `energyDrain`, `staminaDrain` and `healthDrain`. Factors must be nonnegative. Available inputs are declared meters, `state.formTicks`, `state.stackTicks`, `self.healthPercent` and the attack sources below.

Maintenance checks on the server each tick. A failed condition or expired duration clears the form/stack, starts its cooldown and attempts the optional fallback using ordinary activation requirements. `durationTicks: -1` means unlimited. Main and stack ages are separate. An omitted maintenance entry adds no maintenance rule.

## Attack windows

```json
{"behaviors": {"attacks": {"your_technique_id": {"activeTicks": 2, "recoveryTicks": 12}}}}
```

Use the technique's actual registry key; existing keys may be unnamespaced. Defaults are one active tick and no recovery. Active accepts 1..72000 and recovery 0..72000 ticks. These windows provide rule inputs; they do not extend projectile lifetime or add an input lock.

`attack.charging`, `attack.active` and `attack.recovery` are 0/1. `attack.ticks` is elapsed time in the current phase. `attack.charge` is charge percent, 0..200, retained through active/recovery. A new charge replaces the current window. Phases synchronize to clients and reset on character load/clone or runtime reload.

## Native events and inputs

| Event | Trigger |
| --- | --- |
| `dmz:tick` | Created character tick after meter decay |
| `dmz:form_changed` | Main or stack change, after form-scope reset |
| `dmz:combat_enter` | First damaging contact out of combat |
| `dmz:combat_exit` | Combat inactivity timeout |
| `dmz:damage_received`, `dmz:damage_dealt` | Committed server damage |
| `dmz:evaded` | Successful configured dodge |
| `dmz:attack_started`, `dmz:attack_released` | Charged Ki technique start/release |
| `dmz:attack_cancelled` | Charge interrupted |
| `dmz:attack_recovery`, `dmz:attack_finished` | Configured phase transition |

Damage events expose `event.damage`, `event.originalDamage` and `event.blockedDamage`; `event.damage` is final committed damage. JSON event actions can read declared meters and attack sources. Attack cancellation/finish events observe idle. Attack adapters currently cover charged Ki techniques, not all strike/basic-blast paths.

## Force-relative dodge

```json
{
  "reactions": [{
    "id": "example:instinct",
    "when": {"content": "example:instinct"},
    "attacks": ["melee", "projectile"],
    "powerSource": "battlePower",
    "hitChance": {"curve": {"input": {"source": "event.powerRatio"}, "points": [[0.5, 0], [1, 0.5], [2, 1]]}},
    "distance": 1,
    "staminaCost": 0.02,
    "energyCost": 0,
    "cooldownTicks": 10
  }]
}
```

This is the probability of being hit, not of evading. `event.powerRatio` is attacker power divided by defender power: a stronger defender lowers hit chance. Curve endpoints can produce 0% and 100%. Both powers zero yields ratio 1. Expressions may also read declared meters. `powerSource` accepts `battlePower`, `STR`, `PWR` or `SPEED`. Other living entities use native battle power when supplied, otherwise max health as a fallback score.

The first matching reaction replaces the native speed-dodge roll for that attack category. Environment damage is excluded. Successful evasion needs sufficient resources, cooldown readiness and a loaded, bounded, collision-free displacement path; a wall cannot grant free damage cancellation. Energy/stamina costs are fractions of maximum resources in 0..1; distance accepts 0.1..4 blocks and cooldown 0..72000 ticks. Animation and position are synchronized by the server.

For optional scripts that use the same meters, see [Lua scripts](Lua-scripts).

### Weighted power and continuous projectiles

Replace `powerSource` with `powerWeights` to combine sources:

```json
{
  "reactions": [{
    "id": "example:weighted_instinct",
    "attacks": ["projectile"],
    "powerWeights": {"STR": 3, "PWR": 1},
    "hitChance": {"curve": {"input": {"source": "event.powerRatio"}, "points": [[0.5, 0], [1, 0.5], [2, 1]]}},
    "cooldownTicks": 0,
    "reactionTicks": 10
  }]
}
```

Weights form a normalized weighted mean, so this example uses 75% strength and 25% Ki power for both fighters. Choose weights appropriate to your stat scales. Accepted keys are `battlePower`, `STR`, `PWR`, `SPEED`; weights accept 0..1000000, with at least one positive weight. Use either `powerSource` or `powerWeights`.

`reactionTicks` limits attempts against the same projectile and defender, separately for each rule. It accepts 0..72000 ticks, default 0 (no additional limit). A failed probability roll or blocked displacement consumes that attempt window too. Another projectile gets its own attempt. Contacts during the interval deal damage normally: this is not an invulnerability period. The ordinary reaction cooldown still applies separately. Attempt windows are temporary and reset on runtime reload, character load or clone.

### Blocking and counters

`response` defaults to `dodge`. Choose `block` for damage reduction or `counter` for reduction with an immediate retaliation:

```json
{
  "reactions": [{
    "id": "example:guard",
    "when": {"content": "example:guard"},
    "response": "block",
    "attacks": ["melee", "projectile"],
    "hitChance": 0.25,
    "damageFactor": 0.2,
    "staminaCost": 0.03,
    "cooldownTicks": 10
  }]
}
```

The example attempts a block on 75% of eligible contacts; successful blocks retain 20% of the damage reaching this stage. `damageFactor` is in 0..1, defaults to 0, and a `block` must reduce damage. This factor applies after the mod's incoming damage calculation; armor and later damage processing still apply. Full reduction cancels damage. Blocking does not move the defender or require a displacement path.

For a counter, use `response: "counter"` and optionally `counterFactor` (default 0.5, positive up to 10) and `counterRange` (default 3, 0.1..32 blocks). Retaliation uses incoming damage at this stage times `counterFactor`, through the native generic strike damage path. It requires a living attacker in range and line of sight. Normal target protection, armor and damage handling remain in effect; retaliation can be rejected by the target. Reactions cannot chain recursively from retaliation damage. `counterFactor`/`counterRange` are invalid on other response types, and defense fields are invalid on `dodge`.

Responses share the existing conditions, probability curve, resources, cooldown and per-projectile interval. A matching rule replaces native speed dodge. `hitChance` is the chance of bypassing the response; a successful block may still leave residual damage.

Events `dmz:blocked` and `dmz:countered` expose `event.originalDamage`, `event.damage` (remaining incoming damage) and `event.blockedDamage`. `countered` is emitted when the native retaliation accepts damage; otherwise the defense emits `blocked`. These are measurements at the defense stage, not final post-armor damage. Use `dmz:damage_received`/`dmz:damage_dealt` for final committed damage.
