# Rencana Implementasi: Perbaikan Tekstur Zirah (Armor) & Audit Menyeluruh 59 Item (Takasha v1.6.2)

> **Versi Dokumen:** 1.6.2  
> **Target Rilis SemVer:** v1.6.2 (PATCH Release — Armor Texture Fix & Multi-Set Comprehensive Audit)  
> **Lingkungan Target:** Minecraft Java 1.21.11, Fabric Loader >=0.19.3, OpenJDK 21 LTS (`java-runtime-delta`)  
> **Status:** Pending Review / Siap Dieksekusi  
> **Kepatuhan Kebijakan:** PRD Bagian 8 (SemVer 2.0.0), Bagian 10 (Deployment Lokal `Hasil mod/Takasha-1.6.2.jar`), Bagian 11 (Dokumen Rencana Fisik & Interaktif)

---

## 1. Ringkasan Eksekutif & Landasan Semantic Versioning (SemVer 2.0.0)

Berdasarkan laporan pengguna:
1. **Masalah Tekstur Zirah:** Seluruh zirah (armor pieces baik Pink Legacy maupun Valentine) yang terpasang pada slot zirah karakter pemain tidak merender tekstur 3D-nya pada model tubuh entitas pemain (karakter tampak tidak memakai zirah/hanya skin dasar pemain).
2. **Audit Menyeluruh:** Permintaan audit komprehensif terhadap seluruh 59 item aktif dari 3 set (Sakura: 20 item, Pink Legacy: 19 item, Valentine: 20 item) untuk menjamin stabilitas di runtime Minecraft 1.21.11.

### Klasifikasi Semantic Versioning (v1.6.2 PATCH):
Sesuai aturan SemVer 2.0.0 dan PRD Bagian 8:
- **`MAJOR`**: Dilarang (0 breaking changes terhadap 59 item dan API yang sudah ada).
- **`MINOR`**: Tidak berlaku (tidak ada set senjata baru yang ditambahkan).
- **`PATCH` (v1.6.2)**: **Tepat**. Modifikasi ini memperbaiki bug *rendering* zirah yang tidak tampil, memperbaiki kegagalan deserialisasi codec Minecraft 1.21.11, memutakhirkan folder data tags ke format 1.21 (`tags/item/`), dan membersihkan encoding BOM tanpa memecah kompatibilitas apapun.

---

## 2. Analisis Mendalam Akar Masalah (Root Cause Analysis)

Melalui penelusuran log runtime `latest.log` pada profil game Minecraft 1.21.11:

```
[Worker-Main-1/ERROR]: Couldn't parse data file 'pink_legacy:pink_legacy' from 'pink_legacy:equipment/pink_legacy.json': 
DataResult.Error['Unknown element name:humanoid_baby missed input: {"humanoid_baby":[{"texture":"pink_legacy:pink_legacy"}]}': 
class_10186[layers={HUMANOID=[...], HUMANOID_LEGGINGS=[...]}]]
```

### 1. Akar Masalah Utama: Kegagalan Codec `EquipmentAssetManager` akibat `humanoid_baby`
- Di Minecraft 1.21.11, enum `EquipmentClientInfo$LayerType` hanya memiliki nilai:
  `HUMANOID`, `HUMANOID_LEGGINGS`, `WINGS`, `WOLF_BODY`, `HORSE_BODY`, `LLAMA_BODY`, `PIG_SADDLE`, `STRIDER_SADDLE`, `CAMEL_SADDLE`, `HORSE_SADDLE`, dll.
  **Tidak ada enum `humanoid_baby`** pada Minecraft 1.21.11!
- File `assets/pink_legacy/equipment/pink_legacy.json` dan `assets/valentine/equipment/valentine.json` sebelumnya memuat entri `"humanoid_baby": [{"texture": "..."}]`.
- Akibatnya, `Codec.parse` mengembalikan `DataResult.Error`. `EquipmentAssetManager` menganggap file tersebut rusak/gagal, mencatat pesan ERROR, dan **menolak mendaftarkan aset zirah** ke dalam map internalnya (`equipmentAssets`).
- Ketika karakter memakai helm, zirah dada, celana, atau sepatu (baik item mod asli maupun item vanilla hasil rename), `EquipmentAssetManager.get(...)` mengembalikan `MISSING` (map layer kosong).
- Metode bawaan `EquipmentLayerRenderer.renderLayers(...)` melakukan pengecekan:
  ```java
  List<EquipmentClientInfo.Layer> layers = info.getLayers(layerType);
  if (layers.isEmpty()) {
      return; // Langsung keluar tanpa menggambar apapun!
  }
  ```
  Sehingga zirah menjadi **100% transparan / tidak terlihat**.

