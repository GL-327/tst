"""Generates the Modulo expansion's art (docs/MODULO.md): block and item textures, two technique
icons, character and boss skins (64x64 player layout), Toranosuke's model, animation and texture
(the Divine Dog rig, recoloured), and the wancha octopus texture (squid layout).

All original programmer art. Canon characters get original looks; the only canon traits used are
species traits (a Simurian's third eye, a Deskunte's vertical eye and horns).

Usage: python tools/gen_modulo.py src/main/resources/assets/jjkcraft
"""

import json
import math
import os
import random
import shutil
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_items import noise_block, rgb, shade  # noqa: E402
from gen_textures import finish, icon_base, stroke  # noqa: E402

MUL = rgb("#7CF2A8")
MUL_DARK = rgb("#1F6B45")
HULL = rgb("#D9D8CF")


# ---------------------------------------------------------------- blocks (16x16)

def hull(seed, base, seam):
    img = noise_block(seed, base, shade(base, 0.92), shade(base, 1.05))
    px = img.load()
    for i in range(16):
        px[i, 0] = seam
        px[0, i] = seam
        px[i, 8] = seam if i % 4 else shade(seam, 0.8)
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        px[x, y] = shade(seam, 0.7)
    return img


def mul_ore():
    img = hull(31, HULL, rgb("#A9A79C"))
    px = img.load()
    rnd = random.Random(7)
    for _ in range(4):
        cx, cy = rnd.randint(3, 12), rnd.randint(3, 12)
        for dx, dy in ((0, 0), (1, 0), (0, 1), (-1, 0), (0, -1), (1, 1)):
            x, y = cx + dx, cy + dy
            if 0 <= x < 16 and 0 <= y < 16:
                px[x, y] = MUL if (dx + dy) % 2 == 0 else MUL_DARK
        px[cx, cy] = (220, 255, 232, 255)
    return img


def conduit():
    img = Image.new("RGBA", (16, 16), rgb("#23302A"))
    px = img.load()
    for y in range(16):
        for x in range(16):
            if 3 <= x <= 12 and 3 <= y <= 12:
                d = abs(x - 7.5) + abs(y - 7.5)
                px[x, y] = (220, 255, 232, 255) if d < 3 else (MUL if d < 6 else MUL_DARK)
            elif x in (0, 15) or y in (0, 15):
                px[x, y] = rgb("#3B4A43")
    return img


def hard_light():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            edge = min(x, y, 15 - x, 15 - y)
            v = 255 - edge * 6
            px[x, y] = (v, v, max(150, v - 60), 255)
    return img


def gate():
    img = Image.new("RGBA", (16, 16), rgb("#16241E"))
    px = img.load()
    for y in range(16):
        for x in range(16):
            a = math.atan2(y - 7.5, x - 7.5)
            r = math.hypot(x - 7.5, y - 7.5)
            if r < 7.5 and (math.sin(a * 3 + r * 0.9) > 0.2 or r < 2):
                px[x, y] = MUL if r > 2 else (230, 255, 240, 255)
    return img


# ---------------------------------------------------------------- items (16x16)

def canvas16():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    return img, ImageDraw.Draw(img), img.load()


def crystal(core, light, sparkle=False):
    img, d, px = canvas16()
    d.polygon([(8, 1), (12, 6), (11, 13), (8, 15), (5, 13), (4, 6)], fill=core, outline=shade(core, 0.55))
    d.polygon([(8, 1), (12, 6), (8, 8), (4, 6)], fill=light)
    d.line([(8, 8), (8, 15)], fill=shade(core, 0.75))
    if sparkle:
        for x, y in ((13, 2), (2, 10), (14, 12)):
            px[x, y] = (255, 255, 255, 255)
    return img


def blade(edge, hilt, guard, glow=None):
    img, d, px = canvas16()
    for i in range(10):
        px[4 + i, 11 - i] = edge
        px[5 + i, 11 - i] = shade(edge, 0.8)
        if glow is not None and i % 2 == 0:
            px[4 + i, 12 - i] = glow
    d.line([(2, 13), (4, 11)], fill=hilt, width=2)
    d.line([(2, 10), (6, 14)], fill=guard)
    return img


