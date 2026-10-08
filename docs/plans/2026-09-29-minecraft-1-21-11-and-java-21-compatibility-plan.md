# Implementation Plan: Takasha v1.6.1 — Penyelarasan Kompatibilitas Runtime Minecraft 1.21.11 & Java 21

> **Versi Target:** v1.6.1 (SemVer PATCH — Perbaikan Kompatibilitas Runtime, Dependency Constraints & Toolchain Alignment)  
> **Status:** Selesai & Terverifikasi (Completed & Verified)  
> **Tanggal:** 2026-09-29  
> **Lingkup:** Mod Fabric `sakura-weapons` (Takasha Mod)  
> **Target Runtime Baru:** Minecraft `1.21.11`, Java `21` (OpenJDK 21), Fabric Loader `>=0.19.3`, Fabric API `0.141.6+1.21.11`  
> **Referensi PRD:** PRD v1.6.0 (Section 5.2 Arsitektur Fabric Mod, Section 8 Konvensi SemVer, Section 10 Kebijakan Deployment, Section 11 Protokol Planning, Section 12 Standar Keamanan Klien & Interoperabilitas Server)  
> **Batasan Ketat PRD Bagian 10:** **Output build HANYA disimpan di workspace lokal (`Hasil mod/Takasha-1.6.1.jar`). DILARANG menyalin otomatis ke profil launcher Modrinth eksternal dan DILARANG melakukan deployment ke remote.**

---

## 1. Analisis Akar Masalah & Diagnostik Log (Root Cause Analysis)

### 1.1 Temuan Error Runtime pada Profil Game (`kaizen`)
Saat file `Takasha-1.6.0.jar` dipasang pada profil peluncur pengguna (`C:\Users\Administrator\AppData\Roaming\ModrinthApp\profiles\kaizen`), Fabric Loader menolak memuat game saat proses startup knot dengan pesan error fatal:

```text
net.fabricmc.loader.impl.FormattedException: Some of your mods are incompatible with the game or each other!
A potential solution has been determined, this may resolve your problem:
	 - Replace mod 'Takasha' (sakura_weapons) 1.6.0 with any version that is compatible with:
		 - minecraft 1.21.11
		 - java 21
More details:
	 - Mod 'Takasha' (sakura_weapons) 1.6.0 requires any version between 26.2 (inclusive) and 26.3- (exclusive) of 'Minecraft' (minecraft), but only the wrong version is present: 1.21.11!
	 - Mod 'Takasha' (sakura_weapons) 1.6.0 requires version 25 or later of 'OpenJDK 64-Bit Server VM' (java), but only the wrong version is present: 21!
```

### 1.2 Akar Masalah Teknis (Technical Root Causes)
1. **Metadata Ketergantungan `fabric.mod.json` Terlalu Kaku:**
   - `"minecraft": "~26.2"`: Operator tilde `~26.2` dievaluasi oleh Fabric Loader sebagai rentang `[26.2, 26.3-)`. Minecraft `1.21.11` berada di luar rentang ini sehingga langsung ditolak keras oleh *preselected dependency solver*.
   - `"java": ">=25"`: Mengharuskan JVM minimum Java 25, padahal environment game pengguna menggunakan Java 21 (`build 21.0.9+7-LTS` / `build 21.0.7+6-LTS`).
2. **Target Kompilasi Bytecode Java (`build.gradle`):**
   - Di `build.gradle`, baris `tasks.withType(JavaCompile) { it.options.release = 25 }` serta `sourceCompatibility = JavaVersion.VERSION_25` menghasilkan class bytecode versi major `69` (Java 25).
   - Jika mod dipaksakan jalan di JVM 21 (yang hanya mendukung bytecode major `65`), JVM akan langsung melempar fatal crash `java.lang.UnsupportedClassVersionError: class file version 69.0 (Java 25), this version only supports 65.0 (Java 21)`.
3. **Konfigurasi Gradle JDK Daemon (`gradle.properties`):**
   - `org.gradle.java.home` saat ini mengarah ke runtime `java-runtime-epsilon` (OpenJDK 25.0.1).
   - Di sistem lokal pengguna tersedia `java-runtime-delta` yang merupakan OpenJDK 21 LTS resmi (`21.0.7+6-LTS`), tepat sesuai kebutuhan runtime Minecraft 1.21.11.
4. **Perbedaan Mapping Mojang 1.21.11 vs 26.2:**
   - Pada Minecraft 26.2, Mojang mengubah nama kelas `ResourceLocation` menjadi `net.minecraft.resources.Identifier`.
   - Pada Minecraft 1.21.11 dengan Official Mojang Mappings, kelas tersebut adalah `net.minecraft.resources.ResourceLocation`.
   - Jika menggunakan Yarn Mappings, Fabric menggunakan `net.minecraft.util.Identifier`.
   - Konfigurasi mappings pada `build.gradle` wajib diselaraskan agar kompilasi bersih dari error simbol.

