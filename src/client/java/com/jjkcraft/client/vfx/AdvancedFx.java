package com.jjkcraft.client.vfx;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.ARGB;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.client.config.ClientConfig;
import com.jjkcraft.client.state.ClientProgress;
import com.jjkcraft.client.state.ClientSorcery;
import com.jjkcraft.client.state.ClientAwakening;
import com.jjkcraft.curse.CurseEntity;

/** Procedural shell geometry, pose echoes, lightning, wound shimmer and bounded fear mist. */
public final class AdvancedFx {
    private record Pose(Vec3 position, float yaw, float pitch, float stride, float height, float width) { }
    private static final Map<Integer, ArrayDeque<Pose>> HISTORY = new HashMap<>();
    private static final Map<Integer, Long> PROJECTION = new HashMap<>();
    private record HeldPose(long frame, float body, float yaw, float pitch, float walk, float speed) { }
    private static final Map<Integer,HeldPose> HELD_POSES = new HashMap<>();
    private static net.minecraft.client.multiplayer.ClientLevel world;
    private static List<LivingEntity> visible = List.of();
    private static boolean registered;
    private AdvancedFx() { }
    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(AdvancedFx::tick);
        HudElementRegistry.addLast(JjkCraft.id("traits_overlay"), (g, dt) -> {
            Minecraft mc = Minecraft.getInstance(); if (mc.player == null || mc.gui.hud.isHidden() || ClientConfig.effectsOff()) return;
            if (ClientAwakening.view().record().sixEyes()) {
                int w = g.guiWidth(), h = g.guiHeight();
                for (int n=0; n<8; n++) { int y = h*(n+1)/9; g.fill(0,y,20+n*3,y+1,0x504DBFFF); g.fill(w-20-n*3,y,w,y+1,0x504DBFFF); }
            }
            if (ClientProgress.get().heavenlyRestricted()) { g.fill(0,0,g.guiWidth(),2,0x5040DBBA); g.fill(0,g.guiHeight()-2,g.guiWidth(),g.guiHeight(),0x5040DBBA); }
        });
    }
    private static void tick(Minecraft mc) {
        if (world != mc.level) { world = mc.level; registered = false; HISTORY.clear(); PROJECTION.clear(); HELD_POSES.clear(); }
        if (world == null || ClientConfig.effectsOff()) { registered = false; visible = List.of(); return; }
        if (mc.player == null || mc.isPaused()) return;
        List<LivingEntity> close = new ArrayList<>();
        for (Entity entity : world.entitiesForRendering()) if (entity instanceof LivingEntity living && living.isAlive() && mc.player.distanceToSqr(living) < 48*48
                && (living instanceof CurseEntity || living instanceof Player)) close.add(living);
        close.sort(Comparator.comparingDouble(mc.player::distanceToSqr));
        visible = close.stream().limit(ClientConfig.lowVfx() ? 12 : 24).toList();
        Set<Integer> present = new HashSet<>();
        for (LivingEntity e : visible) {
            present.add(e.getId());
            ArrayDeque<Pose> poses = HISTORY.computeIfAbsent(e.getId(), id -> new ArrayDeque<>());
            poses.addFirst(new Pose(e.position(), e.getYRot(), e.getXRot(), (float) (Math.sin(world.getGameTime()*0.6)*e.getDeltaMovement().lengthSqr()*7), e.getBbHeight(), e.getBbWidth()));
            while (poses.size() > 5) poses.removeLast();
        }
        HISTORY.keySet().retainAll(present); PROJECTION.values().removeIf(until -> until < world.getGameTime());
        HELD_POSES.keySet().retainAll(PROJECTION.keySet());
        if (!registered) { registered = true; final var level = world; WorldFx.add(frame -> { if (Minecraft.getInstance().level != level || ClientConfig.effectsOff()) return false; render(frame); return true; }); }
    }
    private static int color(LivingEntity e) { return e instanceof CurseEntity c ? c.definition().map(d -> d.codex().color()).orElse(0x8F5CFF) : 0x8F5CFF; }
    private static void render(WorldFx.Frame f) {
        Minecraft mc = Minecraft.getInstance();
        int aura = ClientConfig.effect(ClientConfig.get().vfx.aura);
        for (LivingEntity e : visible) {
            if (!f.budgetLeft()) break;
            boolean firstPerson = e == mc.player && mc.options.getCameraType().isFirstPerson();
            float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            Vec3 base = e.getPosition(partial); int color = color(e);
            if (!firstPerson && aura > 0) shell(f, e, base, color, aura);
            if (e instanceof CurseEntity curse && (curse.isEmerging() || curse.getEntityData().get(CurseEntity.STATE) == com.jjkcraft.curse.brain.CurseBrain.State.CAST.ordinal())) {
                double r = e.getBbWidth()*0.18; Vec3 core = base.add(0,e.getBbHeight()*0.7,0);
                f.beam(core.add(0,-r,0),core.add(0,r,0),r*2,WorldFx.fade(color,0.75f),WorldFx.fade(color,0.1f));
            }
            if (!firstPerson && ClientConfig.effect(ClientConfig.get().vfx.afterimages) > 0 && !ClientConfig.reducedMotion()
                    && (e.getDeltaMovement().horizontalDistanceSqr() > 0.16 || PROJECTION.containsKey(e.getId()))) echoes(f,e,color);
            if (e == mc.player && ClientSorcery.reverse().healing() && ClientConfig.effect(ClientConfig.get().vfx.healing)>0) {
                for (int n=0;n<8;n++) { double a=f.time()*0.15+n*Math.PI/4, y=(n/8.0+f.time()*0.025)%1;
                    Vec3 to=base.add(Math.cos(a)*0.18,y*e.getBbHeight(),Math.sin(a)*0.18);
                    f.beam(to.add(Math.cos(a)*0.6,0.2,Math.sin(a)*0.6),to,0.06,WorldFx.fade(0x4BE8C5,0),WorldFx.fade(0x4BE8C5,0.7f)); }
            }
        }
        if (ClientConfig.effect(ClientConfig.get().vfx.environment)>0 && mc.level != null && mc.level.isDarkOutside()) {
            int count = Math.min(6, visible.size());
            for(int n=0;n<count;n++) { Vec3 p=visible.get(n).position(); double a=f.time()*0.008+n*2;
                f.beam(p.add(Math.cos(a)*2,0.1,Math.sin(a)*2),p.add(-Math.cos(a)*2,0.3,-Math.sin(a)*2),0.8,WorldFx.fade(0x21112F,0),WorldFx.fade(0x42215D,0.08f)); }
        }
    }
    private static void shell(WorldFx.Frame f, LivingEntity e, Vec3 base, int color, int level) {
        int segments = level == 1 ? 8 : level == 2 ? 12 : 18;
        double radius = e.getBbWidth()*0.66 + 0.08;
        for (int band=0;band<4;band++) for(int n=0;n<segments;n++) {
            double a=n*Math.PI*2/segments, b=(n+1)*Math.PI*2/segments;
            double noise=0.92+Math.sin(a*3+f.time()*0.045+e.getId())*0.06+Math.cos(a*7-f.time()*0.03+band)*0.04;
            double y0=band*e.getBbHeight()/4, y1=(band+1)*e.getBbHeight()/4;
            float fade0=(float)Math.sin(Math.PI*band/4)*0.12f, fade1=(float)Math.sin(Math.PI*(band+1)/4)*0.12f;
            f.quad(base.add(Math.cos(a)*radius*noise,y0,Math.sin(a)*radius*noise),base.add(Math.cos(b)*radius*noise,y0,Math.sin(b)*radius*noise),
                base.add(Math.cos(b)*radius*noise,y1,Math.sin(b)*radius*noise),base.add(Math.cos(a)*radius*noise,y1,Math.sin(a)*radius*noise),WorldFx.fade(color,fade0),WorldFx.fade(color,fade1));
        }
    }
    private static void echoes(WorldFx.Frame f, LivingEntity e, int color) {
        var history = HISTORY.get(e.getId()); if(history == null) return;
        int n=0;
        for(Pose pose:history) { if(++n==1) continue; float alpha=(1-n/6f)*0.14f; double yaw=Math.toRadians(pose.yaw()), sx=Math.cos(yaw)*pose.width()/2, sz=Math.sin(yaw)*pose.width()/2;
            Vec3 base=pose.position();
            f.quad(base.add(-sx,0,-sz),base.add(sx,0,sz),base.add(sx,pose.height()*0.8,sz),base.add(-sx,pose.height()*0.8,-sz),WorldFx.fade(color,alpha),WorldFx.fade(color,alpha*0.5f));
            Vec3 head=base.add(0,pose.height()*0.87,0); f.beam(head.add(0,-0.12,0),head.add(0,0.12,0),pose.width()*0.55,WorldFx.fade(color,alpha),WorldFx.fade(color,alpha));
            Vec3 shoulder=base.add(0,pose.height()*0.7,0);
            for (int side : new int[]{-1,1}) {
                Vec3 arm=shoulder.add(sx*side*1.4, -pose.height()*0.28, sz*side*1.4 + Math.sin(pose.stride())*0.12);
                f.beam(shoulder.add(sx*side,0,sz*side),arm,pose.width()*0.22,WorldFx.fade(color,alpha),WorldFx.fade(color,0));
            }
        }
    }
    public static void projection(int entityId) { if(world!=null) PROJECTION.put(entityId,world.getGameTime()+40); }
    public static void quantize(LivingEntity entity, net.minecraft.client.renderer.entity.state.LivingEntityRenderState state, float partial) {
        if (world==null || !PROJECTION.containsKey(entity.getId()) || ClientConfig.reducedMotion() || ClientConfig.effectsOff()) return;
        long frame=(long)Math.floor((world.getGameTime()+partial)*1.2); // 20 TPS × 1.2 = 24 poses/second.
        HeldPose previous=HELD_POSES.get(entity.getId());
        if(previous==null || previous.frame()!=frame) { previous=new HeldPose(frame,state.bodyRot,state.yRot,state.xRot,state.walkAnimationPos,state.walkAnimationSpeed); HELD_POSES.put(entity.getId(),previous); }
        state.bodyRot=previous.body(); state.yRot=previous.yaw(); state.xRot=previous.pitch(); state.walkAnimationPos=previous.walk(); state.walkAnimationSpeed=previous.speed();
    }
    public static void summon(int caster, int summoned) {
        if (ClientConfig.effect(ClientConfig.get().vfx.trails)==0) return;
        Emitters.add(age -> {
            Minecraft mc=Minecraft.getInstance(); if(mc.level==null || age>20)return false;
            Entity target=mc.level.getEntity(summoned), owner=mc.level.getEntity(caster); if(target==null || owner==null)return true;
            float start=mc.level.getGameTime();
            WorldFx.add(f -> { if(mc.level==null || target.isRemoved() || f.time()-start>18)return false;
                Vec3 base=owner.position().add(0,0.04,0), to=target.position().add(0,0.05,0); float fade=1-(f.time()-start)/18;
                for(int strand=0;strand<4;strand++) { Vec3 previous=base;
                    for(int n=1;n<=10;n++) { double t=n/10.0; Vec3 next=base.lerp(to,t).add(Math.sin(t*8+strand+f.time()*0.1)*0.2,Math.sin(t*Math.PI)*0.18,Math.cos(t*8+strand)*0.2);
                        f.beam(previous,next,0.1,WorldFx.fade(0x1B1029,fade),WorldFx.fade(0x4A2868,fade*0.5f)); previous=next; }
                } return true; }); return false;
        });
    }
    public static void blackFlash(int target) {
        Minecraft mc=Minecraft.getInstance(); if(mc.level==null || ClientConfig.effect(ClientConfig.get().vfx.impacts)==0) return;
        Entity entity=mc.level.getEntity(target); if(entity==null)return;
        float start=mc.level.getGameTime(); Vec3 at=entity.position().add(0,entity.getBbHeight()*0.6,0);
        WorldFx.add(f -> { float age=f.time()-start;if(age>8)return false;
            int seed=target*61;
            for(int ray=0;ray<6;ray++) { Vec3 last=at;
                for(int n=1;n<=6;n++) { double angle=ray*Math.PI/3, reach=n*0.25; seed=seed*1664525+1013904223;
                    double jitter=((seed>>>16)&255)/255.0-0.5; Vec3 next=at.add(Math.cos(angle)*reach,jitter*0.45,Math.sin(angle)*reach);
                    f.beam(last,next,0.12,WorldFx.fade(0x09050A,1-age/8),WorldFx.fade(0x09050A,1-age/8));
                    f.beam(last.add(0,0.01,0),next.add(0,0.01,0),0.035,WorldFx.fade(0xE0303F,1-age/8),WorldFx.fade(0xE0303F,1-age/8)); last=next; }
            } return true; });
    }
}
