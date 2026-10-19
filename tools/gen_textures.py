"""Draws the mod's original GUI icons, particle sprites and effect icons.

Every image is generated from simple shapes, so the art is original and reproducible.
Usage: python tools/gen_textures.py <assets/jjkcraft directory>
"""
import json
import math
import os
import random
import sys

from PIL import Image, ImageDraw, ImageFilter

S = 4  # supersampling factor


def hexrgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def canvas(size):
    return Image.new("RGBA", (size * S, size * S), (0, 0, 0, 0))


def finish(img, size):
    return img.resize((size, size), Image.LANCZOS)


# ---------------------------------------------------------------- technique icons (32x32)

def icon_base(color):
    img = canvas(32)
    d = ImageDraw.Draw(img)
    # Ink tile with a slightly ragged rim in the technique color.
    d.rounded_rectangle((4, 4, 32 * S - 4, 32 * S - 4), radius=18, fill=(14, 12, 20, 255))
    rnd = random.Random(sum(color))
    for i in range(80):
        a = rnd.uniform(0, 2 * math.pi)
        r = 32 * S / 2 - rnd.uniform(4, 9)
        x = 64 + r * math.cos(a)
        y = 64 + r * math.sin(a)
        d.ellipse((x - 3, y - 3, x + 3, y + 3), fill=color + (110,))
    glow = Image.new("RGBA", img.size, (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse((28, 28, 100, 100), fill=color + (70,))
    return Image.alpha_composite(img, glow.filter(ImageFilter.GaussianBlur(14)))


def stroke(d, pts, color, width):
    d.line(pts, fill=color + (255,), width=width, joint="curve")
    for x, y in (pts[0], pts[-1]):
        d.ellipse((x - width / 2, y - width / 2, x + width / 2, y + width / 2), fill=color + (255,))


def g_strike(d, c):
    for k in range(8):
        a = k * math.pi / 4
        r1, r2 = 14, 44 if k % 2 == 0 else 34
        stroke(d, [(64 + r1 * math.cos(a), 64 + r1 * math.sin(a)), (64 + r2 * math.cos(a), 64 + r2 * math.sin(a))], c, 9)
    d.ellipse((50, 50, 78, 78), fill=c + (255,))


def g_bolt(d, c, curved=False):
    d.ellipse((70, 34, 98, 62), fill=c + (255,))
    if curved:
        pts = [(84 - 50 * t + 18 * math.sin(t * 3), 48 + 46 * t) for t in [i / 10 for i in range(11)]]
    else:
        pts = [(84 - 52 * t, 48 + 48 * t) for t in [i / 10 for i in range(11)]]
    for i in range(len(pts) - 1):
        w = int(16 * (1 - i / len(pts))) + 3
        d.line([pts[i], pts[i + 1]], fill=c + (int(255 * (1 - i / len(pts))) + 40,), width=w)


def g_crescent(d, c):
    d.pieslice((16, 16, 112, 112), 200, 340, fill=c + (255,))
    d.pieslice((26, 34, 102, 110), 190, 350, fill=(14, 12, 20, 255))


def g_line(d, c):
    stroke(d, [(24, 104), (104, 24)], c, 6)
    for k in range(6):
        a = k * math.pi / 3
        d.line([(104, 24), (104 + 12 * math.cos(a), 24 + 12 * math.sin(a))], fill=c + (255,), width=4)


def g_attract(d, c):
    for r in (40, 28, 16):
        d.arc((64 - r, 64 - r, 64 + r, 64 + r), 20, 300, fill=c + (255,), width=7)
    d.ellipse((58, 58, 70, 70), fill=c + (255,))


def g_repel(d, c):
    d.ellipse((50, 50, 78, 78), fill=c + (255,))
    for k in range(6):
        a = k * math.pi / 3 + 0.3
        x1, y1 = 64 + 22 * math.cos(a), 64 + 22 * math.sin(a)
        x2, y2 = 64 + 44 * math.cos(a), 64 + 44 * math.sin(a)
        stroke(d, [(x1, y1), (x2, y2)], c, 7)
        d.polygon([(x2 + 8 * math.cos(a), y2 + 8 * math.sin(a)),
                   (x2 + 8 * math.cos(a + 2.3), y2 + 8 * math.sin(a + 2.3)),
                   (x2 + 8 * math.cos(a - 2.3), y2 + 8 * math.sin(a - 2.3))], fill=c + (255,))


def g_merge(d, c):
    d.ellipse((22, 34, 82, 94), fill=(61, 139, 255, 220))
    d.ellipse((46, 34, 106, 94), fill=(255, 61, 78, 200))
    d.ellipse((46, 40, 82, 88), fill=c + (255,))


def g_command(d, c):
    d.ellipse((28, 50, 52, 78), fill=c + (255,))
    for r in (22, 34, 46):
        d.arc((40 - r, 64 - r, 40 + r, 64 + r), -45, 45, fill=c + (255,), width=7)


def g_command_arrow(down):
    def draw(d, c):
        g_command(d, c)
        x = 96
        y0, y1 = (30, 98) if down else (98, 30)
        stroke(d, [(x, y0), (x, y1)], c, 7)
        tip = y1
        sign = 1 if down else -1
        d.polygon([(x - 12, tip - 14 * sign), (x + 12, tip - 14 * sign), (x, tip + 4 * sign)], fill=c + (255,))
    return draw


def g_heal(d, c):
    d.rounded_rectangle((54, 26, 74, 102), radius=6, fill=c + (255,))
    d.rounded_rectangle((26, 54, 102, 74), radius=6, fill=c + (255,))


def g_chain(d, c):
    for i in range(4):
        x = 22 + i * 22
        y = 30 + i * 22
        if i % 2 == 0:
            d.ellipse((x, y, x + 34, y + 22), outline=c + (255,), width=7)
        else:
            d.ellipse((x + 6, y - 6, x + 28, y + 28), outline=c + (255,), width=7)


def g_barrier(d, c):
    pts = [(64 + 42 * math.cos(math.pi / 6 + k * math.pi / 3), 64 + 42 * math.sin(math.pi / 6 + k * math.pi / 3)) for k in range(6)]
    d.polygon(pts, outline=c + (255,), width=8)
    pts2 = [(64 + 24 * math.cos(math.pi / 6 + k * math.pi / 3), 64 + 24 * math.sin(math.pi / 6 + k * math.pi / 3)) for k in range(6)]
    d.polygon(pts2, fill=c + (120,))


def g_veil(d, c):
    for row in range(3):
        y = 40 + row * 24
        pts = [(20 + x, y + 8 * math.sin(x / 10 + row)) for x in range(0, 90, 4)]
        d.line(pts, fill=c + (255,), width=6)


def g_hound(d, c):
    # A hound's head in profile: ear, snout, eye.
    d.polygon([(30, 70), (52, 40), (62, 22), (70, 44), (100, 56), (104, 70), (80, 74), (70, 96), (44, 96)], fill=c + (255,))
    d.ellipse((66, 52, 76, 62), fill=(14, 12, 20, 255))


def g_crane(d, c):
    d.polygon([(64, 30), (78, 64), (64, 74), (50, 64)], fill=c + (255,))
    d.polygon([(64, 58), (112, 40), (78, 70)], fill=c + (220,))
    d.polygon([(64, 58), (16, 40), (50, 70)], fill=c + (220,))
    stroke(d, [(64, 34), (60, 18)], c, 5)


# Ten Shadows: Toad. Still draws the old tortoise icon until the Phase 3 art pass.
def g_tortoise(d, c):
    d.pieslice((22, 36, 106, 120), 180, 360, fill=c + (255,))
    for x in (40, 64, 88):
        stroke(d, [(x, 56), (x, 76)], (14, 12, 20), 4)
    d.ellipse((98, 62, 116, 80), fill=c + (255,))


def g_doll(d, c):
    d.ellipse((52, 20, 76, 44), fill=c + (255,))
    stroke(d, [(64, 44), (64, 84)], c, 10)
    stroke(d, [(36, 56), (92, 56)], c, 7)
    stroke(d, [(64, 84), (46, 108)], c, 7)
    stroke(d, [(64, 84), (82, 108)], c, 7)
    stroke(d, [(92, 24), (70, 62)], (230, 230, 240), 4)


def g_shape(d, c):
    d.ellipse((20, 44, 60, 84), outline=c + (255,), width=6)
    d.ellipse((66, 30, 114, 98), fill=c + (255,))
    stroke(d, [(54, 64), (70, 64)], c, 5)


def g_clock(d, c):
    d.ellipse((24, 24, 104, 104), outline=c + (255,), width=7)
    stroke(d, [(64, 64), (64, 36)], c, 7)
    stroke(d, [(64, 64), (86, 76)], c, 6)
    d.arc((14, 14, 114, 114), 300, 360, fill=c + (255,), width=5)


def g_meridian(d, c):
    stroke(d, [(16, 96), (112, 32)], c, 12)
    stroke(d, [(16, 96), (112, 32)], (255, 240, 240), 4)
    d.ellipse((8, 88, 26, 106), fill=c + (255,))


def g_rain(d, c):
    for k, x in enumerate((28, 46, 64, 82, 100)):
        y = 24 + (k % 2) * 14
        stroke(d, [(x, y), (x - 8, y + 44)], c, 6)
    d.ellipse((30, 96, 98, 112), outline=c + (255,), width=4)


def g_star(d, c):
    for r, a in ((46, 90), (34, 160), (22, 230)):
        d.ellipse((64 - r, 64 - r, 64 + r, 64 + r), outline=c[:3] + (a,), width=5)
    d.ellipse((50, 50, 78, 78), fill=(14, 12, 20, 255))
    d.ellipse((56, 56, 72, 72), outline=(230, 220, 255, 255), width=3)


def g_oath(d, c):
    stroke(d, [(28, 100), (100, 28)], c, 9)
    d.ellipse((36, 36, 92, 92), outline=c + (255,), width=5)
    stroke(d, [(44, 84), (84, 44)], (14, 12, 20), 3)


def g_step(d, c):
    for k, alpha in enumerate((90, 160, 255)):
        x = 28 + k * 26
        d.polygon([(x, 40), (x + 26, 64), (x, 88)], fill=c + (alpha,))


def g_fan(d, c):
    for k in range(5):
        a = math.radians(-50 + k * 25) - math.pi / 2
        stroke(d, [(64, 104), (64 + 70 * math.cos(a), 104 + 70 * math.sin(a))], c, 5)
    d.ellipse((56, 96, 72, 112), fill=c + (255,))


def g_quake(d, c):
    stroke(d, [(16, 92), (112, 92)], c, 6)
    for r in (16, 30, 44):
        d.arc((64 - r, 92 - r, 64 + r, 92 + r), 200, 340, fill=c + (255,), width=6)
    stroke(d, [(64, 92), (64, 28)], c, 8)


def g_flame(d, c):
    d.polygon([(64, 18), (88, 58), (84, 90), (64, 108), (44, 90), (40, 58)], fill=c + (255,))
    d.polygon([(64, 50), (76, 76), (64, 96), (52, 76)], fill=(255, 220, 140, 255))


def g_mend(d, c):
    d.rounded_rectangle((54, 30, 74, 98), radius=6, fill=c + (255,))
    d.rounded_rectangle((30, 54, 98, 74), radius=6, fill=c + (255,))
    d.arc((14, 14, 114, 114), 200, 320, fill=c + (255,), width=5)


GLYPHS = {
    "ink_iron_palm": g_strike,
    "grudge_bolt": g_bolt,
    "seeking_grudge_bolt": lambda d, c: g_bolt(d, c, True),
    "cleave": g_crescent,
    "dismantle": g_line,
    "lapse_blue": g_attract,
    "reversal_red": g_repel,
    "hollow_purple": g_merge,
    "cursed_speech_dont_move": g_command,
    "cursed_speech_crumble_away": g_command_arrow(True),
    "cursed_speech_blast_away": g_command_arrow(False),
    "reverse_cursed_technique": g_heal,
    "grave_chains": g_chain,
    "lacquer_ward": g_barrier,
    "infinity": g_veil,
    "ten_shadows_divine_dogs": g_hound,
    "ten_shadows_nue": g_crane,
    "ten_shadows_toad": g_tortoise,
    "straw_doll_resonance": g_doll,
    "idle_transfiguration": g_shape,
    "projection_sorcery": g_clock,
    "piercing_blood": g_meridian,
    "thousand_lances": g_rain,
    "maximum_output_blue": g_star,
    "oathbound_edge": g_oath,
    "ink_step": g_step,
    "splinter_volley": g_fan,
    "ground_toll": g_quake,
    "ember_skin": g_flame,
    "positive_energy_output": g_mend,
}


def technique_icons(assets, data_dir):
    out = os.path.join(assets, "textures", "gui", "technique")
    os.makedirs(out, exist_ok=True)
    for name in sorted(os.listdir(data_dir)):
        tid = name[:-5]
        with open(os.path.join(data_dir, name), encoding="utf-8") as f:
            color = hexrgb(json.load(f)["visuals"]["color"])
        img = icon_base(color)
        glyph = GLYPHS.get(tid, g_strike)
        glyph(ImageDraw.Draw(img), color)
        finish(img, 32).save(os.path.join(out, tid + ".png"))
        print("icon", tid)


# ---------------------------------------------------------------- particles (16x16, white so they tint)

def particle_frames(name, frames, draw):
    imgs = []
    for f in range(frames):
        img = canvas(16)
        draw(ImageDraw.Draw(img), f / max(1, frames - 1))
        imgs.append(finish(img, 16))
    return imgs


def p_spark(d, t):
    r = 26 * (1 - 0.6 * t) + 4
    w = 7 * (1 - 0.5 * t) + 2
    d.polygon([(32, 32 - r), (32 + w, 32), (32, 32 + r), (32 - w, 32)], fill=(255, 255, 255, 255))
    d.polygon([(32 - r, 32), (32, 32 - w), (32 + r, 32), (32, 32 + w)], fill=(255, 255, 255, 255))


def p_aura(d, t):
    r = 20 - 10 * t
    for k in range(6, 0, -1):
        a = int(255 * (k / 6) ** 2 * (1 - 0.5 * t))
        rr = r * (1 + (6 - k) * 0.15)
        d.ellipse((32 - rr, 32 - rr, 32 + rr, 32 + rr), fill=(255, 255, 255, a // 3))


def p_glow(d, t):
    r = 22 - 6 * t
    d.ellipse((32 - r, 32 - r, 32 + r, 32 + r), outline=(255, 255, 255, 255), width=5)
    d.ellipse((24, 24, 40, 40), fill=(255, 255, 255, 200))


def p_lightning(d, t):
    rnd = random.Random(int(t * 100) + 7)
    pts = [(32 + rnd.uniform(-14, 14), y) for y in range(2, 64, 8)]
    d.line(pts, fill=(255, 255, 255, 255), width=5)


def p_shard(d, t):
    a = t * math.pi
    pts = [(32 + 26 * math.cos(a + k * 2.1), 32 + 18 * math.sin(a + k * 2.1)) for k in range(3)]
    d.polygon(pts, fill=(255, 255, 255, 235))


def p_dissolve(d, t):
    rnd = random.Random(int(t * 10) + 3)
    for _ in range(5):
        x, y = rnd.uniform(10, 54), rnd.uniform(10, 54)
        r = rnd.uniform(3, 8) * (1 - 0.5 * t)
        d.rectangle((x - r, y - r, x + r, y + r), fill=(255, 255, 255, 230))


PARTICLES = {
    "cursed_spark": (4, p_spark),
    "ce_aura": (4, p_aura),
    "reverse_glow": (3, p_glow),
    "black_lightning": (4, p_lightning),
    "domain_shard": (4, p_shard),
    "dissolve": (3, p_dissolve),
}


def particles(assets):
    tex = os.path.join(assets, "textures", "particle")
    desc = os.path.join(assets, "particles")
    os.makedirs(tex, exist_ok=True)
    os.makedirs(desc, exist_ok=True)
    for name, (frames, draw) in PARTICLES.items():
        ids = []
        for i, img in enumerate(particle_frames(name, frames, draw)):
            img.save(os.path.join(tex, f"{name}_{i}.png"))
            ids.append(f"jjkcraft:{name}_{i}")
        with open(os.path.join(desc, name + ".json"), "w", encoding="utf-8") as f:
            json.dump({"textures": ids}, f, indent=2)
            f.write("\n")
        print("particle", name)


# ---------------------------------------------------------------- mob effect icons (18x18)

def effects(assets):
    out = os.path.join(assets, "textures", "mob_effect")
    os.makedirs(out, exist_ok=True)

    img = canvas(18)
    d = ImageDraw.Draw(img)
    for i in range(3):
        d.ellipse((8 + i * 18, 22 + (i % 2) * 8, 34 + i * 18, 44 + (i % 2) * 8), outline=(176, 141, 87, 255), width=6)
    finish(img, 18).save(os.path.join(out, "bound.png"))

    img = canvas(18)
    d = ImageDraw.Draw(img)
    for k in range(5):
        a = k * 2 * math.pi / 5 - math.pi / 2
        d.ellipse((36 + 22 * math.cos(a) - 6, 36 + 22 * math.sin(a) - 6, 36 + 22 * math.cos(a) + 6, 36 + 22 * math.sin(a) + 6),
                  fill=(233, 227, 213, 255))
    finish(img, 18).save(os.path.join(out, "stunned.png"))
    print("effects ok")


if __name__ == "__main__":
    assets = sys.argv[1]
    data_dir = os.path.join(os.path.dirname(os.path.dirname(assets)), "data", "jjkcraft", "jjkcraft", "technique")
    technique_icons(assets, data_dir)
    particles(assets)
    effects(assets)
