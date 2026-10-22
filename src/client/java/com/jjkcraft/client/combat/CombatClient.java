package com.jjkcraft.client.combat;

import net.minecraft.client.Minecraft;

import net.minecraft.world.phys.HitResult;

import net.minecraft.network.chat.Component;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

import com.jjkcraft.JjkCraft;

import com.jjkcraft.client.JjkKeys;

import com.jjkcraft.client.state.ClientProgress;

import com.jjkcraft.client.ui.Ink;

import com.jjkcraft.client.ui.Palette;

import com.jjkcraft.network.payload.CombatPayloads;

public final class CombatClient {

    private static CombatPayloads.View view = new CombatPayloads.View(false, 100, 0, 0, "", 0);

    private static boolean pressed;
    private static CombatPayloads.AutoStats autoStats=new CombatPayloads.AutoStats(0,0,0);
    private static boolean attackClick;

    private static boolean lightDown,heavyDown,leftDown;
    private static CombatPayloads.Endurance endurance=new CombatPayloads.Endurance(100,100,0,0,0);
    public static CombatPayloads.Endurance endurance(){return endurance;}
    private static int autoNext,manualUntil;
    private static boolean migrated;

    private static int guard = -1, ticks, lastMovement;

    private static final long[] lastTap = {-100, -100, -100, -100};

    private static int lockTarget = -1;

    private static int lastAttack=-100;

    private static CombatPayloads.Resources resources=new CombatPayloads.Resources(0,0,0);

    private CombatClient() { }

    public static boolean enabled() { return view.enabled() && ClientProgress.get().awakened(); }
    public static boolean staggered(){return view.stun()>0;}

    public static void init() {
        PunchImbuementClient.init();
        ClientPlayNetworking.registerGlobalReceiver(CombatPayloads.Endurance.TYPE,(next,ctx)->endurance=next);
        com.jjkcraft.client.vfx.ChargeFx.init();
        ClientPlayNetworking.registerGlobalReceiver(CombatPayloads.AutoStats.TYPE,(next,context)->autoStats=next);

        com.jjkcraft.client.vfx.CombatImpactFx.init();

        ClientPlayNetworking.registerGlobalReceiver(CombatPayloads.Resources.TYPE,(next,ctx)->resources=next);

        ClientPlayNetworking.registerGlobalReceiver(CombatPayloads.View.TYPE, (next, context) -> view = next);

        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> { view = new CombatPayloads.View(false, 100, 0, 0, "", 0); autoStats=new CombatPayloads.AutoStats(0,0,0); resources=new CombatPayloads.Resources(0,0,0);endurance=new CombatPayloads.Endurance(100,100,0,0,0);leftDown=false;lastAttack=-100;pressed = false;attackClick=false;lightDown=false;heavyDown=false;autoNext=0;manualUntil=0; guard = -1; lockTarget = -1; lastMovement = 0; ticks = 0; java.util.Arrays.fill(lastTap, -100); });

        ClientPreAttackCallback.EVENT.register((mc, player, clicks) -> {

            if (!enabled() || mc.gui.screen() != null || (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK)) return false;

            // Vanilla already resolved the player's Attack mapping, including swapped mouse buttons.
            // A full press/release can arrive between ticks: remember the callback, not just isDown().
            if (clicks > 0) attackClick = true;
            return true;

        });

        JjkKeys.addHandler(CombatClient::tick);

