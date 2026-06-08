from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parent
OUT = ROOT / "media" / "overlays"
OUT.mkdir(parents=True, exist_ok=True)

W, H = 1080, 1920
BLUE = "#0D47A1"
BLUE_INK = "#08316F"
CREAM = "#FFF4D6"
LAVENDER = "#EDE6F5"
RED = "#E83A33"
TEXT = "#1B1B1F"
MUTED = "#5D5A66"
WHITE = "#F7FBFF"

FONT_ROUNDED = "/System/Library/Fonts/Supplemental/Arial Rounded Bold.ttf"
FONT_BOLD = "/System/Library/Fonts/Supplemental/Arial Bold.ttf"


def font(path, size):
    try:
        return ImageFont.truetype(path, size)
    except OSError:
        return ImageFont.load_default(size=size)


def text_size(draw, text, face):
    box = draw.textbbox((0, 0), text, font=face)
    return box[2] - box[0], box[3] - box[1]


def draw_wrapped(draw, xy, text, face, fill, max_width, line_gap=10):
    x, y = xy
    words = text.split()
    lines = []
    current = []
    for word in words:
        candidate = " ".join(current + [word])
        if text_size(draw, candidate, face)[0] <= max_width or not current:
            current.append(word)
        else:
            lines.append(" ".join(current))
            current = [word]
    if current:
        lines.append(" ".join(current))

    for line in lines:
        draw.text((x, y), line, font=face, fill=fill)
        y += text_size(draw, line, face)[1] + line_gap
    return y


def rounded(draw, box, radius, fill, outline=None, width=1):
    draw.rounded_rectangle(box, radius=radius, fill=fill, outline=outline, width=width)


def pill(draw, xy, text, face, fill=(255, 255, 255, 222), text_fill=BLUE_INK, pad_x=28, pad_y=16, outline=(13, 71, 161, 44)):
    x, y = xy
    tw, th = text_size(draw, text, face)
    box = (x, y, x + tw + pad_x * 2, y + th + pad_y * 2)
    rounded(draw, (box[0] + 6, box[1] + 10, box[2] + 6, box[3] + 10), 999, (8, 49, 111, 35))
    rounded(draw, box, 999, fill, outline, 3)
    draw.text((x + pad_x, y + pad_y - 2), text, font=face, fill=text_fill)
    return box


def dice(draw, xy, label, size=132):
    x, y = xy
    rounded(draw, (x + 8, y + 14, x + size + 8, y + size + 14), 34, (8, 49, 111, 38))
    rounded(draw, (x, y, x + size, y + size), 34, (253, 254, 254, 240), BLUE, 7)
    face = font(FONT_ROUNDED, int(size * 0.48))
    tw, th = text_size(draw, label, face)
    draw.text((x + (size - tw) / 2, y + (size - th) / 2 - 8), label, font=face, fill=RED if label == "6" else TEXT)


def button(draw, xy, text):
    face = font(FONT_BOLD, 31)
    x, y = xy
    tw, th = text_size(draw, text, face)
    box = (x, y, x + tw + 68, y + 70)
    rounded(draw, (box[0] + 8, box[1] + 14, box[2] + 8, box[3] + 14), 999, (8, 49, 111, 50))
    rounded(draw, box, 999, BLUE)
    draw.text((x + 34, y + (70 - th) / 2 - 3), text, font=face, fill=WHITE)
    return box


def mini_pill(draw, xy, text):
    return pill(
        draw,
        xy,
        text,
        font(FONT_BOLD, 25),
        fill=(237, 230, 245, 228),
        text_fill=TEXT,
        pad_x=21,
        pad_y=12,
        outline=(13, 71, 161, 34),
    )


def wash(draw, top=True, bottom=True, bottom_y=1458):
    if top:
        draw.rectangle((0, 0, W, 300), fill=(243, 248, 255, 168))
    if bottom:
        draw.rectangle((0, bottom_y, W, H), fill=(243, 248, 255, 226))
    draw.rectangle((0, 0, 70, H), fill=(8, 49, 111, 22))
    draw.rectangle((W - 70, 0, W, H), fill=(8, 49, 111, 22))


def headline(draw, xy, title, subtitle, size=78, max_width=880):
    y = draw_wrapped(draw, xy, title, font(FONT_ROUNDED, size), TEXT, max_width, line_gap=10)
    draw_wrapped(draw, (xy[0], y + 24), subtitle, font(FONT_BOLD, 36), MUTED, max_width, line_gap=8)


