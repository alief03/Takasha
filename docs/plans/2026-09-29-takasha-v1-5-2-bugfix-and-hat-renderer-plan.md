# Implementation Plan: Takasha v1.5.2 — Bugfix & Sakura Hat 3D Worn Head Feature Rendering

> **Versi Target:** v1.5.2  
> **Status:** Ready for Review / Execution  
> **Tanggal:** 2026-09-29  
> **Referensi PRD:** PRD v1.5.2 (Section 3 Taksonomi Archetype, Section 8 SemVer, Section 11 Protokol Planning, Section 12 Pure Client & Plugin Safety)

---

## 1. Analisis Status & Ringkasan Kebutuhan Pengguna

Berdasarkan laporan pengguna dan evaluasi langsung terhadap workspace:
1. **Sakura Club Termasuk ke Sword:**  
   - Sakura Club sebelumnya dikelompokkan di bawah *Senjata Tumpul* (Axe / Mace) di `AnvilItemCatalog.java` dan PRD, sementara karakteristik di `ModItems.java` dan tag `swords.json` sudah merupakan sword (`.sword(...)`).  
   - Pengguna meminta: Sakura Club secara resmi masuk ke dalam archetype **Sword / Pedang** (Bahan dasar: Semua Tier Sword, Paper).
2. **Item Dasar Netherite Axe Belum Berubah Pada Resources Sakura Mace:**  
   - Pada file `netherite_axe.json` (dan file tier kapak lainnya: diamond, iron, gold, stone, wood, copper), case rename untuk `Sakura Mace` tidak ditemukan sama sekali! Akibatnya saat me-rename Netherite Axe menjadi Sakura Mace, modelnya tidak berubah.  
   - File `sakura_mace.properties` di CIT OptiFine dan CIT Resewn juga salah mengacu pada deretan `sword` bukannya `axe`.  
   - Selain itu, `copper_sword.json` secara tidak sengaja memuat case `Sakura Mace`.
3. **Sakura Pickaxe & Pink Legacy Seharusnya ke Item Pickaxe & Resource Pack Pickaxe Bersih:**  
   - Mod Fabric belum memiliki tag `data/minecraft/tags/items/pickaxes.json`, sehingga kedua beliung (`sakura_pickaxe` dan `pink_legacy_pickaxe`) tidak terdaftar dalam `#minecraft:pickaxes`.  
   - Pada resource pack, file `netherite_pickaxe.json` dan `diamond_pickaxe.json` secara keliru memuat case `Sakura Hammer` (Palu Sakura) di dalam file beliung.  
   - CIT `sakura_pickaxe.properties` belum memiliki pasangan bilingual `beliung_sakura.properties`.
4. **Semua Resources Spear Masuk ke Item Dasar Spear dan Trident:**  
   - Pada file `*_spear.json` (netherite, diamond, iron, gold, stone, wood, copper, spear), hanya `Sakura Spear` yang terdaftar; `Pink Legacy Spear` sama sekali belum didaftarkan di dalam file-file spear tersebut.  
   - Pengguna meminta: seluruh sumber daya spear (`Sakura Spear` dan `Pink Legacy Spear`) wajib masuk ke seluruh item dasar spear dan trident.
5. **Sakura Hat Masih Belum Tampil, Masih Helmet (Bukti Gambar Terlampir):**  
   - Di Minecraft 26.2, saat pemain memakai helm vanilla (seperti Golden Helmet pada tangkapan layar pengguna) yang di-rename menjadi Sakura Hat / Topi Sakura:  
     1. `EquipmentLayerRenderer` vanilla tetap merender lapisan tekstur helm emas (*golden helmet texture layer*) pada kepala pemain karena tidak disupresi.  
     2. Tidak ada feature renderer di mod klien yang menggambar model 3D `hat.json` pada kepala pemain untuk item bertipe helm.  
   - Solusi:  
     1. Buat `SakuraHatUtil.java` untuk mendeteksi Sakura Hat dengan Server Conflict Guard.  
     2. Update `EquipmentLayerRendererMixin.java`: Jika mendeteksi Sakura Hat pada slot kepala, return `new EquipmentClientInfo(Map.of())` untuk mematikan/menyembunyikan tekstur helm vanilla secara total.  
     3. Buat `SakuraHatFeatureRenderer.java`: RenderLayer yang menempel pada `parentModel.head`, mengalihkan translasi via `CustomHeadLayer.translateToHead(poseStack, CustomHeadLayer.Transforms.DEFAULT)`, dan merender model 3D `ModItems.SAKURA_HAT` dengan `ItemDisplayContext.HEAD`.  
     4. Daftarkan `SakuraHatFeatureRenderer` di `SakuraWeaponsClient.java` untuk `AvatarRenderer` (pemain) dan `ArmorStandRenderer`.

