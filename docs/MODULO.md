# Modulo expansion (Phase 1, M10)

Jujutsu Kaisen Modulo (Gege Akutami and Yuji Iwasaki, 25 chapters, September 2025 to March 2026)
as an optional expansion: two dimensions, the Naunax Descent event, Mul energy, Simurian items,
companions, eleven missions in story order, four bosses and the epilogue. It is off unless the
server turns on `modulo.enabled`.

**Spoilers:** this file and the missions follow the whole story, including the ending.

## 1. Canon basis

Checked on the Jujutsu Kaisen Wiki (jujutsu-kaisen.fandom.com) on 2026-10-18 through its
MediaWiki API: the 25 chapter pages ("Modulo Chapter 1" to "Modulo Chapter 25"), Jujutsu Kaisen
Modulo, Simuria, Marulu Val Vol Yelvori, Cross Val Vol Yelvori, Dabura Karaba (and its synopsis),
Light, Otherworld Between Darkness and Light: Reverse Transcendence, Spejo, Dura Val Bobbidi
Mechika, Kalyan, Dapa, Kou Usami, Yuka Okkotsu, Tsurugi Okkotsu, Marulu Val Vol Yelvori vs.
Tsurugi Okkotsu, Reverse Cursed Technique.

What the game uses from canon:

- 2086, 68 years after the Culling Game. The mothership **Naunax**, carrying nearly 50,000
  Simurians, appears 30,000 m above Nevada; the United States delegated diplomacy to Japan
  (chapter 1). The Jujutsu Inspector General classifies it a special grade incident.
- **Simurians** have a third eye, the source of their **rolloluca** (their word for cursed
  energy). The ship carries the **Rumel** tribe, refugees from the **Deskunte**, who fought them
  over **Mul**, "a special ore that can convert rolloluca into all forms of energy" (Simuria).
  The Naunax runs on cursed energy (chapter 11). On Earth, Japanese citizens are trafficked
  because their cursed energy is an energy source (chapter 1).
- **Yuka Okkotsu** (Ten Shadows; summons Mahoraga against Dabura) and **Tsurugi Okkotsu**
  (Heavenly Restriction, "Physically Gifted"; the cursed tool Honoyagi; he powers New Shadow
  Style techniques with the cursed energy inside his cursed tool), grandchildren of Yuta Okkotsu
  and Maki Zenin.
- **Maru** (Marulu Val Vol Yelvori), the Simurian inspector, and his brother **Cross**. Maru's
  techniques are **Chaos** (mental confusion through the third eye, bent projectiles, gravity
  nullified or turned, water boiled or frozen) and **Harmony** (brings a person into harmony
  with the world; finds Tsurugi through Yuka, moves people, and drives the Ritual of Harmony).
- **Dabura Karaba**, the strongest Simurian, champion of the Deskunte; his sister is **Spejo**.
  Techniques: an unpronounceable Deskunte technique ("murderous intent given tangible form") and
  **Light** (light beams, solid light constructs that restrain or bear weight, sub-light speed).
  Pouring positive energy into Light makes **darkness** (its Cursed Technique Reversal, unnamed).
  Domain Expansion: **Otherworld Between Darkness and Light: Reverse Transcendence** (its
  sure-hit effect is never shown).
- **Kou Usami**, grade 1 sorcerer and acting minister for foreign affairs, uses Cursed Speech;
  **Mino** is his subordinate; **Kyoko Tomoe** is Kyoto Jujutsu High's nutritionist; **Osada** is
  a former sorcerer; **Masayoshi** is a boy with a guardian spirit; **Masaki Takeda** is an
  elderly curse user whose technique, **Marmalade Boy**, reshapes anything using human skin,
  himself and his guardian included, and who fights with needles.
- The ending: the **Ritual of Harmony** makes Tokyo's cursed spirits dissolve into light, cures
  Yuka and Tsurugi, and means no new sorcerers will be born. Dabura returns to Simuria and becomes
  the Deskunte's chief. Yuji Itadori tells Nobara he will become a cursed object after his death,
  so that someone could take the energy back if it is ever needed. The last chapter is
  "A Bright Future".

## 2. Where the brief and the wiki disagree

The wiki wins; each case below follows the wiki.

