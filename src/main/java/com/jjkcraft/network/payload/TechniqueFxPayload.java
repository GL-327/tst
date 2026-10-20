package com.jjkcraft.network.payload;

import java.util.List;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import com.jjkcraft.JjkCraft;

/**
 * Server to nearby clients: draw the visuals of a technique event. Carries only what the client
 * cannot look up itself; colors, particles, sounds and trail shapes come from the synced
 * technique definition.
 *
 * @param caster    caster entity id
 * @param technique technique id
 * @param phase     which moment of the cast this is
 * @param origin    where the effect starts
 * @param direction aim direction (normalized) or, for fields, (radius, duration, 0)
 * @param targets   ids of entities hit
 * @param power     relative strength, for scaling visuals (1 = normal)
 */
public record TechniqueFxPayload(int caster, Identifier technique, Phase phase, Vec3 origin, Vec3 direction, List<Integer> targets, float power,int color,int trail)
		implements CustomPacketPayload {
	public TechniqueFxPayload(int caster,Identifier technique,Phase phase,Vec3 origin,Vec3 direction,List<Integer> targets,float power){this(caster,technique,phase,origin,direction,targets,power,-1);}
	public TechniqueFxPayload(int caster,Identifier technique,Phase phase,Vec3 origin,Vec3 direction,List<Integer> targets,float power,int color){this(caster,technique,phase,origin,direction,targets,power,color,-1);}
	public static final Type<TechniqueFxPayload> TYPE = new Type<>(JjkCraft.id("technique_fx"));

	public enum Phase {
		WINDUP, CAST, HIT, CHANNEL_START, CHANNEL_END, FIELD, BURST, BLOCK;

		public static final StreamCodec<ByteBuf, Phase> STREAM_CODEC =
				ByteBufCodecs.idMapper(i -> values()[Math.floorMod(i, values().length)], Phase::ordinal);
	}

	public static final StreamCodec<ByteBuf, TechniqueFxPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, TechniqueFxPayload::caster,
			Identifier.STREAM_CODEC, TechniqueFxPayload::technique,
			Phase.STREAM_CODEC, TechniqueFxPayload::phase,
			Vec3.STREAM_CODEC, TechniqueFxPayload::origin,
			Vec3.STREAM_CODEC, TechniqueFxPayload::direction,
			ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(64)), TechniqueFxPayload::targets,
			ByteBufCodecs.FLOAT, TechniqueFxPayload::power,ByteBufCodecs.VAR_INT,TechniqueFxPayload::color,ByteBufCodecs.VAR_INT,TechniqueFxPayload::trail,
			TechniqueFxPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
