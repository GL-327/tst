# GitHub / local reconciliation — 1.12.2-preview

GitHub main was pulled from `f1c56ba` to `5cb71c5` on 22 October 2026. The resulting
working tree combines the independently developed versions.

| Area | Combined behavior |
| --- | --- |
| Families and controls | GitHub's 51 families, 296 moves, variant wheels, rebinding and selected-power HUD |
| Projection Sorcery | GitHub's editable route planner, terrain checks, combat choreography and rejection feedback |
| RCT | GitHub's reverse-flow training drills, positive-energy applications and healing vows |
| Creatures | GitHub's 75 distinct native rigs, face UVs, damage textures, glow masks, LODs and animation clips |
| Ten Shadows | Local defeat-to-tame rituals integrated into the GitHub wheel and server casting gates |
| Death | Local repair for the stale respawn state that could block every subsequent hit |
| Infinity | GitHub's independent field and filtering, plus local automatic activation and persisted toggle preference |
| Equipment | Both sets coexist; shared IDs use GitHub's current behavior and art |
| VFX | GitHub's spatial/material effects plus local painted atlas accents at distant impacts |
| Combat | GitHub's assisted-combat changes plus shared Black Flash weapon callbacks and hit counting |

## Controls and save compatibility

Keep using GitHub's controls: hold **G** and scroll to select a power slot; **R** opens
an equipped family's variant wheel. All controls remain rebindable. Choose a family in
the loadout editor to put its variants in one slot, or pin individual moves directly.

The Ten Shadows wheel has a **Subjugation** button. Selecting an untamed summon in
Survival opens ritual management. Choose **Subjugate** on clear ground, defeat that
shikigami personally, and stay within 48 blocks. Another combatant's intervention fails
the ritual. Unlocks survive death and reconnect; creative practice does not grant them.

Old local `family_limitless`-style menu slots migrate to `family/limitless` selectors.
The migration retains direct bindings, actual learned moves and their mastery. No
obsolete menu pseudo-techniques are added to the 296-move roster.

Infinity starts enabled for Limitless and can be disabled through its ordinary variant
or direct binding. The preference survives death and reconnect. Before RCT, it consumes
CE and suppresses passive CE recovery; learned RCT removes upkeep unless a vow forbids
RCT. Environmental damage and sure-hits still apply, and eligible dispelling tools
interrupt the field. An exhausted reserve suspends the field while its preference stays
enabled, so it can resume after energy is available.

The old `respawning`-state immunity is repaired automatically during the server tick.
Vanilla's normal brief respawn protection still applies.

## Equipment retained from the local version

Black Rope, Vowbound Blade (`executioners_blade`), Soulsteel Katana, the four Ritual
armour pieces, Warding Talisman and Sealed Reliquary coexist with GitHub's three armour
sets, Stormbell Spear, Ember Wakizashi and Sealbreaker Tanto. Playful Cloud and Dragon
Bone are registered once and retain the GitHub version's physical weapon behavior.
The local Soulsteel Katana lends Grudge Bolt while held. Native Use, grade, energy and
cooldown checks apply to lent powers.

Each Ritual armour piece wards 4% of technique damage for 1.5 CE per health prevented;
the complete set wards 16%. Empty CE, sure-hits and bypass-invulnerability damage receive
no ward. The talisman and reliquary retain their consumable costs and cooldowns.

## Recoverable source history

The complete pre-merge local working tree is saved in the Git stash named
`Local rework preserved before GitHub reconciliation 2026-10-22`. A second file backup,
binary patch and SHA-256 manifest are under `run/reconcile-backups/2026-10-22/`.
The backup contains 1,172 files, including the experimental local creature and Projection
implementations. The active build uses the newer GitHub rigs and planner consistently.

Original local image prompts and source sheets remain in `art/SOURCES.md` and
`art/source/`. Current integration screenshots and verification results are under
`art/verification/reconciliation/`. Earlier verification folders describe earlier builds.

## Development launch

Validation for this combined build: 128 unit tests passed; the Minecraft integration
scenario covers actual death/respawn, stale-state recovery, Infinity upkeep, family-slot
migration, successful and failed rituals, reconnect persistence, and Black Flash weapon
callbacks. The creature/equipment scenario and GitHub wheel/Projection/VFX controls
scenario also passed. All 75 distinct rigs passed structural validation. There are 69
screenshots under `art/verification/reconciliation/`; `results.json` records the package
hash, source revisions and check results. These are local default-renderer checks, not a
new shader-pack or multiplayer-latency compatibility matrix.

```powershell
.\gradlew.bat runClient -PclientRunDir=run/rework-preview
```

This reopens the existing local play profile and its saves. The reconciliation does not
replace world files. Build the distributable with `gradlew assemble`.
