# NeoForge correction release

Scope: Minecraft 1.21.1, Java 21, branch `fix/cape-body-accessories`. This release stops before implementing the Lua Extension Engine or starting Fabric work. Existing JSON/Lua extension points remain intact. Original vehicle seating and Space Pod teleport fixes were already integrated and were not reimplemented.

## Changes

| Area | Result |
| --- | --- |
| Appearance | Themed body editor with adjacent unblurred model; uniform overall height, separate head/torso/arm/leg proportions, transverse width/depth, Alex/Steve frame and zero bust. Head-only hair/eye thumbnails and fitted full-body previews. Vanilla first-person arm follows width/depth. |
| Capes | Fixed-tick bounded simulation, detailed interpolated mesh, animated attachments and body/head/limb/loose-tail colliders, wind and charging/transformation lift. All nine clothing capes and Piccolo weight cape share environment lighting and matching outline geometry. Physics toggle restores native upstream geometry. Equipment cape is suppressed in appearance editors and thumbnail contexts. |
| Flight and aura | Search pose requires half the current forward-speed cap; lateral/backward motion does not trigger it. Prone head follows the local travel frame; non-head-owned animations retain free look. Animation movement sampling uses elapsed game ticks; aura emission depends on current state instead of time since last rendered frame. |
| Techniques | Themed native pose selection and pose-specific sphere attachments. Growth preserves near edges; synchronized/saved baselines and reload-safe original charge size. Configurable overcharge and growth gamerule retain the Kamehameha exception. Fired trajectory planes protect terrain behind the emitter, including Heat Dome. Reverting Oozaru removes mouth attacks and timed scream damage/effects. |
| Release control | Second Alt + same slot releases the caster without deleting the attack. Frozen trajectory, ownership, damage, geometry and original expiry survive; no homing, following, movement lock or renewed lock from child volley shots. Server validates equipped slot and ownership, including powers outside local tracking; other charging/firing animations are preserved. |
| Character and menus | Global hair preset library; themed form-hair/NPC menus; scoped form-name colors. Sprint and jump percentage controls use the flight radial interaction. Rebirth clears vanilla, Curios and native Accessories functional/cosmetic/invalid stacks. Server Limit Release ceiling and operator reset/race commands support validated custom races. |
| Integrations | Nearby visible NPC attention includes demon residents/time patrollers. Space Pod destinations retain JSON/Lua icons and append/replace slots. Optional Wakes Ki/flight bridge is bounded to four blocks. Known DH/Voxy SeedGen entry points reject Otherworld, leaving native generators enabled. |

## Executed verification

- `minor-release-final.log`: build and all **123 required GameTests passed** with native Accessories and its dependencies loaded. Includes all predefined Ki attacks through the real charge/fire dispatcher, actual terrain carving on both sides of emission planes, charge reload/growth/config/gamerule/Kamehameha cases, actual timed Rage Scream cancellation, distant/parked release, other-charge preservation, movement restrictions, native sprint attribute and 40 Accessories slots.
- `minor-final-client3.log`: packaged production JAR plus external test fixture; five GUI screens, ten capes, native-geometry toggle, 1024 matching visible/outline vertices, environment light, loose tail and flight pitches, appearance framing, and sprint/jump selection/wheel/reset over actual C2S/S2C passed.
- Low-FPS client test measured **592–863 ms between rendered frames**. Charging pose/growth/cache identity remained stable; an emitter still spawned particles after a 100-game-tick render gap. This is a focused regression test, not a promise about arbitrary animation combinations.
- Clean inspected screenshots: `combat-visual-smoke/game/screenshots/2026-11-13_00.14.22.png` (NPC dialogue clipping) and `2026-11-13_00.14.24.png` (body editor). Harness root is `/home/lucas/Dev/dmz/dragonminez-port-tools/`; logs are under `/home/lucas/Dev/dmz/`.

- `dedicated-multiplayer-final2.log`: **DEDICATED_TWO_CLIENTS_PASS**. Two independent client JVMs connected to a real dedicated server over TCP, with Curios, Accessories, its compatibility layer and owo installed. Both clients and the server confirmed C2S/S2C sprint/jump limits; clients verified the other player's appearance proportions and an owned synchronized sphere. This is targeted multiplayer regression coverage, not a claim that every gameplay system has been exhaustively exercised in multiplayer.

- `minor-final-ki-client3.log`: **FINAL_KI_WAKES_CLIENT_PASS**. Client metadata matched held baselines; 125%→150% growth preserved the rear edge; fired sphere remained clear of its caster. Real C2S release synchronized RELEASED, kept the beam alive and maintained direction after the caster moved/turned (direction dot product 0.99999997). Installed Wakes 1.4.1 accepted both Ki and player ripples, rejected effects beyond four blocks and through solid cover.
- Production JAR excludes all external client/server fixture classes. English and Brazilian Portuguese language JSON parsed successfully.

Production JAR SHA256: `2d02e4fe6552f93b800e56ad0f6a91702b803de6bbb401e38e7932ebb8d3f997`.

## Practical limits

The cape test measures bounded simulation/render work and frame-rate independence; it does not guarantee 60 FPS for an arbitrary modpack, GPU or shader. Optional SeedGen hooks target the known public implementations, tolerate missing mods and do not claim compatibility with unknown future internals. Tests do not establish every possible third-party mod combination or every custom animation. No Lua engine or Fabric parity claim is made by this correction release.
