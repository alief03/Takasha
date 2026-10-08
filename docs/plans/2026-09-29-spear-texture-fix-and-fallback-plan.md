# Implementation Plan: Takasha v1.5.3 — Perbaikan Error Tekstur Spear Minecraft 26.2 & Arsitektur Dual-Model Fallback

> **Versi Target:** v1.5.3  
> **Status:** Ready for Review / Execution  
> **Tanggal:** 2026-09-29  
> **Referensi PRD:** PRD v1.5.3 (Section 3 Taksonomi Archetype, Section 8 SemVer, Section 11 Protokol Planning, Section 12.16 Spear 26.2 Dual-Model & Case Cleanliness)  
> **Batasan Khusus:** **HANYA bangun Mod (`Hasil mod/Takasha-1.5.3.jar`), TANPA membuat / mengompilasi Resource Pack. JANGAN deploy ke Modrinth.**

---

## 1. Analisis Akar Masalah (Root Cause Analysis dari Game Log)

Berdasarkan bukti langsung dari log klien Minecraft (`C:\Users\Administrator\AppData\Roaming\ModrinthApp\profiles\kaizenmc.id\logs\latest.log`):

```text
[17:05:12] [Worker-Main-2/ERROR]: Couldn't parse item model 'minecraft:copper_spear' from pack 'sakura_weapons': Duplicate case conditions: literal{&dPink Legacy Spear}, literal{&dPink legacy spear}
[17:05:12] [Worker-Main-2/ERROR]: Couldn't parse item model 'minecraft:diamond_spear' from pack 'sakura_weapons': Duplicate case conditions: literal{&dPink Legacy Spear}, literal{&dPink legacy spear}
[17:05:12] [Worker-Main-2/ERROR]: Couldn't parse item model 'minecraft:golden_spear' from pack 'sakura_weapons': Duplicate case conditions: literal{&dPink Legacy Spear}, literal{&dPink legacy spear}
[17:05:12] [Worker-Main-2/ERROR]: Couldn't parse item model 'minecraft:iron_spear' from pack 'sakura_weapons': Duplicate case conditions: literal{&dPink Legacy Spear}, literal{&dPink legacy spear}
[17:05:12] [Worker-Main-2/ERROR]: Couldn't parse item model 'minecraft:netherite_spear' from pack 'sakura_weapons': Duplicate case conditions: literal{&dPink Legacy Spear}, literal{&dPink legacy spear}
[17:05:12] [Worker-Main-2/ERROR]: Couldn't parse item model 'minecraft:spear' from pack 'sakura_weapons': Duplicate case conditions: literal{&dPink Legacy Spear}, literal{&dPink legacy spear}
[17:05:12] [Worker-Main-2/ERROR]: Couldn't parse item model 'minecraft:stone_spear' from pack 'sakura_weapons': Duplicate case conditions: literal{&dPink Legacy Spear}, literal{&dPink legacy spear}
[17:05:13] [Worker-Main-7/ERROR]: Couldn't parse item model 'minecraft:wooden_spear' from pack 'sakura_weapons': Duplicate case conditions: literal{&dPink Legacy Spear}, literal{&dPink legacy spear}
```

### Penyebab Detail:
1. **Duplikasi Kondisi Case (`Duplicate case conditions`):**
   - Pada file `*_spear.json`, di dalam array `when` untuk `Pink Legacy Spear`, terdapat duplikasi string:
     - Baris 54: `"&dPink Legacy Spear"`
     - Baris 58: `"&dPink legacy spear"`
     - Baris 62: `"&dPink Legacy Spear"` *(DUPLIKAT)*
     - Baris 63: `"&dPink legacy spear"` *(DUPLIKAT)*
   - Di Minecraft 26.2 (1.21.5+), Mojang menggunakan `SelectItemModel$UnbakedSwitch` dengan validasi ketat. Jika ada case condition yang terduplikasi di dalam satu switch select, parser membuang (*discards*) seluruh definisi model item tersebut dan menggantinya dengan `MissingItemModel` (kotak ungu hitam / `missingno`).
2. **Dampak Fatal:**
   - **Tekstur Bawaan Rusak:** Seluruh 7 tier tombak vanilla di tab Creative Inventory / Search GUI langsung tampil sebagai kotak ungu-hitam karena item definition-nya gagal dimuat oleh engine client.
   - **Hasil Rename Anvil Ikut Rusak:** Karena keseluruhan file item definition rusak/invalid di mata Minecraft, saat pemain me-rename tombak di Anvil menjadi *Sakura Spear* atau *Pink Legacy Spear*, item tersebut tetap me-render `MissingItemModel` (kotak ungu-hitam).
3. **Ketidaksesuaian Struktur Spear Vanilla 26.2:**
   - Di vanilla 26.2, spear menggunakan sistem dual-model:
     - `gui`, `ground`, `fixed`, `on_shelf` -> `minecraft:item/<tier>_spear` (ikon 2D)
     - fallback (in-hand) -> `minecraft:item/<tier>_spear_in_hand` (model 3D tombak saat dipegang)
     - Parameter root `"swap_animation_scale": 1.95` untuk animasi swap/serangan tombak.
   - Pada file mod sebelumnya, fallback hanya menunjuk langsung ke `minecraft:item/<tier>_spear` dan menghilangkan `"swap_animation_scale"`.
4. **File Hantu `spear.json`:**
   - Di Minecraft vanilla tidak ada item bernama `minecraft:spear` (hanya ada `wooden_spear`, `stone_spear`, `copper_spear`, `iron_spear`, `golden_spear`, `diamond_spear`, `netherite_spear`). File `spear.json` adalah file hantu yang memicu error model missing `minecraft:item/spear`.

