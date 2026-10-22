# Jujutsu Craft (`jjkcraft`)

A Fabric mod for Minecraft Java 26.3 that adds cursed energy, cursed techniques, sorcerer
grades, Domain Expansions, original curse mobs and a Culling Game event.

**Status: 1.14.0-preview.** The full roster gains continuous mastery scaling through
400% training, lower mastered CE costs/cooldowns and stronger supported utility effects.
New filtered effect materials replace Limitless's flat cores and wire hoops, with
camera-centered moving charges and material effects across all 51 families. Empty-hand
combat uses skin-mapped fists and bent wrists. Infinity slows and holds approaching targets,
with separation only at close contact. [Changes and verification](docs/MASTERY_AND_VFX.md).
Blue/Red fist coatings, separate CE imbuement and wheel-only domain information remain.
[Controls](docs/COMBAT_REFINEMENT.md).

The quality-of-life update adds searchable family abilities,
live readiness filters, remembered wheel selections, summon dismissal, an Infinity
status/upkeep display and a configurable Ten Shadows ritual tracker.
[Controls and verification](docs/QUALITY_OF_LIFE.md).
The GitHub and local reworks remain combined, including the Projection planner, RCT
training, 75 creature rigs, defeat-to-tame subjugation, respawn-immunity repair and
additional equipment. [Combined behavior and migration](docs/RECONCILIATION.md).

The visual overhaul rebuilds 75 creature rigs with articulated
limbs, individual UV materials, emissive accents and sixteen animation clips each. Spells
use layered material effects, dark cores, orbiting filaments, crystals and blood tendrils.
Combat adds a breathing stance, body weight shifts, two visible first-person fists and
lighter impact pauses. Eight new cursed weapons and four four-piece armor sets have
crafting recipes and working combat bonuses. See [the visual and equipment guide](docs/VISUAL_OVERHAUL.md).

Suitable moves such as Blue, Red, Purple, Piercing Blood and
Granite Blast support holding to charge, then releasing to cast: up to 100%, or 200%
for Hollow Purple. Other moves use their normal press, toggle or held-channel controls.
Tooltips and the loadout show each casting style. Charging rapidly consumes CE, especially without Six Eyes.
Blue pulls real terrain into its core; Purple, destructive slashes and blasts damage blocks.
See [charging and terrain controls](docs/CHARGING.md) for costs, cancellation and server settings.

Twelve power slots are available from awakening. Bind a whole family to open its learned
variants, or bind a specific move directly. Hold G and scroll to select a slot; R opens a
family selector or casts its bound move. The wheel replaces the four-row sidebar. See
[wheel, Infinity and RCT controls](docs/TECHNIQUE_WHEEL.md). Each power has an independent expression and ten optional
binding-vow restrictions. Forsaking ordinary combat grants that power x5 output; restrictions
persist when switching slots, and casting seals vow changes for one minute.

Twenty original six-move families add marks, resonance, three-hit guards, movement,
control and prepared finishers. There are now **296 moves across 51 families**. Projection
Sorcery has a terrain-aware route preset and an editor for destinations and physical moves.
Explosive Flesh spends actual health, and regrowing its ammunition requires learned RCT.
See [the original families](docs/ORIGINAL_TECHNIQUES.md). Every family has at least four distinct moves. Ten Shadows now includes all ten summons,
Totality, Well’s Unknown Abyss, Agito and configurable two-to-four-component chimeras.
There are 18 new owned shikigami types and 22 new powers, including Moon Dregs, Garuda,
Rika and an original Moonlit Menagerie. See [the summoning guide](docs/MENAGERIE.md).

Starting CE is **350–550**, with independent regeneration levels **4–6**. Older survival
potential migrates once; custom Creative values remain exact. Lower reserves still train
more cheaply. Creative selection handles missing family data safely, clears stale death
protection and removes old defensive technique effects; defensive buffs cannot reach immunity.
The optional maximum Creative preset learns RCT with automatic healing off.

The extremely rare **Divine Chalice** replaces the innate family and clears technique mastery,
expressions, equipped moves, captured spirits, summons, fusion recipes and the old domain.
It preserves body/clan attributes, grade, growth, CE potential/training and universal skills.
Rare high-grade boss drops and an expensive endgame recipe are documented in the guide.

