# Rencana Komprehensif: Full Custom Enchant Engine (Combat & Utility) + AE GUI Cursor-Snap Elimination

> **Target Version:** `v1.3.0+26.2`  
> **Modul:** `takasha-rpg` (Java 25, Fabric 1.21.4 / 26.2)  
> **Dependensi:** `sakura-weapons` **LOCKED** pada `v2.5.0` (Tidak diubah sama sekali)  
> **Status:** Siap Dieksekusi  

---

## 1. Executive Summary & Tanggapan Umpan Balik Pengguna

Berdasarkan koreksi dan umpan balik pengguna:
1. **Klarifikasi Cursor Snap:**  
   Pengguna mengklarifikasi bahwa cursor mouse **masih terkadang melompat / snap ke tengah layar** saat membuka atau bertransisi di antarmuka GUI **AdvancedEnchantments** (meskipun belum diketahui persis di GUI mana antara `/ce` Enchanter, konfirmasi pembelian, `/alchem`, `/tinker`, atau `/enchants`).  
   *Tindakan:* Dilakukan audit menyeluruh pada seluruh GUI AdvancedEnchantments dan penyempurnaan `MouseHandlerMixin` di sisi client serta penghapusan penutupan kontainer paksa (`closeContainer()`) di sisi server.
2. **Cakupan Enchantment: Tidak Hanya Tempur (Combat), Namun Juga Utilitas (Non-Combat / Utility):**  
   Pengguna menegaskan: *"pastikan tidak hanya tempur saja, seperti replanter dll"*.  
   *Tindakan:* Sistem engine enchantment diperluas dari sekadar Combat Engine menjadi **Dua Engine Komplementer**:
   - `TakashaEnchantCombatEngine`: Menangani 107 combat & status enchantments (Melee, Ranged/Projectile resolver termasuk `virus` Poison + Wither, Armor Thorns & Cleansing).
   - `TakashaEnchantUtilityEngine`: Menangani 45+ non-combat utility enchantments:
     * **Pertanian (Farming)**: `replanter`, `harvest`, `planter`, `carrotplanter`, `potatoplanter`.
     * **Pertambangan (Mining)**: `telepathy`, `smelting` / `autosmelt`, `veinminer`, `blast_mining` / `trench`, `hasten`, `experience`, `gemify`, `multiplication`, `oxygenate`.
     * **Mobilitas & Ketahanan (Mobility & Defense)**: `jellylegs` / `feather_step`, `gears`, `springs`, `waterwalker`.
     * **Memancing (Fishing)**: `autoreel`, `firehook`, `poisonedhook`, `sharpnesshook`, `hook`.
     * **Survival Pasif**: `implants` / `alienimplants`, `masterblacksmith`.
3. **Penyelarasan PRD & Semantic Versioning:**  
   Menaikkan versi dari `1.2.0+26.2` ke `1.3.0+26.2` di `takasha-rpg/build.gradle`.

---

## 2. Audit Teknis Audit Forensik (Audit Forensik)

### A. Akar Masalah Bug Status Effect `virus` & Senjata Proyektil (Crossbow/Bow)
1. **Omission di Dummy**: Di `DamageDummyEntity.java` baris 153–189, pengecekan string hanya mencakup 12 kata kunci: `bleed`, `freeze`, `burn`, `shock`, `silence`, `disarm`, `fracture`, `snowblind`, `poison`, `wither`, `slow`, `weak`. Enchantment **`virus`** tidak mengandung kata "poison" atau "wither", sehingga terabaikan (*silent drop*).
2. **Spesifikasi Resmi `virus` di AdvancedEnchantments**:
   ```yaml
   virus:
     type: SHOOT
     applies-to: Bow, Crossbow
     effects:
       - "POTION:POISON:2:120 @Victim"
       - "POTION:WITHER:2:120 @Victim"
   ```
   `virus` memberikan **DUA** efek sekaligus: **Poison II** dan **Wither II** selama 6 detik (120 tick).
3. **Penyebab Gagal Deteksi Senjata Panah (Crossbow)**:
   Saat terkena tembakan panah:
   - `damageSource.getEntity()` adalah pemain penembak (`ServerPlayer`).
   - `damageSource.getDirectEntity()` adalah panah (`AbstractArrow` / `Projectile`).
   - Kode dummy sebelumnya hanya membaca `attacker.getMainHandItem()`. Jika Crossbow dipegang di *off-hand* atau pemain mengganti item setelah menembak, enchant tidak terdeteksi.
   - Solusi: Di Minecraft 1.21+ / 26.2, gunakan `((AbstractArrow) projectile).getWeaponItem()` dengan fallback ke `mainHand` dan `offHand`.

