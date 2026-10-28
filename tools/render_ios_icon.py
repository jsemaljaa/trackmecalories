"""Renders the iOS app icon (1024x1024 PNG, no transparency) from the Android launcher icon.

Draws the same shapes as res/drawable/ic_launcher_background.xml + ic_launcher_foreground.xml
(gradient square, ring track, sweep-gradient progress arc, leaf) at 4x and scales down. The
1024 px square shows the 72 dp an Android launcher shows of the 108 dp adaptive canvas; iOS
rounds the corners itself. Re-run after changing the Android icon:

    python tools/render_ios_icon.py
"""
import math
import pathlib

from PIL import Image, ImageDraw

OUT = pathlib.Path(__file__).resolve().parent.parent / 'iosApp/TrackMeCalories/Assets.xcassets/AppIcon.appiconset/icon-1024.png'
SIZE = 1024
SS = 4                      # supersampling
PX = SIZE * SS
VIEW_MIN, VIEW_MAX = 18.0, 90.0  # the part of the 108 dp canvas a launcher shows


def p(v):
    """Android viewport units -> supersampled pixels."""
    return (v - VIEW_MIN) / (VIEW_MAX - VIEW_MIN) * PX


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(len(a)))


def hexc(s):
    s = s.lstrip('#')
    return tuple(int(s[i:i + 2], 16) for i in (2, 4, 6, 0))  # AARRGGBB -> RGBA


img = Image.new('RGBA', (PX, PX))

# Background: vertical gradient #0E6B45 -> #05391F over the full 108 canvas.
top, bottom = hexc('#FF0E6B45'), hexc('#FF05391F')
bg = ImageDraw.Draw(img)
for y in range(PX):
    v = VIEW_MIN + y / PX * (VIEW_MAX - VIEW_MIN)
    bg.line([(0, y), (PX, y)], fill=mix(top, bottom, v / 108))


def stroke_arc(layer, start_deg, sweep_deg, color_at, width, steps=1440):
    """Arc of radius 22 around (54,54), drawn as round dots; angles clockwise from 3 o'clock."""
    d = ImageDraw.Draw(layer)
    r = p(54 + 22) - p(54)
    w = (p(width) - p(0)) / 2
    for i in range(steps + 1):
        a = start_deg + sweep_deg * i / steps
        x = p(54) + r * math.cos(math.radians(a))
        y = p(54) + r * math.sin(math.radians(a))
        d.ellipse([x - w, y - w, x + w, y + w], fill=color_at(a))


# Ring track: white at 0x29 alpha.
track = Image.new('RGBA', (PX, PX))
stroke_arc(track, 0, 360, lambda a: (255, 255, 255, 255), 6.5)
track.putalpha(track.getchannel('A').point(lambda v: v * 0x29 // 255))
img.alpha_composite(track)

# Progress arc: 290 degrees from 12 o'clock, sweep gradient mint -> amber -> mint, measured
# from 12 o'clock (the group's -90 degree rotation).
mint, amber = hexc('#FFA6F2C5'), hexc('#FFF6C453')
stops = [(0.0, mint), (0.806, amber), (0.88, amber), (0.93, mint), (1.0, mint)]


def sweep(a):
    t = ((a + 90) % 360) / 360
    for (o1, c1), (o2, c2) in zip(stops, stops[1:]):
        if t <= o2:
            return mix(c1, c2, (t - o1) / (o2 - o1) if o2 > o1 else 0)
    return stops[-1][1]


stroke_arc(img, -90, 290, sweep, 6.5)


def bezier(p0, p1, p2, p3, n=200):
    return [tuple((1 - t) ** 3 * p0[k] + 3 * (1 - t) ** 2 * t * p1[k] + 3 * (1 - t) * t ** 2 * p2[k] + t ** 3 * p3[k] for k in (0, 1))
            for t in (i / n for i in range(n + 1))]


# Leaf (two cubic curves) with its vein cut out (evenOdd).
leaf = bezier((44.2, 63.8), (44.2, 50.6), (52.3, 43.6), (64.4, 43.6)) + bezier((64.4, 43.6), (64.4, 55.7), (57.5, 63.8), (44.2, 63.8))
vein = [(45.86, 60.86), (57.36, 49.36), (58.64, 50.64), (47.14, 62.14)]
mask = Image.new('L', (PX, PX))
md = ImageDraw.Draw(mask)
md.polygon([(p(x), p(y)) for x, y in leaf], fill=255)
md.polygon([(p(x), p(y)) for x, y in vein], fill=0)
img.paste(Image.new('RGBA', (PX, PX), hexc('#FFF4FFF8')), mask=mask)

OUT.parent.mkdir(parents=True, exist_ok=True)
img.resize((SIZE, SIZE), Image.LANCZOS).convert('RGB').save(OUT)
print('wrote', OUT)
