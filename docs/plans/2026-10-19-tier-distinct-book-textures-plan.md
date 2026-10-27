# [Rencana Implementasi] Tekstur Khas Per Tier & Pemisahan Mystery Book untuk takasha-rpg (Minecraft 26.2)

> **Versi Dokumen:** `v1.0.0`  
> **Target Versi Mod:** `v1.4.0+26.2` (Peningkatan Versi Semantik: Minor Feature Release)  
> **Target PRD:** `docs/PRD-takasha-rpg.md` -> `v3.4.0`  
> **Batasan Ketat:** `sakura-weapons` (`v2.5.0`) berstatus **TERKUNCI (LOCKED)** dan tidak boleh diubah sedikit pun.

---

## 1. Analisis Akar Masalah & Tujuan

### 1.1 Akar Masalah Tampilan Tekstur Identik
1. **Keluarga Aset yang Keliru:** Tekstur `custom_book_<tier>.png` yang saat ini terpasang ternyata diambil dari `RPGBundle-Vol21`, yang sebenarnya berisi jimat mistis, liontin, atau kristal/orb—**bukan buku, grimoire, ataupun gulungan perkamen asli**.
2. **Mystery Book dan Revealed Book Berbagi Item yang Sama:** Sebelumnya, Buku Misteri (yang belum terbuka) dan Buku Enchant Khusus (yang sudah terbuka dan memiliki data sihir) sama-sama memakai item ID `ModRpgItems.CUSTOM_BOOK_<TIER>` dan berkas model `items/custom_book_<tier>.json`. Akibatnya:
   - Buku yang dibeli dari Enchanter dan buku hasil buka klik-kanan memiliki tekstur yang persis sama.
   - Seluruh 9 tier kelangkaan tampak serupa dan tidak memiliki identitas buku RPG yang otentik.

### 1.2 Solusi & Tujuan Desain
1. **Membuat Item Khusus Buku Misteri (9 Tier):**
   - Mendaftarkan item `ModRpgItems.MYSTERY_BOOK_SIMPLE` sampai `MYSTERY_BOOK_MASTERY` (`takasha_rpg:mystery_book_<tier>`).
   - Berupa **Grimoire Kuno Tertutup / Terkunci** dengan ornamen sesuai warna kelangkaan.
   - Klik kanan di tangan mengonsumsi 1 buku misteri, memunculkan partikel sihir, memutar suara perayaan, dan memberikan buku sihir terbuka (`custom_book_<tier>`).
2. **Mempertahankan & Mempercantik Buku Enchant Terbuka (9 Tier):**
   - `takasha_rpg:custom_book_<tier>` dikhususkan untuk **Buku Sihir Terbuka / Gulungan Elemen Bercahaya (Luminous Scroll)** yang membawa data sihir (`EnchantId`, `EnchantLevel`, `SuccessRate`, `DestroyRate`).
   - Siap diaplikasikan ke senjata/armor lewat drag-and-drop atau paron (anvil).
3. **Penyelarasan 18 Tekstur HD 32x32:**
   - Diambil langsung dari folder referensi: `D:\Mod Minecraft\weapon set\sakura\referensi-rp\RPG\Item-custom`.

---

## 2. Spesifikasi Pemetaan Tekstur (Resolusi 32x32)

Semua berkas di bawah telah divalidasi dan dipastikan tersedia di repositori referensi:

### 2.1 Buku Misteri (Mystery Books — Grimoire Kuno Tertutup & Tersegel)

| Tier Kelangkaan | Nama Item | Kode Warna | Berkas Asal di `referensi-rp\RPG\Item-custom` | Berkas Tujuan di `takasha-rpg` | Deskripsi Visual |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Simple** | Simple Mystery Book | `§f` Putih | `Fuben/.../fb_item/fb_71.png` | `mystery_book_simple.png` | Buku sampul kulit perunggu cokelat dengan medali bintang emas |
| **Unique** | Unique Mystery Book | `§a` Hijau Lime | `Fuben/.../fb_item/fb_73.png` | `mystery_book_unique.png` | Buku sampul kulit hijau zamrud dengan ornamen sudut emas |
| **Elite** | Elite Mystery Book | `§b` Biru Muda (Aqua) | `Fuben/.../fb_item/fb_190.png` | `mystery_book_elite.png` | Grimoire biru kobalt arkana dengan lambang perak & pita pembatas |
| **Ultimate** | Ultimate Mystery Book | `§e` Kuning Emas | `Fuben/.../fb_item/fb_192.png` | `mystery_book_ultimate.png` | Grimoire emas berkilau mewah dengan segel bintang surya |
| **Legendary**| Legendary Mystery Book | `§6` Oranye | `Fuben/.../fb_item/fb_74.png` | `mystery_book_legendary.png` | Grimoire tembaga/amber berpola api dengan lambang bintang |
| **Fabled** | Fabled Mystery Book | `§c` Merah Darah | `Fuben/.../fb_item/fb_138.png` | `mystery_book_fabled.png` | Grimoire bersayap kuno merah tua dengan inti batu delima mistis |
| **Heroic** | Heroic Mystery Book | `§d` Pink Magenta | `Fuben/.../fb_item/fb_136.png` | `mystery_book_heroic.png` | Grimoire bersayap ungu royal langit dengan kristal melayang |
| **Soul** | Soul Mystery Book | `§3` Sian Gelap | `Fuben/.../fb_item/fb_172.png` | `mystery_book_soul.png` | Grimoire kulit obsidian hitam dengan gembok perak & pita ungu |
| **Mastery** | Mastery Mystery Book | `§4§l` Merah Pekat | `Fuben/.../fb_item/fb_137.png` | `mystery_book_mastery.png` | Grimoire ilahi tertinggi bersayap zamrud dengan batu permata bersinar |

