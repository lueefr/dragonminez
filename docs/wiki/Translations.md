# Config translations

Create UTF-8 JSON dictionaries under `config/dragonminez/lang/<locale>.json`. No resource pack or JAR editing is required. Values are display text; keys are stable identifiers.

```json
{
  "race.dragonminez.example_race.form.path.foundation": "Foundation",
  "race.dragonminez.example_race.form.path.training": "Training",
  "race.dragonminez.example_race.form.path.stage": "Advanced state",
  "race.dragonminez.example_race.group.path": "Custom path",
  "skill.dragonminez.custom_training": "Custom training",
  "example.npc.guide": "Guide",
  "example.dialogue.greeting": "Welcome.",
  "example:trial": "Complete the trial",
  "rules.dragonminez.use_denied": "Cannot use %s: %s"
}
```

Use the selected language's filename, such as `pt_br.json`, and provide `en_us.json` for the English fallback. Do not change the format placeholders of existing messages: `%s` arguments can be nested translatable components carrying their own form colors.

## Lookup order

1. Config dictionary for the receiving client's selected locale.
2. Config dictionary for English (`en_us`).
3. Bundled/resource translations for the selected locale, with the usual bundled English fallback.
4. The original translation key, unchanged.

Within each config locale, server-synchronized entries take precedence over the client's local entries. English config entries intentionally take precedence over bundled selected-locale entries. Reloading config translations invalidates cached translatable text.

The same overlay applies to names, condition descriptions, denial/warning messages, buttons, tooltips and chat components. A server sends translation keys and arguments, allowing clients with different selected languages to read different translations of the same message. Do not resolve a message into a server-localized string before sending it.

`config.dragonminez.field.<field>` overrides labels in the config editor. Extracted built-in text templates use `text.dragonminez.<class>.<identifier>`; the bundled `en_us.json` is the complete template catalog. Numeric values, player-supplied text, identifiers and external exception details remain data arguments rather than English sentence templates.

A key displayed literally indicates an exhausted fallback chain. Add it to the desired config dictionary or bundled source catalog; do not change progression IDs to fix a translation.

## Java API

- `ConfigTranslations.text(key)` creates a translatable component without resolving the server's locale.
- `ConfigTranslations.INSTANCE.find(locale, key)` returns a config override or null, allowing a language/resource layer to continue.
- `ConfigTranslations.INSTANCE.translate(locale, key)` performs config, bundled and raw-key fallback for plain-string consumers.
- `reload(directory)`, `localSnapshot`, `replaceRemote` and `revision` support reload/synchronization integrations.
- `QuestReward.getDescription(multiplier, data)` preserves player-dependent names. Pass the viewing/receiving player's `StatsData` for transformation rewards; the no-context overload cannot infer a racial form's owner. Native quest panels and world-boss results pass this context.
- Condition extensions should return translatable components from `Conditions.Type.describe`; override its context-aware variant when names require race/form context.

Renderer internals that draw individual already-resolved glyphs do not translate those glyphs a second time. This preserves the color and outline of each form-name span.


## Auditing player text

Run `python3 scripts/audit-player-text.py`. It checks bundled keys, component entry points, literal message arguments, appended text, formatted labels/units and local text initializers reaching rendering helpers. Pure numeric values remain data. The local quest update report is a diagnostic file and is excluded from the appended-UI-text rule.

The client language hook also records exhausted dynamic lookups. Inspect `ConfigTranslations.INSTANCE.missingSnapshot()` or the debug log for locale/key pairs. Entries are deduplicated and bounded; config translation reload clears them. This keeps the raw-key fallback intact while making missing dynamic translations observable. `recordMissing` is an audit hook, not a replacement for translation.

The auditor's own regression tests run with `python3 -m unittest discover -s scripts/tests -v`. CI runs both commands. Numeric formatting, player-provided values and IDs used as data are distinct from built-in sentence/label templates; new templates must go in the catalog.