---

### B. Akar Masalah Cursor Snap ke Tengah di GUI AdvancedEnchantments
Melalui pelacakan kode di `MouseHandlerMixin.java` dan kelas menu (`EnchanterMenu`, `AlchemistMenu`, `TinkererMenu`):
1. **Race Condition di `MouseHandlerMixin`**:
   Saat layar GUI ditutup sesaat (ketika server mengganti container menu dari 18-slot Enchanter ke 27-slot Confirmation, atau membuka Tinkerer/Alchemist):
   - Vanilla memanggil `grabMouse()`.
   - Di frame berikutnya, `turnPlayer` terpanggil oleh delta mouse sisa (`accumulatedDX != 0`), yang langsung mereset `takasha$hasSavedGuiPosition = false`.
   - Akibatnya, saat GUI baru terbuka beberapa milidetik kemudian, `releaseMouse` menemukan `hasSavedGuiPosition == false` dan vanilla mengeksekusi:
     ```java
     this.xpos = screenWidth / 2;
     this.ypos = screenHeight / 2;
     ```
     Kursor mouse pun langsung melompat ke tengah layar!
2. **Panggilan `player.closeContainer()` yang Tidak Perlu**:
   Di `EnchanterMenu.java` baris 147 dan 155:
   ```java
   case 9 -> { player.closeContainer(); TinkererMenu.open(player); }
   case 17 -> { player.closeContainer(); AlchemistMenu.open(player); }
   ```
   Memanggil `player.closeContainer()` mengirimkan `ClientboundContainerClosePacket` ke client yang memaksa penutupan layar dan penguncian mouse, sebelum paket `ClientboundOpenScreenPacket` berikutnya tiba. Di Fabric/Vanilla, membuka menu baru (`player.openMenu(...)`) secara otomatis menangani penutupan menu lama tanpa jitter.
3. **Perbedaan Ukuran Slot Enchanter (18 slot) vs Konfirmasi (27 slot)**:
   Transisi dari 18 slot ke 27 slot memaksa pembuatan instance GUI baru di client.

---

## 3. Matriks Audit Lengkap Enchantment (Combat & Utility)

### A. Non-Combat & Utility Enchantments (45+ Enchantments)
| Kategori | Enchantment ID | Target Item | Efek & Mekanisme Teknis Fabric |
| :--- | :--- | :--- | :--- |
| **Pertanian** | `replanter` | Cangkul (Hoe) | Memanen tanaman matang (Gandum, Wortel, Kentang, Beetroot, Nether Wart) dan menanamnya kembali secara instan pada `age = 0`. |
| **Pertanian** | `harvest` | Cangkul (Hoe) | Memanen dan menanam kembali seluruh tanaman matang dalam radius 3x3. |
| **Pertanian** | `planter`, `carrotplanter`, `potatoplanter` | Cangkul / Seeds | Menanam benih dari inventori pemain ke blok farmland di sekitar dalam radius 3x3. |
| **Pertambangan** | `telepathy` | Beliung, Sekop, Kapak | Seluruh hasil tambang / block drops langsung ditransfer ke inventaris pemain (tidak jatuh ke tanah). |
| **Pertambangan** | `smelting`, `autosmelt` | Beliung (Pickaxe) | Melebur ore secara otomatis (Iron Ore -> Iron Ingot, Raw Gold -> Gold Ingot, Sand -> Glass, Cobblestone -> Stone). |
| **Pertambangan** | `veinminer` | Beliung (Pickaxe) | Menambang seluruh blok urat bijih yang tersambung (connected ore vein) hingga batas 24 blok. |
| **Pertambangan** | `blast_mining`, `trench` | Beliung / Sekop | Menghancurkan area batuan tambang 3x3 secara serentak dengan drop terkumpul. |
| **Pertambangan** | `hasten` | Beliung (Pickaxe) | Peluang memberikan buff Haste I–II saat menambang blok. |
| **Pertambangan** | `experience` | Beliung (Pickaxe) | Melipatgandakan exp orb yang dijatuhkan dari penambangan bijih. |
| **Pertambangan** | `oxygenate` | Beliung / Helm | Mengisi penuh gelembung oksigen saat menambang di bawah air. |
| **Mobilitas** | `jellylegs`, `feather_step` | Sepatu (Boots) | Meniadakan 100% Fall Damage (`source.is(DamageTypes.FALL)` dibatalkan). |
| **Mobilitas** | `gears` | Sepatu (Boots) | Memberikan buff `Speed` pasif konstan selama sepatu terpasang. |
| **Mobilitas** | `springs` | Sepatu (Boots) | Memberikan buff `Jump Boost` pasif konstan selama sepatu terpasang. |
| **Mobilitas** | `waterwalker` | Sepatu (Boots) | Berjalan di atas air layaknya daratan (menempatkan frosted ice sementara / buff kecepatan air). |
| **Survival Pasif** | `implants`, `alienimplants` | Helm / Armor | Memulihkan poin kelaparan (Hunger) dan saturasi secara berkala setiap interval tick. |
| **Survival Pasif** | `masterblacksmith` | Armor / Senjata | Memperbaiki durability peralatan yang rusak secara bertahap saat bertualang. |
| **Memancing** | `autoreel` | Pancingan (Rod) | Menarik kail pancing secara otomatis ketika ikan menggigit umpan. |
| **Memancing** | `firehook`, `poisonedhook` | Pancingan (Rod) | Membakar / meracuni entitas yang tersangkut oleh kail pancing. |

