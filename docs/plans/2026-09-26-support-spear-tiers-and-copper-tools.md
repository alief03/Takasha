# Support Spear Tiers & Copper Equipment Implementation Plan

> **Goal:** Extend Takasha Resource Pack to support all vanilla tiered Spears (`wooden_spear`, `stone_spear`, `copper_spear`, `golden_spear`, `iron_spear`, `diamond_spear`, `netherite_spear`, `spear`) and all newly introduced Copper tier equipment (`copper_sword`, `copper_axe`, `copper_pickaxe`, `copper_shovel`, `copper_hoe`, `copper_spear`, `copper_helmet`) across CIT Resewn/OptiFine properties and Minecraft 1.21.4+/26.2 Vanilla Item Definitions.

**Architecture:** Dual-layer integration: (1) Update CIT properties in both `assets/minecraft/optifine/cit/sakura/` and `assets/minecraft/citresewn/cit/sakura/` to include new item identifiers in the `items=` parameter; (2) Create and register vanilla `minecraft:select` item definitions in `assets/minecraft/items/` for all 7 spear tiers and 6 copper equipment items; (3) Build and release as a **MINOR** SemVer update (`Takasha-1.1.0.zip`).

**Tech Stack:** CIT Resewn / OptiFine CIT (`.properties`), Minecraft 26.2 / 1.21.4+ Item Definitions (`.json`), Python 3.14 (build & validation scripts).

---

### Task 1: Audit & Map All New Target Items to Sakura Weapons

**Files:**
- Reference: `PRD.md:113-130`
- Target: CIT properties and Vanilla Item Definitions

**Detail Pemetaan:**
1. **Tiered Spears (7 Tiers + generic fallback):**
   - `wooden_spear`, `stone_spear`, `copper_spear`, `golden_spear`, `iron_spear`, `diamond_spear`, `netherite_spear`, `spear`
   - Mapping: `sakura:item/spear`
2. **Copper Equipment:**
   - `copper_sword` -> `sakura:item/katana`, `sword`, `bigsword`, `dagger`, `spear`, `halberd`, `hammer`, `club`, `mace`, `gauntlet`
   - `copper_axe` -> `sakura:item/axe`, `halberd`, `hammer`
   - `copper_pickaxe` -> `sakura:item/pickaxe`
   - `copper_shovel` -> `sakura:item/shovel`
   - `copper_hoe` -> `sakura:item/hoe`
   - `copper_spear` -> `sakura:item/spear`
   - `copper_helmet` -> `sakura:item/hat`

---

### Task 2: Update CIT Properties (OptiFine & CIT Resewn Dual Paths)

**Files to Modify:**
- `assets/minecraft/optifine/cit/sakura/sakura_spear.properties`
- `assets/minecraft/citresewn/cit/sakura/sakura_spear.properties`
- `assets/minecraft/optifine/cit/sakura/sakura_sword.properties`
- `assets/minecraft/citresewn/cit/sakura/sakura_sword.properties`
- `assets/minecraft/optifine/cit/sakura/sakura_katana.properties`
- `assets/minecraft/citresewn/cit/sakura/sakura_katana.properties`
- `assets/minecraft/optifine/cit/sakura/sakura_bigsword.properties`
- `assets/minecraft/citresewn/cit/sakura/sakura_bigsword.properties`
- `assets/minecraft/optifine/cit/sakura/sakura_dagger.properties`
- `assets/minecraft/citresewn/cit/sakura/sakura_dagger.properties`
- `assets/minecraft/optifine/cit/sakura/sakura_gauntlet.properties`
- `assets/minecraft/citresewn/cit/sakura/sakura_gauntlet.properties`
- `assets/minecraft/optifine/cit/sakura/sakura_club.properties`
- `assets/minecraft/citresewn/cit/sakura/sakura_club.properties`
- `assets/minecraft/optifine/cit/sakura/sakura_mace.properties`
- `assets/minecraft/citresewn/cit/sakura/sakura_mace.properties`
- `assets/minecraft/optifine/cit/sakura/sakura_hammer.properties`
- `assets/minecraft/citresewn/cit/sakura/sakura_hammer.properties`
- `assets/minecraft/optifine/cit/sakura/sakura_halberd.properties`
- `assets/minecraft/citresewn/cit/sakura/sakura_halberd.properties`
- `assets/minecraft/optifine/cit/sakura/sakura_axe.properties`
- `assets/minecraft/citresewn/cit/sakura/sakura_axe.properties`
- `assets/minecraft/optifine/cit/sakura/sakura_pickaxe.properties`
- `assets/minecraft/citresewn/cit/sakura/sakura_pickaxe.properties`
- `assets/minecraft/optifine/cit/sakura/sakura_shovel.properties`
- `assets/minecraft/citresewn/cit/sakura/sakura_shovel.properties`
- `assets/minecraft/optifine/cit/sakura/sakura_hoe.properties`
- `assets/minecraft/citresewn/cit/sakura/sakura_hoe.properties`
- `assets/minecraft/optifine/cit/sakura/sakura_hat*.properties` & `topi_sakura*.properties`
- `assets/minecraft/citresewn/cit/sakura/sakura_hat*.properties` & `topi_sakura*.properties`

