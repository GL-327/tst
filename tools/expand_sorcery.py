"""Original assets and data for the integrated sorcery/attributes update. Python + Pillow."""
import copy, json, builtins
from pathlib import Path
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
DATA=ROOT/'src/main/resources/data/jjkcraft/jjkcraft'
ASSETS=ROOT/'src/main/resources/assets/jjkcraft'
LANG=ROOT/'src/main/generated/assets/jjkcraft/lang/en_us.json'
translations={}
def write(path,data):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(data,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
def move(name,title,desc,color,behavior,grade='grade_4',cost=16,cooldown=100,range=12,base='grudge_bolt'):
    value=json.loads((DATA/'technique'/(base+'.json')).read_text())
    value.update(name='technique.jjkcraft.'+name,description='technique.jjkcraft.'+name+'.desc',grade='jjkcraft:'+grade,
        innate_weight=0,icon='jjkcraft:textures/gui/technique/'+name+'.png',behavior=behavior)
    value['cast']={'cost':cost,'cooldown':cooldown,'range':range}
    value['category']='utility' if behavior['type']=='jjkcraft:manga_ability' else 'projectile' if behavior['type']=='jjkcraft:projectile' else 'strike' if behavior['type']=='jjkcraft:strike' else 'ultimate' if grade=='special_grade' else 'area'
    value.pop('vow_modifiers',None)
    value['visuals'].update(color=color,shake=0.15,hit_stop=0)
    if behavior['type']=='jjkcraft:manga_ability': value['visuals']['trail']='aura'
    write(DATA/'technique'/(name+'.json'),value)
    translations[value['name']]=title;translations[value['description']]=desc
    icon=Image.open(ASSETS/'textures/gui/technique'/(base+'.png')).convert('RGBA')
    rgb=tuple(int(color[n:n+2],16) for n in (1,3,5));px=icon.load()
    for y in builtins.range(icon.height):
        for x in builtins.range(icon.width):
            r,g,b,a=px[x,y]
            if a and max(r,g,b)-min(r,g,b)>30:px[x,y]=rgb+(a,)
    icon.save(ASSETS/'textures/gui/technique'/(name+'.png'))
def ability(name,title,desc,kind,color,damage=0,duration=60,radius=4,**args):
    move(name,title,desc,color,dict(type='jjkcraft:manga_ability',ability=kind,damage=damage,duration=duration,radius=radius),**args)
def projectile(name,title,desc,color,damage=8,**args):
    move(name,title,desc,color,dict(type='jjkcraft:projectile',damage=damage,speed=1.8,size=0.3,lifetime=45),**args)
def family(name,title,moves,weight=12,clan='',inherited=0,domain=''):
    key='family.jjkcraft.'+name;translations[key]=title
    write(DATA/'innate_family'/(name+'.json'),dict(name=key,clan=clan,inherited_weight=inherited,random_weight=weight,
        techniques=['jjkcraft:'+s for s in moves],domain='jjkcraft:'+domain if domain else ''))

ability('brain_transplant','Brain Transplant','Aim at a fallen humanoid body within six blocks. Inhabit that body and inherit up to three host moves. A living player cannot be taken over.','body_transfer','#CC7F99',cost=25,cooldown=200,range=6)
ability('antigravity_system','Antigravity System','Lift nearby enemies and slow their fall. A close-range body-transfer family extension.','antigravity','#9373D2',radius=5,duration=80,grade='grade_3',cost=24)
ability('gravity_reversal','Antigravity System: Reversal','Press enemies down with a dense gravitational field.','gravity','#49336D',damage=10,duration=60,radius=5,grade='grade_1',cost=55)
family('brain_transplant','Brain Transplant',['brain_transplant','antigravity_system','gravity_reversal'],4)
ability('boogie_woogie','Boogie Woogie','Clap to exchange positions with the target you are aiming at. Both destinations need free space.','swap','#E5BA70',cost=10,cooldown=25,range=18)
ability('boogie_woogie_extended','Boogie Woogie: Extended Range','A more distant positional exchange. Use the terrain to turn a fight.','swap','#E5BA70',grade='grade_2',cost=24,cooldown=65,range=32)
family('boogie_woogie','Boogie Woogie',['boogie_woogie','boogie_woogie_extended'],20)
ability('star_rage','Star Rage','Add virtual mass to your strikes: temporary Strength and Resistance without slowing your movement.','mass','#F0CA4F',duration=160,cost=18,cooldown=220)
move('garuda_strike','Star Rage: Garuda Strike','A concentrated, heavy mass strike.','#F0CA4F',dict(type='jjkcraft:strike',damage=15,knockback=1.3,black_flash=True),grade='grade_2',cost=30,range=5,cooldown=100,base='ink_iron_palm')
family('star_rage','Star Rage',['star_rage','garuda_strike'],10)
ability('cursed_spirit_manipulation','Cursed Spirit Manipulation','Sneak-cast to absorb a curse below 30% health; cast normally to release one. Store eight and command up to three at once.','curse_manipulation','#7F3FA3',cost=18,cooldown=60,range=12)
ability('maximum_uzumaki','Cursed Spirit Manipulation: Maximum Uzumaki','Consume every stored curse in a powerful linear cursed-energy attack. More captured spirits increase its damage.','uzumaki','#551479',grade='grade_1',cost=80,cooldown=600,range=30)
family('curse_manipulation','Cursed Spirit Manipulation',['cursed_spirit_manipulation','maximum_uzumaki'],7)
ability('disaster_flames','Disaster Flames: Ignition','Ignite and burn nearby enemies with volcanic cursed energy.','flames','#F35A28',damage=5,radius=5,cost=18)
projectile('ember_insect','Disaster Flames: Ember Insect','A concentrated ember projectile that strikes at range.','#F35A28',cost=14,grade='grade_3')
ability('volcanic_burst','Disaster Flames: Volcanic Burst','A violent close-range eruption of heat.','flames','#F35A28',damage=18,radius=9,cost=65,cooldown=300,grade='grade_1')
family('disaster_flames','Disaster Flames',['disaster_flames','ember_insect','volcanic_burst'],8,domain='coffin_of_the_iron_mountain')
ability('frost_calm','Ice Formation: Frost Calm','Freeze enemies around you in a short burst of cold.','freeze','#80D5E2',damage=4,duration=35,radius=5,cost=18)
projectile('ice_lance','Ice Formation: Ice Lance','Launch a sharp lance of cursed ice.','#80D5E2',damage=12,grade='grade_3',cost=22)
ability('icefall','Ice Formation: Icefall','A wide wave of freezing cursed energy.','freeze','#80D5E2',damage=10,duration=70,radius=9,cost=60,grade='grade_1',cooldown=300)
family('ice_formation','Ice Formation',['frost_calm','ice_lance','icefall'],12)
ability('construction_armor','Construction: Insect Armor','Build temporary armor around your body, exchanging speed for protection.','armor','#A79572',duration=200,cost=20,cooldown=260)
projectile('liquid_metal','Construction: Liquid Metal','Shape a dense liquid-metal projectile.','#A79572',damage=10,grade='grade_3',cost=24)
move('perfect_sphere','Construction: Perfect Sphere','A compact armor-piercing line of crushing pressure.','#A79572',dict(type='jjkcraft:line_cut',damage=28,width=1.2,pierce=1),grade='special_grade',cost=160,cooldown=700,range=20,base='piercing_blood')
family('construction','Construction',['construction_armor','liquid_metal','perfect_sphere'],10)
ability('cursed_roots','Disaster Plants: Cursed Roots','Roots bind nearby enemies in place.','roots','#6B9F50',damage=3,duration=50,radius=5)
projectile('wooden_ball','Disaster Plants: Wooden Ball','A tightly packed wooden projectile.','#6B9F50',grade='grade_3',damage=10)
ability('flower_field','Disaster Plants: Flower Field','A broad field of roots that immobilizes enemies.','roots','#B7849C',damage=5,duration=90,radius=9,grade='grade_1',cost=55,cooldown=320)
family('disaster_plants','Disaster Plants',['cursed_roots','wooden_ball','flower_field'],8)
ability('disaster_tides','Disaster Tides: Flood','A rushing pressure wave pushes nearby enemies away.','repel','#3CAAC5',damage=5,radius=6)
projectile('water_lance','Disaster Tides: Water Lance','A high-pressure projectile of cursed water.','#3CAAC5',damage=11,grade='grade_3',cost=22)
family('disaster_tides','Disaster Tides',['disaster_tides','water_lance'],8,domain='horizon_of_the_captivating_skandha')
ability('sky_manipulation','Sky Manipulation','Bend the air around you to repel enemies.','repel','#A9C9F4',damage=3,radius=5,cost=15)
move('thin_ice_breaker','Sky Manipulation: Thin Ice Breaker','Strike a bent surface of the sky and shatter it through the target.','#A9C9F4',dict(type='jjkcraft:line_cut',damage=16,width=1.2,pierce=0.5),grade='grade_2',cost=32,range=12,cooldown=120,base='piercing_blood')
ability('sky_step','Sky Manipulation: Sky Step','A rising step with temporary slow falling.','flight','#A9C9F4',grade='grade_3',duration=100,cost=16,cooldown=100)
family('sky_manipulation','Sky Manipulation',['sky_manipulation','sky_step','thin_ice_breaker'],12)
ability('flowing_red_scale','Blood Manipulation: Flowing Red Scale','Control your circulation to raise physical speed and striking power.','flowing_blood','#B91C38',grade='grade_3',duration=180,cost=20,cooldown=240)
p=DATA/'innate_family/blood_manipulation.json';value=json.loads(p.read_text());value['techniques']=['jjkcraft:'+s for s in ['crimson_binding','blood_needle','flowing_red_scale','piercing_blood']];write(p,value)

# Attributes include original restriction variants, explicitly labeled as such.
def trait(name,title,description,category='physical',weight=10,**values):
    key='trait.jjkcraft.'+name;translations[key]=title;translations[key+'.desc']=description
    write(DATA/'trait'/(name+'.json'),dict(name=key,description=key+'.desc',category=category,weight=weight,**values))
trait('six_eyes','Six Eyes','A Gojo-clan gift, separate from Limitless. Cursed technique costs are reduced and energy perception is sharper.','clan',0,clan='Gojo',regen=1.1)
trait('heavenly_body','Heavenly Restriction: Unbound Body','Zero cursed energy in exchange for enhanced strength, speed and senses.','restriction_zero',50)
trait('silent_stride','Heavenly Restriction: Silent Stride (Original)','Zero CE. A light, fast body sacrifices health for exceptional mobility.','restriction_zero',20,speed=0.2,health=-4)
trait('iron_frame','Heavenly Restriction: Iron Frame (Original)','Zero CE. A dense body gains health and armor at the cost of speed.','restriction_zero',20,speed=-0.08,health=8,armor=4)
trait('spring_tendons','Heavenly Restriction: Spring Tendons (Original)','Zero CE. Explosive movement and striking force come with lower durability.','restriction_zero',10,speed=0.12,damage=2,health=-6)
trait('fragile_conduit','Heavenly Restriction: Fragile Conduit','A frail body trades health and mobility for a much larger cursed-energy reserve.',weight=4,capacity=2.0,regen=1.2,health=-8,speed=-0.12)
trait('iron_constitution','Iron Constitution','A naturally resilient body with additional maximum health.',health=4)
trait('fleet_footed','Fleet Footed','Naturally faster movement without spending a technique slot.',speed=0.1)
trait('powerful_frame','Powerful Frame','Stronger physical strikes and a slightly heavier body.',damage=1.5,speed=-0.03)
trait('ce_abundance','Abundant Cursed Energy','An unusually large innate energy reserve.',capacity=1.4,weight=6)
trait('ce_efficiency','Refined Circulation','Cursed energy replenishes more quickly.',regen=1.35,weight=7)
trait('electric_energy','Electrified Cursed Energy','Innate electrical cursed-energy texture adds a small shock to your physical hits.',weight=4)
trait('rough_energy','Rough Cursed Energy','A coarse cursed-energy texture makes physical impacts harsher.',damage=1,weight=5)
trait('soul_awareness','Soul Awareness','A resilient sense of self improves cursed-energy circulation and bodily durability.',regen=1.1,armor=1,weight=6)
trait('vengeful_spirit','Vengeful Cursed Spirit','A cursed body with more CE and health. CE regenerates injuries; exorcism creates a new character.','spirit',0,capacity=1.25,regen=1.4,health=6,armor=2)

# Complete the existing reserved domain, plus themed Disaster-family domains.
for name,title,color,grade in [('self_embodiment_of_perfection','Self-Embodiment of Perfection','#B07A6A','special_grade'),('coffin_of_the_iron_mountain','Coffin of the Iron Mountain','#F35A28','grade_1'),('horizon_of_the_captivating_skandha','Horizon of the Captivating Skandha','#3CAAC5','grade_1')]:
    value=json.loads((DATA/'domain/malevolent_shrine.json').read_text())
    value.update(name='domain.jjkcraft.'+name,description='domain.jjkcraft.'+name+'.desc',grade='jjkcraft:'+grade,innate_weight=0)
    value['visuals'].update(color=color,dome='#150F1B',ambience='glyphs');value['behaviors']=[dict(type='jjkcraft:sure_hit_damage',interval=15,damage=2.5),dict(type='jjkcraft:apply_effect',effect='minecraft:slowness',amplifier=1,duration=25,interval=20)]
    write(DATA/'domain'/(name+'.json'),value);translations[value['name']]=title;translations[value['description']]='A themed Domain Expansion with a sure-hit pulse and oppressive terrain pressure.'
strings=json.loads(LANG.read_text(encoding='utf-8'));strings.update(translations);write(LANG,strings)
java='package com.jjkcraft.datagen;\n\nfinal class ExpansionLang {\n    static void add(net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider.TranslationBuilder b) {\n'
java+=''.join('        b.add('+json.dumps(k)+', '+json.dumps(v)+');\n' for k,v in sorted(translations.items()))+'    }\n}\n'
(ROOT/'src/datagen/java/com/jjkcraft/datagen/ExpansionLang.java').write_text(java,encoding='utf-8')
print('Added',len(list((DATA/'trait').glob('*.json'))),'attributes;',len(list((DATA/'technique').glob('*.json'))),'techniques;',len(list((DATA/'innate_family').glob('*.json'))),'families.')
