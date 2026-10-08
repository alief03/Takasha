import os
import json
import shutil
import zlib
import struct
import zipfile

WORKSPACE = r"d:\Mod Minecraft\weapon set\sakura"
PACK_DIR = os.path.join(WORKSPACE, "sakura-resourcepack")
HAT_JSON = os.path.join(PACK_DIR, "assets", "sakura", "models", "item", "hat.json")
TEXTURE_SRC = os.path.join(PACK_DIR, "assets", "sakura", "textures", "item")

def create_transparent_png(path, width=64, height=32):
    raw = b''
    for _ in range(height):
        raw += b'\x00' + (b'\x00\x00\x00\x00' * width)
    compressed = zlib.compress(raw)
    
    png = b'\x89PNG\r\n\x1a\n'
    ihdr = struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0)
    png += struct.pack('>I', len(ihdr)) + b'IHDR' + ihdr + struct.pack('>I', zlib.crc32(b'IHDR' + ihdr))
    png += struct.pack('>I', len(compressed)) + b'IDAT' + compressed + struct.pack('>I', zlib.crc32(b'IDAT' + compressed))
    png += struct.pack('>I', 0) + b'IEND' + struct.pack('>I', zlib.crc32(b'IEND'))
    
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'wb') as f:
        f.write(png)
    print("Created PNG:", path)

def generate_jem_model():
    with open(HAT_JSON, 'r', encoding='utf-8') as f:
        hat_data = json.load(f)

    # Base translation:
    # Item model was designed in [0..16, 0..16, 0..16] with center at (8, 8, 8).
    # Head local space in CEM: neck pivot at (0, 24, 0), top at y=32, center at (0, 28, 0).
    # With parent translate: [0, -24, 0], head center is at (0, 28, 0).
    # Brim at y=2.2 + 26.0 = 28.2 (brow line). Conical top at y=14.7 + 26.0 = 40.7.
    # Ribbons drape down to y=-13.2 + 26.0 = 12.8 (shoulders).
    DX = -8.0
    DY = 26.0
    DZ = -7.0

    submodels_static = []
    submodels_animated = []

    for i, el in enumerate(hat_data['elements']):
        f = el['from']
        t = el['to']
        w = round(t[0] - f[0], 4)
        h = round(t[1] - f[1], 4)
        d = round(t[2] - f[2], 4)

        rot = el.get('rotation')
        if rot:
            angle = rot['angle']
            axis = rot['axis']
            orig = rot['origin']
        else:
            angle = 0
            axis = 'y'
            orig = [f[0], f[1], f[2]]

        tx = round(orig[0] + DX, 4)
        ty = round(orig[1] + DY, 4)
        tz = round(orig[2] + DZ, 4)

        bx = round(f[0] - orig[0], 4)
        by = round(f[1] - orig[1], 4)
        bz = round(f[2] - orig[2], 4)

        rx = angle if axis == 'x' else 0
        ry = angle if axis == 'y' else 0
        rz = angle if axis == 'z' else 0

        is_anim = (i >= 18)
        uv_mult = 2.0 if is_anim else 16.0

        box_def = {
            "coordinates": [bx, by, bz, w, h, d]
        }

        faces = el.get('faces', {})
        for face_name in ['north', 'south', 'east', 'west', 'up', 'down']:
            cem_key = "uv" + face_name.capitalize()
            if face_name in faces:
                uv = faces[face_name]['uv']
                face_rot = faces[face_name].get('rotation', 0)
                u1 = round(uv[0] * uv_mult, 2)
                v1 = round(uv[1] * uv_mult, 2)
                u2 = round(uv[2] * uv_mult, 2)
                v2 = round(uv[3] * uv_mult, 2)

                if face_rot == 180:
                    u1, v1, u2, v2 = u2, v2, u1, v1

                box_def[cem_key] = [u1, v1, u2, v2]
            else:
                box_def[cem_key] = [0, 0, 0, 0]

        sub = {
            "id": f"hat_part_{i}",
            "invertAxis": "xy",
            "translate": [tx, ty, tz],
            "rotate": [rx, ry, rz],
            "boxes": [box_def]
        }

        if is_anim:
            submodels_animated.append(sub)
        else:
            submodels_static.append(sub)

    sakura_hat_jem = {
        "credit": "Converted from sakura hat.json for ETF/EMF by Antigravity",
        "texture": "textures/sakura_texture.png",
        "textureSize": [256, 256],
        "models": [
            {
                "part": "head",
                "id": "head",
                "invertAxis": "xy",
                "translate": [0, -24, 0],
                "submodels": [
                    {
                        "id": "sakura_hat_static",
                        "invertAxis": "xy",
                        "translate": [0, 0, 0],
                        "boxes": [],
                        "submodels": submodels_static
                    },
                    {
                        "id": "sakura_hat_ribbons",
                        "invertAxis": "xy",
                        "texture": "textures/sakura_animation_01.png",
                        "textureSize": [32, 640],
                        "translate": [0, 0, 0],
                        "boxes": [],
                        "submodels": submodels_animated
                    }
                ]
            },
            {
                "part": "headwear",
                "id": "headwear",
                "invertAxis": "xy",
                "translate": [0, -24, 0]
            }
        ]
    }
    return sakura_hat_jem

