package com.jjkcraft.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.config.JjkConfig;
import com.jjkcraft.config.JjkServerConfig;
import com.jjkcraft.core.RateLimiter;
import com.jjkcraft.core.domain.DomainMath;
import com.jjkcraft.core.progress.ProgressionMath;
import com.jjkcraft.damage.DamageModifiers;
import com.jjkcraft.domain.behavior.DomainBehavior;
import com.jjkcraft.effect.JjkEffects;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.grade.GradeDefinition;
import com.jjkcraft.grade.Grades;
import com.jjkcraft.network.JjkNetworking;
import com.jjkcraft.network.PlayerSyncQueue;
import com.jjkcraft.network.payload.CastFeedbackPayload;
import com.jjkcraft.network.payload.DomainActionPayload;
import com.jjkcraft.network.payload.DomainEventPayload;
import com.jjkcraft.network.payload.DomainSelfPayload;
import com.jjkcraft.network.payload.DomainWorldPayload;
import com.jjkcraft.particle.JjkParticles;
import com.jjkcraft.sorcerer.Awakening;
import com.jjkcraft.sorcerer.Progression;
import com.jjkcraft.sorcerer.SorcererLifecycle;
import com.jjkcraft.sorcerer.SorcererProgress;
import com.jjkcraft.sound.JjkSounds;
import com.jjkcraft.technique.CastFailure;
import com.jjkcraft.technique.TechniqueService;

/**
 * The server side of Domain Expansion: casting rules, the open/active/clash/close phases, upkeep,
 * the barrier that holds trapped targets, clashes, Simple Domains, burnout and client sync.
 * Everything here is authoritative; clients only draw what the payloads describe.
 */
public final class DomainService {
	/** Moments clients are told about. Ordinals travel in {@link DomainEventPayload}. */
	public enum Event {
		OPEN, CLASH, CLASH_WON, CLASH_LOST, PULSE, GAMBLE, JACKPOT, REPEAT, CLOSE
	}

	/** Runs when a domain closes (for any reason), after its behaviors have cleaned up. */
	@FunctionalInterface
	public interface CloseListener {
		void onClosed(ActiveDomain domain);
	}

	private record Simple(LivingEntity holder, double radius, long openedAt) {
	}

	/** Feedback slot for domain refusals, so the technique hotbar can tell them apart. */
	public static final int FEEDBACK_SLOT = -100;
	/** How far from its center (in radii) a caster may stray before the domain collapses. */
	private static final double ANCHOR_RADII = 1.5;
	private static final double SYNC_RANGE = 192;

	public static final AttachmentType<DomainState> STATE = AttachmentRegistry.create(JjkCraft.id("domain"),
			builder -> builder.persistent(DomainState.CODEC).initializer(() -> DomainState.EMPTY));

	private static final Map<UUID, ActiveDomain> BY_CASTER = new HashMap<>();
	private static final Map<UUID, Simple> SIMPLE = new HashMap<>();
	private static final List<CloseListener> CLOSE_LISTENERS = new CopyOnWriteArrayList<>();
	private static final List<java.util.function.Function<LivingEntity, CastFailure>> EXPAND_BLOCKERS = new CopyOnWriteArrayList<>();
	private static final PlayerSyncQueue.Channel SYNC = PlayerSyncQueue.channel("domain", DomainService::sendSelf);
	private static final RateLimiter LIMITER = new RateLimiter(4, 4);
	/** Raised whenever the world view changes; players get a new view when it differs from theirs. */
	private static int worldVersion;
	private static final Map<UUID, Integer> SENT_VERSION = new HashMap<>();
	private static int nextInstance = 1;

	private DomainService() {
	}

	public static void addCloseListener(CloseListener listener) {
		CLOSE_LISTENERS.add(listener);
	}

	/** Refuses domains for a reason outside the domain itself (vows). Return null to allow. */
	public static void addExpandBlocker(java.util.function.Function<LivingEntity, CastFailure> blocker) {
		EXPAND_BLOCKERS.add(blocker);
	}

