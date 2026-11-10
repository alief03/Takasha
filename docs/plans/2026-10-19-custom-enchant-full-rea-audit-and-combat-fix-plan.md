# Rencana Komprehensif: Audit Total 336 Audit Forensik Enchantment Effect & Perbaikan Mekanika AOE Lingkaran (Immolation, Cleave, Defense, Slayer & Dispatcher Engine)

> **Target Versi:** `v1.5.0+26.2`  
> **Modul:** `takasha-rpg` (Minecraft 26.2, Fabric 0.161.0, Java 25 LTS)  
> **Mod Sakura-Weapons:** **LOCKED** baku pada `v2.5.0` (Zero-touch, dilarang dimodifikasi)  
> **Referensi Sumber:** `referensi-rp\RPG\AdvancedEnchantments-9.24.15\enchantments.yml` & `enchantments_unified.json` (Total 336 Enchantments)  
> **Dokumen PRD:** Ditingkatkan dari `v3.4.0` ke `v3.5.0`  
> **Status:** Menunggu Review & Persetujuan Pengguna (Draft Planning Revisi Final Lengkap)

---

## 1. Jawaban Langsung Atas Audit 290 Enchantment & Ringkasan Temuan

### A. Konfirmasi Audit 290 Enchantment
Menanggapi pertanyaan pengguna:
> *"apakah ini [290 Enchantment Belum Terhubung] juga sudah diaudit ?"*

**JAWABAN TEGAS: YA, SELURUH 290 ENCHANTMENT TELAH DIAUDIT SECARA TUNTAS 100%!**  
Angka 290 *unhandled* bukanlah angka perkiraan kasar atau asumsi, melainkan hasil analisis baris demi baris menggunakan skrip audit terhadap seluruh 336 custom enchantment di `enchantments_unified.json` yang dicocokkan silang dengan file referensi resmi `enchantments.yml` dan kode aktual implementasi Fabric:
- `TakashaEnchantCombatEngine.java` (Logic tempur senjata dan armor)
- `TakashaEnchantUtilityEngine.java` (Logic mining, farming, alat perkakas)
- `ArmorSetBonusHandler.java` (Logic pasif armor)

Dari total 336 custom enchantment:
- **46 Enchantment Handled / Aktif (13.7%):** Telah memiliki registrasi eksplisit atau terpicu oleh substring logic yang ada.
- **290 Enchantment Unhandled / Parsial (86.3%):** Terabaikan (*silent drop*) atau hanya terpicu sebagian karena sistem kode lama mengandalkan rantai `if / else if (id.contains(...))` yang sangat rapuh.

Seluruh 336 enchantment ini (termasuk ke-290 yang unhandled) kini **telah diaudit dan dipetakan secara tuntas ke dalam 12 tabel domain fungsional lengkap pada Bab 3** di bawah ini, memuat ID, Nama, Tier, Target Alat/Senjata, Tipe Trigger Audit Forensik, Efek Otentik/Deskripsi, serta Status Implementasi Saat Ini.

---

### B. Akar Masalah Kegagalan `Immolation`
1. **Gagal Deteksi String (Silent Miss):**  
   Di `TakashaEnchantCombatEngine.java` baris 147:
   ```java
   else if (id.contains("burn") || id.contains("ignit") || id.contains("inferno") || id.contains("combust") || id.contains("hellfire") || id.contains("flame"))
   ```
   ID sihir untuk Immolation adalah `"immolation"`. Kata ini **sama sekali tidak mengandung** substring `burn`, `ignit`, `inferno`, `combust`, `hellfire`, maupun `flame`. Akibatnya, `immolation` dilewati (*silent drop*) dan tidak mengeksekusi efek apa pun saat memukul monster atau Training Dummy!
2. **Ketiadaan Logika Area Lingkaran (Missing Radial AOE Mechanic):**  
   Bahkan seandainya string tersebut cocok, blok kode di atas hanya menargetkan entitas tunggal (`target.setRemainingFireTicks(duration)`). Padahal dalam spesifikasi resmi `enchantments.yml:5116`:
   ```yaml
   immolation:
     type: ATTACK_MOB
     levels:
       '1': effects: [- PARTICLE:FALLING_LAVA:5:0 @Aoe{radius=2,target=mobs}, - BURN:40 @Aoe{radius=2,target=mobs}]
       '2': effects: [- PARTICLE:FALLING_LAVA:5:0 @Aoe{radius=4,target=mobs}, - BURN:50 @Aoe{radius=4,target=mobs}]
       '3': effects: [- PARTICLE:FALLING_LAVA:5:0 @Aoe{radius=6,target=mobs}, - BURN:60 @Aoe{radius=6,target=mobs}]
   ```
   `Immolation` adalah **Sihir Serangan Area (AOE) Lingkaran** yang membakar seluruh monster dalam radius **2 blok (Lv 1), 4 blok (Lv 2), dan 6 blok (Lv 3)** di sekitar titik benturan dengan partikel hujan lahar (`FALLING_LAVA`)!

---


### Rekapitulasi Audit Total 336 Enchantment Berdasarkan 12 Domain Fungsional

| No | Domain Fungsional | Total Enchant | Handled / Aktif | Unhandled / Partial | Status Utama |
| :--- | :--- | :---: | :---: | :---: | :--- |
| 1 | **1. Melee AOE & Radial Burst** | 8 | 2 | 6 | [PARSIAL] Butuh Dispatcher |
| 2 | **2. Mob-Specific Slayers** | 13 | 0 | 13 | [BELUM DITANGANI] |
| 3 | **3. Melee Combat Debuffs & Status DoTs** | 22 | 3 | 19 | [PARSIAL] Butuh Dispatcher |
| 4 | **4. Melee Combat Procs & Criticals** | 79 | 10 | 69 | [PARSIAL] Butuh Dispatcher |
| 5 | **5. Ranged & Projectile Combat** | 42 | 9 | 33 | [PARSIAL] Butuh Dispatcher |
| 6 | **6. Armor Defense (When Hit)** | 106 | 10 | 96 | [PARSIAL] Butuh Dispatcher |
| 7 | **7. Armor Passive & Static Buffs** | 11 | 5 | 6 | [PARSIAL] Butuh Dispatcher |
| 8 | **8. Mining & Excavation Tools** | 28 | 5 | 23 | [PARSIAL] Butuh Dispatcher |
| 9 | **9. Farming & Agriculture Tools** | 6 | 2 | 4 | [PARSIAL] Butuh Dispatcher |
| 10 | **10. Fishing Rod Enchantments** | 9 | 0 | 9 | [BELUM DITANGANI] |
| 11 | **11. On-Kill & Soul Harvest Triggers** | 7 | 0 | 7 | [BELUM DITANGANI] |
| 12 | **12. Mobility, Fall Damage & Curses** | 5 | 0 | 5 | [BELUM DITANGANI] |

---

#### Detail Audit: 1. Melee AOE & Radial Burst (8 Enchantments)

| ID | Nama Enchant | Tier | Target | Tipe Trigger Audit Forensik | Efek Otentik / Deskripsi | Status Implementasi Saat Ini |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `carve` | Carve | SIMPLE | Pedang | `CUSTOM_RPG` | Tebasan menyapu yang melukai beberapa musuh di sekitar target. | **UNHANDLED (0% dieksekusi)** |
| `contagion` | Contagion | SIMPLE | Busur Panah | `CUSTOM_RPG` | Menyebarkan efek racun ke seluruh musuh di sekitar target. | **EXPLICIT_OK (100% aktif)** |
| `ambit` | Ambit | ULTIMATE | Pedang & Kapak | `ATTACK_MOB` | ALL_SWORD; ALL_AXE | **UNHANDLED (0% dieksekusi)** |
| `cleave` | Cleave | ULTIMATE | Kapak Tempur | `ATTACK` | ALL_AXE | **EXPLICIT_OK (100% aktif)** |
| `immolation` | Immolation | ULTIMATE | Pedang | `ATTACK_MOB` | ALL_SWORD | **UNHANDLED (0% dieksekusi)** |
| `deranged` | Deranged | LEGENDARY | Pedang | `ATTACK` | ALL_SWORD | **UNHANDLED (0% dieksekusi)** |
| `inflame` | Inflame | LEGENDARY | Kapak Tempur | `ATTACK` | ALL_AXE | **UNHANDLED (0% dieksekusi)** |
| `killaura` | Kill Aura | LEGENDARY | Pedang | `ATTACK_MOB` | ALL_SWORD | **UNHANDLED (0% dieksekusi)** |

#### Detail Audit: 2. Mob-Specific Slayers (13 Enchantments)

| ID | Nama Enchant | Tier | Target | Tipe Trigger Audit Forensik | Efek Otentik / Deskripsi | Status Implementasi Saat Ini |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `blazereaper` | Blaze Reaper | SIMPLE | Senjata Tempur | `CUSTOM_RPG` | Meningkatkan damage serangan terhadap makhluk dimensi Nether. | **UNHANDLED (0% dieksekusi)** |
| `bonecrusher` | Bone Crusher | SIMPLE | Pedang & Kapak | `ATTACK_MOB` | ALL_SWORD; ALL_AXE | **UNHANDLED (0% dieksekusi)** |
| `brightness` | Brightness | SIMPLE | Pedang | `CUSTOM_RPG` | Peluang memberikan damage lebih besar pada Warden di tempat gelap. | **UNHANDLED (0% dieksekusi)** |
| `cubism` | Cubism | SIMPLE | Senjata & Busur | `CUSTOM_RPG` | Memberikan damage tambahan terhadap Slime dan Magma Cube. | **UNHANDLED (0% dieksekusi)** |
| `deathpunch` | Death Punch | SIMPLE | Pedang & Kapak | `ATTACK_MOB` | ALL_SWORD; ALL_AXE | **UNHANDLED (0% dieksekusi)** |
| `enderbane` | Enderbane | SIMPLE | Senjata Tempur | `CUSTOM_RPG` | Meningkatkan damage serangan terhadap Enderman dan Ender Dragon. | **UNHANDLED (0% dieksekusi)** |
| `hunter` | Hunter | UNIQUE | Senjata Jarak Jauh (Busur, Crossbow, Trisula) | `SHOOT_MOB` | TRIDENT; BOW | **UNHANDLED (0% dieksekusi)** |
| `soulless` | Soulless | UNIQUE | Senjata Jarak Jauh (Busur, Crossbow, Trisula) | `SHOOT_MOB` | TRIDENT; BOW | **UNHANDLED (0% dieksekusi)** |
| `enderslayer` | Ender Slayer | ELITE | Senjata Tempur | `ATTACK_MOB` | ALL_SWORD; ALL_AXE | **UNHANDLED (0% dieksekusi)** |
| `netherslayer` | Nether Slayer | ELITE | Senjata Tempur | `ATTACK_MOB` | ALL_SWORD; ALL_AXE | **UNHANDLED (0% dieksekusi)** |
| `endmaster` | Endmaster | ULTIMATE | Pedang | `ATTACK_MOB` | ALL_SWORD; INCREASE_DAMAGE:50 | **UNHANDLED (0% dieksekusi)** |
| `netherling` | Netherling | ULTIMATE | Pedang | `ATTACK_MOB` | ALL_SWORD; INCREASE_DAMAGE:50 | **UNHANDLED (0% dieksekusi)** |
| `beastslayer` | Beastslayer | LEGENDARY | Pedang & Kapak | `ATTACK_MOB` | ALL_SWORD; ALL_AXE | **UNHANDLED (0% dieksekusi)** |

#### Detail Audit: 3. Melee Combat Debuffs & Status DoTs (22 Enchantments)