def rumelian_blade():
    img = blade(rgb("#D8E0DA"), rgb("#2E3B33"), rgb("#9AA79F"))
    px = img.load()
    # A forked tip, and Mul set in the hilt.
    px[13, 1] = (0, 0, 0, 0)
    px[15, 2] = rgb("#D8E0DA")
    px[12, 1] = rgb("#D8E0DA")
    px[3, 12] = MUL
    return img


def harness():
    img, d, px = canvas16()
    d.rectangle((2, 6, 13, 9), fill=rgb("#3A3530"))
    d.rectangle((2, 6, 13, 6), fill=rgb("#5A524A"))
    d.ellipse((5, 4, 10, 11), fill=MUL_DARK, outline=rgb("#1B1B1B"))
    d.ellipse((6, 5, 9, 10), fill=MUL)
    return img


def ward():
    img, d, px = canvas16()
    d.ellipse((1, 1, 14, 14), fill=rgb("#C9CABF"), outline=rgb("#6B6A61"))
    d.ellipse((4, 4, 11, 11), fill=MUL_DARK)
    d.ellipse((6, 6, 9, 9), fill=MUL)
    return img


def beacon():
    img, d, px = canvas16()
    d.rectangle((5, 6, 10, 14), fill=rgb("#D9D8CF"), outline=rgb("#6B6A61"))
    d.ellipse((5, 1, 10, 6), fill=MUL, outline=MUL_DARK)
    px[7, 3] = (240, 255, 245, 255)
    d.line([(6, 9), (9, 9)], fill=MUL_DARK)
    return img


def honoyagi():
    return blade(rgb("#E8E4DA"), rgb("#2A1A1A"), rgb("#B08040"), glow=rgb("#FF7A2E"))


def octopus():
    img, d, px = canvas16()
    d.ellipse((4, 2, 12, 9), fill=rgb("#B5473A"), outline=rgb("#6E241C"))
    d.line([(8, 9), (9, 11), (8, 13), (10, 15)], fill=rgb("#B5473A"), width=2)
    px[6, 5] = (240, 230, 200, 255)
    px[10, 5] = (240, 230, 200, 255)
    for x, y in ((9, 12), (9, 14)):
        px[x, y] = rgb("#F0C9B8")
    return img


def takoyaki():
    img, d, px = canvas16()
    d.polygon([(1, 11), (15, 11), (13, 14), (3, 14)], fill=rgb("#C9A86A"))
    for cx in (4, 8, 12):
        d.ellipse((cx - 3, 6, cx + 2, 11), fill=rgb("#B9762E"), outline=rgb("#6A3E14"))
        px[cx - 1, 7] = rgb("#E9D7A8")
        px[cx, 8] = rgb("#3B5A2A")
    return img


def cub():
    img, d, px = canvas16()
    d.ellipse((3, 4, 12, 13), fill=rgb("#E08A2E"), outline=rgb("#6E3E10"))
    d.polygon([(3, 5), (5, 2), (6, 5)], fill=rgb("#E08A2E"))
    d.polygon([(9, 5), (11, 2), (12, 5)], fill=rgb("#E08A2E"))
    for x in (5, 10):
        px[x, 8] = (20, 20, 20, 255)
    d.line([(7, 10), (8, 10)], fill=rgb("#F2E6D8"))
    for x in (4, 11):
        d.line([(x, 6), (x, 9)], fill=rgb("#2A1A0A"))
    return img


def cursed_object():
    img, d, px = canvas16()
    d.rounded_rectangle((4, 2, 11, 14), radius=3, fill=rgb("#2B2430"), outline=rgb("#120E16"))
    d.rectangle((5, 6, 10, 10), fill=rgb("#E9E3D5"))
    d.line([(6, 7), (9, 9)], fill=rgb("#B0283A"))
    d.line([(9, 7), (6, 9)], fill=rgb("#B0283A"))
    px[7, 3] = rgb("#5A4A66")
    return img


# ---------------------------------------------------------------- skins (64x64 player layout)

