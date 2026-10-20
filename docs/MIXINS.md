# Mixins

1.8.0-preview removes the inventory-key interception mixin. Vanilla Q/E/F and hotbar keys
are never consumed; direct powers use separate configurable defaults.

`MobCombatMixin` injects at `Mob.doHurtTarget` to defer hostile melee and Culling entrant
contact into shared startup/active/recovery windows. Curse entities retain their specialized
brain path. Fabric has no event for replacing a mob's attempted melee strike before contact.
`CombatPoseMixin` now also applies received humanoid mob poses. The existing entity render
extraction hook associates render states with entity ids through weak keys.

The GameTest-only `IntegratedServerShutdownMixin` queues 26.3's new blocking player cleanup
during test disconnect. Fabric's phased test threads otherwise deadlock: the render thread waits
in `IntegratedServer.halt` while the server waits at Fabric's phase barrier. This mixin belongs
to the test mod and is absent from the distributed jar. Normal shutdown still saves the world.

1.4.0-preview adds `CombatPoseMixin` at the end of `HumanoidModel.setupAnim` for player
and armor pose keyframes, and `CombatHandsMixin` around `submitArmWithItem` for balanced
first-person transforms. Both use server pose snapshots; Fabric has no equivalent pose
replacement hook. The real client test exercises each move and verifies changed model limbs.

The mod uses Fabric events and APIs wherever they exist, and a mixin only where none does. Every
mixin is listed here with what it changes and why no event or API could do it. The check was made
against Fabric API 0.163.0+26.3 and Minecraft 26.3 on 2026-10-18.

## Common (`jjkcraft.mixins.json`)

| Mixin | Target | What it does | Why not an event |
|---|---|---|---|
| `LivingEntityMixin` | `LivingEntity.hurtServer`, damage amount at the head (`@ModifyVariable`) | runs incoming damage through `DamageModifiers` (vow bonuses, Reverse Cursed Technique against curses, domain and technique modifiers) before vanilla applies armor and effects | `ServerLivingEntityEvents` has `ALLOW_DAMAGE` (cancel only) and `AFTER_DAMAGE` (after the fact). Nothing lets a mod change the amount |
| `ServerPlayerMixin` | `ServerPlayer.jumpFromGround`, head (`@Inject`) | tells `PlayerActions` that a player jumped (the vow against jumping, the Echo Mimic's replay) | no jump event exists in Fabric API |

## Client (`jjkcraft.client.mixins.json`)

| Mixin | Target | What it does | Why not an event or API |
|---|---|---|---|
| `CameraMixin` | `Camera.alignWithEntity`, tail | adds camera shake after vanilla positions the camera | no camera event in Fabric API |
| `ClientLevelMixin` | `ClientLevel.tickNonPassenger`, head, cancellable | hit-stop: skips the client tick of entities frozen for a few ticks, so they hold still (the server is never paused) | no per-entity client tick event that can skip the tick |
| `EntityRendererMixin` | `EntityRenderer.extractRenderState`, tail | sensed curses glow in their grade colour, the way vanilla draws glowing entities | no hook into the render state's outline colour |
| `BossHealthOverlayAccessor` | `BossHealthOverlay.events` (`@Accessor`) | reads how many boss bars show, so the top-centre HUD elements sit below them | the field is private and no API exposes it |

## Tests only (`jjkcraft-gametest.mixins.json`, not in the mod jar)

| Mixin | Target | What it does | Why not an event or API |
|---|---|---|---|
| `GameTestServerMixin` | the world-loading lambda in `GameTestServer.create` (`lambda$create$1`), the `WorldDimensions.bake` call (`@WrapOperation`) | bakes the GameTest world with the datapack's dimensions instead of an empty registry, so the Modulo tests can use `modulo:naunax` and `modulo:tokyo_2086` (1.1.0) | vanilla's test server deliberately passes an empty dimension registry, and neither Fabric's GameTest API nor a test environment can add a dimension. A normal world (and the client GameTest's world) loads them without help |

The Modulo expansion added no mixin to the mod itself (1.1.0): travel uses `ServerPlayer.teleport`,
the ship's sky uses `LevelRenderEvents`, the HUD uses `HudElementRegistry`, Mahoraga's adaptation
and the Mul Ward use the existing `DamageModifiers` hook, and Mul costs use
`TechniqueService.addCostPayer` and `addCastBlocker`.

## Not mixins

Hooks that looked like they might need a mixin but use an API: death (`ServerLivingEntityEvents.ALLOW_DEATH`
for the Reverse Cursed Technique save), HUD (`HudElementRegistry`), networking
(`PayloadTypeRegistry`), entity data (the attachment API), key bindings (`KeyMappingHelper`),
registry aliases for renamed ids (`Registry.addAlias` from Fabric's registry sync, 1.1.0), and the
Iris and Jade integrations (their own APIs).

## 1.2.0-preview: ProjectionPoseMixin

Client-only TAIL injection in LivingEntityRenderer.extractRenderState(LivingEntity,
LivingEntityRenderState,float). Quantizes only active Projection Sorcery poses to 24 fps;
reduced motion bypasses it. Fabric has no equivalent event for replacing completed
living render state. The existing common mixins remain unchanged.

## 1.3.0-preview: BodyFormMixin

Client RETURN injection into EntityRenderDispatcher.extractEntity retains the original
AvatarRenderState and associates it with a cached host proxy's state. A HEAD injection
into submit redirects only visible geometry to that proxy. Keeping the Avatar state is
required by Minecraft's local-player extraction and first-person hand pipeline. Proxies
have an assigned entity id and are never Players, preventing recursive form extraction.
World changes/disconnects clear proxy entity caches; render-state associations use weak
keys. First-person hands retain the player's arm model. Fabric has no equivalent event
for replacing the whole submitted entity model.
