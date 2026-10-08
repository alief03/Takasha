import json
import os
from pathlib import Path

tiers = ['wooden', 'stone', 'copper', 'iron', 'golden', 'diamond', 'netherite']

sakura_spear_cases = [
    "Sakura Spear",
    "sakura spear",
    "SAKURA SPEAR",
    "Sakura spear",
    "Tombak Sakura",
    "tombak sakura",
    "TOMBAK SAKURA",
    "Tombak sakura",
    "§d§l🌸 Sakura Spear 🌸",
    "§d§lSakura Spear",
    "§dSakura Spear",
    "&d&l🌸 Sakura Spear 🌸",
    "&d&lSakura Spear",
    "&dSakura Spear",
    "§d§l🌸 Sakura spear 🌸",
    "§d§lSakura spear",
    "§dSakura spear",
    "&d&l🌸 Sakura spear 🌸",
    "&d&lSakura spear",
    "&dSakura spear",
    "§d§l🌸 Tombak Sakura 🌸",
    "§d§lTombak Sakura",
    "§dTombak Sakura",
    "&d&l🌸 Tombak Sakura 🌸",
    "&d&lTombak Sakura",
    "&dTombak Sakura"
]

pink_legacy_spear_cases = [
    "Pink Legacy Spear",
    "pink legacy spear",
    "PINK LEGACY SPEAR",
    "Pink legacy spear",
    "Tombak Pink Legacy",
    "tombak pink legacy",
    "TOMBAK PINK LEGACY",
    "Tombak pink legacy",
    "§d§lPink Legacy Spear",
    "§dPink Legacy Spear",
    "&d&lPink Legacy Spear",
    "&dPink Legacy Spear",
    "§d§lPink legacy spear",
    "§dPink legacy spear",
    "&d&lPink legacy spear",
    "&dPink legacy spear",
    "§d§lTombak Pink Legacy",
    "§dTombak Pink Legacy",
    "&d&lTombak Pink Legacy",
    "&dTombak Pink Legacy"
]

# Verify uniqueness of case conditions
assert len(sakura_spear_cases) == len(set(sakura_spear_cases)), "Duplicate found in sakura_spear_cases!"
assert len(pink_legacy_spear_cases) == len(set(pink_legacy_spear_cases)), "Duplicate found in pink_legacy_spear_cases!"

dest_dirs = [
    Path(r"d:\Mod Minecraft\weapon set\sakura\sakura-weapons\src\main\resources\assets\minecraft\items"),
    Path(r"d:\Mod Minecraft\weapon set\sakura\sakura-resourcepack\assets\minecraft\items")
]

for tier in tiers:
    data = {
        "model": {
            "type": "minecraft:select",
            "property": "minecraft:component",
            "component": "minecraft:custom_name",
            "cases": [
                {
                    "when": sakura_spear_cases,
                    "model": {
                        "type": "minecraft:model",
                        "model": "sakura:item/spear"
                    }
                },
                {
                    "when": pink_legacy_spear_cases,
                    "model": {
                        "type": "minecraft:model",
                        "model": "pink_legacy:item/spear"
                    }
                }
            ],
            "fallback": {
                "type": "minecraft:select",
                "property": "minecraft:display_context",
                "cases": [
                    {
                        "when": [
                            "gui",
                            "ground",
                            "fixed",
                            "on_shelf"
                        ],
                        "model": {
                            "type": "minecraft:model",
                            "model": f"minecraft:item/{tier}_spear"
                        }
                    }
                ],
                "fallback": {
                    "type": "minecraft:model",
                    "model": f"minecraft:item/{tier}_spear_in_hand"
                }
            }
        },
        "swap_animation_scale": 1.95
    }
    
    for d in dest_dirs:
        out_file = d / f"{tier}_spear.json"
        with open(out_file, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
        print(f"Generated: {out_file}")

# Remove obsolete/invalid spear.json files
remove_paths = [
    Path(r"d:\Mod Minecraft\weapon set\sakura\sakura-weapons\src\main\resources\assets\minecraft\items\spear.json"),
    Path(r"d:\Mod Minecraft\weapon set\sakura\sakura-weapons\src\main\resources\assets\minecraft\models\item\spear.json"),
    Path(r"d:\Mod Minecraft\weapon set\sakura\sakura-resourcepack\assets\minecraft\items\spear.json"),
    Path(r"d:\Mod Minecraft\weapon set\sakura\sakura-resourcepack\assets\minecraft\models\item\spear.json"),
]

for p in remove_paths:
    if p.exists():
        p.unlink()
        print(f"Removed ghost file: {p}")

print("All spear files successfully processed!")
