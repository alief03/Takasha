# Implementation Plan: Standarisasi Arsitektur Dual Source Code Multi-Version (Minecraft 1.21.11 & 26.2)

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Mengimplementasikan standarisasi arsitektur dual-source multi-project Gradle pada modul `sakura-weapons` (Takasha Mod) agar cabang **Minecraft 1.21.11 (LTS)** dan **Minecraft 26.2 (Modern/Production)** memiliki isolasi source code, compiler JVM, dan dependensi Fabric yang independen, dengan single source of truth untuk aset bersama (`shared-resources/`) serta pipeline build lokal paralel ke `Hasil mod/`.

**Architecture:** Membagi modul `sakura-weapons` menjadi Gradle Multi-Project Submodules: subproject `versions/1.21.11` (target Java 21 LTS, Fabric API 0.141.6+1.21.11, Loom Remap Intermediary, bytecode 65.0) dan subproject `versions/26.2` (target Java 25 LTS, Fabric API 0.161.0+26.2, Loom Unobfuscated, bytecode 69.0). Aset Blockbench 3D model JSON, tekstur, animasi `.mcmeta`, dan sound definition disatukan dalam `shared-resources/` dan didistribusikan ke kedua subproyek via task Gradle `processResources`. Root `build.gradle` mengorkestrasi kompilasi paralel via task `buildAll` yang menyimpan output ke folder lokal `Hasil mod/Takasha-{VERSION}+{MC_VERSION}.jar` tanpa deployment eksternal.

**Tech Stack:** 
- Minecraft Java Edition 1.21.11 & 26.2
- Fabric Loader >=0.19.3
- Fabric API 0.141.6+1.21.11 & 0.161.0+26.2
- Fabric Loom 1.17.21 (Remap Mode untuk 1.21.11, Unobfuscated Mode untuk 26.2)
- OpenJDK 21 LTS (`java-runtime-delta`, bytecode 65.0) & OpenJDK 25 LTS (`java-runtime-epsilon`, bytecode 69.0)
- Gradle 8.13+ Multi-Project Submodules

---

## 1. Analisis Status & Kesenjangan PRD (Gap Analysis)

### 1.1 Status Saat Ini (Current State)
1. **Mono-Project untuk 26.2:** Direktori `sakura-weapons/` saat ini dikonfigurasi sebagai single project tunggal yang hanya menargetkan Minecraft 26.2 dengan Java 25 (`java-runtime-epsilon`, bytecode 69.0).
2. **Ketiadaan Cabang Aktif 1.21.11:** Kode untuk 1.21.11 sebelumnya tertimpa saat migrasi 26.2 atau hanya ada dalam riwayat JAR rilis `Takasha-1.6.5.jar`. Direktori `src_26.2_experimental` merupakan sisa eksperimen masa transisi dan belum terstandarisasi.
3. **Duplikasi / Resiko Regresi:** Mempertahankan satu pohon source tunggal menyebabkan crash instan:
   - Di Java 21 (1.21.11): Crash `UnsupportedClassVersionError: class file version 69.0 (Java 25)`.
   - Di Fabric Loom 26.2: Crash `Cannot use Mojang mappings in a non-obfuscated environment` jika dipaksa mapping remapping lama.
   - Di level API: Crash compile karena perbedaan nama kelas `Identifier` vs `ResourceLocation`, API GUI `extractWidgetRenderState` vs `renderWidget`, dan callback render layer.

### 1.2 Target Sesuai PRD v2.2.0 (Target State)
1. **Pemisahan Source Code Formal:** Subdirektori `sakura-weapons/versions/1.21.11` dan `sakura-weapons/versions/26.2` berdiri sebagai subproyek independen.
2. **Shared Assets Terpusat:** Direktori `sakura-weapons/shared-resources/` menampung seluruh model 3D Blockbench, tekstur PNG, animasi `.mcmeta`, dan file bahasa `lang/en_us.json` agar 0 duplikasi biner terjadi.
3. **Target Platform Eksplisit:** **`Dual-Platform (Minecraft 1.21.11 & Minecraft 26.2)`**.
4. **Output Rilis Lokal Berversi Ganda (PRD Bagian 2.5, 8 & 10):**
   - `Hasil mod/Takasha-2.2.0+1.21.11.jar`
   - `Hasil mod/Takasha-2.2.0+26.2.jar`
   - Nol deployment eksternal ke profil launcher Modrinth.

---

## 2. Inventaris Lengkap Item Takasha Multi-Set (84 Item Native)

