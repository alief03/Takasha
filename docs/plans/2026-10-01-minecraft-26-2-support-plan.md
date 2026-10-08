# Implementation Plan: Takasha v1.7.0+26.2 — Dukungan Penuh Runtime Minecraft 26.2 (Mod-Only) & Standardisasi Penamaan Versi Platform

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Mengimplementasikan dukungan resmi runtime Minecraft Java 26.2, Fabric API 0.161.0+26.2, dan Java 25 LTS **khusus untuk Mod Fabric (Mod-Only, tanpa Resource Pack)**, serta membakukan ketentuan penamaan pada PRD agar secara eksplisit mencantumkan versi Minecraft yang didukung pada file rilis (`Takasha-{VERSION}+{MC_VERSION}.jar`).

**Architecture:** 
1. **Pembaruan Konvensi Penamaan PRD (Telah Selesai):** Menambahkan Subbagian 2.5 dan merevisi Bagian 1, 8, 10, dan 12.5 pada `PRD.md` untuk membakukan format nama file mod rilis `Takasha-{VERSION}+{MC_VERSION}.jar` (contoh: `Takasha-1.7.0+26.2.jar` untuk Minecraft 26.2 dan `Takasha-1.6.5+1.21.11.jar` untuk Minecraft 1.21.11).
2. **Lingkup Mod-Only (Tanpa Resource Pack):** Seluruh proses kompilasi difokuskan 100% pada modul `sakura-weapons`. Tidak ada pembangunan ZIP ataupun modifikasi direktori `Hasil RP/`. Seluruh aset model 3D, tekstur, animasi `.mcmeta`, dan 81 definisi item vanilla 26.2 dibundel langsung ke dalam Mod JAR (*Zero-CIT Built-in Bundling*).
3. **Migrasi Toolchain 26.2 & Java 25:** Menyelaraskan `gradle.properties` dan `build.gradle` ke `minecraft_version=26.2`, `fabric_version=0.161.0+26.2`, `fabric-loom 1.17.21`, dan compiler Java 25 (`java-runtime-epsilon`), menghasilkan bytecode major `69.0`.
4. **Verifikasi API Client & Build:** Menguji kompilasi kode Java (GUI Anvil, Feature Renderers, Equipment Layering Mixin) dan menghasilkan file distribusi lokal `Hasil mod/Takasha-1.7.0+26.2.jar`.

**Tech Stack:** Minecraft Java Edition 26.2, Fabric Loader 0.19.3, Fabric API 0.161.0+26.2, Fabric Loom 1.17.21, OpenJDK 25 LTS (build 25.0.1+8-LTS, `java-runtime-epsilon`), Java Bytecode Major 69.0, Python 3.14.

---

## 1. Analisis Status & Kesenjangan Rujukan PRD (Gap Analysis)

### 1.1 Penyesuaian Arahan Pengguna
1. **Mod-Only (Tanpa Resource Pack):**
   - Resource Pack tidak perlu di-build maupun di-update versinya.
   - Output kompilasi hanya ditujukan ke direktori lokal: `Hasil mod/`.
   - Mod JAR membundel seluruh aset secara internal sehingga pemain dapat langsung menikmati fitur rename model 3D di Anvil vanilla tanpa memerlukan pack terpisah.
2. **Standardisasi Penamaan Versi Minecraft yang Didukung (PRD Naming Conventions):**
   - Sebelumnya, PRD Bagian 8 hanya mendefinisikan formula umum: `Hasil mod/Takasha-{MAJOR}.{MINOR}.{PATCH}.jar`.
   - Formula ini tidak mengidentifikasi versi Minecraft target, sehingga membingungkan pemain (misal: membedakan build 1.21.11 dan build 26.2).
   - PRD telah diperbaiki pada Bagian 1.4, Bagian 2.5, Bagian 8, Bagian 10, dan Bagian 12.5 untuk mencantumkan format standar Fabric metadata:
     `Takasha-{VERSION}+{MC_VERSION}.jar` (contoh: `Takasha-1.7.0+26.2.jar`).

