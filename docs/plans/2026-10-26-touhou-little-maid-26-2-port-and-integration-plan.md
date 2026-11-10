# Touhou Little Maid 26.2 Port, Takasha-RPG Advanced Enchantments & Low-Spec Anti-Crash Architecture Plan

> **Target Version:** Minecraft 26.2 (Fabric API 0.161.0+26.2, Fabric Loader 0.19.3, OpenJDK 25 LTS)  
> **Target Hardware Profile:** AMD Ryzen 3 7320U (4C/8T), 5.74 GB RAM (Free ~0.7 GB), AMD Radeon™ 610M iGPU (Shared VRAM)  
> **Source Directory (Sumber Komprehensif):** `d:\Mod Minecraft\weapon set\sakura\referensi-rp\touhou`  
> **Source Artifacts Audited:**  
> 1. `touhoulittlemaid-fabric-0.8.2-neo1.5.3+mc1.21.1.jar` (2,480 decompiled classes)  
> 2. `tlmo-additions-1.0.3+mc1.21.1.jar` (228 decompiled classes)  
> 3. `ysm-2.6.5-fabric+mc1.21.1-release.jar` (729 decompiled classes)  
> **Integrated Systems:** `takasha-rpg` (`AuraSkillsEngine`, `TakashaEnchantCombatEngine`, `TakashaEnchantManager`, `TakashaAoeHelper`)  
> **Mod Output:** `Hasil mod/TouhouLittleMaid-0.8.2+26.2.jar` (Standalone Subproject)  
> **Status:** Draft / Awaiting User Approval (Planning Mode)

---

## 1. Goal Description

Rencana implementasi komprehensif untuk merekonstruksi, mem-porting, dan mengadaptasi ekosistem **Touhou Little Maid (Orihime)** dari `referensi-rp/touhou` ke **Minecraft 26.2** (Fabric Loader 0.19.3, Java 25 LTS) dengan:
1. **Dukungan First-Class Takasha-RPG:** Sinkronisasi atribut dinamis (*Owner Stat Scaling*), integrasi penuh sihir **AdvancedEnchantments** (senjata, armor, bauble, set resonance, on-kill triggers), dan kekebalan friendly-fire 100%.
2. **Arsitektur Optimalisasi Anti-Crash Low-Memory (AMD Ryzen 3 7320U, 6 GB RAM, Radeon 610M):** Mencegah *Out Of Memory (OOM)*, *GPU Driver Timeout (`amdxx64.dll` crash)*, dan *GC micro-stutter* melalui sistem Lazy Loading, LRU Model Cache, Frustum Culling, Animation Distance LOD, dan Particle Clamping.

---

## 2. User Review Required (Key Architectural Decisions)

