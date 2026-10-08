# Implementation Plan: Fix Worn Armor Textures, Sakura Gauntlet Sword Classification & Vanilla Built-in Coloring

> **Versi Target:** v1.5.1  
> **Status:** Pending Review / Revised with User Feedback  
> **Tanggal:** 2026-09-29  
> **Referensi PRD:** PRD Section 3 (Taksonomi Archetype), Section 11 (Protokol Planning), Section 12 (Plugin & Pure Client Safety)

---

## 1. Ringkasan Masalah & Analisis Akar Masalah (Root Cause Analysis)

### 1.1 Masalah 1: Tekstur Zirah Armor (Helmet, Chestplate, Leggings, Boots) Tidak Timbul Saat Dipakai
- **Gejala:**  
  Ketika pemain memakai zirah hasil rename (misalnya `golden_helmet`, `golden_chestplate`, `golden_leggings`, `golden_boots` yang di-rename menjadi Pink Legacy) atau zirah bawaan mod, ikon 2D di slot inventory sudah berubah menjadi Pink Legacy, namun tubuh karakter pemain (baik di tampilan paperdoll GUI maupun tampilan 3D orang ketiga di dunia) tetap menampilkan zirah vanilla emas (*golden armor*).
- **Akar Masalah (Root Cause):**
  1. Pada arsitektur Minecraft 1.21.2+ / 26.2, sistem model item vanilla (`assets/minecraft/items/*.json`) **hanya** mengubah tampilan 2D item di inventory dan tangan. Minecraft vanilla **tidak memiliki fitur bawaan** untuk mengubah tekstur entitas zirah pada tubuh pemain hanya berdasarkan `minecraft:custom_name`.
  2. Rendering zirah pada tubuh entitas dikendalikan oleh `HumanoidArmorLayer` yang memanggil `EquipmentLayerRenderer.renderLayers()`. Renderer ini membaca komponen `DataComponents.EQUIPPABLE` dari `ItemStack` dan mengambil `ResourceKey<EquipmentAsset> assetId`.
  3. Pada item armor vanilla (misalnya `golden_chestplate`), `assetId` bernilai tetap `EquipmentAssets.GOLD`. Saat di-rename di Anvil, nama item berubah di `DataComponents.CUSTOM_NAME`, tetapi komponen `EQUIPPABLE` tetap mengarah ke `minecraft:gold`. Akibatnya, Minecraft selalu merender tekstur zirah emas.
  4. Pada item bawaan mod (`ModItems.PINK_LEGACY_*`), registrasi sebelumnya hanya memanggil `equippable(slot)` tanpa menetapkan `EquipmentAsset`, sehingga `assetId` bernilai kosong (`Optional.empty()`).
  5. Konfigurasi CIT lama (`type=armor` di folder `optifine/` dan `citresewn/`) hanya berjalan jika pemain memasang mod pihak ketiga (OptiFine atau CIT Resewn). Pada instalasi Fabric standalone vanilla hanya dengan mod Takasha, file `.properties` tersebut diabaikan oleh Minecraft.

### 1.2 Masalah 2: Sakura Gauntlet Terdaftar Sebagai Boots, Bukan Sword
- **Gejala:**  
  Di `AnvilItemCatalog.java`, Sakura Gauntlet diberi keterangan bahan dasar `"Leather / Iron / Diamond Boots, Paper"` dan terfilter ketika pemain memasukkan sepatu (*boots*) ke dalam Anvil.
- **Akar Masalah:**
  Pengelompokan di katalog Anvil sebelumnya menempatkan gauntlet ke dalam kategori boots (`FOOT_ARMOR`), padahal pengguna menegaskan: *"sakura gauntlet itu termasuk sword"*. Karakteristik item di `ModItems.java` dan tag `swords.json` sudah menggunakan atribut pedang (`.sword(...)`), sehingga katalog Anvil dan panduan PRD perlu diselaraskan.

### 1.3 Masalah 3: Format Nama Gradasi Tidak Berfungsi di Vanilla Anvil
- **Gejala:**  
  Nama bergradasi RGB hex panjang (`§x§E§4§3§A§9§6...` atau `&x&E&4...`) gagal diterapkan saat rename di Anvil Minecraft.
- **Akar Masalah:**
  1. Di `AnvilMenu.validateName`, Minecraft vanilla memvalidasi nama dengan:
     ```java
     String filtered = StringUtil.filterText(name);
     if (filtered.length() <= 50) return filtered;
     return null;
     ```
     String gradasi RGB hex membutuhkan lebih dari 120-160 karakter untuk satu nama item! Karena panjangnya melebihi batas 50 karakter (`> 50`), `AnvilMenu` mengembalikan `null` dan menolak seluruh proses rename!
  2. Selain itu, `StringUtil.filterText` secara eksplisit menghapus simbol Section sign `§` (`c != '§'`), sehingga kode warna hex rusak saat diterima server/menu container.
