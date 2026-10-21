package com.jjkcraft.client.screen;

import java.util.List;
import com.jjkcraft.client.ui.Palette;
import com.jjkcraft.technique.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Terrain and route checks are cached; drawing the editor does no collision scans. */
final class ProjectionPreview {
    final int[] cells=new int[144],states=new int[8];
    final Vec3[] destinations=new Vec3[8];
    private long sampled=-100;private Vec3 origin=Vec3.ZERO;private float yaw;
    private net.minecraft.world.level.Level level;
    boolean dirty=true;
    void refresh(Player p,List<ProjectionPlan.Step> steps,double reach){
        if(p==null)return;long now=p.level().getGameTime();float facing=p.getYRot()+com.jjkcraft.client.state.ClientPersonalization.get(com.jjkcraft.JjkCraft.id("projection_sorcery")).details().expression().yaw();
        if(!dirty&&level==p.level()&&now-sampled<10&&p.position().distanceToSqr(origin)<.01&&Math.abs(facing-yaw)<1)return;
        dirty=false;sampled=now;origin=p.position();yaw=facing;level=p.level();
        double a=Math.toRadians(yaw);Vec3 right=new Vec3(Math.cos(a),0,Math.sin(a)),forward=new Vec3(-Math.sin(a),0,Math.cos(a));
        for(int z=0;z<12;z++)for(int x=0;x<12;x++){
            Vec3 desired=origin.add(right.scale((x-5.5)*2)).add(forward.scale((5.5-z)*2));var foot=ProjectionPlans.ground(p,desired);
            cells[z*12+x]=foot==null?1:Math.abs(foot.y-origin.y)>.05?2:0;
        }
        java.util.Arrays.fill(states,0);java.util.Arrays.fill(destinations,null);Vec3 last=origin;boolean broken=false;
        for(int i=0;i<steps.size();i++){
            var step=steps.get(i);Vec3 desired=origin.add(right.scale(step.right())).add(forward.scale(step.forward()));var foot=ProjectionPlans.ground(p,desired);destinations[i]=foot;
            if(broken){states[i]=4;continue;}
            int state=desired.distanceTo(origin)>reach+.01?3:foot==null?1:!ProjectionPlans.sweep(p,last,foot)?2:0;states[i]=state;
            if(state!=0)broken=true;else last=foot;
        }
    }
    int unsafe(List<ProjectionPlan.Step> steps){for(int i=0;i<steps.size();i++)if(states[i]!=0)return i;return -1;}
    static String explanation(int state){return switch(state){case 1->"No safe footing or body clearance";case 2->"Path crosses a wall, gap or steep step";case 3->"Outside current reach";case 4->"Earlier unsafe step blocks this stage";default->"Safe in the current terrain preview";};}
    static int routeColor(int state){return state==0?Palette.energyHigh():state==4?Palette.muted():Palette.danger();}
}
