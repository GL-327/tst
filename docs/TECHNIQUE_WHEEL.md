# Technique wheel and reverse energy -- 1.12.1-preview

Open **J -> Moves**. The catalogue now includes family selectors alongside individual
moves. Select a family, then an empty or occupied slot, to bind its selector. The Families screen also has a Bind family button for the selected slot. Bind an
individual move in the same way to use it directly. Right-click a slot to clear it;
cleared slots can be rebound. All twelve slots remain available.

Hold **G**, scroll or point at a slot, then release G to select it. This selects without
casting. **R** activates the selected binding. A family binding opens its learned moves
on a second wheel; click a family on the slot wheel to browse it directly. Scroll changes
the highlighted variant. More than twelve variants use additional wheel pages.

In a family wheel, hold left click (or the configured cast key) on a chargeable variant,
then release to fire. Instant moves activate on press. Escape cancels a held charge.
Right-click a variant to pin that specific move into the slot. A family binding stays a
family after ordinary casting. Unlearned moves are omitted; server grade, CE, cooldown,
vow and knowledge checks still apply. Every control can be changed in Minecraft Controls.
The HUD now shows a compact selected-power badge instead of the four-row sidebar.

Mouse bindings work for holding/releasing the slot wheel and for charging variants.
When casting is bound to right click, middle click pins a variant instead. The center
reports remaining cooldown, grade/CE requirements and whether a toggle is active.
Only releasing the input that began a charge fires it; another button/key release cannot.
Melee stagger, stun, death, spectator mode and dimension changes cancel the affected cast.
An unrelated slot edit preserves a charge; removing that move's binding cancels it.

The compact combat panel defaults to the bottom left. The charge meter sits below the
selected-power badge on the right. Configure their visibility, position, scale and opacity
independently in the client HUD settings.

## Limitless

The selector includes Blue, Mini Blue, Blue Punch, a three-point Mini Blue Cluster,
Maximum Blue and Orbit; Red, Mini Red, Red Punch and Maximum Red; Infinity; and Hollow
Purple when learned. Blue/Red punches require an aimed close hit and use physical combat
poses. Mini casts and punches activate immediately. Full Blue/Red and their maximum
versions support held charging; Purple can reach 200%. Each application keeps its own
personalisation, cooldown and binding vows.

Use the All, Blue, Red, Infinity and Purple buttons to narrow the Limitless selector.
These buttons appear when the screen is at least 320 GUI pixels wide; All remains the
default view, and unlearned branches are disabled. Blue/Red punches and Mini Blue Cluster
support independent force tuning. Presets preserve vows, custom appearance and filters.

**Infinity** is a passive toggle with no activation cost. It runs alongside other casts
and held channels. Forsaking RCT through a binding vow disables its free upkeep and positive-energy applications. Its base upkeep is 1.5 CE/second before filter, expression and trait
modifiers. Without learned RCT, natural CE recovery pauses while it is on, so it slowly
uses reserves and drops when they run out. Learned RCT removes its upkeep and recovery
penalty. The default filter protects against contact and projectiles; the workshop can
specialise it. Domain sure-hits and environmental hazards bypass the field; eligible dispelling weapons cut through and cancel it. Death,
disconnect, character reset and dispelling end it; ordinary key release does not.

The workshop's Defaults button restores combined protection. Projectile-only and
contact-only filters are saved independently and use less upkeep than combined protection.

## Projection planner

Click the grid to add up to eight relative destinations. Drag a numbered point to move
it; right-click that point to remove it. Right-click empty space or use Undo to remove
the last point. Click an attack row to cycle its physical action. Save route persists it.

The grid distinguishes floor, raised/lowered steps and unsafe footing. Connected route
segments turn red for blocked paths or insufficient reach, and later blocked stages are
marked. These are previews of current terrain; the server checks the route again when
casting. Slabs/stairs use their actual collision height. Crouch or combat interruption
ends execution. The terrain preset selects safe approaches to an enemy; without one it
performs movement only.
The preview also follows your saved aim-yaw personalisation, matching the actual route.

## Learning and using RCT

Learn reinforcement in Practice for three XP levels. Then complete three reverse-flow
drills, each taking ten seconds of stillness, 30 CE and three XP levels. Start each drill
in Practice; its progress bar stays visible after the menu closes. Stand on the ground.
Movement, taking damage, using/casting powers or crouching interrupts the exercise.
Interrupted drills retain the CE already spent but do not spend XP or award a lesson.
Completed lessons persist through ordinary death and Divine Chalice rerolls. The rare near-death awakening remains.

Completion learns RCT with automatic healing initially off. **H** toggles healing;
Practice adjusts healing output and health threshold. Learning also makes three optional
applications available without changing your existing bindings: held self-healing,
Mending Output for allies, and **Positive-Energy Discharge**. Bind the discharge like any
other move. It damages cursed spirits along a short aimed line, including spirit players,
while leaving living non-curse targets unharmed. CE, range, output and vows still apply.

Creative character settings can set learned RCT and automatic healing independently.
Choosing the maximum preset still learns it with automatic healing off.
