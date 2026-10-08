# Planning Perbaikan: Anvil Rename Model, Integrasi Pink Legacy & Format Gradasi Warna (SemVer 1.4.0)

> **Versi Rilis Target:** Takasha v1.4.0 (Semantic Versioning: Minor Feature & Major Visual Fix)  
> **Minecraft Versi:** Java 26.2 (Fabric Loader >= 0.19.3)  
> **Status:** Siap Dieksekusi

---

## 1. Analisis Masalah Berdasarkan Screenshot Pengguna

### A. Masalah Gambar 1: Item di Anvil Tidak Berubah Tekstur Saat Di-Rename
* **Gejala:** Pemain meletakkan *Netherite Sword* di Anvil, mengetikkan `Sakura Katana`, namun slot hasil (*result slot*) tetap berupa *Netherite Sword* biasa tanpa tekstur 3D katana.
* **Akar Masalah:**
  1. Modpack pemain tidak memiliki mod CIT (seperti `citresewn`). Modpack hanya memuat `iris`, `sodium`, dll.
  2. Vanilla Minecraft 26.2 mengandalkan sistem Item Models bawaan (`assets/minecraft/items/*.json` dengan selector `minecraft:custom_name`).
  3. Berkas `assets/minecraft/items/netherite_sword.json` dan model 3D `assets/sakura/` sebelumnya **hanya berada di Resource Pack eksternal**, bukan di dalam file JAR mod.
  4. Sementara itu, file resource pack di profil game (`sakura-resourcepack.zip.disabled`) dalam kondisi **nonaktif/gagal muat** karena kesalahan sintaks `supported_formats: [15, 88]` pada `pack.mcmeta` Minecraft 26.2 (`JsonParseException: missing mandatory fields min_format and max_format`).
  5. Akibatnya, Minecraft tidak memiliki definisi model untuk nama `Sakura Katana`, sehingga tekstur tidak berubah sama sekali!

---

### B. Masalah Gambar 2: Set Pink Legacy Belum Muncul Ikonnya & Teks Menimpa Header Tab
* **Gejala 1 (Ikon Vanilla):** Pada tab Pink Legacy, item seperti *Pink Legacy Bow*, *Shield*, *Fishing Rod*, *Pickaxe*, *Axe* hanya menampilkan ikon vanilla biasa (*stick*, *bow*, *shield*, *diamond pickaxe*).
  * **Akar Masalah:** Di class `ModItems.java`, hanya 20 item Sakura yang terdaftar sebagai item Fabric resmi. 19 item Pink Legacy **belum didaftarkan di `ModItems.java`**, sehingga `AnvilItemCatalog` memanggil fallback ke `Items.BOW`, `Items.SHIELD`, `Items.DIAMOND_PICKAXE`.
* **Gejala 2 (List Menimpa Tab):** Teks `Pink Legacy Staff` merender menumpuk di atas tombol tab `All`, `Sakura`, `Pink`.
  * **Akar Masalah:** Loop rendering di `AnvilSideListWidget.java` tidak menggunakan *Scissor Clipping* (`GuiGraphicsExtractor.enableScissor`). Item baris yang berada di antara `listTop - ROW_HEIGHT` dan `listTop` tetap digambar sebagian dan meluap ke area tab bar.

---

### C. Analisis Format Pewarnaan Gradasi Teks Item di Vanilla (&x vs §x)
* **Pertanyaan Pengguna:**
  *Apakah bisa ketika rename, teks nama item juga berubah misal `"Sakura Sword"` menjadi `&x&E&4&3&A&9&6&l🌸 &x&E&7&3&F&9&B&lS&x&E&9&4&2&9&E&la...`? Apa format pewarnaan yang dapat terbaca gradasi pada vanilla?*

