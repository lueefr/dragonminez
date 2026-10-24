# Changelog

## 2.2 - Hotfix 7 — Minecraft 1.21.1 (NeoForge)

### Fixed

- Corrected the placement of aura outlines and sparks relative to the player.
- Improved muscle overlay rendering with shaders; the texture remained visible in checks with shaders disabled and with Iris plus Complementary Shaders r5.9.2 using `bodyType=1`.
- Put Otherworld landmarks on the normal per-chunk feature pipeline. A Distant Horizons 3.3.1 and DH Seedgen 0.8.0 check confirmed generation and persistence of the tested Otherworld chunk; a Distant Horizons FullData cell at the corresponding position was also persisted.

The Distant Horizons check covers that setup only. It does not establish Voxy, other Seedgen, or universal LOD compatibility; an addon that skips custom chunk features may omit landmarks. See [Hotfix 7 validation](HOTFIX-VALIDATION.md).

## 2.2 - Hotfix 2 — Minecraft 1.21.1 (NeoForge)

### Changed

- Publish a numbered GitHub hotfix release for every update to the main branch, resetting the sequence when the DragonMineZ source version changes.

## 2.2 - Hotfix 1 — Minecraft 1.21.1 (NeoForge)

### Fixed

- Fixed muscle shadow overlays appearing excessively contrasted on player models in-world.

## 2.2.0 — Minecraft 1.21.1 (NeoForge)

This release ports DragonMineZ 2.2 to Minecraft 1.21.1 on NeoForge.

### Changed

- Migrated the mod from Forge to NeoForge, including registration, events, networking, menus, entities, dimensions, attributes, spawning, and library integrations.
- Updated player data persistence to serializable NeoForge data attachments while preserving DragonMineZ 2.2 progression fields, cooldowns, and double-precision training points.
- Migrated item behavior to Minecraft data components, including enchantments, enchanted books, potions, trims, Senzu Bags, maps, and fireworks. Added datapack definitions for all nine mod enchantments.
- Preserved Pattern Station recipes up to 6×6 with JSON codecs and custom network synchronization.
- Updated rendering, HUD, camera, models, and mixins for the 1.21.1 APIs.
- Converted datapack resources to current directory names and formats, including loot tables, recipes, advancements, tags, and biome modifiers.
- Updated build and release workflows for Java 21, NeoForge, and the 1.21.1 dependency line.

### Requirements

- Minecraft 1.21.1 and Java 21.
- NeoForge 21.1.238 or a later 21.1 release.
- GeckoLib 4.9.2, TerraBlender 1.21.1-4.1.0.8, and Curios 9.5.1+1.21.1 for NeoForge.
- JEI is optional. Weapon Leveling and Apotheosis integrations are optional.

### Validation

- Data generation, build, integration tests, and a normal-world client smoke test completed for this port.
- Legacy Forge 1.20.1 world migration and binary addons from the previous version have not been validated.
