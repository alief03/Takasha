# Planning Perbaikan Sakura Wings (Native Torso Layer — Tanpa EMF & ETF)

> **Status:** Draft / Ready for Review  
> **Target Versi:** `1.3.0` (Fabric Mod & Resource Pack)  
> **Lingkungan:** Minecraft Java 26.2 (Fabric Loader >= 0.19.3, Fabric API) + CIT Resewn / Vanilla Fallback  

---

## 1. Analisis Screenshot & Root Cause Kegagalan CEM (EMF/ETF)

Berdasarkan screenshot in-game terbaru yang dikirimkan:
1. **Kemiringan Diagonal ~15°:** Seluruh model sayap miring ke samping kanan bawah.
2. **Pergeseran Pusat (Side-Shift):** Poros tengah ranting kayu tergeser jauh ke arah kiri bahu/tangan pemain ($X \approx -8.5\text{ px}$ dari tulang belakang).
3. **Sayap Kanan Hilang / Patah:** Model sayap kanan tidak muncul atau terpotong akibat pemisahan part/anchoring pada satu sisi sayap saja.

### Mengapa Pendekatan CEM Elytra (EMF/ETF) Harus Ditinggalkan?
1. **Arsitektur `ElytraModel` Vanilla Mojang:**  
   Class `ElytraModel.setupAnim()` menerapkan kalkulasi rotasi dinamis bawaan:
   - Pitch: $+15^\circ$ ($+0.2618\text{ rad}$)
   - Roll: $-15^\circ$ (sayap kiri) dan $+15^\circ$ (sayap kanan)
   - Pivot dasar sayap berada di pundak kiri ($X = +5.0$) dan pundak kanan ($X = -5.0$).
2. **Keterbatasan Engine CEM EMF:**  
   Animasi reset `"left_wing.rx": 0` pada layer equipment diabaikan atau ditimpa oleh render loop Minecraft. Upaya *counter-rotation* matematis bersifat rapuh (*brittle*): posisi sayap langsung berantakan saat pemain bergerak, berjalan, jongkok (sneak), melompat, atau terbang.
3. **Ketergantungan Eksternal yang Merepotkan:**  
   Pengguna diwajibkan memasang mod Entity Model Features (EMF) dan Entity Texture Features (ETF). Tanpa kedua mod tersebut, sayap hanya tampak sebagai elytra abu-abu atau bahkan transparan kosong (`empty_elytra.png`).

---

## 2. Arsitektur Baru: Solusi Bersih Tanpa EMF & ETF

Kita beralih ke arsitektur **Dual-Clean Architecture**:

```
                       ┌─────────────────────────────────────────────────────┐
                       │            Pemain Menggunakan Sakura Wings          │
                       └──────────────────────────┬──────────────────────────┘
                                                  │
                 ┌────────────────────────────────┴────────────────────────────────┐
                 ▼                                                                 ▼
   [ Mode 1: Fabric Mod (Utama) ]                                    [ Mode 2: Resource Pack (Fallback) ]
   • Mod: sakura-weapons                                             • Vanilla Minecraft / CIT Resewn
   • Layer: SakuraWingsFeatureRenderer                               • Slot: Head / Carved Pumpkin / Helmet
   • Anchor: model.body (Torso/Spine)                                • Transform: "head" di wing.json
   • Posisi: X=0 presisi di tulang belakang                          • Offset: -15.75 turun ke punggung
   • Gerak: Alami ikut napas & crouching                             • Dependency: 0 mod (100% Vanilla / CIT)
   • Dependency: 0 EMF, 0 ETF (Hanya Fabric API)
```

### A. Pilar 1: Fabric Mod Native Torso Layer (`sakura-weapons`)
- Memanfaatkan **Fabric Rendering API v1** (`LivingEntityFeatureRendererRegistrationCallback`), tanpa perlu mixin berbahaya.
- Menambahkan **`SakuraWingsFeatureRenderer`** pada `PlayerEntityRenderer` dan `ArmorStandEntityRenderer`.
- Anchor rendering langsung pada **`model.body`** (torso badan pemain):
  - **Sumbu $X = 0$:** Tepat di tengah tulang belakang pemain (sempurna simetris).
  - **Kemiringan $0^\circ$:** Tegak lurus, tidak terpengaruh rotasi 15° sayap elytra.
  - **Crouching Alami:** Ketika pemain jongkok/sneak, `model.body` otomatis memiringkan badan ke depan (~$28.6^\circ$). Sayap ikut memiringkan diri secara realistis mengikuti punggung.
  - **Model Utuh 3D:** Merender seluruh 18 elemen `sakura:item/wing` secara utuh (kedua sayap kiri dan kanan sekaligus bersama bunga sakura beranimasi).