---

## 2. Inventaris Lengkap Item & Matriks Perubahan

| Item ID | Set | Archetype Baru | Base Items Terkait | Perubahan Teknis yang Dilakukan |
|:---|:---|:---|:---|:---|
| `sakura_club` | Sakura | **Sword** (Pedang) | Semua Tier Sword, `paper` | Update `AnvilItemCatalog.java` (pindah ke `isInputSword`, hapus dari `isInputAxe`/`mace`), update base hint, sinkronisasi CIT |
| `sakura_mace` | Sakura | **Mace / Blunt** | `mace`, Semua Tier Axe, `paper` | Tambahkan case `Sakura Mace` ke `netherite_axe.json` dan semua tier axe JSON, perbaiki `sakura_mace.properties` (sword -> axe), hapus dari `copper_sword.json` |
| `sakura_pickaxe` | Sakura | **Pickaxe** (Alat) | Semua Tier Pickaxe, `paper` | Bersihkan `Sakura Hammer` dari `*pickaxe.json`, buat tag `pickaxes.json` di mod, buat `beliung_sakura.properties` |
| `pink_legacy_pickaxe` | Pink Legacy | **Pickaxe** (Alat) | Semua Tier Pickaxe, `paper` | Daftarkan ke tag `pickaxes.json` di mod, pastikan bersih di seluruh tier pickaxe JSON |
| `sakura_spear` | Sakura | **Spear** (Panjang) | Semua Tier Spear, `trident`, `paper` | Verifikasi kehadiran di seluruh 8 tier `*_spear.json` dan `trident.json`, sinkronisasi properti CIT |
| `pink_legacy_spear` | Pink Legacy | **Spear** (Panjang) | Semua Tier Spear, `trident`, `paper` | Daftarkan case `Pink Legacy Spear` ke seluruh 8 tier `*_spear.json` (`spear.json`, `netherite_spear.json`, dll.) |
| `sakura_hat` | Sakura | **Hat** (Kosmetik Kepala) | Semua Tier Helmet, `carved_pumpkin`, `paper` | Supresi tekstur helm vanilla via `EquipmentLayerRendererMixin`, render model 3D via `SakuraHatFeatureRenderer` |

---

## 3. Rincian Tahapan Implementasi Atomic

### Task 1: Reklasifikasi Sakura Club ke Sword Archetype
**File Target:**
- Modifikasi: `sakura-weapons/src/main/java/net/sakura/weapons/client/gui/AnvilItemCatalog.java`
- Modifikasi: `sakura-resourcepack/assets/minecraft/citresewn/cit/sakura/sakura_club.properties`
- Modifikasi: `sakura-resourcepack/assets/minecraft/optifine/cit/sakura/sakura_club.properties`
- Modifikasi: `sakura-weapons/src/main/resources/assets/minecraft/citresewn/cit/sakura/sakura_club.properties`
- Modifikasi: `sakura-weapons/src/main/resources/assets/minecraft/optifine/cit/sakura/sakura_club.properties`

**Langkah Kerja:**
1. Di `AnvilItemCatalog.java`:
   - Pada `isInputSword`: Tambahkan `|| base.contains("club")`.
   - Pada `isInputAxe`: Hapus `|| base.contains("club")`.
   - Pada `input.is(Items.MACE)`: Hapus `|| base.contains("club")`.
   - Pada entri registrasi `sakura_club`: Ubah hint menjadi `"Diamond / Netherite / Iron Sword, Paper"`.
2. Verifikasi CIT properties `sakura_club.properties` telah memuat daftar pedang dan paper:
   `items=diamond_sword netherite_sword iron_sword golden_sword stone_sword wooden_sword copper_sword paper`

---

