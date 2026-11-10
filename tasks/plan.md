# Master Implementation Plan: Standarisasi Sistem RPG Items (35 Modul RPG) & Resolusi Konfigurasi /drop Mod Takasha

## 1. Executive Summary & Dual Objective

Rencana induk ini disusun secara mendalam menggunakan metodologi **Audit Forensik Bytecode** serta seluruh **35 domain arsitektur sistem RPG Items**, ditambah penyelesaian definitif untuk **masalah ketidakjelasan konfigurasi perintah `/drop` pada mod Takasha (`sakura-weapons`)**.

---

## 2. Bedah Lengkap 35 Modul Arsitektur Sistem RPG Items & Audit Bytecode

Berikut adalah rincian lengkap per modul: **Isi Data & Konfigurasi**, **Cara Kerja & Alur Operasional**, serta **Implementasi Native di Fabric 26.2 Takasha-RPG**.

### Cluster 1: Perintah & Manajemen Sistem (Modul 1 - 7)

#### 1. `general/commands`
- **Domain Referensi:** `Standar Arsitektur Sistem RPG`
- **Komponen Arsitektur:** `*` (`CreateCommandTreeNode`, `EditCommandTreeNode`, `BrowseCommandTreeNode`, `CopyCommandTreeNode`, `DeleteCommandTreeNode`, `GiveCommandTreeNode`, `GiveAllCommandTreeNode`, `TakeCommandTreeNode`, `DropCommandTreeNode`, `ReloadCommandTreeNode`, `UpdateItemCommand`, `RevisionIDCommandTreeNode`).
- **Isi Modul:**
  - Pohon perintah `/mi` dengan 17 sub-perintah administratif dan pengujian.
  - Parameter wajib: `<type>` (kategori item) dan `<id>` (identifier template item).
  - Parameter opsional: `[player]`, `[amount]`, `[unloader-charges]`, `[drop-chance]`.
- **Cara Kerja:**
  - Memeriksa hak akses pemain (pada Takasha-RPG dibatasi eksklusif pemain dengan status `player.isCreative()`).
  - Memvalidasi argumen tipe item (`Type.isValid(type)`). Jika tidak valid, mengirim daftar tipe item yang tersedia: `There is no item type called <type>. Please choose one among: ...`.
  - Normalisasi string ID menjadi format uppercase dan penggantian strip menjadi garis bawah (`id.toUpperCase().replace("-", "_")`).
  - Menjalankan aksi sesuai command: `/mi create` langsung menginstansiasi template dan membuka GUI `ItemEdition`; `/mi edit` membuka GUI editor untuk item yang sudah ada; `/mi copy` menduplikasi YAML konfigurasi; `/mi delete` menghapus dari disk dan cache; `/mi browse` membuka `TypeBrowser`; `/mi give` merender `ItemStack` live dengan NBT lengkap dan memasukkannya ke inventori pemain.
- **Implementasi Fabric 26.2:** Ditangani di `net/takasha/rpg/command/MMOCommand.java` terintegrasi dengan Brigadier command dispatcher.

#### 2. `general/placeholders`
- **Domain Referensi:** `Standar Arsitektur Sistem RPG`
- **Komponen Arsitektur:** `class`.
- **Isi Modul:**
  - Identifier placeholder: `%rpg_items_stat_<stat>%`, `%rpg_items_item_<stat>%`, `%rpg_items_durability%`, `%rpg_items_max_durability%`, `%rpg_items_durability_bar%`, `%rpg_items_tier%`, `%rpg_items_type%`.
- **Cara Kerja:**
  - Hook ke engine placeholder (PlaceholderAPI / MiniMessage).
  - Mengambil item yang sedang dipegang di main-hand pemain, membaca NBT dual-layer `tag NBT RPG` atau CompoundTag `TakashaMMOItem`.
  - Menghitung bar durabilitas visual (`getCurrentDurabilityBar`) dengan format progress simbol seperti `■■■■■□□□□□` sesuai rasio durabilitas saat ini terhadap maksimum durabilitas.
- **Implementasi Fabric 26.2:** Terintegrasi di `net/takasha/rpg/mmo/MiniMessageParser.java` dan rendering lore item.

#### 3. `items/creation`
- **Domain Referensi:** `Standar Arsitektur Sistem RPG`
- **Komponen Arsitektur:** `class`, `class`.
- **Isi Modul:**
  - Alur kreasi item in-game: Perintah `/mi create <type> <id>` dan tombol Slot 51 `ANVIL` ("Create New") di `ItemBrowser`.
- **Cara Kerja:**
  - Jika dipicu dari GUI: memanggil `NewItemEdition.enable()`, menutup GUI inventori, mencetak pesan dekorasi strip kuning `&e&m-----------------------------------------------------`, prompt `Write in the chat the ID of the new item.`, `Type 'cancel' to abort.`, dan mengirim title layar `&6&lItem Creation` / `See chat.`.
  - Server mengintersepsi pesan chat berikutnya via listener: memvalidasi karakter ID, membuat konfigurasi awal dengan `base.material` default tipe, menyimpan ke disk, dan langsung membuka GUI `ItemEdition` untuk item baru tersebut.