---

### B. Combat & Status Enchantments (107 Enchantments)
| Sub-Kategori | Enchantment ID | Target Senjata / Armor | Efek & Status Mekanisme |
| :--- | :--- | :--- | :--- |
| **Ranged Debuff** | `virus` | Crossbow, Bow | **Poison II** + **Wither II** (6–8 detik) pada target. |
| **Ranged Debuff** | `perish` | Crossbow, Bow | Efek **Wither II–III** berkepanjangan pada target. |
| **Ranged Debuff** | `frost` | Crossbow, Bow | Efek **Freeze** kustom + **Slowness II**. |
| **Ranged Debuff** | `contagion` | Crossbow, Bow | Efek **Poison AOE** menyebar ke musuh di sekitar target. |
| **Ranged Debuff** | `flashbang` | Crossbow, Bow | Efek **Blindness** dan **Slowness** pada target. |
| **Ranged Elemental** | `strike`, `lightning`, `thor` | Crossbow, Bow | Menghantam target dengan sambaran petir (**Shock** status). |
| **Ranged Elemental** | `hellfire`, `flame` | Crossbow, Bow | Membakar target dengan api bertingkat tinggi (**Burn** status). |
| **Melee Custom AE** | `bleed`, `deepbleed`, `lacerate`, `gouge` | Pedang / Kapak | Memberikan status **Bleed** (DoT pendarahan darah). |
| **Melee Custom AE** | `iceaspect`, `permafrost`, `glacier` | Pedang / Kapak | Memberikan status **Freeze** (pembekuan gerak). |
| **Melee Custom AE** | `burn`, `immolation`, `divineimmolation` | Pedang / Kapak | Memberikan status **Burn** (api murni berdurasi tinggi). |
| **Melee Custom AE** | `shock`, `thunderlord`, `deranged` | Pedang / Kapak | Memberikan status **Shock** (kelumpuhan listrik). |
| **Melee Custom AE** | `silence`, `mute` | Pedang / Kapak | Memberikan status **Silence** (menonaktifkan skill lawan). |
| **Melee Custom AE** | `disarm`, `disarmor` | Pedang / Kapak | Memberikan status **Disarm** (melumpuhkan senjata lawan). |
| **Melee Custom AE** | `fracture`, `mightycleave`, `shatter` | Pedang / Kapak | Memberikan status **Fracture** (mengurangi armor lawan). |
| **Melee Potion** | `confuse` | Pedang / Kapak | Memberikan efek **Nausea / Pusing**. |
| **Melee Potion** | `diminish`, `dominate` | Pedang / Kapak | Memberikan efek **Weakness** dan Mining Fatigue. |
| **Melee Potion** | `stun`, `cripple` | Pedang / Kapak | Memberikan efek **Slowness** berat. |
| **Melee Lifesteal** | `lifesteal`, `chainlifesteal` | Pedang / Kapak | Menyerap persentase HP lawan menjadi darah pengguna. |
| **Armor Retaliation** | `molten`, `scorching` | Chestplate, Leggings | Membakar balik penyerang saat pengguna terkena serangan. |
| **Armor Retaliation** | `frozen` | Chestplate, Leggings | Memberikan efek Slow & Freeze kepada penyerang. |
| **Armor Retaliation** | `unholy` | Chestplate, Leggings | Memberikan efek Wither & Weakness kepada penyerang. |
| **Armor Cleansing** | `clarity`, `lucid`, `metaphysical` | Helm, Armor | Membersihkan debuff Blindness, Poison, Darkness, dan Slowness. |