- Item **`ModItems.SAKURA_WING`** dikonfigurasi `.equippable(EquipmentSlot.CHEST)` agar pemain bisa langsung memakainya di slot dada/sayap.
- Mendukung juga item vanilla Elytra/Chestplate yang dinamai `Sakura Wings` atau `Sayap Sakura` di anvil.

### B. Pilar 2: Resource Pack Vanilla Fallback (Head Slot)
- Untuk pemain yang hanya menggunakan resource pack tanpa mod Fabric:
- Model `sakura:item/wing` (`wing.json`) sudah memiliki display transform `"head"` yang dikalibrasi oleh pembuat model asli (EliteCreatures):
  - `rotation: [-146.25, -88.82, -146.26]`
  - `translation: [-1.0, -15.75, 5.5]` (turun ke punggung)
  - `scale: [1.46133, 1.46133, 1.46133]`
- Pemain cukup memakai **Carved Pumpkin** (atau Helm via CIT Resewn) bernama `Sakura Wings` / `Sayap Sakura`.
- Dengan overlay `pumpkinblur.png` transparan 100%, pemain mendapatkan tampilan sayap di punggung tanpa mod apapun.

### C. Pilar 3: Decommissioning & Cleanup EMF/ETF CEM
- Hapus semua file `.jem` elytra yang bermasalah di:
  - `sakura-resourcepack/assets/minecraft/optifine/cem/elytra*.jem`
  - `sakura-resourcepack/assets/minecraft/emf/cem/elytra*.jem`
- Hapus `empty_elytra.png` dari CIT elytra agar elytra vanilla pemain lain tidak menjadi transparan/rusak.
- Nonaktifkan skrip generator CEM `build_sakura_wings_etf_emf.py`.

---

## 3. Rencana Eksekusi Langkah-demi-Langkah (Task Breakdown)

### Phase 1: Implementasi Native Renderer di Fabric Mod (`sakura-weapons`)

- [ ] **Task 1.1: Konfigurasi Item `sakura_wing` Menjadi Equippable**
  - File: `sakura-weapons/src/main/java/net/sakura/weapons/registry/ModItems.java`
  - Ubah definisi `SAKURA_WING`:
    ```java
    public static final Item SAKURA_WING = register("sakura_wing", Item::new,
        new Item.Properties().stacksTo(1).equippable(EquipmentSlot.CHEST));
    ```
  - Ini memungkinkan item `sakura_wing` langsung dipakai di slot chestplate (seperti Elytra) di Minecraft 26.2.

- [ ] **Task 1.2: Buat Class `SakuraWingsFeatureRenderer`**
  - File baru: `sakura-weapons/src/main/java/net/sakura/weapons/client/render/SakuraWingsFeatureRenderer.java`
  - Inherit dari `FeatureRenderer<T, M>` (di mana `T extends LivingEntity`, `M extends EntityModel<T>`).
  - Logika render:
    1. Cek apakah entity memakai `ModItems.SAKURA_WING` di slot `EquipmentSlot.CHEST`, ATAU memakai Elytra bernama `Sakura Wings` / `Sayap Sakura`.
    2. Matriks diposisikan pada part torso:
       ```java
       if (this.getContextModel() instanceof PlayerModel<?> playerModel) {
           playerModel.body.translateAndRotate(matrices);
       }
       ```
    3. Terapkan offset presisi ke permukaan belakang baju:
       - `matrices.translate(0.0, -0.25, 0.15)`
       - Rotasi penyesuaian sumbu model 3D Blockbench
    4. Render ItemStack `sakura_wing` menggunakan `ItemRenderer.renderItem(...)` dengan context `ItemDisplayContext.FIXED` atau `HEAD`.

- [ ] **Task 1.3: Daftarkan Feature Renderer di `SakuraWeaponsClient`**
  - File: `sakura-weapons/src/main/java/net/sakura/weapons/SakuraWeaponsClient.java`
  - Gunakan Fabric API:
    ```java
    LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
        if (entityRenderer instanceof PlayerEntityRenderer playerRenderer) {
            registrationHelper.register(new SakuraWingsFeatureRenderer<>(playerRenderer, context.getItemRenderer()));
        }
        if (entityRenderer instanceof ArmorStandEntityRenderer armorStandRenderer) {
            registrationHelper.register(new SakuraWingsFeatureRenderer<>(armorStandRenderer, context.getItemRenderer()));
        }
    });
    ```

