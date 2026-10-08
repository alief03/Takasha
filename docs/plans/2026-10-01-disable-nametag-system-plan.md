# Rencana Implementasi (Revisi): Sistem Disable Nametag Pemain & Mannequin
**Mod:** Takasha (`sakura_weapons`) | **Versi Target:** Minecraft Java 26.2 (Fabric Mod-Only)  
**Lingkungan:** OpenJDK 25 LTS, Bytecode 69.0, Fabric Loader `0.19.3+`  
**Target File Output:** `Hasil mod/Takasha-1.8.0+26.2.jar`

---

## 1. Pemahaman & Penyesuaian Kebutuhan Pengguna

Berdasarkan masukan dan klarifikasi pengguna:
1. **Posisi Pemain (Bukan Admin / Non-OP):**
   - Pengguna bermain di server multiplayer sebagai **pemain biasa (regular player)**, bukan sebagai administrator/OP server.
   - Fitur harus dapat menyembunyikan nametag **seluruh pemain lain (dan diri sendiri)** di layar pengguna **tanpa memerlukan izin OP/admin server**, dan bekerja di server mana pun (Vanilla, Fabric, Paper, Spigot, Realms, LAN, maupun Singleplayer).
2. **Perintah Sederhana & Ramah Pemain:**
   - Perintah disederhanakan menjadi langsung: **`/takasha nametag`** (tanpa pembagian rumit `client/server`).
   - Eksekusi `/takasha nametag` langsung men-toggle ON/OFF secara instan.
   - Opsi pendukung: `/takasha nametag on`, `/takasha nametag off`, dan `/takasha nametag status`.
   - Menggunakan **Client Command Fabric API v2** sehingga diproses 100% di client pengguna, tidak memicu pesan error *"Unknown command"* atau *"You do not have permission"* dari server.
3. **Keybind Kustom:**
   - Default keybind diatur ke **Unbound** (tidak mengikat tombol keyboard bawaan, sehingga tidak bertabrakan dengan tombol kontrol pemain lain). Pemain bebas mengaturnya sendiri di menu `Options` -> `Controls` -> `Key Binds` -> kategori `Takasha`.
4. **Culling Nametag Mannequin / NPC 100% Tuntas:**
   - Memastikan mannequin Takasha saat di-set "Nama: OFF" benar-benar hilang nametagnya, termasuk saat kursor crosshair diarahkan langsung ke mannequin.

---

## 2. Arsitektur Teknis (Client-Driven Zero-Permission Architecture)

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                   TAKASHA PLAYER-CENTRIC NAMETAG ARCHITECTURE                    │
└────────────────────────────────────────┬─────────────────────────────────────────┘
                                         │
    ┌────────────────────────────────────┴────────────────────────────────────┐
    ▼                                                                         ▼
