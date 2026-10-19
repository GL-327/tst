package com.jjkcraft.client.modulo;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import com.jjkcraft.client.ui.InkToast;
import com.jjkcraft.modulo.ModuloPayloads;

/** The client's copy of its Modulo view, the dialogue waiting to be shown, and short-lived effects. */
public final class ClientModulo {
	public static final int ACCENT = 0xFF7CF2A8;

	/** A line waiting in the dialogue box. */
	public record Line(String speaker, String text) {
	}

	private static ModuloPayloads.View view = ModuloPayloads.View.EMPTY;
	private static final Deque<Line> QUEUE = new ArrayDeque<>();
	private static @Nullable Line showing;
	private static long showingSince;
	private static ModuloPayloads.@Nullable Dialogue choice;
	private static @Nullable String sequence;
	private static long sequenceStart;
	/** Mahoraga's wheel: entity id to the time of its last turn, for the turning animation. */
	private static final Map<Integer, Long> WHEEL_TURNED = new HashMap<>();

	private ClientModulo() {
	}

	public static ModuloPayloads.View view() {
		return view;
	}

	public static void reset() {
		view = ModuloPayloads.View.EMPTY;
		QUEUE.clear();
		showing = null;
		choice = null;
		sequence = null;
		WHEEL_TURNED.clear();
	}

	public static void accept(ModuloPayloads.Sync payload) {
		view = payload.view();
	}

	public static void accept(ModuloPayloads.Dialogue payload) {
		if (!payload.options().isEmpty()) {
			choice = payload;
			return;
		}

		QUEUE.add(new Line(payload.speaker(), payload.text()));
	}

	public static void accept(ModuloPayloads.Event payload) {
		List<String> args = payload.args();
		switch (payload.kind()) {
			case "stage_done" -> InkToast.show(Component.translatable("hud.jjkcraft.modulo.stage_done", ""),
					Component.translatable(args.isEmpty() ? "" : args.getFirst(), "", ""), ACCENT);
			case "mission_done" -> InkToast.show(Component.translatable("message.jjkcraft.modulo.mission_done", ""),
					Component.translatable(args.isEmpty() ? "" : args.getFirst()), ACCENT);
			case "descent" -> InkToast.show(Component.translatable("item.jjkcraft.naunax_beacon"),
					Component.translatable("hud.jjkcraft.modulo.descent", args.size() > 0 ? args.get(0) : "?", args.size() > 1 ? args.get(1) : "?"), ACCENT);
			case "sequence" -> {
				sequence = args.isEmpty() ? "epilogue" : args.getFirst();
				sequenceStart = Util.getMillis();
			}
			case "adapted" -> {
				if (!args.isEmpty()) {
					try {
						WHEEL_TURNED.put(Integer.parseInt(args.getFirst()), Util.getMillis());
					} catch (NumberFormatException ignored) {
						// A malformed id draws no turn.
					}
				}
			}
			default -> {
			}
		}
	}

	/** Runs every client tick: advances the dialogue and opens a pending choice. */
	public static void tick(Minecraft mc) {
		long now = Util.getMillis();
		if (showing != null && now - showingSince > duration(showing)) {
			showing = null;
		}

		if (showing == null && !QUEUE.isEmpty()) {
			showing = QUEUE.poll();
			showingSince = now;
		}

		if (choice != null && mc.player != null && mc.gui.screen() == null) {
			ModuloPayloads.Dialogue pending = choice;
			choice = null;
			mc.gui.setScreen(new ModuloChoiceScreen(pending));
		}

		if (sequence != null && now - sequenceStart > EpilogueOverlay.LENGTH_MS) {
			sequence = null;
		}

		// The epilogue: Tokyo's cursed spirits rise as motes of light around the player.
		if ("epilogue".equals(sequence) && mc.level != null && mc.player != null && !com.jjkcraft.client.config.ClientConfig.reducedMotion()) {
			for (int i = 0; i < 3; i++) {
				double x = mc.player.getX() + (mc.level.getRandom().nextDouble() - 0.5) * 24;
				double z = mc.player.getZ() + (mc.level.getRandom().nextDouble() - 0.5) * 24;
				mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.END_ROD, x, mc.player.getY() + mc.level.getRandom().nextDouble() * 4, z, 0, 0.06, 0);
			}
		}
	}

	private static long duration(Line line) {
		String text = Component.translatable(line.text()).getString();
		long base = 3000 + text.length() * 45L;
		// With more lines waiting, keep things moving.
		return QUEUE.size() > 1 ? Math.min(base, 2500) : base;
	}

	public static @Nullable Line showing() {
		return showing;
	}

	public static long showingFor() {
		return Util.getMillis() - showingSince;
	}

	public static @Nullable String sequence() {
		return sequence;
	}

	public static long sequenceElapsed() {
		return Util.getMillis() - sequenceStart;
	}

	/** Milliseconds since Mahoraga {@code entityId}'s wheel last turned, or a large number. */
	public static long sinceWheelTurned(int entityId) {
		Long at = WHEEL_TURNED.get(entityId);
		return at == null ? Long.MAX_VALUE / 2 : Util.getMillis() - at;
	}
}
