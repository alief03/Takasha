# Implementation Plan: Takasha v1.5.4 — Perbaikan Smart Filter Anvil Pickaxe & Isolasi Archetype Tool

> **Versi Target:** v1.5.4  
> **Status:** Selesai (Completed)  
> **Tanggal:** 2026-09-29  
> **Referensi PRD:** PRD v1.5.4 (Section 3 Taksonomi Archetype, Section 8 SemVer, Section 10 Kebijakan Deployment, Section 11 Protokol Planning, Section 12.8 Smart Context Filter, Section 12.17 Substring Collision Isolation)  
> **Batasan Khusus:** **HANYA simpan build di `Hasil mod/Takasha-1.5.4.jar`. JANGAN menyalin ke profil Modrinth eksternal dan JANGAN deploy ke Modrinth (sesuai PRD Bagian 10).**

---

## 1. Analisis Akar Masalah (Root Cause Analysis)

Ketika pemain memasukkan item bertipe Pickaxe (seperti `diamond_pickaxe`, `netherite_pickaxe`, dll.) ke slot 0 Anvil, daftar rename di GUI samping (`AnvilSideListWidget`) memunculkan item-item yang bukan pickaxe (seperti Sakura Axe, Pink Legacy Axe, Sakura Halberd, Pink Legacy Halberd, Sakura Hammer, Pink Legacy Hammer, Sakura Mace).

### Penyebab Teknis pada `AnvilItemCatalog.java`:
1. **Tabrakan Substring ("pickaxe" memuat "axe"):**
   Pada metode `matchesInput(ItemStack input)`:
   ```java
   // 4. Axes
   boolean isInputAxe = input.is(ItemTags.AXES) || input.getItem().toString().toLowerCase().contains("axe");
   if (isInputAxe) {
       return base.contains("axe") || base.contains("halberd") || base.contains("hammer")
               || base.contains("mace");
   }

   // 5. Pickaxes
   if (input.is(ItemTags.PICKAXES) || input.getItem().toString().toLowerCase().contains("pickaxe")) {
       return base.contains("pickaxe");
   }
   ```
2. **Eksekusi Logika yang Keliru:**
   - String `"pickaxe"` secara gramatikal berakhiran `"axe"`.
   - Akibatnya, saat `input` adalah `diamond_pickaxe`, pemanggilan `input.getItem().toString().toLowerCase().contains("axe")` bernilai **`true`**!
   - Karena blok pengecekan Axe (`// 4. Axes`) ditempatkan **sebelum** Pickaxe (`// 5. Pickaxes`), Pickaxe secara keliru dievaluasi sebagai Axe.
   - Selanjutnya, di dalam blok Axe, pengecekan `base.contains("axe")` juga bernilai `true` untuk `sakura_axe`, `pink_legacy_axe`, `sakura_pickaxe`, dan `pink_legacy_pickaxe` (karena `"sakura_pickaxe".contains("axe")` bernilai true).
   - Dampak ganda:
     1. Saat memasukkan **Pickaxe**, GUI menampilkan: Axe, Halberd, Hammer, Mace, dan Pickaxe (10 item).
     2. Saat memasukkan **Axe**, GUI juga keliru memunculkan Pickaxe di antara kapak-kapak lainnya.

---

## 2. Rincian Rencana Tindakan (Action Plan)

### Tahap 1: Isolasi Logika Filter Pickaxe vs Axe pada `AnvilItemCatalog.java`
**File Target:** `sakura-weapons/src/main/java/net/sakura/weapons/client/gui/AnvilItemCatalog.java`

1. Pindahkan pengecekan **Pickaxes** ke atas, mendahului **Axes**:
   ```java
   // 4. Pickaxes (Wajib dievaluasi sebelum Axe untuk mencegah false positive "pickaxe".contains("axe"))
   boolean isInputPickaxe = input.is(ItemTags.PICKAXES) || input.getItem().toString().toLowerCase().contains("pickaxe");
   if (isInputPickaxe) {
       return base.contains("pickaxe");
   }
   ```
2. Perketat pengecekan **Axes** agar secara eksplisit mengecualikan `"pickaxe"` baik pada item input maupun pada `baseName`:
   ```java
   // 5. Axes
   boolean isInputAxe = input.is(ItemTags.AXES) || (input.getItem().toString().toLowerCase().contains("axe")
           && !input.getItem().toString().toLowerCase().contains("pickaxe"));
   if (isInputAxe) {
       return (base.contains("axe") && !base.contains("pickaxe"))
               || base.contains("halberd") || base.contains("hammer") || base.contains("mace");
   }
   ```

### Tahap 2: Pembaruan SemVer & Dokumentasi PRD
1. Update [VERSION](file:///d:/Mod%20Minecraft/weapon%20set/sakura/VERSION) -> `1.5.4`
2. Update [sakura-weapons/gradle.properties](file:///d:/Mod%20Minecraft/weapon%20set/sakura/sakura-weapons/gradle.properties) (`mod_version=1.5.4`)
3. Update [PRD.md](file:///d:/Mod%20Minecraft/weapon%20set/sakura/PRD.md):
   - Update header versi menjadi `1.5.4`
   - Tambahkan Bagian 12.17: *Anvil Smart Context Filter Substring Collision & Tool Archetype Isolation*.

### Tahap 3: Kompilasi & Build Mod JAR
1. Jalankan kompilasi Gradle:
   ```powershell
   ./gradlew build
   ```
2. Salin output build ke direktori resmi lokal:
   - Dari: `sakura-weapons/build/libs/Takasha-1.5.4.jar`
   - Ke: `Hasil mod/Takasha-1.5.4.jar`
3. **Patuhi Batasan PRD Bagian 10:** JANGAN menyalin file ke profil Modrinth eksternal dan JANGAN deploy ke Modrinth.

---

## 3. Kriteria Verifikasi Selesai (Definition of Done)

- [x] Saat slot 0 Anvil berisi Pickaxe (`diamond_pickaxe`, `iron_pickaxe`, dll.), GUI rename HANYA menampilkan pickaxe (`Sakura Pickaxe` dan `Pink Legacy Pickaxe`).
- [x] Saat slot 0 Anvil berisi Axe (`diamond_axe`, `netherite_axe`, dll.), pickaxe TIDAK muncul di daftar kapak.
- [x] Kompilasi `./gradlew build` sukses tanpa error/warning baru.
- [x] File `Hasil mod/Takasha-1.5.4.jar` terbuat di folder `Hasil mod/`.
- [x] Profil Modrinth eksternal tidak dimodifikasi secara otomatis (sesuai PRD Bagian 10).
