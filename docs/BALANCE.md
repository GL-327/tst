# Balance

**1.13 preview amendment:** offensive techniques now use a configurable 2.5x
runtime damage multiplier, with a separate Shrine efficiency pass. Strong powers
can defeat weak curses in one hit. The current simulation bounds full output
against the strongest special-grade boss instead of enforcing the historical
same-grade no-one-shot rule below. See `COMBAT_REFINEMENT.md` for charge profiles,
fist coatings and summon changes. Older milestone tables remain historical.

All numbers are defaults. Server operators change them in `config/jjkcraft-server.json5`, and
datapacks change them in grade, technique, curse and domain data. Formulas live in
`com.jjkcraft.core` and are covered by unit tests, which are named in each section.

## 1. Cursed energy (M1)

| Setting | Default | Where |
|---|---|---|
| Base capacity (Grade 4, no growth) | 100 CE | `energy.baseCapacity` |
| Normal regen | 1.5% of max per second | `energy.regenPercentPerSecond` |
| Idle regen | 2.5x normal | `energy.idleRegenMultiplier` |
| Idle delay | 5 s after the last CE action | `energy.idleDelaySeconds` |
| Overexertion trigger | a spend that leaves less than 10% of max | `energy.overexertThresholdPercent` |
| Overexertion window | 3 s with no regen | `energy.overexertSeconds` |
| Respawn CE | 50% of max | `death.respawnEnergyPercent` |

Capacity: `base * grade.ce_multiplier * (1 + 0.03 * output_points)`. Heavenly Restriction sets it to 0.

Regen modifier: `1 + grade.perks.regen_bonus + 0.02 * efficiency_points`, multiplied by any
active hooks (for example a domain that suppresses enemy regen).

| Grade | CE multiplier | Max CE (no growth) | Normal regen/s | Idle regen/s | Empty to full while idle |
|---|---|---|---|---|---|
| Grade 4 | 1.0 | 100 | 1.50 | 3.75 | about 30 s |
| Grade 3 | 1.4 | 140 | 2.21 | 5.51 | about 29 s |
| Semi-Grade 2 | 1.8 | 180 | 2.97 | 7.43 | about 28 s |
| Grade 2 | 2.3 | 230 | 3.97 | 9.92 | about 26 s |
| Semi-Grade 1 | 2.9 | 290 | 5.22 | 13.05 | about 25 s |
| Grade 1 | 3.6 | 360 | 7.02 | 17.55 | about 23 s |
| Special Grade | 5.0 | 500 | 10.88 | 27.19 | about 21 s |

"Empty to full while idle" counts the first 5 s at the normal rate, then the idle rate.

**Why these numbers.** A fight should force a choice between casting now and keeping a reserve.
At normal regen a Grade 4 player gets back about one cheap technique (10 to 15 CE) every 8 s of
combat. A full pool covers 6 to 10 casts. Running dry overexerts the player for 3 s, so dumping
everything has a cost. Idle regen makes the time between fights short, so players are not stuck
waiting. Higher grades refill a little faster in relative terms (regen bonus), but absolute CE
grows much faster than regen, so large reserves still take long to rebuild in combat.

Tests: `EnergyMathTest` (closed form equals tick-by-tick, caps, overexertion only on crossing,
capacity formula, time to reach a value).

## 2. Grades and progression (M2)

| Grade | Rank | XP needed (total) | Min accuracy | Trials | Slots | Domain tier |
|---|---|---|---|---|---|---|
| Grade 4 | 0 | 0 | - | - | 2 | 0 |
| Grade 3 | 1 | 300 | 30% | 5 curses of Grade 4+ | 3 | 0 |
| Semi-Grade 2 | 2 | 900 | 35% | 4 curses of Grade 3+ | 3 | 0 |
| Grade 2 | 3 | 2200 | 40% | 8 curses of Grade 3+, master 1 technique | 4 | 0 |
| Semi-Grade 1 | 4 | 4800 | 45% | 3 curses of Grade 2+, 3 days under a vow | 5 | 0 |
| Grade 1 | 5 | 9000 | 50% | 6 curses of Grade 2+, master 2 techniques, 20 Culling Game points | 6 | 1 |
| Special Grade | 6 | - | - | The Unbound Ritual | 6 | 2 |

