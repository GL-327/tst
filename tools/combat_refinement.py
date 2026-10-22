"""Curated 1.13 combat definitions. Safe to rerun after the content generators."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]
DATA=ROOT/'src/main/resources/data/jjkcraft/jjkcraft/technique'
LANG=ROOT/'src/main/generated/assets/jjkcraft/lang/en_us.json'

def edit(name,cast=None,behavior=None):
    p=DATA/f'{name}.json';d=json.loads(p.read_text(encoding='utf-8'))
    d['cast'].update(cast or {});d['behavior'].update(behavior or {})
    if d['behavior']['type']=='jjkcraft:field':d['behavior'].pop('damage',None)
    p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8',newline='\n')

edit('cleave',{'cost':14,'cooldown':24,'range':6},{'damage':16,'ce_scaling':1.1,'arc':110})
edit('dismantle',{'cost':12,'cooldown':20,'range':26},{'damage':19,'width':.65,'pierce':.7})
edit('shrine_web',{'cost':26,'cooldown':100,'range':16},{'damage':25,'radius':8})
edit('shrine_cage',{'cost':34,'cooldown':160,'range':20},{'damage_per_second':28,'radius':7})
edit('shrine_furnace',{'cost':70,'cooldown':280,'range':48},{'damage':70,'blast_radius':9,'size':2.2})
edit('lapse_blue',{'cost':24,'cooldown':65},{'radius':5,'damage_per_second':12,'duration':65})
edit('reversal_red',{'cost':34,'cooldown':90},{'radius':6,'damage_per_second':20,'duration':30,'burst_damage':36,'burst_radius':7})
edit('maximum_output_red',{'cost':95,'cooldown':320,'range':38},{'radius':11,'damage_per_second':32,'burst_damage':90,'burst_radius':12,'duration':45})
edit('maximum_output_blue',{'cost':140,'cooldown':420,'max_duration':80,'range':36},{'radius':16,'distance':20,'damage':90,'blast_radius':12,'strength':.4})
edit('hollow_purple',{'cost':120,'cooldown':360,'range':60},{'damage':70,'size':3.2,'speed':.85,'lifetime':100})
edit('blue_punch',{'cost':8,'cooldown':0},{'damage':10})
edit('red_punch',{'cost':12,'cooldown':0},{'damage':15})

lang=json.loads(LANG.read_text(encoding='utf-8'))
lang.update({
    'key.jjkcraft.ce_imbue':'Toggle CE fist imbuement',
    'text.autoconfig.jjkcraft-server.option.casting.techniqueDamageMultiplier':'Technique damage multiplier',
    'technique.jjkcraft.blue_punch.desc':'Toggle Blue imbuement on your fists. A landed unarmed punch pulls its target toward you and deals bonus Limitless damage for 5 CE. Cast again to clear it. Works alongside CE fist imbuement.',
    'technique.jjkcraft.red_punch.desc':'Toggle Red imbuement on your fists. A landed unarmed punch repels its target and deals bonus Limitless damage for 7 CE. Cast again to clear it. Works alongside CE fist imbuement.',
    'technique.jjkcraft.cleave.desc':'Adaptive close-range slashes. Cleave scales against the target\u2019s health, armour and cursed energy; 40% of its damage bypasses armour.',
    'technique.jjkcraft.dismantle.desc':'Send a precise, long-range cutting line through enemies. Most of its damage ignores armour.',
    'technique.jjkcraft.maximum_output_blue.desc':'A slow, maximum-output charge. Launch a collapsing attraction core outward; its sixteen-block influence gathers enemies before a twelve-block collapse.',
    'technique.jjkcraft.maximum_output_red.desc':'Build a slow maximum-output charge, then launch a large repulsion field outward. Full charge reaches an eleven-block influence radius.',
})
LANG.write_text(json.dumps(lang,ensure_ascii=False,indent=2)+'\n',encoding='utf-8',newline='\n')
print('Refined twelve technique profiles and their descriptions; universal technique damage defaults to 2.5x.')
