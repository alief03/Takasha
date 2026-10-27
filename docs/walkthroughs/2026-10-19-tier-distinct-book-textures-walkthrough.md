# Walkthrough: Implementasi 18 Tekstur Buku HD 32x32 Per-Tier & Pemisahan Item Mystery vs Revealed Book

> **Status:** Selesai & Terverifikasi (Build Sukses)  
> **Mod:** `takasha-rpg` (`v1.4.0+26.2`)  
> **PRD:** `v3.4.0` (Updated)  
> **Mod Inti:** `sakura-weapons` (`v2.5.0` Locked - Zero Touch)  

---

## 1. Ringkasan Implementasi

Sesuai permintaan dan persetujuan dokumen rencana, seluruh tekstur buku sihir per-tier yang sebelumnya berwujud sama/seragam kini telah digantikan dengan **18 tekstur HD 32x32 otentik** dari `referensi-rp\RPG\Item-custom`.

Selain pembaruan visual, arsitektur item juga dipisahkan (*decoupled*) menjadi 2 kelas item mandiri:
1. **`TakashaMysteryBookItem` (`mystery_book_<tier>`):** Kitab terselubung misteri yang belum teridentifikasi. Digunakan untuk gacha di menu Enchanter (`/ce`), drop dungeon/mob, dan dapat diungkap segelnya dengan **Klik Kanan**.
2. **`TakashaCustomBookItem` (`custom_book_<tier>`):** Kitab/gulungan sihir bercahaya yang sudah terungkap, memuat NBT `EnchantId`, `EnchantLevel`, `SuccessRate`, dan `DestroyRate` untuk diterapkan via drag-and-drop ke perlengkapan.

---

## 2. Pemetaan 18 Tekstur HD 32x32 Otentik

| No | Tier Kelangkaan | Mystery Book Texture (Terselubung) | Custom Book Texture (Terungkap/Bercahaya) |
|---|---|---|---|
| 1 | **Simple** | `fb_71.png` (Kitab cokelat berpita pengikat) | `paper1.png` (Perkamen gulungan antik sederhana) |
| 2 | **Unique** | `fb_73.png` (Kitab oranye permata amber) | `fb_191.png` (Kitab terbuka bercahaya amber) |
| 3 | **Elite** | `fb_190.png` (Kitab safir runik gelap mistis) | `fb_schematic_61.png` (Skematik gulungan berbingkai safir) |
| 4 | **Ultimate** | `fb_192.png` (Kitab zamrud bersudut emas) | `fb_schematic_58.png` (Skematik gulungan berbingkai zamrud) |
| 5 | **Legendary** | `fb_74.png` (Kitab rubi emas mewah kekaisaran) | `fb_schematic_70.png` (Skematik gulungan emas rubi) |
| 6 | **Fabled** | `fb_138.png` (Kitab ungu magis fabled) | `fb_schematic_55.png` (Skematik gulungan ungu mistis) |
| 7 | **Heroic** | `fb_136.png` (Kitab perak bercahaya sakral) | `fb_schematic_64.png` (Skematik gulungan perak bercahaya) |
| 8 | **Soul** | `fb_172.png` (Kitab sculk/jiwa kegelapan) | `fb_schematic_59.png` (Skematik gulungan jiwa sculk berpendar) |
| 9 | **Mastery** | `fb_137.png` (Kitab emas kosmik tertinggi) | `fb_schematic_68.png` (Skematik gulungan emas agung penguasa) |

Semua tekstur disimpan di folder:  
`takasha-rpg/src/main/resources/assets/takasha_rpg/textures/item/enchant/`

---

## 3. Komponen yang Dibuat & Dimodifikasi