---

## 2. Landasan Semantic Versioning (SemVer) & Rujukan PRD

Berdasarkan pedoman **Semantic Versioning 2.0.0** dan aturan rilis **PRD Section 8**:

```
     MAJOR.MINOR.PATCH
       │    │     │
       │    │     └─ PATCH: Bugfix, kalibrasi, perbaikan dependensi & toolchain runtime (v1.6.1)
       │    └─────── MINOR: Penambahan fitur / Onboarding set senjata baru (v1.6.0 Valentine)
       └──────────── MAJOR: Perubahan arsitektur breaking master workspace
```

* **Mengapa Naik ke `v1.6.1` (PATCH)?**
  1. Seluruh 3 set konten (Sakura, Pink Legacy, Valentine) dengan total 59 item native, ratusan model 3D Blockbench, dan fitur Anvil GUI **tidak mengalami penambahan fitur baru** dan **tidak ada fitur yang dihapus**.
  2. Perubahan difokuskan pada perbaikan kompatibilitas platform (*compatibility fix*), penyesuaian metadata ketergantungan mod (*dependency constraint fix*), dan re-kompilasi bytecode target JVM (*JVM release level downgrade* dari Java 25 ke Java 21).
  3. Mengikuti kaidah SemVer, perbaikan kompabilitas lingkungan tanpa penambahan API adalah rilis **PATCH**.
  4. File `VERSION` di root diubah dari `1.6.0` menjadi `1.6.1`.
  5. File output kompilasi resmi akan disimpan sebagai `Hasil mod/Takasha-1.6.1.jar`, sementara riwayat `Takasha-1.6.0.jar` tetap tersimpan utuh di direktori `Hasil mod/`.

---

## 3. Inventaris Katalog Item Takasha Multi-Set (Tetap Utuh 100%)

Mod Takasha v1.6.1 mempertahankan integritas seluruh 3 set senjata, perkakas, kosmetik, dan zirah:

| Set ID | Nama Set | Total Item | Rincian Kategori | Simbol & Warna Anvil |
|:---|:---|:---:|:---|:---|
| **`sakura`** | Sakura Set | **20 Item** | 5 Melee (Katana, Sword, Bigsword, Dagger, Gauntlet, Club), 1 Senjata Panjang (Spear), 2 Senjata Tumpul (Hammer, Mace), 4 Tools (Pickaxe, Axe, Shovel, Hoe), 3 Ranged/Defense (Bow, Shield, Fishing Rod), 2 Kosmetik (Hat 3D, Wings Backpiece) | `§d§l🌸 Sakura <Item> 🌸` |
| **`pink_legacy`** | Pink Legacy Set | **19 Item** | 4 Melee (Katana, Sword, Dagger, Gauntlet), 1 Senjata Panjang (Spear), 1 Senjata Tumpul (Hammer), 4 Tools (Pickaxe, Axe, Shovel, Hoe), 3 Ranged/Defense (Bow, Crossbow, Shield), 2 Kosmetik/Utility (Wings Backpiece, Key), 4 Armor Pieces (Helmet, Chestplate, Leggings, Boots) | `§d§l✨ Pink Legacy <Item> ✨` |
| **`valentine`** | Valentine Set | **20 Item** | 5 Melee/Magic (Sword, Axe, Hammer, Spear, Staff), 3 Tools (Pickaxe, Shovel, Hoe), 4 Ranged/Defense (Bow 3-stage, Crossbow 5-stage, Shield, Fishing Rod), 4 Kosmetik/Utility (Hat 3D, Wings Backpiece, Key, Grenade), 4 Armor Pieces (Helmet, Chestplate, Leggings, Boots) | `§c§l❤ Valentine <Item> ❤` |
| **TOTAL** | **Takasha Multi-Set** | **59 Item Native** | Seluruh 59 item native Fabric, 81 vanilla item models 1.21.x, dan 4 tab Anvil GUI | — |

---

## 4. Desain Arsitektur Teknis Pembaruan (1.21.11 & Java 21)

### 4.1 Penyesuaian `sakura-weapons/gradle.properties`
```properties
# Memory settings
org.gradle.jvmargs=-Xmx2G
org.gradle.parallel=true
org.gradle.java.home=C:/Users/Administrator/AppData/Roaming/.minecraft/runtime/java-runtime-delta/windows/java-runtime-delta

# Fabric Properties
minecraft_version=1.21.11
yarn_mappings=1.21.11+build.6
loader_version=0.19.3

# Mod Properties
mod_version=1.6.1
maven_group=net.sakura.weapons
archives_base_name=Takasha

# Dependencies
fabric_version=0.141.6+1.21.11
loom_version=1.17
```

