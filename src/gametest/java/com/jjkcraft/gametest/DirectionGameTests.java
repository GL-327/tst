package com.jjkcraft.gametest;

import java.util.*;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.network.payload.TechniqueFxPayload;
import com.jjkcraft.technique.*;
import com.jjkcraft.vfx.EffectOrientation;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;

public final class DirectionGameTests {
    @GameTest(environment="jjkcraft:expansion") public void effectFramesRemainOrthonormalAtEveryPitch(GameTestHelper h){
        for(int yaw=-180;yaw<=180;yaw+=30)for(int pitch=-90;pitch<=90;pitch+=15){double y=Math.toRadians(yaw),p=Math.toRadians(pitch);Vec3 aim=new Vec3(-Math.sin(y)*Math.cos(p),-Math.sin(p),Math.cos(y)*Math.cos(p));var frame=EffectOrientation.of(aim);
            h.assertTrue(frame.forward().distanceTo(aim)<1e-7,"forward keeps yaw and pitch");h.assertTrue(Math.abs(frame.right().dot(frame.up()))<1e-8&&Math.abs(frame.forward().dot(frame.right()))<1e-8,"axes remain perpendicular");h.assertTrue(Math.abs(frame.right().length()-1)<1e-8&&Math.abs(frame.up().length()-1)<1e-8,"pole axes never collapse");}
        h.assertTrue(EffectOrientation.of(new Vec3(Double.NaN,0,0)).forward().equals(new Vec3(0,0,1)),"invalid legacy input has a finite fallback");h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion") public void speechRadiusNeverBecomesItsDirectionAndSnapshotSurvivesTurning(GameTestHelper h){
        var p=TestPlayers.fresh(h);var id=JjkCraft.id("cursed_speech_dont_move");var d=Techniques.get(p.registryAccess(),id).orElseThrow();
        for(int yaw:new int[]{0,90,180,-90})for(int pitch:new int[]{-60,0,60}){p.setYRot(yaw);p.setXRot(pitch);var ctx=new CastContext(p,h.getLevel(),id,d,1,1);var packet=TechniqueFx.payload(ctx,TechniqueFxPayload.Phase.CAST,ctx.eye(),new Vec3(13,0,0),List.of());Vec3 original=ctx.look();p.setYRot(yaw+80);p.setXRot(0);
            h.assertTrue(packet.aim().distanceTo(original)<1e-8,"event preserves the cast direction after turning");h.assertTrue(packet.direction().equals(new Vec3(13,0,0)),"radius metadata is retained independently");
            var bytes=io.netty.buffer.Unpooled.buffer();try{TechniqueFxPayload.CODEC.encode(bytes,packet);var restored=TechniqueFxPayload.CODEC.decode(bytes);h.assertTrue(restored.equals(packet)&&!bytes.isReadable(),"aim and parameters survive exact network roundtrip");}finally{bytes.release();}}
        h.succeed();
    }
    @GameTest(environment="jjkcraft:expansion") public void personalizedYawAndPitchAreIncludedInEffectAim(GameTestHelper h){
        var p=TestPlayers.fresh(h);com.jjkcraft.sorcerer.Awakening.awaken(p,false);var id=JjkCraft.id("cursed_speech_dont_move");TechniqueService.grant(p,id);Personalization.save(p,id,TechniqueTuning.DEFAULT.with(16,70).with(17,-35));p.setYRot(0);p.setXRot(0);var d=Techniques.get(p.registryAccess(),id).orElseThrow();var ctx=new CastContext(p,h.getLevel(),id,d,1,1);var fx=TechniqueFx.payload(ctx,TechniqueFxPayload.Phase.CAST,ctx.eye(),new Vec3(9,0,0),List.of());
        h.assertTrue(fx.aim().x<-.7&&fx.aim().y>.5&&fx.aim().distanceTo(ctx.look())<1e-8,"render direction follows the personalized cast, not the unmodified observer camera");h.succeed();
    }
}