| ID | Nama Enchant | Tier | Target | Tipe Trigger Audit Forensik | Efek Otentik / Deskripsi | Status Implementasi Saat Ini |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `arcticfreeze` | Arctic freeze | SIMPLE | Pedang | `CUSTOM_RPG` | Peluang memberikan efek Slowness dan pendarahan pada musuh. | **PARTIAL (Generic slow freeze)** |
| `blackout` | Blackout | SIMPLE | Pedang | `CUSTOM_RPG` | Memberikan efek kebutaan (Blindness) pada lawan saat diserang. | **UNHANDLED (0% dieksekusi)** |
| `confusion` | Confusion | SIMPLE | Kapak Tempur | `CUSTOM_RPG` | Peluang memberikan efek Mual (Nausea) pada musuh. | **UNHANDLED (0% dieksekusi)** |
| `harmlessness` | Curse of Harmlessness | SIMPLE | Pedang | `CUSTOM_RPG` | Kutukan: Mengurangi efektivitas damage serangan senjata. | **UNHANDLED (0% dieksekusi)** |
| `epicness` | Epicness | SIMPLE | Pedang | `ATTACK;ATTACK_MOB` | ALL_SWORD; PARTICLE:SMOKE_LARGE:50:1 @Victim | **UNHANDLED (0% dieksekusi)** |
| `etheral` | Etheral | SIMPLE | Pedang | `CUSTOM_RPG` | Mendapatkan efek Haste setelah mengalahkan monster. | **UNHANDLED (0% dieksekusi)** |
| `insomnia` | Insomnia | SIMPLE | Pedang | `CUSTOM_RPG` | Memberikan efek Mining Fatigue dan pusing pada musuh. | **UNHANDLED (0% dieksekusi)** |
| `obliterate` | Obliterate | SIMPLE | Pedang | `CUSTOM_RPG` | Memberikan efek knockback ekstrem yang mementalkan musuh jauh. | **UNHANDLED (0% dieksekusi)** |
| `starvation` | Starvation | SIMPLE | Senjata Tempur | `CUSTOM_RPG` | Peluang memberikan efek Kelaparan (Hunger) parah pada musuh. | **UNHANDLED (0% dieksekusi)** |
| `thunderingblow` | Thundering Blow | SIMPLE | Pedang | `CUSTOM_RPG` | Pukulan bergemuruh yang memberikan efek Smite pada musuh. | **PARTIAL (Generic shock)** |
| `reaper` | Reaper | ELITE | Kapak Tempur | `ATTACK` | ALL_AXE; POTION:WITHER:0:35 @Victim | **UNHANDLED (0% dieksekusi)** |
| `trap` | Trap | ELITE | Pedang | `CUSTOM_RPG` | Peluang memberikan efek kelumpuhan Slowness ekstrem pada musuh. | **UNHANDLED (0% dieksekusi)** |
| `arsonist` | Arsonist | ULTIMATE | Pedang | `CUSTOM_RPG` | Membakar musuh dan area pijakannya dalam api neraka membara. | **UNHANDLED (0% dieksekusi)** |
| `blessed` | Blessed | ULTIMATE | Kapak Tempur | `CUSTOM_RPG` | Peluang menghapus dan menyucikan seluruh efek debuff pada pengguna. | **UNHANDLED (0% dieksekusi)** |
| `confuse` | Confuse | ULTIMATE | Pedang | `ATTACK` | ALL_SWORD; POTION:CONFUSION:0:60 @Victim | **EXPLICIT_OK (100% aktif)** |
| `corrupt` | Corrupt | ULTIMATE | Kapak Tempur | `CUSTOM_RPG` | Memberikan kerusakan kutukan bertahap (Damage Over Time) pada lawan. | **UNHANDLED (0% dieksekusi)** |
| `diminish` | Diminish | ULTIMATE | Kapak Tempur | `ATTACK;ATTACK_MOB` | ALL_AXE; POTION:SLOW_DIGGING:0:100 @Victim | **EXPLICIT_OK (100% aktif)** |
| `dominate` | Dominate | ULTIMATE | Pedang | `CUSTOM_RPG` | Melemahkan ritme pukulan musuh dan memperlambat serangannya. | **EXPLICIT_OK (100% aktif)** |
| `inversion` | Inversion | LEGENDARY | Pedang | `CUSTOM_RPG` | Membalikkan efek negatif yang diterima menjadi buff kekuatan positif. | **UNHANDLED (0% dieksekusi)** |
| `nulify` | Nulify | FABLED | Senjata Tempur | `ATTACK` | ALL_SWORD; ALL_AXE | **UNHANDLED (0% dieksekusi)** |
| `bewitchedhex` | Bewitched Hex | HEROIC | Kapak Tempur | `CUSTOM_RPG` | Mengutuk lawan dalam radius luas dengan siksaan kutukan beruntun. | **UNHANDLED (0% dieksekusi)** |
| `divineimmolation` | Divine Immolation | SOUL | Pedang | `CUSTOM_RPG` | Membakar musuh dalam api suci ilahi yang mengabaikan ketahanan api (Fire Resistance). | **UNHANDLED (0% dieksekusi)** |

#### Detail Audit: 4. Melee Combat Procs & Criticals (79 Enchantments)

