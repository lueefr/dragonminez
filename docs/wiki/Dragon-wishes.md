# Dragon wishes and race changes

Wish definitions are **server-side** and shared by every world opened by the same server installation. Edit them in the instance's `config` directory:

```text
config/dragonminez/wishes/
├── shenron.json
├── porunga.json
└── porunga_daima.json
```

Back up your existing configuration before editing and restart the server to load changes. The server validates the requirements and grants the rewards; clients receive the wish information needed to display the selection menu. Wishes remain available across worlds without copying JSON files.

### Migrating existing world wishes

When loading, the mod copies `<world>/dragonminez/wishes/*.json` into the shared configuration folder **only if that filename does not already exist there**. Both built-in and custom JSON filenames are supported. Existing server-wide files take priority; the old world files remain untouched, so customized wishes can be merged manually if both locations contain a file with the same name.

Defaults are created only if `shenron.json`, `porunga.json` or `porunga_daima.json` is missing. Repeat starts do not overwrite existing files or repeat completed migrations. Old array-format files are converted in the global folder with an exact backup under `oldBackup/`; the original world copy is preserved.

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

### IDs, text written in JSON, and translations

Each new wish should define its own **stable, namespaced `id`**. The `name` and
`description` fields can contain readable text, rather than translation keys.
When available, the client's language entries for the ID override those
strings; otherwise the text from the wish JSON is shown.

For example, inside `shenron.json`:

```json
{
  "summoning": { "wishCount": 1 },
  "wishes": [
    {
      "type": "item_list_wish",
      "id": "example:shenron_senzu",
      "name": "Senzu Beans",
      "description": "Receive 16 Senzu Beans.",
      "repeatable": true,
      "items": [
        {
          "itemType": "generic_item",
          "itemId": "dragonminez:senzu_bean",
          "count": 16
        }
      ]
    }
  ]
}
```

Language entries use the ID automatically (replace a slash `/` in the ID
path with a dot `.`):

For `assets/example/lang/en_us.json`:

```json
{
  "wish.example.shenron_senzu.name": "Senzu Beans",
  "wish.example.shenron_senzu.desc": "Receive 16 Senzu Beans."
}
```

For `assets/example/lang/pt_br.json`:

```json
{
  "wish.example.shenron_senzu.name": "Sementes dos Deuses",
  "wish.example.shenron_senzu.desc": "Receba 16 Sementes dos Deuses."
}
```

Add the entries to resource packs or the mod's actual
`assets/<namespace>/lang/<language>.json` files. Translation files are
client resources; the server does not need them to grant the rewards.

For `id: "example:shenron/extra"`, the keys become
`wish.example.shenron.extra.name` and `wish.example.shenron.extra.desc`.
If `name` is omitted, the final ID segment is used as the fallback title;
if `description` is omitted, its fallback is empty. The wish's type,
requirements, repeatability, item lists, amount, and rewards are still
configured inside the same JSON object.

Existing wishes without IDs remain supported: their old `name` and
`description` values work as translation keys as before. Newly generated
built-in wish files have namespaced IDs, and their original English and
Portuguese translations are retained under new ID-based keys. Changing a
wish ID changes its identity for nonrepeatable-wish progress; the built-in
legacy translation keys are still checked for previously completed wishes.

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
