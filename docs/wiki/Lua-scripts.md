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

Limits: 64 KiB per source, 32 script declarations, up to 32 event subscriptions per declaration, and 64 combined returned commands per event. Execution is bounded by instructions, wall time, call depth and concatenated string size. Infinite loops, including loops wrapped in `pcall`, are interrupted. Runtime errors disable that character's event rules until reload.

Lua returns typed meter/gameplay commands and can declare content at loading. Arbitrary Java field writes and direct camera/model control are unavailable. Synchronized meters can drive configured HUD widgets and dynamic aura parameters.


## Typed gameplay actions

Scripts can return an entry with `reward` instead of a meter command. It uses
the same action objects as JSON wishes/content rewards:

```lua
return {
  onEvent = function(ctx)
    return {
      {meter = 'example:spirit', operation = 'add', value = 1},
      {reward = {resource = {id = 'energy', add = -2}}},
      {reward = {effect = {id = 'minecraft:regeneration', ticks = 40, amplifier = 0}}}
    }
  end
}
```

`resource` accepts energy, stamina or health and a finite `add` delta within
-1e9..1e9; existing resource bounds still apply. `effect` uses a registered
effect ID, 1..72000 ticks and amplifier 0..255. `activate` requests a form
through its normal server access checks:

```lua
{reward = {activate = {form = 'example.stage', stack = false}}}
```

Existing action types such as content, skill, form unlock, mastery, TP, race,
appearance and item use their normal JSON schemas. Content IDs and form names
remain entirely configurable. Scripts return data, never Java references.

All script commands are converted and validated before meter changes commit.
A batch has at most 64 combined meter/reward commands. Meter updates remain
transactional. Gameplay actions run afterwards in order; they can produce
external effects and are not rolled back as a group. If an execution fails,
further event rules for that character stop until reload; logs say whether
the meter commit already occurred. Design rules to avoid repeating permanent
rewards every tick (for example, use content conditions).


## Declare forms and attributes

A module can return `content`, with or without `onEvent`. For a content-only
module, subscribe with just `{"id":"example:content"}` in `runtime.json`.
The definitions load on the server and use the same schemas as the JSONs;
clients receive the resolved forms and attributes through normal config sync,
without downloading or executing Lua source.

```lua
return {
  content = {
    {
      type = 'form_group', id = 'example/forms', race = 'saiyan',
      value = {
        identity = {groupName = 'example/forms', formType = 'falseform'},
        forms = {
          focus = {
            name = 'focus',
            appearance = {hair = {color = '#FF1234'}},
            stats = {multipliers = {strength = 2}},
            requirements = {use = {content = 'example:gift'}}
          }
        }
      }
    },
    {
      type = 'attribute', id = 'example:gift',
      value = {
        when = {content = 'example:gift'},
        base = {STR = 1.5},
        forms = {grant = {'example/forms.focus'}}
      }
    }
  }
}
```

This example grants the configured form when the content is completed;
`falseform` is the existing form category for content-granted forms rather than
skill purchases. Use `stack_group` without `race` for a shared stack group.
Use `attribute` without `race` for an attribute; its `when` selects eligibility.
Group IDs use lowercase letters, numbers, underscore, slash or hyphen;
attribute IDs use `namespace:path`. A group's `identity.groupName` must match
its ID, and each form key must match its `name`. Translate the normal form name
keys through the [translation configuration](Translations).

`mode` defaults to `add`, which rejects existing IDs. `replace` requires an
existing definition and replaces that entire group or attribute: include all
fields/forms you want to retain. Definitions cannot duplicate each other in
one reload. Existing composition, visual, size, condition and attribute
validators still apply, including composition cycle/missing-reference checks.

Definitions are overlays in memory, not generated JSON files. Removing a
script and reloading restores the previous owner definitions; newer owner
objects loaded from disk are preserved. `/dmzreload scripts` prepares and
validates all definitions before changing the live registries. Failure keeps
the previous working runtime and content. Startup errors leave owner files
untouched and disable the invalid script contribution; inspect server logs.
Changing an owner JSON under a script replacement does not make that script
replacement disappear: remove or edit the script, then reload scripts.

Limits: 64 definitions per module, 128 across scripts, and a 4096-node,
16-level startup data conversion budget per module. Returned gameplay commands
retain their smaller existing limits. Ordinary Lua tables represent objects;
nonempty consecutive integer tables represent arrays. Use JSON files when an
explicit empty array is needed. Register content only during module loading;
event callbacks cannot dynamically mutate registries. No new Minecraft entity
or block registry type is created by these declarations.
