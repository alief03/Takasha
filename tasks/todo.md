# Task Checklist: Standarisasi Sistem RPG Items & Optimasi Antigravity CPU

Tracked from [tasks/plan.md](plan.md).

## Phase 1: Optimasi Antigravity IDE & Konfigurasi Workspace Anti-100% CPU
- [x] Task 1.1: Terapkan konfigurasi `files.watcherExclude`, `search.exclude`, `java.import.exclusions`, dan JVM JDT-LS pada `.vscode/settings.json`
- [x] Task 1.2: Jalankan pembersihan proses zombie Java dengan `taskkill /F /IM java.exe` dan verifikasi penurunan beban CPU host (< 10%)

## Phase 2: Mesin Persistensi Disk YAML (`MMOItemDiskManager`)
- [x] Task 2.1: Buat kelas `MMOItemDiskManager.java` di paket `net.takasha.rpg.mmo` untuk serialisasi & deserialisasi template ke format disk YAML
- [x] Task 2.2: Integrasikan `saveTemplate()`, `deleteTemplateDisk()`, dan `loadCustomTemplates()` pada `MMOItemManager.java`
- [x] Task 2.3: Hubungkan perintah `/mi create`, `/mi copy`, dan `/mi delete` agar langsung menulis/menghapus konfigurasi pada disk secara real-time
- [x] Task 2.4: Perbarui perintah `/mi reload` agar memuat ulang baik pack default maupun konfigurasi disk kustom

## Phase 3: Rekonstruksi Dinamis `MMOItemEditionMenu` Sesuai Standar RPG
- [x] Task 3.1: Implementasikan pemfilteran stat dinamis per tipe item (`Type.getAvailableStats()`) memfilter stat berdasarkan ModifierSource tipe (bukan kategori buatan)
- [x] Task 3.2: Terapkan paginasi Slot 27 (`Previous Page`) dan Slot 35 (`Next Page`) dengan dynamic state Page 1 vs Page 2, auto-fill `GRAY_STAINED_GLASS_PANE` (`- No Item Stat -`), serta preservasi `previousPage` saat chat edition
- [x] Task 3.3: Terapkan format lore tombol stat presisi (`Current Value: ...`, `➔ Left click to change`, `➔ Right click to reset`)
- [x] Task 3.4: Hubungkan setiap tombol stat ke `MMOChatEditionEngine` dan auto-save ke `MMOItemDiskManager`

## Phase 4: Penyempurnaan `MMOItemBrowserMenu` & Sub-Menu Pendukung
- [x] Task 4.1: Selaraskan judul browser secara dinamis (`Item Explorer: <TYPE>` / `Delete Mode: <TYPE>`) dan paginasi Slot 18/26 dengan auto-fill `GRAY_STAINED_GLASS_PANE` (`- No Item -`)
- [x] Task 4.2: Integrasikan penghapusan fisik file YAML saat item dihapus dalam Delete Mode
- [x] Task 4.3: Tambahkan sub-menu `MMOGemSocketMenu` untuk mengatur soket permata pada item

## Phase 5: Kompatibilitas Lanjutan & Crafting Station
- [x] Task 5.1: Implementasikan GUI `CraftingStationMenu.java` 54-slot untuk command `/mi stations open <station_id>`
- [x] Task 5.2: Sinkronisasi perintah `/mi revid` dan `/mi update` dengan persistensi disk

## Phase 6: Verifikasi Kompilasi Biner, PRD, CHANGELOG, dan Bumping Versi
- [x] Task 6.1: Update `PRD.md` Bab 28 (Dinamis Item Edition & Optimasi Antigravity)
- [x] Task 6.2: Update `CHANGELOG.md` Entri 47
- [x] Task 6.3: Bumping versi: Root `VERSION` -> `2.9.2`, `takasha-rpg` -> `1.9.2+26.2`, `sakura-weapons/versions/26.2` -> `2.9.2+26.2`
- [x] Task 6.4: Jalankan kompilasi bersih `./gradlew build --no-daemon` dan verifikasi seluruh berkas JAR di `Hasil mod/`