> [!IMPORTANT]
> **1. Integrasi AdvancedEnchantments Takasha-RPG (Combat, Defense, Set Resonance & On-Kill)**  
> - **Weapon Combat Enchants:** Maid yang memegang senjata dengan custom enchant (misal: `Immolation`, `Cleave`, `Ambit`, `Lifesteal`, `Decapitation`, `Snare`, `Bleed`, `Demonforged`, `Venom`, `Lightning`) memicu pipeline `TakashaEnchantCombatEngine.applyWeaponCombatEnchants(maid, target, source, damage)`. Efek AOE, lifesteal heal maid, dan perbaikan senjata aktif secara otentik.
> - **Armor & Bauble Defense Enchants:** Saat maid menerima damage, memicu `TakashaEnchantCombatEngine.applyArmorCombatEnchants(maid, attacker, source, damage)` (`Cactus`, `Thorns`, `Hardened`, `BurnShield`, `Frozen`).
> - **Owner Legendary Set Resonance Aura:** Jika pemilik mengenakan 1 dari 7 Set Armor Legendaris Takasha-RPG (`Phantom`, `Supreme`, `Ranger`, `Koth`, `Yeti`, `Yijki`, `DimensionalTraveller`), maid dalam radius 16 blok mendapatkan buff aura resonansi (+15% damage, +10% dodge, slow freeze, atau knockback shield).
> - **Friendly-Fire Guarantee:** Serangan sihir area pemain tidak melukai maid, dan serangan AOE maid tidak melukai pemain/pet sekutu via [`TakashaAoeHelper`](file:///d:/Mod%20Minecraft/weapon%20set/sakura/takasha-rpg/src/main/java/net/takasha/rpg/enchant/TakashaAoeHelper.java).

> [!WARNING]
> **2. Strategi Anti-Crash untuk Spesifikasi Device Ini (5.74 GB RAM, Radeon 610M)**  
> Berdasarkan profiling hardware nyata pada sistem ini (RAM fisik 5.74 GB dengan sisa < 1 GB, iGPU Radeon 610M berbagi RAM sebagai VRAM):
> - **Anti-OOM (Lazy Loading & LRU Model Cache):** **DILARANG** memuat seluruh 40+ model Bedrock JSON dan tekstur HD ke dalam RAM/VRAM saat game dibuka. Model dan tekstur **hanya dimuat saat karakter tersebut muncul di dunia** (on-demand), dengan batas maksimal 4 model aktif di memori (LRU cache eviction setelah 5 menit tidak aktif).
> - **Frustum Culling & Distance LOD:** Jika maid tidak terlihat di layar (di luar frustum kamera), evaluasi rotasi tulang Molang dilewati 100%. Untuk maid berjarak 24 - 48 blok, frekuensi animasi diturunkan ke 10 Hz (menghemat 80% beban CPU Ryzen 3). Di atas 48 blok, maid di-render dalam pose statis.
> - **Particle Clamping:** Jumlah partikel sihir AOE (seperti cincin api `Immolation`) dibatasi maksimal 10 - 12 partikel (dari default 60) saat terdeteksi iGPU Radeon 610M guna mencegah *fill-rate choke*.
> - **Zero-Allocation Math Pooling:** Transformasi matriks (`Matrix4f`, `Vector3f`) menggunakan scratch buffer pre-allocated untuk mencegah GC Stop-The-World.
> - **Rekomendasi JVM Heap:** Game client disarankan dialokasikan **3 GB heap** (`-Xms2G -Xmx3G -XX:+UseG1GC`), menyisakan ~2.7 GB untuk OS Windows 11 dan VRAM Radeon 610M.

> [!TIP]
> **3. Arsitektur Subproject Bebas Bloat (`touhou-little-maid`)**  
> Dibuat sebagai subproject mandiri di `sakura-weapons/settings.gradle`. Mod inti `sakura-weapons` tetap berukuran 1.8 MB tanpa membebani jar utama. Output mod: `Hasil mod/TouhouLittleMaid-0.8.2+26.2.jar`.

---

## 3. Matriks Kompatibilitas Takasha-RPG & Touhou Little Maid 26.2

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                         TAKASHA-RPG & MAID ARCHITECTURE                          │
├───────────────────────────────────┬──────────────────────────────────────────────┤
│ Komponen Takasha-RPG              │ Integrasi pada Touhou Little Maid 26.2       │
├───────────────────────────────────┼──────────────────────────────────────────────┤
│ AuraSkillsEngine (7 Atribut)      │ MaidRpgBondingBridge (HP, STR, DEF, SPD, LCK)│
│ TakashaEnchantCombatEngine        │ Trigger Custom Weapon & Armor Enchantments   │
│ TakashaEnchantManager             │ Parse Custom Enchantment NBT dari Senjata    │
│ TakashaAoeHelper                  │ Proteksi Friendly-Fire 100% (AOE & Arrows)   │
│ 7 Armor Set Legendaris            │ Owner Legendary Set Resonance Aura           │
│ On-Kill Pipeline                  │ ExpHunter, SoulStealer, Scavenger Harvest    │
│ BedrockModelCache (Anti-Crash)    │ Lazy Loading & 4-Slot LRU Memory Guard       │
│ Frustum Culling & Distance LOD    │ Hemat 80% Beban CPU Ryzen 3 & VRAM 610M      │
└───────────────────────────────────┴──────────────────────────────────────────────┘
```

---

## 4. Alur Kerja Arsitektur (Architecture Workflow)

```mermaid
graph TD
    subgraph Takasha RPG Engine
        DATA[TakashaRpgData: Stats Pemain] --> STATS[AuraSkills: HP, STR, DEF, SPD, LCK, REG, WIS]
        ENCH[TakashaEnchantCombatEngine] --> AOE[TakashaAoeHelper: Anti Friendly Fire]
        SETS[7 Legendary Armor Sets] --> RES[Set Resonance Aura Handler]
    end

    subgraph Maid RPG Bonding Bridge
        STATS -->|Tick Sync| MRB[MaidRpgBondingBridge]
        MRB -->|Apply Attribute Modifiers| MAID[EntityMaid 26.2]
        RES -->|Aura Buff| MAID
        MAID -->|Melee / Bow Attack| ENCH
        ENCH -->|AOE & Particles Clamped| AOE
    end

    subgraph Low-Memory Optimization Pipeline (AMD Ryzen 3 / Radeon 610M)
        MAID --> CULL[Frustum Culling & Distance LOD Engine]
        CULL -->|Offscreen?| SKIP[Lewati Evaluasi Molang]
        CULL -->|Distance > 24m| LOD[10 Hz Animation Tick]
        CULL -->|Onscreen & < 24m| RENDER[MaidRenderer 26.2]
        RENDER --> LRU[BedrockModelCache: 4-Slot LRU On-Demand]
        LRU --> POOL[Zero-Allocation Scratch Matrix Pool]
        POOL --> GPU[SubmitNodeCollector & Radeon 610M Shared VRAM]
    end
```

---

## 5. Rincian Perubahan Teknis (Phased Implementation Plan)

### Tahap 1: Scaffolding Subproject & Toolchain Gradle 26.2
1. **`sakura-weapons/settings.gradle`:** Tambahkan `include 'touhou-little-maid'`.
2. **`touhou-little-maid/build.gradle`:** Konfigurasi Loom 1.17.21, Minecraft 26.2, Java 25 (`options.release = 25`), dengan dependensi `compileOnly project(':takasha-rpg')`.
3. **`touhou-little-maid/src/main/resources/fabric.mod.json`:** Metadata mod mandiri dengan rekomendasi `takasha_rpg`.

---

### Tahap 2: Komponen Entitas, Advanced Enchantments & Bonding Bridge
1. **`net.sakura.touhou.compat.rpg.MaidRpgBondingBridge.java`:**
   - Menangani sinkronisasi 7 Atribut RPG (HP, Serangan Fisik, Armor/Toughness, Kecepatan Gerak, Keberuntungan, Pemulihan, EXP).
   - Menangani **Set Resonance Aura**: Mendeteksi jika pemilik memakai Full Set `Supreme` (+15% Damage), `Phantom` (+10% Dodge), `Yeti` (Freeze Attack), atau `Koth` (Knockback Shield).
2. **`net.sakura.touhou.compat.enchant.MaidEnchantBridge.java`:**
   - Memicu `TakashaEnchantCombatEngine.applyWeaponCombatEnchants` saat maid menyerang.
   - Memicu `TakashaEnchantCombatEngine.applyArmorCombatEnchants` saat maid diserang.
   - Memicu `applyOnKillEnchants` (`ExpHunter`, `SoulStealer`, `Scavenger`) saat maid mengeksekusi mob.
3. **`net.sakura.touhou.entity.EntityMaid.java`:**
   - Mewarisi `TamableAnimal` Minecraft 26.2.
   - Pengecekan ramah lingkungan: Menggunakan `TakashaAoeHelper` agar serangan area pemain dan maid tidak saling melukai.
4. **`net.sakura.touhou.attachment.MaidDataAttachment.java`:**
   - Menyimpan data inventori 36-slot, item bauble, tingkat kasih sayang, dan status ikatan RPG via Fabric Data Attachment API 26.2.

---

### Tahap 3: Pipeline Rendering RenderState 26.2 & Low-Memory Anti-Crash Engine
1. **`net.sakura.touhou.client.render.cache.BedrockModelCache.java` (Anti-Crash Core):**
   - **Lazy Loader:** Membaca JSON dan tekstur Bedrock hanya saat model ID pertama kali di-request oleh maid di dunia.
   - **LRU Cache Eviction:** Maksimal 4 entri model 3D di memori secara simultan. Jika cache penuh atau model tidak digunakan selama 5 menit, tekstur di-*unload* dari GPU VRAM.
2. **`net.sakura.touhou.client.render.state.MaidRenderState.java`:**
   - State visual pembawa data model ID, status duduk/tidur, item genggam, dan pose animasi.
3. **`net.sakura.touhou.client.render.MaidRenderer.java`:**
   - Mengimplementasikan **Frustum Culling**: Jika `!camera.isBoundingBoxInFrustum(maid.getBoundingBox())`, evaluasi Molang diskip.
   - Mengimplementasikan **Distance LOD**:
     - $\le 24$ blok: Animasi penuh 60 FPS.
     - $24 - 48$ blok: Animasi 10 Hz.
     - $> 48$ blok: Pose statis.
   - Menggunakan `SubmitNodeCollector` dan `CameraRenderState` Minecraft 26.2.
4. **`net.sakura.touhou.client.render.pool.MatrixPool.java`:**
   - Scratch buffer pre-allocated untuk komputasi matriks tulang Bedrock, mencegah alokasi jutaan objek sementara per menit (zero GC thrashing).

---

### Tahap 4: Migrasi Model Pack & Aset Otentik (`tlm_custom_pack`)
- Ekstraksi aset dari `referensi-rp/touhou/.../tlm_custom_pack/touhou_little_maid-1.0.0/assets/touhou_little_maid/` ke `touhou-little-maid/src/main/resources/assets/touhou_little_maid/`:
  - 40+ Karakter Touhou (`maid_model.json`, model Bedrock JSON, dan tekstur PNG).
  - Audio otentik & dialog suara maid (`sounds.json`, audio OGG, bahasa `en_us.json` dan `id_id.json`).

---

### Tahap 5: GUI Maid dengan Tab Status RPG
- **`net.sakura.touhou.client.gui.MaidRpgStatusScreen.java`:**
  - Menampilkan panel status RPG di inventaris maid: HP, Damage fisik, Kecepatan, Armor, Indikator Set Resonance aktif, dan daftar Custom Enchantment yang terpasang pada senjata/zirah maid.

---

## 6. Verification Plan

### Automated Tests & Compilations
1. **Gradle Build Subproject (Memory Cap 2GB):**
   ```powershell
   cd "d:\Mod Minecraft\weapon set\sakura\sakura-weapons"
   .\gradlew.bat :touhou-little-maid:build
   ```
   *Expected:* Kompilasi sukses tanpa OOM, output JAR: `Hasil mod/TouhouLittleMaid-0.8.2+26.2.jar`.
2. **Bytecode Verification Java 25:**
   ```powershell
   & "C:\Users\Administrator\AppData\Roaming\.minecraft\runtime\java-runtime-epsilon\windows\java-runtime-epsilon\bin\javap.exe" -v -cp "..\Hasil mod\TouhouLittleMaid-0.8.2+26.2.jar" "net.sakura.touhou.entity.EntityMaid"
   ```
   *Expected:* `major version: 69` (Java 25).

### In-Game Stress Tests & Verification (Minecraft 26.2 di AMD Ryzen 3 / Radeon 610M)
1. **Memory Footprint & Anti-Crash Test:**
   - Alokasikan `-Xmx3G` pada launcher Minecraft.
   - Masuk dunia dan spawn 5 maid dengan model berbeda (misal Reimu, Marisa, Sakuya, Flandre, Youmu).
   - Tekan F3: Verifikasi penggunaan RAM tidak melompat melebihi batas 2.5 GB, dan framerate stabil 50-60 FPS pada Radeon 610M.
   - Pindah jarak > 48 blok: Verifikasi animasi beralih ke LOD statis tanpa lag CPU.
2. **Pengujian AdvancedEnchantments Senjata Maid:**
   - Berikan pedang ber-enchant `Immolation III` dan `Lifesteal II` ke maid.
   - Perintahkan maid menyerang sekelompok zombie.
   - Verifikasi: Cincin api radial muncul dengan partikel yang telah di-clamp (10-12 partikel, bebas drop FPS), zombie di sekitar terbakar, dan HP maid pulih saat menyerang.
3. **Pengujian Armor Defensive Enchant:**
   - Pasang zirah ber-enchant `Thorns` atau `Frozen` pada maid.
   - Biarkan skeleton memanah maid: Verifikasi skeleton menerima pantulan damage dan terkena Slowness.
4. **Pengujian Set Resonance Aura:**
   - Pemain memakai Full Set `Supreme Armor`: Verifikasi maid di dekat pemain mendapatkan partikel api halus dan bonus +15% damage pukulan.
5. **Pengujian Friendly-Fire Immunity:**
   - Pemain menggunakan jurus pedang AOE atau tombak petir tepat di sebelah maid.
   - Verifikasi: Maid 100% tidak menerima damage atau efek knockback.
