# Jujutsu Craft (`jjkcraft`)

A Fabric mod for Minecraft Java 26.3 that adds cursed energy, cursed techniques, sorcerer
grades, Domain Expansions, original curse mobs and a Culling Game event.

**Status: 1.1.0.** Milestones M0 to M9 (1.0.0), the canon naming pass (Phase 0) and the optional
Modulo expansion (Phase 1) are built and covered by automated tests. Nobody has played it by hand
yet, so feel and balance come from tests and numbers; see Known issues. Phases 2 to 7 (visual
effects, curses, combat, awakening, death and cursed objects, release) are planned in
`docs/DESIGN.md` section 8.

**Names and art.** Powers, techniques, domains, clans and terms that exist in Jujutsu Kaisen use
their canon names, checked on the Jujutsu Kaisen Wiki (`docs/NAMING.md`). Content with no
canon counterpart, such as the 20 curses and the Culling Warden, has original names. All art,
sounds and models are original, made by the scripts in `tools/`; the mod contains no official
Jujutsu Kaisen assets.

## Installing

1. Install Fabric Loader 0.19.5 or newer for Minecraft 26.3.
2. Put these in `mods/`: `jjkcraft-1.1.0.jar`, Fabric API 0.163.0+26.3 or newer, GeckoLib 5.5.7
   or newer, Cloth Config 26.3.159 or newer. Mod Menu is optional (it adds a config button).
3. Install it on both the client and the server for multiplayer.

## What it adds

- **Cursed energy** on every player and on many mobs: a pool that regenerates (faster when idle),
  overexertion when it runs dry, a segmented HUD bar. Kept through death (configurable) and saved
  with the world.
- **Grades**, Grade 4 to Special Grade, earned with grade experience, measured stats (Output,
  Control) and trials, with a promotion ceremony. A growth tree (Output, Control, Efficiency,
  Domain). Heavenly Restriction is a rare roll at awakening: no CE, a stronger body.
- **30 cursed techniques** across ten categories, among them Cursed Technique Lapse: Blue and
  Reversal: Red (they burst when they meet), Hollow Purple, Infinity, Cleave and Dismantle, Cursed
  Speech (Don't Move, Crumble Away, Blast Away), Straw Doll Technique: Resonance, Idle
  Transfiguration, Projection Sorcery, Reverse Cursed Technique and Positive Energy Output, the
  Ten Shadows' Divine Dogs, Nue and Toad, and three ultimates, one per top grade (Piercing Blood,
  Thousand Lances, Maximum Output: Blue), plus original techniques such as Grudge Bolt.
  One innate technique is rolled at awakening; more come from scrolls and cursed tools. A six-slot
  hotbar, a technique wheel and a drag-and-drop loadout.
- **Black Flash** (a chance on the reinforced strike, for 1.9x to 3.8x and "the zone"), **Reverse
  Cursed Technique** (awakened at the edge of death: healing, a once-a-while save), **Binding Vows**
  (swear restrictions for power in the Vow Builder; break them and pay).
- **Domain Expansion**: six domains (Malevolent Shrine, Unlimited Void, Chimera Shadow Garden, Idle
  Death Gamble, Authentic Mutual Love, and the original Sevenfold Recitation), barrierless and
  Simple Domains, clashes decided by CE, burnout, and a full opening sequence.
- **20 original curses**, Grade 4 to Special Grade, each with one telegraphed mechanic and a
  counter, born from a fear field that rises with darkness and death. Nests, trophies, a codex
  with 3D previews, sensing through walls.
- **The Culling Game**: colonies behind barriers around spawn; awakened sorcerers have 19 days to
  enter one; inside you can't leave. Kills score points, points buy rules negotiated with the game
  master through the Kogane, and nine base rules (plus ten more to negotiate) run a small rule
  engine. NPC entrants make it playable alone. A map, scoreboard, rule list and HUD panel.
- **Cursed tools and objects**: five grade-tagged weapons that lend techniques or act on hit
  (among them the Inverted Spear of Heaven, the Split Soul Katana and the Chain of a Thousand
  Miles), a cursed object (the Withered Digit), a flight gourd, a rule-bound points ledger, technique scrolls.
- **The Special Grade path**: the Unbound Ritual (a mastered innate technique, an active vow, and a
  special-grade curse killed inside your own domain).
- **The Modulo expansion** (optional, off by default; `docs/MODULO.md`): 2086, the Simurian ship
  Naunax over the badlands. Every few days a Descent lowers a ramp of light that Grade 1 and higher
  sorcerers can board. Two dimensions (the Naunax and Tokyo 2086), eleven missions that follow the
  Jujutsu Kaisen Modulo story with its characters (Maru, Cross, Yuka and Tsurugi Okkotsu, Kou Usami,
  Dabura Karaba and others), boss fights against Toranosuke, Masaki Takeda, Maru and Dabura,
  Mahoraga adapting to whatever hurts it, a branch choice, and an epilogue. Mul, the Simurian ore,
  turns cursed energy into Mul energy for Simurian gear and Mul techniques; cursed tools can be
  used as mediums that spend durability instead of CE. A mission tracker, Mul meter and dialogue
  on the HUD.

Keys (all rebindable): J profile, R cast, V next slot, G technique wheel (hold), B sensing, K
Domain Expansion (sneak + K: barrierless), N Simple Domain, H Reverse Cursed Technique auto-heal,
M Culling Game map. Screens and HUD are described in `docs/UI_SPEC.md`.

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
(`modulo.enabled`, off by default; Mul ratio and pool; the epilogue; the Descent's minimum grade,
interval, length and search radius; boss health per extra player). `config/jjkcraft-client.json5`
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
- The Modulo expansion has been checked by GameTests and screenshots only: the missions'
  pacing, the boss fights' difficulty and how the ship looks in the sky need someone to play
  them (`docs/TEST_REPORT.md`, Phase 1). Its sites are built from blocks by code and are plain.

## Documentation

- `docs/DESIGN.md`: architecture, decisions, risks, unverified assumptions
- `docs/NAMING.md`: canon names, their wiki sources, renamed ids and how old worlds still load
- `docs/AUDIT.md`: the state of the mod at the start of the 1.1.0 work
- `docs/MIXINS.md`: every mixin and why no event or API could do its job
- `docs/MODULO.md`: the Modulo expansion: its canon basis and sources, where the brief and the wiki
  differ, the design, missions and config
- `docs/SCHEMAS.md`: every data file format, with examples
- `docs/BALANCE.md`: numbers and the reasoning behind them, and the balance test's budgets
- `docs/UI_SPEC.md`: every HUD element and screen
- `docs/VFX_SPEC.md`: every visual effect and how the options change it
- `docs/TEST_REPORT.md`: what was tested at each milestone, and how
- `CHANGELOG.md`: per-milestone changes
- `samples/README.md`: the sample content packs
"# tst" 
