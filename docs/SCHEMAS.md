# Data schemas

Every piece of content is a JSON file in a datapack. The mod's own content lives in
`data/jjkcraft/jjkcraft/<registry>/<name>.json`. A datapack adds content at
`data/<its namespace>/jjkcraft/<registry>/<name>.json`, and the entry id is
`<its namespace>:<name>`. Files are validated by codecs when the world loads. A bad file stops
the world from loading and the log names the file and the field. These registries load with the
world, so `/reload` does not pick up changes to them; restart the world after editing (see
`docs/DESIGN.md` 3.1).

Types used below: `id` is a namespaced id such as `jjkcraft:grade_2`. `color` is `"#RRGGBB"`.
Ranges are inclusive. Fields marked optional have the default shown.

## Grade (`jjkcraft/grade`)

Synced to clients. The ladder is ordered by `rank`.

| Field | Type | Default | Meaning |
|---|---|---|---|
| `rank` | int 0 to 32 | required | Position on the ladder (Grade 4 = 0, Special Grade = 6) |
| `name` | string | required | Translation key of the display name |
| `color` | color | required | Badge color |
| `ce_multiplier` | number 0 to 1000 | required | Multiplier on base CE capacity |
| `technique_slots` | int 1 to 6 | required | Usable hotbar slots |
| `domain_tier` | int 0 to 2 | 0 | 0: no domains; 1: domains that need tier 1; 2: every domain |
| `xp_required` | int >= 0 | 0 | Total grade experience needed to be promoted into this grade |
| `min_control` | number 0 to 1 | 0 | Minimum cast accuracy (needs 10+ casts) |
| `min_output` | number >= 0 | 0 | Minimum lifetime CE spent on techniques |
| `trials` | list of trial | [] | Requirements to be promoted into this grade |
| `special` | bool | false | Anomaly grade: only reachable through an `unbound_ritual` trial |
| `perks` | perks | see below | Numeric perks |
| `tool_durability` | int 0 to 1000000 | 0 | Durability of a cursed tool used as a medium by a sorcerer of this grade (0: `96 + 64 x rank`; 1.1.0) |

Trial: `id` (string, stored on completion), `type` (one of `defeat_curse`, `master_techniques`,
`culling_points`, `vow_days`, `domain_clash_wins`, `unbound_ritual`), `grade` (id, the minimum
curse grade for `defeat_curse`, optional), `count` (int 1 to 100000, default 1).

Perks: `sense_range` (blocks, default 16), `regen_bonus` (added to the regen multiplier, default
0), `black_flash_bonus` (percentage points, default 0).

A special grade without an `unbound_ritual` trial is refused by the promotion check, so it can
never be reached by grinding.

## Energy profile (`jjkcraft/energy_profile`)

Server only. Gives CE to vanilla or modded mobs.

| Field | Type | Default | Meaning |
|---|---|---|---|
| `entities` | entity type list or `#tag` | required | Mobs this profile covers |
| `grade` | id | required | Tier the mob counts as (experience, sensing) |
| `capacity` | number 0 to 1000000 | required | Max CE |
| `regen_multiplier` | number 0 to 100 | 1 | Multiplier on the configured regen |

## Technique (`jjkcraft/technique`)

Synced to clients, so tooltips and visuals come from the same file.

| Field | Type | Default | Meaning |
|---|---|---|---|
| `name` | string | required | Translation key of the name |
| `description` | string | required | Translation key of the description |
| `category` | one of `strike`, `projectile`, `area`, `restriction`, `summon`, `domain`, `passive`, `healing`, `utility`, `ultimate` | required | UI category |
| `grade` | id | required | Lowest grade that can cast it |
| `cast` | cast | required | Costs and timing |
| `behavior` | behavior | required | What it does (see below) |
| `visuals` | visuals | required | Look and sound |
| `icon` | id | required | Icon texture, for example `jjkcraft:textures/gui/technique/x.png` |
| `vow_modifiers` | list | [] | Multipliers while the caster holds a vow |
| `innate_weight` | int 0 to 10000 | 0 | Weight in the awakening roll (0: never innate) |
| `mastery_uses` | int 1 to 100000 | 50 | Landed casts to master it (-10% cost, +10% power) |

Cast: `cost` (CE), `cooldown` (ticks), `cast_time` (windup ticks, default 0), `range` (blocks,
default 0), `mode` (`instant`, `channel` while held, `toggle`; default `instant`), `upkeep` (CE
per second for channel and toggle, default 0), `max_duration` (ticks, 0 = no limit), `mul_cost`
(Mul energy paid on top of the CE cost, default 0; a technique with a Mul cost can only be cast
while the Modulo expansion is on; 1.1.0).

Visuals: `color`, `particle` (particle type id, optional), `sound` (cast sound event, optional),
`hit_sound` (optional), `shake` (0 to 5, camera shake for the caster and anyone hit), `hit_stop`
(0 to 20 ticks of client hit-stop on targets), `trail` (`none`, `slash`, `line`, `beam`, `ring`,
`chain`, `orb`, `aura`, `shout`).

