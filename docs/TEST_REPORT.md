# Test report

## 1.17.1-preview, 2026-10-22

All **140 unit tests and 279 server scenarios passed**, and packaging succeeded in
`build/output-balance-final-build.log`. New coverage checks monotonically increasing
grade/training/mastery ceilings, paid output beyond 300%, finite extreme costs and
substantially stronger veteran power. Every registered technique retains its behavior
identity with the expanded tuning bounds.

Server scenarios also verify effective limits on older saves, normalization before
persistence, a real 1,000% Dismantle hit and its increased CE deduction, refusal with
insufficient energy, control-drill limits for reinforcement/RCT, and four-digit creative
settings through the codec and complete server character validation. The barrier
regression now expects the intended stronger starter protection while still checking
CE payment and removal when lowered.

The focused native `output` scenario with Sodium/Iris **passed**. It uses the actual workshop and
universal-skill fields to save 3,000%, checks the server replies, then lowers the character's
grade/training and verifies that the client returns to the 300% ceiling. Its log is
`build/output-balance-client.log`; screenshots stay under `build/`.

## 1.17.0-preview, 2026-10-22

All **137 unit tests and 275 server scenarios passed** in `build/server-117-final.log`.
An additional targeted animated Black Flash training regression passed in
`build/dojo-contact-check.log`. It covers the real framed combat path, including the
replacement damage source, rather than calling the doll's damage handler alone.

The new server coverage checks bounded physical bonuses, blocked survival purchases,
contact rate limits, reinforcement learning, timed breaths and AFK rejection, XP-free
reverse-flow study, weapon profiles, offhand damage isolation, death persistence,
network round trips and habitat preferences. Existing characters keep their learned skills.

The final native `dojo` client run with **Sodium and Iris passed** in
`build/client-117-release.log`. It uses actual combat input against the training doll,
renders five held-item profiles in first and third person, opens every mod menu, drags a
layout part using real mouse input, imports/exports the design, reloads a generated prop
pack and checks that the live resource contains the edited model. It also renders all
52 curse species. Screenshot review corrected prop-preview occlusion and anchored
weapon windups at the hand grip. The final art fixture fixes front-view interpolation
before each capture; screenshots live only under `build/`. Packaging succeeded in
`build/package-117.log` and produced `build/libs/jjkcraft-1.17.0-preview.jar`.

The asset validator passed: **75 creature rigs, 2,335 mapped cubes, 1,200 animation
clips, 300 atlases, 17 equipment items and 296 technique icons**, plus sixteen filtered
glow materials and sixteen seamless volume surfaces (`build/asset-check-117.log`).
The training dummy is additionally validated by the native pack-reload scenario.

## 1.12.1-preview, 2026-10-21

All **128 unit tests and 256 server scenarios passed** in `build/hour-final-build.log`.
Fifteen new server regressions cover projectile-only Infinity persistence/defaults,
revoked/dead/spectator/dimension-sensitive casting, short-stun interruption, a real melee
hit interrupting charge, failed channel replacement, line-of-sight mending, exact slab
footing, gaps/walls, targetless Projection, spatial punch force, family-binding edits and
per-owner/per-dimension field budgets. The existing mixed-load benchmark recorded a
**9.50 ms average server tick** with 40 curses, 12 entrants and four summons over 200 ticks;
this is a test workload measurement, not a client FPS claim.

The creature/item audit still passes 75 distinct rigs, 4,841 UV-mapped cubes, 1,200 clips,
300 atlases, 17 equipment items and all 296 move icons. Charge profiles remain 19 opt-in
chargeable moves and 277 normal activations. No runtime worlds, configuration or logs are
part of the source commit.

Native Sodium/Iris input/render checks passed in `build/hour-input-vfx-final.log`, with
**26 screenshots** preserved in `build/hour-verified-screenshots`. They cover keyboard and
rebound mouse selection/casting, releasing a different button during charge, branch
filters/pinning, Infinity filter resets, immediate effects-off/restoration, rapid passive
toggles without duplicate geometry, presets retaining vows/style, Red/Purple charge VFX,
the compact HUD, waypoint dragging/deletion/saving, blocked segments, a personalized aim
offset and cancelling a paid windup across dimensions. Existing wheel/RCT/curse damage,
family binding and save-reopen checks passed in the same native run.

The final client-only corrections were packaged in `build/hour-package-final.log` after
the full server pass; packaging kept the verified main/server code. Artifact:
`build/libs/jjkcraft-1.12.1-preview.jar`, **11,934,304 bytes**, SHA-256
`e095b943e78d1fd8a3f3b31bdf8098937e9b2e9fc33682cd7943f329fb73174f`.

## 1.12.0-preview, 2026-10-21

All **128 unit tests and 241 server scenarios passed** in `build/release-final.log`.
The full suite covers the existing awakening/death, CE balance, techniques, charging,
terrain, domains, curses, summons/fusions and Culling systems, plus the new work:

- Measured Auto attacks commit 6/10 contacts against an in-range stationary target;
  aimed manual attacks contact 10/10. Feints, guard limitations and multi-target counters
  are checked. Actual contact still depends on aim, reach, movement and defense.
- All twenty original six-move families load and their primers actually hit/build
  independent resources. Finishers consume preparation, guards are bounded and CE-paid,
  movement respects terrain, and Projection uses actual physical attacks and safe routes.
- Explosive Flesh spends real health and refuses unsafe casts; regrowth requires RCT.
- Family bindings accept cleared slots, hide unlearned Purple, reject forged variants,
  use normal charging/release, survive Creative edits and persist through world reopen.
- Infinity coexists with channels, drains CE without RCT, drops on exhaustion, and is
  bypassed by sure-hits/hazards and cancelled by eligible dispelling weapons. A Forsake
  RCT vow disables free Infinity upkeep, positive discharge and brain recovery.
- Three completed RCT drills consume nine XP levels, interruptions grant no lesson/XP
  loss, universal skills preserve bindings, Chalice preserves completed lessons, and
  positive-energy discharge damages curses while leaving non-curse targets unharmed.
- Blue Punch deals real contact damage and attraction; Mini Blue Cluster creates three
  separate attraction fields. Equipment recipes, native armor effects and tool dispels pass.

Native Sodium/Iris tests passed with shaders disabled on Intel UHD 620:
`build/reborn-native-final.log` loads/renders all 75 new creatures, representative action
clips, all new equipment, material VFX, actual inverted mouse Auto input, manual attacks,
domains, the editable Projection route and physical impacts. Its **128 screenshots** are
preserved in `build/reborn-final-screenshots/screenshots`. The focused final inward-hand
run passed in `build/reborn-hands-final.log`; **15 screenshots** are preserved in
`build/reborn-hands-screenshots/screenshots`. Idle hands return to vanilla outside fights.

`build/release-native.log` passed actual G/scroll/release slot selection, R family opening,
held mouse Blue charging/release, persistent Infinity synchronization, the three-stage
Practice UI and cancellable drill, actual curse damage from RCT, resonance HUD updates,
the Families screen's Bind family button, and binding persistence after reopening the
world. Its **10 screenshots** are in `build/release-wheel-screenshots/screenshots`.
Visual/input testing caught and fixed initial mouse-hover overriding scroll, HUD/wheel
occlusion, a missing resource update, old O/F block defaults, Iris R/K/I conflicts, item
atlas paths and Projection's right-click handling. Final selector text and badge labels
were compiled in the packaging pass.

