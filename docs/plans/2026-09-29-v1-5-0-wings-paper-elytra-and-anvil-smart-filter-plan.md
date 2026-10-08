# Rencana Implementasi: Takasha v1.5.0 — Khusus Mod Fabric (Wings & Smart Anvil Filter)

> **Versi Rilis:** `1.5.0` (Semantic Versioning Specification 2.0.0: Minor Release)  
> **Target Deliverable:** **HANYA Mod Fabric** (`Hasil mod/Takasha-1.5.0.jar`)  
> **Status:** Menunggu Persetujuan Pengguna (*Awaiting User Approval via Proceed*)  
> **Dokumen Terkait:** [PRD.md](../../PRD.md)  

---

## 1. Penyesuaian Ruang Lingkup (Scope Refinement)

Sesuai instruksi pengguna (*"buat mod nya saja"*):
1. **Fokus Tunggal pada Mod Fabric (`sakura-weapons`):**
   - Seluruh model 3D Blockbench, tekstur animasi `.mcmeta`, dan vanilla item model overrides (`assets/minecraft/items/*.json`) di-bundel langsung ke dalam file JAR mod.
   - **TIDAK** melakukan proses build atau ekspor Resource Pack terpisah (`Hasil RP/` diabaikan/dilewati).
   - Deliverable tunggal: **`Hasil mod/Takasha-1.5.0.jar`**.
2. **Kepatuhan Deployment:**
   - **DILARANG KERAS** menyalin file ke profil launcher Modrinth (`profiles/kaizenmc.id/`).
3. **Pembatalan Chestplate ke Wings:**
   - Chestplate tetap berfungsi normal sebagai armor zirah. Tidak ada pengalihan model sayap ke chestplate.

---

## 2. Penegakan Semantic Versioning (SemVer 2.0.0)

Berdasarkan spesifikasi **Semantic Versioning 2.0.0 (`MAJOR.MINOR.PATCH`)**:
- Dari **`1.4.0` &rarr; `1.5.0`** (**MINOR bump**), karena penambahan fitur baru yang backwards-compatible:
  - Smart Context Anvil Input Filter (penyaringan otomatis katalog berdasarkan item di slot 0 Anvil).
  - Dukungan Wings pada Elytra & Paper (switching model 3D dan rendering entitas punggung Sakura & Pink Legacy).

### Lokasi Sinkronisasi SemVer `1.5.0` (Mod Only):
| No | Lokasi File | Bagian / Variabel | Nilai Baru |
|:---:|:---|:---|:---|
| 1 | `VERSION` (Workspace Root) | Seluruh isi file | `1.5.0` |
| 2 | `sakura-weapons/gradle.properties` | `mod_version` | `1.5.0` |
| 3 | `sakura-weapons/src/main/resources/fabric.mod.json` | `"version": "${version}"` | Di-inject otomatis oleh Loom/Gradle menjadi `1.5.0` |
| 4 | `PRD.md` | Header & Changelog Section | Versi Dokumen `1.5.0`, Status `v1.5.0` |
| 5 | Output Deliverable | Target Build Mod JAR | `Hasil mod/Takasha-1.5.0.jar` |

---

## 3. Desain & Spesifikasi Teknis Pembaruan

### A. Dukungan Base Item Wings: Elytra & Paper (Built-in Mod JAR)
1. **Pembuatan `elytra.json` di Mod:**
   - Lokasi: `sakura-weapons/src/main/resources/assets/minecraft/items/elytra.json`.
   - Menggunakan `minecraft:select` pada `minecraft:custom_name`.
   - **Kasus Sakura Wing:**
     - Plain string: `"Sakura Wing"`, `"Sakura Wings"`, `"sakura wing"`, `"sakura wings"`, `"Sayap Sakura"`, `"sayap sakura"`.
     - Varian gradasi warna: format Section symbol (`§x...`) dan Ampersand (`&x...`) dengan dan tanpa bunga sakura `🌸`.
     - Output model: `"sakura:item/wing"`.
   - **Kasus Pink Legacy Wings:**
     - Plain string: `"Pink Legacy Wings"`, `"Pink Legacy Wing"`, `"pink legacy wings"`, `"pink legacy wing"`, `"Sayap Pink Legacy"`, `"sayap pink legacy"`.
     - Varian gradasi warna: format `§x...` dan `&x...` (bold, emoji bunga).
     - Output model: `"pink_legacy:item/wings"`.
   - Fallback: `"minecraft:item/elytra"`.
