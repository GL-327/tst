package com.jjkcraft.client.state;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import com.jjkcraft.core.technique.TechniqueMath;
import com.jjkcraft.grade.Grades;
import com.jjkcraft.network.payload.CastFeedbackPayload;
import com.jjkcraft.network.payload.TechniqueSyncPayload;
import com.jjkcraft.technique.CastFailure;
import com.jjkcraft.technique.TechniqueDefinition;
import com.jjkcraft.technique.TechniqueState;
import com.jjkcraft.technique.Techniques;

/** The local player's techniques as last sent by the server, plus HUD feedback state. */
public final class ClientTechniques {
	private static TechniqueState state = TechniqueState.EMPTY;
	private static Optional<Identifier> active = Optional.empty();
    private static boolean infinity;
    private static boolean infinityPreferred;
    private static double infinityUpkeep;
    private static int infinityReason;
    public static double infinityUpkeep(){return infinityUpkeep;}
    public static int infinityReason(){return infinityReason;}
    public static boolean infinityEnabled(){return infinityPreferred;}
    public static boolean infinityActive(){return infinity;}
    public static void accept(com.jjkcraft.network.payload.InfinityPayload payload){infinity=payload.enabled();infinityPreferred=payload.preferred();infinityUpkeep=payload.upkeep();infinityReason=payload.reason();}
	private static int feedbackSlot = -2;
	private static CastFailure feedback = CastFailure.NONE;
	private static long feedbackAt;
    private static com.jjkcraft.network.payload.ChargePayload charge=new com.jjkcraft.network.payload.ChargePayload(-1,0,100,0);
    private static long chargeAt;
    public static void accept(com.jjkcraft.network.payload.ChargePayload payload){charge=payload;chargeAt=System.currentTimeMillis();if(payload.percent()==0&&net.minecraft.client.Minecraft.getInstance().player!=null)com.jjkcraft.client.vfx.ChargeFx.stop(net.minecraft.client.Minecraft.getInstance().player.getId());}
    public static com.jjkcraft.network.payload.ChargePayload charge(){return System.currentTimeMillis()-chargeAt<1500?charge:new com.jjkcraft.network.payload.ChargePayload(-1,0,100,0);}

	private ClientTechniques() {
	}

	public static void accept(TechniqueSyncPayload payload) {
		state = payload.state();
		active = payload.active();
	}

	public static void accept(CastFeedbackPayload payload) {
		feedbackSlot = payload.slot();
		feedback = payload.failure();
		feedbackAt = System.currentTimeMillis();
	}

	public static void reset() {
        infinity=false;
        infinityPreferred=false;
        infinityUpkeep=0;infinityReason=0;
        charge=new com.jjkcraft.network.payload.ChargePayload(-1,0,100,0);
		state = TechniqueState.EMPTY;
		active = Optional.empty();
		feedback = CastFailure.NONE;
	}

	public static TechniqueState state() {
		return state;
	}

	public static Optional<Identifier> active() {
		return active;
	}

	/** The failure to show for a slot, if it happened in the last 1.5 s. */
	public static CastFailure recentFailure(int slot) {
		if (System.currentTimeMillis() - feedbackAt > 1500) {
			return CastFailure.NONE;
		}

		return feedbackSlot == slot || (slot == state.selected() && feedbackSlot == -1) ? feedback : CastFailure.NONE;
	}

	public static float feedbackAge() {
		return (System.currentTimeMillis() - feedbackAt) / 1500f;
	}

	public static Optional<TechniqueDefinition> definition(Identifier id) {
		if(com.jjkcraft.technique.TechniqueBindings.family(id))return ClientProgress.registries().flatMap(r->com.jjkcraft.technique.TechniqueBindings.definition(r,id).flatMap(f->f.techniques().stream().filter(state.known()::contains).map(move->Techniques.get(r,move)).flatMap(Optional::stream).findFirst().map(d->new TechniqueDefinition(f.name(),"screen.jjkcraft.family_selector.desc",d.category(),d.grade(),d.cast(),d.behavior(),d.visuals(),d.icon(),d.vowModifiers(),0,d.masteryUses()))));
		return ClientProgress.registries().flatMap(r -> Techniques.get(r, id)).map(ClientPersonalization.get(id)::apply);
	}

    public static boolean bindable(Identifier id){return ClientProgress.registries().map(r->com.jjkcraft.technique.TechniqueBindings.allowed(r,state,id)).orElse(false);}
    public static List<Identifier> families(){return ClientProgress.registries().map(r->com.jjkcraft.technique.TechniqueBindings.available(r,state)).orElse(List.of());}
    public static List<Identifier> variants(Identifier id){return ClientProgress.registries().map(r->com.jjkcraft.technique.TechniqueBindings.moves(r,state,id)).orElse(List.of());}

	/** Known techniques that exist in the current registry, innate first. */
	public static List<Identifier> known() {
		Set<Identifier> ids = new LinkedHashSet<>(state.known());
		List<Identifier> list = new ArrayList<>();
		for (Identifier id : ids) {
			if (definition(id).isPresent()) {
				list.add(id);
			}
		}

		return list;
	}

	public static int usableSlots() {
		return TechniqueState.SLOTS;
	}

	/** Remaining cooldown as a fraction of the full cooldown (1 = just cast, 0 = ready). */
	public static float cooldownFraction(Identifier id) {
		long now = ClientEnergy.gameTime();
		long ready = state.readyAt(id);
		if (ready <= now) {
			return 0;
		}

		int full = definition(id).map(d -> TechniqueMath.cooldown(d.cast().cooldown(), ClientProgress.get().growth().efficiency())).orElse(1);
		return Math.min(1, (float) (ready - now) / Math.max(1, full));
	}

	public static boolean mastered(Identifier id) {
		return definition(id).map(d -> state.masteryOf(id) >= d.masteryUses()).orElse(false);
	}

	/** The CE cost the player would pay, without vow modifiers (shown in tooltips). */
	public static double cost(Identifier id) {
        if(com.jjkcraft.technique.TechniqueBindings.family(id))return 0;
		return definition(id).map(d -> TechniqueMath.cost(d.cast().cost(), ClientProgress.get().growth().control(), mastered(id), ClientAwakening.view().record().sixEyes()?0.75:1.0)).orElse(0.0);
	}

	public static boolean affordable(Identifier id) {
		return ClientEnergy.predicted() + 1e-6 >= cost(id);
	}

	public static boolean gradeAllows(TechniqueDefinition definition) {
		return ClientProgress.registries().map(r -> Grades.rank(r, ClientProgress.get().grade()) >= Grades.rank(r, definition.grade())).orElse(false);
	}

	public static boolean activeIs(Identifier id) {
		if(id.getPath().equals("infinity"))return infinity;
		return active.map(id::equals).orElse(false);
	}

	public static Minecraft mc() {
		return Minecraft.getInstance();
	}
}
