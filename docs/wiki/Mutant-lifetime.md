# Permanent mutant ownership

Mutant ownership is stored separately from character progression in the world's saved data. Death, story resets, character recreation and ordinary effect-clearing do not revoke it. The visible status effect is restored from that ownership record if necessary.

## Configuration

In `config/dragonminez/general-server.json`, configure the existing `mutant` section:

```json
{
  "mutant": {
    "enabled": true,
    "chance": 0.2,
    "initialChance": 0.2,
    "maxHolders": 1,
    "retryEnabled": false,
    "retryChance": 0.2,
    "rollIntervalMinutes": 30,
    "playersPerRoll": 1
  }
}
```

| Field | Behavior |
| --- | --- |
| `enabled` | Enables automatic attempts. Explicit administrative grants remain available. |
| `chance` | Shared probability fallback when a separate chance is omitted. Range 0–1. |
| `initialChance` | Optional initial-attempt probability. |
| `maxHolders` | Maximum world ownership records allowed by automatic rolls. Zero prevents automatic grants. Offline holders count. |
| `retryEnabled` | Enables periodic retries. **False by default**, including old files without the field. |
| `retryChance` | Optional retry probability, falling back to `chance`. |
| `rollIntervalMinutes` | Retry interval, with a minimum of one minute. Has no effect when retries are disabled. |
| `playersPerRoll` | Maximum eligible online players considered per retry round. |

The first automatic attempt occurs after the player has a character, on login/creation or the next server check. The attempted flag is saved even if the player loses the roll or the holder limit is already full. Recreating a character, reconnecting or changing the configured chance does not provide a second initial attempt. Turning retries off does not clear attempt history.

Existing native mutant effects are adopted into ownership records, and existing holder records restore the effect. The previous `keepMutantOnDeath` behavior is superseded by permanent ownership; a historical false value cannot strip the trait.

## Explicit removal with confirmation

Use the existing administrative effect command to request removal:

```text
/dmzeffect remove mutant <targets>
/dmzeffect confirm_mutant
```

The first command states how many players would be affected. The same command source must confirm within 30 seconds. The confirmation requires administrator level 2. General effect clearing does not revoke mutant ownership.

Revocation records a tombstone in world data, so an older character/effect snapshot cannot resurrect a revoked trait. Administrative grants explicitly restore ownership. Revocation does not erase the player's consumed initial attempt.

For trusted server integrations, `MutantManager.grant`, `reconcileHolder`, `rollForPlayer` and `runLottery` preserve these rules. `revoke`/`revokeOffline` are explicit administrative operations; do not call them during lifecycle events.