- **Solusi Pengguna:**
  *"nama seperti gradasi juga tidak berfungsi, gunakan pewarnaan bawaan minecraft saja"*.
  Mengganti gradasi hex dengan kode warna bawaan resmi Minecraft (Formatting Codes: `§d` untuk Light Purple / Pink, `§f` untuk White, `§l` untuk Bold). Panjang nama menjadi sangat ringkas (hanya 15-22 karakter, jauh di bawah batas 50 karakter) dan 100% didukung oleh parser vanilla Minecraft.

---

## 2. Arsitektur Solusi Teknis (Technical Design)

```
[ItemStack di Armor Slot]
         │
         ├── Cek 1: Apakah Native Pink Legacy Armor? (ModItems.PINK_LEGACY_*)
         │               OU
         └── Cek 2: Apakah Custom Name memuat "Pink" & ("Helmet"/"Chestplate"/"Leggings"/"Boots"/"Zirah"/"Celana"/"Sepatu")?
         │
         ├── Guard: Apakah memiliki CustomModelData / tag ItemsAdder / Oraxen?
         │         └── YA -> Yield (Biarkan plugin server merender kosmetiknya)
         │
         └── TIDAK -> EquipmentLayerRendererMixin mengalihkan assetId ke:
                      "pink_legacy:pink_legacy" (ModEquipmentAssets.PINK_LEGACY)
                            │
                            ▼
               [assets/pink_legacy/equipment/pink_legacy.json]
                            │
                            ├── humanoid -> assets/pink_legacy/textures/entity/equipment/humanoid/pink_legacy.png
                            │               (Helm, Zirah Dada, Sepatu Boots)
                            │
                            ├── humanoid_leggings -> assets/pink_legacy/textures/entity/equipment/humanoid_leggings/pink_legacy.png
                            │                       (Celana Leggings)
                            │
                            └── humanoid_baby -> assets/pink_legacy/textures/entity/equipment/humanoid_baby/pink_legacy.png
```

### 2.1 Pendaftaran Modern 26.2 Equipment Asset & Tekstur Zirah
1. Buat file aset equipment di:  
   - `sakura-weapons/src/main/resources/assets/pink_legacy/equipment/pink_legacy.json`  
   - `sakura-resourcepack/assets/pink_legacy/equipment/pink_legacy.json`
2. Konversi dan salin file tekstur zirah dari layer legacy (128x64 PNG):
   - `pink_legacy_armor_layer_1.png` -> `assets/pink_legacy/textures/entity/equipment/humanoid/pink_legacy.png`
   - `pink_legacy_armor_layer_2.png` -> `assets/pink_legacy/textures/entity/equipment/humanoid_leggings/pink_legacy.png`
   - `pink_legacy_armor_layer_1.png` -> `assets/pink_legacy/textures/entity/equipment/humanoid_baby/pink_legacy.png`
3. Pertahankan file `.properties` CIT di `citresewn/` dan `optifine/` agar pemain yang menggunakan CIT Resewn / OptiFine tetap 100% kompatibel.

### 2.2 Mixin Engine: `EquipmentLayerRendererMixin` & `AnvilMenuMixin`
1. Konfigurasi `sakura_weapons.mixins.json` didaftarkan ke `fabric.mod.json`.
2. **`EquipmentLayerRendererMixin`**:
   - Target: `net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer`.
   - Mengalihkan pemanggilan `this.equipmentAssets.get(assetId)` ke `manager.get(ModEquipmentAssets.PINK_LEGACY)` jika item yang dipakai adalah zirah Pink Legacy.
   - Hasil render menyatu 100% dengan pencahayaan vanilla, pose tubuh, gerakan merunduk (*sneak*), glint enchantment, dan armor stand tanpa *Z-fighting*.
   - Server Plugin Conflict Guard: Mengalah jika item memiliki data kustom server (`itemsadder` / `oraxen`).
3. **`AnvilMenuMixin`**:
   - Target: `net.minecraft.world.inventory.AnvilMenu`.
   - Mengizinkan kode pewarnaan bawaan Minecraft pada saat pemain me-rename item: mendukung karakter `§` dan menerjemahkan `&` menjadi `§` untuk kode warna bawaan (`0-9`, `a-f`, `l`, `o`, `r`), dengan panjang string tetap terjaga di bawah 50 karakter vanilla.

