"""Reproducible 1.8 content: low-grade ecology, original UV art, combat clips and technique families."""
from pathlib import Path
import copy,json,sys,os,io
from PIL import Image
import gen_curses as art
ROOT=Path(__file__).resolve().parents[1]
DATA=ROOT/'src/main/resources/data/jjkcraft'
BASE=DATA/'jjkcraft'
ASSETS=ROOT/'src/main/resources/assets/jjkcraft'
names={}
def load(p):return json.loads(p.read_text(encoding='utf-8'))
def text(p,value):
 t=p.with_suffix(p.suffix+'.tmp');t.write_text(value,encoding='utf-8',newline='\n');os.replace(t,p)
def png(p,image):
 buffer=io.BytesIO();image.save(buffer,format='PNG');t=p.with_suffix('.tmp');t.write_bytes(buffer.getvalue());os.replace(t,p)
def save(p,data):
 p.parent.mkdir(parents=True,exist_ok=True);t=p.with_suffix(p.suffix+'.tmp');t.write_text(json.dumps(data,indent=2,ensure_ascii=False)+'\n',encoding='utf-8',newline='\n');os.replace(t,p)

# Each low-grade species has a distinct silhouette, fear theme, material palette and combat kit.
roster=[
 ('paper_tick','Paper Tick','ledger_crawler','grade_4',0.66,'#B9A98B','discarded letters','grave_chains'),
 ('gutter_grub','Gutter Grub','tithe_burrower','grade_4',0.62,'#679881','clogged drains','ground_toll'),
 ('chalk_biter','Chalk Biter','echo_mimic','grade_4',0.58,'#C2C9C5','empty classrooms','grudge_bolt'),
 ('static_mite','Static Mite','wick_moth','grade_4',0.63,'#84B9E0','electrical crackle','blood_needle'),
 ('whisper_knot','Whisper Knot','hollow_hymnal','grade_4',0.67,'#927CB6','overheard whispers','cursed_speech_dont_move'),
 ('dustling','Dustling','wick_moth','grade_4',0.50,'#B2A390','abandoned rooms','grudge_bolt'),
 ('damp_effigy','Damp Effigy','salt_lantern','grade_4',0.61,'#739DAB','wet laundry in the dark','water_lance'),
 ('porcelain_nipper','Porcelain Nipper','salt_lantern','grade_3',0.72,'#D3A5B0','cracked dolls','crimson_binding'),
 ('rustjaw_pup','Rustjaw Pup','divine_dog','grade_3',0.76,'#BA7E56','barking behind locked gates','blood_edge'),
 ('drain_skitter','Drain Skitter','ledger_crawler','grade_3',0.82,'#68A49C','movement beneath grates','water_lance'),
 ('needle_cricket','Needle Cricket','tithe_burrower','grade_3',0.70,'#A29A64','nighttime scratching','splinter_volley'),
 ('worry_doll','Worry Doll','echo_mimic','grade_3',0.78,'#B48DBB','forgotten promises','grave_chains')]
