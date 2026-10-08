# Rencana Implementasi: Perbaikan Desinkronisasi Emote ReplayMod & Kegagalan Seluruh 32 Preset Pose Mannequin (v2.0.1+26.2 PATCH)

> **Kepatuhan Protokol Perencanaan PRD:** Sesuai mandat **PRD Bagian 11 (Artifact Planning Protocol)**, dokumen ini merupakan rencana kerja komprehensif yang telah diselaraskan dengan arsitektur resmi [`PRD.md`](file:///d:/Mod%20Minecraft/weapon%20set/sakura/PRD.md) dan disimpan secara fisik di [`docs/plans/2026-10-04-fix-replaymod-emote-and-preset9-sitting-desync-plan.md`](file:///d:/Mod%20Minecraft/weapon%20set/sakura/docs/plans/2026-10-04-fix-replaymod-emote-and-preset9-sitting-desync-plan.md).  
> **Mod:** Takasha (`sakura_weapons`) | **Versi Target:** Minecraft Java 26.2 (Fabric Mod-Only)  
> **Lingkungan Build:** OpenJDK 25 LTS (`java-runtime-epsilon`), Bytecode Major `69.0`, Fabric Loom `1.17.21`, Fabric API `0.161.0+26.2`  
> **Target Output Rilis:** [`Hasil mod/Takasha-2.0.1+26.2.jar`](file:///d:/Mod%20Minecraft/weapon%20set/sakura/Hasil%20mod/Takasha-2.0.1+26.2.jar) (Kepatuhan SemVer 2.0.0 & PRD Bagian 2.5, 5.5.2, 5.5.4, 7, 8, 10, 11 & 27)  
> **Status:** Siap Digunakan / Menunggu Persetujuan Eksekusi

---

## 1. Analisis Masalah & Validasi Bukti Gambar (Visual Evidence & Root Cause Analysis)

### 1.1 Masalah 1: Di Halaman Edit ReplayMod, NPC Tidak Menjalankan Emote (Gambar 1 vs Gambar 2)
- **Bukti Visual:**
  - **Gambar 2 (In-Game Live):** NPC berambut pink terbaring KO / tewas di tanah dengan pedang tertancap di dada, sementara NPC berpakaian jubah putih di belakang duduk bersandar di lantai tanah di bawah pohon sakura.
  - **Gambar 1 (ReplayMod Edit Screen):** Pada timeline ReplayMod (posisi 00:00), kedua NPC tersebut berdiri tegak lurus kaku (default vanilla standing T-pose). Emote dan pose rebah/duduk tidak terputar sama sekali.
- **Akar Masalah Teknis:**
  1. **Fatal False-Positive pada `EmotecraftCompat.hasActiveEmoteAnimation(AvatarRenderState state)`:**
     Pada baris 210–229 `EmotecraftCompat.java`:
     ```java
     Object manager = m.invoke(state);
     if (manager != null) {
         for (Method mm : manager.getClass().getMethods()) {
             if ((mm.getName().equals("isActive") || mm.getName().equals("isAnimationActive")) && mm.getParameterCount() == 0) {
                 Object active = mm.invoke(manager);
                 if (Boolean.TRUE.equals(active)) return true;
             }
         }
         return true; // <-- FATAL BUG: Selalu mengembalikan TRUE meskipun isActive() bernilai FALSE!
     }
     ```
     Karena pustaka PlayerAnimationLib menyematkan instance `manager` non-null pada setiap `AvatarRenderState`, method ini **selalu mengembalikan `true`** 100% dari waktu ketika Emotecraft/PlayerAnimationLib terpasang.
  2. **Pemblokiran Total Posing & Prosedural di `TakashaNpcModel.java`:**
     Di `TakashaNpcModel.java:139–144` dan baris 195:
     ```java
     boolean emoteActive = npcState.isEmotePlaying || net.sakura.weapons.compat.EmotecraftCompat.hasActiveEmoteAnimation(state);
     if (!emoteActive && npcState.emoteId != null && !npcState.emoteId.isBlank()) {
         proceduralHandled = applyProceduralEmote(npcState.emoteId, npcState.animTime);
     }
     ...
     if (!emoteActive && !proceduralHandled) {
         // Mengatur rotasi sendi (head, body, arms, legs)...
         applyBendableCuboidsPoses(preset, npcState.emoteId);
     }
     ```
     Karena `emoteActive` secara keliru permanen bernilai `true`, baik `applyProceduralEmote` maupun penataan sudut rotasi sendi (`headPose`, `bodyPose`, dll.) serta deformasi Bendable Cuboids **dilewati seluruhnya**!
  3. **Pemblokiran Matriks Rotasi Horizontal di `TakashaNpcRenderer.scale()`:**
     Di `TakashaNpcRenderer.java:234`, pengecekan rotasi tidur terlentang/tengkurap untuk `emoteId` dibatasi oleh `!net.sakura.weapons.compat.EmotecraftCompat.hasActiveEmoteAnimation(state)`. Akibatnya, pose tidur NPC pink tidak pernah diputar mendatar pada ReplayMod.
  4. **Karakteristik Lingkungan ReplayMod:**
     Pada ReplayMod timeline editor, `entity.tick()` tidak dipanggil secara siklis dan controller animasi pihak ketiga tidak berjalan. NPC mod bergantung mutlak pada data sendi `SynchedEntityData` dan mesin animasi prosedural internal berbasis `npcState.animTime`.

---

### 1.2 Masalah 2: Pemilihan SELURUH 32 Preset Pose di GUI Mannequin Menyebabkan NPC Tetap Berdiri Tegak (Gambar 3 + Feedback)
- **Bukti Visual & Umpan Balik:**
  - **Umpan Balik Pengguna:** *"tidak hanya preset 9, preset yang lain pun sama saja. perbaiki planningnya"*
  - **Gambar 3 (GUI Mannequin):** Pengguna memilih preset di GUI. Namun model NPC di latar belakang maupun di live preview tetap berdiri tegak lurus tanpa ada artikulasi sendi apa pun.
- **Akar Masalah Teknis Menyeluruh:**
  1. **Lockout Sistemik pada Seluruh 32 Preset:**
     Saat pemain memilih preset apa pun (mulai dari Preset 1 s/d 32), data rotasi sendi (`headPose`, `bodyPose`, `leftArmPose`, `rightArmPose`, `leftLegPose`, `rightLegPose`) telah berhasil dihitung dan dicatat di `TakashaNpcEntity.applyPosePreset(preset)`.
     Namun saat model di-render di `TakashaNpcModel.setupAnim()`:
     ```java
     boolean emoteActive = npcState.isEmotePlaying || net.sakura.weapons.compat.EmotecraftCompat.hasActiveEmoteAnimation(state);
     ...
     if (!emoteActive && !proceduralHandled) {
         // Blok penataan rotasi sendi dan artikulasi siku/lutut
         applyBendableCuboidsPoses(preset, npcState.emoteId);
     }
     ```
     Karena `hasActiveEmoteAnimation(state)` selalu bernilai `true` (false-positive akibat `return true;`), maka `emoteActive` selalu `true`.
     Akibatnya: **SELURUH 32 PRESET (Presets 1..32) DILEWATI 100%!** Model mengabaikan rotasi dari entitas dan merender pose default tegak lurus `(0, 0, 0)` Mojang.
  2. **Klasifikasi Pivot Anatomis yang Keliru pada Kategori Duduk Lantai (Termasuk Preset 9):**
     Di `TakashaNpcModel.java:146`:
     ```java
     boolean isSitting = (preset == 8 || preset == 9 || ...);
     boolean isFloorSitting = (preset == 10 || preset == 11 || preset == 14 || ...);
     ```
     Preset 9 ("Duduk Bersandar") dimasukkan ke dalam `isSitting` (kursi, $Y=8.5$). Seharusnya dikelompokkan ke dalam `isFloorSitting` ($Y=11.5F, \text{legs.y}=20.5F, \text{legs.z}=-2.5F$) agar pantat dan paha menempel tepat di atas permukaan rumput/tanah.
  3. **Penekukan Lutut Bendable Cuboids Berlebih:**
     Di `TakashaNpcModel.java:350-351`, nilai penekukan lutut `1.35f` ($\approx 77^\circ$) menekuk betis ke bawah menembus tanah. Untuk pose duduk bersandar selonjor, penekukan harus rileks mendatar (`0.05f`).

---

## 2. Matriks Komparasi Solusi Teknis Terhadap PRD

| Komponen | Implementasi Bermasalah (Sebelumnya) | Solusi Perbaikan Baku (v2.0.1+26.2) | Kepatuhan PRD |
|:---|:---|:---|:---|
| **`EmotecraftCompat.java`** | `return true;` di luar loop `isActive()`, memicu false-positive permanen. | Hanya kembalikan `true` jika method `isActive()` atau `isAnimationActive()` secara eksplisit mengembalikan `Boolean.TRUE`. Jika loop selesai tanpa kecocokan, kembalikan `false`. | PRD 5.5.3 |
| **`TakashaNpcModel.java` (Fondasi Sendi Seluruh 32 Preset)** | Rotasi sendi seluruh preset (1..32) dikurung dalam `if (!emoteActive && !proceduralHandled)`. | **Hapus pembatas.** Rotasi sendi dari `SynchedEntityData` dan `applyBendableCuboidsPoses()` SELALU diaplikasikan secara mutlak untuk seluruh preset 1..32. Animasi runtime (prosedural atau Emotecraft) diaplikasikan sebagai overlay di atasnya. | PRD 5.5.4 & PRD 27 |
| **`TakashaNpcModel.java` (Preset 9 Pivot Lantai)** | Preset 9 masuk ke `isSitting` (kursi, $Y=8.5$). | Pindahkan Preset 9 ke `isFloorSitting` ($Y=11.5$, paha $Y=20.5, Z=-2.5$). Pantat dan paha menempel pas di atas permukaan tanah/kasur. | PRD 5.5.2 & PRD 27 |
| **`TakashaNpcModel.java` (Preset 9 Bending)** | `bendLeg(..., 1.35f)` menekuk betis ke dalam tanah. | Ubah menjadi `bendLeg(..., 0.05f)` agar kaki berselonjor santai di atas tanah selaras Gambar 2. | PRD 5.5.2 |
| **`TakashaNpcRenderer.java` (Scale & Matrix)** | Pengecekan rotasi horizontal tidur terkunci oleh false-positive emote check. | Pastikan Preset 1..7 selalu menerima rotasi mendatar horizontal ($X=\pm 90^\circ, Z=\pm 90^\circ$). Emote tidur/rebah pada Preset 0 juga diaplikasikan rotasi jika tidak ada controller aktif. | PRD 5.5.2 & PRD 27 |
| **ReplayMod Scrubber Support** | Animasi prosedural macet saat ReplayMod di-pause/scrub. | `npcState.animTime` menghitung interpolasi waktu mulus dari `System.currentTimeMillis()` ketika `gameTime == 0`, menjamin animasi emote tetap hidup saat timeline digeser. | PRD 5.5.4 |

---

## 3. Rencana Eksekusi Kode Bertahap (Step-by-Step Implementation)

### Tahap 1: Perbaikan Deteksi Emote Aktif di `EmotecraftCompat.java`
- Perbaiki method `hasActiveEmoteAnimation(AvatarRenderState state)`:
  - Iterasi method pada objek `animManager`.
  - Jika menemukan method `isActive` atau `isAnimationActive` dengan 0 parameter, panggil dan verifikasi apakah nilainya `Boolean.TRUE`.
  - Jika tidak ada method aktif yang bernilai `true`, **kembalikan `false`** (hapus baris `return true;` prematur).

### Tahap 2: Restrukturisasi Posing & Koreksi Seluruh Preset di `TakashaNpcModel.java`
1. **Unconditional Baseline Posing untuk Seluruh 32 Preset:**
   - Hapus pembatas `if (!emoteActive && !proceduralHandled)`.
   - Aplikasikan `head`, `body`, `leftArm`, `rightArm`, `leftLeg`, `rightLeg` dari `npcState` serta `applyBendableCuboidsPoses(preset, npcState.emoteId)` secara mutlak untuk seluruh preset (1 s/d 32).
2. **Koreksi Kategori Duduk Lantai (Preset 9 Duduk Sandar):**
   - Keluarkan Preset 9 dari `isSitting`.
   - Masukkan Preset 9 ke `isFloorSitting`:
     ```java
     boolean isSitting = (preset == 8 || ...);
     boolean isFloorSitting = (preset == 9 || preset == 10 || preset == 11 || preset == 14 || ...);
     ```
3. **Penyelarasan Sendi & Bending Preset 9:**
   - Pada `applyBendableCuboidsPoses`, ubah `bendLeg` untuk Preset 9 menjadi `0.05f` (rileks di atas lantai) dan `bendArm` menjadi `-0.4f`.
4. **Overlay Animasi Prosedural:**
   - Jalankan `applyProceduralEmote(npcState.emoteId, npcState.animTime)` setelah rotasi dasar diterapkan jika `!hasActiveEmoteAnimation(state)` dan `emoteId` tidak kosong.

### Tahap 3: Pemantapan Render Matrix di `TakashaNpcRenderer.java`
- Pada method `scale(AvatarRenderState state, PoseStack poseStack)`:
  - Pastikan preset tidur 1..7 secara konsisten melakukan rotasi PoseStack horizontal mendatar ($X=\pm 90^\circ, Z=\pm 90^\circ$).
  - Pada preset 0 dengan `emoteId` tidur/rebahan, pastikan rotasi horizontal diterapkan secara presisi dengan status emote yang telah diperbaiki.

### Tahap 4: Sinkronisasi GUI di `TakashaNpcScreen.java`
- Pastikan saat tombol `cyclePose` ditekan untuk memilih preset apa pun (1..32), pembaruan data sendi seketika tercermin pada preview 3D GUI dan entity in-game.

### Tahap 5: Penyelarasan Dokumentasi Resmi `PRD.md`
- Bagian 27 telah dimutakhirkan untuk mendokumentasikan resolusi universal lockout 32 preset dan zero-desync ReplayMod.

### Tahap 6: Kompilasi, Validasi & Pembuatan Output JAR
- Perbarui `gradle.properties` ke versi `mod_version=2.0.1`.
- Jalankan `./gradlew build` di direktori `sakura-weapons/`.
- Verifikasi keberhasilan pembuatan file `Hasil mod/Takasha-2.0.1+26.2.jar` dengan spesifikasi OpenJDK 25 LTS (bytecode major 69.0).