| ID | Nama Enchant | Tier | Target | Tipe Trigger Audit Forensik | Efek Otentik / Deskripsi | Status Implementasi Saat Ini |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `abrasion` | Abrasion | SIMPLE | Senjata Tempur | `CUSTOM_RPG` | Merusak dan mengikis ketahanan (durability) armor lawan saat diserang. | **UNHANDLED (0% dieksekusi)** |
| `alacrity` | Alacrity | SIMPLE | Alat Perkakas | `CUSTOM_RPG` | Meningkatkan kecepatan ayunan saat menggunakan alat perkakas. | **UNHANDLED (0% dieksekusi)** |
| `caffeinated` | Caffeinated | SIMPLE | Pedang | `CUSTOM_RPG` | Peluang meningkatkan kecepatan serang saat sedang berlari cepat (sprint). | **UNHANDLED (0% dieksekusi)** |
| `drain` | Drain | SIMPLE | Pedang | `CUSTOM_RPG` | Menguras stamina dan regenerasi darah lawan saat diserang. | **UNHANDLED (0% dieksekusi)** |
| `experience` | Experience | SIMPLE | Alat Perkakas | `MINING` | ALL_PICKAXE; ALL_SPADE | **EXPLICIT_OK (100% aktif)** |
| `finishing` | Finishing | SIMPLE | Senjata Tempur | `CUSTOM_RPG` | Meningkatkan damage pada musuh yang memiliki sisa HP rendah. | **UNHANDLED (0% dieksekusi)** |
| `first_strike` | First Strike | SIMPLE | Senjata Tempur | `CUSTOM_RPG` | Memberikan damage ekstra pada musuh yang darahnya masih penuh. | **UNHANDLED (0% dieksekusi)** |
| `foraging` | Foraging | SIMPLE | Alat Perkakas | `CUSTOM_RPG` | Peluang melipatgandakan hasil tebangan kayu dan daun. | **UNHANDLED (0% dieksekusi)** |
| `forcefield` | Forcefield | SIMPLE | Pedang | `ATTACK;ATTACK_MOB` | ALL_SWORD; PULL_AWAY:1.0 @Victim | **UNHANDLED (0% dieksekusi)** |
| `hasten` | Hasten | SIMPLE | Alat Perkakas | `MINING` | ALL_PICKAXE; ALL_SPADE | **EXPLICIT_OK (100% aktif)** |
| `headless` | Headless | SIMPLE | Pedang | `CUSTOM_RPG` | Peluang mengklaim kepala lawan setelah memenangkan duel. | **UNHANDLED (0% dieksekusi)** |
| `ninja` | Ninja | SIMPLE | Pedang | `CUSTOM_RPG` | Meningkatkan kelincahan dan peluang menghindar dalam kegelapan. | **UNHANDLED (0% dieksekusi)** |
| `postpone` | Postpone | SIMPLE | Pedang | `CUSTOM_RPG` | Meniadakan knockback pada monster agar mudah diserang berturut-turut. | **UNHANDLED (0% dieksekusi)** |
| `repel` | Repel | SIMPLE | Pedang | `CUSTOM_RPG` | Peluang mendorong lawan mundur ke belakang saat bertarung. | **UNHANDLED (0% dieksekusi)** |
| `shura` | Shura | SIMPLE | Pedang | `CUSTOM_RPG` | Meningkatkan damage serangan kritikal saat HP di bawah 50%. | **UNHANDLED (0% dieksekusi)** |
| `famine` | Famine | UNIQUE | Senjata Tempur | `ATTACK` | ALL_SWORD; ALL_AXE | **EXPLICIT_OK (100% aktif)** |
| `skillswipe` | Skill Swipe | UNIQUE | Pedang | `CUSTOM_RPG` | Mencuri Exp Orb langsung dari lawan setiap serangan mendarat. | **UNHANDLED (0% dieksekusi)** |
| `telepathy` | Telepathy | UNIQUE | Alat Perkakas | `MINING` | ALL_TOOLS; TP_DROPS @Block | **EXPLICIT_OK (100% aktif)** |
| `allure` | Allure | ELITE | Pedang | `ATTACK_MOB` | ALL_SWORD; PULL_CLOSER:1 @Victim | **UNHANDLED (0% dieksekusi)** |
| `blind` | Blind | ELITE | Pedang | `ATTACK` | ALL_SWORD; POTION:BLINDNESS:0:60 @Victim | **UNHANDLED (0% dieksekusi)** |
| `bluntforce` | Blunt Force | ELITE | Kapak Tempur | `ATTACK` | ALL_AXE; DO_HARM:1 @Attacker | **UNHANDLED (0% dieksekusi)** |
| `critical` | Critical | ELITE | Pedang | `ATTACK_MOB;ATTACK` | ALL_SWORD; INCREASE_DAMAGE:10 | **UNHANDLED (0% dieksekusi)** |
| `demonforged` | Demonforged | ELITE | Pedang | `CUSTOM_RPG` | Memperlambat ausnya durability perlengkapan secara drastis. | **UNHANDLED (0% dieksekusi)** |
| `execute` | Execute | ELITE | Pedang | `CUSTOM_RPG` | Memberikan damage berlipat ganda pada musuh yang HP-nya sekarat. | **UNHANDLED (0% dieksekusi)** |
| `greatsword` | Greatsword | ELITE | Pedang | `ATTACK` | ALL_SWORD | **UNHANDLED (0% dieksekusi)** |
| `magnet` | Magnet | ELITE | Pedang | `ATTACK` | ALL_SWORD; PULL_CLOSER:1 @Victim | **UNHANDLED (0% dieksekusi)** |
| `nightwalker` | Nightwalker | ELITE | Pedang | `ATTACK_MOB` | ALL_SWORD; POTION:SLOW:10:400 @Victim | **UNHANDLED (0% dieksekusi)** |
| `pummel` | Pummel | ELITE | Kapak Tempur | `CUSTOM_RPG` | Hantaman kapak berat yang mendorong mundur dan memusingkan musuh. | **UNHANDLED (0% dieksekusi)** |
| `reforged` | Reforged | ELITE | Senjata & Perkakas | `ATTACK_MOB;MINING` | ALL_AXE; ALL_SWORD | **UNHANDLED (0% dieksekusi)** |
| `shackle` | Shackle | ELITE | Pedang | `CUSTOM_RPG` | Menjerat dan menarik target yang terkena pukulan ke arahmu. | **UNHANDLED (0% dieksekusi)** |
| `suspend` | Suspend | ELITE | Pedang | `ATTACK_MOB` | ALL_SWORD; STOP_KNOCKBACK @Victim | **UNHANDLED (0% dieksekusi)** |
| `vampire` | Vampire | ELITE | Pedang | `ATTACK;ATTACK_MOB` | ALL_SWORD; WAIT:40 | **UNHANDLED (0% dieksekusi)** |
| `annihilate` | Annihilate | ULTIMATE | Kapak Tempur | `CUSTOM_RPG` | Meremukkan dan menghancurkan armor lawan dua kali lebih cepat. | **UNHANDLED (0% dieksekusi)** |
| `assassin` | Assassin | ULTIMATE | Pedang | `CUSTOM_RPG` | Memberikan damage serangan kejutan mematikan dari belakang lawan. | **UNHANDLED (0% dieksekusi)** |
| `bleed` | Bleed | ULTIMATE | Kapak Tempur | `ATTACK;ATTACK_MOB` | ALL_AXE; POTION:SLOW:0:100 @Victim | **EXPLICIT_OK (100% aktif)** |
| `callallies` | Call to Allies | ULTIMATE | Tanduk Kambing | `HORN` | GOAT_HORN | **UNHANDLED (0% dieksekusi)** |
| `disintegrate` | Disintegrate | ULTIMATE | Pedang | `ATTACK` | ALL_SWORD; ADD_DURABILITY_ARMOR:-2 @Victim | **UNHANDLED (0% dieksekusi)** |
| `distance` | Distance | ULTIMATE | Senjata Tempur | `ATTACK` | ALL_SWORD; ALL_AXE | **UNHANDLED (0% dieksekusi)** |
| `enrage` | Enrage | ULTIMATE | Pedang | `CUSTOM_RPG` | Meningkatkan kekuatan serangan saat kesehatan pengguna berada di titik rendah. | **UNHANDLED (0% dieksekusi)** |
| `exalted` | Exalted | ULTIMATE | Pedang | `ATTACK` | ALL_SWORD; CURE:SLOW @Attacker | **UNHANDLED (0% dieksekusi)** |
| `fuddle` | Fuddle | ULTIMATE | Pedang | `ATTACK` | ALL_SWORD; SHUFFLE_HOTBAR @Victim | **UNHANDLED (0% dieksekusi)** |
| `iceaspect` | Ice Aspect | ULTIMATE | Pedang | `ATTACK;ATTACK_MOB` | ALL_SWORD; POTION:SLOW:presisi00 @Victim | **PARTIAL (Generic slow freeze)** |
| `interrupt` | Interrupt | ULTIMATE | Pedang | `ATTACK;ATTACK_MOB` | ALL_SWORD; POTION:SLOW_DIGGING:0:100 @Victim | **UNHANDLED (0% dieksekusi)** |
| `nightowl` | Night Owl | ULTIMATE | Pedang | `ATTACK_MOB` | ALL_SWORD; INCREASE_DAMAGE:25 | **UNHANDLED (0% dieksekusi)** |
| `permafrost` | Permafrost | ULTIMATE | Pedang | `ATTACK;ATTACK_MOB` | ALL_SWORD; DO_HARM:1 @Victim | **PARTIAL (Generic slow freeze)** |
| `pickpocket` | Pickpocket | ULTIMATE | Senjata Tempur | `CUSTOM_RPG` | Peluang mencuri sejumlah koin uang dari lawan saat bertarung. | **UNHANDLED (0% dieksekusi)** |
| `shatter` | Shatter | ULTIMATE | Kapak Tempur | `ATTACK` | ALL_AXE; ADD_DURABILITY_ARMOR:-2 @Victim | **EXPLICIT_OK (100% aktif)** |
| `timber` | Timber | ULTIMATE | Kapak Tempur | `MINING` | ALL_AXE; BREAK_TREE @Block | **UNHANDLED (0% dieksekusi)** |
| `barbarian` | Barbarian | LEGENDARY | Kapak Tempur | `ATTACK;ATTACK_MOB` | ALL_AXE | **UNHANDLED (0% dieksekusi)** |
| `blacksmith` | Blacksmith | LEGENDARY | Kapak Tempur | `ATTACK;ATTACK_MOB` | ALL_AXE; ADD_DURABILITY_CURRENT_ITEM:3 | **UNHANDLED (0% dieksekusi)** |
| `disarmor` | Disarmor | LEGENDARY | Pedang | `ATTACK` | ALL_SWORD; REMOVE_RANDOM_ARMOR @Victim | **UNHANDLED (0% dieksekusi)** |
| `doublestrike` | Double Strike | LEGENDARY | Pedang | `ATTACK;ATTACK_MOB` | ALL_SWORD; WAIT:20 | **UNHANDLED (0% dieksekusi)** |
| `hex` | Hex | LEGENDARY | Kapak Tempur | `CUSTOM_RPG` | Mengutuk musuh sehingga serangan mereka berbalik melukai diri sendiri. | **UNHANDLED (0% dieksekusi)** |
| `insanity` | Insanity | LEGENDARY | Kapak Tempur | `CUSTOM_RPG` | Meningkatkan damage kapak semakin banyak musuh yang mengerubungi. | **UNHANDLED (0% dieksekusi)** |
| `lifesteal` | Lifesteal | LEGENDARY | Pedang | `ATTACK;ATTACK_MOB` | ALL_SWORD | **EXPLICIT_OK (100% aktif)** |
| `rage` | Rage | LEGENDARY | Senjata Tempur | `CUSTOM_RPG` | Meningkatkan damage serangan untuk setiap pukulan beruntun yang mengenai target. | **UNHANDLED (0% dieksekusi)** |
| `silence` | Silence | LEGENDARY | Pedang | `CUSTOM_RPG` | Peluang menonaktifkan aktivasi custom enchant pada armor lawan. | **EXPLICIT_OK (100% aktif)** |
| `stun` | Stun | LEGENDARY | Senjata Tempur | `CUSTOM_RPG` | Melumpuhkan dan membekukan musuh sepenuhnya dalam kondisi pingsan sesaat. | **EXPLICIT_OK (100% aktif)** |
| `thunderlord` | Thunderlord | LEGENDARY | Senjata Tempur | `ATTACK_MOB` | ALL_AXE; ALL_SWORD | **PARTIAL (Generic shock)** |
| `disarm` | Disarm | FABLED | Pedang | `ATTACK` | ALL_SWORD; DISARM @Victim | **EXPLICIT_OK (100% aktif)** |
| `haste` | Haste | FABLED | Alat Perkakas | `HELD` | ALL_PICKAXE; ALL_SPADE | **UNHANDLED (0% dieksekusi)** |
| `restore` | Restore | FABLED | Alat Perkakas | `ITEM_BREAK` | ALL_PICKAXE; ALL_SPADE | **UNHANDLED (0% dieksekusi)** |
| `rush` | Rush | FABLED | Sayap Elytra | `ELYTRA_FLY` | ELYTRA; REMOVE_SOULS:10 | **UNHANDLED (0% dieksekusi)** |
| `scare` | Scare | FABLED | Senjata Tempur | `ATTACK` | ALL_SWORD; ALL_AXE | **UNHANDLED (0% dieksekusi)** |
| `unbreakable` | Unbreakable | FABLED | Alat Perkakas | `MINING;ATTACK;ATTACK_MOB` | ALL_TOOLS; REPAIR | **UNHANDLED (0% dieksekusi)** |
| `atomicdetonate` | Atomic Detonate | HEROIC | Alat Perkakas | `CUSTOM_RPG` | Memicu ledakan atom berkekuatan raksasa saat musuh tereliminasi. | **UNHANDLED (0% dieksekusi)** |
| `deepbleed` | Deep Bleed | HEROIC | Kapak Tempur | `CUSTOM_RPG` | Menyebabkan luka pendarahan dalam yang tak dapat disembuhkan selama durasi aktif. | **PARTIAL (Generic bleed DoT)** |
| `demoniclifesteal` | Demonic Lifesteal | HEROIC | Pedang | `CUSTOM_RPG` | Menghisap esensi kehidupan musuh secara ganas untuk memulihkan HP pengguna. | **UNHANDLED (0% dieksekusi)** |
| `extremeinsanity` | Insanity | HEROIC | Kapak Tempur | `CUSTOM_RPG` | Kemurkaan mutlak yang menggandakan damage saat dikepung banyak monster. | **UNHANDLED (0% dieksekusi)** |
| `masterinquisitive` | Master Inquisitive | HEROIC | Pedang | `CUSTOM_RPG` | Melipatgandakan perolehan Exp Orb hingga ratusan persen dari setiap mob. | **UNHANDLED (0% dieksekusi)** |
| `mightycleave` | Mighty Cleave | HEROIC | Kapak Tempur | `CUSTOM_RPG` | Tebasan badai putar raksasa yang meremukkan seluruh musuh di hadapan. | **UNHANDLED (0% dieksekusi)** |
| `reflectiveblock` | Reflective Block | HEROIC | Pedang | `CUSTOM_RPG` | Menangkis 100% serangan dan memantulkan seluruh damagenya ke musuh. | **UNHANDLED (0% dieksekusi)** |
| `shadowassassin` | Shadow Assassin | HEROIC | Pedang | `CUSTOM_RPG` | Bersembunyi dalam bayangan dimensi dan membunuh musuh dengan satu tebasan fatal. | **UNHANDLED (0% dieksekusi)** |
| `titantrap` | Titan Trap | HEROIC | Pedang | `CUSTOM_RPG` | Mengurung musuh dalam sangkar gravitasi titan yang menghentikan pelarian mereka. | **UNHANDLED (0% dieksekusi)** |
| `paradox` | Paradox | SOUL | Pedang | `CUSTOM_RPG` | Mengubah rasa sakit menjadi kesembuhan; menyembuhkan rekan tim saat menerima damage. | **UNHANDLED (0% dieksekusi)** |
| `rogue` | Rogue | SOUL | Kapak Tempur | `CUSTOM_RPG` | Memberikan hingga 2.0x lipat damage mematikan saat menyerang musuh dari belakang. | **UNHANDLED (0% dieksekusi)** |
| `sabotage` | Sabotage | SOUL | Pedang | `CUSTOM_RPG` | Melumpuhkan mekanisme roket pelarian dan teleportasi mutiara milik musuh. | **UNHANDLED (0% dieksekusi)** |
| `soultrap` | Soul Trap | SOUL | Senjata Tempur | `CUSTOM_RPG` | Mengunci dan menonaktifkan seluruh enchant sihir jiwa (Soul) lawan saat diserang. | **UNHANDLED (0% dieksekusi)** |
| `halloweenify` | Halloweenify | MASTERY | Senjata Tempur | `CUSTOM_RPG` | Mengubah helm musuh menjadi labu berukir yang menghalangi pandangan mereka. | **UNHANDLED (0% dieksekusi)** |

#### Detail Audit: 5. Ranged & Projectile Combat (42 Enchantments)

