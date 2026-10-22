# Changelog

All notable changes are listed here. Versions follow the milestone plan in `docs/DESIGN.md`:
0.1.0 is M0, and each later milestone raises the minor version by one. From 1.1.0 the plan is
phases: Phases 0 and 1 are 1.1.0, Phases 2 to 6 are 1.2.0 to 1.6.0, and Phase 7 is 2.0.0.
Entries before 1.1.0 use the names of their time; `docs/NAMING.md` maps them to the canon names.

## 1.16.0-preview, 2026-10-22

- Moved fighting hands toward the lower corners; added physical right/left punch keys,
  hand-aware first/third-person poses, faster alternating recovery and server stamina.
  Auto Combat chooses fists; default heavy moves to semicolon, preserving vanilla bindings.
- Added Cursed Vessel as an independent physical attribute and an original Sukuna's
  Finger item, rare special-grade drops, finger progression, false-curse hallucinations
  and bounded server-controlled takeovers with blackout and lifecycle cleanup.
- Added a domain workshop with radius/output/duration/barrier trade-offs, custom
  colours, names and scenery, observer synchronisation and compatible open/closed modes.
  Unlimited Void and rule domains require closed barriers. Repaired vertical containment,
  whole-body boundaries, direct cross-wall damage, barrier punching and release.
- Added dedicated server and native client coverage for the new mechanics and layouts.

## 1.15.0-preview, 2026-10-22

- Separated cast aim from overloaded VFX radius/duration parameters. Cursed Speech
  follows yaw and pitch, remains fixed after casting, and uses traveling 3D pressure fronts.
- Replaced flat primary material envelopes with UV-mapped volumes: energy ellipsoids,
  thick pressure shells, crystalline fragments, shadow pools and reverse-energy helices.
  Held charge cores and actual projectiles share the same depth-tested rendering.
  Spatial cores have wrapped bright currents and preserve distinct authored charge colors.
- Rebuilt all 52 curses with connected block anatomy, purposeful faces and material
  details. New crisp pixel textures use restrained palettes, limited eye glow and subtle
  wound variants. Art is reproducible from source recipes; full rigs and LODs stay unique.
- Added packet roundtrip and full-pitch direction regressions, multi-angle native visual
  checks, and seamless-surface asset assertions. Updated older gameplay fixtures for
  subjugated summons, passive Infinity, punch coatings and traveling spatial releases.

## 1.14.0-preview, 2026-10-22

- Rebuilt charge, spatial-field and projectile effects with sixteen filtered, guttered
  materials. Blue/Red/Purple have distinct cores and flow. Every family receives material
  casting/impact effects, with fewer random particles and crossing Shrine cage incisions.
- Centered local charges on the current camera axis and server projectile/release origins
  on the eye/aim ray. Removed the side/down hand offset and opaque ball/wire-hoop rendering.
- Replaced repeated vanilla combat arm transforms with skin-mapped fists, thumbs, bent
  forearms, sleeves, slim/left-handed handling and continuous attack transitions.
- All techniques gain continuous mastery benefits up to four original milestones:
  1.60×/2.25×/3.00× power at 100/200/400%, up to 35% less CE and cooldown, and supported
  reach/duration growth. New server configs default to 3.0× offensive damage. Existing
  configured values and saved mastery counts are retained. Creative maximum presets use 400%.
- Coatings refresh their output at contact. Tooltips, cooldown overlays, workshop previews,
  Projection reach and passive/channel upkeep account for mastery.
- Infinity slows inward motion, preserving tangent motion and living gravity, and only
  separates overlapping targets. Incoming projectiles fully suspend and resume when released.
  Removed constant outward acceleration, minimum-speed leakage and the first-person floor ring.
- Added native checks for the full registry, all families, ultrawide hands/moving casts,
  real Infinity motion and projectile restoration, plus mastery/Infinity unit regressions.

## 1.13.0-preview, 2026-10-22

- Reworked first-person empty-hand combat rendering: one scoped pose per arm, compact
  stance, independent hand selection and no third-person pose leaking into the hands.
- Domain information appears only on the technique wheel. Repositioned the wheel
  and domain strip, stabilized hover selection around side buttons, and restricted
  click casting to a node or the selected move's center panel.
- Charging VFX use one interpolated effect per caster. Release positions match the
  charge hand; Blue and Red travel outward before their fields form. Projectile
  ribbons sample game ticks for consistent trails at different frame rates.
- Blue/Red Punch toggle independent fist imbuements, applying spatial damage and
  pull/repulsion on a landed unarmed punch. A separate O key and wheel button toggle
  CE fist imbuement. Both coatings work together and clear on death or disconnect.
- Maximum charges take longer than ordinary charges; Purple's overcharge slows
  further. Removed unintended double application of charge damage scaling.
- Maximum Blue/Red have larger real influence radii and visible envelopes. Purple
  has a larger, faster projectile with reduced near-camera glare.
- Offensive techniques gain a configurable 2.5x damage multiplier. Shrine gets
  adaptive, partly armour-piercing Cleave, a stronger long-range Dismantle, a larger
  Furnace blast and stronger sustained cutting fields. Summons gain health and melee output.
- Repainted all 75 creature rigs with denser UV materials, continuous form shading
  and distinct fur, cloth, scale, bone and metal treatments. Rebuilt Mahoraga, Nue,
  Rika, Toad and Well's Unknown Abyss forms; corrected Mahoraga's eight-spoke wheel.
- See `docs/COMBAT_REFINEMENT.md` for controls, balance changes, source recipes and checks.

## 1.12.3-preview, 2026-10-22

- Family ability browsers now support search and All, Learned, Ready and Untamed
  filters, with live cooldown, grade and CE feedback. Opening a specific move selects
  its page automatically; search focus survives refreshes.
- Family wheels remember their selected move and Limitless branch until disconnect.
  Added Abilities and Dismiss summons shortcuts, including owned fusion summons.
- Infinity's HUD and tooltip show its saved toggle, actual field state and server
  upkeep, including waiting for CE or suppression. Learning RCT updates the display
  without toggling Infinity again.
- Ten Shadows rituals have a configurable countdown and distance HUD, with warnings
  near the time or 48-block distance limit. End ritual cancels without granting an
  unlock or refunding the CE spent. Summon management shows the active owned count.
- Instant casts and toggles from a family wheel no longer release an unrelated
  channel. Summon dismissal leaves other players' summons alone.
- Added the repository workflow reminder to push completed updates and launch
  Minecraft afterward. See `docs/QUALITY_OF_LIFE.md` for controls and verification.

## 1.12.2-preview, 2026-10-22

- Reconciled GitHub main `5cb71c5` with the local rework. Retains the 51 families,
  296 moves, variant wheels, Projection planner, RCT drills, combat polish and all
  75 creature rigs from GitHub.
- Ten Shadows subjugation integrates with the family wheel. Untamed survival summons
  open ritual management; direct casts and fusions use the same persistent unlocks.
  Hostile trials cannot act as friendly guards or fusion components.
- Old local `family_*` loadouts migrate to `family/` bindings while preserving direct
  slots, learned moves, mastery and the selected slot.
- Fixed damage immunity from a stale `respawning` soul attachment. Existing affected
  characters recover automatically, including on reopening their world.
