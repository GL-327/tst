package com.jjkcraft.sorcerer;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import com.jjkcraft.attachment.JjkAttachments;
import com.jjkcraft.config.JjkConfig;
import com.jjkcraft.core.RateLimiter;
import com.jjkcraft.core.progress.ProgressionMath;
import com.jjkcraft.curse.CurseEntity;
import com.jjkcraft.curse.Curses;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.energy.EnergyProfile;
import com.jjkcraft.grade.GradeDefinition;
import com.jjkcraft.grade.GradedEntity;
import com.jjkcraft.grade.Grades;
import com.jjkcraft.grade.TrialType;
import com.jjkcraft.network.JjkNetworking;
import com.jjkcraft.network.PlayerSyncQueue;
import com.jjkcraft.network.payload.ProgressSyncPayload;
import com.jjkcraft.network.payload.PromotionPayload;
import com.jjkcraft.network.payload.SpendGrowthPayload;
import com.jjkcraft.sound.JjkSounds;
import com.jjkcraft.vfx.ServerFx;

/**
 * Server-side progression: grade experience, trial counters, promotion and growth points.
 * Every change goes through {@link #set} so that energy, Heavenly Restriction stats and the
 * client stay in step.
 */
public final class Progression {
	private static final PlayerSyncQueue.Channel SYNC = PlayerSyncQueue.channel("progress",
			player -> JjkNetworking.send(player, new ProgressSyncPayload(get(player))));
	private static final RateLimiter SPEND_LIMIT = new RateLimiter(10, 10);

	private Progression() {
	}

	public static void init() {
		ServerPlayNetworking.registerGlobalReceiver(SpendGrowthPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			if (player.isCreative() && SPEND_LIMIT.tryAcquire(player.getUUID(), player.level().getGameTime())) {
				spendGrowth(player, payload.path());
			}
		});

		ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((level, killer, killed, source) -> {
			if (killer instanceof ServerPlayer player) {
				onKill(player, killed);
			}
		});