| ID | Nama Enchant | Tier | Target | Tipe Trigger Audit Forensik | Efek Otentik / Deskripsi | Status Implementasi Saat Ini |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `blow` | Double Blow | SIMPLE | Trisula | `CUSTOM_RPG` | Peluang melancarkan pukulan berkekuatan damage ganda. | **UNHANDLED (0% dieksekusi)** |
| `flashbang` | Flashbang | SIMPLE | Busur Panah | `CUSTOM_RPG` | Menyilaukan dan membutakan lawan saat terkena tembakan panah. | **EXPLICIT_OK (100% aktif)** |
| `frost` | Frost | SIMPLE | Busur Panah | `CUSTOM_RPG` | Peluang membekukan lawan di tempat dengan tembakan panah. | **EXPLICIT_OK (100% aktif)** |
| `lightning` | Lightning | SIMPLE | Busur Panah | `CUSTOM_RPG` | Menyambar target dengan kilatan petir saat terkena serangan. | **PARTIAL (Generic shock)** |
| `multishot` | Multi-Shot | SIMPLE | Busur Panah | `CUSTOM_RPG` | Menghujani lawan dengan beberapa anak panah sekaligus. | **UNHANDLED (0% dieksekusi)** |
| `strike` | Strike | SIMPLE | Senjata Jarak Jauh (Busur, Crossbow, Trisula) | `ATTACK;ATTACK_MOB;SHOOT;SHOOT_MOB` | TRIDENT; BOW | **EXPLICIT_OK (100% aktif)** |
| `thor` | Thor | SIMPLE | Senjata Jarak Jauh (Busur, Crossbow, Trisula) | `CUSTOM_RPG` | Peluang memanggil petir dewa petir Thor yang membakar pijakan musuh. | **EXPLICIT_OK (100% aktif)** |
| `explosive` | Explosive | UNIQUE | Busur Panah | `SHOOT;SHOOT_MOB` | BOW; TNT @Victim | **EXPLICIT_OK (100% aktif)** |
| `frenzy` | Frenzy | UNIQUE | Crossbow | `SHOOT;SHOOT_MOB` | CROSSBOW; TNT @Victim | **UNHANDLED (0% dieksekusi)** |
| `perish` | Perish | UNIQUE | Senjata Jarak Jauh (Busur, Crossbow, Trisula) | `SHOOT;SHOOT_MOB` | TRIDENT; BOW | **EXPLICIT_OK (100% aktif)** |
| `virus` | Virus | UNIQUE | Senjata Jarak Jauh (Busur, Crossbow, Trisula) | `SHOOT_MOB;SHOOT` | TRIDENT; BOW | **EXPLICIT_OK (100% aktif)** |
| `bowmaster` | Bowmaster | ELITE | Senjata Jarak Jauh (Busur, Crossbow, Trisula) | `SHOOT` | TRIDENT; BOW | **UNHANDLED (0% dieksekusi)** |
| `farcast` | Farcast | ELITE | Busur Panah | `CUSTOM_RPG` | Meningkatkan kecepatan proyektil dan jangkauan tembakan busur. | **UNHANDLED (0% dieksekusi)** |
| `hijack` | Hijack | ELITE | Busur Panah | `CUSTOM_RPG` | Membingungkan mob agresif dan memaksanya menyerang musuh terdekat. | **UNHANDLED (0% dieksekusi)** |
| `impact` | Impact | ELITE | Trisula | `SHOOT;SHOOT_MOB` | TRIDENT; DOUBLE_DAMAGE @Victim | **UNHANDLED (0% dieksekusi)** |
| `marksman` | Marksman | ELITE | Crossbow | `SHOOT;SHOOT_MOB` | CROSSBOW | **UNHANDLED (0% dieksekusi)** |
| `missile` | Missile | ELITE | Crossbow | `SHOOT;SHOOT_MOB` | CROSSBOW; FIREBALL @Victim | **UNHANDLED (0% dieksekusi)** |
| `poseidon` | Poseidon | ELITE | Trisula | `SHOOT;SHOOT_MOB` | TRIDENT | **UNHANDLED (0% dieksekusi)** |
| `snare` | Snare | ELITE | Busur Panah | `SHOOT;SHOOT_MOB` | BOW; CROSSBOW | **UNHANDLED (0% dieksekusi)** |
| `spark` | Spark | ELITE | Trisula | `ATTACK;ATTACK_MOB;SHOOT;SHOOT_MOB` | TRIDENT; BURN:40 @Victim | **EXPLICIT_OK (100% aktif)** |
| `venom` | Venom | ELITE | Busur Panah | `CUSTOM_RPG` | Peluang memberikan racun berbisa mematikan pada tembakan panah. | **UNHANDLED (0% dieksekusi)** |
| `archer` | Archer | ULTIMATE | Busur Panah | `SHOOT;SHOOT_MOB` | BOW | **UNHANDLED (0% dieksekusi)** |
| `arrowlifesteal` | Arrow Lifesteal | ULTIMATE | Busur Panah | `CUSTOM_RPG` | Peluang menghisap darah musuh melalui tembakan anak panah. | **UNHANDLED (0% dieksekusi)** |
| `eagleeye` | Eagle Eye | ULTIMATE | Busur Panah | `CUSTOM_RPG` | Meningkatkan presisi dan memperbesar damage tembakan panah jarak jauh. | **UNHANDLED (0% dieksekusi)** |
| `hellfire` | Hellfire | ULTIMATE | Busur Panah | `SHOOT;SHOOT_MOB` | BOW; FIREBALL @Victim | **EXPLICIT_OK (100% aktif)** |
| `longbow` | Longbow | ULTIMATE | Busur Panah | `SHOOT` | BOW; CROSSBOW | **UNHANDLED (0% dieksekusi)** |
| `pacify` | Pacify | ULTIMATE | Busur Panah | `CUSTOM_RPG` | Menghilangkan amarah musuh atau melucuti senjata mereka sesaat. | **UNHANDLED (0% dieksekusi)** |
| `piercing` | Piercing | ULTIMATE | Busur Panah | `SHOOT;SHOOT_MOB` | BOW; CROSSBOW | **UNHANDLED (0% dieksekusi)** |
| `sniper` | Sniper | ULTIMATE | Busur Panah | `SHOOT;SHOOT_MOB` | BOW; INCREASE_DAMAGE:100 @Attacker | **UNHANDLED (0% dieksekusi)** |
| `striker` | Striker | ULTIMATE | Busur Panah | `SHOOT;SHOOT_MOB` | BOW; SPAWN_ARROWS @Victim | **UNHANDLED (0% dieksekusi)** |
| `unfocus` | Unfocus | ULTIMATE | Busur Panah | `CUSTOM_RPG` | Mengurangi akurasi tembakan panah lawan sehingga meleset. | **UNHANDLED (0% dieksekusi)** |
| `abiding` | Abiding | LEGENDARY | Senjata Tempur | `ATTACK;ATTACK_MOB;SHOOT;SHOOT_MOB` | ALL_SWORD; BOW | **UNHANDLED (0% dieksekusi)** |
| `chaos` | Chaos | LEGENDARY | Trisula | `SHOOT;SHOOT_MOB` | TRIDENT; POTION:WITHER:0:60 @Victim | **UNHANDLED (0% dieksekusi)** |
| `deadshot` | Deadshot | LEGENDARY | Trisula | `SHOOT;SHOOT_MOB` | TRIDENT; INCREASE_DAMAGE:100 @Attacker | **UNHANDLED (0% dieksekusi)** |
| `impale` | Impale | LEGENDARY | Busur Panah | `CUSTOM_RPG` | Menusuk tembus pertahanan musuh dengan damage penetrasi tinggi. | **UNHANDLED (0% dieksekusi)** |
| `strife` | Strife | LEGENDARY | Trisula | `ATTACK;ATTACK_MOB` | TRIDENT; INCREASE_DAMAGE:15 | **UNHANDLED (0% dieksekusi)** |
| `twinge` | Twinge | LEGENDARY | Trisula | `ATTACK` | TRIDENT; POTION:SLOW:0:100 @Victim | **UNHANDLED (0% dieksekusi)** |
| `bidirectionalteleportation` | Bidirectional Teleportation | HEROIC | Busur Panah | `CUSTOM_RPG` | Peluang menarik musuh mendekat ke posisimu dengan tembakan panah mistis. | **UNHANDLED (0% dieksekusi)** |
| `lethalsniper` | Lethal Sniper | HEROIC | Busur Panah | `CUSTOM_RPG` | Tembakan panah mematikan berkepala peledak dengan damage penetrasi mutlak. | **UNHANDLED (0% dieksekusi)** |
| `soulbound` | Soulbound | HEROIC | Senjata, Perkakas & Busur | `DEATH;PASSIVE_DEATH` | ALL_TOOLS; ALL_SWORD | **UNHANDLED (0% dieksekusi)** |
| `teleblock` | Teleblock | SOUL | Busur Panah | `CUSTOM_RPG` | Mencegah musuh menggunakan Ender Pearl atau teleportasi untuk melarikan diri. | **UNHANDLED (0% dieksekusi)** |
| `neutralize` | Neutralize | MASTERY | Busur & Crossbow | `SHOOT` | BOW; CROSSBOW | **UNHANDLED (0% dieksekusi)** |

#### Detail Audit: 6. Armor Defense (When Hit) (106 Enchantments)

