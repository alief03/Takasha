"""
build_sakura_wings_etf_emf.py
==============================
Generator untuk Custom Entity Model (CEM) Sakura Wings (Elytra)
dan sinkronisasi properti CIT untuk OptiFine & CIT Resewn.

Fungsi utama:
1. Generate empty_elytra.png (64x32 transparan 100%).
2. Ekstrak frame 0 (32x32) untuk tekstur dasar CEM dan copy tekstur statis.
3. Buat OptiFine Custom Animation properties (optifine/anim/) untuk ETF/OptiFine streaming.
4. Konversi wing.json menjadi elytra.jem (CEM) dengan single-layer counter-rotation Euler XYZ:
   rotate: [75.0, -75.0, 90.0] dan translate: [-3.2767, -5.9422, 5.2157].
5. Buat properti type=elytra dan type=item di optifine/cit/ dan citresewn/cit/
   dengan support carved_pumpkin dan helmets untuk mode pure vanilla tanpa mod.
"""

import os
import json
import shutil
import zlib
import struct
from PIL import Image

WORKSPACE = r"d:\Mod Minecraft\weapon set\sakura"
PACK_DIR = os.path.join(WORKSPACE, "sakura-resourcepack")
WING_JSON = os.path.join(PACK_DIR, "assets", "sakura", "models", "item", "wing.json")
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

def generate_elytra_jem():
    with open(WING_JSON, 'r', encoding='utf-8') as f:
        wing_data = json.load(f)

    submodels_static = []
    submodels_anim1 = []
    submodels_anim2 = []

    for i, el in enumerate(wing_data['elements']):
        f_pos = el['from']
        t_pos = el['to']
        w = round(t_pos[0] - f_pos[0], 4)
        h = round(t_pos[1] - f_pos[1], 4)
        d = round(t_pos[2] - f_pos[2], 4)

        rot = el.get('rotation')
        if rot:
            angle = rot['angle']
            axis = rot['axis']
            orig = rot['origin']
        else:
            angle = 0
            axis = 'y'
            orig = [f_pos[0], f_pos[1], f_pos[2]]

        # Detect texture used by faces
        faces = el.get('faces', {})
        first_face = list(faces.values())[0] if faces else {}
        tex_id = first_face.get('texture', '#0')

        if tex_id == '#1':
            uv_mult = 2.0
            anim_type = 1
        elif tex_id == '#2':
            uv_mult = 2.0
            anim_type = 2
        else:
            uv_mult = 16.0
            anim_type = 0

        # Submodel local coordinates relative to element origin
        bx = round(f_pos[0] - orig[0], 4)
        by = round(f_pos[1] - orig[1], 4)
        bz = round(f_pos[2] - orig[2], 4)

        rx = angle if axis == 'x' else 0
        ry = angle if axis == 'y' else 0
        rz = angle if axis == 'z' else 0

        box_def = {
            "coordinates": [bx, by, bz, w, h, d]
        }

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

        # In CEM single-layer hierarchy:
        # Subtract trunk center [10.0, 8.0, 8.0] directly from element translate
        tx = round(orig[0] - 10.0, 4)
        ty = round(orig[1] - 8.0, 4)
        tz = round(orig[2] - 8.0, 4)

        sub = {
            "id": f"wing_el_{i}",
            "invertAxis": "xy",
            "translate": [tx, ty, tz],
            "rotate": [rx, ry, rz],
            "boxes": [box_def]
        }

        if anim_type == 1:
            submodels_anim1.append(sub)
        elif anim_type == 2:
            submodels_anim2.append(sub)
        else:
            submodels_static.append(sub)

    # Single-Layer Counter-Rotation Euler XYZ:
    # R_parent = Rz(-15) @ Rx(15)
    # R_sub = Rx(-75.0) @ Ry(-75.0) @ Rz(90.0)
    # R_total aligns wingspan across shoulders and branches upright towards the sky (+Y)
    # Translation compensates left_wing shoulder pivot (X=5.0) to center on player spine:
    # T_comp = [-4.0532, -3.4020, 3.4998]
    def make_counter_rotated_wing(group_name, submodels, texture=None, texture_size=None):
        root_node = {
            "id": f"sakura_wings_{group_name}",
            "invertAxis": "xy",
            "translate": [-4.0532, -3.4020, 3.4998],
            "rotate": [-75.0, -75.0, 90.0],
            "boxes": [],
            "submodels": submodels
        }
        if texture and texture_size:
            root_node["texture"] = texture
            root_node["textureSize"] = texture_size
        return root_node

    left_wing_submodels = []
    if submodels_static:
        left_wing_submodels.append(make_counter_rotated_wing("static", submodels_static, "textures/sakura_texture.png", [256, 256]))
    if submodels_anim1:
        left_wing_submodels.append(make_counter_rotated_wing("anim1", submodels_anim1, "textures/sakura_animation_01.png", [32, 32]))
    if submodels_anim2:
        left_wing_submodels.append(make_counter_rotated_wing("anim2", submodels_anim2, "textures/sakura_animation_02-export.png", [32, 32]))

    # Right wing is kept empty so no duplicate wings render
    right_wing_submodels = []

    elytra_jem = {
        "credit": "Converted from sakura wing.json for ETF/EMF by Antigravity",
        "texture": "textures/sakura_texture.png",
        "textureSize": [256, 256],
        "models": [
            {
                "part": "left_wing",
                "id": "left_wing",
                "invertAxis": "xy",
                "translate": [0, 0, 0],
                "boxes": [],
                "submodels": left_wing_submodels
            },
            {
                "part": "right_wing",
                "id": "right_wing",
                "invertAxis": "xy",
                "translate": [0, 0, 0],
                "boxes": [],
                "submodels": right_wing_submodels
            }
        ]
    }
    return elytra_jem

