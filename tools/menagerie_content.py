"""1.10 shikigami roster, fusion recipes, original UV models, rare chalice and scrolls."""
from pathlib import Path
import json,os,io,math,itertools
from PIL import ImageDraw
import gen_curses as art
import gen_textures as ink
ROOT=Path(__file__).resolve().parents[1];DATA=ROOT/'src/main/resources/data/jjkcraft';REG=DATA/'jjkcraft';A=ROOT/'src/main/resources/assets/jjkcraft';LANG=ROOT/'src/main/generated/assets/jjkcraft/lang/en_us.json'
def read(p):return json.loads(p.read_text(encoding='utf-8'))
def save(p,d):
 p.parent.mkdir(parents=True,exist_ok=True);t=p.with_suffix(p.suffix+'.writing');t.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8',newline='\n');os.replace(t,p)
def png(p,im):
 p.parent.mkdir(parents=True,exist_ok=True);b=io.BytesIO();im.save(b,format='PNG');t=p.with_suffix('.writing');t.write_bytes(b.getvalue());os.replace(t,p)
lang=read(LANG);powers=[]
def power(key,title,desc,behavior,grade='grade_3',category='summon',cost=30,cd=140,reach=0,color='#9693DA'):
 d=dict(name='technique.jjkcraft.'+key,description='technique.jjkcraft.'+key+'.desc',grade='jjkcraft:'+grade,category=category,cast=dict(cost=cost,cooldown=cd,range=reach),behavior=behavior,visuals=dict(color=color,particle='jjkcraft:ce_aura',sound='jjkcraft:technique.summon',shake=.15,hit_stop=0,trail='aura'),icon='jjkcraft:textures/gui/technique/'+key+'.png',innate_weight=0,mastery_uses=60)
 save(REG/'technique'/f'{key}.json',d);lang[d['name']]=title;lang[d['description']]=desc;powers.append(key)
 im=ink.icon_base(ink.hexrgb(color));dr=ImageDraw.Draw(im);[ink.g_shape,ink.g_chain,ink.g_star,ink.g_oath,ink.g_fan,ink.g_attract][len(powers)%6](dr,ink.hexrgb(color));dr.ellipse((44,96,84,115),outline=ink.hexrgb(color)+(255,),width=3);png(A/'textures/gui/technique'/f'{key}.png',ink.finish(im,32))
 return 'jjkcraft:'+key
def summon(entity,count=1,life=600):return dict(type='jjkcraft:summon',entity='jjkcraft:'+entity,count=count,lifetime=life)
def fusion(parts,result):return dict(type='jjkcraft:fusion',components=['jjkcraft:'+p for p in parts],result='jjkcraft:'+result,lifetime=600)
def addfamily(key,title,ids,clan='',random=4):
 p=REG/'innate_family'/f'{key}.json';d=read(p) if p.exists() else dict(name='family.jjkcraft.'+key,clan=clan,inherited_weight=0,random_weight=random,techniques=[],domain='')
 for id in ids:
  if id not in d['techniques']:d['techniques'].append(id)
 save(p,d);lang[d['name']]=title
newten=[]
for entity,title,desc,grade,cost,count in [
 ('great_serpent','Great Serpent','A coiling serpent that binds enemies it bites.','grade_3',32,1),
 ('max_elephant','Max Elephant','A heavy beast whose close water shockwaves push and slow nearby enemies.','grade_2',55,1),
 ('rabbit_escape','Rabbit Escape','Six fragile rabbits scatter around you and distract hostile mobs. They do not deal damage.','grade_4',24,6),
 ('round_deer','Round Deer','A positive-energy support beast. Shared support healing restores one HP per three seconds for 12 of your CE; healing vows apply.','grade_2',48,1),
 ('piercing_ox','Piercing Ox','Charges toward its target; a longer approach increases its next impact up to double damage.','grade_2',42,1),
 ('tiger_funeral','Tiger Funeral','An aggressive tiger with fast close-range attacks. Its gameplay interpretation is original.','grade_1',55,1),
 ('shadow_mahoraga','Eight-Handled Sword Divergent Sila Divine General Mahoraga','An owned divine general that learns damage kinds it survives. Adaptation is capped at 60 percent and never grants immunity.','special_grade',160,1)]:
 newten.append(power('ten_shadows_'+('mahoraga' if entity=='shadow_mahoraga' else entity),'Ten Shadows: '+title,desc,summon(entity,count),'grade_4' if entity=='rabbit_escape' else grade,cost=cost,cd=300 if entity=='shadow_mahoraga' else 180))
