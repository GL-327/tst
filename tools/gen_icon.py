"""Draws the original Jujutsu Craft mod icon (an ink-brush ring with a cursed red slash).

Usage: python tools/gen_icon.py <output.png>
"""
import math
import random
import sys

from PIL import Image, ImageDraw, ImageFilter

SIZE = 512  # drawn large, scaled down for smooth edges
OUT = 128


def main(path):
    rnd = random.Random(26_3)
    img = Image.new("RGBA", (SIZE, SIZE), (13, 11, 18, 255))
    draw = ImageDraw.Draw(img)

    # Soft violet glow in the centre.
    glow = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glow)
    gdraw.ellipse((96, 96, SIZE - 96, SIZE - 96), fill=(92, 40, 160, 150))
    glow = glow.filter(ImageFilter.GaussianBlur(60))
    img = Image.alpha_composite(img, glow)
    draw = ImageDraw.Draw(img)

    # Enso-style brush ring: overlapping dabs whose width tapers along the stroke.
    cx = cy = SIZE / 2
    radius = 170
    start, sweep = math.radians(-60), math.radians(320)
    steps = 900
    for i in range(steps):
        t = i / steps
        a = start + sweep * t
        width = 34 * (1 - t) ** 0.6 + 6
        jitter = rnd.uniform(-3, 3)
        x = cx + (radius + jitter) * math.cos(a)
        y = cy + (radius + jitter) * math.sin(a)
        shade = int(230 - 60 * t)
        draw.ellipse((x - width / 2, y - width / 2, x + width / 2, y + width / 2),
                     fill=(shade, shade - 8, shade + 10, 255))

    # Cursed red slash across the ring.
    slash = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    sdraw = ImageDraw.Draw(slash)
    for i in range(400):
        t = i / 400
        x = 120 + t * 280
        y = 380 - t * 250
        w = 22 * math.sin(math.pi * t) + 2
        sdraw.ellipse((x - w / 2, y - w / 2, x + w / 2, y + w / 2), fill=(214, 34, 52, 255))
    slash_glow = slash.filter(ImageFilter.GaussianBlur(10))
    img = Image.alpha_composite(img, slash_glow)
    img = Image.alpha_composite(img, slash)

    img.resize((OUT, OUT), Image.LANCZOS).save(path)


if __name__ == "__main__":
    main(sys.argv[1])
