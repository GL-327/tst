"""Family move sets and original shaped-soul geometry; safe to rerun without duplicating moves."""
from pathlib import Path
import json,os,io
from PIL import ImageDraw
import gen_curses as art
import gen_textures as ink
ROOT=Path(__file__).resolve().parents[1];DATA=ROOT/'src/main/resources/data/jjkcraft/jjkcraft';ASSETS=ROOT/'src/main/resources/assets/jjkcraft'
def load(p):return json.loads(p.read_text(encoding='utf-8'))
def save(p,d):
 p.parent.mkdir(parents=True,exist_ok=True);t=p.with_suffix(p.suffix+'.writing');t.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8',newline='\n');os.replace(t,p)
def png(p,im):
 b=io.BytesIO();im.save(b,format='PNG');t=p.with_suffix('.writing');t.write_bytes(b.getvalue());os.replace(t,p)
def ability(name,damage=0,duration=80,radius=4):return dict(type='jjkcraft:manga_ability',ability=name,damage=damage,duration=duration,radius=radius)
def bolt(damage=7,count=1,blast=0,speed=1.5,fire=0):return dict(type='jjkcraft:projectile',damage=damage,count=count,speed=speed,spread=18 if count>1 else 0,size=.35,lifetime=50,blast_radius=blast,burn_seconds=fire)
def field(damage=4,radius=4,duration=100,pull=0):return dict(type='jjkcraft:field',strength=pull,radius=radius,duration=duration,damage_per_second=damage,burst_damage=0,burst_radius=0)
def summon(count):return dict(type='jjkcraft:summon',entity='jjkcraft:transfigured_soul',count=count,lifetime=600)
MOVES={}
def add(f,key,title,desc,b,grade='grade_3',category='utility',cost=24,cd=140,reach=16):MOVES.setdefault(f,[]).append((key,title,desc,b,grade,category,cost,cd,reach))
add('idle_transfiguration','transfigured_soul','Transfigured Soul','Shape an independent soul servant that follows you and fights your enemies. Recast to add another, up to six living souls. Tune vitality, mobility, count and lifetime.',summon(1),'grade_4','summon',28,100,0)
add('idle_transfiguration','soul_multiplicity','Soul Multiplicity','Create three shaped souls at once. They share a six-soul ownership cap with Transfigured Soul.',summon(3),'grade_2','summon',65,240,0)
add('idle_transfiguration','soul_recall','Soul Recall','Recall your living shaped souls beside you without replacing them.',ability('soul_recall'),cost=8,cd=45)
add('idle_transfiguration','soul_blades','Body Repel: Blades','Reshape your arms into a striking form: gain strength and faster mining for a limited time.',ability('soul_blades',duration=180),'grade_4',cost=20)
add('idle_transfiguration','soul_touch','Soul Distortion','Touch an aimed enemy to deal soul-piercing damage, shrink their body and bind their movement.',ability('soul_touch',8,60),'grade_2','strike',38,180,5)
add('idle_transfiguration','soul_body','Instant Spirit Body','Harden and enlarge your own body, gaining resistance. Does not grant invulnerability.',ability('soul_body',duration=240),'grade_1',cost=55,cd=320)
add('idle_transfiguration','soul_detonation','Soul Multiplicity: Rupture','Consume your active shaped souls in separate nearby bursts. Each explosion damages enemies around that soul.',ability('soul_detonate',9,1,3),'special','ultimate',70,300)
add('idle_transfiguration','soul_dash','Transfigured Lunge','Reshape your legs into a short collision-checked evasive step.',dict(type='jjkcraft:dash',distance=7,invulnerable_ticks=5),'grade_4',cost=16,cd=65,reach=7)
add('projection','frame_rush','Frame Rush','Accelerate along a short fixed path. Collisions stop the movement; tune distance and step direction.',dict(type='jjkcraft:dash',distance=8,invulnerable_ticks=3),'grade_4',cost=18,cd=65,reach=8)
add('projection','frame_lock','Frame Lock','Touch an aimed enemy and trap them in a frozen animation frame.',ability('frame_lock',4,30),'grade_3','restriction',25,150,5)
add('projection','projection_overclock','Twenty-Four Frame Overclock','Temporarily accelerate your movement and mining speed; the high CE cost pays for sustained speed.',ability('projection_overclock',duration=160),'grade_1',cost=48,cd=280)
add('projection','frame_crash','Frame Crash','Release a forward fan of compressed frame impacts.',bolt(5,5),'grade_2','projectile',35,170,22)
add('ratio','ratio_expose','Seven-to-Three: Exposure','Mark an aimed enemy with weakness, lowering their ordinary attack damage.',ability('ratio_expose',3,140),'grade_4','restriction',18,100,14)
add('ratio','ratio_collapse','Collapse','Exploit nearby structural weak points with a close-range piercing impact. Leaves world blocks intact.',ability('ratio_collapse',12,1,5),'grade_2','area',38,200,5)
add('ratio','ratio_overtime','Overtime','Trade sustained hunger for a short strength surge. Normal attack vows still apply.',ability('ratio_overtime',duration=220),'grade_1',cost=42,cd=360)
add('straw_doll','straw_nail_volley','Nail Volley','Fire a fan of three CE nails. Personalize count, spread, size, steering and trajectory.',bolt(4,3),'grade_4','projectile',18,70,26)
add('straw_doll','straw_hairpin','Hairpin','Detonate a close ring of cursed nails to damage and push enemies away.',ability('straw_hairpin',9,1,5),'grade_3','area',28,150,5)
add('straw_doll','straw_stitch','Soul Stitch','Pin an aimed target in place with cursed threads.',ability('straw_stitch',3,70),'grade_4','restriction',20,110,16)
add('straw_doll','straw_effigy','Extended Effigy','Create a linked straw doll with six soul strikes and an extended lifetime.',dict(type='jjkcraft:straw_doll',damage=7,nails=6,duration=1000),'grade_1',cost=48,cd=280,reach=22)
add('shrine','shrine_furnace','Furnace','Launch a large flame-bearing explosive arrow. Its contact explosion costs substantially more CE than Dismantle.',bolt(19,1,4,1.1,4),'grade_1','projectile',65,260,32)
add('shrine','shrine_web','Spiderweb Cleave','Cut a close circular zone with a piercing pressure burst.',ability('ratio_collapse',10,1,4),'grade_2','area',34,170,4)
add('shrine','shrine_cage','Dismantle: Cage','Place a stationary lattice of sustained cuts at the aimed point.',field(6,4,100),'special','area',60,260,18)
add('boogie_woogie','swap_feint','Clap Feint','Make a short evasive step to change your angle before a swap.',dict(type='jjkcraft:dash',distance=5,invulnerable_ticks=4),'grade_4',cost=12,cd=55,reach=5)
add('boogie_woogie','swap_recall','Companion Swap','Exchange positions with your nearest owned shikigami. Both destinations must be unobstructed.',ability('swap_recall'),'grade_2',cost=22,cd=100,reach=24)
add('brain_transplant','brain_recover','Host Restoration','Spend CE to repair your current host body and gain brief regeneration. Forsake RCT prevents this healing.',ability('brain_recover',4,100),'grade_2','healing',36,220)
add('curse_manipulation','curse_recall','Spirit Recall','Return your nearby controlled spirits to storage, up to the eight-spirit stock limit.',ability('curse_recall'),'grade_4',cost=8,cd=50)
add('curse_manipulation','curse_command','Spirit Command','Order your released spirits to focus the aimed enemy. Commands do not conjure additional spirits.',ability('curse_command'),'grade_4',cost=6,cd=35,reach=28)
add('disaster_tides','tide_prison','Water Prison','Bind an aimed enemy inside compressed water and deal contact damage.',ability('tide_prison',5,65),'grade_4','restriction',22,130,18)
add('disaster_tides','tidal_guard','Tidal Mantle','Wrap yourself in a defensive mantle, granting resistance and temporary absorption.',ability('tidal_guard',duration=180),'grade_2',cost=32,cd=240)
add('disaster_flames','flame_vents','Volcanic Mantle','Harden your exterior with a heated mantle, granting resistance and temporary absorption.',ability('flame_vents',duration=160),'grade_2',cost=32,cd=230)
add('disaster_plants','flower_drain','Root Siphon','Bind nearby enemies and siphon a small amount of their actual stored CE. Only half of drained CE returns to you.',ability('flower_drain',14,50,5),'grade_2','restriction',24,200,5)
add('blood_manipulation','blood_condense','Convergence','Concentrate blood into a brief strength state. Initial weakness makes the preparation a commitment.',ability('blood_condense',duration=160),'grade_3',cost=22,cd=180)
add('ice_formation','ice_armor','Frost Shell','Encase your body in protective ice, granting resistance and temporary absorption.',ability('ice_armor',duration=180),'grade_2',cost=34,cd=240)
add('construction','construction_blades','Constructed Arsenal','Create and launch a wide fan of cursed blades.',bolt(3,5),'grade_3','projectile',35,150,26)
add('sky_manipulation','sky_counter','Folded Sky Mantle','Fold a layer of sky around your body for temporary resistance and absorption.',ability('sky_counter',duration=140),'grade_2',cost=30,cd=220)
add('star_rage','mass_anchor','Mass Anchor','Increase your effective mass: strong resistance at the cost of slower movement.',ability('mass_anchor',duration=160),'grade_3',cost=28,cd=200)
add('star_rage','garuda_orbit','Garuda: Orbit','Establish a close gravitational orbit that pulls and damages enemies at the aimed location.',field(4,4,120,.08),'grade_1','area',44,240,10)
add('cursed_speech','cursed_speech_sleep','Cursed Speech: Sleep','Command nearby enemies to freeze briefly. Speech strain is still paid.',dict(type='jjkcraft:cursed_speech',command='stop',radius=6,duration=60,damage=0,strain=2),'grade_2','restriction',28,180,6)
add('ten_shadows','shadow_escape','Shadow Escape','Slip through a short step and briefly hide your form.',ability('shadow_escape',duration=60),'grade_4',cost=18,cd=85,reach=6)
add('ten_shadows','shadow_recall','Return to Shadows','Dismiss your active shikigami safely. This does not refund their summoning cost.',ability('shadow_recall'),'grade_4',cost=5,cd=35)
add('ten_shadows','shadow_siphon','Shadow Grasp','Bind nearby enemies and siphon a small amount of their stored CE through the shadow.',ability('shadow_siphon',10,45,4),'grade_2','restriction',25,170,4)
add('limitless','blue_orbit','Lapse: Orbital Blue','Place a sustained blue attraction field with a smaller damage profile than maximum output.',field(3,4,140,.11),'grade_2','area',35,200,20)
for f,key,title,desc,b,cat in [
 ('echo_ledger','echo_silence','Closed Account','Stagger nearby enemies briefly with a pulse of collected echoes.',ability('echo_silence',4,12,4),'area'),
 ('electrification','electric_armor','Charged Body','Run controlled current through your body for speed and a small strength bonus.',ability('electric_armor',duration=160),'utility'),
 ('granite_discharge','granite_charge','Granite Compression','Compress your output into a defensive state: high resistance with slower movement.',ability('granite_charge',duration=140),'utility'),
 ('love_rendezvous','love_orbit','Constellation Mantle','Surround your body with a defensive constellation for resistance and absorption.',ability('love_orbit',duration=180),'utility'),
 ('mercury_mirror','mirror_step','Mercury Slip','Take a short evasive step and briefly harden your mirrored surface.',ability('mirror_step',duration=12),'utility'),
 ('mourning_thread','thread_snare','Funeral Knot','Tie an aimed enemy into a stationary knot and deal thread damage.',ability('thread_snare',5,70),'restriction'),
 ('tool_manipulation','tool_recall','Broom Recovery','Kick your broom upward and descend safely with slow falling.',ability('tool_recall',duration=160),'utility'),
 ('explosive_flesh','flesh_regrow','Regrow Ammunition','Spend CE to restore your body and gain brief regeneration. Healing vows apply.',ability('flesh_regrow',3,100),'healing'),
 ('modulo_harmony_chaos','harmony_restore','Harmonic Restoration','Restore health with a controlled harmonic pulse. Healing vows apply.',ability('harmony_restore',4,100),'healing'),
 ('modulo_light_darkness','radiant_shards','Radiant Shards','Scatter five small lances of light with independent projectile tuning.',bolt(4,5),'projectile')]:add(f,key,title,desc,b,'grade_2',cat,30,170,18)
