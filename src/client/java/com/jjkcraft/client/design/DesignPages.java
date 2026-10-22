package com.jjkcraft.client.design;
import com.jjkcraft.client.screen.*;
import net.minecraft.client.gui.screens.Screen;
import com.jjkcraft.JjkCraft;
public final class DesignPages{
    private DesignPages(){}
    public static Screen create(String name){return switch(name){case "PracticeSettingsScreen"->new PracticeSettingsScreen();
        case "ProfileScreen"->new ProfileScreen();case "TraitsScreen"->new TraitsScreen();case "GrowthTreeScreen"->new GrowthTreeScreen();case "PracticeScreen"->new PracticeScreen();case "LoadoutScreen"->new LoadoutScreen();case "FamilyTechniqueScreen"->new FamilyTechniqueScreen();case "TechniqueBindingsScreen"->new TechniqueBindingsScreen();case "TechniqueAbilityScreen"->new TechniqueAbilityScreen(JjkCraft.id("limitless"));case "TechniqueWorkshopScreen"->new TechniqueWorkshopScreen(com.jjkcraft.client.state.ClientTechniques.state().innate().orElse(JjkCraft.id("grudge_bolt")));case "CreativeTechniqueScreen"->new CreativeTechniqueScreen();case "TechniqueWheelScreen"->new TechniqueWheelScreen();case "DomainWorkshopScreen"->new DomainWorkshopScreen(null);case "ProjectionPlannerScreen"->new ProjectionPlannerScreen();case "ShikigamiFusionScreen"->new ShikigamiFusionScreen();case "VowBuilderScreen"->new VowBuilderScreen();case "CodexScreen"->new CodexScreen();case "CullingScreen"->new CullingScreen();case "AwakeningScreen"->new AwakeningScreen();case "SoulScreen"->new com.jjkcraft.client.death.SoulScreen(false);default->null;};}
}