### Task 2: Perbaikan Binding Netherite & Tier Axe pada Resources Sakura Mace
**File Target:**
- Modifikasi: `sakura-resourcepack/assets/minecraft/items/netherite_axe.json` (dan `diamond_axe`, `golden_axe`, `iron_axe`, `stone_axe`, `wooden_axe`, `copper_axe`)
- Modifikasi: `sakura-weapons/src/main/resources/assets/minecraft/items/netherite_axe.json` (dan seluruh tier axe)
- Modifikasi: `sakura-resourcepack/assets/minecraft/items/copper_sword.json` (hapus case mace)
- Modifikasi: `sakura-weapons/src/main/resources/assets/minecraft/items/copper_sword.json` (hapus case mace)
- Modifikasi: `sakura-resourcepack/assets/minecraft/citresewn/cit/sakura/sakura_mace.properties`
- Modifikasi: `sakura-resourcepack/assets/minecraft/optifine/cit/sakura/sakura_mace.properties`
- Modifikasi: `sakura-weapons/src/main/resources/assets/minecraft/citresewn/cit/sakura/sakura_mace.properties`
- Modifikasi: `sakura-weapons/src/main/resources/assets/minecraft/optifine/cit/sakura/sakura_mace.properties`
- Modifikasi: `sakura-weapons/src/main/java/net/sakura/weapons/client/gui/AnvilItemCatalog.java`

**Langkah Kerja:**
1. Tambahkan blok case `Sakura Mace` (dengan nama bilingual dan pewarnaan `§d§l`, `§d`, `&d&l`, `&d`) yang mengarah ke model `"sakura:item/mace"` pada:
   - `netherite_axe.json`
   - `diamond_axe.json`
   - `golden_axe.json`
   - `iron_axe.json`
   - `stone_axe.json`
   - `wooden_axe.json`
   - `copper_axe.json`
2. Hapus case `Sakura Mace` dari `copper_sword.json`.
3. Perbaiki `sakura_mace.properties`:
   `items=mace netherite_axe diamond_axe iron_axe golden_axe stone_axe wooden_axe copper_axe paper`
4. Di `AnvilItemCatalog.java`: Update base hint untuk Sakura Mace menjadi `"Mace, Netherite / Diamond / Iron Axe, Paper"`.

---

### Task 3: Standardisasi Pickaxe & Penambahan Tag Mod
**File Target:**
- Buat: `sakura-weapons/src/main/resources/data/minecraft/tags/items/pickaxes.json`
- Buat: `sakura-weapons/src/main/resources/data/minecraft/tags/items/axes.json`
- Buat: `sakura-weapons/src/main/resources/data/minecraft/tags/items/shovels.json`
- Buat: `sakura-weapons/src/main/resources/data/minecraft/tags/items/hoes.json`
- Modifikasi: `sakura-resourcepack/assets/minecraft/items/netherite_pickaxe.json` (dan `diamond_pickaxe.json`)
- Modifikasi: `sakura-weapons/src/main/resources/assets/minecraft/items/netherite_pickaxe.json` (dan `diamond_pickaxe.json`)
- Buat: `sakura-resourcepack/assets/minecraft/citresewn/cit/sakura/beliung_sakura.properties`
- Buat: `sakura-resourcepack/assets/minecraft/optifine/cit/sakura/beliung_sakura.properties`
- Buat: `sakura-weapons/src/main/resources/assets/minecraft/citresewn/cit/sakura/beliung_sakura.properties`
- Buat: `sakura-weapons/src/main/resources/assets/minecraft/optifine/cit/sakura/beliung_sakura.properties`

**Langkah Kerja:**
1. Buat tag `pickaxes.json` berisi `sakura_weapons:sakura_pickaxe` dan `sakura_weapons:pink_legacy_pickaxe`.
2. Hapus case `Sakura Hammer` dari `netherite_pickaxe.json` dan `diamond_pickaxe.json` (karena hammer adalah mace/axe).
3. Buat `beliung_sakura.properties` untuk mencocokkan nama Bahasa Indonesia di OptiFine/CIT Resewn.

---

### Task 4: Propagasi Sumber Daya Spear ke Seluruh Item Dasar Spear & Trident
**File Target:**
- Modifikasi: Seluruh 8 file `*_spear.json` di `sakura-resourcepack/assets/minecraft/items/`:
  `spear.json`, `netherite_spear.json`, `diamond_spear.json`, `golden_spear.json`, `iron_spear.json`, `stone_spear.json`, `wooden_spear.json`, `copper_spear.json`
- Modifikasi: Seluruh 8 file `*_spear.json` di `sakura-weapons/src/main/resources/assets/minecraft/items/`
- Verifikasi: `trident.json` di kedua direktori
- Modifikasi: `sakura-weapons/src/main/java/net/sakura/weapons/client/gui/AnvilItemCatalog.java`

