"""Data-native Modulo powers, ordinary craftable scrolls and localization; no story prerequisites."""
from pathlib import Path
import json,copy
ROOT=Path(__file__).resolve().parents[1]
DATA=ROOT/'src/main/resources/data/jjkcraft'
TECH=DATA/'jjkcraft/technique'
names={}
def move(key,name,desc,template,grade,cost,behavior,category=None,range=16,cooldown=50,color='#76DDD0'):
    d=copy.deepcopy(json.loads((TECH/(template+'.json')).read_text()))
    d.update(name='technique.jjkcraft.'+key,description='technique.jjkcraft.'+key+'.desc',grade='jjkcraft:'+grade,behavior=behavior,innate_weight=0,mastery_uses=50)
    d['cast']={'cost':cost,'cooldown':cooldown,'range':range};d['visuals']['color']=color
    if category:d['category']=category
    (TECH/(key+'.json')).write_text(json.dumps(d,indent=2)+'\n')
    names[d['name']]=name;names[d['description']]=desc
move('harmony_step','Harmony: Step','A short, safe spatial step. Express it as a forward, backward, sideways or vertical movement.','ink_step','grade_4',8,{'type':'jjkcraft:dash','distance':6,'invulnerable_ticks':3},'utility',range=6,cooldown=35)
move('chaos_twist','Chaos: Twist','Disrupt nearby opponents with a spatial repulsion. Tune its reach, force, impact lift and commitment.','sky_manipulation','grade_3',22,{'type':'jjkcraft:manga_ability','ability':'repel','damage':4,'duration':40,'radius':4},'area',range=8,color='#CD76E8')
move('harmony_field','Harmony: Field','Shape a sustained attracting field. Personalize its center, pulse rhythm, force and edge falloff.','lapse_blue','grade_2',40,{'type':'jjkcraft:field','strength':0.6,'radius':5,'duration':100,'damage_per_second':2,'burst_damage':12,'burst_radius':4},'area',range=16,cooldown=120)
move('chaos_fracture','Chaos: Fracture','A narrow spatial fracture along your aim. Customize its width, reach, output and piercing.','dismantle','grade_1',60,{'type':'jjkcraft:line_cut','damage':13,'width':0.5,'pierce':0.4},'area',range=20,cooldown=100,color='#CD76E8')
move('light_ray','Light: Ray','Fire a bright, fast bolt. Create your own gravity, size, volley pattern and guidance.','grudge_bolt','grade_4',10,{'type':'jjkcraft:projectile','damage':4,'speed':2.2,'size':0.2,'lifetime':28},'projectile',range=40,cooldown=25,color='#FFF0BF')
move('darkness_field','Darkness: Field','Gather opponents in a dark pressure field. Choose placement, timing, output and target class.','lapse_blue','grade_2',45,{'type':'jjkcraft:field','strength':0.85,'radius':5,'duration':100,'damage_per_second':3,'burst_damage':15,'burst_radius':4},'area',range=16,cooldown=120,color='#7C68BF')
move('twin_radiance','Light: Twin Radiance','Release a spread of luminous projectiles. Change its pattern, count, gravity and steering.','grudge_bolt','grade_1',65,{'type':'jjkcraft:projectile','damage':5,'speed':1.8,'size':0.25,'lifetime':35,'count':4,'spread':18},'projectile',range=40,cooldown=100,color='#FFF0BF')
move('darkness_nova','Darkness: Nova','Release a close-range pressure wave. Shape its area, output, impact lift and recoil.','volcanic_burst','special_grade',130,{'type':'jjkcraft:shockwave','radius':8,'damage':17,'launch':0.55},'ultimate',range=8,cooldown=220,color='#7C68BF')
for key,title,moves in [('modulo_harmony_chaos','Modulo: Harmony / Chaos',['harmony_step','chaos_twist','harmony_field','chaos_fracture']),('modulo_light_darkness','Modulo: Light / Darkness',['light_ray','darkness_field','twin_radiance','darkness_nova'])]:
    name='family.jjkcraft.'+key;names[name]=title
    (DATA/'jjkcraft/innate_family'/(key+'.json')).write_text(json.dumps({'name':name,'clan':'','inherited_weight':0,'random_weight':8,'techniques':['jjkcraft:'+m for m in moves],'domain':''},indent=2)+'\n')
materials=['ender_pearl','echo_shard','prismarine_shard','obsidian','glowstone_dust','crying_obsidian','blaze_powder','wither_skeleton_skull']
for key,material in zip(['harmony_step','chaos_twist','harmony_field','chaos_fracture','light_ray','darkness_field','twin_radiance','darkness_nova'],materials):
    recipe={'type':'minecraft:crafting_shapeless','category':'misc','ingredients':['minecraft:paper','minecraft:amethyst_shard','minecraft:'+material],
        'result':{'id':'jjkcraft:technique_scroll','count':1,'components':{'jjkcraft:loaded_technique':'jjkcraft:'+key}}}
    path=DATA/'recipe'/('scroll_'+key+'.json');path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(recipe,indent=2)+'\n')
names['cast_failure.jjkcraft.not_enough_health']='Not enough health for this expression'
lang=ROOT/'src/main/generated/assets/jjkcraft/lang/en_us.json';content=json.loads(lang.read_text());content.update(names);lang.write_text(json.dumps(content,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
java='package com.jjkcraft.datagen;\nfinal class SandboxLang {\n    static void add(net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider.TranslationBuilder b){\n'
for k,v in names.items():java+='        b.add('+json.dumps(k)+','+json.dumps(v)+');\n'
(ROOT/'src/datagen/java/com/jjkcraft/datagen/SandboxLang.java').write_text(java+'    }\n}\n')
print('Added eight sandbox powers, two families, eight craftable scrolls and localization.')