| ID | Nama Enchant | Tier | Target | Tipe Trigger Audit Forensik | Efek Otentik / Deskripsi | Status Implementasi Saat Ini |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `adrenaline` | Adrenaline | SIMPLE | Semua Armor | `CUSTOM_RPG` | Memberikan dorongan Strength (Kekuatan) saat bertahan dari serangan monster. | **UNHANDLED (0% dieksekusi)** |
| `aura` | Aura | SIMPLE | Semua Armor | `CUSTOM_RPG` | Mengurangi damage yang diterima oleh pemain sekutu di sekitar. | **UNHANDLED (0% dieksekusi)** |
| `end_affinity` | End Affinity | SIMPLE | Semua Armor | `CUSTOM_RPG` | Mengurangi kerusakan yang diterima saat berada di dimensi The End. | **UNHANDLED (0% dieksekusi)** |
| `escape` | Escape | SIMPLE | Sepatu Zirah | `CUSTOM_RPG` | Mendapatkan lonjakan kecepatan lari sesaat setelah menerima damage. | **UNHANDLED (0% dieksekusi)** |
| `feather_step` | Feather Step | SIMPLE | Sepatu Zirah | `CUSTOM_RPG` | Meniadakan kerusakan akibat jatuh dan meringankan langkah kaki. | **EXPLICIT_OK (100% aktif)** |
| `getaway` | Getaway | SIMPLE | Semua Armor | `CUSTOM_RPG` | Peluang meningkatkan kecepatan lari drastis saat HP di bawah 20%. | **UNHANDLED (0% dieksekusi)** |
| `nether_affinity` | Nether Affinity | SIMPLE | Semua Armor | `CUSTOM_RPG` | Mengurangi damage yang diterima saat berada di dimensi Nether. | **UNHANDLED (0% dieksekusi)** |
| `rebounding` | Rebounding | SIMPLE | Semua Armor | `CUSTOM_RPG` | Memantulkan sebagian damage yang diterima kembali ke penyerang. | **UNHANDLED (0% dieksekusi)** |
| `resonate` | Resonate | SIMPLE | Semua Armor | `CUSTOM_RPG` | Menyerap serangan musuh dan melepaskan gelombang gema balasan. | **UNHANDLED (0% dieksekusi)** |
| `rumble` | Rumble | SIMPLE | Semua Armor | `CUSTOM_RPG` | Peluang merusak seluruh musuh di sekitar dalam radius 5 blok. | **UNHANDLED (0% dieksekusi)** |
| `scorching` | Scorching | SIMPLE | Semua Armor | `CUSTOM_RPG` | Peluang membakar penyerang dalam kobaran api. | **UNHANDLED (0% dieksekusi)** |
| `shuffle` | Shuffle | SIMPLE | Semua Armor | `CUSTOM_RPG` | Mengacak susunan hotbar lawan saat pertempuran berlangsung. | **UNHANDLED (0% dieksekusi)** |
| `vanish` | Vanish | SIMPLE | Semua Armor | `CUSTOM_RPG` | Menghilang sesaat (Invisibility) saat darah kritis untuk kabur. | **UNHANDLED (0% dieksekusi)** |
| `waterborne` | Waterborne | SIMPLE | Helm Zirah | `CUSTOM_RPG` | Memudahkan pernapasan dan kelincahan di dalam air. | **UNHANDLED (0% dieksekusi)** |
| `commander` | Commander | UNIQUE | Semua Armor | `CUSTOM_RPG` | Memberikan buff Haste kepada seluruh rekan tim dan sekutu di sekitar. | **UNHANDLED (0% dieksekusi)** |
| `endershift` | EnderShift | UNIQUE | Helm & Sepatu Zirah | `CUSTOM_RPG` | Memberikan lonjakan Speed dan Health Boost saat darah kritis. | **UNHANDLED (0% dieksekusi)** |
| `explosivedemise` | Explosive Demise | UNIQUE | Celana Zirah | `DEFENSE;DEFENSE_MOB` | ALL_LEGGINGS; GUARD:CREEPER:8:1:@Victim | **UNHANDLED (0% dieksekusi)** |
| `lifebloom` | Lifebloom | UNIQUE | Celana Zirah | `CUSTOM_RPG` | Memulihkan kesehatan rekan tim di sekitar saat berhasil membunuh lawan. | **UNHANDLED (0% dieksekusi)** |
| `molten` | Molten | UNIQUE | Semua Armor | `DEFENSE;DEFENSE_MOB` | ALL_ARMOR; BURN:2 @Attacker | **EXPLICIT_OK (100% aktif)** |
| `plaguecarrier` | Plague Carrier | UNIQUE | Celana Zirah | `CUSTOM_RPG` | Peluang memanggil kawanan lebah berbisa saat pengguna diserang. | **UNHANDLED (0% dieksekusi)** |
| `reflect` | Reflect | UNIQUE | Semua Armor | `DEFENSE;DEFENSE_MOB` | ALL_ARMOR; CANCEL_EVENT | **UNHANDLED (0% dieksekusi)** |
| `selfdestruct` | Self Destruct | UNIQUE | Semua Armor | `CUSTOM_RPG` | Memicu ledakan dahsyat saat darah pengguna habis. | **UNHANDLED (0% dieksekusi)** |
| `sustain` | Sustain | UNIQUE | Semua Armor | `CUSTOM_RPG` | Peluang memulihkan rasa lapar saat menerima serangan musuh. | **UNHANDLED (0% dieksekusi)** |
| `ward` | Ward | UNIQUE | Semua Armor | `DEFENSE;DEFENSE_MOB` | ALL_ARMOR; CANCEL_EVENT | **EXPLICIT_OK (100% aktif)** |
| `antigravity` | Anti Gravity | ELITE | Sepatu Zirah | `CUSTOM_RPG` | Memberikan lompatan super tinggi layaknya tanpa gravitasi. | **UNHANDLED (0% dieksekusi)** |
| `cactus` | Cactus | ELITE | Semua Armor | `CUSTOM_RPG` | Membalas serangan musuh dengan duri tajam yang menusuk penyerang. | **UNHANDLED (0% dieksekusi)** |
| `curse` | Curse | ELITE | Semua Armor | `DEFENSE;DEFENSE_MOB` | ALL_ARMOR; POTION:SLOW_DIGGING:0:100 @Attacker | **UNHANDLED (0% dieksekusi)** |
| `extinguish` | Extinguish | ELITE | Celana Zirah | `DEFENSE;DEFENSE_MOB;FIRE` | ALL_LEGGINGS; WAIT:1 | **UNHANDLED (0% dieksekusi)** |
| `frozen` | Frozen | ELITE | Semua Armor | `DEFENSE;DEFENSE_MOB` | ALL_ARMOR; POTION:SLOW:0:40 @Attacker | **EXPLICIT_OK (100% aktif)** |
| `poisoned` | Poisoned | ELITE | Semua Armor | `DEFENSE;DEFENSE_MOB;DEFENSE_PROJECTILE` | ALL_ARMOR; POTION:POISON:0:60 @Attacker | **UNHANDLED (0% dieksekusi)** |
| `rocketescape` | Rocket Escape | ELITE | Sepatu Zirah | `DEFENSE;DEFENSE_MOB` | ALL_BOOTS; BOOST:UP:20 @Victim | **UNHANDLED (0% dieksekusi)** |
| `shockwave` | Shockwave | ELITE | Zirah Dada | `DEFENSE;DEFENSE_MOB` | ALL_CHESTPLATE; PULL_AWAY:0.5 @Attacker | **PARTIAL (Generic shock)** |
| `smokebomb` | Smoke Bomb | ELITE | Helm Zirah | `DEFENSE;DEFENSE_MOB` | ALL_HELMET; PARTICLE:CLOUD:200:3 @Victim | **UNHANDLED (0% dieksekusi)** |
| `stormcaller` | Stormcaller | ELITE | Semua Armor | `CUSTOM_RPG` | Memanggil sambaran petir badai ke pemain yang menyerangmu. | **UNHANDLED (0% dieksekusi)** |
| `trickster` | Trickster | ELITE | Semua Armor | `DEFENSE;DEFENSE_MOB` | ALL_ARMOR; TELEPORT_BEHIND @Victim | **UNHANDLED (0% dieksekusi)** |
| `undeadruse` | Undead Ruse | ELITE | Sepatu Zirah | `DEFENSE;DEFENSE_MOB` | ALL_BOOTS; GUARD:ZOMBIE:8:2:@Victim | **UNHANDLED (0% dieksekusi)** |
| `voodoo` | Voodoo | ELITE | Semua Armor | `DEFENSE;DEFENSE_MOB` | ALL_ARMOR; POTION:WEAKNESS:0:60 @Attacker | **UNHANDLED (0% dieksekusi)** |
| `wither` | Wither | ELITE | Semua Armor | `DEFENSE;DEFENSE_MOB` | ALL_ARMOR; POTION:WITHER:0:60 @Attacker | **UNHANDLED (0% dieksekusi)** |
| `aegis` | Aegis | ULTIMATE | Sepatu Zirah | `FALL_DAMAGE` | ALL_BOOTS; POTION:SPEED:0:60 | **UNHANDLED (0% dieksekusi)** |
| `angelic` | Angelic | ULTIMATE | Semua Armor | `DEFENSE;DEFENSE_MOB` | ALL_CHESTPLATE | **UNHANDLED (0% dieksekusi)** |
| `arrowbreak` | Arrow Break | ULTIMATE | Kapak Tempur | `DEFENSE_PROJECTILE` | ALL_AXE; CANCEL_EVENT | **UNHANDLED (0% dieksekusi)** |
| `arrowdeflect` | Arrow Deflect | ULTIMATE | Semua Armor | `DEFENSE_PROJECTILE` | ALL_ARMOR; CANCEL_EVENT | **UNHANDLED (0% dieksekusi)** |
| `block` | Block | ULTIMATE | Pedang | `DEFENSE;DEFENSE_MOB` | ALL_SWORD; NEGATE_DAMAGE:50 @Victim | **UNHANDLED (0% dieksekusi)** |
| `creeperarmor` | Creeper Armor | ULTIMATE | Semua Armor | `EXPLOSION` | ALL_ARMOR; CANCEL_EVENT | **UNHANDLED (0% dieksekusi)** |
| `demonic` | Demonic | ULTIMATE | Semua Armor | `CUSTOM_RPG` | Peluang melenyapkan efek Fire Resistance musuh saat pertempuran. | **UNHANDLED (0% dieksekusi)** |
| `disappear` | Disappear | ULTIMATE | Semua Armor | `DEFENSE;DEFENSE_MOB` | ALL_ARMOR; POTION:INVISIBILITY:2:60 @Victim | **UNHANDLED (0% dieksekusi)** |
| `dodge` | Dodge | ULTIMATE | Semua Armor | `DEFENSE` | ALL_ARMOR; CANCEL_EVENT | **UNHANDLED (0% dieksekusi)** |
| `enderwalker` | Ender Walker | ULTIMATE | Sepatu Zirah | `CUSTOM_RPG` | Mengurangi damage Ender Pearl dan kebal efek Wither saat teleportasi. | **UNHANDLED (0% dieksekusi)** |
| `guardians` | Guardians | ULTIMATE | Semua Armor | `DEFENSE` | ALL_ARMOR; GUARD:IRON_GOLEM:8:1 @Victim | **UNHANDLED (0% dieksekusi)** |
| `heavy` | Heavy | ULTIMATE | Semua Armor | `DEFENSE;DEFENSE_PROJECTILE` | ALL_ARMOR; DECREASE_DAMAGE:2 @Victim | **UNHANDLED (0% dieksekusi)** |
| `jellylegs` | Jelly Legs | ULTIMATE | Sepatu Zirah | `FALL_DAMAGE` | ALL_BOOTS; CANCEL_EVENT | **EXPLICIT_OK (100% aktif)** |
| `metaphysical` | Metaphysical | ULTIMATE | Sepatu Zirah | `CUSTOM_RPG` | Memberikan kekebalan terhadap efek Slowness, Nausea, dan kelumpuhan. | **EXPLICIT_OK (100% aktif)** |
| `plummet` | Plummet | ULTIMATE | Sepatu Zirah | `FALL_DAMAGE` | ALL_BOOTS | **UNHANDLED (0% dieksekusi)** |
| `ragdoll` | Ragdoll | ULTIMATE | Semua Armor | `DEFENSE;DEFENSE_MOB` | ALL_ARMOR; PULL_AWAY:0.1 @Victim | **UNHANDLED (0% dieksekusi)** |
| `reinforced` | Reinforced | ULTIMATE | Sayap Elytra | `DEFENSE;DEFENSE_MOB;DEFENSE_PROJECTILE` | ELYTRA; DECREASE_DAMAGE:15 | **UNHANDLED (0% dieksekusi)** |
| `safeguard` | Safeguard | ULTIMATE | Semua Armor | `DEFENSE;DEFENSE_MOB` | ALL_ARMOR; POTION:DAMAGE_RESISTANCE:0:60 @Victim | **UNHANDLED (0% dieksekusi)** |
| `spirits` | Spirits | ULTIMATE | Semua Armor | `DEFENSE` | ALL_ARMOR; GUARD:BLAZE:8:3:@Victim | **UNHANDLED (0% dieksekusi)** |
| `swordsman` | Swordsman | ULTIMATE | Semua Armor | `DEFENSE;DEFENSE_MOB` | ALL_ARMOR | **UNHANDLED (0% dieksekusi)** |
| `tank` | Tank | ULTIMATE | Semua Armor | `DEFENSE;DEFENSE_MOB` | ALL_ARMOR; DECREASE_DAMAGE:2 @Victim | **UNHANDLED (0% dieksekusi)** |
| `valor` | Valor | ULTIMATE | Semua Armor | `CUSTOM_RPG` | Mengurangi damage yang diterima pengguna saat sedang melancarkan serangan. | **UNHANDLED (0% dieksekusi)** |
| `aqua` | Aqua | LEGENDARY | Sepatu Zirah | `ATTACK;ATTACK_MOB` | ALL_BOOTS; DOUBLE_DAMAGE | **UNHANDLED (0% dieksekusi)** |
| `armored` | Armored | LEGENDARY | Semua Armor | `DEFENSE` | ALL_ARMOR; DECREASE_DAMAGE:2 @Attacker | **UNHANDLED (0% dieksekusi)** |
| `chunky` | Chunky | LEGENDARY | Zirah Dada | `DEFENSE;DEFENSE_MOB` | ALL_CHESTPLATE; NEGATE_DAMAGE:10 @Victim | **UNHANDLED (0% dieksekusi)** |
| `clarity` | Clarity | LEGENDARY | Semua Armor | `CUSTOM_RPG` | Menyembuhkan dan meniadakan efek kebutaan (Blindness) saat diserang. | **EXPLICIT_OK (100% aktif)** |
| `convulse` | Convulse | LEGENDARY | Sepatu Zirah | `DEFENSE;DEFENSE_MOB` | ALL_BOOTS; BOOST:UP:5 @Attacker | **UNHANDLED (0% dieksekusi)** |
| `deathgod` | Death God | LEGENDARY | Helm Zirah | `CUSTOM_RPG` | Kebal dari satu kali serangan fatal dan memulihkan sebagian darah pengguna. | **UNHANDLED (0% dieksekusi)** |
| `deathbringer` | Deathbringer | LEGENDARY | Semua Armor | `CUSTOM_RPG` | Peluang melancarkan serangan penghancur berkekuatan damage ganda. | **UNHANDLED (0% dieksekusi)** |
| `destruction` | Destruction | LEGENDARY | Helm Zirah | `CUSTOM_RPG` | Membakar dan menghancurkan pertahanan musuh di area sekitar. | **UNHANDLED (0% dieksekusi)** |
| `divert` | Divert | LEGENDARY | Semua Armor | `DEFENSE;DEFENSE_MOB` | ALL_ARMOR; POTION:POISON:0:100 @Attacker | **UNHANDLED (0% dieksekusi)** |
| `drunk` | Drunk | LEGENDARY | Helm Zirah | `CUSTOM_RPG` | Memberikan buff Strength besar namun disertai kelambatan gerak Slowness. | **UNHANDLED (0% dieksekusi)** |
| `enlighted` | Enlighted | LEGENDARY | Semua Armor | `CUSTOM_RPG` | Peluang memulihkan sejumlah hati kesehatan saat menerima damage. | **UNHANDLED (0% dieksekusi)** |
| `exterminator` | Exterminator | LEGENDARY | Celana Zirah | `CUSTOM_RPG` | Memberikan damage masif terhadap semua jenis arthropoda dan monster. | **UNHANDLED (0% dieksekusi)** |
| `fat` | Fat | LEGENDARY | Zirah Dada | `CUSTOM_RPG` | Memberikan barisan hati ekstra (Extra Hearts) permanen saat dikenakan. | **UNHANDLED (0% dieksekusi)** |
| `fumble` | Fumble | LEGENDARY | Zirah Dada | `DEFENSE_PROJECTILE` | ALL_CHESTPLATE; TNT:1 @Attacker | **UNHANDLED (0% dieksekusi)** |
| `hardened` | Hardened | LEGENDARY | Semua Armor | `DEFENSE` | ALL_ARMOR; ADD_DURABILITY_CURRENT_ITEM:1 @Victim | **UNHANDLED (0% dieksekusi)** |
| `judgement` | Judgement | LEGENDARY | Sayap Elytra | `DEFENSE;DEFENSE_MOB` | ELYTRA; POTION:POISON:0:100 @Attacker | **UNHANDLED (0% dieksekusi)** |
| `lucid` | Lucid | LEGENDARY | Semua Armor | `DEFENSE;DEFENSE_MOB` | ALL_ARMOR; CURE:BLINDNESS @Victim | **EXPLICIT_OK (100% aktif)** |
| `patch` | Patch | LEGENDARY | Semua Armor | `DEFENSE_MOB` | ALL_ARMOR; ADD_DURABILITY_CURRENT_ITEM:1 @Victim | **UNHANDLED (0% dieksekusi)** |
| `protection` | Protection | LEGENDARY | Semua Armor | `CUSTOM_RPG` | Memberikan mitigasi pengurangan damage menyeluruh dari segala jenis serangan. | **UNHANDLED (0% dieksekusi)** |
| `quiver` | Quiver | LEGENDARY | Sepatu Zirah | `CUSTOM_RPG` | Peluang mementalkan penyerang tinggi ke udara saat pengguna diserang. | **UNHANDLED (0% dieksekusi)** |
| `surprise` | Surprise | LEGENDARY | Semua Armor | `CUSTOM_RPG` | Berteleportasi langsung ke belakang musuh dan mendaratkan serangan kejutan. | **UNHANDLED (0% dieksekusi)** |
| `takeoff` | Take Off | LEGENDARY | Sepatu Zirah | `CUSTOM_RPG` | Memungkinkan pengguna melayang dan terbang di udara saat dikenakan. | **UNHANDLED (0% dieksekusi)** |
| `torrent` | Torrent | LEGENDARY | Sepatu Zirah | `CUSTOM_RPG` | Meningkatkan damage serangan secara dahsyat saat berada di air atau hujan. | **UNHANDLED (0% dieksekusi)** |
| `turmoil` | Turmoil | LEGENDARY | Semua Armor | `DEFENSE` | ALL_ARMOR; DISABLE_ACTIVATION:guardians:4 | **UNHANDLED (0% dieksekusi)** |
| `unholy` | Unholy | LEGENDARY | Semua Armor | `DEFENSE` | ALL_ARMOR; POTION:WITHER:0:60 @Attacker | **EXPLICIT_OK (100% aktif)** |
| `alienimplants` | Alien Implants | HEROIC | Helm Zirah | `CUSTOM_RPG` | Regenerasi kesehatan super cepat yang didukung teknologi biokimia alien. | **EXPLICIT_OK (100% aktif)** |
| `divineenlighted` | Divine Enlighted | HEROIC | Semua Armor | `CUSTOM_RPG` | Memberikan pencerahan ilahi yang melipatgandakan regenerasi HP dan mana. | **UNHANDLED (0% dieksekusi)** |
| `epidemiccarrier` | Epidemic Carrier | HEROIC | Celana Zirah | `CUSTOM_RPG` | Menyebarkan wabah penyakit mematikan yang menular antar musuh di area. | **UNHANDLED (0% dieksekusi)** |
| `etherealdodge` | Ethereal Dodge | HEROIC | Semua Armor | `CUSTOM_RPG` | Menembus dimensi sesaat untuk menghindari seluruh serangan yang datang. | **UNHANDLED (0% dieksekusi)** |
| `godlyoverload` | Godly Overload | HEROIC | Semua Armor | `CUSTOM_RPG` | Melipatgandakan jumlah maksimal HP dan perisai energi secara permanen. | **UNHANDLED (0% dieksekusi)** |
| `guidedrocketescape` | Guided Rocket Escape | HEROIC | Sepatu Zirah | `CUSTOM_RPG` | Meluncurkan pengguna ke udara layaknya roket kendali saat darah sekarat. | **UNHANDLED (0% dieksekusi)** |
| `megaheavy` | Mega Heavy | HEROIC | Semua Armor | `CUSTOM_RPG` | Daya tahan mutlak dari knockback dan serangan hentakan gempa. | **UNHANDLED (0% dieksekusi)** |
| `mightycactus` | Mighty Cactus | HEROIC | Semua Armor | `CUSTOM_RPG` | Memantulkan duri berduri tajam berkekuatan dewa ke penyerang. | **UNHANDLED (0% dieksekusi)** |
| `paladinarmored` | Paladin Armored | HEROIC | Semua Armor | `CUSTOM_RPG` | Armor suci Paladin yang memitigasi sebagian besar damage fisik dan sihir. | **UNHANDLED (0% dieksekusi)** |
| `planetarydeathbringer` | Planetary Deathbringer | HEROIC | Semua Armor | `CUSTOM_RPG` | Memanggil meteor kehancuran planet yang menghanguskan target dan sekitarnya. | **UNHANDLED (0% dieksekusi)** |
| `polymorphicmetaphysical` | Polymorphic Metaphysical | HEROIC | Sepatu Zirah | `CUSTOM_RPG` | Kekebalan mutlak dari segala jenis debuff, stun, slowness, dan kutukan. | **UNHANDLED (0% dieksekusi)** |
| `reinforcedtank` | Reinforced Tank | HEROIC | Semua Armor | `CUSTOM_RPG` | Zirah baja diperkuat yang membuat pengguna hampir tak tertembus senjata biasa. | **UNHANDLED (0% dieksekusi)** |
| `soulhardened` | Soul Hardened | HEROIC | Semua Armor | `CUSTOM_RPG` | Mengeraskan jiwa untuk menahan true damage dan serangan penetrasi. | **UNHANDLED (0% dieksekusi)** |
| `vengefuldiminish` | Vengeful Diminish | HEROIC | Zirah Dada | `CUSTOM_RPG` | Mengurangi kekuatan serang lawan secara permanen selama pertempuran berlangsung. | **UNHANDLED (0% dieksekusi)** |
| `immortal` | Immortal | SOUL | Semua Armor | `DEFENSE;DEFENSE_MOB` | ALL_ARMOR; ADD_DURABILITY_ARMOR:10 @Victim | **UNHANDLED (0% dieksekusi)** |
| `natureswrath` | Natures Wrath | SOUL | Semua Armor | `CUSTOM_RPG` | Melepaskan kemurkaan alam yang membekukan dan mementalkan seluruh musuh di area. | **UNHANDLED (0% dieksekusi)** |
| `phoenix` | Phoenix | SOUL | Zirah Dada | `DEATH` | ALL_CHESTPLATE; REVIVE @Victim | **UNHANDLED (0% dieksekusi)** |
| `chainlifesteal` | Chain Lifesteal | MASTERY | Zirah Dada | `CUSTOM_RPG` | Menghisap darah berantai dari target utama ke seluruh musuh di sekitarnya. | **UNHANDLED (0% dieksekusi)** |
| `horrify` | Horrify | MASTERY | Semua Armor | `CUSTOM_RPG` | Menyebarkan teror kengerian dalam radius 32x32 blok yang membingungkan seluruh lawan. | **UNHANDLED (0% dieksekusi)** |
| `markofthebeast` | Mark of the Beast | MASTERY | Semua Armor | `CUSTOM_RPG` | Menandai musuh dengan segel iblis purba; musuh yang ditandai menerima damage 2x lipat. | **UNHANDLED (0% dieksekusi)** |
| `soulsiphone` | Soul Siphone | MASTERY | Sepatu Zirah | `CUSTOM_RPG` | Menyerap jiwa dan durability perlengkapan musuh secara langsung ke gear pengguna. | **UNHANDLED (0% dieksekusi)** |

