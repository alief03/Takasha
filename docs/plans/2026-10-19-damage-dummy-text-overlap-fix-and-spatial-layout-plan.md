# Rencana Kerja: Perbaikan Teks Bertumpuk (Overlapping) pada Target Dummy & Penyelarasan PRD

> **Status Dokumen:** Menunggu Persetujuan Pengguna  
> **Target Semantic Version:** Takasha-RPG `v1.3.1+26.2` (Patch Release)  
> **Mod Utama (`sakura-weapons`):** Terkunci Baku pada `v2.5.0` (DILARANG MODIFIKASI)  
> **Target Output Rilis:** `Hasil mod/Takasha-RPG-1.3.1+26.2.jar`  
> **Dokumen PRD Terkait:** [`PRD.md`](file:///d:/Mod%20Minecraft/weapon%20set/sakura/PRD.md)

---

## 1. Analisis Akar Masalah (Root Cause Analysis dari Screenshot Pengguna)

Berdasarkan screenshot yang dilampirkan oleh pengguna, terjadi fenomena teks bertumpuk parah (*heavy overlapping / ghosting text*) pada Target Dummy:

```
[ TARGET DUMMY ] ❤ HP: ∞ ❤ 1000/1000  <-- (HUD Mod Eksternal seperti Jade/WTHIT)
─────────────────────────────────────
[ TARGET DUMMY ] ❤ HP: ∞              <-- (TextDisplay Line 1)
DPS: 13.64 || Last: 7.0 / 11.0        <-- (DUPLIKASI 2 TextDisplay saling menimpa!)
        182.5                         <-- (Teks Total bertumpuk)
[Status Efek: Tidak Ada Efek / ...]    <-- (Teks status bertumpuk dua layer)
    -7.0
    -1.0
    -1.0                              <-- (Indikator Damage melayang naik lurus
    -1.0                                   menembus dan menabrak billboard di atasnya!)
```

### 1.1 Duplikasi Entitas `Display.TextDisplay` pada Chunk/World Reload
1. Di Minecraft modern (1.19.4+ / 1.21+ / 26.2), `Display.TextDisplay` adalah **entitas dunia nyata** (`net.minecraft.world.entity.Display.TextDisplay`).
2. Saat pemain menyimpan dunia, keluar-masuk (relog), atau berpindah dimensi, entitas `TextDisplay` yang sudah di-spawn otomatis disimpan ke file region `.mca` chunk oleh Minecraft.
3. Namun di [`DamageDummyEntity.java`](file:///d:/Mod%20Minecraft/weapon%20set/sakura/takasha-rpg/src/main/java/net/takasha/rpg/entity/DamageDummyEntity.java), UUID dari `overheadDisplay` **TIDAK PERNAH disimpan ke NBT** (`addAdditionalSaveData`/`readAdditionalSaveData`).
4. Saat chunk dimuat kembali:
   - Minecraft memuat entitas `TextDisplay` lama dari file save.
   - Minecraft memuat entitas `DamageDummyEntity`, namun field `this.overheadDisplay` bernilai `null`!
   - Pada tick ke-0 atau ke-4 di `updateOverheadDisplay()`, kode mendeteksi `this.overheadDisplay == null` lalu memanggil:
     ```java
     this.overheadDisplay = new Display.TextDisplay(EntityTypes.TEXT_DISPLAY, level);
     level.addFreshEntity(this.overheadDisplay);
     ```
   - **Akibat Fatal:** Tercipta entitas `TextDisplay` **KEDUA** di posisi koordinat yang persis sama $(x, y + 2.25, z)$!
   - Entitas lama terus merender teks lama, sedangkan entitas baru merender teks baru dengan latar belakang hitam yang sama, sehingga huruf-huruf `DPS:`, angka damage `Last: 7.0` dan `Last: 11.0`, serta baris status saling menimpa (*double vision*).

### 1.2 Entitas Yatim Piatu (*Orphan Display*) yang Tidak Pernah Dihapus
1. Jika pemain menjalankan perintah `/rpg dummy remove` atau `/dummy clear`, perintah hanya memanggil `d.discard()` pada `DamageDummyEntity`.
2. Jika referensi Java `overheadDisplay` sudah terputus (akibat relog atau despawn), entitas `TextDisplay` di dunia tidak terhapus dan melayang abadi di koordinat tersebut.
3. Saat dummy baru dipasang di lokasi yang sama atau berdekatan, dummy baru membuat `TextDisplay` tambahan di atas `TextDisplay` lama yang masih tertinggal.

### 1.3 Trayektori Indikator Damage Melayang Menabrak Billboard Overhead
1. Di `spawnFloatingIndicator`, teks damage (seperti `-7.0`, `-1.0`) di-spawn pada:
   `offsetY = 1.6 + (random - 0.5) * 0.3` (ketinggian sekitar $1.45 - 1.75$) dengan deviasi horizontal sempit ($X/Z \pm 0.3$).
2. Di `tickFloatingIndicators`, setiap tick posisinya dinaikkan lurus ke atas:
   `ind.display().setPos(..., ind.display().getY() + 0.035, ...)`.
3. Dalam 25 tick ($25 \times 0.035 = 0.875$ blok), teks melayang naik dari $Y=1.6$ menuju **$Y=2.475$**.
4. Posisi billboard overhead berada di **$Y=2.25$**.
5. **Akibat Fatal:** Seluruh angka damage melayang lurus ke atas menembus dan berhenti tepat di dalam kotak billboard overhead! Saat menggunakan Crossbow Virus IV yang memicu Poison dan Wither berkali-kali per detik, tercipta kolom tumpukan angka merah `-1.0`, `-1.0`, `-1.0` yang menutupi informasi statistik DPS dan status.

### 1.4 Tabrakan Header dengan HUD Mod Klien (Jade / WTHIT / ToroHealth / Neat)
1. Di konstruktor `DamageDummyEntity`:
   `this.setCustomName(Component.literal("§6§l[ TARGET DUMMY ] §a❤ HP: ∞"));`
2. Walaupun `this.setCustomNameVisible(false);` telah dipanggil, mod client HUD tetap membaca `entity.getCustomName()` dan merender nameplate bar di atas kepala dummy:
   `[ TARGET DUMMY ] ❤ HP: ∞ ❤ 1000/1000` dengan bar hijau.
3. Karena `overheadDisplay` juga merender judul `[ TARGET DUMMY ] ❤ HP: ∞` pada Line 1, terjadi duplikasi judul ganda tepat di atas billboard.

---

## 2. Arsitektur Solusi & Rencana Perbaikan Komprehensif

### A. Garansi Billboard Tunggal (*Single Billboard Guarantee & Orphan Purge*)
1. **Penyimpanan UUID di NBT:**
   - Di `addAdditionalSaveData`: Simpan UUID `overheadDisplay` ke NBT (`output.putUUID("OverheadDisplayUUID", this.overheadDisplay.getUUID())`).
   - Di `readAdditionalSaveData`: Baca UUID tersebut (`this.savedOverheadUuid = input.getUUIDOr("OverheadDisplayUUID", null)`).
2. **Pembersihan Orphan Real-Time (`purgeOrphanDisplays`):**
   - Di `updateOverheadDisplay()`, sebelum membuat atau memperbarui teks:
     Pindai seluruh entitas `Display.TextDisplay` dalam radius 2.5 blok dari dummy.
     Jika entitas tersebut adalah `overheadDisplay` milik dummy ini (dicocokkan via UUID tersimpan atau tag `takasha_dummy_overhead_<dummyUUID>`), gunakan kembali entitas tersebut tanpa membuat baru.
     Seluruh entitas `Display.TextDisplay` liar/bekas sebelumnya yang berada di area kepala dummy ($Y \ge \text{dummy.getY()} + 1.8$) dan bukan merupakan indikator melayang aktif **WAJIB DI-DISCARD SEKETIKA**.
3. **Pembersihan Bersih saat Hapus Dummy:**
   - Di `DamageDummyEntity.remove()`: Buang `overheadDisplay` dan seluruh `indicators`.
   - Di `TakashaRpgCommands.executeRemoveDummies()`: Cari dan discard seluruh `Display.TextDisplay` dalam radius 5 blok dari pemain untuk memastikan tidak ada teks melayang yang tertinggal.

### B. Pemisahan Spasial Indikator Damage Melayang (*Side-Drift Arc & DoT Separation*)
1. **Spawn Menyamping (Dispersi Radial):**
   - Angka damage tidak lagi di-spawn di tengah kepala/badan dummy.
   - Damage diposisikan menyamping ke kiri ($X - 0.75$) atau ke kanan ($X + 0.75$) secara bergantian (staggered) atau acak pada radius $0.7 - 0.9$ blok dari titik pusat dummy.
2. **Trayektori Lengkung Melayang Keluar (*Outward Arc Drift*):**
   - Alih-alih melayang lurus ke atas ke tengah billboard, indikator diberikan kecepatan horizontal keluar ($dx = \pm 0.015$, $dz = \pm 0.015$) dan kenaikan vertikal pelan ($dy = +0.02$).
   - Angka melayang menjauh ke samping dummy dan menghilang (fading out) dalam 20 tick ($1.0$ detik), sehingga **100% tidak akan pernah menabrak atau menutupi kotak overhead display**.
3. **Kategori Khusus Damage over Time (Poison, Wither, Bleed):**
   - Damage DoT kecil (seperti 1.0 atau 1.5) diberikan format visual ringkas dengan ikon status:
     - Poison: `§2-1.0 ☠`
     - Wither: `§8-1.0 ☣`
     - Bleed: `§4-1.5 🩸`
   - Di-spawn pada ketinggian lebih rendah ($Y = 1.3$) dengan pergerakan meluncur ke samping, jelas terpisah dari damage serangan utama.
4. **Pembatasan Maksimal Indikator Aktif (Cap):**
   - Batasi maksimal 6 indikator aktif secara bersamaan. Jika terjadi serangan cepat beruntun (misalnya Multishot + DoT ticks), indikator tertua otomatis di-discard agar layar tetap bersih dan tidak terjadi penumpukan teks.

### C. Penyesuaian Tata Letak Overhead & Kompatibilitas HUD Mod
1. **Eliminasi Judul Ganda:**
   - Set `this.setCustomName(null)` pada ArmorStand dummy. Dengan demikian, mod HUD eksternal (Jade, WTHIT, Neat, ToroHealth) akan membaca nama entitas resmi (`Target Dummy`) tanpa merender nama kustom panjang yang menabrak billboard.
2. **Ketinggian & Jarak Pandang Billboard:**
   - Atur posisi `overheadDisplay` pada $Y = 2.35$ dengan `BillboardConstraints.CENTER` agar selalu menghadap pemain dengan jarak pandang tajam.
3. **Format Garis yang Rapi dan Terstruktur:**
   - **Baris 1:** `§6§l[ TARGET DUMMY ] §a❤ HP: ∞`
   - **Baris 2:** `§eDPS: §b%.1f §7| §cLast: §f%.1f §7| §6Peak: §f%.1f §7| §aTotal: §f%.1f`
   - **Baris 3:** Status efek aktif terformat rapi (misal: `§2☠ Poison II (0:07) §8☣ Wither II (0:07)`), atau `§7[Status: Netral]` jika tidak ada efek.

---

## 3. Rencana Tahapan Pelaksanaan (Step-by-Step Implementation Tasks)

### Tahap 1: Perbaikan Logika `DamageDummyEntity.java`
- Tambahkan penyimpanan & pemuatan `overheadDisplayUuid` pada NBT (`addAdditionalSaveData` & `readAdditionalSaveData`).
- Implementasikan metode `purgeOrphanDisplays(ServerLevel level)` yang menghapus seluruh entitas `TextDisplay` yatim piatu di sekitar dummy.
- Terapkan penandaan tag entitas unik (`takasha_dummy_overhead_` + `getUUID()`).
- Refaktor `spawnFloatingIndicator` agar menggunakan dispersi horizontal menyamping ($X \pm 0.75$, $Z \pm 0.75$) dengan arc drift keluar dan format khusus DoT (`☠`, `☣`, `🩸`).
- Terapkan batas maksimal 6 indikator aktif simultan.
- Atur `setCustomName(null)` untuk mencegah tabrakan dengan mod HUD klien.

### Tahap 2: Pembersihan Perintah di `TakashaRpgCommands.java`
- Di `executeRemoveDummies()`, tambahkan pencarian dan penghapusan seluruh entitas `Display.TextDisplay` di dalam radius 5 blok dari lokasi perintah agar area bersih total.
- Di `dummy spawn`, pastikan tidak ada `TextDisplay` lama yang tertinggal di lokasi baru.

### Tahap 3: Pembaruan `PRD.md` & Semantic Versioning
- Perbarui dokumentasi `PRD.md` Bagian 1, 4, dan 14:
  - Dokumentasikan sistem arsitektur `DamageDummyEntity` bebas duplikasi entitas (*Single Billboard Guarantee*).
  - Dokumentasikan sistem *Spatial Radial Arc Drift* untuk floating damage numbers.
- Naikkan versi di [`takasha-rpg/build.gradle`](file:///d:/Mod%20Minecraft/weapon%20set/sakura/takasha-rpg/build.gradle):
  `globalModVersion = '1.3.1'` (menghasilkan rilis `v1.3.1+26.2`).

### Tahap 4: Kompilasi & Verifikasi Build
- Jalankan kompilasi Gradle: `.\gradlew.bat :takasha-rpg:build` di direktori `sakura-weapons`.
- Pastikan build sukses (`BUILD SUCCESSFUL` dengan 0 error).
- Verifikasi keberadaan file artefak di `d:\Mod Minecraft\weapon set\sakura\Hasil mod\Takasha-RPG-1.3.1+26.2.jar`.
- Pastikan mod utama `sakura-weapons` (`v2.5.0`) tetap tidak tersentuh.

---

## 4. Kriteria Keberhasilan (Verification & Acceptance Criteria)
1. **Nol Teks Bertumpuk:** Tidak ada lagi dua layer billboard hitam yang merender angka dan status saling menimpa saat dunia dimuat ulang, chunk dimuat ulang, atau dummy dipukul berkali-kali.
2. **Jalur Indikator Bersih:** Angka damage merah (`-7.0`, dll) dan status DoT (`-1.0 ☠`, dll) melayang menyamping menjauhi dummy dan tidak pernah menabrak/masuk ke dalam kotak billboard overhead.
3. **Pembersihan Otomatis:** Saat dummy di-shift-pukul atau dihapus lewat `/dummy remove`, seluruh display teks di udara langsung terhapus bersih tanpa jejak.
4. **Build Bersih:** Jar baru `Takasha-RPG-1.3.1+26.2.jar` berhasil dibuat dan siap dimainkan.
