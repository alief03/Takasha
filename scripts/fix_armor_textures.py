"""
fix_armor_textures.py  –  Armor Visual Fidelity & Source Parity Restoration (v1.6.5)

Strategy:
  - Start from CLEAN source files in 'Bahan Set/' (never from already-edited outputs)
  - Apply surgical UV masking per the v1.6.5 PRD Section 2.4 calibration matrix
  - Write outputs to all required destinations (mod + resourcepack mirrors)

Usage:
    python scripts/fix_armor_textures.py

Requirements:
    pip install Pillow
"""

from pathlib import Path
from PIL import Image
import shutil
import sys

# ---------------------------------------------------------------------------
# Root paths
# ---------------------------------------------------------------------------
REPO = Path(__file__).resolve().parent.parent

MOD_ASSETS   = REPO / "sakura-weapons/src/main/resources/assets"
RPACK_ASSETS = REPO / "sakura-resourcepack/assets"

# Source files (canonical – never edit these)
VAL_SRC_L1 = (
    REPO
    / "Bahan Set/elitecreatures-valentines_animated_weapon_and_tool_set_v1"
    / "ItemsAdder/data/resource_pack/assets/elitecreatures"
    / "textures/valentines_animated_weapon_and_tool_set_v1/armor_layer_1.png"
)
VAL_SRC_L2 = VAL_SRC_L1.with_name("armor_layer_2.png")

PINK_SRC_L1 = (
    REPO
    / "Bahan Set/elitecreatures-pink_legacy_animated_weapon_set"
    / "ItemsAdder/data/resourcepack/assets/elitecreatures"
    / "textures/pink_legacy_animated_weapon_set/pink_legacy_armor_layer_1.png"
)
PINK_SRC_L2 = PINK_SRC_L1.with_name("pink_legacy_armor_layer_2.png")


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def load_source(path: Path) -> Image.Image:
    if not path.exists():
        sys.exit(f"[ERROR] Source not found: {path}")
    img = Image.open(path).convert("RGBA")
    print(f"  [SRC] {path.name}  {img.size}")
    return img


def erase(img: Image.Image, x0: int, y0: int, x1: int, y1: int) -> None:
    """Set a rectangular region to fully transparent (RGBA 0,0,0,0)."""
    if x0 >= x1 or y0 >= y1:
        return
    w, h = img.size
    x0, y0, x1, y1 = (
        max(0, x0), max(0, y0),
        min(w, x1), min(h, y1),
    )
    region_w, region_h = x1 - x0, y1 - y0
    blank = Image.new("RGBA", (region_w, region_h), (0, 0, 0, 0))
    img.paste(blank, (x0, y0))


def save_to(img: Image.Image, *dests: Path) -> None:
    for dest in dests:
        dest.parent.mkdir(parents=True, exist_ok=True)
        img.save(str(dest), "PNG")
        print(f"  [OUT] {dest.relative_to(REPO)}")


# ---------------------------------------------------------------------------
# Valentine – Layer 1  (HUMANOID / humanoid_baby)
# ---------------------------------------------------------------------------
def fix_valentine_l1() -> Image.Image:
    """
    PRD §3.1 – Layer 1 calibration for Valentine armor.

    KEEP  (untouched from source):
      - Head base    [0,   0,  64, 32]  – full face, back neck
      - Hat overlay  [64,  0, 128, 32]  – tiara + red heart emblem
      - Torso plate  [32, 32,  80, 62]  – full chestplate incl. silver abs Y=54..61
      - Arms         [80, 32, 112, 64]  – shoulder + wrist guards
      - Boots        [0,  54,  32, 64]  – pink boots + gold heel accent

    ERASE (zero pixels):
      - Torso crotch gap  [32, 62,  80, 64]  – let L2 belt show through
      - Legs thigh+knee   [0,  32,  32, 54]  – remove boot-puff on thigh
    """
    print("\n[Valentine L1] Processing...")
    img = load_source(VAL_SRC_L1)
    w, h = img.size  # expected 128x64

    # Erase torso crotch gap (Y=62..64 in 1-indexed = rows 62..63 in 0-indexed)
    erase(img, 32, 62, 80, 64)   # Torso crotch

    # Erase legs thigh + knee (keep only boots at Y>=54)
    erase(img, 0, 32, 32, 54)    # Leg upper (thigh + knee)

    return img


# ---------------------------------------------------------------------------
# Valentine – Layer 2  (HUMANOID_LEGGINGS)
# ---------------------------------------------------------------------------
def fix_valentine_l2() -> Image.Image:
    """
    PRD §3.2 – Layer 2 calibration for Valentine armor.

    KEEP  (untouched from source):
      - Torso belt+pelvis  [32, 54,  80, 64]  – gold belt + yellow peplum
      - Legs full          [0,  32,  32, 64]  – thigh, knee pad, dark calf tights

    ERASE (zero pixels):
      - Head + hat     [0,   0, 128, 32]  – leggings have no head element
      - Arms           [80, 32, 112, 64]  – leggings have no arm element
      - Torso upper    [32, 32,  80, 54]  – prevent inner shirt clipping above L1 plate
    """
    print("\n[Valentine L2] Processing...")
    img = load_source(VAL_SRC_L2)

    # Erase head section (full top half of texture)
    erase(img, 0, 0, 128, 32)

    # Erase arms
    erase(img, 80, 32, 112, 64)

    # Erase torso upper (inner shirt, above belt line)
    erase(img, 32, 32, 80, 54)

    return img


