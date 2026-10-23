# Fabric 26.1.2 migration status

Status: **incomplete; no playable JAR and no Fabric release**.

## Source and target

- Branch: `port/2.2-fabric-26.1.2`; main remains the separate working 1.21.1 port.
- Starting point: NeoForge 1.21.1 port at `08ad003e`.
- Original 2.2 reference: official Forge 1.20.1 source at `d824a29c`.
- Target: Minecraft 26.1.2 / Fabric, Java 25, mod version 2.2.
- Loader/API, GeckoLib 5.5.2 and TerraBlender dependencies resolve through Loom. Team Reborn Energy 5.0.0 is included in the artifact for native Fabric energy interoperability.
- Original build and README are retained under `reference/` for migration comparison.

## Independent verification

`./gradlew verifyExtensionApiCore` compiles the extension algorithms and selected platform modules against the actual target dependencies, then runs standalone checks. These do **not** replace the complete mod compilation, mixin application, a server launch or gameplay validation.

Verified checks cover:

- Completion counters: NBT, player-data copy, identifier validation.
- Composition: recursive chains, player isolation, memoization, dynamic base changes, configuration invalidation, self-exclusion and static cycles.
- Config translations: locale/English/key fallback, synchronized overlays and atomic rejection.
- Isolated custom NPC definitions and placements, reference/duplicate validation.
- Native NBT UUID compatibility and entity ValueInput/ValueOutput flat-key preservation.
- Actual native registry binding, stable holders, registration order and duplicate rejection.
- Event priorities, subtype delivery, cancellation and failure propagation.
- Native armor components, defense, equipment slots, texture asset IDs and original durability values.
- Network scheduling, decoder arrival timing, protocol codecs and duplicate registration.
- GeckoLib model/texture extraction and render snapshot isolation.
- Exact native bytecode locations/descriptors used by damage and gameplay bridge mixins.
- Resource pack metadata generated from the target game's client/server pack versions.
- Machine inventory insertion/extraction simulation, slot validation, limits and component-aware persistence.
- Transactional energy simulation/rollback, source limits, partial transfer conservation and persistence.

## Platform migration implemented in source

These integrations still require a successful whole-mod build and runtime validation:

- Native Fabric common/client/server entrypoints and mod metadata; explicit native registry registration and required item/block/entity IDs.
- Persistent player and entity attachments, player copy/respawn/dimension/tracking synchronization, storage-before-login snapshot ordering.
- Native Fabric play payloads with all original packet handlers/codecs; configuration-phase protocol negotiation; dedicated-client class isolation.
- Tick, server/level/chunk lifecycle, player lifecycle/name/crafting, interaction and combat bridges. Explicit generated subscriber manifests preserve priority/cancellation semantics without classpath scans.
- Real native hooks for incoming/applied damage, absorption ordering, death, healing, knockback, jumping, fall damage, mob targeting, effect addition/expiration/removal, item use and equipment changes.
- Entity admission cancellation before registration, disk-load distinction, unload callbacks and dynamic player dimensions.
- Native armor material/equipment components, water-based custom fluid physics/render models, queued spawn placement registration, chest loot modification, game rules and runtime resource packs.
- Native weapon/tool materials, attack values, enchantability, repair tags and durability, including indestructible weapons. Spawn egg entity components resolve after registry construction; item model asset migration remains pending.
- Native machine inventories and extended menus; ValueInput/ValueOutput block entity persistence; Fabric item automation and transactional Team Reborn energy connections.
- Native Fabric permission nodes preserve the command permission catalog and ordinary-player defaults, administrator permission and operator fallback.
- Partial GeckoLib 5 model extraction, immutable render resource snapshots and head-bone transforms. Most renderers and the complex player model still need further migration.
- Native HUD registration and vanilla HUD cancellation hooks; HUD rendering bodies still use old graphics APIs.
- Native client input and connection/respawn callbacks, camera/FOV extraction and fog color/distance hooks. Cinematic and shoulder camera hooks target native camera alignment; the remaining renderers still need migration and runtime verification.
- Fabric integration test entrypoint and `runGameTest` configuration are present, including entity construction, resources, attachments, content completion and weapon/tool properties. These tests have not run because the complete mod cannot compile yet.

## Four extension systems

- Stats save/load/copy and quest/ritual completion hooks include generic lifetime completion flags.
- Form JSON supports completion requirements, dynamic bases and multiplier inheritance. Transformation gates, config graph validation and per-player cache adapters are wired in source.
- Config language maps synchronize to clients; language lookup and translated-component invalidation mixins are present.
- Custom NPC definitions load separately from existing NPCs. Runtime custom entities, rendering, services, placements and spawn commands remain absent.
- See the branch README for the draft API and generic JSON examples. These features are not yet validated in a running game.

## Remaining work

The full `compileJava` task still fails. The latest recorded diagnostic pass reports **637 errors**; this is not a count of all remaining work, because later compiler phases can expose additional errors.

- Finish remaining gameplay, spawning, placement, projectile, explosion and client event hooks.
- Finish native entity/item/block/recipe/world-generation/data-generation API migration and multipart entity integration.
- Complete GeckoLib 5 rendering and Minecraft extraction/submission pipelines, player appearance, hair, armor, GUI, shaders and client mixins.
- Replace the old accessory dependency with a functioning Fabric system, preserving slots, UI, persistence, synchronization and effects.
- Implement the additive custom NPC runtime and all requested services/commands, without modifying existing NPC definitions.
- Migrate generated/static resource formats and validate every datapack and client asset.
- Port and run integration GameTests, launch dedicated server and client, diagnose actual mixin/resource failures and validate every original mechanic.
- Complete the branch release workflow only after build and gameplay validation. Release names must contain `26.1.2-fabric`, keep mod version 2.2 and continue the source-version hotfix sequence.

No gameplay source directory has been excluded to obtain a successful artifact. No stub gameplay handlers replace missing mechanics. The branch workflow checks the independent modules and the complete build, and does not publish incomplete releases.
