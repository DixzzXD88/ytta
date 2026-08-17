import os

try:
    from PIL import Image, ImageDraw
except ImportError:
    os.system("pip install Pillow")
    from PIL import Image, ImageDraw

ICON_SIZES = {
    "mdpi":    48,
    "hdpi":    72,
    "xhdpi":   96,
    "xxhdpi":  144,
    "xxxhdpi": 192
}

THEMES = {
    "ic_theme_whatsapp":  "whatsapp.jpg",
    "ic_theme_youtube":   "youtube.jpg",
    "ic_theme_instagram": "instagram.jpg",
    "ic_theme_telegram":  "telegram.jpg",
    "ic_theme_xnxx":      "xnxx.jpg",
}

def load_img(path):
    img = Image.open(path)
    if img.mode == "RGBA":
        bg = Image.new("RGB", img.size, (0, 0, 0))
        bg.paste(img, mask=img.split()[3])
        return bg
    elif img.mode != "RGB":
        return img.convert("RGB")
    return img

def make_placeholder(size):
    icon = Image.new("RGB", (size, size), (8, 12, 16))
    draw = ImageDraw.Draw(icon)
    draw.ellipse([size//4, size//4, 3*size//4, 3*size//4], fill=(0, 212, 255))
    return icon

# ── Default icon (sync.jpg) ──
use_icon = os.path.isfile("sync.jpg")
img_base = load_img("sync.jpg") if use_icon else None

for density, size in ICON_SIZES.items():
    out_dir = "android/app/src/main/res/mipmap-" + density
    os.makedirs(out_dir, exist_ok=True)

    icon = img_base.resize((size, size), Image.LANCZOS) if use_icon else make_placeholder(size)
    icon.save(os.path.join(out_dir, "ic_launcher.png"), "PNG")
    icon.save(os.path.join(out_dir, "ic_launcher_round.png"), "PNG")
    print(f"Default {density}: {size}x{size}")

    # ── Theme icons ──
    for name, src_file in THEMES.items():
        if os.path.isfile(src_file):
            theme_img = load_img(src_file).resize((size, size), Image.LANCZOS)
        else:
            print(f"  WARNING: {src_file} tidak ditemukan, pakai placeholder untuk {name}")
            theme_img = make_placeholder(size)

        theme_img.save(os.path.join(out_dir, f"{name}.png"), "PNG")
        print(f"  Theme {name} {density}: {size}x{size}")

print("Icons done")