- Infinity defaults on with Limitless, preserves its manual toggle across death and
  reconnect, and consumes CE until RCT is learned. Dispelling tools interrupt it.
- Retains the local ritual armour, Black Rope, Vowbound Blade, Soulsteel Katana,
  Warding Talisman and Sealed Reliquary alongside GitHub's equipment. Common weapon
  IDs retain GitHub's behavior. Local painted sprites accent distant VFX impacts.
- Successful Black Flashes now apply weapon wear, on-hit tool effects and the
  combat-hit counter, while retaining the newer assisted-combat contact accounting.

## 1.12.1-preview, 2026-10-21

- Limitless selectors have All, Blue, Red, Infinity and Purple filters on normal-size
  screens. Cooldown, grade, CE and active-state hints appear in the wheel. Rebound mouse
  wheel/cast controls work; middle click pins moves when right click is bound to casting.
- Projection's editor supports selecting, dragging and deleting individual waypoints,
  with cached terrain, reach and swept-path warnings. Server replies explain rejected
  edits. Movement uses exact collision-shape heights for slabs and stairs, checks every
  segment, and interrupts on combat stun. A targetless preset moves without air attacks.
- Fixed projectile-only Infinity settings being discarded, incorrect default resets,
  and unintended default upkeep inflation. Presets retain the draft's filter, vows,
  custom name, color and trail. Spatial punches expose independent push/pull force;
  instant punches/discharge no longer charge for an unused duration setting.
- Pending casts recheck eligibility at the end of windup and cancel on dimension changes,
  death or spectator mode. Sustained casts also stop on dimension/spectator changes.
  Failed replacements preserve the existing channel. Ally mending requires line of sight.
  Melee stagger interrupts charge, windup and channels immediately; short stuns cannot
  let an old windup resume. Editing another slot preserves a family variant's charge.
- New Red variants use repulsion effects and a bright core; Purple has opposed Blue/Red
  filaments. Infinity uses a faint spatial envelope with a clear first-person view.
  Sustained rings and beam particles avoid filling the camera; positive-energy beams
  are narrower. Duplicate channel effects and expired remote channel bookkeeping are cleaned up.
- Compact combat and charge panels keep the center clear. Both have independent HUD
  visibility, anchor, position, scale and opacity controls; charge sits below the selected power.
- Effects OFF hides already-running geometry immediately and restores live effects when
  re-enabled. Camera-shake/reduced-motion changes apply immediately. Spatial fields have
  configurable oldest-first budgets: 16 per caster and 128 per dimension by default.

## 1.12.0-preview, 2026-10-21

- Added twenty original six-move families, raising the roster to 296 moves across 51 families.
  Primers/marks, resonance, limited paid guards, movement, control and finishers interact;
  each family has its own secondary effects. Balanced all nonzero Random family weights.
- Added whole-family bindings and learned-variant wheels alongside exact-move bindings.
  Hold G + scroll to select a slot; R opens its family. Variants can charge with held click
  or be pinned directly. A compact selected-power badge replaces the vertical sidebar.
- Added six Limitless applications: Mini Blue, Blue Punch, Mini Blue Cluster, Mini Red,
  Red Punch and chargeable Maximum Red. Infinity is an independent passive toggle;
  it drains CE and suppresses recovery without RCT, while learned RCT removes upkeep.
- Replaced instant XP-purchased RCT with three interruptible reverse-flow practice drills.
  Added curse-only Positive-Energy Discharge, keeping ally mending and held self-healing.
- Projection Sorcery now executes terrain-checked destinations and actual physical attacks,
  with an editable route and automatic terrain preset. Explosive Flesh spends real health;
  flesh regeneration requires learned RCT.
- All curses now use entirely new silhouettes. All item icons, technique emblems, weapon
  materials and armor atlases were redrawn. First-person fists angle inward during fights;
  idle hands return to vanilla outside combat.

- Rebuilt all 75 creature models from editable source recipes: shaped silhouettes,
  articulated limbs, jaws/eyes/crests, individual face UVs, material textures, emissive
  masks, damage variants, compatible LOD rigs and sixteen animation clips per creature.
- Added layered technique VFX by material, dark Limitless/shadow cores, orbiting rings,
  blood tendrils, ice crystals, fire filaments, healing helices and timed melee trails.
  Bolt cores are smaller, with orbiting energy; domains gain layered seals and motes.
  Shared geometry budgets, reduced VFX and effects-off controls apply to the new layers.
- Added breathing combat stances, hip/leg/head anticipation and recovery, distinct feints,
  improved first-person motion and an empty offhand fist. Heavy impacts pause for one tick;
  light attacks no longer freeze the camera. Item-use animations retain vanilla control.
- Auto Combat commits three of every five accepted attack opportunities with extra recovery
  and feints; manual input remains uncapped. Assisted guard takes chip instead of perfect
  parries. The HUD reports observed contact rate, counting each multi-target swing once.
- Added Playful Cloud, Dragon Bone and three original cursed tools: Stormbell Spear, Ember
  Wakizashi and Sealbreaker Tanto. Tool effects spend CE where appropriate; dispel cancels
  charging as well as active techniques. Granted tool abilities cast with Use.
- Moved block's default to Right Bracket because Minecraft 26.3 uses O for Friends.
  Old O/F block defaults migrate once; default Iris R/K/I conflicts move to available
  F8/F9/F10, preserving customized Iris keys. The binding editor detects vanilla and
  third-party conflicts as well as duplicate mod keys.
- Added twelve wearable pieces across Sorcerer Uniform, Binding Vestments and Heavenly
  Hunter sets: up to 8% technique CE discount, CE-paid 20% damage ward, or 12% movement
  speed respectively. Native armor durability, attributes, textures and recipes are used.

## 1.11.1-preview, 2026-10-20

- Charging is now opt-in per move: eighteen appropriate high-output, compression and
  elemental attacks support it; 151 moves use normal activation. Blue, Red and Purple,
  Piercing Blood, Granite Blast, Furnace and Maximum Uzumaki retain charging.
- Summons, fusions, swaps, movement, ordinary melee, body changes and utility skills
  activate on press at normal output. Infinity toggles on press; guard/healing channels
  start immediately while held and stop on release or menus.
- Added casting-style labels to shared technique tooltips and the loadout. Personalisation
  preserves the authored charge flag; datapack moves default to ordinary activation.

## 1.11.0-preview, 2026-10-20

- Shared server-owned hold/release charging for every player technique: 25% taps, 100%
  normal cap and 200% Hollow Purple. Output, supported area/reach/force/duration and CE
  consumption scale with charge. Six Eyes strongly reduces ongoing charge drain.
- Added a live charge percentage and CE-per-second meter. Cancel safely on menus, stun,
  edits, exhaustion, death, disconnect and dimension changes. Channels start on release
  and stop on a second press; existing windup counts time already held.
- Blue and Maximum Output Blue pull real nearby blocks into visible transient debris.
  Purple drills its flight corridor; destructive projectiles, slashes, shockwaves and
  spatial-field collisions damage terrain. Bounded server budgets, container/unbreakable
  protection, claim vetoes, mobGriefing and a server terrain switch are supported.

## 1.10.0-preview, 2026-10-20

- Fixed the Creative editor's null family rendering crash. Both server replies and client
  rendering validate missing/empty family IDs; native tests select and apply a different family.
