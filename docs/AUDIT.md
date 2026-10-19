# Audit (Phase 0, 2026-10-18)

The state of Jujutsu Craft at the start of the eight-phase plan (Phase 0 to Phase 7), before the
canon naming pass. The pass itself, with every rename, its wiki source and the migration, is in
`docs/NAMING.md`.

## 1. Versions

Checked on 2026-10-18 against meta.fabricmc.net, maven.fabricmc.net and Modrinth's API. Nothing
changed since the M0 check on 2026-10-17, so nothing was re-pinned.

| Component | Pinned | Latest on 2026-10-18 |
|---|---|---|
| Minecraft Java | 26.3 | 26.3 is the newest stable release. 26.4 has not shipped (26.4-snapshot-4 is the newest build) |
| Java | 25 | |
| Gradle | 9.8.1 | |
| Fabric Loom | 1.19.1 | 1.19.1 |
| Fabric Loader | 0.19.5 | 0.19.5 stable (0.20.0 is beta) |
| Fabric API | 0.163.0+26.3 | 0.163.0+26.3 (0.163.0+26.4 exists for the snapshots) |
| GeckoLib | 5.5.7 | 5.5.7 (2026-09-22) |
| Cloth Config | 26.3.159 | 26.3.159 |
| Mod Menu (optional) | 21.0.0 | 21.0.0 |
| Jade, Iris (compile only) | 26.3.5+fabric, 1.11.7+26.3 | as of the compatibility run on 2026-10-18 |

Hooks the brief asks to confirm before later phases depend on them (Fabric API 0.163.0+26.3):

- Death: `ServerLivingEntityEvents.ALLOW_DEATH` and `AFTER_DEATH` exist (Phase 6).
- Damage: `ALLOW_DAMAGE` (cancel) and `AFTER_DAMAGE` exist; there is no event that changes the
  amount, which is why `LivingEntityMixin` exists (`docs/MIXINS.md`).
- Jump: no event; `ServerPlayerMixin`.
- Attack and input: to be checked at the start of Phase 4.
- Hardcore flag accessor: to be checked at the start of Phase 6.
- Whitelist: in 26.3 `white-list` defaults to `true` (`DedicatedServerProperties`, checked in
  the 26.3 server code). The GameTest server takes no logins, so tests are unaffected. A
  dedicated test server (`runServer`) needs `white-list=false` in `run/server.properties` or the
  test accounts added (README).

## 2. Milestones

M0 to M9 are complete, and none is broken: every automated test passes (section 4). The gaps
are the ones the README already lists:

- Nobody has played the mod by hand. Feel, balance and difficulty come from tests and numbers;
  the manual checklist in `docs/TEST_REPORT.md` is still open.
- Multiplayer was tested with simulated players on one server, never with two real clients.
- Art is generated programmer art.
- No alternative or experimental renderer (Vulkan) was tested.
- Changing content data needs a world restart; data-only curses must reuse a curse entity type.
- M10 (Modulo) was not built; it is Phase 1 of this plan.

Parts of the current design that later phases replace or change, noted here so nothing is
replaced by accident:

| Now | Replaced or changed in |
|---|---|
| No post shaders and no sky or fog hooks (`docs/VFX_SPEC.md`) | Phase 2, with post effects only while no shader pack is in use |
| Vanilla melee plus the Ink-Iron Palm's Black Flash roll | Phase 4 (Cursed Combat) |
| Awakening rolls an innate technique on first join (2% Heavenly Restriction) | Phase 5 (path choice, clans) |
| Death policy `KEEP`, `PARTIAL` or `RESET_TECHNIQUES` | Phase 6 (two rulesets, cursed objects, pacts) |
| The `stagnation` rule halves cursed energy; canon removes the technique | Phase 6 review |
| One Kogane per colony; canon gives one to each player | Phase 6 (Culling Game integration) |

## 3. Non-canon names for JJK-based powers

Everything below used an original name for something Jujutsu Kaisen already names. All of it was
renamed in Phase 0; `docs/NAMING.md` has each old name, new name and source.