Vow modifier: `vow` (id of a `jjkcraft/vow` entry), `damage`, `cost`, `range` (multipliers,
default 1). They apply while the caster's active Binding Vow includes that vow, on top of the
vow's own focus bonus. Example: the Ink-Iron Palm hits 1.35x under Grounded Oath.

### Behaviors (`behavior.type`)

Behaviors are implemented in code; data picks one and sets its parameters. Types renamed in 1.1.0
show their old name; the old name still decodes (`docs/NAMING.md`).

| Type | Parameters |
|---|---|
| `jjkcraft:strike` | `damage`, `knockback` (0.5), `black_flash` (false): melee hit on the aimed target within `cast.range` (4 if 0) |
| `jjkcraft:projectile` | `damage`, `speed` (blocks/tick), `homing` (false), `homing_strength` (0.15), `blast_radius` (0), `pierce` (false), `size` (0.35), `lifetime` (60 ticks), `gravity` (0), `count` (1), `spread` (0 degrees each side: a volley fans evenly) |
| `jjkcraft:arc_slash` | `damage`, `arc` (120 degrees), `ce_scaling` (0.5 base damage per 100 target CE, capped at 300 CE), `knockback` (0.4) |
| `jjkcraft:line_cut` | `damage`, `width` (0.3), `pierce` (0.4, share that ignores armor) |
| `jjkcraft:field` | `strength` (+ pulls, - pushes), `radius`, `duration` (ticks), `damage_per_second` (0), `burst_damage` (20), `burst_radius` (5). Attract and repel fields that overlap collide into a burst |
| `jjkcraft:cursed_speech` (was `command`) | `command` (`stop`, `knockdown`, `leap`), `radius`, `duration` (40), `damage` (0), `strain` (0.5 backlash per target, more against higher grades) |
| `jjkcraft:heal` | `health_per_second`, `ce_per_health` (use with `mode: channel`) |
| `jjkcraft:bind` | `duration` (ticks), `damage` (0) |
| `jjkcraft:barrier` | `block` (share of frontal damage absorbed), `arc` (150), `ce_per_damage` (1) (use with `mode: channel`) |
| `jjkcraft:infinity` (was `projectile_veil`) | `radius`, `slow` (velocity multiplier per tick), `min_speed` (0.05) (use with `mode: toggle`) |
| `jjkcraft:summon` | `entity` (a shikigami type: `jjkcraft:divine_dog`, `jjkcraft:nue`, `jjkcraft:toad`), `count` (1), `lifetime` (600 ticks). Casting again dismisses the caster's summons of that type first; health scales with power |
| `jjkcraft:straw_doll` (was `effigy_link`) | `damage` per nail, `nails` (3), `duration` (600 ticks): binds the aimed target to a Straw Doll given to the caster (a non-player caster nails at once) |
| `jjkcraft:idle_transfiguration` (was `reshape`) | `self_ticks` (400), `other_ticks` (200): the aimed target is Shrunk, or with nothing aimed the caster is Bulked |
| `jjkcraft:projection_sorcery` (was `time_skip`) | `radius` (8), `skip_ticks` (14), `blink` (6 blocks): enemies around freeze, the caster steps forward. Give it a `cast.cast_time`: the windup is the tell |
| `jjkcraft:barrage` | `radius` (5), `strike_radius` (1.6), `damage`, `interval` (2 ticks): lances fall on the marked spot (use with `mode: toggle` and `max_duration`) |
| `jjkcraft:singularity` | `distance` (12), `radius` (10), `pull_ticks` (40), `strength` (0.25), `damage`, `blast_radius` (6): pulls, then bursts (70% ignores armor) (use with `mode: toggle`) |
| `jjkcraft:vow_strike` | `damage`, `per_point` (0.1), `knockback` (0.6): damage times `1 + per_point * vow points`, half without an active vow |
| `jjkcraft:dash` | `distance` (6), `invulnerable_ticks` (6) |
| `jjkcraft:shockwave` | `radius` (6), `damage`, `launch` (0.6): damage falls to half at the edge |
| `jjkcraft:thorns_aura` | `damage`, `burn_seconds` (3): melee attackers of the caster are hurt and set alight (use with `mode: toggle` and `upkeep`) |
| `jjkcraft:mend_other` | `amount`, `regen_ticks` (100), `self_fraction` (0.6): heals the nearest ally in a narrow cone, or the caster for less |

Visual trails: `beam` draws a heavy line with a white core along the cast (ultimates, falling
lances, mending); `line` a thinner one.

Example (`data/mypack/jjkcraft/technique/ember_lance.json`):

```json
{
  "name": "technique.mypack.ember_lance",
  "description": "technique.mypack.ember_lance.desc",
  "category": "projectile",
  "grade": "jjkcraft:grade_3",
  "cast": { "cost": 20, "cooldown": 40, "range": 30 },
  "behavior": { "type": "jjkcraft:projectile", "damage": 8, "speed": 2.0, "pierce": true, "lifetime": 30 },
  "visuals": { "color": "#FF8A3D", "particle": "jjkcraft:cursed_spark", "sound": "jjkcraft:technique.bolt_fire", "trail": "orb" },
  "icon": "mypack:textures/gui/technique/ember_lance.png",
  "innate_weight": 5
}
```

