# Rules, rewards and form visuals

These optional JSON fields extend the existing configuration. Keep them absent to retain the existing visuals, hairstyle selection and completion behavior. No addon is needed for the built-in conditions or rewards.

## Locate a custom NPC

```text
/dmzlocate npc example:guide
```

The command suggests IDs from `custom_npcs`. It uses the existing `/dmzlocate` permission. In the current dimension it prefers a loaded instance, then a configured placement. If neither exists there, it checks other dimensions. Results include the dimension and a clickable teleport suggestion using the NPC's Y coordinate.

A placement can be located before its chunk is loaded. Locating does not generate chunks, spawn the NPC, or force-load dimensions. A command-spawned NPC without a placement can only be located while loaded. Existing structure lookup syntax is unchanged.

## First-completion rewards

Create `config/dragonminez/rules.json`. Each content ID maps to a list of actions:

```json
{
  "content": {
    "example:trial": [
      {"form": "path.stage"},
      {"mastery": {"form": "path.stage", "add": 25}},
      {"content": "example:follow_up"},
      {"tp": 100}
    ]
  }
}
```

The existing completion API, native quest hooks and configurable ritual completion ID all use this mechanism. Completing `example:trial` once grants its rewards automatically. Later completions still increase the completion counter but do not grant these rewards again. Content completed before the rule was added is not rewarded retroactively.

| Action | Behavior |
| --- | --- |
| `form` | Permanently raises the player's skill to at least the referenced form's `unlockOnSkillLevel`. The skill is determined from that group's `formType`. Requires a skill-backed form in the player's race or stack configuration; scripted forms are not permanent skill unlocks. |
| `skill` | `{"skill":{"id":"custom_training","level":2}}` raises a configured skill to at least level 2. It never lowers an existing level. |
| `mastery` | Adds the specified mastery points to the referenced form, capped by its configured maximum. This is an exact grant, without racial mastery multipliers. |
| `content` | Completes another content ID through the same API, including that ID's own first-completion rewards. |
| `tp` | Adds the exact number of whole training points, without party sharing or gain multipliers. |

Skill rewards respect configured maximum levels. Prices of `-1` still allow rewards to grant the skill; no purchase or administrator command is needed. A reward does not remove the form's separate use prerequisites.

Content-to-content cycles are rejected with a configuration error. Chains are limited to 64 content IDs. If a built-in action fails, the chain restores the player's mod state, including completion flags; reward and completion events are emitted only after a successful chain. Custom Java actions must handle any non-mod state they modify, such as external storage.

An absent `rules.json` supplies no rewards and no additional global purchase rules. Reload with the existing mod config reload command after editing JSON. Do not change `configVersion` on the existing race/form files just to add these optional fields.

## Purchase requirements

The same `rules.json` can require conditions for a specific skill level. Keys are **target levels**, not the level before buying:

```json
{
  "buy": {
    "custom_training": {
      "*": {"content": "example:trial"},
      "2": {"mastery": {"form": "path.stage", "min": 40}}
    }
  }
}
```

`*` applies to every purchased level. The target-level rule is added to it. In this example, buying level 2 requires both the content flag and 40 mastery points. Combine `content` and `buy` in one file when using both features.

A form can also add a requirement directly to its own JSON entry:

```json
{
  "name": "stage",
  "unlockOnSkillLevel": 2,
  "buy": {
    "content": "example:trial",
    "mastery": {"form": "path.foundation", "min": 50}
  }
}
```

Global skill rules and form rules are cumulative. The form's existing level, saga, completed-content and mastery prerequisites also gate the skill level that unlocks it. Existing `formRequisiteType` controls whether referenced masteries require all or any. When several forms share a skill level, their requirements are checked together, as with the existing level/content gates.

Checks run on the server for both master purchases and menu upgrades, before TP is removed or the skill changes. Refusals identify missing content, mastery, skill or level requirements. The client uses the server-synced rules for the existing lock descriptions. Prices, master range, alignment and other existing purchase checks still apply. Java/completion rewards intentionally bypass purchase gates.

## Conditions

Condition objects are shared by `buy` and visual rules. All keys in an object must match. Use `all`, `any` and `not` for composition:

```json
{
  "content": "example:trial",
  "any": [
    {"mastery": {"form": "path.foundation", "min": 50}},
    {"skill": {"id": "custom_training", "level": 3}}
  ]
}
```

