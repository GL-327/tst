"""Reproducible innate-family data and twenty additional original curse designs.

Extends the project's existing box/ink art system; no downloaded or canon art.
Run from the repository root: python tools/expand_content.py
"""
import copy
import json
import math
import random
from pathlib import Path
from PIL import Image, ImageDraw
import gen_curses as art

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / 'src/main/resources/data/jjkcraft/jjkcraft'
ASSETS = ROOT / 'src/main/resources/assets/jjkcraft'
LANG_PATH = ROOT / 'src/main/generated/assets/jjkcraft/lang/en_us.json'
lang = json.loads(LANG_PATH.read_text(encoding='utf-8'))
base_lang = dict(lang)

def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + '\n', encoding='utf-8')

def read(kind, name):
    return json.loads((DATA / kind / (name + '.json')).read_text(encoding='utf-8'))

def technique(name, title, desc, base, color):
    entry = read('technique', base)
    entry.update(name=f'technique.jjkcraft.{name}', description=f'technique.jjkcraft.{name}.desc',
                 icon=f'jjkcraft:textures/gui/technique/{name}.png', innate_weight=0, grade='jjkcraft:grade_4')
    entry['visuals']['color'] = color
    lang[entry['name']], lang[entry['description']] = title, desc
    write(DATA / 'technique' / (name + '.json'), entry)
    image = Image.open(ASSETS / 'textures/gui/technique' / (base + '.png')).convert('RGBA')
    rgb = tuple(int(color[n:n+2], 16) for n in (1, 3, 5))
    tint = Image.new('RGBA', image.size, rgb + (255,))
    image = Image.blend(image, tint, 0.3)
    image.save(ASSETS / 'textures/gui/technique' / (name + '.png'))

technique('crimson_binding', 'Blood Manipulation: Crimson Binding',
          'Weave your blood into a net that binds a target. Stronger Blood Manipulation unlocks with your grade.', 'grave_chains', '#B52E48')
technique('ratio', 'Ratio Technique',
          'Strike the seven-to-three weak point. Accurate hits during Cursed Combat gain a Ratio opening.', 'ink_iron_palm', '#D8B96A')

FAMILIES = [
    ('limitless', 'Limitless', 'Gojo', 4, 0, ['lapse_blue', 'infinity', 'reversal_red', 'hollow_purple', 'maximum_output_blue'], 'unlimited_void'),
    ('ten_shadows', 'Ten Shadows Technique', "Zen'in", 3, 0, ['ten_shadows_divine_dogs', 'ten_shadows_nue', 'ten_shadows_toad'], 'chimera_shadow_garden'),
    ('projection', 'Projection Sorcery', "Zen'in", 35, 0, ['projection_sorcery'], ''),
    ('blood_manipulation', 'Blood Manipulation', 'Kamo', 35, 0, ['crimson_binding', 'blood_needle', 'blood_edge', 'piercing_blood'], ''),
    ('cursed_speech', 'Cursed Speech', 'Inumaki', 35, 0, ['cursed_speech_dont_move', 'cursed_speech_crumble_away', 'cursed_speech_blast_away'], ''),
    ('shrine', 'Shrine', '', 0, 20, ['cleave', 'dismantle'], 'malevolent_shrine'),
    ('straw_doll', 'Straw Doll Technique', '', 0, 35, ['straw_doll_resonance'], ''),
    ('ratio', 'Ratio Technique', '', 0, 35, ['ratio'], ''),
    ('idle_transfiguration', 'Idle Transfiguration', '', 0, 10, ['idle_transfiguration'], 'self_embodiment_of_perfection'),
]
for ident, name, clan, inherited, general, moves, domain in FAMILIES:
    key = f'family.jjkcraft.{ident}'
    lang[key] = name
    write(DATA / 'innate_family' / (ident + '.json'), dict(name=key, clan=clan, inherited_weight=inherited,
        random_weight=general, techniques=['jjkcraft:' + move for move in moves], domain='jjkcraft:' + domain if domain else ''))
    starter = read('technique', moves[0])
    starter['grade'] = 'jjkcraft:grade_4'
    write(DATA / 'technique' / (moves[0] + '.json'), starter)

