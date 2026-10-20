# Modulo sandbox powers — 1.7.0-preview

Modulo contributes powers and equipment to the normal game. It has no active story,
mission progression, Descent event, boarding requirement, expedition pact or story travel.
Old mission/site/dimension identifiers remain registered so existing saves still load.

## Getting the powers

Random awakening can choose either new family. Creative players can select either in
J → Moves → Creative picker and receive its full Calamity loadout. The ordinary grade
unlock rules apply in Survival. These are gameplay adaptations of Harmony/Chaos and
Light/Darkness; the individual move names describe their Minecraft effects.

- **Harmony / Chaos:** Harmony: Step (Grade 4), Chaos: Twist (Grade 3), Harmony: Field
  (Grade 2), Chaos: Fracture (Grade 1).
- **Light / Darkness:** Light Ray (Grade 4), Darkness Field (Grade 2), Twin Radiance
  (Grade 1), Darkness Nova (Special Grade).

All eight moves also have shapeless technique-scroll recipes. Each recipe uses one
paper, one amethyst shard and the following third ingredient. Reading a scroll still
requires the move's grade; no story or mission must be completed.

- Harmony: Step — ender pearl.
- Chaos: Twist — echo shard.
- Harmony: Field — prismarine shard.
- Chaos: Fracture — obsidian.
- Light Ray — glowstone dust.
- Darkness Field — crying obsidian.
- Twin Radiance — blaze powder.
- Darkness Nova — wither skeleton skull.

All moves use the same four-slot loadout and per-player personalization as the rest of
the mod. Their applicable controls change actual range, force, projectile delivery,
field timing, movement, CE cost and appearance. Choose J → Moves → Personalize.

## Existing content and saves

Mul, Simurian equipment and the existing standalone powers/boss entities remain
registered. `modulo.enabled` controls their existing Mul/gear systems; it never enables
story progression. Old epilogue/Descent config fields are ignored. Operators retain
`/jjk modulo mul` and `/jjk modulo leave`; story jump/mission/boarding commands are removed.

On joining, players in old Modulo story dimensions return to their remembered boarding
location, or their normal respawn point. Old per-player mission progress is cleared.
A player already in an ordinary world stays there. Old gates/beacons cannot start travel.
Legacy dimensions and structures are retained for save compatibility, not required play.

Regenerate the new definitions, recipes and language entries with
`python tools/sandbox_powers.py` after older content generators. The generated
`SandboxLang` provider preserves these translations during normal datagen.
