package com.jjkcraft.client.state;

import java.util.List;
import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;

import com.jjkcraft.core.progress.ProgressionMath;
import com.jjkcraft.grade.GradeDefinition;
import com.jjkcraft.grade.Grades;
import com.jjkcraft.network.payload.ProgressSyncPayload;
import com.jjkcraft.sorcerer.Progression;
import com.jjkcraft.sorcerer.SorcererProgress;

/** The local player's progression as last sent by the server, plus lookups against the synced grade registry. */
public final class ClientProgress {
	private static SorcererProgress progress = SorcererProgress.DEFAULT;
	private static boolean received;

	private ClientProgress() {
	}

	public static void accept(ProgressSyncPayload payload) {
		progress = payload.progress();
		received = true;
	}

	public static void reset() {
		progress = SorcererProgress.DEFAULT;
		received = false;
	}

	public static boolean hasData() {
		return received;
	}

	public static SorcererProgress get() {
		return progress;
	}

	public static Optional<RegistryAccess> registries() {
		Minecraft mc = Minecraft.getInstance();
		return mc.level == null ? Optional.empty() : Optional.of(mc.level.registryAccess());
	}

	public static Optional<GradeDefinition> grade() {
		return registries().flatMap(r -> Grades.get(r, progress.grade()));
	}

	public static Optional<Grades.Entry> nextGrade() {
		return registries().flatMap(r -> Progression.nextGrade(r, progress));
	}

	public static List<ProgressionMath.Blocker> blockers() {
		return registries().map(r -> Progression.blockers(r, progress)).orElse(List.of());
	}

	public static int unspentPoints() {
		return registries().map(r -> Progression.unspentPoints(r, progress)).orElse(0);
	}

	public static int rank() {
		return grade().map(GradeDefinition::rank).orElse(0);
	}
}