The asset audit passes **75 distinct rigs, 4,841 UV-mapped cubes, 1,200 clips, 300 creature
atlases, 17 new gear items and 296 technique emblems**. It validates rig parents, animation
bone references, face UV bounds, LOD compatibility and equipment chains. Charging is
opt-in for **19** suitable moves; **277** use ordinary activation. The ZIP integrity and
version audit confirms 296 move definitions / 51 families, no GameTest classes and no
nested third-party jars. Sodium/Iris publisher SHA-512 records are in
`docs/RENDERING_DEPENDENCIES.json`; those optional jars are local and excluded from Git.

Artifact: `build/libs/jjkcraft-1.12.0-preview.jar`, **11,920,078 bytes**. SHA-256:
`ba945280ff1ee68a931a275f843e08bfa8eb96847b0eb5d7242315c6fad8baec`.

This is a tested preview, not evidence of every AI matchup, multiplayer latency condition,
shader pack or long-session/PvP balance outcome. NoAI galleries validate visible assets;
combat scenarios validate controlled mechanics. OSHI/Realms development warnings and
Sodium option normalization are recorded in logs and did not fail the passing runs.

## 1.11.1-preview, 2026-10-20

All **127 unit tests and 212 server scenarios passed** in
`build/selective-charge-server.log`. New checks confirm only eighteen authored moves
charge; ordinary summons activate on press and are not replaced on release; Boogie
Woogie swaps immediately; Infinity toggles immediately and survives menu cancellation;
guard channels start on press and stop on release/menu. Charging flags survive every
technique's personalisation, and older/datapack cast definitions default to false.

The native run passed in `build/selective-charge-native.log`: real R taps toggle Infinity
and summon Divine Dogs without charging, guard sustains while held and ends on release,
and Blue/Purple still charge to 100%/200% with CE drain and terrain effects. Client
registries receive the charge opt-in. Eight screenshots are preserved in
`build/selective-charge-screenshots`; the normal-activation label was reviewed at compact
GUI scale. A final two-pixel description adjustment keeps room for text below that label.
`tools/charge_profiles.py --check`, ZIP/version checks, all 169 packaged move definitions,
the 18/151 casting split and exclusion of test classes passed. Runtime logs and worlds
are excluded from the commit.

Final artifact: `build/libs/jjkcraft-1.11.1-preview.jar`, **4,851,039 bytes**; SHA-256
`9cb574aa5bb851eb555e45b8dccd42682a6945619bf0e99a4779424aadfdf08e`.

## 1.11.0-preview, 2026-10-20

All **127 unit tests and 208 server scenarios passed** in
`build/charge-verified-package.log`. New checks cover hold/release casting, the server's
200% Purple cap, unrelated key releases, cancellation/unbinding, CE exhaustion, actual
Six Eyes spending, channels starting on release and stopping on a second press, real
log/leaf removal, transient debris cleanup, Purple's corridor, protected containers and
bedrock, Fabric claim vetoes and the server terrain toggle. Charging suppresses natural
regeneration so high-capacity characters cannot regenerate through its CE cost.

The final native client run and package succeeded in `build/charge-final-native.log`.
Actual held R input charges Maximum Output Blue to 100%, visibly drains CE, removes
tree blocks on release and renders its effects. Purple reaches 200%; opening the profile
cancels a held charge. Screenshot review corrected the charge/combat HUD overlap.
Six final screenshots are preserved in `build/charge-final-screenshots`.
The final network cancellation refinement preserves unrelated active toggles and is
covered by this native cancellation path. These checks do not establish multiplayer
latency balance or compatibility with every third-party protection mod.

Artifact: `build/libs/jjkcraft-1.11.0-preview.jar`, **4,850,197 bytes**; SHA-256
`b3479ad877515d97c05aeee5663a39667208b1d0a008e6a746d87cb2c3b475d8`.
ZIP integrity, packaged version, all 169 technique definitions and exclusion of GameTest
classes were verified. Runtime logs, worlds and build caches are excluded from staging.

## 1.10.0-preview, 2026-10-20

123 unit tests and all 198 server scenarios passed in the final full build:
`build/menagerie-final-package.log`. The final Chalice body-health preservation refinement
passed its focused rerun with the rebuilt artifact (`build/menagerie-chalice-final.log`).
New scenarios exercise missing/empty Creative family IDs; ordered, one-time potential
migration; exact Creative overrides; all Ten Shadows entity definitions; Agito's four active
components; failed-fusion atomicity; saved custom component inheritance; invalid/duplicate
recipe refusal; Divine Dogs Totality; complete Chalice technique reset with preserved body,
clan, growth and CE; deliberate use and exact consumption; real loaded-player Blood/Mass
damage; guardian redirection and shared heal limits; and bounded adaptation. A loot codec
round trip confirms player-kill and 1% chance conditions are retained by Minecraft rather
than ignored JSON.

The native client scenario passed twice, including the final run in
`build/menagerie-client-final.log`: a legacy missing-family record opens safely; selecting
Ten Shadows remains a draft, then applies successfully; fusion choices are saved through
the UI; an actual cast consumes the live components into one chimera; the recipe survives
world reopen. All eighteen new beast types render without missing resources. Twenty-three
screenshots are preserved in `build/menagerie-final-screenshots`; the previous review set is
in `build/menagerie-screenshots`. The static NoAI gallery shows their fixed body orientation;
it is a resource/render check, not evidence for every combat animation or AI matchup.

There are 169 powers, 31 families, 18 additional owned shikigami types, 52 curse species,
16 attributes and nine domains. All referenced icons exist, and scroll ingredient checks
found no conflicting shapeless recipes. Generator reruns do not duplicate family moves or
loot pools. `git diff --check` passed.

Final jar: `build/libs/jjkcraft-1.10.0-preview.jar` (4,833,601 bytes), SHA-256
`d9a494d6718f363042c8c032bf375af1c80d3d2c443891a99d999e43ac29f4e4`. ZIP/version/definition counts passed, and test classes are excluded. Normal
Minecraft was reopened with the final build. GeckoLib loads 131 models and 77 animation sets.
Human PvP/latency balance, every fusion combination and canon-accurate high-detail art remain
outside these automated checks; this remains a playable preview.

## 1.9.0-preview, 2026-10-20

123 unit tests passed. The final full server run passed 182/183 scenarios, including all ten
new family/character checks. The existing Tithe payment scenario failed after its random
resurfacing could leave the harness's entity-ticking area; the fixture now returns it to the
payment area after testing resurfacing, holds its movement and the offering, then exercises
the unchanged payment mechanic. Its focused confirmation passed. All 183 scenarios passed
across the final full and focused runs. Logs: `build/atelier-final-package.log` and
`build/atelier-payment-final.log`. No production curse mechanic was weakened for this test.

New server coverage verifies all 29 families have at least four valid moves; 147 registered
powers; additive owned souls with a six-soul cap; real summon count/vitality/lifetime tuning;
servant AI attacks; soul damage/binding; Furnace contact ignition and retained tuned fire;
custom grade, CE, growth, mastery, attributes and skill values; no unwanted Calamity/Six Eyes,
family moves or domain regrant; atomic invalid/survival edit refusal; exact uncapped Creative
Mechamaru balances; and movement/healing vow enforcement.

