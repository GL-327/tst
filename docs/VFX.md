# VFX additions in 1.2.0-preview

The existing world-geometry renderer now enforces a quad/CPU submission budget and has
optional mean/peak profiling logs. OFF/LOW/MEDIUM/HIGH are available, with per-family
settings for aura, trails, afterimages, impacts, domains, healing and environment.
New effects include procedural energy shells, stored-pose afterimages, healing streaks,
night ambience, summoning tendrils, Six Eyes/HR indicators and 24 fps Projection pose
sampling. Black Flash uses two-tick hit-stop and limits flash starts to three per second.
Existing reduced-motion and reduced-flashing controls remain available.

Curses use Minecraft 26.3's native cutout-dissolve render type with original per-grade
noise masks, plus a fullbright core pass. Damage-state textures and distant mesh LOD
are selected at render time. This avoids adding a custom dissolve shader pipeline.

GPU post distortion, full skinned afterimage meshes, complete sky/fog inversion,
all per-effect OFF paths and new shader-pack/backend verification remain pending.
Generated flow strips are available as assets but are not yet a verified animated layer.