- Defensive mass/armor buffs cap at Resistance III. Old overpowered effects are repaired on
  login; character replacement clears stale death-menu protection and previous defensive effects.
  Maximum Creative presets learn RCT with automatic healing off, still configurable in Skills.
- New birth reserves are 350–550 with independent regen levels 4–6. One-time old survival-save
  migration preserves training and luck order. Explicit Creative settings remain unchanged.
- Added all remaining Ten Shadows summons, Divine Dogs: Totality, Well’s Unknown Abyss,
  Agito and configurable two-to-four-creature fusion recipes. Fusions consume actual owned
  component summons and inherit their mechanics; invalid casts leave components intact.
- Added 18 owned shikigami types with original geometry, UV art and animation, 22 powers and
  two families (169 powers, 31 families total). The additional roster includes Moon Dregs,
  Garuda, Rika and four original familiars. Summon ownership has a twelve-body cap.
- Added the Divine Chalice, extremely rare player-kill boss loot and an expensive endgame
  recipe. Deliberate sneak-use consumes one item for a different family and clears technique
  progress while preserving the body, grade, growth, birth potential and universal skills.
- Bounded support healing, non-stacking guardian interception and Mahoraga adaptation capped
  at 60% prevent new summon combinations from granting immunity.

## 1.9.0-preview, 2026-10-20

- 52 additional moves; all 29 innate families now contain at least four individual abilities.
- Idle Transfiguration has nine moves, including additive owned soul servants, a six-soul cap,
  recall, tuned vitality/mobility/count/lifetime, body forms, soul contact and detonation.
- Attack input preserves clicks that begin and end between ticks, including inverted mouse
  controls and default-on Auto Combat. Vanilla Attack/Use mappings remain authoritative.
- Larger shared screens, clearer cards and controls, a family/move browser, inline loadout
  descriptions and a direct personalization section chooser.
- Creative character drafts offer exact grade, XP, four growth paths, birth/trained/stored CE,
  regeneration, attributes, body form, individual learned moves/mastery, domain and universal skills.
  Optional Auto Calamity defaults off; the maximum preset is explicit. Server ACKs confirm saves;
  validation rejects invalid/spoofed edits before mutation. Custom choices survive login unchanged.
- Furnace projectiles carry actual fire, retained by personal tuning. New movement/healing
  abilities respect personal vows; soul summoning and detonation exclude the owner and their allies.

## 1.8.0-preview, 2026-10-20

- Twelve slots at every grade, three four-power banks, unique bindings and old-save expansion.
- Ten optional per-expression vows, real restrictions, x5 output for forsaking ordinary combat,
  combined output capped at x12, and a persistent 60-second seal after casting.
- Manual light/heavy/block keys with double-tap specials, default-on Auto Combat assistance,
  preserved vanilla Q/E/F/hotbar behavior, and reliable quick taps between render ticks.
- Hostile mobs and Culling entrants use framed contact; curse attacks retain species mechanics,
  CE drain and reactive guard behavior. Added five combat clips to the existing rigs.
- Twelve new low-grade species, reduced natural encounter rate, weak mixed packs and isolated
  Grade 2+ encounters. New entities have dedicated models, palettes, spawn eggs, loot and codex entries.
- Twenty-four powers in eight families: Electrification, Granite Blast, Love Rendezvous,
  Explosive Flesh, Tool Manipulation, and original Echo Ledger, Mercury Mirror and Mourning Thread.
  Added craftable scrolls and private, expiring charge/echo/star marks and stored-force counters.

## 1.7.0-preview, 2026-10-20

- Fixed native mouse handling in the loadout and wheel: all four cleared slots can be selected
  and refilled. Removing the last equipped copy of an active channel stops it.
- Converted Modulo to sandbox content: no missions, Descent, boarding, story tracker or expedition
  pact. Legacy story saves return to normal play. Added Harmony / Chaos and Light / Darkness
  families with eight grade-scaled abilities and eight ordinary scroll recipes.
- Expanded personalization to eight pages and 32 controls: aim offsets, gravity, turn rate,
  projectile size and patterns, pulses, summon count/health/movement, impact lift, edge falloff,
  health sacrifice, recoil, dash directions, RGB colors and trail selection. Applicable mechanical
  changes have CE trade-offs. Other players see the caster's chosen trail.
- Old tuning saves retain defaults; custom expressions persist independently for each player.

## 1.6.0-preview, 2026-10-19

- Four-hit combat chain, buffered follow-ups, airborne/crouched finishers, knockdowns,
  a paid evasive escape with a ten-second cooldown, contact bursts, parry flashes and impact audio.
- Direct skill keys 1–4 in combat, F block, and a dedicated binding editor. Shared defaults
  preserve inventory behavior outside combat. Releasing another slot no longer cancels a channel.
- Searchable Moves catalogue; slot selection uses immediate local state and dragging requires
  actual movement, preventing accidental equips. Each move occupies one of four slots.
- Workshop sliders and exact-value fields, normalized previews, server save acknowledgements,
  timeout/rejection feedback, and synchronized numeric/slider values.
- Rebuilt all 40 curse material atlases with deliberate planes, sockets, folds, bone and cleaner
  emissive masks; wounded/critical variants retain the same UVs. Material-specific technique accents.
- Domain interiors, visible barrier damage and breach, hand-sign interruption, and collapse under
  sustained caster damage. Ordinary guard/dodge no longer cancels sure-hits; Simple Domain still does.

## 1.5.0-preview, 2026-10-19

- Creative-only, server-validated family picker: Calamity grade, all family moves mastered,
  maximum Growth, matching domain and learned reinforcement/RCT; optional Mechamaru restriction.
- Four workshop pages with 1/5/25-point precision, projectile count/speed/spread/homing,
  armor penetration, force, preparation time, field placement, target classes, names,
  eight color choices and editable presets. Advanced delivery has actual CE trade-offs.
- Independent, persistent regeneration levels (1–10, 0.6–2.4x), visible in Practice.
  Smaller current capacities purchase larger reserve gains at lower XP prices.
- Mechamaru's restriction trades health/mobility for an uncapped CE bank. It starts empty,
  regenerates at a fixed working-output rate and does not receive free energy from Fill.

## 1.4.0-preview, 2026-10-19

- Persistent per-character technique tuning: output, reach, area, duration and cooldown,
  with server-enforced CE trade-offs. Infinity offers projectile/contact/combined filters.
- Independent 100–3000 birth CE reserves; XP-funded training and existing grade/Output growth.
- Learnable universal reinforcement and RCT, with output/healing controls and no slots used.
- Body/armor combat poses, first-person transforms, observer synchronization and casting gestures.
- Technique-colored UI, workshop previews, a Practice screen, safe teleport destinations,
  and duplicate-windup prevention.

## 1.3.0-preview, 2026-10-19

- Fixed family hotbars filled with unrelated punch/teleport powers; existing family
  characters repair themselves on login. Blood Manipulation has a full blood-themed path.
- Four equipped moves maximum, including the quick-cast wheel; old learned libraries stay.
- Normal vanilla respawn points, pact-gated colony reincarnation, cursed-spirit lives in
  both modes, and a different innate family after exorcism.