# Progression moves are revealed at meaningful grades, never silently rolled as a starting move.
for ident in ('ten_shadows_nue',):
    entry = read('technique', ident); entry['grade'] = 'jjkcraft:grade_4'
    write(DATA / 'technique' / (ident + '.json'), entry)

# id, title, original rig, health scale, silhouette accent; different data/move mixes per design.
NEW_CURSES = [
    ('soot_imp', 'Soot Imp', 'ledger_crawler', 0.8, 'horn'),
    ('shriek_vessel', 'Shriek Vessel', 'hollow_hymnal', 1.15, 'crown'),
    ('railcrawler', 'Railcrawler', 'ledger_crawler', 1.3, 'spine'),
    ('lantern_leech', 'Lantern Leech', 'wick_moth', 1.2, 'tail'),
    ('needle_widow', 'Needle Widow', 'pale_stilt', 0.85, 'spine'),
    ('grin_hound', 'Grin Hound', 'echo_mimic', 1.3, 'horn'),
    ('sewer_effigy', 'Sewer Effigy', 'salt_lantern', 1.4, 'tail'),
    ('mirror_wraith', 'Mirror Wraith', 'cracked_bell', 0.8, 'crown'),
    ('rust_pilgrim', 'Rust Pilgrim', 'thorn_matriarch', 0.9, 'spine'),
    ('bone_orchard', 'Bone Orchard', 'thorn_matriarch', 1.25, 'crown'),
    ('blackwater_bride', 'Blackwater Bride', 'tidewash', 1.2, 'tail'),
    ('knell_keeper', 'Knell Keeper', 'cracked_bell', 1.25, 'horn'),
    ('marrow_regent', 'Marrow Regent', 'gravebound_gate', 0.8, 'crown'),
    ('lacquer_butcher', 'Lacquer Butcher', 'ash_oni', 1.2, 'spine'),
    ('veil_weaver', 'Veil Weaver', 'vow_kin', 1.2, 'tail'),
    ('ossuary_king', 'Ossuary King', 'ten_mouth_sovereign', 0.8, 'crown'),
    ('night_procession', 'Night Procession', 'paper_tide_gravemind', 1.1, 'horn'),
    ('ink_leviathan', 'Ink Leviathan', 'unfinished_ferryman', 1.2, 'tail'),
    ('grief_cathedral', 'Grief Cathedral', 'gravebound_gate', 1.5, 'spine'),
    ('hunger_eclipse', 'Hunger Eclipse', 'ten_mouth_sovereign', 1.25, 'crown'),
]
palette_by_grade = {'grade_4': (140, 107, 182), 'grade_3': (125, 85, 184), 'grade_2': (156, 62, 162),
                    'grade_1': (190, 45, 84), 'special_grade': (171, 45, 215)}
