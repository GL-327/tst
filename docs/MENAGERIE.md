# Summoning and rebirth — 1.10.0-preview

## Creative crash and defensive buffs

The editor now resolves absent, empty and removed family IDs to a registered family before
rendering or sending a snapshot. Browsing remains a draft; Apply validates the full character.
Character replacement exits old death-choice protection and clears previous defensive effects.
Mass/armor resistance caps at amplifier 2 (Resistance III, 60% ordinary reduction), including
extreme output/vows. Joining with an older overpowered technique buff downgrades it safely.
Blood Manipulation is tested with real damage on a loaded Survival player. Maximum Creative
presets learn RCT but leave automatic RCT off; enable it deliberately in Skills or with H.

## Birth balance

New sorcerers roll 350–550 base CE in steps of ten. Regeneration independently rolls level
4–6 (1.2–1.6 times the base rate). The largest birth reserve is below 1.6 times the smallest,
compared with the former thirtyfold difference. Grade, training and deliberate build trade-offs
still matter. Low reserves retain larger, cheaper training gains.

Persistent potential version 2 upgrades older rolled survival birth values monotonically from
100–3000 to 350–550, and regen levels 1–10 to 4–6, once. Training remains unchanged. Explicit
Creative character values stay exact; existing zero-birth restrictions keep zero. Uncapped
Mechamaru storage remains uncapped. A Chalice never rerolls CE potential or training.

## Ten Shadows and other shikigami

Ten Shadows includes Divine Dogs, Nue, Toad, Great Serpent, Max Elephant, Rabbit Escape,
Round Deer, Piercing Ox, Tiger Funeral and Mahoraga. Each is a separately equipped summon
power. Later summons unlock by grade. Tiger Funeral's combat details are an original gameplay
interpretation; the mod does not claim unseen manga mechanics as canon.

- Great Serpent binds with its bite; Max Elephant pushes/slows enemies with water impacts.
- Rabbits have low health and distract mobs instead of dealing damage.
- Round Deer heals one HP for 12 owner CE at most once every three seconds, shared across
  all the owner's deer/healing chimeras. Healing vows block this support.
- Piercing Ox gains impact damage from a longer charge, capped at double damage.
- Mahoraga gains resistance to damage kinds it survives, capped at 60%, never immunity.
- Rika and Iron Tortoise can redirect 25% of a nearby owner's ordinary hit, at most five damage,
  into their own health. Only one guardian intercepts each hit. Guard vows and sure-hits bypass it.
- Moon Dregs poisons, Garuda charges/binds, Paper Heron casts feather rays, Lantern Moth burns,
  and Grave Hound poisons. Moon Dregs and Moonlit Menagerie are complete four-move families.

Summons follow their creator, target the creator's enemies and exclude owned allies. They
expire, dissolve when their owner disappears/dies or is too far away, and do not drop loot.
There is a twelve-summon ownership cap; the existing transfigured-soul sub-cap remains six.
Recasting an ordinary summon replaces its previous group; soul summoning remains additive.

## Merging

Summon every component first, then cast the fusion ability:

- Divine Dogs: Totality consumes two Divine Dogs.
- Well’s Unknown Abyss consumes Nue and Toad.
- Agito consumes Nue, Great Serpent, Round Deer and Tiger Funeral.

For personal combinations, learn **Chimera Fusion** (Ten Shadows Grade 1 or its scroll), open
**Moves → Fusion**, choose two to four different learned summon types and save the recipe.
Summon those types, then cast Chimera Fusion. Their live bodies are consumed into one creature
with their combined mechanical features. Saving a recipe does not conjure its components.
Unlearned types and duplicate custom choices are rejected. Missing components or blocked
space leave the existing summons intact. Fusion recipes persist through ordinary death/relog.

Personalize Chimera Fusion's output, health, mobility, lifetime, CE trade-offs and name as with
other powers. A personal title also names the summoned chimera. Fusion bodies are temporary,
not an irreversible destruction of the player's ability to summon their components again.
This is a playable sandbox rule rather than the manga's permanent-destruction ritual.

Roster and Agito component reference: [Ten Shadows Technique](https://jujutsu-kaisen.fandom.com/wiki/Ten_Shadows_Technique).
All models, combat numbers, original familiars and fusion rules here are this mod's implementation.

## Divine Chalice

**Sneak and use** the chalice. Ordinary use only explains the consequence. One chalice is
consumed, including in Creative, for one different registered family. It clears all learned
techniques, individual mastery, loadout, cooldowns, expression tuning/vows, captured spirits,
active summons, saved fusion recipe and the old domain. It then grants the new family's moves
appropriate to the retained grade. A matching domain can unlock normally.

Body traits, clan identity, grade/XP/growth, birth/trained CE, the current CE balance and learned
reinforcement/RCT remain. The chalice cannot be used while dead, spectating or in an unresolved
death choice, and does not reroll body restrictions. It cannot be used to farm a luckier CE roll.

Acquisition:

- Ten Mouth Sovereign: 1% chance on a player kill.
- Culling Warden, Paper Tide Gravemind and Unfinished Ferryman: 0.25% chance on a player kill.
- Crafting is deterministic but requires two netherite ingots, two nether stars, two vanilla
  echo shards, one dragon breath and two Sovereign Teeth from Calamity curse kills.

Recipe grid: top row netherite / nether star / netherite; middle row echo shard / dragon breath /
echo shard; bottom row Sovereign Tooth / nether star / Sovereign Tooth. The item is EPIC and
stacks to one. It also appears in the mod Creative tab for testing.

## Resources

Run `tools/menagerie_content.py` after older generators/datagen using Python with Pillow.
It writes the 22 powers, 18 original shikigami assets, scroll/chalice recipes, loot additions,
translations and `build/menagerie-inventory.json`. Re-running does not duplicate family moves
or loot pools. Screenshots and final test evidence are recorded in TEST_REPORT.md.
