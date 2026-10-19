package com.jjkcraft.item;

import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** A Straw Doll's link: whom it is bound to, until when, how many nails are left and how hard each drives. */
public record StrawDollLink(UUID target, String name, long expires, int nails, float damage) {
	public static final Codec<StrawDollLink> CODEC = RecordCodecBuilder.create(i -> i.group(
			UUIDUtil.CODEC.fieldOf("target").forGetter(StrawDollLink::target),
			Codec.STRING.fieldOf("name").forGetter(StrawDollLink::name),
			Codec.LONG.fieldOf("expires").forGetter(StrawDollLink::expires),
			Codec.INT.fieldOf("nails").forGetter(StrawDollLink::nails),
			Codec.FLOAT.fieldOf("damage").forGetter(StrawDollLink::damage)
	).apply(i, StrawDollLink::new));

	public static final StreamCodec<ByteBuf, StrawDollLink> STREAM_CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC, StrawDollLink::target,
			ByteBufCodecs.STRING_UTF8, StrawDollLink::name,
			ByteBufCodecs.VAR_LONG, StrawDollLink::expires,
			ByteBufCodecs.VAR_INT, StrawDollLink::nails,
			ByteBufCodecs.FLOAT, StrawDollLink::damage,
			StrawDollLink::new);

	public StrawDollLink withNails(int remaining) {
		return new StrawDollLink(target, name, expires, remaining, damage);
	}
}