---

## 4. Rancang Bangun Arsitektur Teknis

```
[Fabric Game Events]
       │
       ├── PlayerBlockBreakEvents.BEFORE / AFTER ──► [TakashaEnchantUtilityEngine]
       │                                                 ├── Farming: replanter, harvest, planter
       │                                                 └── Mining: telepathy, smelting, veinminer
       │
       ├── ServerTickEvents.START_PLAYER_TICK ────► [TakashaEnchantUtilityEngine]
       │                                                 ├── Passives: gears (Speed), springs (Jump)
       │                                                 └── Survival: implants (Hunger regen)
       │
       └── ServerLivingEntityEvents.ALLOW_DAMAGE ─► [TakashaEnchantCombatEngine]
                                                         ├── Ranged Resolver: Crossbow / Bow / Arrow
                                                         ├── Status Applicator: virus (Poison+Wither), etc.
                                                         ├── DamageDummyEntity Direct Hook
                                                         └── Armor Defense & Retaliation
```

### Komponen 1: `TakashaEnchantCombatEngine.java` (BARU)
- **Lokasi:** `net.takasha.rpg.enchant.TakashaEnchantCombatEngine`
- **Fungsi Utama:**
  - `resolveWeapon(LivingEntity attacker, DamageSource source)`:
    Mendeteksi apakah damage berasal dari proyektil (`AbstractArrow`). Jika ya, ambil `arrow.getWeaponItem()`. Jika kosong, lakukan fallback ke tangan penyerang (`attacker.getMainHandItem()` dan `attacker.getOffhandItem()`).
  - `applyWeaponCombatEnchants(LivingEntity attacker, LivingEntity target, DamageSource source, float damage)`:
    Mengeksekusi seluruh 107 combat enchantments. Menjamin `virus` memberikan `MobEffects.POISON` dan `MobEffects.WITHER` dengan amplifier dan durasi yang sesuai level buku (Level 1–4).
  - `applyArmorCombatEnchants(LivingEntity defender, LivingEntity attacker, DamageSource source, float damage)`:
    Mengeksekusi Thorns, Molten, Frozen, dan Shielding.

### Komponen 2: `TakashaEnchantUtilityEngine.java` (BARU)
- **Lokasi:** `net.takasha.rpg.enchant.TakashaEnchantUtilityEngine`
- **Fungsi Utama:**
  - `handleBlockBreak(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state, BlockEntity entity)`:
    1. **Replanter**: Memeriksa apakah `state.getBlock() instanceof CropBlock` dan `crop.isMaxAge(state)`. Jika cangkul memiliki `replanter`, tanam kembali benih tanaman pada blok tersebut di tick yang sama (`crop.getStateForAge(0)`).
    2. **Harvest**: Jika memiliki `harvest`, lakukan pemanenan dan penanaman kembali dalam area 3x3 di sekitar blok yang dipukul.
    3. **Telepathy**: Mencegah block drop jatuh ke tanah; langsung memasukkan item hasil tambang ke `player.getInventory()`.
    4. **Smelting / Autosmelt**: Melebur item mentah (`Raw Iron -> Iron Ingot`, `Raw Copper -> Copper Ingot`, `Raw Gold -> Gold Ingot`, `Cobblestone -> Stone`, `Sand -> Glass`).
    5. **Vein Miner**: Mencari blok bijih sejenis yang bersentuhan (rekursif hingga 24 blok) dan memecahkannya secara serentak.
    6. **Blast Mining / Trench**: Memecahkan area kubus 3x3x3 di sekitar blok tambang.
  - `handlePlayerTick(ServerPlayer player)`:
    Memeriksa armor yang terpasang setiap 20 tick (1 detik):
    - `gears` -> Berikan buff `MobEffects.MOVEMENT_SPEED`.
    - `springs` -> Berikan buff `MobEffects.JUMP_BOOST`.
    - `implants` / `alienimplants` -> Tambahkan 1 titik kelaparan (`player.getFoodData().eat(1, 0.5f)`).
    - `masterblacksmith` -> Perbaiki 1 durability perlengkapan yang rusak.
  - `handleFallDamage(LivingEntity entity, DamageSource source)`:
    Jika entitas mengenakan sepatu dengan `jellylegs` atau `feather_step`, batalkan damage jatuh (`return false`).