Katalog mencakup seluruh 84 item native yang wajib didukung secara setara di kedua versi:

| Set ID | Nama Set | Total Item | Rincian Item Native & Archetype | Format Anvil (Shift+Klik) |
|:---|:---|:---:|:---|:---|
| **`sakura`** | Sakura Set | **20 Item** | • **Melee (6):** `katana`, `sword`, `bigsword`, `dagger`, `gauntlet`, `club`<br>• **Long (1):** `spear`<br>• **Blunt (2):** `hammer`, `mace`<br>• **Tools (4):** `pickaxe`, `axe`, `shovel`, `hoe`<br>• **Ranged/Def (3):** `bow`, `shield`, `fishing_rod`<br>• **Cosmetic (2):** `hat`, `wing` (elytra/backpiece) | `§d§l🌸 Sakura <Item> 🌸` |
| **`pink_legacy`** | Pink Legacy | **19 Item** | • **Melee (4):** `katana`, `sword`, `dagger`, `gauntlet`<br>• **Long (1):** `spear`<br>• **Blunt (1):** `hammer`<br>• **Tools (4):** `pickaxe`, `axe`, `shovel`, `hoe`<br>• **Ranged/Def (3):** `bow`, `crossbow`, `shield`<br>• **Cosmetic/Util (2):** `wings`, `key`<br>• **Armor (4):** `helmet`, `chestplate`, `leggings`, `boots` | `§d§l✨ Pink Legacy <Item> ✨` |
| **`valentine`** | Valentine Set | **20 Item** | • **Melee/Magic (5):** `sword`, `axe`, `hammer`, `spear`, `staff`<br>• **Tools (3):** `pickaxe`, `shovel`, `hoe`<br>• **Ranged/Def (4):** `bow` (3-stage), `crossbow` (5-stage), `shield`, `fishing_rod`<br>• **Cosmetic/Util (4):** `hat`, `wing`, `key`, `grenade`<br>• **Armor (4):** `helmet`, `chestplate`, `leggings`, `boots` | `§c§l❤ Valentine <Item> ❤` |
| **`dragon_mecha_overlord`** | Dragon Mecha Overlord | **25 Item** | • **Melee/Long (9):** `sword`, `great_sword`, `rapier_sword`, `dagger`, `spear`, `staff`, `scythe`, `hammer`, `trident`<br>• **Tools (4):** `axe`, `pickaxe`, `shovel`, `hoe`<br>• **Ranged/Def (3):** `bow`, `crossbow`, `shield`<br>• **Armor (4):** `helmet`, `chestplate`, `leggings`, `boots`<br>• **Cosmetic/Func (5):** `wing`, `tail`, `jetpack`, `mask`, `core` | `§6§l🐲 Dragon Mecha Overlord <Item> 🐲` |
| **TOTAL** | **Takasha Multi-Set** | **84 Item** | **Lengkap di 1.21.11 dan 26.2** | — |

---

## 3. Matriks Arsitektur Teknis & Divergensi API

```
sakura-weapons/
├── settings.gradle                      # include ':versions:1.21.11', ':versions:26.2'
├── build.gradle                         # Root aggregator: task buildAll, cleanAll
├── gradle.properties                    # mod_version=2.2.0, archives_base_name=Takasha
├── shared-resources/                    # Single Source of Truth
│   ├── assets/
│   │   ├── sakura_weapons/              # Shared models, textures, sounds.json, lang
│   │   ├── dragon_mecha_overlord/
│   │   ├── pink_legacy/
│   │   ├── valentine/
│   │   └── minecraft/
│   └── data/                            # Shared tags, equipment definitions
└── versions/
    ├── 1.21.11/                         # Subproject 1.21.11 (LTS)
    │   ├── build.gradle                 # Java 21, Loom Remap, Intermediary, bytecode 65.0
    │   ├── gradle.properties            # minecraft_version=1.21.11, fabric_version=0.141.6+1.21.11
    │   └── src/main/
    │       ├── java/net/sakura/weapons/ # ResourceLocation, renderWidget(GuiGraphics), FabricItemGroup
    │       └── resources/
    │           ├── fabric.mod.json      # "minecraft": ">=1.21.11- <=1.21.11", "java": ">=21"
    │           └── sakura_weapons.mixins.json
    └── 26.2/                            # Subproject 26.2 (Modern)
        ├── build.gradle                 # Java 25, Loom Unobfuscated, bytecode 69.0
        ├── gradle.properties            # minecraft_version=26.2, fabric_version=0.161.0+26.2
        └── src/main/
            ├── java/net/sakura/weapons/ # Identifier, extractWidgetRenderState, FabricCreativeModeTab, ImGui Flashback
            └── resources/
                ├── fabric.mod.json      # "minecraft": "~26.2", "java": ">=25"
                └── sakura_weapons.mixins.json
```