Auto Combat is on by default: holding attack chooses coarse attacks/defensive responses.
It commits three of every five attack opportunities and visibly feints the others. Actual
contact still requires range, aim and timing; manual attacks have no artificial accuracy cap.
Assisted guards cannot perfectly parry. The HUD reports the observed contact percentage.
Manual hit keys offer faster timing, double-tap uppercuts/sweeps and deliberate parries.
Vanilla Q/E/F and the normal hotbar keys remain available. Hostile melee mobs share server
frame windows, and curses have distinct combat clips and reactive guards.

There are now **296 powers across 51 families, 52 curse species, 16 body/clan attributes and
nine domains**. Twelve new species broaden the weak curse roster. Natural encounters are
less frequent, weak curses appear in small mixed packs, and Grade 2+ curses spawn alone
with space around them. See [the play guide](docs/PLAYTEST.md), [new powers and vows](docs/ARSENAL.md)
and [scope](docs/EXPANSION.md). The build remains a preview requiring human balance testing.

**Names and art.** Powers, techniques, domains, clans and terms that exist in Jujutsu Kaisen use
their canon names, checked on the Jujutsu Kaisen Wiki (`docs/NAMING.md`). Content with no
canon counterpart, such as the 52 curses and the Culling Warden, has original names. All art,
sounds and models are original, made by the scripts in `tools/`; the mod contains no official
Jujutsu Kaisen assets.

## Installing

1. Install Fabric Loader 0.19.5 or newer for Minecraft 26.3.
2. Put these in `mods/`: `jjkcraft-1.12.1-preview.jar`, Fabric API 0.163.0+26.3 or newer, GeckoLib 5.5.7
   or newer, Cloth Config 26.3.159 or newer. Mod Menu is optional (it adds a config button).
3. Install it on both the client and the server for multiplayer.

## What it adds

- **Cursed energy** on every player and on many mobs: a pool that regenerates (faster when idle),
  overexertion when it runs dry, a segmented HUD bar. Kept through death (configurable) and saved
  with the world.
- **Grades**, Grade 4 to Special Grade, earned with grade experience, measured stats (Output,
  Control) and trials, with a promotion ceremony. A growth tree (Output, Control, Efficiency,
  Domain). Heavenly Restriction is a rare roll at awakening: no CE, a stronger body.
- **296 cursed techniques** across ten categories, among them Cursed Technique Lapse: Blue and
  Reversal: Red (they burst when they meet), Hollow Purple, Infinity, Cleave and Dismantle, Cursed
  Speech (Don't Move, Crumble Away, Blast Away), Straw Doll Technique: Resonance, Idle
  Transfiguration, Projection Sorcery, Reverse Cursed Technique and Positive Energy Output, the
  Ten Shadows' Divine Dogs, Nue and Toad, and three ultimates, one per top grade (Piercing Blood,
  Thousand Lances, Maximum Output: Blue), plus original techniques such as Grudge Bolt.
  Choose Inherit for a clan family or Random for the general pool. Family moves unlock with grade; more techniques come from scrolls and cursed tools. A twelve-slot
  loadout, a technique wheel and a drag-and-drop loadout.
- **Black Flash** (a chance on the reinforced strike, for 1.9x to 3.8x and "the zone"), **Reverse
  Cursed Technique** (awakened at the edge of death: healing, a once-a-while save), **Binding Vows**
  (swear restrictions for power in the Vow Builder; break them and pay).
- **Domain Expansion**: nine domains, including Malevolent Shrine, Unlimited Void, Chimera Shadow
  Garden, Idle Death Gamble, Authentic Mutual Love and the disaster-curses' domains; barrierless
  and Simple Domains, clashes, burnout, interior scenery and breakable barriers.
- **52 original curses**, Grade 4 to Special Grade, each with one telegraphed mechanic and a
  counter, born from a fear field that rises with darkness and death. Nests, trophies, a codex
  with 3D previews, sensing through walls.
- **The Culling Game**: colonies behind barriers around spawn; awakened sorcerers have 19 days to
  enter one; inside you can't leave. Kills score points, points buy rules negotiated with the game
  master through the Kogane, and nine base rules (plus ten more to negotiate) run a small rule
  engine. NPC entrants make it playable alone. A map, scoreboard, rule list and HUD panel.
- **Cursed tools and objects**: ten grade-tagged weapons that lend techniques or act on hit
  (among them the Inverted Spear of Heaven, the Split Soul Katana and the Chain of a Thousand
  Miles), a cursed object (the Withered Digit), a flight gourd, a rule-bound points ledger, technique scrolls.
