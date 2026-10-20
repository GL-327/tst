"""Generates the original curse models, animations and textures for GeckoLib.

Each curse is described as a set of bones made of boxes. The script writes:
  assets/jjkcraft/geckolib/models/entity/<id>.geo.json        (Bedrock geometry, box UV)
  assets/jjkcraft/geckolib/animations/entity/<id>.animation.json
  assets/jjkcraft/textures/entity/<id>.png                       (painted per face)
Every curse gets idle, walk, attack, cast, hurt, spawn and death animations. Animations are
built from small motion recipes so each silhouette moves differently.

Usage: python tools/gen_curses.py <assets/jjkcraft directory>
"""
import json
import math
import os
import random
import sys

from PIL import Image, ImageDraw

TEX = 128


# ---------------------------------------------------------------- model building

class Model:
    def __init__(self, name, palette):
        self.name = name
        self.palette = palette  # dict of part kind -> (base rgb, accent rgb)
        self.bones = []
        self.u = 0
        self.v = 0
        self.row_h = 0
        self.faces = []  # (cube uv origin, size, kind, features)

    def alloc(self, w, h, d):
        """Box-UV allocation: a cube needs (2d + 2w) x (d + h) texels."""
        bw, bh = 2 * d + 2 * w, d + h
        if self.u + bw > TEX:
            self.u = 0
            self.v += self.row_h
            self.row_h = 0
        uv = (self.u, self.v)
        self.u += bw
        self.row_h = max(self.row_h, bh)
        if self.v + bh > TEX:
            raise ValueError(f"{self.name}: texture atlas overflow")
        return uv

    def bone(self, name, pivot, parent=None, cubes=(), rotation=None):
        bone = {"name": name, "pivot": list(pivot)}
        if parent:
            bone["parent"] = parent
        if rotation:
            bone["rotation"] = list(rotation)
        out = []
        for c in cubes:
            origin, size = c[0], c[1]
            kind = c[2] if len(c) > 2 else "body"
            features = c[3] if len(c) > 3 else ()
            w, h, d = (max(1, int(round(s))) for s in size)
            uv = self.alloc(w, h, d)
            cube = {"origin": list(origin), "size": list(size), "uv": list(uv)}
            if len(c) > 4:
                cube["inflate"] = c[4]
            out.append(cube)
            self.faces.append((uv, (w, h, d), kind, features))
        if out:
            bone["cubes"] = out
        self.bones.append(bone)
        return name

    def geo(self):
        return {
            "format_version": "1.12.0",
            "minecraft:geometry": [{
                "description": {
                    "identifier": f"geometry.{self.name}",
                    "texture_width": TEX,
                    "texture_height": TEX,
                    "visible_bounds_width": 4,
                    "visible_bounds_height": 4,
                    "visible_bounds_offset": [0, 1.5, 0],
                },
                "bones": self.bones,
            }],
        }

    def texture(self, seed):
        rnd = random.Random(seed)
        img = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        for (u, v), (w, h, dd), kind, features in self.faces:
            base, accent = self.palette.get(kind, self.palette["body"])
            regions = {
                "top": (u + dd, v, w, dd), "bottom": (u + dd + w, v, w, dd),
                "east": (u, v + dd, dd, h), "north": (u + dd, v + dd, w, h),
                "west": (u + dd + w, v + dd, dd, h), "south": (u + 2 * dd + w, v + dd, w, h),
            }
            for face, (x, y, fw, fh) in regions.items():
                shade = {"top": 1.18, "bottom": 0.64, "north": 1.0, "south": 0.83, "east": 0.77, "west": 0.91}[face]
                for px in range(fw):
                    for py in range(fh):
                        # Broad painted planes, restrained stepped shading and material seams.
                        # Independent bright noise used to obscure the face and silhouette.
                        edge=min(px,fw-1-px,py,fh-1-py)
                        bevel=0.78 if edge==0 else 1.05 if edge==1 else 1.0
                        grad=1.08-0.22*(py/max(1,fh-1))
                        bands=0.96 if (py//3+px//6)%5==0 else 1.0
                        lift=10 if max(base)<90 else 0
                        col=tuple(max(0,min(255,int((c+lift)*shade*grad*bevel*bands)//4*4)) for c in base)
                        img.putpixel((x + px, y + py), col + (255,))
                if fw>4 and fh>5:
                    seam=tuple(max(0,int(c*0.5)) for c in base)+(255,)
                    d.line((x+1,y+fh-2,x+fw-2,y+fh-2),fill=seam)
                    if kind in ('bone','mask','horn'):
                        d.line((x+fw//2,y+2,x+fw//2-1,y+fh-3),fill=seam)
                    elif kind in ('cloth','paper'):
                        for fold in range(2,fw-1,4):d.line((x+fold,y+2,x+fold,y+fh-3),fill=seam)
            fx, fy, fw, fh = regions["north"]
            for feature in features:
                draw_feature(d, feature, fx, fy, fw, fh, accent, rnd)
        return img


def draw_feature(d, feature, x, y, w, h, accent, rnd):
    if feature == "eyes":
        ey = y + max(1, h // 3)
        for ex in (x + w // 4, x + w - 1 - w // 4):
            d.rectangle((ex-1,ey-1,ex+1,ey+1),fill=(12,10,18,255))
            d.point((ex,ey),fill=accent+(255,))
            if w>8:d.point((ex-1,ey),fill=(248,232,196,255))
    elif feature == "many_eyes":
        for _ in range(max(3, w * h // 10)):
            ex, ey = x + rnd.randrange(max(1, w)), y + rnd.randrange(max(1, h))
            d.point((ex, ey), fill=(240, 220, 120, 255))
    elif feature == "mouth":
        my = y + h - max(2, h // 3)
        d.rectangle((x + 1, my, x + w - 2, my + max(1, h // 6)), fill=(20, 4, 8, 255))
        for tx in range(x + 1, x + w - 1, 2):
            d.point((tx, my), fill=(235, 230, 210, 255))
            if h>7:d.point((tx+1,my+max(1,h//6)),fill=(194,179,157,255))
    elif feature == "glow":
        d.rectangle((x + w // 2 - 1, y + h // 2 - 1, x + w // 2 + 1, y + h // 2 + 1), fill=accent + (255,))
    elif feature == "mask":
        d.rectangle((x, y, x + w - 1, y + h - 1), fill=(232, 226, 210, 255))
        for ex in (x + w // 4, x + w - 1 - w // 4):
            d.ellipse((ex - 1, y + h // 3 - 1, ex + 1, y + h // 3 + 1), fill=(15, 10, 15, 255))
        d.ellipse((x + w // 2 - 2, y + h - h // 3 - 1, x + w // 2 + 2, y + h - h // 3 + 2), fill=(15, 10, 15, 255))
    elif feature == "tally":
        for i in range(0, w - 1, 2):
            d.line((x + i, y + 1, x + i, y + h - 2), fill=(230, 220, 190, 255))
    elif feature == "crack":
        cx = x + w // 2
        for i in range(h):
            d.point((cx + (i % 3) - 1, y + i), fill=(15, 10, 10, 255))
    elif feature == "saint":
        d.rectangle((x, y, x + w - 1, y + h - 1), fill=(225, 222, 214, 255))
        d.rectangle((x + 1, y + h // 3, x + w - 2, y + h // 3 + 1), fill=(20, 18, 20, 255))


# ---------------------------------------------------------------- animation recipes

def kf(times_values):
    return {f"{t:.4g}": [round(v, 3) for v in val] for t, val in times_values}


def wave(length, amp, axis=0, phase=0.0, steps=8, offset=(0, 0, 0)):
    out = []
    for i in range(steps + 1):
        t = length * i / steps
        v = list(offset)
        v[axis] += amp * math.sin(2 * math.pi * (i / steps) + phase)
        out.append((t, v))
    return kf(out)


def anims(name, rig):
    """rig: dict with role -> list of bone names, plus 'style'."""
    A = {}
    style = rig.get("style", "walker")
    body = rig.get("body", [])
    legs = rig.get("legs", [])
    arms = rig.get("arms", [])
    head = rig.get("head", [])
    extra = rig.get("extra", [])
    root = rig["root"]

    def anim(key, length, bones, loop=True, hold=False):
        a = {"animation_length": length, "bones": bones}
        if loop:
            a["loop"] = True
        elif hold:
            a["loop"] = "hold_on_last_frame"
        A[key] = a

    # idle
    bones = {}
    if style == "floater":
        bones[root] = {"position": wave(3, 1.6, axis=1)}
    for b in body:
        bones.setdefault(b, {})["rotation"] = wave(3, 3, axis=0)
    for i, b in enumerate(head):
        bones.setdefault(b, {})["rotation"] = wave(4, 6, axis=1, phase=i)
    for i, b in enumerate(extra):
        bones.setdefault(b, {})["rotation"] = wave(2 + i % 2, 12, axis=2, phase=i * 0.7)
    for i, b in enumerate(arms):
        bones.setdefault(b, {})["rotation"] = wave(3, 4, axis=2, phase=i * math.pi)
    anim("idle", 4 if style != "floater" else 3, bones)

    # walk
    bones = {}
    for i, b in enumerate(legs):
        bones[b] = {"rotation": wave(1, 32, axis=0, phase=i * math.pi)}
    for i, b in enumerate(arms):
        bones[b] = {"rotation": wave(1, 24, axis=0, phase=(i + 1) * math.pi)}
    for b in body:
        bones.setdefault(b, {})["position"] = wave(0.5, 0.6, axis=1)
    if style == "floater":
        bones[root] = {"position": wave(1, 1.0, axis=1), "rotation": kf([(0, [12, 0, 0]), (1, [12, 0, 0])])}
    if style == "crawler":
        for i, b in enumerate(extra):
            bones.setdefault(b, {})["rotation"] = wave(0.6, 20, axis=1, phase=i)
    anim("walk", 1, bones)

    # attack: windup then strike
    bones = {}
    strikers = arms or head or body
    for b in strikers:
        bones[b] = {"rotation": kf([(0, [0, 0, 0]), (0.2, [-70, 0, 0]), (0.35, [40, 0, 0]), (0.6, [0, 0, 0])])}
    for b in body:
        bones.setdefault(b, {})["rotation"] = kf([(0, [0, 0, 0]), (0.2, [-8, 0, 0]), (0.35, [14, 0, 0]), (0.6, [0, 0, 0])])
    anim("attack", 0.6, bones, loop=False)

    # cast: rise and spread
    bones = {root: {"position": kf([(0, [0, 0, 0]), (0.3, [0, 2, 0]), (0.8, [0, 2, 0]), (1.0, [0, 0, 0])])}}
    for i, b in enumerate(arms):
        side = -1 if i % 2 == 0 else 1
        bones[b] = {"rotation": kf([(0, [0, 0, 0]), (0.3, [-40, 0, side * 60]), (0.8, [-40, 0, side * 60]), (1.0, [0, 0, 0])])}
    for b in head:
        bones.setdefault(b, {})["rotation"] = kf([(0, [0, 0, 0]), (0.3, [-25, 0, 0]), (0.8, [-25, 0, 0]), (1, [0, 0, 0])])
    for i, b in enumerate(extra):
        bones.setdefault(b, {})["scale"] = kf([(0, [1, 1, 1]), (0.4, [1.25, 1.25, 1.25]), (1, [1, 1, 1])])
    anim("cast", 1.0, bones, loop=False)

    # hurt: recoil
    bones = {root: {"rotation": kf([(0, [0, 0, 0]), (0.08, [-14, 0, 6]), (0.3, [0, 0, 0])])}}
    anim("hurt", 0.3, bones, loop=False)

    # spawn: emerge from the ground
    bones = {root: {
        "position": kf([(0, [0, -28, 0]), (0.9, [0, -4, 0]), (1.2, [0, 1, 0]), (1.4, [0, 0, 0])]),
        "scale": kf([(0, [0.6, 0.6, 0.6]), (1.0, [1, 1, 1])]),
    }}
    anim("spawn", 1.4, bones, loop=False, hold=True)

    # death: sag and fall apart (the client also fades and sheds ash)
    bones = {root: {
        "rotation": kf([(0, [0, 0, 0]), (0.6, [24, 0, -12]), (1.0, [70, 0, -20])]),
        "position": kf([(0, [0, 0, 0]), (1.0, [0, -6, 0])]),
        "scale": kf([(0, [1, 1, 1]), (1.0, [0.85, 0.7, 0.85])]),
    }}
    for i, b in enumerate(arms + legs + extra):
        bones[b] = {"rotation": kf([(0, [0, 0, 0]), (1.0, [(-1) ** i * 40, 0, (-1) ** i * 30])])}
    anim("death", 1.0, bones, loop=False, hold=True)

    return {"format_version": "1.8.0", "animations": A}


# ---------------------------------------------------------------- the curses

def c(o, s, kind="body", features=(), inflate=None):
    t = (o, s, kind, tuple(features))
    return t + ((inflate,) if inflate is not None else ())


def hollow_hymnal():
    m = Model("hollow_hymnal", {"body": ((60, 48, 72), (190, 160, 220)), "mask": ((232, 226, 210), (120, 30, 40)),
                                "cloth": ((40, 30, 50), (150, 110, 190))})
    m.bone("root", (0, 12, 0), cubes=[])
    m.bone("mask", (0, 20, 0), "root", [c((-6, 14, -2), (12, 14, 4), "mask", ["mask"])])
    m.bone("hood", (0, 20, 0), "mask", [c((-7, 13, 1), (14, 16, 6), "cloth")])
    for i, x in enumerate((-9, -5, 5, 9)):
        m.bone(f"veil{i}", (x * 0.6, 13, 2), "hood", [c((x * 0.6 - 1.5, 2, 1), (3, 11, 2), "cloth")])
    m.bone("throat", (0, 14, -2), "mask", [c((-2, 10, -3), (4, 4, 2), "body", ["mouth"])])
    return m, {"root": "root", "style": "floater", "head": ["mask"], "extra": ["veil0", "veil1", "veil2", "veil3"], "body": ["hood"]}


def ledger_crawler():
    m = Model("ledger_crawler", {"body": ((70, 58, 40), (200, 190, 150)), "limb": ((50, 40, 30), (150, 120, 80)),
                                 "page": ((215, 205, 175), (90, 70, 50))})
    m.bone("root", (0, 0, 0))
    m.bone("body", (0, 6, 0), "root", [c((-5, 4, -8), (10, 5, 16), "body", ["tally"])])
    m.bone("head", (0, 7, -8), "body", [c((-3, 4, -13), (6, 5, 5), "page", ["many_eyes"])])
    legs = []
    for i, (x, z) in enumerate([(-5, -6), (5, -6), (-5, 0), (5, 0), (-5, 6), (5, 6)]):
        side = -1 if x < 0 else 1
        n = m.bone(f"leg{i}", (x, 6, z), "body", [c((x + (side - 1) * 3 if side < 0 else x, 1, z - 1), (6, 2, 2), "limb")])
        legs.append(n)
    m.bone("spine", (0, 9, 4), "body", [c((-2, 9, 0), (4, 6, 2), "page", ["tally"])])
    return m, {"root": "root", "style": "crawler", "legs": legs, "head": ["head"], "body": ["body"], "extra": ["spine"]}


def wick_moth():
    m = Model("wick_moth", {"body": ((40, 32, 28), (255, 170, 60)), "wing": ((70, 52, 40), (255, 200, 120))})
    m.bone("root", (0, 10, 0))
    m.bone("thorax", (0, 10, 0), "root", [c((-2, 8, -4), (4, 4, 9), "body", ["glow"])])
    m.bone("head", (0, 10, -4), "thorax", [c((-2, 8, -7), (4, 4, 3), "body", ["eyes"])])
    m.bone("wing_l", (-2, 11, 0), "thorax", [c((-12, 11, -4), (10, 1, 9), "wing", ["many_eyes"])])
    m.bone("wing_r", (2, 11, 0), "thorax", [c((2, 11, -4), (10, 1, 9), "wing", ["many_eyes"])])
    m.bone("wick", (0, 12, 2), "thorax", [c((-0.5, 12, 1), (1, 4, 1), "body", ["glow"])])
    return m, {"root": "root", "style": "floater", "head": ["head"], "extra": ["wing_l", "wing_r"], "body": ["thorax"]}


def pale_stilt():
    m = Model("pale_stilt", {"body": ((222, 218, 206), (90, 80, 100)), "limb": ((200, 195, 182), (70, 60, 80))})
    m.bone("root", (0, 0, 0))
    m.bone("torso", (0, 38, 0), "root", [c((-3, 38, -2), (6, 10, 4), "body")])
    m.bone("head", (0, 48, 0), "torso", [c((-2, 48, -2), (4, 7, 4), "body", ["eyes"])])
    m.bone("leg_l", (-2, 38, 0), "torso", [c((-3, 0, -1), (2, 38, 2), "limb")])
    m.bone("leg_r", (2, 38, 0), "torso", [c((1, 0, -1), (2, 38, 2), "limb")])
    m.bone("arm_l", (-3, 47, 0), "torso", [c((-5, 24, -1), (2, 23, 2), "limb")])
    m.bone("arm_r", (3, 47, 0), "torso", [c((3, 24, -1), (2, 23, 2), "limb")])
    return m, {"root": "root", "style": "walker", "legs": ["leg_l", "leg_r"], "arms": ["arm_l", "arm_r"], "head": ["head"], "body": ["torso"]}


def echo_mimic():
    m = Model("echo_mimic", {"body": ((40, 44, 60), (120, 200, 230)), "limb": ((30, 34, 48), (90, 160, 200))})
    m.bone("root", (0, 0, 0))
    m.bone("torso", (0, 12, 0), "root", [c((-4, 12, -2), (8, 12, 4), "body")])
    m.bone("head", (0, 24, 0), "torso", [c((-4, 24, -4), (8, 8, 8), "body", ["mouth"])])
    m.bone("face2", (0, 28, 0), "head", [c((-3, 26, -5), (6, 5, 1), "limb", ["eyes"])])
    m.bone("leg_l", (-2, 12, 0), "torso", [c((-4, 0, -2), (4, 12, 4), "limb")])
    m.bone("leg_r", (2, 12, 0), "torso", [c((0, 0, -2), (4, 12, 4), "limb")])
    m.bone("arm_l", (-5, 22, 0), "torso", [c((-8, 10, -2), (4, 12, 4), "limb")])
    m.bone("arm_r", (5, 22, 0), "torso", [c((4, 10, -2), (4, 12, 4), "limb")])
    return m, {"root": "root", "style": "walker", "legs": ["leg_l", "leg_r"], "arms": ["arm_l", "arm_r"], "head": ["head"], "extra": ["face2"], "body": ["torso"]}


def salt_lantern():
    m = Model("salt_lantern", {"body": ((210, 214, 220), (255, 250, 200)), "cage": ((60, 64, 70), (230, 230, 240))})
    m.bone("root", (0, 0, 0))
    m.bone("post", (0, 0, 0), "root", [c((-1.5, 0, -1.5), (3, 14, 3), "cage")])
    m.bone("lamp", (0, 16, 0), "post", [c((-4, 14, -4), (8, 9, 8), "body", ["glow"])])
    m.bone("cap", (0, 23, 0), "lamp", [c((-5, 23, -5), (10, 2, 10), "cage")])
    for i, (x, z) in enumerate([(-4, -4), (4, -4), (-4, 4), (4, 4)]):
        m.bone(f"bar{i}", (x, 18, z), "lamp", [c((x - 0.5, 14, z - 0.5), (1, 9, 1), "cage")])
    m.bone("foot_l", (-2, 0, 0), "post", [c((-4, 0, -2), (3, 2, 4), "cage")])
    m.bone("foot_r", (2, 0, 0), "post", [c((1, 0, -2), (3, 2, 4), "cage")])
    return m, {"root": "root", "style": "walker", "legs": ["foot_l", "foot_r"], "head": ["lamp"], "extra": ["cap"], "body": ["post"]}


def tidewash():
    m = Model("tidewash", {"body": ((30, 70, 90), (140, 220, 230)), "limb": ((24, 56, 72), (110, 190, 210))})
    m.bone("root", (0, 0, 0))
    m.bone("mass", (0, 6, 0), "root", [c((-7, 0, -6), (14, 12, 12), "body", ["many_eyes"])])
    m.bone("crest", (0, 12, 0), "mass", [c((-5, 12, -4), (10, 6, 8), "body", ["mouth"])])
    m.bone("arm_l", (-7, 10, 0), "mass", [c((-24, 8, -1.5), (17, 3, 3), "limb")])
    m.bone("arm_r", (7, 10, 0), "mass", [c((7, 8, -1.5), (17, 3, 3), "limb")])
    m.bone("hand_l", (-24, 9, 0), "arm_l", [c((-28, 6, -3), (4, 6, 6), "limb")])
    m.bone("hand_r", (24, 9, 0), "arm_r", [c((24, 6, -3), (4, 6, 6), "limb")])
    return m, {"root": "root", "style": "walker", "arms": ["arm_l", "arm_r"], "head": ["crest"], "extra": ["hand_l", "hand_r"], "body": ["mass"]}


def thorn_matriarch():
    m = Model("thorn_matriarch", {"body": ((52, 64, 36), (180, 40, 60)), "bark": ((70, 52, 36), (130, 160, 80))})
    m.bone("root", (0, 0, 0))
    m.bone("trunk", (0, 0, 0), "root", [c((-5, 0, -4), (10, 20, 8), "bark")])
    m.bone("head", (0, 20, 0), "trunk", [c((-4, 20, -4), (8, 8, 8), "body", ["eyes", "mouth"])])
    m.bone("crown", (0, 28, 0), "head", [c((-6, 28, -6), (12, 4, 12), "body", ["many_eyes"])])
    m.bone("arm_l", (-5, 18, 0), "trunk", [c((-9, 4, -2), (4, 14, 4), "bark")])
    m.bone("arm_r", (5, 18, 0), "trunk", [c((5, 4, -2), (4, 14, 4), "bark")])
    for i, a in enumerate((0, 1.6, 3.2, 4.7)):
        x, z = 6 * math.cos(a), 6 * math.sin(a)
        m.bone(f"root{i}", (x, 1, z), "trunk", [c((x - 1, 0, z - 1), (2, 3, 6), "bark")])
    return m, {"root": "root", "style": "walker", "arms": ["arm_l", "arm_r"], "head": ["head"], "extra": ["crown", "root0", "root1"], "body": ["trunk"]}


def cracked_bell():
    m = Model("cracked_bell", {"body": ((150, 110, 50), (60, 40, 20)), "bronze": ((120, 86, 40), (230, 190, 110))})
    m.bone("root", (0, 0, 0))
    m.bone("bell", (0, 26, 0), "root", [c((-8, 6, -8), (16, 20, 16), "body", ["crack", "eyes"])])
    m.bone("rim", (0, 6, 0), "bell", [c((-9, 4, -9), (18, 2, 18), "bronze")])
    m.bone("clapper", (0, 14, 0), "bell", [c((-2, 0, -2), (4, 8, 4), "bronze", ["glow"])])
    m.bone("crown", (0, 26, 0), "bell", [c((-3, 26, -3), (6, 4, 6), "bronze")])
    return m, {"root": "root", "style": "floater", "head": ["bell"], "extra": ["clapper", "rim"], "body": ["crown"]}


def redacted_saint():
    # Humanoid on the player-skin layout so it can wear a disguise: head, body, arms, legs at the
    # standard 64x64 skin UVs. Its own texture is a robed, faceless figure.
    m = Model("redacted_saint", {"body": ((225, 222, 214), (30, 26, 30)), "robe": ((200, 196, 188), (120, 20, 30))})
    m.bone("root", (0, 0, 0))
    m.bone("body", (0, 24, 0), "root", [c((-4, 12, -2), (8, 12, 4), "robe")])
    m.bone("head", (0, 24, 0), "body", [c((-4, 24, -4), (8, 8, 8), "body", ["saint"])])
    m.bone("arm_r", (-5, 22, 0), "body", [c((-8, 12, -2), (4, 12, 4), "robe")])
    m.bone("arm_l", (5, 22, 0), "body", [c((4, 12, -2), (4, 12, 4), "robe")])
    m.bone("leg_r", (-1.9, 12, 0), "body", [c((-3.9, 0, -2), (4, 12, 4), "robe")])
    m.bone("leg_l", (1.9, 12, 0), "body", [c((-0.1, 0, -2), (4, 12, 4), "robe")])
    return m, {"root": "root", "style": "walker", "legs": ["leg_l", "leg_r"], "arms": ["arm_l", "arm_r"], "head": ["head"], "body": ["body"]}


def ash_oni():
    m = Model("ash_oni", {"body": ((70, 40, 36), (255, 120, 40)), "horn": ((220, 210, 190), (60, 40, 30)), "ash": ((50, 46, 46), (200, 80, 30))})
    m.bone("root", (0, 0, 0))
    m.bone("torso", (0, 18, 0), "root", [c((-7, 18, -4), (14, 14, 8), "body", ["glow"])])
    m.bone("head", (0, 32, -1), "torso", [c((-4, 32, -5), (8, 7, 7), "body", ["eyes", "mouth"])])
    m.bone("horn_l", (-3, 39, -2), "head", [c((-5, 39, -3), (2, 5, 2), "horn")])
    m.bone("horn_r", (3, 39, -2), "head", [c((3, 39, -3), (2, 5, 2), "horn")])
    m.bone("arm_l", (-7, 30, 0), "torso", [c((-12, 12, -3), (5, 18, 6), "ash")])
    m.bone("arm_r", (7, 30, 0), "torso", [c((7, 12, -3), (5, 18, 6), "ash")])
    m.bone("leg_l", (-3, 18, 0), "torso", [c((-6, 0, -3), (5, 18, 6), "ash")])
    m.bone("leg_r", (3, 18, 0), "torso", [c((1, 0, -3), (5, 18, 6), "ash")])
    return m, {"root": "root", "style": "walker", "legs": ["leg_l", "leg_r"], "arms": ["arm_l", "arm_r"], "head": ["head"], "body": ["torso"]}


def gravebound_gate():
    m = Model("gravebound_gate", {"body": ((60, 58, 66), (170, 40, 60)), "stone": ((96, 92, 100), (200, 190, 170))})
    m.bone("root", (0, 0, 0))
    m.bone("pillar_l", (-12, 0, 0), "root", [c((-16, 0, -4), (8, 34, 8), "stone", ["many_eyes"])])
    m.bone("pillar_r", (12, 0, 0), "root", [c((8, 0, -4), (8, 34, 8), "stone", ["many_eyes"])])
    m.bone("lintel", (0, 34, 0), "root", [c((-20, 34, -5), (40, 6, 10), "stone")])
    m.bone("maw", (0, 18, 0), "root", [c((-8, 4, -2), (16, 26, 2), "body", ["eyes", "mouth"])])
    m.bone("chain_l", (-8, 30, 0), "lintel", [c((-9, 14, -1), (2, 16, 2), "body")])
    m.bone("chain_r", (8, 30, 0), "lintel", [c((7, 14, -1), (2, 16, 2), "body")])
    return m, {"root": "root", "style": "golem", "legs": ["pillar_l", "pillar_r"], "head": ["maw"], "extra": ["chain_l", "chain_r"], "body": ["lintel"]}


def saint_disguise_geo():
    """The Redacted Saint's disguise: same bones as its true form, on the 64x64 player skin layout."""
    def cube(origin, size, uv, inflate=None):
        out = {"origin": list(origin), "size": list(size), "uv": list(uv)}
        if inflate:
            out["inflate"] = inflate
        return out

    bones = [
        {"name": "root", "pivot": [0, 0, 0]},
        {"name": "body", "parent": "root", "pivot": [0, 24, 0], "cubes": [cube((-4, 12, -2), (8, 12, 4), (16, 16)), cube((-4, 12, -2), (8, 12, 4), (16, 32), 0.25)]},
        {"name": "head", "parent": "body", "pivot": [0, 24, 0], "cubes": [cube((-4, 24, -4), (8, 8, 8), (0, 0)), cube((-4, 24, -4), (8, 8, 8), (32, 0), 0.5)]},
        {"name": "arm_r", "parent": "body", "pivot": [-5, 22, 0], "cubes": [cube((-8, 12, -2), (4, 12, 4), (40, 16)), cube((-8, 12, -2), (4, 12, 4), (40, 32), 0.25)]},
        {"name": "arm_l", "parent": "body", "pivot": [5, 22, 0], "cubes": [cube((4, 12, -2), (4, 12, 4), (32, 48)), cube((4, 12, -2), (4, 12, 4), (48, 48), 0.25)]},
        {"name": "leg_r", "parent": "body", "pivot": [-1.9, 12, 0], "cubes": [cube((-3.9, 0, -2), (4, 12, 4), (0, 16)), cube((-3.9, 0, -2), (4, 12, 4), (0, 32), 0.25)]},
        {"name": "leg_l", "parent": "body", "pivot": [1.9, 12, 0], "cubes": [cube((-0.1, 0, -2), (4, 12, 4), (16, 48)), cube((-0.1, 0, -2), (4, 12, 4), (0, 48), 0.25)]},
    ]
    return {"format_version": "1.12.0", "minecraft:geometry": [{"description": {
        "identifier": "geometry.redacted_saint_disguise", "texture_width": 64, "texture_height": 64,
        "visible_bounds_width": 3, "visible_bounds_height": 3, "visible_bounds_offset": [0, 1.5, 0]}, "bones": bones}]}


def saint_disguise_texture():
    """A hooded pilgrim on the 64x64 player-skin layout: plain robe, rope belt, face in shadow."""
    import random
    rnd = random.Random(7041)
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    px = img.load()

    def shade(rgb, k):
        return tuple(max(0, min(255, int(c * k))) for c in rgb[:3]) + (255,)

    def noisy(rgb, amount=0.08):
        return shade(rgb, 1 + rnd.uniform(-amount, amount))

    def box(u, v, w, h, d, face_fn):
        # Player-skin box unwrap: (name, x, y, width, height)
        faces = [("top", u + d, v, w, d), ("bottom", u + d + w, v, w, d), ("right", u, v + d, d, h),
                 ("front", u + d, v + d, w, h), ("left", u + d + w, v + d, d, h), ("back", u + 2 * d + w, v + d, w, h)]
        for name, x0, y0, fw, fh in faces:
            for y in range(fh):
                for x in range(fw):
                    c = face_fn(name, x, y, fw, fh)
                    if c is not None:
                        px[x0 + x, y0 + y] = c

    robe = (104, 92, 80)
    hood = (84, 74, 66)
    skin = (196, 170, 150)
    rope = (176, 150, 96)

    def head(name, x, y, w, h):
        if name == "front":
            # Shadowed face inside the hood opening; two faint eyes.
            if 1 <= x <= 6 and y >= 2:
                if y in (4,) and x in (2, 5):
                    return (24, 20, 22, 255)
                return noisy(shade(skin, 0.45 + 0.08 * (y - 2)), 0.04)
            return noisy(hood)
        return noisy(hood)

    def hat(name, x, y, w, h):
        # Hood overlay: a lip around the face and a draped back.
        if name == "front":
            return noisy(shade(hood, 0.85)) if (x in (0, 7) or y in (0, 1)) else None
        if name == "bottom":
            return None
        return noisy(shade(hood, 0.92)) if rnd.random() > 0.15 else None

    def body(name, x, y, w, h):
        if name in ("front", "back") and y == 6:
            return noisy(rope, 0.05)
        if name == "front" and x in (3, 4) and y < 6:
            return noisy(shade(robe, 0.8))
        return noisy(robe)

    def body_overlay(name, x, y, w, h):
        if name in ("front", "back", "left", "right") and y >= h - 3:
            return noisy(shade(robe, 0.75))
        return None

    def arm(name, x, y, w, h):
        if y >= h - 2 and name != "top":
            return noisy(shade(skin, 0.8), 0.04)
        return noisy(shade(robe, 0.95))

    def leg(name, x, y, w, h):
        if y >= h - 2:
            return noisy((52, 44, 40))
        return noisy(shade(robe, 0.9))

    box(0, 0, 8, 8, 8, head)
    box(32, 0, 8, 8, 8, hat)
    box(16, 16, 8, 12, 4, body)
    box(16, 32, 8, 12, 4, body_overlay)
    box(40, 16, 4, 12, 4, arm)
    box(32, 48, 4, 12, 4, arm)
    box(0, 16, 4, 12, 4, leg)
    box(16, 48, 4, 12, 4, leg)
    return img


def vow_kin():
    # A figure knotted together from rope and old promises.
    m = Model("vow_kin", {"body": ((92, 70, 44), (176, 141, 87)), "rope": ((140, 110, 70), (60, 40, 20)), "face": ((30, 24, 20), (200, 170, 110))})
    m.bone("root", (0, 0, 0))
    m.bone("torso", (0, 22, 0), "root", [c((-4, 12, -2), (8, 12, 4), "rope", ["tally"])])
    m.bone("head", (0, 24, 0), "torso", [c((-3.5, 24, -3.5), (7, 8, 7), "face", ["eyes", "mouth"])])
    m.bone("knot", (0, 32, 0), "head", [c((-1, 32, -1), (2, 4, 2), "rope")])
    m.bone("arm_l", (5, 22, 0), "torso", [c((4, 8, -1.5), (3, 15, 3), "rope")])
    m.bone("arm_r", (-5, 22, 0), "torso", [c((-7, 8, -1.5), (3, 15, 3), "rope")])
    m.bone("leg_l", (2, 12, 0), "torso", [c((0.5, 0, -1.5), (3, 12, 3), "rope")])
    m.bone("leg_r", (-2, 12, 0), "torso", [c((-3.5, 0, -1.5), (3, 12, 3), "rope")])
    return m, {"root": "root", "style": "walker", "legs": ["leg_l", "leg_r"], "arms": ["arm_l", "arm_r"], "head": ["head"], "extra": ["knot"], "body": ["torso"]}


def paper_tide_gravemind():
    # A drifting mass of grave charms around an ink-dark core, crowned by a turning wheel of paper.
    m = Model("paper_tide_gravemind", {"body": ((232, 224, 200), (90, 70, 60)), "ink": ((40, 34, 40), (160, 40, 50))})
    m.bone("root", (0, 2, 0))
    m.bone("core", (0, 16, 0), "root", [c((-6, 6, -4), (12, 18, 8), "body", ["many_eyes"])])
    m.bone("head", (0, 24, 0), "core", [c((-4, 24, -4), (8, 8, 8), "ink", ["eyes", "mouth"])])
    m.bone("wheel", (0, 33, 0), "head", [c((-5, 33, -5), (10, 1, 10), "ink", ["glow"])])
    sheets = []
    for i, (x, z) in enumerate([(-8, -3), (8, -3), (-7, 4), (7, 4), (0, 6), (0, -6)]):
        n = m.bone(f"sheet{i}", (x, 18, z), "core", [c((x - 3, 8, z - 0.5), (6, 14, 1), "body", ["tally"] if i % 2 == 0 else [])])
        sheets.append(n)
    return m, {"root": "root", "style": "floater", "head": ["head"], "extra": sheets + ["wheel"], "body": ["core"]}


def unfinished_ferryman():
    # A tall hooded boatman with an oar in one hand and a lantern in the other.
    m = Model("unfinished_ferryman", {"body": ((52, 58, 66), (180, 196, 200)), "robe": ((70, 84, 92), (30, 40, 48)),
                                      "oar": ((110, 86, 60), (60, 44, 30)), "lamp": ((240, 220, 150), (255, 255, 220))})
    m.bone("root", (0, 0, 0))
    m.bone("torso", (0, 22, 0), "root", [c((-4.5, 10, -2.5), (9, 14, 5), "robe")])
    m.bone("head", (0, 24, 0), "torso", [c((-3, 24, -3), (6, 7, 6), "body", ["eyes"])])
    m.bone("hood", (0, 27, 0), "head", [c((-3.5, 27, -3.5), (7, 5, 7), "robe")])
    m.bone("arm_r", (-5.5, 22, 0), "torso", [c((-8, 10, -1.5), (3, 13, 3), "robe")])
    m.bone("oar", (-6.5, 11, 0), "arm_r", [c((-7, -6, -0.5), (1, 32, 1), "oar"), c((-8, -8, -0.5), (3, 6, 1), "oar")])
    m.bone("arm_l", (5.5, 22, 0), "torso", [c((5, 10, -1.5), (3, 13, 3), "robe")])
    m.bone("lamp", (6.5, 9, 0), "arm_l", [c((5.5, 5, -1), (2, 3, 2), "lamp", ["glow"])])
    m.bone("leg_l", (2, 10, 0), "torso", [c((0, 0, -2), (4, 10, 4), "robe")])
    m.bone("leg_r", (-2, 10, 0), "torso", [c((-4, 0, -2), (4, 10, 4), "robe")])
    return m, {"root": "root", "style": "walker", "legs": ["leg_l", "leg_r"], "arms": ["arm_l", "arm_r"], "head": ["head"], "extra": ["oar", "lamp", "hood"], "body": ["torso"]}


def culling_warden():
    # An armored judge carrying a stone tablet of edicts.
    m = Model("culling_warden", {"body": ((60, 20, 24), (224, 48, 63)), "plate": ((120, 110, 104), (40, 36, 34)),
                                 "tablet": ((200, 190, 170), (60, 50, 40))})
    m.bone("root", (0, 0, 0))
    m.bone("torso", (0, 22, 0), "root", [c((-5, 10, -3), (10, 14, 6), "plate", ["crack"])])
    m.bone("head", (0, 24, 0), "torso", [c((-3.5, 24, -3.5), (7, 8, 7), "body", ["many_eyes"])])
    m.bone("crest", (0, 32, 0), "head", [c((-0.5, 32, -4), (1, 4, 8), "plate")])
    m.bone("arm_l", (5.5, 22, 0), "torso", [c((5, 9, -2), (4, 15, 4), "plate")])
    m.bone("tablet", (7, 9, 0), "arm_l", [c((6, 4, -4), (1, 8, 6), "tablet", ["tally"])])
    m.bone("arm_r", (-5.5, 22, 0), "torso", [c((-9, 9, -2), (4, 15, 4), "plate")])
    m.bone("leg_l", (2.5, 10, 0), "torso", [c((0.5, 0, -2), (4, 10, 4), "plate")])
    m.bone("leg_r", (-2.5, 10, 0), "torso", [c((-4.5, 0, -2), (4, 10, 4), "plate")])
    return m, {"root": "root", "style": "walker", "legs": ["leg_l", "leg_r"], "arms": ["arm_l", "arm_r"], "head": ["head"], "extra": ["tablet", "crest"], "body": ["torso"]}


def ten_mouth_sovereign():
    # A crowned mass of flesh and teeth with mouths all over it.
    m = Model("ten_mouth_sovereign", {"body": ((70, 20, 26), (192, 57, 43)), "flesh": ((120, 40, 44), (242, 193, 78)),
                                      "bone": ((220, 210, 190), (80, 60, 40))})
    m.bone("root", (0, 0, 0))
    m.bone("mass", (0, 14, 0), "root", [c((-9, 4, -6), (18, 18, 12), "body", ["mouth"])])
    m.bone("head", (0, 22, 0), "mass", [c((-5, 22, -5), (10, 8, 10), "flesh", ["many_eyes", "mouth"])])
    jaws = []
    for i, (x, y, z) in enumerate([(-7, 18, -6.5), (5, 16, -6.5), (-3, 9, -6.5), (7, 8, -6.5), (-9.5, 12, 0), (9.5, 12, 0)]):
        n = m.bone(f"jaw{i}", (x, y, z), "mass", [c((x - 2, y - 1.5, z - 1), (4, 3, 2), "flesh", ["mouth"])])
        jaws.append(n)
    m.bone("arm_l", (9, 18, 0), "mass", [c((9, 6, -2), (4, 14, 4), "flesh")])
    m.bone("arm_r", (-9, 18, 0), "mass", [c((-13, 6, -2), (4, 14, 4), "flesh")])
    m.bone("pillar_l", (-5, 4, 0), "root", [c((-8, 0, -3), (5, 6, 6), "bone")])
    m.bone("pillar_r", (5, 4, 0), "root", [c((3, 0, -3), (5, 6, 6), "bone")])
    return m, {"root": "root", "style": "golem", "legs": ["pillar_l", "pillar_r"], "arms": ["arm_l", "arm_r"], "head": ["head"], "extra": jaws, "body": ["mass"]}


def shadow_divine_dog():
    # Not a curse: the shadow-garden domain's summon. A lean four-legged shape of dark ink with
    # violet eyes; animated with the crawler rig.
    m = Model("shadow_divine_dog", {"body": ((26, 20, 36), (143, 108, 192)), "limb": ((16, 12, 24), (90, 70, 130))})
    m.bone("root", (0, 0, 0))
    m.bone("body", (0, 9, 0), "root", [c((-3, 8, -7), (6, 5, 13), "body", ["glow"])])
    m.bone("head", (0, 12, -7), "body", [c((-2.5, 10, -12), (5, 5, 5), "body", ["eyes", "mouth"])])
    m.bone("tail", (0, 12, 6), "body", [c((-1, 11, 6), (2, 2, 6), "limb")])
    legs = []
    for i, (x, z) in enumerate([(-2, -5), (2, -5), (-2, 4), (2, 4)]):
        n = m.bone(f"leg{i}", (x, 8, z), "body", [c((x - 1, 0, z - 1), (2, 8, 2), "limb")])
        legs.append(n)
    return m, {"root": "root", "style": "crawler", "legs": legs, "head": ["head"], "body": ["body"], "extra": ["tail"]}


def doorframe_hunter():
    # A plank-thin figure as tall as a doorway: flat body, arms that hang to its knees, a narrow
    # head with one slit of an eye. It stands in frames the way a door would.
    m = Model("doorframe_hunter", {"body": ((92, 74, 58), (40, 30, 24)), "limb": ((70, 56, 44), (30, 22, 18)),
                                   "face": ((214, 206, 190), (20, 12, 10))})
    m.bone("root", (0, 0, 0))
    m.bone("torso", (0, 20, 0), "root", [c((-3.5, 14, -1), (7, 16, 2), "body", ["crack"])])
    m.bone("head", (0, 30, 0), "torso", [c((-2, 30, -1.5), (4, 6, 3), "face", ["eyes"])])
    m.bone("arm_r", (-4.5, 29, 0), "torso", [c((-5.5, 10, -0.5), (2, 19, 1), "limb")])
    m.bone("arm_l", (4.5, 29, 0), "torso", [c((3.5, 10, -0.5), (2, 19, 1), "limb")])
    m.bone("leg_r", (-1.5, 14, 0), "torso", [c((-3, 0, -0.5), (2, 14, 1), "limb")])
    m.bone("leg_l", (1.5, 14, 0), "torso", [c((1, 0, -0.5), (2, 14, 1), "limb")])
    return m, {"root": "root", "legs": ["leg_r", "leg_l"], "arms": ["arm_r", "arm_l"], "head": ["head"], "body": ["torso"]}


def gilded_mimic():
    # A gilded chest on four jointed legs; the lid is a jaw, and a long tongue hangs out of it.
    m = Model("gilded_mimic", {"body": ((122, 84, 44), (217, 164, 65)), "gold": ((217, 164, 65), (255, 230, 150)),
                               "flesh": ((150, 40, 52), (230, 120, 130)), "limb": ((60, 40, 26), (30, 20, 12))})
    m.bone("root", (0, 0, 0))
    m.bone("base", (0, 6, 0), "root", [c((-7, 4, -7), (14, 7, 14), "body", ["mouth"]), c((-7.5, 4, -7.5), (15, 1, 15), "gold")])
    m.bone("head", (0, 11, 6), "base", [c((-7, 11, -7), (14, 4, 14), "body", ["eyes"]), c((-1, 10, -8), (2, 3, 1), "gold", ["glow"])])
    m.bone("tongue", (0, 9, -7), "base", [c((-1.5, 8.5, -15), (3, 1, 8), "flesh")])
    legs = []
    for i, (x, z) in enumerate([(-6, -6), (6, -6), (-6, 6), (6, 6)]):
        legs.append(m.bone(f"leg{i}", (x, 5, z), "base", [c((x - 1, 0, z - 1), (2, 5, 2), "limb")]))
    return m, {"root": "root", "style": "crawler", "legs": legs, "head": ["head"], "body": ["base"], "extra": ["tongue"]}


def gilded_mimic_disguise():
    # What it looks like while it waits: an ordinary, well-kept chest with a gold latch.
    m = Model("gilded_mimic_disguise", {"body": ((134, 96, 52), (96, 66, 34)), "gold": ((217, 164, 65), (255, 230, 150))})
    m.bone("root", (0, 0, 0))
    m.bone("base", (0, 0, 0), "root", [c((-7, 0, -7), (14, 10, 14), "body")])
    m.bone("head", (0, 10, 7), "base", [c((-7, 10, -7), (14, 5, 14), "body"), c((-1, 8, -8), (2, 4, 1), "gold")])
    return m


def tithe_burrower():
    # A low, broad digger with a coin-heavy belly sack, spade claws and a long snout.
    m = Model("tithe_burrower", {"body": ((88, 70, 52), (176, 141, 87)), "sack": ((150, 120, 70), (230, 200, 120)),
                                 "claw": ((40, 34, 30), (220, 210, 190)), "face": ((110, 90, 70), (30, 20, 16))})
    m.bone("root", (0, 0, 0))
    m.bone("body", (0, 8, 0), "root", [c((-5, 4, -7), (10, 8, 14), "body", ["many_eyes"])])
    m.bone("sack", (0, 5, 2), "body", [c((-3.5, 1, -1), (7, 4, 7), "sack", ["tally"])])
    m.bone("head", (0, 9, -7), "body", [c((-3, 6, -12), (6, 5, 5), "face", ["eyes"]), c((-1, 6, -14), (2, 2, 2), "claw")])
    legs = []
    for i, (x, z) in enumerate([(-4, -5), (4, -5), (-4, 5), (4, 5)]):
        legs.append(m.bone(f"leg{i}", (x, 4, z), "body", [c((x - 1.5, 0, z - 1.5), (3, 4, 3), "claw")]))
    return m, {"root": "root", "style": "crawler", "legs": legs, "head": ["head"], "body": ["body"], "extra": ["sack"]}


def divine_dog():
    # Divine Dog (Ten Shadows): a lean ink-black dog with a brush-stroke mane and a pale mask.
    m = Model("divine_dog", {"body": ((30, 26, 44), (110, 90, 160)), "limb": ((22, 18, 32), (80, 66, 120)),
                            "mask": ((232, 226, 210), (20, 14, 20))})
    m.bone("root", (0, 0, 0))
    m.bone("body", (0, 9, 0), "root", [c((-3, 8, -7), (6, 5, 13), "body", ["glow"])])
    m.bone("mane", (0, 13, -5), "body", [c((-3.5, 12, -6), (7, 3, 4), "body")])
    m.bone("head", (0, 12, -7), "body", [c((-2.5, 10, -12), (5, 5, 5), "mask", ["eyes", "mouth"])])
    m.bone("tail", (0, 12, 6), "body", [c((-1, 11, 6), (2, 2, 7), "limb")])
    legs = []
    for i, (x, z) in enumerate([(-2, -5), (2, -5), (-2, 4), (2, 4)]):
        legs.append(m.bone(f"leg{i}", (x, 8, z), "body", [c((x - 1, 0, z - 1), (2, 8, 2), "limb")]))
    return m, {"root": "root", "style": "crawler", "legs": legs, "head": ["head"], "body": ["body"], "extra": ["tail", "mane"]}


def nue():
    # Nue (Ten Shadows), still the old paper-crane shape until the Phase 3 art pass: a folded paper crane with sharp wings and a red-tipped beak.
    m = Model("nue", {"body": ((236, 230, 214), (200, 60, 60)), "wing": ((226, 218, 198), (150, 140, 120))})
    m.bone("root", (0, 4, 0))
    m.bone("body", (0, 5, 0), "root", [c((-1.5, 3, -4), (3, 3, 8), "body")])
    m.bone("head", (0, 6, -4), "body", [c((-0.5, 6, -8), (1, 4, 1), "body"), c((-0.5, 9, -9), (1, 1, 2), "body", ["glow"])])
    m.bone("tail", (0, 5, 4), "body", [c((-0.5, 5, 4), (1, 3, 3), "body")])
    m.bone("wing_l", (1.5, 6, 0), "body", [c((1.5, 5.5, -3), (8, 1, 5), "wing")])
    m.bone("wing_r", (-1.5, 6, 0), "body", [c((-9.5, 5.5, -3), (8, 1, 5), "wing")])
    return m, {"root": "root", "style": "floater", "head": ["head"], "body": ["body"], "extra": ["wing_l", "wing_r", "tail"]}


def toad():
    # Toad (Ten Shadows), still the old stone-tortoise shape until the Phase 3 art pass: a broad tortoise of stacked stone with a moss-lined shell.
    m = Model("toad", {"body": ((120, 128, 120), (70, 110, 70)), "shell": ((96, 104, 98), (150, 170, 120)),
                                 "limb": ((100, 106, 100), (60, 64, 60))})
    m.bone("root", (0, 0, 0))
    m.bone("shell", (0, 7, 0), "root", [c((-8, 4, -9), (16, 8, 18), "shell", ["crack"]), c((-6, 12, -7), (12, 3, 14), "shell")])
    m.bone("head", (0, 7, -9), "shell", [c((-2.5, 5, -14), (5, 5, 6), "body", ["eyes"])])
    legs = []
    for i, (x, z) in enumerate([(-6, -6), (6, -6), (-6, 6), (6, 6)]):
        legs.append(m.bone(f"leg{i}", (x, 5, z), "shell", [c((x - 2, 0, z - 2), (4, 5, 4), "limb")]))
    return m, {"root": "root", "style": "crawler", "legs": legs, "head": ["head"], "body": ["shell"]}


EXTRA = [shadow_divine_dog, divine_dog, nue, toad]


CURSES = [hollow_hymnal, ledger_crawler, wick_moth, pale_stilt, echo_mimic, salt_lantern,
          tidewash, thorn_matriarch, cracked_bell, redacted_saint, ash_oni, gravebound_gate,
          vow_kin, paper_tide_gravemind, unfinished_ferryman, culling_warden, ten_mouth_sovereign,
          doorframe_hunter, gilded_mimic, tithe_burrower]


def main(assets):
    geo_dir = os.path.join(assets, "geckolib", "models", "entity")
    anim_dir = os.path.join(assets, "geckolib", "animations", "entity")
    tex_dir = os.path.join(assets, "textures", "entity")
    for d in (geo_dir, anim_dir, tex_dir):
        os.makedirs(d, exist_ok=True)

    for build in CURSES + EXTRA:
        model, rig = build()
        with open(os.path.join(geo_dir, model.name + ".geo.json"), "w", encoding="utf-8") as f:
            json.dump(model.geo(), f, indent=1)
        with open(os.path.join(anim_dir, model.name + ".animation.json"), "w", encoding="utf-8") as f:
            json.dump(anims(model.name, rig), f, indent=1)
        model.texture(sum(map(ord, model.name))).save(os.path.join(tex_dir, model.name + ".png"))
        print("curse", model.name, len(model.bones), "bones")

    with open(os.path.join(geo_dir, "redacted_saint_disguise.geo.json"), "w", encoding="utf-8") as f:
        json.dump(saint_disguise_geo(), f, indent=1)
    saint_disguise_texture().save(os.path.join(tex_dir, "redacted_saint_disguise.png"))
    chest = gilded_mimic_disguise()
    with open(os.path.join(geo_dir, "gilded_mimic_disguise.geo.json"), "w", encoding="utf-8") as f:
        json.dump(chest.geo(), f, indent=1)
    chest.texture(5150).save(os.path.join(tex_dir, "gilded_mimic_disguise.png"))
    print("disguise models ok")


if __name__ == "__main__":
    main(sys.argv[1])