Accuracy needs at least 10 casts before it counts. Experience: 10, 25, 50, 90, 150, 240 and 500
per cursed-capable enemy by rank, and 1 per landed cast. Reaching Grade 1 needs roughly 9000
experience, which is on the order of 60 Grade 2 curses or a mix of curses, casts and Culling Game
play, plus the trials. That is "real effort", as the brief asks.

Growth: one point per 200 experience plus two per grade rank, 20 points per path. Effects per
point: Output +3% max CE and +2% technique damage; Control -1.5% CE cost and +0.25 Black Flash
chance; Efficiency +2% regen and -1.5% cooldowns; Domain +3% clash power and -2% domain cost.
Cost and cooldown reductions are capped at 30%.

Heavenly Restriction bonuses at rank r with o Output points: speed +12% + 1.2% r; attack damage
+2 + 0.75 r + 0.5 o; attack speed +20% + 2% r; armor +4 + 0.5 r; toughness +2 + 0.25 r;
knockback resistance +0.25 + 0.03 r; safe fall +3 + 0.5 r; jump +10% + 1% r; step height +0.4.
Heals 1 health per second after 3 s without damage.

Tests: `ProgressionMathTest`, `ProgressionGameTests`.

## 3. Techniques (M3)

Power: `(1 + 0.08 rank) * (1 + 0.02 output) * (1.10 if mastered) * vow`. Cost:
`base * (1 - 0.015 control) * (0.90 if mastered) * vow`, with reductions capped at 30%. Cooldown:
`base * (1 - 0.015 efficiency)`, never below 1 tick.

| Technique | Grade | Cost | Cooldown | Damage (base) | Notes |
|---|---|---|---|---|---|
| Ink-Iron Palm | 4 | 10 | 0.8 s | 7 | Black Flash eligible |
| Grudge Bolt | 4 | 12 | 1.0 s | 6 | 1.6 blocks/tick |
| Cleave | 4 | 18 | 2.0 s | 6, +60% per 100 target CE | 140 degree arc, 5 blocks |
| Grave Chains | 4 | 16 | 5.0 s | 2 | binds 2.5 s |
| Cursed Speech: Don't Move | 4 | 22 | 7.0 s | - | freezes 2 s, 8 block radius |
| Lacquer Ward | 4 | 6 + 3/s | 2.0 s | - | blocks 70% frontal, 0.6 CE per damage blocked |
| Seeking Grudge Bolt | 3 | 18 | 1.5 s | 5 | homing |
| Dismantle | 3 | 16 | 1.8 s | 9 (45% ignores armor) | 12 block line |
| Cursed Technique Lapse: Blue / Cursed Technique Reversal: Red | 3 | 30 each | 6.0 s | 2-3/s; burst 24 | burst when they overlap |
| Cursed Speech: Crumble Away / Rise | 3 | 28 / 20 | 8 / 6 s | 4 / - | |
| Reverse Cursed Technique | 3 | 5 + 16 CE per 4 health | 3.0 s | heals 4/s | channel, 10 s max |
| Infinity | 3 | 8 + 1.5/s | 1.0 s | - | slows incoming projectiles 45% per tick |
| Hollow Purple | 1 | 160 | 30 s | 40, pierces | 1.5 s telegraphed windup |

A Grade 4 sorcerer (100 CE) can open with about 6 to 8 casts before running dry. The palm is the
cheapest repeatable option, and the field burst rewards combining two techniques.
`TechniqueMathTest.maxDamageStaysInBudget` keeps a fresh Grade 4 palm under a player's 20 health
and caps the Grade 1 merge.

## 4. Curses and fear (M4)