# ---------------------------------------------------------------------------
# Pink Legacy – Layer 1  (HUMANOID / humanoid_baby)
# ---------------------------------------------------------------------------
def fix_pink_l1() -> Image.Image:
    """
    PRD §3.1 (Pink Legacy variant).

    KEEP:
      - Head base    [0,   0,  64, 32]
      - Hat overlay  [64,  0, 128, 32]  – visor + antennae (232 px) – DO NOT ERASE
      - Torso plate  [32, 32,  80, 64]  – full chestplate incl. lower plate Y=54..63
      - Arms         [80, 32, 112, 64]

    ERASE:
      - Legs thigh+knee  [0, 32, 32, 54]  – avoid boot-puff on thigh
    """
    print("\n[Pink Legacy L1] Processing...")
    img = load_source(PINK_SRC_L1)

    # Erase leg thigh + knee area (boots only from Y>=54)
    erase(img, 0, 32, 32, 54)

    return img


# ---------------------------------------------------------------------------
# Pink Legacy – Layer 2  (HUMANOID_LEGGINGS)
# ---------------------------------------------------------------------------
def fix_pink_l2() -> Image.Image:
    """
    PRD §3.2 (Pink Legacy variant).

    KEEP:
      - Torso belt  [32, 49, 80, 64]  – belt + waist
      - Legs full   [0,  32, 32, 64]  – full leg coverage

    ERASE:
      - Head + hat   [0,   0, 128, 32]
      - Arms         [80, 32, 112, 64]
      - Torso upper  [32, 32,  80, 49]  – clears collar above chestplate
    """
    print("\n[Pink Legacy L2] Processing...")
    img = load_source(PINK_SRC_L2)

    erase(img, 0, 0, 128, 32)        # Head
    erase(img, 80, 32, 112, 64)      # Arms
    erase(img, 32, 32, 80, 49)       # Torso upper

    return img


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
def main() -> None:
    print("=" * 60)
    print("  Sakura Weapons – Armor Texture Fix  (v1.6.5)")
    print("=" * 60)

    # ── Valentine ──────────────────────────────────────────────
    val_l1 = fix_valentine_l1()
    val_l2 = fix_valentine_l2()

    VAL_HUMANOID_DIR     = MOD_ASSETS / "valentine/textures/entity/equipment/humanoid"
    VAL_BABY_DIR         = MOD_ASSETS / "valentine/textures/entity/equipment/humanoid_baby"
    VAL_LEGGINGS_DIR     = MOD_ASSETS / "valentine/textures/entity/equipment/humanoid_leggings"

    print("\n[Valentine] Saving...")
    save_to(val_l1,
        VAL_HUMANOID_DIR  / "valentine.png",
        VAL_BABY_DIR      / "valentine.png",
    )
    save_to(val_l2,
        VAL_LEGGINGS_DIR  / "valentine.png",
    )

    # resourcepack mirror (valentine doesn't exist there yet – create)
    RPACK_VAL_H  = RPACK_ASSETS / "valentine/textures/entity/equipment/humanoid"
    RPACK_VAL_HB = RPACK_ASSETS / "valentine/textures/entity/equipment/humanoid_baby"
    RPACK_VAL_L  = RPACK_ASSETS / "valentine/textures/entity/equipment/humanoid_leggings"
    save_to(val_l1,
        RPACK_VAL_H  / "valentine.png",
        RPACK_VAL_HB / "valentine.png",
    )
    save_to(val_l2,
        RPACK_VAL_L / "valentine.png",
    )

    # ── Pink Legacy ────────────────────────────────────────────
    pink_l1 = fix_pink_l1()
    pink_l2 = fix_pink_l2()

    PINK_HUMANOID_DIR  = MOD_ASSETS / "pink_legacy/textures/entity/equipment/humanoid"
    PINK_BABY_DIR      = MOD_ASSETS / "pink_legacy/textures/entity/equipment/humanoid_baby"
    PINK_LEGGINGS_DIR  = MOD_ASSETS / "pink_legacy/textures/entity/equipment/humanoid_leggings"

    print("\n[Pink Legacy] Saving...")
    save_to(pink_l1,
        PINK_HUMANOID_DIR / "pink_legacy.png",
        PINK_BABY_DIR     / "pink_legacy.png",
    )
    save_to(pink_l2,
        PINK_LEGGINGS_DIR / "pink_legacy.png",
    )

    # resourcepack mirror
    RPACK_PINK_H  = RPACK_ASSETS / "pink_legacy/textures/entity/equipment/humanoid"
    RPACK_PINK_HB = RPACK_ASSETS / "pink_legacy/textures/entity/equipment/humanoid_baby"
    RPACK_PINK_L  = RPACK_ASSETS / "pink_legacy/textures/entity/equipment/humanoid_leggings"
    save_to(pink_l1,
        RPACK_PINK_H  / "pink_legacy.png",
        RPACK_PINK_HB / "pink_legacy.png",
    )
    save_to(pink_l2,
        RPACK_PINK_L / "pink_legacy.png",
    )

    print("\n[DONE] All armor textures regenerated from clean sources.")
    print("       Run: python scripts/validate_armor_layers.py")


if __name__ == "__main__":
    main()
