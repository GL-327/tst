package com.jjkcraft.client.design;
import java.util.*;
import java.util.zip.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
/** Exports only bounded original model data; it cannot alter recipes, entities or server mechanics. */
public final class PropPack{
    private PropPack(){}
    private static void json(ZipOutputStream zip,String name,Object data)throws Exception{zip.putNextEntry(new ZipEntry(name));zip.write(new GsonBuilder().create().toJson(data).getBytes(StandardCharsets.UTF_8));zip.closeEntry();}
    public static Path export()throws Exception{
        var m=Minecraft.getInstance();Path out=m.getResourcePackDirectory().resolve("jjkcraft-training-design.zip");Files.createDirectories(out.getParent());Path temp=out.resolveSibling("jjkcraft-training-design.tmp");
        try(var zip=new ZipOutputStream(Files.newOutputStream(temp))){
            var fmt=net.minecraft.SharedConstants.getCurrentVersion().packVersion(net.minecraft.server.packs.PackType.CLIENT_RESOURCES);
            json(zip,"pack.mcmeta",Map.of("pack",Map.of("description","Your Jujutsu Craft training props","min_format",List.of(fmt.major(),fmt.minor()),"max_format",List.of(fmt.major(),fmt.minor()))));
            for(String name:List.of("training_dummy","training_mat","focus_stone","balance_beam")){
                List<Map<String,Object>> boxes=new ArrayList<>();for(var c:UiDesign.prop(name)){
                    Map<String,Object> faces=new LinkedHashMap<>();for(String f:List.of("north","south","east","west","up","down"))faces.put(f,Map.of("uv",List.of(c.material()*16,0),"uv_size",List.of(16,16)));
                    boxes.add(Map.of("origin",List.of(c.x(),c.y(),c.z()),"size",List.of(c.w(),c.h(),c.d()),"uv",faces));
                }
                if(name.equals("training_dummy"))json(zip,"assets/jjkcraft/geckolib/models/entity/training_dummy.geo.json",Map.of("format_version","1.12.0","minecraft:geometry",List.of(Map.of("description",Map.of("identifier","geometry.training_dummy","texture_width",128,"texture_height",16,"visible_bounds_width",4,"visible_bounds_height",4,"visible_bounds_offset",List.of(0,1,0)),"bones",List.of(Map.of("name","root","pivot",List.of(0,0,0)),Map.of("name","body","parent","root","pivot",List.of(0,12,0),"cubes",boxes))))));
                else{
                    var elements=new ArrayList<>();for(var c:UiDesign.prop(name)){Map<String,Object> faces=new LinkedHashMap<>();for(String f:List.of("north","south","east","west","up","down"))faces.put(f,Map.of("uv",List.of(0,0,16,16),"texture","#m"+c.material()));elements.add(Map.of("from",List.of(c.x(),c.y(),c.z()),"to",List.of(c.x()+c.w(),c.y()+c.h(),c.z()+c.d()),"faces",faces));}
                    var textures=new LinkedHashMap<>();textures.put("particle","jjkcraft:block/training_wood");String[] mats={"training_straw","training_wood","training_cloth","training_red","training_stone","training_jade","training_dark","training_stone"};for(int i=0;i<8;i++)textures.put("m"+i,"jjkcraft:block/"+mats[i]);json(zip,"assets/jjkcraft/models/block/"+name+".json",Map.of("textures",textures,"elements",elements));
                }
            }
            json(zip,"jjkcraft-design.json",UiDesign.document());
        }Files.move(temp,out,StandardCopyOption.REPLACE_EXISTING);return out;
    }
    public static void apply()throws Exception{
        export();var m=Minecraft.getInstance();var repo=m.getResourcePackRepository();repo.reload();var ids=new ArrayList<>(repo.getSelectedIds());if(!ids.contains("file/jjkcraft-training-design.zip"))ids.add("file/jjkcraft-training-design.zip");repo.setSelected(ids);m.options.resourcePacks.clear();m.options.resourcePacks.addAll(ids.stream().filter(s->!s.equals("vanilla")&&!s.startsWith("fabric")).toList());m.options.save();m.reloadResourcePacks();
    }
}
