"""Data additions for the combat/awakening completion work. Run after expand_content.py."""
import json
import random
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter
import gen_items as item_art

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / 'src/main/resources/data/jjkcraft/jjkcraft'

MOVES = {
    'jab': dict(startup=3, active=2, recovery=6, damage=2.4, ce=1, height=1, reach=3, guard_damage=13, stun=4, knockback=0.28),
    'hook': dict(startup=5, active=3, recovery=8, damage=3, ce=2, height=2, reach=3, guard_damage=20, stun=5, knockback=0.5),
    'backfist': dict(startup=4, active=2, recovery=9, damage=3, ce=2, height=1, reach=3, guard_damage=17, stun=6, knockback=0.6),
    'uppercut': dict(startup=5, active=2, recovery=12, damage=3.2, ce=4, height=2, reach=3.2, guard_damage=24, stun=8, knockback=0.2, lift=0.45),
    'sweep': dict(startup=6, active=3, recovery=12, damage=2.6, ce=3, height=0, reach=3, guard_damage=18, stun=10, knockback=0.3),
    'heavy': dict(startup=9, active=3, recovery=17, damage=5.2, ce=8, height=1, reach=3.4, guard_damage=48, stun=10, knockback=0.85),
}
for name, move in MOVES.items():
    path = DATA / 'combat_move' / (name + '.json'); path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(move, indent=2) + '\n')
for index, path in enumerate(sorted((DATA / 'curse').glob('*.json'))):
    definition = json.loads(path.read_text())
    definition['combat_moves'] = ['jjkcraft:jab', 'jjkcraft:' + ['hook', 'sweep', 'heavy', 'uppercut'][index % 4]]
    path.write_text(json.dumps(definition, indent=2) + '\n')
    loot = definition.get('loot_table')
    if loot:
        source = ROOT / 'src/main/generated/data/jjkcraft/loot_table' / (loot.split(':')[1] + '.json')
        target = source.with_name(path.stem + '.json')
        if source.exists() and source != target: target.write_bytes(source.read_bytes())
    if definition['grade'] == 'jjkcraft:grade_4' and path.stem in ['soot_imp','shriek_vessel','railcrawler','lantern_leech']:
        definition['spawn']['max_light'] = 15
        path.write_text(json.dumps(definition,indent=2)+'\n')
lang_path = ROOT / 'src/main/generated/assets/jjkcraft/lang/en_us.json'
lang = json.loads(lang_path.read_text(encoding='utf-8'))
lang.update({'key.jjkcraft.combat': 'Toggle Cursed Combat', 'key.jjkcraft.dash': 'Dash', 'key.jjkcraft.lock_on': 'Lock on'})
lang_path.write_text(json.dumps(lang, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
assets = ROOT / 'src/main/resources/assets/jjkcraft'
generated = ROOT / 'src/main/generated/assets/jjkcraft'
vfx_dir = assets / 'textures/vfx'; vfx_dir.mkdir(parents=True, exist_ok=True)
for index, grade in enumerate(['grade_4', 'grade_3', 'semi_grade_2', 'grade_2', 'semi_grade_1', 'grade_1', 'special_grade']):
    rng = random.Random(5271 + index)
    noise = Image.new('L', (64, 64)); noise.putdata([rng.randrange(256) for _ in range(64*64)])
    noise = noise.resize((256,256), Image.Resampling.BICUBIC).filter(ImageFilter.GaussianBlur(1.8 if index < 3 else 0.5))
    low, high = noise.getextrema(); noise = noise.point(lambda p: int((p-low)*253/max(1,high-low))+1)
    texture = Image.new('RGBA',(256,256),(100+index*12,30,160,0)); texture.putalpha(noise)
    texture.save(vfx_dir / ('dissolve_' + grade + '.png'))
# Shared original ink/noise atlas, also usable in resource-pack-defined VFX.
Image.open(vfx_dir / 'dissolve_grade_2.png').save(vfx_dir / 'shared_noise.png')
for path in (DATA / 'curse').glob('*.json'):
    definition = json.loads(path.read_text()); ident = path.stem
    flow = Image.new('RGBA',(32,128)); draw = ImageDraw.Draw(flow)
    color = tuple(int(definition['codex']['color'][n:n+2],16) for n in (1,3,5))
    egg_path = assets / 'textures/item' / (ident + '_spawn_egg.png')
    if not egg_path.exists():
        item_art.egg(ident, '#D7CBC4', definition['codex']['color']).save(egg_path)
    for frame in range(4):
        for y in range(32):
            for x in range(32):
                if (x + y*2 + frame*4)%19 < 4: draw.point((x,y+frame*32),fill=color+(150,))
    flow_path = assets / 'textures/entity' / (ident+'_flow.png'); flow.save(flow_path)
    flow_path.with_suffix('.png.mcmeta').write_text(json.dumps({'animation':{'frametime':4,'interpolate':True}}))
    if definition['grade']=='jjkcraft:special_grade':
        for suffix in ['', '_glow', '_wear', '_wounded', '_critical']:
            p=assets / 'textures/entity' / (ident+suffix+'.png'); im=Image.open(p)
            if im.width<256: im.resize((256,256),Image.Resampling.NEAREST).save(p)
# Cursed object and reliquary use the same original ink-and-bone texture.
image=Image.new('RGBA',(32,32),(0,0,0,0)); draw=ImageDraw.Draw(image)
draw.rounded_rectangle((9,3,22,28),radius=4,fill=(213,197,170,255),outline=(61,28,71,255),width=2)
for y in range(5,29,5): draw.line((10,y,21,y-2),fill=(164,47,207,255),width=2)
item_dir=assets/'textures/item'; item_dir.mkdir(parents=True,exist_ok=True); image.save(item_dir/'soul_object.png')
block_dir=assets/'textures/block'; block_dir.mkdir(parents=True,exist_ok=True)
block=Image.new('RGBA',(32,32),(27,19,34,255)); block.alpha_composite(image); block.save(block_dir/'soul_reliquary.png')
def asset(path,data):
    path.parent.mkdir(parents=True,exist_ok=True); path.write_text(json.dumps(data,indent=2)+'\n')
asset(generated/'models/item/soul_object.json',{'parent':'minecraft:item/generated','textures':{'layer0':'jjkcraft:item/soul_object'}})
asset(generated/'items/soul_object.json',{'model':{'type':'minecraft:model','model':'jjkcraft:item/soul_object'}})
asset(generated/'models/block/soul_reliquary.json',{'parent':'minecraft:block/cube_all','textures':{'all':'jjkcraft:block/soul_reliquary'}})
asset(generated/'blockstates/soul_reliquary.json',{'variants':{'':{'model':'jjkcraft:block/soul_reliquary'}}})
lang.update({'item.jjkcraft.soul_object':'Cursed Object','block.jjkcraft.soul_reliquary':'Cursed Object','entity.jjkcraft.vessel':'Vessel'})
lang_path.write_text(json.dumps(lang,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
print('Wrote six combat moves, curse move lists and combat key names.')

# Blood-family icons extend the existing sprite system with a red palette.
for name, source in [('blood_needle', 'grudge_bolt'), ('blood_edge', 'cleave'), ('crimson_binding', 'grave_chains')]:
    icon = Image.open(assets / 'textures/gui/technique' / (source + '.png')).convert('RGBA')
    pixels = icon.load()
    for y in range(icon.height):
        for x in range(icon.width):
            r, g, b, a = pixels[x, y]
            if a and max(r,g,b) - min(r,g,b) > 30:
                pixels[x,y] = (190, 28, 54, a)
    icon.save(assets / 'textures/gui/technique' / (name + '.png'))