- **Implementasi Fabric 26.2:** `net/takasha/rpg/menu/MMOChatEditionEngine.java` dan `net/takasha/rpg/menu/MMOItemBrowserMenu.java`.

#### 4. `items/templates`
- **Domain Referensi:** `Standar Arsitektur Sistem RPG`
- **Komponen Arsitektur:** `class`, `class`.
- **Isi Modul:**
  - Struktur file konfigurasi per-tipe (misal `tool.yml`, `sword.yml`, `armor.yml`):
    ```yaml
    ITEM_ID:
      base:
        material: DIAMOND_PICKAXE
        name: '&eNama Item'
        attack-damage: 12
      modifiers:
        SHARP:
          prefix: '&fTajam '
          stats:
            attack-damage: 3..6
    ```
- **Cara Kerja:**
  - Template memuat blok data `base` sebagai nilai stat statis atau baseline.
  - Saat item digenerate via `/mi give` atau reward dungeon, template menggabungkan nilai `base` dengan modifikasi kondisional atau roll acak dari `modifiers`.
- **Implementasi Fabric 26.2:** `net/takasha/rpg/mmo/MMOItemTemplate.java` dan `net/takasha/rpg/mmo/MMOItemManager.java`.

#### 5. `items/modifiers`
- **Domain Referensi:** `Standar Arsitektur Sistem RPG`
- **Komponen Arsitektur:** `class`, `*`.
- **Isi Modul:**
  - Node modifier acak dengan bobot probabilitas (*weight*), prefix nama, suffix nama, dan rentang stat numerik (misal `5..10`).
- **Cara Kerja:**
  - Generator item melakukan evaluasi probabilitas roll modifier saat instansiasi item.
  - Jika modifier terpilih, nama item dimodifikasi dengan prefix/suffix (misal `"Tajam " + "Pedang Baja"`), dan rentang angka di-roll menghasilkan nilai integer/double acak yang diinjeksi ke stat NBT.
- **Implementasi Fabric 26.2:** `net/takasha/rpg/mmo/MMOItemTemplate.java` (metode `rollModifiers()`).

#### 6. `items/revision-id`
- **Domain Referensi:** `Standar Arsitektur Sistem RPG`
- **Komponen Arsitektur:** `class`, `class`.
- **Isi Modul:**
  - Kunci konfigurasi `revision-id: <integer>` dan NBT tag `RPG_REVISION_ID`.
- **Cara Kerja:**
  - Saat admin mengubah konfigurasi template item di server dan menaikkan revision-id, item lama di tas pemain yang memiliki NBT `RPG_REVISION_ID` lebih rendah akan otomatis di-update saat pemain membuka inventori atau melalui perintah `/mi update`.
  - Item diperbarui stat dan lore-nya tanpa menghilangkan gem stone, enchant kustom, atau soulbound yang sudah terpasang.
- **Implementasi Fabric 26.2:** Pengecekan versi NBT pada event container update di `net/takasha/rpg/mmo/MMOItemManager.java`.

#### 7. `items/obtain-item`
- **Domain Referensi:** `Standar Arsitektur Sistem RPG`
- **Komponen Arsitektur:** `class`, `class`.
- **Isi Modul:**
  - Alur perolehan item di dunia game: drop monster, drop penambangan blok kustom, peti loot dungeon, dan stasiun crafting.
- **Cara Kerja:**
  - Di mode kreatif, item diperoleh via GUI atau `/mi give`.
  - Di mode survival, drop diproteksi murni melalui tabel drop dungeon akumulatif (`DungeonRewardAccumulator.java`) berdasarkan gelombang musuh (Wave 3, 4, 5).
- **Implementasi Fabric 26.2:** `net/takasha/rpg/dungeon/reward/DungeonRewardAccumulator.java`.

---

### Cluster 2: Statistik, Atribut & Durabilitas (Modul 8 - 12)

#### 8. `features/stats/stats`
- **Domain Referensi:** `Standar Arsitektur Sistem RPG`
- **Komponen Arsitektur:** `class`, package `*` (122 stat classes).
- **Isi Modul:**
  - Katalog stat lengkap: Physical Damage, Attack Speed, Defense, Critical Strike Chance, Knockback, Max Durability, Movement Speed, Armor, Armor Toughness, Luck.
- **Cara Kerja:**
  - Setiap stat mengimplementasikan class `ItemStat` dengan hook visual GUI (`whenDisplayed`), penanganan klik GUI (`whenClicked`), input chat (`whenInput`), dan penerapan NBT item builder (`whenApplied`).
- **Implementasi Fabric 26.2:** `net/takasha/rpg/mmo/MMOStatType.java`.

#### 9. `features/stats/numerical-stats`
- **Domain Referensi:** `Standar Arsitektur Sistem RPG`
- **Komponen Arsitektur:** `class`, `class`.
- **Isi Modul:**
  - Penskalaan stat desimal, format persentase, dan flag `multiplyWhenDisplaying()`.
- **Cara Kerja:**
  - Nilai disimpan sebagai angka floating point murni di NBT (misal `0.15`), namun saat dirender ke tooltip lore dikalikan 100 dengan simbol `%` (menjadi `+15% Critical Chance`).
