"""Generates the Culling Game art: the Kogane (GeckoLib model, animation, texture) and the
entrant skins (64x64, player-skin layout). All original.

Usage: python tools/gen_culling.py <assets/jjkcraft directory>
"""
import json
import os
import random
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_curses import Model, anims, c, kf, wave  # noqa: E402


def kogane():
    # A paper-mask lantern that floats at a colony's centre: a pale mask over a glowing paper
    # body, a ring of tally beads turning around it, and two strips of ledger paper hanging below.
    m = Model("kogane", {"body": ((236, 226, 196), (196, 150, 70)), "frame": ((54, 40, 34), (150, 110, 60)),
                              "mask": ((242, 236, 222), (30, 20, 24)), "bead": ((40, 30, 28), (212, 163, 90)),
                              "paper": ((226, 214, 184), (120, 90, 60))})
    m.bone("root", (0, 8, 0))
    m.bone("lantern", (0, 16, 0), "root", [c((-4, 8, -4), (8, 12, 8), "body", ["glow", "tally"]),
                                           c((-5, 20, -5), (10, 2, 10), "frame"), c((-5, 6, -5), (10, 2, 10), "frame")])
    m.bone("mask", (0, 15, -4), "lantern", [c((-3.5, 10, -5.5), (7, 8, 1), "mask", ["mask"])])
    m.bone("cap", (0, 22, 0), "lantern", [c((-2, 22, -2), (4, 3, 4), "frame"), c((-0.5, 25, -0.5), (1, 3, 1), "frame")])
    m.bone("ring", (0, 14, 0), "lantern", [c((-8, 13.5, -0.5), (1, 1, 1), "bead"), c((7, 13.5, -0.5), (1, 1, 1), "bead"),
                                           c((-0.5, 13.5, -8), (1, 1, 1), "bead"), c((-0.5, 13.5, 7), (1, 1, 1), "bead"),
                                           c((-6, 13.5, -6), (1, 1, 1), "bead"), c((5, 13.5, 5), (1, 1, 1), "bead"),
                                           c((5, 13.5, -6), (1, 1, 1), "bead"), c((-6, 13.5, 5), (1, 1, 1), "bead")])
    m.bone("strip_l", (-2, 6, 0), "lantern", [c((-3, -4, -0.5), (2, 10, 1), "paper", ["tally"])])
    m.bone("strip_r", (2, 6, 0), "lantern", [c((1, -2, -0.5), (2, 8, 1), "paper", ["tally"])])
    return m


def kogane_anims():
    a = anims("kogane", {"root": "root", "style": "floater", "extra": ["strip_l", "strip_r"], "head": ["mask"]})
    # The bead ring turns all the time.
    idle = a["animations"]["idle"]
    idle["bones"]["ring"] = {"rotation": kf([(0, [0, 0, 0]), (3, [0, 360, 0])])}
    idle["animation_length"] = 3
    return a


# ---------------------------------------------------------------- entrant skins