lang=load(ROOT/'src/main/generated/assets/jjkcraft/lang/en_us.json');new=[]
glyphs=[ink.g_shape,ink.g_strike,ink.g_chain,ink.g_step,ink.g_fan,ink.g_attract,ink.g_doll,ink.g_star]
for f,entries in MOVES.items():
 fp=DATA/'innate_family'/f'{f}.json';family=load(fp);starter=load(DATA/'technique'/f"{family['techniques'][0].split(':')[-1]}.json");color=starter['visuals']['color']
 for n,(key,title,desc,b,grade,category,cost,cd,reach) in enumerate(entries):
  d=dict(name='technique.jjkcraft.'+key,description='technique.jjkcraft.'+key+'.desc',category=category,grade='jjkcraft:'+('special_grade' if grade=='special' else grade),cast=dict(cost=cost,cooldown=cd,range=reach),behavior=b,visuals=dict(color=color,particle='jjkcraft:ce_aura',sound='jjkcraft:technique.summon' if category=='summon' else 'jjkcraft:technique.cast',shake=.15,hit_stop=0,trail='orb' if category=='projectile' else 'aura'),icon='jjkcraft:textures/gui/technique/'+key+'.png',innate_weight=0,mastery_uses=45)
  save(DATA/'technique'/f'{key}.json',d);id='jjkcraft:'+key
  import itertools
  materials=['iron_ingot','copper_ingot','gold_ingot','lapis_lazuli','redstone','quartz','flint','bone','feather','string','clay_ball','slime_ball','blaze_powder','prismarine_shard','coal']
  pair=list(itertools.combinations(materials,2))[len(new)]
  recipe=dict(type='minecraft:crafting_shapeless',category='misc',ingredients=['minecraft:paper','minecraft:paper','minecraft:amethyst_shard','minecraft:ink_sac']+['minecraft:'+m for m in pair],result=dict(id='jjkcraft:technique_scroll',components={'jjkcraft:loaded_technique':id}))
  save(ROOT/'src/main/resources/data/jjkcraft/recipe'/f'scroll_{key}.json',recipe)

  if id not in family['techniques']:family['techniques'].append(id)
  lang[d['name']]=title;lang[d['description']]=desc
  im=ink.icon_base(ink.hexrgb(color));draw=ImageDraw.Draw(im);glyphs[(sum(map(ord,f))+n)%len(glyphs)](draw,ink.hexrgb(color));draw.line((25,102,103,102),fill=ink.hexrgb(color)+(220,),width=2)
  for k in range(n+1):draw.rectangle((28+k*10,108,33+k*10,112),fill=ink.hexrgb(color)+(255,))
  png(ASSETS/'textures/gui/technique'/f'{key}.png',ink.finish(im,32));new.append(key)
 save(fp,family)