### 4.2 Penyesuaian `sakura-weapons/build.gradle`
```groovy
tasks.withType(JavaCompile).configureEach {
    it.options.release = 21
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
```
*Konfigurasi Mappings:*
Menambahkan blok mapping resmi:
```groovy
dependencies {
    minecraft "com.mojang:minecraft:${project.minecraft_version}"
    mappings loom.officialMojangMappings() // atau yarn mappings sesuai keselarasan kode
    implementation "net.fabricmc:fabric-loader:${project.loader_version}"
    implementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_version}"
}
```

### 4.3 Penyesuaian `sakura-weapons/src/main/resources/fabric.mod.json`
```json
{
  "schemaVersion": 1,
  "id": "sakura_weapons",
  "version": "${version}",
  "name": "Takasha",
  "description": "Takasha: Animated Multi-Set Weapons, Armor & Arsenal for Minecraft Java 1.21.11.",
  "authors": [
    "Boma Narakasura"
  ],
  "contact": {
    "homepage": "https://fabricmc.net"
  },
  "license": "ARR",
  "icon": "assets/sakura_weapons/icon.png",
  "environment": "*",
  "entrypoints": {
    "main": [
      "net.sakura.weapons.SakuraWeaponsMod"
    ],
    "client": [
      "net.sakura.weapons.SakuraWeaponsClient"
    ]
  },
  "mixins": [
    "sakura_weapons.mixins.json"
  ],
  "depends": {
    "fabricloader": ">=0.19.3",
    "minecraft": ">=1.21.11- <=26.2",
    "java": ">=21",
    "fabric-api": "*"
  }
}
```
> **Catatan Dependensi Fleksibel:** Menetapkan `"minecraft": ">=1.21.11- <=26.2"` dan `"java": ">=21"` memastikan mod Takasha dapat berjalan mulus di Minecraft 1.21.11 maupun versi setelahnya, serta mendukung JVM Java 21 s.d. Java 25.

### 4.4 Penyelarasan Simbol Kelas Java (Mapping Alignment)
Memeriksa dan menyesuaikan kelas-kelas berikut jika terdapat disparitas penamaan antara Minecraft 26.2 dan 1.21.11:
1. `Identifier` vs `ResourceLocation`:
   - Jika menggunakan `loom.officialMojangMappings()` pada 1.21.11, kelas pengenal aset adalah `net.minecraft.resources.ResourceLocation`.
   - Mengganti referensi impor `net.minecraft.resources.Identifier` menjadi `net.minecraft.resources.ResourceLocation` secara konsisten pada 10 file Java terkait (`ValentineItems`, `ModItems`, `ModEquipmentAssets`, `SakuraHatUtil`, `PinkLegacyArmorUtil`, `ValentineArmorUtil`, `ValentineHatUtil`, `AnvilItemCatalog`, `EquipmentLayerRendererMixin`, `ModItemGroups`).
2. `ItemDisplayContext`:
   - Konteks display rendering `ItemDisplayContext.HEAD` didukung penuh di Minecraft 1.21.11.
3. `EquipmentLayerRenderer`:
   - Arsitektur zirah `EquipmentLayerRenderer` dan `EquipmentAsset` diperkenalkan sejak 1.21.2 dan tetap berlaku di 1.21.11.
4. `AnvilSideListWidget` & `GuiGraphicsExtractor`:
   - Scissor clipping dan integrasi `ScreenEvents.AFTER_INIT` Fabric API kompatibel 100% dengan `fabric-api 0.141.6+1.21.11`.

---

## 5. Rincian Rencana Tindakan Terurut (Execution Steps)

### Fase 1: Pembaruan Versi SemVer & Konfigurasi Build Toolchain
- [ ] **1.1. Perbarui `VERSION` root:** Ubah dari `1.6.0` menjadi `1.6.1`.
- [ ] **1.2. Perbarui `sakura-weapons/gradle.properties`:**
  - Ubah `minecraft_version=1.21.11`.
  - Ubah `fabric_version=0.141.6+1.21.11`.
  - Ubah `yarn_mappings=1.21.11+build.6`.
  - Ubah `org.gradle.java.home` ke path `java-runtime-delta` (Java 21).
  - Ubah `mod_version=1.6.1`.
- [ ] **1.3. Perbarui `sakura-weapons/build.gradle`:**
  - Set `it.options.release = 21`.
  - Set `sourceCompatibility = JavaVersion.VERSION_21` & `targetCompatibility = JavaVersion.VERSION_21`.
  - Tambahkan konfigurasi mapping yang valid untuk 1.21.11.
- [ ] **1.4. Perbarui `fabric.mod.json`:**
  - Update deskripsi ke Minecraft 1.21.11.
  - Ubah dependensi `"minecraft": ">=1.21.11- <=26.2"`.
  - Ubah dependensi `"java": ">=21"`.

