# Planning: Sakura Wings Dekoratif pada Slot Elytra (Tetap Elytra)

> **Dokumen:** Rencana Implementasi Sakura Wings Dekoratif pada Slot Elytra  
> **Tanggal:** 2026-09-26 (Direvisi berdasarkan arahan pengguna)  
> **Status:** Selesai Diimplementasikan & Terverifikasi (Completed & Verified)  
> **Target:** `Takasha` (Resource Pack) & `sakura-weapons` (Fabric Mod Minecraft 26.2)  
> **Prinsip Utama:** **Tetap menggunakan item/slot Elytra**, namun tampilan visual model sayap sakura berfungsi sebagai **dekorasi punggung utuh & anggun** (tidak perlu animasi mengepak/melipat fungsional sayap elytra yang membelah model).

---

## 1. Klarifikasi & Arahan Desain

### 1.1 Arahan Pengguna: "Tetap Elytra Saja"
* Item dasar yang digunakan **tetap Elytra** (dipasang di slot dada/elytra `EquipmentSlot.CHEST`).
* Di Anvil GUI side list, basis item tetap **`Wings (Ely/Pap)`**.
* Pemain **tidak dipindahkan ke helm atau labu ukir di kepala**.

### 1.2 "Dekoratif Saja, Tidak Perlu Sefungsional Elytra"
* **Bukan Sayap Mekanis/Biologis:** Model asli Sakura Wings buatan EliteCreatures adalah ornamen ranting pohon sakura berbunga, jimat kertas, dan kelopak animasi. Ini adalah sebuah karya seni dekorasi punggung (*decorative backpiece* / *back ornament*).
* **Masalah Elytra Fungsional Terdahulu:**
  - Minecraft Elytra secara default membagi sayap menjadi 2 part bergerak (`left_wing` dan `right_wing`) yang masing-masing berputar dinamis saat istirahat (roll $-15^\circ$, pitch $+15^\circ$), mengepak saat terbang, dan terdorong oleh hembusan angin.
  - Ketika kita memaksakan Sakura Wings masuk ke model fungsional elytra vanilla, model terpotong/hilang sebelah, miring diagonal $15^\circ$, dan tergeser jauh ke arah bahu kiri ($X \approx -8.5\text{ px}$).
* **Solusi Dekoratif Murni:**
  - Model Sakura Wings ditampilkan secara **utuh (single unified model)** di punggung pemain: kedua sayap kiri-kanan, ranting pohon, dan kelopak animasi sakura tampil lengkap.
  - Model menempel tegak lurus ($0^\circ$ tilt) tepat di tengah tulang belakang ($X = 0$).
  - Model **tidak perlu mengepak atau terbelah secara fungsional layaknya sayap elytra burung**. Model cukup bertengger stabil dan anggun sebagai hiasan punggung, baik saat pemain berdiri, berjalan, jongkok (*crouch/sneak*), maupun saat meluncur di udara.

---

## 2. Arsitektur Solusi (Dua Pilar)

```
┌─────────────────────────────────────────────────────────────────────────┐
│              SAKURA WINGS (TETAP ELYTRA - DEKORATIF UTUH)               │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
       ┌─────────────────────────────┴─────────────────────────────┐
       ▼                                                           ▼
【Pilar 1: Fabric Mod (sakura-weapons)】                  【Pilar 2: Resource Pack (Takasha)】
 ├── Item SAKURA_WING: .equippable(EquipmentSlot.CHEST)    ├── Base Item Anvil: Tetap Elytra (dan Paper)
 ├── Native Feature Renderer: SakuraWingsFeatureRenderer   ├── Anvil Side List: Tetap Wings (Ely/Pap)
 ├── Anchor: model.body (Torso/Spine Pemain)               ├── CEM Elytra: Model utuh terpusat di X=0
 ├── Posisi: X=0 presisi di tengah tulang belakang         ├── Bersih dari rotasi dinamis sayap pecah
 ├── Gerak Alami: Ikut napas & sneaking                    └── Kompatibilitas OptiFine & CIT Resewn
 └── Support: Mod SAKURA_WING & Vanilla Elytra Rename
```