### 2. Akar Masalah Sekunder: Ketiadaan Lapisan Fallback Terprogram pada `EquipmentLayerRendererMixin`
- `EquipmentLayerRendererMixin.java` mengalihkan pemanggilan `manager.get(key)`. Namun, jika file JSON di disk mengalami kendala pemuatan atau reload resource pack, `manager.get(...)` mengembalikan `MISSING`.
- Diperlukan instansiasi lapisan fallback terprogram (`PINK_LEGACY_INFO` dan `VALENTINE_INFO`) langsung di kode Java sebagai jaring pengaman lapis baja (*fail-safe defense*) agar jika `manager.get` mengembalikan layer kosong, mod langsung menyuplai layer `HUMANOID` dan `HUMANOID_LEGGINGS` secara instan.

### 3. Temuan Audit: Format Folder Data Tags 1.21+ (`tags/item/` vs `tags/items/`)
- Mojang sejak 1.21 menstandardisasi folder data tags menjadi bentuk tunggal (*singular*): `data/minecraft/tags/item/` (bukan `tags/items/`).
- Saat ini mod meletakkan tag di `data/minecraft/tags/items/`. Akibatnya, di 1.21.11 tag alat/senjata tidak otomatis terhubung ke sistem tag vanilla Minecraft. Kita akan melengkapi direktori `data/minecraft/tags/item/` beserta penambahan tag zirah (`head_armor`, `chest_armor`, `leg_armor`, `foot_armor`).

---

## 3. Hasil Audit Menyeluruh 59 Item (Multi-Set Inventory)