**Langkah Kerja:**
1. Tambahkan `copper_sword` ke seluruh senjata berdasar sword.
2. Tambahkan `copper_axe` ke `sakura_axe`, `sakura_halberd`, dan `sakura_hammer`.
3. Tambahkan `copper_pickaxe` ke `sakura_pickaxe`.
4. Tambahkan `copper_shovel` ke `sakura_shovel`.
5. Tambahkan `copper_hoe` ke `sakura_hoe`.
6. Tambahkan `wooden_spear stone_spear copper_spear golden_spear iron_spear diamond_spear netherite_spear spear` dan `copper_sword` ke `sakura_spear.properties`.
7. Tambahkan `copper_helmet` ke file properti `sakura_hat` dan `topi_sakura`.
8. Tambahkan `mace` ke `sakura_mace.properties`.

---

### Task 3: Create Vanilla 1.21.4+/26.2 Item Definitions (`assets/minecraft/items/*.json`)

**Files to Create:**
- `assets/minecraft/items/wooden_spear.json`
- `assets/minecraft/items/stone_spear.json`
- `assets/minecraft/items/copper_spear.json`
- `assets/minecraft/items/golden_spear.json`
- `assets/minecraft/items/iron_spear.json`
- `assets/minecraft/items/diamond_spear.json`
- `assets/minecraft/items/netherite_spear.json`
- `assets/minecraft/items/copper_sword.json`
- `assets/minecraft/items/copper_axe.json`
- `assets/minecraft/items/copper_pickaxe.json`
- `assets/minecraft/items/copper_shovel.json`
- `assets/minecraft/items/copper_hoe.json`
- `assets/minecraft/items/copper_helmet.json`

**Template Format (`minecraft:select` with `minecraft:custom_name`):**
Contoh `copper_sword.json` (mendukung variasi pedang & senjata tajam Sakura):
```json
{
  "model": {
    "type": "minecraft:select",
    "property": "minecraft:component",
    "component": "minecraft:custom_name",
    "cases": [
      {
        "when": ["Sakura Katana", "sakura katana", "Katana Sakura", "katana sakura"],
        "model": { "type": "minecraft:model", "model": "sakura:item/katana" }
      },
      {
        "when": ["Sakura Sword", "sakura sword", "Pedang Sakura", "pedang sakura"],
        "model": { "type": "minecraft:model", "model": "sakura:item/sword" }
      },
      {
        "when": ["Sakura Bigsword", "sakura bigsword", "Pedang Besar Sakura", "pedang besar sakura"],
        "model": { "type": "minecraft:model", "model": "sakura:item/bigsword" }
      },
      {
        "when": ["Sakura Dagger", "sakura dagger", "Belati Sakura", "belati sakura"],
        "model": { "type": "minecraft:model", "model": "sakura:item/dagger" }
      },
      {
        "when": ["Sakura Spear", "sakura spear", "Tombak Sakura", "tombak sakura"],
        "model": { "type": "minecraft:model", "model": "sakura:item/spear" }
      },
      {
        "when": ["Sakura Halberd", "sakura halberd"],
        "model": { "type": "minecraft:model", "model": "sakura:item/halberd" }
      },
      {
        "when": ["Sakura Hammer", "sakura hammer", "Palu Sakura", "palu sakura"],
        "model": { "type": "minecraft:model", "model": "sakura:item/hammer" }
      },
      {
        "when": ["Sakura Club", "sakura club", "Gada Sakura", "gada sakura"],
        "model": { "type": "minecraft:model", "model": "sakura:item/club" }
      },
      {
        "when": ["Sakura Mace", "sakura mace"],
        "model": { "type": "minecraft:model", "model": "sakura:item/mace" }
      },
      {
        "when": ["Sakura Gauntlet", "sakura gauntlet"],
        "model": { "type": "minecraft:model", "model": "sakura:item/gauntlet" }
      }
    ],
    "fallback": {
      "type": "minecraft:model",
      "model": "minecraft:item/copper_sword"
    }
  }
}
```

---

### Task 4: Automated Verification Script

**Test Script:** `validate_cit_and_items.py`
1. Validasi sintaks semua JSON di `assets/minecraft/items/*.json`.
2. Validasi bahwa semua nama model target (`sakura:item/*`) ada di filesystem `assets/sakura/models/item/*.json`.
3. Validasi sinkronisasi 1-ke-1 antara `optifine/cit/sakura/` dan `citresewn/cit/sakura/`.

---

### Task 5: PRD Documentation Update & Semantic Versioning Minor Release

**Files to Modify:**
- `PRD.md` (Update tabel kompatibilitas, item dasar, dan status fitur)
- `VERSION` (`1.0.1` -> `1.1.0` via `python build.py --minor`)
- Output: `Takasha-1.1.0.zip`

---
