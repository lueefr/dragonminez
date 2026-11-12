# Dynamic cape physics plan

Scope: design only, as requested. Apply one shared simulation to clothing and
weighted capes (`DMZCapeLayer`, `DMZWeightCapeLayer`, `ArmorCapeRenderer`,
`WeightCapeRenderer`), with separate material parameters; do not change armor
stats or make this a server-side simulation.

## Simulation and attachment

Use a small cloth grid (5 columns by 7 rows) in player-local space. Pin the
shoulder row to the actual animated torso transform. Integrate Verlet positions
at fixed 1/60-second steps with an accumulator, at most four steps per frame;
discard excess backlog after long stalls. Interpolate previous/current states
for rendering. Reset on equip, dimension change, teleport or model/race change,
never because a rendered frame was skipped. Keep states per player UUID and
cape slot; remove disconnected entities. Run offscreen at reduced frequency.

Solve stretch, shear and bend constraints for 5–8 iterations. Constrain cloth
against torso/leg capsules transformed from the current pose, and clamp maximum
stretch. Integrate actual player scale so giant/small forms behave consistently.
The cape root follows the chest, not the camera or an independent world yaw.

## Forces and anime direction

- Gravity and measured acceleration produce hanging, jumping and braking motion.
- Relative airflow opposes movement. Flight/sprinting increases drag, pushing the
  cape behind the body; falling folds it upward. Turning imparts lateral inertia.
- Altitude adds a smooth world-space wind field. Blend its strength with height
  above local terrain (cached samples), precipitation and a stable dimension/seed
  phase. High altitude gives sustained one-direction flow with small gusts,
  instead of random jitter every frame. Indoors suppress wind using cached sky
  visibility, leaving movement forces active.
- Ki charge adds small radial flutter. A beam or barrage applies a sustained
  backward airflow impulse opposite the attack direction, with smooth onset and
  release. This gives Piccolo's swept-back cape silhouette without teleporting
  vertices or repeatedly restarting an animation. A large attack adds a brief
  initial impulse, followed by bounded sustained pressure.
- A crouch/landing impulse uses torso acceleration; idle cloth settles with
  damping. Do not use FPS or camera motion as force inputs.

## Rendering and editable profiles

Existing rigid cape geometry needs cloth vertices or segmented bones; retain the
current texture UVs. Start with segmented cape bones for compatibility, then add
an optional mesh path. Include vanilla skin capes using the skin's cape texture
and the same animated torso anchor; avoid drawing a second cape over an elytra
or an equipped clothing cape. Render after the animated torso, preserving bone state and
lighting; both clothing and Curios weights use the same attachment resolver.

An editable JSON profile per cape/material should expose length, width, mass,
stiffness, damping, drag, wind response, ki response and collision margin, with
finite bounds. Heavy weights use stronger damping and lower wind/ki response;
cloth remains expressive. Allow `enabled: false` and a low-quality segment mode.
JSON/Lua may select a validated profile, but must not run arbitrary per-vertex
Lua each frame. Preserve missing/unknown custom fields on reload.

## Implementation stages and acceptance

1. Add deterministic solver and tests for pins, stretch limits, timestep/FPS
   independence, teleport reset, collisions and finite output.
2. Connect both cape renderers and test standing, flight, crouching, riding,
   small/giant forms and cosmetic/weight slots.
3. Connect stable altitude wind and attack state, then tune Piccolo beam/charge
   silhouettes using captured references at 15, 30, 60 and 144 FPS.
4. Add profiles, LOD and UI toggle. Verify no per-frame allocations, bounded
   update cost, multiplayer isolation and cleanup; test Iris/Sodium, resizing,
   first/third person and resource reload. Keep native animation fallback until
   these checks pass. Implementation is not included in this change.
