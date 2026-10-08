import os
import json
import glob

MC_ITEMS = r"d:\Mod Minecraft\weapon set\sakura\sakura-weapons\src\main\resources\assets\minecraft\items"

def generate_when_list_val(item_en, item_id):
    names = []
    # English plain
    names.append(f"Valentine {item_en}")
    names.append(f"valentine {item_en.lower()}")
    names.append(f"VALENTINE {item_en.upper()}")
    names.append(f"Valentine {item_en.lower()}")

    # Indonesian plain
    if item_id:
        names.append(f"{item_id} Valentine")
        names.append(f"{item_id.lower()} valentine")
        names.append(f"{item_id.upper()} VALENTINE")
        names.append(f"{item_id} valentine")

    # English decorated
    names.append(f"§c§l❤ Valentine {item_en} ❤")
    names.append(f"§c§lValentine {item_en}")
    names.append(f"§cValentine {item_en}")
    names.append(f"&c&l❤ Valentine {item_en} ❤")
    names.append(f"&c&lValentine {item_en}")
    names.append(f"&cValentine {item_en}")

    names.append(f"§c§l❤ valentine {item_en.lower()} ❤")
    names.append(f"§c§lvalentine {item_en.lower()}")
    names.append(f"§cvalentine {item_en.lower()}")
    names.append(f"&c&l❤ valentine {item_en.lower()} ❤")
    names.append(f"&c&lvalentine {item_en.lower()}")
    names.append(f"&cvalentine {item_en.lower()}")

    # Indonesian decorated
    if item_id:
        names.append(f"§c§l❤ {item_id} Valentine ❤")
        names.append(f"§c§l{item_id} Valentine")
        names.append(f"§c{item_id} Valentine")
        names.append(f"&c&l❤ {item_id} Valentine ❤")
        names.append(f"&c&l{item_id} Valentine")
        names.append(f"&c{item_id} Valentine")

        names.append(f"§c§l❤ {item_id.lower()} valentine ❤")
        names.append(f"§c§l{item_id.lower()} valentine")
        names.append(f"§c{item_id.lower()} valentine")
        names.append(f"&c§l❤ {item_id.lower()} valentine ❤")
        names.append(f"&c§l{item_id.lower()} valentine")
        names.append(f"&c{item_id.lower()} valentine")

    return list(dict.fromkeys(names))

# Create complex models
VAL_BOW_MODEL = {
    "type": "minecraft:condition",
    "property": "minecraft:using_item",
    "on_false": {
        "type": "minecraft:model",
        "model": "valentine:item/bow"
    },
    "on_true": {
        "type": "minecraft:range_dispatch",
        "property": "minecraft:use_duration",
        "scale": 0.05,
        "entries": [
            {
                "threshold": 0.65,
                "model": {
                    "type": "minecraft:model",
                    "model": "valentine:item/bow_1"
                }
            },
            {
                "threshold": 0.9,
                "model": {
                    "type": "minecraft:model",
                    "model": "valentine:item/bow_2"
                }
            }
        ],
        "fallback": {
            "type": "minecraft:model",
            "model": "valentine:item/bow_0"
        }
    }
}

VAL_SHIELD_MODEL = {
    "type": "minecraft:condition",
    "property": "minecraft:using_item",
    "on_false": {
        "type": "minecraft:model",
        "model": "valentine:item/shield"
    },
    "on_true": {
        "type": "minecraft:model",
        "model": "valentine:item/shield_blocking"
    }
}

VAL_ROD_MODEL = {
    "type": "minecraft:condition",
    "property": "minecraft:fishing_rod/cast",
    "on_false": {
        "type": "minecraft:model",
        "model": "valentine:item/rod"
    },
    "on_true": {
        "type": "minecraft:model",
        "model": "valentine:item/rod_cast"
    }
}

def make_case(when_list, model_obj):
    return {
        "when": when_list,
        "model": model_obj
    }

def make_simple_case(item_en, item_id, model_name):
    return make_case(generate_when_list_val(item_en, item_id), {
        "type": "minecraft:model",
        "model": f"valentine:item/{model_name}"
    })