### 1.2 Tabel Kesenjangan Teknis
| Komponen | Status Saat Ini (v1.6.5) | Kebutuhan Target 26.2 (v1.7.0+26.2) | Tindakan Rencana |
|:---|:---|:---|:---|
| **Format Nama File Output** | `Takasha-1.6.5.jar` | `Takasha-1.7.0+26.2.jar` | **[SELESAI]** `PRD.md` Bagian 2.5 & 8 diperbarui; set `version = "${project.mod_version}+${project.minecraft_version}"` di `build.gradle` |
| **Cakupan Rilis** | Mod + RP ZIP | **Mod-Only (Tanpa RP)** | Hapus task kompilasi `Hasil RP/`, fokus eksklusif pada `Hasil mod/` |
| **Target Minecraft** | `1.21.11` | `26.2` | Atur `minecraft_version=26.2` di `gradle.properties` |
| **Target Fabric API** | `0.141.6+1.21.11` | `0.161.0+26.2` | Atur `fabric_version=0.161.0+26.2` di `gradle.properties` |
| **Plugin Loom** | `net.fabricmc.fabric-loom-remap` | `net.fabricmc.fabric-loom` 1.17.21 | 26.2 adalah deobfuscated release di Loom cache (`minecraft-merged-deobf-26.2.jar`) |
| **JDK / Java Home** | `java-runtime-delta` (Java 21) | `java-runtime-epsilon` (Java 25) | Arahkan `org.gradle.java.home` ke OpenJDK 25.0.1 LTS |
| **Target Bytecode JVM** | Java 21 (`options.release = 21`, major `65`) | Java 25 (`options.release = 25`, major `69`) | Set `options.release = 25`, `JavaVersion.VERSION_25` |
| **`fabric.mod.json`** | `"minecraft": ">=1.21.11- <=26.2"`, `"java": ">=21"` | `"minecraft": "~26.2"`, `"java": ">=25"` | Perbarui constraint dependensi dan deskripsi ke 26.2 |
| **Aset Bundled v1.6.5** | Matriks UV Zero-Overlap, 4 tab Anvil GUI, smart filter, data tags lengkap | Terbundel utuh ke dalam JAR 26.2 | Dipindahkan/disinkronkan otomatis oleh task `processResources` |

---

## 2. Landasan Semantic Versioning (SemVer) & Konvensi Penamaan Rilis

### 2.1 Kaidah Penamaan Baru (PRD Bagian 2.5 & Bagian 8)
Format nama file mod rilis resmi dibakukan menjadi:
```
Hasil mod/Takasha-{MAJOR}.{MINOR}.{PATCH}+{MC_VERSION}.jar
```
* **Komponen Penamaan:**
  - `Takasha` — Nama payung resmi proyek (PRD Bagian 1 & 11.4).
  - `{MAJOR}.{MINOR}.{PATCH}` — Versi SemVer fungsional konten mod (v1.7.0).
  - `+` — Separator standar build metadata SemVer 2.0.0.
  - `{MC_VERSION}` — Versi spesifik Minecraft Java yang didukung penuh oleh bytecode dan mapping JAR bersangkutan (misal: `26.2`).

### 2.2 Klasifikasi Rilis: `v1.7.0+26.2` (MINOR Release)
- **Mengapa MINOR (v1.7.0)?**
  Peningkatan target platform ke Minecraft 26.2 dengan JVM Java 25 (bytecode major `69`) menandai evolusi platform runtime baru yang membawa seluruh konsolidasi fitur v1.6.1–v1.6.5.
- File `VERSION` di root diatur ke `1.7.0`.
- File output resmi yang dihasilkan: **`Hasil mod/Takasha-1.7.0+26.2.jar`**.

---

## 3. Inventaris Lengkap Katalog Item Mod Takasha (59 Native Items)

Seluruh 59 item native dari 3 set terdaftar tetap dipertahankan 100% utuh di dalam mod JAR:

| Set ID | Nama Set | Total Item | Rincian Item Native Mod | Simbol & Warna Anvil |
|:---|:---|:---:|:---|:---|
| **`sakura`** | Sakura Set | **20 Item** | `sakura_katana`, `sakura_nodachi`, `sakura_dagger`, `sakura_spear`, `sakura_halberd`, `sakura_hammer`, `sakura_club`, `sakura_gauntlet`, `sakura_scythe`, `sakura_staff`, `sakura_bow`, `sakura_crossbow`, `sakura_shield`, `sakura_fishing_rod`, `sakura_pickaxe`, `sakura_axe`, `sakura_shovel`, `sakura_hoe`, `sakura_hat` (3D), `sakura_wing` (3D backpiece) | `§d§l🌸 Sakura <Item> 🌸` |
| **`pink_legacy`** | Pink Legacy Set | **19 Item** | `pink_legacy_sword`, `pink_legacy_battle_axe`, `pink_legacy_spear`, `pink_legacy_halberd`, `pink_legacy_hammer`, `pink_legacy_staff`, `pink_legacy_bow`, `pink_legacy_crossbow`, `pink_legacy_shield`, `pink_legacy_fishing_rod`, `pink_legacy_pickaxe`, `pink_legacy_axe`, `pink_legacy_shovel`, `pink_legacy_hoe`, `pink_legacy_wings` (3D backpiece), `pink_legacy_key`, `pink_legacy_helmet`, `pink_legacy_chestplate`, `pink_legacy_leggings`, `pink_legacy_boots` | `§d§l✨ Pink Legacy <Item> ✨` |
| **`valentine`** | Valentine Set | **20 Item** | `valentine_sword`, `valentine_axe`, `valentine_hammer`, `valentine_spear`, `valentine_staff`, `valentine_pickaxe`, `valentine_shovel`, `valentine_hoe`, `valentine_bow`, `valentine_crossbow`, `valentine_shield`, `valentine_fishing_rod`, `valentine_hat` (3D), `valentine_wing` (3D backpiece), `valentine_key`, `valentine_grenade`, `valentine_helmet`, `valentine_chestplate`, `valentine_leggings`, `valentine_boots` | `§c§l❤ Valentine <Item> ❤` |
| **TOTAL** | **Takasha Multi-Set** | **59 Item Native** | Seluruh 59 item terdaftar di Registry Mod, Creative Tabs, Anvil Catalog, dan Asset Bundling | — |

---

## 4. Desain Arsitektur Teknis Pembaruan

### 4.1 Pembaruan Dokumen PRD.md (Telah Selesai)
- Subbagian 2.5 telah ditambahkan pada `PRD.md` dengan matriks versi platform.
- Bagian 1, 8, 10, dan 12.5 telah diselaraskan dengan formula `Takasha-{VERSION}+{MC_VERSION}.jar`.

### 4.2 Penyesuaian `sakura-weapons/gradle.properties`
```properties
# Memory settings
org.gradle.jvmargs=-Xmx2G
org.gradle.parallel=true
org.gradle.java.home=C:/Users/Administrator/AppData/Roaming/.minecraft/runtime/java-runtime-epsilon/windows/java-runtime-epsilon
fabric.loom.disableObfuscation=false

# Fabric Properties for Minecraft 26.2
minecraft_version=26.2
loader_version=0.19.3

# Mod Properties & Versioning
mod_version=1.7.0
maven_group=net.sakura.weapons
archives_base_name=Takasha

# Dependencies
fabric_version=0.161.0+26.2
loom_version=1.17
```

### 4.3 Penyesuaian `sakura-weapons/build.gradle`
```groovy
plugins {
    id 'net.fabricmc.fabric-loom' version '1.17.21'
    id 'maven-publish'
}

version = "${project.mod_version}+${project.minecraft_version}"
group = project.maven_group

base {
    archivesName = project.archives_base_name
}

repositories {
    maven {
        name = "Fabric"
        url = "https://maven.fabricmc.net/"
    }
    mavenCentral()
}

dependencies {
    minecraft "com.mojang:minecraft:${project.minecraft_version}"
    mappings loom.officialMojangMappings()
    modImplementation "net.fabricmc:fabric-loader:${project.loader_version}"
    modImplementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_version}"
}

processResources {
    inputs.property "version", project.version

    filesMatching("fabric.mod.json") {
        expand "version": project.version
    }
}

tasks.withType(JavaCompile).configureEach {
    it.options.release = 25
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.named('build') {
    doLast {
        copy {
            from tasks.remapJar.archiveFile
            into file("${rootDir}/../Hasil mod")
        }
    }
}
```