	public static void init() {
		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DAMAGE.register((entity,source,base,taken,blocked)->{
			var d=BY_CASTER.get(entity.getUUID());if(d==null||taken<=0)return;
			double damage=((Number)d.memory().getOrDefault("caster_damage",0.0)).doubleValue()+taken;d.memory().put("caster_damage",damage);
			if(damage>=entity.getMaxHealth()*(d.phase()==ActiveDomain.Phase.OPENING?0.18:0.55))d.requestClose();
		});
		com.jjkcraft.damage.DamageModifiers.add((level,target,source,amount)->{
            if(!(source.getEntity() instanceof LivingEntity attacker)||source.is(com.jjkcraft.damage.JjkDamageTypes.SURE_HIT))return amount;
			for(var d:BY_CASTER.values())if(d.level()==level&&d.hasBarrier()&&d.phase()==ActiveDomain.Phase.ACTIVE&&attacker!=d.caster()&&d.contains(target)&&!d.contains(attacker)){
				d.damageBarrier(amount);worldVersion++;return 0;
			}return amount;
		});
		ServerPlayNetworking.registerGlobalReceiver(DomainActionPayload.TYPE, (payload, context) -> onAction(context.player(), payload.action()));
		ServerTickEvents.END_SERVER_TICK.register(DomainService::tick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayer player = handler.getPlayer();
			close(player);
			dropSimple(player);
			SENT_VERSION.remove(player.getUUID());
			LIMITER.forget(player.getUUID());
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			close(entity);
			dropSimple(entity);
		});

		SorcererLifecycle.addFullSync(player -> {
			SYNC.sendNow(player);
			SENT_VERSION.remove(player.getUUID());
		});
		SorcererLifecycle.addDeathCopier((oldPlayer, newPlayer, policy, config) ->
				// The domain itself is kept under every policy; burnout ends with death.
				newPlayer.setAttached(STATE, state(oldPlayer).withBurnoutUntil(0)));
		Awakening.addResetHook(player -> {
			close(player);
			dropSimple(player);
			player.setAttached(STATE, DomainState.EMPTY);
			SYNC.markDirty(player);
		});
		Progression.addGradeHook((player, from, to) -> {
			grantIfEligible(player);
			SYNC.markDirty(player);
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> grantIfEligible(handler.getPlayer()));

		TechniqueService.addCastBlocker((caster, id, definition) -> {
			if (caster.hasEffect(JjkEffects.OVERLOADED)) {
				return CastFailure.OVERLOADED;
			}

			if (caster instanceof Player player && state(player).burntOut(caster.level().getGameTime())) {
				return CastFailure.BURNOUT;
			}

			return null;
		});
		CursedEnergy.addRegenModifier(DomainService::regenMultiplier);
		DamageModifiers.add((level, target, source, amount) -> simpleBarrier(target, source.getEntity(), amount));
	}

	// ---------------------------------------------------------------- state

	public static DomainState state(Player player) {
		return player.getAttachedOrCreate(STATE);
	}

	public static void setState(ServerPlayer player, DomainState state) {
		player.setAttached(STATE, state);
		SYNC.markDirty(player);
	}

	public static Optional<ActiveDomain> active(LivingEntity caster) {
		return Optional.ofNullable(BY_CASTER.get(caster.getUUID()));
	}

	public static List<ActiveDomain> all() {
		return List.copyOf(BY_CASTER.values());
	}

	/** Open domains in a level that contain the point, opening ones included. */
	public static List<ActiveDomain> containing(ServerLevel level, Vec3 point) {
		List<ActiveDomain> out = new ArrayList<>();
		for (ActiveDomain domain : BY_CASTER.values()) {
			if (domain.level() == level && domain.phase() != ActiveDomain.Phase.CLOSING && domain.contains(point)) {
				out.add(domain);
			}
		}

		return out;
	}

	public static boolean hasSimpleDomain(LivingEntity entity) {
		return SIMPLE.containsKey(entity.getUUID());
	}

	/** Whether a grade lets a player expand a domain at all. */
	public static boolean gradeAllowsDomains(ServerPlayer player) {
		return Grades.get(player.registryAccess(), Progression.get(player).grade()).map(GradeDefinition::domainTier).orElse(0) > 0;
	}

	public static boolean canRaiseSimple(ServerPlayer player) {
		SorcererProgress progress = Progression.get(player);
		return progress.awakened() && !progress.heavenlyRestricted()
				&& Grades.rank(player.registryAccess(), progress.grade()) >= JjkConfig.server().domains.simpleDomainMinRank;
	}

