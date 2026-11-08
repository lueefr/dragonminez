# Dragon wishes and race changes

Edit `config/dragonminez/wishes/<dragon>.json` in the server installation (outside the world directory). Restart the server after editing wishes. On first load, legacy JSON files are copied from `<world>/dragonminez/wishes/` only when the corresponding server-wide file does not yet exist. Legacy world files remain untouched and server-wide files take precedence; the bundled defaults never overwrite existing files.

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

## Race and appearance rewards

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

## Connect a wish to a persistent attribute

A `content` reward records a completion ID; it does not create a buff on its own.
Define the corresponding condition and effects in
[`progression/attributes.json`](Character-attributes). The wish and attribute IDs may
be different: their connection is the ID in `rewards.content` and `when.content`.
Add the wish's name, description and displayed completion IDs to the
[translation dictionaries](Translations).

Each entry in `rewards` is a separate action. Inventory items use a registered item ID
and a positive `count`; item quantities are independent of `summoning.wishCount`.
Use the shared [condition vocabulary](Use-and-purchase) in `requirements` to restrict
eligibility. Nonrepeatable tracking uses the dragon ID and stable wish ID (or name when
no ID is supplied); changing those identifiers changes the saved identity of the wish.

Race changes select a configured race/class rather than presenting a race picker.
Use one wish per destination, or `customize` for the existing appearance editor.
An item ID from another mod requires that mod to be installed.
