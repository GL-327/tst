package com.jjkcraft.curse.spawn;
/** Habitat preferences retain broad encounter variety; high-grade curses need the strongest fear. */
public final class CurseHabitat{
    private CurseHabitat(){}
    public static double weight(String name,String biome,boolean cave,boolean rain,double fear,int rank){
        if(rank>=6&&fear<75||rank>=3&&fear<45)return 0;
        boolean wet=rain||biome.contains("swamp")||biome.contains("river")||biome.contains("ocean")||biome.contains("beach");
        boolean woods=biome.contains("forest")||biome.contains("taiga")||biome.contains("jungle");
        if(name.contains("sewer")||name.contains("drain")||name.contains("gutter")||name.contains("damp"))return cave||wet?2.4:.3;
        if(name.contains("thorn")||name.contains("orchard")||name.contains("moth"))return woods?2.2:cave?.25:.7;
        if(name.contains("ash")||name.contains("soot")||name.contains("rust"))return cave||biome.contains("badlands")||biome.contains("nether")?2:.5;
        if(name.contains("paper")||name.contains("doll")||name.contains("ledger"))return cave?.7:1.5;
        return 1;
    }
}
