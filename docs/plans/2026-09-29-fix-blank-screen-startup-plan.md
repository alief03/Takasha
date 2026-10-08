# Planning Perbaikan: Layar Blank Saat Startup & Penanganan Konflik Data Server (ItemsAdder / Oraxen / Server Plugins)

> **Versi Target Mod:** Takasha v1.3.1  
> **Minecraft Versi:** Java 26.2 (Fabric Loader >= 0.19.3)  
> **Lingkup:** Perbaikan Blank Screen Startup + Kompatibilitas Menyeluruh Multiplayer Server Plugins (ItemsAdder, Oraxen, CIT Resewn, CustomModelData)

---

## 1. Analisis Masalah & Root Cause Diagnosis

### A. Blank / Black Screen Saat Startup Game
Dari investigasi file log runtime (`profiles\kaizenmc.id\logs\latest.log`), ditemukan crash fatal:
```text
Caused by: java.lang.NullPointerException: Components not bound yet
	at knot//net.minecraft.world.item.ItemStack.<init>(ItemStack.java:265)
	at knot//net.sakura.weapons.client.render.SakuraWingsFeatureRenderer.<init>(SakuraWingsFeatureRenderer.java:21)
	at knot//net.sakura.weapons.SakuraWeaponsClient.lambda$onInitializeClient$0(SakuraWeaponsClient.java:27)
	at knot//net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback.lambda$static$1(LivingEntityRenderLayerRegistrationCallback.java:55)
	at knot//net.minecraft.client.renderer.entity.EntityRenderers.redirect$bcf000$fabric-rendering-v1$createEntityRenderer(EntityRenderers.java:565)
```
* **Akar Masalah:** `SakuraWingsFeatureRenderer` menginisialisasi `new ItemStack(ModItems.SAKURA_WING)` secara eager pada atribut field. Saat callback `LivingEntityRenderLayerRegistrationCallback` dipanggil pada startup untuk membuat renderer entitas (`AvatarRenderer` & `ArmorStandRenderer`), sistem *Data Components* item Minecraft 26.2 (`Holder$Reference.components`) belum di-bind ke item registry. Hal ini memicu `NullPointerException` seketika, membatalkan pembuatan renderer, memicu reload pack darurat, dan menggantung pipeline rendering dalam kondisi blank screen.
* **Risiko Saat Server Resource Pack Didorong:** Masalah yang sama akan terjadi kembali ketika pemain masuk ke server yang menggunakan ItemsAdder/Oraxen karena server akan memaksa reload resource pack (`ClientboundResourcePackPushPacket` -> `reloadResourcePacks()`). Jika renderer entitas dibuat ulang atau diakses saat reload, layar akan langsung blank/crash lagi.

---

### B. Analisis Potensi Konflik dengan Server Plugin (ItemsAdder / Oraxen / Paper / Bukkit)
Server publik atau server multiplayer seperti `kaizenmc.id` sering menggunakan plugin seperti **ItemsAdder**, **Oraxen**, atau **CustomModelData (CMD)**. Berikut potensi konflik yang wajib diantisipasi:

1. **Konflik Double-Rendering & Z-Fighting pada Sayap/Kosmetik (Cosmetic Layer Collision):**
   * ItemsAdder sering mengubah item `minecraft:elytra` atau armor dada menjadi kosmetik sayap/zirah server menggunakan `CustomModelData` atau shader.
   * Jika `SakuraWingsFeatureRenderer` memaksakan render 3D model Sakura Wing pada semua Elytra bernama sakura, maka pada server yang sudah memiliki kosmetik ItemsAdder, sayap akan ter-render ganda (tumpuk 2 sayap sekaligus atau glitch tekstur).
   * **Solusi:** Tambahkan pengecekan **CustomModelData & ItemsAdder Data Component Guard**. Jika item dada memiliki `DataComponents.CUSTOM_MODEL_DATA` atau data NBT `itemsadder:id` yang bukan milik Takasha/Sakura, `SakuraWingsFeatureRenderer` harus secara cerdas mengalah (yield) dan membiarkan renderer server yang aktif.

