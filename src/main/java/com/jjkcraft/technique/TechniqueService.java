package com.jjkcraft.technique;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import com.jjkcraft.attachment.JjkAttachments;
import com.jjkcraft.config.DeathPolicy;
import com.jjkcraft.config.JjkConfig;
import com.jjkcraft.core.RateLimiter;
import com.jjkcraft.core.progress.ProgressionMath;
import com.jjkcraft.core.technique.TechniqueMath;
import com.jjkcraft.damage.DamageModifiers;
import com.jjkcraft.effect.JjkEffects;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.grade.GradeDefinition;
import com.jjkcraft.grade.Grades;
import com.jjkcraft.network.JjkNetworking;
import com.jjkcraft.network.PlayerSyncQueue;
import com.jjkcraft.network.payload.CastFeedbackPayload;
import com.jjkcraft.network.payload.CastPayload;
import com.jjkcraft.network.payload.CastTechniquePayload;
import com.jjkcraft.network.payload.SelectSlotPayload;
import com.jjkcraft.network.payload.SetLoadoutPayload;
import com.jjkcraft.network.payload.TechniqueFxPayload;
import com.jjkcraft.network.payload.TechniqueSyncPayload;
import com.jjkcraft.sorcerer.Awakening;
import com.jjkcraft.sorcerer.Progression;
import com.jjkcraft.sorcerer.SorcererLifecycle;
import com.jjkcraft.sorcerer.SorcererProgress;
import com.jjkcraft.sound.JjkSounds;
import com.jjkcraft.technique.behavior.BarrierBehavior;

/**
 * The server cast pipeline. Clients only send intents (slot pressed or released); everything is
 * checked here: rate, slot, knowledge, grade, Heavenly Restriction, stun, cooldown and CE. The
 * same path serves players and mobs; mobs skip the loadout checks.
 */
public final class TechniqueService {
	/** Multipliers from vows, tools or rules for one cast. */
	public record Modifiers(double damage, double cost, double range) {
		public static final Modifiers NONE = new Modifiers(1, 1, 1);

		Modifiers times(Modifiers other) {
			return new Modifiers(damage * other.damage, cost * other.cost, range * other.range);
		}
	}

	@FunctionalInterface
	public interface ModifierProvider {
		Modifiers get(LivingEntity caster, Identifier id, TechniqueDefinition definition);
	}

	/** Blocks casts for a reason outside the technique itself (rules, domains). Returns null to allow. */
	@FunctionalInterface
	public interface CastBlocker {
		CastFailure check(LivingEntity caster, Identifier id, TechniqueDefinition definition);
	}

	/**
	 * Pays for a cast some other way than the caster's CE, such as a cursed tool used as a medium
	 * (docs/MODULO.md 3.6). A payer that covers a cast also lets Heavenly Restriction cast it.
	 */
	public interface CostPayer {
		boolean covers(LivingEntity caster, Identifier id, TechniqueDefinition definition);

		/** Takes the cast's CE-equivalent cost; false if it cannot, and the cast fails as if CE ran short. */
		boolean pay(LivingEntity caster, Identifier id, TechniqueDefinition definition, double cost);
	}

	/** Told about every cast that goes off (after cast time), for things that react to techniques. */
	@FunctionalInterface
	public interface CastListener {
		void onCast(LivingEntity caster, Identifier id, TechniqueDefinition definition);
	}

	/** A running channel or toggle technique. */
	public static final class Active {
		final CastContext ctx;
		final Identifier id;
		final TechniqueDefinition definition;
		final int slot;
		int ticks;

		Active(CastContext ctx, Identifier id, TechniqueDefinition definition, int slot) {
			this.ctx = ctx;
			this.id = id;
			this.definition = definition;
			this.slot = slot;
		}

		public Identifier id() {
			return id;
		}

		public TechniqueBehavior behavior() {
			return definition.behavior();
		}

		public CastContext context() {
			return ctx;
		}
	}

	private record Pending(CastContext ctx, Identifier id, TechniqueDefinition definition, long executeAt, double costPaid, int slot) {
	}