- **Implementasi Fabric 26.2:** `net/takasha/rpg/mmo/MMOStatType.java` (metode `formatValue()`).

#### 10. `features/stats/stats-advanced`
- **Domain Referensi:** `Standar Arsitektur Sistem RPG`
- **Komponen Arsitektur:** `class`, `class`, `class`.
- **Isi Modul:**
  - Atribut alat tingkat lanjut: `AUTOSMELT` (melebur ore langsung saat ditambang), `BOUNCING_CRACK` (menambang area 3x3x3), dan `PICKAXE_POWER` (tier efektivitas memecah ore keras).
- **Cara Kerja:**
  - Terintegrasi langsung pada event block break Fabric `PlayerBlockBreakEvents.BEFORE`. Jika item memiliki tag NBT `RPG_AUTOSMELT = true`, drop bijih besi langsung dikonversi menjadi ingot besi disertai partikel lava. Jika `RPG_BOUNCING_CRACK = true`, memecah hingga 8 blok sejenis di sekitarnya.
- **Implementasi Fabric 26.2:** `net/takasha/rpg/enchant/TakashaEnchantUtilityEngine.java`.

#### 11. `features/custom-durability`
- **Domain Referensi:** `Standar Arsitektur Sistem RPG`
- **Komponen Arsitektur:** `class`, `class`.
- **Isi Modul:**
  - Tag NBT: `RPG_MAX_DURABILITY`, `RPG_DURABILITY`, `RPG_WILL_BREAK`.
- **Cara Kerja:**
  - Memisahkan durabilitas custom dari durabilitas vanilla Minecraft.
  - Setiap penggunaan perkakas/senjata mengurangi 1 poin durabilitas custom.
  - Item menampilkan lore `Durability: {cur} / {max}`. Jika habis dan `will-break = false`, item menjadi tumpul dan tidak bisa digunakan tanpa hancur; jika `will-break = true`, item pecah menghasilkan efek `ITEM_BREAK`.
- **Implementasi Fabric 26.2:** `net/takasha/rpg/mmo/MMOItemTemplate.java` dan `TakashaEnchantUtilityEngine.java`.

#### 12. `features/gem-stones`
- **Domain Referensi:** `Standar Arsitektur Sistem RPG`
- **Komponen Arsitektur:** `class`, `class`, `class`.
- **Isi Modul:**
  - Definisi tipe socket: `empty`, `ruby`, `sapphire`, `topaz`.
  - Item tipe `GEM_STONE` yang memberikan bonus stat saat dipasang ke socket kosong.
- **Cara Kerja:**
  - Pemain dapat men-drag dan meletakkan permata di atas item senjata/armor yang memiliki slot socket kosong di antarmuka inventori/anvil.
  - `GemStone.applyOntoItem()` memvalidasi kecocokan tipe socket, menyerap stat permata ke dalam NBT senjata, dan memperbarui lore socket dari `[Empty Socket]` menjadi `[Ruby: +5 Attack Damage]`.
- **Implementasi Fabric 26.2:** `net/takasha/rpg/mmo/MMOItemData.java` dan penanganan container click.

---

### Cluster 3: Presentasi Visual, Lore & Tooltip (Modul 13 - 18)

#### 13. `features/item-textures`
- **Domain Referensi:** `Standar Arsitektur Sistem RPG`
- **Komponen Arsitektur:** `class`.
- **Isi Modul:**
  - Tag NBT integer `CustomModelData` dan pemetaan tekstur/model json kustom.
- **Cara Kerja:**
  - Injeksi integer CMD ke dalam Minecraft `DataComponents.CUSTOM_MODEL_DATA` sehingga item vanilla menampilkan tekstur resolusi tinggi atau model 3D Blockbench kustom tanpa menimpa item vanilla lain.
- **Implementasi Fabric 26.2:** `net/takasha/rpg/mmo/MMOItemTemplate.java`.

#### 14. `features/tiers`
- **Domain Referensi:** `Standar Arsitektur Sistem RPG`
- **Komponen Arsitektur:** `class`, `class`.
- **Isi Modul:**
  - 6 Tier kelangkaan: `COMMON` (Putih/Abu), `UNCOMMON` (Hijau), `RARE` (Biru), `EPIC` (Ungu), `LEGENDARY` (Emas), `MYTHIC` (Merah).
- **Cara Kerja:**
  - Menentukan warna nama item, glowing border di GUI, bobot roll modifier, dan peluang drop di dungeon.
- **Implementasi Fabric 26.2:** `net/takasha/rpg/mmo/MMOItemTier.java`.

#### 15. `features/types`
- **Domain Referensi:** `Standar Arsitektur Sistem RPG`
- **Komponen Arsitektur:** `class`, `class`.
- **Isi Modul:**
  - Daftar tipe item: `SWORD`, `DAGGER`, `AXE`, `HAMMER`, `STAFF`, `BOW`, `CROSSBOW`, `TOOL`, `ARMOR`, `CONSUMABLE`, `GEM_STONE`, `ACCESSORY`, `CATALYST`, `MISCELLANEOUS`.