lang['entity.jjkcraft.transfigured_soul']='Transfigured Soul'
save(ROOT/'src/main/generated/assets/jjkcraft/lang/en_us.json',lang)
# Original asymmetric stitched servant, allocated UV faces and a hunched reaching animation rig.
m=art.Model('transfigured_soul',{'body':((105,79,112),(190,148,178)),'bone':((179,160,145),(223,207,189))})
m.bone('root',(0,0,0));m.bone('torso',(0,12,0),'root',[art.c((-4,8,-2),(8,10,5),'body',['ribs'])]);m.bone('head',(0,18,0),'torso',[art.c((-3,17,-3),(6,7,6),'body',['eyes','mouth']),art.c((2,20,-4),(3,3,2),'bone')]);m.bone('left_arm',(4,16,0),'torso',[art.c((4,3,-2),(3,14,3),'body'),art.c((4,1,-3),(4,3,5),'bone')]);m.bone('right_arm',(-4,16,0),'torso',[art.c((-7,6,-2),(3,11,3),'body'),art.c((-8,3,-3),(4,4,4),'bone')]);m.bone('left_leg',(2,8,0),'root',[art.c((1,0,-1),(3,9,3),'body')]);m.bone('right_leg',(-2,8,0),'root',[art.c((-4,0,-1),(3,9,3),'body')]);m.bone('stitches',(0,12,-2),'torso',[art.c((-3,12,-3),(6,1,1),'bone'),art.c((-2,9,-3),(4,1,1),'bone')])
rig=dict(root='root',body=['torso'],head=['head'],arms=['left_arm','right_arm'],legs=['left_leg','right_leg'],extra=['stitches'],style='walker')
save(ASSETS/'geckolib/models/entity/transfigured_soul.geo.json',m.geo());save(ASSETS/'geckolib/animations/entity/transfigured_soul.animation.json',art.anims('transfigured_soul',rig));png(ASSETS/'textures/entity/transfigured_soul.png',m.texture(2391))
report={p.stem:len(load(p)['techniques']) for p in (DATA/'innate_family').glob('*.json')};assert min(report.values())>=4,report
save(ROOT/'build/family-expansion-inventory.json',dict(added=len(new),moves=new,families=report))
print('Added',len(new),'moves;',len(report),'families; smallest family',min(report.values()))