	/** Gives a player at a domain-capable grade a domain they can cast, if they have none. */
	public static void grantIfEligible(ServerPlayer player) {
        if(com.jjkcraft.awakening.CharacterAwakening.get(player).path().equals("creative_custom"))return;
		SorcererProgress progress = Progression.get(player);
		if (!progress.awakened() || progress.heavenlyRestricted() || !gradeAllowsDomains(player)) {
			return;
		}

		DomainState state = state(player);
		if (state.known().flatMap(id -> Domains.get(player.registryAccess(), id)).isPresent()) {
			return;
		}

		int rank = Grades.rank(player.registryAccess(), progress.grade());
		var family = com.jjkcraft.awakening.CharacterAwakening.family(player);
		if (!com.jjkcraft.awakening.CharacterAwakening.get(player).path().equals("legacy") && family.isPresent() && family.get().domain().isEmpty()) return;
		// Existing characters retain the original Grade 1 domain pool after family migration.
		if (!com.jjkcraft.awakening.CharacterAwakening.get(player).path().equals("legacy")
				&& family.isPresent() && !family.get().domain().isEmpty()) {
			Identifier familyDomain = Identifier.tryParse(family.get().domain());
			if (familyDomain != null && Domains.get(player.registryAccess(), familyDomain)
					.map(d -> Grades.rank(player.registryAccess(), d.grade()) <= rank).orElse(false)) {
				setState(player, state.withKnown(Optional.of(familyDomain)));
			}
			return;
		}
		List<Domains.Entry> options = Domains.all(player.registryAccess()).stream()
				.filter(e -> e.definition().innateWeight() > 0 && Grades.rank(player.registryAccess(), e.definition().grade()) <= rank)
				.toList();
		int total = options.stream().mapToInt(e -> e.definition().innateWeight()).sum();
		if (total <= 0) {
			return;
		}

		int roll = player.getRandom().nextInt(total);
		for (Domains.Entry entry : options) {
			roll -= entry.definition().innateWeight();
			if (roll < 0) {
				setState(player, state.withKnown(Optional.of(entry.id())));
				player.sendSystemMessage(Component.translatable("message.jjkcraft.domain_learned",
						Component.translatable(entry.definition().name()).withColor(entry.definition().visuals().color())));
				return;
			}
		}
	}

	// ---------------------------------------------------------------- casting

	private static void onAction(ServerPlayer player, DomainActionPayload.Action action) {
		if (!player.isAlive() || player.isSpectator() || !LIMITER.tryAcquire(player.getUUID(), player.level().getGameTime())) {
			return;
		}

		CastFailure failure = switch (action) {
			case EXPAND -> toggle(player, DomainMath.Variant.FULL);
			case EXPAND_BARRIERLESS -> toggle(player, DomainMath.Variant.BARRIERLESS);
			case SIMPLE -> toggleSimple(player);
		};

		if (failure != CastFailure.NONE) {
			JjkNetworking.send(player, new CastFeedbackPayload(FEEDBACK_SLOT, failure));
		}
	}

	/** Opens the player's domain, or closes it if it is already open. */
	public static CastFailure toggle(ServerPlayer player, DomainMath.Variant variant) {
		if (BY_CASTER.containsKey(player.getUUID())) {
			close(player);
			return CastFailure.NONE;
		}

		Optional<Identifier> known = state(player).known();
		if (known.isEmpty()) {
			return CastFailure.NO_DOMAIN;
		}

		return expand(player, known.get(), variant, false);
	}

