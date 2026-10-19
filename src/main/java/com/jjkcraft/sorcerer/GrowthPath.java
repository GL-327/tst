package com.jjkcraft.sorcerer;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/** The four Growth Tree paths. */
public enum GrowthPath implements StringRepresentable {
	OUTPUT("output"),
	CONTROL("control"),
	EFFICIENCY("efficiency"),
	DOMAIN("domain");

	public static final Codec<GrowthPath> CODEC = StringRepresentable.fromEnum(GrowthPath::values);
	public static final StreamCodec<ByteBuf, GrowthPath> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[Math.floorMod(i, values().length)], GrowthPath::ordinal);

	private final String name;

	GrowthPath(String name) {
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return name;
	}

	public String translationKey() {
		return "growth.jjkcraft." + name;
	}
}
