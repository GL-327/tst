# 1.8.0-preview scope

Implemented: twelve equipped slots with old-save normalization; Inherit/Random families
without the universal punch/teleport fillers; Blood Manipulation starter repair; normal
respawn at vanilla points; spirit choices in both modes; different-family awakening after
exorcism; persistent body/clan attributes; fallen-host Brain Transplant; captured-spirit
storage/release; personal Kogane and explicit Culling entry; Modulo sandbox abilities with
no story missions or required travel; ten workshop pages with per-player expressions.

The roster has 95 techniques, 29 innate families, 16 attribute definitions, and nine
domains. The added families are Brain Transplant, Boogie Woogie, Star Rage, Cursed Spirit
Manipulation, Disaster Flames, Ice Formation, Construction, Disaster Plants, Disaster
Tides and Sky Manipulation. Each has a themed set of grade unlocks. NPC entrants now
choose one family instead of an arbitrary mixture of unrelated moves.

Six Eyes is a Gojo-clan attribute, uses no technique slot, reduces costs and increases
energy perception range. Zero-CE restriction builds include Unbound Body and three
explicitly original variants (Silent Stride, Iron Frame, Spring Tendons). Other attributes
include Fragile Conduit, Iron Constitution, Fleet Footed, Powerful Frame, Abundant CE,
Refined Circulation, Electrified CE, Rough CE and Soul Awareness. Rolls persist through
relog and ordinary respawn; a new character rerolls them.

These are Minecraft gameplay adaptations. Several attacks share existing projectile or
field infrastructure; Garuda Strike is a mass attack rather than a fully independent
Garuda companion, and Perfect Sphere uses an armor-piercing attack rather than a complete
sphere simulation. The new domains have themed palettes and functional sure-hits, with
damageable barriers and rendered interior scenery. Body-transfer hosts use NPC/entity
models; arbitrary player skins, inventory takeover and every host's physical/clan trait
are not cloned. Multiplayer possession of living players is deliberately unsupported.

Automated tests do not replace human combat balance, long Hardcore sessions, four-player
network play, every individual effect/art review, or shader/backend compatibility. These
items remain on the wider continuation checklist. The build remains a preview.

Lore references consulted for family names and core concepts:
- https://jujutsu-kaisen.fandom.com/wiki/Kenjaku
- https://jujutsu-kaisen.fandom.com/wiki/Cursed_Technique
- https://jujutsu-kaisen.fandom.com/wiki/Cursed_Spirit_Manipulation
- https://jujutsu-kaisen.fandom.com/wiki/Culling_Game
- https://jujutsu-kaisen.fandom.com/wiki/Kogane
- https://jujutsu-kaisen.fandom.com/wiki/Six_Eyes
- https://jujutsu-kaisen.fandom.com/wiki/Heavenly_Restriction

Reproduce new data/icons with tools/expand_sorcery.py after the existing content scripts.
ExpansionLang.java is generated from the same descriptions. Existing resource identifiers
are retained for compatibility; additional JSON fields are optional on old saves.