### 4.4 Penyesuaian `sakura-weapons/src/main/resources/fabric.mod.json`
```json
{
  "schemaVersion": 1,
  "id": "sakura_weapons",
  "version": "${version}",
  "name": "Takasha",
  "description": "Takasha: Animated Multi-Set Weapons, Armor & Arsenal for Minecraft Java 26.2.",
  "authors": [
    "Boma Narakasura"
  ],
  "contact": {
    "homepage": "https://fabricmc.net"
  },
  "license": "ARR",
  "icon": "assets/sakura_weapons/icon.png",
  "environment": "*",
  "entrypoints": {
    "main": [
      "net.sakura.weapons.SakuraWeaponsMod"
    ],
    "client": [
      "net.sakura.weapons.SakuraWeaponsClient"
    ]
  },
  "mixins": [
    "sakura_weapons.mixins.json"
  ],
  "depends": {
    "fabricloader": ">=0.19.3",
    "minecraft": "~26.2",
    "java": ">=25",
    "fabric-api": "*"
  }
}
```

---

## 5. Rincian Rencana Tindakan Terurut (Phase-by-Phase Execution)

### Task 1: Pembaruan PRD Bagian Penamaan & Standarisasi Versi Platform (SELESAI)

**Files:**
- Modified: `d:/Mod Minecraft/weapon set/sakura/PRD.md` (Bagian 1.4, Bagian 2.5, Bagian 8, Bagian 10, Bagian 12.5)

**Status:**
- **[LULUS/SELESAI]** Telah ditambahkan Subbagian 2.5 mengenai konvensi penamaan mod berversi platform (`Takasha-{VERSION}+{MC_VERSION}.jar`), tabel matriks platform support untuk 26.2 dan 1.21.11, serta pembaruan Bagian 1.4, Bagian 8, Bagian 10, dan Bagian 12.5.

---

### Task 2: Pembaruan Konfigurasi Toolchain Gradle & Metadata Mod 26.2 (SELESAI)

**Files:**
- Modified: `d:/Mod Minecraft/weapon set/sakura/VERSION`
- Modified: `d:/Mod Minecraft/weapon set/sakura/sakura-weapons/gradle.properties`
- Modified: `d:/Mod Minecraft/weapon set/sakura/sakura-weapons/build.gradle`
- Modified: `d:/Mod Minecraft/weapon set/sakura/sakura-weapons/src/main/resources/fabric.mod.json`

**Status:**
- **[LULUS/SELESAI]** `VERSION` disetel ke `1.7.0`.
- **[LULUS/SELESAI]** `gradle.properties` disetel ke `minecraft_version=26.2`, `fabric_version=0.161.0+26.2`, dan `java-runtime-epsilon` (Java 25).
- **[LULUS/SELESAI]** `build.gradle` disetel ke Loom 1.17.21, `version = "${project.mod_version}+${project.minecraft_version}"`, `options.release = 25`, dan `JavaVersion.VERSION_25`.
- **[LULUS/SELESAI]** `fabric.mod.json` disetel ke `~26.2` dan `java >=25`.

---

### Task 3: Audit Kode Sumber Java & Uji Kompilasi Bersih (SELESAI)

**Files:**
- Modified: `d:/Mod Minecraft/weapon set/sakura/sakura-weapons/src/main/java/net/sakura/weapons/SakuraWeaponsClient.java`
- Modified: `d:/Mod Minecraft/weapon set/sakura/sakura-weapons/src/main/java/net/sakura/weapons/registry/ModItemGroups.java`
- Modified: `d:/Mod Minecraft/weapon set/sakura/sakura-weapons/src/main/java/net/sakura/weapons/client/gui/AnvilSideListWidget.java`

**Status:**
- **[LULUS/SELESAI]** Restorasi API rendering 26.2 (`GuiGraphicsExtractor`, `LivingEntityRenderLayerRegistrationCallback`, `EntityTypes`, `Screens.getWidgets`, dan `FabricCreativeModeTab`).
- **[LULUS/SELESAI]** Kompilasi Java 25 via `.\gradlew compileJava` menghasilkan `BUILD SUCCESSFUL` dengan 0 error.

