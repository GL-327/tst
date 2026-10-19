# Jujutsu Craft sample content

Two packs that show how to add content without code.

- `jjkcraft-sample-pack/` is a data pack (format 121, Minecraft 26.3). It adds the technique Ember
  Lance (a piercing projectile) and Riverstep (a short dash with a vow modifier), the domain Ashen
  Court, the vow Still Feet, the Culling Game rule First Blood, a CE profile for the vanilla warden,
  retunes the mod's Bonewood Staff to lend Ember Lance, and adds diamonds to the Tithe Burrower's
  wants (`jjkcraft:tithe_offerings`).
- `jjkcraft-sample-resources/` is the matching resource pack (format 97): the English names. The
  technique icons are left out on purpose, to show the fallback; add
  `assets/jjksample/textures/gui/technique/<id>.png` (32x32) for real icons.

To use: copy the data pack folder (or `build/distributions/jjkcraft-sample-pack-<version>.zip`)
into a world's `datapacks/` folder before opening it, and the resource pack into
`resourcepacks/`. These registries load with the world, so restart the world after editing
(`/reload` is not enough). A bad file stops the world from loading and the log names it.

Every file format is in `docs/SCHEMAS.md`. The GameTest `SamplePackGameTests` decodes every file in
this pack with the mod's codecs, so the sample stays valid as the mod changes.