### 2.2 Buku Enchant Terbuka (Revealed Custom Books — Gulungan Sihir & Halaman Luminous)

| Tier Kelangkaan | Nama Item | Kode Warna | Berkas Asal di `referensi-rp\RPG\Item-custom` | Berkas Tujuan di `takasha-rpg` | Deskripsi Visual |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Simple** | Simple Custom Book | `§f` Putih | `RPGBundle-Vol1/.../textures/paper1.png` | `custom_book_simple.png` | Gulungan perkamen kuno bertuliskan rune magis murni |
| **Unique** | Unique Custom Book | `§a` Hijau Lime | `Fuben/.../fb_item/fb_191.png` | `custom_book_unique.png` | Kitab sihir hijau zamrud terbuka dengan medali rune perak |
| **Elite** | Elite Custom Book | `§b` Biru Muda (Aqua) | `Fuben/.../schematic/fb_schematic_61.png` | `custom_book_elite.png` | Gulungan sihir terbuka biru sian bersinar terang (unsur es/arkana) |
| **Ultimate** | Ultimate Custom Book | `§e` Kuning Emas | `Fuben/.../schematic/fb_schematic_58.png` | `custom_book_ultimate.png` | Gulungan sihir emas terbuka bermotif lambang surya menyala |
| **Legendary**| Legendary Custom Book | `§6` Oranye | `Fuben/.../schematic/fb_schematic_70.png` | `custom_book_legendary.png` | Gulungan sihir api membara oranye dengan percikan energi panas |
| **Fabled** | Fabled Custom Book | `§c` Merah Darah | `Fuben/.../schematic/fb_schematic_55.png` | `custom_book_fabled.png` | Gulungan sihir darah kuno merah pekat dengan segel rune aktif |
| **Heroic** | Heroic Custom Book | `§d` Pink Magenta | `Fuben/.../schematic/fb_schematic_64.png` | `custom_book_heroic.png` | Gulungan sihir ungu/magenta kerajaan berkilau anggun |
| **Soul** | Soul Custom Book | `§3` Sian Gelap | `Fuben/.../schematic/fb_schematic_59.png` | `custom_book_soul.png` | Gulungan sihir jiwa bayangan ungu gelap memancarkan aura misterius |
| **Mastery** | Mastery Custom Book | `§4§l` Merah Pekat | `Fuben/.../schematic/fb_schematic_68.png` | `custom_book_mastery.png` | Gulungan sihir kosmik astral bertuliskan starlight multi-warna |

---

## 3. Rencana Perubahan Kode & Arsitektur

### 3.1 Kelas Baru: `TakashaMysteryBookItem.java`
- Menjadi turunan dari `Item`.
- Menyimpan field kelangkaan: `EnchantTier tier`.
- Logika Klik Kanan (`use`):
  - Memastikan eksekusi sisi server: memanggil `TakashaCustomBookItem.createRandomRevealedBook(tier)`.
  - Mengurangi item mystery book di tangan sebanyak 1 (`held.shrink(1)`).
  - Memberikan buku sihir terbuka ke inventori pemain (atau menjatuhkannya jika inventori penuh).
  - Memainkan suara `SoundEvents.PLAYER_LEVELUP` dan memunculkan partikel `ENCHANT` serta `TOTEM_OF_UNDYING`.
  - Mengirim pesan konfirmasi perayaan ke bilah chat: `"§a§l✦ BUKU TERBUKA! ✦\n..."`.
- Tooltip:
  - Menampilkan nama tier, deskripsi cara buka (`"§eKlik kanan buku di tangan untuk mengungkap sihir!"`), dan catatan panduan.

### 3.2 Pembaruan `ModRpgItems.java`
- Mendaftarkan 9 item baru:
  - `MYSTERY_BOOK_SIMPLE` s/d `MYSTERY_BOOK_MASTERY` (`stacksTo(16)`).