def main():
    print("=== Step 1: Generating empty_elytra.png ===")
    empty_paths = [
        os.path.join(PACK_DIR, "assets", "minecraft", "textures", "entity", "empty_elytra.png"),
        os.path.join(PACK_DIR, "assets", "minecraft", "textures", "models", "armor", "empty_elytra.png"),
        os.path.join(PACK_DIR, "assets", "minecraft", "optifine", "cit", "sakura", "empty_elytra.png"),
        os.path.join(PACK_DIR, "assets", "minecraft", "citresewn", "cit", "sakura", "empty_elytra.png"),
        os.path.join(PACK_DIR, "assets", "minecraft", "textures", "empty_elytra.png"),
        os.path.join(PACK_DIR, "assets", "minecraft", "empty_elytra.png"),
    ]
    for p in empty_paths:
        create_transparent_png(p, width=64, height=32)
        dir_p = os.path.dirname(p)
        shutil.copy2(p, os.path.join(dir_p, "empty_elytra"))
        shutil.copy2(p, os.path.join(dir_p, "empty_elytra.png.png"))

    print("\n=== Step 2: Preparing CEM & ETF animation textures ===")
    cem_tex_dirs = [
        os.path.join(PACK_DIR, "assets", "minecraft", "optifine", "cem", "textures"),
        os.path.join(PACK_DIR, "assets", "minecraft", "optifine", "cem"),
        os.path.join(PACK_DIR, "assets", "minecraft", "emf", "cem", "textures"),
        os.path.join(PACK_DIR, "assets", "minecraft", "emf", "cem"),
        os.path.join(PACK_DIR, "assets", "minecraft", "textures"),
        os.path.join(PACK_DIR, "assets", "minecraft", "textures", "entity"),
    ]
    for ctex in cem_tex_dirs:
        os.makedirs(ctex, exist_ok=True)

    # 1. Copy full static texture
    static_src = os.path.join(TEXTURE_SRC, "sakura_texture.png")
    for ctex in cem_tex_dirs:
        shutil.copy2(static_src, os.path.join(ctex, "sakura_texture.png"))

    # 2. Extract 32x32 base frame (frame 0) for animated textures in CEM
    # Frame 0 is the top 32x32 square of the vertical animation strip
    anim01_full = Image.open(os.path.join(TEXTURE_SRC, "sakura_animation_01.png"))
    anim01_f0 = anim01_full.crop((0, 0, 32, 32))
    
    anim02_full = Image.open(os.path.join(TEXTURE_SRC, "sakura_animation_02-export.png"))
    anim02_f0 = anim02_full.crop((0, 0, 32, 32))

    for ctex in cem_tex_dirs:
        anim01_f0.save(os.path.join(ctex, "sakura_animation_01.png"))
        anim02_f0.save(os.path.join(ctex, "sakura_animation_02-export.png"))
        anim02_f0.save(os.path.join(ctex, "sakura_animation_02.png"))
        print(f"Saved base 32x32 frame textures to: {ctex}")

    # 3. Create OptiFine Custom Animation properties in assets/minecraft/optifine/anim/
    anim_dir = os.path.join(PACK_DIR, "assets", "minecraft", "optifine", "anim")
    emf_anim_dir = os.path.join(PACK_DIR, "assets", "minecraft", "emf", "anim")
    for adir in [anim_dir, emf_anim_dir]:
        os.makedirs(adir, exist_ok=True)

    anim01_prop = """from=/assets/sakura/textures/item/sakura_animation_01.png
to=/assets/minecraft/optifine/cem/textures/sakura_animation_01.png
x=0
y=0
w=32
h=32
duration=2
"""
    anim01_emf_prop = """from=/assets/sakura/textures/item/sakura_animation_01.png
to=/assets/minecraft/emf/cem/textures/sakura_animation_01.png
x=0
y=0
w=32
h=32
duration=2
"""
    anim02_prop = """from=/assets/sakura/textures/item/sakura_animation_02-export.png
to=/assets/minecraft/optifine/cem/textures/sakura_animation_02-export.png
x=0
y=0
w=32
h=32
duration=2
"""
    anim02_emf_prop = """from=/assets/sakura/textures/item/sakura_animation_02-export.png
to=/assets/minecraft/emf/cem/textures/sakura_animation_02-export.png
x=0
y=0
w=32
h=32
duration=2
"""
    for adir in [anim_dir, emf_anim_dir]:
        with open(os.path.join(adir, "sakura_wings_anim01.properties"), "w", encoding="utf-8") as f:
            f.write(anim01_prop)
        with open(os.path.join(adir, "sakura_wings_anim01_emf.properties"), "w", encoding="utf-8") as f:
            f.write(anim01_emf_prop)
        with open(os.path.join(adir, "sakura_wings_anim02.properties"), "w", encoding="utf-8") as f:
            f.write(anim02_prop)
        with open(os.path.join(adir, "sakura_wings_anim02_emf.properties"), "w", encoding="utf-8") as f:
            f.write(anim02_emf_prop)
        print("Generated OptiFine/ETF animation properties in:", adir)

    print("\n=== Step 3: Generating CEM JEM models for Elytra ===")
    elytra_jem = generate_elytra_jem()

    base_model_names = [
        "elytra",
        "player_elytra",
        "player_slim_elytra",
        "armor_stand_elytra",
    ]

    cem_root_dirs = [
        os.path.join(PACK_DIR, "assets", "minecraft", "optifine", "cem"),
        os.path.join(PACK_DIR, "assets", "minecraft", "emf", "cem"),
    ]

    for mdir in cem_root_dirs:
        os.makedirs(mdir, exist_ok=True)
        for base in base_model_names:
            for ext_name in [f"{base}.jem", f"{base}2.jem", f"{base}_sakura.jem"]:
                with open(os.path.join(mdir, ext_name), "w", encoding="utf-8") as f:
                    json.dump(elytra_jem, f, indent=2)

        # Also populate entity-specific subfolders: player, player_slim, armor_stand
        for entity_sub in ["player", "player_slim", "armor_stand"]:
            sdir = os.path.join(mdir, entity_sub)
            os.makedirs(sdir, exist_ok=True)
            for sub_base in ["elytra.jem", "elytra2.jem"]:
                with open(os.path.join(sdir, sub_base), "w", encoding="utf-8") as f:
                    json.dump(elytra_jem, f, indent=2)

        print("Written elytra JEM models in:", mdir)

    print("\n=== Step 4: Generating CIT Elytra & Item properties ===")
    cit_dirs = [
        os.path.join(PACK_DIR, "assets", "minecraft", "optifine", "cit", "sakura"),
        os.path.join(PACK_DIR, "assets", "minecraft", "citresewn", "cit", "sakura"),
    ]

    # Properties for Elytra entity texture suppression (standard without extension)
    sakura_wings_elytra_prop = """type=elytra
texture=empty_elytra
nbt.display.Name=ipattern:*Sakura Wings*
components.minecraft:custom_name=ipattern:*Sakura Wings*
components.custom_name=ipattern:*Sakura Wings*
"""

    sayap_sakura_elytra_prop = """type=elytra
texture=empty_elytra
nbt.display.Name=ipattern:*Sayap Sakura*
components.minecraft:custom_name=ipattern:*Sayap Sakura*
components.custom_name=ipattern:*Sayap Sakura*
"""

    # Properties with explicit .png extension for strict loaders
    sakura_wings_elytra_png = """type=elytra
texture=empty_elytra.png
nbt.display.Name=ipattern:*Sakura Wings*
components.minecraft:custom_name=ipattern:*Sakura Wings*
components.custom_name=ipattern:*Sakura Wings*
"""

    sayap_sakura_elytra_png = """type=elytra
texture=empty_elytra.png
nbt.display.Name=ipattern:*Sayap Sakura*
components.minecraft:custom_name=ipattern:*Sayap Sakura*
components.custom_name=ipattern:*Sayap Sakura*
"""

    # Item properties with expanded items:
    # Supports elytra, carved_pumpkin, paper, and helmets for dual-compatibility!
    items_list_expanded = "elytra carved_pumpkin netherite_helmet diamond_helmet iron_helmet golden_helmet chainmail_helmet leather_helmet turtle_helmet copper_helmet paper"

    sakura_wings_item_prop = f"""type=item
items={items_list_expanded}
model=sakura:item/wing
nbt.display.Name=ipattern:*Sakura Wings*
components.minecraft:custom_name=ipattern:*Sakura Wings*
components.custom_name=ipattern:*Sakura Wings*
"""

    sayap_sakura_item_prop = f"""type=item
items={items_list_expanded}
model=sakura:item/wing
nbt.display.Name=ipattern:*Sayap Sakura*
components.minecraft:custom_name=ipattern:*Sayap Sakura*
components.custom_name=ipattern:*Sayap Sakura*
"""

    for cdir in cit_dirs:
        os.makedirs(cdir, exist_ok=True)
        with open(os.path.join(cdir, "sakura_wings.properties"), "w", encoding="utf-8") as f:
            f.write(sakura_wings_item_prop)
        with open(os.path.join(cdir, "sayap_sakura.properties"), "w", encoding="utf-8") as f:
            f.write(sayap_sakura_item_prop)
        with open(os.path.join(cdir, "sakura_wings_elytra.properties"), "w", encoding="utf-8") as f:
            f.write(sakura_wings_elytra_prop)
        with open(os.path.join(cdir, "sayap_sakura_elytra.properties"), "w", encoding="utf-8") as f:
            f.write(sayap_sakura_elytra_prop)
        with open(os.path.join(cdir, "sakura_wings_elytra_png.properties"), "w", encoding="utf-8") as f:
            f.write(sakura_wings_elytra_png)
        with open(os.path.join(cdir, "sayap_sakura_elytra_png.properties"), "w", encoding="utf-8") as f:
            f.write(sayap_sakura_elytra_png)
        print("Written CIT Elytra & Item properties in:", cdir)

    print("\nALL SAKURA WINGS ETF/EMF ASSETS GENERATED SUCCESSFULLY!")

if __name__ == "__main__":
    main()