- Fifteen independent physical/clan attributes with persistent rolls and a Traits screen.
- Brain Transplant's consumed fallen hosts, rendered bodies, host health and borrowed moves.
- Cursed Spirit Manipulation absorption/storage/ownership and stock-consuming Uzumaki.
- Expanded to 63 techniques, 19 families and nine domains; NPCs use coherent families.
- Explicit Culling colony entry, personal Kogane negotiation, summon kill credit, optional
  last-survivor ending, and normal-respawn release from colony confinement.
- Modulo enabled by default, Grade 2 access and colony Vessel expedition agreements.

## 1.2.0-preview, 2026-10-19

A combined playable checkpoint of the continuation work; it does not certify completion
of every Phase 2-7 acceptance item. See docs/IMPLEMENTATION.md for remaining work.

- Explicit, persistent Inherit / Random awakening across nine families, server-side roll
  validation, grade-based family unlocks, a rare Six Eyes trait, and legacy character migration.
- Named, paginated technique loadout with costs, cooldowns, requirements and six visible slots.
- Twenty added curse variants (forty total), original procedural assets, glow/damage/LOD
  resources and windup/stagger animation states. Four spawn attempts, a sixteen-curse local
  cap, weak ambient encounters and Creative encounters by default.
- Six server-framed melee moves, height-aware guard/parry, guard breaks, dash, stun,
  combos, Ratio openings, Black Flash integration and rebindable combat controls.
- Survival death choices, identity-tagged cursed objects and reliquaries, colony Vessels,
  revival, Awakening Rites, Vessel traits and initial Kenjaku/Sukuna pact paths.
- Budgeted world effects, aura shells, pose echoes, summoning tendrils, Projection pose
  sampling, native noise dissolve and accessibility controls.
- Eleven added server integration tests and seven added unit tests. Hardened the client
  persistence test by pausing regeneration so load time cannot change its expected value.

## 1.1.0 (Phases 0 and 1: canon naming and the Modulo expansion), 2026-10-19

### Phase 1: the Modulo expansion

Optional and off by default (`modulo.enabled`). Spoilers for Jujutsu Kaisen Modulo in
`docs/MODULO.md`, which also lists where the brief and the wiki disagree (the wiki was followed).

- **Two dimensions**: `modulo:naunax`, the Simurian ship (hull decks, Mul conduits, Mul ore in the
  hull), and `modulo:tokyo_2086`, Tokyo and the Kyoto region in 2086 (Kyoto-style streets west of
  the river, the ruins of the Inhuman Makyo east of it). Flat generators with two new features.
- **The Naunax Descent**: every three game days, if a Grade 1 or higher player is in the
  overworld, the ship hangs over the nearest badlands for ten minutes with a ramp of light down
  to the ground; stepping onto the foot boards it. Announced with its coordinates; drawn on the
  client (no blocks are placed in the overworld). Gate pads on the ship and in Tokyo travel
  between the two and back to where you boarded.
- **Mul energy**: a second pool beside cursed energy (at most 200), made by pouring cursed energy
  into Mul (0.5 per CE). Kept through death. Heavenly Restriction users cannot make it.
- **Items and blocks**: Mul ore, Mul, High-Purity Mul, Naunax hull, deck plate, Mul conduit, Hard
  Light (Dabura's solid light, temporary), gate pads; the Rumelian Blade (lends Mul Lance, paid in
  Mul energy), Honoyagi (Tsurugi's katana; lends Batto Sword Drawing), the Gravity Harness, the
  Mul Ward, the Naunax Beacon, takoyaki, the wancha octopus, the tiger cub, Yuji Itadori's Cursed
  Object. Their own creative tab.
- **Cursed tools as mediums**: a tool marked `medium` pays for its lent technique with durability
  instead of CE, so Heavenly Restriction users can cast through it. Honoyagi and the Split Soul
  Katana are mediums. A medium's durability comes from the wielder's grade.
- **Eleven missions** in story order with the expansion's characters (Maru, Cross, Yuka and Tsurugi
  Okkotsu, Kou Usami, Mino, Kyoko Tomoe, Osada, Masayoshi, Jabaloma, Dabura Karaba), stage by
  stage with checkpoints that survive death and relogs. Maru becomes a companion after the first
  mission. Chaos and Harmony has a choice that picks the branch (duel Maru, or stand aside).
- **Bosses**: the trafficker, Toranosuke, Masaki Takeda (three phases with his Skin Guardian),
  Maru (gravity inverts in the arena), Dabura (Light beams, Hard Light pillars and cages, his
  Domain Expansion, and darkness; health grows with each extra fighter), and Mahoraga, which
  adapts to each kind of damage that hurts it while its wheel turns.
- **Epilogue**: "A Bright Future", once per player, with its advancement and Yuji Itadori's
  Cursed Object (kept for the Phase 6 cursed object system).
- **HUD**: a Mul meter and a mission tracker in the top-left column (they step below the grade
  badge and the Culling Game panel when they would overlap), a dialogue box, a choice screen and
  the epilogue overlay. New client options `hud.mulMeter`, `hud.missionTracker`, `hud.dialogue`.
- **Data**: three new server registries, `modulo_mission`, `modulo_site` and `modulo_boss`
  (`docs/SCHEMAS.md`), and new fields: a technique's `mul_cost`, a grade's `tool_durability`, a
  cursed tool's `medium` and `durability_per_ce`. Two damage types, `jjkcraft:light` and
  `jjkcraft:darkness` (darkness ignores armor). The mod's Modulo content lives in the `modulo`
  namespace. `tools/gen_modulo_data.js` writes it; `tools/gen_modulo.py` draws its art.
- **Commands**: `/jjk modulo` (descent, board, ship, tokyo, leave, mission, mul, companion,
  site build).
- **Config**: the `modulo` section replaces the M0 placeholder `systems.moduloExpansion`.

Fixes found while building it:

- Since the port to 26.3, the mod's "let the next hit through" resets did nothing: 26.3 moved the
  hit cooldown from `Entity.invulnerableTime` (now a separate full invulnerability) to
  `LivingEntity.damageCooldownTime`. A Black Flash right after another hit lost that hit's damage,
  a domain's sure-hit could be cut short by a recent hit, and the armor-piercing half of a split
  hit was mostly swallowed. All three now reset the right field; a new GameTest checks the Black
  Flash case.
- A boss's health above vanilla's ceiling of 1024 (Dabura with four fighters) was silently capped.
  It now becomes damage scaling, so the fight lasts as long as the formula says.

Tests: 95 unit tests (adds `ModuloMathTest` and `ModuloDataTest`, and a Mul budget in the balance
test), 120 server GameTests (adds 13 `ModuloGameTests` and the Black Flash check), the client
GameTest with 56 screenshots (8 new for Modulo). A test-only mixin lets the GameTest server load
the datapack dimensions (`docs/MIXINS.md`); the mod itself has no new mixin.

### Phase 0: audit and canon naming

Names only; behavior is unchanged.

