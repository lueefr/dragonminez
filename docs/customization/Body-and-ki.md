# Body proportions, articulated capes and overcharge

The body tab in character creation exposes height and head sliders. **Body editor**
opens the dedicated proportions panel, retaining the character preview. Height,
head, leg length, arm length, torso height and torso width support 50–150%.
The body frame can follow the skin, use Steve, or use Alex. Bust size supports
0–125%; zero removes its geometry. Existing characters retain their original
proportions until edited through an authorized customization flow.

Proportions are bounded on the server, persisted with the character, copied with
appearance and synchronized to other players. Old saves default to 100% and the
skin's frame. Model transforms are scoped to each rendered bone and restored
afterwards, preventing accumulation or leaking between players. Height and
proportions also adjust dimensions and eye height. Both client and server require
the updated mod because appearance packet layouts changed.

## Server overcharge limit

Add this root property to `config/dragonminez/techniques.json`:

```json
"MaxOverchargePercent": 1000
```

The default remains 175. Supported limits are 100–100000; 100 disables overcharge.
The server's value is synchronized to the HUD. Existing charge speed and the
cost per overcharge percentage remain unchanged. The size multiplier continues
above 100% to the configured ceiling, including the held sphere and the launched
attack. Native Kamehameha retains its existing size behavior.

As attacks grow, their centers move outward so their near edge stays at the
cast attachment. Overhead casts move upward. Fired waves retain their authored
hand/mouth attachment instead of switching to a generic central launch point.
Terrain carving excludes a small volume around the caster and their feet.

## Capes in clothing and weights

The three existing cape segments in clothing and weighted-cape models use
per-wearer damped springs at a fixed 60 Hz in game time. Motion, vertical travel,
altitude wind, water and active Ki poses change the forces. Weights use a heavier,
more damped response. Segment limits prevent the chain from folding through the
wearer's back. Render passes share the same state, and teleport/time jumps clear
velocity rather than accumulating an impulse.

This is articulated cape physics using the existing mesh, not a remeshed cloth
surface or a general-purpose cloth simulator.