### 2.3 Pembaruan Native `ModItems.java` Armor Properties
Di `ModItems.java`:
- Definisikan `ResourceKey<EquipmentAsset> PINK_LEGACY_ARMOR_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath("pink_legacy", "pink_legacy"))`.
- Daftarkan `PINK_LEGACY_HELMET`, `PINK_LEGACY_CHESTPLATE`, `PINK_LEGACY_LEGGINGS`, dan `PINK_LEGACY_BOOTS` menggunakan `Equippable.builder(slot).setAsset(PINK_LEGACY_ARMOR_ASSET).setEquipSound(SoundEvents.ARMOR_EQUIP_NETHERITE).build()`.

### 2.4 Reklasifikasi Sakura Gauntlet Sebagai Sword
1. Di `AnvilItemCatalog.java`:
   - Ubah `baseItemHint` entri `sakura_gauntlet` dari `"Leather / Iron / Diamond Boots, Paper"` menjadi `"Diamond / Netherite / Iron Sword, Paper"`.
   - Pada method `matchesInput(ItemStack input)`:
     - Hapus `|| base.contains("gauntlet")` dari blok pemeriksaan `ItemTags.FOOT_ARMOR`.
     - Pastikan pada blok `isInputSword`, `base.contains("gauntlet")` tetap aktif sehingga meletakkan pedang apa pun di Anvil akan menampilkan Sakura Gauntlet di katalog panduan.
2. Di `PRD.md`:
   - Perbarui Tabel Taksonomi Archetype (Bagian 3) agar `gauntlet` berada di bawah kategori Pedang/Blades dengan Base Items baku: `Semua Tier Sword, paper`.

### 2.5 Penerapan Pewarnaan Bawaan Minecraft (Vanilla Formatting Codes)
1. **Format Pewarnaan Baku:**
   - **Sakura Set:** Menggunakan warna bawaan Pink/Light Purple (`§d` / `ChatFormatting.LIGHT_PURPLE`) dan Bold (`§l`), misal: `§d§l🌸 Sakura Sword 🌸` atau `§dSakura Sword` (panjang 15-23 karakter).
   - **Pink Legacy Set:** Menggunakan warna bawaan Pink/Light Purple (`§d`) dan Bold (`§l`), misal: `§d§lPink Legacy Sword` atau `§dPink Legacy Sword` (panjang 19-21 karakter).
   - **Server Friendly Alternative:** Juga mendukung format ampersand (`&d&l🌸 Sakura Sword 🌸` / `&dPink Legacy Sword`).
2. **Perilaku Tombol GUI Anvil:**
   - **Klik Normal:** Menerapkan nama CIT polos standar (contoh: `Sakura Sword`, `Pink Legacy Helmet`).
   - **Shift + Klik:** Menerapkan nama dengan pewarnaan bawaan Minecraft (contoh: `§d§l🌸 Sakura Sword 🌸` atau `§d§lPink Legacy Helmet`).
3. **Pembersihan File Model Item (`assets/minecraft/items/*.json`):**
   - Hapus string gradasi hex 150-karakter yang tidak valid.
   - Daftarkan variasi pewarnaan bawaan Minecraft yang ringkas (`§d`, `§d§l`, `&d`, `&d&l`, serta nama polos) pada blok `cases`.

---

## 3. Rincian Rencana Eksekusi (Actionable Tasks)

### Tahap 1: Persiapan Aset Equipment & Tekstur 26.2
- [x] Buat file `assets/pink_legacy/equipment/pink_legacy.json` pada `sakura-weapons` dan `sakura-resourcepack`.
- [x] Buat folder dan salin tekstur ke:
  - `assets/pink_legacy/textures/entity/equipment/humanoid/pink_legacy.png`
  - `assets/pink_legacy/textures/entity/equipment/humanoid_leggings/pink_legacy.png`
  - `assets/pink_legacy/textures/entity/equipment/humanoid_baby/pink_legacy.png`

### Tahap 2: Implementasi Helper & Registry
- [x] Buat helper class `PinkLegacyArmorUtil.java` untuk mendeteksi zirah Pink Legacy (native maupun rename bilingual EN/ID) serta memfilter CustomModelData server plugin.
- [x] Definisikan `ResourceKey<EquipmentAsset> PINK_LEGACY_ARMOR_ASSET` di `ModEquipmentAssets.java` / `ModItems.java`.

