package com.jjkcraft.death;

import java.util.*;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.awakening.CharacterAwakening;
import com.jjkcraft.config.JjkConfig;
import com.jjkcraft.core.RateLimiter;
import com.jjkcraft.core.combat.CombatMath;
import com.jjkcraft.culling.CullingGame;
import com.jjkcraft.culling.CullingState;
import com.jjkcraft.damage.DamageModifiers;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.entity.JjkEntities;
import com.jjkcraft.network.JjkNetworking;
import com.jjkcraft.network.payload.SoulPayloads;
import com.jjkcraft.sorcerer.Awakening;
import com.jjkcraft.sorcerer.Progression;
import com.jjkcraft.sorcerer.SorcererLifecycle;
import com.jjkcraft.technique.TechniqueService;
import com.jjkcraft.vow.Vows;

/** Death choices use vanilla death/respawn in Survival. Hardcore pacts intercept before death. */
public final class SoulSystem {
    public static final ResourceKey<Registry<PactDefinition>> PACTS = ResourceKey.createRegistryKey(JjkCraft.id("pact"));
    public static final AttachmentType<SoulState> STATE = AttachmentRegistry.create(JjkCraft.id("soul"), b -> b.persistent(SoulState.CODEC).copyOnDeath().initializer(() -> SoulState.EMPTY));
    private static final AttachmentType<Map<String, SoulObject>> PLACED = AttachmentRegistry.create(JjkCraft.id("placed_souls"), b -> b.persistent(Codec.unboundedMap(Codec.STRING, SoulObject.CODEC)).initializer(HashMap::new));
    private static final AttachmentType<Map<String, Long>> DESTROYED = AttachmentRegistry.create(JjkCraft.id("lost_souls"), b -> b.persistent(Codec.unboundedMap(Codec.STRING, Codec.LONG)).initializer(HashMap::new));
    public static final DataComponentType<SoulObject> OBJECT_DATA = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, JjkCraft.id("soul_object"), DataComponentType.<SoulObject>builder().persistent(SoulObject.CODEC).networkSynchronized(ByteBufCodecs.fromCodec(SoulObject.CODEC)).build());
    private static final ResourceKey<Item> ITEM_KEY = ResourceKey.create(Registries.ITEM, JjkCraft.id("soul_object"));
    public static final Item OBJECT = Registry.register(BuiltInRegistries.ITEM, ITEM_KEY, new SoulObjectItem(new Item.Properties().setId(ITEM_KEY).stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC)));
    private static final ResourceKey<Block> BLOCK_KEY = ResourceKey.create(Registries.BLOCK, JjkCraft.id("soul_reliquary"));
    public static final Block RELIQUARY = Registry.register(BuiltInRegistries.BLOCK, BLOCK_KEY, new SoulReliquary(BlockBehaviour.Properties.of().setId(BLOCK_KEY).strength(0.3f).noLootTable().lightLevel(s -> 8)));
    public static final EntityType<VesselEntity> VESSEL = JjkEntities.register("vessel", EntityType.Builder.<VesselEntity>of(VesselEntity::new, MobCategory.MISC).sized(0.65f, 1.3f).clientTrackingRange(10).noSave().noLootTable());
    private static final RateLimiter ACTIONS = new RateLimiter(2, 3);
    private static final Map<UUID, Long> MENUS = new HashMap<>();
    private static final Map<UUID, Long> TAKEOVER = new HashMap<>();
    private SoulSystem() { }
    public static void register() { DynamicRegistries.registerSynced(PACTS, PactDefinition.CODEC); }
    public static SoulState state(Player p) { return p.getAttachedOrCreate(STATE); }
    /** Explicit Creative character replacement exits any old death-choice protection. */
    public static void restoreLivingCharacter(ServerPlayer p){
        MENUS.remove(p.getUUID());TAKEOVER.remove(p.getUUID());
        set(p,state(p).alive(0));p.setInvulnerableTime(0);
        p.getAbilities().invulnerable=p.isCreative()||p.isSpectator();p.onUpdateAbilities();
    }
    private static long now(MinecraftServer server) { return server.overworld().getGameTime(); }
    private static MinecraftServer server(ServerPlayer p) { return p.level().getServer(); }
    private static void set(ServerPlayer p, SoulState state) { p.setAttached(STATE, state); send(p, false); }
    public static void init() {
        FabricDefaultAttributeRegistry.register(VESSEL, com.jjkcraft.culling.entity.KoganeEntity.createAttributes());
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof ItemEntity item && item.getItem().has(OBJECT_DATA)) item.setExtendedLifetime();
        });
        ServerPlayNetworking.registerGlobalReceiver(SoulPayloads.Action.TYPE, (payload, context) -> { if (ACTIONS.tryAcquire(context.player().getUUID(), now(server(context.player())))) action(context.player(), payload.action()); });
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, damage) -> {
            if (!(entity instanceof ServerPlayer p) || !JjkConfig.server().death.choices) return true;
            SoulState s = state(p);
            if (s.phase().equals("spirit")) { CullingGame.recordSoulDeath(p,source); restartAfterSpirit(p); return false; }
            if (!p.level().getLevelData().isHardcore()) return true;
            if (!s.phase().equals("alive")) return false;
            CullingGame.recordSoulDeath(p,source);
            p.setHealth(1);
            if (!p.level().getGameRules().get(net.minecraft.world.level.gamerules.GameRules.KEEP_INVENTORY)) p.getInventory().dropAll();
            if(s.pact().equals("sukuna") && s.expires()>0 && (s.expires()<=now(server(p)) || validTokens(server(p),s).isEmpty())) set(p,s.pact(""));
            TechniqueService.stop(p); set(p,snapshot(p,"choosing")); p.setGameMode(GameType.SPECTATOR); return false;
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer p && JjkConfig.server().death.choices && !p.level().getLevelData().isHardcore()) set(p, snapshot(p, "choosing"));
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldP, p, alive) -> { if (!alive) resolveRespawn(p); });
        SorcererLifecycle.addFullSync(p -> send(p, false));
        UseEntityCallback.EVENT.register((player, level, hand, target, hit) -> {
            if (player instanceof ServerPlayer carrier && target instanceof ServerPlayer vessel && state(vessel).vessel()) {
                ItemStack held = carrier.getItemInHand(hand); SoulObject object = held.get(OBJECT_DATA);
                if (object != null) { revive(carrier, vessel, held, object); return InteractionResult.SUCCESS; }
            }
            return InteractionResult.PASS;
        });
        // Transitional attachments can arrive after AFTER_RESPAWN when Fabric copies the old
        // player. A living, corporeal respawn must never inherit death-menu damage immunity.
        DamageModifiers.add((level, target, source, amount) -> target instanceof ServerPlayer p && protectedSoul(p) ? 0 : amount);
        ServerTickEvents.END_SERVER_TICK.register(SoulSystem::tick);
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, srv) -> { MENUS.remove(handler.getPlayer().getUUID()); TAKEOVER.remove(handler.getPlayer().getUUID()); });
    }
    private static SoulState snapshot(ServerPlayer p, String phase) {
        SoulState s = state(p);
        return new SoulState(phase, s.pact(), s.tokens(), s.expires(), p.level().dimension().identifier().toString(), p.getX(), p.getY(), p.getZ(), p.gameMode().getName(), s.possession(), s.warned(), s.vessel());
    }
    public static void send(ServerPlayer p, boolean menu) {
        var config = JjkConfig.server();
        JjkNetworking.send(p, new SoulPayloads.View(state(p), now(server(p)), p.level().getLevelData().isHardcore(), menu,
            new SoulPayloads.Terms(config.death.objectMinutes, config.progression.riteCost, config.death.vesselCost, config.death.possessionThreshold)));
    }
    public static void open(ServerPlayer p) { MENUS.put(p.getUUID(), now(server(p)) + 600); send(p, true); }
    private static boolean nearVessel(ServerPlayer p) { return !p.level().getEntitiesOfClass(VesselEntity.class, p.getBoundingBox().inflate(6)).isEmpty(); }

    public static void action(ServerPlayer p, String action) {
        SoulState s = state(p);
        if (allowedDeathChoice(s,p.level().getLevelData().isHardcore(),action)) {
            if(action.equals("reincarnate")) {
                var pact=p.registryAccess().lookupOrThrow(PACTS).getValue(JjkCraft.id("kenjaku"));
                if(pact!=null && !payPoints(p,pact.price())){message(p,"This pact's reincarnation costs "+pact.price()+" Culling points.");return;}
            }
            set(p, s.phase(action.equals("spirit") ? "becoming_spirit" : action.equals("respawn") ? "respawning" : action.equals("object") ? "becoming_object" : "reincarnating"));
            if (p.isAlive()) resolveRespawn(p);
            return;
        }
        if (!s.phase().equals("alive") || !p.isAlive() || p.isSpectator() || !nearVessel(p) || MENUS.getOrDefault(p.getUUID(), 0L) < now(server(p))) return;
        if (action.equals("respec")) {
            if (!payPoints(p, JjkConfig.server().progression.riteCost)) { message(p, "The Awakening Rite costs " + JjkConfig.server().progression.riteCost + " Culling Game points."); return; }
            // Keep grade, XP and growth; only the innate character identity and techniques are rerolled.
            var progress = Progression.get(p); Awakening.reset(p); Progression.set(p, progress.withHeavenlyRestriction(false));
            Progression.set(p, new com.jjkcraft.sorcerer.SorcererProgress(false, -1, false, progress.grade(), progress.gradeXp(), progress.growth(), progress.trials(), progress.counters(), progress.specialUnlocked(), progress.ceSpentTotal(), progress.castsLanded(), progress.castsAttempted()));
            CharacterAwakening.firstJoin(p); send(p, false); return;
        }
        if (action.equals("vessel")) {
            boolean offering = p.getInventory().contains(new ItemStack(com.jjkcraft.modulo.ModuloItems.YUJI_CURSED_OBJECT));
            if (s.vessel() || (!offering && !payPoints(p, JjkConfig.server().death.vesselCost))) { message(p, "Vessel training costs " + JjkConfig.server().death.vesselCost + " points, or requires Yuji's cursed object."); return; }
            if (offering) removeOffering(p, Identifier.fromNamespaceAndPath("modulo", "yuji_cursed_object"), 1);
            set(p, s.withVessel()); message(p, "Vessel trait unlocked. Others can hand you a cursed object to revive its owner."); return;
        }
        if (action.equals("break")) { if (!s.pact().isEmpty()) { CursedEnergy.set(p, 0); Vows.breakVow(p); set(p, s.pact("")); message(p, "Pact broken. Your cursed energy is exhausted and any Binding Vow debt applies."); } return; }
        if (!s.pact().isEmpty() || Progression.get(p).counter("pact_contracts") > 0) { message(p, "Only one pact can be contracted per character."); return; }
        if (!action.equals("kenjaku") && !action.equals("sukuna")) return;
        PactDefinition pact = p.registryAccess().lookupOrThrow(PACTS).getValue(JjkCraft.id(action)); if (pact == null) return;
        if (!pact.approvedVows().isEmpty() && (!Vows.state(p).active() || Vows.state(p).vows().stream().noneMatch(pact.approvedVows()::contains))) { message(p, "Swear an approved Binding Vow before accepting this pact."); return; }
        boolean paid = pact.offeringRequired() ? removeOffering(p, pact.offering(), pact.offeringCount())
            : payPoints(p, pact.price()) || removeOffering(p, pact.offering(), pact.offeringCount());
        if (!paid) { message(p, "You cannot pay this pact's cost."); return; }
        Progression.addCounter(p, "pact_contracts", 1);
        set(p, s.pact(action)); message(p, pact.name() + " contracted. Read its revival terms before entering combat.");
    }
    public static boolean allowedDeathChoice(SoulState soul,boolean hardcore,String action) {
        return soul.phase().equals("choosing") && (action.equals("spirit") || (action.equals("respawn") && !hardcore)
            || (action.equals("reincarnate") && soul.pact().equals("kenjaku")) || (action.equals("object") && (!hardcore || !soul.pact().isEmpty())));
    }

    private static boolean payPoints(ServerPlayer p, int cost) {
        if (cost == 0) return true;
        var participant = CullingGame.state(server(p)).participant(p.getUUID());
        if (participant == null || participant.points < cost) return false;
        CullingGame.addPoints(server(p), p.getUUID(), -cost, null); return true;
    }
    private static boolean removeOffering(ServerPlayer p, Identifier id, int count) {
        Item item = BuiltInRegistries.ITEM.getValue(id); if (item == null || item == net.minecraft.world.item.Items.AIR) return false;
        int held = 0; for (int n = 0; n < p.getInventory().getContainerSize(); n++) if (p.getInventory().getItem(n).is(item)) held += p.getInventory().getItem(n).getCount();
        if (held < count) return false;
        for (int n = 0; n < p.getInventory().getContainerSize() && count > 0; n++) { ItemStack stack = p.getInventory().getItem(n); if (stack.is(item)) { int take = Math.min(count, stack.getCount()); stack.shrink(take); count -= take; } }
        return true;
    }
    private static void message(ServerPlayer p, String text) { p.sendSystemMessage(Component.literal(text)); }
    private static void resolveRespawn(ServerPlayer p) {
        switch (state(p).phase()) {
            case "respawning" -> { var soul = state(p); leaveGame(p); p.setGameMode(GameType.byName(soul.mode())); set(p, soul.alive(soul.possession())); SorcererLifecycle.fullSync(p); }
            case "becoming_spirit" -> becomeSpirit(p);
            case "becoming_object" -> becomeObject(p);
            case "reincarnating" -> reincarnate(p);
            case "choosing" -> { p.setGameMode(GameType.SPECTATOR); send(p, false); }
            default -> { }
        }
    }
    private static boolean transitioning(SoulState soul) {
        return Set.of("respawning", "becoming_spirit", "becoming_object", "reincarnating").contains(soul.phase());
    }
    private static boolean protectedSoul(ServerPlayer p) {
        String phase = state(p).phase();
        return !phase.equals("alive") && !phase.equals("spirit")
            && (!transitioning(state(p)) || !p.isAlive() || p.isSpectator());
    }
    /** Also repairs saves written while a respawn attachment was still in transit. */
    public static void finishPendingRespawn(ServerPlayer p) {
        if (p.isAlive() && transitioning(state(p))) resolveRespawn(p);
    }
    private static void leaveGame(ServerPlayer p) {
        var game=CullingGame.state(server(p));
        var participant=game.participant(p.getUUID());
        if(participant!=null) participant.status=CullingState.Status.WITHDRAWN;
        game.deadlines.remove(p.getUUID());game.changed();
    }
    public static void becomeSpirit(ServerPlayer p) {
        var soul=state(p); leaveGame(p);
        Progression.set(p,Progression.get(p).withHeavenlyRestriction(false));
        com.jjkcraft.sorcerer.HeavenlyRestriction.refresh(p);
        String[] forms={"echo_mimic","redacted_saint","vow_kin","ash_oni"};
        String form="jjkcraft:"+forms[p.getRandom().nextInt(forms.length)];
        com.jjkcraft.awakening.CharacterTraits.spirit(p,form);
        set(p,soul.phase("spirit")); p.setGameMode(GameType.SURVIVAL);p.setHealth(p.getMaxHealth()); CursedEnergy.fill(p);
        var level=server(p).getLevel(ResourceKey.create(Registries.DIMENSION,Identifier.parse(soul.dimension())));
        if(level!=null) { int x=(int)Math.floor(soul.x()), z=(int)Math.floor(soul.z()); int y=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z); p.teleportTo(level,x+0.5,y+1,z+0.5,Set.of(),p.getYRot(),0,true); }
        message(p,"Reborn as a cursed spirit. Your technique remains. Exorcism ends this character and awakens a new one.");
        SorcererLifecycle.fullSync(p);
    }
    public static void restartAfterSpirit(ServerPlayer p) {
        String oldFamily=CharacterAwakening.get(p).family();
        if(!p.level().getGameRules().get(net.minecraft.world.level.gamerules.GameRules.KEEP_INVENTORY)) p.getInventory().dropAll();
        leaveGame(p); var game=CullingGame.state(server(p)); game.participants.remove(p.getUUID());game.changed();
        Awakening.reset(p);set(p,SoulState.EMPTY);p.setGameMode(GameType.SURVIVAL);
        com.jjkcraft.energy.SorceryPractice.newCharacter(p);
        p.teleport(p.findRespawnPositionAndUseSpawnBlock(false,net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
        p.setHealth(p.getMaxHealth());p.clearFire();p.removeAllEffects();
        CharacterAwakening.rebirth(p,oldFamily); CursedEnergy.fill(p);p.setHealth(p.getMaxHealth());
        message(p,"Your cursed spirit was exorcised. A new character begins with a different innate technique.");SorcererLifecycle.fullSync(p);
    }
    public static void reincarnate(ServerPlayer p) {
        MinecraftServer server = server(p); SoulState soul = state(p);
        var state = CullingGame.state(server);
        var colony = state.colonies.stream().filter(c -> c.dimension.identifier().toString().equals(soul.dimension()))
            .min(Comparator.comparingDouble(c -> c.center.distToCenterSqr(soul.x(), soul.y(), soul.z()))).orElse(null);
        ServerLevel level = colony == null ? server.overworld() : server.getLevel(colony.dimension);
        if (level == null) level = server.overworld();
        if (colony == null) {
            BlockPos spawn = level.getRespawnData().globalPos().pos();
            if (!state.running && JjkConfig.server().systems.cullingGame) CullingGame.start(level, spawn, 1, JjkConfig.server().cullingGame.colonyRadius, 0);
            colony = state.colonies.stream().findFirst().orElseGet(() -> CullingGame.raise(server.overworld(), spawn, JjkConfig.server().cullingGame.colonyRadius, "Reincarnation Colony"));
            level = server.getLevel(colony.dimension);
        }
        int x = colony.center.getX() + colony.radius - 3, z = colony.center.getZ();
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + 1;
        p.setGameMode(GameType.byName(soul.mode())); p.teleportTo(level, x + 0.5, y, z + 0.5, Set.of(), p.getYRot(), p.getXRot(), true);
        p.setHealth(p.getMaxHealth()); CursedEnergy.fill(p);
        state.participants.remove(p.getUUID()); CullingGame.join(p, colony.id);
        state.deadlines.put(p.getUUID(), now(server) + JjkConfig.server().cullingGame.entryWindowDays * 24000L); state.changed();
        set(p, soul.alive(soul.possession())); SorcererLifecycle.fullSync(p);
    }
    private static List<String> validTokens(MinecraftServer server, SoulState state) {
        var lost = server.overworld().getAttachedOrCreate(DESTROYED);
        return state.tokens().stream().filter(t -> !lost.containsKey(t)).toList();
    }
    public static void becomeObject(ServerPlayer p) {
        SoulState s = state(p); MinecraftServer server = server(p); long expires = CombatMath.decayAt(now(server), JjkConfig.server().death.objectMinutes);
        List<String> tokens = new ArrayList<>();
        if (s.pact().equals("sukuna") && s.expires() > now(server) && !s.tokens().isEmpty()) { tokens.addAll(validTokens(server, s)); expires = s.expires(); }
        else {
            var pact = s.pact().isEmpty() ? null : p.registryAccess().lookupOrThrow(PACTS).getValue(JjkCraft.id(s.pact()));
            int count = pact == null ? 1 : pact.fingers();
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, Identifier.parse(s.dimension()))); if (level == null) level = server.overworld();
            BlockPos pos = BlockPos.containing(s.x(), s.y(), s.z());
            for (int n = 0; n < count; n++) {
                String token = UUID.randomUUID().toString(); tokens.add(token);
                SoulObject object = new SoulObject(p.getUUID().toString(), token, p.getName().getString(), s.pact().equals("sukuna") ? "jjkcraft:special_grade" : Progression.get(p).grade().toString(), expires);
                if (n == 0 && level.getBlockState(pos).canBeReplaced() && level.getFluidState(pos).isEmpty()) {
                    level.setBlockAndUpdate(pos, RELIQUARY.defaultBlockState()); var map = new HashMap<>(level.getAttachedOrCreate(PLACED)); map.put(Long.toString(pos.asLong()), object); level.setAttached(PLACED, map);
                } else level.addFreshEntity(new ItemEntity(level, s.x(), s.y() + 0.2, s.z(), stack(object)));
            }
        }
        set(p, new SoulState("object", s.pact(), List.copyOf(tokens), expires, s.dimension(), s.x(), s.y(), s.z(), s.mode(), s.possession(), true, s.vessel()));
        p.setHealth(Math.max(1, p.getHealth())); p.setGameMode(GameType.SPECTATOR); TechniqueService.stop(p);
        message(p, "You are a cursed object. Deliver an object to a Vessel before it decays. Destruction loses this character.");
    }
    public static ItemStack stack(SoulObject object) {
        ItemStack stack = new ItemStack(OBJECT); stack.set(OBJECT_DATA, object);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(object.name() + " — " + object.grade().replace("jjkcraft:", "").replace('_', ' ') + " Cursed Object")); return stack;
    }
    public static SoulObject takePlaced(ServerLevel level, BlockPos pos) {
        var map = new HashMap<>(level.getAttachedOrCreate(PLACED)); SoulObject object = map.remove(Long.toString(pos.asLong())); if (object != null) level.setAttached(PLACED, map); return object;
    }
    public static void destroy(MinecraftServer server, SoulObject object) {
        var map = new HashMap<>(server.overworld().getAttachedOrCreate(DESTROYED)); map.put(object.token(), object.expires()); server.overworld().setAttached(DESTROYED, map);
    }
    public static boolean revive(ServerPlayer carrier, net.minecraft.world.entity.LivingEntity vessel, ItemStack item, SoulObject object) {
        MinecraftServer server = server(carrier); ServerPlayer owner;
        try { owner = server.getPlayerList().getPlayer(UUID.fromString(object.owner())); } catch (IllegalArgumentException e) { return false; }
        if (owner == null) { message(carrier, "The object's owner must be online for revival."); return false; }
        SoulState s = state(owner);
        if (!s.phase().equals("object") || s.expires() != object.expires() || object.expires() <= now(server) || !validTokens(server, s).contains(object.token()) || carrier.distanceToSqr(vessel) > 36) { message(carrier, "This cursed object has expired or was already consumed."); return false; }
        if (s.pact().equals("kenjaku")) {
            var pact = owner.registryAccess().lookupOrThrow(PACTS).getValue(JjkCraft.id("kenjaku"));
            if (pact != null && !payPoints(owner, pact.price())) { message(carrier, "Kenjaku revival requires " + pact.price() + " points in the owner's ledger."); return false; }
        }
        List<String> left = new ArrayList<>(validTokens(server, s)); left.remove(object.token());
        int possession = s.possession() + (s.pact().equals("sukuna") ? 1 : 0);
        set(owner, new SoulState("alive", s.pact(), List.copyOf(left), s.pact().equals("sukuna") ? s.expires() : 0, s.dimension(), s.x(), s.y(), s.z(), s.mode(), possession, true, s.vessel()));
        item.shrink(1); owner.setGameMode(GameType.byName(s.mode()));
        owner.teleportTo((ServerLevel) vessel.level(), vessel.getX() + 1, vessel.getY(), vessel.getZ(), Set.of(), owner.getYRot(), 0, true);
        owner.setHealth(Math.max(1, owner.getMaxHealth() * 0.25f)); CursedEnergy.fill(owner);
        owner.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 1200, 1)); owner.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 1200, 0));
        if (possession > 0 && possession % JjkConfig.server().death.possessionThreshold == 0) { TAKEOVER.put(owner.getUUID(), now(server) + 100); message(owner, "Possession takes hold for five seconds. Your body will attack nearby hostile curses."); }
        SorcererLifecycle.fullSync(owner); return true;
    }
    private static void lost(ServerPlayer p) {
        if (p.level().getLevelData().isHardcore() || !JjkConfig.server().death.newCharacterOnLoss) { set(p, state(p).phase("permanent")); p.setGameMode(GameType.SPECTATOR); message(p, "Your last cursed object was lost. This character cannot return."); return; }
        Awakening.reset(p); set(p, SoulState.EMPTY); p.setGameMode(GameType.SURVIVAL); p.setHealth(p.getMaxHealth());
        CullingGame.state(server(p)).participants.remove(p.getUUID()); CullingGame.state(server(p)).deadlines.remove(p.getUUID()); CullingGame.state(server(p)).changed();
        var level = server(p).overworld(); var pos = level.getRespawnData().globalPos().pos(); p.teleportTo(level, pos.getX()+0.5, pos.getY()+1, pos.getZ()+0.5, Set.of(), 0, 0, true);
        message(p, "Your cursed object was lost. A new character begins; choose a new awakening path."); CharacterAwakening.firstJoin(p);
    }
    private static void tick(MinecraftServer server) {
        long now = now(server);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            finishPendingRespawn(p);
            SoulState s = state(p);
            if (s.phase().equals("choosing") && p.isAlive()) { if(!p.isSpectator()) p.setGameMode(GameType.SPECTATOR); }
            if (s.phase().equals("spirit") && p.isAlive() && server.getTickCount()%40==0 && p.getHealth()<p.getMaxHealth() && CursedEnergy.trySpend(p,3)) p.heal(1);
            if (s.phase().equals("object")) {
                if (now >= s.expires() || validTokens(server, s).isEmpty()) { lost(p); continue; }
                if (!p.isSpectator()) p.setGameMode(GameType.SPECTATOR);
                if (server.getTickCount() % 10 == 0) {
                    var level = server.getLevel(ResourceKey.create(Registries.DIMENSION, Identifier.parse(s.dimension())));
                    if (level != null) p.teleportTo(level, s.x(), Math.max(level.getMinY()+2, s.y())+1.5, s.z(), Set.of(), p.getYRot(), p.getXRot(), true);
                    send(p, false);
                }
                p.setDeltaMovement(Vec3.ZERO);
            }
            long takeover = TAKEOVER.getOrDefault(p.getUUID(), 0L);
            if (takeover > now && server.getTickCount() % 12 == 0) {
                var targets = p.level().getEntitiesOfClass(com.jjkcraft.curse.CurseEntity.class, p.getBoundingBox().inflate(4));
                if (!targets.isEmpty()) { var target = targets.getFirst(); com.jjkcraft.combat.CursedCombat.start(p, JjkCraft.id("heavy"), target.getEyePosition().subtract(p.getEyePosition())); }
            } else if (takeover <= now) TAKEOVER.remove(p.getUUID());
        }
        if (server.getTickCount() % 100 == 0) {
            var state = CullingGame.state(server);
            for (var colony : state.colonies) {
                ServerLevel level = server.getLevel(colony.dimension); if (level == null || !level.hasChunkAt(colony.center)) continue;
                if (level.getEntitiesOfClass(VesselEntity.class, new net.minecraft.world.phys.AABB(colony.center).inflate(12)).isEmpty()) {
                    VesselEntity vessel = VESSEL.create(level, EntitySpawnReason.EVENT); if (vessel == null) continue;
                    vessel.setColony(colony.id); vessel.snapTo(colony.center.getX()+4.5, colony.center.getY()+1, colony.center.getZ()+0.5, 0, 0); vessel.setCustomName(Component.literal("Vessel · Awakening Rite")); vessel.setCustomNameVisible(true); level.addFreshEntity(vessel);
                }
            }
            for (ServerLevel level : server.getAllLevels()) {
                var placed = new HashMap<>(level.getAttachedOrCreate(PLACED));
                for (var entry : List.copyOf(placed.entrySet())) if (entry.getValue().expires() <= now) { placed.remove(entry.getKey()); var pos = BlockPos.of(Long.parseLong(entry.getKey())); if (level.hasChunkAt(pos) && level.getBlockState(pos).is(RELIQUARY)) level.removeBlock(pos, false); }
                level.setAttached(PLACED, placed);
            }
            var destroyed = new HashMap<>(server.overworld().getAttachedOrCreate(DESTROYED)); destroyed.values().removeIf(expiry -> expiry <= now); server.overworld().setAttached(DESTROYED, destroyed);
        }
    }
}
