package com.jjkcraft.client.state;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import com.jjkcraft.client.ui.InkToast;
import com.jjkcraft.culling.rule.CullingRule;
import com.jjkcraft.network.payload.CullingPayloads;
import com.jjkcraft.registry.JjkRegistries;

/** The client's copy of its view of the Culling Game, and the short events the server sends. */
public final class ClientCulling {
	public static final int ACCENT = 0xFFD4A35A;
	private static CullingPayloads.View view = CullingPayloads.View.EMPTY;
	/** Set when the Kogane is spoken to; the client opens the negotiation page on its next tick. */
	private static boolean openNegotiation;

	private ClientCulling() {
	}

	public static CullingPayloads.View view() {
		return view;
	}

	public static CullingPayloads.Me me() {
		return view.me();
	}

	public static boolean running() {
		return view.running();
	}

	public static void reset() {
		view = CullingPayloads.View.EMPTY;
		openNegotiation = false;
	}

	public static void accept(CullingPayloads.Sync payload) {
		view = payload.view();
	}

	public static void accept(CullingPayloads.Event event) {
		List<String> args = event.args();
		String first = args.isEmpty() ? "" : args.getFirst();
		switch (event.kind()) {
			case "joined" -> InkToast.show(Component.translatable("toast.jjkcraft.culling.joined"),
					Component.translatableWithFallback(first, first), ACCENT);
			case "points" -> {
				int delta = parse(first);
				if (delta != 0) {
					InkToast.show(Component.translatable("toast.jjkcraft.culling.points"),
							Component.translatable(delta > 0 ? "toast.jjkcraft.culling.points.gain" : "toast.jjkcraft.culling.points.loss", Math.abs(delta)),
							delta > 0 ? ACCENT : 0xFF9A93A8);
				}
			}
			case "rule_added" -> {
				Identifier id = Identifier.tryParse(first);
				Component text = id == null ? Component.literal(first) : ruleText(id);
				InkToast.show(Component.translatable("toast.jjkcraft.culling.rule", args.size() > 1 ? args.get(1) : "?"), text, ACCENT);
			}
			case "deadline_warning" -> InkToast.show(Component.translatable("toast.jjkcraft.culling.deadline"),
					Component.translatable("toast.jjkcraft.culling.deadline.body", first), 0xFFE0303F);
			case "started" -> InkToast.show(Component.translatable("toast.jjkcraft.culling.started"),
					Component.translatable("toast.jjkcraft.culling.started.body", first), ACCENT);
			case "ended" -> InkToast.show(Component.translatable("toast.jjkcraft.culling.ended"),
					Component.translatable("toast.jjkcraft.culling.ended.body", args.size() > 1 ? args.get(1) : "-"), ACCENT);
			case "open_negotiation" -> openNegotiation = true;
			default -> {
			}
		}
	}

	/** True once after the Kogane was spoken to. */
	public static boolean takeOpenNegotiation() {
		boolean open = openNegotiation;
		openNegotiation = false;
		return open;
	}

	private static int parse(String value) {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	// ---------------------------------------------------------------- rules

	public record RuleEntry(Identifier id, CullingRule rule) {
	}

	public static Optional<CullingRule> rule(Identifier id) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			return Optional.empty();
		}

		return mc.level.registryAccess().lookup(JjkRegistries.CULLING_RULE).flatMap(r -> r.getOptional(id));
	}

	/** Every rule the server knows, in list order. */
	public static List<RuleEntry> allRules() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			return List.of();
		}

		List<RuleEntry> out = new ArrayList<>();
		mc.level.registryAccess().lookup(JjkRegistries.CULLING_RULE).ifPresent(r -> r.listElements()
				.forEach(h -> out.add(new RuleEntry(h.key().identifier(), h.value()))));
		out.sort((a, b) -> a.rule().order() != b.rule().order() ? Integer.compare(a.rule().order(), b.rule().order())
				: a.id().toString().compareTo(b.id().toString()));
		return out;
	}

	/** A rule's wording with its arguments filled in from the live server values. */
	public static Component ruleText(Identifier id) {
		return rule(id).map(ClientCulling::ruleText).orElse(Component.literal(id.toString()));
	}

	public static Component ruleText(CullingRule rule) {
		Object[] args = new Object[rule.textArgs().size()];
		for (int i = 0; i < args.length; i++) {
			String arg = rule.textArgs().get(i);
			args[i] = switch (arg) {
				case "config:entry_days" -> String.valueOf(view.configValue(0, 19));
				case "config:sorcerer_points" -> String.valueOf(view.configValue(1, 5));
				case "config:non_sorcerer_points" -> String.valueOf(view.configValue(2, 1));
				case "config:rule_cost" -> String.valueOf(view.configValue(3, 100));
				default -> arg;
			};
		}

		return Component.translatable(rule.text(), args);
	}

	/** Points to add a rule on this server. */
	public static int cost(CullingRule rule) {
		return rule.cost().orElse(view.configValue(3, 100));
	}

	public static boolean ruleActive(Identifier id) {
		for (CullingPayloads.RuleView rule : view.rules()) {
			if (rule.id().equals(id)) {
				return true;
			}
		}

		return false;
	}

	/** Whether some rule in force lets a participant ask for {@code action}. */
	public static boolean allowed(com.jjkcraft.culling.rule.RuleEvent.Action action) {
		for (CullingPayloads.RuleView active : view.rules()) {
			if (rule(active.id()).map(r -> r.allows(action)).orElse(false)) {
				return true;
			}
		}

		return false;
	}

	public static Optional<CullingPayloads.ColonyView> colony(int id) {
		for (CullingPayloads.ColonyView colony : view.colonies()) {
			if (colony.id() == id) {
				return Optional.of(colony);
			}
		}

		return Optional.empty();
	}

	public static Component colonyName(int id) {
		return colony(id).map(c -> (Component) Component.translatableWithFallback(c.name(), c.name())).orElse(Component.literal("?"));
	}

	/** Days left in the viewer's entry window, or -1 if none is running. */
	public static int daysLeft() {
		Minecraft mc = Minecraft.getInstance();
		long deadline = view.me().deadline();
		if (deadline < 0 || mc.level == null) {
			return -1;
		}

		return com.jjkcraft.core.culling.CullingMath.daysLeft(mc.level.getGameTime(), deadline);
	}
}
