# Character attributes, properties and form variants

Edit `config/dragonminez/progression/attributes.json` on the server, then restart it. The initial file has no predefined special traits. Attribute IDs are namespaced identifiers chosen by you.

For a potential bonus granted by a [dragon wish](Dragon-wishes), use:

```json
{
  "attributes": {
    "example:potential": {
      "when": { "content": "example:potential" },
      "base": { "all": 1.5 },
      "tp": 1.25,
      "stackLimit": 1,
      "stacking": "multiply",
      "forms": {
        "replace": { "path.original": "path.alternate" },
        "block": ["path.forbidden"],
        "grant": ["path.alternate"]
      }
    }
  }
}
```

Replace the example form references with forms present in your race configuration.
`base` multiplies the character before transformations: base × 1.5 × form multiplier.
Specific stats (`STR`, `SKP`, `RES`, `VIT`, `PWR`, `ENE`, `DEF`, `STM`) can override
individual factors; `all` and the specific factor multiply together. Values below 1
are debuffs. Purchased stats, level and purchase costs remain unchanged.

Conditions use the shared vocabulary, including `all`, `any`, `not`, `content`,
`mastery`, `race`, `originRace` and `previousRace`. For example:

```json
{ "all": [
  { "content": "example:potential" },
  { "originRace": "glind" }
] }
```

A form replacement uses the original slot, key and chain position, while using the
replacement's appearance, costs and use requirements. Its separate entry is hidden.
Mastery reads and writes through the original form, including other forms' requirements.
Define the alternate form normally; it can inherit multipliers from the original.
Normal activation checks blocks on the server. Forced administrative activation remains
available with a warning. No named flag or native form replacement is installed by default.

For counted bonuses, set `stackContent` to a completion ID. Completion count determines
stacks; `stackLimit: 0` means unlimited. Modes are `multiply`, `add`, and `maximum`.
Different active attribute definitions multiply together.



## Inherit resolved power and declare properties

An attribute can inherit resolved form multipliers, including composition chains and
mastery, without activating that form or changing the base appearance:

```json
{
  "attributes": {
    "example:absorbed_power": {
      "when": { "content": "example:absorbed_power" },
      "composition": {
        "inherits": [
          { "form": "path.source", "factor": 1.0, "stats": ["STR", "SKP", "PWR"] }
        ]
      },
      "properties": { "example:ki_signature": "example:special_energy" }
    },
    "example:perception": {
      "when": { "content": "example:perception" },
      "properties": { "example:perceived_energy": ["example:special_energy"] }
    }
  },
  "detection": [
    {
      "channels": ["ki_sense", "lock_on", "instant_transmission", "scouter"],
      "mode": "replace",
      "target": {
        "property": { "id": "example:ki_signature", "equals": "example:special_energy" }
      },
      "observer": {
        "property": { "id": "example:perceived_energy", "contains": "example:special_energy" }
      }
    }
  ]
}
```

`composition.inherits` multiplies the selected source factors into the numeric `base`
factors. Multiple sources multiply together. Empty `stats` means all supported stats;
`RES` covers both `DEF` and `STM`. `factor` scales that source. TP is independent.
If a source resolves to 21 and the numeric base factor is 3, the effective base factor
is 63; a subsequent transformation of 2 produces 126. References read composition
and mastery, without copying other character attributes, temporary effects or the
character's complete final multiplier. A missing source for the current race logs a
warning and contributes a neutral factor; use race conditions for race-specific sources.
A referenced form can itself use dynamic `best`/`current` composition.

Properties are namespaced values, independent of native flags. Values may be strings,
booleans, finite numbers, or arrays of scalar values. Later matching attribute definitions
win when several define the same property. They are derived from conditions rather than
saved as a separate mutable copy. Saved completion flags therefore preserve wish-granted
properties across reconnects, while changing the JSON updates their behavior.

The shared `property` condition supports `equals`, `contains`, `present`, `min`, and `max`.
Conditions can use a property to activate another attribute, replace a form, block its use,
or gate a wish. Avoid circular property dependencies; recursive lookups return an unset
value rather than recursing indefinitely. Add lang entries for property IDs and symbolic
values when they appear in requirement descriptions.

Detection rules evaluate `target` against the detected player and `observer` against the
viewer. Empty `channels` applies to all channels. `restrict` adds a requirement to native
detection; `replace` replaces the native result for matching targets. Rules run in order.
Unmatched targets keep native behavior. These rules affect ki sense, lock-on, scouters,
and instant transmission; lock-on and teleport decisions are also checked on the server.
They do not bypass distance, line-of-sight, skill or other travel requirements.
No particular energy type, form name or completion flag is built into this system.

## Attribute fields

| Field | Meaning |
| --- | --- |
| `enabled` | Enables this definition; defaults to `true`. |
| `when` | Shared condition object; `{}` matches every character. |
| `base` | Nonnegative stat factors; omitted factors are `1`. Also accepts `SPEED`. |
| `tp` | TP gain factor; defaults to `1`. |
| `composition.inherits` | Resolved form sources added to the numeric base factors by multiplication. |
| `properties` | Derived namespaced properties; values have no mechanics unless a rule consumes them. |
| `stackContent` | Optional completion ID whose completion count determines the stack count. |
| `stackLimit` | Maximum count; default `1`, `0` means unlimited. |
| `stacking` | `multiply` (default), `add`, or `maximum`. |
| `forms.replace` | Original slot to replacement form, both `group.form`. |
| `forms.block` | Form references denied for ordinary activation. |
| `forms.grant` | Form references made available by this attribute. |

For a factor of `1.5` and two stacks, `multiply` gives `2.25`, `add` gives `2`,
and `maximum` gives `1.5`. These are attribute stacking modes, separate from
[additive form composition](Aura-and-composition).
Properties and form changes apply while the definition matches and has at least one
stack; their values are not multiplied by the stack count.

Replacement targets share mastery with their source even while the trait is inactive.
Avoid defining two different sources for one target, or circular replacement chains:
validation rejects ambiguous or cyclic mastery aliases. Source and target forms must
exist for the race that uses the rule; gate race-specific definitions with `race`.

## Verify a setup

1. Create the source/replacement forms and their translations.
2. Add the content-gated attribute and restart the server.
3. Run `/dmzcontent complete <player> example:potential`.
4. Use the original slot through the radial and charged transformation. Check that one
   entry appears, with the replacement name, colors and aura, and shared mastery.
5. Reconnect and respawn to check persistence. Completion progress survives cloning;
   JSON-derived properties are recalculated from the current rules.

These files configure server behavior and synchronize the required data to clients.
Friends need the same compatible mod version, not a manual copy of these JSONs.
External PNGs and other added assets have their own distribution requirements; see
[custom NPC textures](Home#external-npc-textures).
