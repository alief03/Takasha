# Rencana Perbaikan REA: Status Dummy NPC, Sub-GUI Interaktif AuraSkills, dan Optimasi Interval DOT

> **Dokumen Perencanaan REA (Reverse Engineering & Audit)**  
> **Target Subproject:** `:takasha-rpg` (Minecraft 26.2, Fabric Loom 1.17.21, Java 25)  
> **Scope Guard:** Subproject `sakura-weapons` **STRICTLY ZERO-TOUCH**  
> **Status:** DISETUJUI & DALAM IMPLEMENTASI (Poin C.1 & C.2 Diterapkan, Poin C.3 Dihapus)

---

## 1. Latar Belakang & Analisis Permasalahan (Root Cause Analysis)

### 1.1 Status Informasi pada Dummy NPC Hilang / Tidak Muncul
Berdasarkan investigasi mendalam terhadap `DamageDummyEntity.java`:
1. **Peniadaan Nametag Native ArmorStand:** Pada saat inisialisasi, entity memanggil `setCustomName(null)` dan `setCustomNameVisible(false)`. Hal ini membuat entity 100% bergantung pada entity terpisah `Display.TextDisplay`.
2. **Bug Pembersihan Display (`purgeOrphanDisplays`):** Saat chunk di-reload atau pemain keluar-masuk world, `this.overheadDisplay` bernilai `null` hingga re-bind. Logika pembersihan mendeteksi tag `ownTag` dan langsung men-`discard()` TextDisplay miliknya sendiri karena disangka orphan.
3. **Konfigurasi Klien & Jarak Render:** Pada klien dengan render distance rendah atau mod optimasi (seperti Sodium/FerriteCore), Display Entity sering kali di-cull atau tidak di-render di atas batas jarak tertentu, sehingga Dummy tampak telanjang tanpa informasi apapun.
4. **Kekurangan Parameter Pertahanan:** Baris overhead saat ini hanya menampilkan HP, DPS, dan nama potion, tanpa menampilkan status ketahanan/armor (Armor Points, Armor Toughness, Persentase Reduksi Damage).
5. **Penanganan Klik Kanan (`interact`):** Klik kanan selain menggunakan tangan kosong atau ember susu diteruskan ke vanilla `ArmorStand.interact()`. Jika memegang senjata/panah/item pengujian, interaksi tidak memunculkan laporan tempur apapun di chat.
6. **Eksklusi AuraSkillsEngine:** `AuraSkillsEngine.ALLOW_DAMAGE` secara eksplisit mengecualikan dummy (`if (entity instanceof DamageDummyEntity) return true;`), menyebabkan status Bleed dan Stun dari AuraSkills tidak pernah aktif pada dummy.

---

### 1.2 GUI Skill Belum Memunculkan Sub-GUI Interaktif Sesuai Sumber (AuraSkills 2.4.0)
Berdasarkan dekompilasi dan audit konfigurasi kanonikal `referensi-rp/RPG/AuraSkills-2.4.0/menus/`:
1. **Hierarki Menu Kanonikal AuraSkills:**
   - Di dalam `level_progression.yml` (GUI saat membuka skill seperti `/farming`, `/mining`, dsb):
     - **Slot 2 (`sources`):** Item botol pengalaman (`experience_bottle`). Kanonikalnya membuka `sources.yml`.
     - **Slot 3 (`abilities`):** Item dye/bintang nether (`abilities`). Kanonikalnya membuka `abilities.yml`.
2. **Kondisi Saat Ini di `AuraLevelProgressionMenu.java`:**
   - Slot 2 dan Slot 3 hanya berisi item display statis dengan lore teks ringkasan.
   - Ketika diklik, listener hanya memutar suara `SoundEvents.UI_BUTTON_CLICK` tanpa membuka Container GUI baru.
3. **Spesifikasi Kanonikal `sources.yml`:**
   - Ukuran Chest: 54 slot (6 baris).
   - Menampilkan item nyata di Minecraft yang menghasilkan XP untuk skill terkait (misal: Gandum, Wortel, Kentang, Melon, Labu untuk Farming; Bijih Besi, Berlian, Obsidian untuk Mining).
   - Setiap item menampilkan lore XP kanonikal: `§7XP Dihasilkan: §a+{xp} XP` dan tipe aksinya.
   - Terdapat tombol navigasi kanonikal: Baris ke-6 (Slot 45 Back/Kembali, Slot 48 Prev Page, Slot 49 Sorter Hopper, Slot 51 Next Page).
4. **Spesifikasi Kanonikal `abilities.yml`:**
   - Menampilkan 5 Passive Abilities + 1 Mana Ability dari skill terkait.
   - Menampilkan status terbuka/terkunci berdasarkan level pemain, deskripsi formula, dan biaya mana.

---

### 1.3 Optimasi Interval Kerusakan Tipe DOT (Device Performance)
Berdasarkan evaluasi performa klien dan server:
1. **Permasalahan Ticking Tiap Detik (20 Ticks):**
   - Efek DOT seperti Bleed dan Burn yang berdetak setiap 20 tick (1 detik) memicu kalkulasi damage, pengiriman paket partikel, sound effect, dan spawning entity TextDisplay melayang (`FloatingIndicator`) terus-menerus.
   - Pada pertempuran ramai (misal Dungeon Arena dengan puluhan monster), ratusan partikel dan damage text per detik menyebabkan FPS drop signifikan pada perangkat spesifikasi menengah ke bawah.