| Curse | Grade | Health | Armor | Melee | CE | Techniques | Spawns at fear | Trophy chance |
|---|---|---|---|---|---|---|---|---|
| Hollow Hymnal | 4 | 16 | 0 | 3 | 40 | - | 0+, weight 30, 1-2 | 20% |
| Ledger Crawler | 4 | 18 | 2 | 3 (+15% per mark, max 8) | 30 | - | 0+, weight 25, 1-3 | 20% |
| Wick Moth | 4 | 10 | 0 | 2 (x1.4 in darkness) | 25 | - | 0+, light 4 or less, 2-4 | 15% |
| Pale Stilt | 3 | 34 | 2 | 6 | 80 | - | 15+, weight 12 | 30% |
| Echo Mimic | 3 | 30 | 2 | 5 | 120 | Grudge Bolt, plus one echo | 15+, weight 12 | 30% |
| Salt Lantern | 3 | 26 | 0 | 4 | 60 | - | 20+, weight 10 | 30% |
| Tidewash | 2 | 60 | 4 | 8 | 140 | Grave Chains | 30+, weight 8 | 45% |
| Thorn Matriarch | 2 | 70 | 6 | 7 | 150 | Grave Chains | 30+, weight 6 | 45% |
| Cracked Bell | 2 | 55 | 8 | 6 | 120 | - | 35+, weight 6 | 45% |
| Redacted Saint | 1 | 90 | 4 | 10 (x2 first strike) | 300 | Cursed Speech: Don't Move, Dismantle | 45+, weight 3 | 100% |
| Ash Oni | 1 | 140 | 8 | 14; slam 12 | 250 | Cleave | 50+, weight 3 | 100% |
| Gravebound Gate (boss) | 1 | 300 | 10 | 10; maw 4 per 0.5 s | 400 | Grudge Bolt | 80+, weight 1 | 100% |
| Vow Kin | 1 | 110 | 6 | 9 (+8% per vow point of its quarry) | 220 | Grave Chains | 45+, weight 3 | 100% |
| Paper Tide Gravemind (boss) | Special | 420 | 10 | 12; tide 7 | 600 | Cleave, Grave Chains | 95+, light 4 or less | 100% |
| Ferryman of Unfinished Roads (boss) | Special | 480 | 12 | 13; ferried 8 + Wither | 600 | Seeking Grudge Bolt, Grudge Bolt | 95+, light 4 or less | 100% |
| Culling Warden (boss) | Special | 450 | 14 | 14 | 500 | Cursed Speech: Crumble Away, Dismantle | not natural (eggs, commands; the Culling Game in M7) | 100% |
| Sovereign of Ten Mouths (boss) | Special | 800 | 14 | 16; chorus 5, bite 10 | 800 | Cursed Speech: Don't Move, Kneel, Rise; its own domain | not natural (eggs, commands) | 100% |
| Doorframe Hunter | 4 | 20 | 1 | 3.5 (x1.8 ambush) | 35 | - | 5+, weight 18 | 20% |
| Gilded Mimic | 3 | 32 | 6 | 5; grab 2/s | 70 | - | 15+, light 7 or less, weight 10 | 30% |
| Tithe Burrower | 2 | 50 | 4 | 5, steals a stack | 130 | Grave Chains | 30+, weight 7 | 45% |

Trophy chances need a player kill; Looting adds 2 points per level. Shards: 0-1 (Grade 4), 1-2
(Grade 3), 2-3 (Grade 2), 3-5 (Grade 1), 6-9 (the Gate), 10-14 (Special Grade), 16-24 (the
Sovereign), plus Looting.

Every special attack is telegraphed for at least 0.8 s (the Hymnal's breath 1.2 s, the Lantern's
aim 1.5 s and locked for the last 0.4 s, Tidewash's arm 0.8 s, roots 1.1 s, the bell's swing 1.3 s,
the oni's charge 1.1 s, the gate's call and pull 1.5 s, the Gravemind's tide 1.1 s, the
Sovereign's chorus 1.2 s and bites 1.4 s), and each curse has a counter: hit the Hymnal's mouth,
keep fights near the Ledger short, light up for Wick Moths, stay on the ground near the Stilt
(it hits double and takes half while you are airborne; its five-block snatch, the one untelegraphed
move, only reaches airborne prey), don't do anything you would not want done
back to you near the Echo (it replays your last five actions three seconds later), break line of
sight or raise a shield against the Lantern's beam and hit it hard while it charges, fight Tidewash
on dry ground, fight the Matriarch at dawn or night (she grows from nothing at sunrise to full
strength at dusk, and holds 20% through the night), sense the Saint, step aside from the oni's charge and make it hit a wall (or deal
30 damage quickly) to stagger it, and kill the Gate's summons first (while two stand, it takes 35%
damage).

