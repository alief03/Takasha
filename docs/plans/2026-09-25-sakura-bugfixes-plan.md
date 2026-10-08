# Sakura Weapon Set & Resource Pack - Bugfixes & Improvements Implementation Plan (Updated)

> **Goal:** Fix 4 critical issues in the Sakura Resource Pack (and Fabric mod) with explicit support for **CIT Resewn Continuation**:
> 1. Inverted Shield model and handle in the right hand (mainhand).
> 2. Sakura Hat implementation using **CIT Resewn Continuation** targeting **Helmet** and **Paper** (fixing `#missing` textures, display transforms, and properties).
> 3. Sakura Chest reassignment to block item `chest` (and `paper`), removing it from armor `chestplate`.
> 4. Tool & weapon grip position held too high up in third person (aligning grip with vanilla tool handles).

---

## 1. Technical Analysis & Root Causes

### Issue 1: Bug Shield Terbalik di Tangan Kanan (Gagang di Luar)
* **Kondisi:** Saat dipegang di tangan kanan (`thirdperson_righthand`), perisai menghadap ke dalam (ke arah tubuh pemain) dan gagang pegangan perisai menghadap ke luar/depan (ke arah kamera).
* **Penyebab:**
  Di `assets/sakura/models/item/shield.json`, blok `thirdperson_righthand` memiliki:
  ```json
  "rotation": [0, 180, 0],
  "translation": [4.25, -1.5, 1.25],
  "scale": [0.7, 0.7, 0.7]
  ```
  Pada tangan kanan di Minecraft Java, rotasi sumbu Y $180^\circ$ membalik permukaan perisai ke arah badan pemain dan mendorong gagang keluar.
* **Solusi:**
  - Ubah rotasi `thirdperson_righthand` menjadi `[0, 0, 0]`.
  - Sesuaikan `translation` agar perisai menempel pas di lengan bawah kanan pemain (`[1.5, -1.5, 1.25]`).
  - Verifikasi `shield_blocking.json` untuk memastikan posisi saat menangkis (*blocking*) di tangan kanan tetap simetris dan menghadap ke depan.

---

