# VFX specification

What every visual effect looks like, what triggers it, and how the options tone it down. All
effects are client side and driven by small server events (`TechniqueFxPayload`,
`DomainEventPayload`, `BlackFlashPayload` and so on): the server decides what happened, the
client only draws it.

## 1. Principles

- No post-processing shaders and no sky or fog hooks. World effects are geometry drawn with
  vanilla render types: `lightning()` (additive, for glows, rings, beams, slashes) and
  `JjkRenderTypes.dome()` (translucent, for domes and barriers). Screen effects are HUD layers.
  This keeps everything working with Sodium and Iris, and lets "custom shaders off" tone effects
  down without breaking them.
- `dome()` is vanilla's `debug_quads` pipeline under the mod's own name. A shader pack draws only
  pipelines Iris has a program for, and Iris has none for `debug_quads`: with a pack on, domes and
  barriers were not drawn at all. With Iris installed, `IrisCompat` assigns the mod's pipeline the
  pack's basic program (see `docs/TEST_REPORT.md`, "Compatibility runs").
- Additive light can't draw darkness: dark looks come from translucent geometry (domain domes,
  colony barriers) or from bright rims around an unlit centre.
- Cursed-energy light (1.18) uses the mod's own `jjkcraft:pipeline/glow`: additive, textured
  from `textures/vfx/glow_atlas.png` (`tools/glow_atlas.py`), depth-tested without writing
  depth, with a small fragment shader (`shaders/core/glow.fsh`) that fades light out with fog
  instead of mixing toward the fog colour. Each glow effect also draws an alpha-blended tint
  pass first, so colours stay saturated against a bright sky. `GlowFx` holds the shared looks:
  Blue (inward streaks and an accretion disk), Red (outward streaks and a pressure ring),
  Purple (a dark violet mass with a lit rim, crackling arcs and, while charging, a blue and a red
  mote that merge), generic orbs, impact flashes and comet tails. A glow the camera is inside
  fades away so releases never fill the screen. Iris assigns the pipeline its emissive-entity
  program.
- Every strong effect has a tell: windups draw a glyph ring and wisps spiralling in for the whole
  cast time, so others can react.
- Budgets: world effects share a quad budget per frame, particles are capped
  (`vfx.maxParticlesPerFrame`, 600), and Low VFX halves segment counts.

## 2. Options

| Option | Effect |
|---|---|
| `vfx.level` LOW | fewer segments and particles, shorter effect distance, still overlays |
| `vfx.customShaders` off | animated screen distortion becomes a still tint |
| `vfx.cameraShake` off | no camera shake |
| `vfx.hitStop` off | no visual freeze on heavy hits (the server never pauses) |
| `accessibility.reducedMotion` | no shake, no hit-stop, no sliding stripes, no blooming wheel, titles shown whole |
| `accessibility.reduceFlashing` | flashes become steady washes: the Black Flash frame, the domain title flash, the overexertion pulse, the flood flicker |

## 3. Techniques

Each technique's data picks a colour, a particle, sounds, camera shake, hit-stop and a trail.

| Phase | Drawn |
|---|---|
| Windup (cast time) | a glyph ring at the caster's feet and wisps spiralling into their hands for the cast time |
| Cast, `slash` trail | a crescent ribbon in front of the caster, tilted like a diagonal cut |
| Cast, `line` trail | a thin glowing line along the cut or path, with particles |
| Cast, `beam` trail | a heavy line with a white-hot core (Piercing Blood, Thousand Lances' falling lances, Positive Energy Output's beam) |
| Cast, `shout` trail | a cone of rings from the caster's mouth; targets flash above their heads |
| Cast, `orb` trail | a burst at the hand; the bolt entity carries the look (below) |
| Hit | particle burst in the technique colour and white sparks, the hit sound, shake and hit-stop for those involved; `chain` draws a chain from caster to target |
| Field | a glyph ring at the field's radius with particles spiralling in (attract) or out (repel) |
| Burst | an expanding shockwave ring sized to the effect, shards, a heavy sound, shake nearby |
| Channel or toggle | an aura around the caster while it lasts |
| Block (barrier) | shards where the blow lands |

Bolts (`CursedBoltRenderer`): a soft round glow in three layers (halo, core, white centre) that
fades to the rim, and a ribbon trail of recent positions, in the technique's colour. Limitless
bolts (Blue, Red, Hollow Purple) draw their `GlowFx` signature and a comet tail instead.

## 4. Black Flash