def character_skin(spec, seed):
    """A player-layout skin from a small spec: colours, hair, outfit, and marks."""
    rnd = random.Random(seed)
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    px = img.load()
    skin = spec["skin"]
    hair = spec["hair"]
    coat = spec["coat"]
    trim = spec.get("trim", tuple(min(255, int(v * 1.5)) for v in coat))
    under = spec.get("under", coat)
    trousers = spec.get("trousers", (36, 36, 44))
    shoes = spec.get("shoes", (20, 18, 20))

    def noisy(c, amount=0.05):
        k = 1 + rnd.uniform(-amount, amount)
        return tuple(max(0, min(255, int(v * k))) for v in c[:3]) + (255,)

    def box(u, v, w, h, d, face_fn):
        faces = [("top", u + d, v, w, d), ("bottom", u + d + w, v, w, d), ("right", u, v + d, d, h),
                 ("front", u + d, v + d, w, h), ("left", u + d + w, v + d, d, h), ("back", u + 2 * d + w, v + d, w, h)]
        for name, x0, y0, fw, fh in faces:
            for y in range(fh):
                for x in range(fw):
                    col = face_fn(name, x, y, fw, fh)
                    if col is not None:
                        px[x0 + x, y0 + y] = col

    def head(name, x, y, w, h):
        if name == "top":
            if spec.get("horns") and x in (1, 6) and y in (2, 3):
                return noisy((40, 30, 26))
            return noisy(hair) if not spec.get("bald") else noisy(skin)
        if name == "bottom":
            return noisy(skin)
        long_hair = spec.get("long_hair", False)
        if y < 2 or (name in ("left", "right") and y < (7 if long_hair else 4)) or name == "back":
            if name == "back" and y > 5 and not long_hair:
                return noisy(skin)
            return noisy(spec.get("hair_light", hair) if y == 0 else hair)
        if name == "front":
            eye = spec.get("third_eye")
            if eye == "flat" and y == 2 and x in (3, 4):
                return (240, 236, 220, 255) if x == 3 else (60, 160, 110, 255)
            if eye == "vertical" and x in (3, 4) and y in (1, 2, 3):
                return (220, 210, 80, 255) if y == 2 else (60, 40, 30, 255)
            if y == 4 and x in (1, 2, 5, 6):
                return (245, 245, 240, 255) if x in (1, 6) else spec.get("eyes", (30, 26, 34)) + (255,)
            if spec.get("glasses") and y == 4 and x in (0, 3, 4, 7):
                return (30, 30, 34, 255)
            if spec.get("stitches") and (x + y) % 3 == 0:
                return noisy((90, 40, 40))
            if y == 6 and 3 <= x <= 4:
                return noisy(tuple(int(v * 0.75) for v in skin))
            if spec.get("beard") and y >= 6:
                return noisy(hair)
        return noisy(skin)

    def hat(name, x, y, w, h):
        if spec.get("cap") and (name == "top" or (y < 2 and name != "bottom")):
            return noisy(spec["cap"])
        if spec.get("long_hair") and name in ("back", "left", "right") and y < 8:
            return noisy(hair)
        return None

    def body(name, x, y, w, h):
        if spec.get("stitches") and (x * 3 + y) % 5 == 0:
            return noisy((90, 40, 40))
        if spec.get("apron") and name == "front" and y >= 2:
            return noisy((236, 232, 222))
        if spec.get("open") and name == "front" and 2 <= x <= 5:
            return noisy(under)
        if name == "front" and x == 3 and y in (2, 5, 8) and spec.get("buttons", True):
            return noisy((200, 180, 110))
        if name in ("front", "back") and y == h - 1:
            return noisy(trim)
        if name == "front" and y == 0 and 2 <= x <= 5:
            return noisy(trim)
        return noisy(coat)

    def arm(name, x, y, w, h):
        if spec.get("stitches") and (x + y * 2) % 5 == 0:
            return noisy((90, 40, 40))
        if y >= h - 3 and name != "top":
            return noisy(skin)
        if y == h - 4 and name != "top":
            return noisy(trim)
        return noisy(coat)

    def leg(name, x, y, w, h):
        if y >= h - 2:
            return noisy(shoes)
        if spec.get("shorts") and y >= 5:
            return noisy(skin)
        return noisy(trousers)

    box(0, 0, 8, 8, 8, head)
    box(32, 0, 8, 8, 8, hat)
    box(16, 16, 8, 12, 4, body)
    box(40, 16, 4, 12, 4, arm)
    box(32, 48, 4, 12, 4, arm)
    box(0, 16, 4, 12, 4, leg)
    box(16, 48, 4, 12, 4, leg)
    return img


