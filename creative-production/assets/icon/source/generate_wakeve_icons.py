#!/usr/bin/env python3
"""Generate Wakeve icon PNG variants from deterministic vector geometry."""

from __future__ import annotations

import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter


ROOT = Path(__file__).resolve().parent
OUT = ROOT
IOS_ICONSET = ROOT.parents[3] / "iosApp" / "src" / "Assets.xcassets" / "AppIcon.appiconset"
SIZE = 1024
SYMBOL_SCALE = 4


def lerp(a: int, b: int, t: float) -> int:
    return round(a + (b - a) * t)


def hex_rgb(value: str) -> tuple[int, int, int]:
    value = value.strip("#")
    return tuple(int(value[i : i + 2], 16) for i in (0, 2, 4))


def gradient(size: int, stops: list[tuple[float, str]], vertical_bias: float = 1.0) -> Image.Image:
    img = Image.new("RGBA", (size, size))
    pix = img.load()
    parsed = [(p, hex_rgb(c)) for p, c in stops]
    for y in range(size):
        for x in range(size):
            nx = x / (size - 1)
            ny = y / (size - 1)
            t = min(1.0, max(0.0, (nx * 0.38 + ny * 0.62) * vertical_bias))
            for idx, (pos, color) in enumerate(parsed):
                if t <= pos:
                    p0, c0 = parsed[max(0, idx - 1)]
                    p1, c1 = pos, color
                    span = max(0.001, p1 - p0)
                    local = (t - p0) / span
                    rgb = tuple(lerp(c0[i], c1[i], local) for i in range(3))
                    pix[x, y] = (*rgb, 255)
                    break
            else:
                pix[x, y] = (*parsed[-1][1], 255)
    return img