Hit-stop and shake for the striker and target, red and black particle lightning at the point of
impact, a toast with the multiplier, and the full-screen frame: diagonal bands of deep red and
black that slide, strongest at the edges and clear around the crosshair, with a jag of black
lightning across the middle, for 0.4 s. Reduced flashing: a steady dark red wash.

## 5. Domains

The sequence: (1) "Domain Expansion" and the domain name written letter by letter over an ink
flash; (2) a short hit-stop for everyone inside; (3) the dome rises from the caster, near-opaque
with a barrier (closing off the sky from inside) or a faint tint barrierless; (4) a turning glyph
ring at the boundary; (5) a fade when it ends. Each domain adds its own ambience inside: blades
or cuts in the air, glyph streams, shadow tendrils, lanterns, orbiting blades, ripples. Clashes
throw shards where two domes meet. Inside someone else's domain the screen edges distort; while
Overloaded, a flood of glyph noise buries the view and HUD.

## 6. Curses

Emergence: they rise out of the ground over 1.4 s with dark particles. Death: they sag and fall
apart, fade and shed dissolve particles in their codex colour. Telegraphs: every special attack
shows its tell for at least 0.8 s (a glowing mouth, a marked circle on the ground, an aim line
that locks before firing, a door rattling, a chest lid clattering, dirt kicking up before a
burrow). Bosses carry vanilla boss bars that name their state (phase, mouths left, the Edict).
Sensing outlines curses through walls with an aura in their grade colour.

## 7. Culling Game

Colony barriers: a dark curtain wall 24 blocks high under a dome, with slow lighter streaks
running down it like wet ink; nearly opaque from outside, thin from inside so a colony is gloomy
rather than blind; drawn only within about 320 blocks. The Kogane is a floating paper-mask
lantern with a turning ring of tally beads.

## 8. Sorcery

Reverse Cursed Technique: green glow particles while healing; a larger burst on the awakening
and the emergency save. Vows: a sound and message when sworn, broken or fulfilled. Promotion: the
full-screen ink ceremony (UI_SPEC). Shikigami arrive and leave in a puff of their colour. The
Drifting Gourd leaves a trail of cloud under its rider.

## 9. The Modulo expansion (1.1.0)

All of it is plain geometry, particles and HUD drawing (no shaders), so it works with Sodium and
under shader packs the way the domes do. Phase 2 (the VFX overhaul) revisits the ship and the
fights; what is listed here is what 1.1.0 draws.

| Effect | Drawn |
|---|---|
| The Naunax over the overworld | during a Descent, a dark flattened hull 140 blocks across, 150 blocks above the ramp's foot, lighter on top, with a pulsing band of Mul light round its waist; drawn with the mod's translucent type, which has no fog, so it reads from far off |
| The ramp | a pale beam with a green glow from the ship's belly to the foot, turned to face the camera, flickering slightly |
| The Naunax inside | a hull-plate ceiling, deck-plate floor and pillars with Mul conduits (light 14) in rows, all blocks (the `naunax` dimension has its own sky and fog colour) |
| Tokyo 2086 | blocks only: streets and Kyoto-style houses with yards west of the river, ruined towers east of it |
| Gates | a thin glowing pad (2 pixels high) in the destination's colour; stepping on it travels |
| Mul | green sparks while pouring; a chime each second; a crack when a piece crumbles |
| Mahoraga's wheel | a gold ring with eight spokes over its head; each time it adapts the wheel eases round by one spoke (0.6 s), a bell rings and light rises from it |
| Dabura | Light: a thin line of light from his eyes to the target for the tell, then sparks and a bright line for the beam; Hard Light pillars and cages as glowing blocks that fade after their time; Darkness: ink gathering between his hands with a heartbeat for 5 s, then an ink burst over the radius |
| Maru (duel) | a mirror sound and rising portal motes under each player for the 2 s tell before gravity flips; sparks at his eyes when his gaze lands |
| Masaki Takeda | crimson spores at the reveal; crit sparks along each needle's path after a crossbow-loading tell; red sparks when he is torn out of the Skin Guardian |
| Toranosuke | dust at its feet during the roar before a pounce |
| Trafficker | enchant glyphs and a teleport sound when he blinks |
| Epilogue | a dark veil over the whole screen and HUD, the title in the Modulo green and six lines of text, five seconds each, fading in and out; green motes rise round the player. Reduced motion: each line whole, no fades |

Reduced flashing: nothing in the expansion flashes; the beam and the darkness burst are particle
lines and clouds, not screen flashes. Low VFX: fewer hull segments and rim segments.

Needs in-game review (it cannot be judged from screenshots): how the hull reads against a sunset
or night sky, the ramp's width from close up, the gate pads' glow at night, and the pace of the
wheel's turn.
