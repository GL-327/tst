package com.jjkcraft.vow;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.core.progress.ProgressionMath;
import com.jjkcraft.core.vow.VowMath;
import com.jjkcraft.damage.DamageModifiers;
import com.jjkcraft.domain.DomainService;
import com.jjkcraft.effect.JjkEffects;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.network.JjkNetworking;
import com.jjkcraft.network.PlayerSyncQueue;
import com.jjkcraft.network.payload.CastFeedbackPayload;
import com.jjkcraft.network.payload.VowPayloads;
import com.jjkcraft.registry.JjkRegistries;
import com.jjkcraft.sorcerer.Awakening;
import com.jjkcraft.sorcerer.PlayerActions;
import com.jjkcraft.sorcerer.Progression;
import com.jjkcraft.sorcerer.SorcererLifecycle;
import com.jjkcraft.sound.JjkSounds;
import com.jjkcraft.technique.CastFailure;
import com.jjkcraft.technique.TechniqueDefinition;
import com.jjkcraft.technique.TechniqueService;

/**
 * Binding Vows on the server: swearing (validated), the restrictions (breakable ones end the vow,
 * enforced ones refuse), the power (damage, cost, pool, regen, technique vow modifiers), fulfilment
 * at the end of the term, and the price of breaking: CE loss, a stun and a debt of halved regen.
 */
public final class Vows {
	/** Why a vow ended, for listeners (Vow Kin flees when its quarry's vow breaks). */
	public enum Ending {
		FULFILLED, BROKEN, RENOUNCED
	}

	@FunctionalInterface
	public interface EndListener {
		void onEnded(ServerPlayer player, VowState vow, Ending ending);
	}

	public static final AttachmentType<VowState> STATE = AttachmentRegistry.create(JjkCraft.id("vow"),
			builder -> builder.persistent(VowState.CODEC).initializer(() -> VowState.EMPTY));

	private static final PlayerSyncQueue.Channel SYNC = PlayerSyncQueue.channel("vow", Vows::send);
	private static final List<EndListener> END_LISTENERS = new CopyOnWriteArrayList<>();

	private Vows() {
	}

	public static void addEndListener(EndListener listener) {
		END_LISTENERS.add(listener);
	}

	/** Swearing and renouncing from the client: a few requests a second at most. */
	private static final com.jjkcraft.core.RateLimiter LIMITER = new com.jjkcraft.core.RateLimiter(2, 3);