	private static final PlayerSyncQueue.Channel SYNC = PlayerSyncQueue.channel("techniques", TechniqueService::sendState);
	private static final List<ModifierProvider> MODIFIER_PROVIDERS = new CopyOnWriteArrayList<>();
	private static final List<CastBlocker> CAST_BLOCKERS = new CopyOnWriteArrayList<>();
	private static final List<CastListener> CAST_LISTENERS = new CopyOnWriteArrayList<>();
	private static final List<CostPayer> COST_PAYERS = new CopyOnWriteArrayList<>();
	private static final Map<UUID, Active> ACTIVE = new HashMap<>();
	private static final List<Pending> PENDING = new ArrayList<>();
    private static final Map<UUID,Charge> CHARGES=new HashMap<>();
    private static final class Charge {
        final ServerPlayer player; final Identifier id; final TechniqueDefinition definition;
        final int slot; final double base; final ServerLevel level; int ticks; double spent;
        Charge(ServerPlayer p,Identifier id,TechniqueDefinition d,int slot,double base){this.player=p;this.id=id;this.definition=d;this.slot=slot;this.base=base;this.spent=base;this.level=p.level();}
        boolean purple(){return id.getPath().equals("hollow_purple");}
        double fraction(){return com.jjkcraft.core.technique.ChargeMath.fraction(ticks,purple());}
        double drain(){return com.jjkcraft.core.technique.ChargeMath.upkeep(base,ticks,purple(),com.jjkcraft.awakening.CharacterAwakening.get(player).sixEyes());}
        void sync(){JjkNetworking.send(player,new com.jjkcraft.network.payload.ChargePayload(slot,(int)Math.round(fraction()*100),purple()?200:100,(float)(drain()*20)));}
    }
    public static int chargePercent(ServerPlayer p){Charge c=CHARGES.get(p.getUUID());return c==null?0:(int)Math.round(c.fraction()*100);}
    public static void cancelCharge(LivingEntity caster){
        if(!CHARGES.containsKey(caster.getUUID()))return;
        // Settle elapsed energy while suppression still applies; release cannot retroactively regenerate.
        CursedEnergy.trySpend(caster,0);
        CHARGES.remove(caster.getUUID());
        if(caster instanceof ServerPlayer p)JjkNetworking.send(p,new com.jjkcraft.network.payload.ChargePayload(-1,0,100,0));
    }
	private static final Map<UUID, Map<Identifier, Long>> MOB_COOLDOWNS = new HashMap<>();
	private static RateLimiter castLimiter;
	private static double castLimiterRate;

	private TechniqueService() {
	}

	public static void addModifierProvider(ModifierProvider provider) {
		MODIFIER_PROVIDERS.add(provider);
	}

	public static void addCastBlocker(CastBlocker blocker) {
		CAST_BLOCKERS.add(blocker);
	}

	public static void addCastListener(CastListener listener) {
		CAST_LISTENERS.add(listener);
	}

	public static void addCostPayer(CostPayer payer) {
		COST_PAYERS.add(payer);
	}

	/** The payer that takes this cast's cost instead of CE, if any. */
	public static Optional<CostPayer> payer(LivingEntity caster, Identifier id, TechniqueDefinition definition) {
		for (CostPayer payer : COST_PAYERS) {
			if (payer.covers(caster, id, definition)) {
				return Optional.of(payer);
			}
		}

		return Optional.empty();
	}

