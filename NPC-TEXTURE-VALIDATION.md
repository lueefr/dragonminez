# External custom NPC texture validation — NeoForge 1.21.1

- Java 21 `./gradlew build`: passed (`/tmp/dmz-npc-external-texture-final-build.log`). All six unit tests passed.
- `./gradlew build runGameTestServer`: all 14 required GameTests passed (`/tmp/dmz-npc-external-texture-validation.log`). The new integration test checks independent texture/model persistence and synchronization, legacy aliases, and invalid filenames.
- Isolated Minecraft client loaded the reviewed JAR copy with three custom NPC body models. Male and slim used the external default, while the female used a cyan `custom_outfit.png` selected by filename. Hair Salon hair and cosmetic armor remained visible.
- Client startup created `config/dragonminez/npc_textures/master_base.png`. Its SHA-256 matches the original master texture and bundled seed: `e1ca6148d218243f12d565083a2d2d913c0ca40b09d0aae7def8482d05f45edd`.
- Client log: `/tmp/dmz-api-visual/run-external-texture.log`; scene: `/tmp/dmz-api-visual/game-client/screenshots/2026-10-24_17.13.28.png`. The probe completed and the client exited successfully.
- File-change detection is implemented but this run did not capture an in-game edit before the automated client exited. No multiplayer image transfer is implemented: PNGs must be installed on each client.

Existing masters, player textures, progression, and world generation were not changed by this patch.
