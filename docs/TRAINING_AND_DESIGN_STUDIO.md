# Physical training and the visual design studio — 1.17

## Train in the world

The **Practice** page is now a training journal. Survival players learn and improve
through actions; spending experience levels or clicking growth buttons cannot bypass
that work. Existing learned skills, mastery and growth remain. Energy output, RCT output
and automatic-healing preferences remain editable under **Energy / RCT settings**.

Craft and place these original props:

- **Training Straw Doll:** hay block above a centre hay block, sticks on each side of
  that middle hay block, and one oak plank below. Use the item on a clear floor. Strike
  it with fists, weapons or techniques. Sneak-use the doll to pack it up. It gives real
  hit reactions and can survive practice without generating kill loot or kill experience.
- **Breathing Mat:** three red wool over three wheat. Stand on it, hold Sneak for
  **2–5 seconds**, then release while still. A completed breath costs 5 CE and trains
  reserves and efficiency. Long AFK holds and interrupted breaths give no progress.
- **Reverse-flow Focus Stone:** an amethyst shard over three stone. Learn reinforcement
  first, then use the stone while grounded and remain still. Complete three ten-second
  drills, costing 30 CE each. Physical drills do not purchase skills with experience.
- **Balance Beam:** three oak planks above a centre stick (crafts two). Walk the narrow
  top to train agility. Ordinary grounded sprinting also trains agility and endurance;
  teleports, passengers, creative flight and possession cannot generate training work.

Fist contacts build strength. Weapon contacts build weapon skill. Both build endurance;
CE-paid contact drills teach reinforcement after 40 control work. Accurate technique
contact trains output/control and retains the technique's existing mastery progression.
Sustaining a real domain for at least four seconds contributes domain practice.

Physical levels cap at 20. At that cap, strength adds 2.4 attack damage, endurance adds
eight health and forty stamina, agility adds 6% movement speed, and weapon skill reduces
weapon recovery by up to four ticks. Gains slow as levels rise. Growth practice assigns
available grade-growth points to the path actually practised; it does not create free
grade points. Every ten completed breaths adds a reserve-training increment. Smaller CE
pools retain their larger training gains. Ordinary death preserves physical work; the
Divine Chalice preserves it while rerolling techniques. A wholly new character resets it.

## Make the UI your own without drawing

Press the rebindable **F6** design-studio key in the world or on a mod screen. Opening
it from a screen captures its real controls and labels. The page selector covers every
mod screen, including awakening, soul choices, family abilities, creative editing,
domains, projection and fusion. **Open page** opens a selected page; press F6 there to
capture its current state. Event-only dialogue can be edited when that event is shown.

- Drag an existing control or label on the canvas. Drag its lower-right corner to resize.
  Positions snap to a grid. Controls retain their original actions and server checks.
- Add panels, labels, bars and dividers from the ready-made parts. Use the inspector for
  exact positions, sizes and custom label text. **Tint** cycles a restrained colour palette.
- **Theme** cycles Midnight, Vermilion and Jade. The shared clean panels, buttons and
  typography apply throughout the mod, including HUD panels. Technique accent colours
  remain tied to the current technique.
- Select **HUD** to move and resize the actual HUD elements already drawn in the world.
- **Undo** restores a prior edit. **Reset page** restores that page's original arrangement.
  **Delete** hides the selected element. The editor's own controls cannot be hidden.
- **Save** writes `config/jjkcraft-design.json` in the running Minecraft profile.
  **Copy** puts the whole design in the clipboard; **Import** reads a copied design.
  Imports validate their size, page names, part types and bounds before changing anything.

This is a visual editor for layout and appearance. Adding a visual bar or label does not
invent a new gameplay action. A moved real button still performs its original action.

## Build training props from blocks

In the studio choose **Props**, then select the doll, mat, focus stone or beam. The
isometric preview uses the same block dimensions as the exported model. Click a block
or use **Next block**, add/duplicate blocks, drag the selected block in X/Y, and edit Z and
dimensions in the inspector. A unit is 1/16 of a Minecraft block. Change **Material**
to use straw, wood, cloth, binding red, stone or other original pixel materials.

**Apply prop** writes and enables `resourcepacks/jjkcraft-training-design.zip`, then
reloads resources. The export contains the actual GeckoLib doll geometry and actual
block models. It also includes the design JSON for sharing. Saved world objects and
server hitboxes stay unchanged: these are client visual designs. Models are limited to
48 blocks per prop and bounded coordinates. **Reset prop**, followed by Apply, restores
the original model. The resource pack can also be disabled in Minecraft's pack menu.

## Weapons and curse encounters

Blades, polearms and heavy weapons use separate server timings and hand-aware slashes,
returns, thrusts and heavy attacks. Their actual held models animate in first person;
third-person poses follow the same strike. Weapons keep their real attributes,
enchantments, durability and cursed-tool effects. Empty offhands cannot borrow main-hand
weapon damage. Ordinary held items have weaker improvised swings; consuming an item
prevents attacking. Unarmed alternating fists retain their quicker two-hand cadence.

Fifteen cursed weapons have separate crisp inventory art and held material atlases,
with rebuilt held models. Other custom item icons and worn armour have consistent pixel
density. All 52 curse designs retain their identities, with simpler connected heads,
painted eyelids/pupils/mouths, restrained materials and fewer floating detail cubes.

Natural curse encounters check every twelve seconds, begin 28–52 blocks away, avoid
clear forward-view pop-ins and keep at least 24 blocks from other players. A successful
encounter rests its 64-block region for ninety seconds. Habitat preferences favour wet,
forest and underground designs in suitable places, while repeat species are downweighted.
High grades remain solitary; lower grades form varied small groups. Boss-level encounters
require very high fear. Loaded-chunk checks, population limits, Peaceful, mob-spawn rules
and existing server spawn-rate controls still apply.

## Verification

Server regressions cover physical benefits and caps, real dummy contact, contact-spam
limits, timed breathing and AFK rejection, XP-free reverse-flow learning, weapon selection,
death persistence, invalid training keys and habitat policy. Native client checks exercise
actual training hits, first/third-person weapon rendering, every menu, native designer
dragging, design import/export, live prop-pack reloads and close-ups of every curse.
