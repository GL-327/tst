"""Authored Limitless applications and the universal positive-energy discharge."""
from pathlib import Path
import json, os, copy
ROOT=Path(__file__).resolve().parents[1]
D=ROOT/'src/main/resources/data/jjkcraft/jjkcraft'
A=ROOT/'src/main/resources/assets/jjkcraft'
def save(p,v):
    t=p.with_suffix('.writing');t.write_text(json.dumps(v,indent=2,ensure_ascii=False)+'\n',encoding='utf-8');os.replace(t,p)
L=ROOT/'src/main/generated/assets/jjkcraft/lang/en_us.json'
lang=json.loads(L.read_text(encoding='utf-8'))
def move(key,name,desc,source,cost,cooldown,grade,range_,behavior=None,charge=False):
    d=copy.deepcopy(json.loads((D/'technique'/f'{source}.json').read_text(encoding='utf-8')))
    d.update(name=f'technique.jjkcraft.{key}',description=f'technique.jjkcraft.{key}.desc',grade=f'jjkcraft:{grade}',icon=f'jjkcraft:textures/gui/technique/{key}.png',innate_weight=0)
    d['cast']={'cost':cost,'cooldown':cooldown,'range':range_}
    if charge:d['cast']['chargeable']=True
    if behavior:d['behavior']=behavior
    save(D/'technique'/f'{key}.json',d);lang[d['name']]=name;lang[d['description']]=desc
    recipe={'type':'minecraft:crafting_shapeless','category':'misc','ingredients':['minecraft:paper','minecraft:amethyst_shard','minecraft:ink_sac','minecraft:lapis_lazuli'],'result':{'id':'jjkcraft:technique_scroll','components':{'jjkcraft:loaded_technique':'jjkcraft:'+key}}}
    save(ROOT/'src/main/resources/data/jjkcraft/recipe'/f'scroll_{key}.json',recipe)
move('mini_blue','Lapse: Mini Blue','A fast, inexpensive attraction point. Short-lived and precise; adjust reach, area and force independently.','lapse_blue',12,35,'grade_4',14,{'type':'jjkcraft:field','strength':.65,'radius':1.6,'duration':24,'damage_per_second':3,'burst_damage':8,'burst_radius':2})
move('blue_punch','Lapse: Blue Punch','A close-range spatial strike that pulls the opponent toward your fist. Aim and timing matter; this punch does not charge.','lapse_blue',16,35,'grade_4',3.5,{'type':'jjkcraft:manga_ability','ability':'blue_punch','damage':8,'radius':1.5,'duration':1})
move('blue_cluster','Lapse: Mini Blue Cluster','Place three small attraction points across your aimed plane. Terrain clips each point; smaller fields trade reach for cross-pressure.','lapse_blue',32,100,'grade_3',16,{'type':'jjkcraft:manga_ability','ability':'blue_cluster','damage':2,'radius':1.6,'duration':36})
move('mini_red','Reversal: Mini Red','An inexpensive, immediate repulsion field. Short knockback burst with a small footprint; useful for disengaging.','reversal_red',18,45,'grade_3',12,{'type':'jjkcraft:field','strength':-1.1,'radius':1.8,'duration':12,'damage_per_second':5,'burst_damage':10,'burst_radius':2})
move('red_punch','Reversal: Red Punch','A point-blank repulsion strike that launches the target away. Personalize output and force; it requires a real aimed hit.','reversal_red',24,55,'grade_3',3.5,{'type':'jjkcraft:manga_ability','ability':'red_punch','damage':11,'radius':1.5,'duration':1})
move('maximum_output_red','Reversal: Maximum Red','Hold to build a large repulsion field. Maximum output is expensive and can damage nearby terrain.','reversal_red',65,220,'grade_1',28,{'type':'jjkcraft:field','strength':-3,'radius':7,'duration':35,'damage_per_second':7,'burst_damage':42,'burst_radius':8},True)
move('positive_energy_output','RCT: Mending Output','Send positive energy into an ally to heal them, or mend yourself less efficiently. Requires learned RCT.','reverse_cursed_technique',24,140,'grade_4',10,{'type':'jjkcraft:mend_other','amount':8,'regen_ticks':100,'self_fraction':.6})
move('positive_energy_discharge','RCT: Positive-Energy Discharge','Project positive energy through a short, aimed line. Damages cursed spirits, including spirit players, and passes harmlessly through living humans. Requires learned RCT.','reverse_cursed_technique',36,60,'grade_4',6,{'type':'jjkcraft:manga_ability','ability':'positive_output','damage':22,'duration':1,'radius':2})
p=D/'technique/positive_energy_discharge.json';d=json.loads(p.read_text());d['category']='strike';d['visuals']['trail']='beam';save(p,d)
p=D/'innate_family/limitless.json';d=json.loads(p.read_text());
for key in ['mini_blue','blue_punch','blue_cluster','mini_red','red_punch','maximum_output_red']:
    if 'jjkcraft:'+key not in d['techniques']:d['techniques'].append('jjkcraft:'+key)
save(p,d)
p=D/'technique/infinity.json';d=json.loads(p.read_text());d['cast']={'cost':0,'cooldown':0,'mode':'toggle','upkeep':1.5};d['behavior']['filter']=2;save(p,d)
lang['technique.jjkcraft.infinity.desc']='Toggle an independent passive spatial field. It stays active while other powers are used. Without learned RCT it drains CE slowly and prevents natural recovery; RCT removes that drain. Environmental hazards and domain sure-hits bypass it.'
lang['screen.jjkcraft.family_selector.desc']='A family selector: activate to open the wheel of learned applications. Hold a variant to charge where supported, or bind one move directly.'
lang['screen.jjkcraft.wheel.hint']='Scroll / point, then release to select'
lang['key.jjkcraft.wheel']='Hold technique wheel (scroll to select)'
p=D/'technique/reverse_cursed_technique.json';d=json.loads(p.read_text());d['innate_weight']=0;d['grade']='jjkcraft:grade_4';save(p,d)
save(L,lang)
print('6 Limitless variants + universal positive-energy output; Infinity passive metadata updated.')
