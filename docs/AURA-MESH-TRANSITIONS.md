# Configuring aura geometry transitions

`aura.transitionSeconds` controls the transition between 3D aura profiles as well
as their opacity. Smooth, sparking and god profiles interpolate their radial
mesh shape, deformation parameters and colours during the transition. Rapid
changes start from the currently visible mixture. Both the visible pass and the
bloom pass use the same interpolated shape. Pausing the game pauses this motion.

This affects the built-in 3D profiles. Custom 2D sprite auras continue to crossfade
their textures. Existing configurations do not need migration.