newten.append(power('divine_dog_totality','Divine Dogs: Totality','Merge two active Divine Dogs into one stronger hunter. Both component summons are consumed.',fusion(['divine_dog','divine_dog'],'divine_dog_totality'),'grade_2',cost=45,cd=240))
newten.append(power('well_unknown_abyss','The Well’s Unknown Abyss','Merge one active Nue and Toad into a flying, binding shikigami. Components are consumed.',fusion(['nue','toad'],'well_unknown_abyss'),'grade_2',cost=45,cd=220))
newten.append(power('merged_beast_agito','Nue: Totality – Merged Beast Agito','Merge active Nue, Great Serpent, Round Deer and Tiger Funeral. Agito inherits ranged lightning, binding, claws and bounded CE-paid support healing.',fusion(['nue','great_serpent','round_deer','tiger_funeral'],'agito'),'special_grade',cost=110,cd=320))
newten.append(power('chimera_fusion','Chimera Fusion','Configure two to four learned summon types in Moves → Fusion. Summon those components, then consume them into a personal chimera. Name, health, movement and lifetime can be personalized.',fusion([],'custom_chimera'),'grade_1',cost=90,cd=260))
newten.append(power('elephant_surge','Max Elephant: Water Surge','Release a close water impact that pushes enemies back.',dict(type='jjkcraft:shockwave',radius=5,damage=7,launch=.2),'grade_2','area',32,140,5))
addfamily('ten_shadows','Ten Shadows',newten,"Zen'in")
original=[]
for entity,title,desc in [('paper_heron','Paper Heron','A flying paper familiar that casts cutting feather rays.'),('lantern_moth','Lantern Moth','A hovering lantern familiar that burns hostile targets at range.'),('iron_tortoise','Iron Tortoise','A slow armored familiar. While within four blocks, it absorbs one quarter of ordinary incoming damage, up to five damage per hit, using its own health. Guard vows apply.'),('grave_hound','Grave Hound','A hunting familiar whose bites inflict short poison.')]:original.append(power('summon_'+entity,title,desc,summon(entity),'grade_4' if entity in ('paper_heron','grave_hound') else 'grade_3',cost=28,cd=170,color='#9EC2B2'))
# Put a genuine starter first in every new family.
addfamily('moonlit_menagerie','Moonlit Menagerie',original)
moon=[power('moon_dregs','Moon Dregs','A floating jellyfish shikigami whose stings poison its creator’s enemies.',summon('moon_dregs'),'grade_4',cost=28,color='#C393DB'),power('venom_cloud','Venom Cloud','Poison nearby enemies for a short time. A radius control changes the affected space.',dict(type='jjkcraft:manga_ability',ability='poison',damage=2,duration=70,radius=4),'grade_3','area',24,140,4,'#C393DB'),power('jelly_mantle','Jelly Mantle','A brief protective membrane that grants bounded resistance, never damage immunity.',dict(type='jjkcraft:manga_ability',ability='armor',duration=120,radius=1),'grade_2','utility',30,180,0,'#C393DB'),power('venom_tether','Venom Tether','Pin an aimed target with a binding tendril and deal light contact damage.',dict(type='jjkcraft:bind',duration=45,damage=4),'grade_4','restriction',20,120,16,'#C393DB')]
addfamily('moon_dregs','Moon Dregs',moon,random=3)
power('summon_garuda','Garuda','A flying, coiling shikigami with charge-enhanced impacts and binding strikes.',summon('garuda'),'grade_1',cost=65,cd=240)
f=REG/'innate_family/star_rage.json';d=read(f)
if 'jjkcraft:summon_garuda' not in d['techniques']:d['techniques'].append('jjkcraft:summon_garuda')
save(f,d)
power('summon_rika','Rika','A powerful guardian shikigami. Nearby guard interception uses Rika’s own health and cannot negate all damage.',summon('rika'),'special_grade',cost=100,cd=300)
# Six ingredients distinguish these scrolls from all older five/six-ingredient recipes.
materials=['iron_ingot','copper_ingot','gold_ingot','lapis_lazuli','redstone','quartz','flint','bone','feather','string','clay_ball','slime_ball']
for n,key in enumerate(powers):
 pair=list(itertools.combinations(materials,2))[n]
 save(DATA/'recipe'/f'scroll_{key}.json',dict(type='minecraft:crafting_shapeless',category='misc',ingredients=['minecraft:paper','minecraft:amethyst_shard','minecraft:ink_sac','minecraft:glowstone_dust']+['minecraft:'+v for v in pair],result=dict(id='jjkcraft:technique_scroll',components={'jjkcraft:loaded_technique':'jjkcraft:'+key})))
