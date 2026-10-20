package com.jjkcraft.awakening;

import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.config.JjkConfig;
import com.jjkcraft.core.RateLimiter;
import com.jjkcraft.core.progress.WeightedRoll;
import com.jjkcraft.grade.Grades;
import com.jjkcraft.network.JjkNetworking;
import com.jjkcraft.network.payload.AwakeningPayloads;
import com.jjkcraft.sorcerer.Awakening;
import com.jjkcraft.sorcerer.Progression;
import com.jjkcraft.sorcerer.SorcererLifecycle;
import com.jjkcraft.technique.TechniqueService;
import com.jjkcraft.technique.Techniques;

/** Server owns selection, rolls, unlocks and persistence. Repeated/spoofed choices cannot reroll. */
public final class CharacterAwakening {
    public static final ResourceKey<Registry<InnateFamily>> FAMILIES = ResourceKey.createRegistryKey(JjkCraft.id("innate_family"));
    public static final AttachmentType<AwakeningRecord> RECORD = AttachmentRegistry.create(JjkCraft.id("awakening_record"),
        b -> b.persistent(AwakeningRecord.CODEC).copyOnDeath().initializer(() -> AwakeningRecord.EMPTY));
    private static final RateLimiter LIMIT = new RateLimiter(1, 2);
    private CharacterAwakening() { }