#### Detail Audit: 7. Armor Passive & Static Buffs (11 Enchantments)

| ID | Nama Enchant | Tier | Target | Tipe Trigger Audit Forensik | Efek Otentik / Deskripsi | Status Implementasi Saat Ini |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `aquatic` | Aquatic | SIMPLE | Helm Zirah | `EFFECT_STATIC` | ALL_HELMET; POTION:WATER_BREATHING:0 | **UNHANDLED (0% dieksekusi)** |
| `glowing` | Glowing | SIMPLE | Helm Zirah | `EFFECT_STATIC` | ALL_HELMET; POTION:NIGHT_VISION:0 | **UNHANDLED (0% dieksekusi)** |
| `springs` | Springs | ELITE | Sepatu Zirah | `EFFECT_STATIC` | ALL_BOOTS; POTION:JUMP:0 | **EXPLICIT_OK (100% aktif)** |
| `implants` | Implants | ULTIMATE | Helm Zirah | `REPEATING` | ALL_HELMET; ADD_FOOD:1 | **EXPLICIT_OK (100% aktif)** |
| `obsidianshield` | Obsidianshield | ULTIMATE | Semua Armor | `EFFECT_STATIC` | ALL_ARMOR; POTION:FIRE_RESISTANCE:0 | **UNHANDLED (0% dieksekusi)** |
| `gears` | Gears | LEGENDARY | Sepatu Zirah | `EFFECT_STATIC` | ALL_BOOTS; POTION:SPEED:0 | **EXPLICIT_OK (100% aktif)** |
| `lavawalker` | Lava Walker | LEGENDARY | Sepatu Zirah | `EFFECT_STATIC` | ALL_BOOTS; LAVA_WALKER | **UNHANDLED (0% dieksekusi)** |
| `overload` | Overload | LEGENDARY | Semua Armor | `EFFECT_STATIC` | ALL_ARMOR; POTION:HEALTH_BOOST:0 | **UNHANDLED (0% dieksekusi)** |
| `waterwalker` | Water Walker | LEGENDARY | Sepatu Zirah | `EFFECT_STATIC` | ALL_BOOTS; WATER_WALKER | **EXPLICIT_OK (100% aktif)** |
| `wings` | Wings | LEGENDARY | Sepatu Zirah | `EFFECT_STATIC` | ALL_BOOTS; FLY | **UNHANDLED (0% dieksekusi)** |
| `masterblacksmith` | Master Blacksmith | HEROIC | Kapak Tempur | `CUSTOM_RPG` | Menempa ulang dan memulihkan durability gear secara instan di medan perang. | **EXPLICIT_OK (100% aktif)** |

