# Sakura Wings Elytra 3D Model Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Mengatasi masalah Sakura Wings yang masih berwujud Elytra abu-abu vanilla ketika dipakai (equipped di slot chestplate/elytra) dengan mengonversi model 3D `wing.json` menjadi Custom Entity Model (CEM) `elytra.jem` untuk Entity Model Features (EMF) & OptiFine, menambahkan CIT `type=elytra` dengan tekstur transparan `empty_elytra.png`, serta menyinkronkan seluruh properti di `optifine/cit/` dan `citresewn/cit/`.

**Architecture:** 
1. **CEM 3D Model Layer (`elytra.jem`):** Konversi 18 elemen 3D Sakura Wings dari `wing.json` ke format `.jem` untuk `left_wing` dan `right_wing` di `assets/minecraft/optifine/cem/` dan `assets/minecraft/emf/cem/`. Mengosongkan box vanilla (`"boxes": []`) agar sayap abu-abu vanilla tidak ter-render, serta memetakan cabang sakura kiri ke `left_wing`, cabang kanan ke `right_wing`, dan simpul tengah bunga beranimasi.
2. **CIT Entity Texture Override (`type=elytra`):** Menambahkan aturan `type=elytra` di CIT Resewn dan OptiFine CIT yang merujuk ke `empty_elytra.png` (transparan 100%) sehingga flap elytra vanilla lenyap saat Sakura Wings / Sayap Sakura dipakai.
3. **Automated Pipeline Script (`build_sakura_wings_etf_emf.py`):** Skrip generator otomatis untuk parsing elemen, rotasi, kalkulasi UV, penyalinan tekstur animasi, hingga penulisan file properti dan JEM secara deterministik.
4. **SemVer & Validation:** Validasi integritas via `validate_cit_and_items.py`, bump versi ke `1.1.1` (atau `1.2.0`), pembaruan dokumentasi PRD, dan build paket rilis `Takasha-{VERSION}.zip`.

**Tech Stack:** Minecraft Resource Pack (Java Edition 1.21.4+/26.2), Entity Model Features (EMF) & OptiFine CEM (`.jem`), CIT Resewn / OptiFine CIT (`.properties`), Blockbench JSON Models, Python 3.14.

---

### Task 1: Analisis Geometri & Pemetaan Koordinat `wing.json` ke CEM `elytra.jem`

**Files:**
- Reference: `sakura-resourcepack/assets/sakura/models/item/wing.json`
- Reference script: `build_sakura_hat_etf_emf.py`
- Test script: `scratch/analyze_wing.py`

**Deskripsi Elemen `wing.json`:**
- Memiliki 18 elemen dengan simetri di sumbu `Z = 8.0`:
  - **Sisi Kiri (`mid_z > 8.0`):** Elemen 1, 6, 8, 9, 13, 15, 16 (cabang sakura samping kiri + kelopak bunga animasi).
  - **Sisi Kanan (`mid_z < 8.0`):** Elemen 0, 3, 10, 11, 12, 14, 17 (cabang sakura samping kanan + kelopak bunga animasi).
  - **Pusat / Knot (`mid_z ≈ 8.0`):** Elemen 2, 4, 5, 7 (simpul pusat dan bunga sakura utama).
- Tekstur yang digunakan:
  - `#0`: `sakura:item/sakura_texture` (256x256 static UV mult = 16.0)
  - `#1`: `sakura:item/sakura_animation_01` (32x640 animated UV mult = 2.0)
  - `#2`: `sakura:item/sakura_animation_02-export` (32x320 animated UV mult = 2.0)

**Langkah:**
1. Validasi bounding box dan transformasi rotasi agar sayap melekat pas di punggung player (di balik chestplate/punggung).
2. Tentukan pivot `left_wing` dan `right_wing` agar ketika player terbang (gliding), sayap kiri dan kanan membuka secara alami mengikuti animasi vanilla elytra.

---

### Task 2: Buat Skrip Otomatisasi `build_sakura_wings_etf_emf.py`

**Files:**
- Create: `build_sakura_wings_etf_emf.py`

Skrip ini mengotomatiskan seluruh alur pembuatan aset Elytra:
1. **Membuat `empty_elytra.png`:**
   - PNG 64x32 transparan 100% menggunakan `zlib` & `struct`.
   - Disimpan di:
     - `assets/minecraft/textures/entity/empty_elytra.png`
     - `assets/minecraft/optifine/cit/sakura/empty_elytra.png`
     - `assets/minecraft/citresewn/cit/sakura/empty_elytra.png`
     - `assets/minecraft/textures/models/armor/empty_elytra.png`
     - Disertakan juga alias tanpa ekstensi `empty_elytra` untuk kompatibilitas loader lama.
2. **Menyalin Tekstur Animasi ke Direktori CEM:**
   - Menyalin `sakura_texture.png`, `sakura_animation_01.png` (+ `.mcmeta`), `sakura_animation_02-export.png` (+ `.mcmeta`), dan `sakura_animation_02.png` (+ `.mcmeta`) ke:
     - `assets/minecraft/optifine/cem/textures/`
     - `assets/minecraft/emf/cem/textures/`
     - `assets/minecraft/textures/entity/`
