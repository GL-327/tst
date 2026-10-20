package com.jjkcraft.client.state;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.fabricmc.fabric.api.client.networking.v1.*;
import com.jjkcraft.awakening.*;
import com.jjkcraft.network.payload.TraitPayload;

public final class ClientTraits {
    private static final Map<Integer,TraitState> STATES=new HashMap<>();
    private static final Map<Integer,Entity> PROXIES=new HashMap<>();
    private static Object world;
    private ClientTraits() { }
    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(TraitPayload.TYPE,(payload,ctx)->STATES.put(payload.entity(),payload.state()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler,mc)->{STATES.clear();PROXIES.clear();world=null;});
        com.jjkcraft.client.screen.JjkScreen.addTab(new com.jjkcraft.client.screen.JjkScreen.Tab(net.minecraft.network.chat.Component.literal("Traits"),com.jjkcraft.client.screen.TraitsScreen.class,com.jjkcraft.client.screen.TraitsScreen::new));
    }
    public static TraitState own() {var p=Minecraft.getInstance().player;return p==null?TraitState.EMPTY:STATES.getOrDefault(p.getId(),TraitState.EMPTY);}
    public static Entity form(Player player) {
        var mc=Minecraft.getInstance();if(world!=mc.level){world=mc.level;PROXIES.clear();}
        var state=STATES.get(player.getId());if(state==null || state.form().isEmpty() || mc.level==null)return null;
        Identifier id=Identifier.tryParse(state.form());if(id==null)return null;
        Entity proxy=PROXIES.get(player.getId());
        if(proxy==null || !BuiltInRegistries.ENTITY_TYPE.getKey(proxy.getType()).equals(id)) {
            var type=BuiltInRegistries.ENTITY_TYPE.getValue(id);if(type==null)return null;
            proxy=type.create(mc.level,EntitySpawnReason.LOAD);if(proxy==null || proxy instanceof Player)return null;
            proxy.setId(player.getId());
            PROXIES.put(player.getId(),proxy);
        }
        boolean animate=proxy.tickCount!=player.tickCount;
        proxy.snapTo(player.getX(),player.getY(),player.getZ(),player.getYRot(),player.getXRot());proxy.xOld=player.xOld;proxy.yOld=player.yOld;proxy.zOld=player.zOld;proxy.yRotO=player.yRotO;proxy.xRotO=player.xRotO;
        proxy.tickCount=player.tickCount;proxy.setDeltaMovement(player.getDeltaMovement());proxy.setCustomName(player.getDisplayName());proxy.setCustomNameVisible(true);
        if(proxy instanceof LivingEntity living){living.setYHeadRot(player.getYHeadRot());living.yBodyRot=player.yBodyRot;living.yBodyRotO=player.yBodyRotO;living.yHeadRotO=player.yHeadRotO;if(animate)living.walkAnimation.update(player.walkAnimation.speed(),1,1);living.setHealth(living.getMaxHealth()*player.getHealth()/player.getMaxHealth());}
        if(proxy instanceof com.jjkcraft.curse.CurseEntity curse)curse.getEntityData().set(com.jjkcraft.curse.CurseEntity.EMERGING,0);
        return proxy;
    }
}