save(DATA/'recipe/divine_chalice.json',dict(type='minecraft:crafting_shaped',category='misc',pattern=['SNS','ECE','TNT'],key={'S':'minecraft:netherite_ingot','N':'minecraft:nether_star','E':'minecraft:echo_shard','C':'minecraft:dragon_breath','T':'jjkcraft:sovereign_tooth'},result=dict(id='jjkcraft:divine_chalice',count=1)))
# Very rare, player-kill-only Calamity/Special drops. Expensive crafting is the deterministic route.
for key,chance in [('ten_mouth_sovereign',.01),('culling_warden',.0025),('paper_tide_gravemind',.0025),('unfinished_ferryman',.0025)]:
 p=ROOT/'src/main/generated/data/jjkcraft/loot_table/entities'/f'{key}.json';d=read(p);d['pools']=[pool for pool in d['pools'] if not any(e.get('name')=='jjkcraft:divine_chalice' for e in pool.get('entries',[]))];d['pools'].append(dict(rolls=1,condition=dict(type='minecraft:all_of',terms=[dict(type='minecraft:killed_by_player'),dict(type='minecraft:random_chance',chance=chance)]),entries=[dict(type='minecraft:item',name='jjkcraft:divine_chalice')]))
 # 26.3 loot codec uses singular condition/modifier keys.
 save(p,d)