3. **Mengonversi `wing.json` ke `elytra.jem`:**
   - Membaca `wing.json` dan melakukan iterasi ke-18 elemen.
   - Mengelompokkan elemen ke dalam submodels `left_wing` dan `right_wing`.
   - Menghitung UV wajah (`uvNorth`, `uvSouth`, `uvEast`, `uvWest`, `uvUp`, `uvDown`) dengan skala UV yang tepat untuk masing-masing tekstur (#0 = 16x, #1 & #2 = 2x).
   - Menghapus box vanilla (`"boxes": []`) pada kedua sayap.
   - Menulis `elytra.jem`, `player_elytra.jem`, `armor_stand_elytra.jem`, serta menyalin ke subdirektori `player/`, `player_slim/`, dan `armor_stand/`.
4. **Menghasilkan File CIT Properties untuk Elytra & Item:**
   - `sakura_wings_elytra.properties` (`type=elytra`, `texture=empty_elytra`, name: `*Sakura Wings*`)
   - `sayap_sakura_elytra.properties` (`type=elytra`, `texture=empty_elytra`, name: `*Sayap Sakura*`)
   - `sakura_wings_elytra_png.properties` (`type=elytra`, `texture=empty_elytra.png`)
   - `sayap_sakura_elytra_png.properties` (`type=elytra`, `texture=empty_elytra.png`)
   - `sayap_sakura.properties` (`type=item`, model: `sakura:item/wing`, name: `*Sayap Sakura*` untuk dukungan bahasa Indonesia di inventory).

---

### Task 3: Sinkronisasi CIT Properties (OptiFine & CIT Resewn)

**Files to Create/Modify:**
- `assets/minecraft/optifine/cit/sakura/sakura_wings_elytra.properties`
- `assets/minecraft/citresewn/cit/sakura/sakura_wings_elytra.properties`
- `assets/minecraft/optifine/cit/sakura/sayap_sakura_elytra.properties`
- `assets/minecraft/citresewn/cit/sakura/sayap_sakura_elytra.properties`
- `assets/minecraft/optifine/cit/sakura/sakura_wings_elytra_png.properties`
- `assets/minecraft/citresewn/cit/sakura/sakura_wings_elytra_png.properties`
- `assets/minecraft/optifine/cit/sakura/sayap_sakura_elytra_png.properties`
- `assets/minecraft/citresewn/cit/sakura/sayap_sakura_elytra_png.properties`
- `assets/minecraft/optifine/cit/sakura/sayap_sakura.properties`
- `assets/minecraft/citresewn/cit/sakura/sayap_sakura.properties`

**Langkah:**
1. Pastikan kedua direktori `optifine/cit/sakura` dan `citresewn/cit/sakura` memiliki file yang 100% identik.
2. Pastikan `sakura_wings.properties` yang sudah ada tetap utuh melayani `type=item`.

---

### Task 4: Eksekusi Skrip Generator & Verifikasi Output

**Langkah:**
1. Jalankan `python build_sakura_wings_etf_emf.py`.
2. Verifikasi keberadaan file:
   - `sakura-resourcepack/assets/minecraft/optifine/cem/elytra.jem`
   - `sakura-resourcepack/assets/minecraft/emf/cem/elytra.jem`
   - Semua tekstur animasi di folder CEM textures.
   - Semua file properti CIT elytra.
3. Periksa sintaks JSON pada `elytra.jem` untuk memastikan tidak ada trailing comma, NaN, atau struktur invalid.

---

### Task 5: Validasi Integritas Resource Pack

**Files:**
- Test: `validate_cit_and_items.py`

**Langkah:**
1. Jalankan `python validate_cit_and_items.py`.
2. Pastikan output menyatakan:
   `ALL CHECKS PASSED PERFECTLY!`
   tanpa ada mismatch antara `optifine/cit/` dan `citresewn/cit/`.

---

### Task 6: Update PRD, README & Panduan Penggunaan

**Files to Modify:**
- `PRD.md`
- `sakura-resourcepack/README.txt`

**Langkah:**
1. Tambahkan dokumentasi fitur Sakura Wings (Sayap Sakura) di `PRD.md`:
   - Penjelasan dukungan 3D CEM untuk Elytra saat dipakai via Entity Model Features (EMF) & OptiFine.
   - Penjelasan mekanisme `type=elytra` untuk menyembunyikan sayap abu-abu vanilla.
   - Dukungan nama Inggris (`*Sakura Wings*`) dan Indonesia (`*Sayap Sakura*`).
2. Perbarui `README.txt` di resourcepack dengan panduan penggunaan Sayap Sakura di Anvil.

---

### Task 7: Bump Semantic Version & Build Rilis

**Files to Modify:**
- `VERSION`
- Build Output: `Takasha-{VERSION}.zip`

**Langkah:**
1. Tentukan bump SemVer:
   - Perbaikan fungsional dan penambahan model sayap CEM: bump PATCH (`1.1.1`) atau MINOR (`1.2.0`). (Rekomendasi: `1.2.0` karena menambahkan dukungan Custom Entity Model baru untuk Elytra).
2. Jalankan `python build.py --minor` (atau `--patch`).
3. Verifikasi file zip rilis terbentuk dengan ukuran yang sesuai di root project.

---

## Execution Options

Setelah planning ini disetujui, silakan tentukan opsi eksekusi:

1. **Subagent-Driven (Sesi ini)** - Eksekusi langsung langkah demi langkah dengan validasi menyeluruh di sesi ini.
2. **Manual Checkpoint** - Tinjau detail rencana teknis terlebih dahulu sebelum mulai menulis kode.