    public static AwakeningRecord get(net.minecraft.world.entity.player.Player player) { return player.getAttachedOrCreate(RECORD); }
    public static void register() { DynamicRegistries.registerSynced(FAMILIES, InnateFamily.CODEC); }

    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(com.jjkcraft.network.payload.PersonalizationPayloads.CreativeEdit.TYPE,(payload,ctx)->{
            var p=ctx.player();if(!p.isCreative())return;
            int result=!LIMIT.tryAcquire(p.getUUID(),p.level().getGameTime())?2:payload.apply()&&!CreativeCharacters.apply(p,payload.family(),payload.settings())?1:0;
            JjkNetworking.send(p,new com.jjkcraft.network.payload.PersonalizationPayloads.CreativeReply(CreativeCharacters.familyId(p),CreativeCharacters.snapshot(p),payload.request(),result));
        });
        ServerPlayNetworking.registerGlobalReceiver(com.jjkcraft.network.payload.PersonalizationPayloads.CreativeFamily.TYPE,(payload,ctx)->{
            var p=ctx.player();if(p.isAlive()&&!p.isSpectator()&&LIMIT.tryAcquire(p.getUUID(),p.level().getGameTime()))creative(p,payload.family(),payload.mechamaru());
        });
        ServerPlayNetworking.registerGlobalReceiver(AwakeningPayloads.Choose.TYPE, (payload, context) -> {
            ServerPlayer p = context.player();
            if (p.isAlive() && !p.isSpectator() && LIMIT.tryAcquire(p.getUUID(), p.level().getGameTime()))
                choose(p, payload.path(), payload.family());
        });
        SorcererLifecycle.addFullSync(CharacterAwakening::send);
        Progression.addGradeHook((p, from, to) -> unlock(p));
        Awakening.addResetHook(p -> { p.setAttached(RECORD, AwakeningRecord.EMPTY); send(p); });
        Awakening.addHook((p, heavenly) -> { if (!heavenly && (get(p).path().isEmpty() || get(p).path().equals("pending"))) migrate(p); unlock(p); send(p); });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer p = handler.getPlayer();
            if (Progression.get(p).awakened() && get(p).path().isEmpty()) migrate(p);
            unlock(p);
            send(p);
        });
        TechniqueService.addModifierProvider((caster, id, def) -> caster instanceof ServerPlayer p && get(p).sixEyes()
            ? new TechniqueService.Modifiers(1, 0.75, 1) : TechniqueService.Modifiers.NONE);
    }

    public static void firstJoin(ServerPlayer p) {
        com.jjkcraft.energy.SorceryPractice.ensureBirth(p);
        if (Progression.get(p).awakened()) { send(p); return; }
        if (get(p).path().equals("pending")) { send(p); return; }
        p.setAttached(RECORD, new AwakeningRecord("pending", "", "", false, -1));
        if (p.getRandom().nextDouble() * 100 < JjkConfig.server().progression.heavenlyRestrictionChancePercent) {
            Awakening.awaken(p, true);
            if(CharacterTraits.has(p,"mechamaru_restriction")){rebirth(p,"");CharacterTraits.ensure(p);com.jjkcraft.energy.CursedEnergy.set(p,0);}
            else p.setAttached(RECORD, new AwakeningRecord("heavenly", "", "Zen'in", false, p.level().getGameTime()));
        }
        send(p);
    }

    public static boolean pending(Player p) { return !Progression.get(p).awakened() && get(p).path().equals("pending"); }
    public static boolean creative(ServerPlayer p,Identifier familyId,boolean mechamaru) {
        if(!p.isCreative())return false;
        var family=p.registryAccess().lookupOrThrow(FAMILIES).getValue(familyId);
        if(!valid(p,family)||Grades.get(p.registryAccess(),JjkCraft.id("calamity")).isEmpty())return false;
        TechniqueService.cancel(p);com.jjkcraft.death.SoulSystem.restoreLivingCharacter(p);Awakening.reset(p);
        if(!commit(p,"creative",familyId))return false;
        p.setAttached(RECORD,new AwakeningRecord("creative",familyId.toString(),family.clan(),family.clan().equals("Gojo"),p.level().getGameTime()));
        Progression.set(p,Progression.get(p).withGrade(JjkCraft.id("calamity"),10000).withGrowth(new com.jjkcraft.sorcerer.SorcererProgress.Growth(20,20,20,20)).withSpecialUnlocked());
        var traits=new java.util.ArrayList<Identifier>();if(family.clan().equals("Gojo"))traits.add(JjkCraft.id("six_eyes"));if(mechamaru)traits.add(JjkCraft.id("mechamaru_restriction"));
        CharacterTraits.set(p,new TraitState(true,java.util.List.copyOf(traits),"",java.util.List.of()));
        unlock(p);var mastery=new java.util.HashMap<Identifier,Integer>();
        for(var id:family.techniques())mastery.put(id,Techniques.get(p.registryAccess(),id).orElseThrow().masteryUses());
        TechniqueService.set(p,TechniqueService.get(p).withMastery(mastery).withAutoSlotted(com.jjkcraft.technique.TechniqueState.SLOTS));TechniqueService.resetCooldowns(p);
        com.jjkcraft.reverse.ReverseTechnique.set(p,com.jjkcraft.reverse.ReverseTechnique.state(p).withUnlocked(true).withAutoHeal(false),false);
        com.jjkcraft.energy.SorceryPractice.set(p,com.jjkcraft.energy.SorceryPractice.get(p).withReinforcement(true,false));
        com.jjkcraft.domain.DomainService.grantIfEligible(p);com.jjkcraft.energy.CursedEnergy.fill(p);p.setHealth(p.getMaxHealth());
        SorcererLifecycle.fullSync(p);return true;
    }

    public static boolean choose(ServerPlayer p, String path, String requested) {
        if (Progression.get(p).awakened()) { send(p); return false; }
        if (!path.equals("inherit") && !path.equals("random") && !(path.equals("choose") && JjkConfig.server().progression.allowTechniqueChoice)) return false;
        Registry<InnateFamily> registry = p.registryAccess().lookupOrThrow(FAMILIES);
        List<Identifier> ids = registry.keySet().stream().sorted().filter(id -> valid(p, registry.getValue(id))).toList();
        Identifier selected = null;
        if (path.equals("choose")) {
            Identifier id = Identifier.tryParse(requested);
            if (id != null && ids.contains(id)) selected = id;
        } else {
            List<Integer> weights = ids.stream().map(id -> path.equals("inherit") ? registry.getValue(id).inheritedWeight() : registry.getValue(id).randomWeight()).toList();
            int pick = WeightedRoll.index(weights, p.getRandom().nextDouble());
            if (pick >= 0) selected = ids.get(pick);
            if (selected == null && path.equals("inherit")) {
                JjkCraft.LOGGER.warn("Empty inherited awakening pool for {}; falling back to Random", p.getUUID());
                return choose(p, "random", "");
            }
        }
        if (selected == null) {
            p.sendSystemMessage(Component.literal("No valid awakening family. Check the world's innate_family data pack."));
            return false;
        }
        return commit(p,path,selected);
    }

    /** Losing a cursed-spirit life creates a different innate family, without another HR roll. */
    public static boolean rebirth(ServerPlayer p,String previousFamily) {
        var registry=p.registryAccess().lookupOrThrow(FAMILIES);
        var ids=registry.keySet().stream().sorted().filter(id->!id.toString().equals(previousFamily) && valid(p,registry.getValue(id))).toList();
        int pick=WeightedRoll.index(ids.stream().map(id->Math.max(1,registry.getValue(id).randomWeight()+registry.getValue(id).inheritedWeight())).toList(),p.getRandom().nextDouble());
        return pick>=0 && commit(p,"rebirth",ids.get(pick));
    }

    private static boolean commit(ServerPlayer p,String path,Identifier selected) {
        com.jjkcraft.energy.SorceryPractice.ensureBirth(p);
        InnateFamily family = p.registryAccess().lookupOrThrow(FAMILIES).getValue(selected);
        // Commit before hooks fire. The legacy hook is suppressed by the selected family record.
        p.setAttached(RECORD, new AwakeningRecord(path, selected.toString(), family.clan(),
            family.clan().equals("Gojo") && p.getRandom().nextDouble() * 100 < JjkConfig.server().progression.sixEyesChancePercent,
            p.level().getGameTime()));
        Awakening.awaken(p, false);
        TechniqueService.setInnate(p, family.techniques().getFirst());
        unlock(p);
        send(p);
        p.sendSystemMessage(Component.literal("Awakened: ").append(Component.translatable(family.name())));
        return true;
    }

    private static boolean valid(ServerPlayer p, InnateFamily family) {
        return family != null && !family.techniques().isEmpty() && family.techniques().stream().allMatch(id -> Techniques.get(p.registryAccess(), id).isPresent());
    }

    private static void migrate(ServerPlayer p) {
        var state = TechniqueService.get(p);
        var registry = p.registryAccess().lookupOrThrow(FAMILIES);
        String family = registry.keySet().stream().sorted().filter(id -> state.innate().map(registry.getValue(id).techniques()::contains).orElse(false))
            .map(Object::toString).findFirst().orElse("");
        p.setAttached(RECORD, new AwakeningRecord("legacy", family, "", false, Progression.get(p).awakenedAtTick()));
    }

    public static Optional<InnateFamily> family(ServerPlayer p) {
        Identifier id = Identifier.tryParse(get(p).family());
        return id == null ? Optional.empty() : Optional.ofNullable(p.registryAccess().lookupOrThrow(FAMILIES).getValue(id));
    }

    public static void unlock(ServerPlayer p) {
        if(get(p).path().equals("creative_custom"))return;
        if (!Progression.get(p).awakened() || Progression.get(p).heavenlyRestricted()) return;
        family(p).ifPresent(f -> {
            if (!get(p).path().equals("legacy")) {
                // Remove the preview's auto-granted punch/teleport and repair its displaced starter.
                for (String filler : List.of("ink_iron_palm", "ink_step")) {
                    Identifier id = JjkCraft.id(filler);
                    if (!f.techniques().contains(id)) TechniqueService.revoke(p, id);
                }
                if (valid(p, f)) TechniqueService.setInnate(p, f.techniques().getFirst());
            }
            int rank = Grades.rank(p.registryAccess(), Progression.get(p).grade());
            for (Identifier id : f.techniques()) {
                if (Techniques.get(p.registryAccess(), id).map(d -> Grades.rank(p.registryAccess(), d.grade()) <= rank).orElse(false)
                    && !TechniqueService.knows(p, id)) TechniqueService.grant(p, id);
            }
            TechniqueService.set(p, TechniqueService.get(p).withAutoSlotted(TechniqueService.usableSlots(p)));
        });
    }

    public static void send(ServerPlayer p) {
        JjkNetworking.send(p, new AwakeningPayloads.View(get(p), !Progression.get(p).awakened() && JjkConfig.server().progression.awakenOnFirstJoin,
            JjkConfig.server().progression.allowTechniqueChoice));
    }
}