2. **Ketidakcocokan Kata Kunci Anvil & Bahasa Klien (CIT Keyword Mismatch):**
   * Server dengan ItemsAdder/CIT mencocokkan nama item berdasarkan string persis (contoh: `ipattern:Sakura Katana*`).
   * Jika pemain menggunakan bahasa Indonesia (`id_id`), GUI Anvil sebelumnya memasukkan "Katana Sakura", yang **gagal memicu tekstur** pada konfigurasi CIT / plugin server.
   * **Solusi:** Pastikan tombol sidebar Anvil selalu memasukkan nama baku CIT bahasa Inggris yang valid (`displayNameEn`), dengan opsi menampilkan nama lokal di GUI sebagai label informatif.

3. **Deteksi Base Item untuk Server Tanpa Mod (Pure Client Interop):**
   * Di server Paper/Spigot dengan ItemsAdder, mod `sakura_weapons` tidak terpasang di sisi server. Pemain membuat senjata Takasha dengan cara memasukkan item vanilla (misal `Diamond Sword`) lalu mengubah namanya di Anvil.
   * Jika pemain memasukkan item yang salah (misal meletakkan `Stick` atau `Bow` untuk dibuat jadi `Sakura Katana`), server/CIT tidak akan mengubah teksturnya.
   * **Solusi:** Pada GUI `AnvilSideListWidget`, tambahkan informasi **Base Item Requirement** di tooltip (misal: `Item Dasar: Pedang Berlian / Netherite / Besi`) dan beri indikator visual jika item yang sedang diletakkan di slot input Anvil cocok atau tidak cocok.

4. **Deteksi Item Khusus Server (Server Custom Item Protection):**
   * Jika pemain tidak sengaja memasukkan item ItemsAdder yang sudah memiliki custom abilities/CMD ke Anvil lalu mengklik nama Takasha, data server bisa terganggu atau rename diblokir oleh anti-exploit server.
   * **Solusi:** Beri deteksi aman di widget: jika item di slot Anvil memiliki tag `itemsadder`, `oraxen`, atau custom NBT server, tampilkan badge peringatan pada tooltip: `⚠ Item Plugin Server Terdeteksi`.

5. **Protokol Jaringan Klien Murni (Zero Packet Desync):**
   * Mod Takasha harus bekerja 100% aman di server vanilla maupun server dengan anti-cheat/ItemsAdder.
   * Pengisian nama ke Anvil hanya boleh memodifikasi widget `EditBox` resmi vanilla tanpa mengirim paket kustom ilegal yang dapat memicu disconnect/kick.

---

## 2. Rencana Tindakan Perbaikan (Updated Action Plan)

### Fase 1: Perbaikan Blank Screen Startup & Safe Reload
- [ ] **1.1. Refactor `SakuraWingsFeatureRenderer.java`**:
  - Hapus inisialisasi eager `new ItemStack(ModItems.SAKURA_WING)` dari deklarasi field.
  - Implementasikan lazy-loader `getDisplayStack()` dengan pembungkus `try-catch (Throwable t)` yang mengembalikan `ItemStack.EMPTY` bila komponen belum siap.
  - Pastikan method `submit()` mengecek `chestItem.is(ModItems.SAKURA_WING)` terlebih dahulu sebelum mencoba membuat fallback stack.
- [ ] **1.2. Proteksi Dynamic Reload**:
  - Pastikan saat server ItemsAdder memicu `reloadResourcePacks()`, tidak ada state statis atau model resolver yang tertinggal dalam kondisi deadlock.

