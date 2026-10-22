package com.jjkcraft.combat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
/** One server/client classification, retaining actual held-item damage, durability and enchantments. */
public final class WeaponCombat{
    private WeaponCombat(){}
    public static String kind(ItemStack stack){
        if(stack.isEmpty())return "";String key=BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if(key.contains("spear")||key.contains("staff")||key.contains("trident")||key.contains("chain"))return "pole";
        if(key.contains("mace")||key.contains("cleaver")||key.equals("dragon_bone")||key.equals("playful_cloud"))return "club";
        if(key.contains("sword")||key.contains("katana")||key.contains("blade")||key.contains("tanto")||key.contains("wakizashi")||key.contains("pickaxe")||key.endsWith("axe"))return "blade";
        return "item";
    }
    public static CombatMove move(CombatMove raw,String kind,boolean heavy,int training){
        int wind=kind.equals("club")?8:kind.equals("pole")?6:4;
        int recover=Math.max(4,(kind.equals("club")?11:kind.equals("pole")?8:6)-training/5);
        return new CombatMove(wind+(heavy?5:0),3,recover+(heavy?4:0),raw.damage()*(kind.equals("item")?.6:heavy?1.15:1),raw.ce(),raw.height(),Math.min(5,raw.reach()+(kind.equals("pole")?1.1:kind.equals("item")?0:.45)),raw.guardDamage()*(heavy?1.4:1.1),Math.min(7,raw.stun()),raw.knockback(),raw.lift());
    }
    public static String animation(String kind,String original,int chain){return kind+"_"+(original.equals("heavy")||original.equals("sweep")?"heavy":original.equals("uppercut")?"thrust":chain%2==0?"cut":"return");}
}