* **Jawaban Teknis:**
  1. **Format Gradasi yang Terbaca di Vanilla Java Edition:**
     - Vanilla Minecraft Java Edition **TIDAK membaca kode `&`** secara bawaan; kode `&` adalah format plugin server (Spigot/Bungee/Essentials).
     - Vanilla membaca **karakter kontrol Section `§`** dengan format RGB Hex:
       $$\text{Format Vanilla: } \mathtt{\S x\S R\S R\S G\S G\S B\S B}$$
       Contoh untuk warna `#E43A96`: `§x§E§4§3§A§9§6`.
       Jika ditambah tebal (*bold* `§l`): `§x§E§4§3§A§9§6§l🌸 §x§E§7§3§F§9§B§lS§x§E§9§4§2§9§E§la...`
  2. **Apakah Anvil Bisa Mengisinya Secara Otomatis?**
     - **BISA!** Pemain vanilla memang diblokir mengetikkan simbol `§` dari keyboard biasa, tetapi mod Fabric kita di sisi klien dapat memanggil `editBox.setValue(...)` dengan menyuntikkan teks yang sudah memiliki format `§x§R§R...` secara langsung!
  3. **Tantangan terhadap Model/CIT Matching:**
     - Jika nama item diubah menjadi gradasi warna, selector `minecraft:custom_name` pada `netherite_sword.json` vanilla akan membandingkan string secara harfiah.
     - **Solusi Kami:**
       - Kita daftarkan **keduanya** pada `assets/minecraft/items/*.json`: varian nama polos (`Sakura Sword`) dan varian nama gradasi (`§x...🌸 Sakura Sword 🌸`).
       - Pada sidebar Anvil, sediakan mode fleksibel:
         - **Klik Kiri (Default):** Nama CIT Polos (kompatibel 100% dengan semua server & vanilla).
         - **Shift + Klik Kiri (atau Tombol Mode di Header):** Nama Gradasi Pink-Gold Aesthetic dengan Bunga Sakura `🌸`.

---

## 2. Rencana Implementasi Bertahap (SemVer v1.4.0)

### Fase 1: Penerapan Semantic Versioning (v1.4.0)
- [ ] Naikkan versi di `VERSION` menjadi `1.4.0`.
- [ ] Perbarui `gradle.properties`: `mod_version = 1.4.0`.
- [ ] Perbarui `fabric.mod.json`: pastikan versi terikat pada `1.4.0`.
- [ ] Perbaiki format `pack.mcmeta` di `sakura-resourcepack`:
  ```json
  {
    "pack": {
      "pack_format": 88,
      "min_format": 15,
      "max_format": 88,
      "description": "Takasha Multi-Set Weapons & Arsenal v1.4.0"
    }
  }
  ```
- [ ] Pastikan output rilis diberi nama `Hasil mod/Takasha-1.4.0.jar` dan `Hasil RP/Takasha-1.4.0.zip`.

### Fase 2: Bundling Aset Lengkap ke Dalam Mod JAR (Fix Gambar 1)
- [ ] Sinkronkan seluruh aset dari `sakura-resourcepack/assets/` ke dalam `sakura-weapons/src/main/resources/assets/`:
  - `assets/sakura/` (25 model item + tekstur animasi)
  - `assets/pink_legacy/` (24 model item + tekstur animasi)
  - `assets/minecraft/items/` (definisi item models 26.2 untuk pedang, beliung, kapak, sekop, cangkul, busur, perisai, trisula, dll.)
- [ ] Dengan langkah ini, mod JAR memiliki *built-in resource pack* otomatis; pemain **tidak perlu memasang resource pack terpisah** untuk melihat perubahan tekstur saat rename di vanilla 26.2!

### Fase 3: Registrasi Penuh Set Pink Legacy di Mod (Fix Gambar 2 - Ikon)
- [ ] Daftarkan 19 item Pink Legacy di `ModItems.java`:
  - `PINK_LEGACY_SWORD`, `PINK_LEGACY_BATTLE_AXE`, `PINK_LEGACY_SPEAR`, `PINK_LEGACY_HALBERD`, `PINK_LEGACY_HAMMER`, `PINK_LEGACY_STAFF`, `PINK_LEGACY_BOW`, `PINK_LEGACY_SHIELD`, `PINK_LEGACY_FISHING_ROD`, `PINK_LEGACY_PICKAXE`, `PINK_LEGACY_AXE`, `PINK_LEGACY_SHOVEL`, `PINK_LEGACY_HOE`, `PINK_LEGACY_HELMET`, `PINK_LEGACY_CHESTPLATE`, `PINK_LEGACY_LEGGINGS`, `PINK_LEGACY_BOOTS`, `PINK_LEGACY_WINGS`, `PINK_LEGACY_KEY`.