| Set ID | ID Item | Kategori / Archetype | Kelas Java | Model 2D/3D | Status Worn Renderer | Anvil Auto-fill | Tag Status |
|:---|:---|:---|:---|:---|:---|:---|:---|
| `sakura` | `sakura_sword` | Sword | `Item` | 3D Blockbench | In-hand | OK | `#item/swords` |
| `sakura` | `sakura_katana` | Sword | `SakuraKatanaItem` | 3D Blockbench | In-hand | OK | `#item/swords` |
| `sakura` | `sakura_bigsword` | Sword | `SakuraBigswordItem` | 3D Blockbench | In-hand | OK | `#item/swords` |
| `sakura` | `sakura_dagger` | Sword | `SakuraDaggerItem` | 3D Blockbench | In-hand | OK | `#item/swords` |
| `sakura` | `sakura_hammer` | Blunt / Sword | `SakuraHammerItem` | 3D Blockbench | In-hand | OK | `#item/swords` |
| `sakura` | `sakura_spear` | Spear | `Item` | 3D Blockbench | In-hand | OK | Dual-model fallback |
| `sakura` | `sakura_halberd` | Axe | `Item` | 3D Blockbench | In-hand | OK | `#item/axes` |
| `sakura` | `sakura_club` | Sword | `Item` | 3D Blockbench | In-hand | OK | `#item/swords` |
| `sakura` | `sakura_mace` | Blunt / Sword | `Item` | 3D Blockbench | In-hand | OK | `#item/swords` |
| `sakura` | `sakura_gauntlet` | Sword | `Item` | 3D Blockbench | In-hand | OK | `#item/swords` |
| `sakura` | `sakura_pickaxe` | Pickaxe | `Item` | 3D Blockbench | In-hand | OK | `#item/pickaxes` |
| `sakura` | `sakura_axe` | Axe | `Item` | 3D Blockbench | In-hand | OK | `#item/axes` |
| `sakura` | `sakura_shovel` | Shovel | `Item` | 3D Blockbench | In-hand | OK | `#item/shovels` |
| `sakura` | `sakura_hoe` | Hoe | `Item` | 3D Blockbench | In-hand | OK | `#item/hoes` |
| `sakura` | `sakura_bow` | Ranged | `SakuraBowItem` | 3D (3 Stages) | In-hand | OK | Pulling predicates |
| `sakura` | `sakura_shield` | Shield | `SakuraShieldItem` | 3D (Blocking) | In-hand / Off-hand | OK | Blocking predicate |
| `sakura` | `sakura_fishing_rod` | Tool | `FishingRodItem` | 3D (Cast) | In-hand | OK | Cast predicate |
| `sakura` | `sakura_hat` | Head Cosmetic | `Item` | 3D Blockbench | `SakuraHatFeatureRenderer` | OK | Suppresses vanilla helmet |
| `sakura` | `sakura_key` | Utility | `Item` | 3D Blockbench | In-hand | OK | Stack 64 |
| `sakura` | `sakura_wing` | Back Cosmetic | `Item` | 3D Blockbench | `SakuraWingsFeatureRenderer` | OK | Body-attached torso |
| `pink_legacy` | `pink_legacy_sword` | Sword | `Item` | 3D Blockbench | In-hand | OK | `#item/swords` |
| `pink_legacy` | `pink_legacy_battle_axe` | Axe | `Item` | 3D Blockbench | In-hand | OK | `#item/axes` |
| `pink_legacy` | `pink_legacy_spear` | Spear | `Item` | 3D Blockbench | In-hand | OK | Dual-model fallback |
| `pink_legacy` | `pink_legacy_halberd` | Axe | `Item` | 3D Blockbench | In-hand | OK | `#item/axes` |
| `pink_legacy` | `pink_legacy_hammer` | Blunt / Sword | `SakuraHammerItem` | 3D Blockbench | In-hand | OK | `#item/swords` |
| `pink_legacy` | `pink_legacy_staff` | Sword | `Item` | 3D Blockbench | In-hand | OK | `#item/swords` |
| `pink_legacy` | `pink_legacy_bow` | Ranged | `SakuraBowItem` | 3D (3 Stages) | In-hand | OK | Pulling predicates |
| `pink_legacy` | `pink_legacy_shield` | Shield | `SakuraShieldItem` | 3D (Blocking) | In-hand / Off-hand | OK | Blocking predicate |
| `pink_legacy` | `pink_legacy_fishing_rod` | Tool | `FishingRodItem` | 3D (Cast) | In-hand | OK | Cast predicate |
| `pink_legacy` | `pink_legacy_pickaxe` | Pickaxe | `Item` | 3D Blockbench | In-hand | OK | `#item/pickaxes` |
| `pink_legacy` | `pink_legacy_axe` | Axe | `Item` | 3D Blockbench | In-hand | OK | `#item/axes` |
| `pink_legacy` | `pink_legacy_shovel` | Shovel | `Item` | 3D Blockbench | In-hand | OK | `#item/shovels` |
| `pink_legacy` | `pink_legacy_hoe` | Hoe | `Item` | 3D Blockbench | In-hand | OK | `#item/hoes` |
| `pink_legacy` | `pink_legacy_helmet` | Armor (Head) | `Item` | 2D Icon | `EquipmentLayerRenderer` | OK | Diperbaiki di v1.6.2 |
| `pink_legacy` | `pink_legacy_chestplate` | Armor (Chest) | `Item` | 2D Icon | `EquipmentLayerRenderer` | OK | Diperbaiki di v1.6.2 |
| `pink_legacy` | `pink_legacy_leggings` | Armor (Legs) | `Item` | 2D Icon | `EquipmentLayerRenderer` | OK | Diperbaiki di v1.6.2 |
| `pink_legacy` | `pink_legacy_boots` | Armor (Feet) | `Item` | 2D Icon | `EquipmentLayerRenderer` | OK | Diperbaiki di v1.6.2 |
| `pink_legacy` | `pink_legacy_wings` | Back Cosmetic | `Item` | 3D Blockbench | `SakuraWingsFeatureRenderer` | OK | Body-attached torso |
| `pink_legacy` | `pink_legacy_key` | Utility | `Item` | 3D Blockbench | In-hand | OK | Stack 64 |
| `valentine` | `valentine_sword` | Sword | `Item` | 3D Blockbench | In-hand | OK | `#item/swords` |
| `valentine` | `valentine_axe` | Axe | `Item` | 3D Blockbench | In-hand | OK | `#item/axes` |
| `valentine` | `valentine_hammer` | Blunt / Sword | `SakuraHammerItem` | 3D Blockbench | In-hand | OK | `#item/swords` |
| `valentine` | `valentine_spear` | Spear | `Item` | 3D Blockbench | In-hand | OK | Dual-model fallback |
| `valentine` | `valentine_staff` | Magic / Spear | `Item` | 3D Blockbench | In-hand | OK | Multi-base trigger |
| `valentine` | `valentine_pickaxe` | Pickaxe | `Item` | 3D Blockbench | In-hand | OK | `#item/pickaxes` |
| `valentine` | `valentine_shovel` | Shovel | `Item` | 3D Blockbench | In-hand | OK | `#item/shovels` |
| `valentine` | `valentine_hoe` | Hoe | `Item` | 3D Blockbench | In-hand | OK | `#item/hoes` |
| `valentine` | `valentine_bow` | Ranged | `SakuraBowItem` | 3D (3 Stages) | In-hand | OK | Pulling predicates |
| `valentine` | `valentine_crossbow` | Ranged | `Item` | 3D (5 Stages) | In-hand | OK | 5 charging stages |
| `valentine` | `valentine_shield` | Shield | `SakuraShieldItem` | 3D (Blocking) | In-hand / Off-hand | OK | Blocking predicate |
| `valentine` | `valentine_fishing_rod` | Tool | `FishingRodItem` | 3D (Cast) | In-hand | OK | Cast predicate |
| `valentine` | `valentine_hat` | Head Cosmetic | `Item` | 3D Blockbench | `SakuraHatFeatureRenderer` | OK | Suppresses vanilla helmet |
| `valentine` | `valentine_wing` | Back Cosmetic | `Item` | 3D Blockbench | `SakuraWingsFeatureRenderer` | OK | Body-attached torso |
| `valentine` | `valentine_key` | Utility | `Item` | 3D Blockbench | In-hand | OK | Stack 64 |
| `valentine` | `valentine_grenade` | Utility | `Item` | 3D Blockbench | In-hand | OK | Stack 16 |
| `valentine` | `valentine_helmet` | Armor (Head) | `Item` | 2D Icon | `EquipmentLayerRenderer` | OK | Diperbaiki di v1.6.2 |
| `valentine` | `valentine_chestplate` | Armor (Chest) | `Item` | 2D Icon | `EquipmentLayerRenderer` | OK | Diperbaiki di v1.6.2 |
| `valentine` | `valentine_leggings` | Armor (Legs) | `Item` | 2D Icon | `EquipmentLayerRenderer` | OK | Diperbaiki di v1.6.2 |
| `valentine` | `valentine_boots` | Armor (Feet) | `Item` | 2D Icon | `EquipmentLayerRenderer` | OK | Diperbaiki di v1.6.2 |

