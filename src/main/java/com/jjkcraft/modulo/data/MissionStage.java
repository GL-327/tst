package com.jjkcraft.modulo.data;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;

/**
 * One stage of a Modulo mission ({@code data/<ns>/jjkcraft/modulo_mission/*.json}, field
 * {@code stages}). The {@code type} field picks the kind; every kind shares the {@link Base} fields.
 * Each finished stage is a checkpoint (docs/MODULO.md 3.7, docs/SCHEMAS.md).
 */
public sealed interface MissionStage {
	Base base();

	String type();

	/**
	 * Fields every stage has.
	 *
	 * @param objective translation key of the objective shown on the HUD (may take the stage's progress as %s)
	 * @param site      where the stage happens; its NPCs and spawns appear while someone on the stage is there
	 * @param intro     lines spoken when the stage begins
	 * @param spawns    entities kept at the site while someone on this stage is there
	 * @param when      only for players whose mission choice matches; others skip the stage
	 */
	record Base(String objective, Optional<Identifier> site, List<Line> intro, List<Spawn> spawns, Optional<When> when) {
		public static final MapCodec<Base> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Codec.STRING.fieldOf("objective").forGetter(Base::objective),
				Identifier.CODEC.optionalFieldOf("site").forGetter(Base::site),
				Line.CODEC.listOf().optionalFieldOf("intro", List.of()).forGetter(Base::intro),
				Spawn.CODEC.listOf().optionalFieldOf("spawns", List.of()).forGetter(Base::spawns),
				When.CODEC.optionalFieldOf("when").forGetter(Base::when)
		).apply(i, Base::new));
	}

	/** A line of dialogue: who says it (a character id, or {@code narrator}) and its translation key. */
	record Line(String speaker, String text) {
		public static final Codec<Line> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.fieldOf("speaker").forGetter(Line::speaker),
				Codec.STRING.fieldOf("text").forGetter(Line::text)
		).apply(i, Line::new));
	}

	/** Keeps up to {@code count} of an entity type within {@code radius} of the site's centre. */
	record Spawn(Identifier entity, int count, double radius) {
		public static final Codec<Spawn> CODEC = RecordCodecBuilder.create(i -> i.group(
				Identifier.CODEC.fieldOf("entity").forGetter(Spawn::entity),
				Codec.intRange(1, 32).fieldOf("count").forGetter(Spawn::count),
				Codec.doubleRange(1, 64).optionalFieldOf("radius", 10.0).forGetter(Spawn::radius)
		).apply(i, Spawn::new));
	}

	/** Runs only for players whose choice {@code flag} is {@code value}. */
	record When(String flag, String value) {
		public static final Codec<When> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.fieldOf("flag").forGetter(When::flag),
				Codec.STRING.fieldOf("value").forGetter(When::value)
		).apply(i, When::new));

		public boolean matches(Map<String, String> flags) {
			return value.equals(flags.get(flag));
		}
	}

	/** Talk to a character and hear every line. */
	record Talk(Base base, String npc, List<Line> lines) implements MissionStage {
		static final MapCodec<Talk> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Base.MAP_CODEC.forGetter(Talk::base),
				Codec.STRING.fieldOf("npc").forGetter(Talk::npc),
				Line.CODEC.listOf(1, 32).fieldOf("lines").forGetter(Talk::lines)
		).apply(i, Talk::new));

		@Override
		public String type() {
			return "talk";
		}
	}

	/** Go to the site (or one of its markers). */
	record Travel(Base base, Optional<String> marker, double radius) implements MissionStage {
		static final MapCodec<Travel> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Base.MAP_CODEC.forGetter(Travel::base),
				Codec.STRING.optionalFieldOf("marker").forGetter(Travel::marker),
				Codec.doubleRange(1, 64).optionalFieldOf("radius", 6.0).forGetter(Travel::radius)
		).apply(i, Travel::new));

		@Override
		public String type() {
			return "travel";
		}
	}

	/** Defeat a boss at the site, with allies fighting alongside. */
	record Defeat(Base base, Identifier boss, List<String> allies) implements MissionStage {
		static final MapCodec<Defeat> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Base.MAP_CODEC.forGetter(Defeat::base),
				Identifier.CODEC.fieldOf("boss").forGetter(Defeat::boss),
				Codec.STRING.listOf().optionalFieldOf("allies", List.of()).forGetter(Defeat::allies)
		).apply(i, Defeat::new));

		@Override
		public String type() {
			return "defeat";
		}
	}

	/** Bring items to a character. */
	record Deliver(Base base, Identifier item, int count, String npc, List<Line> lines) implements MissionStage {
		static final MapCodec<Deliver> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Base.MAP_CODEC.forGetter(Deliver::base),
				Identifier.CODEC.fieldOf("item").forGetter(Deliver::item),
				Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(Deliver::count),
				Codec.STRING.fieldOf("npc").forGetter(Deliver::npc),
				Line.CODEC.listOf().optionalFieldOf("lines", List.of()).forGetter(Deliver::lines)
		).apply(i, Deliver::new));

		@Override
		public String type() {
			return "deliver";
		}
	}

	/** Make Mul energy by pouring cursed energy into Mul. */
	record MakeMulEnergy(Base base, double amount) implements MissionStage {
		static final MapCodec<MakeMulEnergy> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Base.MAP_CODEC.forGetter(MakeMulEnergy::base),
				Codec.doubleRange(1, 100_000).fieldOf("amount").forGetter(MakeMulEnergy::amount)
		).apply(i, MakeMulEnergy::new));

		@Override
		public String type() {
			return "make_mul_energy";
		}
	}

	/**
	 * Hold the site: stay inside for {@code seconds} while the spawns come, or until the scene boss
	 * reports it is done (Mahoraga fully adapted).
	 */
	record Defend(Base base, int seconds, Optional<Identifier> boss, Optional<String> protect) implements MissionStage {
		static final MapCodec<Defend> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Base.MAP_CODEC.forGetter(Defend::base),
				Codec.intRange(5, 3600).fieldOf("seconds").forGetter(Defend::seconds),
				Identifier.CODEC.optionalFieldOf("boss").forGetter(Defend::boss),
				Codec.STRING.optionalFieldOf("protect").forGetter(Defend::protect)
		).apply(i, Defend::new));

		@Override
		public String type() {
			return "defend";
		}
	}

	/** Choose one option by talking to a character; the choice is saved as a flag. */
	record Choice(Base base, String npc, String flag, List<String> options) implements MissionStage {
		static final MapCodec<Choice> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Base.MAP_CODEC.forGetter(Choice::base),
				Codec.STRING.fieldOf("npc").forGetter(Choice::npc),
				Codec.STRING.fieldOf("flag").forGetter(Choice::flag),
				Codec.STRING.listOf(2, 4).fieldOf("options").forGetter(Choice::options)
		).apply(i, Choice::new));

		@Override
		public String type() {
			return "choice";
		}
	}

	/** Play a client sequence (the epilogue) for {@code seconds}. */
	record Sequence(Base base, String sequence, int seconds) implements MissionStage {
		static final MapCodec<Sequence> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Base.MAP_CODEC.forGetter(Sequence::base),
				Codec.STRING.fieldOf("sequence").forGetter(Sequence::sequence),
				Codec.intRange(1, 600).fieldOf("seconds").forGetter(Sequence::seconds)
		).apply(i, Sequence::new));

		@Override
		public String type() {
			return "sequence";
		}
	}

	Map<String, MapCodec<? extends MissionStage>> TYPES = Map.of(
			"talk", Talk.CODEC,
			"travel", Travel.CODEC,
			"defeat", Defeat.CODEC,
			"deliver", Deliver.CODEC,
			"make_mul_energy", MakeMulEnergy.CODEC,
			"defend", Defend.CODEC,
			"choice", Choice.CODEC,
			"sequence", Sequence.CODEC);

	Codec<MissionStage> CODEC = Codec.STRING.comapFlatMap(
					type -> TYPES.containsKey(type) ? DataResult.success(type) : DataResult.error(() -> "Unknown stage type: " + type + ", expected one of " + TYPES.keySet()),
					Function.identity())
			.dispatch("type", MissionStage::type, TYPES::get);
}