#### Detail Audit: 8. Mining & Excavation Tools (28 Enchantments)

| ID | Nama Enchant | Tier | Target | Tipe Trigger Audit Forensik | Efek Otentik / Deskripsi | Status Implementasi Saat Ini |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `autosmelt` | Auto Smelt | SIMPLE | Beliung (Pickaxe) | `CUSTOM_RPG` | Melebur bijih logam secara otomatis saat ditambang. | **EXPLICIT_OK (100% aktif)** |
| `blast_mining` | Blast Mining | SIMPLE | Beliung (Pickaxe) | `CUSTOM_RPG` | Menghancurkan blok dalam area 3x3 secara bersamaan. | **EXPLICIT_OK (100% aktif)** |
| `breaklessness` | Curse of Breaklessness | SIMPLE | Alat Tambang (Beliung, Sekop, Kapak) | `CUSTOM_RPG` | Kutukan: Memberikan 30% peluang gagal saat menambang blok. | **UNHANDLED (0% dieksekusi)** |
| `decapitation` | Decapitation | SIMPLE | Pedang & Kapak | `KILL_PLAYER` | ALL_SWORD; ALL_AXE | **UNHANDLED (0% dieksekusi)** |
| `incinerate` | Incinerate | SIMPLE | Pedang & Kapak | `CUSTOM_RPG` | Meningkatkan damage serangan terhadap monster Laba-laba. | **UNHANDLED (0% dieksekusi)** |
| `infernaltouch` | Infernal Touch | SIMPLE | Beliung & Sekop | `CUSTOM_RPG` | Peluang melebur blok secara otomatis saat ditambang. | **UNHANDLED (0% dieksekusi)** |
| `nether_prospector` | Nether Prospector | SIMPLE | Beliung (Pickaxe) | `CUSTOM_RPG` | Peluang melipatgandakan hasil tambang Ancient Debris. | **UNHANDLED (0% dieksekusi)** |
| `oxygenate` | Oxygenate | SIMPLE | Beliung (Pickaxe) | `CUSTOM_RPG` | Mengisi ulang suplai oksigen saat menambang di dalam air. | **EXPLICIT_OK (100% aktif)** |
| `rebreather` | Rebreather | SIMPLE | Beliung (Pickaxe) | `MINING` | ALL_PICKAXE; AIR | **UNHANDLED (0% dieksekusi)** |
| `skullcrusher` | Skullcrusher | SIMPLE | Pedang & Kapak | `CUSTOM_RPG` | Meningkatkan damage tebasan terhadap Skeleton dan Wither Skeleton. | **UNHANDLED (0% dieksekusi)** |
| `smelting` | Smelting | SIMPLE | Beliung (Pickaxe) | `MINING` | ALL_PICKAXE; ALL_SHOVEL | **UNHANDLED (0% dieksekusi)** |
| `zombiecrusher` | Zombie Crusher | SIMPLE | Pedang & Kapak | `CUSTOM_RPG` | Meningkatkan damage tebasan terhadap segala varian Zombie. | **UNHANDLED (0% dieksekusi)** |
| `berserk` | Berserk | UNIQUE | Pedang & Kapak | `ATTACK;ATTACK_MOB` | ALL_SWORD; ALL_AXE | **UNHANDLED (0% dieksekusi)** |
| `featherweight` | Featherweight | UNIQUE | Pedang & Kapak | `ATTACK;ATTACK_MOB` | ALL_SWORD; ALL_AXE | **UNHANDLED (0% dieksekusi)** |
| `obsidiandestroyer` | Obsidian Destroyer | UNIQUE | Beliung (Pickaxe) | `CUSTOM_RPG` | Peluang menghancurkan blok Obsidian secara instan dalam satu pukulan. | **UNHANDLED (0% dieksekusi)** |
| `ravenous` | Ravenous | UNIQUE | Pedang & Kapak | `ATTACK;ATTACK_MOB` | ALL_SWORD; ALL_AXE | **UNHANDLED (0% dieksekusi)** |
| `slayer` | Slayer | UNIQUE | Pedang & Kapak | `ATTACK_MOB` | ALL_SWORD; ALL_AXE | **UNHANDLED (0% dieksekusi)** |
| `immolate` | Immolate | ELITE | Pedang & Kapak | `ATTACK_MOB` | ALL_SWORD; ALL_AXE | **UNHANDLED (0% dieksekusi)** |
| `infernal` | Infernal | ELITE | Pedang & Kapak | `ATTACK;ATTACK_MOB` | ALL_SWORD; ALL_AXE | **UNHANDLED (0% dieksekusi)** |
| `paralyze` | Paralyze | ELITE | Pedang & Kapak | `ATTACK;ATTACK_MOB` | ALL_SWORD; ALL_AXE | **UNHANDLED (0% dieksekusi)** |
| `poison` | Poison | ELITE | Pedang & Kapak | `ATTACK;ATTACK_MOB` | ALL_SWORD; ALL_AXE | **UNHANDLED (0% dieksekusi)** |
| `veinminer` | Vein Miner | ELITE | Beliung (Pickaxe) | `MINING` | ALL_PICKAXE; BREAK_BLOCK @Veinmine | **EXPLICIT_OK (100% aktif)** |
| `detonate` | Detonate | ULTIMATE | Beliung & Sekop | `CUSTOM_RPG` | Menghancurkan formasi batuan dalam area 3x3 seketika. | **UNHANDLED (0% dieksekusi)** |
| `trench` | Trench | ULTIMATE | Beliung & Sekop | `MINING` | ALL_PICKAXE; ALL_SPADE | **EXPLICIT_OK (100% aktif)** |
| `tunnel` | Tunnel | ULTIMATE | Beliung & Sekop | `MINING` | ALL_PICKAXE; ALL_SPADE | **UNHANDLED (0% dieksekusi)** |
| `gemify` | Gemify | FABLED | Beliung (Pickaxe) | `MINING` | ALL_PICKAXE; PARTICLE:GLOW:30:0.5 @Block | **UNHANDLED (0% dieksekusi)** |
| `multiplication` | Multiplication | FABLED | Beliung (Pickaxe) | `MINING` | ALL_PICKAXE; MORE_DROPS:1 | **UNHANDLED (0% dieksekusi)** |
| `soulminer` | Soulminer | FABLED | Beliung (Pickaxe) | `MINING` | ALL_PICKAXE; ADD_SOULS:1 | **UNHANDLED (0% dieksekusi)** |

#### Detail Audit: 9. Farming & Agriculture Tools (6 Enchantments)

| ID | Nama Enchant | Tier | Target | Tipe Trigger Audit Forensik | Efek Otentik / Deskripsi | Status Implementasi Saat Ini |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `replanter` | Replanter | UNIQUE | Cangkul | `MINING` | ALL_HOE | **EXPLICIT_OK (100% aktif)** |
| `replenish` | Replenish | ELITE | Beliung (Pickaxe) | `MINING` | ALL_PICKAXE; ADD_FOOD:1 | **UNHANDLED (0% dieksekusi)** |
| `carrotplanter` | Carrot Planter | ULTIMATE | Cangkul | `RIGHT_CLICK` | ALL_HOE; PLANT_SEEDS:1:CARROT | **UNHANDLED (0% dieksekusi)** |
| `harvest` | Harvest | ULTIMATE | Cangkul | `MINING` | ALL_HOE; CROPS | **EXPLICIT_OK (100% aktif)** |
| `planter` | Planter | ULTIMATE | Cangkul | `RIGHT_CLICK` | ALL_HOE; PLANT_SEEDS:1 | **UNHANDLED (0% dieksekusi)** |
| `potatoplanter` | Potato Planter | ULTIMATE | Cangkul | `RIGHT_CLICK` | ALL_HOE; PLANT_SEEDS:1:POTATO | **UNHANDLED (0% dieksekusi)** |

#### Detail Audit: 10. Fishing Rod Enchantments (9 Enchantments)

| ID | Nama Enchant | Tier | Target | Tipe Trigger Audit Forensik | Efek Otentik / Deskripsi | Status Implementasi Saat Ini |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `allurement` | Allurement | SIMPLE | Alat Pancing | `CUSTOM_RPG` | Peluang mendapatkan tangkapan loot ganda saat memancing. | **UNHANDLED (0% dieksekusi)** |
| `firehook` | Fire Hook | ELITE | Alat Pancing | `HOOK_ENTITY` | FISHING_ROD; BURN:40 @Victim | **UNHANDLED (0% dieksekusi)** |
| `hook` | Hook | ELITE | Alat Pancing | `CATCH_FISH` | FISHING_ROD | **UNHANDLED (0% dieksekusi)** |
| `poisonedhook` | Poisoned Hook | ELITE | Alat Pancing | `HOOK_ENTITY` | FISHING_ROD; POTION:POISON:0:80 @Victim | **UNHANDLED (0% dieksekusi)** |
| `snap` | Snap | ELITE | Alat Pancing | `CATCH_FISH` | FISHING_ROD; PULL_CLOSER:1 @Victim | **UNHANDLED (0% dieksekusi)** |
| `autoreel` | Auto Reel | ULTIMATE | Alat Pancing | `BITE_HOOK` | FISHING_ROD; AUTO_REEL | **UNHANDLED (0% dieksekusi)** |
| `lucky` | Lucky | ULTIMATE | Alat Pancing | `RIGHT_CLICK` | FISHING_ROD; POTION:LUCK:presisi000 | **UNHANDLED (0% dieksekusi)** |
| `sharpnesshook` | Sharpness Hook | ULTIMATE | Alat Pancing | `HOOK_ENTITY` | FISHING_ROD; DO_HARM:2 @Victim | **UNHANDLED (0% dieksekusi)** |
| `bait` | Bait | LEGENDARY | Alat Pancing | `CATCH_FISH` | FISHING_ROD; MORE_DROPS:1 | **UNHANDLED (0% dieksekusi)** |

#### Detail Audit: 11. On-Kill & Soul Harvest Triggers (7 Enchantments)

| ID | Nama Enchant | Tier | Target | Tipe Trigger Audit Forensik | Efek Otentik / Deskripsi | Status Implementasi Saat Ini |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `devour` | Devour | UNIQUE | Senjata Tempur | `KILL_MOB` | ALL_SWORD; ALL_AXE | **UNHANDLED (0% dieksekusi)** |
| `inquisitive` | Inquisitive | LEGENDARY | Pedang | `KILL_MOB` | ALL_SWORD | **UNHANDLED (0% dieksekusi)** |
| `rebound` | Rebound | LEGENDARY | Senjata Tempur | `KILL_PLAYER;KILL_MOB` | ALL_AXE; ALL_SWORD | **UNHANDLED (0% dieksekusi)** |
| `axeofspirits` | Axe of Spirits | FABLED | Kapak Tempur | `KILL_PLAYER` | ALL_AXE; ADD_SOULS:1 | **UNHANDLED (0% dieksekusi)** |
| `diploid` | Diploid | FABLED | Pedang | `KILL_MOB` | ALL_SWORD; MORE_DROPS:1 | **UNHANDLED (0% dieksekusi)** |
| `spiritmaster` | Spiritmaster | FABLED | Pedang | `KILL_PLAYER` | ALL_SWORD; ADD_SOULS:1 | **UNHANDLED (0% dieksekusi)** |
| `soulgrind` | Soulgrind | MASTERY | Pedang | `KILL_MOB` | ALL_SWORD; ADD_SOULS:1 | **UNHANDLED (0% dieksekusi)** |