- **Cara Kerja:**
  - Setiap tipe memiliki ikon representatif vanilla, pembatasan slot equip (cth. Armor di slot armor, Tool di main-hand), dan daftar stat yang valid untuk diedit di GUI `ItemEdition`.
- **Implementasi Fabric 26.2:** `net/takasha/rpg/menu/MMOTypeBrowserMenu.java`.

#### 16. `features/item-lore/name-placeholders`
- **Domain Referensi:** `Standar Arsitektur Sistem RPG`
- **Komponen Arsitektur:** `class`.
- **Isi Modul:**
  - Tag placeholder nama: `%tier%`, `%tier-color%`, `%level%`, `%modifier%`, `%upgrade%`.
- **Cara Kerja:**
  - Dynamic name parser yang merakit nama tampilan akhir item berdasarkan data live. Contoh: `%tier-color%%modifier%Batu Penggali %upgrade%` menjadi `§6Tajam Batu Penggali +3`.
- **Implementasi Fabric 26.2:** `net/takasha/rpg/mmo/MMOItemTemplate.java` (metode `renderDisplayName()`).

#### 17. `features/item-lore/lore-format`
- **Domain Referensi:** `Standar Arsitektur Sistem RPG`
- **Komponen Arsitektur:** `default/language/lore-format.yml`, `class`.
- **Isi Modul:**
  - Struktur urutan baris lore:
    - `#item-type#`
    - `{bar}` (garis strip abu-abu pembatas)
    - `#required-class#`, `#required-level#`, `#required-skill#`
    - `{bar}`
    - `#attack-damage#`, `#attack-speed#`, `#critical-strike-chance#`, `#critical-strike-power#`
    - `#pickaxe-power#`, `#autosmelt#`, `#bouncing-crack#`
    - `#durability#`
    - Footer kelangkaan `#tier#`.
- **Cara Kerja:**
  - Token `{bar}` bersifat cerdas: jika tidak ada data stat di blok bawahnya, bar strip otomatis disembunyikan agar lore tidak memiliki garis berurutan yang kosong.
- **Implementasi Fabric 26.2:** `net/takasha/rpg/mmo/MMOItemTemplate.java` (metode `buildLoreLines()`).

#### 18. `features/item-lore/tooltips`
- **Domain Referensi:** `Standar Arsitektur Sistem RPG`
- **Komponen Arsitektur:** `class`.
- **Isi Modul:**
  - Penjelasan tekstual interaktif pemicu aksi: `§e[Right Click] to cast Ability`, `§e[Sneak + Right Click] to Dash`, durabilitas custom bar.
- **Cara Kerja:**
  - Menampilkan instruksi penggunaan item secara ramah pemain langsung di bawah nama ability atau mekanika aktif.
- **Implementasi Fabric 26.2:** Tooltip builder di `net/takasha/rpg/mmo/MMOItemTemplate.java`.

---

### Cluster 4: Mekanika Lanjutan, Blok & Peningkatan (Modul 19 - 27)

#### 19. `features/skills`
- **Domain Referensi:** `Standar Arsitektur Sistem RPG`
- **Komponen Arsitektur:** `class`, `class`.
- **Isi Modul:**
  - Konfigurasi ability: Nama ability, Trigger Mode (`ON_HIT`, `RIGHT_CLICK`, `SHIFT_RIGHT_CLICK`, `BLOCK_BREAK`), Cooldown (detik), Mana/Stamina Cost.
- **Cara Kerja:**
  - Saat pemain melakukan aksi pemicu dengan memegang item, sistem memeriksa cooldown; jika cooldown siap, memutar partikel/suara dan mengeksekusi logika skill tempur.
- **Implementasi Fabric 26.2:** `net/takasha/rpg/mmo/MMOAbilityTrigger.java` dan `MMOInteractionHandler.java`.

#### 20. `features/custom-recipes`
- **Domain Referensi:** `Standar Arsitektur Sistem RPG`
- **Komponen Arsitektur:** `*` (`RMG_Shaped`, `RMG_Shapeless`, `RMG_Smithing`, `RecipeListGUI`).
- **Isi Modul:**
  - Resep crafting shaped, shapeless, smithing, dan furnace yang menggunakan item RPG Items sebagai bahan input atau output.
- **Cara Kerja:**
  - Ditransformasikan ke dalam JSON data recipe Fabric 26.2 di `data/takasha_rpg/recipe/` yang memvalidasi NBT bahan saat diletakkan di crafting table vanilla.
- **Implementasi Fabric 26.2:** `net/takasha/rpg/menu/MMOEnchantEditionMenu.java` dan JSON Recipe Generator.

#### 21. `features/lootsplosion`
- **Domain Referensi:** `Standar Arsitektur Sistem RPG`
- **Komponen Arsitektur:** `class`.
- **Isi Modul:**
  - Efek ledakan drop item dengan vektor kecepatan acak (`randomVector()`), semburan kembang api berwarna sesuai tier item (`LootColor`), dan suara dentuman dramatis.