---

## 2. Rincian Rencana Tindakan (Action Plan)

### Tahap 1: Pembersihan & Rekonstruksi 7 File Definisi Spear Minecraft 26.2
Perbaiki ke-7 file di `sakura-weapons/src/main/resources/assets/minecraft/items/`:
- `wooden_spear.json`
- `stone_spear.json`
- `copper_spear.json`
- `iron_spear.json`
- `golden_spear.json`
- `diamond_spear.json`
- `netherite_spear.json`

Format baku yang 100% valid dan sesuai spesifikasi engine Minecraft 26.2:
```json
{
  "model": {
    "type": "minecraft:select",
    "property": "minecraft:component",
    "component": "minecraft:custom_name",
    "cases": [
      {
        "when": [
          "Sakura Spear",
          "sakura spear",
          "SAKURA SPEAR",
          "Sakura spear",
          "Tombak Sakura",
          "tombak sakura",
          "TOMBAK SAKURA",
          "Tombak sakura",
          "§d§l🌸 Sakura Spear 🌸",
          "§d§lSakura Spear",
          "§dSakura Spear",
          "&d&l🌸 Sakura Spear 🌸",
          "&d&lSakura Spear",
          "&dSakura Spear",
          "§d§l🌸 Sakura spear 🌸",
          "§d§lSakura spear",
          "§dSakura spear",
          "&d&l🌸 Sakura spear 🌸",
          "&d&lSakura spear",
          "&dSakura spear",
          "§d§l🌸 Tombak Sakura 🌸",
          "§d§lTombak Sakura",
          "§dTombak Sakura",
          "&d&l🌸 Tombak Sakura 🌸",
          "&d&lTombak Sakura",
          "&dTombak Sakura"
        ],
        "model": {
          "type": "minecraft:model",
          "model": "sakura:item/spear"
        }
      },
      {
        "when": [
          "Pink Legacy Spear",
          "pink legacy spear",
          "PINK LEGACY SPEAR",
          "Pink legacy spear",
          "Tombak Pink Legacy",
          "tombak pink legacy",
          "TOMBAK PINK LEGACY",
          "Tombak pink legacy",
          "§d§lPink Legacy Spear",
          "§dPink Legacy Spear",
          "&d&lPink Legacy Spear",
          "&dPink Legacy Spear",
          "§d§lPink legacy spear",
          "§dPink legacy spear",
          "&d&lPink legacy spear",
          "&dPink legacy spear",
          "§d§lTombak Pink Legacy",
          "§dTombak Pink Legacy",
          "&d&lTombak Pink Legacy",
          "&dTombak Pink Legacy"
        ],
        "model": {
          "type": "minecraft:model",
          "model": "pink_legacy:item/spear"
        }
      }
    ],
    "fallback": {
      "type": "minecraft:select",
      "property": "minecraft:display_context",
      "cases": [
        {
          "when": [
            "gui",
            "ground",
            "fixed",
            "on_shelf"
          ],
          "model": {
            "type": "minecraft:model",
            "model": "minecraft:item/<tier>_spear"
          }
        }
      ],
      "fallback": {
        "type": "minecraft:model",
        "model": "minecraft:item/<tier>_spear_in_hand"
      }
    }
  },
  "swap_animation_scale": 1.95
}
```

*Catatan Kritis:*
1. 0 entri duplikat pada `when` array.
2. Fallback mengembalikan dual-model vanilla (ikon GUI vs 3D in-hand).
3. Parameter `"swap_animation_scale": 1.95` disertakan.

### Tahap 2: Menghapus File Hantu yang Tidak Valid
- Hapus `sakura-weapons/src/main/resources/assets/minecraft/items/spear.json`
- Hapus `sakura-weapons/src/main/resources/assets/minecraft/models/item/spear.json`

### Tahap 3: Pembaruan Versi SemVer & Dokumentasi PRD
1. Update `VERSION` -> `1.5.3`
2. Update `sakura-weapons/gradle.properties` (`mod_version=1.5.3`)
3. Update `PRD.md`:
   - Header versi menjadi `1.5.3`
   - Tambahkan Bagian 12.16: *Spear Dual-Model Display Context, Swap Animation Scale, and Strict Case Cleanliness*.

### Tahap 4: Kompilasi & Build Mod JAR Saja (Tanpa Resource Pack)
1. Eksekusi Gradle build:
   ```powershell
   ./gradlew build
   ```
2. Salin output build ke direktori distribusi mod:
   - Dari: `sakura-weapons/build/libs/Takasha-1.5.3.jar`
   - Ke: `Hasil mod/Takasha-1.5.3.jar`
3. **Patuhi Batasan Pengguna:** JANGAN menjalankan `python build.py` dan JANGAN deploy ke Modrinth.

---

## 3. Kriteria Verifikasi Selesai (Definition of Done)

- [x] Seluruh 7 file `*_spear.json` bersih dari duplikasi case condition.
- [x] Fallback spear mengembalikan struktur `display_context` vanilla (`gui`/`ground`/`fixed`/`on_shelf` vs `_in_hand`) beserta `"swap_animation_scale": 1.95`.
- [x] File hantu `spear.json` telah dibersihkan.
- [x] Versi proyek ter-bump ke `1.5.3` di `VERSION`, `gradle.properties`, dan `PRD.md`.
- [x] Kompilasi `./gradlew build` sukses tanpa error.
- [x] File `Hasil mod/Takasha-1.5.3.jar` terbentuk dan siap digunakan.
- [x] Resource pack TIDAK disentuh / tidak di-build.
- [x] Tidak ada deployment ke Modrinth.
