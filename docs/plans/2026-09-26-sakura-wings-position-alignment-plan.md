# Sakura Wings Position Alignment Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Menyesuaikan posisi, skala, dan orientasi Sakura Wings pada Custom Entity Model (CEM) `elytra.jem` agar 100% presisi dan selaras dengan file sumber (`wing.json`), menempel sempurna di punggung player tanpa melayang di udara atau terpisah menjadi dua bagian.

**Architecture:** 
1. **Analisis Transformasi File Sumber (`wing.json`):**
   - File sumber mendefinisikan tampilan wearable di punggung melalui transform `"head"`:
     - `rotation: [-146.25, -88.82, -146.26]` (orientasi sayap menghadap belakang, melebar di pundak)
     - `translation: [-1.0, -15.75, 5.5]` (turun 15.75 unit ke tengah punggung, mundur 5.5 unit ke belakang baju)
     - `scale: [1.46133, 1.46133, 1.46133]` (skala proporsional 1.46x)
2. **Eliminasi Pemisahan & Duplikasi:**
   - Menghubungkan ke-18 elemen menjadi satu kesatuan utuh (unified model) yang bersambung di tengah (`Z = 8.0` / spine player `X = 0`).
   - Meletakkan kesatuan model pada `left_wing` dengan penyesuaian pivot spine, dan mengosongkan `right_wing` (`"boxes": []`) agar tidak terjadi duplikasi sayap ganda yang terpisah.
   - Menambahkan CEM animation reset (`"left_wing.rx": 0`, `"left_wing.ry": 0`, `"left_wing.rz": 0`) agar sayap tidak terpengaruh rotasi flap 15° vanilla yang memiringkan posisi model saat player berdiri.
3. **Pembaruan Script Generator:**
   - Memperbarui `build_sakura_wings_etf_emf.py` dengan kalkulasi koordinat baru yang meniru transform sumber secara eksak.
4. **Validasi & Rilis:**
   - Validasi pack dengan `validate_cit_and_items.py`, bump versi SemVer, dan build `Takasha-{VERSION}.zip`.

**Tech Stack:** Minecraft CEM (`elytra.jem`), Blockbench Model Coordinates, Python 3.14, Entity Model Features (EMF).

---

### Task 1: Audit & Formulasi Matematika Transformasi Sesuai File Sumber

**Files:**
- Reference: `sakura-resourcepack/assets/sakura/models/item/wing.json:1516-1532`
- Script: `build_sakura_wings_etf_emf.py`
- Test: `scratch/test_source_position.py`

**Masalah pada Implementasi Sebelumnya:**
1. **Y Offset Terlalu Tinggi (+18 hingga +25 blok):**
   - Di `build_sakura_wings_etf_emf.py` sebelumnya: `translate: [..., 3.0, ...]`.
   - Ditambah tinggi elemen lokal `orig[1] = 6..10`, menghasilkan posisi akhir `Y > +10` (di atas kepala, melayang di langit).
   - Di file sumber: `translation[1] = -15.75` (bergerak turun ke punggung).
2. **Model Terpecah 2 Bagian:**
   - Sayap kiri dan kanan diekspor ke dua part berbeda yang memiliki rotasi bawaan vanilla `xRot = 15°` dan `zRot = ±15°` serta offset pivot `±5.0`.
   - Mengakibatkan base trunk kayu terpotong di tengah dan menjauh satu sama lain.

**Formulasi Baru:**
- Gunakan translasi vertikal yang menurunkan model ke punggung: `Y = -14.0` hingga `-15.75` (disesuaikan dengan origin spine).
- Z translasi: `Z = +2.5` hingga `+3.5` agar menempel di permukaan belakang baju.
- Satukan ke-18 elemen di bawah satu anchor point spine (`X = 0`), dengan `left_wing` sebagai parent pembawa model dan `right_wing` sebagai part kosong (`"boxes": []`).
- Pasang animasi CEM pengunci rotasi saat berdiri:
  ```json
  "animations": [
    {
      "left_wing.rx": 0,
      "left_wing.ry": 0,
      "left_wing.rz": 0,
      "right_wing.rx": 0,
      "right_wing.ry": 0,
      "right_wing.rz": 0
    }
  ]
  ```

---

### Task 2: Modifikasi Skrip `build_sakura_wings_etf_emf.py`

**Files to Modify:**
- `build_sakura_wings_etf_emf.py`

**Perubahan yang Dilakukan:**
1. Perbarui fungsi `generate_elytra_jem()`:
   - Terapkan skala `scale: 1.46133` (atau sesuaikan koordinat box dan translasi dengan pengali `1.46133`).
   - Ubah root translate submodel agar turun ke area punggung (`Y = -12.5` s.d. `-14.0`, `Z = +3.0`).
   - Jadikan semua 18 elemen menyatu dalam submodels terpadu:
     - `sakura_wing_static` (elemen tekstur `#0`)
     - `sakura_wing_anim1` (elemen tekstur `#1`)
     - `sakura_wing_anim2` (elemen tekstur `#2`)
   - Pasang submodels tersebut di bawah `left_wing` dengan kompensasi pivot shoulder (`X = -5.0` agar tepat di tengah spine).
   - Buat `right_wing` kosong (`"boxes": []`, tanpa submodels) untuk mencegah duplikasi.
   - Tambahkan blok `"animations"` pada `elytra.jem`.

---

### Task 3: Eksekusi Generator & Pengujian Integritas Model

**Langkah:**
1. Jalankan `python build_sakura_wings_etf_emf.py`.
2. Verifikasi file `elytra.jem` yang dihasilkan di `optifine/cem/` dan `emf/cem/`.
3. Pastikan tidak ada duplikasi part dan nilai Y berada di area negatif (menempel di punggung).

---

### Task 4: Validasi Integritas Resource Pack

**Files:**
- Test: `validate_cit_and_items.py`

**Langkah:**
1. Jalankan `python validate_cit_and_items.py`.
2. Pastikan output menyatakan:
   `ALL CHECKS PASSED PERFECTLY!`

---

### Task 5: Bump Semantic Version & Build Paket Rilis Baru

**Files to Modify:**
- `VERSION` (bump ke `1.2.1`)
- `PRD.md`
- Build Output: `Takasha-1.2.1.zip`

**Langkah:**
1. Update `VERSION` ke `1.2.1`.
2. Jalankan `python build.py` untuk memproduksi `Takasha-1.2.1.zip`.
3. Verifikasi file rilis zip di root direktori.
