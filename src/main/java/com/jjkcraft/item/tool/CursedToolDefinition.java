package com.jjkcraft.item.tool;

import java.util.Locale;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;

import com.jjkcraft.registry.LegacyIds;

/**
 * A cursed tool's data (datapack registry {@code jjkcraft/cursed_tool}, synced for tooltips). The
 * item itself is code; this decides its grade, what it grants and what it does. A datapack can
 * retune any tool, or point one at another technique.
 *
 * @param item         the item this describes
 * @param kind         weapon, cursed_object (swallowed, like a cursed finger), flight, ledger
 * @param grade        lowest grade that can use it (weapons still hit below it, without the technique)
 * @param technique    weapon: granted while held; cursed_object: granted on consuming (absent: a random one)
 * @param costMultiplier weapon: multiplier on the granted technique's CE cost while held
 * @param onHit        weapon: what a melee hit does besides damage
 * @param amount       the on-hit amount (CE drained or siphoned), or the cursed object's risk in ticks,
 *                     the flight item's CE per second, or the ledger's points per deposit
 * @param description  translation key of the tooltip line
 * @param medium       weapon: the lent technique is paid with the tool's durability instead of the wielder's CE,
 *                     so Heavenly Restriction can cast it (Tsurugi Okkotsu's tools; docs/MODULO.md 3.6)
 * @param durabilityPerCe medium: durability lost per point of the technique's CE cost (at least 1 a cast)
 */
public record CursedToolDefinition(Identifier item, Kind kind, Identifier grade, Optional<Identifier> technique, double costMultiplier, OnHit onHit,
		double amount, String description, boolean medium, double durabilityPerCe) {
	public enum Kind implements StringRepresentable {
		WEAPON, CURSED_OBJECT, FLIGHT, LEDGER;

		/** Also reads {@code "vessel"}, this kind's name before 1.1.0 (docs/NAMING.md). */
		public static final Codec<Kind> CODEC = Codec.withAlternative(StringRepresentable.fromEnum(Kind::values),
				Codec.STRING.comapFlatMap(s -> "vessel".equals(s) ? DataResult.success(CURSED_OBJECT) : DataResult.error(() -> "Unknown kind: " + s),
						Kind::getSerializedName));

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	public enum OnHit implements StringRepresentable {
		/** Nothing extra. */
		NONE,
		/** Ends the target's active technique and drains {@code amount} CE. */
		DISPEL,
		/** Takes {@code amount} CE from the target and gives it to the wielder. */
		SIPHON,
		/** Sets the target alight for {@code amount} seconds. */
		IGNITE;

		public static final Codec<OnHit> CODEC = StringRepresentable.fromEnum(OnHit::values);

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	public static final Codec<CursedToolDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
			LegacyIds.ITEM_ID.fieldOf("item").forGetter(CursedToolDefinition::item),
			Kind.CODEC.fieldOf("kind").forGetter(CursedToolDefinition::kind),
			Identifier.CODEC.fieldOf("grade").forGetter(CursedToolDefinition::grade),
			LegacyIds.TECHNIQUE_ID.optionalFieldOf("technique").forGetter(CursedToolDefinition::technique),
			Codec.doubleRange(0.1, 10).optionalFieldOf("cost_multiplier", 1.0).forGetter(CursedToolDefinition::costMultiplier),
			OnHit.CODEC.optionalFieldOf("on_hit", OnHit.NONE).forGetter(CursedToolDefinition::onHit),
			Codec.doubleRange(0, 100_000).optionalFieldOf("amount", 0.0).forGetter(CursedToolDefinition::amount),
			Codec.STRING.fieldOf("description").forGetter(CursedToolDefinition::description),
			Codec.BOOL.optionalFieldOf("medium", false).forGetter(CursedToolDefinition::medium),
			Codec.doubleRange(0, 100).optionalFieldOf("durability_per_ce", 0.1).forGetter(CursedToolDefinition::durabilityPerCe)
	).apply(i, CursedToolDefinition::new));
}
