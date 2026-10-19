package com.jjkcraft.client.vfx;

import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import com.jjkcraft.client.state.ClientDomains;
import com.jjkcraft.domain.ActiveDomain;
import com.jjkcraft.sound.JjkSounds;

/** The domain's ambient loop, fading in while the camera is inside an open domain and out after. */
public final class DomainAmbience extends AbstractTickableSoundInstance {
	private static DomainAmbience current;
	private float fade;

	private DomainAmbience(SoundEvent event) {
		super(event, SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
		this.looping = true;
		this.delay = 0;
		this.volume = 0.01f;
		this.relative = true;
		this.attenuation = SoundInstance.Attenuation.NONE;
	}

	private static Optional<ClientDomains.Domain> around(Minecraft mc) {
		return ClientDomains.at(mc.gameRenderer.mainCamera().position())
				.filter(d -> d.phase() == ActiveDomain.Phase.ACTIVE || d.phase() == ActiveDomain.Phase.CLASHING);
	}

	/** Called every client tick: starts the loop when the camera enters a domain. */
	public static void tickClient(Minecraft mc) {
		if (mc.level == null || mc.player == null) {
			current = null;
			return;
		}

		Optional<ClientDomains.Domain> domain = around(mc);
		if (domain.isPresent() && (current == null || current.isStopped())) {
			SoundEvent event = domain.get().definition().flatMap(d -> d.visuals().ambientSound()).flatMap(JjkSounds::byId).orElse(JjkSounds.DOMAIN_AMBIENT);
			current = new DomainAmbience(event);
			mc.getSoundManager().play(current);
		}
	}

	@Override
	public void tick() {
		Minecraft mc = Minecraft.getInstance();
		boolean inside = mc.level != null && around(mc).isPresent();
		fade = Math.max(0, Math.min(1, fade + (inside ? 0.05f : -0.05f)));
		volume = Math.max(0.01f, fade * 0.7f);
		if (!inside && fade <= 0) {
			stop();
		}
	}
}
