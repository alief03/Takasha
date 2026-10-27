# Walkthrough: AuraSkills REA Stat Progression & 1:1 Level GUI Parity

Dokumen ini merangkum perombakan menyeluruh pada subsistem AuraSkills di subproject `:takasha-rpg` berdasarkan artefak rencana perbaikan v3 dan instruksi pengguna.

---

## 1. Ringkasan Perubahan Utama

| Aspek | Sebelum Perubahan | Sesudah Perubahan (Kanonikal 1:1) |
|---|---|---|
| **Sistem AP (Available Stat Points)** | Pemain mendapat 2 AP per level up dan mengalokasikan manual via GUI `/stats`. | **Dihapus total.** Stat meningkat otomatis setiap kali skill naik level sesuai formula resmi AuraSkills. |
| **Command Skill Individual** (`/fighting`, `/farming`, dll) | Hanya mencetak pesan lore singkat di chat box. | **Membuka antarmuka GUI 54-Slot "Kemajuan Level" (Level Progression)** 1:1 dengan layout serpentine track AuraSkills. |
| **Posisi Mouse Kursor pada GUI** | Kursor selalu melompat / reset ke tengah layar setiap berpindah menu atau berganti halaman. | **Zero Cursor Displacement Protocol**: Menggunakan *In-Place Container Reuse* pada menu 54-slot sehingga mouse kursor tetap berada di posisinya tanpa pernah melompat ke tengah. |
| **Kemampuan Skill (Abilities)** | Beberapa ability belum memiliki cooldown, scaling biaya mana dinamis, dan verifikasi unlock level 6. | **55 Passive Abilities & 8 Active Mana Abilities** diaudit sesuai `referensi-rp/RPG/AuraSkills-2.4.0/`, lengkap dengan verifikasi level >= 6 sebelum alat dapat disiagakan (`tryReadyTool`). |

---

## 2. Rincian Teknis Implementasi

### A. Phase 1: `TakashaRpgData.java` (Penghapusan AP & Stat Turunan)
- **Field AP Dihapus:**
  - `availableStatPoints`, `allocatedStats`
  - Method `getAvailableStatPoints()`, `addAvailableStatPoints()`, `allocateStatPoint()`, `resetStatPoints()`
  - Field JSON `available_stat_points` dan `allocated_stats` dihapus dari serialisasi JSON.
- **Kalkulasi Derived Stats Otomatis:**
  - `HEALTH`: Base 0.0 + (Level Pertanian × 1.0) + (Level Bertahan × 1.0) + (0.4 HP per level total).
  - `STRENGTH`: Base 0.0 + (Level Bertarung × 1.0) + (Level Menambang × 0.5) + (Level Menebang × 0.5).
  - `TOUGHNESS`: Base 0.0 + (Level Bertahan × 1.0) + (Level Menambang × 0.5).
  - `LUCK`: Base 0.0 + (Level Memancing × 1.0) + (Level Penggalian × 0.5).
  - `WISDOM`: Base 0.0 + (Level Sihir/Enchanting × 1.0) + (Level Alkimia × 0.5).
  - `REGENERATION`: Base 0.0 + (Level Alkimia × 1.0) + (Level Bertahan × 0.5).
  - `SPEED`: Base 0.0 + (Level Kelincahan × 1.0) + (Level Pertanian × 0.5).
  - `CRIT_CHANCE`: Base 0.0 + (Level Memanah × 0.5) + (Level Bertarung × 0.5).
  - `CRIT_DAMAGE`: Base 0.0 + (Level Memanah × 1.0) + (Level Menebang × 0.5).
- **Helper Ability:**
  - `getPassiveAbilityLevel(skill, slot)`: Level 1 pada skill lv 1, lalu +1 setiap 7-10 level (maks lv 5).
  - `getManaAbilityLevel(skill)`: 0 jika skill < 6; level 1 pada skill lv 6, lalu +1 setiap 7 level.
  - `getManaAbilityCost(skill)`: Konsumsi mana dasar (20) berskala dinamis dengan level kemampuan.
  - `getManaAbilityCooldownSeconds(skill)`: Cooldown dasar (30s) berkurang bertahap seiring naiknya level.

---

### B. Phase 2: `AuraSkillsEngine.java` (Audit Mekanisme Ability & Syarat Level 6)
- **Active Tool Readiness (`tryReadyTool` & `UseItemCallback`):**
  - Pemain yang level skill-nya masih di bawah 6 (`data.getSkillLevel(skill) < 6`) tidak dapat menyiagakan alat untuk active ability (`Replenish`, `Treecapitator`, `Speed Mine`, `Terraform`, `Charged Shot`, `Absorption`, `Sharp Hook`, `Lightning Blade`).