### 2.1 Pilar 1: Fabric Mod Native Torso Feature Renderer (`sakura-weapons`)
Pendekatan paling bersih, presisi, dan bebas bug untuk pemain Fabric:
1. **Slot Pemakaian:**
   `SAKURA_WING` dikonfigurasi `.equippable(EquipmentSlot.CHEST)` di [ModItems.java](file:///d:/Mod%20Minecraft/weapon%20set/sakura/sakura-weapons/src/main/java/net/sakura/weapons/registry/ModItems.java):
   ```java
   public static final Item SAKURA_WING = register("sakura_wing", Item::new,
       new Item.Properties().stacksTo(1).equippable(EquipmentSlot.CHEST));
   ```
2. **`SakuraWingsFeatureRenderer`:**
   - Didaftarkan melalui Fabric API (`LivingEntityFeatureRendererRegistrationCallback`) pada `PlayerEntityRenderer` dan `ArmorStandEntityRenderer`.
   - Mengambil anchor rendering langsung dari **`playerModel.body`** (tulang badan/torso pemain):
     - **Tengah Punggung ($X = 0$):** Sempurna di tengah tulang belakang.
     - **Tegak ($0^\circ$):** Bebas dari rotasi miring $15^\circ$ bawaan part `left_wing` elytra.
     - **Sneak Alami:** Ketika pemain jongkok (*sneak*), torso badan pemain otomatis memiringkan diri ke depan (~$28.6^\circ$). Sayap sakura otomatis ikut menunduk secara alami mengikuti punggung.
   - Merender model item `sakura:item/wing` utuh 100% (dua sisi sayap + ranting tengah + kelopak animasi) di punggung.
   - Mendeteksi apakah pemain memakai `ModItems.SAKURA_WING` ATAU memakai `Items.ELYTRA` yang dinamai *"Sakura Wings"* / *"Sayap Sakura"*.
3. **0 Dependensi Tambahan:** Tidak membutuhkan mod Entity Model Features (EMF) ataupun Entity Texture Features (ETF).

---

### 2.2 Pilar 2: Resource Pack (`Takasha`)
Untuk pemain yang menggunakan Resource Pack saja (dengan CIT Resewn / OptiFine / EMF):
1. **Basis Item di Anvil:**
   - **Tetap `elytra`** dan `paper` (untuk item frame).
   - Di [create_side_list.py](file:///d:/Mod%20Minecraft/weapon%20set/sakura/create_side_list.py): Kembalikan hint ke **`Wings (Ely/Pap)`**.
2. **Penyederhanaan Model CEM `elytra.jem`:**
   - Alih-alih membagi model sayap sakura menjadi dua part yang mengepak dan berputar dinamis secara fungsional (yang menyebabkan sayap patah atau miring), seluruh 18 elemen model `wing.json` disatukan sebagai satu kesatuan dekoratif (*unified decorative model*).
   - Diletakkan di tengah sumbu tulang belakang ($X = 0$) dengan kompensasi sudut statis, sehingga sayap tampak utuh dan melekat kokoh di punggung tanpa terbelah saat terbang.

---

## 3. Rencana Kerja Bertahap (Action Plan)

### Fase 1: Kembalikan Konfigurasi Resource Pack & Anvil GUI ke Elytra
- [ ] Edit `create_side_list.py`:
  - Kembalikan tuple: `("Wings", "Ely/Pap", (220, 175, 255))`
  - Kembalikan compact summary: `("Wings", "Ely/Pap")`
- [ ] Jalankan `python create_side_list.py` untuk meregenerasi tekstur Anvil GUI.
- [ ] Pastikan `sakura_wings.properties` dan `sayap_sakura.properties` memiliki `items=elytra paper`.

### Fase 2: Implementasi Fabric Mod Native Torso Feature Renderer
- [ ] Konfigurasi `ModItems.java`:
  - Pasang `.equippable(EquipmentSlot.CHEST)` pada `SAKURA_WING`.
- [ ] Buat class `SakuraWingsFeatureRenderer.java`:
  - Paket: `net.sakura.weapons.client.render`
  - Hubungkan ke `playerModel.body`.
  - Matriks render: offset translasi ke permukaan belakang torso dan rotasi menghadap ke luar punggung.
  - Dukung baik item `ModItems.SAKURA_WING` maupun vanilla `Items.ELYTRA` yang dinamai Sakura Wings.
- [ ] Daftarkan Feature Renderer di `SakuraWeaponsClient.java` via Fabric API `LivingEntityFeatureRendererRegistrationCallback`.
- [ ] Compile mod via `./gradlew build`.

### Fase 3: Kalibrasi Visual & Penyelarasan Posisi
- [ ] Uji posisi render pada karakter model Slim (Alex) dan Standard (Steve) serta Armor Stand.
- [ ] Pastikan ornamen sakura menempel pas di punggung tanpa tembus (*clipping*) ke dalam tubuh pemain.
- [ ] Pastikan saat pemain jongkok (*sneak*), sayap bergerak selaras dengan punggung.

### Fase 4: Validasi & Packaging Rilis
- [ ] Jalankan `python validate_cit_and_items.py`.
- [ ] Jalankan `python build.py` untuk mengemas resource pack terbaru.
- [ ] Update `PRD.md` mencatat arsitektur dekoratif sayap berbasis Elytra.

---

## 4. Kriteria Keberhasilan

| No | Parameter | Ekspektasi Hasil |
|---|---|---|
| 1 | **Basis Item** | Pemain tetap menggunakan **Elytra** (atau item mod `sakura_wing` di slot chest/elytra) |
| 2 | **Anvil GUI** | Side list Anvil dengan jelas menampilkan `Wings (Ely/Pap)` |
| 3 | **Tampilan Model** | Sayap tampil utuh (kedua sisi sayap + ranting tengah lengkap bersama animasi kelopak) |
| 4 | **Sifat Dekoratif** | Model sayap kokoh menempel di tengah punggung ($X = 0$), tidak terbelah atau miring aneh |
| 5 | **Postur Gerak** | Sayap otomatis ikut menunduk secara realistis saat pemain jongkok (*crouch/sneak*) |
| 6 | **Build Status** | Kompilasi Fabric Mod (`./gradlew build`) sukses tanpa error |
