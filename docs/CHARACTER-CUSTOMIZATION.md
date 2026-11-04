# Character progression and appearance configuration

These files configure gameplay; none requires a built-in special content flag.
Restart the server after editing them. Existing `configVersion` values do not need changing.

| Location | Purpose |
| --- | --- |
| `config/dragonminez/appearance/clothing.json` | Clothing that hides the female body top |
| `config/dragonminez/progression/attributes.json` | Conditional base stats, TP and form changes |
| `config/dragonminez/progression/reincarnation.json` | Enma reincarnation and retention |
| `config/dragonminez/npcs/definitions/*.json` | Custom NPC definitions |
| `config/dragonminez/npcs/placements/*.json` | Custom NPC locations |
| `config/dragonminez/races/<race>/forms/*.json` | Race forms and their properties |
| `config/dragonminez/forms/*.json` | Stack forms |
| `config/dragonminez/lang/<locale>.json` | Display names and other translated text |
| `<world>/dragonminez/wishes/<dragon>.json` | World-specific dragon wishes |

NPC files from `custom_npcs` and `custom_npc_placements` migrate after validation.
Both schema conversion and relocation keep backups in `oldBackup` and log their locations.
Invalid files remain untouched. Resolve conflicting destination files before retrying.
Core settings remain at the config root; races, forms, raids and tournaments keep their
established content folders. Quests and wishes stay in the world save.

## Wishes and character attributes

Each dragon has its own wish file: `shenron.json`, `porunga.json`, or
`porunga_daima.json` (Demon Realm Dragon Balls). For example:

```json
{
  "summoning": { "wishCount": 3 },
  "wishes": [
    {
      "type": "rewards",
      "id": "example:potential",
      "name": "wish.example.potential.name",
      "description": "wish.example.potential.desc",
      "repeatable": false,
      "requirements": {},
      "rewards": [
        { "content": "example:potential" },
        { "item": { "id": "minecraft:diamond", "count": 3 } }
      ]
    }
  ]
}
```

`wishCount` is an integer from 1 to 64. Legacy arrays migrate with an exact backup.
Use stable wish IDs: nonrepeatable wishes are remembered across summons and reconnects.
Rewards run in list order. Existing command, item, skill and other wish types still work.

The matching `attributes.json` could contain:

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

A reward can change race, preserving chosen components:

```json
{ "race": {
  "id": "saiyan",
  "characterClass": "warrior",
  "keepStats": true,
  "keepSkills": true,
  "keepMastery": true,
  "keepRacial": false,
  "keepTp": true
} }
```

Omitted keep options default to true. `originRace` remembers the initial race;
`previousRace` remembers the most recent change. Attributes can therefore retain a
consequence of being a former race. Separate wishes can offer different destination races.
`{ "customize": true }` opens the existing appearance editor. Fixed color changes use
`{ "appearance": { "bodyColor": "#AABBCC", "hairColor": "#112233" } }`.

## Referenced base multipliers and custom properties

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

## Reincarnation

Enma offers a confirmation before reopening character creation. The configuration controls:

- `enabled` and shared `requirements`.
- `retention`: stat/TP/pending-point percentages and independent flags for skills,
  mastery, inventory, content, story, techniques, effects, cooldowns, bonus stats,
  form history, alignment and race history.
- `bonus.multiplier` and `durationTicks` (`-1` means no time limit).
- `bonus.cap.enabled`; `mode` is `previous_level`, `level`, or `tp`.
- `bonus.cap.level` or `bonus.cap.tp` for fixed targets.
- `bonus.stacking.limit` (`0` unlimited), `mode` (`multiply`, `add`, `maximum`).
- `bonus.stacking.renewal`: `restart`, `keep`, or `extend` the cumulative TP budget.
- `bonus.stacking.durationRenewal`: independently `restart`, `keep`, or `extend` the timer.

The previous-level budget uses the new race/class, retained stats and the configured
stat purchase curve. It is computed when creation finishes. The counter measures **all
TP received since reincarnation**, including bonus TP. Spending does not reduce it.
An award crossing the cap only boosts the portion before the cap. Fractional TP is retained.
With the cap disabled and the default stack limit of 1, reincarnating cannot double an
existing permanent bonus. Time advances while the character is online. State survives
save/load and character cloning. Mutant ownership follows its permanent-effect rules.

## Clothing and translations

`/gamerule dmzClothingHidesTop true` enables clothing rules. The default file includes
35 existing open-torso outfits. Rules match item IDs or tags, optional `minDamage` /
`maxDamage` fractions, and `hideTop`; the last matching rule wins. For example:

```json
{ "rules": [
  { "items": ["dragonminez:gogeta_armor_chestplate"], "hideTop": true },
  { "tags": ["example:open_torso"], "minDamage": 0.5, "hideTop": true }
] }
```

Visible cosmetic armor is used. The body top and its shadow are removed; lower underwear
is retained. The texture masks follow the mod's existing female body UV layout.

Lang JSONs can rename built-in and custom forms, for example:
`"race.dragonminez.saiyan.form.path.original": "My Form"`.
No color codes are needed: hair colors are derived automatically. GUI form names use a
contrasting outline; HUD/actionbar text uses color without that outline.
Missing config translations fall back per key through the selected locale, English,
then another available config locale in alphabetical order. Bundled translations in the
selected language or English take precedence over switching to an unrelated locale.
The final fallback is the raw key; missing keys are recorded by the translation audit.
GUI notices appear near the bottom without replacing the menu or blocking clicks;
they expire after five seconds or disappear on the next click. Outside menus they use
the actionbar.

## Checking a custom setup

1. Add the example content-gated attribute, replacement form and translations.
2. Run `/dmzcontent complete <player> example:potential`.
3. Transform using the original slot, charged activation and radial. Confirm only one
   entry appears and that the alternate name, color, aura and shared mastery are used.
4. Reconnect, then exercise the server's player-clone/respawn flow and check persistence.
5. Reincarnate at Enma, select a new race and earn/spend TP. Confirm the bonus ends at
   the configured cumulative received amount, regardless of the current TP balance.
