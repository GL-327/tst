package com.jjkcraft.awakening;

import java.util.List;
import java.util.Map;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.sorcerer.SorcererProgress.Growth;

/** A complete, bounded character draft. Browsing the editor never changes the server character. */
public record CreativeSettings(Identifier grade,int xp,Growth growth,Energy energy,List<Identifier> traits,
        String form,double bodyHealth,boolean heavenly,List<Identifier> learned,Map<Identifier,Integer> mastery,
        List<String> slots,String domain,boolean special) {
    public static final Codec<CreativeSettings> CODEC=RecordCodecBuilder.create(i->i.group(
        Identifier.CODEC.fieldOf("grade").forGetter(CreativeSettings::grade),Codec.intRange(0,1000000).fieldOf("xp").forGetter(CreativeSettings::xp),
        Growth.CODEC.fieldOf("growth").forGetter(CreativeSettings::growth),Energy.CODEC.fieldOf("energy").forGetter(CreativeSettings::energy),
        Identifier.CODEC.listOf(0,64).fieldOf("traits").forGetter(CreativeSettings::traits),Codec.string(0,32).fieldOf("form").forGetter(CreativeSettings::form),
        Codec.doubleRange(4,100).fieldOf("body_health").forGetter(CreativeSettings::bodyHealth),Codec.BOOL.fieldOf("heavenly").forGetter(CreativeSettings::heavenly),
        Identifier.CODEC.listOf(0,512).fieldOf("learned").forGetter(CreativeSettings::learned),
        Codec.unboundedMap(Identifier.CODEC,Codec.intRange(0,1000000)).fieldOf("mastery").forGetter(CreativeSettings::mastery),
        Codec.string(0,128).listOf(0,12).fieldOf("slots").forGetter(CreativeSettings::slots),Codec.string(0,128).fieldOf("domain").forGetter(CreativeSettings::domain),
        Codec.BOOL.fieldOf("special").forGetter(CreativeSettings::special)
    ).apply(i,CreativeSettings::new));
    public static final CreativeSettings DEFAULT=new CreativeSettings(JjkCraft.id("grade_4"),0,Growth.NONE,Energy.DEFAULT,List.of(),"",20,false,List.of(),Map.of(),List.of(),"",false);
    public record Energy(int birth,int trained,int regen,double stored,boolean reinforcement,boolean active,int output,int healing,int threshold,boolean rct,boolean autoRct) {
        public static final Energy DEFAULT=new Energy(100,0,1,100,false,false,100,100,60,false,true);
        public static final Codec<Energy> CODEC=RecordCodecBuilder.create(i->i.group(
            Codec.intRange(0,3000).fieldOf("birth").forGetter(Energy::birth),Codec.intRange(0,10000).fieldOf("trained").forGetter(Energy::trained),
            Codec.intRange(1,10).fieldOf("regen").forGetter(Energy::regen),Codec.doubleRange(0,1000000000).fieldOf("stored").forGetter(Energy::stored),
            Codec.BOOL.fieldOf("reinforcement").forGetter(Energy::reinforcement),Codec.BOOL.fieldOf("active").forGetter(Energy::active),
            Codec.intRange(25,300).fieldOf("output").forGetter(Energy::output),Codec.intRange(25,300).fieldOf("healing").forGetter(Energy::healing),
            Codec.intRange(25,100).fieldOf("threshold").forGetter(Energy::threshold),Codec.BOOL.fieldOf("rct").forGetter(Energy::rct),Codec.BOOL.fieldOf("auto_rct").forGetter(Energy::autoRct)
        ).apply(i,Energy::new));
    }
}