def add_radial_glow(img: Image.Image, center: tuple[float, float], color: str, radius: float, strength: float) -> None:
    glow = Image.new("RGBA", img.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(glow, "RGBA")
    cx, cy = center
    rgb = hex_rgb(color)
    steps = 90
    for i in range(steps, 0, -1):
        r = radius * i / steps
        alpha = int(255 * strength * (i / steps) ** 2)
        draw.ellipse((cx - r, cy - r, cx + r, cy + r), fill=(*rgb, alpha))
    img.alpha_composite(glow.filter(ImageFilter.GaussianBlur(18)))


def cubic(p0: tuple[float, float], p1: tuple[float, float], p2: tuple[float, float], p3: tuple[float, float], n: int = 80) -> list[tuple[float, float]]:
    pts = []
    for i in range(n + 1):
        t = i / n
        mt = 1 - t
        x = mt**3 * p0[0] + 3 * mt**2 * t * p1[0] + 3 * mt * t**2 * p2[0] + t**3 * p3[0]
        y = mt**3 * p0[1] + 3 * mt**2 * t * p1[1] + 3 * mt * t**2 * p2[1] + t**3 * p3[1]
        pts.append((x, y))
    return pts


def scale_points(points: list[tuple[float, float]], scale: int) -> list[tuple[float, float]]:
    return [(x * scale, y * scale) for x, y in points]


def tube_polygon(points: list[tuple[float, float]], width: int) -> list[tuple[float, float]]:
    left: list[tuple[float, float]] = []
    right: list[tuple[float, float]] = []
    half = width / 2
    for i, (x, y) in enumerate(points):
        p0 = points[max(0, i - 1)]
        p1 = points[min(len(points) - 1, i + 1)]
        dx = p1[0] - p0[0]
        dy = p1[1] - p0[1]
        length = math.hypot(dx, dy) or 1
        nx = -dy / length
        ny = dx / length
        left.append((x + nx * half, y + ny * half))
        right.append((x - nx * half, y - ny * half))
    return left + list(reversed(right))


def draw_glass_path(layer: Image.Image, points: list[tuple[float, float]], width: int, color: tuple[int, int, int, int], highlight: tuple[int, int, int, int]) -> None:
    poly = tube_polygon(points, width)
    shadow = Image.new("RGBA", layer.size, (0, 0, 0, 0))
    sd = ImageDraw.Draw(shadow, "RGBA")
    sd.polygon(poly, fill=(5, 12, 26, 82))
    cap = width / 2
    for x, y in (points[0], points[-1]):
        sd.ellipse((x - cap, y - cap, x + cap, y + cap), fill=(5, 12, 26, 82))
    shadow = shadow.filter(ImageFilter.GaussianBlur(14 * SYMBOL_SCALE))
    layer.alpha_composite(shadow, (0, 18))

    body = Image.new("RGBA", layer.size, (0, 0, 0, 0))
    bd = ImageDraw.Draw(body, "RGBA")
    bd.polygon(poly, fill=color)
    for x, y in (points[0], points[-1]):
        bd.ellipse((x - cap, y - cap, x + cap, y + cap), fill=color)
    body = body.filter(ImageFilter.GaussianBlur(0.25 * SYMBOL_SCALE))
    layer.alpha_composite(body)

    hd = ImageDraw.Draw(layer, "RGBA")
    inset_points = [(x - width * 0.08, y - width * 0.16) for x, y in points]
    hd.line(inset_points, fill=highlight, width=max(12 * SYMBOL_SCALE, width // 7))


def symbol_layer(tint: str = "dark") -> Image.Image:
    canvas = SIZE * SYMBOL_SCALE
    layer = Image.new("RGBA", (canvas, canvas), (0, 0, 0, 0))
    if tint == "light":
        colors = [
            (166, 191, 224, 188),
            (205, 184, 232, 178),
            (242, 177, 141, 150),
        ]
        highlight = (255, 255, 255, 148)
    elif tint == "tinted":
        colors = [
            (244, 247, 244, 218),
            (198, 183, 232, 210),
            (142, 169, 214, 200),
        ]
        highlight = (255, 255, 255, 178)
    else:
        colors = [
            (244, 247, 244, 220),
            (190, 211, 239, 198),
            (198, 183, 232, 188),
        ]
        highlight = (255, 255, 255, 170)

    left = cubic((206, 318), (268, 462), (326, 608), (404, 728), 80)
    mid = cubic((404, 728), (474, 586), (526, 462), (594, 314), 80)
    right = cubic((594, 314), (654, 462), (708, 598), (792, 724), 80)

    sleft = scale_points(left, SYMBOL_SCALE)
    smid = scale_points(mid, SYMBOL_SCALE)
    sright = scale_points(right, SYMBOL_SCALE)
    swidth = 132 * SYMBOL_SCALE
    shighlight = tuple(highlight)
    draw_glass_path(layer, sleft, swidth, colors[0], shighlight)
    draw_glass_path(layer, smid, swidth, colors[1], shighlight)
    draw_glass_path(layer, sright, swidth, colors[2], shighlight)

    sparkle = Image.new("RGBA", (canvas, canvas), (0, 0, 0, 0))
    d = ImageDraw.Draw(sparkle, "RGBA")
    for x, y, r, a in [(702, 284, 12, 86), (292, 712, 10, 70), (826, 604, 8, 62)]:
        x *= SYMBOL_SCALE
        y *= SYMBOL_SCALE
        r *= SYMBOL_SCALE
        d.ellipse((x - r, y - r, x + r, y + r), fill=(255, 245, 226, a))
    layer.alpha_composite(sparkle.filter(ImageFilter.GaussianBlur(1.2 * SYMBOL_SCALE)))
    return layer.resize((SIZE, SIZE), Image.Resampling.LANCZOS)


def make_icon(kind: str) -> Image.Image:
    if kind == "light":
        img = gradient(SIZE, [(0, "#F6F3EB"), (0.45, "#DFE9F0"), (1, "#C7D4E7")])
        add_radial_glow(img, (760, 750), "#F2B18D", 330, 0.28)
        img.alpha_composite(symbol_layer("light"))
    elif kind == "tinted-light":
        img = gradient(SIZE, [(0, "#F7F8F2"), (0.55, "#DEE8F4"), (1, "#C6B7E8")])
        add_radial_glow(img, (730, 720), "#F0B58E", 300, 0.20)
        img.alpha_composite(symbol_layer("tinted"))
    elif kind == "tinted-dark":
        img = gradient(SIZE, [(0, "#131A2D"), (0.54, "#23252D"), (1, "#292226")])
        add_radial_glow(img, (760, 746), "#E3B765", 340, 0.25)
        img.alpha_composite(symbol_layer("tinted"))
    else:
        img = gradient(SIZE, [(0, "#111A2E"), (0.52, "#202329"), (1, "#2B2426")])
        add_radial_glow(img, (776, 760), "#F2B18D", 350, 0.31)
        add_radial_glow(img, (248, 206), "#8EA9D6", 280, 0.12)
        img.alpha_composite(symbol_layer("dark"))

    return img.convert("RGB")


def save() -> None:
    outputs = {
        "wakeve-icon-1024-dark.png": make_icon("dark"),
        "wakeve-icon-1024-light.png": make_icon("light"),
        "wakeve-icon-tinted-dark.png": make_icon("tinted-dark"),
        "wakeve-icon-tinted-light.png": make_icon("tinted-light"),
    }
    for name, image in outputs.items():
        image.save(OUT / name, optimize=True)

    IOS_ICONSET.mkdir(parents=True, exist_ok=True)
    outputs["wakeve-icon-1024-light.png"].save(IOS_ICONSET / "AppIcon.png", optimize=True)
    outputs["wakeve-icon-1024-dark.png"].save(IOS_ICONSET / "AppIconDark.png", optimize=True)
    outputs["wakeve-icon-tinted-dark.png"].save(IOS_ICONSET / "AppIconTinted.png", optimize=True)
    make_size_check(outputs).save(OUT / "wakeve-icon-size-check.png", optimize=True)


def make_size_check(outputs: dict[str, Image.Image]) -> Image.Image:
    sizes = [180, 120, 60, 40]
    labels = [
        ("Dark", outputs["wakeve-icon-1024-dark.png"]),
        ("Light", outputs["wakeve-icon-1024-light.png"]),
        ("Tinted Dark", outputs["wakeve-icon-tinted-dark.png"]),
        ("Tinted Light", outputs["wakeve-icon-tinted-light.png"]),
    ]
    margin = 40
    row_h = 230
    col_w = 230
    width = margin * 2 + 160 + col_w * len(sizes)
    height = margin * 2 + row_h * len(labels)
    sheet = Image.new("RGB", (width, height), "#FFFFFF")
    draw = ImageDraw.Draw(sheet)
    for col, size in enumerate(sizes):
        draw.text((margin + 160 + col * col_w, 16), f"{size}px", fill="#222222")
    for row, (label, source) in enumerate(labels):
        y = margin + row * row_h
        draw.text((margin, y + 70), label, fill="#222222")
        for col, size in enumerate(sizes):
            icon = source.resize((size, size), Image.Resampling.LANCZOS)
            x = margin + 160 + col * col_w
            sheet.paste(icon, (x, y + 35))
    return sheet


if __name__ == "__main__":
    save()