- Powers that exist in Jujutsu Kaisen now use their canon names, checked on the Jujutsu Kaisen
  Wiki (`docs/NAMING.md` has the table and sources). 19 techniques, among them Cursed Technique
  Lapse: Blue, Reversal: Red, Hollow Purple, Infinity, Cleave, Dismantle, Piercing Blood, Straw
  Doll Technique: Resonance, Ten Shadows: Divine Dogs, Nue and Toad, Cursed Speech: Don't Move,
  Crumble Away and Blast Away, Idle Transfiguration, Projection Sorcery, Reverse Cursed
  Technique, Positive Energy Output and Maximum Output: Blue. Five domains: Malevolent Shrine,
  Unlimited Void, Chimera Shadow Garden, Idle Death Gamble, Authentic Mutual Love. The shikigami
  (Divine Dog, Nue, Toad), the Kogane (was the Tallykeeper), the Straw Doll, the Inverted Spear
  of Heaven, the Split Soul Katana, the Chain of a Thousand Miles, the Frozen Frame effect and the
  Game Master rule. The Withered Digit is now called a cursed object, not a vessel.
- Ids, lang keys, classes, textures and models follow the new names. Old worlds and data packs
  still load: built-in registries get Fabric aliases, and saves and data packs map old ids to
  new ones as they are read (`LegacyIds`), with one log warning per old id.
- Data format: a cursed tool's `kind` `vessel` is now `cursed_object` (the old value is still
  read). Technique behavior types `effigy_link`, `time_skip`, `reshape`, `projectile_veil` and
  `command` are now `straw_doll`, `projection_sorcery`, `idle_transfiguration`, `infinity` and
  `cursed_speech` (the old ones still decode).
- New docs: `docs/AUDIT.md`, `docs/NAMING.md`, `docs/MIXINS.md`. The README states the whitelist
  default of 26.3 dedicated servers.
- Tests: 82 unit tests (adds `LegacyIdsTest`), 106 server GameTests (adds `LegacyIdGameTests`),
  the client GameTest with 48 screenshots.

### Compatibility run

The first run with Sodium, Iris, Lithium and Jade (`docs/TEST_REPORT.md`, "Compatibility runs")
found three problems, all fixed in the mod:

- With an Iris shader pack on, domain domes and Culling Game barriers were not drawn at all: Iris
  has no shader-pack program for vanilla's `debug_quads` pipeline. They now use the mod's own copy
  of that pipeline (`JjkRenderTypes.dome()`, identical without shaders), which `IrisCompat` hands
  the pack's basic program through Iris's API when Iris is installed.
- Jade's look-at tooltip covered the domain meter (both sit at the top centre). A Jade plugin
  (`JjkJadePlugin`, loaded only with Jade) moves the tooltip clear of the mod's HUD elements.
- Jade gave disguises away: the disguised Redacted Saint showed its own health and "Jujutsu Craft"
  under the player's name it wears, and the Gilded Mimic showed as itself. Jade now sees the
  player whose face the Saint wears and a chest; a sensed curse still shows as itself.
- Build: `-PwithCompat` (and `-PcompatShaders=<pack>`) add hand-downloaded jars from
  `compat/mods/` to development runs; normal builds and runs are unchanged. Jade and Iris are
  compile-only dependencies from Modrinth's maven.
- Tests: 78 unit tests (adds `HudClearanceTest`); the client GameTest checks what an overlay is
  told about the disguised Saint (with and without sensing) and the Mimic, and with Jade present
  takes two extra close-ups.

## 1.0.0 (M9: balance, hardening, release)

- Balance pass, kept honest by a new test (`BalanceSimulationTest`) that reads the shipped data:
  no ordinary technique above 0.9 damage per CE; no ultimate one-shots a curse of its tier even
  fully grown; a sorcerer can finish any curse of their grade, Grade 4 to Grade 1, on one pool; no
  ordinary technique one-shots a curse of its own grade. It found two problems, now fixed: Null
  Convergence (45 to 40) and Thousand Lances. Also tuned: Crimson Meridian (34 damage, 100 CE),
  Hollow Star (48), Effigy Nail (5 a nail), Thousand Lances (every other lance seeks a target in
  the rain, 2.5 each).
- Multiplayer hardening: rate limits on vows, Culling Game requests, the Reverse toggle, loadout
  edits and slot selection (casts, growth and domain actions were already limited); limiter state
  is forgotten when a player leaves; Culling state for a player is cleaned up on disconnect.
- Performance: a crowded-arena GameTest records the average server tick (40 curses, 12 entrants,
  4 summons: about 2 to 4 ms); the client test logs frame rates in the heaviest scenes (294 fps
  in a colony, 508 during Thousand Lances, on the test machine with frame caps lifted).
- Release: the sample content packs (`samples/`): two techniques, a domain, a vow, a Culling Game
  rule, a mob CE profile, a cursed tool retune and a tag addition, with a resource pack for their
  names; zipped by the build; decoded by a GameTest. New docs: `docs/UI_SPEC.md`,
  `docs/VFX_SPEC.md`; README rewritten for the release.
- Removed the "slow motion" client option, which never did anything.
- Seeking Grudge Bolt now seeks: it steers from its second tick at strength 0.25 (was from the
  fourth tick at 0.18, which missed anything a block or two off the line within eight blocks).
- Fixed: a curse's mechanic reacted to blows the curse was immune to (a hidden Doorframe Hunter
  could be revealed by one). The vanilla invulnerability check now comes first.
- Tests: 73 unit tests, 103 server GameTests, the client GameTest with 48 screenshots.

## 0.9.0 (M8: cursed tools, 30 techniques, the full curse roster)

- Fifteen new techniques, for 30 in all and at least one in every category: three shikigami
  (Kennel Call's Ink Hounds flank and bite, Crane Fold's Paper Crane marks enemies and dives,
  Tortoise Ward's Stone Tortoise draws aggression and swats down projectiles), Effigy Nail (the
  straw-doll link: nail a Bound Effigy to hurt its target from afar), Flesh Sculpt (shape-alter:
  shrink a target or bulk yourself up, trading one stat for another), Stolen Frame (snap-speed:
  after a clear windup, enemies freeze while you step forward), Oathbound Edge (scales with your
  vow), Ink Step, Splinter Volley, Ground Toll, Ember Skin, Kindred Mend, and three ultimates, one
  per top grade: Crimson Meridian (Semi-Grade 1), Thousand Lances (Grade 1), Hollow Star (Special).
- Eleven new behavior types (summon, effigy_link, reshape, time_skip, barrage, singularity,
  vow_strike, dash, shockwave, thorns_aura, mend_other); projectiles can fire fanned volleys
  (`count`, `spread`). New effects: Bulked, Shrunk, Lost Moment, Unsettled.
- Cursed tools and objects, with data in `jjkcraft/cursed_tool`: five grade-tagged weapons
  (Split-Soul Nodachi, Thousand-Link Chainwhip, Inverted Tine, Cleaver of Bad Debts, Bonewood
  Staff) that lend techniques while held or act on hit; the Withered Digit (a cursed vessel that
  grants a technique and unsettles you); the Drifting Gourd (cloud flight paid in CE); the Tally
  Ledger (holds Culling Game points under the rules). Technique scrolls for every technique
  (creative tab, sorcerer entrant drops). Tooltips show grade, the lent technique's cost and
  whether your grade can use it.
- The last three curses of the roster, for 20: Doorframe Hunter (hides in door frames, ambushes,
  steps between doors), Gilded Mimic (a chest that grabs and drags), Tithe Burrower (steals from
  your inventory; drop the item it wants and it gives everything back). Curses can now hold a
  stash of items that is saved and dropped on death.
