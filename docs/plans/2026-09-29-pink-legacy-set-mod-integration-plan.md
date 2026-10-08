# Planning Pembaruan Mod Takasha: Integrasi Set Pink Legacy, Scrollable Anvil GUI & Branding Takasha

> **Nama Resmi Mod:** **Takasha** (Fabric Mod)  
> **Ikon Resmi Mod:** `D:\Mod Minecraft\weapon set\sakura\sakura-resourcepack\pack.png`  
> **Target Versi:** `1.3.1` (Fabric Mod & Resource Pack)  
> **Output Mod JAR:** `Hasil mod/Takasha-1.3.1.jar`  
> **Lingkungan Runtime:** Minecraft Java 26.2, Fabric Loader >= 0.19.3, Fabric API 0.161.0+26.2, Java 25  
> **Dokumen Rujukan:** [PRD.md](file:///d:/Mod%20Minecraft/weapon%20set/sakura/PRD.md) (Bagian 4.2, 5.2, 5.3, 8, 10, & 11)  
> **Status Dokumen:** Siap Eksekusi (Ready for Execution)  

---

## 1. Latar Belakang & Analisis Kebutuhan

Berdasarkan dokumen arsitektur [PRD.md](file:///d:/Mod%20Minecraft/weapon%20set/sakura/PRD.md) dan permintaan pengguna:
1. **Unifikasi Nama Proyek (Branding "Takasha"):**
   - Nama tampilan mod resmi di `fabric.mod.json` adalah **Takasha**.
   - Output JAR kompilasi resmi dinamai **`Takasha-1.3.1.jar`** dan diletakkan di folder `Hasil mod/`.
   - Menggunakan ikon resmi dari [sakura-resourcepack/pack.png](file:///d:/Mod%20Minecraft/weapon%20set/sakura/sakura-resourcepack/pack.png).
2. **Integrasi Set #2 (Pink Legacy) ke Mod Fabric:**
   - Resource Pack telah memiliki 46 properti CIT dan aset animasi Pink Legacy.
   - Mod Fabric perlu mendaftarkan 19 item native Pink Legacy (senjata tajam, tumpul, panjang, busur, perisai, alat pancing, tools, 4 potong armor, sayap punggung, dan kunci).
   - Arsitektur registrasi modular per set (`SakuraItems.java`, `PinkLegacyItems.java`, `ModItems.java`).
   - Menyediakan Creative Tab terpisah: **Sakura Arsenal** dan **Pink Legacy Arsenal**.
3. **Scrollable GUI di Samping Anvil (Fitur Interaktif):**
   - Di vanilla / resource pack, panduan rename hanya berupa gambar font glif statis yang tidak dapat di-scroll.
   - Pengguna meminta **GUI scrollable di samping GUI anvil** yang menampilkan **ikon kecil (16x16) mewakili nama setiap item**.
   - Diimplementasikan di sisi mod Fabric (`AnvilSideListWidget`) menggunakan `fabric-screen-api-v1` (`ScreenEvents.AFTER_INIT`).
   - Panel ini menyediakan daftar item lengkap dengan:
     - Ikon item kecil dinamis (`fakeItem`).
     - Nama item (bilingual).
     - Filter tab antar set (`Semua`, `Sakura`, `Pink Legacy`).
     - Scrollbar interaktif + mouse wheel scrolling.
     - Auto-fill kolom teks rename Anvil + copy ke clipboard saat baris item diklik.

---

## 2. Inventaris Item untuk Mod & Anvil Side-List

### 2.1 Set #1: Sakura Set (20 Items)
`sakura_katana`, `sakura_nodachi`, `sakura_dagger`, `sakura_spear`, `sakura_halberd`, `sakura_hammer`, `sakura_club`, `sakura_gauntlet`, `sakura_scythe`, `sakura_staff`, `sakura_bow`, `sakura_crossbow`, `sakura_shield`, `sakura_fishing_rod`, `sakura_pickaxe`, `sakura_axe`, `sakura_shovel`, `sakura_hoe`, `sakura_hat`, `sakura_wing`.

### 2.2 Set #2: Pink Legacy Set (19 Items)
| No | ID Item Mod (`sakura_weapons:`) | Nama EN | Nama ID | Kategori Archetype | Sifat & Mekanisme Khusus |
|:---|:---|:---|:---|:---|:---|
| 1 | `pink_legacy_sword` | Pink Legacy Sword | Pedang Pink Legacy | Blade / Sword | `sword(mat, 4.0f, -2.4f)`, partikel neon magenta on-hit |
| 2 | `pink_legacy_battle_axe` | Pink Legacy Battle Axe | Kapak Perang Pink Legacy | Heavy Weapon / Axe | `axe(mat, 7.0f, -3.1f)`, heavy sweep damage |
| 3 | `pink_legacy_spear` | Pink Legacy Spear | Tombak Pink Legacy | Spear / Polearm | `sword(mat, 4.5f, -2.6f)`, visual reach panjang |
| 4 | `pink_legacy_halberd` | Pink Legacy Halberd | Halberd Pink Legacy | Polearm / Axe | `axe(mat, 6.5f, -3.0f)`, hibrida kapak & tombak |
| 5 | `pink_legacy_hammer` | Pink Legacy Hammer | Palu Pink Legacy | Heavy Blunt / Hammer | Custom `PinkLegacyHammerItem`, AOE smash, Slowness, knockback & magenta explosion |
| 6 | `pink_legacy_staff` | Pink Legacy Staff | Tongkat Pink Legacy | Staff / Magic Weapon | `sword(mat, 3.5f, -2.2f)`, fast swing |
| 7 | `pink_legacy_bow` | Pink Legacy Bow | Busur Pink Legacy | Ranged Bow | Custom `PinkLegacyBowItem`, 3 tahap animasi tarikan (`bow_0`, `bow_1`, `bow_2`) |
| 8 | `pink_legacy_shield` | Pink Legacy Shield | Perisai Pink Legacy | Defense Shield | Custom `PinkLegacyShieldItem`, model aktif `shield_blocking` saat menangkis |
| 9 | `pink_legacy_fishing_rod` | Pink Legacy Fishing Rod | Alat Pancing Pink Legacy | Utility Fishing | `FishingRodItem`, model `fishing_rod_cast` saat dilempar |
| 10 | `pink_legacy_pickaxe` | Pink Legacy Pickaxe | Beliung Pink Legacy | Tool / Pickaxe | `pickaxe(mat, 1.5f, -2.8f)`, tier efisiensi di atas diamond |
| 11 | `pink_legacy_axe` | Pink Legacy Axe | Kapak Pink Legacy | Tool / Axe | `axe(mat, 5.5f, -3.0f)` |
| 12 | `pink_legacy_shovel` | Pink Legacy Shovel | Sekop Pink Legacy | Tool / Shovel | `shovel(mat, 1.5f, -3.0f)` |
| 13 | `pink_legacy_hoe` | Pink Legacy Hoe | Cangkul Pink Legacy | Tool / Hoe | `hoe(mat, -3.0f, 0.0f)` |
| 14 | `pink_legacy_helmet` | Pink Legacy Helmet | Helm Pink Legacy | Armor (Head) | `humanoidArmor(mat, ArmorType.HELMET)`, defense: 3, toughness: 3.0 |
| 15 | `pink_legacy_chestplate` | Pink Legacy Chestplate | Zirah Pink Legacy | Armor (Chest) | `humanoidArmor(mat, ArmorType.CHESTPLATE)`, defense: 8, toughness: 3.0 |
| 16 | `pink_legacy_leggings` | Pink Legacy Leggings | Celana Pink Legacy | Armor (Legs) | `humanoidArmor(mat, ArmorType.LEGGINGS)`, defense: 6, toughness: 3.0 |
| 17 | `pink_legacy_boots` | Pink Legacy Boots | Sepatu Pink Legacy | Armor (Boots) | `humanoidArmor(mat, ArmorType.BOOTS)`, defense: 3, toughness: 3.0 |
| 18 | `pink_legacy_wings` | Pink Legacy Wings | Sayap Pink Legacy | Cosmetic Backpiece | `equippable(CHEST)`, 3D native torso feature renderer di punggung pemain |
| 19 | `pink_legacy_key` | Pink Legacy Key | Kunci Pink Legacy | Utility | `stacksTo(64)`, decorative/roleplay key |

---

## 3. Rencana Arsitektur Teknis

### 3.1 Unifikasi Branding & Output JAR
- `sakura-weapons/gradle.properties`:
  - `archives_base_name=Takasha`
  - `mod_version=1.3.1`
- `sakura-weapons/src/main/resources/fabric.mod.json`:
  - `"name": "Takasha"`
  - `"icon": "assets/sakura_weapons/icon.png"` (sinkron dengan `pack.png`)
- Gradle build task otomatis menghasilkan `Takasha-1.3.1.jar` dan disalin ke `Hasil mod/Takasha-1.3.1.jar`.

### 3.2 Skema Registrasi Modular
- `SakuraItems.java`: Mengisolasi 20 item Sakura Set.
- `PinkLegacyItems.java`: Mengisolasi 19 item Pink Legacy Set.
- `ModItems.java`: Facade sentral yang menginisialisasi seluruh set.
- `ModItemGroups.java`: Menyediakan `SAKURA_TAB` dan `PINK_LEGACY_TAB`.

### 3.3 Sistem Armor & Material 26.2
- `PinkLegacyToolMaterial.java` (Durability: 2000, Speed: 9.0f, Damage: 4.5f).
- `PinkLegacyArmorMaterial.java` (`ResourceKey<EquipmentAsset>` = `sakura_weapons:pink_legacy`).
- `assets/sakura_weapons/equipment/pink_legacy.json` memetakan humanoid layer 1 & 2 ke tekstur di `assets/pink_legacy/textures/entity/equipment/`.

### 3.4 Feature Renderer Sayap di Torso Pemain
- `ModWingsFeatureRenderer.java`:
  - Ter-attach pada `model.body` (tulang belakang torso).
  - Mendeteksi slot dada: `SAKURA_WING` merender model sayap sakura, `PINK_LEGACY_WINGS` merender model sayap pink legacy.

### 3.5 Arsitektur Scrollable Anvil GUI Side-List (`AnvilSideListWidget`)
- **Hook Lifecycle:** `ScreenEvents.AFTER_INIT` memeriksa apakah `screen instanceof AnvilScreen`.
- **Penempatan:**
  - Posisi horizontal: `x = (scaledWidth - 176) / 2 + 176 + 4`. Jika melebihi lebar layar, digeser ke sebelah kiri `(scaledWidth - 176) / 2 - width - 4`.
  - Posisi vertikal: `y = (scaledHeight - 166) / 2`.
  - Dimensi: `width = 135`, `height = 166`.
- **Desain UI:**
  - Header: Tab filter `[Semua]` `[Sakura]` `[Pink Legacy]`.
  - Body: Scrollable container yang merender baris item (tinggi per baris 20px).
  - Setiap baris merender:
    - Ikon kecil item (16x16) via `GuiGraphicsExtractor.fakeItem(itemStack, x, y)`.
    - Teks nama item via `GuiGraphicsExtractor.text(font, name, x + 20, y + 5, color)`.
    - Hover effect: latar belakang highlight pink transparan + tooltip Minecraft native.
  - Interaksi Klik:
    - Mencari `EditBox` pada `AnvilScreen` dan mengisi teks rename dengan nama item yang diklik.
    - Menyalin nama item ke clipboard sistem (`Minecraft.getInstance().keyboardHandler.setClipboard`).
    - Memutar efek suara klik UI.
  - Scroll Interaction:
    - Mouse wheel scrolling via `mouseScrolled` & `ScreenMouseEvents.allowMouseScroll`.
    - Scrollbar vertikal draggable di sisi kanan panel.

---

## 4. Rincian Rencana Kerja (Task Breakdown)

### Tahap 0: Branding & Konfigurasi Build Takasha
- [ ] **Task 0.1:** Salin ikon dari `sakura-resourcepack/pack.png` ke `sakura-weapons/src/main/resources/assets/sakura_weapons/icon.png`.
- [ ] **Task 0.2:** Perbarui `fabric.mod.json`: nama menjadi **Takasha**, deskripsi multi-set.
- [ ] **Task 0.3:** Perbarui `gradle.properties`: `archives_base_name=Takasha`, `mod_version=1.3.1`.

### Tahap 1: Material & Custom Items Pink Legacy
- [ ] **Task 1.1:** Buat `PinkLegacyToolMaterial.java` di package `net.sakura.weapons.item`.
- [ ] **Task 1.2:** Buat `PinkLegacyArmorMaterial.java` di package `net.sakura.weapons.item`.
- [ ] **Task 1.3:** Buat `PinkLegacyHammerItem.java` dengan efek AOE smash + particle burst magenta.
- [ ] **Task 1.4:** Buat `PinkLegacyBowItem.java` dan `PinkLegacyShieldItem.java`.

### Tahap 2: Registrasi Item & Creative Tab
- [ ] **Task 2.1:** Refactor registrasi Sakura ke `SakuraItems.java`.
- [ ] **Task 2.2:** Buat `PinkLegacyItems.java` mendaftarkan 19 item Pink Legacy.
- [ ] **Task 2.3:** Perbarui `ModItems.java` sebagai facade sentral.
- [ ] **Task 2.4:** Daftarkan `PINK_LEGACY_TAB` di `ModItemGroups.java`.
- [ ] **Task 2.5:** Perbarui inisialisasi di `SakuraWeaponsMod.java`.

### Tahap 3: Client Rendering Sayap Punggung
- [ ] **Task 3.1:** Implementasikan `ModWingsFeatureRenderer.java` untuk mendukung `SAKURA_WING` dan `PINK_LEGACY_WINGS`.
- [ ] **Task 3.2:** Daftarkan renderer di `SakuraWeaponsClient.java`.

### Tahap 4: Scrollable Anvil GUI Side-List Widget
- [ ] **Task 4.1:** Buat kelas katalog `AnvilItemCatalog.java` yang mengumpulkan daftar item per set beserta `ItemStack` dan nama bilingualnya.
- [ ] **Task 4.2:** Buat widget UI `AnvilSideListWidget.java` (extends `AbstractWidget` / `Renderable` / `GuiEventListener`):
  - Rendering background panel & border aksen pink.
  - Tab switcher kategori set (`Semua`, `Sakura`, `Pink Legacy`).
  - Scrollable viewport merender item icon 16x16 (`fakeItem`) dan label teks.
  - Hover highlight & native item tooltip.
  - On-click handler: autofill Anvil `EditBox` & clipboard copy.
  - Mouse scroll listener & vertical draggable scrollbar.
- [ ] **Task 4.3:** Daftarkan event hook di `SakuraWeaponsClient.java` via `ScreenEvents.AFTER_INIT` dan `ScreenMouseEvents.allowMouseScroll`.

### Tahap 5: Asset Integration (Model, Tekstur, Equipment, 26.2 Items JSON)
- [ ] **Task 5.1:** Salin model JSON Pink Legacy ke `src/main/resources/assets/pink_legacy/models/item/`.
- [ ] **Task 5.2:** Salin tekstur PNG & `.mcmeta` ke `src/main/resources/assets/pink_legacy/textures/item/`.
- [ ] **Task 5.3:** Siapkan direktori `equipment/` dan buat `assets/sakura_weapons/equipment/pink_legacy.json` serta tekstur entity layer.
- [ ] **Task 5.4:** Buat 19 file definisi item Minecraft 26.2 di `assets/sakura_weapons/items/pink_legacy_*.json`.
- [ ] **Task 5.5:** Perbarui file bahasa `en_us.json` dan `id_id.json`.
- [ ] **Task 5.6:** Daftarkan item ke tag vanilla (swords, tools, armor).

### Tahap 6: Build, Validasi & Rilis
- [ ] **Task 6.1:** Kompilasi mod dengan `./gradlew.bat build`.
- [ ] **Task 6.2:** Pastikan output JAR bernama `Takasha-1.3.1.jar` dan tersalin ke `Hasil mod/Takasha-1.3.1.jar`.
- [ ] **Task 6.3:** Perbarui PRD checklist status fitur.

---

## 5. Kriteria Keberhasilan (Verification Checklist)

1. **Branding Takasha:** Mod bernama **Takasha** dengan icon `pack.png` di game.
2. **Kompilasi Sukses:** Gradle build exit code 0 tanpa error.
3. **Output Terstandarisasi:** Terbentuk file `Hasil mod/Takasha-1.3.1.jar`.
4. **Scrollable Anvil GUI Sidebar:** Saat membuka Anvil, panel samping muncul di kanan Anvil, memuat tab set, dapat di-scroll lancar dengan mouse wheel, menampilkan icon kecil 16x16 di setiap item, dan mengklik item mengisi teks rename Anvil.
5. **Set Pink Legacy Lengkap:** 19 item Pink Legacy tersedia di tab kreatif *Pink Legacy Arsenal*, model 3D, animasi, armor, dan sayap berfungsi normal.
