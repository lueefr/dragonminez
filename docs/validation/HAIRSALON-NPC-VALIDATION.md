# Hair Salon codes on custom NPCs — validation

## Implementation

Custom NPC JSON accepts `hairCode` as an alias for the existing `forcedHairCode` field. Saved/synchronized definitions retain the canonical `forcedHairCode` key. Clipboard whitespace/invisible separators are normalized. The existing decoder reads current Hair Salon exports: `DMZ22:` is one style; `DMZF22:` is a set whose selected style now follows `hairType` instead of always selecting BASE.

The NPC renderer preserves imported global/strand/tip/segment colors instead of forcing a dark color. Numeric presets retain their previous palette behavior. Empty styles are cached and remain empty. Registry/entity snapshot limits are unchanged: a serialized definition must fit 30,000 characters.

## Interoperability and native checks

Inspected `dragonminez-web` at commit `413276750d3090e5a1bc51dd5606e2780180e41d`, particularly `src/lib/hair/codec.ts`, `nbt.ts`, `customHair.ts`, `editorState.ts` and `CodeClipboard.tsx`. No site code or dependencies are added to the production mod.

Generic test exports were generated using that web codec/NBT writer in `/tmp/dmz-hairsalon-fixtures`. The isolated runner substitutes Node raw zlib for pako and lowers one TypeScript constructor parameter-property into equivalent assignments; the serialization layout is unchanged. The exported values are embedded in `HairSalonNpcGameTests`.

`test build runGameTestServer` passed six unit tests and thirteen required native GameTests. The three new native tests cover a full-set non-base slot, geometry and global/strand/tip/segment colors, entity/JSON persistence, single-style import, the legacy JSON field, Unicode clipboard cleanup and rejection of a malformed export. Log: `/tmp/dmz-npc-hairsalon-final-validation.log`.

## Minecraft client check

An isolated client spawned three custom NPCs using web-encoded designs: a full-set base slot, another slot from the same set, and a single-style code. The full and single exports used in this run were 6,194 and 1,682 characters. The inspected scene shows distinct red/green imported colors and the selected style shapes. Cosmetic armor and the service screen also rendered; the client exited successfully.

Screenshot: `/tmp/dmz-api-visual/game-client/screenshots/2026-10-24_16.31.28.png`. Log: `/tmp/dmz-api-visual/run-hairsalon-codes-retry.log`. The initial attempt failed while preparing the temporary JAR due to disk quota, before Minecraft launched; removing a disposable temporary build cache allowed the retry.

The client check used the reviewed JAR with only a temporary automation class injected into a separate copy. The production JAR contains no `portsmoke` or `lodclient` automation classes. These checks establish the exported format and inspected designs; the exact long user-provided code was not used as a fixture, and not every possible Salon design or shader combination was exercised.
