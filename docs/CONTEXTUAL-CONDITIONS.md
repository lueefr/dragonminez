# Contextual conditions for content JSON

Every condition below works in purchase requirements, form use requirements and
visual `if` rules. Keys in the same object are ANDed. Combine alternatives with
`any` and exclusions with `not`. They do not change existing files when absent.
Gameplay requirements are evaluated by the server; visual rules use the client's
synced character state and its local view of the world.

| Condition | Value |
| --- | --- |
| `healthPercent`, `energyPercent`, `staminaPercent` | Inclusive `min`/`max` range, 0–100. |
| `powerRelease` | Inclusive release percentage, 0–1000 (also supports releases above 100%). |
| `altitude` | Inclusive Y-coordinate range, −2048–2048. |
| `timeOfDay` | Inclusive world-clock range, 0–23999 ticks. Use `any` for a range crossing midnight. |
| `dimension` | Namespaced dimension ID, such as `minecraft:the_nether`. |
| `biome` | Namespaced biome ID, including registered mod biomes. |
| `heldItem` | Namespaced item ID in the main hand. `minecraft:air` means empty. |
| `activeForm`, `stackForm` | A `group.form` reference to the current base or stacked form. |
| `effect` | `{"id":"minecraft:speed","minAmplifier":1}`; amplifier is optional, zero-based, integer 0–255. This example requires Speed II or stronger. |
| `raining` | Boolean: rain at the player's position, accounting for shelter and biome. |
| `thundering` | Boolean: the world's thunderstorm state. |
| `sprinting`, `swimming`, `underwater`, `burning`, `gliding`, `moving` | Boolean movement/environment state. Moving means horizontal speed greater than 0.05 blocks/tick, or vertical speed above that threshold while airborne; grounded gravity is ignored. |

Ranges must contain `min`, `max`, or both. Values must be finite and ordered;
invalid ranges and fractional effect amplifiers are rejected during config load.
These are state checks: a use condition is checked when transforming and does not
automatically eject a player from the form after their state changes.

For example, put this inside a form's `requirements.use` to allow it only in
one dimension while energy is sufficient and the player is not burning:

```json
{
  "all": [
    {"dimension": "minecraft:overworld"},
    {"energyPercent": {"min": 40}},
    {"not": {"burning": true}}
  ]
}
```

An optional visual rule can activate an aura while sprinting or gliding:

```json
{
  "if": {"any": [{"sprinting": true}, {"gliding": true}]},
  "set": {"aura": true}
}
```

Dimension, biome, item and effect IDs refer to installed content. Registering an
ID here does not create that content. Condition-description translation keys use
`rules.dragonminez.<condition>` and may be overridden in config language files.