- **Cara Kerja:**
  - Dipicu saat monster bos dungeon mati. Seluruh drop item tidak jatuh menumpuk di 1 titik, melainkan terlontar melayang ke udara secara menyebar melingkar.
- **Implementasi Fabric 26.2:** `net/takasha/rpg/world/event/RpgEventInstance.java`.

#### 22. `features/world-gen-templates`
- **Domain Referensi:** `Standar Arsitektur Sistem RPG`
- **Komponen Arsitektur:** `class`.
- **Isi Modul:**
  - Aturan generasi mineral kustom: dimensi, biome target, ketinggian Y (min/max), ukuran urat (vein size), dan peluang spawn per-chunk.
- **Cara Kerja:**
  - Menginjeksi fitur placed feature Fabric ke generator dunia sehingga ore RPG kustom muncul di gua bawah tanah dan dimensi rift dungeon.
- **Implementasi Fabric 26.2:** `net/takasha/rpg/world/TakashaMobLevelManager.java`.

#### 23. `features/custom-blocks`
- **Domain Referensi:** `Standar Arsitektur Sistem RPG`
- **Komponen Arsitektur:** `class`, `class`.
- **Isi Modul:**
  - ID blok custom (1..160), tingkat kekerasan (hardness), resistance, dan tabel drop unik saat dihancurkan dengan alat bertier sesuai.
- **Cara Kerja:**
  - Memanfaatkan state blok jamur bertekstur (MushroomBlock / NoteBlock) atau register block Fabric asli.
- **Implementasi Fabric 26.2:** Registri blok di `net/takasha/rpg/TakashaRpgMod.java`.

#### 24. `features/upgrading`
- **Domain Referensi:** `Standar Arsitektur Sistem RPG`
- **Komponen Arsitektur:** `class`, `class`.
- **Isi Modul:**
  - Konfigurasi upgrade: Batas level upgrade (cth. max: 5), peluang sukses (`success: 0.8`), penalti gagal (turun level atau hancur), dan batu penempaan (*Upgrade Stone*).
- **Cara Kerja:**
  - Setiap kenaikan level (+1, +2, dst.) mengalikan stat dasar (attack damage, speed, defense) dengan rasio peningkatan bertingkat.
- **Implementasi Fabric 26.2:** GUI upgrade di `net/takasha/rpg/menu/MMOItemEditionMenu.java`.

#### 25. `features/identification`
- **Domain Referensi:** `Standar Arsitektur Sistem RPG`
- **Komponen Arsitektur:** `class`.
- **Isi Modul:**
  - Status item `unidentified: true`, nama bertanda tanya `&c??? Unidentified Weapon ???`, dan item habis pakai `Scroll of Identification`.
- **Cara Kerja:**
  - Stat asli disembunyikan sampai pemain mengklik kanan gulungan identifikasi ke senjata tersebut, memicu roll stat acak dan membuka nama aslinya.
- **Implementasi Fabric 26.2:** Handler klik item di `net/takasha/rpg/mmo/MMOInteractionHandler.java`.

#### 26. `features/soulbound`
- **Domain Referensi:** `Standar Arsitektur Sistem RPG`
- **Komponen Arsitektur:** `class`, `class`.
- **Isi Modul:**
  - NBT tag `RPG_SOULBOUND` (berisi UUID pemain) dan level pengikatan jiwa (Level 1 s/d 3).
- **Cara Kerja:**
  - Mencegah pemain lain mengambil, memakai, atau mencuri item tersebut. Jika pemain mati, item tetap berada di inventori pemain tanpa jatuh ke tanah.
- **Implementasi Fabric 26.2:** Event hook di `TakashaEnchantUtilityEngine.java`.

#### 27. `features/sets`
- **Domain Referensi:** `Standar Arsitektur Sistem RPG`
- **Komponen Arsitektur:** `class`, `class`.
- **Isi Modul:**
  - Definisi set bonus perlengkapan (misal: Set *Sakura Assassin* memberikan +10% Speed pada 2 potong, +25% Crit Damage pada 4 potong).
- **Cara Kerja:**
  - Memantau armor dan senjata yang sedang dikenakan pemain secara berkala (`tick()`), menghitung jumlah potongan set yang aktif, dan memberikan status effect potion atau attribute modifier secara dinamis.
- **Implementasi Fabric 26.2:** `net/takasha/rpg/mmo/MMOItemData.java`.

---

### Cluster 5: Stasiun Perajin & Kompatibilitas Eksternal (Modul 28 - 35)

#### 28. `stations/intro`
- **Domain Referensi:** `Standar Arsitektur Sistem RPG`
- **Komponen Arsitektur:** `class`.
- **Isi Modul:**
  - Stasiun kerja khusus dengan ID stasiun (misal `blacksmith`, `alchemist`, `arcane_forge`), kapasitas antrean crafting (`maxQueueSize`), dan antarmuka visual GUI.
- **Cara Kerja:**
  - Dibuka oleh admin via `/mi stations open <station>` atau dengan mengklik blok stasiun di dunia game.
- **Implementasi Fabric 26.2:** `net/takasha/rpg/menu/MMOTypeBrowserMenu.java`.

