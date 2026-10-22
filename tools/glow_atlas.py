"""Builds textures/vfx/glow_atlas.png: white-on-black sprites for the additive glow pass.

The glow pass adds texture * vertex color to the frame, so every tile is a luminance mask
(RGB = alpha = brightness) and the effect code supplies hue and intensity. Eight 128 px tiles
sit in one row:

    0 soft glow    1 hot core     2 ring        3 streak
    4 star flare   5 wisp         6 shock ring  7 lens flare

Usage: python tools/glow_atlas.py src/main/resources/assets/jjkcraft
"""
import pathlib
import sys

import numpy as np
from PIL import Image

TILE = 128
TILES = 8


def grid():
    c = (np.arange(TILE) + 0.5) / TILE * 2 - 1
    x, y = np.meshgrid(c, c)
    return x, y, np.sqrt(x * x + y * y)


def edge_fade(mask):
    """Forces the outermost pixels to zero so bilinear sampling never bleeds into a neighbour."""
    _, _, r = grid()
    return mask * np.clip((1.0 - r) / 0.08, 0, 1) ** 0.5 if mask.ndim == 2 else mask


def soft_glow():
    _, _, r = grid()
    # A bright centre on a long tail that reaches the tile edge reads as light, not as a flat disc.
    return 0.45 * np.exp(-(r / 0.18) ** 2) + 0.55 * np.clip(1 - r, 0, 1) ** 1.7


def hot_core():
    _, _, r = grid()
    return np.clip(1.15 - (r / 0.34) ** 2.4, 0, 1) ** 0.8 + 0.25 * np.exp(-(r / 0.5) ** 2)


def ring():
    _, _, r = grid()
    return np.exp(-((r - 0.72) / 0.07) ** 2) + 0.18 * np.exp(-((r - 0.6) / 0.2) ** 2)


def streak():
    x, y, _ = grid()
    # Long along x, thin along y, brightest a little ahead of centre like a motion smear.
    along = np.exp(-((x - 0.15) / 0.62) ** 4)
    across = np.exp(-(y / 0.11) ** 2)
    return along * across


def star():
    x, y, r = grid()
    rays = np.exp(-(np.abs(y) / 0.035)) * np.exp(-(np.abs(x) / 0.65)) + np.exp(-(np.abs(x) / 0.035)) * np.exp(-(np.abs(y) / 0.65))
    d = (x + y) / np.sqrt(2), (x - y) / np.sqrt(2)
    diagonal = 0.35 * (np.exp(-(np.abs(d[1]) / 0.03)) * np.exp(-(np.abs(d[0]) / 0.35)) + np.exp(-(np.abs(d[0]) / 0.03)) * np.exp(-(np.abs(d[1]) / 0.35)))
    return np.clip(rays + diagonal + np.exp(-(r / 0.12) ** 2), 0, 1)


def wisp():
    # Smooth value noise: random grids upscaled bicubically, larger octaves weighted more.
    rng = np.random.default_rng(7)
    _, _, r = grid()
    noise = np.zeros_like(r)
    for cells, weight in ((4, 1.0), (8, 0.5), (16, 0.25)):
        small = Image.fromarray((rng.random((cells, cells)) * 255).astype(np.uint8), 'L')
        noise += weight * np.asarray(small.resize((TILE, TILE), Image.BICUBIC), dtype=np.float64) / 255
    noise = (noise - noise.min()) / (noise.max() - noise.min())
    return np.clip(noise * 1.6 - 0.35, 0, 1) ** 1.3 * np.exp(-(r / 0.6) ** 2)


def shock():
    _, _, r = grid()
    # A bright leading edge with a faint trailing fill, for expanding pressure waves.
    lead = np.exp(-((r - 0.8) / 0.045) ** 2)
    trail = np.where(r < 0.8, (r / 0.8) ** 3 * 0.35, 0)
    return lead + trail


def lens():
    x, y, r = grid()
    return np.exp(-(y / 0.035) ** 2) * np.exp(-(np.abs(x) / 0.45)) + 0.6 * np.exp(-(r / 0.1) ** 2)


def main(assets):
    atlas = np.zeros((TILE, TILE * TILES), dtype=np.float64)
    for index, tile in enumerate((soft_glow, hot_core, ring, streak, star, wisp, shock, lens)):
        atlas[:, index * TILE:(index + 1) * TILE] = edge_fade(np.clip(tile(), 0, 1))

    value = (np.clip(atlas, 0, 1) * 255).round().astype(np.uint8)
    rgba = np.stack([value, value, value, value], axis=-1)
    out = pathlib.Path(assets) / 'textures/vfx/glow_atlas.png'
    Image.fromarray(rgba, 'RGBA').save(out)
    out.with_suffix('.png.mcmeta').write_text('{"texture":{"blur":true,"clamp":true}}\n', encoding='utf-8')
    print('wrote', out)


if __name__ == '__main__':
    main(sys.argv[1] if len(sys.argv) > 1 else 'src/main/resources/assets/jjkcraft')
