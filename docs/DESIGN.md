# Jujutsu Craft: Design

Mod id `jjkcraft`. Fabric only. Target: Minecraft Java 26.3.

This document holds the architecture, the key decisions and why they were made, the risks, and
every assumption that has not been verified yet. It is updated at each milestone. When a
statement here has been checked against the game or the docs, it is marked **[verified]**.

## 1. Verified toolchain (M0, checked 2026-10-17; checked again 2026-10-18, unchanged)

| Component | Version | Source |
|---|---|---|
| Minecraft Java | 26.3 (latest stable; 26.4 is in snapshots) | meta.fabricmc.net/v2/versions/game |
| Fabric Loader | 0.19.5 (latest stable; 0.20.0 is beta) | meta.fabricmc.net/v2/versions/loader |
| Fabric API | 0.163.0+26.3 | maven.fabricmc.net, fabric-api maven-metadata |
| Fabric Loom | 1.19.1, plugin id `net.fabricmc.fabric-loom` | maven.fabricmc.net plugin marker |
| Gradle | 9.8.1 (Loom 1.19.1 requires at least 9.8.0; the example mod's 9.7.1 wrapper fails) | services.gradle.org, build error |
| Java | 25 (toolchain and `--release 25`) | example mod and Fabric docs |
| GeckoLib | 5.5.7, `com.geckolib:geckolib-fabric-26.3` | GeckoLib Cloudsmith maven, Modrinth |
| Cloth Config | 26.3.159 (`me.shedaniel.cloth:cloth-config-fabric`) | maven.shedaniel.me, Modrinth |
| Mod Menu | 21.0.0 (development runtime only) | maven.terraformersmc.com, Modrinth |
| owo-lib | **not available for 26.3** (newest build targets 26.2) | Modrinth |

Compatibility targets with 26.3 Fabric builds on Modrinth: Sodium mc26.3-0.9.2 (release),
Iris 1.11.7+26.3, Lithium mc26.3-0.26.2, Jade 26.3.5+fabric.

Facts about the 26.x toolchain, checked against the official example mod and the
FabricMC/fabric-docs reference mod:
- The game ships unobfuscated. Loom's `net.fabricmc.fabric-loom` plugin uses official names
  without remapping. Dependencies use plain `implementation`, not `modImplementation`. **[verified]**
- `fabricApi { configureDataGeneration { createSourceSet; modId; client } }` and
  `fabricApi { configureTests { createSourceSet; modId; enableGameTests; enableClientGameTests; eula } }`
  exist in Loom. **[verified in docs]**
- Server GameTests run as part of `build`. Client GameTests run with `runClientGameTest`.
  **[verified in docs]**
- The data attachment API (`AttachmentRegistry`, `AttachmentSyncPredicate`, `persistent`,
  `copyOnDeath`, `syncWith`) and `DynamicRegistries.registerSynced` exist in the 26.x docs.
  **[verified in docs; compile-checked at M1]**
- The Fabric docs reference mod targets 26.2. Some of its code no longer matches 26.3, so it is a
  starting point, not proof. Differences found so far:
  - Client GameTest: `TestSingleplayerContext.getClientLevel()` is gone. Chunk waits are on
    `getConnection().waitForChunksRender()`. **[verified by compile]**
  - Datagen in its own source set writes under the datagen mod's id unless the entrypoint
    overrides `DataGeneratorEntrypoint.getEffectiveModId()`. This project overrides it to return
    `jjkcraft`. **[verified by running datagen]**
- The HUD API is `HudElementRegistry`, and HUD callbacks receive a `GuiGraphicsExtractor`, not
  the older `GuiGraphics`. Payload registration is
  `PayloadTypeRegistry.clientboundPlay()` and `serverboundPlay()`. **[seen in docs; compile-checked at M1]**
- Server GameTests run without an accepted EULA. Fabric writes a JUnit XML report when
  `-Dfabric-api.gametest.report-file` is set, and the build sets it to
  `build/reports/gametest/server.xml`. **[verified]**

## 2. Project layout

The brief asks for four parts: common, client, datagen and gametest. Loom supports these as
**source sets of one Gradle project**, not as Gradle subprojects. Separate subprojects would
produce separate jars, which would then need jar-in-jar packaging, duplicate run configurations,
and manual classpath wiring for datagen and tests. The source set route is the one the Fabric
example mod and Fabric docs use, so this project uses it.

| Brief name | Source set | Directory | Shipped in the mod jar |
|---|---|---|---|
| jjkcraft-common | `main` | `src/main` | yes |
| jjkcraft-client | `client` | `src/client` | yes (client environment only) |
| jjkcraft-datagen | `datagen` | `src/datagen` | no; its output goes to `src/main/generated` |
| jjkcraft-gametest | `gametest` | `src/gametest` | no |
| (unit tests) | `test` | `src/test` | no |

`splitEnvironmentSourceSets()` makes Loom fail the build if common code references a
client-only class. This enforces the common/client boundary at compile time.

Java packages:

```
com.jjkcraft                 entrypoint, ids, logging
com.jjkcraft.core            pure logic with no Minecraft imports: CE math, grade ladder,
                             damage formulas, clash resolution, rule engine, points.
                             Unit-tested without booting the game.
com.jjkcraft.registry        static registries (behaviors), dynamic registries (content)
com.jjkcraft.energy          cursed energy attachment, regen, overexertion
com.jjkcraft.grade           grade data, promotion
com.jjkcraft.technique       technique data, cast pipeline, behaviors
com.jjkcraft.domain          domain data, server domain manager, clash
com.jjkcraft.curse           curse entities, CurseBrain, fear field, nests
com.jjkcraft.culling         Culling Game state, colonies, rule engine bindings
com.jjkcraft.item            cursed tools and objects
com.jjkcraft.network         payload records and server handlers
com.jjkcraft.config          server config
com.jjkcraft.client.*        HUD, screens, UI kit, VFX, keybinds, client sync caches
```

## 3. Key decisions

### 3.1 Content as data, behavior as code

Each kind of content (technique, curse, domain, grade, Culling Game rule, cursed tool) is a
**synced dynamic registry** registered with `DynamicRegistries.registerSynced` and a `Codec`.

- Our content lives at `data/jjkcraft/jjkcraft/<kind>/<name>.json`. A datapack adds content at
  `data/<its namespace>/jjkcraft/<kind>/<name>.json`. This path rule comes from vanilla: entries
  of a modded dynamic registry live under a folder named after the registry's own namespace.
- Codecs validate at load. A broken file is a load error that names the file. Extra
  cross-reference checks run after load (for example, a technique id that a curse references
  must exist) and log every problem.
- Synced registries reach the client automatically. Tooltips, the codex and the wheel can read
  the same data as the server.
- **Limitation:** dynamic registries load when the world loads. `/reload` does not refresh them.
  A changed datapack needs a world restart. This is vanilla behavior and is documented.

Code exposes a small **static** registry of behaviors for each kind: `technique_behavior`,
`domain_behavior`, `rule_effect`, `rule_trigger` and `curse_behavior_profile`. Each behavior
declares its own parameter codec. Data entries pick a behavior by id and give its parameters, and
the parameters are validated by that behavior's codec. A content pack can combine existing
behaviors with new numbers, names and assets without code. It cannot invent a new behavior
without code. That is the intended boundary.

### 3.2 Server authority

The server owns CE, damage, casts, cooldowns, domains, clashes, Culling Game state, points and
rules. The client sends **intents** only ("cast slot 3 at this look direction", "open
negotiation"). The server checks each intent for range, cooldown, CE, grade, state and rate (a
token bucket per player), and silently drops requests that fail. The client may start a cast
animation or particle early, but CE and damage change only when the server confirms.

### 3.3 Entity state: data attachments

- `jjkcraft:sorcerer`: a persistent attachment on players. It holds CE, grade progress,
  techniques, vows, the Heavenly Restriction flag, mastery and domain progress, and is
  serialized with a `Codec`. On death, the configured death policy is applied when the old
  player's data is copied to the new one: `KEEP` (the default), `PARTIAL` or `RESET_TECHNIQUES`.
- `jjkcraft:energy`: the CE pool on players and cursed-capable mobs (the same attachment).
  Mob capacity comes from `energy_profile` data, or from the entity itself for curses.
- Client sync uses small dedicated payloads, not a full-state sync. **As built (M1):** CE is
  stored lazily (value, last update tick, overexertion end, last action tick), so the client can
  run the same regen function as the server. The server sends CE only when it changes (coalesced
  to one packet per player per tick) and when max CE or the regen modifier drifts (checked once
  a second). The client predicts between packets and eases the bar. Slowly changing values
  (grade, loadout, vows) are sent when they change.

### 3.4 World state

- Domains are runtime objects held by a per-level `DomainManager` (tick, membership, clash).
  They are **not** saved. A domain collapses when the server stops or its chunk unloads. Domains
  are always regions in the current level and never a separate dimension, as the brief
  requires.
- Culling Game state (colonies, participants, points, deadlines, rules in force, the log and
  final standings) is one global `SavedData` (`SavedDataType` on the server's data storage,
  confirmed in 26.3), saved to `data/jjkcraft/culling_game.dat`.
- The fear field is a coarse grid, one value per chunk section, held in a level attachment and
  updated lazily.

### 3.5 Networking

Fabric Networking API v1 with `CustomPacketPayload` records, `StreamCodec`s and
`PayloadTypeRegistry`. No v0 modules. All payloads live in `com.jjkcraft.network` and are
registered once in the common entrypoint.

### 3.6 Configuration

- Persistence: Cloth Config AutoConfig with a JSON serializer. It supports 26.3 and validates
  bounds with annotations.
- `jjkcraft-server.json` holds system toggles (Culling Game, domains, curse spawns, Modulo),
  spawn rates, the death policy and point values. The server owns it, and the values clients
  need are sent to them on join.
- `jjkcraft-client.json` holds the VFX level, the shader toggle, HUD layout, accessibility and
  audio settings. It exists only on the client.
- The settings screen is Cloth's generated screen, opened from Mod Menu. Every other screen uses
  the mod's own UI kit (owo-lib has no 26.3 build).

### 3.7 Rendering and VFX

- HUD elements use Fabric's HUD element API. Its exact 26.3 shape is checked at M1.
- Ribbon trails, glyph rings and the barrier shell are drawn as vertex geometry through the
  vanilla render pipeline, not as particles.
- Screen effects (Black Flash frame, vignette, domain distortion) prefer full-screen passes that
  work without a custom post chain: GUI-layer overlays, fog and sky color hooks. A real post
  shader is used only where an overlay cannot do the job, behind the "custom shaders" toggle.
  This keeps Iris and Sodium compatibility simple.
- Hit-stop and slow motion are client-side and visual only. They scale the partial tick used for
  animation and render interpolation of the affected entities. The server never slows down.
- Every effect has a budget (particles per frame, overlay update rate) and a LOD by distance.
  The "Low VFX" preset lowers all of them.

### 3.8 Curse mobs

GeckoLib 5.5.7 handles models and animations. A shared `CurseBrain` state machine (idle, hunt,
cast, recover, flee) runs on top of vanilla goals. Curse data (grade, CE, stats, techniques,
resistances, loot, model and animation keys, mechanic and its numbers) comes from the `curse`
registry. Each curse has an entity type registered in code, because entity types cannot be
data-driven. Data supplies everything else about it. A datapack curse that names an existing
entity type and mechanic can be added without code.

Each curse's unique behavior is a `CurseMechanic` (tick, damage taken, melee multiplier, after a
hit, may act, may attack, spawn, death, removal), chosen by id. Mechanics keep scratch timers on
the entity and show their state to the client through two synced integers and a disguise flag.
One `CurseRenderer` draws every curse from its data: a fade-in while emerging, a dissolve on
death, the data's scale, the disguise model, and an extra outline pass, because GeckoLib draws
through custom geometry, which vanilla's glow outline does not cover.