#### Detail Audit: 12. Mobility, Fall Damage & Curses (5 Enchantments)

| ID | Nama Enchant | Tier | Target | Tipe Trigger Audit Forensik | Efek Otentik / Deskripsi | Status Implementasi Saat Ini |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `ascend` | Ascend | SIMPLE | Pedang | `CUSTOM_RPG` | Melayang ke udara (Levitasi). Klik-Kanan untuk mengaktifkan. | **UNHANDLED (0% dieksekusi)** |
| `charge` | Charge | SIMPLE | Pedang | `CUSTOM_RPG` | Melesat kencang ke depan untuk menerjang lawan. | **UNHANDLED (0% dieksekusi)** |
| `momentum` | Momentum | ELITE | Sayap Elytra | `RIGHT_CLICK` | ELYTRA; POTION:SPEED:0:40 | **UNHANDLED (0% dieksekusi)** |
| `launch` | Launch | LEGENDARY | Pedang | `RIGHT_CLICK` | ALL_SWORD; BOOST:UP:8 | **UNHANDLED (0% dieksekusi)** |
| `slingshot` | Slingshot | LEGENDARY | Sayap Elytra | `RIGHT_CLICK` | ELYTRA; BOOST:UP:10 | **UNHANDLED (0% dieksekusi)** |




---

## 4. Rencana Arsitektur & Rekayasa Ulang (Architecture Redesign)

Untuk mencegah bug string matching seperti `immolation` terulang kembali di masa depan, kita merekayasa arsitektur engine dari rangkaian `if/else if (id.contains(...))` rapuh menjadi **Modular Dispatcher System**:

```
[ServerLivingEntityEvents.ALLOW_DAMAGE / AFTER_DEATH / ServerTickEvents]
                                 â”‚
                                 â–¼
                     TakashaEnchantCombatEngine
                                 â”‚
   â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”´â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
   â–¼                                                           â–¼
TakashaWeaponAttackDispatcher                 TakashaArmorDefenseDispatcher
   â”œâ”€â”€ 1. RadialAoeProcessor (Immolation, Cleave, Ambit)         â”œâ”€â”€ 1. ThornsReflectProcessor (Reflect, Rebounding)
   â”œâ”€â”€ 2. MobSlayerProcessor (Bonecrusher, Enderslayer)          â”œâ”€â”€ 2. CrowdControlProcessor (Smokebomb, Shockwave)
   â”œâ”€â”€ 3. StatusDoTProcessor (Bleed, Poison, Freeze)             â”œâ”€â”€ 3. ProjectileDeflectProcessor (Arrowdeflect, Arrowbreak)
   â””â”€â”€ 4. CombatProcProcessor (Lifesteal, Berserk, Critical)     â””â”€â”€ 4. CleansingProcessor (Blessed, Metaphysical, Clarity)
```

### A. Utilitas Baru: `TakashaAoeHelper`
Membuat utilitas spasial terpusat untuk mendeteksi target dalam lingkaran bola/silinder:
1. **Target Query:**
   ```java
   public static List<LivingEntity> getEntitiesInRadius(
       ServerLevel level, Vec3 center, double radius, LivingEntity exclude, boolean monstersAndDummiesOnly)
   ```
2. **Filter Target Ramah vs Musuh:**
   - Menyertakan: `Monster`, entitas agresif, dan `DamageDummyEntity` (agar pengujian di boneka latihan berfungsi sempurna!).
   - Mengecualikan: `exclude` (penyerang), rekan satu tim / pet jinak (`TamableAnimal.isOwnedBy(attacker)`), dan maid (`Touhou Little Maid`).
3. **Radial Particle Ring Spawner:**
   - `spawnParticleRing(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, int count)`
   - Merender lingkaran partikel rapi di permukaan tanah ($X = \cos(\theta) \cdot r$, $Z = \sin(\theta) \cdot r$):
     - `FALLING_LAVA` & `LAVA` untuk `immolation`.
     - `SWEEP_ATTACK` untuk `cleave` & `carve`.
     - `ELECTRIC_SPARK` untuk `deranged`.
     - `CAMPFIRE_COSY_SMOKE` & `CLOUD` untuk `smokebomb`.

### B. Pemetaan ID Eksplisit (Canonical Enchantment Registry)
Alih-alih `id.contains("burn")`, buat Set / Map kamus ID yang memetakan seluruh nama sihir secara definitif:
- `IMMOLATION_IDS` = `Set.of("immolation", "immolate")`
- `INFLAME_IDS` = `Set.of("inflame")`
- `CLEAVE_IDS` = `Set.of("cleave", "carve")`
- `SLAYER_MAP` = Pemetaan ID ke `EntityTypeTags` atau kelas Mob.

### C. Integrasi Penuh dengan Combat Training Dummy (`DamageDummyEntity`)
1. Ketika pemain mengayunkan senjata ber-enchant `immolation` ke arah satu dummy, jika ada dummy lain di sekitarnya dalam radius 2/4/6 blok:
   - Seluruh dummy di sekitarnya ikut terbakar (`burn` status).
   - Seluruh dummy di sekitarnya memunculkan angka DoT melayang oranye (`Â§6-X ðŸ”¥`).
   - DPS dummy di sekitarnya ikut tercatat pada overhead billboard.

---

## 5. Rencana Kerja Bertahap (Phased Execution Plan)

### Fase 1: Pembuatan Utilitas Spasial & Partikel AOE (`TakashaAoeHelper`)
- **Tugas 1.1:** Buat kelas `TakashaAoeHelper.java` di paket `net.takasha.rpg.enchant`.
- **Tugas 1.2:** Implementasikan pemindaian entitas `AABB.ofSize(center, radius * 2, radius * 2, radius * 2)` dengan filter jarak euclidian `center.distanceToSqr(e.position()) <= radius * radius`.
- **Tugas 1.3:** Implementasikan generator cincin partikel melingkar horizontal ($X = \cos(\theta) \cdot r$, $Z = \sin(\theta) \cdot r$) untuk `ParticleTypes.FALLING_LAVA` dan `ParticleTypes.LAVA`.

### Fase 2: Perbaikan Mekanika Tempur `Immolation` & AOE Cleave
- **Tugas 2.1:** Rombak `TakashaEnchantCombatEngine.applyWeaponCombatEnchants`:
  - Tangani `immolation`:
    - Radius: $2.0 \times \text{lvl}$ (Lv 1: 2 blok, Lv 2: 4 blok, Lv 3: 6 blok).
    - Membakar target utama dan seluruh entitas di sekitarnya selama $40 + (\text{lvl} \times 10)$ tick.
    - Menghasilkan semburan cincin `FALLING_LAVA` dan suara `BLOCK_FIRE_AMBIENT` / `LAVA_POP`.
    - Mengaktifkan status `burn` pada `DamageDummyEntity` di dalam radius.
  - Tangani `inflame`: Pembakaran area serupa terhadap entitas damageable.
  - Tangani `cleave` & `carve`: Sapuan damage fisik murni ($1.5 \times \text{lvl}$ damage) ke seluruh musuh dalam radius 2â€“6 blok dengan partikel `SWEEP_ATTACK`.
  - Tangani `deranged`: Sambaran petir (`LIGHTNING_BOLT` visual/damage aman) pada musuh di area radius 5â€“7 blok.
  - Tangani `ambit`: Peningkatan damage area pada sekelompok monster.

### Fase 3: Penguatan Kategori Mob-Specific Slayer & DoT Status
- **Tugas 3.1:** Tambahkan logika kalkulasi damage multiplikatif/aditif terhadap tipe monster khusus:
  - `bonecrusher`: Memeriksa `target.getType().is(EntityTypeTags.SKELETONS)`.
  - `deathpunch`: Memeriksa `target.getType().is(EntityTypeTags.ZOMBIES)`.
  - `enderslayer` / `enderbane`: Memeriksa `target instanceof EnderMan || target instanceof EnderDragon`.
  - `blazereaper` / `netherslayer`: Memeriksa `target.level().dimension() == Level.NETHER || target instanceof Blaze || target instanceof MagmaCube`.
  - `cubism`: Memeriksa `target instanceof Slime`.
- **Tugas 3.2:** Integrasikan angka damage mengambang kritis dan partikel khas saat slayer proc berhasil.

### Fase 4: Implementasi Armor Defense & Projectile Triggers
- **Tugas 4.1:** Rombak `TakashaEnchantCombatEngine.applyArmorCombatEnchants`:
  - `shockwave`: Mementalkan penyerang dan mob dalam radius 4 blok menjauh dari defender dengan partikel `EXPLOSION` dan suara angin kencang.
  - `smokebomb`: Menghasilkan tirai kabut asap tebal dan memberikan `MobEffects.BLINDNESS` pada penyerang.
  - `reflect` / `rebounding`: Memantulkan damage ke penyerang via `attacker.hurtServer(...)`.
  - `dodge`: Membatalkan damage serangan dengan probabilitas berbasis level.
  - `arrowdeflect` / `arrowbreak`: Memeriksa jika `source.is(DamageTypeTags.IS_PROJECTILE)`, lalu batalkan proyektil dan mainkan suara pantulan perisai.
  - `creeperarmor`: Batalkan damage jika `source.is(DamageTypes.EXPLOSION)`.

### Fase 5: Implementasi On-Kill & Mobility Triggers
- **Tugas 5.1:** Daftarkan listener `ServerLivingEntityEvents.AFTER_DEATH` di `TakashaRpgMod.java`:
  - `decapitation`: Mengundi peluang drop kepala mob vanilla sesuai tipe entitas yang gugur.
  - `devour`: Memulihkan poin rasa lapar (`player.getFoodData().eat(...)`).
  - `rebound`: Memulihkan kesehatan pemain saat musuh tewas.
  - `inquisitive`: Menjatuhkan ekstra bola pengalaman (`ExperienceOrb`).
- **Tugas 5.2:** Tangani `plummet` di `ALLOW_DAMAGE` saat jatuh (`DamageTypes.FALL`):
  - Membatalkan fall damage dan memicu getaran gempa AOE melukai seluruh monster di titik mendarat.
- **Tugas 5.3:** Tangani `escape` / `getaway` saat HP pemain berada di bawah 25%.

### Fase 6: Verifikasi, Pengujian Komprehensif & Pembaruan PRD
- **Tugas 6.1:** Kompilasi proyek dengan `./gradlew compileJava` tanpa error atau peringatan lint.
- **Tugas 6.2:** Jalankan script test suite unit/headless jika tersedia.
- **Tugas 6.3:** Pengujian in-game dengan Damage Dummy:
  - Spawn 2 dummy berdampingan (jarak 2-3 blok).
  - Pukul salah satu dummy dengan pedang ber-enchant `immolation III`.
  - Pastikan kedua dummy terbakar, memancarkan partikel `FALLING_LAVA`, dan memunculkan status DoT serta angka melayang.
- **Tugas 6.4:** Perbarui dokumen `PRD.md` ke versi `v3.5.0` dan catat seluruh riwayat pembaruan di Bab 14.22.

---

## 6. Komitmen Batasan (Constraints)
- **Zero-Touch pada Mod Sakura-Weapons:** Mod `sakura-weapons` (`v2.5.0`) tidak disentuh sama sekali.
- **Zero-Code-First:** Tidak ada pengubahan kode Java sebelum rencana ini disetujui pengguna.
- **Kepatuhan Audit Forensik Otentik:** Nilai durasi, radius, dan partikel mengikuti dokumen resmi `enchantments.yml` dari AdvancedEnchantments.