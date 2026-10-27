---
name: rea-essentialsx-parity
description: Reverse engineer EssentialsX Spigot/Paper command architectures and translate them into pure Fabric 26.2 RPG modular systems with survival executable loot items, permission-isolated OP commands, dynamic help, and player progress reset.
---

# REA EssentialsX Parity & RPG Command Architecture

Panduan arsitektur dan metodologi *reverse engineering* untuk mentranslasikan ekosistem plugin **EssentialsX** (Spigot/Bukkit/Paper) menjadi arsitektur mod murni **Fabric 26.2** (`takasha-rpg`).

---

## 1. Pembagian Akses Survival vs Creative/OP

Untuk menjaga keseimbangan gameplay survival/RPG tanpa merusak progresi eksplorasi dan single-world, seluruh 153 command turunan EssentialsX dibagi menjadi 3 zona hak akses:

```
┌────────────────────────────────────────────────────────────────────────┐
│                   TAKASHA-RPG COMMAND ECOSYSTEM                        │
├────────────────────────────────┬───────────────────────────────────────┤
│  SURVIVAL DEFAULT (7 Commands) │  SURVIVAL LOOT (29 Executable Items)  │
│  - /help                       │  - Virtual Workstations (9)           │
│  - /spawn                      │  - Explorasi & Navigasi (7)           │
│  - /sethome (Max 1 home awal)  │  - Visual Lingkungan (2)              │
│  - /home                       │  - Pemulihan & Utilitas Vital (6)     │
│  - /delhome                    │  - Manajemen & Ekonomi (5)            │
│  - /homes                      ├───────────────────────────────────────┤
│  - /back & /return             │  CREATIVE / OP ONLY (117 Commands)    │
│    (Warmup 3s, death/teleport) │  - /fly, /god, /speed, /vanish, dll   │
└────────────────────────────────┴───────────────────────────────────────┘
```

### 1.1 Tujuh Perintah Survival Bawaan
1. `/help [page]` — Menampilkan daftar interaktif 7 perintah bawaan dan 29 perintah loot beserta status aktivasi dan deskripsi per baris.
2. `/spawn` — Teleportasi ke World Spawn dengan warmup 3 detik.
3. `/sethome [name]` — Membuat titik home. **Dibatasi maksimal 1 home** di survival (dapat ditingkatkan hingga 10 menggunakan `ancient_keystone`).
4. `/home [name]` — Teleportasi ke titik home dengan warmup 3 detik.
5. `/delhome <name>` — Menghapus titik home.
6. `/homes` — Menampilkan daftar semua titik home yang dimiliki player beserta koordinat dan limit aktif.
7. `/back` & `/return` — Teleportasi kembali ke posisi sesaat sebelum teleportasi terakhir, atau lokasi kematian terakhir jika belum pernah teleportasi.

### 1.2 Dua Puluh Sembilan Perintah Loot Dungeon (Executable Item)
Semua 29 perintah ini **terkunci secara default** pada pemain survival. Pemain harus menjelajahi dungeon, mengalahkan boss, atau membuka peti loot untuk menemukan item fisik, lalu mengonsumsinya (klik-kanan) untuk membuka akses perintah secara permanen.

---

## 2. Mekanisme Executable Item

### 2.1 Pola Konsumsi & Aktivasi
Setiap item loot mewarisi kelas `TakashaExecutableItem`:
- **Interaksi**: Klik-kanan (`use(...)`).
- **Pemeriksaan Status**: Jika perintah sudah terbuka di profil `TakashaRpgData`, tampilkan peringatan suara `NOTE_BLOCK_BASS` dan pesan kuning tanpa mengonsumsi item.
- **Konsumsi**: `itemStack.consume(1, player)`.
- **Feedback Audio-Visual**:
  - Sound: `SoundEvents.TOTEM_USE` (volume 1.0f, pitch 1.0f).
  - Partikel: `ParticleTypes.TOTEM_OF_UNDYING` meledak di sekitar tubuh pemain.
  - Broadcast/Notifikasi: Pesan emas tebal memberitahukan bahwa perintah kini telah aktif permanen di profil RPG pemain.
- **Persistensi**: Dicatat ke NBT/JSON `TakashaRpgData` melalui `data.unlockCommand(commandName)` dan `TakashaRpgManager.savePlayerData(...)`.

---

## 3. Sistem `/help` Dinamis

Perintah `/help` menampilkan informasi komprehensif 3 halaman:
- **Halaman 1**: 7 Perintah Bawaan Survival (orientasi, home, spawn, back).
- **Halaman 2**: 15 Perintah Loot Eksplorasi, Navigasi, Visual, dan Virtual Workstation.
- **Halaman 3**: 14 Perintah Loot Pemulihan, Ekonomi, Utilitas, dan Panduan Reset.

Setiap baris perintah loot menyertakan indikator status real-time:
- `§a✔ UNLOCKED` — Jika pemain telah mengonsumsi item dan memiliki hak akses.
- `§c✘ TERKUNCI (Item Loot: <Nama Item>)` — Jika pemain belum membukanya.
- Penjelasan fungsi perintah dalam bahasa Indonesia yang ringkas dan informatif.

---

## 4. Protokol Reset Progres Player (`/rpg reset`)

Untuk pengujian, gameplay musiman, atau penyesuaian build karakter, tersedia mekanisme reset terstruktur:

### 4.1 Sintaks Perintah
- `/rpg reset` — Meminta konfirmasi dengan peringatan bahaya warna merah.
- `/rpg reset confirm` — Mereset seluruh progres (`ResetScope.ALL`).
- `/rpg reset <skills|commands|homes|economy> confirm` — Mereset kategori tertentu:
  - `skills`: Mereset level skill (Combat, Mining, Foraging, Farming, Fishing) ke 1 dan XP ke 0.
  - `commands`: Mengunci kembali seluruh 29 perintah loot.
  - `homes`: Menghapus seluruh titik home dan mereset kapasitas home ke default (1).
  - `economy`: Mereset saldo koin RPG ke 0.
- `/rpg reset player <target> force` — Khusus Operator (OP/Gamemaster) untuk mereset progres pemain lain secara langsung.

### 4.2 Feedback Dramatis
Reset yang berhasil akan memicu suara `SoundEvents.WITHER_SPAWN` dan semburan partikel `ParticleTypes.SOUL` serta `ParticleTypes.LARGE_SMOKE`.

---

## 5. Implementasi Fabric 26.2 & Kompatibilitas API

| Fitur Spigot / Bukkit | Ekivalen Fabric 26.2 (Mojang Mappings) | Catatan Khusus |
|---|---|---|
| `p.hasPermission("...")` | `src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)` | 26.2 menggunakan `PermissionSet` dan `Permissions` |
| `world.getMinHeight()` | `level.getMinY()` | Menggantikan `getMinBuildHeight()` |
| `world.getMaxHeight()` | `level.getMaxY()` | Menggantikan `getMaxBuildHeight()` |
| `p.kill()` | `player.kill((ServerLevel) player.level())` | Memerlukan argumen `ServerLevel` di 26.2 |
| `ClientboundSetTimePacket` | Rekayasa `ServerClockManager.createFullSyncPacket()` | Map `ClockNetworkState` disesuaikan per pemain |
| Workstation Menu | Subclass `Menu` dengan `ContainerLevelAccess.NULL` dan `stillValid() { return true; }` | Mencegah menu menutup saat pemain bergerak |
| Head Profile NBT | `ResolvableProfile.createUnresolved(ownerName)` | Format komponen profile 26.2 |
