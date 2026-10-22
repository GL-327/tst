package com.jjkcraft.gametest;

import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.item.CursedEquipment;
import com.jjkcraft.energy.CursedEnergy;

final class EquipmentReworkChecks {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static void run(ClientGameTestContext context){
        boolean old=com.jjkcraft.config.JjkConfig.server().modulo.enabled;com.jjkcraft.config.JjkConfig.server().modulo.enabled=false;
        try(var world=context.worldBuilder().create()){
            var server=world.getServer();world.getConnection().waitForChunksRender();
            server.runCommand("gamemode creative @a");server.runCommand("time set noon");server.runCommand("gamerule spawn_mobs false");
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();com.jjkcraft.awakening.CharacterAwakening.creative(p,JjkCraft.id("blood_manipulation"),false);
                p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(CursedEquipment.RITUAL_HOOD));p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(CursedEquipment.RITUAL_COAT));
                p.setItemSlot(EquipmentSlot.LEGS,new ItemStack(CursedEquipment.RITUAL_LEGGINGS));p.setItemSlot(EquipmentSlot.FEET,new ItemStack(CursedEquipment.RITUAL_BOOTS));
            });
            context.waitFor(mc->com.jjkcraft.client.state.ClientTechniques.known().contains(JjkCraft.id("piercing_blood")));
            context.setScreen(()->null);server.runCommand("gamemode survival @a");
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();
                var type=p.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getOrThrow(com.jjkcraft.damage.JjkDamageTypes.TECHNIQUE);
                var source=new com.jjkcraft.damage.TechniqueDamageSource(type,p,JjkCraft.id("grudge_bolt"));CursedEnergy.set(p,100);
                float reduced=com.jjkcraft.damage.DamageModifiers.apply(p.level(),p,source,10);
                check(Math.abs(reduced-8.4)<.001,"full ritual set wards exactly 16 percent");
                check(Math.abs(CursedEnergy.current(p)-97.6)<.01,"ward pays for prevented damage");
                CursedEnergy.set(p,0);check(com.jjkcraft.damage.DamageModifiers.apply(p.level(),p,source,10)==10,"empty CE cannot power the ward");
                CursedEnergy.set(p,100);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(CursedEquipment.WARDING_TALISMAN,2));
                CursedEquipment.WARDING_TALISMAN.use(p.level(),p,net.minecraft.world.InteractionHand.MAIN_HAND);
                check(p.hasEffect(net.minecraft.world.effect.MobEffects.ABSORPTION)&&p.getMainHandItem().getCount()==1,"talisman applies and consumes one");
                check(Math.abs(CursedEnergy.current(p)-75)<.01,"talisman costs 25 CE");
                p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(CursedEquipment.SEALED_RELIQUARY,2));p.setShiftKeyDown(true);
                CursedEquipment.SEALED_RELIQUARY.use(p.level(),p,net.minecraft.world.InteractionHand.MAIN_HAND);p.setShiftKeyDown(false);
                check(p.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS)&&p.getMainHandItem().getCount()==1,"reliquary consumes one and applies its risk");
                check(Math.abs(CursedEnergy.current(p)-325)<.01,"reliquary restores 250 CE");
                p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(CursedEquipment.SOULSTEEL_KATANA));
                check(!com.jjkcraft.technique.TechniqueService.knows(p,JjkCraft.id("grudge_bolt")),"tool check uses an unlearned technique");
                check(com.jjkcraft.technique.TechniqueSources.grants(p,JjkCraft.id("grudge_bolt")),"holding Soulsteel Katana grants its power");
                check(CursedEquipment.SOULSTEEL_KATANA.use(p.level(),p,net.minecraft.world.InteractionHand.MAIN_HAND)!=net.minecraft.world.InteractionResult.FAIL,"Use activates a held tool's technique");
                check(CursedEnergy.current(p)<325,"tool technique spends CE");
                int slot=0;for(var item:List.of(CursedEquipment.PLAYFUL_CLOUD,CursedEquipment.DRAGON_BONE,CursedEquipment.BLACK_ROPE,CursedEquipment.EXECUTIONERS_BLADE,CursedEquipment.SOULSTEEL_KATANA,CursedEquipment.WARDING_TALISMAN,CursedEquipment.SEALED_RELIQUARY,com.jjkcraft.item.JjkItems.SPLIT_SOUL_KATANA,com.jjkcraft.item.JjkItems.INVERTED_SPEAR_OF_HEAVEN))p.getInventory().setItem(slot++,new ItemStack(item));
                p.getInventory().setSelectedSlot(7);com.jjkcraft.sorcerer.SorcererLifecycle.fullSync(p);
            });
            context.waitTicks(10);context.runOnClient(mc->{mc.gui.hud.getChat().clearMessages(false);mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);});
            context.waitTicks(5);context.takeScreenshot("rework-ritual-armour-worn");
            context.setScreen(()->new net.minecraft.client.gui.screens.inventory.InventoryScreen(net.minecraft.client.Minecraft.getInstance().player));
            context.waitTicks(3);context.takeScreenshot("rework-cursed-equipment-inventory");context.setScreen(()->null);
        }finally{com.jjkcraft.config.JjkConfig.server().modulo.enabled=old;}
    }
}
