# Family and character workshop (1.12.0-preview)

Open Moves with J and choose the Moves tab, then **Families**. Each family lists its moves,
learned status and required grade together. Click a move to open its loadout entry; select a
slot to equip it. Personalize edits only that move. Click the current section name to choose
any of the ten sections directly. Every move has its own tuning and binding vows.

There are 296 powers across 51 families, including twenty original six-move families. Each has at least four moves. Family progression remains server controlled for Inherit/Random characters.
New scroll recipes also allow learning these moves outside the innate family.

## Idle Transfiguration

- Idle Transfiguration: the existing shrink/enlarge utility.
- Transfigured Soul: create an owned servant; repeat casts add rather than replace it.
- Soul Multiplicity: create three servants at once. Both summon moves share a six-soul cap.
- Soul Recall: bring your active servants back beside you, with collision checks.
- Body Repel: Blades: a timed strength/haste form.
- Soul Distortion: aimed soul damage, binding and shrinking.
- Instant Spirit Body: a bulked, resistant body form.
- Soul Multiplicity: Rupture: consume servants in separate enemy-only bursts.
- Transfigured Lunge: a short collision-checked step.

Servants follow their creator and fight the creator's recent target or attacker. They do not
hurt their creator or the creator's owned summons, do not drop loot, and dissolve on expiry,
owner death/disconnection, excessive distance or defeat. They are temporary summons rather
than saved residents. Summon count, health, mobility and lifetime have real CE prices.

## Creative

Choose **Character editor** in Moves. The editor reads the actual server character into a
draft. Selecting a family does not apply it. **Auto Calamity** is a saved client preference,
off initially. When enabled, selecting a different family loads the maximum preset into the
draft. **Calamity preset** also loads that preset explicitly. Review before **Apply character**.

- Family: select the innate family and add its whole move set if desired.
- Potential: exact grade, physical HR flag, birth/trained/stored CE, regen level, XP and body health.
- Training: independently set Output, Control, Efficiency and Domain (0–20) and Special unlock.
- Attributes: independently select the registered attributes, including Six Eyes and restrictions.
- Powers: toggle any learned move and set its exact landed-cast mastery; auto-slot the first twelve.
  The normal loadout then lets you arrange individual slots.
- Domain: choose one registered domain, or deliberately have none.
- Skills: learn/activate reinforcement, learn/automate RCT, tune universal outputs/heal threshold,
  and choose human, spirit or transplanted body form.

Apply uses the selected values, not automatic maximums. Unlearned moves are removed from slots;
forged survival edits, missing registry IDs, duplicate slots and out-of-range combinations are
rejected before the character changes. Grade restrictions still affect which learned moves
can be cast. No domain or omitted family moves are automatically granted on login for a custom
Creative character. Reopening the editor reads the saved values again.

Ordinary birth CE must be positive; Mechamaru/physical HR can use zero. Selecting Mechamaru
clears the draft bank to zero; a Creative player may then explicitly set a bank balance.
Mechamaru storage remains uncapped in gameplay, independently of the editor's one-billion
numeric input limit. A maximum preset never fills an uncapped bank from nothing.

Attack always follows Minecraft's Attack mapping, so right mouse works when Attack/Use are
inverted. Quick mouse clicks are retained until the combat tick. Auto Combat is initially on;
F7 toggles it. U/I/Right Bracket (]) are the default manual light/heavy/block keys, editable in Controls.

## Reproducible resources

`tools/family_expansion.py` adds the family moves, scroll recipes, original glyphs, soul model,
UV texture and animation. Run it after older content generators/datagen, using the project's
Python/Pillow runtime. It is idempotent and writes `build/family-expansion-inventory.json`.

Family selectors can be bound alongside individual moves. Hold G and scroll to select; R opens learned variants. See [wheel, passive Infinity and RCT drills](TECHNIQUE_WHEEL.md).