        HudElementRegistry.addLast(JjkCraft.id("combat"), (g, dt) -> {

            Minecraft mc = Minecraft.getInstance();

            if (!enabled() || mc.player == null || mc.gui.hud.isHidden()||mc.gui.screen()!=null) return;

            var config=com.jjkcraft.client.config.ClientConfig.get();var panel=config.hud.combatPanel;if(!panel.visible)return;
            float alpha=panel.opacity();com.jjkcraft.client.hud.HudLayout.push(g,panel,110,PunchImbuementClient.label().isEmpty()?54:65);
            Ink.panel(g,0,0,110,PunchImbuementClient.label().isEmpty()?54:65,view.stun()>0?Palette.danger():Palette.accent(),alpha);
            String status=view.stun()>0?"STAGGERED":view.move().isEmpty()?config.autoCombat?"AUTO COMBAT":"MANUAL COMBAT":view.move().replace('_',' ').toUpperCase(java.util.Locale.ROOT)+(view.combo()>1?" · "+view.combo():"");
            Ink.text(g,Ink.fit(Component.literal(status),102),4,4,view.stun()>0?Palette.danger():view.combo()>1?Palette.gold():Palette.TEXT,alpha);
            String detail=config.autoCombat?autoStats.attempts()<5?"60% commitment":"Contact "+autoStats.hits()+" / "+autoStats.attempts()+" · "+(100*autoStats.hits()/Math.max(1,autoStats.attempts()))+"%":"Hits: "+JjkKeys.LIGHT.getTranslatedKeyMessage().getString()+" / "+JjkKeys.LEFT_PUNCH.getTranslatedKeyMessage().getString();
            Ink.text(g,Ink.fit(Component.literal(detail),102),4,15,Palette.TEXT_DIM,alpha);
            int guardColor=view.guard()<30?Palette.danger():Palette.energyHigh();g.fill(4,26,106,28,net.minecraft.util.ARGB.multiplyAlpha(0xFF241D31,alpha));g.fill(4,26,4+(int)Math.clamp(102*view.guard()/100,0,102),28,net.minecraft.util.ARGB.multiplyAlpha(guardColor,alpha));
            String escape=resources.escapeCooldown()==0?"Evade: "+JjkKeys.DASH.getTranslatedKeyMessage().getString():"Evade: "+String.format(java.util.Locale.ROOT,"%.1f s",resources.escapeCooldown()/20.0);
            Ink.text(g,Ink.fit(Component.literal(escape),102),4,32,resources.escapeCooldown()==0?Palette.good():Palette.TEXT_DIM,alpha);
            if(!PunchImbuementClient.label().isEmpty())Ink.text(g,Ink.fit(Component.literal(PunchImbuementClient.label()),102),4,56,PunchImbuementClient.mode()==2?0xFFFF6A7A:Palette.energyHigh(),alpha);
            Ink.text(g,Component.literal("Stamina "+Math.round(endurance.stamina())+" / "+Math.round(endurance.maximum())),4,43,Palette.TEXT_DIM,alpha);
            g.fill(4,52,4+(int)(102*endurance.stamina()/Math.max(1,endurance.maximum())),54,net.minecraft.util.ARGB.multiplyAlpha(endurance.stamina()<24?Palette.danger():Palette.gold(),alpha));
            g.pose().popMatrix();

        });

    }

    private static void send(int action, int data) { if(com.jjkcraft.client.vessel.VesselClient.controlled())return;ClientPlayNetworking.send(new CombatPayloads.Input(action, data)); }

    private static void tick(Minecraft mc) {

        ticks++;

        boolean focused=!com.jjkcraft.client.vessel.VesselClient.controlled()&&mc.player!=null&&mc.gui.screen()==null&&enabled();

        boolean light=focused&&JjkKeys.LIGHT.isDown(),left=focused&&JjkKeys.LEFT_PUNCH.isDown(),heavy=focused&&JjkKeys.HEAVY.isDown();

        boolean lightClick=JjkKeys.clicked(JjkKeys.LIGHT),leftClick=JjkKeys.clicked(JjkKeys.LEFT_PUNCH),heavyClick=JjkKeys.clicked(JjkKeys.HEAVY);
        if(focused&&(light||lightClick)&&!lightDown){send(0,0);manualUntil=ticks+16;}if(focused&&(left||leftClick)&&!leftDown){send(9,0);manualUntil=ticks+16;}if(focused&&(heavy||heavyClick)&&!heavyDown){send(5,0);manualUntil=ticks+16;}
        while(JjkKeys.AUTO_COMBAT.consumeClick())if(focused){com.jjkcraft.client.config.ClientConfig.get().autoCombat=!com.jjkcraft.client.config.ClientConfig.get().autoCombat;com.jjkcraft.client.config.ClientConfig.save();}
        if(!migrated&&mc.player!=null){migrated=true;
            var config=com.jjkcraft.client.config.ClientConfig.get();
            if(config.controlsRevision<2){
                if(net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.getBoundKeyOf(JjkKeys.HEAVY).getValue()==com.mojang.blaze3d.platform.InputConstants.KEY_I)JjkKeys.HEAVY.setKey(JjkKeys.HEAVY.getDefaultKey());
                int block=net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.getBoundKeyOf(JjkKeys.BLOCK).getValue();
                if(block==com.mojang.blaze3d.platform.InputConstants.KEY_O||block==com.mojang.blaze3d.platform.InputConstants.KEY_F)JjkKeys.BLOCK.setKey(JjkKeys.BLOCK.getDefaultKey());
                config.controlsRevision=2;
            }
            if(!config.irisBindingsAdjusted&&net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("iris")){
                String[] names={"iris.keybind.reload","iris.keybind.toggleShaders","iris.keybind.shaderPackSelection"};
                int[] destinations={com.mojang.blaze3d.platform.InputConstants.KEY_F8,com.mojang.blaze3d.platform.InputConstants.KEY_F9,com.mojang.blaze3d.platform.InputConstants.KEY_F10};
                for(int i=0;i<names.length;i++){
                    var key=net.minecraft.client.KeyMapping.get(names[i]);
                    if(key!=null&&net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.getBoundKeyOf(key).equals(key.getDefaultKey())){
                        var next=com.mojang.blaze3d.platform.InputConstants.Type.KEYBOARD.getOrCreate(destinations[i]);boolean occupied=false;
                        for(var other:mc.options.keyMappings)if(other!=key&&net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.getBoundKeyOf(other).equals(next)){occupied=true;break;}
                        key.setKey(occupied?com.mojang.blaze3d.platform.InputConstants.UNKNOWN:next);
                    }
                }
                config.irisBindingsAdjusted=true;
            }
            com.jjkcraft.client.config.ClientConfig.save();
            for(int n=0;n<4;n++)if(net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.getBoundKeyOf(JjkKeys.SKILLS[n]).getValue()==new int[]{com.mojang.blaze3d.platform.InputConstants.KEY_1,com.mojang.blaze3d.platform.InputConstants.KEY_2,com.mojang.blaze3d.platform.InputConstants.KEY_3,com.mojang.blaze3d.platform.InputConstants.KEY_4}[n])JjkKeys.SKILLS[n].setKey(JjkKeys.SKILLS[n].getDefaultKey());
            if(net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.getBoundKeyOf(JjkKeys.COMBAT).getValue()==com.mojang.blaze3d.platform.InputConstants.KEY_C)JjkKeys.COMBAT.setKey(JjkKeys.COMBAT.getDefaultKey());
            net.minecraft.client.KeyMapping.resetMapping();mc.options.save();
            if(net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.getBoundKeyOf(JjkKeys.BLOCK).getValue()==com.mojang.blaze3d.platform.InputConstants.KEY_F){JjkKeys.BLOCK.setKey(JjkKeys.BLOCK.getDefaultKey());net.minecraft.client.KeyMapping.resetMapping();mc.options.save();}
        }

        lightDown=light;heavyDown=heavy;leftDown=left;

        while(JjkKeys.LIGHT.consumeClick()){}while(JjkKeys.HEAVY.consumeClick()){}

        while (JjkKeys.COMBAT.consumeClick()) if (mc.player != null && mc.gui.screen() == null) send(4, 0);
        while(JjkKeys.CE_IMBUE.consumeClick())if(mc.player!=null&&mc.gui.screen()==null)send(8,0);

        if (!enabled() || mc.player == null) { pressed=false;attackClick=false;guard=-1;autoNext=0;return; }

        boolean inGame = mc.gui.screen() == null && mc.player.isAlive();

        boolean attack = inGame && (mc.options.keyAttack.isDown() || attackClick) && (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK);
        boolean clicked=attackClick;attackClick=false;

        boolean auto=com.jjkcraft.client.config.ClientConfig.get().autoCombat&&ticks>=manualUntil;
        boolean autoGuard=false;
        if(auto&&attack){
            net.minecraft.world.entity.LivingEntity enemy=mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit&&hit.getEntity() instanceof net.minecraft.world.entity.LivingEntity e?e:null;
            if(enemy==null&&mc.level!=null)enemy=mc.level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,mc.player.getBoundingBox().inflate(4),e->e!=mc.player&&e.isAlive()&&mc.player.hasLineOfSight(e)&&e.position().subtract(mc.player.position()).normalize().dot(mc.player.getLookAngle())>0.65).stream().min(java.util.Comparator.comparingDouble(mc.player::distanceToSqr)).orElse(null);
            var pose=enemy==null?null:CombatAnimations.pose(enemy.getId());
            boolean threat=pose!=null&&!pose.move().isEmpty()&&mc.level.getGameTime()-pose.started()>=5&&mc.level.getGameTime()-pose.started()<pose.startup()+pose.active();
            int choice=com.jjkcraft.core.combat.AutoCombatMath.action(enemy==null?4:mc.player.distanceTo(enemy),pose==null?-1:pose.guard(),threat);
            autoGuard=choice==2;
            if(!autoGuard&&(ticks>=autoNext||clicked&&!pressed)){send(6,choice);autoNext=ticks+(choice==5?20:8);lastAttack=ticks;}
            pressed=true;
        }else if(attack&&!pressed){send(mc.player.getMainArm()==net.minecraft.world.entity.HumanoidArm.LEFT&&(!mc.player.getMainHandItem().isEmpty()||!mc.player.getOffhandItem().isEmpty())?9:0,0);pressed=true;lastAttack=ticks;}

        if (!attack && pressed) { send(1, mc.options.keyDown.isDown() ? -1 : mc.options.keyLeft.isDown() || mc.options.keyRight.isDown() ? 1 : 0); pressed = false; }

        boolean usableHand = mc.player.getMainHandItem().isEmpty() || !mc.player.getMainHandItem().has(net.minecraft.core.component.DataComponents.CONSUMABLE);

        int height = inGame && usableHand && (JjkKeys.BLOCK.isDown()||autoGuard) && (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK)

            ? mc.player.isShiftKeyDown() ? 0 : 1 : -1;

        if(JjkKeys.BLOCK.isDown())manualUntil=ticks+8;
        if (height != guard || height >= 0 && ticks % 4 == 0) { send(auto&&!JjkKeys.BLOCK.isDown()?7:2, height); guard = height; }

        int movement = (mc.options.keyUp.isDown() ? 1 : 0) | (mc.options.keyDown.isDown() ? 2 : 0) | (mc.options.keyLeft.isDown() ? 4 : 0) | (mc.options.keyRight.isDown() ? 8 : 0);

        lastMovement = movement;

        while (JjkKeys.DASH.consumeClick()) if (inGame) send(3, mc.options.keyDown.isDown() ? -1 : mc.options.keyLeft.isDown() ? 1 : mc.options.keyRight.isDown() ? 2 : 0);

        while (JjkKeys.LOCK_ON.consumeClick()) if (inGame) {

            if (lockTarget >= 0) lockTarget = -1;

            else if (mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit && hit.getEntity() instanceof net.minecraft.world.entity.LivingEntity) lockTarget = hit.getEntity().getId();

        }

        var target = mc.level == null ? null : mc.level.getEntity(lockTarget);

        if (inGame && target != null && target.isAlive() && mc.player.distanceToSqr(target) < 20 * 20 && mc.player.hasLineOfSight(target)) {

            var toward = target.getEyePosition().subtract(mc.player.getEyePosition());

            mc.player.setYRot((float) (Math.toDegrees(Math.atan2(toward.z, toward.x)) - 90));

            mc.player.setXRot((float) -Math.toDegrees(Math.atan2(toward.y, toward.horizontalDistance())));

        } else lockTarget = -1;

    }

}
