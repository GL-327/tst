package com.jjkcraft.client.state;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import com.jjkcraft.core.technique.TechniqueMath;
import com.jjkcraft.grade.Grades;
import com.jjkcraft.network.payload.CastFeedbackPayload;
import com.jjkcraft.network.payload.TechniqueSyncPayload;
import com.jjkcraft.technique.CastFailure;
import com.jjkcraft.technique.TechniqueDefinition;
import com.jjkcraft.technique.TechniqueState;
import com.jjkcraft.technique.Techniques;

/** The local player's techniques as last sent by the server, plus HUD feedback state. */
public final class ClientTechniques {
	private static TechniqueState state = TechniqueState.EMPTY;
	private static Optional<Identifier> active = Optional.empty();
	private static int feedbackSlot = -2;
	private static CastFailure feedback = CastFailure.NONE;
	private static long feedbackAt;

	private ClientTechniques() {
	}

	public static void accept(TechniqueSyncPayload payload) {
		state = payload.state();
		active = payload.active();
	}

	public static void accept(CastFeedbackPayload payload) {
		feedbackSlot = payload.slot();
		feedback = payload.failure();
		feedbackAt = System.currentTimeMillis();
	}

	public static void reset() {
		state = TechniqueState.EMPTY;
		active = Optional.empty();
		feedback = CastFailure.NONE;
	}

	public static TechniqueState state() {
		return state;
	}

	public static Optional<Identifier> active() {
		return active;
	}

	/** The failure to show for a slot, if it happened in the last 1.5 s. */
	public static CastFailure recentFailure(int slot) {
		if (System.currentTimeMillis() - feedbackAt > 1500) {
			return CastFailure.NONE;
		}

		return feedbackSlot == slot || (slot == state.selected() && feedbackSlot == -1) ? feedback : CastFailure.NONE;
	}

	public static float feedbackAge() {
		return (System.currentTimeMillis() - feedbackAt) / 1500f;
	}

	public static Optional<TechniqueDefinition> definition(Identifier id) {
		return ClientProgress.registries().flatMap(r -> Techniques.get(r, id));
	}

	/** Known techniques that exist in the current registry, innate first. */
	public static List<Identifier> known() {
		Set<Identifier> ids = new LinkedHashSet<>(state.known());
		List<Identifier> list = new ArrayList<>();
		for (Identifier id : ids) {
			if (definition(id).isPresent()) {
				list.add(id);
			}
		}

		return list;
	}

	public static int usableSlots() {
		return ClientProgress.grade().map(g -> g.techniqueSlots()).orElse(2);
	}

	/** Remaining cooldown as a fraction of the full cooldown (1 = just cast, 0 = ready). */
	public static float cooldownFraction(Identifier id) {
		long now = ClientEnergy.gameTime();
		long ready = state.readyAt(id);
		if (ready <= now) {
			return 0;
		}

		int full = definition(id).map(d -> TechniqueMath.cooldown(d.cast().cooldown(), ClientProgress.get().growth().efficiency())).orElse(1);
		return Math.min(1, (float) (ready - now) / Math.max(1, full));
	}

	public static boolean mastered(Identifier id) {
		return definition(id).map(d -> state.masteryOf(id) >= d.masteryUses()).orElse(false);
	}

	/** The CE cost the player would pay, without vow modifiers (shown in tooltips). */
	public static double cost(Identifier id) {
		return definition(id).map(d -> TechniqueMath.cost(d.cast().cost(), ClientProgress.get().growth().control(), mastered(id), 1.0)).orElse(0.0);
	}

	public static boolean affordable(Identifier id) {
		return ClientEnergy.predicted() + 1e-6 >= cost(id);
	}

	public static boolean gradeAllows(TechniqueDefinition definition) {
		return ClientProgress.registries().map(r -> Grades.rank(r, ClientProgress.get().grade()) >= Grades.rank(r, definition.grade())).orElse(false);
	}

	public static boolean activeIs(Identifier id) {
		return active.map(id::equals).orElse(false);
	}

	public static Minecraft mc() {
		return Minecraft.getInstance();
	}
}
