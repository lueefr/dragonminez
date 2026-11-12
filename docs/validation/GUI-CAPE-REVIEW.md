# Added GUI, cast pose and flight cape review

Scope: `fix/cape-body-accessories`, NeoForge 1.21.1. Merge authorized only after all minor corrections are complete and validated, before Lua/Fabric work.

## Changes

- Technique style pose selector uses the existing HairEditorUi scalable button and mod click sound. Duplicate animation IDs are collapsed before cycling; Big Bang/Sokidan and Spirit Bomb/Supernova no longer cause other poses to be skipped.
- Hair editor library opener uses the correct scalable themed background. The library uses ScaledScreen, existing panel/header/field/button/font components and an inline themed delete confirmation; it no longer invokes vanilla blur or ConfirmScreen.
- Form-hair selection and custom NPC offers now use scaled themed panels, labels, buttons and bounded pagination.
- Existing added body editor, character body-editor opener and radial movement limits already use themed controls. Existing upstream controls were not replaced merely for extending vanilla widget classes.
- Custom sphere animation attachments reuse native placement calculations for Big Bang/Burning Attack, Soul Punisher, Neo Kikoho, Spirit Bomb, Cooler Supernova and Blaster Meteor. Native attacks retain their original setup/tick offsets instead of being overwritten by a shared animation mapping. Spirit Bomb and ordinary Supernova share an animation; its single custom pose uses Spirit Bomb placement, while native Supernova retains its own placement.
- During flight, cloth is attracted gently toward the animated back plane and keeps local flutter. World gravity no longer pulls it into a different shape as the wearer pitches up/down. No hard pose reset is used on collision.
- Visible, loose Saiyan tail segments contribute actual animated bone collision boxes, alongside the existing animated head/torso/limb colliders. Wrapped, hidden and absent tails do not contribute those volumes.
- Sprint/jump radial percentage controls remain 5..100 in steps of 5, reset to 100 on selection and adjust with the mouse wheel, using the flight radial's existing rendering. They appear when the corresponding skill is unlocked. Added Portuguese labels.

## Validation

`/home/lucas/Dev/dmz/gui-cape-tests3.log`: build successful; all **105 required GameTests passed**. Added tests cover native/custom sphere attachment preservation, bounded flight cloth with continuous wind and moving head/tail collision projection. Existing frame-rate independence, extreme-speed, lighting-mask and appearance/network checks remain in the suite.

`/home/lucas/Dev/dmz/cape-option-tests.log`: build successful; all **106 required GameTests passed**, including JSON defaults/null handling and persistence for the new cape option.

`/home/lucas/Dev/dmz/cape-option-client.log`: packaged-client pass for native/physics toggle (72 native vertices for clothing and 72 for weight, disabled cloth mesh and bone capture, no duplicate native mesh), appearance-preview suppression, mask/lighting equality, ten capes, charge/transformation and five real loose-tail colliders with pitch -80/+80. Technique selector, saved-hair library and form-hair selector screenshots inspected. Custom-NPC screen fixture needs a repeated visual capture because its unspawned NPC drifted out of interaction range during earlier scripted teleports; production offer packet behavior remains unchanged.

The client option is `capePhysics` in `config/dragonminez/general-user.json`, default true, exposed in the existing Visuals section. Off restores the original GeckoLib cape bones and animation/render type; simulations/collider captures stop and caches clear. Appearance editors hide capes regardless of the option, leaving the edited body/head and UI unobstructed. World and ordinary inventory cape rendering remain available.

Automated cases do not establish all modpack combinations or a guaranteed FPS. The focused client benchmark measured approximately 0.085 ms CPU submission per cloth cape and 0.259 ms per simulated tick in this run.