2. **Solusi Performa Tanpa Mengurangi Efektivitas Tempur:**
   - Mengubah interval dasar ticking dari 20 ticks (1.0s) menjadi **40 ticks (2.0s)** atau **60 ticks (3.0s)** secara adaptif.
   - Mengalikan damage per tick secara proporsional (`damage = baseDps * (interval / 20.0f)`) sehingga **Total DPS yang dihasilkan 100% tetap identik dan adil**.
   - Menyesuaikan frekuensi pembaruan overhead display dan partikel efek agar tidak membebani render thread klien.

---

## 2. Rencana Implementasi Bertahap (Execution Plan)

### Fase 1: Perombakan Sistem Status & Display Dummy NPC (`DamageDummyEntity.java`)
1. **Dual-Layer Fail-Safe Nametag:**
   - *Layer 1 (Native Vanilla Nametag):* Aktifkan kembali nametag native ArmorStand (`setCustomName` & `setCustomNameVisible(true)`) dengan format ringkas:
     `§e§lTARGET DUMMY §7[§c❤ %.0f/%.0f§7]`
     Jika TextDisplay gagal render karena jarak atau mod grafis, nametag native ini tetap terlihat jelas.
   - *Layer 2 (TextDisplay Billboard Overhead):*
     - Perbaiki bug pembersihan orphan pada `purgeOrphanDisplays()` agar tidak men-discard display miliknya sendiri jika UUID cocok.
     - Simpan dan muat `overheadDisplayUuid` dengan benar via NBT.
2. **Kaya Informasi Pertahanan & Status Tempur:**
   - Format 3 Baris Overhead Display:
     - **Baris 1 (Identitas & Ketahanan):**
       `§e§lTARGET DUMMY §7| §c❤ %.1f/%.1f §7(Shield: §9%.1f§7) | §6🛡 %.0f Armor §7(%.1f%% Redux)`
     - **Baris 2 (Metrik Serangan):**
       `§7DPS: §e%.1f §7| Last: §f%.1f §7| Peak: §c%.1f §7| Total: §a%.1f §7(%d hits)`
     - **Baris 3 (Status Efek Aktif):**
       Menampilkan seluruh status aktif (Bleed, Freeze, Burn, Shock, Silence, Disarm, Guard, Fracture, Snowblind, serta efek potion vanilla).
3. **Interaksi Lengkap dengan Item Apa Saja:**
   - **Tangan Kosong + Shift:** Reset statistik tempur.
   - **Item Ember Susu:** Membersihkan semua efek status.
   - **Item Zirah (Helm/Chest/Legs/Boots):** Memasang/melepas zirah pada dummy.
   - **Item Apapun (Senjata, Panah, Stick, dll) atau Tangan Kosong:** Mencetak **Laporan Status Lengkap** ke chat pemain dengan rincian lengkap (Health, Max Health, Defense, Armor Reduction, Total Damage, DPS, Peak Damage, dan Efek Aktif).
4. **Integrasi AuraSkills Engine:**
   - Perbarui `AuraSkillsEngine.ALLOW_DAMAGE` agar status Bleed dan Stun dari AuraSkills dapat diterapkan dan diuji pada `DamageDummyEntity`.

---

### Fase 2: Pembangunan Sub-GUI Interaktif Kanonikal (`AuraSourcesMenu` & `AuraAbilitiesMenu`)
1. **Implementasi `AuraSourcesMenu.java` (54-Slot Chest Container):**
   - Membuat menu katalog sumber XP untuk masing-masing 11 skill AuraSkills:
     - **Farming:** Gandum, Kentang, Wortel, Bit, Nether Wart, Melon, Labu, Tebu, Bambu, Kaktus, Biji Kakao, Sweet Berry, Glow Berry, Pitcher Plant, Torchflower.
     - **Foraging:** Semua jenis kayu/batang pohon (Oak, Spruce, Birch, Jungle, Acacia, Dark Oak, Mangrove, Cherry, Crimson, Warped).
     - **Mining:** Batu, Deepslate, Netherrack, Batu Bara, Besi, Tembaga, Emas, Redstone, Lapis, Berlian, Zamrud, Ancient Debris, Obsidian.
     - **Fishing:** Cod, Salmon, Ikan Tropis, Pufferfish, Treasure, Junk.
     - **Excavation:** Tanah, Rumput, Pasir, Pasir Merah, Kerikil, Tanah Liat, Pasir Jiwa, Tanah Jiwa, Lumpur.
     - **Archery:** Tembakan Busur, Kill Busur, Tembakan Crossbow, Kill Crossbow, Bonus Jarak.
     - **Defense:** Kerusakan Diterima per Heart, Serangan Melee Monster, Serangan Proyektil.
     - **Fighting:** Zombie, Skeleton, Spider, Creeper, Enderman, Piglin, Wither Skeleton, Blaze, Witch, Warden, Dragon, Wither.
     - **Agility:** Sprinting, Jumping, Safe Fall, Swimming.
     - **Alchemy:** Awkward Potion, Potion Tier I/II, Splash Potion, Lingering Potion.
     - **Enchanting:** Meja Sihir per level, Penggabungan Anvil, Disenchant Grindstone.
   - Format Item dalam Grid (Baris 1 s.d 4):
     - Nama item sesuai nama blok/mob/aksi kanonikal.
     - Lore:
       `§8Sumber XP AuraSkills`
       `§7XP Diperoleh: §a+%.1f XP`
       `§7Kategori: §e{Blok / Mob / Aksi Tempur}`
   - Tombol Navigasi Baris 6:
     - Slot 45: Tombol Kembali (Membuka kembali `AuraLevelProgressionMenu` secara in-place).
     - Slot 48: Halaman Sebelumnya.
     - Slot 49: Mode Pengurutan (Berdasarkan XP tertinggi atau Abjad).
     - Slot 51: Halaman Berikutnya.
