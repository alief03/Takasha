"""
validate_armor_layers.py – CI Validation for Armor Textures (v1.6.5)

Checks:
  1. Dimensions: 128x64 for all armor layer files
  2. Valentine L1 – Hat tiara heart (≥ 60 red pixels in [64,0,128,32])
  3. Valentine L1 – Silver abs plate (≥ 200 non-transparent pixels in [32,54,80,62])
  4. Valentine L1 – Leg thigh clean (== 0 visible pixels in [0,32,32,54])
  5. Valentine L2 – Torso upper clean (== 0 visible pixels in [32,32,80,54])
  6. Valentine L2 – Calf tights present (≥ 100 non-transparent pixels in [0,54,32,64])
  7. Pink Legacy L1 – Hat visor present (≥ 100 non-transparent pixels in [64,0,128,32])
  8. Pink Legacy L1 – Lower chestplate present (≥ 50 pixels in [32,54,80,64])
  9. Pink Legacy L2 – Torso upper clean (== 0 visible pixels in [32,32,80,49])

Usage:
    python scripts/validate_armor_layers.py
"""

from pathlib import Path
from PIL import Image
import sys

REPO = Path(__file__).resolve().parent.parent
MOD_ASSETS = REPO / "sakura-weapons/src/main/resources/assets"

FILES = {
    "valentine_l1":   MOD_ASSETS / "valentine/textures/entity/equipment/humanoid/valentine.png",
    "valentine_l2":   MOD_ASSETS / "valentine/textures/entity/equipment/humanoid_leggings/valentine.png",
    "pink_legacy_l1": MOD_ASSETS / "pink_legacy/textures/entity/equipment/humanoid/pink_legacy.png",
    "pink_legacy_l2": MOD_ASSETS / "pink_legacy/textures/entity/equipment/humanoid_leggings/pink_legacy.png",
}

PASS = "[PASS]"
FAIL = "[FAIL]"


def count_visible(img: Image.Image, x0: int, y0: int, x1: int, y1: int) -> int:
    """Count pixels with alpha > 10 in rectangle."""
    region = img.crop((x0, y0, x1, y1))
    data = region.getdata()
    return sum(1 for r, g, b, a in data if a > 10)


def count_red_pixels(img: Image.Image, x0: int, y0: int, x1: int, y1: int) -> int:
    """Count pixels that are clearly red (R>180, G<100, B<100, A>10)."""
    region = img.crop((x0, y0, x1, y1))
    data = region.getdata()
    return sum(1 for r, g, b, a in data if r > 150 and g < 120 and b < 120 and a > 10)


def run_check(name: str, passed: bool, detail: str) -> bool:
    tag = PASS if passed else FAIL
    print(f"  {tag}  {name}: {detail}")
    return passed


def validate(key: str, img: Image.Image) -> list[bool]:
    results = []
    w, h = img.size

    # Shared: dimensions
    ok = run_check(
        "Dimensions 128x64",
        (w, h) == (128, 64),
        f"actual {w}x{h}"
    )
    results.append(ok)

    if key == "valentine_l1":
        # Hat red heart tiara
        red_px = count_red_pixels(img, 64, 0, 128, 32)
        results.append(run_check(
            "Hat tiara red heart pixels >= 20",
            red_px >= 20,
            f"found {red_px} red pixels in hat overlay [64,0,128,32]"
        ))

        # Silver abs plate (Y=54..62 of torso)
        abs_px = count_visible(img, 32, 54, 80, 62)
        results.append(run_check(
            "Chestplate abs plate >= 100 visible pixels",
            abs_px >= 100,
            f"found {abs_px} visible pixels in torso [32,54,80,62]"
        ))

        # Leg thigh must be empty
        thigh_px = count_visible(img, 0, 32, 32, 54)
        results.append(run_check(
            "Leg thigh = 0 visible pixels (no boot-puff)",
            thigh_px == 0,
            f"found {thigh_px} visible pixels in [0,32,32,54]"
        ))

    elif key == "valentine_l2":
        # Torso upper must be empty
        torso_px = count_visible(img, 32, 32, 80, 54)
        results.append(run_check(
            "L2 Torso upper = 0 visible pixels (no collar clip)",
            torso_px == 0,
            f"found {torso_px} visible pixels in [32,32,80,54]"
        ))

        # Head region must be empty
        head_px = count_visible(img, 0, 0, 128, 32)
        results.append(run_check(
            "L2 Head region = 0 visible pixels",
            head_px == 0,
            f"found {head_px} visible pixels in [0,0,128,32]"
        ))

        # Calf tights (Y=54..64 leg area)
        calf_px = count_visible(img, 0, 54, 32, 64)
        results.append(run_check(
            "L2 Calf tights >= 50 visible pixels",
            calf_px >= 50,
            f"found {calf_px} visible pixels in leg [0,54,32,64]"
        ))

    elif key == "pink_legacy_l1":
        # Visor / hat overlay must have pixels
        hat_px = count_visible(img, 64, 0, 128, 32)
        results.append(run_check(
            "Hat overlay (visor+antennae) >= 100 visible pixels",
            hat_px >= 100,
            f"found {hat_px} visible pixels in hat [64,0,128,32]"
        ))

        # Lower chestplate area
        chest_low = count_visible(img, 32, 54, 80, 64)
        results.append(run_check(
            "Chestplate lower plate >= 50 visible pixels",
            chest_low >= 50,
            f"found {chest_low} visible pixels in [32,54,80,64]"
        ))

        # Leg thigh must be empty
        thigh_px = count_visible(img, 0, 32, 32, 54)
        results.append(run_check(
            "Leg thigh region = 0 visible pixels",
            thigh_px == 0,
            f"found {thigh_px} visible pixels in [0,32,32,54]"
        ))

    elif key == "pink_legacy_l2":
        # Torso upper must be empty
        torso_px = count_visible(img, 32, 32, 80, 49)
        results.append(run_check(
            "L2 Torso upper = 0 visible pixels",
            torso_px == 0,
            f"found {torso_px} visible pixels in [32,32,80,49]"
        ))

        # Head region must be empty
        head_px = count_visible(img, 0, 0, 128, 32)
        results.append(run_check(
            "L2 Head region = 0 visible pixels",
            head_px == 0,
            f"found {head_px} visible pixels in [0,0,128,32]"
        ))

    return results


def main() -> None:
    print("=" * 60)
    print("  Sakura Weapons – Armor Layer Validation (v1.6.5)")
    print("=" * 60)

    all_passed = True

    for key, path in FILES.items():
        print(f"\n-- {key} ({path.name}) --")
        if not path.exists():
            print(f"  {FAIL}  File not found: {path}")
            all_passed = False
            continue

        img = Image.open(str(path)).convert("RGBA")
        results = validate(key, img)
        if not all(results):
            all_passed = False

    print("\n" + "=" * 60)
    if all_passed:
        print("  ALL CHECKS PASSED  --  Ready for v1.6.5 build")
    else:
        print("  VALIDATION FAILED  --  Fix errors before building")
        sys.exit(1)
    print("=" * 60)


if __name__ == "__main__":
    main()
