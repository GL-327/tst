# Naming

Version 1.1.0 changes the naming policy. Where a power, technique, domain, clan, term or named
character exists in Jujutsu Kaisen, the mod uses the canon name. An original name is used only
when there is no JJK counterpart: the 20 original curses, original bosses such as the Culling
Warden, and original items, techniques and vows. Canon names are used; canon art is not (see
`docs/DESIGN.md` 3.9).

This file holds the full rename table, what was kept and why, how old worlds and data packs
still load, and where a renamed power's mechanics still differ from canon. Phase 0 changed
names only, never behavior; the mechanic gaps below are closed in later phases.

## Sources

Every canon name was checked on the Jujutsu Kaisen Wiki (jujutsu-kaisen.fandom.com) on
2026-10-18, through its MediaWiki API (the page HTML refuses automated fetches). Pages used:
Limitless, Cursed Technique Lapse: Blue, Hollow Technique: Purple, Shrine, Ten Shadows Technique,
Template:Ten Shadows Shikigami, Cursed Speech, Idle Transfiguration, Projection Sorcery, Reverse
Cursed Technique, Ratio Technique, Straw Doll Technique, Blood Manipulation, Copy, Chimera Shadow
Garden, Malevolent Shrine, Unlimited Void, Idle Death Gamble, Authentic Mutual Love,
Self-Embodiment of Perfection, Coffin of the Iron Mountain, Horizon of the Captivating Skandha,
Deadly Sentencing, Smallpox Deity, Template:Cursed Tools, Inverted Spear of Heaven, Split Soul
Katana, Kogane, Culling Game. Page URLs are `https://jujutsu-kaisen.fandom.com/wiki/<page name>`.

## Rename table

### Techniques

