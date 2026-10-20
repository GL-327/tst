# Charging and terrain, 1.11.0-preview

Hold your selected-power key (R by default), or a direct skill key in combat mode,
then release to cast. All learned techniques use this path, including utility moves,
summons and personalised expressions. A quick tap releases at 25%; a three-second hold
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

Release starts a channel/toggle; press that power again to stop it. Ongoing effects still
pay their normal upkeep and obey duration limits. The technique wheel performs a quick
tap. Server/AI direct casts retain their explicit authored timing and normal output.

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