**Langkah Kerja:**
1. Buka setiap file `*_spear.json` dan tambahkan case untuk `Pink Legacy Spear` (sehingga setiap file memiliki case `Sakura Spear` dan `Pink Legacy Spear`).
2. Pastikan `trident.json` memuat kedua spear.
3. Di `AnvilItemCatalog.java`:
   - Tambahkan pengecekan spear: `if (input.is(Items.TRIDENT) || input.getItem().toString().toLowerCase().contains("spear")) { return base.contains("spear"); }`.
   - Update base hint untuk Sakura Spear & Pink Legacy Spear: `"Trident, All Spear Tiers, Paper"`.

---

### Task 5: Implementasi Sakura Hat Feature Renderer & Supresi Tekstur Helm Vanilla
**File Target:**
- Buat: `sakura-weapons/src/main/java/net/sakura/weapons/util/SakuraHatUtil.java`
- Modifikasi: `sakura-weapons/src/main/java/net/sakura/weapons/mixin/EquipmentLayerRendererMixin.java`
- Buat: `sakura-weapons/src/main/java/net/sakura/weapons/client/render/SakuraHatFeatureRenderer.java`
- Modifikasi: `sakura-weapons/src/main/java/net/sakura/weapons/SakuraWeaponsClient.java`

**Langkah Kerja:**
1. **`SakuraHatUtil.java`**:
   - Metode `isSakuraHat(ItemStack stack)`:
     - Conflict guard untuk `CUSTOM_MODEL_DATA` dan `CUSTOM_DATA` (ItemsAdder / Oraxen).
     - Deteksi `ModItems.SAKURA_HAT`.
     - Deteksi `DataComponents.CUSTOM_NAME` yang memuat keyword bilingual `"sakura"` & (`"hat"` atau `"topi"`).
2. **`EquipmentLayerRendererMixin.java`**:
   - Tambahkan pengecekan:
     ```java
     if (SakuraHatUtil.isSakuraHat(itemStack)) {
         return new EquipmentClientInfo(java.util.Map.of());
     }
     ```
   - Ini secara otomatis menghilangkan tekstur helm emas/diamond vanilla saat pemain memakai Sakura Hat!
3. **`SakuraHatFeatureRenderer.java`**:
   - `RenderLayer<S, M>` turunan untuk `HumanoidRenderState` dan `HumanoidModel`.
   - Di `submit()`:
     ```java
     ItemStack headItem = state.headEquipment;
     if (SakuraHatUtil.isSakuraHat(headItem)) {
         poseStack.pushPose();
         this.getParentModel().head.translateAndRotate(poseStack);
         CustomHeadLayer.translateToHead(poseStack, CustomHeadLayer.Transforms.DEFAULT);
         this.itemModelResolver.updateForTopItem(this.itemRenderState, getSakuraHatDisplayStack(), ItemDisplayContext.HEAD, null, null, 0);
         this.itemRenderState.submit(poseStack, submitNodeCollector, packedLight, OverlayTexture.NO_OVERLAY, state.outlineColor);
         poseStack.popPose();
     }
     ```
4. **`SakuraWeaponsClient.java`**:
   - Daftarkan `SakuraHatFeatureRenderer` ke `AvatarRenderer` dan `ArmorStandRenderer`.

---

### Task 6: Sinkronisasi Aset, Validasi Integritas & Build Rilis v1.5.2
**File Target:**
- Modifikasi: `VERSION` (naikkan ke `1.5.2`)
- Modifikasi: `PRD.md` (dokumentasikan v1.5.2)
- Eksekusi: `python validate_cit_and_items.py`
- Eksekusi: `python build.py` -> `Hasil RP/Takasha-1.5.2.zip`
- Eksekusi: Gradle build -> `Hasil mod/Takasha-1.5.2.jar`

**Kriteria Verifikasi:**
1. `validate_cit_and_items.py` melewati seluruh pemeriksaan tanpa error.
2. `build.py` menghasilkan `Hasil RP/Takasha-1.5.2.zip`.
3. Gradle kompilasi `build` sukses menghasilkan `Hasil mod/Takasha-1.5.2.jar`.
4. Tidak ada deployment ke Modrinth sesuai instruksi pengguna.

---

## 4. Execution Handoff

Dokumen rencana selesai dan disimpan di `docs/plans/2026-09-29-takasha-v1-5-2-bugfix-and-hat-renderer-plan.md`.
Tersedia dua opsi eksekusi:
1. **Subagent-Driven (Sesi ini)** — Eksekusi task-by-task secara otomatis dengan verifikasi ketat di setiap langkah.
2. **Parallel Session (Sesi terpisah)** — Buka sesi baru dengan modul executing-plans.
