> Current 1.9.0-preview: shared panels expand to 560 × 320 within the available GUI viewport.
> Moves includes a family/ability browser, inline move details and twelve slots. Personalize has
> a direct ten-section chooser. Creative has seven draft workspaces with acknowledged server saves,
> optional Auto Calamity and exact character settings. See ATELIER.md for the current workflow.
> The prior milestone descriptions below remain historical.

> Current 1.8.0-preview: twelve slots in the loadout and wheel, three compact HUD banks,
> ten personalization pages including two vow pages, and default-on Auto Combat with
> configurable manual controls. Vanilla Q/E/F and hotbar keys keep their actions. Modulo
> story UI stays disabled. Older milestone descriptions below are historical; PLAYTEST.md
> documents the current controls.

# UI specification

Every HUD element and screen in Jujutsu Craft, what it shows, where it sits, and how the options
change it. All of it is drawn with the shared "ink" style (`client/ui`: `Ink`, `InkButton`,
`Palette`): dark ink panels with a ragged accent rim, the mod's own colour palette, and text with
a drop shadow. Meaning never rests on colour alone: state is also carried by shape, a tag or text.

## 1. Options that affect every element

Client config (`config/jjkcraft-client.json5`, also in Mod Menu through Cloth Config):

| Option | Default | Effect |
|---|---|---|
| `hud.<element>.visible` | true | hide one element |
| `hud.<element>.anchor` | per element | one of nine anchors (corners, edge centres, centre) |
| `hud.<element>.offsetX`, `offsetY` | per element | pixels from the anchor |
| `hud.<element>.scalePercent` | 100 | 50 to 300 |
| `hud.<element>.opacityPercent` | 100 | 10 to 100 |
| `hud.showNumbers` | true | numbers on bars (for example 87 / 100) |
| `accessibility.uiScalePercent` | 100 | 75 to 200, multiplies every element and screen |
| `accessibility.reducedMotion` | false | no shakes, no animated HUD motion, instant screen transitions |
| `accessibility.reduceFlashing` | false | flashes and strobes become steady tints |
| `accessibility.colorblindPalette` | false | swaps the palette for one that stays distinct for common colour vision deficiencies |

Elements anchored at the top centre step below vanilla boss bars when any are showing. The
top-left status column is 124 px wide: CE bar, grade badge, then the combat panel while combat
is on. Event panels (Culling Game, Mul meter, mission tracker, ritual tracker, resonance) are
placed directly under the grade badge and each steps below any column element above it that is
showing, so the column never overlaps and closes up when a panel hides. Configs from before
1.18 that still use an old default position are moved to the new one once (`hudRevision`);
positions a player changed are kept. Every
element hides with the vanilla HUD (F1). With Jade installed, Jade's look-at tooltip moves clear
of any element it would cover (below one in the top half of the screen, above one in the bottom
half), the way Jade moves it below boss bars.

## 2. HUD elements

| Element | Default place | Shows |
|---|---|---|
| CE bar | top left (4, 4) | "CE" with a chevron regen indicator (two while idle-boosted) and the exact value above a full-width segmented bar; danger colours and moving stripes while overexerted |
| Grade badge | top left (4, 28) | grade diamond and name, a thin progress line to the next grade, and state tags: HR (Heavenly Restriction), ZONE (Black Flash zone), RCT (reverse healing), VOW, DEBT, SENSE, SIMPLE (Simple Domain up), UNSETTLED. When the tags would run into the grade name they move to a strip under the badge |
| Combat panel | top left (4, 46), under the grade badge | while Cursed Combat is on: the mode (MANUAL, AUTO with observed contact rate), the current move and combo, or STAGGERED; evade readiness or its cooldown; stamina and guard gauges; active fist coatings. The combat keys are shown once per world as a toast when combat is switched on |
| Culling Game panel | top left, in the column | the game day; your colony and points, or the days left to enter one (red at 3 or fewer); rules in force; a STALLED or KOGANE tag. Shown only while the game runs and concerns you |
| Selected power | bottom right (-4, -4), clear of the hotbar | the selected slot's icon with cooldown sweep and ring, a red wash when CE is too low and a pulse while active; the move's name (without its family prefix), its CE cost or status (cooldown, untamed, stunned), the refusal reason after a failed cast, the cast key, and Infinity's state when known. 118 px wide so it fits right of the hotbar at a 427 px GUI |
| Charge ring | centre, around the crosshair | while a move is held: a ring filling to 100%, a second outer ring for 100–200% (Hollow Purple), the percentage and the CE/s drain |
| Ritual tracker | top left, in the column | a Ten Shadows subjugation: time left, the shikigami, distance against the 48 m limit |
| Resonance | top left, in the column | an original family's resonance (eight pips) and active wards |
| Domain meter | top centre (below boss bars) | your domain (ready and cost, how long your CE can hold it, clashing, burnout) or the one you stand in (name, sure-hit, shielded); gamble reels; recitation count. Hidden when there is nothing to show |
| Domain title | centre | step one of the domain sequence: "Domain Expansion" and the name written letter by letter over an ink flash |
| Sense radar | centre right | while sensing: presences as dots in their grade colour (diamonds for curses, squares otherwise), forward at the top, ticks for above or below |
| Promotion ceremony | full screen overlay | ink wash, a brush stroke in the new grade's colour, the grade name stroke by stroke; about 4 s; play continues |
| Overexertion vignette | screen edge | dark red edge while overexerted, pulsing unless reduced flashing |
| Domain overlay | full screen | edge distortion inside someone else's domain; the information flood while Overloaded |
| Black Flash frame | full screen | red and black frame with a black lightning jag, clear around the crosshair, 0.4 s |
| Mul meter | top left, in the column | Mul energy as a green bar with its number (Modulo, 1.1.0). Shown while the player has any, or is in a Modulo dimension |
| Mission tracker | top left, in the column | the Modulo mission's title (cut with an ellipsis) and its number (1/11), the objective, and where: the site with distance and compass direction, "elsewhere" when the site is in another dimension, or the Descent's coordinates while one is on. Hidden during the epilogue |
| Dialogue box | bottom centre (0, -62) | the speaker's name in the Modulo green and their line; the narrator's lines in paper colour with no name. Lines queue and each stays about 3 s plus its length; drawn over recent chat so a line is never hidden |
| Epilogue | full screen overlay | a dark veil over the world and the whole HUD, the title, and six lines of text, five seconds each (about 32 s); no voice |
| Toasts | top right | short ink toasts: Black Flash, Domain Expansion, clash results, jackpots, promotion, Reverse awakening, Culling Game joins, points, new rules, deadline warnings, the game's start and end |