| Brief | Wiki | In the game |
|---|---|---|
| "Magic power is the energy resource Japan is hunting" | no such term; Japanese people are hunted for their cursed energy, and Mul converts rolloluca (cursed energy) into energy | the resource is **Mul energy**: cursed energy run through Mul. Config `modulo.mulEnergyRatio` replaces `magicPowerRatio` |
| "Simurian ore: a placeholder name" | the ore is **Mul** | Mul ore, Mul, High-Purity Mul ("a stash of high purity Mul" built the Naunax) |
| "the Rumelian weapon rolloluca" | rolloluca is the Simurian word for cursed energy, not a weapon | no item named rolloluca; the Rumelian Blade is an original weapon |
| "Dabura is the Simurian king" | the Deskunte's strongest warrior; he kills the chieftain and becomes chief only in chapter 24 | Dabura is the Deskunte champion |
| "Toranosuke (a Jujutsu High alumnus in a river-drying crisis)" | Toranosuke is a man-eating tiger driven down by a river dried by a solar plant; Osada is the former sorcerer | mission 3 is the tiger hunt, with Osada |
| "Masayoshi, the shapeshifting curse user with a thread-and-needle technique" | Masayoshi is a boy with a guardian spirit; the curse user is Masaki Takeda (Marmalade Boy: human skin as fabric, needles) | mission 4's boss is Masaki Takeda; Masayoshi is a character in it |
| "Maru's Omnipotence awakens" | no Omnipotence; Maru's third eye opens and he fights unconscious with Chaos (chapters 5 and 6) | mission 5 is "Berserk" |
| "Maru chooses to remove cursed energy instead of eradicating humanity" | Maru never planned to eradicate humanity; the Ritual of Harmony keeps cursed spirits from being born | the ritual ends mission 10 |
| Takoyaki party "with a giant octopus" | the wancha octopus, a mutated one-tentacled species; Maru's catch is bigger than Tsurugi's | mission 6's catch is a wancha octopus |

## 3. Design

### 3.1 Turning it on

`modulo.enabled` (default **off**, as the M0 config had it) gates everything: the Descent never
happens, the dimensions stay empty, missions do not start, and Modulo items still exist (so
worlds keep them) but do nothing. `modulo.epilogue` (default on) plays the epilogue sequence;
off skips it but still grants the reward.

### 3.2 Dimensions

Both are data: `data/modulo/dimension_type/` and `data/modulo/dimension/`, flat generators whose
biomes place the mod's features (`data/modulo/worldgen/`). Flat terrain plus code features was
chosen over custom noise settings: 26.3 reworked density functions and material rules, and a
flat base is predictable for mission sites.

- `modulo:naunax`: the ship's interior. A deck of hull plating with a ceiling, ribs and Mul
  conduits (feature `jjkcraft:naunax_deck`), Mul ore veins in the hull below (vanilla ore
  feature). No sky, fixed light, no natural spawns.
- `modulo:tokyo_2086`: Tokyo and, west of x = 0, the Kyoto region. City blocks on a street grid
  (feature `jjkcraft:city_block`): low-rise blocks in the Kyoto region, the ruins of the Inhuman
  Makyo in Tokyo. Day and night as in the overworld.

### 3.3 The Naunax Descent

A world event. When a player who has reached the configured grade (Grade 1) is in the overworld
and the timer is due, the Naunax appears above the nearest badlands (searched within
`descentSearchRadius`; without badlands, above the player's surroundings) for `descentMinutes`.
Clients draw the ship and a light ramp down to the ground. A player who steps onto the ramp's
foot boards the ship (`modulo:naunax`). No blocks are placed in the overworld. The event repeats
every `descentIntervalDays` game days and is announced to eligible players with its position.
After their first boarding, a player can return to the Naunax with Maru's Harmony (once Maru is
a companion) or a Naunax Beacon.

### 3.4 Mul energy

A second pool beside cursed energy (attachment `jjkcraft:mul_energy`, saved, kept through death,
synced to its owner). Hold Mul or High-Purity Mul (use) to pour cursed energy into it: each
second drains cursed energy and adds `drained × modulo.mulEnergyRatio` Mul energy, until the
piece is spent. Mul energy pays for Simurian gear and for techniques with a `mul_cost`. It never
replaces cursed energy, and Heavenly Restriction players (no cursed energy) cannot make it.
A HUD meter shows it when the player has any or is in a Modulo dimension.

### 3.5 Items and blocks

Simurian (original art and names unless canon): Mul ore, Naunax hull, deck plate, Mul conduit;
Mul, High-Purity Mul, the Rumelian Blade (lends Mul Lance, paid in Mul energy), the Gravity
Harness (Chaos-like gravity control paid in Mul energy), the Mul Ward (absorbs blows with Mul
energy), the Naunax Beacon (returns you to the ship). Canon: **Honoyagi** (Tsurugi's katana;
lends Batto Sword Drawing), takoyaki and the wancha octopus, Yuji Itadori's Cursed Object (the
epilogue reward).

### 3.6 Cursed tools as mediums

