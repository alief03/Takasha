# 🌸 Sakura-Weapons (Takasha Core)

[![Minecraft](https://img.shields.io/badge/Minecraft-26.2%20%7C%201.21.11-brightgreen.svg)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-25%20%7C%2021-orange.svg)](https://adoptium.net/)
[![Fabric](https://img.shields.io/badge/Loader-Fabric%20%3E%3D0.19.3-blue.svg)](https://fabricmc.net/)
[![Multi-Project](https://img.shields.io/badge/Architecture-Dual--Version%20Gradle-purple.svg)](build.gradle)

Mod inti dari **Takasha Ecosystem** yang menghadirkan persenjataan animasi tingkat lanjut (*animated custom weapons*), set armor 3D dan sayap kosmetik, otomatisasi gameplay mutakhir (Crop Plot Memory Auto-Farming, Auto-Bow, Auto-Fishing), sistem NPC Statue & ReplayMod Zero-Desync, serta GUI Dashboard terpusat.

---

## 📑 Daftar Isi
- [Arsitektur Dual-Version](#-arsitektur-dual-version)
- [Fitur Utama](#-fitur-utama)
  - [Persenjataan Animasi & Partikel](#persenjataan-animasi--partikel)
  - [Armor 3D, Wings & Elytra Flight](#armor-3d-wings--elytra-flight)
  - [Auto-Farm dengan Crop Plot Memory](#auto-farm-dengan-crop-plot-memory)
  - [Automasi Lainnya](#automasi-lainnya)
  - [NPC Statue, Emotecraft & ReplayMod](#npc-statue-emotecraft--replaymod)
  - [Takasha Dashboard GUI](#takasha-dashboard-gui)
- [Struktur Proyek](#-struktur-proyek)
- [Panduan Kompilasi & Build](#-panduan-kompilasi--build)
- [Integrasi Mod Lain](#-integrasi-mod-lain)

---

## 🏗 Arsitektur Dual-Version

`sakura-weapons` dibangun menggunakan arsitektur Gradle multi-project yang memisahkan target runtime Minecraft namun tetap berbagi aset dan resource yang sama:

| Subproject | Versi Minecraft | Versi Java | Fabric Loader | Fitur Kunci |
| :--- | :--- | :--- | :--- | :--- |
| **`versions/26.2`** | Minecraft 26.2 (1.21.4+) | Java 25 | `>=0.19.3` | Pipeline render modern, Flashback 26.2 compat, YSM compat |
| **`versions/1.21.11`** | Minecraft 1.21.11 | Java 21 | `>=0.16.5` | ReplayMod legacy compat, backport fitur automasi & armor |
| **`shared-resources`** | Semua | - | - | Model JSON, Textures, Sound events, Language (ID/EN) |

---

## ⚔ Fitur Utama

### Persenjataan Animasi & Partikel
- **Weapon Classes**: Katana, Greatsword, Scythe, Dagger, Staff, Spear, Bow.
- **Custom Renderers**: Interpolasi frame animasi berbasis delta tick, partikel tebasan dinamis (*slashing particle trails*), dan efek pencahayaan dinamis.
- **Spear Tiers & Copper Tools**: Mendukung varian tembaga (*copper tools*) dan tier senjata kustom hingga Netherite.

### Armor 3D, Wings & Elytra Flight
- **Sakura Wings**: Model sayap 3D dengan animasi kepakan dinamis (*idle, sprint, gliding*).
- **Paper & Native Elytra Integration**: Berfungsi ganda sebagai pelindung kosmetik atau elytra fungsional dengan sinkronisasi rotasi roll/pitch/yaw.
- **Set Armor Tambahan**:
  - *Pink Legacy Set*
  - *Valentine's Day Cosmetic Set*
  - *Dragon Mecha Overlord Set*

### Auto-Farm dengan Crop Plot Memory
- **State Memory Tracking**: Menyimpan koordinat setiap petak lahan (`CropPlotMemory`) untuk mencegah desync server-client saat panen dan tanam ulang (*replant*).
- **Smart Seed Rotation**: Membaca inventory pemain secara otomatis untuk mengambil benih yang sesuai dengan jenis tanaman sebelumnya (Wheat, Carrot, Potato, Beetroot, Nether Wart).
- **Safe Harvest Boundary**: Validasi `isValidReplantTarget` dan batas radius panen aman tanpa merusak lahan cangkulan (*farmland*).

### Automasi Lainnya
- **Auto-Bow**: Estimasi lintasan anak panah (*trajectory prediction*), kompensasi gravitasi, dan bantuan bidikan.
- **Auto-Fishing**: Deteksi getaran pelampung pancing (*bobber velocity & splash*) dengan auto-reel instan dan pelemparan ulang otomatis.
- **Auto-Mob / Mob Kill**: Deteksi monster dalam radius aman dengan line-of-sight check untuk grinding mob otomatis.
- **Fortune Miner & Vein Miner**: Penambangan urat bijih cepat dengan optimasi drop rate.

### NPC Statue, Emotecraft & ReplayMod
- **Zero-Desync NPC Statue**: Spawner NPC kustom (`NpcSpawnerWandItem`) untuk menaruh patung pemain/model di world tanpa desync posisi.
- **Emotecraft & ReplayMod Override**: Sinkronisasi pose duduk (*preset 9 sitting*), emote looping, dan integrasi Flashback 26.2 tanpa ghost entity.

### Takasha Dashboard GUI
Tekan hotkey konfigurasi (default: **K** atau via menu) untuk membuka antarmuka terintegrasi:
- **`TakashaDashboardScreen`**: Pusat kendali semua modul.
- **Sub-Screens**:
  - `TakashaFarmingSubScreen`: Konfigurasi radius, jenis benih, dan interval auto-farm.
  - `TakashaAutoBowSubScreen`: Konfigurasi akurasi dan target filter bow.
  - `TakashaFishingSubScreen`: Konfigurasi delay reel dan deteksi air.
  - `TakashaMobKillSubScreen`: Konfigurasi target blacklist/whitelist dan jarak serang.
  - `TakashaFortuneSubScreen`: Konfigurasi vein mining dan filter ore.
  - `TakashaNpcScreen`: Pengaturan skin, pose, dan rotasi NPC statue.
  - `TakashaReplayOverrideScreen`: Pengaturan playback ReplayMod.

---

## 📁 Struktur Proyek

```
sakura-weapons/
├── build.gradle              # Master Gradle orchestrator (buildAll, cleanAll)
├── settings.gradle           # Registrasi module 26.2, 1.21.11, takasha-rpg, touhou-little-maid
├── gradle.properties         # Versi mod, compiler flags, dan path JVM
├── shared-resources/         # Asset bersama (textures, models, sounds, lang)
├── versions/
│   ├── 26.2/                 # Source code & mixin khusus Minecraft 26.2
│   └── 1.21.11/              # Source code & mixin khusus Minecraft 1.21.11
└── docs/                     # Perencanaan fitur dan walk-through teknis
```

---

## 🛠 Panduan Kompilasi & Build

### Prasyarat
- **Java 25** (untuk target 26.2)
- **Java 21** (untuk target 1.21.11)
- Gradle wrapper yang tersedia (`gradlew`)

### Perintah Build
Buka terminal di direktori `sakura-weapons/`:

```powershell
# Kompilasi seluruh target ekosistem (26.2, 1.21.11, takasha-rpg, touhou-little-maid)
./gradlew buildAll

# Kompilasi khusus versi 26.2
./gradlew :versions:26.2:build

# Kompilasi khusus versi 1.21.11
./gradlew :versions:1.21.11:build

# Membersihkan seluruh cache build
./gradlew cleanAll
```

Output `.jar` akan dihasilkan di direktori `versions/<versi>/build/libs/`.

---

## 🔗 Integrasi Mod Lain

- **[Takasha-RPG](../takasha-rpg/)**: Berbagi data combat, skill XP (Swordsmanship saat memakai katana, Archery saat memakai Sakura Bow, Farming saat memakai Auto-Farm).
- **[Touhou Little Maid](../touhou-little-maid/)**: Maid dapat dipersenjatai dengan Sakura Weapons dan dilengkapi Sakura Wings sebagai aksesoris kosmetik.
- **Flashback & ReplayMod**: Dukungan rendering kamera sinematik dengan pose kustom tanpa desync animasi.