---

## 4. Rincian Tahapan Eksekusi (Bite-Sized Atomic Tasks)

### Task 1: Pembentukan Direktori `shared-resources/` & Migrasi Aset Bersama
**Files:**
- Create directory: `sakura-weapons/shared-resources/assets`
- Create directory: `sakura-weapons/shared-resources/data`
- Source: `sakura-weapons/src/main/resources/assets` & `sakura-weapons/src/main/resources/data`

- **Step 1:** Salin seluruh aset Blockbench model 3D (`models/item/`), tekstur (`textures/`), animasi (`.mcmeta`), sounds (`sounds.json`), dan file bahasa (`lang/en_us.json`) dari `src/main/resources/assets` ke `shared-resources/assets`.
- **Step 2:** Salin seluruh tags item (`data/minecraft/tags/items/`) dan tag c2me/equipment dari `src/main/resources/data` ke `shared-resources/data`.
- **Step 3:** Verifikasi integritas file di `shared-resources/` (memastikan tidak ada aset yang tertinggal atau korup).

### Task 2: Restrukturisasi Subproject Modern 26.2 (`versions/26.2/`)
**Files:**
- Move current code: `sakura-weapons/src/` -> `sakura-weapons/versions/26.2/src/`
- Create: `sakura-weapons/versions/26.2/build.gradle`
- Create: `sakura-weapons/versions/26.2/gradle.properties`

- **Step 1:** Buat folder `sakura-weapons/versions/26.2/`.
- **Step 2:** Pindahkan direktori `src/main/java` dan file mixin/json spesifik 26.2 ke `versions/26.2/src/main/`.
- **Step 3:** Buat `versions/26.2/gradle.properties`:
  ```properties
  minecraft_version=26.2
  fabric_version=0.161.0+26.2
  loader_version=0.19.3
  loom_version=1.17
  ```
- **Step 4:** Buat `versions/26.2/build.gradle` dengan toolchain Java 25 (`java-runtime-epsilon`), mode unobfuscated loom, dan task `processResources` yang menyertakan aset dari `shared-resources/`.
- **Step 5:** Uji kompilasi mandiri:
  `./gradlew :versions:26.2:build`
  Expected: BUILD SUCCESSFUL, menghasilkan `Hasil mod/Takasha-2.2.0+26.2.jar` (bytecode major 69.0).

### Task 3: Pembuatan Subproject LTS 1.21.11 (`versions/1.21.11/`) & Adaptasi Source Code
**Files:**
- Create: `sakura-weapons/versions/1.21.11/build.gradle`
- Create: `sakura-weapons/versions/1.21.11/gradle.properties`
- Create: `sakura-weapons/versions/1.21.11/src/main/resources/fabric.mod.json`
- Create: `sakura-weapons/versions/1.21.11/src/main/resources/sakura_weapons.mixins.json`
- Port/Adapt Java files: `sakura-weapons/versions/1.21.11/src/main/java/net/sakura/weapons/...`

- **Step 1:** Buat struktur direktori `versions/1.21.11/`.
- **Step 2:** Konfigurasikan `versions/1.21.11/gradle.properties`:
  ```properties
  minecraft_version=1.21.11
  fabric_version=0.141.6+1.21.11
  loader_version=0.19.3
  loom_version=1.17
  ```
- **Step 3:** Konfigurasikan `versions/1.21.11/build.gradle`:
  - Plugin: `id 'net.fabricmc.fabric-loom-remap' version '1.17.21'`
  - JVM Toolchain: Java 21 LTS (`java-runtime-delta`), `options.release = 21`
  - Mapping: `mappings loom.officialMojangMappings()`
  - Include shared resources dari `shared-resources/`.
- **Step 4:** Adaptasi source code Java 1.21.11:
  1. Ganti import `net.minecraft.resources.Identifier` menjadi `net.minecraft.resources.ResourceLocation`.
  2. Ganti `EntityTypes` menjadi `EntityType`.
  3. Ganti `FabricCreativeModeTab` menjadi `FabricItemGroup`.
  4. Ganti `KeyMappingHelper` menjadi `KeyBindingHelper`.
  5. Sesuaikan `AnvilSideListWidget.java` ke signature vanilla 1.21.11: `renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta)`.
  6. Sesuaikan `LivingEntityRenderLayerRegistrationCallback` ke `LivingEntityFeatureRendererRegistrationCallback`.
  7. Bersihkan dependensi 26.2 khusus (Dear ImGui Flashback yang tidak kompatibel di 1.21.11) atau berikan no-op fallback.