A cursed tool whose data says `"medium": true` pays for the technique it lends with its own
durability instead of the wielder's cursed energy, which lets Heavenly Restriction players cast
it (Tsurugi powers his techniques with his tool's cursed energy). Durability per cast is the
technique's cost × `durability_per_ce` (data). A tool's maximum durability comes from its grade
(`tool_durability` in grade data). Honoyagi and the Split Soul Katana are mediums.

### 3.7 Missions

Eleven missions in story order, data in `data/modulo/jjkcraft/modulo_mission/`, each a list of
stages: `talk`, `travel`, `defeat`, `deliver`, `make_mul_energy`, `defend`, `choice`, `sequence`.
A stage is a checkpoint: progress is saved per player after each one, and after death or a
relog the player resumes at the stage they were on. Mission sites (`modulo_site`) are built the
first time someone needs them. Bosses fight everyone on the same stage who is at the site, and
boss health grows with each extra player.

| # | Mission (chapter title) | Chapters | What the player does |
|---|---|---|---|
| 1 | Special Grade Incident | 1, 2 | board the Naunax in the Descent; meet Maru, Usami, Yuka and Tsurugi in Kyoto. Maru becomes a companion |
| 2 | The Kyoto Abductions | 1 | follow the gaze-manipulation path, defeat the trafficker in the talisman warehouse, free the children |
| 3 | Swimming Through the Forest | 3 | meet Osada at the dried river, hunt the tiger Toranosuke, bring its cub to Osada |
| 4 | The Boy Who Kept Repeating | 4, 5 | find Masayoshi at Shuei Elementary, defeat Masaki Takeda |
| 5 | Berserk | 5, 6 | Maru's third eye opens: reach him through Chaos and call him back; Cross intervenes |
| 6 | Cultural Exchange | 11 | catch a wancha octopus, bring it to Kyoko Tomoe for the takoyaki party (side mission) |
| 7 | Inhuman Makyo | 11 to 13 | learn the Naunax runs on cursed energy and make Mul energy at its core; hear Dabura's challenge |
| 8 | The Forbidden Technique Unleashed | 15 to 19 | Yuka summons Mahoraga against Dabura; hold off the fleeing cursed spirits until Mahoraga adapts |
| 9 | Chaos and Harmony | 14, 16 to 21 | duel Maru in the inverting arena, or stand aside and let Tsurugi fight |
| 10 | Ritual of Harmony | 20 to 23 | the final battle with Dabura, then the ritual |
| 11 | A Bright Future | 24, 25 | the epilogue sequence, once per player |

### 3.8 Companions and allies

Maru joins after mission 1 and follows his player wherever they are in the two Modulo
dimensions; he waits on the ship while they are elsewhere (the entity is not saved and is
brought back next to the player). He fights with Chaos: a confusing look, projectiles turned
aside, a short loss of gravity. Yuka and Tsurugi appear at mission sites, speak their lines and
fight in mission battles. Every multiplayer participant has their own Maru.

### 3.9 Bosses

Data in `data/modulo/jjkcraft/modulo_boss/`, behavior in code.

- **Masaki Takeda** (mission 4): three phases. He first passes as a child, then fights with
  needles beside his skin-sewn guardian, then hides inside the guardian; striking the guardian
  tears him out.
- **Maru** (mission 9, optional): Chaos turns the arena's gravity upside down for a few seconds
  at a time, confuses, and turns projectiles. He yields at low health.
- **Dabura** (mission 10, for 1 to 4 players; health grows per player): Light beams with a
  telegraph, solid light pillars that pin players until broken, his Domain Expansion, and the
  darkness explosion (his Cursed Technique Reversal) after a long charge: get behind a pillar,
  inside a Simple Domain or out of range. At the end Maru's Harmony takes him away, as in canon.
- **Mahoraga** (mission 8, Yuka's): fights Dabura while the player holds off cursed spirits.
  Adaptation is data: the first time a kind of damage hurts it, it gains resistance to that kind
  for the rest of the fight, and more with each further hit; its wheel turns each step. "Kind"
  is the technique that dealt the damage, or the damage type for anything else.
- Toranosuke (mission 3) and the trafficker (mission 2) are smaller fights.

### 3.10 Epilogue

The last mission plays a short sequence once per player: Tokyo's cursed spirits rise as light,
then a few lines of text. It grants the advancement "A Bright Future" (`jjkcraft:modulo/epilogue`)
and Yuji Itadori's Cursed Object, a special grade cursed object kept for the Phase 6 vessel
system.

## 4. Configuration

`config/jjkcraft-server.json5`, section `modulo`: `enabled` (false), `mulEnergyRatio` (0.5),
`maxMulEnergy` (200), `epilogue` (true), `descentMinGrade` (`jjkcraft:grade_1`),
`descentIntervalDays` (3), `descentMinutes` (10), `descentSearchRadius` (3200),
`bossHealthPerExtraPlayer` (0.6). Reasons: `docs/BALANCE.md` section 10. Data formats:
`docs/SCHEMAS.md`. Operators can drive everything with `/jjk modulo` (README, Commands), which
is also how the tests and screenshots reach each part.

## 5. Tests and what still needs a player

`ModuloMathTest` and `ModuloDataTest` (unit), `ModuloGameTests` (13 server GameTests) and the
Modulo scene of the client GameTest (8 screenshots) cover it; `docs/TEST_REPORT.md`, Phase 1,
lists them and what can only be judged by playing.