| Old id | New id | Name in game | Canon source | Fit |
|---|---|---|---|---|
| `inward_hollow` | `lapse_blue` | Cursed Technique Lapse: Blue | Limitless; Cursed Technique Lapse: Blue | attraction field: exact |
| `outward_bloom` | `reversal_red` | Cursed Technique Reversal: Red | Limitless | repulsion: exact |
| `null_convergence` | `hollow_purple` | Hollow Purple | Hollow Technique: Purple ("Hollow Purple" is VIZ's short form, per the wiki) | Blue and Red merged into an erasing mass: exact |
| `hollow_star` | `maximum_output_blue` | Maximum Output: Blue | Cursed Technique Lapse: Blue, "Cursed Technique Lapse - Maximum Cursed Energy Output: Blue" | a vast attraction field; the closing burst is the mod's |
| `crescent_rend` | `cleave` | Cleave | Shrine | adjusts to the target's cursed energy: match. Canon Cleave needs touch outside a domain; the mod's is a ranged arc |
| `hairline_sever` | `dismantle` | Dismantle | Shrine | the standard ranged cut: match |
| `crimson_meridian` | `piercing_blood` | Piercing Blood | Blood Manipulation (extension list) | compressed blood fired in a line: match |
| `effigy_nail` | `straw_doll_resonance` | Straw Doll Technique: Resonance | Straw Doll Technique | a doll linked to the target, nails hurt them: match. Canon needs a piece of the target; the mod links by sight |
| `kennel_call` | `ten_shadows_divine_dogs` | Ten Shadows: Divine Dogs | Ten Shadows Technique; Template:Ten Shadows Shikigami | two hunting dogs: match |
| `crane_fold` | `ten_shadows_nue` | Ten Shadows: Nue | same | a winged scout that dives: match. Canon Nue's wings shock and paralyse; not built |
| `tortoise_ward` | `ten_shadows_toad` | Ten Shadows: Toad | same | the closest canon shikigami for a guardian at its master's side. Canon Toad snares and pulls with its tongue; the mod's taunts and swats projectiles (see Mechanic gaps) |
| `command_halt` | `cursed_speech_dont_move` | Cursed Speech: Don't Move | Cursed Speech, "Known Commands" | paralyses: exact |
| `command_kneel` | `cursed_speech_crumble_away` | Cursed Speech: Crumble Away | same | heavy gravity slams the target down: match |
| `command_rise` | `cursed_speech_blast_away` | Cursed Speech: Blast Away | same | launches the target: match |
| `flesh_sculpt` | `idle_transfiguration` | Idle Transfiguration | Idle Transfiguration | reshapes self or a touched target: match. Canon works on the soul by touch; the mod's by sight |
| `stolen_frame` | `projection_sorcery` | Projection Sorcery | Projection Sorcery | 24-frame movement, targets frozen in a frame: match |
| `reverse_flow` | `reverse_cursed_technique` | Reverse Cursed Technique | Reverse Cursed Technique | healing with positive energy: exact |
| `kindred_mend` | `positive_energy_output` | Positive Energy Output | Reverse Cursed Technique ("output the positive energy to heal others", said of Shoko, Yuta, Rika and Round Deer) | healing another: match. The wiki has no proper name for this; the in-game name uses its wording |
| `still_air_veil` | `infinity` | Infinity | Limitless, "Neutral Limitless: Infinity" | slows what approaches: match. Canon stops everything; the mod's slows projectiles only |

### Domains

| Old id | New id | Name in game | Canon user |
|---|---|---|---|
| `hanging_edges` | `malevolent_shrine` | Malevolent Shrine | Sukuna (Shrine) |
| `boundless_index` | `unlimited_void` | Unlimited Void | Satoru Gojo (Limitless) |
| `lightless_kennels` | `chimera_shadow_garden` | Chimera Shadow Garden | **Megumi Fushiguro** (Ten Shadows). The brief says Geto; the wiki says Megumi |
| `lantern_row_wager` | `idle_death_gamble` | Idle Death Gamble | Kinji Hakari |
| `borrowed_oaths` | `authentic_mutual_love` | Authentic Mutual Love | Yuta Okkotsu (Copy) |

### Entities, items, effects, Culling Game rules

| Kind | Old id | New id | Name in game | Canon source |
|---|---|---|---|---|
| entity | `ink_hound` | `divine_dog` | Divine Dog | Ten Shadows shikigami |
| entity | `paper_crane` | `nue` | Nue | same |
| entity | `stone_tortoise` | `toad` | Toad | same |
| entity | `shadow_hound` | `shadow_divine_dog` | Shadow Divine Dog | Chimera Shadow Garden makes shikigami from the shadows; "Shadow Divine Dog" describes them |
| entity | `tallykeeper` | `kogane` | Kogane | Kogane: the Culling Game's liaison shikigami. The game master decides; the Kogane relays (rules 6 and 7) |
| item | `bound_effigy` | `straw_doll` | Straw Doll | Straw Doll Technique |
| item | `inverted_tine` | `inverted_spear_of_heaven` | Inverted Spear of Heaven | "forced cancellation" of any technique it touches: exact |
| item | `split_soul_nodachi` | `split_soul_katana` | Split Soul Katana | canon ignores physical resistance and cuts the soul; the mod's lends Dismantle (see Mechanic gaps) |
| item | `thousand_link_chainwhip` | `chain_of_a_thousand_miles` | Chain of a Thousand Miles | canon extends while its end is unseen; the mod's adds 2 blocks of reach |
| effect | `skipped` | `frozen_frame` | Frozen Frame | describes Projection Sorcery's freeze ("frozen in an animation frame"); not a canon proper name |
| culling rule | `the_tallykeeper` | `game_master` | The Game Master | rule 6: spend 100 points to negotiate a rule with the game master |
| cursed tool kind | `vessel` | `cursed_object` | (data only) | the Withered Digit is a cursed object; "vessel" is the person who takes one in (Phase 6) |

### Code identifiers

| Old | New |
|---|---|
| `InkHoundEntity`, `PaperCraneEntity`, `StoneTortoiseEntity` | `DivineDogEntity`, `NueEntity`, `ToadEntity` |
| `ShadowHoundEntity`, `ShadowHoundRenderer` | `ShadowDivineDogEntity`, `ShadowDivineDogRenderer` |
| `TallykeeperEntity`, `TallykeeperRenderer` | `KoganeEntity`, `KoganeRenderer` |
| `BoundEffigyItem`, `EffigyLink` | `StrawDollItem`, `StrawDollLink` |
| `EffigyLinkBehavior` (`effigy_link`) | `StrawDollBehavior` (`straw_doll`) |
| `TimeSkipBehavior` (`time_skip`) | `ProjectionSorceryBehavior` (`projection_sorcery`) |
| `ReshapeBehavior` (`reshape`) | `IdleTransfigurationBehavior` (`idle_transfiguration`) |
| `ProjectileVeilBehavior` (`projectile_veil`) | `InfinityBehavior` (`infinity`) |
| `CommandBehavior` (`command`) | `CursedSpeechBehavior` (`cursed_speech`) |
| item component `jjkcraft:effigy_link` | `jjkcraft:straw_doll_link` |
| `JjkEffects.SKIPPED` | `JjkEffects.FROZEN_FRAME` |
| `CursedToolDefinition.Kind.VESSEL` | `Kind.CURSED_OBJECT` |
| lang `message.jjkcraft.effigy.*`, `tooltip.jjkcraft.effigy.*` | `message.jjkcraft.straw_doll.*`, `tooltip.jjkcraft.straw_doll.*` |
| lang `message.jjkcraft.vessel.*`, `tooltip.jjkcraft.tool.vessel*` | `message.jjkcraft.cursed_object.*`, `tooltip.jjkcraft.tool.cursed_object*` |

Technique icons (`textures/gui/technique/`), entity and item textures, GeckoLib models and
animations, and the art scripts in `tools/` follow the new ids. The art itself is unchanged: Nue
is still drawn as the old paper crane and the Toad as the old stone tortoise until the Phase 3
art pass.

## Kept as original

These have no JJK counterpart, so they keep their names.

- **Techniques:** Ink-Iron Palm (a reinforced palm strike; Black Flash is a separate canon
  term the mod already uses), Grudge Bolt, Seeking Grudge Bolt, Splinter Volley, Ground Toll,
  Ember Skin, Ink Step, Lacquer Ward, Grave Chains (chains are a canon cursed tool, not a
  technique; the brief's "blood-based immobilize" would be Crimson Binding, which the mod does
  not have), Oathbound Edge, Thousand Lances.
- **Domains:** Sevenfold Recitation (strikes everyone inside seven times; no canon domain does
  this. Self-Embodiment of Perfection, the brief's suggestion, is a sure-hit Idle Transfiguration
  domain, which would be a behavior change), Throat of Ten Thousand Words (the Sovereign's).
- **The 20 curses, the Culling Warden, entrants**, and the original Culling Game rules (they
  paraphrase canon rules but have their own titles; the canon rule each follows is in
  `docs/SCHEMAS.md`).
- **Items:** Withered Digit (a cursed object that grants a technique; Sukuna's fingers do not do
  that, and Phase 6 uses fingers for the Sukuna Pact), Cleaver of Bad Debts, Bonewood Staff,
  Drifting Gourd, Tally Ledger, Arsenal Blade (canon gives the swords of Authentic Mutual Love no
  name), the curse drops, technique scrolls.
- **Vows:** the eight vows are original restrictions sworn as Binding Vows.
- **Sound event ids** `technique.attract`, `technique.repel`, `technique.merge`: they name the
  sound (a pull, a burst, a merge), several techniques share them, and players never see them.
- Terms that were already canon stay: cursed energy, grades (Grade 4 to Special Grade), Domain
  Expansion, Simple Domain, sure-hit, Black Flash, the zone, Reverse Cursed Technique, Binding
  Vow, Heavenly Restriction, cursed tools, Culling Game, colonies.

## Migration

Old worlds and data packs load without changes. `com.jjkcraft.registry.LegacyIds` holds the
table above.

- **Built-in registries** get Fabric registry aliases (`Registry.addAlias`): items, entity types,
  mob effects, item components and technique behavior types. A saved stack of
  `jjkcraft:inverted_tine` loads as the Inverted Spear of Heaven; a data pack technique with
  `"type": "jjkcraft:time_skip"` decodes as Projection Sorcery.
- **Data-driven ids** are mapped when read, then written back under the new id: a player's
  innate and learned techniques, hotbar, mastery and cooldowns; their known domain; Culling Game
  rules in force and rule conflicts; the technique on a scroll or Arsenal Blade; an entrant's
  techniques; and in data packs, a curse's techniques and entity, a cursed tool's item and
  technique, a summon's entity and the arsenal's fallback techniques. Each old id read logs one
  warning naming its new id.
- **Not covered:** a data pack that *overrides* a renamed file must use the new file name.
  For example, a retune of the old `cursed_tool/inverted_tine.json` now adds a second entry
  for the same item instead of replacing the shipped one; rename it to
  `inverted_spear_of_heaven.json`. Translations in resource packs must use the new lang keys.
- Shikigami, shadow dogs and the Kogane are never saved, so nothing in a world refers to their
  old entity ids; the aliases cover commands and data packs that name them.
- Tests: `LegacyIdsTest` (an old player save loads with the new ids; every renamed data file
  exists and no old one remains) and `LegacyIdGameTests` (old item, entity, effect, component
  and behavior ids resolve in a running server; an old saved stack and scroll load; an old data
  pack technique decodes).

## Mechanic gaps

Renamed powers whose mechanics still differ from canon. Phase 0 changes names only, so these
are listed for later phases.

| Power | Canon | Mod now | Phase |
|---|---|---|---|
| Cleave | contact only outside a domain | ranged arc | 4 (combat) |
| Ten Shadows: Toad | snares and pulls with its tongue | taunts, swats projectiles | 3 (model), 4 (moves) |
| Ten Shadows: Nue | electric wings paralyse | dives for damage | 4 |
| Infinity | stops everything that approaches | slows projectiles | 4 |
| Idle Transfiguration | touch, works on the soul | by sight | 4 |
| Straw Doll Technique: Resonance | needs a piece of the target | linked by sight | 6 (cursed objects) |
| Maximum Output: Blue | attraction only | pulls, then bursts; drawn purple | 2 (VFX colour) |
| Split Soul Katana | ignores physical resistance | lends Dismantle | 4 (weapon movesets) |
| Chain of a Thousand Miles | extends while its end is unseen | +2 reach | 4 |
| Kogane | one per player | one per colony | 6 (Culling Game integration) |
| Authentic Mutual Love's Copy | Rika eats part of the target | copies techniques seen cast nearby | later, with Copy as a technique |

## Brief corrections found while checking

- Chimera Shadow Garden is Megumi Fushiguro's domain, not Geto's.
- The wiki gives the Smallpox Deity's domain no name. "Graveyard Domain" is not on the wiki;
  if that domain is added, its name needs another source or it stays unnamed in game.
- Nanami's technique is "Ratio Technique": lines in tenths, a critical hit at the 7:3 point.
  The mod has no Ratio yet (Phase 4).
- The wiki spells the clan "Zenin"; VIZ writes "Zen'in". To be settled in Phase 5.
- Culling Game: rule 1 gives 19 days to join after awakening a technique; rule 8 removes the
  technique of a player whose score stays the same for 19 days (for Phase 6).
- For Phase 1: Kou Usami (a Modulo official) uses Cursed Speech; Dabura's full name on the
  wiki is Dabura Karaba and his technique is called Light. To be checked again in Phase 1.
