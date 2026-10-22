package com.jjkcraft.client.vfx;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.client.state.ClientTechniques;
import com.jjkcraft.network.payload.TechniqueFxPayload;
import com.jjkcraft.particle.ColoredParticleOptions;
import com.jjkcraft.particle.JjkParticles;
import com.jjkcraft.sound.JjkSounds;
import com.jjkcraft.technique.TechniqueDefinition;

/**
 * Turns {@link TechniqueFxPayload}s into visuals. All looks come from the synced technique data
 * (color, particle, sounds, trail, shake, hit-stop), so a datapack technique gets visuals without
 * client code.
 */
public final class TechniqueFxClient {
	/** Generation tokens prevent a restarted channel from reviving its previous effects. */
	private static final Map<String,Long> REMOTE_ACTIVE = new HashMap<>();
	private static long generation;

	private TechniqueFxClient() {
	}

	public static void reset() {
		REMOTE_ACTIVE.clear();
        SpectralFx.clear();
        TexturedFx.clear();
        ChargeFx.clear();
        TechniqueMaterials.clear();
        com.jjkcraft.client.render.CursedBoltRenderer.clearHistory();
        SpeechFx.clear();
	}

	public static void handle(TechniqueFxPayload fx) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			return;
		}

		Optional<TechniqueDefinition> found = ClientTechniques.definition(fx.technique());
		if (found.isEmpty()) {
			return;
		}

		TechniqueDefinition def = found.get();
		if(fx.trail()>=0){var v=def.visuals();var trail=TechniqueDefinition.Trail.values()[Math.min(TechniqueDefinition.Trail.values().length-1,fx.trail())];
			def=new TechniqueDefinition(def.name(),def.description(),def.category(),def.grade(),def.cast(),def.behavior(),new TechniqueDefinition.Visuals(v.color(),v.particle(),v.sound(),v.hitSound(),v.shake(),v.hitStop(),trail),def.icon(),def.vowModifiers(),def.innateWeight(),def.masteryUses());}
		if (def.behavior() instanceof com.jjkcraft.technique.behavior.SummonBehavior && fx.phase() == TechniqueFxPayload.Phase.BURST)
			for (int summoned : fx.targets()) AdvancedFx.summon(fx.caster(), summoned);
		if (fx.technique().getPath().equals("projection_sorcery") && fx.phase() == TechniqueFxPayload.Phase.BURST) AdvancedFx.projection(fx.caster());
		TechniqueDefinition.Visuals v = def.visuals();
		int color = fx.color()<0?v.color():fx.color();
		ParticleOptions particle = particle(v, color, 1f);
		Entity caster = mc.level.getEntity(fx.caster());
		float now = mc.level.getGameTime() + mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		boolean involvesMe = mc.player != null && (fx.caster() == mc.player.getId() || fx.targets().contains(mc.player.getId()));
        if(fx.phase()==TechniqueFxPayload.Phase.WINDUP&&def.cast().chargeable()){ChargeFx.update(fx,now);return;}
        if(fx.phase()==TechniqueFxPayload.Phase.CAST||fx.phase()==TechniqueFxPayload.Phase.CHANNEL_START||fx.phase()==TechniqueFxPayload.Phase.FIELD)ChargeFx.stop(fx.caster());
        boolean speech=SpeechFx.event(fx,def,color,now);
        if(!speech)TechniqueMaterials.event(fx,color,now);
        boolean spectral=speech||SpectralFx.event(fx,color,now);

		switch (fx.phase()) {
			case WINDUP -> {if(!spectral)windup(caster, def, color);}
			case CAST -> {if(!spectral)cast(fx, def, color, particle, caster, now);}
			case HIT -> {
				ClientFx.burst(particle, fx.origin(), com.jjkcraft.client.config.ClientConfig.lowVfx()?2:5, 0.20, 0.10);
				ClientFx.burst(JjkParticles.of(JjkParticles.CURSED_SPARK, color, 0.6f), fx.origin(), 2, 0.2, 0.25);
				sound(v.hitSound(), fx.origin(), SoundMixer.Priority.IMPACT, 1f);
				if (def.visuals().trail() == TechniqueDefinition.Trail.CHAIN && !fx.targets().isEmpty()) {
					WorldFx.add(WorldFx.chain(fx.caster(), fx.targets().getFirst(), color, now, (float) Math.max(10, fx.direction().x)));
				}

				if (involvesMe) {
					ClientFx.shake(v.shake(), 220 + v.hitStop() * 40);
				}

				hitStop(fx, v.hitStop());
			}
			case FIELD -> {if(!spectral)field(fx, color,particle,now);}
			case BURST -> {
				double radius = Math.max(1, fx.direction().x);
				if(!spectral)WorldFx.add(WorldFx.burst(fx.origin(), radius, color, now));
				ClientFx.burst(particle, fx.origin(), spectral?4:8, radius * 0.35, 0.25);
				ClientFx.burst(JjkParticles.of(JjkParticles.DOMAIN_SHARD, color, .8f), fx.origin(), spectral?2:6, radius * 0.3, 0.3);
				sound(Optional.of(JjkSounds.REPEL.location()), fx.origin(), SoundMixer.Priority.DRAMATIC, 1.2f);
				if (mc.player != null && mc.player.position().distanceTo(fx.origin()) < radius * 4) {
					ClientFx.shake(0.6f + 0.2f * (float) radius / 5, 450);
				}

				hitStop(fx, Math.max(2, v.hitStop()));
			}
			case CHANNEL_START -> {
                if(def.behavior() instanceof com.jjkcraft.technique.behavior.SingularityBehavior)break;
				String key = fx.caster() + ":" + fx.technique();
				if(REMOTE_ACTIVE.containsKey(key))break;
				long token=++generation;REMOTE_ACTIVE.put(key,token);
				java.util.function.BooleanSupplier current=()->java.util.Objects.equals(REMOTE_ACTIVE.get(key),token);
				boolean mine = mc.player != null && fx.caster() == mc.player.getId();
				boolean infinity=def.behavior() instanceof com.jjkcraft.technique.behavior.InfinityBehavior;
				WorldFx.add(WorldFx.aura(fx.caster(), color, now,
						() -> current.getAsBoolean()&&(!mine||ClientTechniques.activeIs(fx.technique())),infinity));
				Emitters.add(age -> {
					boolean alive = current.getAsBoolean()&&(!mine||ClientTechniques.activeIs(fx.technique())||age<3);
					Entity e = mc.level == null ? null : mc.level.getEntity(fx.caster());
					if (!alive || e == null) {
						REMOTE_ACTIVE.remove(key,token);
						return false;
					}

					if (age % 3 == 0 && !(infinity && mine && mc.options.getCameraType().isFirstPerson())) {
						ClientFx.burst(particle, e.position().add(0, e.getBbHeight() * 0.5, 0), 3, e.getBbWidth(), 0.03);
					}

					return true;
				});
			}
			case CHANNEL_END -> REMOTE_ACTIVE.remove(fx.caster() + ":" + fx.technique());
			case BLOCK -> {
				ClientFx.burst(JjkParticles.of(JjkParticles.DOMAIN_SHARD, color, 0.8f), fx.origin(), 10, 0.3, 0.2);
				sound(v.hitSound(), fx.origin(), SoundMixer.Priority.IMPACT, 0.8f);
				if (involvesMe) {
					ClientFx.shake(0.2f, 150);
				}
			}
		}
	}

	private static void windup(Entity caster, TechniqueDefinition def, int color) {
		if (caster == null) {
			return;
		}

		int duration = Math.max(4, def.cast().castTime());
		ParticleOptions aura = JjkParticles.of(JjkParticles.CE_AURA, color, 1.2f);
		float start = caster.level().getGameTime();
		WorldFx.add(WorldFx.glyphRing(caster.position(), 1.6, color, start, duration, true));
		Emitters.add(age -> {
			if (age > duration || caster.isRemoved()) {
				return false;
			}

			// Wisps spiral in toward the caster's hands: a clear telegraph for the windup.
			double a = age * 0.6;
			double r = 1.6 * (1 - (double) age / duration) + 0.2;
			Vec3 p = caster.position().add(Math.cos(a) * r, caster.getBbHeight() * 0.6, Math.sin(a) * r);
			ClientFx.burst(aura, p, 2, 0.1, 0.01);
			return true;
		});
	}

	private static void cast(TechniqueFxPayload fx, TechniqueDefinition def, int color, ParticleOptions particle, Entity caster, float now) {
		Vec3 dir = fx.direction();
		Vec3 aim=EffectAim.forward(fx,caster);double yaw = Math.atan2(-aim.x, aim.z);
		switch (def.visuals().trail()) {
			case SLASH -> {
				WorldFx.add(WorldFx.slash(fx.origin(), yaw, Math.max(2.5, def.cast().range() * 0.7), 140, Math.toRadians(32), color, now));
				ClientFx.burst(particle, fx.origin().add(dir.scale(2)), 10, 1.2, 0.1);
			}
			case LINE -> {
				Vec3 end = fx.origin().add(dir);
				WorldFx.add(WorldFx.line(fx.origin().add(0, -0.2, 0), end, 0.35, color, now, 8));
				ClientFx.line(particle, fx.origin(), end, 16, 0.05);
			}
			case BEAM -> {
				// A heavy line with a white-hot core: ultimates, falling lances, mending beams.
				Vec3 end = fx.origin().add(dir);
				boolean positive=def.category()==com.jjkcraft.technique.TechniqueCategory.HEALING||def.behavior() instanceof com.jjkcraft.technique.behavior.MangaAbilityBehavior m&&m.ability().equals("positive_output");
				WorldFx.add(WorldFx.line(fx.origin(), end, positive?.18:.85, color, now, 14));
				WorldFx.add(WorldFx.line(fx.origin(), end, positive?.055:.22, 0xFFFFF4F0, now, 10));
				ClientFx.line(particle, fx.origin(), end, (int) Math.min(48, 6 + dir.length() * 1.5), 0.08);
			}
			case SHOUT -> {
				Vec3 at = caster != null ? caster.position().add(0, 0.2, 0) : fx.origin();
				WorldFx.add(WorldFx.shout(at, yaw, Math.max(3, dir.x), color, now));
				for (int id : fx.targets()) {
					Entity e = Minecraft.getInstance().level.getEntity(id);
					if (e != null) {
						ClientFx.burst(particle, e.position().add(0, e.getBbHeight(), 0), 6, 0.3, 0.05);
					}
				}

				if (Minecraft.getInstance().player != null && fx.targets().contains(Minecraft.getInstance().player.getId())) {
					ClientFx.shake(def.visuals().shake(), 400);
				}
			}
			default -> ClientFx.burst(particle, fx.origin(), 8, 0.25, 0.12);
		}
	}

	private static void field(TechniqueFxPayload fx, int color, ParticleOptions particle, float now) {
		double radius = fx.direction().x;
		int duration = (int) fx.direction().y;
		boolean attracts = fx.direction().z > 0;
		WorldFx.add(WorldFx.glyphRing(fx.origin(), radius, color, now, duration, attracts));
		WorldFx.add(WorldFx.burst(fx.origin(), radius * 0.6, color, now));
		Emitters.add(age -> {
			if (age > duration) {
				return false;
			}

			for (int i = 0; i < 3; i++) {
				double a = age * 0.35 + i * 2.09;
				double r = attracts ? radius * (1 - (age % 10) / 10.0) : radius * ((age % 10) / 10.0);
				Vec3 p = fx.origin().add(Math.cos(a) * r, Math.sin(a * 0.7) * radius * 0.4, Math.sin(a) * r);
				ClientFx.burst(particle, p, 1, 0.05, 0.02);
			}

			return true;
		});
	}

	private static void hitStop(TechniqueFxPayload fx, int ticks) {
		if (ticks > 0 && !fx.targets().isEmpty()) {
			int[] ids = fx.targets().stream().mapToInt(Integer::intValue).toArray();
			ClientFx.hitStop(ticks, ids);
		}
	}

	private static void sound(Optional<Identifier> id, Vec3 at, SoundMixer.Priority priority, float volume) {
		id.flatMap(JjkSounds::byId).ifPresent(sound -> SoundMixer.play(sound, at, priority, volume, 0.08f));
	}

	@SuppressWarnings("unchecked")
	private static ParticleOptions particle(TechniqueDefinition.Visuals v, int color, float scale) {
		ParticleType<?> type = v.particle().flatMap(BuiltInRegistries.PARTICLE_TYPE::getOptional).orElse(JjkParticles.CURSED_SPARK);
		if (type == JjkParticles.CURSED_SPARK || type == JjkParticles.CE_AURA || type == JjkParticles.REVERSE_GLOW
				|| type == JjkParticles.BLACK_LIGHTNING || type == JjkParticles.DOMAIN_SHARD || type == JjkParticles.DISSOLVE) {
			return JjkParticles.of((ParticleType<ColoredParticleOptions>) type, color, scale);
		}

		if (type instanceof ParticleOptions simple) {
			return simple;
		}

		return JjkParticles.of(JjkParticles.CURSED_SPARK, color, scale);
	}
}
