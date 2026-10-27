# 🧹 Touhou Little Maid (Takasha 26.2 Port)

[![Minecraft](https://img.shields.io/badge/Minecraft-26.2-brightgreen.svg)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://adoptium.net/)
[![Fabric](https://img.shields.io/badge/Loader-Fabric%20%3E%3D0.19.3-blue.svg)](https://fabricmc.net/)
[![License](https://img.shields.io/badge/License-MIT%20%2F%20CC%20BY--NC--SA%204.0-yellow.svg)](#)

Port independen dan ekspansi modern dari **Touhou Little Maid** untuk **Minecraft 26.2 (Fabric Loader)**. Dilengkapi dengan dukungan model Bedrock 1.10/1.12, integrasi Yes Steve Model (YSM), sistem afeksi & interaksi (*Affection & Kiss Handler*), sinkronisasi statistik dengan Takasha-RPG, serta optimasi performa *anti-crash* untuk PC berspesifikasi rendah (*low-spec friendly*).

---

## 📑 Daftar Isi
- [Tentang Mod Ini](#-tentang-mod-ini)
- [Fitur Utama](#-fitur-utama)
  - [1. Bedrock Geometry & Molang Engine](#1-bedrock-geometry--molang-engine)
  - [2. Yes Steve Model (YSM) Compatibility](#2-yes-steve-model-ysm-compatibility)
  - [3. Kecerdasan Buatan (Maid AI & Tasks)](#3-kecerdasan-buatan-maid-ai--tasks)
  - [4. Sistem Afeksi & Interaksi (Affection & Kissing)](#4-sistem-afeksi--interaksi-affection--kissing)
  - [5. Sinergi dengan Ekosistem Takasha](#5-sinergi-dengan-ekosistem-takasha)
  - [6. Optimasi Low-Spec & Anti-Crash](#6-optimasi-low-spec--anti-crash)
- [Struktur Direktori & Kode](#-struktur-direktori--kode)
- [Panduan Penggunaan In-Game](#-panduan-penggunaan-in-game)
- [Kompilasi & Build](#-kompilasi--build)

---

## 💡 Tentang Mod Ini

Modul ini sebelumnya berada di dalam direktori resource `sakura-weapons`, namun kini telah **dipisahkan secara penuh menjadi subproject independen** (`touhou-little-maid/`) agar:
1. Memiliki siklus rilis dan versi mandiri.
2. Memudahkan integrasi dengan model packs kustom tanpa membengkakkan ukuran mod senjata.
3. Memberikan dukungan penuh untuk pipeline rendering modern Minecraft 26.2 (Java 25).

---

## 🌸 Fitur Utama

### 1. Bedrock Geometry & Molang Engine
- **Bedrock Formats**: Mendukung penuh parsing file model Bedrock `1.10.0` dan `1.12.0` (format standar Blockbench untuk Minecraft Bedrock & Maid packs).
- **Molang Query Evaluator**: Mengevaluasi ekspresi pergerakan dinamis, sudut rotasi kepala, ayunan kaki saat berjalan, dan kedipan mata (*eye blink animations*).
- **In-Game Model Selector GUI**: Antarmuka visual untuk mengganti model baju, rambut, dan aksesoris maid secara instan dari model pack yang terpasang.

### 2. Yes Steve Model (YSM) Compatibility
- **Seamless Model Integration**: Maid dapat menggunakan model kustom berbasis YSM (*Yes Steve Model*).
- **Network Synchronization**: Sinkronisasi paket animasi kustom dan layer tekstur antar pemain di server multiplayer.

### 3. Kecerdasan Buatan (Maid AI & Tasks)
- **Penjinakan & Perintah**:
  - Jinakkan maid liar menggunakan Cake atau camilan manis.
  - Perintah cepat: Duduk (*Sit*), Mengikuti Pemilik (*Follow*), atau Menjaga Wilayah (*Guard Area*).
- **Tugas Fungsional (Maid Tasks)**:
  - 🌾 **Auto-Farm**: Memanen tanaman matang dan menanam benih kembali secara teratur.
  - 🎣 **Auto-Fish**: Menangkap ikan dan menyimpan hasil tangkapan di tasnya.
  - ⚔ **Combat Guard**: Bertarung bersama pemilik, menggunakan busur atau senjata jarak dekat (kompatibel penuh dengan pedang *Sakura Weapons*!).
  - 🎒 **Backpack Storage**: Inventory 36-slot dengan dukungan tas ransel tingkat lanjut.

### 4. Sistem Afeksi & Interaksi (Affection & Kissing)
- **Skor Afeksi (Favorability Rating)**: Meningkat saat maid diberi makan, disembuhkan, diajak berpetualang, atau diberikan hadiah kesukaannya.
- **Interaksi Unik (`KissMaidHandler`)**:
  - Interaksi khusus saat tingkat afeksi mencapai batas tertentu, memunculkan efek partikel hati dan ekspresi tersipu (*blush emote*).
  - Dialog dan respon suara khusus berdasarkan tingkat kedekatan dengan pemilik.

### 5. Sinergi dengan Ekosistem Takasha
- **Stat Synchronization**: Jika dimainkan bersama `takasha-rpg`, maid mendapatkan bonus atribut berdasarkan stat pemiliknya (misal: Strength pemilik meningkatkan Attack Damage maid).
- **Custom Weapon Equipping**: Maid dapat memegang dan memanfaatkan animasi dari seluruh senjata di `sakura-weapons`.

### 6. Optimasi Low-Spec & Anti-Crash
- **`MatrixPool` Recycling**: Manajemen alokasi matriks transformasi render untuk menghindari garbage collection spike saat banyak maid berada dalam satu chunk.
- **Corrupted Model Fallback**: Jika sebuah model pack eksternal memiliki JSON yang rusak atau tidak valid, engine secara otomatis beralih ke model fallback bawaan tanpa membuat game crash.

---

## 📁 Struktur Direktori & Kode

```
touhou-little-maid/src/main/java/net/sakura/touhou/
├── client/
│   ├── model/               # Bedrock geometry parser & Molang runtime
│   ├── render/              # Maid entity renderer & MatrixPool optimization
│   └── gui/                 # Maid inventory GUI & Model selector screen
├── compat/
│   ├── tlmo/affection/      # KissMaidHandler & affection state persistence
│   └── ysm/                 # Yes Steve Model bridge & networking
├── entity/
│   ├── ai/                  # AI tasks (Auto-Farm, Fishing, Combat Guard, Follow)
│   └── MaidEntity.java      # Definisi entitas utama maid
├── item/                    # Maid spawn egg, model wand, camera, craft items
└── registry/                # Registrasi Entity, Sound, Menu, and Network packets
```

---

## 🎮 Panduan Penggunaan In-Game

1. **Memanggil Maid**: Gunakan Maid Spawn Egg atau buat altar pemanggil.
2. **Menjinakkan**: Berikan kue (*Cake*) pada maid dengan klik kanan.
3. **Membuka Inventory**: `Shift + Klik Kanan` pada maid untuk mengakses 36 slot penyimpanan dan perlengkapan senjata.
4. **Mengganti Model**: Klik kanan maid dengan *Model Selection Wand* untuk membuka daftar model Bedrock & YSM yang tersedia.
5. **Memberikan Senjata**: Taruh *Sakura Katana* atau *Scythe* pada slot tangan utama maid untuk mengubahnya menjadi pengawal tempur mematikan.

---

## 🛠 Kompilasi & Build

Proyek ini dapat dikompilasi secara independen atau melalui master Gradle:

```powershell
# Kompilasi khusus touhou-little-maid dari root
cd "d:/Mod Minecraft/weapon set/sakura"
./gradlew :touhou-little-maid:build

# Jalankan uji coba client Minecraft 26.2
./gradlew :touhou-little-maid:runClient
```

Output `.jar` berada di `touhou-little-maid/build/libs/`.