The complete native client run passed in `build/atelier-client-complete.log`. It covers the
family browser, loadout details, summon personalization, optional Auto Calamity toggle,
seven Creative pages, exact numeric edits with server acknowledgement, both GUI sizes,
quick/held inverted right-mouse Auto attacks, inverted manual attacks, Use remaining separate,
visible soul summons, and reopening with custom birth/regen intact. A focused inverted-input
run also passed (`build/atelier-input.log`). Thirteen screenshots are preserved in
`build/atelier-screenshots`. Earlier UI test attempts exposed ambiguous navigation labels;
the final editor uses distinct Training/Attributes/Powers labels. The combined input fixture
now explicitly aims the local and server player after GUI resize/teleport.

The release jar is `build/libs/jjkcraft-1.9.0-preview.jar` (4,626,384 bytes), SHA-256
`de7e4384524519302dd520bc2ee69d73eb3d6043eaee8fe76cbc4ead91a1bfab`.
ZIP, version, 147 packaged definitions, soul resources, missing icon and duplicate scroll
recipe checks passed. Test classes and the removed vanilla-key interception are excluded.
`git diff --check` passed. Normal Minecraft was launched with 1.9.0-preview; GeckoLib loaded
113 models and 59 animation sets. Human PvP/latency balance and the full tuning/vow combination
space still need playtesting.

## 1.8.0-preview, 2026-10-20

123 unit tests passed. The final full server run passed 172 of 173 scenarios, including all
new feature checks; the intermittent door-ambush fixture was isolated and kept beside its
frame, then passed its focused confirmation. All 173 scenarios have therefore passed across
the full and focused runs. The separate curse-payment scenario passed both in isolation
and in the final full run after moving it to its own test environment. Logs:
`build/arsenal-final-package.log`, `build/arsenal-tithe-confirm.log`, `build/arsenal-door-confirm.log`.

The complete real-client run passed in `build/arsenal-client-final.log`: native clearing and
refilling of all twelve slots, banked power casting without changing the inventory hotbar,
normal Q/E/F actions, saved Auto Combat preferences, both vow pages and acknowledged saves,
manual double-tap uppercut/sweep, actual Auto M1 contact, twelve curse previews and reopening
with the twelve-slot loadout and vow intact. Nineteen reviewed screenshots are preserved in
`build/arsenal-screenshots`. One earlier parallel test launch deadlocked in Fabric's initial
phase barrier before any world existed; the disposable client was stopped and a fresh isolated
client completed normally. No production workaround was added for that startup failure.

The build contains 95 techniques, 29 families and 52 curse species. Dedicated art references,
32 new technique glyphs, twelve spawn-egg texture/model sets, loot tables and saved defaults
were checked. Final jar ZIP/version checks passed, with test classes and the removed inventory
key interception excluded. `git diff --check` passed. Human PvP balance, latency and the large
combination space of expressions/vows remain outside this automated run.

## 1.7.0-preview, 2026-10-20

114 unit tests and all 163 server scenarios passed in one complete run. The real client
also passed native right-click clearing and left-click refilling of every slot, eight
workshop pages, a numeric edit with server acknowledgement, Creative Modulo selection,
and expression persistence after reopening the world. Log: `build/sandbox-integration-checks.log`.

New server checks cover cleared-slot reuse/channel cancellation, story suppression with
legacy progress at multiple grades, both new families, all expression axes and old-save
defaults, independent owners, projectile tuning, summon stats, health sacrifice payment
and refusal, and all eight loaded scroll recipes.

Ten screenshots were reviewed and preserved in `build/sandbox-screenshots`. The editor
disables controls that do not apply to the selected move. Human multiplayer balance and
the large space of combined settings remain playtest work.

Final compilation, unit checks and assembly passed in `build/sandbox-final-package.log`.
The 1.7.0-preview jar passed ZIP integrity and embedded-version checks, with test-only
classes excluded. `git diff --check` passed.

## 1.6.0-preview, 2026-10-19

111 unit tests passed. The full server run passed 157 of 158 scenarios; the remaining
finisher/escape fixture was corrected to use a real survival player and the normal 26.3
client-loaded acknowledgement, then passed in isolation. Thus all 158 server scenarios
have passed across the full and focused runs. Logs: `build/quality-server-package.log`
and `build/quality-finisher-confirm.log`. The filter is reusable with `-PgameTestFilter=<id>`.

The real client passed native key rebinding, direct skill casting without changing the
inventory hotbar, numeric workshop edits and server acknowledgements, all four workshop
pages, four rendered domain interiors, and native M1 contact damage. Thirteen screenshots
were reviewed and preserved in `build/quality-overhaul-screenshots`. The final client run
also completed a clean disconnect; log: `build/quality-overhaul-final-checks.log` (its overall
Gradle result reflects the pre-correction server fixture failure, not a client failure).

Asset checks validated 40 base curse atlases and 120 matching glow/wounded/critical variants.
The packaged jar passed ZIP integrity, embedded-version and client-mixin checks; test-only
classes and the Fabric shutdown workaround are excluded. `git diff --check` passed.

Human PvP feel, balance across latency and per-weapon move sets still need playtesting.
This report records actual automated and visual checks, not a claim that every interaction
or balance combination has been tested.

## 1.5.0-preview, 2026-10-19

110 unit tests passed. All 152 required server GameTests passed on the standalone
rerun (`build/deep-server-recheck.log`). The first combined run had one intermittent
Doorframe Hunter AI fixture failure; all new feature tests passed in that run.

The real client completed Creative selection at Calamity, all four workshop pages,
advanced-setting synchronization, independent regeneration display, uncapped storage
above normal capacity, and persistence of the bank/tuning/regen through reopening.
Screenshots in `build/run/clientGameTest/screenshots` were visually inspected.
Combined log: `build/deep-sorcery-checks.log`. New tests also reject survival grants,
verify full family mastery/domain and ensure Fill cannot create an uncapped bank.

Earlier reports below describe their respective versions.

Every check below says how it was run. "Automated" means a Gradle task ran it and it passed.
"Log-checked" means the game was launched and its log was read, without a human playing. Visual
quality, feel and balance need a human tester. Those rows stay open until someone plays.

Sections up to and including the compatibility runs use the names of their time. Phase 0 (below)
renamed many of them to their canon names; `docs/NAMING.md` maps old names to new.

## Test rig

| Item | Value |
|---|---|
| OS | Windows 11 Pro 10.0.26200 |
| Java | Oracle JDK 25.0.1 |
| Gradle | 9.8.1 (wrapper) |
| GPU / frame rate | NVIDIA GeForce RTX 5070, driver 610.88, OpenGL 3.3; CPU Intel Core i5-14600KF (from the 2026-10-18 logs); frame rates per milestone below |

## M0 (0.1.0), 2026-10-17

| Check | How | Result |
|---|---|---|
| `./gradlew build` | automated | pass |
| Unit tests (`JjkCraftIdTest`) | automated, `test` | 1 of 1 pass |
| Server GameTests | automated, `runGameTest` (part of `build`), report at `build/reports/gametest/server.xml` | 2 of 2 pass: `jjkcraft-gametest:smoke_game_test_mod_initialized` and vanilla's `minecraft:always_pass` |
| Datagen | `runDatagen` | pass; writes `src/main/generated/assets/jjkcraft/lang/en_us.json` |
| Client GameTest | automated, `runClientGameTest` | pass; both entrypoints ran, a singleplayer world was created, and a screenshot was saved to `build/run/clientGameTest/screenshots/` |
| Empty mod loads in `runClient` | log-checked | pass; 57 mods loaded, including jjkcraft 0.1.0, GeckoLib 5.5.7, Cloth Config 26.3.159 and Mod Menu 21.0.0. Common and client init logged, the resource reload finished, and the sound engine started. The game was then closed. |
| Dedicated server (`runServer`) | not run | It needs the Minecraft EULA accepted, which is the user's decision. See README. |