- Menambahkan metode pembantu: `getMysteryBookForTier(EnchantTier tier)`.

### 3.3 Penyesuaian `TakashaCustomBookItem.java`
- Memfokuskan kelas ini pada buku sihir terbuka (revealed enchant book).
- Tetap mempertahankan fungsi fallback klik kanan unrevealed jika ada item versi lama tanpa NBT (menjamin backward compatibility).
- Metode `getBookItemForTier(tier)` tetap mengembalikan `CUSTOM_BOOK_<TIER>`.

### 3.4 Penyesuaian Menu GUI
- **`EnchanterMenu.java`:**
  - Slot 13 (pratinjau konfirmasi): Menampilkan item Buku Misteri baru (`ModRpgItems.getMysteryBookForTier(tier)`).
  - Eksekusi pembelian (`processPurchase`): Memberikan item Buku Misteri asli ke pemain, bukan lagi buku enchant kosong.
- **`TinkererMenu.java` & `AlchemistMenu.java`:**
  - Mendukung daur ulang Buku Misteri maupun Buku Sihir Terbuka menjadi Magic Dust dan EXP.

### 3.5 Pembaruan Tab Kreatif & Perintah
- **`ModRpgItemGroups.java`:**
  - Memasukkan Buku Misteri ke dalam tab RPG Combat/Enchant sebelum Buku Sihir Terbuka.
- **`TakashaRpgCommands.java`:**
  - Menambahkan argumen `/rpg give mystery_<tier>` (misal: `/rpg give mystery_mastery`).
  - Mempertahankan argumen `/rpg give book_<tier>` untuk buku sihir terbuka.

### 3.6 Konfigurasi Model JSON & Bahasa (Localization)
- **Model JSON:**
  - Membuat 9 berkas definisi item: `assets/takasha_rpg/items/mystery_book_<tier>.json`.
  - Membuat 9 berkas model: `assets/takasha_rpg/models/item/enchant/mystery_book_<tier>.json`.
  - Memperbarui 9 berkas model: `assets/takasha_rpg/models/item/enchant/custom_book_<tier>.json`.
- **Bahasa (`en_us.json` & `id_id.json`):**
  - Mendaftarkan terjemahan bahasa Inggris dan Indonesia untuk `item.takasha_rpg.mystery_book_<tier>`.

---

## 4. Penyelarasan Versi Semantik & PRD

| Modul | Versi Saat Ini | Target Versi | Jenis Perubahan | Alasan |
| :--- | :--- | :--- | :--- | :--- |
| **Mod `takasha-rpg`** | `v1.3.1+26.2` | **`v1.4.0+26.2`** | Minor (Fitur Baru) | Penambahan 9 item resmi Buku Misteri, pemisahan item Enchanter, dan pembaruan 18 aset visual khas per tier. |
| **Mod `sakura-weapons`** | `v2.5.0` | `v2.5.0` | **Terkunci** | Tidak ada perubahan sama sekali. |
| **PRD (`docs/PRD-takasha-rpg.md`)** | `v3.3.1` | **`v3.4.0`** | Dokumentasi | Mendokumentasikan spesifikasi pemisahan Buku Misteri dan visual asset buku enchant baru. |

---

## 5. Tahapan Eksekusi & Daftar Uji Verifikasi

1. **Tahap 1 - Migrasi Aset:** Salin 18 berkas tekstur dari folder referensi ke folder tujuan `assets/takasha_rpg/textures/item/enchant/`.
2. **Tahap 2 - Model & Bahasa:** Buat 18 berkas JSON model item dan lengkapi `en_us.json` serta `id_id.json`.
3. **Tahap 3 - Registrasi & Kelas Java:** Buat `TakashaMysteryBookItem.java`, daftarkan di `ModRpgItems.java`, dan sesuaikan `TakashaCustomBookItem.java`.
4. **Tahap 4 - Integrasi GUI & Perintah:** Hubungkan item baru ke `EnchanterMenu.java`, `ModRpgItemGroups.java`, dan `TakashaRpgCommands.java`.
5. **Tahap 5 - Pembaruan Dokumen:** Naikkan versi mod di `gradle.properties` / `fabric.mod.json` ke `1.4.0+26.2` dan perbarui `PRD-takasha-rpg.md` ke `v3.4.0`.
6. **Tahap 6 - Verifikasi Build:**
   - Jalankan kompilasi `./gradlew :takasha-rpg:build` untuk memastikan 0 peringatan/error.
   - Verifikasi model item tidak memiliki tekstur ungu-hitam yang hilang (missing texture).
   - Tes alur pembelian Enchanter, buka buku, dan drag-and-drop ke senjata.