The curse, domain, vow, Culling Game rule and cursed tool schemas follow.

## Curse (`jjkcraft/curse`)

Synced to clients, so the codex and the renderer read the same file. Entity types are code; a
datapack curse can reuse any curse entity type and any mechanic with new numbers, art and loot.
Changes need a world restart (dynamic registries don't refresh on `/reload`).

| Field | Type | Default | Meaning |
|---|---|---|---|
| `name` | string | required | Translation key of the name |
| `description` | string | required | Translation key of the one-line description (codex, after the first kill) |
| `lore` | string | required | Translation key of the codex entry (after `unlock_kills`) |
| `entity` | id | required | Entity type it spawns as; must be a curse entity type |
| `grade` | id | required | Grade on the shared ladder (XP, trials, sensing color) |
| `stats` | stats | required | See below |
| `techniques` | list of ids | [] | Techniques the curse AI may cast within its CE |
| `spawn` | spawn | absent | Natural spawning; absent means it never spawns naturally (and never despawns) |
| `loot_table` | id | entity default | Loot table; absent uses `jjkcraft:entities/<entity>` |
| `resistances` | map of damage type id to 0..10 | {} | Damage multiplier: 0 immune, below 1 resists, above 1 weak |
| `model` | model | required | GeckoLib asset ids and scale |
| `mechanic` | id | required | Code mechanic (see below); unknown ids fall back to `plain` with a warning |
| `parameters` | map of string to number | {} | Numbers the mechanic reads; every one has a default |
| `codex` | codex | color `#8F5CFF`, 1 kill | Accent color and kills to unlock the full entry |

Stats: `health` (1..4096), `armor` (0..30, default 0), `speed` (movement attribute, 0..2),
`attack_damage`, `ce_capacity`, `regen_multiplier` (default 1; fear adds up to +100%),
`drain_on_hit` (CE taken from the victim and added to the curse per hit, default 4),
`follow_range` (default 24), `knockback_resistance` (0..1, default 0).

Spawn: `weight`, `min_fear` (0..100), `group_min` / `group_max` (default 1), `max_light` (block
light, default 7), `water` (default false), `dimensions` (list; empty means any).

Model: `geo` (for example `jjkcraft:entity/ash_oni`, meaning
`assets/jjkcraft/geckolib/models/entity/ash_oni.geo.json`), `texture` (full texture path),
`animation` (`jjkcraft:entity/ash_oni` for `geckolib/animations/entity/ash_oni.animation.json`),
`scale` (default 1), `disguise` (optional `geo` and `texture`, shown while the mechanic marks the
curse disguised). Animations used: `idle`, `walk`, `spawn`, `death` (base controller) and
`attack`, `cast`, `hurt` (triggered).

### Mechanics (`mechanic`)

| Id | Parameters (default) |
|---|---|
| `jjkcraft:plain` | none: melee and techniques only |
| `jjkcraft:hymnal` | `hymn_radius` (5), `hymn_charge` (24 ticks), `hymn_stun` (30), `hymn_interval` (120), `silence_duration` (200) |
| `jjkcraft:ledger` | `max_tally` (8), `damage_per_mark` (0.15) |
| `jjkcraft:wick` | `eat_radius` (6); eats light only with `mob_griefing` on |
| `jjkcraft:stilt` | `airborne_multiplier` (2.0), `grounded_multiplier` (0.6), `airborne_damage_taken` (0.5), `grounded_damage_taken` (1.25), `airborne_speed` (0.5), `grounded_speed` (-0.3), `snatch_reach` (5), `snatch_interval` (30); empowered while its target is off the ground |
| `jjkcraft:echo` | `echo_length` (5 actions), `echo_delay` (60 ticks); records its target's jumps, blows and casts (or the nearest player's) and plays each back after the delay |
| `jjkcraft:salt_lantern` | `charge_ticks` (30), `aim_lock_ticks` (8), `beam_range` (16), `beam_interval` (100), `beam_damage` (2), `burn_per_ce` (0.05 per CE of the target), `drain_fraction` (0.08 of max CE), `burn_seconds` (2), `snuff_threshold` (6), `snuff_ticks` (80) |
| `jjkcraft:tidewash` | `wet_regen` (1.5 per 2 s), `dry_damage_taken` (1.25), `flood_range` (10), `flood_radius` (2), `flood_interval` (300), `flood_ticks` (200), `arm_reach` (7), `arm_windup` (16), `grab_interval` (140), `grab_damage` (4), `pull_strength` (0.9), `grab_slow_ticks` (60), `breath_stolen` (120); flooding needs `mob_griefing` on and only fills air |
| `jjkcraft:thorn` | `root_range` (14), `root_windup` (22), `bind_ticks` (40), `root_damage` (5), `root_lifetime` (200), `root_interval` (120), `thorns_fraction` (0.2), `day_bonus` (0.6), `hedge_half_width` (3); damage and pace scale with the time of day |
| `jjkcraft:bell` | `wave_radius` (10), `wave_speed` (0.45 blocks/tick), `wave_damage` (6), `wave_knockback` (1), `swing_ticks` (26), `ringing_ticks` (50), `ringing_damage_taken` (2), `toll_interval` (140) |
| `jjkcraft:saint` | `reveal_distance` (2.6), `max_patience` (400), `redisguise_ticks` (600), `first_strike_multiplier` (2), `blind_ticks` (40); disguised, it shows the nearest player's skin and name |
| `jjkcraft:oni` | `charge_windup` (22), `charge_ticks` (25), `charge_speed` (0.85), `charge_min` (5), `charge_max` (18), `charge_interval` (160), `sweep_width` (2.2), `sweep_damage` (10), `wall_stagger_ticks` (70), `stagger_poise` (30), `poise_stagger_ticks` (40), `stagger_damage_taken` (1.5), `slam_radius` (4), `slam_damage` (9), `slam_interval` (100), `rage_threshold` (0.4) |
| `jjkcraft:gate` | `arena_radius` (14), `arena_ticks` (900), `phase_two_arena_scale` (0.65), `graves` (6), `grave_damage` (3 per second), `max_minions` (3), `summon_interval` (220), `pull_radius` (16), `pull_strength` (1.1), `pull_interval` (140), `maw_radius` (2.5), `maw_damage` (4), `guarded_damage_taken` (0.35), `bar_range` (40); the arena wall and graves need `mob_griefing` on |
| `jjkcraft:vow_kin` | `hunt_range` (24), `damage_per_point` (0.08 per vow point), `flee_ticks` (100); hunts only players holding a vow |
| `jjkcraft:paper_tide` | `first_fold_hits` (4), `first_fold` (0.6), `second_fold_hits` (8), `second_fold` (0.25), `forget_ticks` (600), `tide_range` (9), `tide_windup` (22), `tide_interval` (120), `tide_damage` (7), `tide_bind_ticks` (30), `bar_range` (48); adapts per damage type id |
| `jjkcraft:ferryman` | `arena_radius` (22), `first_crossing` (200), `interval` (600) and `interval_second` (400), `crossing_ticks` (200) and `crossing_ticks_second` (260), `pull` (0.06) and `pull_second` (0.075), `ferry_damage` (8), `wither_ticks` (100), `leash` (6), `sweep_damage` (5), `bar_range` (48); `_second` values apply below half health |
| `jjkcraft:warden` | `seals` (3), `seals_second` (2), `seal_radius` (9), `seal_lifetime` (12000), `edict_interval` (400) and `edict_interval_second` (240), `weak_point` (1.5), `off_edict` (0.15), `bar_range` (48); can't drop below 1 health while a Rule Seal stands or an outside rule holds |
| `jjkcraft:doorframe` | `door_range` (12), `reveal_distance` (4), `ambush_multiplier` (1.8), `cross_interval` (100), `cross_windup` (15), `cross_range` (6), `exposed_damage_taken` (1.3); hides in the frame of the nearest door |
| `jjkcraft:mimic` | `wake_distance` (2.5), `look_distance` (5), `grab_range` (6), `lash_windup` (12), `lash_interval` (160), `grab_ticks` (60), `grab_damage` (2 per second), `drag_strength` (0.35), `back_speed` (0.12), `break_damage` (6), `settle_ticks` (240), `chest_damage_taken` (0.5); give it a `model.disguise` (the chest) |
| `jjkcraft:tithe` | `pay_radius` (4), `max_stash` (6), `burrow_ticks` (60), `resurface_min` (6), `resurface_max` (12); it wants an item from the item tag `jjkcraft:tithe_offerings` |
| `jjkcraft:sovereign` | `chorus_radius` (12), `chorus_windup` (24), `chorus_interval` (160), `chorus_stun` (24), `chorus_damage` (5), `bite_range` (18), `bite_radius` (1.8), `bite_windup` (28), `bite_interval` (100), `bite_damage` (10), `bite_hold` (40), `bar_range` (64); at 3 mouths or fewer it expands `jjkcraft:ten_mouth_throat` |

