# Rencana Implementasi: ReplayMod Zero-Desync Protocol, 32 Categorized Poses, Horizontal Bed/Floor Matrix, Procedural Animations & Bake Emote to Pose (v2.0.0+26.2)

> **Kepatuhan Protokol Perencanaan PRD:** Sesuai mandat **PRD Bagian 11 (Artifact Planning Protocol)**, dokumen ini merupakan rencana kerja komprehensif yang telah diselaraskan dengan arsitektur resmi `PRD.md` dan disimpan secara fisik di `docs/plans/2026-10-03-replaymod-zero-desync-emotecraft-poses-plan.md`.  
> **Mod:** Takasha (`sakura_weapons`) | **Versi Target:** Minecraft Java 26.2 (Fabric Mod-Only)  
> **Lingkungan Build:** OpenJDK 25 LTS (`java-runtime-epsilon`), Bytecode Major `69.0`, Fabric Loom `1.17.21`, Fabric API `0.161.0+26.2`  
> **Target Output Rilis:** `Hasil mod/Takasha-2.0.0+26.2.jar` (Kepatuhan SemVer 2.0.0 & PRD Bagian 2.5, 5.5, 7, 8, 10 & 12)  
> **Status:** Siap Digunakan / Sedang Diimplementasikan

---

## 1. Analisis Status & Kesenjangan Terhadap PRD (PRD Cross-Reference & Gap Analysis)

Berdasarkan laporan pengguna:
1. *Ketika melihat hasil record dari ReplayMod dan akan melakukan edit melalui ReplayMod, NPC tidak berada dalam posisi emote ketika record. Emote hilang menjadi berdiri biasa.*
2. *Emote dari Emotecraft belum ada / belum terputar.*
3. *Pose bawaan yang variasi belum bisa / kurang variasi.*

### Akar Masalah & Matriks Solusi:
| Bagian PRD | Spesifikasi & Ketentuan PRD | Status Sebelumnya (v1.9.5) | Akar Masalah Teknis | Solusi Standar PRD v2.0.0 |
|:---|:---|:---|:---|:---|
| **PRD 5.5.4** | ReplayMod Zero-Desync Architecture | Pose hilang di ReplayMod viewer/timeline editor | Di `TakashaNpcModel.java:136`, `boolean hasEmote = npcState.emoteId != null && !npcState.emoteId.isBlank(); if (!hasEmote) { ... }`. Saat `emoteId` aktif, semua penimpaan sendi manual dilewati. Di ReplayMod viewer, ticking entitas tidak berjalan reguler dan controller animasi tidak aktif sehingga sendi default ke (0,0,0) (berdiri tegak). | Hapus pembatas `if (!hasEmote)`. Selalu terapkan rotasi sendi dari `SynchedEntityData` (`headPose`, `bodyPose`, dll.) sebagai fondasi anatomi. Jika animasi berjalan, overlay di atasnya. |
| **PRD 5.5.3** | Integrasi Emotecraft & Animasi Prosedural Native | Emote Emotecraft belum ada / error | `TakashaNpcEntity` extends `Avatar`, namun KosmX hanya me-mixin `IPlayerEntity` ke `Player`/`AbstractClientPlayer`. Refleksi melempar `IllegalArgumentException`. Emote bawaan (`wave`, `clap`, dll.) adalah string kosong. | 1. Bungkus pemanggilan `IPlayerEntity` dengan `isInstance` check.<br>2. Sediakan mesin animasi prosedural matematis native (`wave`, `clap`, `cheer`, `dance`, `salute`, `point`, `bow`) berbasis `animTime = tickCount + partialTick`. |
| **PRD 5.5.2** | 32 Variasi Pose Terkategori & Horizontal Bed Alignment | Variasi pose terbatas; pose tidur berdiri tegak | Vanilla `LivingEntityRenderer` hanya memutar horizontal jika blok kasur vanilla terdeteksi (`state.bedOrientation != null`). Tanpa kasur, entitas tidur tetap berdiri. Preset hanya 0-21 tanpa kategorisasi jelas. | 1. Di `TakashaNpcRenderer.scale()`, tambahkan rotasi PoseStack horizontal ($X=\pm 90^\circ, Z=\pm 90^\circ$) selaras `customYaw` untuk pose tidur 1-7.<br>2. Perluas katalog pose menjadi 32 preset terstruktur dalam 4 kategori (🛌 Tidur, 🧘 Duduk, ⚔ Tempur, 🎭 Gestur).<br>3. Handle `DATA_POSE_PRESET` dan `DATA_YAW_ROTATION` di `onSyncedDataUpdated()`. |
| **PRD 5.5.3** | Fitur "📌 Bekukan ke Pose" (Bake Emote to Static Pose) | Belum tersedia | Pengguna ingin membekukan gerakan dinamis menjadi pose statis permanen yang anti-desync di ReplayMod. | Tambahkan tombol `[📌]` di GUI yang menyalin rotasi sendi aktif dari frame saat ini ke data sendi `headPose`, `bodyPose`, dll., lalu mematikan `emoteId`. |