| Key | Value and meaning |
| --- | --- |
| `content` | Content ID completed at least once. Native quest completion IDs also work. |
| `level` | Minimum player level. |
| `skill` | Object with `id` and minimum `level`. |
| `mastery` | Object with `form` (`group.form`) and minimum `min` mastery points. |
| `used` | `group.form` used at least once by this player in the current race. |
| `race` | Exact configured race ID. |
| `first` | Boolean: whether this is the form's first activation session. Visual rules supply their own form as context; purchase rules do not have a current-form context. |
| `flying` | Boolean: the mod's flight skill is active. |
| `charging` | Boolean: a mod action is charging. |
| `aura` | Boolean: the underlying aura/permanent-aura/forced-aura state is active. This reads gameplay state, not a visual rule's output. |
| `grounded` | Boolean: player is on the ground. |
| `sneaking` | Boolean: player is sneaking. |
| `water` | Boolean: player is in water. |
| `night` | Boolean: the current dimension reports night. |
| `all` / `any` | Array of condition objects. |
| `not` | One condition object to negate. |

Boolean conditions accept `true` and `false`. Empty objects match. `all: []` matches; `any: []` does not. Nesting is limited to 32 levels. Unknown condition keys fail validation after addon registration.

## Body parts, colors and conditional effects

Add `visual` inside a form entry. This example uses one optional object and an ordered list of overrides:

```json
{
  "name": "stage",
  "unlockOnSkillLevel": 2,
  "visual": {
    "parts": {
      "left_forearm": [1.5, 1, 1.5],
      "right_forearm": [1.5, 1, 1.5]
    },
    "fur": {"texture": "file:coat", "color": "#AABBCC"},
    "tail": "#886644",
    "eyes": "#663399",
    "aura": false,
    "lightning": false,
    "outline": false,
    "rules": [
      {"if": {"first": true}, "set": {"lightning": true, "outline": true}},
      {"if": {"flying": true}, "set": {"aura": true}}
    ]
  }
}
```

On the first activation session, lightning and the energy outline are enabled. On subsequent activations they remain disabled. The aura is visible while flying. Conditions do not change the player's flight, aura toggle, stats, or other gameplay state.

Rules run in array order; later matching `set` objects override earlier values. `set` uses the same visual fields as `visual` but cannot contain another `rules` list. Base-form visuals are resolved first and stack-form overrides afterward. Omitted values keep the existing renderer's result. Visual evaluation is cached per player/tick and invalidated by form, history and config changes.

| Field | Usage |
| --- | --- |
| `parts` | Stable part IDs mapped to `[x,y,z]` scale factors, or `false` to hide that part. Factors must be finite and between 0.05 and 8. |
| `fur.color` | `#RRGGBB` tint independent of hair/body color. Omit `texture` to recolor an existing fur layer. |
| `fur.texture` | Custom UV-aligned, transparent fur mask. `file:coat` loads `config/dragonminez/form_textures/coat.png`; a normal `namespace:textures/...png` ID uses a resource asset. An explicit texture also permits a fur overlay on a model that has no built-in fur selection. |
| `fur: false` | Hides the fur overlay. |
| `tail` | `#RRGGBB` tint for the existing tail, or `false` to hide it. Does not grant a tail to a race without one. |
| `eyes` | `#RRGGBB` for the colored marking around the eyes on the supported fur face layouts, independent of iris and fur color; `false` hides the marking. |
| `hair` | Boolean: show/hide rendered hair. |
| `aura` | Boolean override for aura visibility. |
| `lightning` | Boolean override for lightning visibility. Existing lightning color/style settings continue to apply. |
| `outline` | Boolean override for the transformation energy outline. Existing outline shader settings continue to supply its style. |

Supported stable part IDs:

| ID | Geometry |
| --- | --- |
| `head` | Head and attached child bones. |
| `body` | Torso and its child bones. |
| `left_arm`, `right_arm` | Entire arm and attached child bones. |
| `left_forearm`, `right_forearm` | Explicit forearm bone, or the lower half of an unsplit standard arm. The latter is joined smoothly at the elbow and retains the original UVs. |
| `left_leg`, `right_leg` | Leg and attached child bones. |
| `tail` | Tail root and descendants. |

Scales compose with whole-model scaling and parent-bone transforms. They are cosmetic and do not change hitboxes or reach. A missing anatomical part has no effect. Addon models can register further stable part IDs through the Java API.

