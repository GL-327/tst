package com.jjkcraft.attachment;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.core.energy.EnergyState;
import com.jjkcraft.sorcerer.SorcererProgress;
import com.jjkcraft.technique.TechniqueState;

/**
 * Entity data attachments. None of these copy on death automatically: {@code SorcererLifecycle}
 * copies them with the configured death policy applied. Non-death respawns (leaving the End) copy
 * every attachment, which Fabric does by itself.
 */
public final class JjkAttachments {
	/** CE pool on players and cursed-capable mobs. */
	public static final AttachmentType<EnergyState> ENERGY = AttachmentRegistry.create(JjkCraft.id("energy"),
			builder -> builder.persistent(EnergyState.CODEC));

	/** Grade, growth, trials and awakening state on players. */
	public static final AttachmentType<SorcererProgress> PROGRESS = AttachmentRegistry.create(JjkCraft.id("progress"),
			builder -> builder.persistent(SorcererProgress.CODEC).initializer(() -> SorcererProgress.DEFAULT));

	/** Innate and learned techniques, loadout, mastery and cooldowns on players. */
	public static final AttachmentType<TechniqueState> TECHNIQUES = AttachmentRegistry.create(JjkCraft.id("techniques"),
			builder -> builder.persistent(TechniqueState.CODEC).initializer(() -> TechniqueState.EMPTY));

	private JjkAttachments() {
	}

	public static void init() {
		// Loads the class so the static fields register.
	}
}