def transparent():
    return Image.new("RGBA", (W, H), (0, 0, 0, 0))


def scene_hero():
    img = transparent()
    draw = ImageDraw.Draw(img)
    wash(draw, bottom_y=1450)
    pill(draw, (78, 78), "Snake & Ladder", font(FONT_ROUNDED, 30), pad_x=24, pad_y=14)
    dice(draw, (862, 76), "1", size=126)
    headline(draw, (80, 1486), "Roll. Climb. Dodge.", "A classic board race built for quick mobile matches.", size=82)
    button(draw, (80, 1760), "Start a game")
    mini_pill(draw, (454, 1768), "2-4 players")
    img.save(OUT / "01_hero.png")


def scene_settings():
    img = transparent()
    draw = ImageDraw.Draw(img)
    wash(draw, bottom_y=1448)
    pill(draw, (80, 82), "Match controls", font(FONT_ROUNDED, 30), pad_x=24, pad_y=14)
    dice(draw, (862, 76), "6", size=126)
    headline(draw, (80, 1488), "Tune every round.", "Save, restart, pause, and adjust the match without losing the board.", size=68)
    x = 80
    for label in ["Rules", "Visuals", "Audio"]:
        box = mini_pill(draw, (x, 1772), label)
        x = box[2] + 18
    img.save(OUT / "02_settings.png")


def scene_campaign():
    img = transparent()
    draw = ImageDraw.Draw(img)
    wash(draw, top=True, bottom=True, bottom_y=1270)
    pill(draw, (80, 74), "Quest map", font(FONT_ROUNDED, 30), pad_x=24, pad_y=14)
    headline(draw, (80, 1304), "Progress beyond one match.", "Campaign nodes, bosses, and daily goals keep the race moving.", size=62)
    panel = (80, 1540, 1000, 1830)
    rounded(draw, (panel[0] + 10, panel[1] + 18, panel[2] + 10, panel[3] + 18), 42, (8, 49, 111, 42))
    rounded(draw, panel, 42, (255, 255, 255, 220), (13, 71, 161, 36), 3)
    face = font(FONT_BOLD, 30)
    labels = ["Campaign", "Daily goals", "Boss nodes", "Rewards"]
    positions = [(116, 1574), (558, 1574), (116, 1690), (558, 1690)]
    for label, (x, y) in zip(labels, positions):
        rounded(draw, (x, y, x + 386, y + 110), 30, LAVENDER)
        tw, th = text_size(draw, label, face)
        draw.text((x + (386 - tw) / 2, y + (110 - th) / 2 - 2), label, font=face, fill=TEXT)
    img.save(OUT / "03_campaign.png")


def scene_menu():
    img = transparent()
    draw = ImageDraw.Draw(img)
    wash(draw, top=True, bottom=True, bottom_y=1456)
    pill(draw, (80, 74), "Quick launch", font(FONT_ROUNDED, 30), pad_x=24, pad_y=14)
    headline(draw, (80, 1450), "Pick your way to play.", "Local play, bot matches, campaign, saved games, store, and guide.", size=66)
    x = 80
    for label in ["Quick Start", "New Game", "Progress"]:
        box = mini_pill(draw, (x, 1782), label)
        x = box[2] + 16
    img.save(OUT / "04_menu.png")


def scene_close():
    img = transparent()
    draw = ImageDraw.Draw(img)
    draw.rectangle((0, 0, W, H), fill=(243, 248, 255, 244))
    draw.rectangle((0, 300, W, 1230), fill=(255, 255, 255, 244))
    dice(draw, (422, 360), "6", size=236)
    title = "Snake & Ladder"
    title_face = font(FONT_ROUNDED, 104)
    tw, th = text_size(draw, title, title_face)
    draw.text(((W - tw) / 2, 690), title, font=title_face, fill=TEXT)
    subtitle = "Roll into a classic race with modern mobile modes."
    subtitle_face = font(FONT_BOLD, 38)
    y = draw_wrapped(draw, (190, 838), subtitle, subtitle_face, TEXT, 700, line_gap=8)
    del y
    button(draw, (255, 1022), "Play the climb to 100")
    img.save(OUT / "05_close.png")


if __name__ == "__main__":
    scene_hero()
    scene_settings()
    scene_campaign()
    scene_menu()
    scene_close()
    print(f"Wrote overlays to {OUT}")