The special grades each ask for a different skill. The Gravemind punishes repetition: four hits of
one damage type cut that type to 60%, eight to 25%, and it forgets after 30 s without that type.
The Ferryman tests movement: every 30 s (20 s below half health) the crossing drags everyone in
22 blocks toward its shore at 0.06 blocks per tick (0.075 later), so walk against it; reaching
the shore costs 8 damage and Wither. The Warden tests planning: it can't drop below 1 health
while a Rule Seal stands (3 at the start, 2 more at half health), and only the current Edict's
damage type (melee, projectiles or techniques) hurts it properly (1.5x; everything else 0.15x).
The Sovereign is a fight in three acts by its mouth count: chorus (stun and 5 damage in 12
blocks) above 6 mouths, ground bites (10, marked 1.4 s ahead) at 4 to 6, and its own domain at 3
or fewer: bring a Simple Domain or a domain to clash with.

Fear: deaths add 15 (player), 10 (villager) or 2.5 (other); stored fear fades 3 points per
minute. Surroundings add up to 40 for darkness, 12 at night, 10 below sea level and 25 inside
a haunted structure; the total is capped at 100. A curse's spawn weight is
`weight * (1 + (fear - min_fear) / 25)`. Curse CE regen is multiplied by `1 + fear / 50`, and
fear 100 halves the wait between casts. Nests form at fear 75 or more (3% per spawn attempt).

Sensing costs 0.5 CE per second; its range is the grade's `sense_range` (12 at Grade 4, then 16,
20, 24, 32, 40, and 64 at Special Grade).

## 5. Domains (M5)

Cast cost and upkeep: `base * variant * (1 - 0.02 domain_points, at most -40%) * config`. Power:
`variant * (1 + 0.03 domain_points)`. Barrierless: cost x1.6, power x1.3, radius x1.4, clash x1.2.

| Domain | Grade | Cast | CE/s | Radius | Effect at power 1 | Clash priority |
|---|---|---|---|---|---|---|
| Malevolent Shrine | 1 | 140 | 20 | 12 | 1.5 every 0.5 s (3/s), caster +50% attack speed | 2 |
| Unlimited Void | Special | 220 | 28 | 14 | Overloaded 3 s every 4 s, enemy regen 0 | 4 |
| Chimera Shadow Garden | 1 | 150 | 22 | 16 | 4 shadow Divine Dogs (14 health, bite 4), darkness | 1 |
| Idle Death Gamble | 1 | 130 | 18 | 12 | 2 spins/s: damage 3 (30%), heal 3 (15%), +15 CE (20%), stun 1.5 s (10%), whiff (25%) | 1 |
| Authentic Mutual Love | Special | 200 | 24 | 14 | 4 blades at half cost, slowness II | 3 |
| Sevenfold Recitation | 1 | 140 | 16 | 12 | 5 damage and weakness, exactly 7 times, 2 s apart | 2 |

How long a pool holds one (in combat regen, see `DomainMathTest`): a Grade 1 sorcerer (360 CE, 7 CE/s)
holds a Grade 1 domain about 17 s; a Special Grade (500 CE, 10.9 CE/s) holds Unlimited Void about
16 s; a Grade 1 barrierless domain lasts about 5 s. A domain is a decisive, expensive play, and
the 20 s burnout after it means the caster fights on with melee alone for a while.

Sure-hit damage ignores armor, so domains are deadly to players: the cutting field takes a
20-health player in about 7 s and the recitation deals 35 over 14 s. The counters are a Simple
Domain (20 CE, 3 CE/s, 1.5 CE per cancelled hit, Grade 3 and up), a domain of your own (clash),
leaving a barrierless domain, or striking the caster, who is anchored within 1.5 radii.

Clashes: the larger `CE * growth * priority * variant` wins after 2 s. Loser: domain gone, 3 s
stun, 30% of max CE lost, overexerted (config).