Known log noise, not caused by the mod: OSHI reports `Failed to get system info` for process
stats on this machine (Windows perf counters), Realms authorization fails in development, and
the client logs `Illegal option value 0 for Anisotropic Filtering` when run with fresh game options.

## M1 (0.2.0), 2026-10-17

| Check | How | Result |
|---|---|---|
| `./gradlew build` | automated | pass |
| Unit tests | automated | 17 of 17 pass (`EnergyMathTest` 13, `DeathPolicyTest` 3, `JjkCraftIdTest` 1) |
| Server GameTests | automated | 8 of 8 pass: spend and fail, overexertion blocks regen, regen over 2 s, witch has 60 CE and cows have none, Grade 1 has 360 CE and Heavenly Restriction has 0, death keeps progress and refills to 50% |
| Client: HUD bar and prediction | automated (`runClientGameTest`), screenshot `0001_jjkcraft-m1-energy-bar.png` | pass; client sees 100 max CE, prediction tracks server regen |
| Client: overexertion visuals | automated, screenshot `0002_jjkcraft-m1-overexerted.png` | pass; red bar with stripes, edge vignette |
| Dimension change | automated (teleport to the Nether and back) | pass; CE kept |
| Relog | automated (close and reopen the save) | pass; CE kept |
| Death and respawn in a real client | not done by a human yet | the copy logic is covered by a GameTest |
| HUD layout at 1280x720 and larger UI scales | not done yet | default layout checked at the test window size (854x480) |

## M2 (0.3.0), 2026-10-17

| Check | How | Result |
|---|---|---|
| `./gradlew build` | automated | pass |
| Unit tests | automated | 27 of 27 pass (adds `ProgressionMathTest` 9; `DeathPolicyTest` now 4) |
| Server GameTests | automated | 13 of 13 pass: adds promotion needs experience, control and trials; Special Grade only through the ritual; Heavenly Restriction stats and removal; growth point limits; kill experience |
| Profile screen | client GameTest screenshot `0003_jjkcraft-m2-profile.png` | renders; trial text overflow fixed after review |
| Growth Tree screen | screenshot `0004_jjkcraft-m2-growth.png` | renders |
| Promotion ceremony and toast | screenshot `0005_jjkcraft-m2-promotion.png`; client asserts the ceremony is active and then ends | pass; camera-filling particles found and fixed |
| Grade badge | screenshot `0006_jjkcraft-m2-badge.png` | renders Grade 3 after promotion |
| Each grade by playing | not done by a human | each grade's data is loaded and used by the GameTests above |
| Heavenly Restriction feel (speed, damage) | not done by a human | attribute values verified by GameTest |

## M3 (0.4.0), 2026-10-17

| Check | How | Result |
|---|---|---|
| `./gradlew build` | automated | pass |
| Unit tests | automated | 40 of 40 pass (adds `TechniqueMathTest` 7, `TechniqueStateTest` 6) |
| Server GameTests | automated | 24 of 24 pass: adds all techniques load with valid grades; strike spends CE, damages and cools down; server refuses unknown, under-grade, unaffordable and Heavenly Restriction casts; chains bind; command halts; bolt flies and hits; barrier blocks 70% of frontal damage for CE and stops when lowered; healing channel converts CE to health; attract and repel collide into a burst; awakening rolls and slots an innate technique; mastery counts toward trials |
| Each technique category, visually | client GameTest screenshots `0007` to `0014` | hotbar, slash, bolt, chains, field, burst, wheel and loadout render. Review found and fixed: a flat slash (now diagonal), particles at the camera (now skipped), the field ring underground, and a stale innate technique left in slot 1 |
| Bolt trail color | screenshot | appeared in the default violet instead of the technique color in one frame; to check in the polish pass |
| Feel of casting (timing, shake, hit-stop) | not done by a human | |

## M4 (0.5.0), 2026-10-18

