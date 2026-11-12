# Space pod destinations

Destinations are server owned. Clients receive the same ordered entries and icons on login, datapack reload and `/dmzreload`; an open travel menu refreshes immediately. Travel still checks the unlock rule, the destination dimension and the player's actual pod on the server. Landing and passenger behavior remain the native travel implementation.

## JSON datapack

Create `data/<namespace>/spacepod/<name>.json`. Files merge after the bundled list, in resource ID order. A destination replaces an existing entry with the same `id` or `slot`; otherwise it adds an entry. A root `"replace": true` clears the previous list, so omit it when extending the defaults.

```json
{
  "destinations": [{
    "id": "example:moon",
    "slot": 1,
    "name": "Moon",
    "translate": false,
    "dimension": "minecraft:overworld",
    "icon_texture": "example:textures/gui/moon.png",
    "x": 100,
    "y": 100,
    "z": 100,
    "show_when_locked": true,
    "unlock_rules": "ALWAYS"
  }]
}
```

The example replaces Namek's slot. Use a free slot, such as `8`, to add a destination. Default slots: Overworld `0`, Namek `1`, Otherworld `2`, Sacred Kai Planet `3`, Cereal `4`, Beerus `5`, Time Chamber `6`, Demon Realm `7`. Slots range from `0` to `255`; entries without a slot follow numbered entries. These are positions in the scrollable destination list, not passenger seats.

`icon_texture` points to a square PNG supplied by the client's resource pack; icons render at 11×11 pixels. Alternatively, `icon_index` selects a row in the bundled icon atlas. A custom texture takes precedence. `name` is a translation key when `translate` is true. Set all three coordinates, only `y`, or omit all coordinates to retain native landing behavior. Coordinates must be finite and within ±30,000,000.

Unlock rules support the existing `ALWAYS`, `NEVER`, quest and compound expressions. A dimension must exist on the server to travel there; listing an addon dimension does not create it.

## Lua content

A controlled Lua module can add or replace a destination using the same value fields:

```lua
return {
  content = {{
    type = "spacepod_destination",
    id = "example:moon",
    mode = "replace",
    value = {
      slot = 1,
      name = "Moon",
      translate = false,
      dimension = "minecraft:overworld",
      icon_texture = "example:textures/gui/moon.png",
      x = 100, y = 100, z = 100,
      unlock_rules = "ALWAYS"
    }
  }}
}
```

Declare the module in the runtime configuration's existing `scripts` list. `mode = "add"` requires a free ID and slot; `"replace"` requires an existing ID or slot. Lua overlays apply after datapack destinations, validate before installation, and do not overwrite the owner's JSON. Removing the module restores the datapack destination. Use `/dmzscript check` before `/dmzreload scripts`.