- **Passive Double Drops (`PlayerBlockBreakEvents.AFTER`):**
  - `bountiful_harvest`: Pertanian (gandum, wortel, kentang, bit).
  - `lumberjack`: Penebangan (kayu bulat log).
  - `lucky_miner`: Penambangan (batu bara, besi, tembaga, emas, redstone, lapis, berlian, netherite, kuarsa).
  - `bigger_scoop`: Penggalian (tanah, pasir, kerikil, tanah liat).
  - `metal_detector`: Peluang drop ekstra nugget besi/emas saat menggali.
- **Combat Passives & Active Abilities (`ServerLivingEntityEvents.ALLOW_DAMAGE`):**
  - `sword_master` & `axe_master`: Bonus damage flat/persen.
  - `first_strike`: Bonus damage ekstra jika target masih berdarah penuh (100% HP).
  - `bleed`: Menimbulkan efek withering berkala selama 2-4 detik.
  - `stun`: Panah memicu efek `MobEffects.SLOWNESS` + `MobEffects.WEAKNESS`.
  - `retrieval`: Peluang mengembalikan panah ke inventaris pemanah.
  - `parry`: Menangkis serangan melee (membatalkan damage masuk).
  - `fleeting`: Memberikan `MobEffects.SPEED` darurat saat HP sekarat (<= 35%).

---

### C. Phase 3: `AuraLevelProgressionMenu.java` (GUI Kemajuan Level 1:1)
- **Layout Serpentine Track 24 Slot Kanonikal:**
  - Track slot posisi: `[9, 18, 27, 36, 37, 38, 29, 20, 11, 12, 13, 22, 31, 40, 41, 42, 33, 24, 15, 16, 17, 26, 35, 44]`
  - Menampilkan 24 level per halaman hingga level 100 (5 halaman total).
- **Status Panel Kaca Warna:**
  - `Lime Stained Glass Pane`: Level yang sudah terbuka (**Unlocked**).
  - `Yellow Stained Glass Pane`: Level yang sedang berlangsung (**In Progress**) dengan persentase XP dan progress bar ASCII.
  - `Red Stained Glass Pane`: Level yang belum tercapai (**Locked**).
- **Header Info & Navigasi:**
  - Slot 0: Ikon Skill & Progress Ringkasan.
  - Slot 1: Ranking & Statistik.
  - Slot 2: Sumber XP Skill.
  - Slot 3: Daftar 5 Passive & 1 Active Ability.
  - Slot 4: Hadiah Kenaikan Atribut Otomatis.
  - Slot 45: Tombol Kembali ke Menu Skills.
  - Slot 46: Tombol Tutup GUI.
  - Slot 52: Halaman Sebelumnya.
  - Slot 53: Halaman Berikutnya.
- **Zero Cursor Displacement Protocol:**
  - Jika pemain sudah berada di menu 54-slot (`ReadOnlyChestMenu`), server tidak mengirim paket pembukaan layar baru (`ClientboundOpenScreenPacket`), melainkan memperbarui container dan memanggil `broadcastChanges()`. Kursor mouse tetap berada di titik klik tanpa melompat ke tengah layar.

---

### D. Phase 4: `TakashaRpgCommands.java`
- `/fighting`, `/farming`, `/mining`, `/foraging`, `/fishing`, `/excavation`, `/archery`, `/defense`, `/agility`, `/alchemy`, `/enchanting`, `/combat`, `/healing`:
  - Default: Langsung membuka `AuraLevelProgressionMenu.open(player, skill, 1)`.
  - Subperintah `/skill text`: Menampilkan teks lore untuk konsol atau command block.
- Menghapus referensi AP dari `/skills addlevel`, `/skills stats`, dan `/rank`.

---

### E. Phase 5: `AuraSkillsMenu.java` & `AuraStatsMenu.java`
- **`AuraSkillsMenu`:**
  - Menggunakan In-Place Container Reuse 54-slot.
  - Mengklik ikon salah satu dari 11 skill langsung membuka `AuraLevelProgressionMenu` untuk skill tersebut secara instan tanpa menggerakkan kursor.
  - Slot 48 mengarah ke menu `/stats` tanpa reset kursor.