2. **Pembaruan `paper.json` di Mod:**
   - Lokasi: `sakura-weapons/src/main/resources/assets/minecraft/items/paper.json`.
   - Menambahkan kata kunci bentuk tunggal `"Sakura Wing"` dan `"Pink Legacy Wing"` beserta seluruh varian gradasinya agar Paper yang di-rename langsung berubah menjadi model 3D sayap di tangan.
3. **Pembaruan Render Entitas Punggung (`SakuraWingsFeatureRenderer.java`):**
   - Hanya mendeteksi `Items.ELYTRA` (serta item native mod `ModItems.SAKURA_WING` dan `ModItems.PINK_LEGACY_WINGS`).
   - Jika Elytra bernama Pink Legacy Wings &rarr; me-render model sayap Pink Legacy di punggung.
   - Jika Elytra bernama Sakura Wing &rarr; me-render model sayap Sakura di punggung.
   - Chestplate sama sekali tidak di-render sebagai sayap (chestplate ke sayap resmi dibatalkan).
   - Mempertahankan proteksi *Server Plugin Conflict Guard* (`CUSTOM_MODEL_DATA`, `itemsadder`, `oraxen`).

---

### B. Smart Context Anvil Input Filter

Membuat sidebar Anvil otomatis menyaring daftar item agar relevan dengan item yang ditaruh pemain di slot input kiri Anvil (Slot 0).

1. **Deteksi Real-Time Slot 0 Anvil:**
   - `AnvilSideListWidget` membaca `((AbstractContainerScreen<?>) parentScreen).getMenu().getSlot(0).getItem()`.
2. **Matriks Kesesuaian Kategori (`matchesInput`):**
   - **Pedang** (`ItemTags.SWORDS` / "sword"): `Sakura Katana`, `Sakura Sword`, `Sakura Bigsword`, `Sakura Dagger`, `Sakura Spear`, `Pink Legacy Sword`, `Pink Legacy Spear`.
   - **Kapak** (`ItemTags.AXES` / "axe"): `Sakura Axe`, `Sakura Halberd`, `Sakura Hammer`, `Sakura Club`, `Sakura Mace`, `Pink Legacy Axe`, `Pink Legacy Battle Axe`, `Pink Legacy Halberd`, `Pink Legacy Hammer`.
   - **Beliung** (`ItemTags.PICKAXES`): `Sakura Pickaxe`, `Pink Legacy Pickaxe`.
   - **Sekop** (`ItemTags.SHOVELS`): `Sakura Shovel`, `Pink Legacy Shovel`.
   - **Cangkul** (`ItemTags.HOES`): `Sakura Hoe`, `Pink Legacy Hoe`.
   - **Busur** (`Items.BOW`): `Sakura Bow`, `Pink Legacy Bow`.
   - **Perisai** (`Items.SHIELD`): `Sakura Shield`, `Pink Legacy Shield`.
   - **Alat Pancing** (`Items.FISHING_ROD`): `Sakura Fishing Rod`, `Pink Legacy Fishing Rod`.
   - **Mace** (`Items.MACE`): `Sakura Mace`, `Sakura Hammer`, `Sakura Club`, `Pink Legacy Hammer`.
   - **Trident** (`Items.TRIDENT`): `Sakura Spear`, `Pink Legacy Spear`.
   - **Elytra** (`Items.ELYTRA`): `Sakura Wing`, `Pink Legacy Wings`.
   - **Paper** (`Items.PAPER`): `Sakura Wing`, `Pink Legacy Wings`, `Sakura Key`, `Pink Legacy Key`, `Sakura Hat`, `Sakura Gauntlet`, `Sakura Katana`.
   - **Helm / Pumpkin** (`ItemTags.HEAD_ARMOR`, `Items.CARVED_PUMPKIN`): `Sakura Hat`, `Pink Legacy Helmet`.
   - **Chestplate** (`ItemTags.CHEST_ARMOR`): `Pink Legacy Chestplate` (murni armor dada).
   - **Leggings** (`ItemTags.LEG_ARMOR`): `Pink Legacy Leggings`.
   - **Boots** (`ItemTags.FOOT_ARMOR`): `Pink Legacy Boots`, `Sakura Gauntlet`.
   - **Utility / Lainnya** (`TRIPWIRE_HOOK`, `STICK`, `BLAZE_ROD`): `Sakura Key`, `Pink Legacy Key`, `Pink Legacy Staff`.