## 3. Screens

The profile family shares one tab bar (Profile, Growth, Techniques, Vows, Codex, Culling). The
J key opens it on Profile; M opens it on Culling. When the tabs fill the bar, the screen title is
left out (the selected tab names the screen). Screens never pause the game.

| Screen | Contents |
|---|---|
| Profile | the player's model, name, grade and status; stats (max CE and regen, Output, Control, Efficiency, Domain tier) and unspent growth points; the next grade and its requirements; a strip of sections (techniques, Binding Vow, Reverse Technique, domain, completed trials) that cut long text with an ellipsis |
| Growth Tree | four paths (Output, Control, Efficiency, Domain) of 20 nodes; spending is a request the server checks |
| Techniques (Loadout) | known techniques as icons with tooltips (cost, cooldown, grade, coloured by eligibility); drag onto a hotbar slot, right-click a slot to clear, left-click to select; slots above the grade's count are locked |
| Vows (Vow Builder) | the vows as toggles with their weight; focus (Power, Efficiency, Reserve) and term (5 minutes, one day, Binding); the vow's power, what it gives, and the price of breaking it; Swear. With a vow held: its restrictions, gains, time left, and Renounce with its cost |
| Codex | every curse by grade. Undefeated: grade only. After the first kill: name and description. After `unlock_kills`: a turning 3D model, lore, techniques, weaknesses and trophy |
| Culling | four pages. Map: colonies drawn to scale with your position and revealed participants, a colony list (participants and points) and the recent log; hovering a colony gives its centre and radius. Scoreboard: rank, name (or "Unknown participant"), colony, points, sorcerer mark, with the actions the rules allow (reveal, give points in steps of 5, move colony, withdraw). Rules: every rule in force, numbered, with who added it and when it lapses. Negotiate: the rules that can be added with their cost; a chosen rule's text, how long it lasts and the Kogane's verdict (already in force, would conflict, points short, or the price); Propose. Negotiating needs you within 8 blocks of your colony's Kogane |
| Modulo choice | opened when a mission asks the player to choose (Chaos and Harmony): the character's name, their question, and two to four ink buttons. The choice is a request the server checks (on that stage, near the character) |
| Technique wheel | hold G: techniques on a ring; release over one to cast it, left click to equip it in the selected slot, right click to cast without closing; nodes bloom out (instant with reduced motion) |

## 4. Tooltips

Technique icons and scrolls: CE cost, cooldown and grade requirement, green when your grade can
use it and red when not. Cursed tools: their grade, the technique they lend with its cost under
the tool, what their blows do, and whether your grade can wield the power. Bound Effigies: who
they are bound to and the nails left. The Tally Ledger: the points it holds.

## 5. Keys

All rebindable, in the "Jujutsu Craft" category: J profile, R cast the selected slot, V next slot,
G technique wheel, B sensing, K Domain Expansion (sneak + K: barrierless), N Simple Domain, H
Reverse Cursed Technique auto-heal, M Culling Game.