### Fase 2: Implementasi Server Data Conflict Guard (ItemsAdder & Oraxen)
- [ ] **2.1. Deteksi Konflik Kosmetik di `SakuraWingsFeatureRenderer`**:
  - Periksa apakah `chestItem` memiliki `DataComponents.CUSTOM_MODEL_DATA` atau komponen kustom dari plugin server (`custom_data` yang memuat namespace `itemsadder`, `oraxen`, atau `ia_gui`).
  - Jika item tersebut adalah kosmetik server yang memiliki CustomModelData aktif dan bukan item bawaan mod Takasha, skip rendering sayap mod untuk mencegah tumpang-tindih (double wings Z-fighting).
- [ ] **2.2. Standardisasi Nama CIT di `AnvilSideListWidget`**:
  - Pastikan method `applyItemNameToAnvil` selalu mengisikan format nama CIT standar (English Name) agar dikenali oleh server plugin dan CIT Resewn/OptiFine:
    ```java
    String citRenameString = entry.displayNameEn();
    editBox.setValue(citRenameString);
    ```
  - Tetap tampilkan bilingual di UI (English + Bahasa Indonesia) agar pengguna lokal paham.
- [ ] **2.3. Tooltip Cerdas: Base Item & Status Kompatibilitas**:
  - Pada tooltip item di sidebar Anvil, tambahkan:
    - `Base Items:` (contoh: `Diamond Sword, Netherite Sword, Iron Sword`)
    - `Status Server:` Menampilkan status `Kompatibel Server (ItemsAdder / CIT Mode)`
  - Deteksi apakah item di slot input Anvil saat ini valid untuk resep yang dipilih.

### Fase 3: Penguatan Safety di `AnvilItemCatalog`
- [ ] **3.1. Lazy Instantiation per Item**:
  - Semua entri item dibuat secara lazy saat pertama kali dibuka di GUI Anvil.
  - Fallback aman ke `Items.STICK` atau `Items.DIAMOND_SWORD` bila terjadi galat registry tanpa melempar exception ke game loop.
- [ ] **3.2. Caching Item Stack**:
  - Simpan instance `ItemStack` yang sudah berhasil dibuat ke dalam cache memori agar proses scroll tetap mulus 60 FPS tanpa alokasi objek berulang.

### Fase 4: Build, Deployment & Uji Multiplayer
- [ ] **4.1. Kompilasi Mod JAR**:
  - Jalankan `./gradlew.bat build` untuk memproduksi `Hasil mod/Takasha-1.3.1.jar`.
- [ ] **4.2. Deploy ke Profil Klien**:
  - Salin jar baru ke `C:\Users\Administrator\AppData\Roaming\ModrinthApp\profiles\kaizenmc.id\mods/`.
- [ ] **4.3. Verifikasi Pengujian**:
  - Uji 1: Startup Minecraft dari launcher -> Title Screen harus muncul tanpa blank screen.
  - Uji 2: Masuk ke server multiplayer -> Resource pack server ItemsAdder dapat terunduh tanpa freeze.
  - Uji 3: Buka Anvil di server -> Sidebar Anvil muncul di sebelah kanan, dapat di-scroll, dan ketika diklik nama terisi dengan benar.
  - Uji 4: Cek Sayap Sakura -> Tampil rapi di punggung tanpa merusak kosmetik server lain.

---

## 3. Matriks Kompatibilitas Sistem

| Lingkungan | Mekanisme Visual | Potensi Konflik | Mitigasi Takasha |
|---|---|---|---|
| **Singleplayer (Mod Aktif)** | Native Registry (`ModItems.*`) | Blank Screen saat Startup | Lazy loading di `SakuraWingsFeatureRenderer` |
| **Server dengan ItemsAdder** | Vanilla Item + CustomModelData / Server RP | Double Wings, Overlap Tekstur | Deteksi CMD & DataComponent Guard; yield render jika server cosmetic |
| **Server Paper/Spigot (CIT Mode)** | Vanilla Item + Rename Anvil | Nama tidak cocok jika bahasa ID | Force CIT standard name pada `applyItemNameToAnvil` |
| **Server Reload Resource Pack** | `reloadResourcePacks()` dinamis | Crash di tengah permainan | Isolasi model resolver & dynamic reload safety |