registrations=[]
for n,(key,title,source,grade,scale,color,fear,tech) in enumerate(roster):
 model,rig=getattr(art,source)();model.name=key
 tint=tuple(int(color[x:x+2],16) for x in (1,3,5))
 model.palette={kind:(tuple(int((v+tint[i])*0.45) for i,v in enumerate(base)),tint) for kind,(base,accent) in model.palette.items()}
 # An extra crest, cheek horn or tail barb changes the outline, with its own allocated UV island.
 parent=rig.get('head',rig.get('body',['root']))[0]
 model.bone('fear_crest',(0,15,0),parent,[art.c((-1-n%2,14,-3),(2+n%2,3+n%3,2),'body',['glow'])])
 save(ASSETS/'geckolib/models/entity'/f'{key}.geo.json',model.geo())
 save(ASSETS/'geckolib/models/entity'/f'{key}_lod.geo.json',model.geo())
 save(ASSETS/'geckolib/animations/entity'/f'{key}.animation.json',art.anims(key,rig))
 image=model.texture(sum(map(ord,key)));png(ASSETS/'textures/entity'/f'{key}.png',image)
 glow=Image.new('RGBA',image.size)
 for y in range(image.height):
  for x in range(image.width):
   r,g,b,a=image.getpixel((x,y))
   if a and max(r,g,b)-min(r,g,b)>50 and max(r,g,b)>110:glow.putpixel((x,y),tint+(170,))
 png(ASSETS/'textures/entity'/f'{key}_glow.png',glow)
 template='ledger_crawler' if grade=='grade_4' else 'echo_mimic'
 d=load(BASE/'curse'/f'{template}.json')
 d.update(name='curse.jjkcraft.'+key,description='curse.jjkcraft.'+key+'.desc',lore='curse.jjkcraft.'+key+'.lore',entity='jjkcraft:'+key,grade='jjkcraft:'+grade,
  model={'geo':'jjkcraft:entity/'+key,'texture':'jjkcraft:textures/entity/'+key+'.png','animation':'jjkcraft:entity/'+key,'scale':scale},
  techniques=['jjkcraft:'+tech],combat_moves=['jjkcraft:jab','jjkcraft:'+(['hook','backfist','sweep'][n%3])],
  mechanic='jjkcraft:plain',parameters={},codex={'color':color,'unlock_kills':3},loot_table='jjkcraft:entities/'+key)
 d['stats']={'health':(20 if grade=='grade_3' else 10)+n%4*2,'armor':0,'speed':0.22+(n%4)*0.018,'attack_damage':2.0+(n%3)*0.45,'ce_capacity':35+n%4*10,'follow_range':22}
 d['spawn']={'weight':9,'min_fear':10 if grade=='grade_4' else 24,'group_min':2,'group_max':4 if grade=='grade_4' else 3,'max_light':10}
 save(BASE/'curse'/f'{key}.json',d)
 save(DATA/'loot_table/entities'/f'{key}.json',load(ROOT/'src/main/generated/data/jjkcraft/loot_table/entities'/f'{template}.json'))
 names[d['name']]=title;names['entity.jjkcraft.'+key]=title;names[d['description']]=f'A small pack curse born from fear of {fear}. Watch its windup; block its light attacks and punish recovery.'
 names[d['lore']]=f'Fear of {fear} collects in neglected corners. A single one is weak; a nest of them is dangerous.'
 egg=key+'_spawn_egg';names['item.jjkcraft.'+egg]=title+' Spawn Egg'
 save(ROOT/'src/main/generated/assets/jjkcraft/items'/f'{egg}.json',{'model':{'type':'minecraft:model','model':'jjkcraft:item/'+egg}})
 save(ROOT/'src/main/generated/assets/jjkcraft/models/item'/f'{egg}.json',{'parent':'minecraft:item/generated','textures':{'layer0':'jjkcraft:item/'+egg}})
 import gen_items
 png(ASSETS/'textures/item'/f'{egg}.png',gen_items.egg(key,'#39323F',color))
 flying=rig.get('style')=='floater';registrations.append(f'        CurseEntities.{"flying" if flying else "ground"}("{key}", {0.65 if flying else 0.75}f, {0.8 if flying else 1.1}f);')
java='package com.jjkcraft.curse;\npublic final class LowGradeCurseTypes {\n    private LowGradeCurseTypes(){}\n    public static void init(){\n'+'\n'.join(registrations)+'\n    }\n}\n'
text(ROOT/'src/main/java/com/jjkcraft/curse/LowGradeCurseTypes.java',java)

# Combat clips operate on actual rig bones; crawlers and flyers use body/legs instead of human arms.
for p in (ASSETS/'geckolib/animations/entity').glob('*.animation.json'):
 geo=ASSETS/'geckolib/models/entity'/p.name.replace('.animation.json','.geo.json')
 if not geo.exists():continue
 bones=load(geo)['minecraft:geometry'][0]['bones'];ids={b['name'] for b in bones};root=next((b['name'] for b in bones if 'parent' not in b),'root')
 limbs=[b for b in ids if any(w in b for w in ['arm','claw','leg','wing'])];body=next((b for b in ['body','torso','shell','head'] if b in ids),root)
 a=load(p);clips=a.setdefault('animations',{})
 def clip(name,length,contact,rotation,lift=0):
  frames={'0':[0,0,0],str(contact*0.5):[-v*0.45 for v in rotation],str(contact):rotation,str(length):[0,0,0]}
  target=limbs[0] if limbs else body
  pose={target:{'rotation':frames},body:{'rotation':{'0':[0,0,0],str(contact):[rotation[0]*0.2,rotation[1]*0.3,0],str(length):[0,0,0]}}}
  if lift:pose[root]={'position':{'0':[0,0,0],str(contact):[0,lift,0],str(length):[0,0,0]}}
  clips[name]={'animation_length':length,'bones':pose}
 clip('jab',0.55,0.15,[-65,0,0]);clip('heavy',1.45,0.45,[80,-25,0],-1)
 clip('sweep',0.95,0.3,[0,110,65],-2);clip('uppercut',0.85,0.25,[-135,0,-15],2)
 clips['guard']={'animation_length':0.65,'bones':{b:{'rotation':[-65,15 if n%2 else -15,20 if n%2 else -20]} for n,b in enumerate(limbs[:4] or [body])}}
 save(p,a)