External form PNGs are local client files, just like NPC PNGs. Install them on every client that should see them. They are not transferred from the server. The renderer detects file changes while drawing; PNGs are limited to 4 MiB and 2048 × 2048 pixels. Invalid files retain the existing available fur appearance. Custom masks must match the model's UV layout; their own shading replaces the bundled fur shadow layer.

## Lifetime first-use history

History is stored with player mod data, scoped by race and `group.form`. Base forms and stack forms are tracked independently. It survives saves, reconnects and player cloning.

An activation counts when it remains active at the end of a server player tick. Failed attempts, previews and temporary changes reverted within the same tick do not consume first use. `first` remains true for the first active session and becomes false after leaving it. Reconnecting during that same session does not create a second first use.

For saves created before this feature, currently active forms are treated as previously used, without replaying first-use effects. Historical uses of other forms cannot be reconstructed. Completion flags and this usage history are separate systems.

## Player-owned form hairstyles

The optional `editHair` flag controls who supplies the hairstyle's shape:

```json
{
  "name": "stage",
  "unlockOnSkillLevel": 2,
  "editHair": true
}
```

- `true`: the form appears in **Edit hair** in character customization. The player gets a separate saved hairstyle for this form. Its shape overrides `hairType` and `forcedHairCode` in the form JSON. Until a personal style is saved, the ordinary base hairstyle is used. Existing form hair-color settings still control transformation color.
- `false`: no editor entry is shown. Declare `forcedHairCode` in JSON, or `hairType: "none"` for no hair. Player-owned form styles are ignored, including styles saved before the flag was turned off.
- Omitted: the existing hairstyle-resolution behavior is preserved.

Example of a JSON-owned hairstyle with no hair:

```json
{"name":"stage","editHair":false,"hairType":"none"}
```

For a fixed visible hairstyle, set `editHair: false` and paste a complete valid Hair Salon export into `forcedHairCode`. Do not abbreviate its code. The player-owned form editor uses the existing strand editor; ordinary hairstyle slots remain available separately. Creation, recustomization and the existing nearby appearance-service permission checks still apply on the server.

## Java extension API

Register condition/reward types and body-part aliases in the addon constructor, on both client and server. Validation is finalized during common setup, after constructors have run. Duplicate registrations are rejected.

```java
Conditions.register("addon:ready", new Conditions.Type() {
    public void validate(JsonElement value) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean())
            throw new IllegalArgumentException("addon:ready requires a boolean");
    }
    public boolean test(Conditions.Context context, JsonElement value) {
        return customState(context.data()) == value.getAsBoolean();
    }
    public Component describe(JsonElement value) {
        return Component.translatable("addon.requirement.ready");
    }
});
```

JSON can then use `{"addon:ready": true}` wherever a condition object is accepted. Condition evaluators must be side-effect-free and work on both sides using synchronized state. `Context.data()` provides player stats and `Context.form()` is the visual rule's `group.form`, or `null` for a purchase.

```java
Rewards.register("addon:grant", new Rewards.Type() {
    public void validate(JsonElement value) { validateCustomReward(value); }
    public void apply(ServerPlayer player, StatsData data, JsonElement value) {
        applyCustomReward(player, data, value);
    }
});

FormVisuals.registerPart("addon:crest", Set.of("crest_bone"));
```

The reward above can be used as `{"addon:grant": ...}` in a content reward list. Actions execute on the server thread. Do not perform network or disk operations from visual condition evaluators.

Listen on the NeoForge game event bus:

| Event | Data |
| --- | --- |
| `ContentCompletedEvent` | Existing completion event: player, content ID, kind, count and first completion. Emitted after configured rewards succeed. |
| `Rewards.Applied` | `getPlayer()`, `getContent()` and a copy of the action JSON from `getReward()`. Emitted for each committed action, not for repeated completion with no reward. |
| `FormUsedEvent` | `getPlayer()`, `getForm()` and `isFirst()`. Emitted once per actual activation, after the active state and history are available. |

`StatsData.getFormHistory()` exposes `used(race, form)`, `count(race, form)`, `first(race, form)` and `revision()`. Let the server tracker own mutations. `FormHair.editable(race)` lists editor entries; `FormHair.name(race, form)` resolves their usual form translation key with a raw-name fallback. `FormVisuals.parts()` lists stable anatomical aliases; `resolve(data)` provides the resolved visual object for the current tick, which callers must treat as read-only. `Conditions.validate/test/missing` lets addons reuse validation, matching and requirement descriptions.
