package com.jjkcraft.client.state;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.client.config.ClientConfig;
import com.jjkcraft.client.hud.DomainTitle;
import com.jjkcraft.client.ui.InkToast;
import com.jjkcraft.client.vfx.ClientFx;
import com.jjkcraft.client.vfx.DomainFx;
import com.jjkcraft.core.domain.DomainMath;
import com.jjkcraft.domain.ActiveDomain;
import com.jjkcraft.domain.DomainDefinition;
import com.jjkcraft.domain.DomainService;
import com.jjkcraft.domain.Domains;
import com.jjkcraft.network.payload.CastFeedbackPayload;
import com.jjkcraft.network.payload.DomainEventPayload;
import com.jjkcraft.network.payload.DomainSelfPayload;
import com.jjkcraft.network.payload.DomainWorldPayload;
import com.jjkcraft.technique.CastFailure;

/** Domains as the server last described them, the player's own domain state, and recent domain moments. */
public final class ClientDomains {
	/** One open domain. */
	public record Domain(int instance, int casterEntity, Identifier id, Vec3 center, float radius, DomainMath.Variant variant,
			ActiveDomain.Phase phase, long phaseStart, long openedAt) {
		public Optional<DomainDefinition> definition() {
			Minecraft mc = Minecraft.getInstance();
			return mc.level == null ? Optional.empty() : Domains.get(mc.level.registryAccess(), id);
		}

		public boolean contains(Vec3 point) {
			return point.distanceToSqr(center) <= (double) radius * radius;
		}

		public boolean barrier() {
			return variant == DomainMath.Variant.FULL;
		}

		public boolean mine() {
			Minecraft mc = Minecraft.getInstance();
			return mc.player != null && mc.player.getId() == casterEntity;
		}
	}

	private static final Map<Integer, Domain> DOMAINS = new HashMap<>();
	private static List<DomainWorldPayload.SimpleView> simple = List.of();
	private static DomainSelfPayload self = new DomainSelfPayload(Optional.empty(), 0, -1, false, false);
	/** Recent gamble rolls per domain, newest first. */
	private static final Map<Integer, Deque<Integer>> ROLLS = new HashMap<>();
	private static final Map<Integer, Integer> REPEATS = new HashMap<>();
	private static final Map<Integer, Long> PULSES = new HashMap<>();
	private static CastFailure feedback = CastFailure.NONE;
	private static long feedbackAt;

	private ClientDomains() {
	}

	public static void accept(DomainWorldPayload payload) {
		Map<Integer, Domain> next = new HashMap<>();
		for (DomainWorldPayload.View v : payload.domains()) {
			next.put(v.instance(), new Domain(v.instance(), v.casterEntity(), v.domain(), new Vec3(v.x(), v.y(), v.z()), v.radius(),
					DomainMath.Variant.values()[Math.floorMod(v.variant(), DomainMath.Variant.values().length)],
					ActiveDomain.Phase.values()[Math.floorMod(v.phase(), ActiveDomain.Phase.values().length)], v.phaseStart(), v.openedAt()));
		}

		DOMAINS.clear();
		DOMAINS.putAll(next);
		ROLLS.keySet().retainAll(DOMAINS.keySet());
		REPEATS.keySet().retainAll(DOMAINS.keySet());
		PULSES.keySet().retainAll(DOMAINS.keySet());
		simple = List.copyOf(payload.simple());
	}

	public static void accept(DomainSelfPayload payload) {
		self = payload;
	}

	public static void accept(CastFeedbackPayload payload) {
		if (payload.slot() == DomainService.FEEDBACK_SLOT) {
			feedback = payload.failure();
			feedbackAt = System.currentTimeMillis();
		}
	}

