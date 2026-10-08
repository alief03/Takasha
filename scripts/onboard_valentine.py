import os
import shutil
import json
import glob

RAW_BASE = r"d:\Mod Minecraft\weapon set\sakura\Bahan Set\elitecreatures-valentines_animated_weapon_and_tool_set_v1\ItemsAdder\data\resource_pack\assets\elitecreatures"
RAW_MODELS = os.path.join(RAW_BASE, "models", "valentines_animated_weapon_and_tool_set_v1")
RAW_TEX = os.path.join(RAW_BASE, "textures", "valentines_animated_weapon_and_tool_set_v1")

MOD_ASSETS = r"d:\Mod Minecraft\weapon set\sakura\sakura-weapons\src\main\resources\assets"
VALENTINE_MOD = os.path.join(MOD_ASSETS, "valentine")
MOD_MODELS = os.path.join(VALENTINE_MOD, "models", "item")
MOD_TEX_ITEM = os.path.join(VALENTINE_MOD, "textures", "item")
MOD_TEX_HUMANOID = os.path.join(VALENTINE_MOD, "textures", "entity", "equipment", "humanoid")
MOD_TEX_HUMANOID_BABY = os.path.join(VALENTINE_MOD, "textures", "entity", "equipment", "humanoid_baby")
MOD_TEX_LEGGINGS = os.path.join(VALENTINE_MOD, "textures", "entity", "equipment", "humanoid_leggings")
MOD_EQUIPMENT = os.path.join(VALENTINE_MOD, "equipment")

def setup_directories():
    for d in [MOD_MODELS, MOD_TEX_ITEM, MOD_TEX_HUMANOID, MOD_TEX_HUMANOID_BABY, MOD_TEX_LEGGINGS, MOD_EQUIPMENT]:
        os.makedirs(d, exist_ok=True)
    print("Directories created.")

def copy_textures():
    # Copy item textures and mcmeta
    files = os.listdir(RAW_TEX)
    copied = 0
    for f in files:
        src = os.path.join(RAW_TEX, f)
        if f.startswith("armor_layer") or f.startswith("valentines_armor_layer"):
            continue
        dst = os.path.join(MOD_TEX_ITEM, f)
        shutil.copy2(src, dst)
        copied += 1
    print(f"Copied {copied} item texture/mcmeta files to {MOD_TEX_ITEM}")

    # Copy worn armor layers
    l1 = os.path.join(RAW_TEX, "armor_layer_1.png")
    l2 = os.path.join(RAW_TEX, "armor_layer_2.png")
    shutil.copy2(l1, os.path.join(MOD_TEX_HUMANOID, "valentine.png"))
    shutil.copy2(l1, os.path.join(MOD_TEX_HUMANOID_BABY, "valentine.png"))
    shutil.copy2(l2, os.path.join(MOD_TEX_LEGGINGS, "valentine.png"))
    print("Copied worn entity armor layers to humanoid and humanoid_leggings.")

def create_equipment_json():
    eq = {
        "layers": {
            "humanoid": [
                {"texture": "valentine:valentine"}
            ],
            "humanoid_leggings": [
                {"texture": "valentine:valentine"}
            ],
            "humanoid_baby": [
                {"texture": "valentine:valentine"}
            ]
        }
    }
    with open(os.path.join(MOD_EQUIPMENT, "valentine.json"), "w", encoding="utf-8") as f:
        json.dump(eq, f, indent=2)
    print("Created equipment/valentine.json")

def process_models():
    # Process 28 3D Blockbench models (exclude chest.json)
    # helmet.json -> hat.json
    model_files = glob.glob(os.path.join(RAW_MODELS, "*.json"))
    processed = 0
    for p in model_files:
        fname = os.path.basename(p)
        if fname == "chest.json":
            print("Skipping chest.json as requested.")
            continue
        
        target_name = "hat.json" if fname == "helmet.json" else fname
        with open(p, "r", encoding="utf-8") as f:
            content = f.read()
        
        # Replace texture namespace
        content = content.replace("elitecreatures:valentines_animated_weapon_and_tool_set_v1/", "valentine:item/")
        
        target_path = os.path.join(MOD_MODELS, target_name)
        with open(target_path, "w", encoding="utf-8") as f:
            f.write(content)
        processed += 1
    print(f"Processed {processed} 3D Blockbench models.")

    # Create 4 2D armor item models
    armor_pieces = ["helmet", "chestplate", "leggings", "boots"]
    for piece in armor_pieces:
        model_data = {
            "parent": "minecraft:item/generated",
            "textures": {
                "layer0": f"valentine:item/{piece}_icon"
            }
        }
        with open(os.path.join(MOD_MODELS, f"{piece}.json"), "w", encoding="utf-8") as f:
            json.dump(model_data, f, indent=2)
    print("Generated 4 2D armor item models.")

if __name__ == "__main__":
    setup_directories()
    copy_textures()
    create_equipment_json()
    process_models()
    print("Asset preparation complete.")