- [ ] **Task 1.4: Kalibrasi Visual Transform Matriks Torso**
  - Tentukan nilai translasi dan rotasi optimal agar pusat batang sakura menempel pas di punggung tanpa clipping ke dalam tubuh pemain.

- [ ] **Task 1.5: Kompilasi & Validasi Build Mod**
  - Jalankan `./gradlew build` di direktori `sakura-weapons`.
  - Pastikan output `sakura-weapons-1.3.0.jar` terkompilasi tanpa error.

---

### Phase 2: Pembersihan Resource Pack (Hapus Residu EMF/ETF)

- [ ] **Task 2.1: Hapus File CEM Elytra yang Rusak**
  - Hapus semua file `.jem` elytra di `sakura-resourcepack/assets/minecraft/optifine/cem/` dan `sakura-resourcepack/assets/minecraft/emf/cem/`:
    - `elytra.jem`, `elytra2.jem`, `elytra_sakura.jem`
    - `player_elytra*.jem`, `player_slim_elytra*.jem`, `armor_stand_elytra*.jem`
    - Subfolder `player/`, `player_slim/`, `armor_stand/`
  - Hapus folder `emf/` jika sudah tidak diperlukan.

- [ ] **Task 2.2: Hapus CIT Elytra & `empty_elytra.png`**
  - Hapus properti CIT `type=elytra`:
    - `sakura_wings_elytra.properties`
    - `sayap_sakura_elytra.properties`
    - `sakura_wings_elytra_png.properties`
    - `sayap_sakura_elytra_png.properties`
  - Hapus file gambar `empty_elytra.png` dari `textures/entity/` dan folder CIT.
  - Pertahankan `sakura_wings.properties` dan `sayap_sakura.properties` untuk `type=item` (inventory, item frame, anvil).

- [ ] **Task 2.3: Verifikasi Mode Fallback Vanilla Carved Pumpkin & Helmet**
  - Pastikan `assets/minecraft/items/carved_pumpkin.json` tetap mengarahkan nama `Sakura Wings` & `Sayap Sakura` ke `sakura:item/wing`.
  - Pastikan `pumpkinblur.png` transparan tetap ada.

- [ ] **Task 2.4: Validasi Integritas Resource Pack**
  - Jalankan `python validate_cit_and_items.py`.
  - Pastikan seluruh 30 item CIT sinkron sempurna.

---

### Phase 3: Dokumentasi, SemVer & Packaging Rilis

- [ ] **Task 3.1: Pembaruan `PRD.md`**
  - Dokumentasikan arsitektur baru: Sakura Wings dirender via Native Fabric Torso Feature Renderer (bebas EMF/ETF).
  - Catat slot pemakaian: Slot Dada (Chest) pada Mod Fabric, dan Slot Kepala (Head/Carved Pumpkin) pada Vanilla.

- [ ] **Task 3.2: Bump Semantic Version ke `1.3.0`**
  - Update `VERSION` ke `1.3.0` (peningkatan MINOR besar karena restrukturisasi sistem rendering sayap).
  - Update versi di `gradle.properties` mod menjadi `1.3.0`.

- [ ] **Task 3.3: Kompilasi & Build Paket Rilis Akhir**
  - Jalankan `python build.py` untuk menghasilkan `Takasha-1.3.0.zip`.
  - Jalankan `./gradlew build` untuk menghasilkan `sakura-weapons-1.3.0.jar`.
  - Verifikasi kedua file rilis di root direktori workspace.

---

## 4. Kriteria Keberhasilan (Acceptance Criteria)

| Parameter | Kondisi Sukses |
|-----------|----------------|
| **Ketergantungan Mod** | 0% ketergantungan pada Entity Model Features (EMF) dan Entity Texture Features (ETF). |
| **Simetri & Posisi ($X$)** | Model sayap tepat di tengah tulang belakang ($X = 0$), tidak melenceng ke kiri bahu. |
| **Kemiringan (Tilt)** | Berdiri tegak ($0^\circ$), tidak ada miring diagonal $15^\circ$ bawaan elytra. |
| **Keutuhan Sayap** | Kedua sayap (kiri & kanan) dan ranting tengah tampil utuh 100% bersama animasinya. |
| **Animasi Tubuh** | Sayap otomatis ikut menunduk saat pemain jongkok (sneak) dan berayun halus saat berjalan. |
| **Integritas Elytra Vanilla** | Tekstur Elytra vanilla biasa tidak lagi terpengaruh atau menjadi transparan. |
| **Build Stability** | `./gradlew build` dan `python validate_cit_and_items.py` lulus 100% tanpa error. |
