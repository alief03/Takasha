# 🛡️ Takasha-RPG

[![Minecraft](https://img.shields.io/badge/Minecraft-26.2-brightgreen.svg)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://adoptium.net/)
[![Fabric](https://img.shields.io/badge/Loader-Fabric%20%3E%3D0.19.3-blue.svg)](https://fabricmc.net/)
[![License](https://img.shields.io/badge/License-ARR-red.svg)](#)

Mod ekspansi RPG komprehensif untuk Minecraft Singleplayer & Fabric Server. **Takasha-RPG** menghadirkan kembali fungsionalitas plugin legendaris Spigot/Paper (AuraSkills, AdvancedEnchantments, AdvancedDungeonArena, EconomyShopGUI, EssentialsX) ke dalam arsitektur murni Fabric modern tanpa dependensi eksternal!

---

## 📑 Daftar Isi
- [Latar Belakang & Filosofi](#-latar-belakang--filosofi)
- [Fitur Utama](#-fitur-utama)
  - [1. AuraSkills Progression Engine](#1-auraskills-progression-engine)
  - [2. AdvancedEnchantments 6-Tier Engine](#2-advancedenchantments-6-tier-engine)
  - [3. Dungeon Rift Arena](#3-dungeon-rift-arena)
  - [4. Damage Dummy & DoT Sub-GUI](#4-damage-dummy--dot-sub-gui)
  - [5. Dynamic Economy & ShopGUI](#5-dynamic-economy--shopgui)
  - [6. Grimoire Sovereignty & Title System](#6-grimoire-sovereignty--title-system)
  - [7. World Events & Quest System](#7-world-events--quest-system)
- [Struktur Paket & Kode](#-struktur-paket--kode)
- [Perintah & Kontrol (/commands)](#-perintah--kontrol-commands)
- [Kompilasi & Pengujian](#-kompilasi--pengujian)
- [Integrasi Web Companion](#-integrasi-web-companion)

---

## 💡 Latar Belakang & Filosofi

Dahulu, fitur MMORPG Minecraft berskala besar seperti skill leveling, ratusan custom enchantment, dungeon bertingkat, dan shop ekonomi hanya dapat dinikmati di server Spigot/Paper melalui tumpukan plugin berat. 

**Takasha-RPG** memecahkan batasan tersebut dengan merekonstruksi (*clean-room implementation*) seluruh sistem tersebut menjadi **Fabric Mod Native**:
- **Zero Spigot/Bukkit Dependency**: 100% berjalan di client singleplayer maupun dedicated server Fabric.
- **Client-Side Rendering Halus**: GUI interaktif berbasis vanilla Canvas, Actionbar HUD dinamis, dan floating numbers tanpa packet spam.
- **Data Persistence**: Seluruh progres pemain tersimpan secara aman dalam NBT file world pemain (`takasha_rpg_data.dat`).

---

## ⚔ Fitur Utama

### 1. AuraSkills Progression Engine
Implementasi murni dari mekanisme leveling stat dan atribut pemain:
- **7 Tipe Skill**:
  - `FARMING`: Meningkatkan Health & Panen ganda.
  - `MINING`: Meningkatkan Toughness & Peluang drop ore ekstra.
  - `COMBAT`: Meningkatkan Strength & Crit Damage.
  - `FORAGING`: Meningkatkan Strength & Double wood drop.
  - `FISHING`: Meningkatkan Luck & Treasure catch rate.
  - `AGILITY`: Meningkatkan Movement Speed & Dodge rate.
  - `ALCHEMY`: Meningkatkan Wisdom & Durasi potion.
- **6 Atribut Karakter**: *Strength*, *Health*, *Wisdom* (Mana regen), *Toughness* (Damage reduction), *Luck*, dan *Ferocity* (Double strike).
- **HUD & UI Interaktif**:
  - `TakashaActionbarHud`: Menampilkan bar HP, Mana, serta popup XP real-time saat melakukan aktivitas.
  - GUI Skill Tree: Menu visual untuk melihat progres skill, statistik atribut, dan milestone reward.

### 2. AdvancedEnchantments 6-Tier Engine
Sistem enchant kustom paling dalam dengan 6 tingkatan kelangkaan (*rarity tiers*):
- **Tingkatan Rarity**:
  1. ⚪ **Simple** (Tier I - Basic enhancements)
  2. 🟢 **Unique** (Tier II - Utility & standard combat)
  3. 🔵 **Elite** (Tier III - Advanced procs & AoE triggers)
  4. 🟣 **Ultimate** (Tier IV - High-tier stats & status effects)
  5. 🟡 **Legendary** (Tier V - Master combat & elemental devastation)
  6. 🔴 **Fabled** (Tier VI - God-tier game-changing passives)
- **Combat & Utility Engine**:
  - Menangani puluhan enchantment unik (Lifesteal, Bleed, Overload, Lightning Strike, Telepathy, Auto-Smelt, dll.).
  - Efek partikel dan sound cue terspesialisasi untuk setiap proc.
- **Sistem Anvil & Scrolls**:
  - `TakashaAnvilHandler`: Penggabungan buku custom enchant pada anvil tanpa konflik batas level vanilla.
  - **Mystery Books & Dust**: Item misteri untuk mengungkap enchant acak dengan success/destroy rate.
  - **White Scrolls & Black Scrolls**: Melindungi item dari kehancuran atau mengekstrak enchant dari senjata.

### 3. Dungeon Rift Arena
Arena pertarungan bergelombang (*wave-based survival arena*):
- **5 Tier Dungeon**: Coal, Iron, Gold, Diamond, dan Netherite Rift.
- **Keystone Portals**: Aktifkan portal rift menggunakan `DungeonRiftKeystoneItem`.
- **Intelligent Mob Hunter AI**: Monster di dalam arena dilengkapi `MobHunterGoal` yang terus melacak pemain di mana pun di dalam arena tanpa batasan jarak pandang standar.
- **Permadeath & Drop Mekanik**: Kegagalan menyelesaikan dungeon dalam batas waktu memberikan penalti, sementara penyelesaian gelombang boss membuka peti reward legendaris berisi `DungeonMaterialItem`.

### 4. Damage Dummy & DoT Sub-GUI
Alat uji coba DPS paling komprehensif untuk pengujian build senjata dan enchantment:
- **Target Dummy Entity**: Entity uji coba kebal yang merekam setiap damage yang masuk.
- **Floating Numbers Anti-Overlap**: Angka damage melayang di atas dummy dengan pemisahan spasial (*spatial layout algorithm*) sehingga tidak bertumpuk.
- **Interactive Status Sub-GUI**:
  - Grafik dan rincian total damage serta Average DPS.
  - Deteksi DoT terpisah: Bleed tick, Burn tick, Poison tick, dan Wither tick.
  - Rasio serangan kritis dan log 50 serangan terakhir.

### 5. Dynamic Economy & ShopGUI
- **/shop**: Menu GUI belanja untuk membeli bibit, ore, perlengkapan, dan material RPG.
- **/sellgui**: GUI serbaguna untuk menjual tumpukan item secara instan berdasarkan nilai ekonomi dinamis.
- Kompatibel dengan sistem mata uang lokal pemain (*Takasha Vault*).

### 6. Grimoire Sovereignty & Title System
- **16 Judul Bergengsi (*RPG Titles*)**: Dibuka setelah memenuhi tantangan tertentu (misal: "The Dragon Slayer", "Rift Conqueror", "Grand Alchemist").
- Penataan Gelar: Muncul pada tab list, nametag di atas kepala pemain, dan chat prefix.

### 7. World Events & Quest System
- **Dynamic World Bosses**: Event kemunculan boss berkala di dunia luar dengan bar kesehatan global dan minion pendamping.
- **Quest System**: Misi harian dan mingguan untuk mendapatkan Keystone, Mystery Books, dan koin ekonomi.

---

## 📁 Struktur Paket & Kode

```
takasha-rpg/src/main/java/net/takasha/rpg/
├── client/
│   ├── gui/                  # Layar GUI (AuraSkillsScreen, DamageDummyScreen, ShopScreen)
│   └── hud/                  # Actionbar HUD & Health scaling bar
├── dungeon/
│   ├── ai/                   # AI mob arena (MobHunterGoal)
│   ├── arena/                # State management wave & rift boundary
│   └── reward/               # Pool drop dan peti hadiah
├── economy/                  # Standalone currency engine & Shop catalog
├── enchant/                  # Combat engine, utility engine, anvil handler, registry
├── entity/                   # DamageDummyEntity & Dungeon Boss entities
├── item/                     # Keystones, Mystery Books, Orbs, Dungeon Materials
├── menu/                     # ScreenHandlers untuk sinkronisasi inventory server-client
├── network/                  # C2S & S2C payload networking (Quests, Titles, HUD toggles)
├── quest/                    # Engine misi harian & mingguan
├── skills/                   # AuraSkills engine, XP progression, Stat modifiers
├── title/                    # Sistem 16 RPG Title & display formatting
└── world/                    # Mob level scaling & world boss event manager
```

---

## 🎮 Perintah & Kontrol (/commands)

| Perintah | Deskripsi |
| :--- | :--- |
| `/skills` | Membuka AuraSkills progression GUI |
| `/shop` | Membuka toko ekonomi Takasha |
| `/sellgui` | Membuka antarmuka jual cepat item |
| `/rpg status` | Menampilkan ringkasan atribut dan statistik aktif pemain |
| `/rpg dummy spawn` | Memunculkan Damage Dummy untuk pengujian DPS |
| `/rpg title` | Memilih dan memasang gelar RPG yang telah dibuka |
| `/rpg quest` | Memeriksa progres misi harian/mingguan aktif |

---

## 🛠 Kompilasi & Pengujian

Proyek ini terdaftar sebagai subproject dalam master Gradle:

```powershell
# Kompilasi takasha-rpg
cd "d:/Mod Minecraft/weapon set/sakura"
./gradlew :takasha-rpg:build

# Jalankan client uji coba (Fabric RunClient 26.2)
./gradlew :takasha-rpg:runClient
```

---

## 🌐 Integrasi Web Companion

Takasha-RPG terhubung dengan web companion lokal di direktori `takasha-rpg-web/`:
- Jalankan `node server.js` di folder `takasha-rpg-web`.
- Buka browser di `http://localhost:5173/` untuk mengakses katalog enchantment interaktif, kalkulator stat, dan panduan rift dungeon!
