# Character administration

These commands require operator permission level 2. The console must specify targets.

| Command | Behavior |
| --- | --- |
| `/dmzcharacter reset [targets]` | Clear character progression and equipment, stop owned Ki powers and reopen character creation. |
| `/dmzreset [targets]` | Alias of character reset. |
| `/dmzrace set <race> [targets]` | Change race while retaining base attributes and training points. Clear incompatible active forms and racial abilities; retain the current class when supported, otherwise choose the first supported class deterministically. |

Targets use Minecraft player selectors. With no target, the command applies to the executing player. Race suggestions come from the server's loaded race registry, including custom JSON races. `namek` is accepted as an alias for `namekian` unless a server race is actually named `namek`. Invalid races are rejected before player state is changed. Hair preset files are independent of character state and are not deleted.

Examples:

```
/dmzrace set namek
/dmzrace set my_server_race @p
/dmzcharacter reset @p
```

The server gameplay configuration also accepts `maxPowerRelease` (default `115`, bounded to `5..1000000`). The existing potential-unlock and racial progression is scaled relative to that ceiling, preserving the default calculation. This is the release limit, separate from technique overcharging.