- **Step 5:** Uji kompilasi mandiri:
  `./gradlew :versions:1.21.11:build`
  Expected: BUILD SUCCESSFUL, menghasilkan `Hasil mod/Takasha-2.2.0+1.21.11.jar` (bytecode major 65.0).

### Task 4: Konfigurasi Root Gradle Multi-Project & Agregasi Build
**Files:**
- Modify: `sakura-weapons/settings.gradle`
- Modify: `sakura-weapons/build.gradle`
- Modify: `VERSION` (set to `2.2.0`)

- **Step 1:** Perbarui `sakura-weapons/settings.gradle`:
  ```groovy
  rootProject.name = 'sakura-weapons'
  include ':versions:1.21.11'
  include ':versions:26.2'
  ```
- **Step 2:** Perbarui root `sakura-weapons/build.gradle`:
  - Definisikan shared configurations `subprojects { ... }`.
  - Daftarkan task `buildAll`:
    ```groovy
    task buildAll {
        dependsOn ':versions:1.21.11:build', ':versions:26.2:build'
        group = 'build'
        description = 'Kompilasi paralel untuk kedua target Minecraft (1.21.11 dan 26.2)'
    }
    ```
- **Step 3:** Bersihkan folder temporary lama (`src_26.2_experimental/` jika sudah dipindahkan sepenuhnya).

### Task 5: Validasi Komprehensif & Verifikasi Output Biner Lokal
**Files:**
- Verify outputs in: `d:/Mod Minecraft/weapon set/sakura/Hasil mod/`

- **Step 1:** Jalankan `./gradlew clean buildAll` dari root `sakura-weapons`.
- **Step 2:** Verifikasi keberadaan file di `Hasil mod/`:
  - `Hasil mod/Takasha-2.2.0+1.21.11.jar` (Ada, ukuran ~1.7 - 1.8 MB).
  - `Hasil mod/Takasha-2.2.0+26.2.jar` (Ada, ukuran ~1.7 - 1.8 MB).
- **Step 3:** Verifikasi versi bytecode:
  - Ekstrak sampel `.class` dari JAR 1.21.11 dan jalankan `javap -v`: wajib major `65.0` (Java 21).
  - Ekstrak sampel `.class` dari JAR 26.2 dan jalankan `javap -v`: wajib major `69.0` (Java 25).
- **Step 4:** Verifikasi Kepatuhan PRD Bagian 10:
  - Pastikan 0 file disalin ke direktori launcher eksternal Modrinth.
  - Resource Pack (`Hasil RP/`) tetap murni tanpa modifikasi tidak sah.

---

## 5. Kriteria Keberhasilan & Checklist Verifikasi

- [x] Subproyek `versions/1.21.11` terisolasi dan dapat dikompilasi dengan `./gradlew :versions:1.21.11:build` (BUILD SUCCESSFUL).
- [x] Subproyek `versions/26.2` terisolasi dan dapat dikompilasi dengan `./gradlew :versions:26.2:build` (BUILD SUCCESSFUL).
- [x] Task `./gradlew buildAll` membangun kedua JAR secara berurutan/paralel dengan status `BUILD SUCCESSFUL`.
- [x] Aset di `shared-resources/` termuat 100% utuh di kedua file JAR (735 aset termasuk seluruh 84 model item, tekstur, sounds, dan lang).
- [x] File JAR 1.21.11 terverifikasi Java 21 bytecode 65.0 (`Takasha-2.2.0+1.21.11.jar`).
- [x] File JAR 26.2 terverifikasi Java 25 bytecode 69.0 (`Takasha-2.2.0+26.2.jar`).
- [x] File output berada di `Hasil mod/Takasha-2.2.0+1.21.11.jar` dan `Hasil mod/Takasha-2.2.0+26.2.jar`.
- [x] 0 file otomatis ditransfer ke ModrinthApp launcher eksternal (mematuhi PRD Bagian 10).

---

## 6. Execution Handoff

Plan complete and saved to `docs/plans/2026-10-08-standardisasi-dual-version-source-code-plan.md`. Two execution options:

1. **Subagent-Driven (this session)** - I dispatch fresh subagent per task, review between tasks, fast iteration.
2. **Parallel Session (separate)** - Open new session with executing-plans, batch execution with checkpoints.

**Which approach would you like to take?**