Example (`data/jjkcraft/jjkcraft/curse/wick_moth.json`):

```json
{
  "name": "curse.jjkcraft.wick_moth",
  "description": "curse.jjkcraft.wick_moth.desc",
  "lore": "curse.jjkcraft.wick_moth.lore",
  "entity": "jjkcraft:wick_moth",
  "grade": "jjkcraft:grade_4",
  "stats": { "health": 10, "speed": 0.3, "attack_damage": 2, "ce_capacity": 25, "drain_on_hit": 2 },
  "spawn": { "weight": 30, "min_fear": 0, "group_min": 2, "group_max": 4, "max_light": 4 },
  "resistances": { "minecraft:fall": 0.0, "minecraft:in_fire": 1.5, "minecraft:on_fire": 1.5 },
  "model": { "geo": "jjkcraft:entity/wick_moth", "texture": "jjkcraft:textures/entity/wick_moth.png", "animation": "jjkcraft:entity/wick_moth" },
  "mechanic": "jjkcraft:wick",
  "parameters": { "eat_radius": 6 },
  "codex": { "color": "#FFB547", "unlock_kills": 4 }
}
```

## Domain (`jjkcraft/domain`)

Synced to clients (the title, dome and meter read it). Changes need a world restart.

| Field | Type | Default | Meaning |
|---|---|---|---|
| `name` | string | required | Translation key of the name (the opening title) |
| `description` | string | required | Translation key of the description |
| `grade` | id | required | Lowest grade that can cast it; the grade's `domain_tier` must also be above 0 |
| `cast_cost` | number | required | CE to open it |
| `upkeep` | number | required | CE per second while it stands |
| `radius` | 3 to 48 | required | Radius in blocks (x1.4 barrierless) |
| `max_duration` | ticks | 1200 | It closes by itself after this |
| `burnout` | ticks | 400 | After it closes, the caster's techniques are burnt out this long |
| `sure_hit` | bool | true | true: effects use the sure-hit damage type (ignores armor and shields) and only a Simple Domain stops them |
| `clash_priority` | 0 to 20 | 0 | +5% clash power each, and the first tie-breaker |
| `behaviors` | list | required | What happens inside (below) |
| `visuals` | visuals | required | `color`, `secondary`, `dome` (hex colors), `ambience` (`blades`, `glyphs`, `shadows`, `lanterns`, `arsenal`, `rings`, `none`), optional `open_sound` and `ambient_sound` |
| `innate_weight` | int | 10 | Weight when a sorcerer reaching a domain grade is given one (0: never) |