## 6. Black Flash, Reverse Cursed Technique and Binding Vows (M6)

### Black Flash

Only strikes marked Black Flash-eligible can become one (the Ink-Iron Palm; more in M8).

`chance = min(35%, (base + grade bonus + 0.25 per Control point + 8 per zone stack) * timing)`

| Input | Value |
|---|---|
| Base (`blackFlashBaseChancePercent`) | 4% |
| Grade bonus | Grade 4: 0, Grade 3: 0.5, Semi-Grade 2: 1, Grade 2: 1.5, Semi-Grade 1: 2, Grade 1: 3, Special: 4 |
| Timing (attack charge) | 1.5x at 90% or more, 1x at 50% or more, 0.5x below |
| Zone | each Black Flash adds a stack (max 2) for 30 s |

Damage: `(1.3 + 0.4 * CE fraction) ^ 2.5` times the strike, so 1.9x with an empty pool to 3.8x
with a full one (`blackFlashExponent`). It also refunds 15% of max CE and knocks back hard. A
Grade 1 sorcerer with 8 Control points and a full swing starts at (4 + 3 + 2) x 1.5 = 13.5%, then
25.5% and 35% (the cap) in the zone. That rewards timed swings and streaks without making Black
Flash the default outcome.

### Reverse Cursed Technique

| Rule | Value |
|---|---|
| Awakening | a lethal blow on an awakened sorcerer of Semi-Grade 1 or higher (`reverseUnlockMinRank` 4) holding at least 35% CE; 35% chance (`reverseUnlockChancePercent`). Survive at 1 health, lose 30% of max CE |
| Healing | below 60% health: 1 health every 0.5 s for 3 CE (6 CE per heart, 2 health per second); stops below 8% CE; toggle with H |
| Emergency save | once every 5 min, a lethal blow leaves 4 health instead, for 40% of max CE |
| Against curses | techniques and Black Flashes deal 1.25x to curses |
| Burnout | domain burnout halved |

Healing 20 health costs 60 CE, about a fifth of a Grade 1 pool, and is slow enough (10 s) that
burst damage still kills. The save is the safety net; the cooldown keeps it from being a second
life in every fight.

### Binding Vows

Points: `(sum of weights)^0.85 * term factor`, with up to 3 restrictions. Terms: Skirmish (5
min, x0.8), Day (one in-game day, x1.0), Binding (until broken or renounced, x1.25).

| Focus | Per point | At 4 points | At 8 points |
|---|---|---|---|
| Power | technique damage +6%, melee +4% | +24%, +16% | +48%, +32% |
| Efficiency | technique cost -5% (floor 60%) | -20% | -40% |
| Reserve | max CE +5%, regen +4% | +20%, +16% | +40%, +32% |

| Vow | Restriction | Weight | Technique bonuses under it |
|---|---|---|---|
| Open Guard | no shield | 1 | - |
| Measured Pace | no sprinting | 2 | - |
| Closed Heart | no Domain Expansion | 2 | - |
| Grounded Oath | no jumping | 3 | Ink-Iron Palm 1.35x damage |
| Night Covenant | techniques only at night or underground | 3 | Grudge Bolt cost 0.7x, range 1.2x |
| Bare Skin | no armor | 4 | Dismantle 1.3x damage |
| Single Path | innate technique only | 4 | - |
| Half Vessel | max CE halved | 5 | - |

Three of the heaviest (Half Vessel, Bare Skin, Single Path; weight 13) on a Binding term make
11.1 points: a Power vow then gives +66% technique damage, at the price of half your CE, no armor
and one technique. Breaking a vow costs `min(50%, 6% per point)` of max CE, a 2 s stun and a
regen debt (regen halved) of one minute per point; renouncing halves all three. Vow Kin hunt vow
holders and hit 8% harder per point, so a heavy vow is also a beacon. Days under a vow count
toward the Semi-Grade 1 trial (3 days).

### The Special Grade path

The Unbound Ritual (the Special Grade trial) completes when a Grade 1 sorcerer who has mastered
their innate technique, holds a Binding Vow and has their domain active kills a special-grade
curse inside that domain. It can't be ground out: every condition is a separate system mastered,
and only the four special-grade curses qualify.