| Check | How | Result |
|---|---|---|
| `./gradlew build` | automated | pass |
| `./gradlew runDatagen` | automated | pass: 13 loot tables, haunted structure tag (39 structures), 25 item and 3 block models, English strings |
| Unit tests | automated | 44 of 44 pass (adds `FearMathTest` 4) |
| Server GameTests | automated | 42 of 42 pass, 18 new: every curse loads with a valid grade, mechanic, entity type, techniques and loot table; spawning applies data stats and starts the emergence; data resistances apply (fire does nothing to the Ash Oni); the Hymnal's song stuns; a hit to its mouth silences it for 1.5x damage; the Ledger tallies nearby deaths; the Stilt counts as watched only when looked at; the Echo copies a nearby cast but not one above its grade; the Salt Lantern mends an ally and a heavy blow snuffs it; Tidewash takes 25% more when dry; Thorn roots bind the marked target and ring the mark; the bell's wave hits a grounded target; the Saint is disguised until struck, then strikes double; the Oni's slam sets a target alight; the Gate never despawns, shows a boss bar and summons; a player kill advances the codex; sensing finds curses in range; fear is stored |
| Every curse, visually | client screenshots `0015` (Grade 4 and 3) and `0016` (Grade 2 and 1, with the Gate's boss bar) | all 12 render with their models, textures and name tags |
| Saint's disguise | screenshot `0017` | renders the hooded pilgrim model |
| Sensing | screenshot `0018`; the client test asserts three presences arrive and that turning sensing off clears them | outlines draw through a wall and around the GeckoLib Saint; radar and badge tag render |
| Codex | screenshot `0019`; the client test asserts the kill counters (3 Hymnals, 1 Saint) | unlocked entry with the 3D preview, lore, weaknesses and trophy; locked entries show `???` |
| Found and fixed in testing | GameTests, screenshots | goals were registered before the curse's brain existed (crash); the Cracked Bell's own melee knocked targets over its wave (it no longer attacks while ringing); codex previews had no entity id (crash); Grade 4's grey outline was hard to see on stone (outlines are now lightened) |
| Natural spawning, nests, fear over time | not observed in play | spawn weights are unit-tested; the spawner was disabled for the screenshots |
| Fighting each curse by hand (timing of telegraphs, difficulty) | not done by a human | |
| Body rotation when posed by `/tp` | screenshot `0016` | the Oni and the Gate face sideways in the posed lineup; staging only (no-AI mobs don't turn their bodies on teleport) |

## M5 (0.6.0), 2026-10-18

| Check | How | Result |
|---|---|---|
| `./gradlew build` | automated | pass |
| Unit tests | automated | 51 of 51 pass (adds `DomainMathTest` 7: variant costs, growth floor, pool against cost for Grade 1, Special Grade and barrierless, clash by pool, growth, priority and variant, ties) |
| Server GameTests | automated | 56 of 56 pass, 14 new: every domain loads; casting checks domain, grade, CE, one at a time, and pressing again closes it into burnout that blocks techniques; reaching Grade 1 grants a domain; sure-hit cuts through netherite; the barrier pulls a target back; barrierless costs 1.6x, reaches 1.4x and traps no one; upkeep running out closes it; a Simple Domain cancels sure-hit while an exposed curse is cut; the larger pool wins a clash (loser stunned, win counted); the flood overloads, stops regen and blocks casts, and regen returns after; garden hounds appear and melt away; the gamble rolls; arsenal blades work only inside and vanish; the recitation lands exactly seven times and closes |
| Domain sequence | screenshots `0020` (opening: title mid-reveal, dome growing, toast, meter) and `0021` (cutting field inside) | renders; the husk is visibly cut |
| Shadow garden, gamble | screenshots `0022`, `0023` | hounds, tendrils; meter shows hold time and three roll reels |
| Dome from outside, clash | screenshots `0024`, `0025`; the client test asserts the Special Grade player wins | dark dome reads clearly from 28 blocks; clash shards where the two domes meet |
| Information flood | screenshot `0026`; the client test asserts the player is Overloaded | view and HUD buried in glyph noise |
| Found and fixed in testing | GameTests, screenshots | a domain that won a clash while still opening never started its behaviors (it now takes hold on winning; active time counts from then); the title collided with the domain meter (moved down); the meter's hold text ran into the gamble reels (shortened); lanterns read as flat white squares (smaller, dimmer, with caps and strings) |
| Staging note | screenshot review | the Gravebound Gate raised its arena wall around the outside view (its mechanic working as designed); the client test turns mob griefing off for those shots |
| Feel of domains in play (timing, readability, lethality against players) | not done by a human | numbers are in BALANCE.md section 5 |
| Shader compatibility (Sodium, Iris, Vulkan) | not tested | domain visuals use only vanilla render types and HUD drawing |
| Multiplayer clash between two real players | not done | covered by a GameTest with two server players |

## M6 (0.7.0), 2026-10-18

| Check | How | Result |
|---|---|---|
| `./gradlew build` | automated | pass |
| Unit tests | automated | 60 of 60 pass (adds `BlackFlashMathTest` 4: swing timing bands, grade, Control and zone adding up to the cap, the multiplier range by CE, a Grade 4 Black Flash not one-shotting a Grade 2 curse; `VowMathTest` 5: points by weight and term with diminishing returns, where each focus sends the power, the cost floor, breaking against renouncing, the strongest vow staying under +75% damage) |
| Server GameTests | automated | 71 of 71 pass, 15 new: a forced Black Flash multiplies the strike, refunds CE and enters the zone; Reverse is refused below Semi-Grade 1, awakens on a lethal blow for a Semi-Grade 1 with CE (survive at 1 health), then heals itself for CE and halves domain burnout; the emergency save leaves 4 health for 40% of max CE and waits five minutes; vow power feeds techniques and their `vow_modifiers`; jumping breaks Grounded Oath (drain, debt); enforced vows refuse instead of breaking; Half Vessel halves the pool; Vow Kin ignores the unvowed and flees a broken vow; the Gravemind adapts to repeated damage; the Ferryman's crossing drags a target to its shore; the Warden can't die while seals stand; its Edict picks the weak point; the Sovereign opens its domain on its last mouths; the Unbound Ritual promotes to Special Grade. The M4 curse tests for Pale Stilt, Echo Mimic, Salt Lantern and Ash Oni were rewritten for the reworked mechanics (the oni test became two) |
| Black Flash | screenshot `0027`; the client test asserts the player is in the zone | red and black frame with a black lightning jag, clear around the crosshair |
| Vow Builder | screenshot `0028`; the client test clicks two vows, a focus and Swear by label, then asserts the vow is held | power, gains and the price preview match `VowMath` |
| Vow held, Reverse healing | screenshots `0029`, `0030`; the client test asserts the client knows Reverse is unlocked | held vow page; HUD tags VOW, RCT and ZONE in their own strip |
| Profile | screenshot `0031` | Binding Vow and Reverse sections |
| New curses, Saint | screenshots `0032` (all five, four boss bars), `0033` | distinct silhouettes; the Saint shows the test player's own skin and name |
| Found and fixed in testing | client test, screenshots | the client test could not click any mod button (ink buttons were not vanilla `Button`s; they are now); grade-badge tags covered the grade name (they now move to a strip below); profile columns overlapped (text now ends in an ellipsis, labels shortened); the domain meter sat on top of boss bars (top-centre elements now step below them); the Ferryman's bar title ran under the CE panel (shortened); the Black Flash frame washed the screen pink (deeper red, darker black, clear centre); Warden seals could not be placed in cramped arenas (closer fallback rings); the Ferryman only pulled players (now any non-curse) |
| Reworked M4 curses | GameTests above | M4's Pale Stilt, Echo Mimic, Salt Lantern, Tidewash, Thorn Matriarch, Redacted Saint, Ash Oni and Gravebound Gate did not match the brief's mechanics; M6 brought them in line. Doorframe Hunter, Gilded Mimic and Tithe Burrower are still missing (M8) |
| Reverse awakening chance and the lethal path with a real player | not done by a human | GameTests call the death handler directly because the test player is invulnerable |
| Feel of Black Flash timing, vow trade-offs and the special-grade fights | not done by a human | numbers are in BALANCE.md section 6 |

## M7 (0.8.0), 2026-10-18

| Check | How | Result |
|---|---|---|
| `./gradlew build` | automated | pass |
| Unit tests | automated | 68 of 68 pass (adds `CullingMathTest` 8: days, the entry window countdown, curse values, transfers in whole steps, death loss, both end conditions, the wall-and-dome shape, final experience and the colony ring) |
| Server GameTests | automated | 73 of 73 pass, 2 new. The Culling Game's state is one per server, so the whole loop is one ordered test: the game starts with the nine base rules; whoever is inside at the start is a resident; awakening opens an entry window; stepping inside makes a participant and closes it; the barrier pulls a participant back while a resident walks out; entrants are participants; a sorcerer kill is worth 5 and another 1, and points feed the Grade 1 trial; a transfer moves whole steps of 5 only; a reveal costs 1 and shows name and position, while outsiders see no names; the Tallykeeper takes its place; a base rule can't be bought, a rule is added for 100, a conflicting rule and a second purchase are refused; the Warden's Mandate sends a Warden that the rule binds; the Law of the Bow fixes its weak point; three idle days halve CE until the next score; the one-day mandate lapses; substitution moves a participant to another colony for 30; withdrawal needs its rule; a missed window takes the innate technique; saving and loading keep participants, rules, points and colonies; the end keeps standings and removes entrants. A second test loads every rule and checks there are nine base rules and that conflicts name real rules |
| Barrier, joining, Tallykeeper | screenshots `0034` (outside: a dark curtain with entrants faintly visible), `0035` (inside: entrants, the Tallykeeper, the HUD panel), `0040` (the Tallykeeper close up); the client test asserts the player became a participant by walking in | renders |
| Map, scoreboard, rules, negotiation | screenshots `0036` to `0039`; the client test selects a rule, clicks Propose, and asserts the rule is in force and 100 of 120 points were spent | other colonies' participants show as unknown |
| Found and fixed in testing | GameTests, screenshots | rules that lapse never lapsed when the game clock was young (expiry times went negative and collided with the "never" marker, now `Long.MAX_VALUE`); the fake test player can't be seen by the world scan at the start (a villager stands in for it); points did not reach a fake player's trial counter (participants are now found by id); the barrier drew each quad twice and stacked into a black wedge inside; the HUD panel sat under chat (moved below the grade badge) and its tag overlapped the rule count; the scoreboard's amount line collided with a button; the rule list printed a number with no text at the bottom |
| A full Culling Game played by people (19-day window, several days of entrants, multiplayer kills, the end) | not done | the loop is covered by the GameTest with the clock advanced by command |
| Balance of points, rule prices and entrant strength | not done by a human | numbers are in BALANCE.md section 7 |

## M8 (0.9.0), 2026-10-18

| Check | How | Result |
|---|---|---|
| `./gradlew build` | automated | pass |
| Unit tests | automated | 68 of 68 pass (no new pure maths in M8; the new behaviors are covered in game) |
| Server GameTests | automated | 97 of 97 pass, 24 new. Content: 30 techniques load with three ultimates and every category but Domain represented; cursed tool data names real items and techniques; the brief's 20 curses load. Techniques: Kennel Call brings two hounds (a second cast replaces them) that flank and bite the owner's target; the Paper Crane marks the target and dives; the Stone Tortoise swats an arrow aimed at its owner and draws a zombie's anger; Effigy Nail gives a bound effigy whose nail hurts the target for CE; Flesh Sculpt bulks the caster and shrinks a target (scale, armor); Stolen Frame does nothing during its windup, then freezes the enemy while the caster steps forward; Crimson Meridian cuts a far target; Thousand Lances hits the marked target; Hollow Star drags a zombie in, then bursts; Oathbound Edge cuts more than twice as deep on a heavy vow; Ink Step moves the caster; Splinter Volley fans five bolts; Ground Toll hurts and throws two zombies; Ember Skin burns a zombie that strikes the wearer; Kindred Mend heals the ally in sight. Tools: the Nodachi lends Hairline Sever at a quarter off only at Grade 1; the Cleaver moves CE on hit; the Withered Digit grants a technique and unsettles (max CE x0.7); the Drifting Gourd gives flight and lands its holder gently; scrolls teach only within grade; the Tally Ledger stores and returns points (inside the Culling Game loop). Curses: the Doorframe Hunter hides in a door frame, springs when the door opens, ambushes for 1.8x and is exposed away from doors; the Gilded Mimic waits as a chest, grabs, drags and lets go under a hard hit; the Tithe Burrower steals a stack, burrows, surfaces, and returns everything when paid; its stash spills on death |
| New curses, summons, ultimates, tools | screenshots `0041` to `0047` | the Mimic is a chest until it springs; hounds, tortoise and crane render; the volley's bolts carry the technique's colour (the "bolt trail colour" check); lances fall as columns on a marked ring; Hollow Star shows a dome, ring and inward pull; the weapons and objects read in the hotbar |
| Found and fixed in testing | GameTests, screenshots | the Mimic's drag used a one-off pull each tick and launched its victim (now a steady drag); the Hunter could not path into a closed door (it now walks up and steps in); Kindred Mend's ray passed over small allies (a narrow cone now); the palm cost test failed a few percent of the time when a Black Flash refunded CE (now allowed for); the Paper Crane's damped dive could circle its target without landing and failed one full run (the dive is now a straight stoop that lands or gives up; two runs passed after); the new behaviors sent zero-sized fields and bursts, and the beam trail drew nothing (sizes sent, beam drawn); bolts drew as squares (now round glows); Hollow Star's near-black colour vanished in additive light (brightened); summons spawned behind the camera in the test scene, and a test clear ran from world spawn instead of the player |
| Two ember auras striking each other | code review | retaliation answers only plain melee, so two auras can't trade burns forever |
| Crimson Meridian from first person | screenshot `0046` | seen end-on it reads as a flare along the aim; a side view was not captured |
| Feel and balance of the 15 techniques, the summons' AI in real fights, flight in survival, the Mimic and Burrower against real players | not done by a human | numbers are in BALANCE.md section 8 |

## M9 (1.0.0), 2026-10-18

| Check | How | Result |
|---|---|---|
| `./gradlew build` | automated | pass; produces `jjkcraft-1.0.0.jar` and the two sample pack zips |
| Unit tests | automated | 73 of 73 pass (adds `BalanceSimulationTest` 5: the ladder, the damage-per-CE ceiling, ultimates against their tier, one pool per same-grade curse, no one-shots within a grade) |
| Server GameTests | automated | 103 of 103 pass, 6 new: the crowded arena (performance), the sample pack decoding with the world's codecs, and four technique checks (Command: Kneel, Command: Rise, the seeking bolt curving onto a target off the line, Still Air Veil slowing an arrow) |
| Client GameTest | automated | pass, 48 screenshots (`0000` to `0047`) across M1 to M8, relog persistence and the dimension change |
| Balance pass | `BalanceSimulationTest` | found two ultimates over budget (Null Convergence 97 against a 90-health Grade 1 curse; Thousand Lances 129); both retuned (BALANCE.md section 9) |
| Server performance | crowded-arena GameTest, average tick over 200 ticks | 2.17 ms with 40 curses (AI on), 12 entrants and 4 summons, other tests sharing the server (2.2 to 3.8 ms across this milestone's runs); budget 50 ms |
| Client performance | client GameTest frame counter, frame caps lifted (no vsync, no frame limit, no idle limit) | 294 fps inside a colony barrier with entrants and the Tallykeeper; 508 fps in the second after casting Thousand Lances. One reading each, on one machine (NVIDIA GPU, driver 610.88, model not recorded; OpenGL; the test's default window): not a benchmark. Earlier runs read 30 fps in both scenes because vanilla caps a game at 30 fps after a minute without input, and the test sends none; the test now lifts that cap before measuring |
| Multiplayer hardening | code review, GameTests | every serverbound packet validated and rate limited; refusals change nothing; limiter state dropped on disconnect |
| Dedicated server | every GameTest run | the GameTest server is a dedicated server: common code loads no client classes |
| Sodium, Iris, Lithium, Jade | not run | not tested at M9; run afterwards, see "Compatibility runs" below |
| Two real clients on one server | not run | Fabric's client GameTests drive one client; multiplayer logic is covered with several simulated players on one server |

### The brief's manual checklist

The brief asks for these to be checked by hand. None has been played by a person yet; the
second column says what automated tests cover in the meantime.

| Item | Automated coverage | By hand |
|---|---|---|
| Each grade | promotion, trials, grade perks and the Unbound Ritual in GameTests; the ceremony in a screenshot | open |
| Each technique category | every one of the 30 techniques has a GameTest; screenshots of every category's visuals | open |
| Each curse | a mechanic GameTest for all 20; screenshots of all 20 | open |
| Each domain | 14 domain GameTests covering all six domains, clashes and Simple Domains; screenshots | open |
| Black Flash | forced Black Flash GameTest; the math tests; a screenshot | open |
| Overexertion | energy GameTests and the client test asserts the overexerted state | open |
| Death and respawn | death policy unit tests; Reverse Technique's death handler in GameTests | open |
| Dimension change | client test: CE is kept through a dimension change | open |
| Relog persistence | client test: CE is kept through a save and reopen | open |
| A multiplayer session with two or more players | simulated players only (transfers, reveals, kills between participants) | open |
| A complete Culling Game loop | one ordered GameTest plays the whole loop with the clock advanced by command; the client test joins and negotiates | open |

### Fixes found by the final runs

- **Seeking Grudge Bolt hardly sought.** It began steering only after about four blocks and
  turned gently, so it missed anything a block or two off the line within eight blocks: the new
  GameTest never saw it hit. A simulation of the steering showed why. It now steers from its second
  tick at strength 0.25 (was 0.18); the simulation has it hitting every offset tried, from 1 block
  to the side at 4 ahead to 5 to the side at 12 ahead.
- **Curses reacted to blows they were immune to.** A curse's mechanic saw damage before the
  vanilla invulnerability check, so a blow the curse ignored could still reveal a hidden
  Doorframe Hunter or trigger other on-hurt effects. The check now comes first. The Doorframe
  Hunter test had failed once, most likely because a stray from a neighbouring test (the seeking
  bolt that missed is a candidate) kept restarting its calm count; it now keeps the Hunter
  invulnerable (the damage step calls the mechanic directly).

## Compatibility runs, 2026-10-18

Jujutsu Craft 1.0.0 run with Sodium, Iris, Lithium and Jade, on the test rig above (the client
test's default 854x480 window). Nobody played; everything below comes from the GameTests, their
screenshots and the logs.

### What was run, and how

The four jars are the current 26.3 Fabric releases on Modrinth, checked on the day: Sodium
mc26.3-0.9.2 (a newer 0.9.3-beta.1 exists; the release was used), Iris 1.11.7+26.3, Lithium
mc26.3-0.26.2, Jade 26.3.5+fabric. For the shader pack run: Complementary Reimagined r5.9.3
(lists 26.3). Each was downloaded from Modrinth with the owner's approval and checked against
Modrinth's SHA-512. They sit in `compat/mods/` and `compat/shaderpacks/` (git-ignored) and are
added to development runs only with `-PwithCompat` (README, "Compatibility runs"). The normal
build does not see them.

| Run | Mods | Shader pack | Client GameTest | Screenshots | fps in a colony | fps during Thousand Lances |
|---|---|---|---|---|---|---|
| Baseline | none | none | pass | 48 | 307 | 609 |
| Baseline, again (noise) | none | none | pass | 48 | 313 | 551 |
| All four, before fixes | Sodium, Iris, Lithium, Jade | off | pass | 48 | 323 | 569 |
| All four, shaders, before fixes | Sodium, Iris, Lithium, Jade | Complementary Reimagined r5.9.3 | pass | 48 | 247 | 385 |
| All four, shaders, after fixes | Sodium, Iris, Lithium, Jade | Complementary Reimagined r5.9.3 | pass | 50 | 245 | 400 |
| All four, after fixes | Sodium, Iris, Lithium, Jade | off | pass | 50 | 306 | 601 |
| Default build, after fixes (`./gradlew build runClientGameTest`) | none | none | pass | 48 | 323 | 624 |

The two extra screenshots in runs with Jade are Jade close-ups of the disguised curses (below).
Frame rates are logged the way M9 does: the test lifts vanilla's frame caps (vsync, frame limit,
idle limit) and reads the frame counter once per scene. One reading per scene per run; the two
identical baseline runs differed by 2% and 10%, so differences of that size mean nothing. Without
a shader pack the mods made no measurable difference in these small scenes. With the shader pack,
about 20% fewer frames in the colony and about 35% fewer during Thousand Lances.

Server GameTests: `./gradlew runGameTest -PwithCompat` loads Lithium and Jade on the GameTest
server (Sodium and Iris are client-only, so the loader skips them there): 103 of 103 pass; the
crowded arena averaged 2.77 ms a tick (3.16 ms in the default build's run the same evening).

Screenshot comparison: each shot was compared with the same shot from the baseline (mean pixel
difference and the share of pixels that changed visibly), and that was set against the
difference between the two baseline runs, which differ by themselves: the innate technique is
rolled at random, animations and particles are caught at different moments, and colony positions
are random. Every shot was also looked at side by side, with crops of the world effects, the HUD
and the curse models. Contact sheets, crops, per-shot tables and the scripts are kept with the
run outputs in `build/compat-runs/` (not committed).

### Results

- **World effects without a shader pack (Sodium, Iris with shaders off):** domain domes inside and
  out, the clash, the information flood, colony barriers inside and out, slashes, bolts, chains,
  fields and bursts, the Black Flash frame and jag, the ultimates (Hollow Star, Thousand Lances,
  Crimson Meridian) and the sensing outlines all look as they do without the mods. No shot
  differed from the baseline by more than the baseline differs from itself, except where Jade
  drew its tooltip and the startup shot (the Culling Game panel is synced every 10 ticks and the
  shot is taken 5 ticks after the reset; the next shot shows it gone).
- **HUD layers:** unchanged with all four mods, apart from Jade's tooltip (below). Screens (profile,
  growth tree, wheel, loadout, codex, vows, Culling map, board, rules, negotiation) unchanged.
- **GeckoLib curse models:** all 20 curses, the shikigami, entrants and the Tallykeeper render in
  every run, shader pack included.
- **Problem 1, Iris with a shader pack: domes and barriers not drawn.** Every domain dome (from
  inside and outside, the clash) and the colony barrier wall were missing; Iris logged "Missing
  program minecraft:pipeline/debug_quads in override list". A pack draws a pipeline only through
  a program Iris assigned it, and Iris has none for `debug_quads`. Effects drawn with the
  lightning type, HUD layers and models were fine. **Fixed:** domes and barriers use the mod's own
  copy of that pipeline (`JjkRenderTypes.dome()`; without shaders it looks the same, checked
  against the baseline), and with Iris installed `IrisCompat` assigns it the pack's basic program
  through Iris's public API. After the fix Iris logs "Found okay program match for
  jjkcraft:pipeline/dome: BASIC_COLOR" and every dome and the barrier show under the pack, in
  their colours.
- **Problem 2, Jade: tooltip over the domain meter.** Both sit at the top centre; whenever the
  player looked at something in reach, Jade's box covered the meter (Black Flash, the Saint shots,
  sensing). **Fixed:** a Jade plugin moves Jade's tooltip clear of the mod's HUD elements, the way
  Jade moves it below boss bars. After the fix it sits just below the meter; a short tooltip that
  fits above it stays there.
- **Problem 3, Jade: disguises given away.** The disguised Redacted Saint showed under the
  player's name with its own 90 health and "Jujutsu Craft" as the mod; a Gilded Mimic would show
  as itself instead of a chest. **Fixed:** Jade now sees the player whose face the Saint wears (that
  player's health, "Minecraft") and a chest for the Mimic; a faceless Saint shows nothing; a sensed
  curse shows as itself, as sensing already sees through disguises in the world. The Doorframe
  Hunter and a burrowed Tithe Burrower were already hidden: they are invisible and Jade skips
  invisible entities. The default client test now checks what an overlay is told (Saint, Saint
  while sensed, Mimic), so this is covered without Jade too; with Jade present it takes two
  close-ups (`compat-jade-redacted_saint`, `compat-jade-gilded_mimic`).
- **Logged, not a problem found in the mod:** with the shader pack, Iris logs "Missing program"
  errors for vanilla's entity outline pass, which sensing uses; the outlines still showed in the
  sensing shot. The pack logs a warning about a stone slab block-state name. Sodium applied an
  NVIDIA driver workaround and noticed RivaTuner, both about this machine. No other errors from
  any of the mods.

### Not checked

- Each mod on its own: the client runs had all four together, the server run Lithium and Jade
  together. The problems found each belong clearly to one mod.
- Any shader pack other than Complementary Reimagined r5.9.3. Packs differ in how their basic
  program treats translucent geometry. The domes get no shadow-pass program, so they cast no
  shadows under shaders (intended, not inspected). The shader run after the fixes still had a
  test bug that left the Saint standing beside the Mimic close-up (fixed and checked in the
  no-shader run after it, not repeated with shaders).
- Vanilla's "Improved Transparency" option (order-independent transparency), with or without the
  mods.
- Jade beyond its default settings: other tooltip positions and scales, other HUD layouts, and
  what it shows for the mod's other entities (only seen in passing: shikigami, entrants, the
  Tallykeeper).
- A dedicated server with real players, Jade on both sides, or two real clients.
- Performance as a benchmark: one reading per scene per run.
- Playing by hand with any of these mods.

## Phase 0: audit and canon naming, 2026-10-18

Names only; no behavior was meant to change. The checks below show that it did not, and that
worlds and data packs from before the rename still load.

| Check | How | Result |
|---|---|---|
| Baseline before any change | `./gradlew build` | 78 unit tests and 103 of 103 server GameTests pass |
| Rename applied by script, then compiled | `compileJava`, `compileClientJava`, `compileTestJava`, `compileGametestJava`, `compileDatagenJava` | all compile. The rename touched 94 files and moved 88 (`docs/AUDIT.md` section 3) |
| Lang and models regenerated | `./gradlew runDatagen` | 72 keys renamed, 12 strings reworded; no stale generated file left (checked against datagen's cache) |
| No old names left | text search of `src/`, `samples/` and `tools/` for every old name | only the alias table, the tests that read old ids on purpose, and unrelated words (a curse's "breath stolen", a Gate's "reshape arena") |
| Unit tests | `./gradlew build` | 82 pass. New: `LegacyIdsTest` (an old player save loads with the new technique and domain ids; every renamed data file exists and no old one remains; no id is both old and new) |
| Server GameTests | `./gradlew build` | 106 of 106 pass. New, `LegacyIdGameTests`: old item, entity, effect, item component and behavior type ids resolve to the renamed entries; an old saved spear and an old scroll load; a data pack technique with an old behavior type decodes; every renamed technique is loaded. The shipped data reads no old id (no "Old id" warning except from the test's own scroll) |
| Client GameTest | `./gradlew runClientGameTest` | passes, 48 screenshots. Two screenshots were renamed with their techniques (`0044_jjkcraft-m8-maximum-output-blue`, `0046_jjkcraft-m8-piercing-blood`; `0040` is now `jjkcraft-m7-kogane`). Looked at: the wheel, loadout, domain meter, domain toast and title, and the shikigami. The canon names fit, including the longest domain title, Chimera Shadow Garden, at the test's 854x480 window |
| Whitelist default | read in the 26.3 server code | `white-list` defaults to true; documented in the README |

Not checked: the renamed names in a real world saved by 1.0.0 (the tests build old saves and
stacks from JSON instead); translations other than English (there are none); shikigami art
(unchanged, still the old crane and tortoise shapes, as `docs/NAMING.md` says).


## 1.2.0-preview validation, 2026-10-19

Latest baseline unit run: 102 tests, zero failures. Expanded server run: all 130 required
GameTests passed. New integration coverage includes one-shot Inherit/Random awakening,
forged/repeated selection, grade unlocks, forty registered curses with valid combat
moves, startup frames, guard height/parry, soul/item codecs, placed reliquaries and
one-time Vessel revival. Stress evidence is in docs/PERF.md.

The initial real-client run produced 56 scene screenshots and reached its final relog
assertion. That assertion failed (43.3125 CE against an allowed 33-40) because regeneration
continued during world loading. The test now pauses regeneration during save/reload and
requires exactly 33 CE, restoring the setting in finally. Its focused rerun result is
recorded below when complete. The 56 captured scenes are retained locally under
build/reports/client-preview-scenes. Loadout and curse screenshots were visually inspected.
This is not a claim that every individual added curse or new pact path was visually
reviewed, or that the complete revised client suite passed in one uninterrupted run.

Data generation is run separately before packaging to avoid scheduling sourcesJar ahead
of runDatagen's generated-resource output. Validation found the Vessel required an
explicit no-loot-table registration. The new object item/block model providers and
spawn-egg textures are included. Final results follow below.

### Final checkpoint

runDatagen: passed (strict resource/data providers). Final `test assemble runGameTest
runClientGameTest -PclientTestFocus=relog`: BUILD SUCCESSFUL. All 131 required server
GameTests passed in 43.66 seconds, all 102 unit tests passed, and the focused real-client
save/reload check passed with regeneration paused. The added domain regression confirms
that new Limitless characters wait for Unlimited Void's grade, while the legacy test
continues to grant a Grade 1 domain. The earlier 56-scene client run plus the focused
persistence rerun are recorded separately; the revised full client suite was not rerun.

Final crowded scene: 40 curses, 12 entrants, 4 summons, 200 ticks, mean 10.33 ms/tick.
Jar validation found no missing curse textures, spawn-egg item definitions, models or
textures among all forty definitions. Output: build/libs/jjkcraft-1.2.0-preview.jar.
Runtime launch log: build/play-client.log. Changes remain local and have not been pushed.

## 1.3.0-preview final validation, 2026-10-19

Strict runDatagen passed. All 140 required server GameTests passed in the final run
(42.76 s of test execution). The crowded scene averaged 18.95 ms/tick with forty curses,
twelve NPC entrants and four summons over 200 ticks. Unit tests passed, including old
six-slot save normalization without deleting learned moves.

The focused real-client revision run passed normal respawning at a configured vanilla
spawn point with Blood Manipulation retained, the four-slot/family loadout, the Traits
screen, cursed-spirit rebirth/rendering, and exorcism into a different innate family.
Six screenshots are in build/run/clientGameTest/screenshots. The loadout, death menu
and spirit form were visually inspected. A render failure found by that test was fixed
by retaining AvatarRenderState and redirecting only submitted geometry. The capture
fixture was corrected to clear solid terrain outside the tiny empty test template;
its aimed absorb/release assertions now pass.

Additional integration coverage: spirit/deal choices in both mode policies, fallen-host
consumption and duplicate rejection, host health/body persistence, Six Eyes outside
technique slots, complete family references, captured-curse ownership and explicit
colony entry with personal Kogane negotiation. Full long-running Hardcore worlds,
four-player play, every technique's balance and new renderer/backend combinations
remain manual checks. The full older 56-scene suite was not rerun for this expansion.

Final output: build/libs/jjkcraft-1.3.0-preview.jar. Launch log: build/play-client.log.
Source changes remain local, without a GitHub push.

## 1.4.0-preview personalization and animation, 2026-10-19

`test assemble runGameTest runClientGameTest -PclientTestFocus=personalization` passed:
107 unit tests and all 147 required server GameTests. The mixed stress scene averaged
18.71 ms/tick for 40 curses, 12 entrants and four summons over 200 ticks.

New server coverage checks every registered technique's default/tuned behavior, bounded
finite costs, unknown-move rejection, player isolation, actual customized teleport
distance/CE/cooldown, birth-reserve persistence across death/respec, XP-funded universal
learning, zero-CE restrictions, RCT output and Infinity filter pricing.

The focused real client exercised workshop synchronization, reserve/skill UI, paid
reinforcement activation, six combat poses with assertions on changed model limbs,
first-person rendering, and saved tuning/birth reserves across world reopening.
Screenshots were inspected and panel opacity/layout problems corrected. Casting gestures
are also checked for field techniques, which use a different VFX phase.

Logs: `build/personalization-release-check.log`, `build/personalization-gesture-check.log`.
The previous long scene suite was not rerun. Human PvP balance and third-party player-model
compatibility remain manual checks. Release jar: `build/libs/jjkcraft-1.4.0-preview.jar`.