#### 29. `stations/crafting-recipes`
- **Domain Referensi:** `Standar Arsitektur Sistem RPG`
- **Komponen Arsitektur:** `class`.
- **Isi Modul:**
  - Resep khusus stasiun kerja yang mendefinisikan durasi pembuatan (detik) dan output item.
- **Cara Kerja:**
  - Pemain memasukkan bahan, menekan tombol craft, dan menunggu progress bar waktu crafting selesai sebelum mengambil hasil barang.
- **Implementasi Fabric 26.2:** `net/takasha/rpg/data/TakashaRpgManager.java`.

#### 30. `stations/upgrading-recipes`
- **Domain Referensi:** `Standar Arsitektur Sistem RPG`
- **Komponen Arsitektur:** `class`.
- **Isi Modul:**
  - Resep peningkatan tier senjata menggunakan material langka hasil dungeon sebagai katalis.
- **Cara Kerja:**
  - Memverifikasi apakah item target memenuhi syarat tier dasar sebelum mengonsumsi bahan dan menaikkan tier item.
- **Implementasi Fabric 26.2:** `net/takasha/rpg/menu/MMOItemEditionMenu.java`.

#### 31. `stations/ingredients`
- **Domain Referensi:** `Standar Arsitektur Sistem RPG`
- **Komponen Arsitektur:** `class`, `VanillaIngredient.class`.
- **Isi Modul:**
  - Abstraksi bahan baku crafting: Mendukung item Minecraft vanilla maupun item RPG Items spesifik (dengan verifikasi Type dan ID).
- **Cara Kerja:**
  - Memindai inventori pemain untuk memastikan jumlah item dan tipe NBT cocok persis sebelum mengizinkan proses crafting dimulai.
- **Implementasi Fabric 26.2:** `net/takasha/rpg/mmo/MMOItemManager.java`.

#### 32. `stations/triggers`
- **Domain Referensi:** `Standar Arsitektur Sistem RPG`
- **Komponen Arsitektur:** `class`, `SoundTrigger.class`, `ParticleTrigger.class`.
- **Isi Modul:**
  - Aksi yang dieksekusi otomatis ketika resep crafting berhasil diselesaikan (cth. memutar suara lonceng anvil, efek partikel percikan api, dan siaran pengumuman server).
- **Cara Kerja:**
  - Server memicu paket partikel dan suara ke pemain saat item diambil dari slot output stasiun.
- **Implementasi Fabric 26.2:** `net/takasha/rpg/world/event/RpgEventInstance.java`.

#### 33. `stations/conditions`
- **Domain Referensi:** `Standar Arsitektur Sistem RPG`
- **Komponen Arsitektur:** `class`, `ClassCondition.class`, `PermCondition.class`.
- **Isi Modul:**
  - Batasan prasyarat pemain: Level minimum, profesi AuraSkills (misal harus Blacksmithing Lv 20), atau permission node khusus.
- **Cara Kerja:**
  - GUI menandai resep dengan warna merah jika pemain belum memenuhi syarat, dan menolak aksi klik.
- **Implementasi Fabric 26.2:** `net/takasha/rpg/skills/AuraSkillsEngine.java`.

#### 34. `compatibility/mythic`
- **Domain Referensi:** `Standar Arsitektur Sistem RPG`
- **Komponen Arsitektur:** `*`.
- **Isi Modul:**
  - Drop table monster bos MythicMobs yang menjatuhkan item RPG Items dengan persentase drop khusus dan pemanggilan skill proyektil.
- **Cara Kerja:**
  - Menghubungkan entity kill event bos dungeon dengan generator loot akumulatif.
- **Implementasi Fabric 26.2:** `net/takasha/rpg/dungeon/reward/DungeonRewardAccumulator.java`.

#### 35. `compatibility/enchant-plugins`
- **Domain Referensi:** `Standar Arsitektur Sistem RPG`
- **Komponen Arsitektur:** `*`.
- **Isi Modul:**
  - Dukungan penuh integrasi custom enchantments dari AdvancedEnchantments (AE).
- **Cara Kerja:**
  - Membaca NBT enchantment kustom, menampilkannya pada lore sesuai tier warna AE, dan mengizinkan drag-and-drop buku enchantment kustom langsung ke atas senjata/perkakas RPG Items di inventori pemain.
- **Implementasi Fabric 26.2:** `net/takasha/rpg/enchant/TakashaEnchantUtilityEngine.java`.

---

## 3. Resolusi Definitif Masalah Deteksi `/drop` Mod Takasha (`sakura-weapons`)

### Akar Masalah:
Pada mod klien `sakura-weapons` (cabang `26.2` dan `1.21.11`), kelas `TakashaAutoDropHandler.java` memiliki fitur gatekeeper perintah `/drop` untuk server KaizenMC. Jika mode diatur ke `KAIZEN`, mod secara otomatis mengirim perintah `/drop` ke server. 

