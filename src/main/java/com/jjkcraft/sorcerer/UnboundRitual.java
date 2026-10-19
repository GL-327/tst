package com.jjkcraft.sorcerer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

import com.jjkcraft.core.progress.ProgressionMath;
import com.jjkcraft.curse.CurseEntity;
import com.jjkcraft.domain.ActiveDomain;
import com.jjkcraft.domain.DomainService;
import com.jjkcraft.grade.GradeDefinition;
import com.jjkcraft.grade.Grades;
import com.jjkcraft.grade.TrialType;
import com.jjkcraft.sound.JjkSounds;
import com.jjkcraft.technique.TechniqueService;
import com.jjkcraft.technique.Techniques;
import com.jjkcraft.vow.Vows;

/**
 * The Unbound Ritual, the only road to Special Grade (docs/DESIGN.md section 4). A Grade 1 sorcerer
 * who has mastered their innate technique, holds a Binding Vow, and defeats a Special Grade curse
 * inside their own Domain Expansion completes it. The ritual counter then satisfies the Special
 * Grade trial and the promotion follows.
 */
public final class UnboundRitual {
	public enum Missing {
		GRADE_ONE, INNATE_MASTERED, ACTIVE_VOW, OWN_DOMAIN
	}

	private UnboundRitual() {
	}

	public static void init() {
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (source.getEntity() instanceof ServerPlayer player && entity instanceof CurseEntity curse && isSpecialGrade(curse)) {
				attempt(player, curse);
			}
		});
	}

	private static boolean isSpecialGrade(CurseEntity curse) {
		return Grades.get(curse.registryAccess(), curse.grade()).map(GradeDefinition::special).orElse(false);
	}

	/** What the player still lacks to complete the ritual on a Special Grade kill at {@code where}. */
	public static List<Missing> missing(ServerPlayer player, LivingEntity where) {
		List<Missing> out = new ArrayList<>();
		SorcererProgress progress = Progression.get(player);
		// The grade the ritual asks for is set on the Special Grade's trial (Grade 1 by default).
		int needed = Grades.special(player.registryAccess())
				.flatMap(special -> special.definition().trials().stream().filter(t -> t.type() == TrialType.UNBOUND_RITUAL).findFirst())
				.map(t -> Grades.rank(player.registryAccess(), t.grade())).orElse(5);
		if (Grades.rank(player.registryAccess(), progress.grade()) < needed) {
			out.add(Missing.GRADE_ONE);
		}

		Optional<Identifier> innate = TechniqueService.get(player).innate();
		boolean mastered = innate.flatMap(id -> Techniques.get(player.registryAccess(), id))
				.map(def -> TechniqueService.isMastered(player, innate.get(), def)).orElse(false);
		if (!mastered) {
			out.add(Missing.INNATE_MASTERED);
		}

		if (!Vows.active(player)) {
			out.add(Missing.ACTIVE_VOW);
		}

		boolean inOwnDomain = DomainService.active(player)
				.filter(d -> d.phase() == ActiveDomain.Phase.ACTIVE && d.contains(where)).isPresent();
		if (!inOwnDomain) {
			out.add(Missing.OWN_DOMAIN);
		}

		return out;
	}

	private static void attempt(ServerPlayer player, CurseEntity curse) {
		if (Progression.get(player).specialUnlocked() || !missing(player, curse).isEmpty()) {
			return;
		}

		Progression.addCounter(player, ProgressionMath.UNBOUND_RITUAL, 1);
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), JjkSounds.PROMOTE, SoundSource.PLAYERS, 2f, 0.6f);
		player.sendSystemMessage(Component.translatable("message.jjkcraft.unbound_ritual"));
		Progression.tryPromote(player);
	}
}
