# Visuals, combat and equipment — 1.12.1-preview

The 1.12.1 pass gives the new Red applications the spatial repulsion style and a brighter
core, with opposed blue/red strands and small orbiting cores for Purple. Infinity uses
faint spatial brackets and a floor ring; its owner sees a clear first-person view.
Channel rings stay below the owner's view, line particles keep camera clearance, and
positive-energy beams are narrower. Combat and charging use independently movable HUD
panels so charge effects and aiming remain visible.

Effects OFF also hides existing sustained geometry immediately; re-enabling it restores
still-active effects. Server casting settings bound attraction/repulsion fields to 16 per
caster and 128 per dimension by default, retiring the oldest when a limit is reached.
Mini Blue Cluster uses three fields. Server owners can adjust both limits.

This version builds on the available checkout. Unpushed changes on another computer are
not part of this build; preserve and merge that work separately when it is available.

All 52 curses and 23 summon/entity models have rebuilt native GeckoLib geometry and
face-specific UVs. Large volumes have shaped edges; long limbs have knees/elbows and feet;
creatures have material-specific patterns and emissive features. Mahoraga has a segmented
wheel. Each rig has idle, walking, running, windup, melee, casting, guarding, reaction,
spawn and death clips. Damage textures and matching lower-detail rigs are generated too.
The style remains Minecraft geometry, with original art rather than franchise assets.

`tools/reborn_creatures.py` authors all 52 curses from empty rigs: hollow cages, torn
veils, bookswarms, spiders, portals, bone structures and other species silhouettes.
`tools/spectral_anatomy.py` authors the large summon silhouettes; the remaining summons
also have new blueprints. `tools/spectral_model_sources.json` is an editable reference
snapshot, not a replacement for the authored blueprints. All item icons, 296 technique
emblems and six armor atlases have new materials from `tools/reborn_item_art.py`.

Generator order: `spectral_content.py`, `original_families.py`, `limitless_variants.py`,
`reborn_item_art.py`, then `verify_spectral_assets.py` and `charge_profiles.py --check`.
Run from the repository with Python/Pillow. Verification covers 75 distinct meshes,
4,841 UV-mapped cubes, 1,200 animation clips, 300 creature atlases, all equipment chains
and all technique icons. It checks parents, animation bone names and UV bounds too.

Technique effects now share bounded additive and translucent geometry passes. Materials
have different motion: inward spirals and dark cores for spatial techniques, blood
tendrils, ice crystals, rising flame filaments, shadow seals and healing double helices.
Domain interiors gain animated concentric seals and suspended motes. Existing VFX quality,
geometry/time budgets, distance checks and accessibility settings remain available.

Combat uses a raised two-hand stance, breathing and authored anticipation/contact/recovery
motion. Both hands angle inward in first person during a fight, when empty or using a cursed weapon. Outside fights the vanilla idle hand returns.
Vanilla item use keeps its own motion. Manual keys are U/I/Right Bracket (]), double-tap U/I for specials,
Left Alt for evasive movement; all can be rebound. Normal Attack/Use remaps are respected.

Auto Combat adds six to ten recovery ticks and visibly feints two of every five accepted
attacks. In-range stationary targets yield 60% contact over complete five-attack cycles;
range, aim, terrain, invulnerability windows, movement and defense can reduce that number.
This is not target aim correction. Manual attacks do not have the feint/accuracy cap.
Assisted defense commits three of five acquisitions, takes chip and cannot perfectly parry;
repeated guard packets cannot reroll the same acquisition. The HUD reports real contacts.

## Equipment

- **Sorcerer Uniform:** four pieces; each gives 2% technique CE discount, maximum 8%.
- **Binding Vestments:** each wards 5% damage, maximum 20%, spending 1.75 CE per damage
  prevented. Empty CE gives no ward. Sure-hit and bypass-invulnerability damage bypass it.
- **Heavenly Hunter:** each grants 3% movement speed and physical knockback resistance.
  No CE is required. The movement bonus disappears when unequipped.
- **Playful Cloud:** a physical three-section staff usable without CE.
- **Dragon Bone:** spends 3 CE on an additional bounded impact knockback.
- **Stormbell Spear:** original tool; 4 CE for a short stun, and lends Return Stroke.
- **Ember Wakizashi:** original tool; ignites targets and lends Disaster Flames.
- **Sealbreaker Tanto:** original tool; cancels active/charging powers and drains 8 CE.

Armor uses native armor slots, durability and attributes. Weapons retain grade requirements
and native attack attributes. Held tools that lend powers cast them with the vanilla Use
mapping, through normal CE, grade and cooldown checks. Recipes use CE shards plus leather,
iron, diamond or the relevant weapon material. Use a recipe viewer or Creative inventory
to inspect the pieces; they are in the existing Jujutsu Craft tab.

## Optional rendering support installed locally

Official Fabric releases of [Sodium](https://modrinth.com/mod/sodium) 0.9.2 for Minecraft
26.3 and [Iris](https://modrinth.com/mod/iris) 1.11.7 for 26.3 were downloaded from their
Modrinth releases and verified against publisher SHA-512 hashes. GeckoLib already supplies
the animation runtime. These third-party jars are local dependencies, excluded from Git
and the Jujutsu Craft jar. Iris is installed with shaders disabled by default: the Intel
UHD 620 test machine can use the new effects without a shader pack.

The old O block default migrates once to Right Bracket, preserving Minecraft Friends. On the first Iris-equipped launch, unchanged Iris defaults R/K/I move to F8/F9/F10 when those destinations are free (otherwise they are left unbound). Existing custom Iris keys are preserved. These bindings remain editable in Controls.

Compatibility tests use `gradlew runClientGameTest -PclientTestFocus=spectral
-PwithCompat=sodium,iris`. Ordinary development launch uses the jars in `run/mods` without
`withCompat`, to avoid loading them twice. On another computer, install the two official
Minecraft 26.3 Fabric releases into that instance's `mods` folder if desired.

Visual and controlled combat tests do not establish every PvP matchup, shader-pack
combination or multiplayer latency condition. The mod remains a preview for playtesting.