	public static void init() {
        CursedEnergy.addRegenModifier(caster->CHARGES.containsKey(caster.getUUID())?0:1);
		ServerPlayNetworking.registerGlobalReceiver(CastPayload.TYPE, (payload, context) -> {if(payload.cancelled())cancelKey(context.player(),payload.slot());else onCastKey(context.player(), payload.slot(), payload.pressed());});
		ServerPlayNetworking.registerGlobalReceiver(CastTechniquePayload.TYPE, (payload, context) -> onQuickCast(context.player(), payload.technique()));
		ServerPlayNetworking.registerGlobalReceiver(SetLoadoutPayload.TYPE, (payload, context) -> {
			if (LOADOUT_LIMIT.tryAcquire(context.player().getUUID(), context.player().level().getGameTime())) {
				setLoadout(context.player(), payload.slot(), payload.technique());
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(SelectSlotPayload.TYPE, (payload, context) -> {
			if (LOADOUT_LIMIT.tryAcquire(context.player().getUUID(), context.player().level().getGameTime())) {
				selectSlot(context.player(), payload.slot());
			}
		});

		ServerTickEvents.END_SERVER_TICK.register(TechniqueService::tick);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server->{CHARGES.clear();PENDING.clear();ACTIVE.clear();MOB_COOLDOWNS.clear();});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> cancel(handler.getPlayer()));
		SorcererLifecycle.addFullSync(SYNC::sendNow);
		SorcererLifecycle.addDeathCopier((oldPlayer, newPlayer, policy, config) -> {
			cancel(oldPlayer);
			newPlayer.setAttached(JjkAttachments.TECHNIQUES, applyPolicy(get(oldPlayer), policy, config.partialMasteryLossPercent));
		});

		Awakening.addResetHook(player -> {
			cancel(player);
			set(player, TechniqueState.EMPTY);
		});
		Awakening.addHook((player, heavenly) -> {
			if (!heavenly && com.jjkcraft.awakening.CharacterAwakening.get(player).family().isEmpty()) {
				rollInnate(player);
			}
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			SorcererProgress progress = Progression.get(player);
			// Backfill: awakened before techniques existed, or the innate technique was removed by a datapack.
			if (progress.awakened() && !progress.heavenlyRestricted()
					&& get(player).innate().flatMap(id -> Techniques.get(player.registryAccess(), id)).isEmpty()) {
				rollInnate(player);
			}
		});

		DamageModifiers.add((level, target, source, amount) -> {
			Active active = ACTIVE.get(target.getUUID());
			if (active != null && active.behavior() instanceof BarrierBehavior barrier) {
				return barrier.absorb(active.ctx, target, source, amount);
			}

			return amount;
		});
	}

	// ---------------------------------------------------------------- state

	public static TechniqueState get(Player player) {
		return player.getAttachedOrCreate(JjkAttachments.TECHNIQUES);
	}

	public static void set(ServerPlayer player, TechniqueState state) {
		if (!get(player).equals(state)) {
			player.setAttached(JjkAttachments.TECHNIQUES, state);
			SYNC.markDirty(player);
		}
	}

	public static Optional<Active> active(LivingEntity entity) {
		return Optional.ofNullable(ACTIVE.get(entity.getUUID()));
	}

	private static void sendState(ServerPlayer player) {
		JjkNetworking.send(player, new TechniqueSyncPayload(get(player), active(player).map(Active::id)));
	}

	public static int usableSlots(Player player) {
		return TechniqueState.SLOTS;
	}

	public static boolean knows(Player player, Identifier id) {
		return get(player).known().contains(id);
	}

	public static void grant(ServerPlayer player, Identifier id) {
		set(player, get(player).withLearned(id).withAutoSlotted(usableSlots(player)));
	}

	public static void revoke(ServerPlayer player, Identifier id) {
		active(player).filter(a -> a.id.equals(id)).ifPresent(a -> stop(player));
		set(player, get(player).withoutLearned(id));
	}

	public static void setInnate(ServerPlayer player, Identifier id) {
		set(player, get(player).withInnate(id).withAutoSlotted(usableSlots(player)));
	}

	/** Weighted roll of an innate technique. Higher grades can roll stronger techniques. */
	public static Optional<Identifier> rollInnate(ServerPlayer player) {
		int rank = Grades.rank(player.registryAccess(), Progression.get(player).grade());
		List<Techniques.Entry> pool = new ArrayList<>();
		double total = 0;
		List<Double> weights = new ArrayList<>();
		for (Techniques.Entry entry : Techniques.all(player.registryAccess())) {
			int techRank = Techniques.gradeRank(player.registryAccess(), entry.definition());
			if (entry.definition().innateWeight() <= 0 || techRank > rank + 1) {
				continue;
			}

			double weight = entry.definition().innateWeight() * (techRank <= rank ? 1.0 : 0.35);
			pool.add(entry);
			weights.add(weight);
			total += weight;
		}

		if (pool.isEmpty()) {
			return Optional.empty();
		}

		double roll = player.getRandom().nextDouble() * total;
		for (int i = 0; i < pool.size(); i++) {
			roll -= weights.get(i);
			if (roll <= 0 || i == pool.size() - 1) {
				Identifier id = pool.get(i).id();
				setInnate(player, id);
				player.sendSystemMessage(Component.translatable("message.jjkcraft.innate", Component.translatable(pool.get(i).definition().name())));
				return Optional.of(id);
			}
		}

		return Optional.empty();
	}

	/** Pure death policy step for techniques. */
	public static TechniqueState applyPolicy(TechniqueState state, DeathPolicy policy, double partialMasteryLossPercent) {
		TechniqueState cleared = state.withoutCooldowns();
		return switch (policy) {
			case KEEP -> cleared;
			case PARTIAL -> {
				Map<Identifier, Integer> mastery = new HashMap<>();
				cleared.mastery().forEach((id, uses) -> mastery.put(id, (int) Math.floor(uses * (1 - partialMasteryLossPercent / 100.0))));
				yield cleared.withMastery(mastery);
			}
			case RESET_TECHNIQUES -> {
				TechniqueState reset = TechniqueState.normalized(cleared.innate(), List.of(), cleared.loadout(), cleared.selected(), Map.of(), Map.of());
				List<Optional<Identifier>> slots = new ArrayList<>(reset.loadout());
				slots.replaceAll(s -> s.filter(id -> cleared.innate().map(id::equals).orElse(false)));
				yield TechniqueState.normalized(reset.innate(), List.of(), slots, reset.selected(), Map.of(), Map.of());
			}
		};
	}

	// ---------------------------------------------------------------- client intents

	private static boolean allowRequest(ServerPlayer player) {
		double rate = JjkConfig.server().casting.maxCastRequestsPerSecond;
		if (castLimiter == null || castLimiterRate != rate) {
			castLimiter = new RateLimiter(rate, Math.max(2, rate));
			castLimiterRate = rate;
		}

		return castLimiter.tryAcquire(player.getUUID(), player.level().getGameTime());
	}

	public static void onCastKey(ServerPlayer player, int requestedSlot, boolean pressed) {
		if (!player.isAlive() || player.isSpectator()) {
			return;
		}

		TechniqueState state = get(player);
		int slot = requestedSlot < 0 ? state.selected() : requestedSlot;
		if (!pressed) {
            Charge charge=CHARGES.get(player.getUUID());
            if(charge!=null&&(requestedSlot<0||charge.slot==requestedSlot)){
                cancelCharge(player);
                if(player.level()!=charge.level||!Personalization.definition(player,charge.id,Techniques.get(player.registryAccess(),charge.id).orElse(charge.definition)).equals(charge.definition))return;
                CastFailure result=castCharged(player,charge.id,charge.slot,charge.spent,charge.fraction(),charge.ticks);
                if(result!=CastFailure.NONE)feedback(player,charge.slot,result);
                return;
            }
            releaseChannel(player,requestedSlot);

			return;
		}

		if (!allowRequest(player)) {
			return;
		}

		if (slot < 0 || slot >= Math.min(TechniqueState.SLOTS, usableSlots(player))) {
			feedback(player, slot, CastFailure.EMPTY_SLOT);
			return;
		}

		Optional<Identifier> id = state.slot(slot);
		if (id.isEmpty()) {
			feedback(player, slot, CastFailure.EMPTY_SLOT);
			return;
		}

		CastFailure failure = beginCharge(player, id.get(), slot);
		if (failure != CastFailure.NONE) {
			feedback(player, slot, failure);
		}
	}

    private static void releaseChannel(ServerPlayer player,int slot){
        PENDING.removeIf(p->p.ctx().caster().getUUID().equals(player.getUUID())&&p.definition().cast().mode()==CastMode.CHANNEL&&(slot<0||p.slot()==slot));
        Active active=ACTIVE.get(player.getUUID());
        if(active!=null&&active.definition.cast().mode()==CastMode.CHANNEL&&(slot<0||active.slot==slot))stop(player);
    }
    public static void cancelKey(ServerPlayer player,int slot){
        Charge charge=CHARGES.get(player.getUUID());
        if(charge!=null&&(slot<0||charge.slot==slot))cancelCharge(player);
        releaseChannel(player,slot);
    }
	public static void onQuickCast(ServerPlayer player, Identifier id) {
		if (!player.isAlive() || player.isSpectator() || !allowRequest(player)) {
			return;
		}
		if (get(player).loadout().stream().limit(usableSlots(player)).noneMatch(slot -> slot.filter(id::equals).isPresent())
				&& !TechniqueSources.grants(player, id)) {
			feedback(player, -1, CastFailure.EMPTY_SLOT);
			return;
		}

		CastFailure failure = beginCharge(player, id, -1);
		if(failure==CastFailure.NONE&&CHARGES.containsKey(player.getUUID()))onCastKey(player,-1,false);
		if (failure != CastFailure.NONE) {
			feedback(player, -1, failure);
		}
	}

	/** Loadout edits and slot selection from the client: generous, but bounded. */
	private static final com.jjkcraft.core.RateLimiter LOADOUT_LIMIT = new com.jjkcraft.core.RateLimiter(20, 20);

	public static void setLoadout(ServerPlayer player, int slot, Optional<Identifier> id) {
		if (slot < 0 || slot >= TechniqueState.SLOTS || slot >= usableSlots(player)) {
			return;
		}

		if (id.isPresent() && !knows(player, id.get())) {
			return;
		}

		var next=get(player).withSlot(slot,id);
        Charge charging=CHARGES.get(player.getUUID());
        if(charging!=null&&charging.slot>=0&&!next.slot(charging.slot).equals(Optional.of(charging.id)))cancelCharge(player);
		active(player).filter(a->next.loadout().stream().noneMatch(s->s.filter(a.id()::equals).isPresent())).ifPresent(a->stop(player));
		set(player,next);
	}

	public static void selectSlot(ServerPlayer player, int slot) {
		if (slot >= 0 && slot < Math.min(TechniqueState.SLOTS, usableSlots(player))) {
			set(player, get(player).withSelected(slot));
		}
	}

	private static void feedback(ServerPlayer player, int slot, CastFailure failure) {
		JjkNetworking.send(player, new CastFeedbackPayload(slot, failure));
	}

	// ---------------------------------------------------------------- casting

    private static CastFailure beginCharge(ServerPlayer player,Identifier id,int slot){
        if(CHARGES.containsKey(player.getUUID())||PENDING.stream().anyMatch(p->p.ctx().caster()==player))return CastFailure.BUSY;
        var found=Techniques.get(player.registryAccess(),id);if(found.isEmpty())return CastFailure.NOT_KNOWN;
        var d=Personalization.definition(player,id,found.get());
        if(!d.cast().chargeable())return cast(player,id,slot);
        var active=ACTIVE.get(player.getUUID());
        if(active!=null&&active.id.equals(id)){stop(player);return CastFailure.NONE;}
        CastFailure failure=check(player,id,d);if(failure!=CastFailure.NONE)return failure;
        int sacrifice=Personalization.get(player,id).details().expression().sacrifice();
        if(!player.isCreative()&&sacrifice>0&&player.getHealth()<=sacrifice+1)return CastFailure.NOT_ENOUGH_HEALTH;
        double cost=effectiveCost(player,id,d,modifiers(player,id,d));
        var payer=payer(player,id,d);
        if(!(payer.isPresent()?payer.get().pay(player,id,d,cost):CursedEnergy.trySpend(player,cost)))return CastFailure.NOT_ENOUGH_ENERGY;
        Charge charge=new Charge(player,id,d,slot,cost);CHARGES.put(player.getUUID(),charge);charge.sync();
        TechniqueFx.castPose(new CastContext(player,player.level(),id,d,1,1));
        return CastFailure.NONE;
    }

	/** Validates and runs a cast for a player or mob. Returns NONE on success. */
	public static CastFailure cast(LivingEntity caster, Identifier id, int slot) {
        return castCharged(caster,id,slot,-1,1,0);
    }
    private static CastFailure castCharged(LivingEntity caster, Identifier id, int slot,double prepaid,double charge,int heldTicks) {
		if (!(caster.level() instanceof ServerLevel level)) {
			return CastFailure.DISABLED;
		}

		Optional<TechniqueDefinition> found = Techniques.get(level.registryAccess(), id);
		if (found.isEmpty()) {
			return CastFailure.NOT_KNOWN;
		}

		TechniqueDefinition definition = Personalization.definition(caster,id,found.get());
        if(CHARGES.containsKey(caster.getUUID()))return CastFailure.BUSY;
		if(PENDING.stream().anyMatch(p->p.ctx().caster().getUUID().equals(caster.getUUID())))return CastFailure.BUSY;
		CastMode mode = definition.cast().mode();
		Active active = ACTIVE.get(caster.getUUID());
		if (active != null && mode != CastMode.INSTANT) {
			boolean same = active.id.equals(id);
			stop(caster);
			if (same) {
				// Pressing an active toggle (or a stuck channel) turns it off.
				return CastFailure.NONE;
			}
		}

		CastFailure failure = check(caster, id, definition);
		if (failure != CastFailure.NONE) {
			return failure;
		}

		Modifiers modifiers = modifiers(caster, id, definition);
		int sacrifice=Personalization.get(caster,id).details().expression().sacrifice();
		boolean creative=caster instanceof ServerPlayer p&&p.isCreative();
		if(!creative&&sacrifice>0&&caster.getHealth()<=sacrifice+1)return CastFailure.NOT_ENOUGH_HEALTH;
		double cost = effectiveCost(caster, id, definition, modifiers);
		Optional<CostPayer> payer = payer(caster, id, definition);
		if (prepaid<0 && (payer.isPresent() ? !payer.get().pay(caster, id, definition, cost) : !CursedEnergy.trySpend(caster, cost))) {
			return CastFailure.NOT_ENOUGH_ENERGY;
		}

		PersonalPacts.seal(caster,id);
        // Output counts CE the caster spent; a medium's energy is the tool's.
		if(!creative&&sacrifice>0)caster.setHealth(caster.getHealth()-sacrifice);
		double spent = payer.isPresent() ? 0 : prepaid>=0?prepaid:cost;
        if(prepaid>=0)definition=chargedDefinition(definition,charge,heldTicks);

		CastContext ctx = new CastContext(caster, level, id, definition, power(caster, id, definition, modifiers)*charge, modifiers.range());
        ctx.memory.put("charge",charge);
		playCastSound(ctx);
		TechniqueFx.castPose(ctx);
		if (definition.cast().castTime() > 0) {
			ctx.fx(TechniqueFxPayload.Phase.WINDUP, caster.position(), caster.getLookAngle(), List.of());
			PENDING.add(new Pending(ctx, id, definition, level.getGameTime() + definition.cast().castTime(), spent, slot));
			return CastFailure.NONE;
		}

		execute(ctx, id, definition, spent, slot);
		return CastFailure.NONE;
	}

    private static TechniqueDefinition chargedDefinition(TechniqueDefinition d,double q,int held){
        double scale=.5+.5*q;var c=d.cast();
        int mode=d.behavior() instanceof com.jjkcraft.technique.behavior.InfinityBehavior b?b.filter():0;
        var tuning=new TechniqueTuning((int)Math.round(q*100),(int)Math.round(scale*100),(int)Math.round((.6+.4*q)*100),(int)Math.round(scale*100),100,mode);
        var cast=new TechniqueDefinition.Cast(c.cost(),c.cooldown(),Math.max(0,c.castTime()-held),c.range()*scale,c.mode(),c.upkeep()*Math.max(.5,q),c.maxDuration()==0?0:Math.max(1,(int)(c.maxDuration()*scale)),c.mulCost(),c.chargeable());
        return new TechniqueDefinition(d.name(),d.description(),d.category(),d.grade(),cast,TunedBehaviors.apply(d.behavior(),tuning),d.visuals(),d.icon(),d.vowModifiers(),d.innateWeight(),d.masteryUses());
    }
	public static CastFailure check(LivingEntity caster, Identifier id, TechniqueDefinition definition) {
        var pactFailure=PersonalPacts.check(caster,id,definition);if(pactFailure!=CastFailure.NONE)return pactFailure;
		if (caster.hasEffect(JjkEffects.STUNNED)) {
			return CastFailure.STUNNED;
		}

		for (CastBlocker blocker : CAST_BLOCKERS) {
			CastFailure blocked = blocker.check(caster, id, definition);
			if (blocked != null && blocked != CastFailure.NONE) {
				return blocked;
			}
		}

		if (caster instanceof Player player) {
			SorcererProgress progress = Progression.get(player);
			if (progress.heavenlyRestricted() && payer(caster, id, definition).isEmpty()) {
				return CastFailure.HEAVENLY_RESTRICTION;
			}

			if (!knows(player, id) && !TechniqueSources.grants(player, id)) {
				return CastFailure.NOT_KNOWN;
			}

			int rank = Grades.rank(player.registryAccess(), progress.grade());
			if (rank < Techniques.gradeRank(player.registryAccess(), definition)) {
				return CastFailure.GRADE_TOO_LOW;
			}
		}

		if (caster.level().getGameTime() < readyAt(caster, id)) {
			return CastFailure.ON_COOLDOWN;
		}

		return CastFailure.NONE;
	}

	private static void execute(CastContext ctx, Identifier id, TechniqueDefinition definition, double cost, int slot) {
		LivingEntity caster = ctx.caster();
		CastResult result = definition.behavior().cast(ctx);
		if(ctx.details().expression().recoil()>0){caster.push(ctx.look().scale(-ctx.details().expression().recoil()/150.0));caster.needsSync=true;}
		if (definition.cast().mode() == CastMode.INSTANT) {
			startCooldown(caster, id, definition);
		} else {
			ACTIVE.put(caster.getUUID(), new Active(ctx, id, definition, slot));
		}

		for (CastListener listener : CAST_LISTENERS) {
			listener.onCast(caster, id, definition);
		}

		if (caster instanceof ServerPlayer player) {
			Progression.recordCast(player, cost, result.landed());
			if (result.landed()) {
				addMastery(player, id, definition);
			}

			SYNC.markDirty(player);
		}
	}

	/** Credits a delayed hit (a bolt landing) to the caster's Control and mastery. */
	public static void creditHit(LivingEntity caster, Identifier id) {
		if (caster instanceof ServerPlayer player) {
			Progression.set(player, Progression.get(player).withLandedCredit());
			Techniques.get(player.registryAccess(), id).ifPresent(def -> addMastery(player, id, def));
		}
	}

	private static void addMastery(ServerPlayer player, Identifier id, TechniqueDefinition definition) {
		TechniqueState state = get(player);
		int before = state.masteryOf(id);
		set(player, state.withMasteryAdded(id, 1));
		if (before < definition.masteryUses() && before + 1 >= definition.masteryUses()) {
			Progression.addCounter(player, ProgressionMath.MASTERED, 1);
			player.sendSystemMessage(Component.translatable("message.jjkcraft.mastered", Component.translatable(definition.name())));
		}
	}

	public static boolean isMastered(LivingEntity caster, Identifier id, TechniqueDefinition definition) {
		return caster instanceof Player player && get(player).masteryOf(id) >= definition.masteryUses();
	}

	public static Modifiers modifiers(LivingEntity caster, Identifier id, TechniqueDefinition definition) {
		Modifiers result = Modifiers.NONE;
		for (ModifierProvider provider : MODIFIER_PROVIDERS) {
			result = result.times(provider.get(caster, id, definition));
		}

		return result;
	}

	public static double effectiveCost(LivingEntity caster, Identifier id, TechniqueDefinition definition, Modifiers modifiers) {
		int control = caster instanceof Player player ? Progression.get(player).growth().control() : 0;
		return TechniqueMath.cost(definition.cast().cost(), control, isMastered(caster, id, definition), modifiers.cost());
	}

	public static double effectiveCost(LivingEntity caster, Identifier id, TechniqueDefinition definition) {
		return effectiveCost(caster, id, definition, modifiers(caster, id, definition));
	}

	public static double power(LivingEntity caster, Identifier id, TechniqueDefinition definition, Modifiers modifiers) {
		int rank = Progression.gradeRank(caster.registryAccess(), caster);
		int output = caster instanceof Player player ? Progression.get(player).growth().output() : 0;
		return TechniqueMath.power(rank, output, isMastered(caster, id, definition), modifiers.damage()) * Personalization.get(caster,id).output()/100.0 * Personalization.get(caster,id).pact().output();
	}

	public static int effectiveCooldown(LivingEntity caster, TechniqueDefinition definition) {
		int efficiency = caster instanceof Player player ? Progression.get(player).growth().efficiency() : 0;
		return TechniqueMath.cooldown(definition.cast().cooldown(), efficiency);
	}

	public static long readyAt(LivingEntity caster, Identifier id) {
		if (caster instanceof Player player) {
			return get(player).readyAt(id);
		}

		return MOB_COOLDOWNS.getOrDefault(caster.getUUID(), Map.of()).getOrDefault(id, 0L);
	}

	private static void startCooldown(LivingEntity caster, Identifier id, TechniqueDefinition definition) {
		long now = caster.level().getGameTime();
		long readyAt = now + effectiveCooldown(caster, definition);
		if (caster instanceof ServerPlayer player) {
			set(player, get(player).withCooldown(id, readyAt, now));
		} else {
			MOB_COOLDOWNS.computeIfAbsent(caster.getUUID(), k -> new HashMap<>()).put(id, readyAt);
		}
	}

	public static void resetCooldowns(ServerPlayer player) {
		set(player, get(player).withoutCooldowns());
	}

	/** Ends the caster's active channel or toggle, starting its cooldown. */
	public static void stop(LivingEntity caster) {
		Active active = ACTIVE.remove(caster.getUUID());
		if (active == null) {
			return;
		}

		active.behavior().end(active.ctx);
		startCooldown(caster, active.id, active.definition);
		if (caster instanceof ServerPlayer player) {
			SYNC.markDirty(player);
		}
	}
	public static void cancel(LivingEntity caster){cancelCharge(caster);PENDING.removeIf(p->p.ctx().caster().getUUID().equals(caster.getUUID()));stop(caster);}

	private static void playCastSound(CastContext ctx) {
		ctx.level().playSound(null, ctx.caster().getX(), ctx.caster().getY(), ctx.caster().getZ(),
				ctx.technique().visuals().sound().flatMap(JjkSounds::byId).orElse(JjkSounds.CAST),
				ctx.caster() instanceof Player ? SoundSource.PLAYERS : SoundSource.HOSTILE,
				0.9f, 0.92f + ctx.caster().getRandom().nextFloat() * 0.16f);
	}

	// ---------------------------------------------------------------- ticking

	private static void tick(MinecraftServer server) {
        for(Charge charge:List.copyOf(CHARGES.values())){
            var player=charge.player;
            if(!player.isAlive()||player.isRemoved()||player.isSpectator()||player.level()!=charge.level||charge.ticks>=400||check(player,charge.id,charge.definition)!=CastFailure.NONE||!Personalization.definition(player,charge.id,Techniques.get(player.registryAccess(),charge.id).orElse(charge.definition)).equals(charge.definition)){
                cancelCharge(player);continue;
            }
            charge.ticks++;double drain=charge.drain();var payer=payer(player,charge.id,charge.definition);
            if(!(payer.isPresent()?payer.get().pay(player,charge.id,charge.definition,drain):CursedEnergy.trySpend(player,drain))){cancelCharge(player);feedback(player,charge.slot,CastFailure.NOT_ENOUGH_ENERGY);continue;}
            charge.spent+=drain;
            if(charge.ticks%2==0)charge.sync();
            if(charge.ticks%8==0)new CastContext(player,player.level(),charge.id,charge.definition,charge.fraction(),1).fx(TechniqueFxPayload.Phase.WINDUP,player.getEyePosition().add(player.getLookAngle().scale(1.2)));
        }
		if (!PENDING.isEmpty()) {
			for (Iterator<Pending> it = PENDING.iterator(); it.hasNext(); ) {
				Pending pending = it.next();
				LivingEntity caster = pending.ctx().caster();
				if (!caster.isAlive() || caster.isRemoved()) {
					it.remove();
					continue;
				}

				if (pending.ctx().level().getGameTime() >= pending.executeAt()) {
					it.remove();
					if (!caster.hasEffect(JjkEffects.STUNNED) && PersonalPacts.check(caster,pending.id(),pending.definition())==CastFailure.NONE) {
						execute(pending.ctx(), pending.id(), pending.definition(), pending.costPaid(), pending.slot());
					}
				}
			}
		}

		if (ACTIVE.isEmpty()) {
			return;
		}

		for (Active active : List.copyOf(ACTIVE.values())) {
			LivingEntity caster = active.ctx.caster();
			if (!caster.isAlive() || caster.isRemoved() || caster.hasEffect(JjkEffects.STUNNED) || PersonalPacts.check(caster,active.id,active.definition)!=CastFailure.NONE) {
				stop(caster);
				continue;
			}

			active.ticks++;
			double upkeep = active.definition.cast().upkeep() / 20.0 * modifiers(caster, active.id, active.definition).cost();
			int maxDuration = active.definition.cast().maxDuration();
			boolean keep = (upkeep <= 0 || CursedEnergy.trySpend(caster, upkeep))
					&& (maxDuration <= 0 || active.ticks < maxDuration)
					&& active.behavior().tick(active.ctx, active.ticks);
			if (!keep) {
				stop(caster);
			}
		}
	}
}