**Status Integritas Audit:** Seluruh 59 item telah terverifikasi memiliki model JSON valid, tekstur lengkap di disk, registrasi kreatif tab lengkap, dan entri terjemahan bilingual (`en_us.json` dan `id_id.json`) tanpa satupun yang hilang.

---

## 4. Tahapan Rencana Aksi (Action Steps)

### Fase 1: Perbaikan File Definisi Equipment JSON & Sanitasi BOM
1. **Hapus `humanoid_baby` dari Definisi Equipment JSON:**
   - Ubah `assets/pink_legacy/equipment/pink_legacy.json`: buang blok `"humanoid_baby"`, pertahankan hanya `"humanoid"` dan `"humanoid_leggings"`.
   - Ubah `assets/valentine/equipment/valentine.json`: buang blok `"humanoid_baby"`, pertahankan hanya `"humanoid"` dan `"humanoid_leggings"`.
   - Pastikan juga di `sakura-resourcepack/assets/pink_legacy/equipment/` jika ada.
2. **Sanitasi UTF-8 BOM pada 11 File Model Pink Legacy:**
   - Bersihkan header BOM 3-byte (`\xef\xbb\xbf`) pada `axe.json`, `battle_axe.json`, `halberd.json`, `hammer.json`, `hoe.json`, `key.json`, `pickaxe.json`, `shovel.json`, `spear.json`, `staff.json`, `sword.json` agar menjadi UTF-8 murni.

