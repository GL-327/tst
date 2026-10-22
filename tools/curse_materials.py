"""Pixel materials and faces for the 52 curse species (two texels per model unit).

Each cube face is painted as a small hand-shaded panel: lit from above, darker toward its base,
with a highlight on the top edge and a shadow on the lower edge so blocks read as solid forms.
Materials add a few legible details (veins, cracks, rust, talisman script, folds) clustered in
two or three tones rather than per-pixel noise. Faces follow the curse look of the source
material: sunken eyes with lit irises and wide, toothed mouths.

Used by spectral_content.py for every species in reborn_creatures.DESIGNS.
"""
import hashlib
import math

import numpy as np
from PIL import Image

SCLERA = np.array((222, 212, 163), dtype=np.float64)
TEETH = np.array((226, 216, 189), dtype=np.float64)
GUMS = np.array((92, 24, 34), dtype=np.float64)
THROAT = np.array((28, 9, 14), dtype=np.float64)
INK = np.array((24, 18, 26), dtype=np.float64)
SEAL_RED = np.array((150, 32, 38), dtype=np.float64)

FACE_LIGHT = {'north': 1.0, 'south': 0.86, 'east': 0.92, 'west': 0.95, 'up': 1.12, 'down': 0.66}

NEUTRALS = {
    'bone': (206, 197, 170), 'gold': (171, 133, 62), 'paper': (214, 203, 170), 'shadow': (30, 28, 42),
}


def _mix(a, b, t):
    return np.asarray(a, dtype=np.float64) * (1 - t) + np.asarray(b, dtype=np.float64) * t


def material_color(kind, base, accent):
    base = np.asarray(base, dtype=np.float64)
    if kind in NEUTRALS:
        return np.asarray(NEUTRALS[kind], dtype=np.float64)

    return {
        'metal': _mix(base, (84, 90, 98), 0.45), 'wood': _mix(base, (98, 72, 50), 0.55),
        'stone': _mix(base, (96, 98, 104), 0.58), 'cloth': _mix(base, (38, 41, 50), 0.22),
        'belly': _mix(base, (196, 180, 148), 0.38), 'glass': _mix(base, (132, 176, 183), 0.45),
        'chitin': _mix(base, (34, 37, 41), 0.32), 'plant': _mix(base, (52, 78, 52), 0.5),
        'wing': _mix(base, (152, 133, 116), 0.25), 'scale': _mix(base, (58, 84, 92), 0.25),
        'fur': _mix(base, (60, 50, 50), 0.15), 'skin': base,
    }.get(kind, base)


def _seed(*parts):
    return int(hashlib.sha256('|'.join(map(str, parts)).encode()).hexdigest()[:12], 16)


def _noise(rng, w, h, cell):
    """Smooth value noise in [0, 1], bilinearly upsampled from a coarse grid."""
    gw, gh = w // cell + 2, h // cell + 2
    grid = rng.random((gh, gw))
    ys = (np.arange(h) + 0.5) / cell
    xs = (np.arange(w) + 0.5) / cell
    y0 = np.floor(ys).astype(int)
    x0 = np.floor(xs).astype(int)
    ty = (ys - y0)[:, None]
    tx = (xs - x0)[None, :]
    a = grid[y0][:, x0]
    b = grid[y0][:, x0 + 1]
    c = grid[y0 + 1][:, x0]
    d = grid[y0 + 1][:, x0 + 1]
    return (a * (1 - tx) + b * tx) * (1 - ty) + (c * (1 - tx) + d * tx) * ty


def _walk(rng, w, h, length, start=None, drift=(0, 1)):
    """A meandering one-texel line, for veins and cracks."""
    x, y = start if start else (int(rng.integers(0, max(1, w))), 0)
    points = []
    for _ in range(length):
        if 0 <= x < w and 0 <= y < h:
            points.append((x, y))
        step = rng.random()
        if step < 0.55:
            x, y = x + drift[0], y + drift[1]
        elif step < 0.78:
            x += 1
        else:
            x -= 1
    return points


