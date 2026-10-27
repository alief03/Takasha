# Walkthrough: Full REA 336 Custom Enchantments Audit & Combat Engine Overhaul (Takasha-RPG v1.5.0)

## 1. Ringkasan Eksekutif
Dalam pembaruan ini, seluruh 336 custom enchantment (182 AdvancedEnchantments otentik + 154 Custom RPG) telah diaudit secara menyeluruh dengan REA skill dari berkas referensi `referensi-rp\RPG\AdvancedEnchantments-9.24.15\enchantments.yml`.

Akar masalah di mana enchant seperti **Immolation** tidak berfungsi telah teridentifikasi dan diperbaiki secara tuntas:
- **Penyebab:** Pada engine lama, pengecekan string parsial hanya mencocokkan `"burn"` atau `"ignit"`, sehingga sihir `"immolation"` terabaikan (*silent drop*). Selain itu, sistem lama hanya membakar target tunggal tanpa query spasial AoE atau partikel lingkaran.
- **Solusi:** Dibuat kelas pembantu utilitas spasial `TakashaAoeHelper` dan dilakukan *overhaul* menyeluruh pada `TakashaEnchantCombatEngine` serta integrasi DoT damage pada `DamageDummyEntity`.

---

## 2. Rincian Perubahan Kode

### A. Utilitas Spasial & Partikel Matematis (`TakashaAoeHelper.java`)
- **Lokasi:** `takasha-rpg/src/main/java/net/takasha/rpg/enchant/TakashaAoeHelper.java`
- **Fitur:**
  1. `getEntitiesInRadius`: Menghitung AABB dan jarak Euclidean murni dari titik pusat.
  2. **Filter & Proteksi Friendly-Fire:** Hanya menargetkan `Monster`, `Enemy`, dan `DamageDummyEntity`. Menolak secara ketat `Player`, hewan peliharaan jinak (`TamableAnimal.isTame()`), dan Touhou Little Maid (`takasha_maid`).
  3. `spawnParticleRing` & `spawnParticleRingDouble`: Merender cincin partikel horisontal di atas permukaan tanah menggunakan formula parametrik $X = \cos(\theta) \cdot r$ dan $Z = \sin(\theta) \cdot r$.

### B. Overhaul Combat Engine Dispatcher (`TakashaEnchantCombatEngine.java`)
- **Lokasi:** `takasha-rpg/src/main/java/net/takasha/rpg/enchant/TakashaEnchantCombatEngine.java`
- **Fitur Baru & Perbaikan:**
  1. **Radial AOE Attacks:**
     - `immolation`: Membakar target dan musuh dalam radius 2/4/6 blok selama `40 + lvl * 20` ticks, serta memunculkan partikel cincin ganda lava `FALLING_LAVA` + `LAVA`.
     - `inflame`: Api melingkar instan radius $3 + \text{lvl}$ blok.
     - `cleave` / `carve` / `mightycleave`: Sweep damage radial $1.5 \times \text{lvl}$ + debuff `fracture` + partikel sapuan pedang `SWEEP_ATTACK`.
     - `deranged`: Kejutan petir kosmik radius $4 + \text{lvl}$ blok.
     - `killaura`: Tebasan otomatis kontinu terhadap musuh sekitar.
     - `ambit`: Serangan tebasan radial melingkar.
     - `contagion`: Ledakan racun beracun menyebar ke sekeliling victim saat terpukul.
  2. **Mob-Specific Slayers:**
     - Multiplier dinamis terhadap Skeleton (`bonecrusher`), Zombie (`deathpunch`), Ender (`enderslayer`/`enderbane`/`endmaster`), Nether mobs (`blazereaper`/`netherslayer`/`netherling`), Arthropods/Beasts (`beastslayer`/`hunter`), Slimes (`cubism`), Warden/Gelap (`brightness`), dan Hostile mobs (`soulless`).
  3. **Melee Status Debuffs & DoTs:**
     - `frost`, `bleed`, `shock`, `silence`, `disarm`, `fracture`, `flashbang`, `confuse`, `diminish`, `stun`, `famine`.
  4. **Procs & Criticals:**
     - `lifesteal`, `berserk`, `critical`, `execute`, `allure`/`magnet` (tarik), `forcefield`/`repel` (dorong), `explosive`.
  5. **Armor Defense Triggers:**
     - `creeperarmor` (negasi ledakan 100%), `arrowdeflect`/`arrowbreak` (tangkis panah), `dodge` (tepis fisik), `jellylegs`/`feather_step` (negasi fall damage), `plummet` (fall damage jadi gempa AoE), `shockwave` (dorongan angin radial), `smokebomb` (asap & blind), `reflect` (pantul damage), `guard` (absorption), `escape` (speed + regen saat sekarat), Cleansing (`clarity`, `lucid`, `metaphysical`, `blessed`).
  6. **On-Kill Triggers:**
     - `decapitation` (drop kepala mob), `devour` (restore food & saturation), `rebound` (heal HP), `inquisitive` (bonus exp orb).

