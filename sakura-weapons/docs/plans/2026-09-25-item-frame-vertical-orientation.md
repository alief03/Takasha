# Item Frame Vertical Orientation Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Mengoreksi orientasi rendering seluruh senjata dan perkakas mod Sakura Weapons di Item Frame (slot transformasi `"fixed"`) dari kondisi mendatar horizontal menjadi 100% tegak lurus vertikal (upright) dan terpusat rapi di tengah frame.

**Architecture:** Menerapkan transformasi Euler `[-90, -90, -90]` pada slot `"fixed"` di file model JSON item, serta mengkalkulasi vektor translasi `[Tx, Ty, Tz]` terpusat sesuai bounding box 3D masing-masing senjata agar simetris tanpa offset melenceng. Memperbarui skrip generator `migrate_assets.py`, menjalankan migrasi ulang, memvalidasi JSON, dan mengompilasi ulang mod `.jar`.

**Tech Stack:** Minecraft Java Edition 26.2, Fabric Loader 0.19.5, Blockbench JSON Model Spec, Python 3 (NumPy/JSON automation), Gradle 9.5.1 / Fabric Loom 1.17.

---

## Analisis Matematis & Root Cause

Berdasarkan screenshot in-game pemain, katana saat ini terpasang secara **horizontal (mendatar dari kiri ke kanan)** di Item Frame lantai.

### 1. Sistem Koordinat Minecraft Model Transformation
Minecraft memproses rotasi transformasi model item menggunakan urutan perkalian Euler:
$$\mathbf{R} = \mathbf{R}_x(\theta_x) \times \mathbf{R}_y(\theta_y) \times \mathbf{R}_z(\theta_z)$$

Model bilah senjata Sakura (seperti `katana.json`) dibuat memanjang vertikal sepanjang sumbu $Y$ lokal ($\vec{v}_{\text{blade}} = [0, 1, 0]$ dari $Y = -16$ sampai $Y = 32$).

### 2. Efek Sudut Rotasi pada Layar Item Frame
Jika $\theta_x = -90^\circ$ dan $\theta_z = -90^\circ$:
- **Bawaan ItemsAdder/RSS ($\theta_y = -45^\circ$):**
  $$\mathbf{R} \times [0, 1, 0]^T = [0.707, 0.707, 0.0]^T \quad \rightarrow \text{Miring 45}^\circ \text{ (Diagonal)}$$
- **Kondisi Saat Ini ($\theta_y = 0^\circ$):**
  $$\mathbf{R} \times [0, 1, 0]^T = [1.0, 0.0, 0.0]^T \quad \rightarrow \text{Mendatar sepanjang sumbu X (Horizontal - seperti di foto)}$$
- **Target Koreksi Vertikal ($\theta_y = -90^\circ$):**
  $$\mathbf{R} \times [0, 1, 0]^T = [0.0, 1.0, 0.0]^T \quad \rightarrow \mathbf{100\%} \text{ Tegak Lurus Vertikal (Ujung ke atas, gagang di bawah)}$$

### 3. Matriks Translasi Ideal per Senjata (Penyelarasan Pusat Frame)
Dengan $\mathbf{R} = [-90, -90, -90]$ dan skala $1.25$, vektor translasi $T = [T_x, T_y, T_z]$ disesuaikan agar pusat geometris senjata berada tepat di titik tengah $(0, 0)$ Item Frame:
- `katana`: $T = [5.0, 0.0, -4.5]$ (mengimbangi lengkungan sarung pedang/scabbard).
- `sword`: $T = [2.8, 1.25, -4.5]$
- `bigsword`: $T = [0.0, 0.0, -4.5]$
- `dagger`: $T = [4.4, 6.8, -4.5]$
- `spear`: $T = [0.0, 1.25, -4.5]$
- `halberd`: $T = [0.0, 0.0, -4.5]$
- `hammer`: $T = [0.0, 5.0, -4.5]$
- `club` / `mace`: $T = [0.0, 3.1, -4.5]$
- `axe`: $T = [0.0, 4.7, -4.5]$
- `pickaxe` / `shovel`: $T = [0.0, 3.4, -4.5]$
- `hoe`: $T = [0.0, 4.1, -4.5]$
- `bow`: $T = [2.9, 3.1, -4.5]$
- `fishing_rod`: $T = [-1.1, 6.25, -4.5]$
- `key`: $T = [2.0, 8.4, -4.5]$

---

## Rincian Tugas (Bite-Sized Tasks)

### Task 1: Update Skrip Otomasi Migrasi Aset (`migrate_assets.py`)

**Files:**
- Modify: `d:\Mod Minecraft\weapon set\sakura\sakura-weapons\migrate_assets.py`

**Rincian Perubahan:**
1. Perbarui dictionary translasi presisi untuk setiap model perkakas/senjata.
2. Ubah rotasi slot `"fixed"` menjadi `[-90, -90, -90]` untuk semua item berkategori tools/weapons.
3. Pertahankan `scale: [1.25, 1.25, 1.25]` dan $T_z = -4.5$ agar permukaan model menempel pas pada kanvas Item Frame.

---

### Task 2: Eksekusi Migrasi & Timpa Model JSON Item

**Files:**
- Target Directory: `d:\Mod Minecraft\weapon set\sakura\sakura-weapons\src\main\resources\assets\sakura_weapons\models\item\` (26 file JSON)

**Langkah:**
1. Jalankan `python migrate_assets.py`.
2. Pastikan file model seperti `katana.json`, `sword.json`, `bigsword.json`, dll. terbarui dengan konfigurasi rotasi baru.

---

### Task 3: Verifikasi Otomatis Integritas Model JSON

**Files:**
- Test Script: `d:\Mod Minecraft\weapon set\sakura\sakura-weapons\verify_models.py`

**Langkah:**
1. Jalankan script validasi yang membaca seluruh model JSON di `src/main/resources/assets/sakura_weapons/models/item/`.
2. Validasi assertion:
   - Seluruh model senjata/alat memiliki `"display"."fixed"."rotation" == [-90, -90, -90]`.
   - Tidak ada sintaks JSON yang rusak.
   - Semua rujukan tekstur tetap menunjuk ke namespace `sakura_weapons:item/*`.

---

### Task 4: Kompilasi Ulang Binary Mod Fabric (`sakura-weapons-1.0.0.jar`)

**Files:**
- Output JAR: `d:\Mod Minecraft\weapon set\sakura\sakura-weapons\build\libs\sakura-weapons-1.0.0.jar`
- Final Deliverable: `d:\Mod Minecraft\weapon set\sakura\sakura-weapons-1.0.0.jar`

**Langkah:**
1. Jalankan `./gradlew.bat build`.
2. Salin file `.jar` yang baru dihasilkan ke folder induk mod.

---

### Task 5: Panduan Verifikasi Visual In-Game

**Langkah untuk Pengguna:**
1. Masukkan file JAR mod yang diperbarui ke folder `.minecraft/mods/`.
2. Masuk kembali ke world Minecraft 26.2.
3. Letakkan `sakura_katana` atau senjata sakura lainnya ke dalam Item Frame (baik di dinding maupun di lantai).
4. Amati bahwa senjata kini langsung tampil **berdiri tegak lurus vertikal**.