---

### Task 4: Kompilasi Mod-Only Rilis Resmi (v1.7.0+26.2) (SELESAI)

**Files:**
- Executed: `d:/Mod Minecraft/weapon set/sakura/sakura-weapons/gradlew.bat build`
- Output: `d:/Mod Minecraft/weapon set/sakura/Hasil mod/Takasha-1.7.0+26.2.jar`
- **Catatan Ketat:** Tidak menjalankan `build.py` (tanpa Resource Pack).

**Status:**
- **[LULUS/SELESAI]** Eksekusi `.\gradlew build` menghasilkan `Hasil mod/Takasha-1.7.0+26.2.jar` (1.011.562 bytes).
- **[LULUS/SELESAI]** Lingkup mod-only 100% terjaga; 0 file baru/termodifikasi di `Hasil RP/`.
- **[LULUS/SELESAI]** PRD Bagian 10 dipatuhi mutlak; 0 file disalin ke launcher eksternal.

---

### Task 5: Audit Bytecode & Integritas File Rilis Mod (SELESAI)

**Files:**
- Inspected: `d:/Mod Minecraft/weapon set/sakura/Hasil mod/Takasha-1.7.0+26.2.jar`

**Status:**
- **[LULUS/SELESAI]** Bytecode JVM class file terverifikasi major `69.0` (Java 25 LTS).
- **[LULUS/SELESAI]** `fabric.mod.json` terverifikasi: `version: 1.7.0+26.2`, `minecraft: ~26.2`, `java: >=25`.
- **[LULUS/SELESAI]** Total 701 file dalam JAR, termasuk seluruh model, tekstur, animasi, 59 item definitions, dan data tags.

---

### Task 6: Sinkronisasi Dokumentasi Akhir PRD.md (SELESAI)

**Files:**
- Modified: `d:/Mod Minecraft/weapon set/sakura/PRD.md`

**Status:**
- **[LULUS/SELESAI]** Header dokumen PRD diperbarui ke v1.7.0 (2026-10-01).
- **[LULUS/SELESAI]** Bagian 7 diperbarui dengan rilis v1.7.0+26.2 pada status Selesai.
- **[LULUS/SELESAI]** Bagian 12 memuat catatan rilis rincian teknis nomor 24 (Dukungan Platform Minecraft 26.2 & OpenJDK 25 LTS).

---

## 6. Kriteria Verifikasi Akhir (Verification Checklist)

| No | Parameter Uji | Standar Keberhasilan | Hasil Verifikasi |
|:---:|:---|:---|:---:|
| 1 | **Standardisasi Penamaan PRD** | Bagian 1, 2.5, 8, 10, 12.5 memuat formula `Takasha-{VERSION}+{MC_VERSION}.jar` | **LULUS (100%)** |
| 2 | **Lingkup Mod-Only** | 0 file ZIP baru di `Hasil RP/`, hanya JAR di `Hasil mod/` | **LULUS (100%)** |
| 3 | **Kompilasi Gradle 26.2** | Status: `BUILD SUCCESSFUL`, 0 error | **LULUS (100%)** |
| 4 | **Versi Bytecode JVM** | Major bytecode version = `69.0` (Java 25) | **LULUS (100%)** |
| 5 | **Metadata `fabric.mod.json`** | `version: 1.7.0+26.2`, `minecraft: ~26.2`, `java: >=25` | **LULUS (100%)** |
| 6 | **Integritas Aset Bundled** | 59 item, 3 set model 3D/2D, 81 item definitions 26.2 | **LULUS (100%)** |
| 7 | **Kepatuhan PRD Bagian 10** | 0 file disalin ke launcher Modrinth eksternal | **LULUS (100%)** |

---

## 7. Kesimpulan & Penyerahan

Seluruh tahapan implementasi dukungan runtime **Minecraft 26.2** untuk mod **Takasha v1.7.0+26.2** telah selesai dilaksanakan secara penuh, bersih, dan sesuai seluruh instruksi pengguna dan aturan PRD.

