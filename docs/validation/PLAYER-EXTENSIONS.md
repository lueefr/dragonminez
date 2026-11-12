# Player polish and content extensions

Requested scope excludes the already merged mounting and Space Pod travel fixes
(PRs25/26). Cape physics is a design document, not an implemented solver; see
`docs/development/Cape-physics.md`.

Changes include forward-speed flight poses at 50% of the current cap, mouse head
tracking outside transformations/head-owning animation clips, input-edge Ki
charge packets, existing-technique pose selection with a server whitelist, and
pose-aware energy sphere offsets. Network revision 20 requires matching builds.

Aura alpha and pulse no longer reset because rendering skipped two game ticks;
alpha uses elapsed time and pulse uses game time. Culling a player model no
longer suppresses an active aura shell. NPC attention is continuous within a
five-block spherical range and requires line of sight.

Saved hair presets use client-installation storage and retain complete hair NBT,
independent of character IDs/worlds/races. Applying them uses existing server
customization permissions. See `docs/wiki/Hair-presets.md`.

Space Pod destinations support datapack JSON and Lua content definitions, slot
replacement/addition and custom icons. Existing travel rules and arrival logic
are preserved. See `docs/wiki/Space-pod-destinations.md`.

Optional Wakes bridge targets the 1.21.1 NeoForge 1.4.1 API. Source and installed JAR
signatures were checked: [Wakes 1.21.1 source](https://github.com/Leclowndu93150/Wakes/tree/main/versions/1.21.1).
Flight (including hover/combat) and Ki affect visible water no more than four
blocks below them, attenuated by distance and stopped by intervening geometry.
No Wakes dependency is imposed.

Otherworld seed approximation guards target DH SeedGen 0.9.1 and Voxy SeedGen 0.3.0.
They do not disable native Distant Horizons or Voxy chunk generation. Optional
mixin targets/signatures were checked against local decompiled sources; unknown
versions fail open. No universal hook exists for arbitrary seedgen mods; new
implementations require a matching integration. Live optional-mod injection and
visual water behavior require client verification; source checks alone do not
establish those results.

## Verification

- Java 21 `build` and all 84 required GameTests passed:
  `/tmp/dmz-polish-checks.log`. New coverage includes five-block NPC attention,
  solid-wall rejection, persisted hair NBT including connected joints, custom
  destinations, pose limits/whitelists, wire roundtrip and native projectile pose
  metadata transfer. The NPC fixture clears the GameTest boundary from its sight
  corridor so only its explicit wall blocks visibility.
- Packaged resource JSON/model-parent validation passed. Player-text audit passed
  (6971 bundled keys); all 5 auditor regression tests passed.
- Client component checks with Iris/Sodium/GeckoLib 4.9.3 confirmed scoped
  skill/radial/hotbar colors, uncolored ordinary screens/prose/“Super Forms”,
  stable aura growth at an explicit 5 FPS limit, and actual Wakes 1.4.1 node
  insertion with distance/solid-cover rejection. Wakes amplitude was corrected
  after its integer conversion rejected the initial normalized value.
- Complete external client harness passed (`/tmp/dmz-polish-client-verified.log`):
  `COMBAT_VISUAL_FINAL_PASS`, `LOW_FPS_AURA_PASS`, `FORM_NAME_RENDER_PASS`,
  `WAKES_OPTIONAL_PASS`, and normal shutdown. The older unrelated meter fixture
  was restored after removal of its retired-flight setup; its HUD proof also
  passed. Fixture classes are external and are not shipped in the production JAR.
- Final Java 21 packaging build passed in `/tmp/dmz-polish-release-build.log`.
  Optional seedgen injections remain source-verified, not live-tested; no claim
  is made that arbitrary/future seed approximation implementations are blocked.