Masalahnya:
1. **Tidak Ada Saklar Konfigurasi:** Pemain tidak memiliki cara di config untuk mematikan perilaku pengiriman perintah `/drop`.
2. **Tombol GUI yang Tidak Jelas:** Tombol di `TakashaAutoDropSubScreen` berlabel `"Uji /drop Server"`, yang hanya melakukan tes ping perintah tanpa memberikan opsi ON/OFF.
3. **Spam & Kebingungan di Server Biasa:** Jika pemain bermain di server lain atau server lokal yang tidak memiliki plugin `/drop`, mod memicu kebingungan karena status drop tidak pernah aktif atau mengirim pesan yang tidak dikenali server.

### Solusi yang Diimplementasikan:
1. **Konfigurasi Eksplisit (`TakashaMasterConfig.java`):**
   ```java
   // Toggle kontrol penuh untuk deteksi /drop gatekeeper server Kaizen
   public boolean serverDropGatekeeperEnabled = false; // Default FALSE agar aman untuk semua jenis server
   ```
2. **Tombol Interaktif di GUI (`TakashaAutoDropSubScreen.java`):**
   - Mengubah tombol menjadi:
     `Gatekeeper /drop Server: [AKTIF / NONAKTIF]`
   - Menambahkan tooltip transparan:
     `"Ketika NONAKTIF, mod sama sekali tidak akan mendeteksi atau mengirimkan perintah /drop ke server. Item akan dibuang secara lokal tanpa verifikasi server."`
3. **Bypass Logika Server (`TakashaAutoDropHandler.java`):**
   ```java
   // Jika gatekeeper dinonaktifkan oleh pengguna, lewati seluruh pengecekan server
   if (config.serverDropGatekeeperEnabled && isKaizen) {
       if (serverDropStatus != ServerDropStatus.ACTIVE && serverDropStatus != ServerDropStatus.UNSUPPORTED) {
           ensureServerDropActive(client);
           return;
       }
   }
   ```
4. **Dukungan Multi-Versi:**
   - Diterapkan secara identik pada Minecraft 26.2 (`versions/26.2`) dan Minecraft 1.21.11 (`versions/1.21.11`).

---

## 4. Matriks Rilis & SemVer

| Komponen Mod | Target Platform | Versi Saat Ini | Versi Rilis Baru | File Output Biner |
| :--- | :--- | :--- | :--- | :--- |
| **Root Workspace** | Multi-Platform | `2.9.0` | **`2.9.1`** | `VERSION` |
| **`sakura-weapons/versions/26.2`** | Minecraft 26.2 (Unobfuscated Mojang) | `2.8.14` | **`2.8.15+26.2`** | `Hasil mod/Takasha-2.8.15+26.2.jar` |
| **`sakura-weapons/versions/1.21.11`** | Minecraft 1.21.11 (Intermediary Loom) | `2.6.1` | **`2.6.2+1.21.11`** | `Hasil mod/Takasha-2.6.2+1.21.11.jar` |
| **`takasha-rpg`** | Minecraft 26.2 (Fabric RPG) | `1.9.0` | **`1.9.1+26.2`** | `Hasil mod/Takasha-RPG-1.9.1+26.2.jar` |

---

## 5. Rencana Eksekusi Bertahap (Phased Execution)

1. **Phase 1: Resolusi /drop Mod Takasha (`sakura-weapons`)**
   - Perbarui `TakashaMasterConfig.java` (26.2 & 1.21.11).
   - Perbarui `TakashaAutoDropSubScreen.java` (26.2 & 1.21.11).
   - Modifikasi `TakashaAutoDropHandler.java` untuk bypass penuh.
   - Kompilasi biner `sakura-weapons`.
2. **Phase 2: Command Parity & Chat Edition Engine (`takasha-rpg`)**
   - Implementasikan command tree `/mi` presisi di `MMOCommand.java`.
   - Implementasikan `MMOChatEditionEngine.java` dengan event listener chat server.
3. **Phase 3: GUI Suite 54-Slot presisi Parity (`takasha-rpg`)**
   - Rekonstruksi presisi `MMOTypeBrowserMenu.java`, `MMOItemBrowserMenu.java`, dan `MMOItemEditionMenu.java`.
4. **Phase 4: Sub-Edition Menus & Ekosistem Lanjutan (`takasha-rpg`)**
   - Implementasikan `MMOEnchantEditionMenu.java`, `MMOAbilityEditionMenu.java`, dan NBT `RPG_REVISION_ID`.
5. **Phase 5: Verifikasi Akhir & Build Biner**
   - Jalankan kompilasi bersih `./gradlew build --no-daemon` pada kedua sub-proyek.
   - Verifikasi artefak JAR di folder `Hasil mod/`.

---

## 6. Rencana Lanjutan: presisi Paritas Dinamis RPG Items (Showcase Video & Bytecode) & Optimasi Antigravity CPU

### 6.1 Analisis Komparasi Mod vs Plugin RPG Items (Showcase `pzbUG-imguc` & Bytecode `standar RPG`)
- **Daftar Stat Dinamis per Tipe (`Type.getAvailableStats()`):**
  - Pada `ItemEdition`, stat yang muncul pada grid 21 slot disaring sesuai tipe (`SWORD`, `TOOL`, `ARMOR`, `CONSUMABLE`), bukan stat statis campuran.
  - Paginasi Slot 27 & 35 aktif dinamis saat jumlah stat melebihi 21.