## 7. The Culling Game (M7)

| Setting | Default |
|---|---|
| Colonies, radius, distance from spawn | 4, 64, 400 |
| Entry window after awakening | 19 days (about 6 hours and 20 minutes of play) |
| Sorcerer / non-sorcerer kill | 5 / 1 points |
| Rule cost | 100 points |
| Entrants per colony at the start, per day, at most | 6, 2, 10 (35% sorcerers) |
| Points lost on a player participant's death | 50% |
| End | after 60 days, or when one participant is left standing |

Entrants: non-sorcerers have 24 health, 4 attack, 2 armor and a sword or axe; sorcerers have 32
health, a Grade 3 or Grade 2 pool (150 or 220 CE) and two techniques. A colony's starting six
entrants are worth about 6 + 2 x 5 = 12 points, so the Grade 1 trial's 20 points takes a little
more than one colony's worth of kills, or a couple of days of reinforcements. Buying a rule (100
points) needs about 20 sorcerer kills: a long game's decision, not an early one.

Rule prices in points: Open Tally 1, Substitution 30, Withdrawal 100, any added rule 100. Shared
Ledger moves points in steps of 5. Stagnation caps CE at half after 3 days without scoring. The
Laws of the Blade, Bow and Sorcery last 2 days and add 1 point per matching kill; the Warden's
Mandate binds a Warden for one day. Final grade experience: 20 per point, plus 2000, 1000 and 500
for the top three.

## 8. Techniques, summons and cursed tools (M8)

The new fifteen bring the library to 30 across every category.

| Technique | Category | Grade | Cost | Cooldown | Numbers |
|---|---|---|---|---|---|
| Ten Shadows: Nue | summon | 4 | 20 | 15 s | Nue for 45 s: marks enemies hunting you (glowing 3 s), dives for 3 every 3 s |
| Ink Step | utility | 4 | 10 | 3 s | 6 block dash, 6 ticks untouchable |
| Ten Shadows: Divine Dogs | summon | 3 | 40 | 20 s | the two Divine Dogs for 30 s: 16 health, bite 4 every second after circling 1.3 to 2.3 s |
| Straw Doll Technique: Resonance | restriction | 3 | 26 | 15 s | 3 nails of 5 (pierce) within 64 blocks for 8 CE each, 30 s |
| Idle Transfiguration | utility | 3 | 24 | 12 s | self: scale +30%, armor +6, knockback resistance +0.5, speed -25% for 20 s; target: scale -45%, attack -40%, speed +20% for 10 s |
| Splinter Volley | projectile | 3 | 24 | 3.5 s | 5 bolts of 3 in a 30 degree fan |
| Ten Shadows: Toad | summon | 2 | 55 | 25 s | the Toad for 30 s: 40 health, armor 12, taunts every 2 s, swats projectiles within 3.5 blocks of you |
| Projection Sorcery | utility | 2 | 45 | 18 s | 0.7 s windup; enemies within 8 freeze 0.7 s, you step 6 blocks and gain Speed II 2 s |
| Oathbound Edge | strike | 2 | 22 | 2.5 s | 9 x (1 + 0.1 per vow point), half without a vow; 1.2x under Measured Pace |
| Ground Toll | area | 2 | 30 | 8 s | 8 in 6 blocks, half at the edge, launch 0.6 |
| Ember Skin | passive | 2 | 10 + 2/s | 5 s | melee attackers take 3 and burn 3 s |
| Positive Energy Output | healing | 2 | 24 | 7 s | heals an ally 8 and Regeneration I 5 s; yourself 4.8 |
| Piercing Blood (ultimate) | ultimate | Semi-Grade 1 | 100 | 30 s | 1.5 s windup, 34 along 32 blocks, 70% ignores armor |
| Thousand Lances (ultimate) | ultimate | Grade 1 | 140 | 35 s | 30 lances of 2.5 over 3 s in a 5 block radius, every other one at someone in it |
| Maximum Output: Blue (ultimate) | ultimate | Special | 200 | 45 s | pulls everything in 10 blocks for 2 s, then 48 in 6 blocks (70% ignores armor) |