- [ ] Daftarkan ke `ModItemGroups.java` pada tab kreatif Takasha.
- [ ] Perbarui `AnvilItemCatalog.java` agar menggunakan `ModItems.PINK_LEGACY_*`, sehingga sidebar menampilkan ikon 3D kustom Pink Legacy secara sempurna!

### Fase 4: Perbaikan Scissor Clipping pada Anvil Side-List (Fix Gambar 2 - Overlap)
- [ ] Pada `AnvilSideListWidget.java`, tambahkan scissor clipping di awal dan akhir render list:
  ```java
  extractor.enableScissor(px, listTop, px + pw, listBottom);
  // ... render rows ...
  extractor.disableScissor();
  ```
- [ ] Hal ini menjamin tidak ada teks baris maupun ikon yang bocor atau menimpa header tab `All / Sakura / Pink`.

### Fase 5: Generator Teks Gradasi Warna Bawaan Vanilla & Dual-Rename Mode
- [ ] Buat utility helper `ColorGradientUtil`:
  - Mengonversi teks biasa (misal `"Sakura Sword"`) menjadi format warna gradasi vanilla `§x§R§R§G§G§B§B` (palet gradasi Sakura Pink ke Deep Rose `#FFB7C5` -> `#E43A96` -> `#F251AD`).
  - Menyediakan format Spigot `&x&R&R...` bila dibutuhkan untuk server plugin.
- [ ] Tambahkan tombol toggle atau interaksi:
  - **Klik biasa:** Masukkan nama standar CIT (`"Sakura Sword"`).
  - **Shift + Klik:** Masukkan nama warna gradasi cantik dengan bunga sakura (`"§x§E§4§3§A§9§6§l🌸 §x§E§7§3§F§9§B§lS...§l🌸"`).
- [ ] Daftarkan pola nama gradasi ini ke dalam `cases` di `items/*.json`.

### Fase 6: Kompilasi, Build & Verifikasi
- [ ] Jalankan `./gradlew.bat build` untuk memproduksi `Hasil mod/Takasha-1.4.0.jar`.
- [ ] Bangun Resource Pack `Hasil RP/Takasha-1.4.0.zip`.
- [ ] Salin JAR ke profil `kaizenmc.id\mods\`.
- [ ] Verifikasi di dalam game bahwa:
  1. Netherite Sword di-rename menjadi `Sakura Katana` langsung berganti model 3D di slot Anvil.
  2. Tab Pink Legacy menampilkan ikon 3D kustom asli, bukan item vanilla.
  3. Scroll list tidak lagi menimpa header tab.
  4. Fitur rename nama gradasi bekerja dengan indah.

---

## 3. Matriks Perubahan & Estimasi Efek

| Masalah | Target Modul | Solusi Teknis | Hasil Akhir |
|---|---|---|---|
| **Gambar 1 (Tidak berubah)** | `sakura-weapons/assets/` & `items/*.json` | Bundle seluruh aset 26.2 langsung ke dalam Mod JAR | Rename langsung berubah di slot output Anvil tanpa butuh CIT Resewn |
| **Gambar 2 (Ikon vanilla)** | `ModItems.java`, `AnvilItemCatalog.java` | Registrasi 19 item Pink Legacy di Java Mod | Sidebar menampilkan ikon model 3D Pink Legacy asli |
| **Gambar 2 (Tab tertimpa)** | `AnvilSideListWidget.java` | Implementasi `enableScissor(listTop, listBottom)` | Scrolling rapi terpotong tepat di batas viewport |
| **Gradasi Teks Vanilla** | `ColorGradientUtil.java`, `items/*.json` | Format `§x§R§R§G§G§B§B` dengan Dual-Mode Anvil Click | Nama item berwarna gradasi dan tetap memicu model 3D |
