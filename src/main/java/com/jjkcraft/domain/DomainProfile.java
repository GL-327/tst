package com.jjkcraft.domain;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
/** Bounded, persistent domain expression; every mechanical gain has a server-paid CE cost. */
public record DomainProfile(int size,int output,int duration,int barrier,int color,int secondary,boolean open,String title,String ambience){
    public static final DomainProfile DEFAULT=new DomainProfile(100,100,100,100,-1,-1,false,"","");
    public static final Codec<DomainProfile> CODEC=RecordCodecBuilder.create(i->i.group(
        Codec.intRange(50,200).optionalFieldOf("size",100).forGetter(DomainProfile::size),Codec.intRange(50,200).optionalFieldOf("output",100).forGetter(DomainProfile::output),
        Codec.intRange(50,200).optionalFieldOf("duration",100).forGetter(DomainProfile::duration),Codec.intRange(50,200).optionalFieldOf("barrier",100).forGetter(DomainProfile::barrier),
        Codec.intRange(-1,0xffffff).optionalFieldOf("color",-1).forGetter(DomainProfile::color),Codec.intRange(-1,0xffffff).optionalFieldOf("secondary",-1).forGetter(DomainProfile::secondary),
        Codec.BOOL.optionalFieldOf("open",false).forGetter(DomainProfile::open),Codec.string(0,48).optionalFieldOf("title","").forGetter(DomainProfile::title),Codec.string(0,24).optionalFieldOf("ambience","").forGetter(DomainProfile::ambience)
    ).apply(i,DomainProfile::new));
    public double cost(){return Math.pow(size/100.0,.9)*Math.pow(output/100.0,1.5)*Math.pow(duration/100.0,.6)*(open?1:Math.pow(barrier/100.0,.4));}
    public DomainProfile withOpen(boolean value){return new DomainProfile(size,output,duration,barrier,color,secondary,value,title,ambience);}
}
