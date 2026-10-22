package com.jjkcraft.awakening;

import java.util.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.domain.*;
import com.jjkcraft.energy.*;
import com.jjkcraft.grade.Grades;
import com.jjkcraft.reverse.*;
import com.jjkcraft.sorcerer.*;
import com.jjkcraft.technique.*;

/** Creative-only edits are validated in full before any character state is changed. */
public final class CreativeCharacters {
    private CreativeCharacters() { }
    public static Identifier familyId(ServerPlayer p){
        var registry=p.registryAccess().lookupOrThrow(CharacterAwakening.FAMILIES);
        var id=Identifier.tryParse(CharacterAwakening.get(p).family());
        return id!=null&&registry.getValue(id)!=null?id:registry.keySet().stream().sorted().findFirst().orElse(JjkCraft.id("limitless"));
    }
    public static CreativeSettings snapshot(ServerPlayer p){
        var progress=Progression.get(p);var energy=SorceryPractice.get(p);var reverse=ReverseTechnique.state(p);var traits=CharacterTraits.get(p);var moves=TechniqueService.get(p);
        return new CreativeSettings(progress.grade(),progress.gradeXp(),progress.growth(),new CreativeSettings.Energy(energy.birth(),energy.trained(),Math.max(1,energy.regenLevel()),Math.min(1e9,CursedEnergy.current(p)),energy.reinforcement(),energy.active(),energy.output(),energy.healing(),energy.healBelow(),reverse.unlocked(),reverse.autoHeal()),
            traits.traits(),traits.form(),traits.hostHealth(),progress.heavenlyRestricted(),List.copyOf(moves.known()),moves.mastery(),moves.loadout().stream().map(s->s.map(Object::toString).orElse("")).toList(),DomainService.state(p).known().map(Object::toString).orElse(""),progress.specialUnlocked());
    }
    public static boolean apply(ServerPlayer p,Identifier familyId,CreativeSettings s){
        if(!p.isCreative()||!p.isAlive())return false;
        var family=p.registryAccess().lookupOrThrow(CharacterAwakening.FAMILIES).getValue(familyId);
        var growth=s.growth();var traitRegistry=p.registryAccess().lookupOrThrow(CharacterTraits.DEFINITIONS);
        var potential=s.energy();
        if(family==null||family.techniques().isEmpty()||Grades.get(p.registryAccess(),s.grade()).isEmpty()
            ||s.xp()<0||s.xp()>1000000||s.slots().size()>12||s.learned().size()>512||s.traits().size()>64
            ||!Double.isFinite(s.bodyHealth())||s.bodyHealth()<4||s.bodyHealth()>100
            ||potential.birth()<0||potential.birth()>3000||potential.trained()<0||potential.trained()>10000||potential.regen()<1||potential.regen()>10
            ||!Double.isFinite(potential.stored())||potential.stored()<0||potential.stored()>1e9
            ||potential.output()<25||potential.output()>3000||potential.healing()<25||potential.healing()>3000||potential.threshold()<25||potential.threshold()>100
            ||Arrays.stream(new int[]{growth.output(),growth.control(),growth.efficiency(),growth.domain()}).anyMatch(n->n<0||n>20)
            ||s.mastery().size()>512||s.traits().stream().anyMatch(id->traitRegistry.getValue(id)==null)
            ||s.learned().stream().anyMatch(id->Techniques.get(p.registryAccess(),id).isEmpty())
            ||s.mastery().entrySet().stream().anyMatch(e->!s.learned().contains(e.getKey())||e.getValue()<0||e.getValue()>1000000)
            ||!Set.of("","spirit","transplanted").contains(s.form())
            ||s.energy().birth()==0&&!s.traits().contains(JjkCraft.id("mechamaru_restriction"))&&!s.heavenly()
            ||s.heavenly()&&s.traits().contains(JjkCraft.id("mechamaru_restriction")))return false;
        var domain=s.domain().isEmpty()?Optional.<Identifier>empty():Optional.ofNullable(Identifier.tryParse(s.domain()));
        if(!s.domain().isEmpty()&&(domain.isEmpty()||Domains.get(p.registryAccess(),domain.get()).isEmpty()))return false;
        var slots=new ArrayList<Optional<Identifier>>();var assigned=new HashSet<Identifier>();
        for(String raw:s.slots()){
            if(raw.isEmpty()){slots.add(Optional.empty());continue;}
            var id=Identifier.tryParse(raw);if(id==null||!TechniqueBindings.allowed(p.registryAccess(),TechniqueState.normalized(Optional.empty(),s.learned(),List.of(),0,Map.of(),Map.of()),id)||!assigned.add(id))return false;slots.add(Optional.of(id));
        }
        TechniqueService.cancel(p);DomainService.close(p);
        com.jjkcraft.death.SoulSystem.restoreLivingCharacter(p);
        if(!Progression.get(p).awakened()||!familyId.toString().equals(CharacterAwakening.get(p).family())){
            Awakening.reset(p);
            p.setAttached(CharacterAwakening.RECORD,new AwakeningRecord("creative_custom",familyId.toString(),family.clan(),false,p.level().getGameTime()));
            Awakening.awaken(p,false);
        }
        boolean six=s.traits().contains(JjkCraft.id("six_eyes"));
        p.setAttached(CharacterAwakening.RECORD,new AwakeningRecord("creative_custom",familyId.toString(),family.clan(),six,p.level().getGameTime()));
        var old=Progression.get(p);
        Progression.set(p,new SorcererProgress(true,old.awakenedAtTick(),s.heavenly(),s.grade(),s.xp(),growth,old.trials(),old.counters(),s.special(),old.ceSpentTotal(),old.castsLanded(),old.castsAttempted()));
        var e=s.energy();SorceryPractice.set(p,new PracticeState(e.birth(),e.trained(),e.reinforcement(),e.active(),e.output(),e.healing(),e.threshold(),e.regen(),true));
        CharacterTraits.set(p,new TraitState(true,List.copyOf(new LinkedHashSet<>(s.traits())),s.form(),List.of(),s.bodyHealth()));
        var innate=family.techniques().stream().filter(s.learned()::contains).findFirst();
        TechniqueService.set(p,TechniqueState.normalized(innate,s.learned(),slots,0,s.mastery(),Map.of()));
        ReverseTechnique.set(p,new ReverseState(e.rct(),e.autoRct(),ReverseState.EMPTY.lastSaveTick()),false);
        DomainService.setState(p,new DomainState(domain,0,0));
        CursedEnergy.set(p,e.stored());p.setHealth(p.getMaxHealth());SorcererLifecycle.fullSync(p);return true;
    }
}