- **Visual & Lore Tombol Stat presisi:**
  - Menampilkan `Current Value: <value>` atau `No value set.`
  - `➔ Left click to change.` / `➔ Right click to reset.`
- **Persistensi File Disk YAML (`config/takasha/rpg_items/item/<TYPE>.yml`):**
  - Pembuatan item `/mi create`, edit item di GUI, duplicate `/mi copy`, dan delete `/mi delete` langsung tersinkronisasi ke file YAML di disk secara real-time.

### 6.2 Solusi Definitif CPU 100% Antigravity IDE
- Menerapkan `files.watcherExclude`, `search.exclude`, dan `java.import.exclusions` pada `.vscode/settings.json` untuk mengecualikan `referensi-rp/**`, `Hasil mod/**`, `.gradle/**`, dan `build/**`.
- Menyetel memory cap JVM JDT-LS ke `-Xmx768M -XX:MaxMetaspaceSize=256M -XX:+UseSerialGC` dan mematikan autobuild latar belakang.

### 6.3 Roadmap Pelaksanaan Bertahap
- **Phase 1:** Optimasi `.vscode/settings.json` & pembersihan proses zombie Java.
- **Phase 2:** Implementasi `MMOItemDiskManager.java` (YAML serializer/deserializer disk).
- **Phase 3:** Rekonstruksi dinamis `MMOItemEditionMenu.java` berbasis kategori tipe dan paginasi Slot 27 & 35.
- **Phase 4:** Penyempurnaan `MMOItemBrowserMenu.java` (Dynamic title `Item Explorer`/`Delete Mode` dan delete disk sync).
- **Phase 5:** Bumping versi ke `v2.9.2` (Root), `takasha-rpg` ke `v1.9.2+26.2`, update `PRD.md` Bab 28 & `CHANGELOG.md` Entri 47, serta build JAR final.

---

## 7. Milestone v1.9.3: Paritas Faktual presisi GUI RPG Items (Audit 186 Stat Bytecode), Porting 216 Item Bawaan (34 File YAML), dan Matriks Pengujian Stat 7-Layer

### 7.1 Hasil Audit Forensik Audit Forensik Link & Sumber Resmi
1. **Verifikasi 35 Tautan Arsitektur RPG:** Seluruh 35 modul tetap utuh dan terverifikasi terhadap kelas bytecode `paket referensi biner`.
2. **Eliminasi Anomali Gambar 1:**
   - **Packet Judul Layar:** Memperbaiki pengiriman `ClientboundOpenScreenPacket` baru agar judul GUI tidak tertinggal pada teks menu sebelumnya (`Type Browser` -> `Item Explorer: <TYPE>` -> `Item Edition: <ITEM_ID>`).
   - **Border Kaca Abu-Abu:** Menghapus loop pengisian paksa 54-slot. Seluruh slot non-grid murni kosong (`ItemStack.EMPTY`), placeholder kaca `§c- No Item Stat -` HANYA berada pada slot kosong di dalam 21 grid stat (`[19..25, 28..34, 37..43]`).
   - **Tombol Kontrol Item Explorer:** Slot 47 `CAULDRON` (`Delete Item`), Slot 49 `ARROW` (`Back`), Slot 51 `WRITABLE_BOOK` (`Create New`), Slot 45 `HOPPER` (Download Resourcepack khusus block).
3. **Katalog 186 Stat Bytecode (`ItemStats.class`):** Pemetaan lengkap 186 stat dengan ikon material vanilla asli Spigot ke dalam `MMOStatRegistry.java`.
4. **Porting 216 Item Bawaan (34 File YAML):** Ekstraksi utuh 34 file konfigurasi `default/item/*.yml` ke `takasha/rpg_packs/default_items/item/` dan pendaftaran auto-load di `TakashaPackManager.java`.
5. **Matriks Pengujian Stat 7-Layer:**
   - Layer 1: Papan Diagnostik Karakter (`/mi stats`)
   - Layer 2: Combat Training Dummy (`DamageDummyEntity` + Action Bar)
   - Layer 3: Pengujian Pertahanan (PoE Mitigation & Fall/Fire Damage)
   - Layer 4: Pengujian Utilitas Penambangan (`AUTOSMELT` & `BOUNCING_CRACK`)
   - Layer 5: Pengujian Interaksi Drag-and-Drop (`SCROLL_OF_IDENTIFICATION`, `ITEM_CRUSHER`, Socketing `GEM_STONE`)
   - Layer 6: Pengujian Set Bonus Armor (Threshold 4/4 Dragon, Steel, Spellcaster)
   - Layer 7: Automated Unit Tests (`gradlew test`)

### 7.2 Status Sinkronisasi Dokumen & Versi
- **`VERSION`:** Ditingkatkan ke `2.9.3`.
- **`takasha-rpg/build.gradle`:** `globalModVersion` ditingkatkan ke `1.9.3`.
- **`PRD.md`:** Ditingkatkan ke versi `4.12.0` dengan penambahan Bab 29.
- **`CHANGELOG.md`:** Ditambahkan entri 48 untuk rilis v1.9.3+26.2 / v2.9.3.

