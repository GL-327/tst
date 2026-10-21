"""Apply curated charge opt-ins after content generators; --check validates without writing."""
from pathlib import Path
import json, os, sys

ROOT = Path(__file__).resolve().parents[1]
TECHNIQUES = ROOT / 'src/main/resources/data/jjkcraft/jjkcraft/technique'
CHARGEABLE = {
    'lapse_blue', 'reversal_red', 'hollow_purple', 'maximum_output_blue', 'maximum_output_red',
    'piercing_blood', 'maximum_uzumaki', 'granite_blast', 'granite_cannon',
    'shrine_furnace', 'perfect_sphere', 'ice_lance', 'icefall', 'water_lance',
    'mul_lance', 'darkness_nova', 'chaos_fracture', 'eye_blast', 'volcanic_burst',
}

def save(path, value):
    temp = path.with_suffix('.writing')
    temp.write_text(json.dumps(value, indent=2, ensure_ascii=False) + '\n', encoding='utf-8', newline='\n')
    os.replace(temp, path)

def main():
    paths = {p.stem: p for p in TECHNIQUES.glob('*.json')}
    assert CHARGEABLE <= paths.keys(), CHARGEABLE - paths.keys()
    mismatches = []
    for name, path in paths.items():
        data = json.loads(path.read_text(encoding='utf-8'))
        expected = name in CHARGEABLE
        if data['cast'].get('chargeable', False) != expected:
            mismatches.append(name)
            if '--check' not in sys.argv:
                if expected:
                    data['cast']['chargeable'] = True
                else:
                    data['cast'].pop('chargeable', None)
                save(path, data)
    if '--check' in sys.argv:
        assert not mismatches, mismatches
    print(f'{len(CHARGEABLE)} chargeable moves; {len(paths) - len(CHARGEABLE)} use normal activation.')

if __name__ == '__main__':
    main()
