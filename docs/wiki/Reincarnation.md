# Enma reincarnation

Edit `config/dragonminez/progression/reincarnation.json` on the server and restart it. Enma offers reincarnation to an eligible dead, unfused character in Otherworld. The server checks eligibility again when confirming.

## Complete configuration

```json
{
  "enabled": true,
  "requirements": {},
  "retention": {
    "statsPercent": 0,
    "skills": false,
    "mastery": false,
    "inventory": true,
    "content": true,
    "tpPercent": 0,
    "pendingPercent": 0,
    "story": false,
    "techniques": false,
    "effects": false,
    "cooldowns": false,
    "bonusStats": false,
    "history": false,
    "alignment": false,
    "raceHistory": true
  },
  "bonus": {
    "multiplier": 2.0,
    "durationTicks": -1,
    "cap": {
      "enabled": true,
      "mode": "previous_level",
      "level": 100,
      "tp": 200000
    },
    "stacking": {
      "limit": 1,
      "mode": "multiply",
      "renewal": "restart",
      "durationRenewal": "restart"
    }
  }
}
```

Enma offers a confirmation before reopening character creation. The configuration controls:

- `enabled` and shared `requirements`.
- `retention`: stat/TP/pending-point percentages and independent flags for skills,
  mastery, inventory, content, story, techniques, effects, cooldowns, bonus stats,
  form history, alignment and race history. Effect retention includes secondary combat
  modifiers and is independent of skill retention.
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

## Retention fields

| Field | What is retained |
| --- | --- |
| `statsPercent` | Percentage of purchased character stats. |
| `tpPercent` | Percentage of the current unspent TP balance. |
| `pendingPercent` | Percentage of pending attribute points. |
| `skills`, `mastery` | Learned skills and form mastery, independently. |
| `inventory`, `content` | Inventory and content completion progress. |
| `story`, `techniques` | Quest/story state and learned techniques. |
| `effects`, `cooldowns` | Mod effects (including secondary modifiers) and cooldowns. |
| `bonusStats`, `history` | Bonus stats and form-use history. |
| `alignment`, `raceHistory` | Alignment and original/previous race records. |

Percentages are integers from `0` to `100`. Permanent mutant ownership follows
[its own removal rules](Mutant-lifetime), even when `effects` is false.
If `content` is false, a wish attribute gated by a completion flag is no longer active
after that flag is cleared. Enable retention when such wishes should survive rebirth.

## Choose the bonus policy

The following are fragments to place under `bonus`, not separate configuration files.

A fixed cumulative received-TP cap:

```json
{ "cap": { "enabled": true, "mode": "tp", "tp": 200000 } }
```

The bonus expires after **200,000 total TP received since reincarnation**, including
the boosted TP. It does not require a 200,000 TP wallet balance. Spending TP does not
restore the budget. For example, at 199,900 received TP, an incoming 100 TP with a 2×
bonus yields 150 TP: 50 of the original award is doubled, and the remaining 50 is normal.

A target level instead of the death level:

```json
{ "cap": { "enabled": true, "mode": "level", "level": 100 } }
```

The budget is the TP cost calculated from the new character's retained stats and
race/class purchase curve; it is not a check of the character's current level.

A permanent, nonstacking bonus:

```json
{
  "multiplier": 2,
  "durationTicks": -1,
  "cap": { "enabled": false },
  "stacking": { "limit": 1, "mode": "multiply", "renewal": "keep", "durationRenewal": "keep" }
}
```

For a timed bonus, use a nonnegative `durationTicks` (20 ticks per second).
Both the TP cap and timer must permit the bonus when both are enabled.
`renewal` and `durationRenewal` affect a still-active bonus on a subsequent rebirth:
`restart` starts that counter again, `keep` preserves it, and `extend` adds the new
budget or duration. An expired bonus starts a new cycle. Stack limits are configurable;
`multiply` raises the factor to the stack count, `add` adds each extra factor above 1,
and `maximum` uses one factor.

## Verify a setup

Reincarnate through Enma, finish creation, then earn and spend TP. Check the bonus
against total TP received rather than the wallet balance. Reconnect and respawn to
check retained components. With an unlimited duration and cap disabled, repeat
reincarnation and confirm the configured stack limit still applies.

Invalid reincarnation JSON is logged and left untouched; reincarnation is disabled
until the configuration is corrected. Existing `configVersion` values in other files
need no change.
