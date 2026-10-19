package com.jjkcraft.culling.rule;

import java.util.Locale;
import java.util.UUID;

import com.mojang.serialization.Codec;

import org.jspecify.annotations.Nullable;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;

/**
 * Something that happened in the Culling Game, offered to every active rule. Built by
 * {@link com.jjkcraft.culling.CullingGame}; triggers read it, effects act on it.
 */
public final class RuleEvent {
	public enum Kind {
		/** A participant ended someone (or something) inside a colony. */
		KILL,
		/** Someone stepped inside a colony's barrier. */
		ENTER,
		/** A participant is outside their colony's barrier. */
		LEAVE,
		/** An awakened sorcerer's entry window closed without them joining. */
		DEADLINE,
		/** A day passed without the participant scoring. */
		IDLE,
		/** A new game day began. */
		TIMER,
		/** A participant's points changed. */
		POINTS,
		/** A participant asked for something (transfer, reveal, negotiate...). */
		ACTION,
		/** The rule itself was just added. */
		ADDED
	}

	/** Who died, for kill triggers. */
	public enum Victim implements StringRepresentable {
		ANY, PARTICIPANT, SORCERER, NON_SORCERER, CURSE, LEADER;

		public static final Codec<Victim> CODEC = StringRepresentable.fromEnum(Victim::values);

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	/** How they died, for kill triggers. */
	public enum Cause implements StringRepresentable {
		ANY, MELEE, PROJECTILE, TECHNIQUE;

		public static final Codec<Cause> CODEC = StringRepresentable.fromEnum(Cause::values);

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	/** What a participant can ask for; a rule must allow it. */
	public enum Action implements StringRepresentable {
		TRANSFER, REVEAL, SUBSTITUTE, WITHDRAW, NEGOTIATE;

		public static final Codec<Action> CODEC = StringRepresentable.fromEnum(Action::values);

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	public final Kind kind;
	public final ServerLevel level;
	public final UUID actor;
	public @Nullable LivingEntity actorEntity;
	public @Nullable UUID target;
	public @Nullable LivingEntity targetEntity;
	/** The colony it happened in (or the one asked for), -1 if none. */
	public int colony = -1;

	// kill
	public boolean victimParticipant;
	public boolean victimSorcerer;
	public boolean victimCurse;
	public boolean victimLeader;
	public int curseRank;
	public boolean curseSpecial;
	public Cause cause = Cause.ANY;
	public double distance;
	public boolean night;

	// timer, idle, points
	public int day;
	public int idleDays;
	public int pointsBefore;
	public int pointsAfter;

	// action, added
	public @Nullable Action action;
	public int amount;
	public @Nullable Identifier rule;

	public RuleEvent(Kind kind, ServerLevel level, UUID actor) {
		this.kind = kind;
		this.level = level;
		this.actor = actor;
	}

	public boolean victimMatches(Victim filter) {
		return switch (filter) {
			case ANY -> true;
			case PARTICIPANT -> victimParticipant;
			case SORCERER -> victimParticipant && victimSorcerer;
			case NON_SORCERER -> victimParticipant && !victimSorcerer;
			case CURSE -> victimCurse;
			case LEADER -> victimParticipant && victimLeader;
		};
	}
}