| Kind | Count | Where |
|---|---|---|
| Techniques | 19 | `data/jjkcraft/jjkcraft/technique/`: the attract and repel fields and their merge, the arc and line cuts, the crimson line, the straw-doll link, three shikigami summons, three commands, shape-altering, the snap-speed window, two healings, the projectile-slowing veil, the special grade singularity |
| Domains | 5 | `data/jjkcraft/jjkcraft/domain/`: the cutting field, the information flood, the shadow garden, the gamble, the arsenal |
| Entities | 5 | the three shikigami, the shadow garden's hounds, the Culling Game's interface (Tallykeeper) |
| Items and cursed tool data | 4 items, 3 tool files | the straw doll, and three tools named after canon cursed tools (Inverted Spear of Heaven, Split Soul Katana, Chain of a Thousand Miles) |
| Effect | 1 | the Projection Sorcery freeze ("Lost Moment") |
| Culling Game rule | 1 | the rule-negotiation rule ("The Tallykeeper") |
| Terms | 1 | the Withered Digit was called a "cursed vessel"; it is a cursed object, and "vessel" is reserved for the Phase 6 role |
| Java classes | 14 | the shikigami and shadow hound entities, the Tallykeeper entity and renderer, the shadow hound renderer, the straw doll item and link, five technique behaviors (`EffigyLinkBehavior`, `TimeSkipBehavior`, `ReshapeBehavior`, `ProjectileVeilBehavior`, `CommandBehavior`) |
| Technique behavior type ids | 5 | `effigy_link`, `time_skip`, `reshape`, `projectile_veil`, `command` |
| Lang keys | 72 renamed, 12 reworded | technique, domain, entity, item, effect, culling rule, straw doll, cursed object and Culling Game keys |
| Textures and models | 28 textures, 10 GeckoLib files, 8 generated item files | technique icons, entity and item textures, shikigami and Kogane models and animations |
| Art scripts | 4 | `tools/gen_textures.py`, `gen_curses.py`, `gen_items.py`, `gen_culling.py` |

Items of the brief's mapping that have **no counterpart in the mod yet**, so there was nothing to
rename. They are new features for later phases:

- Blood Manipulation as a bleed or immobilize (canon Crimson Binding). The mod's only blood
  technique is Piercing Blood, renamed from the crimson line.
- Copy as a technique of its own. Copying exists only inside Authentic Mutual Love.
- Ratio Technique (the 7:3 weak point): Phase 4.
- Six Eyes and the clans: Phase 5.
- Mahoraga and the other Ten Shadows shikigami (Great Serpent, Max Elephant, Round Deer, Piercing
  Ox, Tiger Funeral, Rabbit Escape): Phase 1 (Mahoraga) and later.
- The domains Coffin of the Iron Mountain, Horizon of the Captivating Skandha, Deadly Sentencing,
  Self-Embodiment of Perfection, the Smallpox Deity's and Dabura's, and the canon bosses.

On the brief's line "Ultimates become Hollow Purple, Malevolent Shrine, or Unlimited Void": the
mod's three ultimate techniques are a crimson line, a rain of lances and a special grade
singularity. Hollow Purple already exists as the merge of Blue and Red (renamed from Null
Convergence), and Malevolent Shrine and Unlimited Void are domains. So the line became Piercing
Blood, the singularity became Maximum Output: Blue (its canon counterpart), and the rain of
lances, which has none, kept the name Thousand Lances.

## 4. Tests

| | Before Phase 0 | After Phase 0 |
|---|---|---|
| Unit tests | 78, all pass | 82, all pass (adds `LegacyIdsTest`) |
| Server GameTests | 103 of 103 pass | 106 of 106 pass (adds `LegacyIdGameTests`) |
| Client GameTest | passes, 48 screenshots (the 2026-10-18 compatibility run) | passes, 48 screenshots |

No test was failing at the start. The rename found no failing behavior either. The only fixes were
compile errors the rename itself caused, found and fixed in the same pass.

## 5. Found later (Phase 1, 2026-10-19)

A bug the Phase 0 audit missed, because no test hit two blows on one target in the same instant:
since the port to 26.3, the hit cooldown is `LivingEntity.damageCooldownTime`, and
`Entity.invulnerableTime` is a separate, full invulnerability. The mod still reset
`invulnerableTime` to let a second hit through, which did nothing. Black Flash right after
another hit, a domain's sure-hit on a recently hit target, and the armor-piercing half of a split
hit all dealt less than they should. Fixed in 1.1.0, with a GameTest
(`SorceryGameTests.aBlackFlashRightAfterAnotherHitStillLandsInFull`). The dash's brief
invulnerability does want `invulnerableTime` and was left alone.