2. **Implementasi `AuraAbilitiesMenu.java` (54-Slot Chest Container):**
   - Menampilkan 5 Ability Pasif + 1 Ability Mana untuk skill yang dipilih.
   - Menampilkan status terbuka (Level terpenuhi) atau terkunci (Level belum cukup).
   - Menampilkan lore kanonikal mengenai persentase efek, bonus stat, dan biaya mana.
   - Slot 45: Tombol Kembali.
3. **Penyambungan Interaksi pada `AuraLevelProgressionMenu.java`:**
   - Klik pada Slot 2 (`sources`): Membuka `AuraSourcesMenu.open(player, skill, 0)`.
   - Klik pada Slot 3 (`abilities`): Membuka `AuraAbilitiesMenu.open(player, skill)`.
   - Klik tombol Kembali pada sub-menu: Membuka kembali `AuraLevelProgressionMenu.open(player, skill)` dengan **Zero Cursor Displacement Protocol** (kursor mouse tetap stabil di posisi tengah).

---

### Fase 3: Optimasi Interval DOT & Efisiensi Perangkat Rendah (Performance Scaling)
1. **Desain Interval Ticking Adaptif:**
   - Mengubah baseline interval DOT dari 20 ticks (1s) menjadi **40 ticks (2s)**.
   - Terapkan fungsi kalkulasi adaptif berdasarkan kepadatan entitas di sekitar dummy/pemain:
     - 0 - 10 entitas: **40 ticks (2.0 detik)** -> Damage x2.0
     - 11 - 25 entitas: **60 ticks (3.0 detik)** -> Damage x3.0
     - 26 - 40 entitas: **80 ticks (4.0 detik)** -> Damage x4.0
     - > 40 entitas: **100 ticks (5.0 detik)** -> Damage x5.0
2. **Preservasi DPS Matematik (100% Invariant):**
   - `float scale = interval / 20.0f;`
   - `float dotDamage = baseDamagePerSecond * scale;`
   - Contoh: Jika Bleed menghasilkan 3.0 damage/detik, maka pada interval 40 tick (2 detik), Bleed akan memicu 6.0 damage tiap 2 detik. Total DPS selama 10 detik tetap tepat 30 damage.
3. **Throttling Partikel & Overhead Updates:**
   - Interval partikel disesuaikan agar tidak membanjiri buffer render GPU perangkat klien.
   - Teks melayang (`FloatingIndicator`) memunculkan angka damage akumulasi, misal `§c§l-6.0 🩸 BLEED`, sehingga mengurangi jumlah entity TextDisplay sementara di dunia.
   - Pembaruan overhead display diperbarui tiap `interval / 4` (10 s.d 20 tick), bukan tiap 4 tick.

---

## 3. Protokol Verifikasi & Uji Kompilasi

1. **Verifikasi Kompilasi Gradle:**
   - Menjalankan `.\gradlew.bat :takasha-rpg:compileJava` dari root build folder untuk memastikan 0 syntax error dan 0 missing symbols.
2. **Verifikasi Subproject Isolation:**
   - Memastikan tidak ada berkas dalam `sakura-weapons/` yang tersentuh atau dimodifikasi.
3. **Verifikasi Fungsionalitas Tempur & GUI:**
   - Memastikan Dummy menampilkan status zirah, persentase reduksi, dan nametag tetap terbaca.
   - Memastikan klik kanan dengan senjata memunculkan laporan status di chat.
   - Memastikan interaksi slot 2 membuka `AuraSourcesMenu` dengan item asli Minecraft dan XP yang akurat.
   - Memastikan interaksi slot 3 membuka `AuraAbilitiesMenu`.
   - Memastikan klik tombol kembali tidak menutup kursor atau merusak navigasi pemain.
   - Memastikan damage DOT berdetak tiap 2-3 detik dengan damage yang terskala proporsional.

---

## 4. Persetujuan Pengguna

Rencana ini siap untuk dieksekusi setelah mendapatkan persetujuan pengguna. Sesuai instruksi: **"jangan eksekusi, buat planning nya terlebih dahulu."**