# Original code-native cup texture.
im=ink.icon_base((216,195,110));dr=ImageDraw.Draw(im);dr.polygon([(35,24),(93,24),(85,64),(66,79),(45,64)],fill=(229,205,118,255));dr.rectangle((59,72,69,103),fill=(229,205,118,255));dr.rectangle((42,102,86,110),fill=(239,218,146,255));dr.ellipse((40,25,88,38),fill=(79,49,110,255));dr.line((43,42,50,62,63,72),fill=(255,243,191,255),width=3);png(A/'textures/item/divine_chalice.png',ink.finish(im,32));save(A/'items/divine_chalice.json',dict(model=dict(type='minecraft:model',model='jjkcraft:item/divine_chalice')));save(A/'models/item/divine_chalice.json',dict(parent='minecraft:item/generated',textures=dict(layer0='jjkcraft:item/divine_chalice')))
lang['item.jjkcraft.divine_chalice']='Divine Chalice'
# Every beast gets its own silhouette, UV atlas, texture and animation file.
def model(key):
 pal={'body':((78,75,106),(173,164,218)),'bone':((206,203,190),(247,233,191)),'mark':((25,24,39),(124,104,163))}
 m=art.Model(key,pal);rig=dict(root='root',body=['body'],head=['head'],arms=[],legs=[],extra=[],style='walker');m.bone('root',(0,0,0))
 def bone(name,pivot,parent='root',boxes=(),role=None,rotation=None):m.bone(name,pivot,parent,boxes,rotation);(rig[role].append(name) if role else None)
 c=art.c
 if key in ('great_serpent','garuda'):
  for n in range(5):bone('coil'+str(n),(0,4,8-n*5),boxes=[c((-3,1,5-n*5),(6,6,7))],role='extra')
  bone('body',(0,4,0),boxes=[]);bone('head',(0,7,-14),boxes=[c((-4,4,-19),(8,7,9),'body',['eyes','mouth']),c((-3,3,-18),(1,4,1),'bone'),c((2,3,-18),(1,4,1),'bone')]);rig['style']='floater' if key=='garuda' else 'walker'
 elif key in ('shadow_mahoraga','agito','rika'):
  m.palette['body']=((189,186,175),(246,232,213));bone('body',(0,20,0),boxes=[c((-6,12,-4),(12,18,8),'body',['ribs'])]);bone('head',(0,30,0),'body',[c((-4,29,-4),(8,9,8),'body',['eyes','mouth'])]);
  for side in (-1,1):
   bone('arm'+str(side),(side*7,26,0),'body',[c((side*7-2,9,-2),(4,18,5),'body')],role='arms');bone('leg'+str(side),(side*3,13,0),boxes=[c((side*3-2,0,-2),(4,14,5))],role='legs')
  if key=='shadow_mahoraga':
   bone('wheel',(0,42,0),boxes=[],role='extra')
   for n in range(8):
    angle=n*math.pi/4;px,pz=math.cos(angle)*9,math.sin(angle)*9;bone('spoke'+str(n),(0,42,0),'wheel',[c((px-1,41,pz-1),(3,2,3),'bone')])
  elif key=='agito':
   for side in (-1,1):bone('antler'+str(side),(side*4,34,0),'head',[c((side*5-1,34,0),(2,10,2),'bone'),c((side*7-1,39,0),(4,2,2),'bone')],role='extra')
   bone('tail',(0,12,4),'body',[c((-2,10,4),(4,4,13))],role='extra')
  else:bone('jaw',(0,29,-4),'head',[c((-6,25,-6),(12,5,4),'bone',['mouth'])],role='extra')
 elif key=='moon_dregs':
  m.palette['body']=((144,106,175),(231,189,248));bone('body',(0,16,0),boxes=[c((-6,13,-6),(12,7,12),'body',['many_eyes'])]);bone('head',(0,17,-4),'body',[])
  for n in range(6):bone('tendril'+str(n),(math.cos(n)*4,13,math.sin(n)*4),'body',[c((math.cos(n)*4-1,1,math.sin(n)*4-1),(2,13,2))],role='extra')
  rig['style']='floater'
 elif key in ('paper_heron','lantern_moth','well_unknown_abyss'):
  if key=='well_unknown_abyss':
   m.palette['body']=((71,106,88),(164,211,174));bone('body',(0,6,0),boxes=[c((-5,2,-4),(10,6,9))]);bone('head',(0,7,-3),'body',[c((-4,5,-7),(8,4,5),'body',['eyes','mouth'])])
   for side in (-1,1):bone('haunch'+str(side),(side*4,3,1),'body',[c((side*4-2,1,0),(4,3,6))],role='legs')
  elif key=='lantern_moth':
   m.palette['body']=((113,83,54),(247,198,101));bone('body',(0,10,0),boxes=[c((-2,5,-2),(4,10,4))]);bone('head',(0,15,0),'body',[c((-2,13,-3),(4,5,4),'body',['eyes'])])
   for side in (-1,1):bone('antenna'+str(side),(side*2,17,-1),'head',[c((side*2,17,-1),(1,5,1),'bone')],role='extra')
  else:
   m.palette['body']=((184,192,180),(237,232,209));bone('body',(0,10,0),boxes=[c((-2,5,-2),(4,11,5))]);bone('head',(0,16,-2),'body',[c((-2,14,-4),(4,5,4),'body',['eyes']),c((-1,14,-11),(2,2,8),'bone')])
   for side in (-1,1):bone('leg'+str(side),(side*2,6,0),'body',[c((side*2,0,0),(1,6,1),'bone')],role='legs')
  for side in (-1,1):bone('wing'+str(side),(side*3,10,0),'body',[c((side*8-5,5,-1),(10,12,2),'bone') if key=='lantern_moth' else c((side*7-5,8,0),(10,1,6),'bone')],role='arms')
  rig['style']='floater'

 else:
  large=key=='max_elephant';small=key=='rabbit_escape';turtle=key=='iron_tortoise';deer=key=='round_deer';ox=key=='piercing_ox';tiger=key=='tiger_funeral';chimera=key=='custom_chimera'
  bw,bh,bd=(18,14,20) if large else (6,6,8) if small else (12,7,16) if turtle else (10,9,16)
  leg=10 if large else 3 if small or turtle else 12 if deer else 7
  if tiger:m.palette['body']=((154,121,85),(228,180,108))
  if deer or small:m.palette['body']=((185,191,177),(230,239,219))
  bone('body',(0,leg+bh/2,0),boxes=[c((-bw/2,leg,-bd/2),(bw,bh,bd),'body',['ribs'])]);hw=10 if large else 5 if small else 6
  bone('head',(0,leg+bh-2,-bd/2),'body',[c((-hw/2,leg+bh-5,-bd/2-hw),(hw,hw,hw),'body',['eyes','mouth'])]);
  for n,(xx,zz) in enumerate([(-bw/3,-bd/3),(bw/3,-bd/3),(-bw/3,bd/3),(bw/3,bd/3)]):bone('leg'+str(n),(xx,leg,zz),boxes=[c((xx-1,0,zz-1),(2 if not large else 4,leg+1,3))],role='legs')
  if large:
   bone('trunk',(0,leg+bh-3,-bd/2-hw),'head',[c((-2,4,-bd/2-hw-3),(4,leg+bh-6,4))],role='extra')
   for side in (-1,1):
    bone('ear'+str(side),(side*5,leg+bh-2,-bd/2),'head',[c((side*7-2,leg+bh-9,-bd/2),(4,13,2))],role='extra')
    bone('tusk'+str(side),(side*4,leg+bh-5,-bd/2-hw),'head',[c((side*4-1,leg+bh-8,-bd/2-hw-5),(2,2,7),'bone')])
  if small:
   for side in (-1,1):bone('ear'+str(side),(side*2,leg+bh,-bd/2),'head',[c((side*2,leg+bh,-bd/2),(1,8,2),'bone')],role='extra')
  if deer or ox or chimera:
   for side in (-1,1):bone('horn'+str(side),(side*3,leg+bh,-bd/2),'head',[c((side*3,leg+bh,-bd/2),(1,8 if deer else 5,2),'bone')],role='extra')
  if chimera:
   for side in (-1,1):bone('wing'+str(side),(side*4,leg+bh,0),'body',[c((side*8-5,leg+bh,0),(10,2,7),'bone')],role='arms')
   bone('tail',(0,leg+3,bd/2),'body',[c((-2,leg+1,bd/2),(4,3,14))],role='extra')
  if tiger or key in ('divine_dog_totality','grave_hound'):bone('muzzle',(0,leg+bh-3,-bd/2-hw),'head',[c((-2,leg+bh-5,-bd/2-hw-3),(4,3,5),'bone')])
 return m,rig
keys=['great_serpent','max_elephant','rabbit_escape','round_deer','piercing_ox','tiger_funeral','shadow_mahoraga','divine_dog_totality','well_unknown_abyss','agito','custom_chimera','moon_dregs','garuda','rika','paper_heron','lantern_moth','iron_tortoise','grave_hound']
for key in keys:
 m,rig=model(key);save(A/'geckolib/models/entity'/f'{key}.geo.json',m.geo());save(A/'geckolib/animations/entity'/f'{key}.animation.json',art.anims(key,rig));png(A/'textures/entity'/f'{key}.png',m.texture(sum(map(ord,key))));lang['entity.jjkcraft.'+key]=key.replace('_',' ').title()
save(LANG,lang);save(ROOT/'build/menagerie-inventory.json',dict(new_powers=powers,shikigami=keys))
print('Generated',len(powers),'powers and',len(keys),'owned shikigami models.')