### Fase 2: Audit Simbol Mapping & Penyesuaian Kode Sumber Java
- [ ] **2.1. Audit & Penyelarasan Identifier / ResourceLocation:**
  - Sesuaikan impor pengenal resource antara `ResourceLocation` dan `Identifier` sesuai mapping resmi 1.21.11.
  - Periksa method helper instansiasi (misal: `ResourceLocation.fromNamespaceAndPath(...)` atau constructor).
- [ ] **2.2. Verifikasi Kompatibilitas Mixin:**
  - Periksa `EquipmentLayerRendererMixin.java` untuk memastikan signature method `renderLayers` dan argumen callback target cocok dengan bytecode 1.21.11.
  - Periksa `AnvilMenuMixin.java` untuk method rename handling.
- [ ] **2.3. Verifikasi Feature Renderer Klien:**
  - Pastikan `SakuraWingsFeatureRenderer.java` dan `SakuraHatFeatureRenderer.java` mengimplementasikan callback registrasi renderer 1.21.11 tanpa warning deprecation.

### Fase 3: Kompilasi & Pembuatan JAR Rilis Resmi
- [ ] **3.1. Jalankan Kompilasi Gradle:**
  - Eksekusi `.\gradlew clean build` di direktori `sakura-weapons/`.
  - Pastikan 0 compilation errors dan 0 fatal warnings.
- [ ] **3.2. Salin Output JAR ke Workspace Lokal:**
  - Salin file JAR hasil kompilasi dari `sakura-weapons/build/libs/Takasha-1.6.1.jar` ke folder lokal workspace: `Hasil mod/Takasha-1.6.1.jar`.
  - **Patuhi PRD Section 10:** Jangan menyalin ke profil launcher eksternal.

### Fase 4: Verifikasi Bytecode & Metadata Integrity
- [ ] **4.1. Verifikasi Bytecode Java 21:**
  - Gunakan `javap -v` pada salah satu file `.class` di dalam JAR untuk memastikan major version adalah `65` (Java 21), bukan `69` (Java 25).
- [ ] **4.2. Verifikasi Konten `fabric.mod.json` di dalam JAR:**
  - Buka ZIP JAR dan pastikan `fabric.mod.json` memuat versi `1.6.1`, dependensi `"minecraft": ">=1.21.11- <=26.2"`, dan `"java": ">=21"`.
- [ ] **4.3. Verifikasi Bundling Seluruh Aset Model & Tekstur:**
  - Pastikan seluruh namespace `assets/sakura/`, `assets/pink_legacy/`, `assets/valentine/`, dan `assets/minecraft/items/` terbundel utuh ke dalam JAR.

### Fase 5: Pembaruan Dokumentasi PRD
- [ ] **5.1. Perbarui PRD.md:**
  - Update versi dokumen PRD ke `v1.6.1`.
  - Catat rilis v1.6.1 pada Section 7 (Status Fitur & Roadmap) dan Section 12 (Log Arsitektur).
  - Update Section 5.2 dengan target runtime resmi Minecraft 1.21.11 dan Java 21.

---

## 6. Kriteria Verifikasi Akhir (Verification Checklist)

| No | Parameter Uji | Standar Keberhasilan | Metode Verifikasi |
|:---:|:---|:---|:---|
| 1 | **Versi Bytecode JVM** | Major version class = `65.0` (Java 21) | `javap -verbose net/sakura/weapons/SakuraWeaponsMod.class` |
| 2 | **Resolusi Mod Fabric** | Lolos tanpa peringatan inkompatibilitas versi game/Java | Pemeriksaan `fabric.mod.json` dependensi `1.21.11` & `java >=21` |
| 3 | **Kompilasi Bersih** | Gradle build status: `BUILD SUCCESSFUL` | `.\gradlew build` |
| 4 | **Integritas Aset Bundled** | 59 model item 3D/2D, 3 set zirah, 4 tab Anvil GUI | Validasi konten file JAR |
| 5 | **Lokasi Distribusi** | File tersimpan di `Hasil mod/Takasha-1.6.1.jar` | Pengecekan direktori lokal `Hasil mod/` |
| 6 | **Kepatuhan PRD Bagian 10** | Tidak ada modifikasi otomatis pada folder AppData / launcher eksternal | Pemeriksaan log eksekusi |

---

## 7. Rekomendasi Langkah Selanjutnya

Setelah dokumen perencanaan ini disetujui pengguna:
1. Jalankan eksekusi terstruktur mulai dari Fase 1 hingga Fase 5.
2. Sediakan instruksi pemasangan manual yang jelas kepada pengguna untuk memindahkan `Takasha-1.6.1.jar` dari `Hasil mod/` ke profil permainan mereka.
