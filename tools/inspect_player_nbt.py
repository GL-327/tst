"""Read-only NBT inspector for the development profile (no third-party packages)."""
import gzip, io, json, struct, sys
from pathlib import Path

def read(path):
    data = Path(path).read_bytes()
    stream = io.BytesIO(gzip.decompress(data) if data[:2] == b'\x1f\x8b' else data)
    def number(fmt): return struct.unpack('>'+fmt, stream.read(struct.calcsize(fmt)))[0]
    def string(): return stream.read(number('H')).decode('utf8')
    def tag(kind):
        if kind in (1,2,3,4,5,6): return number({1:'b',2:'h',3:'i',4:'q',5:'f',6:'d'}[kind])
        if kind == 7: return list(stream.read(number('i')))
        if kind == 8: return string()
        if kind == 9:
            element, length = number('B'), number('i')
            return [tag(element) for _ in range(length)]
        if kind == 10:
            result = {}
            while (child := number('B')):
                name = string()
                result[name] = tag(child)
            return result
        if kind in (11,12): return [number('i' if kind==11 else 'q') for _ in range(number('i'))]
        raise ValueError(kind)
    kind = number('B'); string()
    return tag(kind)

if __name__ == '__main__':
    player = read(sys.argv[1])
    print(json.dumps({k:v for k,v in player.items() if k not in ('Inventory','EnderItems','recipeBook')}, indent=2))
