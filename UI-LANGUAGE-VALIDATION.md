# UI, outline and language triage — NeoForge 1.21.1

## Purchase requirement names

The built-in condition descriptions passed form, skill and content identifiers as string arguments to translatable messages. Minecraft translates the outer message but does not translate string arguments. As a result, purchase refusal text bypassed config translations even though the same names worked in other screens.

Descriptions now retain nested translatable components. A context-aware default method on `Conditions.Type` supplies the player's race for `group.form` references, including nested `all`, `any` and `not` requirements. Existing Java extension types retain their original `describe(JsonElement)` implementation. No JSON format changes are required.

A new GameTest checks nested purchase requirements and their race/group translation keys. The packaged-JAR client resolved the same requirement as `Test Stage` in English and `Estágio de teste` in Portuguese, including a translated generic completed-content name.

## Charged and burst outlines

The surge border selected aura color rather than the form's outline color. Its shader also mixed the color toward white. Separately, the native burst explosion supplied a hard-coded white outline.

The charged/surge border now selects the enabled stack outline, then the enabled racial outline, and falls back to aura color when neither exists. Brightening preserves hue. The native burst uses the same form/stack color precedence through `Character.getActiveOutlineColor()`. Conditional `outline:false` remains respected by the player border.

A new GameTest activates the native surge service and checks the spawned burst's configured green outline while the aura is white. A packaged-JAR client checked an orange outline with a white aura, returning RGB `(1, 0.15686275, 0)` before and after language reload.

## Hair editor flicker

The hair editor called `PanoramaRenderer.render` with the final arguments reversed. In Minecraft 1.21.1 the order is `fade, partialTick`; the screen supplied `partialTick, 1`. This used the changing tick fraction as opacity and repeatedly faded the panorama. It was not a widget rebuild loop.

Corrected argument order in the hair editor and the pixel editor, which had the same error. Full-opacity panoramas retain normal animation timing. Screen render captures confirm that the varying-opacity flicker is removed; the background still moves with the panorama.

## Sinking after changing language — deferred until reproducible

No position/height workaround has been added. Initial reproduction with packaged Hotfix 9 did not reproduce sinking: English → Portuguese → English preserved `(0.5, 81, 0.5)`, standing dimensions and root/body/head transforms. Runs cover the default Saiyan and Frost Demon bodies, a female Saiyan, Iris/Sodium with Complementary Unbound, and GeckoLib 4.9.3 with EMF/ETF, ImmediatelyFast, 3D skin layers and RRLS.

The installed mod set also ran on NeoForge 21.1.264 with rendering configuration copies and the saved character attachment in an isolated disposable world. Position, dimensions and bone transforms remained stable through both language reloads. The original-world copy also preserved Y=73 and standing dimensions across English/Portuguese switches, walking and form changes. Camera collision against the surrounding construction limits the visual assessment at the saved position; bone transforms and collision dimensions were sampled separately. There is no confirmed root cause or fix for this fourth report. Investigation is deferred at the reporter's request until it can be reproduced. Original saves, mod JARs and configs are not modified.

## Checks and local evidence

- Java 21 `build runGameTestServer`: six unit tests and all 26 required GameTests passed. Log: `/tmp/dmz-ui-language-final-validation.log`.
- `dragonminez-port-tools/ui-fixes-smoke/candidate.log`: translated requirement names, outline RGB checks and editor captures.
- `dragonminez-port-tools/ui-language-smoke/`: baseline language/body/editor checks.
- `dragonminez-port-tools/ui-language-shader-smoke/`: baseline with Iris/Sodium and Complementary Unbound.
- `dragonminez-port-tools/ui-language-pack-smoke/`: GeckoLib 4.9.3 and rendering/reload integration checks, including the female body.
- Temporary client automation is injected only into a copy of the packaged JAR. The distributed build does not contain these classes.

- `dragonminez-port-tools/ui-fixes-pack-smoke/candidate.log`: final candidate with the installed mod set, NeoForge 21.1.264, GeckoLib 4.9.3 and active Complementary shader pack. Config-provided form names resolve in both English and Portuguese; the orange player outline is visible in `2026-10-25_01.34.14.png`. Screen render captures cover the corrected editor panorama.
- `dragonminez-port-tools/ui-language-world-smoke/baseline.log` and `baseline-facing-path.log`: copied-world baseline, including the saved character attachment, language changes, walking and transformation/reversion. No original world files were written.
