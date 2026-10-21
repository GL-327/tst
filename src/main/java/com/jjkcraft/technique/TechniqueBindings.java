package com.jjkcraft.technique;

import java.util.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import com.jjkcraft.awakening.CharacterAwakening;
import com.jjkcraft.awakening.InnateFamily;

/** Family bindings are selectors, never executable or implicitly learned techniques. */
public final class TechniqueBindings {
    private TechniqueBindings() { }
    public static boolean family(Identifier id) { return id.getPath().startsWith("family/"); }
    public static Identifier binding(Identifier family) { return Identifier.fromNamespaceAndPath(family.getNamespace(),"family/"+family.getPath()); }
    public static Optional<InnateFamily> definition(RegistryAccess access,Identifier binding) {
        if(!family(binding))return Optional.empty();
        return Optional.ofNullable(access.lookupOrThrow(CharacterAwakening.FAMILIES).getValue(Identifier.fromNamespaceAndPath(binding.getNamespace(),binding.getPath().substring(7))));
    }
    public static List<Identifier> moves(RegistryAccess access,TechniqueState state,Identifier binding) {
        return definition(access,binding).map(f->f.techniques().stream().filter(state.known()::contains).filter(id->Techniques.get(access,id).isPresent()).toList()).orElse(List.of());
    }
    public static boolean allowed(RegistryAccess access,TechniqueState state,Identifier binding) {
        return family(binding)?!moves(access,state,binding).isEmpty():state.known().contains(binding)&&Techniques.get(access,binding).isPresent();
    }
    public static boolean contains(RegistryAccess access,TechniqueState state,Identifier binding,Identifier move) {
        return binding.equals(move)||moves(access,state,binding).contains(move);
    }
    public static List<Identifier> available(RegistryAccess access,TechniqueState state) {
        return access.lookupOrThrow(CharacterAwakening.FAMILIES).keySet().stream().sorted().map(TechniqueBindings::binding).filter(id->allowed(access,state,id)).toList();
    }
}