def move(key,title,desc,grade,cost,behavior,template='grudge_bolt',category='projectile',reach=24,cooldown=60,color='#A38CD9',health=0):
 d=load(BASE/'technique'/f'{template}.json');d.update(name='technique.jjkcraft.'+key,description='technique.jjkcraft.'+key+'.desc',grade='jjkcraft:'+grade,behavior=behavior,category=category,innate_weight=0,mastery_uses=45)
 d['cast']={'cost':cost,'cooldown':cooldown,'range':reach};d['visuals']['color']=color
 save(BASE/'technique'/f'{key}.json',d);names[d['name']]=title;names[d['description']]=desc
 materials=['copper_ingot','redstone','quartz','iron_ingot','lapis_lazuli','glowstone_dust']
 save(DATA/'recipe'/f'scroll_{key}.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':['minecraft:paper','minecraft:amethyst_shard','minecraft:'+materials[len(added)%len(materials)],'minecraft:'+('gunpowder' if 'blast' in key else 'string')], 'result':{'id':'jjkcraft:technique_scroll','components':{'jjkcraft:loaded_technique':'jjkcraft:'+key}}})
 added.append(key)
def ability(name,damage=4,duration=80,radius=5):return {'type':'jjkcraft:manga_ability','ability':name,'damage':damage,'duration':duration,'radius':radius}
def bolt(damage=5,speed=1.6,count=1,blast=0):return {'type':'jjkcraft:projectile','damage':damage,'speed':speed,'count':count,'spread':12 if count>1 else 0,'size':0.3,'lifetime':40,'blast_radius':blast}
added=[]
move('charge_transfer','Electrification: Charge Transfer','Mark the aimed enemy with electrical charge. Up to three charges prime your discharge. Gameplay adaptation.', 'grade_4',9,ability('charge_mark',3),reach=16,color='#8BDAF0')
move('lightning_discharge','Electrification: Discharge','Consume the aimed enemy\'s stored charges for an electrical strike. Requires your own mark and line of sight.','grade_2',32,ability('charge_release',9),reach=28,color='#8BDAF0')
move('return_stroke','Electrification: Return Stroke','Consume charges on all your marked nearby enemies, chaining lightning through the group.','grade_1',65,ability('charge_chain',8,radius=10),category='area',reach=10,color='#8BDAF0')
move('granite_blast','Granite Blast','Project a broad burst of concentrated cursed energy. Heavy output, adjustable reach and blast size. Gameplay adaptation.','grade_4',18,bolt(7,2,blast=1.5),reach=32,color='#D2C6F0')
move('granite_barrage','Granite Blast: Barrage','Split the discharge into several heavy blasts; customize volley geometry and gravity.','grade_2',42,bolt(5,1.7,4,1.2),color='#D2C6F0')
move('granite_cannon','Granite Blast: Full Discharge','A large, piercing concentrated discharge. Its CE price rewards high reserve builds.','grade_1',95,dict(bolt(19,1.4,1,3),pierce=True,size=0.7),category='ultimate',cooldown=170,color='#D2C6F0')
move('constellation_mark','Love Rendezvous: Star Mark','Mark an enemy with your own constellation. Later attraction and repulsion only affect your marked targets. Gameplay adaptation.','grade_4',8,ability('star_mark',1),category='restriction',reach=20,color='#E69CB9')
move('constellation_attract','Love Rendezvous: Attraction','Draw your marked enemies toward the aimed star, binding them briefly when the constellation closes.','grade_2',35,ability('star_pull',5,radius=9),category='area',reach=24,color='#E69CB9')
move('constellation_repel','Love Rendezvous: Repulsion','Repel only enemies you have marked. Marks survive until their one-minute duration expires.','grade_1',45,ability('star_repel',8,radius=8),category='area',reach=16,color='#E69CB9')
move('tooth_blast','Explosive Flesh: Tooth Shot','A small explosive projectile. This Minecraft adaptation uses CE, without permanently removing body parts.','grade_4',14,bolt(5,1.5,1,2),color='#DB8B72')
move('eye_blast','Explosive Flesh: Eye Bomb','A pair of explosive projectiles with a wider blast. Customizable spread, size and gravity.','grade_2',36,bolt(7,1.1,2,2.8),cooldown=95,color='#DB8B72')
move('flesh_detonation','Explosive Flesh: Detonation','Release a blast around your body; tune its recoil, lift and health offering for a dangerous close-range build.','grade_1',55,{'type':'jjkcraft:shockwave','damage':13,'radius':5,'launch':0.6},category='area',reach=5,cooldown=110,color='#DB8B72')
move('wind_scythe','Tool Manipulation: Wind Scythe','A broad cutting arc propelled through a tool. Gameplay adaptation of broom-driven wind attacks.','grade_4',12,{'type':'jjkcraft:arc_slash','damage':4,'arc':95,'knockback':0.45},category='strike',reach=6,color='#B1D7CE')
move('broom_flight','Tool Manipulation: Aerial Lift','Rise on a short current and slow your fall; customize duration and output.','grade_3',20,ability('flight',0,120),category='utility',reach=4,color='#B1D7CE')
move('gale_cutter','Tool Manipulation: Gale Cutter','Launch three narrow wind blades. Shape your own count, pattern and guidance.','grade_1',45,bolt(6,2.1,3),cooldown=85,color='#B1D7CE')
# Explicitly original families, built around conditional marks and stored force rather than renamed projectiles.
move('echo_brand','Echo Ledger: First Impression','Original technique. Brand the aimed target with an echo and remember your current position. Stacking echoes deepens the debt.','grade_4',9,ability('echo_mark',2),reach=18,color='#AE98E0')
move('echo_collect','Echo Ledger: Collection','Original technique. Consume your echoes on the aimed enemy for damage proportional to stored impressions.','grade_2',30,ability('echo_release',7),reach=24,color='#AE98E0')
move('echo_return','Echo Ledger: Reprise','Original technique. Return to the position recorded by First Impression, with collision and distance checks.','grade_1',40,ability('echo_return',0),category='utility',reach=24,color='#AE98E0')
move('mirror_guard','Mercury Mirror: Catch','Original technique. For four seconds, absorb part of incoming direct damage into a mirror. Domain sure-hits bypass it.','grade_4',15,ability('mirror_guard',0,80),category='passive',reach=4,color='#A9D7D7')
move('mirror_release','Mercury Mirror: Return','Original technique. Shatter stored force into nearby enemies. Requires damage absorbed by your own Catch.','grade_2',28,ability('mirror_release',5,radius=5),category='area',reach=6,color='#A9D7D7')
move('mirror_shards','Mercury Mirror: Silver Rain','Original technique. Fire a spray of brittle mirror shards with editable gravity, patterns and aiming offsets.','grade_1',44,bolt(4,1.5,6),cooldown=100,color='#A9D7D7')
move('mourning_thread','Mourning Thread: Stitch','Original technique. Bind the aimed enemy with cursed funeral thread. Longer bindings cost additional CE.','grade_4',12,{'type':'jjkcraft:bind','duration':35,'damage':3},category='restriction',reach=16,color='#BD6C9C')
move('thread_sever','Mourning Thread: Sever','Original technique. Draw a thin cutting line through the funeral thread, with adjustable width and penetration.','grade_2',29,{'type':'jjkcraft:line_cut','damage':10,'width':0.4,'pierce':0.25},category='area',reach=20,color='#BD6C9C')
move('funeral_loom','Mourning Thread: Funeral Loom','Original technique. Cast a binding field around yourself, trapping nearby enemies long enough to prepare a finisher.','grade_1',60,ability('roots',6,50,6),category='area',reach=6,cooldown=150,color='#BD6C9C')
families=[('electrification','Electrification',added[0:3]),('granite_discharge','Granite Blast',added[3:6]),('love_rendezvous','Love Rendezvous',added[6:9]),('explosive_flesh','Explosive Flesh',added[9:12]),('tool_manipulation','Tool Manipulation',added[12:15]),('echo_ledger','Echo Ledger (Original)',added[15:18]),('mercury_mirror','Mercury Mirror (Original)',added[18:21]),('mourning_thread','Mourning Thread (Original)',added[21:24])]
for key,title,moves in families:
 name='family.jjkcraft.'+key;names[name]=title;save(BASE/'innate_family'/f'{key}.json',{'name':name,'clan':'','inherited_weight':0,'random_weight':7,'techniques':['jjkcraft:'+m for m in moves],'domain':''})
