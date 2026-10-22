# Two fists, cursed vessels and domain expression — 1.16

## Fighting

With empty hands, **U punches with the physical right hand** and **I with the left**.
Each hand has its own ten-tick recovery. The opposite fist can follow after five ticks,
once the previous hit's active frames finish. Double-tapping the same hand requests
an uppercut; alternating hands does not accidentally request one. **;** is heavy attack
(double-tap for sweep). All bindings can be changed in Minecraft Controls. Existing
default heavy bindings migrate from I to semicolon; custom bindings are kept. E, Q, F and P remain
Minecraft bindings. Weapons retain their main-hand attack and normal recovery.

Auto Combat remains on by default and chooses the next hand. It retains the existing
60% commitment pattern, including feints; contact depends on range, facing and defence.
Manual timing gives faster deliberate combinations. The player's configured Minecraft
Attack binding, including inverted mouse controls, still works.

Punches spend a separate stamina pool: 100 normally, 140 for a Cursed Vessel. Jabs cost
12, hooks/backfists 15, uppercuts/sweeps 24 and heavies 28. Feints cost 5. Stamina recovers
while resting, after a short delay; blocking slows recovery. Refused exhausted attacks
spend no CE. Alternating fists improves cadence without removing resource limits.

The first-person stance sits farther toward the lower corners. Each fist and the
third-person animation use the authoritative striking hand. The usual Minecraft hand
pose returns after the combat stance times out.

## Cursed Vessel and Sukuna's Finger

Cursed Vessel is a physical attribute independent of the innate technique. It can be
rolled at birth or chosen through the existing creative attribute editor. It grants
+2.5 attack damage, +6 maximum health, +8% movement speed and 140 stamina.

Sukuna's Finger is an epic, non-craftable consumable. Special-grade and stronger curses
have a 1-in-120 death drop chance. Creative inventory also contains it. There is a hard
20-finger limit. Eating one makes a non-vessel into a vessel without replacing their
innate technique. Fingers add 6% CE capacity each and unlock Dismantle at 1, Cleave at 5,
the furnace at 10, and Malevolent Shrine at 15 if no other domain is known.

Feeding Sukuna raises incarnation pressure. Injury below 35% health and very low CE
raise it further; staying healthy and resting reduce it. A body born as a vessel handles
its first relic better. Hallucinations mark nearby harmless living entities as apparent
curses, add misleading whispers and a subtle visual disturbance. Real identities,
factions and server targets are not rewritten. Reduced-motion settings disable sway.

At high pressure Sukuna can temporarily take over. The view goes black while a bounded
server controller pursues nearby living targets, punches and uses available Shrine
powers. It may attack innocents and damage terrain through the existing technique and
terrain rules. It respects creative/spectator protection and server PvP rules. Ordinary
movement, combat, interaction and casting packets cannot fight the controller. Menus,
disconnect and server communication remain available. A takeover lasts at most twenty
seconds, then releases control and grants a two-minute suppression interval. Death,
disconnect, server shutdown or dimension changes clear the transient controller. A
client-side deadline also clears the blackout if an ending packet is missed.

Finger count persists through ordinary death. Hallucinations and control do not persist
through a reconnect. A takeover is an in-game character effect.

## Domain workshop and barriers

Open the technique wheel, then choose **Domain workshop**. Percentages range from 50 to
200 for radius, output, duration and closed-barrier strength. Greater settings increase
cast and upkeep costs; the preview shows the expression's costs before server-wide cost
configuration. Open domains ignore the barrier-strength cost. Choose primary/secondary
RGB colours, a personal name and scenery. Blank colours/name/scenery keep the original
art. Changes are saved on the server and apply to the next expansion. All observers
receive the resulting visual expression. An active domain cannot be edited.

Choose closed or open for compatible domains. **Unlimited Void, Deadly Sentencing
(when provided by a data pack), and Idle Death Gamble require a closed barrier.** Open
domains are 40% wider, 30% stronger and 60% more expensive, and targets can leave. The
normal Domain key uses the saved choice; the explicit barrierless shortcut remains
subject to the same server restrictions.

Closed domains confine survival occupants in all three axes, including caster/allies.
The entire body stays inside; collision checks use the last safe position rather than
forcing feet through terrain. Outgoing and incoming direct attacks cannot cross a closed
wall: they damage the wall instead. Punching its visible boundary also damages it.
Walls are weaker from outside. Breaking the wall, ending the domain, exhausting its
caster or killing its caster releases occupants; the fading visual does not keep them
trapped. Creative and spectator players retain their testing freedom.

## Verification

Server regressions cover per-hand recovery, stamina refusal/regeneration, weapon-hand
consistency, finger limits and innate preservation, bounded possession, vertical closed
barriers, release, open-domain escape, domain costs and observer packet roundtrips.
The `vesselCombat` native client focus checks actual U/I input, both hand poses,
ultrawide and small-screen workshop layouts, an authoritative save, hallucination
packets, real autonomous attacks during blackout and restored control afterward.