3. **Indikator GUI Dinamis:**
   - Menampilkan sub-header badge: `[Filter: <Nama Item Input> (<Jumlah>)]` berwarna hijau muda `0xFF88FF88`.
   - Jika slot 0 kosong &rarr; menampilkan jumlah katalog normal.
   - Scrollbar dan scissor clipping otomatis menyesuaikan tinggi daftar yang terfilter.

---

## 4. Tahapan Eksekusi Kompilasi & Build (Mod Only)

1. **Tahap 1: Sinkronisasi SemVer ke `1.5.0`**
   - Update `VERSION` &rarr; `1.5.0`
   - Update `sakura-weapons/gradle.properties` &rarr; `mod_version=1.5.0`
   - Update `PRD.md` &rarr; catat rilis v1.5.0
2. **Tahap 2: Integrasi Model Elytra & Paper**
   - Generate `assets/minecraft/items/elytra.json` di `sakura-weapons`.
   - Update `assets/minecraft/items/paper.json` di `sakura-weapons`.
3. **Tahap 3: Update Entity Wings Renderer**
   - Update `SakuraWingsFeatureRenderer.java` untuk mendukung Elytra Sakura & Pink Legacy.
   - Pastikan Chestplate sama sekali tidak di-render sebagai sayap.
4. **Tahap 4: Implementasi Smart Context Filter**
   - Update `AnvilItemCatalog.java` (tambahkan `matchesInput(ItemStack)` & perbarui hint base item Wings menjadi `"Elytra, Paper"`).
   - Update `AnvilSideListWidget.java` (integrasikan pembacaan slot 0 dan badge filter).
5. **Tahap 5: Kompilasi & Pengiriman Deliverable Mod**
   - Jalankan `./gradlew.bat build` pada folder `sakura-weapons/`.
   - Salin output JAR ke `Hasil mod/Takasha-1.5.0.jar`.
   - Verifikasi ukuran file dan integritas JAR.

---

## 5. Kriteria Pengujian & Verifikasi Akhir

- [ ] **SemVer Check:** File `VERSION` = `1.5.0`, `gradle.properties` = `1.5.0`.
- [ ] **Mod JAR Content Check:** `Takasha-1.5.0.jar` memuat `assets/minecraft/items/elytra.json` dan `paper.json`.
- [ ] **Elytra Rename Test:** Rename Elytra ke `Sakura Wing` atau `Pink Legacy Wings` berubah menjadi model sayap 3D.
- [ ] **Paper Rename Test:** Rename Paper ke `Sakura Wing` atau `Pink Legacy Wings` berubah menjadi model sayap 3D.
- [ ] **Player Wings Test:** Memakai Elytra yang di-rename menampilkan sayap 3D di punggung karakter.
- [ ] **Chestplate Abort Check:** Chestplate TIDAK menjadi sayap (tetap armor normal).
- [ ] **Smart Anvil Filter Test:**
  - Taruh Pedang di Slot 0 &rarr; List terfilter hanya pedang/tombak.
  - Taruh Elytra di Slot 0 &rarr; List terfilter hanya Sakura Wing & Pink Legacy Wings.
  - Taruh Paper di Slot 0 &rarr; List terfilter sayap, kunci, topi, gauntlet, katana.
  - Ambil item &rarr; List kembali normal.
- [ ] **No Modrinth Deployment:** Folder `profiles/kaizenmc.id/` tidak tersentuh sama sekali. File hanya ada di `Hasil mod/Takasha-1.5.0.jar`.
