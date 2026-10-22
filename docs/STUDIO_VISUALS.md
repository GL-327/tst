# Direction, depth and curse art — 1.15.0-preview

Cursed Speech previously encoded its radius in the same vector several renderers treated
as a direction. A radius such as `(13, 0, 0)` therefore pointed the visual east. The server
now sends a separate normalized aim snapshot. It includes personal yaw/pitch offsets,
supports looking vertically, and does not change when the caster subsequently turns.
Projectile impacts retain their actual travel direction. Radius and duration still have
their existing meaning for gameplay and field rendering.

Speech emits three expanding pressure shells from the mouth (two with Low VFX). Their
curved surfaces have axial depth, remain visible from the side, and disperse outward.
Other material effects use UV-mapped energy volumes, facets, thin cuts, shadow pools
and reverse-energy spirals. Blue, Red, Purple and ordinary held charges share real volume
cores with the projectile renderer. Soft sprites remain as small glow accents. A depth-only
surface pass hides rear faces; an emissive color pass preserves luminous energy without
ordinary entity shading. Soft halos never write depth.
Bright currents wrap the spatial cores. Opaque ARGB colors retain their authored hue,
including separate Blue, Red and Purple charge colors.

All 52 curse species have been rebuilt around connected block forms, articulated limbs,
deliberate faces and a few defining silhouette features. Materials use broad pixel clusters
and restrained palettes instead of bevel stacks, gradients and random bright surface noise.
Eye glow is small; wounded and critical variants add a few dark cracks. Shikigami keep
their existing separate rigs. Existing entity dimensions, attacks, animations and saves
remain supported. The complete creature library has 75 distinct rigs and 1,200 clips.
Curse geometry falls from 3,478 cubes to 1,068, a 69% reduction while giving each species
its own rebuilt silhouette. A geometry check also verifies that articulated segments reach
their next joint in GeckoLib's coordinate system.

## Rebuilding the art

Use Python with Pillow and NumPy:

```powershell
python tools/spectral_content.py --creatures-only
python tools/energy_materials.py
python tools/verify_spectral_assets.py
```

`studio_curses.py` contains the anatomy recipes. `studio_materials.py` paints the pixel
materials. The creatures-only option avoids touching equipment artwork. The VFX atlas
contains sixteen guttered glows and sixteen seamless opaque surfaces. Low VFX reduces
mesh detail; the shared geometry budget and existing accessibility controls still apply.

## Verification

```powershell
.\gradlew.bat test runGameTest
.\gradlew.bat runClientGameTest '-PwithCompat=sodium,iris' -PclientTestFocus=studio
.\gradlew.bat build
```

The studio check sends real Cursed Speech events through the integrated server/network
in four compass directions at three pitches, then turns the caster and checks the captured
aim. It captures front/side views of every curse, all sixteen volume materials, moving
Blue/Red/Purple charges, releases and representatives of all 51 families. It also stresses
simultaneous effects against the shared geometry budget. Screenshots stay under `build/`.

Server direction tests cover the complete yaw/pitch range, vertical basis stability,
personal aim offsets and exact payload codec roundtrips. The existing gameplay fixtures
now explicitly tame their summons and enable Infinity when needed, exercise punch
coatings on contact, allow spatial releases to arrive, and use high-health targets when
comparing amplified damage. These match the gameplay brought in by 1.13/1.14.

Both client and server should run this version because the VFX packet includes a new
aim field. No world conversion or resource pack installation is required.

Verified on 22 October 2026: 137 unit tests, all 259 server GameTests, the complete native
studio gallery and the mastery/moving-charge checks passed. Native rendering used
Sodium and Iris without an external shader pack on Intel UHD Graphics 620. The packaged
`jjkcraft-1.15.0-preview.jar` contains the rebuilt models, textures and aim protocol.