PALE = (238, 214, 190)
WARM = (220, 180, 146)
NAVY = (28, 32, 52)
CHARACTERS = {
    "maru": dict(skin=PALE, hair=(232, 226, 200), hair_light=(250, 246, 226), coat=(214, 222, 214), trim=(80, 160, 110), third_eye="flat",
                 eyes=(60, 120, 90), trousers=(60, 70, 64)),
    "cross": dict(skin=PALE, hair=(232, 226, 200), coat=(70, 80, 74), trim=(170, 60, 60), third_eye="flat", eyes=(60, 120, 90)),
    "jabaloma": dict(skin=WARM, hair=(30, 60, 50), coat=(30, 76, 58), trim=(200, 170, 90), third_eye="flat", buttons=False),
    "yuka": dict(skin=PALE, hair=(24, 22, 28), coat=NAVY, trim=(120, 90, 170), long_hair=True),
    "tsurugi": dict(skin=PALE, hair=(26, 24, 30), coat=NAVY, trim=(170, 120, 60)),
    "usami": dict(skin=WARM, hair=(40, 36, 36), coat=(70, 72, 80), trim=(40, 40, 46), open=True, under=(230, 228, 220)),
    "mino": dict(skin=PALE, hair=(56, 44, 36), coat=(120, 124, 132), trim=(70, 70, 80), glasses=True, open=True, under=(220, 226, 236)),
    "kyoko_tomoe": dict(skin=PALE, hair=(110, 70, 44), coat=(214, 120, 140), apron=True, long_hair=True, buttons=False),
    "osada": dict(skin=WARM, hair=(196, 192, 186), coat=(70, 86, 56), beard=True, buttons=False),
    "masayoshi": dict(skin=PALE, hair=(30, 26, 24), coat=(232, 232, 226), trim=(40, 50, 90), cap=(230, 190, 40), shorts=True, trousers=(40, 50, 90)),
    "dabura": dict(skin=(196, 170, 150), hair=(120, 30, 40), coat=(40, 26, 30), trim=(200, 170, 120), third_eye="vertical", horns=True,
                   trousers=(30, 24, 26), buttons=False),
    "trafficker": dict(skin=WARM, hair=(50, 46, 40), coat=(92, 84, 70), trim=(60, 54, 46), cap=(60, 64, 70)),
    "masaki_takeda": dict(skin=(214, 186, 160), hair=(190, 186, 180), coat=(48, 40, 44), trim=(140, 30, 40), beard=True, buttons=False),
    "skin_guardian": dict(skin=(214, 176, 150), hair=(214, 176, 150), bald=True, coat=(214, 176, 150), trim=(150, 90, 80), trousers=(204, 166, 140),
                          stitches=True, buttons=False, shoes=(150, 100, 90)),
    "mahoraga": dict(skin=(214, 210, 200), hair=(20, 20, 24), bald=False, coat=(26, 26, 30), trim=(200, 170, 90), trousers=(20, 20, 24),
                     buttons=False, eyes=(200, 170, 90)),
}


# ---------------------------------------------------------------- Toranosuke and the octopus