The ultimates are tuned to the grade that unlocks them: Piercing Blood takes a third to a half
of a Grade 1 curse's health in one line; Thousand Lances puts about 15 of its 30 lances on one
target that stays in the rain (about 50 at base power) and spreads the rest over a crowd; Maximum
Output: Blue is a special grade's answer to a crowd. All three cost a third or more of the
pool and have long cooldowns. Shikigami health scales with power (grade and mastery).

Cursed tools (attack damage as the tooltip shows it, including the hand's 1):

| Tool | Grade | Stats | Power |
|---|---|---|---|
| Split Soul Katana | 1 | 10 attack damage, 1.3 attacks/s | lends Dismantle at 0.75x cost |
| Chain of a Thousand Miles | 2 | 6 attack damage, 1.8 attacks/s, +2 reach | lends Grave Chains at 0.8x cost |
| Inverted Spear of Heaven | 1 | 7 attack damage, 1.4 attacks/s | each hit ends the target's active technique and drains 15 CE |
| Cleaver of Bad Debts | 3 | 9 attack damage, 0.9 attacks/s | each hit moves 6 CE from the target to you; lends Cleave |
| Bonewood Staff | 4 | 2 attack damage, 1.6 attacks/s | lends Grudge Bolt at 0.8x cost |
| Withered Digit | 2 | eaten | a technique you lack; Unsettled 60 s (max CE x0.7); below Grade 2: 4 damage and nausea 10 s |
| Drifting Gourd | 3 | held | flight for 3 CE a second, slow falling 5 s when it ends |
| Tally Ledger | 4 | held | stores Culling Game points 5 at a time, safe from death; needs a transfer rule in force |

Technique scrolls teach their technique to a sorcerer of its grade or higher; sorcerer entrants
drop one of theirs a quarter of the time when a player kills them.

## 9. The full pass (M9)

`BalanceSimulationTest` reads the shipped technique, curse and grade data and checks four
budgets, so a datapack edit to the mod's own data that breaks one fails the build:

1. No ordinary (non-ultimate) technique deals more than 0.9 damage per CE at base power. The most
   efficient are the attract and repel bursts (0.8, but they need two casts) and the palm (0.7).
2. No ultimate, or any technique costing 150 CE or more, can one-shot the weakest curse of its tier
   even fully grown (mastered, 20 Output points). The pass found two: Hollow Purple hit 97
   against the Redacted Saint's 90 (now 40 base, 86 grown) and Thousand Lances' aimed lances at 5
   each (now 2.5, 81 grown).
3. A sorcerer can finish any curse of their own grade, from Grade 4 to Grade 1, on one pool with
   their most CE-efficient technique at base growth. The tightest is the Gravebound Gate (300
   health) against a Grade 1 pool of 360 CE.
4. No ordinary technique one-shots a curse of its own grade.

Special-grade curses are deliberately outside budget 3: 420 to 800 health against a 500 CE pool
means a special grade alone needs melee, Black Flash, a domain or help.

Time to kill, at base growth with the grade's best repeatable technique (from the same numbers):
a Grade 4 sorcerer finishes a Hollow Hymnal in 3 palms (30 CE, about 2 s); a Grade 2 a Cracked
Bell in 6 Dismantles (96 CE, about 10 s); a Grade 1 an Ash Oni with Hollow Purple and 7
or 8 Dismantles (about 290 CE of 360, about 16 s with the cooldowns). Curses hit an unarmored 20-health
player for 2 to 3.5 (Grade 4), 4 to 6 (Grade 3), 5 to 8 (Grade 2) and 9 to 14 (Grade 1), so a
player two grades below a curse dies in two to four hits: grade matters, as the brief wants.

## 10. The Modulo expansion (1.1.0)

All of this is off unless `modulo.enabled` is on (docs/MODULO.md). Numbers come from the config,
the shipped data and the constants named below; `ModuloMathTest` checks the formulas and
`BalanceSimulationTest` checks the Mul budget.

### Mul energy

| What | Number | Where |
|---|---|---|
| Ratio | 0.5 Mul energy per CE poured | `modulo.mulEnergyRatio` |
| Pool | 200 at most | `modulo.maxMulEnergy` |
| Pouring | 20 CE per second while holding use on a piece, in quarter-second steps | `MulItem.RATE` |
| A piece of Mul | takes 40 CE (20 Mul energy), then crumbles | `ModuloItems.MUL` |
| High-purity Mul | takes 160 CE (80 Mul energy) | `ModuloItems.HIGH_PURITY_MUL` |
| Heavenly Restriction | pours nothing (no CE to pour) | `MulEnergy.pour` |

A Grade 1 pool of 360 CE fills the 200 Mul pool from 400 CE, so about one full pool and a tenth
of CE, over 20 seconds of pouring. Mul energy is kept through death, like CE growth.

| Spent on | Cost | Gives |
|---|---|---|
| Mul Lance (Rumelian Blade's technique) | 25 Mul, no CE, 1.5 s cooldown | a piercing bolt for 10, about 0.4 damage per Mul against one target |
| Gravity Harness | 2 Mul per second while worn and airborne | slow falling and a higher jump |
| Mul Ward | 2 Mul per point of damage it takes | takes half of each hit while Mul lasts |
| Naunax Beacon | 30 Mul | back to the ship's arrival hall from anywhere |

Budget: no technique paid in Mul deals more than 1.8 damage per Mul at base power
(`mulPaidTechniquesStayUnderTheirCeiling`). Since a point of Mul costs two of CE, that keeps Mul
techniques at or under the 0.9 damage per CE of ordinary techniques (section 9). They are left out
of the per-CE budget because their CE cost is 0.

### Mediums

A cursed tool marked `medium` pays its lent technique with durability: 0.1 per point of CE the
cast would cost, rounded up, at least 1. Its maximum durability is the wielder's grade's
`tool_durability`:

| Grade | 4 | 3 | Semi-2 | 2 | Semi-1 | 1 | Special |
|---|---|---|---|---|---|---|---|
| Durability | 96 | 160 | 224 | 288 | 352 | 448 | 640 |

So Honoyagi's Batto Sword Drawing (30 CE) wears 3 a cast, about 96 casts for a Grade 2; the
Split Soul Katana's Dismantle (12 CE after the katana's 0.75) wears 2, about 224 casts for a
Grade 1. A Heavenly Restriction user can cast through a medium; that is the point of it.

### Bosses

Health is for one fighter; each extra player who fights adds 60% (`modulo.bossHealthPerExtraPlayer`),
recounted while the fight goes on, keeping the share already lost. Above vanilla's ceiling of 1024
the bar stays at 1024 and damage taken is scaled by 1024 / health, so four players against Dabura
(520 x 2.8 = 1456) need the same total damage as the formula says.

| Boss | Health | Armor | Melee | Notes |
|---|---|---|---|---|
| Trafficker | 90 | 4 | 6 | blinks every 5 s, hammer 7 |
| Toranosuke | 110 | 4 | 7 | roars 0.8 s, then pounces for 8 every 4.5 s |
| Masaki Takeda | 170 | 6 | 6 | needles for 5 after a 0.7 s tell; hides in the Skin Guardian (60) below half, 3 hits tear him out |
| Maru (duel) | 240 | 10 | 8 | gravity flips for 4 s every 11 s after a 2 s tell; yields at 15% |
| Dabura | 520 | 14 | 12 | Light beam 9 every 3.5 s after a 1 s tell; Hard Light pillars below 75%; Reverse Transcendence below 50%; Darkness 22 in 18 blocks below 30%, halved by Simple Domain; yields at 5% |
| Mahoraga | 400 | 12 | 14 | adapts (below); fights on the players' side |

Mahoraga's adaptation, per kind of damage: 20% resistance after the first hit, 10% more per hit,
95% at most. A kind counts as fully adapted at 90%, the eighth hit; that ends the Dabura scene.

### Rewards

The eleven missions give 300 to 1500 grade experience each (6,000 in all; reaching Grade 1, which
the Descent asks for, takes 9,000), Mul and gear along the way, Honoyagi and the Rumelian Blade as
the two new tools, and the epilogue's Yuji Itadori's Cursed Object and advancement at the end.