# Every scroll has a distinct recipe even where two moves share the same thematic ingredients.
for n,key in enumerate(added):
 p=DATA/'recipe'/f'scroll_{key}.json';d=load(p);d['ingredients'].append('minecraft:'+['flint','feather','ink_sac','bone'][n//6]);save(p,d)
names.update({'key.jjkcraft.light_hit':'Light hit / double-tap uppercut','key.jjkcraft.heavy_hit':'Heavy hit / double-tap sweep','key.jjkcraft.bank':'Next power bank','key.jjkcraft.auto_combat':'Toggle Auto Combat'})
lang=ROOT/'src/main/generated/assets/jjkcraft/lang/en_us.json';d=load(lang);d.update(names);save(lang,d)
java='package com.jjkcraft.datagen;\nfinal class ArsenalLang {\n    static void add(net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider.TranslationBuilder b){\n'
for k,v in names.items():java+='        b.add('+json.dumps(k)+','+json.dumps(v)+');\n'
text(ROOT/'src/datagen/java/com/jjkcraft/datagen/ArsenalLang.java',java+'    }\n}\n')
import arsenal_icons
arsenal_icons.generate()
print(f'Generated {len(roster)} low-grade species, 5 combat clips per rig, {len(added)} powers, {len(families)} families and recipes.')