- Visual polish: bolts are soft round glows instead of squares; a beam trail for heavy lines
  (ultimates, falling lances, mending); fields, bursts and rings sized to the technique; the HUD
  shows UNSETTLED.
- Fixes: a Black Flash could make the palm's cost test fail by refunding CE (the test now allows for
  it); continuous drags no longer launch their victim.
- Tests: 97 server GameTests (24 new: every technique and tool loading, each new technique, the
  summons, the tools, the three curses); 7 client screenshots.

## 0.8.0 (M7: the Culling Game)

- The Culling Game: colonies are barrier-enclosed regions (a dark curtain wall under a dome).
  Whoever is inside when a barrier rises may come and go; anyone who enters later becomes a
  participant and is held inside. Awakened sorcerers have 19 days to enter a colony. The game
  starts by itself around world spawn when the first awakened player joins (config), or with
  `/jjk culling start`.
- A rule engine: rules are data (`jjkcraft/culling_rule`) made of clauses, each a trigger (kill,
  enter, leave, deadline, idle, timer, points reached, action, added) and validated effects (award
  or deduct points, join, confine, remove technique, transfer, reveal, substitute, withdraw, cap
  energy, reveal the leader, summon a Warden, message, negotiate). Requests a participant makes are
  refused unless a rule in force allows them.