	public static void accept(DomainEventPayload payload) {
		Minecraft mc = Minecraft.getInstance();
		DomainService.Event kind = DomainService.Event.values()[Math.floorMod(payload.kind(), DomainService.Event.values().length)];
		Domain domain = DOMAINS.get(payload.instance());
		long now = mc.level == null ? 0 : mc.level.getGameTime();
		switch (kind) {
			case OPEN -> {
				if (domain != null) {
					opened(domain);
				}
			}
			case CLASH -> {
				if (domain != null && (domain.mine() || near(domain))) {
					InkToast.show(Component.translatable("toast.jjkcraft.domain_clash"), name(domain), 0xFFE0303F);
				}
			}
			case CLASH_WON, CLASH_LOST -> {
				if (domain != null && domain.mine()) {
					boolean won = kind == DomainService.Event.CLASH_WON;
					InkToast.show(Component.translatable(won ? "toast.jjkcraft.clash_won" : "toast.jjkcraft.clash_lost"), name(domain),
							won ? 0xFF76C7A5 : 0xFFE0303F);
				}
			}
			case GAMBLE -> {
				Deque<Integer> rolls = ROLLS.computeIfAbsent(payload.instance(), k -> new ArrayDeque<>());
				rolls.addFirst(payload.value());
				while (rolls.size() > 3) {
					rolls.removeLast();
				}

				PULSES.put(payload.instance(), now);
			}
			case JACKPOT -> {
				if (domain != null && (domain.mine() || near(domain))) {
					InkToast.show(Component.translatable("toast.jjkcraft.jackpot"), name(domain), 0xFFF2C14E);
					ClientFx.shake(0.6f, 500);
				}
			}
			case REPEAT -> {
				REPEATS.put(payload.instance(), payload.value());
				PULSES.put(payload.instance(), now);
				if (domain != null) {
					DomainFx.onRepeat(domain, payload.value());
				}
			}
			case PULSE -> {
				PULSES.put(payload.instance(), now);
				if (domain != null) {
					DomainFx.onPulse(domain);
				}
			}
			default -> {
			}
		}
	}

	private static void opened(Domain domain) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}

		boolean close = domain.contains(mc.player.position()) || near(domain);
		if (!close) {
			return;
		}

		DomainTitle.start(domain);
		if (domain.mine()) {
			InkToast.show(Component.translatable("toast.jjkcraft.domain_expansion"), name(domain),
					domain.definition().map(d -> 0xFF000000 | d.visuals().color()).orElse(0xFF8F5CFF));
		}

		// The short hit-stop in the opening sequence: everything inside freezes for a beat.
		if (ClientConfig.get().vfx.hitStop && !ClientConfig.reducedMotion() && mc.level != null) {
			int[] ids = mc.level.getEntities((Entity) null, new net.minecraft.world.phys.AABB(domain.center(), domain.center()).inflate(domain.radius()),
					e -> domain.contains(e.position())).stream().mapToInt(Entity::getId).toArray();
			ClientFx.hitStop(6, ids);
		}

		ClientFx.shake(0.5f, 450);
	}

	private static boolean near(Domain domain) {
		Minecraft mc = Minecraft.getInstance();
		return mc.player != null && mc.player.position().distanceTo(domain.center()) < domain.radius() + 32;
	}

	public static Component name(Domain domain) {
		return domain.definition().map(d -> (Component) Component.translatable(d.name()).withColor(d.visuals().color()))
				.orElse(Component.literal(domain.id().toString()));
	}

	public static Map<Integer, Domain> domains() {
		return DOMAINS;
	}

	public static List<DomainWorldPayload.SimpleView> simpleDomains() {
		return simple;
	}

	public static DomainSelfPayload self() {
		return self;
	}

	public static Optional<Domain> own() {
		return Optional.ofNullable(DOMAINS.get(self.activeInstance()));
	}

	/** The domain whose space holds {@code point}: the smallest one if they overlap. */
	public static Optional<Domain> at(Vec3 point) {
		return DOMAINS.values().stream()
				.filter(d -> d.contains(point))
				.min((a, b) -> Float.compare(a.radius(), b.radius()));
	}

	/** Whether the local player stands in a Simple Domain (theirs or an ally's). */
	public static boolean shielded() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) {
			return false;
		}

		for (DomainWorldPayload.SimpleView view : simple) {
			Entity holder = mc.level.getEntity(view.casterEntity());
			if (holder != null && holder.distanceTo(mc.player) <= view.radius()) {
				return true;
			}
		}

		return false;
	}

	public static List<Integer> rolls(int instance) {
		return List.copyOf(ROLLS.getOrDefault(instance, new ArrayDeque<>()));
	}

	public static int repeats(int instance) {
		return REPEATS.getOrDefault(instance, 0);
	}

	public static long lastPulse(int instance) {
		return PULSES.getOrDefault(instance, Long.MIN_VALUE);
	}

	/** The latest domain refusal, if it is still fresh. */
	public static CastFailure feedback() {
		return System.currentTimeMillis() - feedbackAt < 2500 ? feedback : CastFailure.NONE;
	}

	public static void reset() {
		DOMAINS.clear();
		simple = List.of();
		self = new DomainSelfPayload(Optional.empty(), 0, -1, false, false);
		ROLLS.clear();
		REPEATS.clear();
		PULSES.clear();
		feedback = CastFailure.NONE;
	}
}