### Fase 2: Penguatan `EquipmentLayerRendererMixin` dengan Programmatic Fallback
1. **Bentuk Lapisan Tetap Anti-Gagal (`PINK_LEGACY_INFO` & `VALENTINE_INFO`):**
   ```java
   private static final EquipmentClientInfo PINK_LEGACY_INFO = new EquipmentClientInfo(Map.of(
       EquipmentClientInfo.LayerType.HUMANOID, List.of(new EquipmentClientInfo.Layer(Identifier.fromNamespaceAndPath("pink_legacy", "pink_legacy"))),
       EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS, List.of(new EquipmentClientInfo.Layer(Identifier.fromNamespaceAndPath("pink_legacy", "pink_legacy")))
   ));

   private static final EquipmentClientInfo VALENTINE_INFO = new EquipmentClientInfo(Map.of(
       EquipmentClientInfo.LayerType.HUMANOID, List.of(new EquipmentClientInfo.Layer(Identifier.fromNamespaceAndPath("valentine", "valentine"))),
       EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS, List.of(new EquipmentClientInfo.Layer(Identifier.fromNamespaceAndPath("valentine", "valentine")))
   ));
   ```
2. **Logika Resolusi Bertingkat pada Mixin Redirect:**
   - Jika `PinkLegacyArmorUtil.isPinkLegacyArmor(itemStack)`:
     - Ambil dari `manager.get(ModEquipmentAssets.PINK_LEGACY)`.
     - Jika kosong atau `MISSING`, fallback ke `PINK_LEGACY_INFO`.
   - Jika `ValentineArmorUtil.isValentineArmor(itemStack)`:
     - Ambil dari `manager.get(ModEquipmentAssets.VALENTINE)`.
     - Jika kosong atau `MISSING`, fallback ke `VALENTINE_INFO`.
   - Jika item adalah Sakura Hat atau Valentine Hat: kembalikan `EMPTY_EQUIPMENT_INFO` (supresi kubus helm vanilla).
   - Jika `assetKey` adalah `PINK_LEGACY` atau `VALENTINE` dan hasil manager kosong: kembalikan `PINK_LEGACY_INFO` / `VALENTINE_INFO`.
   - Jika lainnya: kembalikan `manager.get(assetKey)`.

### Fase 3: Modernisasi Data Pack Tags Minecraft 1.21.11
1. Buat direktori `data/minecraft/tags/item/` (format singular baku 1.21+).
2. Salin dan sinkronkan `swords.json`, `pickaxes.json`, `axes.json`, `shovels.json`, `hoes.json`.
3. Tambahkan tag zirah:
   - `head_armor.json`: mendaftarkan `pink_legacy_helmet` dan `valentine_helmet`.
   - `chest_armor.json`: mendaftarkan `pink_legacy_chestplate` dan `valentine_chestplate`.
   - `leg_armor.json`: mendaftarkan `pink_legacy_leggings` dan `valentine_leggings`.
   - `foot_armor.json`: mendaftarkan `pink_legacy_boots` dan `valentine_boots`.

### Fase 4: Bump Versi SemVer ke v1.6.2 & Kompilasi Lokal Workspace
1. Perbarui `VERSION` di root: `1.6.2`.
2. Perbarui `gradle.properties`: `mod_version=1.6.2`.
3. Perbarui `sakura-weapons/src/main/resources/fabric.mod.json`: deskripsi dan versi `1.6.2`.
4. Perbarui `PRD.md`: Bagian 21 (v1.6.2 Armor Texture Fix & Multi-Set Audit) dan header dokumen.
5. Jalankan kompilasi Gradle: `./gradlew clean build`.
6. Simpan output build eksklusif ke folder lokal: `Hasil mod/Takasha-1.6.2.jar`.
7. **Patuhi PRD Bagian 10:** Jangan pernah menyalin otomatis file JAR ke launcher atau direktori eksternal (`C:\Users\Administrator\AppData\Roaming\ModrinthApp\...`).

---

## 5. Kriteria Verifikasi Akhir (Checklist)

- [ ] File `pink_legacy.json` dan `valentine.json` tidak lagi mengandung `"humanoid_baby"`.
- [ ] 11 file model Pink Legacy bersih dari UTF-8 BOM.
- [ ] `EquipmentLayerRendererMixin` terkompilasi dan memiliki fallback `PINK_LEGACY_INFO` dan `VALENTINE_INFO`.
- [ ] Tag `data/minecraft/tags/item/` lengkap untuk senjata, peralatan, dan 4 jenis zirah.
- [ ] Task `./gradlew clean build` sukses 100% tanpa error kompilasi.
- [ ] File JAR `Hasil mod/Takasha-1.6.2.jar` ter-remap sempurna ke namespace intermediary dan Java 21 bytecode.
- [ ] Script audit otomatis (`scripts/audit_all_items.py`) mengonfirmasi seluruh 59 item valid.