### Issue 2: Sakura Hat Menggunakan Dukungan Mod CIT Resewn Continuation (Mengubah Helmet & Paper)
* **Kebutuhan Pengguna:**
  Sakura Hat dikonfigurasi menggunakan dukungan mod **CIT Resewn Continuation** ([modrinth.com/mod/cit-resewn-continuation](https://modrinth.com/mod/cit-resewn-continuation)), dan item dasar yang diubah adalah **Helmet** (semua jenis helm: Netherite, Diamond, Iron, Gold, Chainmail, Leather, Turtle) dan **Paper**.
* **Analisis Teknis & Solusi:**
  1. **Konfigurasi CIT Resewn Continuation:**
     - Buat/perbarui file properties di kedua path yang didukung CIT Resewn:
       - `assets/minecraft/optifine/cit/sakura/sakura_hat.properties`
       - `assets/minecraft/citresewn/cit/sakura/sakura_hat.properties`
     - Isi konfigurasi:
       ```properties
       type=item
       items=paper iron_helmet diamond_helmet netherite_helmet golden_helmet chainmail_helmet leather_helmet turtle_helmet
       model=sakura:item/hat
       nbt.display.Name=ipattern:*Sakura Hat*
       ```
  2. **Dukungan Vanilla 1.21.4 (Items Definition):**
     - Pastikan file `assets/minecraft/items/` berikut memiliki selector custom name `"Sakura Hat"` yang merujuk ke `"sakura:item/hat"`:
       - `diamond_helmet.json`
       - `netherite_helmet.json`
       - `golden_helmet.json`
       - `iron_helmet.json`
       - `turtle_helmet.json`
       - `paper.json`
  3. **Perbaikan Model 3D `hat.json`:**
     - Selesaikan 40 referensi face dengan `#missing` pada [hat.json](file:///d:/Mod%20Minecraft/weapon%20set/sakura/sakura-resourcepack/assets/sakura/models/item/hat.json) dengan mendaftarkan `"missing": "sakura:item/sakura_texture"` ke blok `"textures"` dan memetakan face tersebut ke `#0`.
     - Kalibrasi display transform `"head"` pada `hat.json`:
       ```json
       "head": {
         "rotation": [0, 0, 0],
         "translation": [0, 8.75, 1],
         "scale": [1.6, 1.6, 1.6]
       }
       ```
       Skala $1.6$ dan translasi yang disesuaikan memastikan topi melingkari kepala karakter dengan rapi saat dipasang/dilihat.
  4. **Panel Samping Anvil:**
     - Perbarui teks petunjuk pada panel samping Anvil menjadi **`Hat (Hlm/Pap)`** agar jelas bahwa item dasarnya adalah Helmet atau Paper.

---

### Issue 3: Sakura Chest untuk Item Chest (Bukan Chestplate Armor)
* **Kondisi:** Sakura Chest sebelumnya salah terpasang di berbagai armor `*_chestplate.json`. Sakura Chest aslinya adalah item peti/dekorasi (`minecraft:chest` dan `minecraft:paper`), bukan baju zirah (*chestplate*).
* **Solusi:**
  - Hapus case `"Sakura Chest"` dari semua file `assets/minecraft/items/*_chestplate.json`.
  - Buat file baru `assets/minecraft/items/chest.json` dengan selector Custom Name `"Sakura Chest"` mengarah ke model `"sakura:item/chest"`, dengan fallback ke model peti normal vanilla (`minecraft:normal`).
  - Pertahankan selector pada `paper.json` (format furniture bawaan Oraxen).
  - Perbarui `assets/minecraft/optifine/cit/sakura/sakura_chest.properties` (dan `citresewn/cit/sakura/sakura_chest.properties`) menjadi `items=chest paper`.
  - Perbarui panel samping Anvil dari `Chest (Chest.)` menjadi **`Chest (Chest/Pap)`**.

---

### Issue 4: Pegangan Item Tools Terlalu Tinggi (Tidak Sama dengan Vanilla)
* **Kondisi:** Pada sudut pandang ketiga (F5), karakter memegang senjata/tools di dekat tsuba/guard (pangkal bilah), sehingga seluruh gagang panjang menjuntai kosong di bawah kepalan tangan. Pada vanilla tools, genggaman tangan berada di ujung bawah gagang (*pommel*).
* **Penyebab:**
  Gagang senjata/tools Sakura berada di rentang koordinat sumbu Y: $-10.5$ hingga $+1.0$.
  Pada penyesuaian sebelumnya, `thirdperson_righthand` dan `thirdperson_lefthand` diberi konfigurasi `translation: [0, 4.0, 0.5]` dengan rotasi `[-55, 0, 0]`. Nilai ini membuat titik genggam tangan jatuh tepat pada koordinat model $(8, 8, 8)$ yang merupakan area bilah (+5 unit di atas guard).
* **Solusi:**
  - Geser titik pivot genggaman turun ke tengah-bawah gagang ($Y \approx -4.5$).
  - Pada rotasi $-55^\circ$ di sumbu X, translasi disesuaikan menjadi:
    ```json
    "translation": [0, 8.5, -2.0]
    ```
    (Sehingga posisi genggaman persis seperti tools vanilla, hanya 1-2 piksel ujung kashira/pommel yang tampak di bawah kepalan tangan).
  - Terapkan secara seragam ke seluruh model senjata dan perkakas di `sakura-resourcepack` dan `sakura-weapons`:
    - `katana.json`, `sword.json`, `bigsword.json`, `dagger.json`, `spear.json`, `halberd.json`, `hammer.json`, `club.json`, `mace.json`, `pickaxe.json`, `axe.json`, `shovel.json`, `hoe.json`, `key.json`.

---

## 2. Rencana Tahapan Eksekusi (Actionable Tasks)

### Task 1: Perbaikan Shield Display Transforms
* **Target Files:**
  - `sakura-resourcepack/assets/sakura/models/item/shield.json`
  - `sakura-resourcepack/assets/sakura/models/item/shield_blocking.json`
  - `sakura-weapons/src/main/resources/assets/sakura_weapons/models/item/shield.json`
  - `sakura-weapons/src/main/resources/assets/sakura_weapons/models/item/shield_blocking.json`
* **Langkah:**
  1. Perbaiki `thirdperson_righthand` pada `shield.json` agar `rotation: [0, 0, 0]` dan `translation: [1.5, -1.5, 1.25]`.
  2. Pastikan `shield_blocking.json` memiliki sudut hadap perisai yang benar saat menangkis.
  3. Validasi struktur JSON.

### Task 2: Implementasi Sakura Hat untuk CIT Resewn Continuation (Helmet & Paper)
* **Target Files:**
  - `sakura-resourcepack/assets/minecraft/optifine/cit/sakura/sakura_hat.properties`
  - `sakura-resourcepack/assets/minecraft/citresewn/cit/sakura/sakura_hat.properties`
  - `sakura-resourcepack/assets/minecraft/items/diamond_helmet.json`
  - `sakura-resourcepack/assets/minecraft/items/netherite_helmet.json`
  - `sakura-resourcepack/assets/minecraft/items/golden_helmet.json`
  - `sakura-resourcepack/assets/minecraft/items/iron_helmet.json`
  - `sakura-resourcepack/assets/minecraft/items/turtle_helmet.json`
  - `sakura-resourcepack/assets/minecraft/items/paper.json`
  - `sakura-resourcepack/assets/sakura/models/item/hat.json`
  - `sakura-weapons/src/main/resources/assets/sakura_weapons/models/item/hat.json`
* **Langkah:**
  1. Perbarui properties CIT Resewn (`items=paper iron_helmet diamond_helmet netherite_helmet golden_helmet chainmail_helmet leather_helmet turtle_helmet`).
  2. Buat duplikat file properties di folder `assets/minecraft/citresewn/cit/sakura/` untuk integrasi native CIT Resewn Continuation.
  3. Pada `hat.json`, tambahkan `"missing": "sakura:item/sakura_texture"` ke blok `"textures"` dan ganti semua `"texture": "#missing"` ke `"#0"`.
  4. Kalibrasi blok `"head"` pada `hat.json` dengan `scale: [1.6, 1.6, 1.6]` dan translasi yang presisi.
  5. Pastikan semua file `*_helmet.json` dan `paper.json` memiliki rule `"Sakura Hat"` $\to$ `"sakura:item/hat"`.

### Task 3: Pemindahan Sakura Chest dari Chestplate ke Item Chest
* **Target Files:**
  - `sakura-resourcepack/assets/minecraft/items/chest.json` *(Buat Baru)*
  - `sakura-resourcepack/assets/minecraft/items/diamond_chestplate.json`
  - `sakura-resourcepack/assets/minecraft/items/golden_chestplate.json`
  - `sakura-resourcepack/assets/minecraft/items/iron_chestplate.json`
  - `sakura-resourcepack/assets/minecraft/items/netherite_chestplate.json`
  - `sakura-resourcepack/assets/minecraft/optifine/cit/sakura/sakura_chest.properties`
  - `sakura-resourcepack/assets/minecraft/citresewn/cit/sakura/sakura_chest.properties`
* **Langkah:**
  1. Buat file `assets/minecraft/items/chest.json` dengan selector Custom Name `"Sakura Chest"` yang merujuk ke `"sakura:item/chest"`, dan fallback ke model vanilla chest (`minecraft:normal`).
  2. Hapus selector `"Sakura Chest"` dari file-file `*_chestplate.json`.
  3. Perbarui `sakura_chest.properties`: ganti daftar items menjadi `items=chest paper` (pada `optifine/cit/` dan `citresewn/cit/`).

### Task 4: Kalibrasi Posisi Genggaman Gagang Tools & Weapons
* **Target Files:**
  - Seluruh 14 file model senjata/tools di `assets/sakura/models/item/`:
    `katana.json`, `sword.json`, `bigsword.json`, `dagger.json`, `spear.json`, `halberd.json`, `hammer.json`, `club.json`, `mace.json`, `pickaxe.json`, `axe.json`, `shovel.json`, `hoe.json`, `key.json`.
  - File yang sama di modul fabric `sakura-weapons`.
* **Langkah:**
  1. Jalankan skrip pembaruan translasi `thirdperson_righthand` dan `thirdperson_lefthand` menjadi: `translation: [0, 8.5, -2.0]`.
  2. Verifikasi koordinat agar kepalan tangan tepat menutup bagian tengah-bawah pegangan, persis seperti cara Steve memegang perkakas vanilla.

### Task 5: Perbarui Teks Panel Samping Anvil & Render Ulang GUI
* **Target Files:**
  - `create_side_list.py`
  - `sakura-resourcepack/assets/minecraft/textures/gui/container/anvil.png`
  - `sakura-resourcepack/assets/minecraft/textures/gui/title/sakura_anvil_side_list.png`
* **Langkah:**
  1. Ubah label `Hat (Helmet)` $\to$ `Hat (Hlm/Pap)`.
  2. Ubah label `Chest (Chest.)` $\to$ `Chest (Chest/Pap)`.
  3. Jalankan `python create_side_list.py` untuk me-render gambar tekstur GUI yang baru.
  4. Salin dan tempelkan ke area sisi kanan `anvil.png` ($X = 176 - 255$).

### Task 6: Packaging, Build Mod Fabric, dan Deployment
* **Target Files:**
  - `sakura-resourcepack.zip`
  - Profil Modrinth lokal: `C:\Users\Administrator\AppData\Roaming\ModrinthApp\profiles\kaizenmc.id\resourcepacks\sakura-resourcepack.zip`
  - Mod JAR: `./sakura-weapons-1.0.0.jar`
* **Langkah:**
  1. Jalankan skrip packaging zip untuk `sakura-resourcepack`.
  2. Deploy zip langsung ke folder resourcepacks profil aktif user (`kaizenmc.id`).
  3. Rebuild mod Fabric `./gradlew build` dan salin JAR ke root workspace jika diperlukan.
  4. Jalankan skrip verifikasi otomatis untuk memastikan tidak ada file JSON yang corrupt atau missing reference.

---

## 3. Kriteria Verifikasi Sukses (Verification Checklist)
- [x] Shield saat di tangan kanan menghadap ke depan dengan gagang di sisi dalam lengan.
- [x] Sakura Hat bekerja dengan **CIT Resewn Continuation** ketika me-rename **Helmet** (netherite, diamond, iron, gold, dll.) atau **Paper**.
- [x] Model `hat.json` bebas dari error/warning `#missing` dan memiliki display transform `"head"` yang pas.
- [x] Sakura Chest dapat dipakai dengan me-rename item `Chest` atau `Paper`, dan tidak lagi muncul di `Chestplate`.
- [x] Senjata dan tools (Katana, Pickaxe, Axe, Shovel, dll.) digenggam pas di bagian pegangan/gagang (seperti tool vanilla), tidak lagi terlalu tinggi dekat guard/bilah.
- [x] Panel Anvil menampilkan petunjuk `Hat (Hlm/Pap)` dan `Chest (Chest/Pap)`.

