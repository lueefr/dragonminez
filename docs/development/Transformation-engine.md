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

## Subsequent integration

Wire damage events from committed server damage, meter ownership into StatsData persistence, shared conditions through an adapter, and public-state synchronization through the existing delta mechanism. Add GameTests before enabling configured effects in gameplay. No automatic UI/Ultra Ego behavior is installed by the foundation.

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
