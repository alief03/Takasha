import os
import json

BASE = r"d:\Mod Minecraft\weapon set\sakura\sakura-weapons\src\main\resources\assets"
SW_ITEMS = os.path.join(BASE, "sakura_weapons", "items")
os.makedirs(SW_ITEMS, exist_ok=True)

# 1. Generate 21 native items for sakura_weapons:valentine_*
simple_models = {
    "valentine_sword": "valentine:item/sword",
    "valentine_axe": "valentine:item/axe",
    "valentine_hammer": "valentine:item/hammer",
    "valentine_spear": "valentine:item/spear",
    "valentine_staff": "valentine:item/staff",
    "valentine_pickaxe": "valentine:item/pickaxe",
    "valentine_shovel": "valentine:item/shovel",
    "valentine_hoe": "valentine:item/hoe",
    "valentine_hat": "valentine:item/hat",
    "valentine_wing": "valentine:item/wing",
    "valentine_key": "valentine:item/key",
    "valentine_grenade": "valentine:item/grenade",
    "valentine_quiver": "valentine:item/quiver",
    "valentine_helmet": "valentine:item/helmet",
    "valentine_chestplate": "valentine:item/chestplate",
    "valentine_leggings": "valentine:item/leggings",
    "valentine_boots": "valentine:item/boots",
}

for item_name, model_path in simple_models.items():
    data = {
        "model": {
            "type": "minecraft:model",
            "model": model_path
        }
    }
    with open(os.path.join(SW_ITEMS, f"{item_name}.json"), "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)

# Bow
bow_data = {
    "model": {
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
}
with open(os.path.join(SW_ITEMS, "valentine_bow.json"), "w", encoding="utf-8") as f:
    json.dump(bow_data, f, indent=2)

# Crossbow
crossbow_data = {
    "model": {
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
}
with open(os.path.join(SW_ITEMS, "valentine_crossbow.json"), "w", encoding="utf-8") as f:
    json.dump(crossbow_data, f, indent=2)

# Shield
shield_data = {
    "model": {
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
}
with open(os.path.join(SW_ITEMS, "valentine_shield.json"), "w", encoding="utf-8") as f:
    json.dump(shield_data, f, indent=2)

# Fishing Rod
rod_data = {
    "model": {
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
}
with open(os.path.join(SW_ITEMS, "valentine_fishing_rod.json"), "w", encoding="utf-8") as f:
    json.dump(rod_data, f, indent=2)

print("Created all 21 native item models in assets/sakura_weapons/items/")