1. **Kelas Item Baru: [`TakashaMysteryBookItem.java`](file:///d:/Mod%20Minecraft/weapon%20set/sakura/takasha-rpg/src/main/java/net/takasha/rpg/item/TakashaMysteryBookItem.java)**
   - Menangani fungsi use/klik-kanan pemain di tangan utama maupun kedua.
   - Mengundi 1 custom enchantment acak dari `TakashaEnchantRegistry` berdasarkan tier buku.
   - Mengonsumsi 1 buku misteri dan menggantikannya dengan `TakashaCustomBookItem` terungkap (dengan success rate 10-100% dan destroy rate 0-50%).
   - Memainkan audio `PLAYER_LEVELUP` dan partikel `ENCHANT` + `TOTEM_OF_UNDYING`.

2. **Registry Item: [`ModRpgItems.java`](file:///d:/Mod%20Minecraft/weapon%20set/sakura/takasha-rpg/src/main/java/net/takasha/rpg/registry/ModRpgItems.java)**
   - Mendaftarkan 9 item `MYSTERY_BOOK_SIMPLE` s.d. `MYSTERY_BOOK_MASTERY`.
   - Menambahkan helper method `getMysteryBookForTier(EnchantTier tier)`.

3. **Item Definition & Models (Minecraft 26.2 Format):**
   - 9 Berkas di `assets/takasha_rpg/items/mystery_book_<tier>.json`.
   - 9 Berkas model di `assets/takasha_rpg/models/item/enchant/mystery_book_<tier>.json`.

4. **Lokalisasi (`en_us.json` & `id_id.json`):**
   - Mendefinisikan nama dan lore untuk ke-9 Mystery Book dalam Bahasa Inggris dan Bahasa Indonesia.

5. **Menu & Integrasi Gameplay:**
   - **[`EnchanterMenu.java`](file:///d:/Mod%20Minecraft/weapon%20set/sakura/takasha-rpg/src/main/java/net/takasha/rpg/menu/EnchanterMenu.java):** Slot 13 confirmation preview dan serah-terima pembelian gacha kini menerbitkan item `ModRpgItems.getMysteryBookForTier(tier)`.
   - **[`TinkererMenu.java`](file:///d:/Mod%20Minecraft/weapon%20set/sakura/takasha-rpg/src/main/java/net/takasha/rpg/menu/TinkererMenu.java):** Mendukung daur ulang baik `TakashaCustomBookItem` maupun `TakashaMysteryBookItem` menjadi Magic Dust atau Secret Dust.
   - **[`ModRpgItemGroups.java`](file:///d:/Mod%20Minecraft/weapon%20set/sakura/takasha-rpg/src/main/java/net/takasha/rpg/registry/ModRpgItemGroups.java):** Menampilkan ke-9 Mystery Book di Creative Tab RPG Utility berdampingan dengan 9 Custom Enchanted Books.
   - **[`TakashaRpgCommands.java`](file:///d:/Mod%20Minecraft/weapon%20set/sakura/takasha-rpg/src/main/java/net/takasha/rpg/command/TakashaRpgCommands.java):** Menambahkan auto-completion dan resolusi `mystery_simple` .. `mystery_mastery` pada perintah `/ae give <player> <item>`, serta memastikan `/ae giverandombook` memberikan item `TakashaMysteryBookItem` otentik.

6. **Versioning & PRD:**
   - [`takasha-rpg/build.gradle`](file:///d:/Mod%20Minecraft/weapon%20set/sakura/takasha-rpg/build.gradle): Ditingkatkan ke `1.4.0` (`version = "1.4.0+26.2"`).
   - [`PRD.md`](file:///d:/Mod%20Minecraft/weapon%20set/sakura/PRD.md): Ditingkatkan ke `v3.4.0` lengkap dengan Riwayat Pembaruan Section 14.21.

---

## 4. Hasil Verifikasi & Build

- **Perintah Build:** `.\gradlew.bat :takasha-rpg:build`
- **Status:** `BUILD SUCCESSFUL in 17s`
- **Output File:** [`Hasil mod/Takasha-RPG-1.4.0+26.2.jar`](file:///d:/Mod%20Minecraft/weapon%20set/sakura/Hasil%20mod/Takasha-RPG-1.4.0+26.2.jar) (666.204 bytes)
- **Verifikasi Konten JAR:**
  - 18 Texture PNGs terverifikasi ada di dalam JAR.
  - 18 Item & Model JSONs terverifikasi.
  - Entri lokalisasi Bahasa Indonesia & Bahasa Inggris terverifikasi.

---

## 5. Panduan Pengujian di Dalam Game (In-Game Testing)

Pemain dapat memverifikasi pembaruan ini secara instan di dalam dunia game:

1. **Mengambil Buku Misteri via Menu Enchanter:**
   - Jalankan `/ce` atau `/enchanter`.
   - Pilih salah satu tier (misalnya Simple, Elite, atau Legendary).
   - Klik konfirmasi di slot kaca hijau.
   - Perhatikan bahwa icon buku misteri kini menggunakan tekstur kitab tertutup HD 32x32 yang khas per tier.

2. **Membuka Segel Buku Misteri:**
   - Pegang buku misteri di tangan utama.
   - Klik Kanan pada udara atau blok.
   - Efek suara denting level-up dan semburan partikel sihir akan muncul.
   - Buku misteri akan berubah menjadi Custom Enchanted Book terungkap dengan tekstur gulungan/kitab terbuka bercahaya sesuai tier-nya.

3. **Mengambil via Perintah Admin:**
   - Memberikan buku misteri spesifik:
     ```
     /ae give @p mystery_simple 1
     /ae give @p mystery_elite 1
     /ae give @p mystery_mastery 1
     ```
   - Memberikan buku sihir yang sudah terungkap:
     ```
     /ae give @p book_simple 1
     /ae give @p book_mastery 1
     ```
   - Memberikan buku misteri acak via tier:
     ```
     /ae giverandombook @p legendary 1
     ```

4. **Daur Ulang di Tinkerer:**
   - Buka `/tinker` atau `/tinkerer`.
   - Taruh buku misteri atau buku sihir terungkap di slot kiri.
   - Tinkerer akan otomatis menawarkan Mystery Dust, Magic Dust, atau Secret Dust di slot kanan.