### Komponen 3: Penyempurnaan `DamageDummyEntity.java`
- Menghubungkan hurt event langsung ke `TakashaEnchantCombatEngine.applyWeaponCombatEnchants()`.
- Menjadikan dummy target uji coba yang menerima **100% status effects** (tanpa batasan chance gacha) agar pemain dapat memverifikasi efek enchant secara akurat.
- Memperbarui hologram overhead multi-baris agar menampilkan:
  - Baris 1: Nama Dummy & Total Damage / DPS
  - Baris 2: Vanilla Effects (`[POISON II (5s)] [WITHER II (5s)]`)
  - Baris 3: Custom AE Statuses (`[BLEED IV (6s)] [FREEZE III (4s)] [SHOCK (3s)]`)

### Komponen 4: Eliminasi Permanen Cursor Snap ke Tengah di Seluruh GUI AE
1. **Penyempurnaan `MouseHandlerMixin.java`**:
   - Jangan pernah menghapus koordinat kursor yang tersimpan (`takasha$hasSavedGuiPosition`) di dalam `turnPlayer` jika waktu penutupan GUI terakhir masih di bawah 2.0 detik.
   - Simpan koordinat kursor secara konsisten pada setiap event pergerakan mouse di GUI.
   - Intersep pembukaan layar baru (`releaseMouse` dan `Screen.init` / `setScreen`): jika transisi terjadi antar GUI, pulihkan posisi mouse tepat di mana kursor berada sebelum pergantian layar.
2. **Pembersihan Server-Side GUI Menu**:
   - Di `EnchanterMenu.java`, hapus `player.closeContainer()` di tombol navigasi (slot 9 Tinkerer dan slot 17 Alchemist).
   - Pastikan transisi antara menu konfirmasi pembelian dan menu utama menggunakan in-place update atau transisi aman tanpa paket close ganda.

---

## 5. Rencana Tahapan Eksekusi (Atomic Checklist)

- [ ] **Fase 1: Implementasi `TakashaEnchantCombatEngine.java`**
  - Buat kelas `net.takasha.rpg.enchant.TakashaEnchantCombatEngine`.
  - Implementasikan resolver senjata proyektil cerdas (`resolveWeapon`) untuk Crossbow, Bow, Trident, dan Arrow.
  - Implementasikan mapping dan eksekusi 107 combat enchantments (khususnya `virus` -> Poison II + Wither II).
  - Hubungkan ke `ServerLivingEntityEvents.ALLOW_DAMAGE` di `TakashaRpgMod`.

- [ ] **Fase 2: Implementasi `TakashaEnchantUtilityEngine.java`**
  - Buat kelas `net.takasha.rpg.enchant.TakashaEnchantUtilityEngine`.
  - Implementasikan `PlayerBlockBreakEvents.BEFORE` & `AFTER` untuk `replanter`, `harvest`, `planter`, `telepathy`, `smelting`, `veinminer`, `blast_mining`, `trench`.
  - Implementasikan `ServerTickEvents.START_PLAYER_TICK` untuk pasif `gears`, `springs`, `implants`, `masterblacksmith`.
  - Daftarkan pembatalan fall damage untuk `jellylegs` / `feather_step`.

- [ ] **Fase 3: Refactoring & Penguatan `DamageDummyEntity.java`**
  - Delegasikan trigger enchant ke `TakashaEnchantCombatEngine`.
  - Dukung visualisasi status effect vanilla dan custom AE pada overhead hologram display.

- [ ] **Fase 4: Perbaikan Menyeluruh Cursor-Snap GUI AdvancedEnchantments**
  - Perbaiki `MouseHandlerMixin.java` agar tidak mengalami race condition dengan `turnPlayer` saat transisi layar.
  - Hapus panggilan redundan `player.closeContainer()` di `EnchanterMenu.java`.

- [ ] **Fase 5: Pembaruan PRD & Semantic Versioning**
  - Ubah `globalModVersion = '1.3.0'` di `takasha-rpg/build.gradle` (output jar: `Takasha-RPG-1.3.0+26.2.jar`).
  - Perbarui catatan PRD / changelog dokumentasi.

- [ ] **Fase 6: Kompilasi, Build & Verifikasi**
  - Jalankan build Gradle: `.\gradlew.bat :takasha-rpg:build` di direktori `sakura-weapons`.
  - Verifikasi jar terdistribusi di `Hasil mod/Takasha-RPG-1.3.0+26.2.jar`.
  - Pastikan modul `sakura-weapons` tetap murni tidak tersentuh pada `v2.5.0`.