def entrant_skin(seed):
    """A 64x64 player-layout skin: hair, face, jacket or uniform, trousers, shoes. Varied by seed."""
    rnd = random.Random(seed)
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    px = img.load()

    def shade(rgb, k):
        return tuple(max(0, min(255, int(v * k))) for v in rgb[:3]) + (255,)

    def noisy(rgb, amount=0.06):
        return shade(rgb, 1 + rnd.uniform(-amount, amount))

    def box(u, v, w, h, d, face_fn):
        faces = [("top", u + d, v, w, d), ("bottom", u + d + w, v, w, d), ("right", u, v + d, d, h),
                 ("front", u + d, v + d, w, h), ("left", u + d + w, v + d, d, h), ("back", u + 2 * d + w, v + d, w, h)]
        for name, x0, y0, fw, fh in faces:
            for y in range(fh):
                for x in range(fw):
                    col = face_fn(name, x, y, fw, fh)
                    if col is not None:
                        px[x0 + x, y0 + y] = col

    skins = [(236, 200, 170), (214, 172, 136), (180, 132, 98), (120, 84, 60)]
    hairs = [(24, 20, 22), (60, 40, 30), (110, 74, 40), (200, 190, 180), (40, 44, 70), (140, 40, 40)]
    coats = [(30, 32, 44), (60, 60, 70), (120, 30, 36), (40, 70, 60), (180, 170, 150), (70, 50, 90), (34, 54, 92), (150, 110, 60)]
    skin = skins[rnd.randrange(len(skins))]
    hair = hairs[rnd.randrange(len(hairs))]
    coat = coats[seed % len(coats)]
    trim = shade(coat, 0.6)[:3] if sum(coat) > 300 else shade(coat, 1.6)[:3]
    trousers = (36, 36, 44) if rnd.random() < 0.6 else (70, 64, 56)
    shoes = (20, 18, 20)
    long_hair = rnd.random() < 0.4
    style = rnd.randrange(3)  # 0 uniform with buttons, 1 hoodie, 2 open jacket over a shirt

    def head(name, x, y, w, h):
        if name == "top":
            return noisy(hair)
        if name == "bottom":
            return noisy(skin)
        if y < 2 or (name in ("left", "right") and y < 5) or name == "back":
            if name == "back" and y > 5 and not long_hair:
                return noisy(skin)
            return noisy(hair)
        if name == "front":
            if y == 4 and x in (1, 2, 5, 6):
                return (245, 245, 240, 255) if x in (1, 6) else (30, 26, 34, 255)
            if y == 3 and x in (1, 2, 5, 6):
                return noisy(shade(hair, 0.9))
            if y == 6 and 3 <= x <= 4:
                return noisy(shade(skin, 0.75))
        return noisy(skin)

    def hat(name, x, y, w, h):
        if long_hair and name in ("back", "left", "right") and y < 7:
            return noisy(shade(hair, 0.95))
        if name == "front" and y == 0:
            return noisy(shade(hair, 0.95))
        return None

    def body(name, x, y, w, h):
        if style == 1 and name == "front" and 2 <= x <= 5 and 6 <= y <= 9:
            return noisy(shade(coat, 0.85))
        if style == 2 and name == "front" and 2 <= x <= 5:
            return noisy((230, 228, 220))
        if style == 0 and name == "front" and x == 3 and y in (2, 5, 8):
            return noisy((210, 190, 120))
        if name in ("front", "back") and y == h - 1:
            return noisy(trim)
        if name == "front" and y == 0 and 2 <= x <= 5:
            return noisy(trim)
        return noisy(coat)

    def arm(name, x, y, w, h):
        if y >= h - 3 and name != "top":
            return noisy(skin)
        if y == h - 4 and name != "top":
            return noisy(trim)
        return noisy(coat)

    def leg(name, x, y, w, h):
        if y >= h - 2:
            return noisy(shoes)
        return noisy(trousers)

    box(0, 0, 8, 8, 8, head)
    box(32, 0, 8, 8, 8, hat)
    box(16, 16, 8, 12, 4, body)
    box(40, 16, 4, 12, 4, arm)
    box(32, 48, 4, 12, 4, arm)
    box(0, 16, 4, 12, 4, leg)
    box(16, 48, 4, 12, 4, leg)
    return img


def main(assets):
    geo_dir = os.path.join(assets, "geckolib", "models", "entity")
    anim_dir = os.path.join(assets, "geckolib", "animations", "entity")
    tex_dir = os.path.join(assets, "textures", "entity")
    skin_dir = os.path.join(tex_dir, "entrant")
    for d in (geo_dir, anim_dir, tex_dir, skin_dir):
        os.makedirs(d, exist_ok=True)

    model = kogane()
    with open(os.path.join(geo_dir, "kogane.geo.json"), "w", encoding="utf-8") as f:
        json.dump(model.geo(), f, indent=1)
    with open(os.path.join(anim_dir, "kogane.animation.json"), "w", encoding="utf-8") as f:
        json.dump(kogane_anims(), f, indent=1)
    model.texture(4242).save(os.path.join(tex_dir, "kogane.png"))
    print("kogane", len(model.bones), "bones")

    for i in range(8):
        entrant_skin(9100 + i * 37).save(os.path.join(skin_dir, f"entrant_{i}.png"))
    print("entrant skins 8")


if __name__ == "__main__":
    main(sys.argv[1])