### Behaviors (`behaviors[].type`)

| Type | Fields (default) | Does |
|---|---|---|
| `jjkcraft:sure_hit_damage` | `interval` ticks, `damage` | Damages every target on a beat |
| `jjkcraft:apply_effect` | `effect` id, `amplifier` (0), `duration`, `interval` (20) | Applies an effect to every target on a beat |
| `jjkcraft:suppress_regen` | `multiplier` (0) | Multiplies the CE regen of targets inside |
| `jjkcraft:caster_attribute` | `attribute` id, `amount`, `operation` (`add_multiplied_base`) | Boosts the caster while they stand inside |
| `jjkcraft:overload` | `surge_interval`, `surge_duration` | Surges of Overloaded (players: slowed, can't cast, view flooded; mobs: frozen) |
| `jjkcraft:summon_hunters` | `count`, `respawn_interval` (60), `damage` (4), `health` (14) | Shadow Divine Dogs hunt targets inside; they melt when it ends |
| `jjkcraft:gamble` | `spin_interval`, `outcomes` (list of `kind` = damage, heal, energy, stun or whiff, `weight`, `amount`), `jackpot_streak` (3), `jackpot_ticks` (200) | Rolls an outcome every spin; the same win several times in a row is a jackpot (full CE, regeneration, resistance) |
| `jjkcraft:arsenal` | `slots`, `fallback` technique ids, `cost_multiplier` (0.5) | Forges a blade per copied technique (seen cast near the caster in the last moments), filling from `fallback`; blades cast it cheaper, only inside |
| `jjkcraft:repetition` | `repeats`, `interval`, `damage`, optional `effect`, `effect_ticks` (40) | Strikes every target exactly `repeats` times, then closes the domain |

Example (`data/jjkcraft/jjkcraft/domain/malevolent_shrine.json`):

```json
{
  "name": "domain.jjkcraft.malevolent_shrine",
  "description": "domain.jjkcraft.malevolent_shrine.desc",
  "grade": "jjkcraft:grade_1",
  "cast_cost": 140,
  "upkeep": 20,
  "radius": 12,
  "clash_priority": 2,
  "behaviors": [
    { "type": "jjkcraft:sure_hit_damage", "interval": 10, "damage": 1.5 },
    { "type": "jjkcraft:caster_attribute", "attribute": "minecraft:attack_speed", "amount": 0.5 }
  ],
  "visuals": { "color": "#E0303F", "secondary": "#F2C14E", "dome": "#2A0A10", "ambience": "blades" }
}
```

## Vow (`jjkcraft/vow`)

Synced to clients (the Vow Builder lists them). A vow is one restriction a sorcerer can swear
to; a Binding Vow combines one to three of them with a focus and a term (chosen in the Vow
Builder, not in data). Changes need a world restart.

| Field | Type | Default | Meaning |
|---|---|---|---|
| `name` | string | required | Translation key of the name |
| `description` | string | required | Translation key of the description |
| `restriction` | one of the restrictions below | required | What the sorcerer gives up |
| `weight` | 0.5 to 10 | required | How much the restriction is worth: a vow's points are `(sum of weights)^0.85` times the term factor |

Restrictions. Breakable ones end the vow the moment they are broken (with a CE drain and a regen
debt); enforced ones simply refuse the action, so they can't be broken.

| Restriction | Kind | Holds that |
|---|---|---|
| `no_jump` | breakable | the sorcerer never jumps |
| `no_sprint` | breakable | the sorcerer never sprints |
| `no_armor` | breakable | no armor is worn |
| `no_shield` | breakable | no shield is raised |
| `innate_only` | enforced | only the innate technique can be cast |
| `no_domain` | enforced | no Domain Expansion |
| `night_only` | enforced | techniques can only be cast at night, or underground where the sky does not reach |
| `halved_reserve` | enforced | max CE is halved |

Example (`data/jjkcraft/jjkcraft/vow/grounded_oath.json`):

```json
{
  "name": "vow.jjkcraft.grounded_oath",
  "description": "vow.jjkcraft.grounded_oath.desc",
  "restriction": "no_jump",
  "weight": 3
}
```

Terms and foci are fixed in code (see `docs/BALANCE.md` 6): Skirmish (5 min, x0.8 points), Day
(one day, x1.0), Binding (until broken or renounced, x1.25); Power (technique and melee damage),
Efficiency (cheaper techniques), Reserve (larger pool, faster regen).

## Culling Game rule (`jjkcraft/culling_rule`)

Synced to clients (the map lists rules and the negotiation page offers them). A rule is a set of
clauses: when a trigger matches an event, its effects run. Triggers and effects are a fixed,
validated set implemented in code; rules only combine them. Changes need a world restart.

The shipped rules have their own titles but follow the canon rules where one exists (Jujutsu
Kaisen Wiki, "Culling Game", checked 2026-10-18):

| Rule | Canon rule | Difference |
|---|---|---|
| `entry_window` | 1 and 2: declare participation within 19 days of awakening, or lose the technique | none |
| `one_way_entry` | not a numbered rule: the barriers hold players because they entered of their own free will | none in effect |
| `sorcerer_bounty`, `ordinary_bounty` | 4 and 5: points for ending players; 5 for a sorcerer, 1 for a non-sorcerer | none |
| `game_master` | 6 and 7: 100 points buy a negotiated rule; the game master must accept one without a long-lasting effect | the mod refuses rules that conflict with one in force |
| `stagnation` | 8: a score unchanged for 19 days means cursed technique removal | the mod halves cursed energy until the score rises (Phase 6 revisits this) |
| `open_tally` | 9 (added by Hajime Kashimo): players can see each other's information | a base rule here, paid per look |
| `shared_ledger` | 10 (added by Hiromi Higuruma): points can be transferred | none |
| `substitution` | 11 (added by Megumi Fushiguro): 100 points to leave by inviting a substitute | the mod moves the player to another colony instead of out of the game |
| the rest | none: original rules | |

| Field | Type | Default | Meaning |
|---|---|---|---|
| `text` | string | required | Translation key of the rule's wording. `<text>.name` is its short name |
| `text_args` | list of strings | [] | Arguments for the wording: literals, or `config:entry_days`, `config:sorcerer_points`, `config:non_sorcerer_points`, `config:rule_cost` for the server's live values |
| `base` | bool | false | In force from the start of every game; can't be negotiated |
| `cost` | int | the server's `ruleCost` (100) | Points a participant pays the Kogane to add it |
| `duration_days` | int | 0 | An added rule lapses after this many game days (0: lasts the whole game) |
| `order` | int | 100 | Position in the rule list |
| `conflicts` | list of ids | [] | The Kogane refuses this rule while any of these is in force (and the other way round) |
| `clauses` | list of 1 to 16 | required | `{ "when": trigger, "then": [effects] }` |
| `warden_edict` | `steel`, `distance` or `energy` | none | While in force, a Culling Warden inside a colony is weak only to this (the newest such rule wins) |
| `binds_warden` | bool | false | While in force, a Culling Warden inside a colony can't drop below 1 health |

### Triggers (`when.type`)

| Type | Fields (default) | Fires when |
|---|---|---|
| `jjkcraft:kill` | `victim` (`participant`; also `any`, `sorcerer`, `non_sorcerer`, `curse`, `leader`), `cause` (`any`, `melee`, `projectile`, `technique`), `min_distance` (0), `night_only` (false) | a participant ends a life inside their colony; the actor is the killer |
| `jjkcraft:enter` | none | someone who is not a participant or resident steps inside a colony |
| `jjkcraft:leave` | none | a participant is outside their colony's barrier |
| `jjkcraft:deadline` | none | an awakened sorcerer's entry window closes before they join |
| `jjkcraft:idle` | `days` | a new day begins and the participant has not scored for at least `days` days |
| `jjkcraft:timer` | `days` (1) | every `days` game days |
| `jjkcraft:points_reached` | `points` | a participant's points rise to `points` or past it |
| `jjkcraft:action` | `action`: `transfer`, `reveal`, `substitute`, `withdraw`, `negotiate` | a participant asks for that action; without such a rule in force, the request is refused |
| `jjkcraft:added` | none | this rule was just added |

### Effects (`then[].type`)

| Type | Fields (default) | Does |
|---|---|---|
| `jjkcraft:award_points` | `scale` (`flat`; also `sorcerer`, `non_sorcerer`, `victim`, `curse`), `amount` (0), `multiplier` (1) | gives the actor `(scale value + amount) * multiplier` points |
| `jjkcraft:deduct_points` | `scale` (`flat`), `amount` (0) | takes that many (never below 0) |
| `jjkcraft:join` | none | the actor becomes a participant of the colony they entered |
| `jjkcraft:confine` | none | the actor is held inside their colony (pushed back, or brought back from afar) |
| `jjkcraft:remove_technique` | none | the actor loses their innate technique (off when `missedEntryPenalty` is `NONE`) |
| `jjkcraft:transfer_points` | `step` (1) | moves the requested amount, in whole steps, from the actor to the chosen participant |
| `jjkcraft:reveal` | `cost` (1), `seconds` (60) | the actor pays and learns the chosen participant's name, colony, score and position |
| `jjkcraft:substitute` | `cost` (30) | the actor pays and moves to the chosen colony; a new entrant is owed to the old one |
| `jjkcraft:withdraw` | `cost` (100) | the actor pays and leaves the game for good |
| `jjkcraft:cap_energy` | `fraction` (0.5) | the actor's max CE is capped until they score again |
| `jjkcraft:reveal_leader` | `seconds` (120) | everyone is told where the leader stands, and the map shows them |
| `jjkcraft:summon_warden` | none | a Culling Warden is sent into the colony |
| `jjkcraft:message` | `key`, `broadcast` (false) | a message to the actor (or every participant); arguments: actor name, colony, points |
| `jjkcraft:negotiate` | none | the Kogane hears the actor's proposal: validates it, takes the cost and adds the rule |

Example (`data/jjkcraft/jjkcraft/culling_rule/archer_law.json`):

```json
{
  "text": "culling_rule.jjkcraft.archer_law",
  "duration_days": 2,
  "order": 27,
  "warden_edict": "distance",
  "clauses": [
    {
      "when": { "type": "jjkcraft:kill", "victim": "participant", "cause": "projectile", "min_distance": 8 },
      "then": [ { "type": "jjkcraft:award_points", "amount": 1 } ]
    }
  ]
}
```

Colonies, participants, points and the rules in force are world state, saved with the world
(`data/jjkcraft/culling_game.dat` in the world folder), not data files.

## Cursed tool (`jjkcraft/cursed_tool`)

Synced to clients (tooltips read it). The tool items are code; each entry gives one of them its
grade, what it lends and what its blows do, so a datapack can retune or repurpose a tool. Changes
need a world restart.

| Field | Type | Default | Meaning |
|---|---|---|---|
| `item` | id | required | The item it describes (one of the mod's cursed tools) |
| `kind` | `weapon`, `cursed_object`, `flight`, `ledger` (`vessel`, the name before 1.1.0, is still read) | required | What sort of tool |
| `grade` | id | required | Lowest grade that can use its power (a weapon still hits below it, but lends nothing) |
| `technique` | id | none | Weapon: lent while held. Vessel: granted when swallowed (none: a random technique the eater lacks) |
| `cost_multiplier` | 0.1 to 10 | 1 | Weapon: multiplier on the lent technique's CE cost while held |
| `on_hit` | `none`, `dispel`, `siphon`, `ignite` | `none` | Weapon: dispel ends the target's active technique and drains `amount` CE; siphon moves `amount` CE to the wielder; ignite burns for `amount` seconds |
| `amount` | number | 0 | The on-hit amount; cursed_object: Unsettled ticks; flight: CE per second; ledger: points per entry |
| `medium` | bool | false | Weapon (1.1.0): the tool is a medium. Its lent technique is paid with the tool's durability instead of CE, so a Heavenly Restriction user can cast it too |
| `durability_per_ce` | number 0 to 100 | 0.1 | Medium: durability lost per point of CE the cast would cost (rounded up, at least 1) |
| `description` | string | required | Translation key of the tooltip line |

Example (`data/jjkcraft/jjkcraft/cursed_tool/debt_cleaver.json`):

```json
{
  "item": "jjkcraft:debt_cleaver",
  "kind": "weapon",
  "grade": "jjkcraft:grade_3",
  "technique": "jjkcraft:cleave",
  "on_hit": "siphon",
  "amount": 6,
  "description": "item.jjkcraft.debt_cleaver.desc"
}
```

A medium's durability comes from the wielder's grade (`tool_durability` on the grade). When a
sorcerer of another grade uses it, the maximum is set to their grade's value and the share already
worn is kept.

## Modulo expansion (`jjkcraft/modulo_mission`, `modulo_site`, `modulo_boss`)

Server only, added in 1.1.0 (docs/MODULO.md). The mod's own entries are in the `modulo` namespace
(`data/modulo/jjkcraft/...`). Text fields are translation keys. A datapack can add missions, sites
and bosses; a boss's `behavior` and a site's `layout` must be ones the code knows (listed below),
because those are code.

### Mission (`jjkcraft/modulo_mission`)

| Field | Type | Default | Meaning |
|---|---|---|---|
| `order` | int 1 to 1000 | required | Position in the story. Missions run one after another in this order |
| `title` | string | required | Name on the HUD and in messages |
| `summary` | string | required | One line on what happens |
| `chapters` | string | "" | The manga chapters it adapts (for reference only) |
| `stages` | list of stage, 1 to 64 | required | What the player does, in order |
| `rewards` | rewards | none | Given when the last stage is done |

Rewards: `items` (list of `{item, count}`), `grade_xp` (int), `mul_energy` (number),
`companion` (bool: Maru joins the player), `advancement` (id; every criterion is awarded).

Every stage has `type` (below), `objective` (translation key on the tracker), `site` (optional
id of a `modulo_site`), `intro` (lines said when the stage starts), `spawns` (list of
`{entity, count 1 to 32, radius 1 to 64 (default 10)}`, kept at the site while a player on the
stage is there) and `when` (optional `{flag, value}`: only players whose choice matches play the
stage, others skip it). A line is `{speaker, text}`: a character id from the list below or
`narrator`, and a translation key.

| `type` | Extra fields | Done when |
|---|---|---|
| `talk` | `npc` (character), `lines` (1 to 32) | the player has heard every line (one per use on the character) |
| `travel` | `marker` (optional), `radius` (1 to 64, default 6) | the player is within `radius` of the site's marker (or centre) |
| `defeat` | `boss` (id of a `modulo_boss`), `allies` (characters who fight beside them) | the boss is beaten; every player who fought it moves on |
| `deliver` | `item`, `count` (1 to 64), `npc`, `lines` | the player uses the character while carrying the items, which are taken |
| `make_mul_energy` | `amount` (1 to 100000) | the player has made that much Mul energy since the stage began |
| `defend` | `seconds` (5 to 3600), `boss` (optional), `protect` (optional character) | the player stayed at the site for the time, or the scene boss says it is over |
| `choice` | `npc`, `flag`, `options` (2 to 4) | the player picks an option on the choice screen; it is saved under `flag` |
| `sequence` | `sequence` (`epilogue`), `seconds` (1 to 600) | the client-side sequence has played for the time |

Characters: `maru`, `cross`, `yuka`, `tsurugi`, `usami`, `mino`, `kyoko_tomoe`, `osada`,
`masayoshi`, `jabaloma`, `dabura`. Each needs `npc.jjkcraft.modulo.<id>` and
`npc.jjkcraft.modulo.<id>.idle` in the lang file and a skin at
`assets/jjkcraft/textures/entity/modulo/<id>.png`. An option needs
`option.jjkcraft.modulo.<flag>.<option>`.

### Site (`jjkcraft/modulo_site`)

| Field | Type | Default | Meaning |
|---|---|---|---|
| `dimension` | dimension id | required | `modulo:naunax` or `modulo:tokyo_2086` (or any loaded level) |
| `center` | [x, y, z] | required | Centre, at standing height |
| `radius` | int 4 to 96 | required | Build size, and the area that counts as "at the site" |
| `layout` | one of `deck_hall`, `mul_core`, `meeting_room`, `plaza`, `alley`, `warehouse`, `river`, `school`, `beach`, `ruins`, `arena`, `open` | required | What the builder raises there the first time a player comes near |
| `name` | string | required | Translation key of its name on the HUD |
| `npcs` | list of `{character, offset}` | [] | Characters standing there while a player is near |
| `markers` | map of name to [dx, dy, dz] | {} | Named points by offset from the centre. `arrival` is where travel lands; `boss` is where a stage's boss appears; `gate_earth`, `gate_tokyo` and `gate_ship` place gate pads |

Sites in the same dimension must not overlap (a unit test checks the shipped ones). Each site is
built once per world; the world remembers which (`data/jjkcraft/modulo.dat` in the world folder).

### Boss (`jjkcraft/modulo_boss`)

| Field | Type | Default | Meaning |
|---|---|---|---|
| `behavior` | one of `trafficker`, `toranosuke`, `masaki_takeda`, `maru_duel`, `dabura`, `mahoraga_scene`, `minion` | required | The fight's code (docs/MODULO.md 3.8) |
| `name` | string | required | Translation key on the boss bar |
| `skin` | string | "" | Texture at `assets/jjkcraft/textures/entity/modulo/<skin>.png` |
| `health` | number 1 to 100000 | required | Health against one fighter; each extra fighter adds `modulo.bossHealthPerExtraPlayer` of it |
| `armor` | number 0 to 30 | 0 | Armor |
| `damage` | number 0 to 1000 | 4 | Melee damage |
| `speed` | number 0 to 2 | 0.3 | Movement speed |
| `scale` | number 0.25 to 4 | 1 | Size |
| `color` | `pink`, `blue`, `red`, `green`, `yellow`, `purple`, `white` | `red` | Boss bar colour |
| `adaptation` | adaptation | none | Mahoraga's rule (below) |
| `params` | map of name to number | {} | Numbers the behavior reads (for example Dabura's beam damage) |

Adaptation: `key` (`technique`: the technique that dealt the damage, or the damage type for
anything else; or `damage_type`), `first` (resistance after the first hit of a kind, 0 to 1),
`per_hit` (added by each further hit), `max` (cap), `full_adaptation` (resistance at which the
kind counts as fully adapted, default 0.9).

Health above vanilla's ceiling of 1024 becomes damage scaling: the bar's maximum stays at 1024 and
damage taken is multiplied by 1024 / health, so the fight lasts as long (BALANCE.md 10).