### C. Paritas Penuh Boneka Latihan (`DamageDummyEntity.java`)
- **Lokasi:** `takasha-rpg/src/main/java/net/takasha/rpg/entity/DamageDummyEntity.java`
- **Perbaikan DoT Api:**
  - Status `burnTicks > 0` kini mengurangi HP dummy sebesar 1.0 setiap 20 ticks (1 detik).
  - Damage tercatat ke dalam sistem sliding window DPS 5 detik.
  - Memunculkan indikator melayang oranye khas api: `§6-1.0 🔥`.

### D. Pengkabelan Event Fabric (`TakashaRpgMod.java`)
- **Lokasi:** `takasha-rpg/src/main/java/net/takasha/rpg/TakashaRpgMod.java`
- Menghubungkan `ALLOW_DAMAGE` dengan `TakashaEnchantCombatEngine.applyArmorCombatEnchants` (sehingga negasi damage CreeperArmor, Dodge, dan ArrowDeflect berjalan pada level Fabric).
- Menghubungkan `AFTER_DEATH` dengan `TakashaEnchantCombatEngine.applyOnKillEnchants` (sehingga Decapitation, Devour, Rebound, dan Inquisitive aktif saat monster mati dibunuh pemain).

---

## 3. Verifikasi Kompilasi & Build

- **Perintah Kompilasi:**
  ```powershell
  .\gradlew.bat :takasha-rpg:compileJava
  ```
  *Hasil:* `BUILD SUCCESSFUL in 29s` (0 error).

- **Perintah Build Distribusi:**
  ```powershell
  .\gradlew.bat :takasha-rpg:build -x test
  ```
  *Hasil:* `BUILD SUCCESSFUL in 15s`.
  *Artefak Terpasang:* `Hasil mod/Takasha-RPG-1.5.0+26.2.jar` (674.492 bytes).

---

## 4. Panduan Uji Coba Dalam Game (In-Game Testing Guide)

### Uji 1: Immolation Radial AOE & Partikel Cincin Api
1. Buka game Minecraft 26.2 dengan mod `Takasha-RPG-1.5.0+26.2.jar`.
2. Pegang pedang di tangan utama, ketik perintah:
   ```mcfunction
   /ae enchant immolation 3
   ```
3. Spawn 3 boneka latihan atau monster yang saling berdekatan:
   ```mcfunction
   /dummy spawn
   ```
4. Pukul boneka latihan di tengah:
   - **Efek Visual:** Lingkaran partikel lava ganda melingkar horizontal sejauh 6 blok di tanah.
   - **Efek Tempur:** Seluruh boneka latihan dan monster dalam radius 6 blok terbakar (`burnTicks`), mengeluarkan partikel api, dan memunculkan angka DoT melayang `§6-1.0 🔥`.
   - **Efek Billboard:** Nilai DPS pada papan billboard overhead dummy naik mencatat damage DoT pembakaran.

### Uji 2: Cleave / Carve Radial Sweep
1. Pasang enchant Cleave pada pedang:
   ```mcfunction
   /ae enchant cleave 3
   ```
2. Serang salah satu dummy:
   - Pukulan memicu partikel tebasan pedang horizontal `SWEEP_ATTACK`.
   - Seluruh dummy dan monster di sekitar menerima sapuan damage dan status `fracture` (retak armor).

### Uji 3: Armor Defense (Dodge & CreeperArmor)
1. Pakai baju zirah dengan enchant CreeperArmor & Dodge:
   ```mcfunction
   /ae enchant creeperarmor 1
   /ae enchant dodge 3
   ```
2. Nyalakan TNT atau biarkan Creeper meledak tepat di samping pemain:
   - Damage ledakan dibatalkan 100% dan muncul partikel asap pelindung.

### Uji 4: On-Kill (Decapitation & Devour)
1. Pasang enchant Decapitation pada pedang:
   ```mcfunction
   /ae enchant decapitation 3
   /ae enchant devour 3
   ```
2. Habisi zombie atau monster di sekitar:
   - Peluang memenggal menjatuhkan item `zombie_head`.
   - Bar makanan dan saturasi pemain pulih seketika.
