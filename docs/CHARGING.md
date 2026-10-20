# Charging and terrain, 1.11.1-preview

Only suitable moves support charging. Tooltips and the loadout label each move's casting
style. Charging applies to eighteen moves: Lapse Blue, Reversal Red, Hollow Purple,
Maximum Output Blue, Piercing Blood, Maximum Uzumaki, Granite Blast, Granite Cannon,
Furnace, Perfect Sphere, Ice Lance, Icefall, Water Lance, Mul Lance, Darkness Nova,
Chaos Fracture, Eye Blast and Volcanic Burst.

For these moves, hold your selected-power key (R by default), or a direct skill key in
combat mode, then release to cast. A quick tap releases at 25%; a three-second hold
reaches 100%. Hollow Purple can continue to 200% after seven seconds. These percentages
multiply your personalised output, grade and vows. Charging also scales supported area,
reach, force and duration; it does not change which ability you equipped.

The meter shows percentage, maximum and current CE drain per second. The normal cast
cost is paid on press; every server tick of holding costs additional CE. Natural CE
regeneration pauses while charging, including for high-capacity players. Higher output
is increasingly expensive. Holding at maximum keeps draining CE at an accelerating rate.
Six Eyes reduces ongoing charge drain by 85%, in addition to its existing base-cost
discount. The modifier applies to every family when the player has that attribute.
Existing windup is reduced by time already spent charging, so it is not counted twice.

Opening a screen, unbinding/changing the expression, stun, death, changing dimension,
disconnecting or exhausting CE cancels the charge. Spent energy is not refunded and no
power fires. One charge per player is allowed. Changing power bank does not change the
slot attached to a held key. A charge has a twenty-second safety timeout.

Other moves activate on press at their normal output, with any authored windup. Summons,
fusions, swaps, movement, melee hits, body changes and utility skills do not require charging.
Infinity and other ordinary toggles switch on/off with a press. Healing and guard channels
start on press, sustain while held, and stop on release or when opening a menu.
Chargeable lasting moves, such as Maximum Output Blue, start on release; another press
stops them. Ongoing effects still pay upkeep and obey duration limits.
The wheel performs a quick tap for chargeable moves and normal activation for other moves.
Server/AI direct casts retain their explicit authored timing and normal output.

Charging is an authored `cast.chargeable` opt-in, defaulting to false for old/new datapack
moves that omit it. Personalisation preserves this flag. After rerunning content generators,
run `python tools/charge_profiles.py`; `--check` audits the curated roster without writing.

Blue and Maximum Output Blue tear nearby destructible blocks out of the world and pull
visible block debris into the core. Purple carves through its flight corridor and stops
at remaining solid obstructions. Destructive projectiles, slashes, shockwaves and field
collisions also affect terrain. Healing, body transfers and other non-destructive utility
powers do not automatically explode the surroundings.

Destroyed blocks are consumed, without item drops. Debris cannot place blocks, drop loot
or persist in saves. Containers, block entities, unbreakable blocks, fluids, mod barriers
and sufficiently hard materials are preserved. Player casts respect spawn/world-border
protection and Fabric block-break vetoes; Adventure/Spectator casts cannot destroy blocks.
Mob casts obey `mobGriefing`. Servers can set `casting.terrainDamage=false` in
`config/jjkcraft-server.json5`. Destruction is bounded to a six-block effect radius,
384 blocks per cast, 256 block changes and 12,000 candidate scans per dimension per tick,
with at most 192 live debris displays across dimensions. This keeps large personalised
effects from doing unbounded terrain work.