- **The Special Grade path**: the Unbound Ritual (a mastered innate technique, an active vow, and a
  special-grade curse killed inside your own domain).
- **Modulo sandbox powers** (`docs/MODULO.md`): Harmony / Chaos and Light / Darkness families,
  with eight moves available through Random awakening, the Creative picker and craftable scrolls.
  Existing Mul energy and Simurian gear remain. No story missions, boarding events, expedition
  choices or forced travel run; old story saves migrate back to ordinary play on joining.

Keys are editable in Minecraft Controls and J -> Moves -> Bindings. **U** light hit
(double tap: uppercut), **I** heavy hit (double tap: sweep), **O** block/parry, **Left Alt**
dash/evasive, **F7** Auto Combat toggle. Hold M1 for Auto Combat assistance. Q/E/F keep
vanilla behavior. The four direct power keys use **Numpad 1-4**; R casts the selected move,
V cycles slots; hold G and scroll, then release to select on the twelve-slot wheel. The bank key is configurable in Bindings.
J opens the profile; K expands a domain, N uses Simple Domain, H toggles RCT, M opens Culling.

## Content packs

Techniques, curses (on the mod's entity types), grades, domains, vows, Culling Game rules, cursed
tools and mob CE profiles are JSON in data packs; `docs/SCHEMAS.md` documents every field. A
working example is in `samples/` (and zipped in `build/distributions/` by the build): a technique,
a domain, a vow, a Culling rule, a mob profile, a tool retune and a tag addition, with a resource
pack for their names.

## Configuration

`config/jjkcraft-server.json5` (server): system toggles (Culling Game, domains, curse spawns), CE
numbers, death policy (KEEP, PARTIAL, RESET_TECHNIQUES), Black Flash and Reverse chances, domain and Simple
Domain numbers, curse spawn rates and caps, every Culling Game number (entry window, point
values, rule cost, colonies, entrants, end condition, auto-start), and the Modulo expansion
(`modulo.enabled`, on by default; Mul ratio and pool; boss health per extra player).
Legacy story settings are ignored. `config/jjkcraft-client.json5`
(client): HUD positions, scale, opacity and visibility per element, VFX level, shake, hit-stop,
reduced motion, reduced flashing, a colourblind palette, UI scale and the sound mix.

## Commands

All under `/jjk`, for operators (permission level 2): `energy`, `config reload`, `grade`, `xp`,
`awaken`, `counter`, `progress reset`, `technique` (grant, revoke, innate, equip, list, cast,
cooldowns), `curse` (spawn, list, clear), `fear`, `sense`, `domain` (grant, revoke, expand,
barrierless, close, simple, burnout clear, list), `blackflash force`, `reverse`, `vow`, `culling`
(start, stop, reset, status, colony add, join, points, rule add|remove, day, entrant), `modulo`
(descent start|stop|status, board, ship, tokyo, leave, mission status|complete|reset|jump, mul,
companion, site build).

## Building and running

You need a JDK 25. Gradle finds an installed JDK 25 through its toolchain support.

| Task | Command |
|---|---|
| Build the jar and sample packs, run unit tests and server GameTests | `./gradlew build` |
| Unit tests only | `./gradlew test` |
| Server GameTests only | `./gradlew runGameTest` |
| Client GameTests (opens a game window, takes screenshots) | `./gradlew runClientGameTest` |
| Development client | `./gradlew runClient` |
| Dedicated development server | `./gradlew runServer` |
| Regenerate data (lang, models, loot) | `./gradlew runDatagen` |
| Regenerate art | `python tools/gen_curses.py src/main/resources/assets/jjkcraft` (and `gen_items.py`, `gen_textures.py`, `gen_culling.py`, `gen_sounds.py`, `gen_modulo.py`) |
| Regenerate the Modulo data (missions, sites, bosses, the Modulo techniques and tools) | `node tools/gen_modulo_data.js .` |

The built jar is `build/libs/jjkcraft-<version>.jar`; the sample packs are in
`build/distributions/`.

**EULA.** A dedicated server, including `runServer`, starts only after you accept the
[Minecraft EULA](https://aka.ms/MinecraftEULA). Accepting it is your decision, so the build
never accepts it for you. To accept it, set `eula=true` in `run/eula.txt` after the first
`runServer`. If the GameTest tasks ask for it, set `eula = true` in the `configureTests` block
of `build.gradle`.

**Whitelist.** Since 26.3 a new dedicated server starts with the whitelist on (`white-list=true`).
For a test server, set `white-list=false` in `run/server.properties` or add the test accounts
with `/whitelist add`. GameTests are not affected.

**Compatibility runs.** Put third-party mod jars in `compat/mods/` and shader packs in
`compat/shaderpacks/` (both are git-ignored; download them yourself). Then
`./gradlew runClientGameTest -PwithCompat` runs with every jar there, `-PwithCompat=sodium,iris`
with only the jars whose names start with those words, and adding
`-PcompatShaders=<pack file name>` turns that pack on through Iris. Without `-PwithCompat`,
nothing changes. Jade and Iris are compile-only dependencies, fetched from Modrinth's maven.

**Windows paths.** Keep the project in a short folder such as `C:\dev\jjkcraft`. Gradle and the
game's run directory create deep paths, and very long base paths can hit Windows path limits.

## Target versions

All versions were checked on 2026-10-17 and are pinned in `gradle.properties`.

| Component | Version |
|---|---|
| Minecraft Java | 26.3 |
| Java | 25 |
| Gradle (wrapper) | 9.8.1 |
| Fabric Loom | 1.19.1 (official names, no remapping) |
| Fabric Loader | 0.19.5 |
| Fabric API | 0.163.0+26.3 |
| GeckoLib | 5.5.7 (required) |
| Cloth Config | 26.3.159 (required) |
| Mod Menu | 21.0.0 (optional) |
| Jade, Iris | 26.3.5+fabric, 1.11.7+26.3 (compile only, for the optional integrations; checked 2026-10-18) |

## Compatibility matrix

Tested on 2026-10-18 with the current 26.3 Fabric releases on Modrinth: the client GameTest (48
screenshots) with all four mods, again with an Iris shader pack, and the 103 server GameTests with
Lithium and Jade on the server. The mods were run together, not one at a time; each problem below
belongs clearly to one mod. Problems found were fixed in this mod. Details and what was not
checked: `docs/TEST_REPORT.md`, "Compatibility runs". To repeat the runs, see "Compatibility
runs" under Building and running.

| Mod | Version | Status |
|---|---|---|
| Sodium | mc26.3-0.9.2 | works: client GameTest passes; screenshots match a run without it (within run-to-run noise) |
| Iris | 1.11.7+26.3 | works without a shader pack. With Complementary Reimagined r5.9.3: problem found and fixed (domes and barriers were not drawn; Iris had no program for their pipeline). Other shader packs not tested |
| Lithium | mc26.3-0.26.2 | works: 103 of 103 server GameTests and the client GameTest pass |
| Jade | 26.3.5+fabric | problems found and fixed: its tooltip covered the domain meter, and it gave away the disguised Redacted Saint and Gilded Mimic. Now the tooltip moves clear and shows the disguise |

## Known issues

- Nobody has played it by hand: feel, balance and difficulty come from tests and numbers
  (`docs/BALANCE.md`). The manual checklist in `docs/TEST_REPORT.md` is still open.
- Multiplayer has been tested only with simulated players on one server; never with two real
  clients.
- Art is programmer art from scripts in `tools/`: original, but plain.
- Content data changes need a world restart (`/reload` does not refresh synced registries).
- Data-only curses must reuse one of the mod's curse entity types.
- The license is not chosen yet. `fabric.mod.json` says All-Rights-Reserved until the owner
  picks one.
- The new Modulo powers and expression combinations still need human balance testing.

## Documentation

- `docs/DESIGN.md`: architecture, decisions, risks, unverified assumptions
- `docs/NAMING.md`: canon names, their wiki sources, renamed ids and how old worlds still load
- `docs/AUDIT.md`: the state of the mod at the start of the 1.1.0 work
- `docs/MIXINS.md`: every mixin and why no event or API could do its job
- `docs/MODULO.md`: sandbox abilities, scroll recipes and migration from old story saves
- `docs/SCHEMAS.md`: every data file format, with examples
- `docs/BALANCE.md`: numbers and the reasoning behind them, and the balance test's budgets
- `docs/UI_SPEC.md`: every HUD element and screen
- `docs/VFX_SPEC.md`: every visual effect and how the options change it
- `docs/TEST_REPORT.md`: what was tested at each milestone, and how
- `CHANGELOG.md`: per-milestone changes
- `samples/README.md`: the sample content packs
"# tst" 
