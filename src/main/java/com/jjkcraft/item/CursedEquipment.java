package com.jjkcraft.item;

import java.util.*;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import com.jjkcraft.JjkCraft;
import com.jjkcraft.damage.DamageModifiers;
import com.jjkcraft.damage.JjkDamageTypes;
import com.jjkcraft.energy.CursedEnergy;
import com.jjkcraft.technique.TechniqueService;

/** Wearable armor uses vanilla durability, attributes and equipment rendering, with bounded set bonuses. */
public final class CursedEquipment {
    private CursedEquipment() { }
    public static final Map<String,Item> ITEMS=new LinkedHashMap<>();
    public static final ArmorMaterial RITUAL_MATERIAL=new ArmorMaterial(28,ArmorMaterials.makeDefense(3,5,7,3,7),18,SoundEvents.ARMOR_EQUIP_NETHERITE,1.5f,.025f,
        net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,JjkCraft.id("ritual_armor_repair")),ResourceKey.create(EquipmentAssets.ROOT_ID,JjkCraft.id("ritual")));
    public static Item PLAYFUL_CLOUD,DRAGON_BONE;
    public static final Item BLACK_ROPE=extraWeapon("black_rope",ToolMaterial.IRON,1,-2.2f);
    public static final Item EXECUTIONERS_BLADE=extraWeapon("executioners_blade",ToolMaterial.DIAMOND,4,-2.8f);
    public static final Item SOULSTEEL_KATANA=extraWeapon("soulsteel_katana",ToolMaterial.DIAMOND,3,-2.5f);
    public static final Item RITUAL_HOOD=ritual("ritual_hood",ArmorType.HELMET);
    public static final Item RITUAL_COAT=ritual("ritual_coat",ArmorType.CHESTPLATE);
    public static final Item RITUAL_LEGGINGS=ritual("ritual_leggings",ArmorType.LEGGINGS);
    public static final Item RITUAL_BOOTS=ritual("ritual_boots",ArmorType.BOOTS);
    public static final Item WARDING_TALISMAN=JjkItems.register("warding_talisman",p->new CursedRelicItem(p,false),new Item.Properties().stacksTo(16).rarity(Rarity.RARE));
    public static final Item SEALED_RELIQUARY=JjkItems.register("sealed_reliquary",p->new CursedRelicItem(p,true),new Item.Properties().stacksTo(4).rarity(Rarity.EPIC));
    private static Item extraWeapon(String key,ToolMaterial mat,float damage,float speed){return JjkItems.register(key,com.jjkcraft.item.tool.CursedToolItem::new,new Item.Properties().sword(mat,damage,speed).rarity(Rarity.RARE));}
    private static Item ritual(String key,ArmorType type){return JjkItems.register(key,CursedArmorItem::new,new Item.Properties().humanoidArmor(RITUAL_MATERIAL,type).rarity(Rarity.RARE));}
    private static final EquipmentSlot[] SLOTS={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
    private static final ArmorMaterial UNIFORM=material("sorcerer_uniform",16,Map.of(ArmorType.HELMET,2,ArmorType.CHESTPLATE,4,ArmorType.LEGGINGS,3,ArmorType.BOOTS,1),0,0);
    private static final ArmorMaterial WARD=material("binding_vestments",30,Map.of(ArmorType.HELMET,3,ArmorType.CHESTPLATE,7,ArmorType.LEGGINGS,5,ArmorType.BOOTS,3),2,0);
    private static final ArmorMaterial HUNTER=material("heavenly_hunter",32,Map.of(ArmorType.HELMET,3,ArmorType.CHESTPLATE,7,ArmorType.LEGGINGS,6,ArmorType.BOOTS,3),2,.05f);
    private static ArmorMaterial material(String name,int durability,Map<ArmorType,Integer> armor,float toughness,float knockback){
        return new ArmorMaterial(durability,armor,18,SoundEvents.ARMOR_EQUIP_LEATHER,toughness,knockback,ItemTags.REPAIRS_LEATHER_ARMOR,ResourceKey.create(EquipmentAssets.ROOT_ID,JjkCraft.id(name)));
    }
    private static class Vestment extends Item {
        final String family;
        Vestment(Properties p,String family){super(p);this.family=family;}
        @Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,Consumer<Component> out,TooltipFlag flag){
            out.accept(Component.translatable("equipment.jjkcraft."+family+".desc").withStyle(ChatFormatting.AQUA));
        }
    }
    public static int pieces(net.minecraft.world.entity.LivingEntity entity,String family){int n=0;for(var slot:SLOTS)if(entity.getItemBySlot(slot).getItem() instanceof Vestment v&&v.family.equals(family))n++;return n;}
    public static void init(){
        for(var entry:Map.of("sorcerer_uniform",UNIFORM,"binding_vestments",WARD,"heavenly_hunter",HUNTER).entrySet())for(var type:List.of(ArmorType.HELMET,ArmorType.CHESTPLATE,ArmorType.LEGGINGS,ArmorType.BOOTS)){
            String key=entry.getKey()+"_"+type.getSerializedName();
            ITEMS.put(key,JjkItems.register(key,p->new Vestment(p,entry.getKey()),new Item.Properties().humanoidArmor(entry.getValue(),type).rarity(Rarity.RARE)));
        }
        weapon("playful_cloud",ToolMaterial.NETHERITE,5,-2.7f);
        weapon("dragon_bone",ToolMaterial.DIAMOND,5,-2.8f);
        weapon("stormbell_spear",ToolMaterial.IRON,4,-2.6f);
        weapon("ember_wakizashi",ToolMaterial.IRON,3,-2.2f);
        weapon("sealbreaker_tanto",ToolMaterial.DIAMOND,3,-2.0f);
        PLAYFUL_CLOUD=ITEMS.get("playful_cloud");DRAGON_BONE=ITEMS.get("dragon_bone");
        ITEMS.putAll(Map.of("black_rope",BLACK_ROPE,"executioners_blade",EXECUTIONERS_BLADE,"soulsteel_katana",SOULSTEEL_KATANA,"ritual_hood",RITUAL_HOOD,"ritual_coat",RITUAL_COAT,"ritual_leggings",RITUAL_LEGGINGS,"ritual_boots",RITUAL_BOOTS,"warding_talisman",WARDING_TALISMAN,"sealed_reliquary",SEALED_RELIQUARY));
        TechniqueService.addModifierProvider((caster,id,d)->new TechniqueService.Modifiers(1,1-.02*pieces(caster,"sorcerer_uniform"),1));
        DamageModifiers.add((level,target,source,amount)->{
            int count=pieces(target,"binding_vestments");if(count==0||source.is(JjkDamageTypes.SURE_HIT)||source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY))return amount;
            double share=amount*.05*count;return CursedEnergy.trySpend(target,share*1.75)?(float)(amount-share):amount;
        });
        DamageModifiers.add((level,target,source,amount)->{
            if(!(source instanceof com.jjkcraft.damage.TechniqueDamageSource)||source.is(JjkDamageTypes.SURE_HIT)||source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY))return amount;
            int count=0;for(var slot:SLOTS)if(target.getItemBySlot(slot).getItem() instanceof CursedArmorItem)count++;
            float share=amount*.04f*count;return share>0&&CursedEnergy.trySpend(target,share*1.5)?amount-share:amount;
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{if(server.getTickCount()%10!=0)return;for(var p:server.getPlayerList().getPlayers()){
            var speed=p.getAttribute(Attributes.MOVEMENT_SPEED);if(speed==null)continue;
            var id=JjkCraft.id("hunter_stride");int count=pieces(p,"heavenly_hunter");
            speed.removeModifier(id);if(count>0)speed.addTransientModifier(new AttributeModifier(id,count*.03,AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }});
    }
    private static void weapon(String key,ToolMaterial material,float damage,float speed){ITEMS.put(key,JjkItems.register(key,com.jjkcraft.item.tool.CursedToolItem::new,new Item.Properties().sword(material,damage,speed).rarity(Rarity.EPIC)));}
}