- **`AuraStatsMenu`:**
  - Seluruh tombol alokasi AP (+1, +5, +10) dan tombol Reset AP dihapus total.
  - Menampilkan 9 atribut karakter dengan penjelasan efek dan skill yang berkontribusi menaikkannya.
  - Slot 49 menyediakan tombol kembali ke Menu Skills tanpa reset kursor.

---

## 3. Hasil Build & Verifikasi

- **Target Build:** `:takasha-rpg:build`
- **Status Kompilasi:** `BUILD SUCCESSFUL in 1m 19s`
- **Output File:**
  - `takasha-rpg/build/libs/Takasha-RPG-1.7.0+26.2.jar` (812,657 bytes)
  - `Hasil mod/Takasha-RPG-1.7.0+26.2.jar` (812,657 bytes)
- **Constraint Check:**
  - Subproject `sakura-weapons`: **STRICTLY ZERO-TOUCH** (tidak ada file di `sakura-weapons` yang diubah).

---

## 4. Perbaikan Stabilitas Target Dummy & Integrasi 9 Stat Kanonikal

### A. Solusi Crash / Force Close Target Dummy
1. **Recursion Guard:** Menambahkan flag instans `isProcessingHit` pada `DamageDummyEntity` dan flag thread-local `TakashaEnchantCombatEngine.setProcessing(true/false)` sebelum memanggil enchantments senjata (Cleave, Koth Sword, Slayers, Deranged, dsb.).
2. **Pembersihan Entitas Otomatis:** Meng-override method `remove(RemovalReason reason)` pada `DamageDummyEntity` agar seluruh TextDisplay (`overheadDisplay` dan `FloatingIndicator`) langsung dibuang dari server ketika dummy mati, hancur, atau chunk dibongkar.
3. **Pemisahan Penanganan Dummy:** `AuraSkillsEngine.ALLOW_DAMAGE` melompati kalkulasi ganda pada dummy karena dummy mengkalkulasikan Critical & Strength secara mandiri di `hurtServer`.

### B. Mekanisme Kanonikal Crit Chance & Crit Damage (AuraSkills 2.4.0)
- **Crit Chance:**
  - Base: 0%. Sumber: **Archery (+1.0% per level)**.
  - Setiap serangan (melee maupun busur/panah) melempar dadu persentase: `random.nextDouble() * 100.0 < critChance`.
  - Serangan yang sukses critical akan memunculkan 12 partikel `ParticleTypes.CRIT` dan memutar suara `SoundEvents.PLAYER_ATTACK_CRIT`.
- **Crit Damage:**
  - Base: 0% bonus. Sumber: **Fighting (+1.0% per level)**.
  - Rumus Multiplier Total:
    - Normal Crit: `Effective Damage = Base Damage * (1.5 + (CritDamage / 100.0))`
    - Jump Crit (Vanilla Jump): `Effective Damage = Base Damage * (1.0 + (CritDamage / 150.0))`
- **Strength (Opsi A Kanonikal):**
  - Sumber: **Foraging (+1.0/lvl)**, **Farming (+0.5/lvl)**, **Archery (+0.5/lvl)**, **Excavation (+0.5/lvl)**.
  - Fighting **tidak** memberikan Strength (hanya Crit Damage & Regeneration).
  - Melee: Dikalibrasi otomatis via `Attributes.ATTACK_DAMAGE` (+0.2 damage per poin stat Strength).
  - Ranged (Panah/Crossbow): Dikalibrasi via trait di `AuraSkillsEngine` (+1% damage panah per poin Strength).

### C. Dynamic Max Mana Engine
- Field statis `private double maxMana = 20.0;` dihapus.
- `getMaxMana()` mengkalkulasikan secara real-time trait `AuraTraitType.MAX_MANA` ($20.0 + \text{Wisdom} \times 2.0$).
- Regenerasi mana per detik diskalakan otomatis sesuai trait `MANA_REGEN` ($1.5 \times (1.0 + \text{Wisdom} / 100.0)$).

### D. Rekonsiliasi Menu GUI & Chat Box
- Deskripsi sumber peningkatan di `/stats` (`AuraStatsMenu.java`) diperbarui 100% konsisten dengan formula aktual Opsi A.
- Pesan chat saat Fighting level-up menampilkan: `Critical Damage: +1.0% | Regeneration: +0.5 (setiap 2 level)`.
- Seluruh teks lawas sisa AP ("+2 Stat Points AP") telah dibersihkan.