class Canvas:
    """A texture under construction: RGB, coverage and an emissive layer."""

    def __init__(self, width, height):
        self.rgb = np.zeros((height, width, 3), dtype=np.float64)
        self.alpha = np.zeros((height, width), dtype=np.float64)
        self.glow = np.zeros((height, width, 4), dtype=np.float64)

    def images(self):
        rgba = np.dstack([np.clip(self.rgb, 0, 255), self.alpha * 255]).round().astype(np.uint8)
        glow = np.clip(self.glow, 0, 255).round().astype(np.uint8)
        return Image.fromarray(rgba, 'RGBA'), Image.fromarray(glow, 'RGBA')


def paint(canvas, x, y, w, h, kind, face, base, accent, seed, origin, size, facial=None):
    rng = np.random.default_rng(_seed(seed, x, y, kind, face))
    color = material_color(kind, base, accent)
    accent = np.asarray(accent, dtype=np.float64)
    region = canvas.rgb[y:y + h, x:x + w]
    canvas.alpha[y:y + h, x:x + w] = 1

    if kind == 'eye':
        _eye_block(canvas, x, y, w, h, accent, face)
        return

    if kind == 'mouth':
        # A mouth cube is mostly throat; its broad faces carry teeth, whichever way it points.
        region[:] = THROAT
        if w >= 3 and h >= 2 and w * h >= 8:
            if h >= 4:
                region[1:-1, 1:-1] = _mix(THROAT, GUMS, 0.55)
                _fangs(region, w, h, top=True)
                _fangs(region, w, h, top=False)
            else:
                _teeth_row(region, 0, w, top=True)
                if h >= 3:
                    _teeth_row(region, h - 1, w, top=False)
        return

    light = FACE_LIGHT[face]
    shade = np.full((h, w), light)
    if face not in ('up', 'down') and h >= 3:
        # Lit from above: a gentle falloff toward the bottom of every side face.
        shade *= (1.06 - 0.24 * (np.arange(h) / max(1, h - 1)))[:, None]

    # Two or three tones in soft clusters keep surfaces alive without speckle.
    if min(w, h) >= 3:
        n = _noise(rng, w, h, 3 if kind not in ('metal', 'stone') else 4)
        tones = np.where(n > 0.68, 1.06, np.where(n < 0.3, 0.92, 1.0))
        if kind in ('glass', 'gold', 'paper'):
            tones = 1 + (tones - 1) * 0.5
        shade *= tones

    region[:] = color[None, None, :] * shade[:, :, None]
    _detail(region, rng, kind, face, w, h, color, accent)

    # Bevel: a light top edge and darker lower and side edges define each block.
    if face not in ('up', 'down') and min(w, h) >= 4:
        region[0, :] *= 1.1
        region[-1, :] *= 0.78
        region[:, 0] *= 0.9
        region[:, -1] *= 0.86
    elif min(w, h) >= 4:
        region[0, :] *= 0.92
        region[-1, :] *= 0.92
        region[:, 0] *= 0.92
        region[:, -1] *= 0.92

    if kind == 'shadow':
        _shimmer(canvas, rng, x, y, w, h, accent)

    if isinstance(facial, dict) and face == 'north':
        _face(canvas, rng, x, y, w, h, facial, color, accent)