		SorcererLifecycle.addFullSync(SYNC::sendNow);
	}

	public static SorcererProgress get(Player player) {
		return player.getAttachedOrCreate(JjkAttachments.PROGRESS);
	}

	/** Told when a player's grade changes (promotion, command, death policy). */
	@FunctionalInterface
	public interface GradeHook {
		void onGradeChanged(ServerPlayer player, Identifier from, Identifier to);
	}

	private static final List<GradeHook> GRADE_HOOKS = new CopyOnWriteArrayList<>();

	public static void addGradeHook(GradeHook hook) {
		GRADE_HOOKS.add(hook);
	}

	/** Stores progress, keeping CE regen, body stats and the client in step. */
	public static void set(ServerPlayer player, SorcererProgress progress) {
		SorcererProgress old = get(player);
		if (old.equals(progress)) {
			return;
		}

		CursedEnergy.checkpoint(player);
		player.setAttached(JjkAttachments.PROGRESS, progress);
		if (old.heavenlyRestricted() != progress.heavenlyRestricted() || !old.grade().equals(progress.grade())
				|| !old.growth().equals(progress.growth())) {
			HeavenlyRestriction.refresh(player);
		}

		SYNC.markDirty(player);
		if (!old.grade().equals(progress.grade())) {
			for (GradeHook hook : GRADE_HOOKS) {
				hook.onGradeChanged(player, old.grade(), progress.grade());
			}
		}
	}

	// ---------------------------------------------------------------- experience and counters

	public static void addXp(ServerPlayer player, int amount) {
		if (amount <= 0) {
			return;
		}

		int scaled = (int) Math.round(amount * JjkConfig.server().progression.gradeXpMultiplier);
		SorcererProgress progress = get(player);
		set(player, progress.withXp(progress.gradeXp() + scaled));
		tryPromote(player);
	}

	public static void addCounter(ServerPlayer player, String key, int amount) {
		set(player, get(player).withCounterAdded(key, amount));
		tryPromote(player);
	}

	/** Records a technique cast for Output and Control. Landed casts also give a little experience. */
	public static void recordCast(ServerPlayer player, double ceSpent, boolean landed) {
		set(player, get(player).withCast(ceSpent, landed));
		if (landed) {
			addXp(player, 1);
		}
	}

	/** Grade experience and curse counters for a kill. Players do not give experience; Culling Game points cover them. */
	public static void onKill(ServerPlayer killer, LivingEntity killed) {
		if (killed instanceof Player || !CursedEnergy.isCapable(killed)) {
			return;
		}

		int rank = gradeRank(killed.registryAccess(), killed);
		addXp(killer, ProgressionMath.killXp(rank));
		if (killed instanceof GradedEntity graded && graded.isCurse()) {
			addCounter(killer, ProgressionMath.curseKillKey(rank), 1);
		}

		if (killed instanceof CurseEntity curse) {
			addCounter(killer, Curses.codexKey(curse.curseId()), 1);
		}
	}

	/** The grade of any entity: a player's own grade, a curse's data grade, or an energy profile's tier. */
	public static Optional<Identifier> gradeOf(LivingEntity entity) {
		if (entity instanceof Player player) {
			return Optional.of(get(player).grade());
		}

		if (entity instanceof GradedEntity graded) {
			return Optional.of(graded.grade());
		}

		return CursedEnergy.profile(entity).map(EnergyProfile::grade);
	}

	public static int gradeRank(RegistryAccess access, LivingEntity entity) {
		return gradeOf(entity).map(id -> Grades.rank(access, id)).orElse(0);
	}

	// ---------------------------------------------------------------- growth

	public static int unspentPoints(RegistryAccess access, SorcererProgress progress) {
		int rank = Grades.rank(access, progress.grade());
		return ProgressionMath.pointsUnspent(progress.gradeXp(), rank, progress.growth().total());
	}

	public static boolean spendGrowth(ServerPlayer player, GrowthPath path) {
		SorcererProgress progress = get(player);
		if (unspentPoints(player.registryAccess(), progress) <= 0 || progress.growth().get(path) >= ProgressionMath.MAX_POINTS_PER_PATH) {
			return false;
		}

		// Heavenly Restriction has no CE, so the Domain path is closed to it.
		if (progress.heavenlyRestricted() && path == GrowthPath.DOMAIN) {
			return false;
		}

		set(player, progress.withGrowth(progress.growth().plus(path)));
		return true;
	}

	// ---------------------------------------------------------------- promotion

	public static ProgressionMath.Requirements requirements(RegistryAccess access, GradeDefinition grade) {
		List<ProgressionMath.TrialRequirement> trials = grade.trials().stream()
				.map(t -> new ProgressionMath.TrialRequirement(t.id(), kind(t.type()),
						Grades.get(access, t.grade()).map(GradeDefinition::rank).orElse(0), t.count()))
				.toList();
		return new ProgressionMath.Requirements(grade.xpRequired(), grade.minControl(), grade.minOutput(), grade.special(), trials);
	}

	public static ProgressionMath.Standing standing(SorcererProgress p) {
		return new ProgressionMath.Standing(p.gradeXp(), p.controlRatio(), p.ceSpentTotal(), p.castsAttempted(),
				p.counters(), p.trials(), p.heavenlyRestricted());
	}

	public static ProgressionMath.TrialKind kind(TrialType type) {
		return ProgressionMath.TrialKind.valueOf(type.name());
	}

	/** The grade the player would be promoted into next: the next normal grade, or the special grade from the top. */
	public static Optional<Grades.Entry> nextGrade(RegistryAccess access, SorcererProgress progress) {
		Optional<Grades.Entry> next = Grades.next(access, progress.grade());
		if (next.isPresent()) {
			return next;
		}

		boolean alreadySpecial = Grades.get(access, progress.grade()).map(GradeDefinition::special).orElse(false);
		return alreadySpecial ? Optional.empty() : Grades.special(access);
	}

	public static List<ProgressionMath.Blocker> blockers(RegistryAccess access, SorcererProgress progress) {
		return nextGrade(access, progress)
				.map(next -> ProgressionMath.promotionBlockers(requirements(access, next.definition()), standing(progress)))
				.orElse(List.of());
	}

	/** Promotes as many times as the player qualifies. Returns true if at least one promotion happened. */
	public static boolean tryPromote(ServerPlayer player) {
		boolean promoted = false;
		for (int guard = 0; guard < 16; guard++) {
			SorcererProgress progress = get(player);
			Optional<Grades.Entry> next = nextGrade(player.registryAccess(), progress);
			if (next.isEmpty() || !blockers(player.registryAccess(), progress).isEmpty()) {
				break;
			}

			promote(player, next.get());
			promoted = true;
		}

		return promoted;
	}

	/** Sets the grade, records completed trials, and plays the ceremony. Also used by {@code /jjk grade set}. */
	public static void promote(ServerPlayer player, Grades.Entry target) {
		SorcererProgress progress = get(player);
		Identifier from = progress.grade();
		SorcererProgress next = progress.withGrade(target.id(), progress.gradeXp());
		for (GradeDefinition.Trial trial : target.definition().trials()) {
			next = next.withTrial(trial.id());
		}

		if (target.definition().special()) {
			next = next.withSpecialUnlocked();
		}

		set(player, next);
		CursedEnergy.fill(player);
		ceremony(player, from, target);
	}

	/** Sets a grade directly without the ceremony (commands, death policy, tests). */
	public static void setGrade(ServerPlayer player, Identifier grade) {
		set(player, get(player).withGrade(grade, get(player).gradeXp()));
	}

	private static void ceremony(ServerPlayer player, Identifier from, Grades.Entry target) {
		JjkNetworking.send(player, new PromotionPayload(from, target.id()));
		ServerLevel level = player.level();
		level.playSound(null, player.getX(), player.getY(), player.getZ(), JjkSounds.PROMOTE, SoundSource.PLAYERS, 1.0f, 1.0f);
		ServerFx.particlesForOthers(level, player, ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(), 60, 0.6, 1.0, 0.6, 0.05);
		ServerFx.particlesForOthers(level, player, ParticleTypes.ENCHANT, player.getX(), player.getY() + 1, player.getZ(), 120, 0.8, 1.2, 0.8, 0.4);
		player.sendSystemMessage(Component.translatable("message.jjkcraft.promoted",
				Component.translatable(target.definition().name()).withColor(target.definition().color())));
	}
}