# Definitions of what cases each file type needs
CASE_CREATORS = {
    "sword": lambda: make_simple_case("Sword", "Pedang", "sword"),
    "axe": lambda: make_simple_case("Axe", "Kapak", "axe"),
    "hammer": lambda: make_simple_case("Hammer", "Palu", "hammer"),
    "spear": lambda: make_simple_case("Spear", "Tombak", "spear"),
    "staff": lambda: make_simple_case("Staff", "Tongkat", "staff"),
    "pickaxe": lambda: make_simple_case("Pickaxe", "Beliung", "pickaxe"),
    "shovel": lambda: make_simple_case("Shovel", "Sekop", "shovel"),
    "hoe": lambda: make_simple_case("Hoe", "Cangkul", "hoe"),
    "hat": lambda: make_simple_case("Hat", "Topi", "hat"),
    "wing": lambda: make_simple_case("Wings", "Sayap", "wing"),
    "key": lambda: make_simple_case("Key", "Kunci", "key"),
    "grenade": lambda: make_simple_case("Grenade", "Granat", "grenade"),
    "quiver": lambda: make_simple_case("Quiver", "Tempat Panah", "quiver"),
    "helmet": lambda: make_simple_case("Helmet", "Helm", "helmet"),
    "chestplate": lambda: make_simple_case("Chestplate", "Baju Zirah", "chestplate"),
    "leggings": lambda: make_simple_case("Leggings", "Celana", "leggings"),
    "boots": lambda: make_simple_case("Boots", "Sepatu", "boots"),
    "bow": lambda: make_case(generate_when_list_val("Bow", "Busur"), VAL_BOW_MODEL),
    "shield": lambda: make_case(generate_when_list_val("Shield", "Perisai"), VAL_SHIELD_MODEL),
    "rod": lambda: make_case(generate_when_list_val("Fishing Rod", "Pancingan"), VAL_ROD_MODEL)
}

# Mapping from file pattern to case keys
FILE_MAPPINGS = {}

# 7 Swords
for m in ["wooden", "stone", "iron", "golden", "diamond", "netherite", "copper"]:
    FILE_MAPPINGS[f"{m}_sword.json"] = ["sword", "hammer", "spear", "staff"]

# 7 Axes
for m in ["wooden", "stone", "iron", "golden", "diamond", "netherite", "copper"]:
    FILE_MAPPINGS[f"{m}_axe.json"] = ["axe", "hammer", "staff"]

# 7 Pickaxes
for m in ["wooden", "stone", "iron", "golden", "diamond", "netherite", "copper"]:
    FILE_MAPPINGS[f"{m}_pickaxe.json"] = ["pickaxe"]

# 7 Shovels
for m in ["wooden", "stone", "iron", "golden", "diamond", "netherite", "copper"]:
    FILE_MAPPINGS[f"{m}_shovel.json"] = ["shovel"]

# 7 Hoes
for m in ["wooden", "stone", "iron", "golden", "diamond", "netherite", "copper"]:
    FILE_MAPPINGS[f"{m}_hoe.json"] = ["hoe"]

# 7 Spears
for m in ["wooden", "stone", "iron", "golden", "diamond", "netherite", "copper"]:
    FILE_MAPPINGS[f"{m}_spear.json"] = ["spear", "staff"]

FILE_MAPPINGS["trident.json"] = ["spear", "staff"]
FILE_MAPPINGS["mace.json"] = ["hammer"]
FILE_MAPPINGS["bow.json"] = ["bow", "quiver"]
FILE_MAPPINGS["shield.json"] = ["shield"]
FILE_MAPPINGS["fishing_rod.json"] = ["rod"]

# Helmets
for m in ["leather", "chainmail", "iron", "golden", "diamond", "netherite", "copper", "turtle"]:
    FILE_MAPPINGS[f"{m}_helmet.json"] = ["hat", "helmet"]

FILE_MAPPINGS["carved_pumpkin.json"] = ["hat"]

# Chestplates
for m in ["leather", "chainmail", "iron", "golden", "diamond", "netherite", "copper"]:
    cases = ["chestplate", "wing"]
    if m == "leather":
        cases.append("quiver")
    FILE_MAPPINGS[f"{m}_chestplate.json"] = cases

# Leggings
for m in ["leather", "chainmail", "iron", "golden", "diamond", "netherite", "copper"]:
    FILE_MAPPINGS[f"{m}_leggings.json"] = ["leggings"]

# Boots
for m in ["leather", "chainmail", "iron", "golden", "diamond", "netherite", "copper"]:
    FILE_MAPPINGS[f"{m}_boots.json"] = ["boots"]

FILE_MAPPINGS["elytra.json"] = ["wing"]
FILE_MAPPINGS["tripwire_hook.json"] = ["key"]
FILE_MAPPINGS["stick.json"] = ["key", "grenade", "staff"]
FILE_MAPPINGS["paper.json"] = ["key", "grenade", "quiver", "staff"]

def enhance_pink_legacy_cases(cases):
    # Ensure Pink Legacy cases also include decorated sparkle versions §d§l✨ Pink Legacy <Item> ✨
    for c in cases:
        when = c.get("when", [])
        if isinstance(when, list):
            new_when = list(when)
            for name in when:
                if "Pink Legacy" in name and not "✨" in name and not "§" in name and not "&" in name:
                    # e.g. "Pink Legacy Sword"
                    sparkle_sec = f"§d§l✨ {name} ✨"
                    sparkle_amp = f"&d§l✨ {name} ✨"
                    if sparkle_sec not in new_when:
                        new_when.append(sparkle_sec)
                    if sparkle_amp not in new_when:
                        new_when.append(sparkle_amp)
                if "Pedang Pink Legacy" in name and not "✨" in name and not "§" in name and not "&" in name:
                    sparkle_sec = f"§d§l✨ {name} ✨"
                    sparkle_amp = f"&d§l✨ {name} ✨"
                    if sparkle_sec not in new_when:
                        new_when.append(sparkle_sec)
                    if sparkle_amp not in new_when:
                        new_when.append(sparkle_amp)
            c["when"] = list(dict.fromkeys(new_when))

