# Optional server Lua scripts

JSON remains sufficient for ordinary meters, conditions and stat factors. Use Lua to compute meter changes that are inconvenient to express as JSON. Scripts execute on the server; players do not install or run your script files.

In `config/dragonminez/progression/runtime.json`:

```json
{
  "meters": {"example:resolve": {"scope": "character", "min": 0, "max": 100}},
  "scripts": [{"id": "example:damage", "events": ["dmz:damage_received"]}]
}
```

Create `config/dragonminez/scripts/example/damage.lua`:

```lua
return {
  onEvent = function(ctx)
    local damage = ctx.values['event.damage']
    return {{meter = 'example:resolve', operation = 'add', value = damage * 2}}
  end
}
```

The ID `namespace:path` resolves to `scripts/namespace/path.lua`; paths must remain inside that directory, including symlinks. Apply changes with `/dmzreload scripts`. A compilation/loading error leaves the previous working runtime installed and does not overwrite your source. Check the server log.

`ctx.event` is the event ID. `ctx.values` contains the pre-event numeric snapshot: declared meters as `meter:<id>`, form/stack ages, health percent, attack-window sources and event-specific values. See [Runtime rules](Runtime-rules). Return `nil` for no actions, or a list of `{meter, operation, value}` commands; operation is `add` or `set` and value must be finite. JSON actions apply first, then scripts in declaration order. A failure rolls back the whole event's meter changes.

Every invocation creates a fresh environment: use declared meters for lasting state. The base language and deterministic math are available. IO, OS access, Java objects, debug, package loading, dynamic loading, coroutines and random functions are unavailable. Scripts are trusted local server files; execution limits are safeguards against accidental runaway code, not process isolation for hostile code.

Limits: 64 KiB per source, 32 script declarations, 1..32 event subscriptions per declaration, and 64 combined returned commands per event. Execution is bounded by instructions, wall time, call depth and concatenated string size. Infinite loops, including loops wrapped in `pcall`, are interrupted. Runtime errors disable that character's event rules until reload.

The current Lua contract returns meter commands. Arbitrary Java field writes, content registration, camera/HUD/model control and unrestricted gameplay mutation are not available through this interface.