- Nine original base rules: the entry window, one way in, sorcerer and ordinary bounties (5 and 1
  points, configurable), the Tallykeeper (100 points adds a rule), a shared ledger (transfers),
  an open tally (reveal another participant), substitution (change colonies) and stagnation (CE
  capped after 3 idle days). Ten more can be negotiated: withdrawal, curse tithe, mark of the
  leader, night hunt and night truce (which refuse each other), the Warden's mandate, the laws of
  the blade, the bow and sorcery (each fixes a Warden's weak point), and rising star.
- The Tallykeeper: the game master, a paper-mask lantern at each colony's centre. Speak to it to
  negotiate.
- Entrants: NPC participants, sorcerers (with techniques and CE) or not, brought into each colony
  and reinforced daily, so the game is playable alone. Original skins.
- The Culling Warden is bound by the rules: the Warden's Mandate keeps it alive, and the laws name
  its weak point.
- UI: the Culling Game screen (M, or the Culling tab) with a colony map, the scoreboard and the
  actions the rules allow, the rule list, and the negotiation page; a HUD panel with the day, your
  colony and points or the days left to enter, and the rule count; toasts for joining, points,
  new rules, deadlines and the end.
- Points count toward the Grade 1 trial. The end (by days or by who is left) keeps final
  standings and pays grade experience by place.
- Commands: `/jjk culling start|stop|reset|status|colony add|join|points|rule add|remove|day|entrant`.
- Hooks: `WardenMechanic.addEdictSource`. Fixes: a full tab bar hides the screen title instead of
  drawing over it.
- Tests: `CullingMathTest` (8), the full game loop as one GameTest, every rule loading, and 7
  client screenshots with a rule proposed by clicking.

## 0.7.0 (M6: Black Flash, Reverse Cursed Technique, vows, Special Grade curses)

- Black Flash: Black Flash-eligible strikes (the Ink-Iron Palm) can land with a chance from the
  config base, the grade, Control growth, "the zone" and how charged the swing was (capped at 35%).
  It hits for 1.9x to 3.8x by the striker's CE, refunds 15% CE, knocks back hard and puts the
  striker in the zone for 30 s (two stacks, +8 points each). Red-and-black frame flash, black
  lightning, hit-stop, shake and a toast; reduced flashing turns it into a steady wash.
- Reverse Cursed Technique: awakens on a lethal blow (Semi-Grade 1 and up, 35% CE or more, 35%
  chance; both configurable): survive at 1 health. Then it heals below 60% health (1 health per
  half second for 3 CE; H toggles it), saves you from a lethal blow once every 5 minutes for 40% of
  max CE, makes your techniques hit curses 1.25x and halves domain burnout.
- Binding Vows: 8 data vows (`jjkcraft/vow`) with restrictions; swear up to three with a focus
  (Power, Efficiency, Reserve) and a term (Skirmish, Day, Binding) in the new Vow Builder (Vows tab
  of the profile). Breakable restrictions (no jumping, sprinting, armor, shield) end the vow with a
  CE drain, stun and regen debt; enforced ones (innate technique only, no domain, night only,
  halved reserve) refuse the action. Techniques' `vow_modifiers` now work. Renouncing costs half.
- The Unbound Ritual, the Special Grade trial: as a Grade 1 who has mastered their innate technique
  and holds a vow, kill a special-grade curse inside your own active domain.
- New curses: Vow Kin (Grade 1, hunts vow holders), Paper Tide Gravemind (adapts to repeated damage
  types), Ferryman of Unfinished Roads (timed crossing that drags everyone to its shore), Culling
  Warden (Rule Seals keep it alive; Edicts pick its weak point), Sovereign of Ten Mouths (three
  acts by mouth count, ending in its own domain, Throat of Ten Thousand Words). Boss bars for all bosses.
- Reworked to match the brief: Pale Stilt (strong only against airborne prey), Echo Mimic (replays
  your last actions), Salt Lantern (charged beam), Tidewash (floods, grabs and drowns), Thorn
  Matriarch (follows the sun, hedges off retreat), Redacted Saint (wears a real player's skin and
  name), Ash Oni (charges and staggers), Gravebound Gate (the arena changes by phase).
- New: Rule Seal block, five trophies and spawn eggs, the vow refusal reason, HUD tags ZONE, RCT,
  VOW and DEBT (they move under the badge when they don't fit), the Vows and Reverse sections in
  the profile.
- Commands: `/jjk blackflash force`, `/jjk reverse grant|revoke|force`, `/jjk vow
  swear|break|clear|list`.
- Hooks: `DomainService.addExpandBlocker`, `Vows.addEndListener`, `PlayerActions.addListener`,
  `WardenMechanic.addRule`.
- Fixes: top-centre HUD elements step below boss bars; profile columns cut long text with an
  ellipsis; ink buttons are vanilla buttons underneath (narration and test clicks work).
- Tests: `BlackFlashMathTest` (4), `VowMathTest` (5), 14 sorcery GameTests and the reworked curse
  GameTests (71 server GameTests in all), 7 client screenshots.

## 0.6.0 (M5: Domain Expansion)

- Domain framework. Domains are synced datapack entries (`jjkcraft/domain`): grade requirement,
  cast cost, CE per second, radius, max duration, burnout, sure-hit flag, clash priority, a list
  of behaviors, visuals and sounds. Behaviors are a registry (`jjkcraft:domain_behavior`) that
  datapacks combine: sure-hit damage, apply effect, suppress regen, caster attribute, overload,
  summon hunters, gamble, arsenal, repetition.
- Server-authoritative regions: a fixed sphere at the caster, phases (opening 1.5 s, active,
  clashing, closing), a per-tick list of who is inside, upkeep every second, and a close when CE,
  time or the caster runs out (or the caster walks too far). A barrier holds every target caught
  inside when the domain takes hold. Burnout afterwards blocks the caster's techniques.
- Variants: with a barrier (K), or barrierless (sneak + K): 1.3x power, 1.4x radius, 1.6x cost, no
  one is trapped.
- Simple Domain (N, Grade 3 and up): a small shield around its holder that cancels sure-hit effects
  (each cancel costs CE) and blocks half of the damage from outside; slows its holder.
- Clashes: overlapping domains push against each other for 2 s, then the larger CE pool wins (Domain
  growth, clash priority and the barrierless bonus count). The loser's domain collapses with stun and
  CE loss; the winner earns a clash win toward trials.
- Six domains with original names: Shrine of Hanging Edges (cutting field, caster attacks faster),
  Boundless Index (information flood, Special Grade), Garden of Lightless Kennels (shadow hounds
  hunt inside), Lantern-Row Wager (a gamble rolled twice a second, jackpots), Armory of Borrowed
  Oaths (blades forged from copied techniques, usable only inside, Special Grade), Sevenfold
  Recitation (one strike repeated exactly seven times).
- Players reaching Grade 1 are given a domain (weighted, within their grade); `innate_weight` in data.
- The domain sequence: the name written letter by letter over a brief ink flash, a short hit-stop
  for everything inside, the sky closed off by a dome (near-opaque with a barrier, a tint without),
  a turning glyph ring at the boundary, and a fade when it ends. Each domain has its own ambience
  (cuts in the air, glyph streams, shadow tendrils, lanterns, orbiting blades, ripples), clash
  shards where two domains meet, an edge distortion inside someone else's domain, and the flood
  overlay that buries view and HUD while overloaded. All geometry and overlays, no post shaders;
  Low VFX, reduced motion, reduced flashing and "custom shaders off" tone them down.
- Domain meter HUD: your domain (ready, cost, how long your CE holds it, clashing, burnout), the
  domain you stand in (sure-hit, shielded), gamble reels and the recitation count. Toasts for
  Domain Expansion, clashes and jackpots. The profile lists your domain. Ambient loop inside.
- New: Overloaded effect, shadow hound entity (GeckoLib), Arsenal Blade item, two item data
  components, refusal reasons (burnout, overloaded, no domain), Simple Domain server settings.
- Commands: `/jjk domain grant|revoke|expand|close|simple|burnout clear|list`.
- Hooks: grade-change listeners in progression; domain close listeners.
- Tests: `DomainMathTest` (7: costs, pool against cost, clash resolution and ties), 14 domain
  GameTests, 7 client screenshots.

## 0.5.0 (M4: curses)

- Curse framework. Curses are synced datapack entries (`jjkcraft/curse`). Entity types are code
  (one shared `CurseEntity` base, a flying variant); data supplies grade, stats, CE, techniques,
  natural spawning, loot table, damage resistances, GeckoLib model keys (with an optional
  disguise), the mechanic id and its numbers, and codex settings.
- Shared curse AI (`CurseBrain`): idle, hunt, cast, recover and flee states; remembers where it
  last saw its target; alerts its own kind when hit; casts data techniques within its CE; casts
  more often where fear is high. Curses emerge over 1.4 s and dissolve on death.
- 12 curses, each with one telegraphed mechanic: Hollow Hymnal (stunning song, silenced by a hit
  to the mouth), Ledger Crawler (stronger with every nearby death), Wick Moth (eats light,
  stronger in the dark), Pale Stilt (moves only unwatched; its stare punishes long eye contact),
  Echo Mimic (copies a nearby cast and throws it back), Salt Lantern (heals curses, throws salt,
  snuffed by a heavy blow), Tidewash (empowered when wet, undertow pull), Thorn Matriarch
  (marked root eruption that binds, thorny bark), Cracked Bell (ground shockwave to jump over,
  vulnerable while ringing), Redacted Saint (disguised until close or struck, double first
  strike, blinding hits), Ash Oni (ember-ring slam, leap, rage below 40%), Gravebound Gate
  (boss: arena wall, boss bar, guarded by its summons, pulling maw at half health).
- Fear: a per-world fear field. Deaths add fear (player 15, villager 10, others 2.5); it fades 3
  points a minute. Surroundings add darkness, night, depth and haunted structures (a datagen
  structure tag). Fear sets which curses can spawn, raises curse CE regen and boldness, and at 75+
  can grow a curse nest (spawns up to 3 curses; breaking it lowers fear and drops shards).
- Natural curse spawning around survival players, 16 to 40 blocks away, within the config rate
  and per-player cap; respects Peaceful and the `spawn_mobs` and `spawn_monsters` gamerules.
- Cursed-energy sensing (key B): range from the grade's `sense_range` perk, 0.5 CE per second.
  Sensed presences glow through walls in their grade color (vanilla mobs and GeckoLib curses
  alike) and show on a radar HUD. Heavenly Restriction users can't sense or be sensed. Disguised
  curses still show, which is how sorcerers spot the Redacted Saint.
- Codex tab: a kill counter per curse. The first kill reveals the name and description; the
  curse's `unlock_kills` reveals a 3D preview, lore, weaknesses, resistances, immunities,
  techniques and trophy.
- Items and blocks: Cursed Energy Shard (restores 25 CE), 12 curse trophies, 12 spawn eggs, Curse
  Nest; temporary Cursed Root and Gate Barrier blocks that remove themselves (saved with the
  chunk). A creative tab.
- Data generation: entity and block loot tables, the haunted structure tag, item and block
  models, all new English strings.
- Original art: GeckoLib models, animations and textures for the 12 curses and the Saint's
  disguise; 16x16 pixel art for 25 items and 3 blocks (`tools/gen_items.py`); two new sounds
  (cracked bell toll, roar).
- Commands: `/jjk curse spawn|list|clear`, `/jjk fear get|add`, `/jjk sense <players> on|off`.
- Hooks for later systems: technique cast listeners; curse mechanics get a removal hook.
- docs/SCHEMAS.md curse schema and every mechanic's parameters; docs/BALANCE.md curse tables.
- Tests: `FearMathTest` (4), 18 curse server GameTests, 5 client screenshots (both lineups, the
  disguise, sensing through a wall, the codex).

## 0.4.0 (M3: techniques)

- Technique framework. Techniques are synced datapack entries (`jjkcraft/technique`) that pick
  a code behavior and set its parameters; ten behaviors: strike, projectile, arc slash, line cut,
  attract/repel field, cursed command, channelled heal, bind, held barrier, projectile veil.
- Server cast pipeline: the client sends only "slot pressed/released". The server checks the
  rate limit, slot, knowledge (or a tool granting it), grade, Heavenly Restriction, stun, extra
  blockers, cooldown and CE, then runs the windup, the cast, channels (upkeep, max duration,
  release) and toggles. Cooldowns, Control accuracy, grade experience and mastery update after.
- 15 techniques across eight categories: Ink-Iron Palm (Black Flash capable strike), Grudge
  Bolt, Seeking Grudge Bolt (homing), Crescent Rend (CE-scaled arc), Hairline Sever
  (armor-piercing line), Inward Hollow and Outward Bloom (attract and repel that collide into a
  burst), Null Convergence (Grade 1 merge), Command: Halt / Kneel / Rise (cursed speech with
  throat strain), Reverse Flow (healing), Grave Chains (bind), Lacquer Ward (held barrier),
  Still Air Veil (passive projectile slow).
- Innate technique rolled at awakening, weighted by grade; backfilled on join for players
  awakened before techniques existed. Mastery (-10% cost, +10% power) counts toward trials.
- Death policy covers techniques: cooldowns reset; PARTIAL loses mastery; RESET_TECHNIQUES keeps
  only the innate technique.
- Damage types (datagen) with vanilla tags: technique, armor-piercing technique, Black Flash,
  sure-hit, backlash. One damage hook (`DamageModifiers`, via a `LivingEntity` mixin) used by the
  barrier now and by later systems.
- VFX: six original tinted particle types; additive vertex-geometry effects (diagonal slash
  ribbons, cut lines, glyph rings, chains that follow their targets, burst shells, shout waves,
  auras); a bolt renderer with a ribbon trail; camera shake (Camera mixin) and hit-stop
  (paused client ticking of hit entities); a prioritized sound mixer with pitch variance; a
  particle budget with distance LOD and no particles at the camera.
- HUD technique hotbar (cooldown sweep and ring, low-CE warning, active glow, refusal reason).
  Technique wheel (hold G): release to cast, click to equip, right-click to cast. Techniques tab
  with drag and drop. Shared technique tooltip colored by eligibility.
- Commands: `/jjk technique grant|revoke|innate|equip|list|cast|cooldowns`.
- docs/SCHEMAS.md with the grade, energy profile and technique schemas and every behavior.
- Tests: 13 new unit tests (technique math including a damage budget, technique state and
  death policies), 11 new server GameTests, client screenshots of the hotbar, slash, bolt,
  chains, field, burst, wheel and loadout.

## 0.3.0 (M2: grades and progression)

- Awakening on first join (configurable). A configurable share (default 2%) roll Heavenly
  Restriction; the rest are awakened sorcerers (the innate technique roll arrives with M3).
- Heavenly Restriction: 0 CE, plus transient attribute bonuses that scale with grade rank and
  Output (speed, attack damage and speed, armor, toughness, knockback resistance, safe fall,
  jump, step height) and faster natural healing after 3 s without damage. The Domain growth path
  is closed to it.
- Grade experience from defeating cursed-capable enemies (10 to 500 by rank) and landed casts.
  Trial counters (curse kills by rank, mastered techniques, Culling Game points, vow days, clash
  wins, the Unbound Ritual).
- Promotion checks experience, cast accuracy (10+ casts), lifetime CE output and every trial from
  grade data, and promotes as far as the player qualifies. Special Grade is reachable only
  through the Unbound Ritual trial; a special grade without that trial is refused.
- Promotion ceremony: ink wash, brush sweep in the grade color, name reveal, toast, original
  chime; reduced-motion variant. Nearby players see particles; the promoted player does not get
  particles spawned at their camera.
- Growth Tree: Output, Control, Efficiency and Domain, 20 points each, one point per 200
  experience and two per grade reached. Spending is a validated, rate-limited server request.
- Screens on the shared UI kit: Profile (model, grade, status, stats, next-grade checklist,
  trials) and Growth Tree, with a shared tab bar. Grade badge HUD with a bar toward the next
  grade and status tags.
- Commands: `/jjk grade get|set`, `/jjk xp add`, `/jjk awaken [heavenly]`, `/jjk counter add`,
  `/jjk progress reset`.
- Death policy PARTIAL now loses only experience earned inside the current grade, so it never
  demotes.
- Key bindings for every later system (cast, next slot, wheel, sense, domain, simple domain)
  are registered now under one "Jujutsu Craft" category.
- Tests: 9 new progression unit tests, 5 new server GameTests (promotion requirements, special
  grade lock, Heavenly Restriction stats, growth limits, kill experience), client screenshots of
  the Profile, Growth Tree, ceremony and badge.

## 0.2.0 (M1: cursed energy)

- Cursed energy (CE) on players and on cursed-capable mobs. Players get capacity from grade data
  and Output growth; Heavenly Restriction sets it to 0. Vanilla mobs get CE from
  `energy_profile` data (witch, vex, allay, evoker, illusioner, elder guardian, breeze, warden,
  wither, ender dragon). Curse entities supply their own capacity.
- Regeneration is computed lazily in closed form (`EnergyMath.advance`), so no entity is ticked
  for CE. Normal regen is 1.5% of max per second, and 2.5 times that after 5 s without a CE
  action. Spending below 10% of max starts a 3 s no-regen overexertion window with an original
  strain sound.
- Sync on change only. The server sends CE when it changes (coalesced per tick) and when max or
  the regen modifier drifts (checked once a second). The client predicts regen with the same
  math and eases the bar toward it.
- HUD: segmented CE bar with a regen indicator (one chevron normally, two while idle-boosted)
  and a danger palette plus diagonal stripes while overexerted. Edge vignette while overexerted.
  Position, scale, opacity and visibility are configurable.
- Persistence across death (death policy KEEP, PARTIAL or RESET_TECHNIQUES; CE refills to 50%),
  relog and dimension change.
- Configs: `config/jjkcraft-server.json5` (systems, energy, death, progression, casting,
  domains, curses, Culling Game) and `config/jjkcraft-client.json5` (HUD, VFX, accessibility,
  audio), through Cloth Config with comments. The client config screen opens from Mod Menu.
- Grade registry data for the full ladder (used by capacity now; promotion comes in M2).
- `/jjk energy get|set|fill|spend`, `/jjk config reload`.
- Shared UI kit: palette with a colorblind-safe variant, ink-brushed panels, rings, gradients.
- 26 original synthesized sounds (`tools/gen_sounds.py`), most of them for later milestones.
- Tests: 17 unit tests (CE math, death policy, ids), 6 new server GameTests (spend, overexertion,
  regen over time, mob profiles, grade capacity, death copy), and a client GameTest covering
  the HUD, prediction, overexertion, dimension change and relog.

## 0.1.0 (M0: scaffold)

- Checked and pinned the toolchain: Minecraft 26.3, Fabric Loader 0.19.5, Fabric API
  0.163.0+26.3, Loom 1.19.1 (`net.fabricmc.fabric-loom`, no remapping), Gradle 9.8.1, Java 25.
  Loom 1.19.1 requires at least Gradle 9.8.0, so the wrapper is newer than the example mod's
  9.7.1. The wrapper jar and distribution are pinned by SHA-256.
- Checked and pinned libraries: GeckoLib 5.5.7, Cloth Config 26.3.159, Mod Menu 21.0.0
  (development only). owo-lib has no 26.3 build, so it is not used.
- Project layout: `main` (common), `client`, `datagen` and `gametest` source sets, plus JUnit tests.
- Empty common and client entrypoints, a datagen entrypoint with an English lang provider, a server
  smoke GameTest, a client smoke GameTest and a unit test.
- Original mod icon, made by `tools/gen_icon.py`.
- `docs/DESIGN.md`: architecture, decisions, risks and unverified assumptions.
- `docs/TEST_REPORT.md`: M0 results.

M0 checks: `build` passes. The unit test passes (1 of 1). Server GameTests pass (2 of 2, one
of them vanilla's `always_pass`). `runDatagen` writes `assets/jjkcraft/lang/en_us.json`.
`runClientGameTest` boots the client, creates a world and saves a screenshot. `runClient`
reaches the main menu with jjkcraft, GeckoLib, Cloth Config and Mod Menu loaded.
