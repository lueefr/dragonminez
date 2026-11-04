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
