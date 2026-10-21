package com.jjkcraft.gametest;

import com.jjkcraft.JjkCraft;
import com.jjkcraft.config.JjkConfig;
import com.jjkcraft.technique.*;
import com.jjkcraft.technique.field.SpatialFields;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;

/** Fields expire on the next tick; stress fixtures cannot leak into later scenarios. */
public final class FieldBudgetGameTests {
    private static CastContext context(GameTestHelper h){var p=TestPlayers.fresh(h);var id=JjkCraft.id("lapse_blue");return new CastContext(p,h.getLevel(),id,Techniques.get(p.registryAccess(),id).orElseThrow(),1,1);}
    @GameTest(environment="jjkcraft:field_budget",padding=32) public void fieldOwnerBudgetRetiresOldestWithoutDeletingAnotherOwnersField(GameTestHelper h){
        var a=context(h);var b=context(h);Vec3 at=h.absoluteVec(new Vec3(2,100,2));var other=SpatialFields.place(b,at,.5,1,0,0,0,1);var first=SpatialFields.place(a,at,.5,1,0,0,0,1);int limit=JjkConfig.server().casting.maxSpatialFieldsPerCaster;
        for(int i=0;i<limit+4;i++)SpatialFields.place(a,at,.5,1,0,0,0,1);
        var fields=SpatialFields.active(h.getLevel());h.assertTrue(fields.stream().filter(f->f.caster()==a.caster()).count()==limit,"per-caster budget is exact");h.assertFalse(fields.contains(first),"oldest own field retires");h.assertTrue(fields.contains(other),"another caster's field survives the owner limit");h.succeed();
    }
    @GameTest(environment="jjkcraft:field_budget",padding=32) public void dimensionFieldBudgetBoundsMultipleCasters(GameTestHelper h){
        Vec3 at=h.absoluteVec(new Vec3(2,100,2));int global=JjkConfig.server().casting.maxSpatialFieldsPerLevel,local=JjkConfig.server().casting.maxSpatialFieldsPerCaster;var newest=context(h);
        for(int n=0;n<global/local+2;n++){newest=context(h);for(int i=0;i<local;i++)SpatialFields.place(newest,at,.5,1,0,0,0,1);}
        var fields=SpatialFields.active(h.getLevel());h.assertTrue(fields.size()==global,"dimension budget stays bounded across many owners");h.assertTrue(fields.getLast().caster()==newest.caster(),"newest field remains usable");h.succeed();
    }
}
