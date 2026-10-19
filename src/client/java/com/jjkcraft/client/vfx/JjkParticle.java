package com.jjkcraft.client.vfx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.RandomSource;

import com.jjkcraft.particle.ColoredParticleOptions;

/**
 * The mod's tinted, self-lit particle. Each type gets its own motion profile: sparks burst and
 * shrink, aura wisps rise, glows pulse, lightning flickers, shards spin and fall, ash drifts.
 */
public final class JjkParticle extends SingleQuadParticle {
	public enum Motion {
		SPARK, AURA, GLOW, LIGHTNING, SHARD, ASH
	}

	private final SpriteSet sprites;
	private final Motion motion;
	private final float baseSize;

	private JjkParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
			ColoredParticleOptions options, SpriteSet sprites, Motion motion, RandomSource random) {
		super(level, x, y, z, xd, yd, zd, sprites.first());
		this.sprites = sprites;
		this.motion = motion;
		this.hasPhysics = false;
		this.setColor(options.red(), options.green(), options.blue());
		this.xd = xd;
		this.yd = yd;
		this.zd = zd;
		switch (motion) {
			case SPARK -> {
				this.lifetime = 6 + random.nextInt(6);
				this.friction = 0.82f;
				this.quadSize = 0.12f;
			}
			case AURA -> {
				this.lifetime = 16 + random.nextInt(10);
				this.friction = 0.92f;
				this.gravity = -0.02f;
				this.quadSize = 0.22f;
			}
			case GLOW -> {
				this.lifetime = 20 + random.nextInt(10);
				this.friction = 0.9f;
				this.gravity = -0.01f;
				this.quadSize = 0.18f;
			}
			case LIGHTNING -> {
				this.lifetime = 3 + random.nextInt(3);
				this.friction = 0.5f;
				this.quadSize = 0.35f;
			}
			case SHARD -> {
				this.lifetime = 20 + random.nextInt(14);
				this.friction = 0.95f;
				this.gravity = 0.04f;
				this.quadSize = 0.14f;
				this.roll = random.nextFloat() * 6.28f;
			}
			case ASH -> {
				this.lifetime = 24 + random.nextInt(16);
				this.friction = 0.94f;
				this.gravity = -0.015f;
				this.quadSize = 0.1f;
			}
		}

		this.quadSize *= options.scale();
		this.baseSize = this.quadSize;
		this.setSpriteFromAge(sprites);
	}

	@Override
	public void tick() {
		super.tick();
		if (removed) {
			return;
		}

		setSpriteFromAge(sprites);
		float t = (float) age / lifetime;
		switch (motion) {
			case SPARK -> quadSize = baseSize * (1 - t);
			case GLOW -> quadSize = baseSize * (0.8f + 0.2f * (float) Math.sin(age * 0.6f));
			case SHARD -> {
				oRoll = roll;
				roll += 0.2f;
			}
			case ASH -> xd += (random.nextFloat() - 0.5f) * 0.01f;
			default -> {
			}
		}

		setAlpha(t < 0.7f ? 1f : 1 - (t - 0.7f) / 0.3f);
	}

	@Override
	protected SingleQuadParticle.Layer getLayer() {
		return SingleQuadParticle.Layer.TRANSLUCENT;
	}

	@Override
	public int getLightCoords(float partialTick) {
		return 0xF000F0;
	}

	/** Provider for one particle type with a fixed motion profile. */
	public record Provider(SpriteSet sprites, Motion motion) implements ParticleProvider<ColoredParticleOptions> {
		@Override
		public Particle createParticle(ColoredParticleOptions options, ClientLevel level, double x, double y, double z,
				double xd, double yd, double zd, RandomSource random) {
			return new JjkParticle(level, x, y, z, xd, yd, zd, options, sprites, motion, random);
		}
	}
}
