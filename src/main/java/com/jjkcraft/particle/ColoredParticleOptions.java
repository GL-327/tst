package com.jjkcraft.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Options for the mod's tinted particles: an RGB color and a size multiplier. */
public record ColoredParticleOptions(ParticleType<ColoredParticleOptions> type, int color, float scale) implements ParticleOptions {
	public static MapCodec<ColoredParticleOptions> codec(ParticleType<ColoredParticleOptions> type) {
		return RecordCodecBuilder.mapCodec(i -> i.group(
				Codec.INT.fieldOf("color").forGetter(ColoredParticleOptions::color),
				Codec.floatRange(0.05f, 10f).optionalFieldOf("scale", 1f).forGetter(ColoredParticleOptions::scale)
		).apply(i, (color, scale) -> new ColoredParticleOptions(type, color, scale)));
	}

	public static StreamCodec<ByteBuf, ColoredParticleOptions> streamCodec(ParticleType<ColoredParticleOptions> type) {
		return StreamCodec.composite(
				ByteBufCodecs.INT, ColoredParticleOptions::color,
				ByteBufCodecs.FLOAT, ColoredParticleOptions::scale,
				(color, scale) -> new ColoredParticleOptions(type, color, scale));
	}

	@Override
	public ParticleType<ColoredParticleOptions> getType() {
		return type;
	}

	public float red() {
		return ((color >> 16) & 0xFF) / 255f;
	}

	public float green() {
		return ((color >> 8) & 0xFF) / 255f;
	}

	public float blue() {
		return (color & 0xFF) / 255f;
	}
}
