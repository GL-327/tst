# Mastery and effects — 1.14.0-preview

All 296 techniques share the new mastery curve. All 51 families receive material
effects; Limitless also has a dedicated charge, traveling release and field renderer.
Existing landed-use counts, equipment, tamed summons, character settings and saves remain valid.

## Training and buffs

Training is landed uses divided by the move's original mastery requirement. A move
that previously mastered at 50 uses reaches 100% at 50, 200% at 100 and 400% at 200.
Power grows between these points, rather than switching on a small bonus at 100%.

| Training | Power from mastery | CE cost/upkeep | Cooldown | Supported reach | Supported duration |
| --- | --- | --- | --- | --- | --- |
| 0% | 1.00× | 100% | 100% | 100% | 100% |
| 100% | 1.60× | 85% | 85% | 107.5% | 112.5% |
| 200% | 2.25× | 78.3% | 78.3% | 115% | 125% |
| 400% | 3.00× | 65% | 65% | 130% | 150% |

The cap prevents integer overflow or old creative values from producing unbounded
effects. Original per-move mastery requirements still apply. Grade, Output, Six Eyes,
personal expression and vows continue to stack with mastery. Summons inherit the
caster's output and longer lifetime. Utility and healing moves receive their supported
power/reach/duration improvements; instantaneous moves do not gain an artificial lifetime.
Area and movement speed are not indiscriminately multiplied. Behavior tuning rounds
reach and duration bonuses to whole percentages.

Blue/Red fist coatings recalculate output at contact, so training or expression changes
apply without toggling them again. The separate CE fist coating still works alongside
either spatial coating. Ordinary weapon/physical damage is not multiplied by the technique
damage setting.

New server configurations use a 3.0 offensive technique multiplier, up from 2.5.
Existing administrator configuration values are retained. Tooltips show training and
mastery power; CE costs, cooldown overlays and the expression workshop include mastery.
The creative family preset and character editor's maximum preset now use 400% training.
Existing characters retain their actual saved counts until trained or edited.

## Effect materials and aim

The atlas contains sixteen authored 256px materials: blue, red, violet and neutral plasma,
accretion wisps, impact streaks, pressure fronts, cuts, flame, blood, ice, shadow, electricity,
folds, healing flow and dispersed sparks. Smooth filtering and transparent gutters prevent
pixel stair-stepping and bleeding between materials. `tools/energy_materials.py` rebuilds it
deterministically. Family geometry adds cuts, frames, fragments, tendrils and other silhouettes.
Shrine's cage uses crossing incisions through its volume rather than a wire globe.

Blue has a dark core and inward-moving field wisps. Red has a hot core and outward pressure.
Purple combines violet plasma with red/blue streams. Traveling projectiles use the same
materials, with tick-sampled fading trails. Charges and releases no longer use opaque low-poly
balls, large white discs or orbiting wire hoops. Ordinary impacts use fewer random particles.

Local charges attach to the current camera's forward axis every rendered frame. Their center
stays on the crosshair while turning and walking, independently of the dominant hand. Server
release positions and ordinary projectile origins use the eye/aim ray, without lateral or
downward offsets. Personalized aim offsets still intentionally affect the mechanical direction.

There is one persistent charge per caster. Fields refresh existing identities. Geometry,
material counts, lifetime and distance are bounded. Low/off VFX, reduced motion and reduced
flashing remain available. The material sprites are depth-tested, so blocks can occlude them.

## Hands and Infinity

Empty-hand combat uses separate skin-mapped fists, thumbs, bent wrists and forearms.
The sleeve layer follows the player's skin settings, with slim and left-handed variants.
The render origin is the wrist; it does not run the full vanilla first-person arm transform
again. Attack windup, extension and recovery blend continuously, and the other hand keeps
its guard. Held items keep their normal renderer. Domain status remains inside the wheel.

Infinity removes the inward component of approaching motion. Targets can move sideways
or retreat, and living targets keep gravity. Approaching targets slow toward a stand-off
distance. Stationary targets outside contact distance receive no outward shove. Only an
overlapping target receives a gentle separation, capped at 0.06 blocks/tick. Incoming
projectiles suspend completely instead of leaking through at a minimum speed; leaving or
disabling the field restores their gravity and stored motion. The caster's own projectiles
remain unaffected. The floor ring is replaced by quiet boundary glints.

Infinity remains enabled by default for Limitless, independently toggleable and subject to CE
upkeep until RCT is learned. Environmental hazards, sure-hits and existing dispel rules remain.

## Verification

- `gradlew.bat test assemble`: pure mastery/Infinity regression checks and packaged client.
- `gradlew.bat runClientGameTest -PclientTestFocus=masteryVfx`: every registered move's
  mastery output, every family's material, real incoming/stationary/contact targets,
  suspended/restored projectiles, centered release origins and bounded simultaneous effects.
- Native screenshots include 1600×440 idle/downward/left-handed attacks and moving
  Blue, Red and Purple charges/releases, plus seven galleries spanning all 51 families.
- `-PclientTestFocus=masteryRelease` also runs combat coatings, maximum/200% charges,
  wheel behavior, Infinity toggles, Projection editing, respawn/persistence and QoL regressions.
- `python tools/verify_spectral_assets.py` checks existing creature/equipment/icon resources
  and the new atlas dimensions, material coverage, transparent gutters and filtering metadata.

Visual review covers representative family effects and the named casts. The automated checks
cover the entire registry; they do not establish that every possible fight has been playtested.