### Tahap 3: Implementasi Fabric Mixins
- [x] Buat konfigurasi `sakura_weapons.mixins.json` di `src/main/resources`.
- [x] Daftarkan `"mixins": ["sakura_weapons.mixins.json"]` pada `fabric.mod.json`.
- [x] Buat mixin `EquipmentLayerRendererMixin.java` untuk mengalihkan `assetId` ke `PINK_LEGACY` pada zirah Pink Legacy.
- [x] Buat mixin `AnvilMenuMixin.java` untuk mendukung pewarnaan bawaan Minecraft (`§` dan `&` color codes) pada proses rename di Anvil.

### Tahap 4: Update Native `ModItems.java` Armor Properties
- [x] Perbarui inisialisasi `PINK_LEGACY_HELMET`, `PINK_LEGACY_CHESTPLATE`, `PINK_LEGACY_LEGGINGS`, dan `PINK_LEGACY_BOOTS` agar menyertakan komponen `Equippable` dengan `PINK_LEGACY_ARMOR_ASSET`.

### Tahap 5: Reklasifikasi Sakura Gauntlet di Katalog Anvil
- [x] Update `AnvilItemCatalog.java` entri `sakura_gauntlet`: ganti base item hint ke Pedang (`Diamond / Netherite / Iron Sword, Paper`).
- [x] Hapus filter `gauntlet` dari `FOOT_ARMOR` (boots) di `matchesInput`.
- [x] Verifikasi bahwa filter pedang (`isInputSword`) mencakup `gauntlet`.

### Tahap 6: Pembaruan Format Pewarnaan Bawaan Minecraft
- [x] Modifikasi `ColorGradientUtil.java` atau buat `MinecraftColorUtil.java` untuk menghasilkan pewarnaan bawaan Minecraft (`§d`, `§l`, dsb.) berukuran ringkas (< 25 karakter).
- [x] Perbarui `AnvilSideListWidget.java` pada metode Shift+Klik agar menerapkan format pewarnaan bawaan Minecraft.
- [x] Perbarui `assets/minecraft/items/*.json` untuk mengganti string gradasi lama dengan format pewarnaan bawaan Minecraft yang bersih.

### Tahap 7: Pembaruan Dokumentasi PRD & SemVer Bump
- [x] Naikkan versi proyek ke `1.5.1` di `VERSION`, `gradle.properties`, dan `PRD.md`.
- [x] Perbarui Tabel Taksonomi Archetype dan Catatan Rilis di `PRD.md`.

### Tahap 8: Kompilasi & Verifikasi Akhir
- [x] Jalankan `./gradlew.bat build` untuk memvalidasi kompilasi Java, mixin annotation processing, dan bundling aset.
- [x] Salin JAR hasil build ke `Hasil mod/Takasha-1.5.1.jar`.
- [x] Pastikan **TIDAK ADA** deployment ke Modrinth launcher sesuai PRD Section 10.

---

## 4. Kriteria Keberhasilan (Verification Checklist)

1. **Worn Armor Visuals:**
   - [x] Saat pemain mengenakan Helm/Chestplate/Leggings/Boots vanilla (Gold, Diamond, Iron, Netherite, dll.) yang di-rename menjadi Pink Legacy, tekstur badan karakter berubah menjadi zirah Pink Legacy (layer 1 untuk helm, dada, sepatu; layer 2 untuk celana).
   - [x] Tampilan di paperdoll GUI inventory dan tampilan F5 third-person player sama-sama menampilkan tekstur Pink Legacy.
   - [x] Item armor bawaan mod (`ModItems.PINK_LEGACY_*`) saat dipakai juga menampilkan tekstur zirah Pink Legacy secara sempurna.
2. **Gauntlet Sword Classification:**
   - [x] Memasukkan Pedang (Diamond/Iron/Netherite/Gold/Stone/Wood/Copper Sword) ke Anvil memunculkan Sakura Gauntlet di GUI samping.
   - [x] Memasukkan Boots ke Anvil **tidak lagi** memunculkan Sakura Gauntlet.
   - [x] Teks panduan item dasar Sakura Gauntlet menampilkan *"Diamond / Netherite / Iron Sword, Paper"*.
3. **Pewarnaan Bawaan Minecraft (Vanilla Formatting):**
   - [x] Shift+Klik di GUI Anvil mengisi nama berformat warna bawaan Minecraft yang ringkas (`§d...` / `< 25 char`).
   - [x] Rename di Anvil berhasil tanpa error atau terpotong oleh batas 50 karakter vanilla.
   - [x] Model 3D kustom langsung aktif ketika pemain menggunakan nama berformat warna bawaan Minecraft maupun nama polos biasa.
4. **Build & Standar Proyek:**
   - [x] `./gradlew.bat build` sukses dengan exit code 0 tanpa error mixin.
   - [x] File JAR resmi tersimpan di `Hasil mod/Takasha-1.5.1.jar`.
