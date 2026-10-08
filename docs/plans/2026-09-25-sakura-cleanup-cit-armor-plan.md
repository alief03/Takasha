# Sakura Weapon Set - Cleanup, Penghapusan Model Chest & Full Support CIT Resewn Continuation Plan

> **Catatan Kebijakan Deployment:** Sesuai instruksi pengguna, **TIDAK ADA deployment / penyalinan ke direktori profil Modrinth eksternal** (`C:\Users\Administrator\AppData\Roaming\ModrinthApp\...`). Seluruh output packaging dan build tetap berada di dalam workspace lokal (`d:\Mod Minecraft\weapon set\sakura\`).

> **Goal:** 
> 1. Membersihkan seluruh file temporary dan skrip bekas testing di root workspace dan modul mod `sakura-weapons`.
> 2. Menghapus tuntas model dan referensi **Sakura Chest** dari resource pack, GUI Anvil, dan mod Fabric.
> 3. Mengonfigurasi dukungan penuh mod **CIT Resewn Continuation** ([modrinth.com/mod/cit-resewn-continuation](https://modrinth.com/mod/cit-resewn-continuation)) untuk seluruh item (termasuk model 3D berbasis armor: Sakura Hat pada Helm dan Sakura Wings pada Elytra).
> 4. Mengubah target dependensi modul Fabric ke **Minecraft 26.2** dan **Fabric Loader 0.19.3** serta menambahkan Mixin agar model 3D Hat langsung tampil di kepala saat helm dipakai.
> 5. Melakukan packaging rilis lokal di root workspace.

---

## 1. Analisis Teknis & Detail Permasalahan

### Issue 1: File Sampah & Scratch Test Files Membebani Repositori
* **Kondisi:** Selama proses penyesuaian sudut pandang, rotasi item frame, dan debugging display transform sebelumnya, tercipta puluhan file dump class, file test python sekali pakai, screenshot crop, dan file scratch Java.
* **Daftar File yang Harus Dibersihkan:**
  1. **Root Directory (`d:/Mod Minecraft/weapon set/sakura/`):**
     - Class dump javap: `ConditionItems.class`, `ConditionNBT.class`, `TypeArmor.class`, `TypeItem.class`, `tmp.class`
     - File crop / gambar pengujian: `anvil_header_test.png`, `anvil_slots_crop.png`, `avatarHead.png`, `dropped_item_crop.png`, `gui_crop.png`, `hotbar_crop.png`
     - Skrip python uji coba sekali pakai: `inspect_issues.py`, `inspect_zip_display.py`, `read_steps.py`, `fix_tool_grips.py`, `update_grip_transforms.py`, `update_items_cases.py`, `verify_all.py`
  2. **Mod Module (`d:/Mod Minecraft/weapon set/sakura/sakura-weapons/`):**
     - Scratch Java & compiled test class: `CheckBounds.class`, `CheckBounds.java`, `FindFloorVertical.class`, `FindFloorVertical.java`, `ReadPlayer.class`, `ReadPlayer.java`, `VerifyJoml.class`, `VerifyJoml.java`
     - Crop test images: `compare_crops.png`, `crop1.png`, `crop2.png`
     - Skrip math & test python: `compare_rot.py`, `find_floor_vertical.py`, `migrate_assets.py`, `parse_level.py`, `read_frame.py`, `read_player.py`, `test_all_walls.py`, `test_ang.py`, `test_flip.py`, `test_frame_axes.py`, `test_katana_pos.py`, `test_rot.py`, `test_tip_touch.py`, `test_vectors.py`, `test_wall.py`, `test_wall_compare.py`, `test_wall_positions.py`, `test_world.py`, `verify_models.py`
     - File planning usang di dalam folder mod: `flip-weapon-handle-up-plan.md`, `resourcepack-anvil-rename-plan.md`

---

### Issue 2: Penghapusan Tuntas Model Sakura Chest
* **Kondisi:** Sakura Chest sebelumnya dibuat sebagai dekorasi/furniture item peti, namun pengguna meminta untuk menghapusnya secara tuntas dari weapon set.
* **Langkah Pembersihan:**
  1. **Pada Resource Pack (`sakura-resourcepack`):**
     - Hapus model 3D: `assets/sakura/models/item/chest.json`
     - Hapus definisi item vanilla: `assets/minecraft/items/chest.json`
     - Hapus konfigurasi CIT:
       - `assets/minecraft/optifine/cit/sakura/sakura_chest.properties`
       - `assets/minecraft/citresewn/cit/sakura/sakura_chest.properties`
     - Hapus pemetaan case `"Sakura Chest"` di:
       - `assets/minecraft/items/paper.json`
       - `assets/minecraft/models/item/paper.json`
     - Hapus entri Sakura Chest dari dokumentasi `README.txt`.
  2. **Pada Mod Fabric (`sakura-weapons`):**
     - Hapus model item: `assets/sakura_weapons/models/item/chest.json` dan `sakura_chest.json`
     - Hapus item definition: `assets/sakura_weapons/items/sakura_chest.json`
     - Hapus registrasi di `ModItems.java` (hapus field `SAKURA_CHEST`).
     - Hapus entri dari creative tab di `ModItemGroups.java`.
     - Hapus key translasi `item.sakura_weapons.sakura_chest` pada `en_us.json` dan `id_id.json`.
  3. **Pada GUI Anvil Side List:**
     - Perbarui `create_side_list.py`: hapus entri `Chest` dari daftar menu tools dan summary. Perbaiki hint `Spear`.
     - Re-render tekstur `anvil_side_list.png` dan perbarui sisi kanan `anvil.png`.

---

### Issue 3: Dukungan Penuh Mod CIT Resewn Continuation
* **Arsitektur CIT Resewn Continuation:**
  - Mod `citresewn-continuation-1.2.2-fork.13+26.2` memindai root folder `mcpatcher`, `optifine`, dan `citresewn`.
  - Mod mendukung tipe item (`type=item`) dengan model 3D Blockbench (`model=sakura:item/<name>`).
  - Untuk pencocokan item di Minecraft 26.2, mod membaca Data Components melalui `ConditionComponents`:
    - `components.minecraft:custom_name=ipattern:*<Item Name>*` mencocokkan nama Anvil secara modern tanpa memicu `warnLegacyNbt`.
    - `nbt.display.Name=ipattern:*<Item Name>*` ditambahkan sebagai fallback kompatibilitas engine legacy OptiFine.
  - Untuk item armor 3D:
    - **Sakura Hat:** Menggunakan `type=item`, `items=paper iron_helmet diamond_helmet netherite_helmet golden_helmet chainmail_helmet leather_helmet turtle_helmet carved_pumpkin`, `model=sakura:item/hat`.
    - **Sakura Wings:** Menggunakan `type=item`, `items=elytra paper`, `model=sakura:item/wing`.
    - Model `hat.json` diperbaiki untuk mendaftarkan `"missing": "sakura:item/sakura_texture"` pada blok `"textures"` dan memetakan 40 face `#missing` ke `#0`.
    - Display transform `"head"` pada `hat.json` dan `wing.json` dikalibrasi presisi agar pas saat dikenakan di slot kepala / armor.
  - Sub-item overrides:
    - Bow: `model.bow_pulling_0`, `model.bow_pulling_1`, `model.bow_pulling_2`.
    - Shield: `model.shield_blocking`.
    - Fishing rod: `model.fishing_rod_cast`.
  - Log Warning Registry:
    - `minecraft:spear` bukan item vanilla; hapus dari `sakura_spear.properties` agar log game bersih.
  - Dua Folder CIT:
    - Sinkronkan 100% seluruh 20 file `.properties` di `optifine/cit/sakura/` dan `citresewn/cit/sakura/`.
  - Koeksistensi Vanilla:
    - Resource pack tetap menyertakan `assets/minecraft/items/*.json` vanilla sehingga pemain tanpa mod tetap mendapatkan model custom.

---

### Issue 4: Kompatibilitas Mod Sakura Weapons ke Fabric Loader 0.19.3
* **Kondisi:**
  Game menolak memuat mod `sakura_weapons` jika versi Fabric Loader pengguna adalah 0.19.3 karena hard dependency `loader_version=0.19.5`.
* **Solusi:**
  - Ubah `loader_version=0.19.3` di `gradle.properties`.
  - Ubah `"fabricloader": ">=0.19.3"` di `fabric.mod.json`.
  - Tambahkan client mixin `HumanoidArmorLayerMixin` pada `sakura-weapons`: saat pemain memakai helm yang di-rename "Sakura Hat", sembunyikan rendering zirah biped vanilla agar `CustomHeadLayer` langsung me-render model 3D `hat.json` melingkari kepala.
  - Recompile mod Fabric menggunakan `./gradlew build`.

---

## 2. Rencana Tahapan Eksekusi

### Task 1: Pembersihan Seluruh File Bekas Test & Scratch Files
* **Target Files:**
  - Root: dump class (`ConditionItems.class`, dll.), crop test images, script python temporer.
  - Mod `sakura-weapons`: scratch class Java, crop images, 19 python math scripts, markdown plan usang.
* **Langkah:** Jalankan script pembersih file atau perintah hapus file target.

### Task 2: Penghapusan Total Model & Referensi Sakura Chest
* **Target Files:**
  - Resource pack: `assets/sakura/models/item/chest.json`, `assets/minecraft/items/chest.json`, `sakura_chest.properties` (optifine & citresewn), `paper.json`, `README.txt`.
  - Mod: `chest.json`, `sakura_chest.json` (models & items), `ModItems.java`, `ModItemGroups.java`, `en_us.json`, `id_id.json`.
* **Langkah:** Hapus file-file di atas, bersihkan selector Chest dari `paper.json`, dan hapus pendaftaran Java.

### Task 3: Konfigurasi Penuh Dukungan Mod CIT Resewn Continuation pada Resource Pack
* **Target Files:**
  - Seluruh file `.properties` di `assets/minecraft/citresewn/cit/sakura/` dan `assets/minecraft/optifine/cit/sakura/`
  - `assets/sakura/models/item/hat.json`
* **Langkah:**
  1. Daftarkan `components.minecraft:custom_name=ipattern:*...*` dan `nbt.display.Name=ipattern:*...*` ke semua 20 properties files.
  2. Konfigurasi `sakura_hat.properties` dengan daftar item helm lengkap, carved_pumpkin, dan paper.
  3. Konfigurasi `sakura_wings.properties` dengan elytra dan paper.
  4. Hapus `spear` dari `sakura_spear.properties`.
  5. Perbaiki tekstur `#missing` di `hat.json` dan pastikan display transform `"head"` valid.
  6. Sinkronkan isi kedua folder CIT.

### Task 4: Target Fabric Loader 0.19.3 & Head Render Mixin pada Mod Sakura Weapons
* **Target Files:**
  - `sakura-weapons/gradle.properties`
  - `sakura-weapons/src/main/resources/fabric.mod.json`
  - `sakura-weapons/src/main/resources/sakura_weapons.mixins.json`
  - `sakura-weapons/src/main/java/net/sakura/weapons/mixin/client/HumanoidArmorLayerMixin.java`
* **Langkah:**
  1. Set `loader_version=0.19.3` di `gradle.properties`.
  2. Set `"fabricloader": ">=0.19.3"` di `fabric.mod.json`.
  3. Daftarkan mixin configuration di `fabric.mod.json` dan buat `HumanoidArmorLayerMixin`.
  4. Jalankan `./gradlew build` dan pastikan build sukses.

### Task 5: Pembaruan GUI Anvil Side List
* **Target Files:**
  - `create_side_list.py`
  - `sakura-resourcepack/assets/minecraft/textures/gui/container/anvil.png`
  - `sakura-resourcepack/assets/sakura/textures/gui/anvil_side_list.png`
* **Langkah:**
  1. Edit `create_side_list.py`: hapus baris `Chest` dan sesuaikan hint `Spear`.
  2. Jalankan `python create_side_list.py` untuk me-render ulang tekstur panel sisi Anvil.
  3. Verifikasi tampilan GUI Anvil bersih dari item Chest.

### Task 6: Packaging Local Workspace (Tanpa Deployment ke Profil Modrinth)
* **Target Files:**
  - `sakura-resourcepack.zip` (di root workspace)
  - `sakura-weapons/build/libs/sakura-weapons-1.0.0.jar` (dan root workspace)
* **Langkah:**
  1. Kemas folder `sakura-resourcepack` menjadi file zip `sakura-resourcepack.zip` di root repositori.
  2. Simpan file JAR hasil build mod Fabric di `sakura-weapons/build/libs/` dan salin ke root workspace.
  3. **Strict Policy:** TIDAK ada file yang disalin ke profil Modrinth eksternal.

---

## 3. Kriteria Verifikasi Sukses

- [ ] Folder root dan folder `sakura-weapons` bersih dari seluruh file temporary, crop screenshot, dan scratch script.
- [ ] Model dan referensi Sakura Chest terhapus 100% dari resource pack (models, items, properties, lang, README, GUI).
- [ ] Model dan referensi Sakura Chest terhapus 100% dari source code mod `sakura-weapons`.
- [ ] Konfigurasi CIT Resewn Continuation (`optifine/cit/` & `citresewn/cit/`) valid, bebas warning registry `spear`.
- [ ] Mod `sakura-weapons` berhasil di-build menargetkan **Minecraft 26.2** dan **Fabric Loader 0.19.3**.
- [ ] Mod tidak lagi mengalami crash/error `HARD_DEP fabricloader >= 0.19.5`.
- [ ] Model 3D topi Sakura (`Sakura Hat`) dan sayap Sakura (`Sakura Wings`) terkonfigurasi presisi dengan display transform `"head"`.
- [ ] File `sakura-resourcepack.zip` dan `sakura-weapons-1.0.0.jar` ter-package dan tersimpan rapi di root workspace lokal tanpa menyentuh folder profil Modrinth.