def process_file(fname, case_keys):
    fpath = os.path.join(MC_ITEMS, fname)
    if not os.path.exists(fpath):
        print(f"Warning: {fpath} not found!")
        return

    with open(fpath, "r", encoding="utf-8") as f:
        data = json.load(f)

    if "model" not in data or data["model"].get("type") != "minecraft:select":
        print(f"Skipping {fname}, not select format.")
        return

    cases = data["model"].get("cases", [])
    enhance_pink_legacy_cases(cases)

    # Filter out any existing valentine cases to avoid duplicates
    new_cases = []
    for c in cases:
        model_str = json.dumps(c.get("model", {}))
        if "valentine:" not in model_str:
            new_cases.append(c)

    # Append new valentine cases
    for ck in case_keys:
        new_cases.append(CASE_CREATORS[ck]())

    data["model"]["cases"] = new_cases

    with open(fpath, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
    print(f"Updated {fname} with {len(case_keys)} Valentine cases.")

# Create Crossbow.json
def create_crossbow_json():
    val_cb_model = {
        "type": "minecraft:select",
        "property": "minecraft:charge_type",
        "cases": [
            {
                "when": "arrow",
                "model": {
                    "type": "minecraft:model",
                    "model": "valentine:item/crossbow_charged"
                }
            },
            {
                "when": "rocket",
                "model": {
                    "type": "minecraft:model",
                    "model": "valentine:item/crossbow_2_charged"
                }
            }
        ],
        "fallback": {
            "type": "minecraft:condition",
            "property": "minecraft:using_item",
            "on_true": {
                "type": "minecraft:range_dispatch",
                "property": "minecraft:crossbow/pull",
                "entries": [
                    {
                        "threshold": 0.58,
                        "model": {
                            "type": "minecraft:model",
                            "model": "valentine:item/crossbow_1"
                        }
                    },
                    {
                        "threshold": 1.0,
                        "model": {
                            "type": "minecraft:model",
                            "model": "valentine:item/crossbow_2"
                        }
                    }
                ],
                "fallback": {
                    "type": "minecraft:model",
                    "model": "valentine:item/crossbow_0"
                }
            },
            "on_false": {
                "type": "minecraft:model",
                "model": "valentine:item/crossbow"
            }
        }
    }

    vanilla_cb_model = {
        "type": "minecraft:select",
        "property": "minecraft:charge_type",
        "cases": [
            {
                "model": {
                    "type": "minecraft:model",
                    "model": "minecraft:item/crossbow_arrow"
                },
                "when": "arrow"
            },
            {
                "model": {
                    "type": "minecraft:model",
                    "model": "minecraft:item/crossbow_firework"
                },
                "when": "rocket"
            }
        ],
        "fallback": {
            "type": "minecraft:condition",
            "on_false": {
                "type": "minecraft:model",
                "model": "minecraft:item/crossbow"
            },
            "on_true": {
                "type": "minecraft:range_dispatch",
                "entries": [
                    {
                        "model": {
                            "type": "minecraft:model",
                            "model": "minecraft:item/crossbow_pulling_1"
                        },
                        "threshold": 0.58
                    },
                    {
                        "model": {
                            "type": "minecraft:model",
                            "model": "minecraft:item/crossbow_pulling_2"
                        },
                        "threshold": 1.0
                    }
                ],
                "fallback": {
                    "type": "minecraft:model",
                    "model": "minecraft:item/crossbow_pulling_0"
                },
                "property": "minecraft:crossbow/pull"
            },
            "property": "minecraft:using_item"
        }
    }

    cb_data = {
        "model": {
            "type": "minecraft:select",
            "property": "minecraft:component",
            "component": "minecraft:custom_name",
            "cases": [
                make_case(generate_when_list_val("Crossbow", "Busur Silang"), val_cb_model),
                make_simple_case("Quiver", "Tempat Panah", "quiver")
            ],
            "fallback": vanilla_cb_model
        }
    }
    with open(os.path.join(MC_ITEMS, "crossbow.json"), "w", encoding="utf-8") as f:
        json.dump(cb_data, f, indent=2)
    print("Created assets/minecraft/items/crossbow.json")

def main():
    create_crossbow_json()
    for fname, case_keys in FILE_MAPPINGS.items():
        process_file(fname, case_keys)
    print("All vanilla item definitions processed successfully!")

if __name__ == "__main__":
    main()