	/**
	 * Opens a domain for any caster. Players are checked against their grade, burnout and CE; with
	 * {@code force}, only CE is checked (commands, tests and curses).
	 */
	public static CastFailure expand(LivingEntity caster, Identifier domainId, DomainMath.Variant variant, boolean force) {
		if (!(caster.level() instanceof ServerLevel level)) {
			return CastFailure.DISABLED;
		}

		JjkServerConfig config = JjkConfig.server();
		if (!config.systems.domains) {
			return CastFailure.DISABLED;
		}

		Optional<DomainDefinition> found = Domains.get(level.registryAccess(), domainId);
		if (found.isEmpty()) {
			return CastFailure.NO_DOMAIN;
		}

		if (BY_CASTER.containsKey(caster.getUUID())) {
			return CastFailure.BUSY;
		}

		DomainDefinition definition = found.get();
		if (caster.hasEffect(JjkEffects.STUNNED)) {
			return CastFailure.STUNNED;
		}

		if (caster.hasEffect(JjkEffects.OVERLOADED)) {
			return CastFailure.OVERLOADED;
		}

		for (var blocker : EXPAND_BLOCKERS) {
			CastFailure blocked = blocker.apply(caster);
			if (blocked != null && blocked != CastFailure.NONE) {
				return blocked;
			}
		}

		int domainPoints = 0;
		if (caster instanceof ServerPlayer player) {
			SorcererProgress progress = Progression.get(player);
			domainPoints = progress.growth().domain();
			if (!force) {
				if (progress.heavenlyRestricted()) {
					return CastFailure.HEAVENLY_RESTRICTION;
				}

				if (!gradeAllowsDomains(player) || Grades.rank(level.registryAccess(), progress.grade()) < Grades.rank(level.registryAccess(), definition.grade())) {
					return CastFailure.GRADE_TOO_LOW;
				}

				if (state(player).burntOut(level.getGameTime())) {
					return CastFailure.BURNOUT;
				}
			}
		}

		double cost = DomainMath.castCost(definition.castCost(), variant, domainPoints, config.domains.costMultiplier);
		if (!CursedEnergy.trySpend(caster, cost)) {
			return CastFailure.NOT_ENOUGH_ENERGY;
		}

		double radius = definition.radius() * variant.radius;
		ActiveDomain domain = new ActiveDomain(nextInstance++, caster, level, domainId, definition, variant, caster.position(), radius,
				DomainMath.power(variant, domainPoints));
		BY_CASTER.put(caster.getUUID(), domain);
		if(caster instanceof ServerPlayer p){
			var pose=new com.jjkcraft.network.payload.CombatPosePayload(caster.getId(),"cast_domain",level.getGameTime(),ActiveDomain.OPENING_TICKS,3,8,-1,0);
			JjkNetworking.send(p,pose);for(var viewer:net.fabricmc.fabric.api.networking.v1.PlayerLookup.tracking(caster))JjkNetworking.send(viewer,pose);
		}
		dropSimple(caster);
		worldVersion++;

		if (caster instanceof ServerPlayer player) {
			setState(player, state(player).withOpened());
		}

		SoundEvent open = definition.visuals().openSound().flatMap(JjkSounds::byId).orElse(JjkSounds.DOMAIN_OPEN);
		level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), open, SoundSource.PLAYERS, 3f, 1f);
		// Clients need the domain before the event that starts its title sequence.
		pushWorld(level);
		sendEvent(domain, Event.OPEN, 0);
		return CastFailure.NONE;
	}

	/** Closes the caster's domain if one is open. Burnout applies however it ends. */
	public static void close(LivingEntity caster) {
		ActiveDomain domain = BY_CASTER.get(caster.getUUID());
		if (domain != null) {
			close(domain);
		}
	}

	private static void close(ActiveDomain domain) {
		if (domain.phase() == ActiveDomain.Phase.CLOSING) {
			return;
		}

		boolean wasOpen = domain.tookHold();
		releaseInside(domain);
		if (wasOpen) {
			for (DomainBehavior behavior : domain.definition().behaviors()) {
				behavior.close(domain);
			}
		}

		if (domain.opponent != null) {
			domain.opponent.opponent = null;
			if (domain.opponent.phase() == ActiveDomain.Phase.CLASHING) {
				if (domain.opponent.tookHold()) {
					domain.opponent.setPhase(ActiveDomain.Phase.ACTIVE);
				} else {
					takeHold(domain.opponent);
				}
			}

			domain.opponent = null;
		}

		domain.setPhase(ActiveDomain.Phase.CLOSING);
		LivingEntity caster = domain.caster();
		if (caster instanceof ServerPlayer player) {
			setState(player, state(player).withBurnoutUntil(domain.level().getGameTime()
					+ com.jjkcraft.reverse.ReverseTechnique.burnout(player, domain.definition().burnout())));
		}

		domain.level().playSound(null, domain.center().x, domain.center().y, domain.center().z, JjkSounds.DOMAIN_CLOSE, SoundSource.PLAYERS, 2f, 1f);
		sendEvent(domain, Event.CLOSE, 0);
		for (CloseListener listener : CLOSE_LISTENERS) {
			listener.onClosed(domain);
		}

		worldVersion++;
	}

	// ---------------------------------------------------------------- tick

	private static void tick(MinecraftServer server) {
		for (ActiveDomain domain : List.copyOf(BY_CASTER.values())) {
			tick(domain);
		}

		findClashes();
		for (Simple simple : List.copyOf(SIMPLE.values())) {
			tickSimple(simple);
		}

		if (server.getTickCount() % 5 == 0) {
			broadcastWorld(server);
		}
	}

	private static void tick(ActiveDomain domain) {
		LivingEntity caster = domain.caster();
		ServerLevel level = domain.level();
		long inPhase = level.getGameTime() - domain.phaseStart();

		if (domain.phase() == ActiveDomain.Phase.CLOSING) {
			if (inPhase >= ActiveDomain.CLOSING_TICKS) {
				BY_CASTER.remove(caster.getUUID(), domain);
				worldVersion++;
			}

			return;
		}

		if (!caster.isAlive() || caster.isRemoved() || caster.level() != level
				|| caster.position().distanceTo(domain.center()) > domain.radius() * ANCHOR_RADII) {
			close(domain);
			return;
		}

		domain.refreshInside();
		switch (domain.phase()) {
			case OPENING -> {
				if(domain.closeRequested()||com.jjkcraft.combat.CursedCombat.knockedDown(caster)){close(domain);return;}
				if (inPhase % 4 == 0) {
					level.sendParticles(JjkParticles.of(JjkParticles.DOMAIN_SHARD, domain.definition().visuals().color(), 1.2f),
							domain.center().x, domain.center().y + 1, domain.center().z, 12, domain.radius() / 3, 1.5, domain.radius() / 3, 0.05);
				}

				if (inPhase >= ActiveDomain.OPENING_TICKS) {
					takeHold(domain);
				}
			}
			case ACTIVE -> {
				if (!payUpkeep(domain) || domain.closeRequested() || domain.activeTicks() >= domain.definition().maxDuration()) {
					close(domain);
					return;
				}

				updateInside(domain);
				confine(domain);
				for (DomainBehavior behavior : domain.definition().behaviors()) {
					behavior.tick(domain);
					if (domain.phase() != ActiveDomain.Phase.ACTIVE) {
						return;
					}
				}

				if (domain.closeRequested()) {
					close(domain);
				}
			}
			case CLASHING -> {
				if (!payUpkeep(domain)) {
					close(domain);
					return;
				}

				updateInside(domain);
				confine(domain);
				if (inPhase % 3 == 0 && domain.opponent != null) {
					Vec3 mid = domain.center().lerp(domain.opponent.center(), domain.radius() / (domain.radius() + domain.opponent.radius()));
					level.sendParticles(JjkParticles.of(JjkParticles.DOMAIN_SHARD, domain.definition().visuals().color(), 1.4f),
							mid.x, mid.y + 1.5, mid.z, 10, 0.4, 2.0, 0.4, 0.15);
				}

				if (inPhase >= DomainMath.CLASH_TICKS && domain.opponent != null) {
					resolveClash(domain, domain.opponent);
				}
			}
			default -> {
			}
		}
	}

	private static void takeHold(ActiveDomain domain) {
		domain.memory().put("caster_damage",0.0);
		domain.setPhase(ActiveDomain.Phase.ACTIVE);
		domain.markHeld();
		if (domain.hasBarrier()) {
			for (LivingEntity enemy : domain.enemiesInside()) {
				domain.trapped.add(enemy.getUUID());
			}
		}

		updateInside(domain);
		for (DomainBehavior behavior : domain.definition().behaviors()) {
			behavior.open(domain);
		}

		worldVersion++;
	}

	private static boolean payUpkeep(ActiveDomain domain) {
		if (domain.level().getGameTime() % 20 != 0) {
			return true;
		}

		int points = domain.caster() instanceof ServerPlayer player ? Progression.get(player).growth().domain() : 0;
		double cost = DomainMath.upkeepPerSecond(domain.definition().upkeep(), domain.variant(), points, JjkConfig.server().domains.costMultiplier);
		return CursedEnergy.trySpend(domain.caster(), cost);
	}

	/**
	 * Keeps the set of entities whose regen this domain may change in step with who is inside.
	 * Each entity's stored CE is checkpointed before its regen rate changes.
	 */
	private static void updateInside(ActiveDomain domain) {
		Set<UUID> now = new HashSet<>();
		for (LivingEntity entity : domain.enemiesInside()) {
			now.add(entity.getUUID());
			if (!domain.inside.contains(entity.getUUID())) {
				CursedEnergy.checkpoint(entity);
			}
		}

		for (UUID gone : domain.inside) {
			if (!now.contains(gone) && domain.level().getEntity(gone) instanceof LivingEntity left) {
				CursedEnergy.checkpoint(left);
			}
		}

		domain.inside.clear();
		domain.inside.addAll(now);
	}

	private static void releaseInside(ActiveDomain domain) {
		for (UUID id : domain.inside) {
			if (domain.level().getEntity(id) instanceof LivingEntity entity) {
				CursedEnergy.checkpoint(entity);
			}
		}

		domain.inside.clear();
		domain.trapped.clear();
	}

	/** The barrier: targets caught when it closed can't walk, fly or be knocked out of it. */
	private static void confine(ActiveDomain domain) {
		if (!domain.hasBarrier() || domain.trapped.isEmpty()) {
			return;
		}

		double limit = domain.radius() - 0.6;
		for (UUID id : domain.trapped) {
			if (!(domain.level().getEntity(id) instanceof LivingEntity entity) || !entity.isAlive()) {
				continue;
			}

			Vec3 offset = entity.position().subtract(domain.center());
			double distance = offset.length();
			if (distance <= limit) {
				continue;
			}

			Vec3 inward = offset.scale(-1 / Math.max(distance, 1e-3));
			Vec3 target = domain.center().add(offset.scale(limit / Math.max(distance, 1e-3)));
			if (entity instanceof ServerPlayer player) {
				player.teleportTo(target.x, Math.max(target.y, entity.getY()), target.z);
			} else {
				entity.setPos(target.x, Math.max(target.y, entity.getY()), target.z);
			}

			entity.setDeltaMovement(inward.scale(0.3));
			entity.needsSync = true;
		}
	}

	private static double regenMultiplier(LivingEntity entity) {
		if (BY_CASTER.isEmpty()) {
			return 1.0;
		}

		double multiplier = 1.0;
		for (ActiveDomain domain : BY_CASTER.values()) {
			if (domain.inside.contains(entity.getUUID())
					&& (domain.phase() == ActiveDomain.Phase.ACTIVE || domain.phase() == ActiveDomain.Phase.CLASHING)) {
				for (DomainBehavior behavior : domain.definition().behaviors()) {
					multiplier *= behavior.regenMultiplier(domain, entity);
				}
			}
		}

		return multiplier;
	}

	// ---------------------------------------------------------------- clashes

	private static void findClashes() {
		List<ActiveDomain> open = BY_CASTER.values().stream()
				.filter(d -> d.phase() == ActiveDomain.Phase.ACTIVE || d.phase() == ActiveDomain.Phase.OPENING)
				.toList();
		for (int i = 0; i < open.size(); i++) {
			for (int j = i + 1; j < open.size(); j++) {
				ActiveDomain a = open.get(i);
				ActiveDomain b = open.get(j);
				if (a.level() == b.level() && a.opponent == null && b.opponent == null
						&& DomainMath.overlap(a.center().distanceTo(b.center()), a.radius(), b.radius())) {
					startClash(a, b);
				}
			}
		}
	}

	private static void startClash(ActiveDomain a, ActiveDomain b) {
		a.opponent = b;
		b.opponent = a;
		a.setPhase(ActiveDomain.Phase.CLASHING);
		b.setPhase(ActiveDomain.Phase.CLASHING);
		Vec3 mid = a.center().lerp(b.center(), 0.5);
		a.level().playSound(null, mid.x, mid.y, mid.z, JjkSounds.DOMAIN_CLASH, SoundSource.PLAYERS, 3f, 1f);
		sendEvent(a, Event.CLASH, b.id());
		sendEvent(b, Event.CLASH, a.id());
		worldVersion++;
	}

	private static DomainMath.Clasher clasher(ActiveDomain domain) {
		int points = domain.caster() instanceof ServerPlayer player ? Progression.get(player).growth().domain() : 0;
		return new DomainMath.Clasher(CursedEnergy.current(domain.caster()), points, domain.definition().clashPriority(), domain.variant(),
				domain.openedAt());
	}

	private static void resolveClash(ActiveDomain a, ActiveDomain b) {
		DomainMath.ClashResult result = DomainMath.resolve(clasher(a), clasher(b));
		ActiveDomain winner = result == DomainMath.ClashResult.FIRST_WINS ? a : b;
		ActiveDomain loser = winner == a ? b : a;
		a.opponent = null;
		b.opponent = null;

		// A domain that clashed while still opening takes hold now that it has won.
		if (winner.tookHold()) {
			winner.setPhase(ActiveDomain.Phase.ACTIVE);
		} else {
			takeHold(winner);
		}

		if (winner.caster() instanceof ServerPlayer player) {
			Progression.addCounter(player, ProgressionMath.CLASH_WINS, 1);
		}

		sendEvent(winner, Event.CLASH_WON, loser.id());
		sendEvent(loser, Event.CLASH_LOST, winner.id());
		close(loser);
		backlash(loser.caster());
	}

	/** Stun and CE loss for whoever lost a clash. */
	public static void backlash(LivingEntity loser) {
		JjkServerConfig.Domains config = JjkConfig.server().domains;
		int stun = (int) Math.round(config.clashBacklashStunSeconds * 20);
		if (stun > 0) {
			loser.addEffect(new MobEffectInstance(JjkEffects.STUNNED, stun, 0, false, true));
		}

		CursedEnergy.drain(loser, DomainMath.backlashEnergy(CursedEnergy.max(loser), config.clashBacklashEnergyPercent));
		CursedEnergy.forceOverexert(loser, stun);
	}

	// ---------------------------------------------------------------- simple domain

	public static CastFailure toggleSimple(ServerPlayer player) {
		if (SIMPLE.containsKey(player.getUUID())) {
			dropSimple(player);
			return CastFailure.NONE;
		}

		if (!JjkConfig.server().systems.domains) {
			return CastFailure.DISABLED;
		}

		SorcererProgress progress = Progression.get(player);
		if (progress.heavenlyRestricted()) {
			return CastFailure.HEAVENLY_RESTRICTION;
		}

		if (!canRaiseSimple(player)) {
			return CastFailure.GRADE_TOO_LOW;
		}

		if (BY_CASTER.containsKey(player.getUUID())) {
			return CastFailure.BUSY;
		}

		return raiseSimple(player);
	}

	/** Raises a Simple Domain around any caster that can pay for it. */
	public static CastFailure raiseSimple(LivingEntity holder) {
		JjkServerConfig.Domains config = JjkConfig.server().domains;
		if (holder.hasEffect(JjkEffects.STUNNED)) {
			return CastFailure.STUNNED;
		}

		if (!CursedEnergy.trySpend(holder, config.simpleDomainCost)) {
			return CastFailure.NOT_ENOUGH_ENERGY;
		}

		SIMPLE.put(holder.getUUID(), new Simple(holder, config.simpleDomainRadius, holder.level().getGameTime()));
		holder.level().playSound(null, holder.getX(), holder.getY(), holder.getZ(), JjkSounds.BARRIER, SoundSource.PLAYERS, 1f, 1.4f);
		markSelf(holder);
		worldVersion++;
		return CastFailure.NONE;
	}

	public static void dropSimple(LivingEntity holder) {
		if (SIMPLE.remove(holder.getUUID()) != null) {
			markSelf(holder);
			worldVersion++;
		}
	}

	private static void tickSimple(Simple simple) {
		LivingEntity holder = simple.holder();
		JjkServerConfig.Domains config = JjkConfig.server().domains;
		long age = holder.level().getGameTime() - simple.openedAt();
		if (!holder.isAlive() || holder.isRemoved() || age > config.simpleDomainMaxSeconds * 20
				|| (age % 20 == 0 && age > 0 && !CursedEnergy.trySpend(holder, config.simpleDomainUpkeep))) {
			dropSimple(holder);
			return;
		}

		// Holding the barrier takes focus: no sprinting.
		holder.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 10, 0, false, false));
	}

	private static Optional<Simple> simpleCovering(LivingEntity entity) {
		for (Simple simple : SIMPLE.values()) {
			if (simple.holder().level() == entity.level() && simple.holder().distanceToSqr(entity) <= simple.radius() * simple.radius()) {
				return Optional.of(simple);
			}
		}

		return Optional.empty();
	}

	/**
	 * Whether a Simple Domain cancels a sure-hit effect on {@code entity}. Each cancel costs the
	 * holder CE; a holder who can't pay loses the Simple Domain and the effect lands.
	 */
	public static boolean shieldedFromSureHit(LivingEntity entity) {
		Optional<Simple> simple = simpleCovering(entity);
		if (simple.isEmpty()) {
			return false;
		}

		if (CursedEnergy.trySpend(simple.get().holder(), JjkConfig.server().domains.simpleDomainCostPerCancel)) {
			return true;
		}

		dropSimple(simple.get().holder());
		return false;
	}

	/** The Simple Domain's barrier blocks part of any hit that comes from outside it. */
	private static float simpleBarrier(LivingEntity target, Entity attacker, float amount) {
		if (SIMPLE.isEmpty() || attacker == null || attacker == target) {
			return amount;
		}

		Optional<Simple> simple = simpleCovering(target);
		if (simple.isEmpty() || simple.get().holder().distanceToSqr(attacker) <= simple.get().radius() * simple.get().radius()) {
			return amount;
		}

		return amount * (float) (1 - JjkConfig.server().domains.simpleDomainBlockPercent / 100.0);
	}

	// ---------------------------------------------------------------- sync

	private static void markSelf(LivingEntity entity) {
		if (entity instanceof ServerPlayer player) {
			SYNC.markDirty(player);
		}
	}

	private static void sendSelf(ServerPlayer player) {
		ActiveDomain domain = BY_CASTER.get(player.getUUID());
		DomainState state = state(player);
		JjkNetworking.send(player, new DomainSelfPayload(state.known(), state.burnoutUntil(), domain == null ? -1 : domain.id(),
				SIMPLE.containsKey(player.getUUID()), canRaiseSimple(player)));
	}

	private static void broadcastWorld(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			Integer sent = SENT_VERSION.get(player.getUUID());
			if (sent != null && sent == worldVersion) {
				continue;
			}

			SENT_VERSION.put(player.getUUID(), worldVersion);
			JjkNetworking.send(player, worldView(player));
			SYNC.markDirty(player);
		}
	}

	private static DomainWorldPayload worldView(ServerPlayer player) {
		List<DomainWorldPayload.View> views = new ArrayList<>();
		for (ActiveDomain d : BY_CASTER.values()) {
			if (d.level() == player.level() && d.center().distanceTo(player.position()) < SYNC_RANGE + d.radius()) {
				views.add(new DomainWorldPayload.View(d.id(), d.caster().getId(), d.domainId(), d.center().x, d.center().y, d.center().z,
						(float) d.radius(), d.variant().ordinal(), d.phase().ordinal(), d.phaseStart(), d.openedAt(),d.integrity()));
			}
		}

		List<DomainWorldPayload.SimpleView> simple = new ArrayList<>();
		for (Simple s : SIMPLE.values()) {
			if (s.holder().level() == player.level() && s.holder().distanceTo(player) < SYNC_RANGE) {
				simple.add(new DomainWorldPayload.SimpleView(s.holder().getId(), (float) s.radius()));
			}
		}

		return new DomainWorldPayload(views, simple);
	}

	/** Sends the current world view to every player in a level right away. */
	private static void pushWorld(ServerLevel level) {
		worldVersion++;
		for (ServerPlayer player : level.players()) {
			SENT_VERSION.put(player.getUUID(), worldVersion);
			JjkNetworking.send(player, worldView(player));
		}
	}

	static void sendEvent(ActiveDomain domain, Event kind, int value) {
		DomainEventPayload payload = new DomainEventPayload(domain.id(), kind.ordinal(), value);
		for (ServerPlayer player : domain.level().players()) {
			if (player.position().distanceTo(domain.center()) < domain.radius() + 64) {
				JjkNetworking.send(player, payload);
			}
		}
	}

	/** Test and command helper: closes every domain and Simple Domain at once. */
	public static void clearAll() {
		for (ActiveDomain domain : List.copyOf(BY_CASTER.values())) {
			close(domain);
		}

		BY_CASTER.clear();
		for (Simple simple : List.copyOf(SIMPLE.values())) {
			dropSimple(simple.holder());
		}

		worldVersion++;
	}
}
