# Implementation Plan: Perbaikan Ukuran & Tumpang Tindih Layer Armor (v1.6.4)

**Tanggal:** 30 September 2026  
**Target Versi:** Takasha `v1.6.4` (Patch Release - SemVer 2.0.0)  
**Dokumen Referensi:** PRD.md (Section 2.4, 8, 10, 11)  
**Status:** Selesai (*Completed & Verified*)

---

## 1. Analisis Diagnostik & Akar Masalah (Root Cause Analysis)

Berdasarkan analisis visual mendalam pada screenshot dalam game (`media_1790734187892.png` & `media_1790734204770.png`) serta audit byte-level pada model Minecraft Java Edition (`HumanoidModel`, `LayerDefinitions`):

### 1.1 Bentrokan Torso 14-Baris (Chestplate vs Leggings)
- **Kondisi Saat Ini:**
  - Layer 1 (Chestplate, Dilatasi `1.0F`): Memiliki piksel dari baris `Y=32` hingga `Y=61`. Pada baris `Y=54..61`, terdapat plat perut perak (*grey 6-pack*).
  - Layer 2 (Leggings, Dilatasi `0.5F`): Memiliki piksel torso dari baris `Y=48` hingga `Y=63` (perut atas pink dan suspender).
  - **Dampak Engine:** Minecraft Java merender dua kotak kuboid torso konsentris pada `Y=48..61`. Saat bergerak atau bernapas, kotak dalam (`0.5F`) menembus kotak luar (`1.0F`), menghasilkan tampilan perut ganda bertumpuk, siluet bengkak (*bulky*), dan *z-fighting*.
- **Standar Kanonikal Vanilla Minecraft (`diamond.png` / `netherite.png`):**
  - Leggings Torso **HANYA** berada pada baris `Y=54..63` (pelvis, selangkangan, dan ikat pinggang rendah).
  - Leggings Torso baris `Y=32..53` **100% KOSONG / TRANSPARAN MURNI (0 PIXEL)**.
  - Chestplate menutup tubuh bagian atas hingga pinggang (`Y=32..53`), dan berhenti sebelum pelvis.

### 1.2 Bentrokan Lutut (Boots vs Leggings)
- **Kondisi Saat Ini:**
  - Layer 1 (Boots, Dilatasi `1.0F`): Memiliki cincin manset pejal 32-piksel pada baris `Y=52..53` tepat di area lutut.
  - Layer 2 (Leggings, Dilatasi `0.5F`): Memiliki rok/celana kuning pejal pada baris `Y=52..55`.
  - **Dampak Engine:** Sepatu luar (`1.0F`) mencekik lutut celana dalam (`0.5F`), membentuk cincin ganda abu-abu yang menonjol keluar dan memotong rok secara tidak wajar.
- **Standar Kanonikal Vanilla:**
  - Boots hanya menutupi betis bawah dan telapak kaki (`Y=54..63`).
  - Paha dan lutut (`Y=32..53`) menjadi hak eksklusif Leggings.

### 1.3 Pembengkakan Helm (Hat Layer Dilatasi `1.5F`)
- **Kondisi Saat Ini:**
  - Tekstur `humanoid/valentine.png` memiliki 66 piksel pada Hat layer (`X=64..128, Y=0..32`).
  - Tekstur `humanoid/pink_legacy.png` memiliki 232 piksel pada Hat layer.
  - **Dampak Engine:** Pada `HumanoidModel`, Hat layer didefinisikan dengan `cubeDeformation.extend(0.5f)` yang pada zirah luar (`1.0F`) menghasilkan **dilatasi raksasa `1.5F`** (ukuran 11x11x11 blok). Ini menciptakan cangkang melayang tebal di sekeliling kepala yang membuat helm terlihat sangat besar (*oversized*).
- **Standar Kanonikal Vanilla:**
  - Helm Diamond dan Netherite memiliki **0 piksel pada Hat layer** (`X=64..128, Y=0..32` transparan murni). Helm hanya dirender rapi dan pas di kepala pada Head base (`1.0F`).

### 1.4 Halangan Garis Mata (Visor Droop)
- Pada muka depan helm Valentine (`X=16..32`), baris `Y=22..24` turun menutupi separuh mata karakter pemain. Tepi bawah pelindung dahi perlu dirapikan ke atas garis alis (`Y=21`) agar mata pemain terlihat jelas.

---

## 2. Matriks Standarisasi UV Nol-Tumpang-Tindih (Zero-Overlap Matrix)

Untuk mengeliminasi tumpang tindih secara absolut, zonasi matematis berikut akan diterapkan pada seluruh set (Valentine & Pink Legacy) skala HD 128x64:

| Bagian Tubuh | Layer 1 (`humanoid`, Outer `1.0F`) | Layer 2 (`humanoid_leggings`, Inner `0.5F`) | Status Visual Akhir |
|:---|:---|:---|:---|
| **Kepala (`Y=0..32`)** | Helm pas kepala (`X=0..64`). Hat layer (`X=64..128`) = **0 px** | 🚫 **0 px (Transparan Murni)** | Ramping, tidak menggembung `1.5F`, mata bebas |
| **Bahu & Dada Atas (`Y=32..40`)** | Pelindung bahu & kerah zirah | 🚫 **0 px (Transparan Murni)** | Bahu bersih, leher pas badan |
| **Dada & Emblem (`Y=40..48`)** | Plat dada & lambang hati | 🚫 **0 px (Transparan Murni)** | Lambang hati jelas & tajam |
| **Perut / Abdomen (`Y=48..53`)** | Plat perut zirah (Chestplate) | 🚫 **0 px (Transparan Murni)** | Bebas tumpang tindih perut ganda |
| **Ikat Pinggang & Pelvis (`Y=54..63`)** | 🚫 **0 px (Transparan Murni)** | Ikat pinggang & pelindung pinggul | Celana/rok tersambung rapi di pinggang |
| **Lengan (`X=80..112, Y=32..64`)** | Lengan baju zirah (Chestplate) | 🚫 **0 px (Transparan Murni)** | Lengan rapi tanpa clipping |
| **Paha & Lutut (`X=0..32, Y=32..53`)** | 🚫 **0 px (Transparan Murni)** | Celana / rok lipit kuning / paha | Rok & celana mulus tanpa cincin lutut |
| **Betis Bawah & Sepatu (`X=0..32, Y=54..64`)** | Sepatu pelindung & sol (Boots) | 🚫 **0 px (Transparan Murni)** | Sepatu pas membalut kaki bawah |

*Hasil:* **TIDAK ADA SATUPUN KOORDINAT PIKSEL YANG BERIRISAN** antara Layer 1 dan Layer 2.

---

## 3. Rencana Eksekusi Bertahap (Action Plan)

### Tahap 1: Pembaruan Skrip Restorasi Tekstur (`scripts/fix_armor_textures.py`)
- Menerapkan aturan pemotongan bersih matematis:
  - **Valentine & Pink Legacy Layer 1:**
    - Kosongkan Hat layer: `X: 64..128, Y: 0..32` -> transparan murni.
    - Kosongkan Torso pelvis: `X: 32..80, Y: 54..64` -> transparan murni.
    - Kosongkan Legs paha/lutut: `X: 0..32, Y: 32..53` -> transparan murni.
    - Rapikan garis alis helm Valentine pada `Y=22..24` muka depan agar mata tidak tertutup.
  - **Valentine & Pink Legacy Layer 2:**
    - Kosongkan Kepala: `X: 0..128, Y: 0..32` -> transparan murni.
    - Kosongkan Lengan: `X: 80..112, Y: 32..64` -> transparan murni.
    - Kosongkan Torso atas/perut: `X: 32..80, Y: 32..53` -> transparan murni.
    - Kosongkan Kaki bawah/sepatu: `X: 0..32, Y: 54..64` -> transparan murni.
- Jalankan skrip dan sinkronkan seluruh file cermin (*mirrors*) di `sakura-weapons` dan `sakura-resourcepack`.

### Tahap 2: Pembaruan Skrip Validasi CI/CD (`scripts/validate_armor_layers.py`)
- Perbarui ambang batas verifikasi otomatis sesuai standar nol tumpang tindih:
  - Layer 1 Hat layer non-zero == 0.
  - Layer 1 Pelvis non-zero == 0.
  - Layer 1 Thigh/Knee non-zero == 0.
  - Layer 2 Upper torso non-zero == 0.
  - Layer 2 Feet non-zero == 0.
- Jalankan verifikasi untuk menjamin 100% kelulusan audit (*zero regression*).

### Tahap 3: Pembaruan Dokumentasi PRD & Sinkronisasi Versi
- Perbarui tabel Section 2.4 pada [PRD.md](file:///d:/Mod%20Minecraft/weapon%20set/sakura/PRD.md) mencakup aturan zonasi baru.
- Catat riwayat rilis v1.6.4 pada Section 8 (SemVer 2.0.0 Patch).
- Naikkan nomor versi ke `1.6.4` pada:
  - `sakura-weapons/gradle.properties` (`mod_version = 1.6.4`)
  - `sakura-weapons/src/main/resources/fabric.mod.json` (`"version": "1.6.4"`)

### Tahap 4: Kompilasi & Packaging Mod Standalone
- Jalankan `./gradlew build` di `sakura-weapons/`.
- Salin artefak hasil kompilasi secara ketat ke:
  `Hasil mod/Takasha-1.6.4.jar`
- **Catatan Kepatuhan:** Tidak melakukan penyalinan otomatis ke folder launcher eksternal sesuai PRD Section 8 & 10.

---

## 4. Kriteria Keberhasilan (Verification Checklist)

- [x] Seluruh tekstur Layer 1 dan Layer 2 memiliki 0 piksel tumpang tindih pada seluruh set (Valentine & Pink Legacy).
- [x] Helm tidak lagi memiliki cangkang melayang dilatasi `1.5F` dan mata pemain terlihat jelas.
- [x] Area perut tidak lagi memiliki efek *double 6-pack* atau *z-fighting* antara baju zirah dan celana.
- [x] Sambungan antara celana dan sepatu di area lutut/betis rata dan tidak bertumpukan.
- [x] Skrip `validate_armor_layers.py` lolos 100% tanpa peringatan.
- [x] `Hasil mod/Takasha-1.6.4.jar` terbangun sukses dan siap diuji coba secara manual oleh pengguna.