[NAMETAG PEMAIN (SERVER & LOCAL)]                         [NAMETAG MANNEQUIN / NPC]
- Perintah Client: /takasha nametag [on|off|status]      - SynchedEntityData (DATA_SHOW_NAME)
- Keybind Unbound di Controls                            - Tombol GUI "Nama: ON / OFF"
- Culling Render via AvatarRendererMixin                 - Quick-Toggle via Takasha Wand
- Berlaku di SEMUA Server Multiplayer (Non-OP!)          - Crosshair-immune Culling
- Kompatibel dengan Replay Mod & Sinematik                (TakashaNpcRenderer.shouldShowName)
```

---

## 3. Rincian Komponen & Alur Kerja

### A. Fitur 1: Nametag Pemain (Player Nametags) di Server
1. **Sistem Perintah Client (`ClientCommandRegistrationCallback`):**
   - Didaftarkan sebagai **Fabric Client Command** (`net.fabricmc.fabric.api.client.command.v2.ClientCommands`).
   - Format:
     - `/takasha nametag` -> Toggle status visibilitas (Jika Aktif -> Mati, Jika Mati -> Aktif).
     - `/takasha nametag on` -> Mengaktifkan nametag pemain (terlihat normal).
     - `/takasha nametag off` -> Menonaktifkan nametag seluruh pemain (tersembunyi).
     - `/takasha nametag status` -> Mengecek status saat ini.
   - **Keunggulan:** Berjalan di level client sehingga **bisa dipakai di server publik manapun** meskipun server tersebut tidak memasang mod Takasha dan pemain tidak memiliki izin admin (Permission Level 0 / Non-OP).
2. **Keybind Pintas (Unbound):**
   - Terdaftar di `SakuraWeaponsClient` dengan kategori `key.categories.sakura_weapons` ("Takasha").
   - Nama entri: `key.sakura_weapons.toggle_player_nametags`.
   - Default tombol: `InputConstants.UNKNOWN` (Unbound), agar pemain dapat menentukan tombol sendiri sesuai kenyamanan tanpa konflik.
   - Diperiksa setiap client tick (`ClientTickEvents.END_CLIENT_TICK`).
3. **Pemberitahuan Action Bar & Suara Lembut:**
   - Saat status berganti, muncul teks informatif di atas hotbar (Action Bar):
     - `§c[Takasha] Nametag Seluruh Pemain: DINONAKTIFKAN`
     - `§a[Takasha] Nametag Seluruh Pemain: DIAKTIFKAN`
   - Pemain langsung mengetahui statusnya tanpa membuka chat.
4. **Rendering Suppression Mixin (`AvatarRendererMixin`):**
   - Menginjeksi `AvatarRenderer.shouldShowName(LivingEntity, double, CallbackInfoReturnable<Boolean>)` atau `AvatarRenderer.shouldShowName(Avatar, double, ...)`.
   - Logika:
     ```java
     if (TakashaNametagConfig.isPlayerNametagDisabled()) {
         cir.setReturnValue(false); // Batalkan render nametag semua pemain seketika
     }
     ```
   - Seluruh player model yang dirender (termasuk pemain lain dan avatar diri sendiri di pandangan F5) tidak akan menampilkan floating nametag.
   - Tidak merusak fungsionalitas lain (skin, armor, item pegangan tetap utuh dan sempurna).
5. **Persistensi Pengaturan Client:**
   - Status pilihan disimpan di file lokal `.minecraft/config/takasha_client.json` sehingga ketika menutup dan membuka kembali Minecraft, pengaturan tetap diingat.

---

### B. Fitur 2: Nametag Mannequin / NPC
1. **Pencegahan Bypass Crosshair Vanilla:**
   - Secara default Minecraft vanilla tetap memunculkan nametag jika entitas memiliki custom name dan crosshair mengarah tepat ke entitas (`crosshairPickEntity == entity`).
   - Di `TakashaNpcRenderer`:
     ```java
     @Override
     protected boolean shouldShowName(TakashaNpcEntity entity, double distanceSq) {
         if (!entity.shouldShowName()) {
             return false; // Mutlak tersembunyi 100%, crosshair tidak akan memunculkan teks
         }
         return super.shouldShowName(entity, distanceSq);
     }
     ```
2. **Kustomisasi di GUI Mannequin:**
   - Tombol toggle `Nama: ON` / `Nama: OFF` yang sudah ada di GUI layar mannequin Takasha langsung mengubah status sinkronisasi `DATA_SHOW_NAME`.
3. **Wand Quick-Toggle:**
   - Sneak (Jongkok) + Klik Kanan dengan `Takasha Mannequin Wand` pada mannequin akan men-toggle nametag mannequin secara instan dengan notifikasi overlay di atas hotbar.

---

## 4. Tahapan Rencana Kerja (Work Breakdown Structure)

| Tahap | Aktivitas | File Terkait |
|---|---|---|
| **Tahap 1** | **Culling Mutlak Nametag Mannequin**<br>- Perbarui `TakashaNpcRenderer.shouldShowName` untuk memastikan 100% culling saat `!entity.shouldShowName()`. | `TakashaNpcRenderer.java` |
| **Tahap 2** | **Konfigurasi & State Manager Client**<br>- Buat `TakashaNametagConfig.java` untuk menyimpan flag boolean `playerNametagsDisabled` dan menyimpan ke file JSON lokal secara otomatis. | `TakashaNametagConfig.java` |
| **Tahap 3** | **Client Keybind & Client Command**<br>- Daftarkan keybind `key.sakura_weapons.toggle_player_nametags` (default Unbound).<br>- Daftarkan Fabric Client Command `/takasha nametag [on\|off\|status]` via `ClientCommandRegistrationCallback`.<br>- Tambahkan action bar feedback saat toggle. | `SakuraWeaponsClient.java`<br>`TakashaClientCommands.java` |
| **Tahap 4** | **AvatarRenderer Mixin untuk Culling Player**<br>- Buat `AvatarRendererMixin.java` yang menargetkan `AvatarRenderer`.<br>- Intersep method `shouldShowName` dan return `false` jika nametag pemain dinonaktifkan.<br>- Daftarkan mixin di `sakura_weapons.mixins.json` (bagian client). | `AvatarRendererMixin.java`<br>`sakura_weapons.mixins.json` |
| **Tahap 5** | **Lokalisasi (Localization)**<br>- Tambahkan string terjemahan bahasa Inggris dan Indonesia untuk nama keybind, pesan action bar, dan deskripsi status command di `en_us.json` dan `id_id.json`. | `en_us.json`<br>`id_id.json` |
| **Tahap 6** | **Build & Verifikasi Kompilasi**<br>- Jalankan `gradlew.bat build` untuk memproduksi `Hasil mod/Takasha-1.8.0+26.2.jar`.<br>- Verifikasi kompilasi bytecode OpenJDK 25 / Minecraft 26.2.<br>- Pastikan file ZIP Resource Pack dan folder `Hasil RP/` tetap tidak tersentuh. | `build/libs/`, `Hasil mod/` |

---

## 5. Ringkasan Pengalaman Pengguna (UX Walkthrough)

1. **Sebagai Pemain Biasa di Server Mana Pun:**
   - Pemain masuk ke server multiplayer (misal server survival, roleplay, atau event teman).
   - Pemain mengetik:
     ```
     /takasha nametag
     ```
   - Di atas hotbar langsung muncul notifikasi:
     `[Takasha] Nametag Seluruh Pemain: DINONAKTIFKAN`
   - Seketika seluruh nametag pemain lain di sekitar dan nametag diri sendiri menghilang dari layar.
   - Pemain dapat merekam video sinematik dengan Replay Mod atau mengambil screenshot bersih tanpa floating text.
   - Jika ingin memunculkan kembali, cukup ketik `/takasha nametag` lagi atau tekan tombol keybind yang telah diatur.
2. **Pada Mannequin:**
   - Saat membuat mannequin, klik tombol `Nama: OFF` di GUI, maka nama mannequin tidak akan pernah muncul sama sekali (bahkan ketika kursor crosshair diarahkan ke mannequin).