def toranosuke(assets):
    geckolib = os.path.join(assets, "geckolib")
    with open(os.path.join(geckolib, "models", "entity", "divine_dog.geo.json"), encoding="utf-8") as f:
        geo = json.load(f)
    geo["minecraft:geometry"][0]["description"]["identifier"] = "geometry.toranosuke"
    with open(os.path.join(geckolib, "models", "entity", "toranosuke.geo.json"), "w", encoding="utf-8") as f:
        json.dump(geo, f, indent=1)
    shutil.copyfile(os.path.join(geckolib, "animations", "entity", "divine_dog.animation.json"),
                    os.path.join(geckolib, "animations", "entity", "toranosuke.animation.json"))
    # The dog's texture, recoloured: orange fur, black stripes, a pale belly.
    src = Image.open(os.path.join(assets, "textures", "entity", "divine_dog.png")).convert("RGBA")
    out = Image.new("RGBA", src.size, (0, 0, 0, 0))
    sp, op = src.load(), out.load()
    for y in range(src.size[1]):
        for x in range(src.size[0]):
            r, g, b, a = sp[x, y]
            if a == 0:
                continue
            lum = (r + g + b) / 765
            stripe = math.sin((x + y * 0.4) * 0.9) > 0.55
            if lum > 0.7:
                op[x, y] = (240, 226, 206, a)
            elif stripe:
                op[x, y] = (26, 20, 16, a)
            else:
                k = 0.75 + lum * 0.6
                op[x, y] = (min(255, int(222 * k)), min(255, int(120 * k)), min(255, int(40 * k)), a)
    out.save(os.path.join(assets, "textures", "entity", "toranosuke.png"))


def wancha_octopus():
    rnd = random.Random(86)
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    px = img.load()
    for y in range(32):
        for x in range(64):
            base = (178, 72, 58) if rnd.random() > 0.15 else (140, 50, 42)
            if (x * 7 + y * 3) % 11 == 0:
                base = (238, 200, 182)
            px[x, y] = base + (255,)
    return img


# ---------------------------------------------------------------- technique icons (32x32)

def icon(color, glyph):
    img = icon_base(color)
    glyph(ImageDraw.Draw(img), color)
    return finish(img, 32)


def g_batto(d, c):
    stroke(d, [(28, 98), (60, 66), (98, 30)], (255, 236, 210), 7)
    for i in range(5):
        x, y = 40 + i * 12, 90 - i * 12
        d.ellipse((x - 6, y - 10, x + 6, y + 2), fill=c + (200,))


def g_lance(d, c):
    stroke(d, [(24, 104), (96, 32)], (220, 255, 232), 8)
    d.polygon([(96, 32), (78, 38), (90, 50)], fill=c + (255,))


def main(assets):
    blocks = os.path.join(assets, "textures", "block")
    items = os.path.join(assets, "textures", "item")
    skins = os.path.join(assets, "textures", "entity", "modulo")
    icons = os.path.join(assets, "textures", "gui", "technique")
    for folder in (blocks, items, skins, icons):
        os.makedirs(folder, exist_ok=True)

    hull(11, HULL, rgb("#A9A79C")).save(os.path.join(blocks, "naunax_hull.png"))
    hull(12, rgb("#9EA19B"), rgb("#6C706A")).save(os.path.join(blocks, "naunax_deck_plate.png"))
    mul_ore().save(os.path.join(blocks, "mul_ore.png"))
    conduit().save(os.path.join(blocks, "mul_conduit.png"))
    hard_light().save(os.path.join(blocks, "hard_light.png"))
    gate().save(os.path.join(blocks, "naunax_gate.png"))

    sprites = {
        "mul": crystal(MUL, (190, 255, 214, 255)),
        "high_purity_mul": crystal(rgb("#8CF6E0"), (230, 255, 250, 255), sparkle=True),
        "rumelian_blade": rumelian_blade(),
        "gravity_harness": harness(),
        "mul_ward": ward(),
        "naunax_beacon": beacon(),
        "honoyagi": honoyagi(),
        "wancha_octopus": octopus(),
        "takoyaki": takoyaki(),
        "tiger_cub": cub(),
        "yuji_cursed_object": cursed_object(),
    }
    for name, img in sprites.items():
        img.save(os.path.join(items, name + ".png"))

    for i, (name, spec) in enumerate(sorted(CHARACTERS.items())):
        character_skin(spec, 900 + i).save(os.path.join(skins, name + ".png"))
    wancha_octopus().save(os.path.join(skins, "wancha_octopus.png"))
    toranosuke(assets)

    icon((255, 122, 46), g_batto).save(os.path.join(icons, "batto_sword_drawing.png"))
    icon((124, 242, 168), g_lance).save(os.path.join(icons, "mul_lance.png"))
    print("Modulo art written to", assets)


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "src/main/resources/assets/jjkcraft")
