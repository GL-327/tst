"""Validate shipped rig graphs, animation references, atlas bounds and native equipment resources."""
import json
import hashlib
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
A = ROOT / 'src/main/resources/assets/jjkcraft'
sources = json.loads((ROOT / 'tools/spectral_model_sources.json').read_text(encoding='utf-8'))
required = {'idle', 'walk', 'run', 'attack', 'jab', 'heavy', 'uppercut', 'sweep', 'cast',
            'attack_windup', 'guard', 'hurt', 'stagger', 'spawn', 'death', 'emerge'}
count = cubes = 0
signatures=set()
for name in sources:
    model = json.loads((A / f'geckolib/models/entity/{name}.geo.json').read_text())['minecraft:geometry'][0]
    width, height = (model['description'][k] for k in ('texture_width', 'texture_height'))
    bones = model['bones']
    names = {b['name'] for b in bones}
    signature=hashlib.sha256(json.dumps([{'name':b['name'],'parent':b.get('parent'),'cubes':[{k:v for k,v in c.items() if k!='uv'} for c in b.get('cubes',[])]} for b in bones],sort_keys=True).encode()).hexdigest()
    assert signature not in signatures,(name,'duplicate geometry')
    signatures.add(signature)
    assert len(names) == len(bones), (name, 'duplicate bone')
    parents = {b['name']: b.get('parent') for b in bones}
    for bone in bones:
        visited = set()
        parent = bone['name']
        while parent:
            assert parent in names and parent not in visited, (name, bone['name'], 'invalid rig graph')
            visited.add(parent)
            parent = parents[parent]
        for cube in bone.get('cubes', []):
            cubes += 1
            assert set(cube['uv']) == {'north', 'south', 'east', 'west', 'up', 'down'}, name
            for face in cube['uv'].values():
                x, y = face['uv']; w, h = face['uv_size']
                assert 0 <= min(x, x+w) <= max(x, x+w) <= width, (name, face)
                assert 0 <= min(y, y+h) <= max(y, y+h) <= height, (name, face)
    clips = json.loads((A / f'geckolib/animations/entity/{name}.animation.json').read_text())['animations']
    assert required <= clips.keys(), (name, required - clips.keys())
    for clip in clips.values():
        assert clip.get('bones', {}).keys() <= names, (name, 'animation references missing bone')
    for suffix in ('', '_glow', '_wounded', '_critical'):
        with Image.open(A / f'textures/entity/{name}{suffix}.png') as im:
            assert im.size == (width, height), (name, suffix, im.size)
            assert im.mode == 'RGBA', (name, suffix, im.mode)
    lod = json.loads((A / f'geckolib/models/entity/{name}_lod.geo.json').read_text())['minecraft:geometry'][0]
    assert {b['name'] for b in lod['bones']} == names, (name, 'LOD rig mismatch')
    count += 1
sets = ('sorcerer_uniform', 'binding_vestments', 'heavenly_hunter')
items = [f'{s}_{p}' for s in sets for p in ('helmet', 'chestplate', 'leggings', 'boots')]
items += ['playful_cloud', 'dragon_bone', 'stormbell_spear', 'ember_wakizashi', 'sealbreaker_tanto']
for item in items:
    for path in (A/f'items/{item}.json', A/f'models/item/{item}.json', ROOT/f'src/main/resources/data/jjkcraft/recipe/{item}.json'):
        assert json.loads(path.read_text()), path
    assert (A/f'textures/item/{item}.png').is_file(), item
for family in sets:
    definition = json.loads((A/f'equipment/{family}.json').read_text())
    for layer, entries in definition['layers'].items():
        for entry in entries:
            texture = entry['texture'].split(':')[1]
            assert (A/f'textures/entity/equipment/{layer}/{texture}.png').is_file(), (family, layer)
techniques=list((ROOT/'src/main/resources/data/jjkcraft/jjkcraft/technique').glob('*.json'))
for path in techniques:
    data=json.loads(path.read_text());icon=data['icon'].split(':',1)[1]
    assert (A/icon).is_file(),(path.stem,'missing icon')
assert len(techniques)==296,len(techniques)
with Image.open(A/'textures/vfx/energy_materials.png') as materials:
    assert materials.mode=='RGBA' and materials.size==(1024,1024),'VFX material atlas layout'
    for tile in range(16):
        cell=materials.crop(((tile%4)*256,(tile//4)*256,(tile%4+1)*256,(tile//4+1)*256))
        alpha=cell.getchannel('A')
        assert alpha.getextrema()[1]>100,(tile,'empty VFX material')
        assert alpha.crop((0,0,256,1)).getextrema()[1]==0,(tile,'missing transparent gutter')
        assert alpha.crop((0,0,1,256)).getextrema()[1]==0,(tile,'missing transparent gutter')
metadata=json.loads((A/'textures/vfx/energy_materials.png.mcmeta').read_text())
assert metadata['texture']['blur'],'VFX materials require smooth filtering'
print(f'PASS: {count} unique creature rigs, {cubes} UV-mapped cubes, {count*16} clips, {count*4} atlases, 17 equipment items and 296 technique icons.')
print('PASS: sixteen filtered 256px VFX materials with transparent atlas gutters.')
