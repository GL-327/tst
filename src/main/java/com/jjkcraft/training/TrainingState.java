package com.jjkcraft.training;
import java.util.*;
import com.mojang.serialization.Codec;
/** Work is stored separately from purchased legacy growth; no old character loses their training. */
public record TrainingState(Map<String,Integer> work){
    public static final List<String> SKILLS=List.of("strength","endurance","agility","weaponry","reserves","control","output","efficiency","domain");
    public static final TrainingState EMPTY=new TrainingState(Map.of());
    public static final Codec<TrainingState> CODEC=Codec.unboundedMap(Codec.STRING,Codec.intRange(0,100000)).xmap(TrainingState::new,TrainingState::work);
    public TrainingState{var clean=new HashMap<String,Integer>();work.forEach((k,v)->{if(SKILLS.contains(k))clean.put(k,Math.clamp(v,0,100000));});work=Map.copyOf(clean);}
    public int xp(String key){return work.getOrDefault(key,0);}
    public static int threshold(int level){return 40*level+12*level*level;}
    public int level(String key){int n=0;while(n<20&&xp(key)>=threshold(n+1))n++;return n;}
    public TrainingState add(String key,int value){var next=new HashMap<>(work);next.put(key,Math.min(100000,xp(key)+Math.max(0,value)));return new TrainingState(next);}
}
