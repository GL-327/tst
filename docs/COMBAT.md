# Cursed Combat — 1.12.0-preview

Y toggles combat. Auto Combat defaults ON: holding M1 chooses coarse attacks or a defensive
response to a visible telegraph. It attacks more slowly and lacks deliberate parry timing,
double-tap specials and escape choices. F7 or Moves -> Bindings toggles the saved preference.

Manual U chains lights; double-U queues an uppercut. I prepares a heavy strike; double-I
selects a sweep instead. Hold Right Bracket (]) to guard, crouch for a low guard, raise it just before contact
for a parry. Left Alt dashes or spends 33 CE for a stagger escape (ten-second cooldown).
Manual input temporarily takes priority over Auto Combat. Every control is rebindable.
Vanilla Q/E/F, hotbar keys and Creative toolbar combinations keep their normal behavior.
The old O block binding migrates once because Minecraft 26.3 uses O for Friends.

Auto commits three of every five accepted attacks and visibly feints the others, with extra
recovery. Aim, reach and defense can lower actual contact below 60%. Its guard cannot
perfectly parry. Manual attacks have no artificial accuracy cap. The contact counter counts
each swing once, even if it strikes several opponents. The updated stance, body motion,
two first-person fists and equipment are described in [the overhaul guide](VISUAL_OVERHAUL.md).

Hostile melee mobs and Culling entrants use the shared frame engine. Curses use species
move sets, distinct jab/heavy/sweep/uppercut/guard clips and reactive defense; weak curses
are less likely to guard than strong ones. Contacts preserve species-specific attacks,
CE drain, enchantments and weapon hooks. All contact decisions remain server-owned.

1.4.0-preview adds body/armor poses synchronized to the server's startup, contact and
recovery windows, with distinct jabs, hooks, backfists, uppercuts, heavy attacks, sweeps
and high/mid/low guards. Observers receive these poses too. First-person hands use matching
transforms; left-handed arm poses mirror. Paid technique casts also use category-specific
gestures, including fields without a CAST particle phase. Animation never decides damage.

Six data-driven moves have startup, active and recovery ticks. The server selects moves
from chain position and player movement, spends CE, performs line-of-sight/reach checks, and
allows at most one hit per target per move. Curse AI uses the same framed attack path.
Guard height, facing, three-tick parries, guard break, stun, dash invulnerability and
combo scaling are server-owned. A capped three-tick position history assists hit checks.
Ratio rewards the 7:3 hit region. Black Flash uses the existing progression hooks.
Weapon/enchantment callbacks are retained; vanilla entity attack is suppressed only
while the player has Cursed Combat enabled. Mining blocks uses vanilla behavior.

This is an initial combat implementation. Per-weapon move sets, full swept hit arcs,
ping-adaptive reconciliation and human PvP balance sessions are not complete. The
server suite checks startup delay, active hit, matching-height parry and a mismatched
height hit. Shared combat arithmetic has unit coverage.
