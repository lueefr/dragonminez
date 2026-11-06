# World generation and other terrain mods

Rocky uses TerraBlender's region system and the active Minecraft climate/noise
sampler. It does not select biomes using an independent coordinate grid or
replace the Overworld generator. Changes made by compatible mods to those
shared channels therefore affect Rocky alongside the native biome slots.
Rocky is classified with common hot/dry Overworld, sparse-vegetation and
wasteland tags, so tag-based biome modifiers can include it.

The Rocky surface rule follows the generator's preliminary surface rather
than a fixed Y threshold. Bedrock remains owned by the active generator.
Native carving defaults are included once; DMZ does not add duplicate canyon
passes. DMZ's rock formations remain normal placed features on that terrain.

## Region settings

Edit `config/dragonminez/general-server.json`, inside `worldGen`:

```json
{
  "rockyWeight": 40,
  "rockyAnchors": [
    "minecraft:badlands",
    "minecraft:eroded_badlands",
    "minecraft:wooded_badlands"
  ]
}
```

Weight is 0..1000; 0 disables the DMZ Overworld region. Inside that region,
the listed native climate slots map to Rocky. Other TerraBlender regions keep
their own biome choices. Anchors must be biome IDs occurring in the native
layout used by TerraBlender; merely registering a biome does not create a slot
for it. Keep 1..64 IDs. Invalid syntax or an unregistered anchor uses the native defaults and logs
the error without rewriting your file. Weight/anchor changes require a full
Minecraft/server process restart because TerraBlender registers regions at
mod setup. TerraBlender's own region-size settings remain in its configuration.

## Share terrain or climate with a dimension

`worldGen.noiseSources` maps **noise settings IDs**, not dimension IDs, to
registered source settings. The default declarations are:

```json
{
  "noiseSources": {
    "dragonminez:namek": {
      "source": "minecraft:overworld", "terrain": true, "climate": true
    },
    "dragonminez:sacredkaiplanet": {
      "source": "minecraft:overworld", "terrain": false, "climate": true
    }
  }
}
```

Namek shares the active Overworld terrain/climate channels while retaining
Namek blocks, water level, surface rules and biomes. Sacred Kai shares climate;
its special cave-free terrain remains native. Layered dimensions such as
Otherworld and Demon Realm are not automatically replaced with Overworld
terrain. They may have explicit bindings if that is the world design you want.
An explicit empty object (`"noiseSources": {}`) disables these bindings.

Sources resolve from the loaded registry after datapacks/mods have supplied
worldgen data; no Tectonic-specific class or mod-ID check is involved. A mod
that changes `minecraft:overworld` supplies the source selected above. You
can select `minecraft:large_biomes` or any other registered noise settings ID.
Targets can also belong to other mods when their generators use those
registered settings through Minecraft's native `RandomState` pipeline.

`terrain` borrows density/aquifer/ore-noise channels. `climate` borrows
temperature, vegetation, continentalness, erosion, depth and ridges. At least
one must be true. Each source is the active registry definition, not another
binding's derived result. There are at most 64 bindings. Unknown IDs or invalid
bindings log an error and retain native samplers without changing the file.

Bindings preserve the target's blocks, fluids, surfaces, sea level, vertical
bounds, mob/aquifer/ore enable flags, biome source and placed features/carvers.
They do not copy a foreign dimension wholesale or enable a completely custom
generator that does not use this pipeline. A temperature-only change affects
temperature-based biome choices; a planet whose biomes differ by another
climate channel still follows that channel. Source terrain taller than the
target's bounds remains limited by the target dimension; change dimension
resources separately if you intend to change its physical height.

Restart the server/world after editing bindings. `/dmzreload` does not replace
samplers already used by loaded dimensions. The adapter only affects newly
constructed samplers: it does not rewrite saved generator definitions, convert
existing chunks or regenerate builds. Changing terrain mods or settings can
change newly generated terrain at existing-world boundaries; there is no
promise of automatic seam repair. Compare generation in a new test world.

## Adjust formations using native biome modifiers

A server datapack can retain external terrain shaping while removing the DMZ
rock formations from Rocky. For example, create
`data/example/neoforge/biome_modifier/rocky_formations.json`:

```json
{
  "type": "neoforge:remove_features",
  "biomes": "dragonminez:rocky",
  "features": [
    "dragonminez:stone_spike_placed",
    "dragonminez:rocky_peak_placed",
    "dragonminez:karst_pillar_placed",
    "dragonminez:rocky_cliff_placed"
  ],
  "steps": ["raw_generation"]
}
```

This is a normal NeoForge biome modifier, not a second DMZ generation engine.
Use your pack's native tools for biome-size settings and resource overrides.
Compatibility depends on which pipeline an external mod actually changes;
mods that replace or bypass TerraBlender/native noise entirely need their own
integration. No SeedGen or LOD system is enabled by these settings.
