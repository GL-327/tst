package com.jjkcraft.network.payload;

import java.util.List;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import com.jjkcraft.JjkCraft;

/**
 * Server to client: every open domain and Simple Domain near the player. Sent when one opens,
 * changes phase or closes, and when the player arrives in a level; the client draws from it.
 */
public record DomainWorldPayload(List<View> domains, List<SimpleView> simple) implements CustomPacketPayload {
	public static final Type<DomainWorldPayload> TYPE = new Type<>(JjkCraft.id("domain_world"));

	/** One open domain. Variant and phase are ordinals of {@code DomainMath.Variant} and {@code ActiveDomain.Phase}. */
	public record View(int instance, int casterEntity, Identifier domain, double x, double y, double z, float radius, int variant, int phase,
			long phaseStart, long openedAt,float integrity) {
		public View(int instance,int casterEntity,Identifier domain,double x,double y,double z,float radius,int variant,int phase,long phaseStart,long openedAt){this(instance,casterEntity,domain,x,y,z,radius,variant,phase,phaseStart,openedAt,1);}
		public static final StreamCodec<ByteBuf, View> CODEC = StreamCodec.of((buf, v) -> {
			ByteBufCodecs.VAR_INT.encode(buf, v.instance);
			ByteBufCodecs.VAR_INT.encode(buf, v.casterEntity);
			Identifier.STREAM_CODEC.encode(buf, v.domain);
			buf.writeDouble(v.x);
			buf.writeDouble(v.y);
			buf.writeDouble(v.z);
			buf.writeFloat(v.radius);
			buf.writeByte(v.variant);
			buf.writeByte(v.phase);
			ByteBufCodecs.VAR_LONG.encode(buf, v.phaseStart);
			ByteBufCodecs.VAR_LONG.encode(buf, v.openedAt);
			buf.writeFloat(v.integrity);
		}, buf -> new View(ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), Identifier.STREAM_CODEC.decode(buf),
				buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readByte(), buf.readByte(),
				ByteBufCodecs.VAR_LONG.decode(buf), ByteBufCodecs.VAR_LONG.decode(buf),buf.readFloat()));
	}

	/** One Simple Domain, centered on its holder. */
	public record SimpleView(int casterEntity, float radius) {
		public static final StreamCodec<ByteBuf, SimpleView> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, SimpleView::casterEntity,
				ByteBufCodecs.FLOAT, SimpleView::radius,
				SimpleView::new);
	}

	public static final StreamCodec<ByteBuf, DomainWorldPayload> CODEC = StreamCodec.composite(
			View.CODEC.apply(ByteBufCodecs.list(64)), DomainWorldPayload::domains,
			SimpleView.CODEC.apply(ByteBufCodecs.list(256)), DomainWorldPayload::simple,
			DomainWorldPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
