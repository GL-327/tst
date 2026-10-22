# Combat refinement — 1.13.0-preview

Historical release notes. The current hand renderer, centered origins, material effects
and mastery buffs are described in [Mastery and effects](MASTERY_AND_VFX.md).

This release addresses first-person combat arms, moving charges, spatial releases,
fist coatings, offensive output, wheel readability and creature presentation.

## Controls

| Action | Control |
| --- | --- |
| Select a power slot | Hold G and scroll |
| Open an equipped family | R |
| Charge a supported move | Hold its cast key, or hold the move in the family wheel |
| Release the charged move | Release the same key or mouse button |
| Prepare Blue or Red fists | Activate Blue Punch or Red Punch |
| Remove the spatial coating | Activate that same punch move again |
| Toggle independent CE fists | O, or the wheel's CE fists button |
| Inspect domain readiness/status | Open the technique wheel |

Keys remain rebindable. Blue and Red replace one another; CE coating can accompany
either. Coatings apply only to unarmed contact. CE coating costs 3 CE per landed hit;
Blue costs 5 and Red costs 7, in addition to their activation cost. An unsuccessful
attack spends no coating-hit energy. A coating that cannot pay its next hit turns
off. Coatings clear on death, disconnect and character reset.

Both ordinary Minecraft melee and the custom combat system apply the coatings.
The hit receives bonus technique damage; Blue draws its target toward the caster
and Red repels it. The combat panel and wheel show the current state.

## Hands, selector and effects

Empty hands use one first-person pose path with a low stance and mirrored handling
for left-handed players. Held items use their ordinary rendering path. The
third-person combat pose is excluded while submitting first-person arms.

Domain information occupies a compact strip inside the wheel. The ordinary combat
view no longer carries its readiness panel. The wheel keeps a complete selected
move name above the ring and a shorter name in its center. Hovering side controls
preserves the selection. Clicking empty space does not fire the selected move.

Charging creates one live visual per caster. Its position comes from interpolated
movement and the camera/hand offset each frame. Repeated packets update this effect
instead of leaving a series of stationary effects. Charged projectiles use the same
release origin. Blue and Red launch outward before opening their attraction or
repulsion field; Maximum Blue also travels before its pull begins.

Spatial fields retain a single visual identity while refreshed. Their envelopes
and influence outlines follow the real radius. Projectile ribbons record positions
once per game tick so high frame rates do not shorten their trails. Glare is reduced
when a large projectile passes close to the camera.

## Charge timing and output

Charges start at 25%. Ordinary profiles reach 100% after 60 ticks (3 seconds).
Maximum profiles take 120 ticks (6 seconds). Purple reaches 100% after 120 ticks,
then takes another 200 ticks (10 seconds) to reach 200%. Mini applications fire
immediately. Holding at the cap continues to consume CE, and the hold limit remains
400 ticks. Charge damage now receives its charge fraction once.

Offensive technique damage defaults to **2.5x**, independently of healing and
control durations. Servers can change **Casting / Technique damage multiplier**.
The default also applies to actual Black Flash technique casts; ordinary physical
Black Flashes retain their physical attack calculation.

Shrine has additional improvements: Cleave adapts to health, armour and CE, with
40% armour penetration; Dismantle has a longer reach and 70% penetration; Furnace
has a nine-block blast; its cutting cage deals stronger sustained damage. Their
CE costs and cooldowns were reduced. Lower-grade curses can now be defeated in
one strong cast; the historical no-one-shot balance contract has been replaced
with a strongest-boss output bound.

At full charge, Maximum Blue influences a **16-block radius** and collapses across
12 blocks; Maximum Red influences **11 blocks**. Purple is larger and faster.
Summons gain 25% health and 75% base melee output, with caller power scaling their
melee attribute. Trials use the corresponding stronger creatures.

## Creature materials and source recipes

All 75 existing creature rigs have repainted materials and a 1024-pixel-wide UV
atlas. Form shading stays continuous across body sections. Cloth, bone, metal,
scales, feathers and fur have different material treatments, with emission reserved
for eyes. Normal, wounded, critical and emissive textures share the same UV layout.

Mahoraga, Nue, Rika, Toad and Well's Unknown Abyss have newly authored forms.
Mahoraga's wheel has eight spokes and a connected rim. Existing rig/controller
names and low-detail models remain compatible with the rendering pipeline.

Editable source recipes:

- `tools/hero_creatures.py`: prominent creature forms.
- `tools/creature_materials.py`: material painting.
- `tools/spectral_content.py`: UV packing, textures, animations and low-detail rigs.
- `tools/combat_refinement.py`: curated combat definitions and descriptions; run
  after the older content generators.

## Checks

The native `refinementAll` scenario exercises first-person and left-handed combat,
the wheel, independent coatings, custom/vanilla melee contact, a moving charge,
outward release, maximum charge timing and Shrine damage. It then renders every
curse and shikigami through GeckoLib. The charge fixture checks one persistent
charge effect and fewer than 600 world-effect quads; frame rate also depends on
hardware and world load.

The `qolRegression` and `polish` scenarios cover the earlier Infinity, respawn,
subjugation, menu, casting and Projection behavior. Unit tests cover the charge
profiles, damage multiplier and the revised balance envelope. Asset validation
checks UV bounds, texture sizes, clips, low-detail rigs, equipment and icons.

```powershell
.\gradlew.bat runClientGameTest -PclientTestFocus=refinementAll
.\gradlew.bat runClientGameTest -PclientTestFocus=qolRegression
.\gradlew.bat runClientGameTest -PclientTestFocus=polish
python tools/verify_spectral_assets.py
.\gradlew.bat test assemble
```

Use the same preview on client and server because new coating and spatial-release
messages are synchronized. Existing saves and technique bindings are retained.

Validated on 22 October 2026: all 131 unit tests passed, with no failures or skips.
The native refinement, combined creature gallery, QoL/reconciliation and Projection
polish checks passed. Asset validation passed for 75 rigs, 4,896 UV-mapped cubes,
1,200 animation clips, 300 creature atlases and 296 technique icons. The packaged
artifact is `build/libs/jjkcraft-1.13.0-preview.jar`.