def _detail(region, rng, kind, face, w, h, color, accent):
    dark = 0.72
    if kind == 'skin' and min(w, h) >= 6:
        vein = _mix(color * 0.62, accent, 0.18)
        for _ in range(int(rng.integers(0, 2 + (w * h > 120)))):
            for px, py in _walk(rng, w, h, int(h * 1.2)):
                region[py, px] = vein
        if w * h >= 80 and rng.random() < 0.6:
            # A small sore or blister, tinted by the curse's accent.
            sx, sy = int(rng.integers(1, w - 2)), int(rng.integers(1, h - 2))
            region[sy, sx] = _mix(color * 0.55, accent, 0.45)
            region[sy, sx + 1] = _mix(color * 0.7, accent, 0.25)
            region[sy + 1, sx] = color * 0.62
    elif kind in ('bone', 'stone') and min(w, h) >= 5:
        for _ in range(1 + int(w * h > 150)):
            for px, py in _walk(rng, w, h, max(3, h // 2 + int(rng.integers(0, h // 2 + 1)))):
                region[py, px] *= dark
        if kind == 'bone':
            for _ in range(int(rng.integers(0, 3))):
                region[int(rng.integers(1, h - 1)), int(rng.integers(1, w - 1))] *= 0.8
    elif kind == 'chitin' and h >= 4:
        band = max(1, h // 4)
        region[band, 1:-1] *= 1.28
        for row in range(band + 3, h - 1, 4):
            region[row, :] *= 0.8
    elif kind == 'scale' and min(w, h) >= 3:
        for row in range(0, h, 3):
            shift = (row // 3) % 2
            for col in range(shift, w, 3):
                if row + 2 < h:
                    region[row + 2, col:col + 2] *= 0.76
                region[row, col] *= 1.08
    elif kind == 'cloth' and face in ('north', 'south', 'east', 'west') and min(w, h) >= 4:
        folds = np.sin(np.arange(w) / max(2.2, w / 3.5) * math.pi + rng.random() * 6)
        region *= (1 + 0.07 * folds)[None, :, None]
        if h >= 6:
            region[-2, :] *= 0.86
            for col in range(int(rng.integers(0, 3)), w, 3):
                region[-1, col] *= 0.7
    elif kind == 'paper' and min(w, h) >= 5:
        # Talisman script: short ink strokes in columns, with a red seal on larger sheets.
        for col in range(1 + int(rng.integers(0, 2)), w - 1, 3):
            row = 1
            while row < h - 2:
                length = int(rng.integers(1, 3))
                region[row:row + length, col] = INK
                row += length + 1 + int(rng.integers(0, 2))
        if w >= 8 and h >= 8:
            sx, sy = w - 4, h - 4
            region[sy:sy + 3, sx:sx + 3] = SEAL_RED
            region[sy + 1, sx + 1] = SEAL_RED * 0.7
    elif kind == 'metal' and min(w, h) >= 4:
        rust = _mix((128, 66, 38), color, 0.25)
        n = _noise(rng, w, h, 2)
        mask = n > 0.74
        region[mask] = _mix(region[mask], rust[None, :], 0.65)
        if w >= 8 and h >= 6 and face == 'north':
            for px, py in ((1, 1), (w - 2, 1), (1, h - 2), (w - 2, h - 2)):
                region[py, px] = color * 1.3
    elif kind == 'gold' and min(w, h) >= 3:
        for i in range(min(w, h)):
            if i < h and i < w:
                region[i, i] = np.minimum(255, color * 1.35)
    elif kind == 'wood' and h >= 3:
        offset = rng.random() * 6
        for row in range(h):
            for col in range(w):
                if math.sin(col * 0.9 + offset + row * 0.15 + math.sin(row * 0.5) * 1.5) > 0.82:
                    region[row, col] *= 0.8
    elif kind == 'fur' and min(w, h) >= 3:
        for col in range(0, w, 2):
            start = int(rng.integers(0, 3))
            for row in range(start, h, 3):
                region[row, col] *= 0.8
                if row + 1 < h:
                    region[row + 1, col] *= 0.9
    elif kind == 'wing' and min(w, h) >= 5:
        vein = color * 0.7
        for k in range(3):
            for t in np.linspace(0, 1, max(w, h)):
                px, py = int(t * (w - 1)), int(t * (h - 1) * (0.3 + 0.35 * k))
                if 0 <= py < h:
                    region[py, px] = vein
    elif kind == 'belly' and h >= 4:
        for row in range(2, h, 3):
            region[row, :] *= 0.86
    elif kind == 'glass' and min(w, h) >= 3:
        for i in range(min(w, h)):
            if i + 1 < w:
                region[i, i + 1] = np.minimum(255, color * 1.45)
    elif kind == 'plant' and min(w, h) >= 4:
        mid = w // 2
        region[:, mid] *= 0.78
        for row in range(1, h, 3):
            for d in range(1, min(3, mid)):
                if row + d < h:
                    region[row + d, mid - d] *= 0.85
                    if mid + d < w:
                        region[row + d, mid + d] *= 0.85


def _shimmer(canvas, rng, x, y, w, h, accent):
    for _ in range(max(1, w * h // 40)):
        px, py = x + int(rng.integers(0, w)), y + int(rng.integers(0, h))
        canvas.rgb[py, px] = _mix(canvas.rgb[py, px], accent, 0.35)
        canvas.glow[py, px] = np.append(accent, 70)


def _teeth_row(region, row, w, top):
    """A single row of alternating teeth, for small mouths."""
    for col in range(w):
        if (col + (0 if top else 1)) % 2 == 0:
            region[row, col] = TEETH * (1.0 if top else 0.9)


def _fangs(region, mw, mh, top):
    """Pointed teeth: each tooth is two texels wide at the gum and one at the tip."""
    gum, tip = (0, 1) if top else (mh - 1, mh - 2)
    shade = 1.0 if top else 0.88
    for col in range(1, mw - 1, 3):
        region[gum, col:min(mw - 1, col + 2)] = TEETH * shade
        if 0 <= tip < mh and mh >= 4:
            region[tip, col + (0 if top else 1)] = TEETH * shade * 0.94


def _vivid(accent):
    """An iris color: the accent pushed toward full saturation so eyes read in any light."""
    c = np.asarray(accent, dtype=np.float64)
    lo, hi = c.min(), c.max()
    if hi - lo < 40:
        return np.array((236, 72, 58), dtype=np.float64)
    return np.clip((c - lo) / (hi - lo) * 230 + 25, 0, 255)


def _eye_block(canvas, x, y, w, h, accent, face):
    """A cube that is itself an eye: sclera, a lit iris and a slit pupil."""
    region = canvas.rgb[y:y + h, x:x + w]
    region[:] = SCLERA * (0.92 if face != 'north' else 1.0)
    if face != 'north':
        return
    iris = _vivid(accent)
    cx0, cx1 = max(0, w // 2 - max(1, w // 4)), min(w, w // 2 + max(1, w // 4) + (w % 2))
    region[:, cx0:cx1] = iris
    region[:, w // 2] = INK
    canvas.glow[y:y + h, x + cx0:x + cx1] = np.append(iris, 210)
    canvas.glow[y:y + h, x + w // 2] = 0


def _eye(region, glow, cx, top, ew, eh, iris, skin):
    """One sunken eye: a socket, a sclera with clipped corners, a lit iris and a slit pupil."""
    h, w = region.shape[:2]
    x0, x1 = max(0, cx - ew // 2), min(w, cx - ew // 2 + ew)
    y0, y1 = max(0, top), min(h, top + eh)
    if x1 - x0 < 2 or y1 - y0 < 2:
        return
    region[max(0, y0 - 1):min(h, y1 + 1), max(0, x0 - 1):min(w, x1 + 1)] = skin * 0.42
    region[y0:y1, x0:x1] = SCLERA
    if x1 - x0 >= 4 and y1 - y0 >= 3:
        for cy_, cx_ in ((y0, x0), (y0, x1 - 1), (y1 - 1, x0), (y1 - 1, x1 - 1)):
            region[cy_, cx_] = skin * 0.42
    iw = 1 if x1 - x0 < 4 else max(2, (x1 - x0) // 3)
    ix0 = x0 + (x1 - x0 - iw) // 2
    region[y0:y1, ix0:ix0 + iw] = iris
    glow[y0:y1, ix0:ix0 + iw] = np.append(iris, 225)
    if iw >= 2:
        pupil = ix0 + iw // 2
        slit = slice(y0 + (1 if y1 - y0 >= 5 else 0), y1 - (1 if y1 - y0 >= 5 else 0))
        region[slit, pupil] = INK
        glow[slit, pupil] = 0
        # A catchlight makes the eye look wet and alive.
        region[y0, ix0] = np.minimum(255, iris * 0.4 + 170)


def _face(canvas, rng, x, y, w, h, spec, skin, accent):
    """A curse face: brows pressing down on sunken, lit eyes over a wide mouth of fangs."""
    if min(w, h) < 6:
        return
    region = canvas.rgb[y:y + h, x:x + w]
    glow = canvas.glow[y:y + h, x:x + w]
    iris = _vivid(accent)
    count = int(spec.get('eyes', 2))

    if count == 1:
        # A cyclops eye dominates the face.
        ew = int(np.clip(w * 0.42, 3, 14))
        eh = int(np.clip(h * 0.3, 2, 8))
    else:
        ew = int(np.clip(w // (count + 1), 3, 7))
        eh = int(np.clip(h // 4, 2, 4))
    ey = max(2, int(h * 0.24))
    for i in range(count):
        cx = int(round(w * (i + 1) / (count + 1)))
        # Faces are a little lopsided: one eye sits lower and the other wider.
        drop = 1 if (i % 2 == 1 and count > 1 and rng.random() < 0.7) else 0
        wide = ew + (1 if i == 0 and count > 1 and rng.random() < 0.5 else 0)
        _eye(region, glow, cx, ey + drop, wide, eh, iris, skin)
        # Brow: a dark ridge slanting down toward the middle of the face.
        x0 = cx - wide // 2 - 1
        for k in range(wide + 2):
            col = x0 + k
            if 0 <= col < w:
                toward_middle = k / max(1, wide + 1) if cx < w / 2 else 1 - k / max(1, wide + 1)
                row = ey + drop - 2 + (1 if toward_middle > 0.55 else 0)
                if 0 <= row < h:
                    region[row, col] = skin * 0.38

    if not spec.get('mouth', True):
        return

    mw = max(4, int(w * (0.64 + 0.16 * rng.random())))
    mh = int(np.clip(h // 3, 3, 6))
    mx = (w - mw) // 2
    my = min(h - mh - 1, max(ey + eh + 2, int(h * 0.56)))
    mouth = region[my:my + mh, mx:mx + mw]
    mouth[:] = THROAT
    if mh >= 4:
        mouth[1:-1, 1:-1] = _mix(THROAT, GUMS, 0.55)
    _fangs(mouth, mw, mh, top=True)
    _fangs(mouth, mw, mh, top=False)
    # The corners pull up into a grin; lips are a shade darker than the skin.
    for col in (mx - 1, mx + mw):
        if 0 <= col < w and my >= 1:
            region[my - 1, col] = THROAT
            region[my, col] = skin * 0.5
    region[max(0, my - 1), mx:mx + mw] = np.minimum(region[max(0, my - 1), mx:mx + mw], skin * 0.6)
    if my + mh < h:
        region[my + mh, mx:mx + mw] = skin * 0.62


def damage(image, name, critical=False):
    """Wounded and critical variants: dark gashes with a rim of the curse's own flesh tone."""
    arr = np.asarray(image.convert('RGBA'), dtype=np.float64).copy()
    opaque = arr[:, :, 3] > 0
    ys, xs = np.nonzero(opaque)
    if len(xs) == 0:
        return image.copy()
    rng = np.random.default_rng(_seed(name, critical))
    for _ in range(14 if critical else 7):
        i = int(rng.integers(0, len(xs)))
        x, y = int(xs[i]), int(ys[i])
        length = int(rng.integers(3, 7 if critical else 5))
        for k in range(length):
            px, py = x + k // 2, y + k
            if 0 <= py < arr.shape[0] and 0 <= px < arr.shape[1] and opaque[py, px]:
                arr[py, px, :3] = (58, 18, 24)
                if px + 1 < arr.shape[1] and opaque[py, px + 1]:
                    arr[py, px + 1, :3] *= 0.7
    return Image.fromarray(arr.round().astype(np.uint8), 'RGBA')
