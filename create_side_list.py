import os
from PIL import Image, ImageDraw, ImageFont

def create_side_list():
    # Native 1:1 scale matching default.json & uniform.json height 266
    WIDTH = 264
    HEIGHT = 266

    img = Image.new('RGBA', (WIDTH, HEIGHT), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Palette — High contrast, vivid, perfectly readable in Minecraft Anvil GUI
    bg_color = (20, 16, 24, 252)          # Deep dark violet-black
    bg_header = (48, 28, 44, 255)         # Header banner
    border_dark = (10, 6, 12, 255)
    border_main = (245, 145, 185, 255)    # Bright sakura pink
    border_highlight = (255, 215, 235, 220)
    text_white = (255, 255, 255, 255)     # Crisp white
    text_shadow = (10, 5, 12, 255)        # Sharp drop shadow
    text_gold = (255, 225, 130, 255)      # Bright gold
    divider_col = (90, 60, 80, 220)       # Section divider

    # Outer border & background
    draw.rectangle([0, 0, WIDTH - 1, HEIGHT - 1], fill=bg_color, outline=border_dark, width=1)
    draw.rectangle([1, 1, WIDTH - 2, HEIGHT - 2], outline=border_main, width=1)
    draw.rectangle([2, 2, WIDTH - 3, HEIGHT - 3], outline=border_highlight, width=1)

    # Header section
    draw.rectangle([3, 3, WIDTH - 4, 19], fill=bg_header)
    draw.line([(3, 20), (WIDTH - 4, 20)], fill=border_main, width=1)

    font = ImageFont.load_default()

    # Blossom mini icon at header corners
    def draw_blossom(x, y):
        p = (255, 175, 210, 255)
        c = (255, 240, 170, 255)
        draw.point((x, y-1), fill=p)
        draw.point((x, y+1), fill=p)
        draw.point((x-1, y), fill=p)
        draw.point((x+1, y), fill=p)
        draw.point((x, y), fill=c)

    draw_blossom(10, 11)
    draw_blossom(WIDTH - 12, 11)

    title_text = "SAKURA RENAME LIST"
    tw = int(font.getlength(title_text))
    tx = (WIDTH - tw) // 2
    draw.text((tx + 1, 6), title_text, fill=text_shadow, font=font)
    draw.text((tx, 5), title_text, fill=text_gold, font=font)

    # Columns Divider
    mid_x = WIDTH // 2  # 132 px
    draw.line([(mid_x, 21), (mid_x, HEIGHT - 18)], fill=divider_col, width=1)

    # Hint color themes
    c_sword = (255, 185, 210)   # Pastel pink
    c_heavy = (255, 215, 130)   # Gold / amber
    c_tool  = (140, 225, 255)   # Ice cyan
    c_bow   = (160, 255, 180)   # Mint green
    c_armor = (235, 195, 255)   # Lavender
    c_cosm  = (255, 175, 240)   # Magenta

    # Column 1: Weapons & Upper Armor (Sakura + Pink Legacy)
    weapons = [
        ("Katana", "Sword", (255, 130, 160), c_sword),
        ("Sword", "Sword", (255, 130, 160), c_sword),
        ("Bigsword", "Sword", (255, 130, 160), c_sword),
        ("Dagger", "Sword", (255, 130, 160), c_sword),
        ("Gauntlet", "Sword", (255, 160, 130), c_sword),
        ("Spear", "Sw/Sp/Tri", (255, 130, 160), c_sword),
        ("Halberd", "Sword/Ax", (255, 130, 160), c_sword),
        ("Hammer", "Mace/Ax", (240, 180, 120), c_heavy),
        ("Mace", "Mace", (240, 180, 120), c_heavy),
        ("Club", "Sword", (255, 130, 160), c_sword),
        ("Key", "Paper/Hk", (255, 215, 120), c_cosm),
        # Pink Legacy
        ("PL Sword", "Sword", (255, 160, 200), c_sword),
        ("PL Spear", "Sw/Sp", (255, 160, 200), c_sword),
        ("PL Halberd", "Sw/Ax", (255, 160, 200), c_sword),
        ("PL Hammer", "Mace/Ax", (255, 190, 140), c_heavy),
        ("PL BAxe", "Axe", (255, 190, 140), c_heavy),
        ("PL Staff", "Sword", (220, 170, 255), c_sword),
        ("PL Key", "Paper/Hk", (255, 220, 130), c_cosm),
        ("PL Helm", "Helmet", (255, 180, 220), c_armor),
        ("PL Chest", "Chest", (255, 180, 220), c_armor),
    ]

    # Column 2: Tools, Lower Armor & Gear (Sakura + Pink Legacy)
    tools = [
        ("Pickaxe", "Pickaxe", (130, 210, 255), c_tool),
        ("Shovel", "Shovel", (130, 210, 255), c_tool),
        ("Axe", "Axe", (130, 210, 255), c_tool),
        ("Hoe", "Hoe", (130, 210, 255), c_tool),
        ("Bow", "Bow", (150, 230, 150), c_bow),
        ("Shield", "Shield", (200, 200, 255), c_tool),
        ("F. Rod", "Rod", (150, 220, 220), c_bow),
        ("Hat", "Hlm/Pap", (255, 200, 170), c_cosm),
        ("Wings", "Paper", (220, 175, 255), c_cosm),
        # Pink Legacy
        ("PL Pick", "Pickaxe", (160, 220, 255), c_tool),
        ("PL Shovel", "Shovel", (160, 220, 255), c_tool),
        ("PL Axe", "Axe", (160, 220, 255), c_tool),
        ("PL Hoe", "Hoe", (160, 220, 255), c_tool),
        ("PL Bow", "Bow", (175, 240, 175), c_bow),
        ("PL Shield", "Shield", (220, 220, 255), c_tool),
        ("PL F.Rod", "Rod", (175, 235, 235), c_bow),
        ("PL Wings", "Paper", (235, 190, 255), c_cosm),
        ("PL Legs", "Legs", (255, 180, 220), c_armor),
        ("PL Boots", "Boots", (255, 180, 220), c_armor),
    ]

    def draw_mini_icon(x, y, col):
        draw.rectangle([x, y, x+5, y+5], fill=(15, 10, 18, 220), outline=border_dark)
        draw.rectangle([x+1, y+1, x+4, y+4], fill=col)

    y_start = 23
    row_h = 11

    # Render Column 1 (Left: 0 .. mid_x)
    for i, (name, btype, dot_col, hint_col) in enumerate(weapons):
        y = y_start + i * row_h
        draw_mini_icon(6, y + 2, dot_col)
        
        # Item name (crisp white with shadow)
        draw.text((16, y + 1), name, fill=text_shadow, font=font)
        draw.text((15, y), name, fill=text_white, font=font)
        
        # Base item hint (right-aligned inside Column 1)
        hint = f"({btype})"
        hw = int(font.getlength(hint))
        hx = mid_x - hw - 6
        draw.text((hx + 1, y + 1), hint, fill=text_shadow, font=font)
        draw.text((hx, y), hint, fill=hint_col, font=font)

    # Render Column 2 (Right: mid_x .. WIDTH)
    for i, (name, btype, dot_col, hint_col) in enumerate(tools):
        y = y_start + i * row_h
        x_base = mid_x + 6
        draw_mini_icon(x_base, y + 2, dot_col)
        
        # Item name
        draw.text((x_base + 11, y + 1), name, fill=text_shadow, font=font)
        draw.text((x_base + 10, y), name, fill=text_white, font=font)
        
        # Base item hint (right-aligned inside Column 2)
        hint = f"({btype})"
        hw = int(font.getlength(hint))
        hx = WIDTH - hw - 6
        draw.text((hx + 1, y + 1), hint, fill=text_shadow, font=font)
        draw.text((hx, y), hint, fill=hint_col, font=font)

    # Footer section
    draw.line([(3, HEIGHT - 18), (WIDTH - 4, HEIGHT - 18)], fill=divider_col, width=1)
    footer_text = "Sakura / Pink Legacy [ItemName]"
    fw = int(font.getlength(footer_text))
    fx = (WIDTH - fw) // 2
    draw.text((fx + 1, HEIGHT - 14), footer_text, fill=text_shadow, font=font)
    draw.text((fx, HEIGHT - 15), footer_text, fill=text_gold, font=font)

    # Save to resource pack
    for sub in ['gui', 'font']:
        out_dir = f'd:/Mod Minecraft/weapon set/sakura/sakura-resourcepack/assets/sakura/textures/{sub}'
        os.makedirs(out_dir, exist_ok=True)
        out_path = os.path.join(out_dir, 'anvil_side_list.png')
        img.save(out_path)
        print(f"Created side list texture: {out_path} ({img.size})")

    # Also save to mod assets if present
    mod_out_dir = 'd:/Mod Minecraft/weapon set/sakura/sakura-weapons/src/main/resources/assets/sakura_weapons/textures/gui'
    os.makedirs(mod_out_dir, exist_ok=True)
    mod_out_path = os.path.join(mod_out_dir, 'anvil_side_list.png')
    img.save(mod_out_path)
    print(f"Created mod side list texture: {mod_out_path}")

    # Patch right side of anvil.png
    anvil_path = 'd:/Mod Minecraft/weapon set/sakura/sakura-resourcepack/assets/minecraft/textures/gui/container/anvil.png'
    if os.path.exists(anvil_path):
        anvil_img = Image.open(anvil_path).convert('RGBA')
        compact_img = Image.new('RGBA', (80, 166), (0, 0, 0, 0))
        cdraw = ImageDraw.Draw(compact_img)
        cdraw.rectangle([0, 0, 79, 165], fill=bg_color, outline=border_dark, width=1)
        cdraw.rectangle([1, 1, 78, 164], outline=border_main, width=1)
        cdraw.rectangle([2, 2, 77, 16], fill=bg_header)
        cdraw.line([(2, 17), (77, 17)], fill=border_main, width=1)
        ctitle = "RENAME"
        ctw = int(font.getlength(ctitle))
        cdraw.text(((80 - ctw)//2, 4), ctitle, fill=text_gold, font=font)
        
        summary = [
            ("Katana", "Sword"), ("Sword", "Sword"), ("Gauntlet", "Sword"),
            ("Spear", "Sw/Sp/Tri"), ("Hammer", "Mace/Ax"), ("Pickaxe", "Pick"),
            ("Axe", "Axe"), ("Bow", "Bow"), ("Shield", "Shield"),
            ("Hat", "Hlm/Pap"), ("Wings", "Paper"), ("Key", "Paper")
        ]
        sy_start = 19
        srow_h = 11
        for i, (sname, shint) in enumerate(summary[:12]):
            sy = sy_start + i * srow_h
            cdraw.text((5, sy + 1), sname, fill=text_shadow, font=font)
            cdraw.text((4, sy), sname, fill=text_white, font=font)
            shw = int(font.getlength(shint))
            cdraw.text((75 - shw, sy + 1), shint, fill=text_shadow, font=font)
            cdraw.text((74 - shw, sy), shint, fill=c_tool, font=font)
            
        anvil_img.paste(compact_img, (176, 0), compact_img)
        anvil_img.save(anvil_path)
        print(f"Patched anvil.png at (176, 0) with compact list")

if __name__ == '__main__':
    create_side_list()