Fear is a per-level data attachment of 16-block cells (event fear with a timestamp, decayed when
read) plus surroundings computed on demand. Sensing is server-driven: the client only toggles
it, and the server sends the presences in range twice a second.

### 3.9 Assets

All textures, sounds and models are original. Textures and model geometry are generated by
scripts in `tools/` or written by hand, so they can be reproduced. They are programmer art: they
work, but they are not polished. A real artist pass is listed as a known gap. Sounds are made
from synthesized sources. There is no voice acting.

### 3.10 Domains (M5)

A domain is a fixed sphere at the caster held by `DomainService` on the server. Each tick it
refreshes who is inside, pays upkeep each second, holds trapped targets inside the barrier (by
moving them back, never with blocks, so no world changes), and runs the data's behaviors. Clashes
compare CE pools after a 2 s push. Regen suppression is a CE regen hook; the domain checkpoints
an entity's stored CE whenever it enters or leaves, so the closed-form regen stays exact.

Clients receive the list of open domains when it changes, and short events (open, clash, rolls).
The look is plain geometry and HUD overlays: a dome drawn with the vanilla translucent
position-color type (it closes off the sky from inside, which is the brief's "sky inverts"), a
glyph ring and ambience with the additive lightning type, and screen-edge distortion and the
information flood as HUD layers. This avoids sky and fog hooks and post-processing shaders, so it
works the same with Sodium and Iris.

### 3.11 Black Flash, Reverse Cursed Technique and vows (M6)

All three are server systems that plug into hooks the earlier milestones exposed, so none of them
edits the technique, energy or domain code directly:

- **Black Flash** is a `StrikeHooks` resolver: an eligible strike asks every resolver whether it
  becomes something else. It rolls the chance, deals `BLACK_FLASH` damage, refunds CE and keeps a
  short "zone" per entity. Clients get one payload and play the flash, hit-stop, shake and toast.
- **Reverse Cursed Technique** is an attachment (unlocked, auto-heal, last save) and a Fabric
  `ALLOW_DEATH` listener: a lethal blow either triggers the awakening roll or the emergency save.
  Healing runs every half second; the curse bonus is a `DamageModifiers` entry; domain burnout
  goes through it.
- **Binding Vows** are an attachment holding the sworn vow ids, focus, term, points and debt. The
  vow's power is a technique modifier provider, a capacity and regen modifier, and a melee damage
  modifier. Enforced restrictions are cast blockers (`TechniqueService.addCastBlocker`) and a
  domain expand blocker (`DomainService.addExpandBlocker`); breakable ones are checked each tick,
  and jumps come from a small `ServerPlayer.jumpFromGround` mixin through `PlayerActions`, the
  same feed the Echo Mimic records from. `Vows.addEndListener` lets other systems react (Vow Kin
  flee when their quarry's vow ends).

The **Unbound Ritual** listens for special-grade curse deaths and checks the four conditions; it
adds a counter that the existing trial system reads, so promotion stays in one place.

New extension points: `StrikeHooks.add`, `DamageModifiers.add`, `PlayerActions.addListener`,
`Vows.addEndListener`, `DomainService.addExpandBlocker`, `WardenMechanic.addRule` (the Culling
Game will use it to keep its Warden bound by game rules).

### 3.12 Curse roster alignment (M6)

M4 shipped twelve curses whose mechanics drifted from the brief. M6 reworked seven of them to match
it (the Stilt is now strong against airborne prey; the Echo replays your own last actions; the
Lantern fires a charged beam; Tidewash floods and grabs; the Matriarch follows the sun and walls off
retreat; the Saint wears a real player's skin; the oni charges and staggers; the Gate reshapes its
arena by phase) and added the five special grades and Vow Kin. The last three curses in the brief
(Doorframe Hunter, Gilded Mimic, Tithe Burrower) arrived in M8, completing the brief's 20.

### 3.13 The Culling Game (M7)

`CullingGame` is the only writer of the saved `CullingState`. It raises colonies (a radius, a
wall 24 blocks high under a dome; the shape is `CullingMath.inside`), checks presence every five
ticks for players and entrants, runs the day clock every second, and turns everything that
happens into a `RuleEvent` offered to the rules in force. The rules decide: a clause whose trigger
matches runs its effects, and the effects are the only code paths that change the game (join,
confine, award points, transfer, reveal, negotiate and so on). A participant's request (transfer,
reveal, substitute, withdraw, negotiate) is refused unless a rule in force answers it, so a
datapack that drops a base rule removes that option from the game.

- Colonies: whoever is inside when a barrier rises is a resident and may come and go; anyone
  who enters later becomes a participant (the `one_way_entry` rule) and is held inside (pushed
  back at the wall, brought back from afar). Creative and spectator players never join.
- Entrants: NPC participants (sorcerer or not) brought into each colony while its centre is
  loaded, a few more each day, so the game works for a single player. They hunt participants of
  their own colony and sometimes each other.
- The Kogane (the players' liaison to the game master; one per colony here, where canon gives one to
  each player) floats at each colony's centre, is not saved and is raised again
  whenever the centre loads without one. Negotiation needs a participant within 8 blocks of it.
- Points feed the Grade 1 trial counter (`culling_points`). Deaths cost players a share of their
  points (config); entrants are eliminated.
- The Culling Warden hooks: rules can bind a Warden standing in a colony (`binds_warden`) and
  name its weak point (`warden_edict`), through `WardenMechanic.addRule` and `addEdictSource`.
- The end condition is configuration (days, or participants left), never the canon ending. At
  the end, final standings are kept, online players get grade experience by place, and barriers,
  entrants and Kogane go.
- Clients get one view per player (`CullingPayloads.View`): what that player may know. Names and
  colonies of participants outside their colony stay hidden unless revealed; the server sends
  positions only for revealed participants.

### 3.14 Shikigami and cursed tools (M8)

Shikigami are entities (`ShikigamiEntity`: Divine Dog, Nue, Toad) owned through
vanilla `OwnableEntity`, so `CastContext.isEnemy` and the rest of the mod already treat them as the
caster's side. Each has its own small AI in `behave()`; they follow whoever their owner is
fighting (the owner's target, last victim or last attacker), are not saved, and dissolve when the
time runs out or the owner is gone. The data side is one behavior, `summon`, that names the type.

Cursed tools split code and data the way curses do. The items are code (`CursedToolItem`,
`CursedFingerItem`, `DriftingGourdItem`, `TallyLedgerItem`); a synced `jjkcraft/cursed_tool` entry
gives each its grade, lent technique, cost multiplier and on-hit effect. Lent techniques reach
casting through `TechniqueSources` (the arsenal domain uses the same path); the cost multiplier is
a `TechniqueService` modifier provider. Tooltips read the synced data; the client adds whether the
player's grade can use the power.

### 3.15 Hardening, performance and release (M9)

- Every client-to-server packet is checked on the server and rate limited per player: casts and
  quick casts (`TechniqueService`), growth spending, domain actions, vows (2 a second), Culling
  Game requests (4 a second), the Reverse toggle, loadout edits and slot selection (20 a second),
  sensing toggles. Limits apply at the packet receiver, so commands and tests are not throttled.
  A player's limiter buckets are dropped when they disconnect. Payloads are bounded by their
  codecs (lists capped, strings capped) and every id, slot, amount and target is validated before
  it is used; a refused request changes nothing.
- Everything runs on the server thread; client packets only ask. The GameTest server is a
  dedicated server, so every test run also checks that common code never loads client classes.
- Performance: a crowded-arena GameTest (40 curses with AI, 12 entrants, 4 summons) records the
  server's average tick time; it stays far under the 50 ms budget (see TEST_REPORT). World
  effects share a quad budget and particles a per-frame cap; barriers and domes draw only within
  range.
- Balance: `BalanceSimulationTest` checks the data against the budgets in BALANCE.md section 9.
- Release: the build produces the mod jar and the two sample packs (`samples/`, zipped to
  `build/distributions/`); a GameTest decodes every sample file with the world's codecs.

### 3.16 Canon names and old ids (Phase 0, 1.1.0)

The second brief replaced the original-name rule: a power, technique, domain, clan, term or
character that exists in Jujutsu Kaisen uses its canon name, checked on the Jujutsu Kaisen Wiki;
content with no counterpart keeps an original name. Phase 0 renamed ids, lang keys, classes,
textures and models, and changed no behavior. `docs/NAMING.md` has the table, the sources, what
was kept and why, and where mechanics still differ from canon.

Old worlds and data packs must keep loading, so `LegacyIds` holds the old-to-new table. Built-in
registries (items, entity types, mob effects, item components, technique behavior types) get
Fabric's registry aliases (`Registry.addAlias`, injected by Fabric's registry sync), which
redirect lookups by old id. Data-driven ids (techniques, domains, Culling Game rules) live in
dynamic registries that are rebuilt on every world load, so instead of aliasing them, every codec
that reads such an id from a save or a data pack maps old to new when it reads. The state then
holds only new ids, and comparisons between ids stay simple.

### 3.17 The Modulo expansion (Phase 1, 1.1.0)

Everything lives in `com.jjkcraft.modulo` (server) and `com.jjkcraft.client.modulo` (client) and
reaches the rest of the mod only through hooks that already existed or were added for it: Mul
costs through `TechniqueService.addCastBlocker` and `addCastListener`; Mahoraga's adaptation and
the Mul Ward through `DamageModifiers`; Dabura's domain through `DomainService`. Turning
`modulo.enabled` off makes those hooks no-ops. One Phase 1 change belongs to the base mod and
stays on: mediums (a cursed tool paying its lent technique with durability, through the new
`TechniqueService.addCostPayer`), so the Split Soul Katana pays Dismantle with durability even
with the expansion off. The design and the canon
behind it are in `docs/MODULO.md`.

- **Data**: missions, sites and bosses are server-only dynamic registries
  (`jjkcraft/modulo_mission`, `modulo_site`, `modulo_boss`), so a data pack can add or retune
  them; a boss's behavior and a site's layout name code. The two dimensions, their biomes and
  features are plain datapack worldgen in the `modulo` namespace.
- **State**: per player, the `jjkcraft:modulo` attachment (`ModuloProgress`: boarded, return
  point, missions done, current mission and stage, a stage counter, companion, choice flags) and
  `jjkcraft:mul_energy`, both kept through death. Per world, `ModuloState` (saved data): the
  Descent's timer and position, and which sites have been built.
- **Missions** run on the server tick every 10 ticks per online player: stage checks, intro
  lines, and the sites players are at (built once, characters and spawns kept while someone is
  near, removed when nobody is). A stage is a checkpoint; nothing of a fight is saved, so after a
  relog the stage simply starts again. Bosses, NPCs and spawns are not saved with the world.
- **Bosses**: one entity class with a behavior chosen by data (`BossBehavior`), plus Toranosuke
  (a GeckoLib model) and Mahoraga (its own class, since it fights beside the players).
- **Client**: one sync payload with what the HUD needs (`ModuloPayloads.View`), dialogue and
  event payloads, and one choice request. The ship and Mahoraga's wheel are custom geometry from
  `LevelRenderEvents`; the HUD elements use `HudElementRegistry`.
- **No new mixin in the mod.** The GameTest server needed one test-only mixin to load datapack
  dimensions (`docs/MIXINS.md`).
- **Health ceiling**: vanilla caps max health at 1024; a boss whose scaled health is higher keeps
  1024 and takes proportionally less damage (`ModuloMath.damageScale`).

## 4. Progression, balance and the Special Grade path (outline)

Full numbers go in `docs/BALANCE.md` at M2 and M9. The outline:

- The grade ladder is the one in the brief: Grade 4, Grade 3, Semi-Grade 2, Grade 2,
  Semi-Grade 1, Grade 1, Special Grade. A player's displayed grade comes from **grade
  experience** plus two measured stats, **Output** (CE spent per cast) and **Control** (cast
  accuracy and efficiency). Two players of the same grade can therefore still differ in stats.
- Promotions above Grade 3 need trial achievements as well as experience: defeat a curse of the
  target grade, master a technique, and so on. The trials are defined in grade data.
- **Special Grade path (one path, documented):** "The Unbound Ritual". The player must be
  Grade 1, have mastered an innate technique, hold an active Binding Vow, and defeat a
  Special Grade curse inside their own Domain Expansion. This is a one-time unlock. Experience
  alone can never reach it. Implemented in M6 (`UnboundRitual`).
- Heavenly Restriction is rolled at awakening with a configurable chance (default 2%). It sets CE
  capacity to 0, disables techniques and domains, and gives physical bonuses. Players with
  Heavenly Restriction still progress through grades, using trials based on physical combat.

## 5. Risks

| Risk | Impact | Mitigation |
|---|---|---|
| Rendering API changes in 26.x (blaze3d pipelines, possible Vulkan backend) | VFX milestones (M3, M5) | Read the decompiled 26.3 sources before writing render code. Prefer GUI overlays and simple geometry. Keep the shader toggle. |
| A Fabric rendering or model module is incomplete for 26.3 | Trails, the shell, the HUD | Check each module's presence in the Fabric API jar before use. Fall back to small mixins. |
| GeckoLib 5.5 API differs from older tutorials | M4 | Read the 5.5.7 sources and the 26.3 branch, not old guides. |
| Iris shader packs override custom post effects | Domain and Black Flash visuals | Overlay-first design. Document what is lost with shader packs on. |
| Volume of original art (20 curses, 30 technique icons, 6 domains) | Visual quality | Generated programmer art, documented as a known gap. |
| Feel and visual quality cannot be judged without playing | All VFX and balance | Client GameTest screenshots where possible. TEST_REPORT.md says honestly what was and was not checked by a human. |
| Long Windows paths in the working folder | Gradle and run directories | Short run directory names. Recommend a short project path in the README. |
| The EULA must be accepted for the dedicated server and some test runs | runServer, multiplayer test | `eula` stays `false` in the build. The user accepts it themselves (see README). |

## 6. Assumptions not yet verified

Each one is checked at the milestone named, and this list is updated.

1. ~~HUD element registry present.~~ **Verified at M1:** `HudElementRegistry` with
   `GuiGraphicsExtractor`; the F1 state is `Minecraft.gui.hud.isHidden()`.
2. ~~Attachment builder matches docs.~~ **Verified at M1.** Non-death respawns copy every
   attachment; deaths copy only `copyOnDeath` types. The mod copies its own data on death
   through `SorcererLifecycle` so it can apply the death policy.
3. ~~AutoConfig present.~~ **Verified at M1:** AutoConfig, `JanksonConfigSerializer` and a shaded
   Jankson `@Comment` are in Cloth Config 26.3.159.
4. ~~Vanilla GameTest structure loading works with an empty template in 26.3 through Fabric's
   `@GameTest`.~~ **Verified at M0:** `@GameTest` with no structure uses
   `fabric-gametest-api-v1:empty` and passes.
5. ~~Fabric `KeyMapping` registration and categories in 26.3.~~ **Verified at M3:**
   `KeyMapping.Category.register` and `KeyMappingHelper.registerKeyMapping`.
6. ~~The vanilla sky and fog color hooks needed for domain sky inversion.~~ **Not needed (M5):**
   the dome geometry closes off the sky, so no sky or fog hook is used.
7. Whether 26.3 has an experimental Vulkan renderer to test against. **Not tested (M9):** no
   alternative renderer was run; every effect uses only vanilla render types and HUD drawing,
   which keeps the risk low.
8. ~~GeckoLib 5.5.7 entity renderer and `GeoEntity` API names.~~ **Verified at M4:**
   `GeoEntityRenderer<T, R extends EntityRenderState>`; `GeoModel` getters take a
   `GeoRenderState` filled through `DataTicket`s; triggerable animations on named controllers.
   Models are submitted as custom geometry, so glow outlines need their own outline pass.
9. ~~Whether the server can tell that a client is using Iris, to warn about shader conflicts.~~
   **Dropped (M9):** with no post shaders and no sky or fog hooks there is nothing for a shader
   pack to conflict with, so no warning is needed. **Corrected by the compatibility run
   (2026-10-18):** a shader pack draws only pipelines Iris has a program for, and it had none for
   `debug_quads`, so domes and barriers vanished. The mod now draws them with its own pipeline and
   assigns it a program through Iris's API (`IrisCompat`). Still no warning is needed.

## 7. Decisions on the brief's open points

- The brief says "Work in the milestones in section 14", but the milestones are in section 15.
  Section 15 is taken as the plan.
- Default death policy: `KEEP`. Progress is kept, and CE refills to 50% on respawn.
- Default Culling Game end condition: the game ends when a configurable game-day limit passes
  (default 60) or one colony is left with one player. Both are configurable. The canon ending is
  not hardcoded.
- Version starts at 0.1.0 (M0) and the minor version goes up by one at each milestone
  (M1 = 0.2.0, and so on).

## 8. Decisions on the second brief (phases 0 to 7)

- **Versions** (agreed with the owner, 2026-10-18): the brief asks for 0.2.0 after Phase 1 and
  1.0.0 at Phase 7, but 0.2.0 was M1 and the mod was already 1.0.0. Phases 0 and 1 ship as
  1.1.0, each of Phases 2 to 6 raises the minor version (1.2.0 to 1.6.0), and Phase 7 is 2.0.0,
  because the renames and the combat overhaul break old data packs (old worlds still load).
- **Iris and the Phase 2 effects** (agreed with the owner, 2026-10-18): each new world effect
  (aura shell, ribbon trails, lightning arcs, dissolve, glyph rings) gets its own render pipeline,
  registered with Iris through `IrisApi.assignPipeline` the way the dome is. Its noise comes from
  a shared noise texture, so a shader pack's textured program still draws it. The screen
  distortion post-effect and the sky and fog inversion run only while
  `IrisApi.isShaderPackInUse()` is false, checked every frame; with a pack on, domains fall back
  to the overlay path of M5 (HUD edge distortion, the dome closing off the sky). A client option
  forces the fallback. The Complementary Reimagined compatibility run is repeated after Phase 2.
- **Phase order:** the brief's order (0 to 7). No dependency has forced a change so far; any
  change will be recorded here with its reason.
- **Modulo stays off by default** (Phase 1): the M0 config already had the expansion as an
  opt-in toggle, and its story spoils the manga's ending. A server turns it on with
  `modulo.enabled`.
- **Modulo's energy resource is Mul energy** (Phase 1): the brief's "magic power" has no canon
  basis; Mul, the Simurian ore, converts cursed energy (rolloluca) into energy, so the pool is
  cursed energy run through Mul (`docs/MODULO.md` section 2 has the full table of corrections).
- **Canon corrections:** where the brief and the wiki disagree, the wiki wins, and the difference
  is recorded in `docs/NAMING.md` (for example, Chimera Shadow Garden is Megumi Fushiguro's
  domain, not Geto's).