---

## 2. Rencana Eksekusi Kode (Implementation Plan)

### Tahap 1: Render State & Renderer (`TakashaNpcRenderState.java` & `TakashaNpcRenderer.java`)
- Tambahkan `public float animTime = 0.0f;` pada `TakashaNpcRenderState`.
- Di `TakashaNpcRenderer.extractRenderState()`, assign `npcState.animTime = entity.tickCount + partialTick;`.
- Di `TakashaNpcRenderer.scale()`, tambahkan pengecekan jika preset berada pada rentang tidur (1-7), lakukan rotasi PoseStack horizontal mendatar dan translasi $Y + 0.5625$ jika di kasur / $Y + 0.0$ di lantai.

### Tahap 2: Model & Animasi Prosedural (`TakashaNpcModel.java`)
- Bongkar blok `if (!hasEmote)`: rotasi sendi dari `npcState` SELALU diaplikasikan.
- Tambahkan method `applyProceduralEmote(String emoteId, float animTime)` untuk menganimasikan `wave`, `clap`, `cheer`, `dance`, `salute`, `point`, `bow`.
- Perbarui `applyBendableCuboidsPoses()` untuk mendukung 32 varian preset.

### Tahap 3: Entitas & Paket Jaringan (`TakashaNpcEntity.java`)
- Tangani `DATA_POSE_PRESET` dan `DATA_YAW_ROTATION` di `onSyncedDataUpdated()`.
- Perbarui `applyPosePreset()` dan `applyEntityPoseForPreset()` hingga 32 preset.

### Tahap 4: Emotecraft Compatibility (`EmotecraftCompat.java`)
- Tambahkan fungsi `sampleCurrentRotations(TakashaNpcEntity entity, String emoteId)` untuk mendukung "Bake Emote to Pose".
- Lindungi `playEmote` dengan `instanceof` / `isInstance` agar tidak melempar `IllegalArgumentException`.

### Tahap 5: GUI Screen (`TakashaNpcScreen.java`)
- Perbarui daftar string preset menjadi 33 opsi (0 = Bebas, 1-32 = Pose Terkategori).
- Tambahkan tombol `bakePoseButton` (`[📌]`) pada Row 8 di sebelah kontrol emote.
- Implementasikan aksi `bakeEmoteToPose()`.

### Tahap 6: Lokalisasi & Bahasa (`en_us.json` & `id_id.json`)
- Daftarkan teks deskripsi dan tooltip untuk tombol bekukan pose serta kategori pose.

### Tahap 7: Kompilasi, Verifikasi & Distribusi
- Jalankan `./gradlew build` di `sakura-weapons/`.
- Verifikasi keberadaan `Hasil mod/Takasha-2.0.0+26.2.jar` dan periksa versi bytecode Java 25 (69.0).
