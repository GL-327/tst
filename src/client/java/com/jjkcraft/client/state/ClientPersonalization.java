package com.jjkcraft.client.state;
import java.util.Map;
import net.minecraft.resources.Identifier;
import com.jjkcraft.technique.TechniqueTuning;
import com.jjkcraft.energy.PracticeState;
import com.jjkcraft.network.payload.PersonalizationPayloads;
public final class ClientPersonalization {
    private static final java.util.Map<Identifier,PersonalizationPayloads.TuneReply> REPLIES=new java.util.HashMap<>();
    private static int sequence;
    private static PersonalizationPayloads.CreativeReply creative;
    public static void accept(PersonalizationPayloads.CreativeReply reply){creative=reply;}
    public static PersonalizationPayloads.CreativeReply creative(){return creative;}
    public static int nextRequest(){return ++sequence;}
    public static void accept(PersonalizationPayloads.TuneReply reply){REPLIES.put(reply.id(),reply);}
    public static PersonalizationPayloads.TuneReply reply(Identifier id){return REPLIES.get(id);}
    private static Map<Identifier,TechniqueTuning> tunings=Map.of();
    private static PracticeState practice=PracticeState.EMPTY;
    private ClientPersonalization() { }
    public static void accept(PersonalizationPayloads.View p){tunings=Map.copyOf(p.tunings());practice=p.practice();}
    public static boolean isCreative(){var p=net.minecraft.client.Minecraft.getInstance().player;return p!=null&&p.isCreative();}
    private static double mastery(Identifier id){return ClientProgress.registries().flatMap(r->com.jjkcraft.technique.Techniques.get(r,id)).map(d->com.jjkcraft.core.technique.TechniqueMath.mastery(ClientTechniques.state().masteryOf(id),d.masteryUses())).orElse(0.0);}
    public static int outputLimit(Identifier id){return isCreative()?com.jjkcraft.core.technique.OutputScaling.MAX_OUTPUT:com.jjkcraft.core.technique.OutputScaling.outputLimit(ClientProgress.rank(),ClientProgress.get().growth().output(),mastery(id));}
    public static int universalLimit(){return isCreative()?com.jjkcraft.core.technique.OutputScaling.MAX_OUTPUT:com.jjkcraft.core.technique.OutputScaling.outputLimit(ClientProgress.rank(),ClientProgress.get().growth().output(),com.jjkcraft.client.training.TrainingClient.state.level("control")/5.0);}
    public static TechniqueTuning get(Identifier id){var saved=tunings.get(id);return saved!=null?saved.limited(ClientProgress.rank(),ClientProgress.get().growth().output(),mastery(id),isCreative()):ClientProgress.registries().flatMap(r->com.jjkcraft.technique.Techniques.get(r,id)).map(TechniqueTuning::defaultFor).orElse(TechniqueTuning.DEFAULT);}
    public static PracticeState practice(){int cap=universalLimit();return practice.output()<=cap&&practice.healing()<=cap?practice:practice.tune(Math.min(cap,practice.output()),Math.min(cap,practice.healing()),practice.healBelow());}
    public static void reset(){tunings=Map.of();practice=PracticeState.EMPTY;REPLIES.clear();}
}