extra_types = []
for index, (ident, title, source, factor, accent) in enumerate(NEW_CURSES):
    definition = read('curse', source)
    definition.update(entity=f'jjkcraft:{ident}', name=f'curse.jjkcraft.{ident}',
        description=f'curse.jjkcraft.{ident}.desc', lore=f'curse.jjkcraft.{ident}.lore')
    # The last two are rare stronger encounters, with the full high-grade budget.
    if ident in ('grief_cathedral', 'hunger_eclipse'): definition['grade'] = 'jjkcraft:special_grade'
    grade = definition['grade'].split(':')[1]
    color = palette_by_grade[grade]
    definition['stats']['health'] = min(1024, round(definition['stats']['health'] * factor))
    definition['stats']['speed'] = round(min(0.4, definition['stats']['speed'] * (1.08 if index % 2 else 0.95)), 3)
    definition['stats']['attack_damage'] = round(definition['stats']['attack_damage'] * (0.85 if factor > 1 else 1.1), 1)
    definition['model'] = dict(geo=f'jjkcraft:entity/{ident}', texture=f'jjkcraft:textures/entity/{ident}.png', animation=f'jjkcraft:entity/{ident}')
    definition['codex'] = dict(color='#' + ''.join(f'{n:02X}' for n in color), unlock_kills=3)
    definition['spawn'] = dict(weight=max(1, 10 - index // 2), min_fear={'grade_4': 15, 'grade_3': 25, 'grade_2': 38, 'grade_1': 50, 'special_grade': 70}[grade],
        max_light=10 if grade in ('grade_4', 'grade_3') else 7,
        group_min=1, group_max=3 if grade == 'grade_4' else 2 if grade == 'grade_3' else 1)
    definition['loot_table'] = f'jjkcraft:entities/{source}'
    write(DATA / 'curse' / (ident + '.json'), definition)
    lang[definition['name']] = title
    lang[f'entity.jjkcraft.{ident}'] = title
    lang[f'item.jjkcraft.{ident}_spawn_egg'] = title + ' Spawn Egg'
    lang[definition['description']] = {'horn': 'A fear-born hunter with a broken bone crest.', 'crown': 'A crowned spirit that announces its attacks with a pulsing core.',
        'spine': 'An armored curse whose exposed core brightens before it strikes.', 'tail': 'A trailing ink spirit that punishes careless pursuit.'}[accent]
    lang[definition['lore']] = f'{title} is born from forgotten fears. Watch its windup, step clear of its cast, and punish its recovery. Its cursed core reveals how badly it is wounded.'
    model, rig = getattr(art, source)()
    model.name = ident
    # New crest geometry uses an unused corner of a larger atlas, leaving the original UV intact.
    geo = model.geo(); geom = geo['minecraft:geometry'][0]
    geom['description']['texture_width'] = 256; geom['description']['texture_height'] = 256
    for bone in geom['bones']:
        for cube in bone.get('cubes', []): cube['uv'] = [v * 2 for v in cube['uv']]
    # Doubling UVs requires explicit per-face UVs for each original cube.
    for bone in geom['bones']:
        for cube in bone.get('cubes', []):
            u, v = cube['uv']; w, h, d = [max(1, round(s)) for s in cube['size']]
            cube['uv'] = {'north': {'uv': [u+2*d, v+2*d], 'uv_size': [2*w, 2*h]},
                'south': {'uv': [u+4*d+2*w, v+2*d], 'uv_size': [2*w, 2*h]},
                'east': {'uv': [u, v+2*d], 'uv_size': [2*d, 2*h]}, 'west': {'uv': [u+2*d+2*w, v+2*d], 'uv_size': [2*d, 2*h]},
                'up': {'uv': [u+2*d, v], 'uv_size': [2*w, 2*d]}, 'down': {'uv': [u+2*d+2*w, v], 'uv_size': [2*w, 2*d]}}
    # Attach the crest to the highest head/body bone so it follows the attack animations.
    parent = next((b['name'] for b in geom['bones'] if b['name'] == 'head'), rig.get('body', ['root'])[0])
    top = max((c['origin'][1] + c['size'][1] for b in geom['bones'] for c in b.get('cubes', [])), default=20)
    crest = []
    for n in range(3 if accent != 'tail' else 1):
        ox = (n - 1) * 3 if accent != 'tail' else 0
        crest.append({'origin': [ox - 0.5, top - 1, -1], 'size': [1, 3 + n % 2, 2], 'uv': [240, 230]})
    geom['bones'].append({'name': 'fear_crest', 'parent': parent, 'pivot': [0, top, 0], 'cubes': crest})
    image = model.texture(sum(map(ord, ident))).resize((256, 256), Image.Resampling.NEAREST)
    pixels = image.load(); rng = random.Random(index + 479)
    for py in range(256):
        for px in range(256):
            r, g, b, a = pixels[px, py]
            if a:
                luminosity = (r + g + b) / 765
                if max(r, g, b) - min(r, g, b) > 45: pixels[px, py] = tuple(int(c * (0.65 + luminosity * 0.35)) for c in color) + (a,)
                elif rng.random() < 0.05: pixels[px, py] = (r//2, g//2, b//2, a)
    ImageDraw.Draw(image).rectangle((240, 230, 255, 250), fill=color + (255,))
    image.save(ASSETS / 'textures/entity' / (ident + '.png'))
    write(ASSETS / 'geckolib/models/entity' / (ident + '.geo.json'), geo)
    write(ASSETS / 'geckolib/animations/entity' / (ident + '.animation.json'), art.anims(ident, rig))
    extra_types.append((ident, source in ('hollow_hymnal', 'wick_moth', 'cracked_bell')))

# Derive masks and damage states for all originals and additions, preserving readable ink/bone palettes.
for file in sorted((DATA / 'curse').glob('*.json')):
    definition = json.loads(file.read_text()); ident = file.stem
    image_path = ASSETS / definition['model']['texture'].split(':')[1]
    if not image_path.exists(): continue
    image = Image.open(image_path).convert('RGBA')
    color = tuple(int(definition['codex']['color'][n:n+2], 16) for n in (1, 3, 5))
    emissive = Image.new('RGBA', image.size); wear = Image.new('RGBA', image.size)
    rng = random.Random(ident)
    for py in range(image.height):
        for px in range(image.width):
            r, g, b, a = image.getpixel((px, py))
            if a and max(r, g, b) - min(r, g, b) > 50:
                emissive.putpixel((px, py), color + (190,))
            if a and rng.random() < 0.07: wear.putpixel((px, py), (26, 17, 32, 120))
    emissive.save(image_path.with_name(ident + '_glow.png'))
    wear.save(image_path.with_name(ident + '_wear.png'))
    for state, opacity in [('wounded', 0.5), ('critical', 1.0)]:
        damage = image.copy(); damage.alpha_composite(wear)
        draw = ImageDraw.Draw(damage)
        for n in range(4 if state == 'wounded' else 9):
            x, y = rng.randrange(image.width), rng.randrange(image.height)
            draw.line((x, y, min(image.width-1, x+8), min(image.height-1, y+16)), fill=color + (200,), width=1)
        damage.save(image_path.with_name(ident + '_' + state + '.png'))
    # Lower-detail mesh keeps all bone names, dropping secondary cubes from dense bones.
    geo_path = ASSETS / 'geckolib/models/entity' / (ident + '.geo.json')
    geo = json.loads(geo_path.read_text())
    for bone in geo['minecraft:geometry'][0]['bones']:
        if len(bone.get('cubes', [])) > 2: bone['cubes'] = bone['cubes'][:2]
    write(geo_path.with_name(ident + '_lod.geo.json'), geo)
    anim_path = ASSETS / 'geckolib/animations/entity' / (ident + '.animation.json')
    animations = json.loads(anim_path.read_text()); entries = animations['animations']
    for new, base in [('run', 'walk'), ('attack_windup', 'cast'), ('stagger', 'hurt')]:
        entries[new] = copy.deepcopy(entries.get(base, {}))
    write(anim_path, animations)

write(LANG_PATH, lang)
java_lang = ROOT / 'src/datagen/java/com/jjkcraft/datagen/ExpandedLang.java'
new_keys = [key for key in lang if key not in base_lang or key in {('family.jjkcraft.' + family[0]) for family in FAMILIES} or key.startswith(('technique.jjkcraft.crimson_binding', 'technique.jjkcraft.ratio')) or any(ident in key for ident, *_ in NEW_CURSES)]
java_lang.write_text('package com.jjkcraft.datagen;\n\nfinal class ExpandedLang {\n    static void add(net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider.TranslationBuilder b) {\n' +
    ''.join('        b.add(' + json.dumps(key) + ', ' + json.dumps(lang[key], ensure_ascii=False) + ');\n' for key in sorted(new_keys)) + '    }\n}\n', encoding='utf-8')
# The Java registrations are generated from the same roster to prevent data/entity mismatches.
java_path = ROOT / 'src/main/java/com/jjkcraft/curse/ExpandedCurseTypes.java'
java_path.write_text('''package com.jjkcraft.curse;

/** Original curse roster extensions; generated by tools/expand_content.py. */
public final class ExpandedCurseTypes {
    private ExpandedCurseTypes() { }
    public static void init() {
''' + ''.join(f'        CurseEntities.{"flying" if flying else "ground"}("{ident}", {0.8 if flying else 0.9}f, {1.2 if flying else 2.0}f);\n' for ident, flying in extra_types) + '    }\n}\n', encoding='utf-8')
print('Generated', len(FAMILIES), 'innate families and', len(NEW_CURSES), 'new original curses; damage masks and LOD assets for all curses.')