	public static void init() {
		ServerPlayNetworking.registerGlobalReceiver(VowPayloads.Commit.TYPE, (payload, context) -> {
			if (!LIMITER.tryAcquire(context.player().getUUID(), context.player().level().getGameTime())) {
				return;
			}

			VowMath.Focus focus = VowMath.Focus.values()[Math.floorMod(payload.focus(), VowMath.Focus.values().length)];
			VowMath.Term term = VowMath.Term.values()[Math.floorMod(payload.term(), VowMath.Term.values().length)];
			CastFailure failure = swear(context.player(), payload.vows(), focus, term);
			if (failure != CastFailure.NONE) {
				JjkNetworking.send(context.player(), new CastFeedbackPayload(DomainService.FEEDBACK_SLOT, failure));
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(VowPayloads.Renounce.TYPE, (payload, context) -> {
			if (LIMITER.tryAcquire(context.player().getUUID(), context.player().level().getGameTime())) {
				renounce(context.player());
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(Vows::tick);
		PlayerActions.addListener((player, kind, technique, target) -> {
			if (kind == PlayerActions.Kind.JUMP) {
				onJump(player);
			}
		});
		SorcererLifecycle.addFullSync(SYNC::sendNow);
		SorcererLifecycle.addDeathCopier((oldPlayer, newPlayer, policy, config) -> newPlayer.setAttached(STATE, state(oldPlayer)));
		Awakening.addResetHook(player -> set(player, VowState.EMPTY));

		TechniqueService.addModifierProvider((caster, id, definition) -> {
			if (!(caster instanceof Player player)) {
				return TechniqueService.Modifiers.NONE;
			}

			VowState vow = state(player);
			if (!vow.active()) {
				return TechniqueService.Modifiers.NONE;
			}

			double damage = VowMath.damageMultiplier(vow.focus(), vow.points());
			double cost = VowMath.costMultiplier(vow.focus(), vow.points());
			double range = 1;
			for (TechniqueDefinition.VowModifier modifier : definition.vowModifiers()) {
				if (vow.vows().contains(modifier.vow())) {
					damage *= modifier.damage();
					cost *= modifier.cost();
					range *= modifier.range();
				}
			}

			return new TechniqueService.Modifiers(damage, cost, range);
		});
		TechniqueService.addCastBlocker((caster, id, definition) -> {
			if (!(caster instanceof ServerPlayer player)) {
				return null;
			}

			for (Restriction restriction : restrictions(player)) {
				if (restriction == Restriction.INNATE_ONLY && !TechniqueService.get(player).innate().map(id::equals).orElse(false)) {
					return CastFailure.VOW;
				}

				if (restriction == Restriction.NIGHT_ONLY && !dark(player)) {
					return CastFailure.VOW;
				}
			}

			return null;
		});
		DomainService.addExpandBlocker(caster -> caster instanceof ServerPlayer player && restrictions(player).contains(Restriction.NO_DOMAIN)
				? CastFailure.VOW : null);
		CursedEnergy.addCapacityModifier((entity, capacity) -> {
			if (!(entity instanceof Player player)) {
				return capacity;
			}

			VowState vow = state(player);
			if (!vow.active()) {
				return capacity;
			}

			double next = capacity * VowMath.capacityMultiplier(vow.focus(), vow.points());
			return restrictions(player).contains(Restriction.HALVED_RESERVE) ? next * 0.5 : next;
		});
		CursedEnergy.addRegenModifier(entity -> {
			if (!(entity instanceof Player player)) {
				return 1.0;
			}

			VowState vow = state(player);
			double multiplier = vow.active() ? VowMath.regenMultiplier(vow.focus(), vow.points()) : 1.0;
			return vow.inDebt(entity.level().getGameTime()) ? multiplier * VowMath.DEBT_REGEN : multiplier;
		});
		DamageModifiers.add((level, target, source, amount) -> {
			if (source.getEntity() instanceof Player player && source.getDirectEntity() == player && source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)) {
				VowState vow = state(player);
				if (vow.active()) {
					return amount * (float) VowMath.meleeMultiplier(vow.focus(), vow.points());
				}
			}

			return amount;
		});
	}

	// ---------------------------------------------------------------- lookups

	public static Optional<Registry<VowDefinition>> registry(RegistryAccess access) {
		return access.lookup(JjkRegistries.VOW);
	}

	public static Optional<VowDefinition> get(RegistryAccess access, Identifier id) {
		return registry(access).flatMap(r -> r.getOptional(id));
	}

	public record Entry(Identifier id, VowDefinition definition) {
	}

	/** Every vow, heaviest first. */
	public static List<Entry> all(RegistryAccess access) {
		return registry(access).map(r -> r.listElements()
				.map(h -> new Entry(h.key().identifier(), h.value()))
				.sorted(Comparator.<Entry>comparingDouble(e -> -e.definition().weight()).thenComparing(e -> e.id().toString()))
				.toList()).orElse(List.of());
	}

	public static VowState state(Player player) {
		return player.getAttachedOrCreate(STATE);
	}

	public static boolean active(LivingEntity entity) {
		return entity instanceof Player player && state(player).active();
	}

	public static List<Restriction> restrictions(Player player) {
		VowState vow = state(player);
		List<Restriction> out = new ArrayList<>();
		for (Identifier id : vow.vows()) {
			get(player.registryAccess(), id).ifPresent(def -> out.add(def.restriction()));
		}

		return out;
	}

	public static double totalWeight(RegistryAccess access, List<Identifier> ids) {
		return ids.stream().mapToDouble(id -> get(access, id).map(VowDefinition::weight).orElse(0.0)).sum();
	}

	private static void set(ServerPlayer player, VowState state) {
		CursedEnergy.checkpoint(player);
		player.setAttached(STATE, state);
		SYNC.markDirty(player);
	}

	/** Sends the vow state to the client again (after a command changed it directly). */
	public static void resync(ServerPlayer player) {
		SYNC.markDirty(player);
	}

	private static void send(ServerPlayer player) {
		JjkNetworking.send(player, new VowPayloads.Sync(state(player)));
	}

	// ---------------------------------------------------------------- swearing and ending

	public static CastFailure swear(ServerPlayer player, List<Identifier> requested, VowMath.Focus focus, VowMath.Term term) {
		List<Identifier> ids = List.copyOf(new LinkedHashSet<>(requested));
		VowState current = state(player);
		long now = player.level().getGameTime();
		if (!Progression.get(player).awakened()) {
			return CastFailure.DISABLED;
		}

		if (current.active()) {
			return CastFailure.BUSY;
		}

		if (current.inDebt(now)) {
			return CastFailure.VOW;
		}

		if (ids.isEmpty() || ids.size() > VowMath.MAX_RESTRICTIONS || ids.stream().anyMatch(id -> get(player.registryAccess(), id).isEmpty())) {
			return CastFailure.DISABLED;
		}

		// A vow can't be sworn while already breaking it.
		for (Identifier id : ids) {
			Restriction restriction = get(player.registryAccess(), id).orElseThrow().restriction();
			if (violating(player, restriction)) {
				return CastFailure.VOW;
			}
		}

		double points = VowMath.points(totalWeight(player.registryAccess(), ids), term);
		set(player, current.sworn(ids, focus, term, now, points));
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), JjkSounds.CHAIN, SoundSource.PLAYERS, 1f, 0.6f);
		player.sendSystemMessage(Component.translatable("message.jjkcraft.vow_sworn", String.format("%.1f", points)));
		return CastFailure.NONE;
	}

	/** Ends the vow early, by choice: half the price of breaking it. */
	public static void renounce(ServerPlayer player) {
		if (state(player).active()) {
			end(player, Ending.RENOUNCED);
		}
	}

	/** The sorcerer did what they swore not to. */
	public static void breakVow(ServerPlayer player) {
		if (state(player).active()) {
			end(player, Ending.BROKEN);
		}
	}

	private static void end(ServerPlayer player, Ending ending) {
		VowState vow = state(player);
		long now = player.level().getGameTime();
		if (ending == Ending.FULFILLED) {
			set(player, vow.ended(true, vow.debtUntil()));
			player.sendSystemMessage(Component.translatable("message.jjkcraft.vow_fulfilled"));
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(), JjkSounds.HEAL, SoundSource.PLAYERS, 1f, 1.2f);
		} else {
			boolean renounced = ending == Ending.RENOUNCED;
			set(player, vow.ended(false, now + VowMath.debtTicks(vow.points(), renounced)));
			CursedEnergy.drain(player, CursedEnergy.max(player) * VowMath.breakDrain(vow.points(), renounced));
			player.addEffect(new MobEffectInstance(JjkEffects.STUNNED, renounced ? 20 : 40, 0, false, true));
			player.sendSystemMessage(Component.translatable(renounced ? "message.jjkcraft.vow_renounced" : "message.jjkcraft.vow_broken"));
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(), JjkSounds.OVEREXERT, SoundSource.PLAYERS, 1.2f, 0.6f);
		}

		for (EndListener listener : END_LISTENERS) {
			listener.onEnded(player, vow, ending);
		}
	}

	// ---------------------------------------------------------------- enforcement

	/** Called when the server sees the player jump. */
	public static void onJump(ServerPlayer player) {
		if (restrictions(player).contains(Restriction.NO_JUMP)) {
			breakVow(player);
		}
	}

	private static boolean violating(ServerPlayer player, Restriction restriction) {
		return switch (restriction) {
			case NO_SPRINT -> player.isSprinting();
			case NO_ARMOR -> !player.getItemBySlot(EquipmentSlot.HEAD).isEmpty() || !player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
					|| !player.getItemBySlot(EquipmentSlot.LEGS).isEmpty() || !player.getItemBySlot(EquipmentSlot.FEET).isEmpty();
			case NO_SHIELD -> player.isBlocking();
			default -> false;
		};
	}

	/** Night, or deep enough underground that no sky reaches. */
	public static boolean dark(ServerPlayer player) {
		ServerLevel level = player.level();
		return level.isDarkOutside() || level.getBrightness(LightLayer.SKY, player.blockPosition()) < 4;
	}

	private static void tick(MinecraftServer server) {
		boolean second = server.getTickCount() % 20 == 0;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			VowState vow = state(player);
			if (!vow.active()) {
				continue;
			}

			long now = player.level().getGameTime();
			if (vow.endsAt() >= 0 && now >= vow.endsAt()) {
				end(player, Ending.FULFILLED);
				continue;
			}

			for (Restriction restriction : restrictions(player)) {
				if (restriction.breakable && violating(player, restriction)) {
					breakVow(player);
					break;
				}
			}

			if (second && state(player).active()) {
				Progression.addCounter(player, ProgressionMath.VOW_TICKS, 20);
			}
		}
	}
}