def main():
    print("=== Step 1: Generating empty_armor.png & pumpkinblur.png ===")
    empty_paths = [
        os.path.join(PACK_DIR, "assets", "minecraft", "textures", "models", "armor", "empty_armor.png"),
        os.path.join(PACK_DIR, "assets", "minecraft", "optifine", "cit", "sakura", "empty_armor.png"),
        os.path.join(PACK_DIR, "assets", "minecraft", "citresewn", "cit", "sakura", "empty_armor.png"),
        os.path.join(PACK_DIR, "assets", "minecraft", "textures", "empty_armor.png"),
        os.path.join(PACK_DIR, "assets", "minecraft", "empty_armor.png"),
        os.path.join(PACK_DIR, "assets", "minecraft", "textures", "misc", "pumpkinblur.png"),
    ]
    for p in empty_paths:
        create_transparent_png(p)
        # Also copy extension-free and double-ext for maximum loader tolerance
        dir_p = os.path.dirname(p)
        shutil.copy2(p, os.path.join(dir_p, "empty_armor"))
        shutil.copy2(p, os.path.join(dir_p, "empty_armor.png.png"))

    print("\n=== Step 2: Generating Comprehensive CIT Armor properties ===")
    cit_dirs = [
        os.path.join(PACK_DIR, "assets", "minecraft", "optifine", "cit", "sakura"),
        os.path.join(PACK_DIR, "assets", "minecraft", "citresewn", "cit", "sakura"),
    ]
    
    # Valid armor items only (paper & carved_pumpkin removed to prevent CIT Resewn parser error)
    helmets_list = "minecraft:netherite_helmet minecraft:diamond_helmet minecraft:iron_helmet minecraft:golden_helmet minecraft:chainmail_helmet minecraft:leather_helmet minecraft:turtle_helmet netherite_helmet diamond_helmet iron_helmet golden_helmet chainmail_helmet leather_helmet turtle_helmet"

    # Properties with extension-free texture path (Standard OptiFine CIT spec)
    sakura_hat_armor_prop = f"""type=armor
items={helmets_list}
matchItems={helmets_list}
texture.netherite_layer_1=empty_armor
texture.diamond_layer_1=empty_armor
texture.iron_layer_1=empty_armor
texture.golden_layer_1=empty_armor
texture.gold_layer_1=empty_armor
texture.chainmail_layer_1=empty_armor
texture.leather_layer_1=empty_armor
texture.turtle_layer_1=empty_armor
texture.layer_1=empty_armor
texture=empty_armor
nbt.display.Name=ipattern:*Sakura Hat*
components.minecraft:custom_name=ipattern:*Sakura Hat*
components.custom_name=ipattern:*Sakura Hat*
"""

    topi_sakura_armor_prop = f"""type=armor
items={helmets_list}
matchItems={helmets_list}
texture.netherite_layer_1=empty_armor
texture.diamond_layer_1=empty_armor
texture.iron_layer_1=empty_armor
texture.golden_layer_1=empty_armor
texture.gold_layer_1=empty_armor
texture.chainmail_layer_1=empty_armor
texture.leather_layer_1=empty_armor
texture.turtle_layer_1=empty_armor
texture.layer_1=empty_armor
texture=empty_armor
nbt.display.Name=ipattern:*Topi Sakura*
components.minecraft:custom_name=ipattern:*Topi Sakura*
components.custom_name=ipattern:*Topi Sakura*
"""

    # Properties with explicit .png extension for loaders requiring explicit extension
    sakura_hat_armor_png = f"""type=armor
items={helmets_list}
matchItems={helmets_list}
texture.netherite_layer_1=empty_armor.png
texture.diamond_layer_1=empty_armor.png
texture.iron_layer_1=empty_armor.png
texture.golden_layer_1=empty_armor.png
texture.gold_layer_1=empty_armor.png
texture.chainmail_layer_1=empty_armor.png
texture.leather_layer_1=empty_armor.png
texture.turtle_layer_1=empty_armor.png
texture.layer_1=empty_armor.png
texture=empty_armor.png
nbt.display.Name=ipattern:*Sakura Hat*
components.minecraft:custom_name=ipattern:*Sakura Hat*
components.custom_name=ipattern:*Sakura Hat*
"""

    topi_sakura_armor_png = f"""type=armor
items={helmets_list}
matchItems={helmets_list}
texture.netherite_layer_1=empty_armor.png
texture.diamond_layer_1=empty_armor.png
texture.iron_layer_1=empty_armor.png
texture.golden_layer_1=empty_armor.png
texture.gold_layer_1=empty_armor.png
texture.chainmail_layer_1=empty_armor.png
texture.leather_layer_1=empty_armor.png
texture.turtle_layer_1=empty_armor.png
texture.layer_1=empty_armor.png
texture=empty_armor.png
nbt.display.Name=ipattern:*Topi Sakura*
components.minecraft:custom_name=ipattern:*Topi Sakura*
components.custom_name=ipattern:*Topi Sakura*
"""

    item_props_sakura_hat = """type=item
items=carved_pumpkin netherite_helmet diamond_helmet iron_helmet golden_helmet chainmail_helmet leather_helmet turtle_helmet paper
model=sakura:item/hat
nbt.display.Name=ipattern:*Sakura Hat*
components.minecraft:custom_name=ipattern:*Sakura Hat*
components.custom_name=ipattern:*Sakura Hat*
"""

    item_props_topi_sakura = """type=item
items=carved_pumpkin netherite_helmet diamond_helmet iron_helmet golden_helmet chainmail_helmet leather_helmet turtle_helmet paper
model=sakura:item/hat
nbt.display.Name=ipattern:*Topi Sakura*
components.minecraft:custom_name=ipattern:*Topi Sakura*
components.custom_name=ipattern:*Topi Sakura*
"""

    for cdir in cit_dirs:
        os.makedirs(cdir, exist_ok=True)
        with open(os.path.join(cdir, "sakura_hat.properties"), "w", encoding="utf-8") as f:
            f.write(item_props_sakura_hat)
        with open(os.path.join(cdir, "topi_sakura.properties"), "w", encoding="utf-8") as f:
            f.write(item_props_topi_sakura)
        with open(os.path.join(cdir, "sakura_hat_armor.properties"), "w", encoding="utf-8") as f:
            f.write(sakura_hat_armor_prop)
        with open(os.path.join(cdir, "topi_sakura_armor.properties"), "w", encoding="utf-8") as f:
            f.write(topi_sakura_armor_prop)
        with open(os.path.join(cdir, "sakura_hat_armor_png.properties"), "w", encoding="utf-8") as f:
            f.write(sakura_hat_armor_png)
        with open(os.path.join(cdir, "topi_sakura_armor_png.properties"), "w", encoding="utf-8") as f:
            f.write(topi_sakura_armor_png)
        print("Written armor properties in:", cdir)

    print("\n=== Step 3: Copying textures for CEM across all possible resolve paths ===")
    cem_tex_dirs = [
        os.path.join(PACK_DIR, "assets", "minecraft", "optifine", "cem", "textures"),
        os.path.join(PACK_DIR, "assets", "minecraft", "optifine", "cem"),
        os.path.join(PACK_DIR, "assets", "minecraft", "emf", "cem", "textures"),
        os.path.join(PACK_DIR, "assets", "minecraft", "emf", "cem"),
        os.path.join(PACK_DIR, "assets", "minecraft", "textures"),
        os.path.join(PACK_DIR, "assets", "minecraft", "textures", "textures"),
        os.path.join(PACK_DIR, "assets", "minecraft", "textures", "models", "armor"),
        os.path.join(PACK_DIR, "assets", "minecraft", "textures", "item"),
    ]
    for ctex in cem_tex_dirs:
        os.makedirs(ctex, exist_ok=True)
        for tname in ["sakura_texture.png", "sakura_animation_01.png", "sakura_animation_01.png.mcmeta"]:
            src = os.path.join(TEXTURE_SRC, tname)
            dst = os.path.join(ctex, tname)
            if os.path.exists(src):
                shutil.copy2(src, dst)
        print("Copied textures to:", ctex)

    print("\n=== Step 4: Generating CEM JEM models (directly populated) ===")
    sakura_hat_jem = generate_jem_model()

    base_model_names = [
        "outer_armor",
        "helmet",
        "player_outer_armor",
        "player_helmet",
        "player_slim_outer_armor",
        "player_slim_helmet",
        "armor_stand_outer_armor",
        "armor_stand_helmet",
    ]

    cem_root_dirs = [
        os.path.join(PACK_DIR, "assets", "minecraft", "optifine", "cem"),
        os.path.join(PACK_DIR, "assets", "minecraft", "emf", "cem"),
    ]

    for mdir in cem_root_dirs:
        os.makedirs(mdir, exist_ok=True)
        # Directly populate base models AND numbered variants so EMF never falls back to an empty model
        for base in base_model_names:
            for ext_name in [f"{base}.jem", f"{base}2.jem", f"{base}_sakura.jem"]:
                with open(os.path.join(mdir, ext_name), "w", encoding="utf-8") as f:
                    json.dump(sakura_hat_jem, f, indent=2)

        # Also populate entity-specific subfolders: player, player_slim, armor_stand
        for entity_sub in ["player", "player_slim", "armor_stand"]:
            sdir = os.path.join(mdir, entity_sub)
            os.makedirs(sdir, exist_ok=True)
            for sub_base in ["outer_armor.jem", "outer_armor2.jem", "helmet.jem", "helmet2.jem"]:
                with open(os.path.join(sdir, sub_base), "w", encoding="utf-8") as f:
                    json.dump(sakura_hat_jem, f, indent=2)

        # Clean up any old broken properties files that contained entity name checks (name.1)
        for prop_name in [
            "outer_armor.properties", "helmet.properties",
            "player_outer_armor.properties", "player_helmet.properties",
            "player_slim_outer_armor.properties", "player_slim_helmet.properties"
        ]:
            prop_file = os.path.join(mdir, prop_name)
            if os.path.exists(prop_file):
                os.remove(prop_file)
                print(f"Removed broken entity-name rule file: {prop_file}")

        print("Written populated JEM models in:", mdir)

    print("\n=== Step 5: Updating README.txt ===")
    readme_path = os.path.join(PACK_DIR, "README.txt")
    guide_text = """================================================================================
SAKURA HAT (TOPI SAKURA) - CUSTOM 3D HAT & ARMOR GUIDE
================================================================================
Model 3D Sakura Hat dapat digunakan dengan 2 CARA:

--------------------------------------------------------------------------------
CARA 1 (PALING MUDAH & 100% BEKERJA DI CIT RESEWN / VANILLA):
--------------------------------------------------------------------------------
1. Ambil CARVED PUMPKIN (Labu Berukir).
2. Di Anvil (Paron), ganti namanya menjadi:
   "Sakura Hat" atau "Topi Sakura"
3. Pasang di kepala (slot helm).
   -> Model 3D Topi Sakura kerucut + bunga sakura + pita animasi akan langsung
      muncul di kepala player secara presisi!
   -> Tampilan layar bebas dari hitam labu karena pumpkinblur sudah dibuat transparan!

--------------------------------------------------------------------------------
CARA 2 (MENGGUNAKAN HELM NETHERITE / DIAMOND / DLL DENGAN ETF + EMF):
--------------------------------------------------------------------------------
Mod yang dibutuhkan di folder .minecraft/mods:
1. Entity Texture Features (ETF)
2. Entity Model Features (EMF)
3. CIT Resewn Continuation (atau CIT Resewn)

Langkah di Game:
1. Ambil helm apa saja (Netherite, Diamond, Iron, Gold, Leather, Turtle Helmet).
2. Di Anvil (Paron), ganti namanya menjadi:
   "Sakura Hat" atau "Topi Sakura"
3. Pasang di kepala:
   -> CIT Resewn / ETF akan menyembunyikan tekstur helm netherite vanilla
      (menjadi transparan 100%).
   -> EMF me-render model 3D Sakura Hat di kepala player!
================================================================================
"""
    with open(readme_path, "w", encoding="utf-8") as f:
        f.write(guide_text)
    print("Updated README.txt")

    print("\n=== Step 6: Packaging sakura-resourcepack.zip ===")
    zip_dest = os.path.join(WORKSPACE, "sakura-resourcepack.zip")
    with zipfile.ZipFile(zip_dest, "w", zipfile.ZIP_DEFLATED) as zf:
        for root, dirs, files in os.walk(PACK_DIR):
            for file in files:
                full_path = os.path.join(root, file)
                rel_path = os.path.relpath(full_path, PACK_DIR)
                zf.write(full_path, rel_path)
    print(f"Packed updated zip: {zip_dest} ({os.path.getsize(zip_dest)} bytes)")
    print("\nALL TASKS COMPLETED SUCCESSFULLY!")

if __name__ == "__main__":
    main()
