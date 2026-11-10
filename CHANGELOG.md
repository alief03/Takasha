# CHANGELOG — Takasha Dual-Mod Ecosystem

> Arsip Riwayat Pembaruan, Patch Notes, Post-Mortem Debugging, dan Evolusi Versi
> Mod Utama: **Takasha Core** (sakura-weapons) & **Takasha-RPG Singleworld Engine** (	akasha-rpg)

---

## 1. Riwayat Pembaruan Takasha Core (sakura-weapons)

18. **Onboarding Set #3 Valentines Animated Weapon, Tool & Armor Set (v1.6.0):**
    - **Namespace & Aset:** Diadopsi namespace `valentine` untuk 28 model 3D Blockbench (17 senjata/tools, 5 kosmetik/utilitas; item `chest` ditiadakan sesuai instruksi), 16 tekstur resolusi tinggi, 8 file animasi `.mcmeta`, 4 ikon zirah 2D, dan 2 layer zirah entitas (`humanoid` & `humanoid_leggings`). Total model item JSON: 32 model.
    - **Standar Pewarnaan & Simbol Identitas Tiap Set:** Diterapkan format penamaan terstandarisasi dengan kode warna dan simbol khas:
      - Set Sakura: `§d§l🌸 Sakura <Item> 🌸` (Pink Bunga Sakura).
      - Set Pink Legacy: `§d§l✨ Pink Legacy <Item> ✨` (Neon Pink Aura Sparkles).
      - Set Valentine: `§c§l❤ Valentine <Item> ❤` (Crimson / Valentine Red Heart).
    - **Penyesuaian Trigger Valentine Staff:** Item dasar `blaze_rod` dihilangkan, digantikan dengan seluruh 7 tier tombak vanilla (`wooden_spear` s.d. `netherite_spear`), seluruh 7 tier kapak vanilla (`wooden_axe` s.d. `netherite_axe`), seluruh tier pedang, `stick`, dan `paper`.
    - **Mesin Status Crossbow & Ranged:** Diterapkan predikat modern Minecraft 26.2 pada `assets/minecraft/items/crossbow.json` yang secara mulus menangani 5 tahapan transisi (`crossbow_0`, `crossbow_1`, `crossbow_2`, `crossbow_2_charged`, `crossbow_charged`) serta `bow.json` untuk 3 tahap penarikan (`bow_0`, `bow_1`, `bow_2`).
    - **Isolasi Topi 3D vs Helm 2D:** Model `helmet.json` dari sumber mentah diidentifikasi sebagai topi/kacamata 3D bertingkah laku `hat: true` dan dialihkan ke `valentine:item/hat`. Ikon helm zirah 2D dibuat dari `helmet_icon.png` (`valentine:item/helmet`). Pada tubuh pemain, saat `Valentine Hat` dikenakan, tekstur kubus helm vanilla ditekan dan model 3D dirender via `SakuraHatFeatureRenderer` pada slot `HEAD`.
    - **Equipment Asset & Layering Zirah:** Didaftarkan `ModEquipmentAssets.VALENTINE` (`valentine:valentine`) dan diintegrasikan ke dalam `EquipmentLayerRendererMixin` untuk zirah tubuh pemain.
    - **Ekspansi Tab Anvil GUI 4 Kategori:** `AnvilSideListWidget` diperluas dari 3 tab menjadi 4 tab (`[All]`, `[Sakura]`, `[Pink]`, `[Valentine]`) dengan lebar tombol responsif agar tidak bertumpuk atau terpotong scissor viewport. Seluruh 21 item Valentine didaftarkan ke `AnvilItemCatalog` dengan filtering cerdas anti-tabrakan substring.
    - **Kepatuhan Kebijakan Deployment & Cakupan Mod-Only:** Sesuai arahan pengguna ("hanya buat MOD nya saja, resources pack tidak perlu"), seluruh aset set Valentine dibundel langsung ke dalam Mod Fabric (`sakura-weapons/src/main/resources/assets/`). Output build dikompilasi secara eksklusif ke `Hasil mod/Takasha-1.6.0.jar` di workspace lokal tanpa pembuatan ZIP resource pack dan tanpa penyalinan ke profil launcher Modrinth eksternal atau deployment remote.
19. **Valentine Wing Singular Keyword Alignment & Quiver Deprecation (v1.6.0 Patch):**
    - **Akar Masalah Valentine Wing:** Di `elytra.json`, kondisi `when` pada versi awal hanya mendaftarkan kata kunci bentuk jamak (`Valentine Wings`, `Sayap Valentine`), sedangkan pada `AnvilItemCatalog` dan antarmuka Anvil item didaftarkan dalam bentuk tunggal (`Valentine Wing`). Ketika pemain memasukkan Elytra dan memilih / mengetik nama `"Valentine Wing"` atau `§c§l❤ Valentine Wing ❤`, model deserializer vanilla 26.2 tidak menemukan pencocokan dan jatuh kembali ke model/tekstur Elytra bawaan.
    - **Resolusi Model Valentine Wing:** Didaftarkan 48 variasi kata kunci lengkap (bentuk tunggal `Valentine Wing` dan jamak `Valentine Wings`, kode warna berdekorasi simbol `§c§l❤ Valentine Wing ❤`, format alternatif `&c`, huruf kecil/besar, dan bilingual `Sayap Valentine`) pada `elytra.json` dan `paper.json` dengan 0 entri duplikat. Blok case sayap yang tidak semestinya pada 7 file definisi zirah dada (`chestplate.json`) dibersihkan total.
    - **Penghapusan Total Quiver (Tempat Anak Panah):** Sesuai instruksi pengguna ("untuk tempat anak panah / quiver hapus saja"):
      - Menghapus file definisi item `valentine_quiver.json` dan model 3D `assets/valentine/models/item/quiver.json` (yang sebelumnya memiliki referensi tekstur `#missing`).
      - Menghapus seluruh blok `when` case quiver dari `bow.json`, `crossbow.json`, `leather_chestplate.json`, dan `paper.json`.
      - Menghapus registrasi `VALENTINE_QUIVER` dari `ValentineItems.java`, creative tab `ModItemGroups.java`, dan katalog Anvil `AnvilItemCatalog.java`.
      - Menghapus key terjemahan dari `en_us.json` dan `id_id.json`.
      - Total item aktif set Valentine disesuaikan menjadi tepat 20 item (5 Senjata Melee/Magic, 3 Tools/Perkakas, 4 Ranged/Pertahanan, 4 Kosmetik/Utilitas [Hat, Wing, Key, Grenade], 4 Armor).
    - **Verifikasi & Build Kompilasi:** Script validasi otomatis `scripts/validate_items.py` mengonfirmasi 313 file JSON valid, 0 duplikasi predikat case, 0 referensi tersisa ke quiver. Mod Fabric berhasil dikompilasi ke `Hasil mod/Takasha-1.6.0.jar`.
20. **Minecraft 1.21.11 & OpenJDK 21 LTS Compatibility Alignment (v1.6.1):**
    - **Akar Masalah:**
      - Mod `Takasha-1.6.0.jar` mengalami kegagalan peluncuran (dependency resolution error) pada Minecraft Java 1.21.11 dengan pesan: `Mod 'Takasha' (sakura_weapons) 1.6.0 requires any version between 26.2 (inclusive) and 26.3- (exclusive) of 'Minecraft' (minecraft), but only the wrong version is present: 1.21.11!` dan `Mod 'Takasha' requires version 25 or later of 'OpenJDK 64-Bit Server VM' (java), but only the wrong version is present: 21!`.
    - **Prinsip SemVer 2.0.0 (PATCH Release):**
      - Karena modifikasi ini memperbaiki kompatibilitas runtime dan dependensi lingkungan tanpa menambah item baru maupun memecah backward compatibility (0 breaking changes, seluruh 59 native items dari 3 set tetap utuh), versi dirilis sebagai **PATCH release: v1.6.1**.
    - **Penyelarasan Toolchain & Dependensi (Loom Remap & Intermediary):**
      - `fabric.mod.json`: Dependensi Minecraft diperluas menjadi `"minecraft": ">=1.21.11- <=26.2"`, dan Java diturunkan menjadi `"java": ">=21"`.
      - `build.gradle`: Menggunakan plugin `net.fabricmc.fabric-loom-remap` versi `1.17.21` (plugin khusus untuk versi Minecraft terobfuscasi seperti 1.21.11). Menambahkan `mappings loom.officialMojangMappings()` dan mengubah dependensi menjadi `modImplementation`. Menargetkan Java 21 (`options.release = 21`, `sourceCompatibility = JavaVersion.VERSION_21`, `targetCompatibility = JavaVersion.VERSION_21`).
      - `gradle.properties`: Mengonfigurasi `minecraft_version=1.21.11` dan `fabric_version=0.141.6+1.21.11` dengan JVM Java 21 (`java-runtime-delta`).
    - **Refaktorisasi & Adaptasi API Kompilasi (14 Compile Errors):**
      - `AnvilSideListWidget.java`: Mengalihkan rendering dari `extractWidgetRenderState(GuiGraphicsExtractor, ...)` ke `renderWidget(GuiGraphics, ...)`. Menggunakan API `GuiGraphics` resmi Minecraft 1.21.11 (`fill`, `drawString`, `renderFakeItem`, `enableScissor`, `disableScissor`, `setComponentTooltipForNextFrame`).
      - `ModItemGroups.java`: Mengalihkan paket dari `net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab` ke `net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup`.
      - `SakuraWeaponsClient.java`: Mengalihkan registrasi layer dari `LivingEntityRenderLayerRegistrationCallback` ke `LivingEntityFeatureRendererRegistrationCallback`. Memperbaiki enum entitas ke `EntityType.PLAYER` dan `EntityType.ARMOR_STAND`. Memperbaiki registrasi widget GUI layar ke `Screens.getButtons(screen).add(sideList)`.
    - **Resolusi Runtime Crash Intermediary (v1.6.1 Final Re-map):**
      - Mengatasi `NoClassDefFoundError: net/minecraft/core/Registry` dan missing mixin targets (`AnvilMenu`, `EquipmentLayerRenderer`) akibat penggunaan mode unobfuscated di Loom.
      - Dengan `net.fabricmc.fabric-loom-remap` dan task `:remapJar`, seluruh class dan mixin (`AnvilMenuMixin`, `EquipmentLayerRendererMixin`) berhasil diremap secara sempurna ke namespace intermediary (`net.minecraft.class_2378`, `class_1706`, `class_10197`, dll).
    - **Verifikasi Build:**
      - File JAR remapped berhasil dikompilasi secara bersih ke `Hasil mod/Takasha-1.6.1.jar` (1.000.253 bytes).
      - Verifikasi `javap -c` & `javap -v` mengonfirmasi bytecode major version `65` (Java 21) dan seluruh referensi class/method telah ter-remap ke namespace `intermediary`.
      - Verifikasi isi `fabric.mod.json` memastikan versi `1.6.1` dengan dependensi `minecraft >=1.21.11- <=26.2` dan `java >=21`.
21. **Armor Texture Invisibility Fix & Multi-Set Comprehensive Audit (v1.6.2 PATCH):**
    - **Akar Masalah Tekstur Zirah Tidak Muncul (Invisible Armor Bug):**
      - Pada Minecraft Java 1.21.11, log runtime `latest.log` mencatat: `Couldn't parse data file 'pink_legacy:pink_legacy' from 'pink_legacy:equipment/pink_legacy.json': DataResult.Error['Unknown element name:humanoid_baby missed input...']`.
      - Enum `EquipmentClientInfo$LayerType` di Minecraft 1.21.11 tidak mengenal `humanoid_baby`. Keberadaan key ini pada file definisi equipment JSON menyebabkan Mojang Codec menolak mem-parsing seluruh file aset, dan `EquipmentAssetManager` mengembalikan `MISSING` (empty layers). Akibatnya `EquipmentLayerRenderer.renderLayers(...)` langsung membatalkan render (`if (layers.isEmpty()) return;`) sehingga zirah yang dikenakan pemain tidak terlihat sama sekali.
    - **Resolusi Definisi Equipment JSON & Sanitasi BOM:**
      - Menghapus blok `"humanoid_baby"` dari `assets/pink_legacy/equipment/pink_legacy.json`, `assets/valentine/equipment/valentine.json`, serta pada resource pack.
      - Membersihkan header UTF-8 BOM (`\xef\xbb\xbf`) dari 19 file model JSON Pink Legacy di mod dan 19 file di resource pack.
    - **Penguatan Jaring Pengaman Java (`EquipmentLayerRendererMixin`):**
      - Menginisialisasi programmatic fallback statis `PINK_LEGACY_INFO` dan `VALENTINE_INFO` pada `EquipmentLayerRendererMixin.java`.
      - Jika pemanggilan `manager.get(...)` mengembalikan map kosong/MISSING saat merender zirah Pink Legacy atau Valentine (baik native item maupun vanilla armor hasil rename di Anvil), mixin secara otomatis mengembalikan layer fallback terprogram.
    - **Audit Menyeluruh 59 Item & Modernisasi Data Tags 1.21.11:**
      - Seluruh 59 item aktif dari 3 set (Sakura: 20 item, Pink Legacy: 19 item, Valentine: 20 item) diaudit dan diverifikasi 100% valid tanpa satupun aset atau terjemahan yang hilang.
      - Menyesuaikan sistem data tags ke standar singular 1.21+ (`data/minecraft/tags/item/`) dan tetap menyinkronkan `tags/items/`.
      - Melengkapi seluruh tag alat dan senjata (`swords.json`, `axes.json`, `pickaxes.json`, `shovels.json`, `hoes.json`) dengan item-item baru yang sebelumnya tertinggal.
      - Menambahkan tag zirah standar (`head_armor.json`, `chest_armor.json`, `leg_armor.json`, `foot_armor.json`).
    - **Kepatuhan Kebijakan Deployment & SemVer (PRD Bagian 8 & 10):**
      - Diklasifikasikan sebagai **PATCH release (v1.6.2)** sesuai SemVer 2.0.0 (0 breaking change, 59 item dipertahankan utuh).
      - Output kompilasi disimpan secara eksklusif di workspace lokal: `Hasil mod/Takasha-1.6.2.jar` tanpa otomatisasi penyalinan ke launcher pengguna.
22. **Armor Texture Standardization, Layer Separation & UV Defect Elimination (v1.6.3 PATCH):**
    - **Akar Masalah Cacat Visual & Tumpang Tindih Layer (Layer Overlap & Texture Bleed):**
      - **Tumpang Tindih Kaki/Paha (Layer 1 Boots vs Layer 2 Leggings):** Pada set Valentine, file `humanoid/valentine.png` (Layer 1, dilatasi 1.0F) memuat 384 piksel paha (baris Y=32..51). Saat pemain mengenakan sepatu boots, Layer 1 membungkus paha dengan dilatasi 1.0F, menimpa dan menutupi celana zirah Layer 2 (dilatasi 0.5F), menimbulkan tabrakan z-fighting parah.
      - **Kebocoran Torso Atas pada Celana Zirah (Leggings Upper Torso Bleed):** Pada set Pink Legacy, file `humanoid_leggings/pink_legacy.png` memuat 512 piksel torso atas (baris Y=32..47) dan 128 piksel pundak/bahu (`[40, 32, 56, 40]`), serta 262 piksel pada Valentine. Celana merender "baju zirah kembar" di dalam chestplate asli.
      - **Kebocoran Telapak Kaki pada Celana Zirah (Leggings Feet Bleed):** File celana memuat 96 piksel pada baris Y=56..58 (Pink Legacy) dan 256 piksel (Valentine) yang menembus telapak sepatu boots.
      - **Celah Rambut Hitam Bolong pada Helm (Helmet Rear Void Gap):** Pada `humanoid/valentine.png`, sisi belakang helm (`[48, 16, 64, 32]`) memiliki baris Y=27..31 transparan penuh (0 piksel), menciptakan celah bolong horizontal selebar 5 piksel yang memperlihatkan rambut hitam pemain secara terpotong dari belakang.
    - **Implementasi Standarisasi Tekstur (PRD Bagian 2.4):**
      - Membersihkan 100% piksel pada Zona Terlarang Paha (`[0, 32, 32, 52]`) pada file Layer 1.
      - Membersihkan 100% piksel pada Zona Terlarang Torso Atas (`[32, 32, 80, 48]`) pada file Layer 2.
      - Membersihkan 100% piksel pada Zona Terlarang Telapak Kaki (`[0, 58, 32, 64]`) pada file Layer 2.
      - Menambal baris Y=27..29 pada sisi belakang helm Valentine dengan palet warna dan shading serasi agar menutup rapi hingga tengkuk kepala pemain.
    - **Skrip Audit Otomatis CI:** Dibuat skrip `scripts/validate_armor_layers.py` untuk mengunci dan memvalidasi aturan matriks UV ini secara berkelanjutan.
23. **Armor Size Elimination & Zero-Overlap Layer Matrix (v1.6.4 PATCH):**
    - **Akar Masalah Zirah Terlalu Besar & Saling Menimpa:**
      - **Hat Layer Bloat (Dilatasi 1.5F):** Tekstur Layer 1 memuat 66 piksel (Valentine) dan 232 piksel (Pink Legacy) pada kotak Hat Layer (`[64, 0, 128, 32]`). Dalam model Mojang `HumanoidModel`, Hat Layer diekspansi dengan `CubeDeformation(1.5F)` (kubus 11x11x11 blok), menciptakan cangkang raksasa yang melayang tebal di luar kepala pemain.
      - **Tumpang Tindih Torso Bawah (Dual Concentric Mesh Clipping):** Baju zirah Layer 1 (`1.0F`) memuat plat abu-abu 6-pack pada baris `Y=54..61`, sedangkan celana zirah Layer 2 (`0.5F`) memuat sabuk dan suspender pink pada baris `Y=48..63`. Kedua kubus konsentris berbenturan pada 14 baris piksel vertikal sehingga saling menembus (*clipping*).
      - **Tumpang Tindih Lutut (Knee Cuff Overlap):** Sepatu Layer 1 memiliki manset solid di `Y=52..53` (`1.0F`), sedangkan celana Layer 2 memiliki kain di `Y=52..55` (`0.5F`), menyebabkan cincin luar sepatu membungkus lutut celana.
      - **Visor Menutupi Mata Karakter:** Pelindung dahi depan pada helm Layer 1 menutupi baris `Y=22..24` (`X: 18..30`), menenggelamkan garis mata karakter pemain di bawah plat logam.
    - **Implementasi Matriks Nol-Tumpang-Tindih Matematis (Zero-Overlap Matrix):**
      - **Pembersihan Mutlak Hat Layer:** Menghapus 100% piksel pada `[64, 0, 128, 32]` sehingga helm hanya dirender pada model kubus dasar kepala (`CubeDeformation 1.0F`).
      - **Partisi Tegas Torso & Pelvis:** Layer 1 eksklusif menangani torso atas (`Y=32..53`). Seluruh piksel di `Y=54..63` dihapus dari Layer 1. Layer 2 eksklusif menangani pelvis dan sabuk (`Y=54..63`).
      - **Partisi Tegas Kaki & Sepatu:** Layer 2 eksklusif menangani paha dan lutut (`Y=32..53`). Layer 1 eksklusif menangani betis bawah, tumit, dan sol sepatu (`Y=54..63`).
      - **Visor Eye Clearance (Lengkungan Alis Dahi):** Menaikkan pelindung dahi 1 baris piksel (`X: 18..30, Y: 22..24` transparan), membuka garis mata karakter pemain dengan jelas seperti estetika helm vanilla, sembari menjaga lambang hati dahi (`Y=16..21`) tetap utuh.
    - **Sinkronisasi Menyeluruh & CI Validation:**
      - Diproses dan disinkronkan ke seluruh 8 file tekstur (mod `sakura_weapons`, `pink_legacy`, `valentine`, serta `sakura-resourcepack`).
      - Lolos uji verifikasi CI `scripts/validate_armor_layers.py` dengan toleransi nol (8/8 file lulus 100%).
    - **Kepatuhan Kebijakan SemVer & Deployment:**
      - Diklasifikasikan sebagai **PATCH release (v1.6.4)** sesuai SemVer 2.0.0 (0 breaking change, 59 item tetap utuh).
      - Output kompilasi eksklusif pada workspace lokal: `Hasil mod/Takasha-1.6.4.jar` tanpa otomatisasi instalasi ke launcher pengguna.
24. **Dukungan Platform Minecraft 26.2 & OpenJDK 25 LTS (v1.7.0+26.2 MINOR):**
    - **Adopsi Toolchain & Runtime:**
      - Upgrade Fabric Loom ke versi `1.17.21`.
      - Penargetan Minecraft `26.2` dan Fabric API `0.161.0+26.2`.
      - Konfigurasi `JavaVersion.VERSION_25` dan `options.release = 25` menggunakan OpenJDK 25 LTS (`java-runtime-epsilon`).
    - **Penyelarasan API & Rendering Fabric 26.2:**
      - Mengadaptasi `extractWidgetRenderState(GuiGraphicsExtractor, int, int, float)` untuk `AnvilSideListWidget` menggantikan `renderWidget(GuiGraphics, ...)`.
      - Menggunakan `FabricCreativeModeTab.builder()` dari package `net.fabricmc.fabric.api.creativetab.v1`.
      - Menggunakan `LivingEntityRenderLayerRegistrationCallback` dan entitas `EntityTypes` pada `SakuraWeaponsClient`.
      - Menggunakan `Screens.getWidgets` untuk integrasi scrolling dan widget pada `AnvilScreen`.
    - **Kepatuhan Penamaan & Kebijakan Deployment (PRD Bagian 2.5, 8 & 10):**
      - Diklasifikasikan sebagai **MINOR release (v1.7.0)** karena evolusi platform runtime baru (Minecraft 26.2 & Java 25).
      - Menggunakan konvensi penamaan platform tagged: `Takasha-1.7.0+26.2.jar`.
      - Lingkup build mod-only (tanpa Resource Pack zip, tanpa menyentuh `Hasil RP/`, dan tanpa penyalinan ke direktori launcher eksternal).
25. **Item Frame Player-Perspective Orientation & Invisibility Toggle (v1.7.1+26.2 PATCH):**
    - **Orientasi Vertikal Berdasarkan Sudut Pandang Pemain:**
      - Menginjeksi `ItemFrame.interact(Player, InteractionHand)` via `ItemFrameInvisibilityMixin`.
      - Saat item Takasha (Sakura, Pink Legacy, Valentine — pedang, tombak, kapak, beliung, palu, perisai, topi, sayap, zirah, dll.) ditaruh ke dalam Item Frame:
        - **Di Dinding (Wall):** Diatur otomatis ke `rotation = 0` (vertikal tegak lurus, gagang/atas di atas dan bilah/ujung di bawah).
        - **Di Lantai (Floor, `Direction.UP`):** Diatur otomatis mengikuti arah hadap pemain peletak (`player.getDirection().get2DDataValue() * 2`), menjamin gagang berada di atas/menjauh dan bilah berada di bawah/mendekat dari perspektif pemain peletak.
        - **Di Langit-Langit (Ceiling, `Direction.DOWN`):** Diatur otomatis sejajar pandangan pemain saat mendongak ke atas (`(dir * 2 + 4) % 8`).
      - Tetap mendukung rotasi manual vanilla (+45° per klik kanan) jika pemain ingin mengarahkan diagonal.
    - **Toggle Invisibility (Hide / Unhide Item Frame):**
      - Interaksi: **Shift (Crouch) + Klik Kanan dengan Tangan Kosong** pada Item Frame yang sedang memuat item.
      - Men-toggle status `setInvisible(!isInvisible())` secara native di server dan tersinkronisasi ke seluruh client via `SynchedEntityData`.
      - Efek suara rotasi `SoundEvents.ITEM_FRAME_ROTATE_ITEM` dan notifikasi teks ActionBar bilingual (`message.sakura_weapons.item_frame.hidden` / `message.sakura_weapons.item_frame.shown`).
    - **Arsitektur Mixin Rendering Client:**
      - Menambahkan `ItemFrameRenderStateAccess`, `ItemFrameRenderStateMixin`, dan `ItemFrameRendererMixin` untuk melacak `ItemStack` asli pada pipeline rendering client 26.2.
    - **Kepatuhan Penamaan & Kebijakan Deployment (PRD Bagian 2.5, 8 & 10):**
      - Diklasifikasikan sebagai **PATCH release (v1.7.1)** sesuai SemVer 2.0.0.
      - Output JAR: `Hasil mod/Takasha-1.7.1+26.2.jar` (OpenJDK 25 LTS, Major 69.0).
      - Mod-only build tanpa menyentuh Resource Pack atau launcher eksternal.
26. **Custom Humanoid NPC, ReplayMod Zero-Desync, Bendable Cuboids & Emotecraft Integration (v2.0.0+26.2 MAJOR MILESTONE):**
    - **Akar Masalah Hilangnya Emote/Pose di ReplayMod ("Berdiri Biasa saat Edit/Play"):**
      - Pada versi terdahulu, keberadaan string `emoteId` memicu pengabaian rotasi sendi (`if (!hasEmote) { applyLimbRotations(); }`).
      - Di dalam ReplayMod timeline player dan video camera scrubber, ticking entitas vanilla (`entity.tick()`) tidak berjalan secara simultan dan controller animasi pihak ketiga tidak aktif pada entitas mod kustom.
      - Akibatnya, tulang model `TakashaNpcModel` tidak menerima transformasi rotasi dari mana pun dan kembali ke $(0, 0, 0)$ bawaan Minecraft (posisi berdiri tegak kaku biasa).
    - **Protokol Nol-Desinkronisasi ReplayMod (SynchedEntityData Foundation):**
      - Menghapus pembatas `if (!hasEmote)` pada `TakashaNpcModel`.
      - Sudut Euler sendi (`headPose`, `bodyPose`, `rightArmPose`, `leftArmPose`, `rightLegPose`, `leftLegPose`) dari `SynchedEntityData` **selalu diaplikasikan secara mutlak** sebagai fondasi bentuk anatomi.
      - Jika ada animasi runtime (prosedural atau Emotecraft), transformasi tersebut melapisi pose dasar. Saat ReplayMod di-pause, di-scrub maju-mundur, atau di-render ke file video, pose patung tetap berdiri/duduk/rebah dengan kokoh dan akurat.
    - **Katalog 32 Variasi Pose Terkategori & Bed Alignment System:**
      - Menghadirkan 32 preset pose terbagi ke dalam 4 kelompok terstruktur:
        1. *🛌 Tiduran & Rebahan (Presets 1–7):* Tidur Kasur, Tengkurap, Miring Kanan, Miring Kiri, Melamun/Stargazing, Peluk Senjata, Terkapar KO.
        2. *🧘 Duduk & Bersimpuh (Presets 8–14):* Kursi Santai, Duduk Bersandar, Duduk Bersila/Lotus, Meditasi Hening, Jongkok Siaga, Berlutut Ksatria, Bersimpuh Seiza.
        3. *⚔ Kuda-kuda Tempur & Senjata (Presets 15–24):* Siaga Berdiri, Siap Bertarung, Iaido Katana Draw, Senjata Ganda (Dual Wield), Membidik Busur, Kuda-kuda Tombak, Mengangkat Palu Godam, Memasang Perisai, Tusukan Cepat, Tebasan Melompat.
        4. *🎭 Gestur & Emote Statis (Presets 25–32):* Bersedekap Dada, Hormat Ksatria, Membungkuk Sopan, Menunjuk Tegas, Melambai Ramah, Menangis Terisak, Facepalm, Berpikir Keras.
      - Matriks rotasi horizontal $X = \pm 90^\circ, Z = \pm 90^\circ$ diselaraskan dengan `customYaw` hadap entitas pada `TakashaNpcRenderer`, dengan elevasi kasur vanilla ($Y + 0.5625$).
      - Mengaktifkan deformasi artikulasi sendi lutut dan siku (Bendable Cuboids) pada pose duduk, bersila, seiza, dan rebahan kasur.
    - **Jembatan Emotecraft Aman & Mesin Animasi Prosedural Native:**
      - Mengeliminasi `IllegalArgumentException` pada pemanggilan refleksi method Emotecraft.
      - Pemindaian dinamis `EmoteHolder.list` untuk mengambil daftar emote terpasang beserta icon resmi 12x12 piksel (`getIconIdentifier()`).
      - Mesin animasi prosedural native berbasis trigonometri waktu (`wave`, `clap`, `cheer`, `dance`, `salute`) yang beroperasi mulus secara mandiri tanpa ketergantungan mod luar.
    - **Fitur "📌 Bekukan ke Pose" (Bake Emote to Static Pose):**
      - Tombol pada antarmuka GUI yang memungkinkan pemain membekukan frame animasi yang sedang berjalan secara instan menjadi pose statis permanen ke dalam `SynchedEntityData`.
    - **Tabbed GUI Navigation & Inventaris 36-Slot Interaktif:**
      - Antarmuka GUI dilengkapi 5 tab kategori navigasi cepat (`[🛌 Tiduran]`, `[🧘 Duduk]`, `[⚔ Tempur]`, `[🎭 Emote]`, `[🎬 Emotecraft]`).
      - Interaksi transfer perlengkapan zirah dan senjata langsung dari 27 slot tas + 9 slot hotbar pemain.
    - **Kepatuhan Penamaan, SemVer & Deployment (PRD Bagian 2.5, 8 & 10):**
      - Diklasifikasikan sebagai **MAJOR MILESTONE release (v2.0.0)** sesuai SemVer 2.0.0 karena implementasi menyeluruh sistem entitas NPC sinematik, ReplayMod zero-desync protocol, dan arsitektur animasi modular.
      - Target penamaan kompilasi lokal: `Hasil mod/Takasha-2.0.0+26.2.jar` (OpenJDK 25 LTS, bytecode major 69.0).
      - Build mod-only tanpa menyentuh Resource Pack atau folder eksternal.
27. **Perbaikan Desinkronisasi Emote ReplayMod & Kegagalan Seluruh 32 Preset Pose Mannequin (v2.0.1+26.2 PATCH):**
    - **Akar Masalah Kegagalan Seluruh 32 Preset Pose (1..32) & Hilangnya Emote di ReplayMod:**
      - **False-Positive `hasActiveEmoteAnimation`:** Pada `EmotecraftCompat.java:223`, method mengembalikan `return true;` jika objek `manager` PlayerAnimationLib tidak bernilai null, meskipun `isActive()` bernilai `false`. Karena PlayerAnimationLib selalu menempelkan animation manager ke setiap `AvatarRenderState`, status emote dianggap selalu aktif secara keliru (100% false-positive).
      - **Universal Pose Lockout pada Model (Penyebab Seluruh Preset 1–32 Berdiri Tegak Kaku):** Di `TakashaNpcModel.java:195-235`, blok penataan rotasi sendi Euler (`headPose`, `bodyPose`, `rightArmPose`, `leftArmPose`, `rightLegPose`, `leftLegPose`) dan method `applyBendableCuboidsPoses()` dibungkus dalam `if (!emoteActive && !proceduralHandled)`. Akibat false-positive di atas, **SELURUH 32 PRESET POSE (Preset 1 s/d 32)** langsung dilewati (skipped) 100%! Akibatnya, ketika pemain memilih preset apa pun di GUI Mannequin (mulai dari tidur, duduk, kuda-kuda tempur, hingga gestur), model NPC mengabaikan seluruh sudut pose dan tetap berdiri tegak kaku (default vanilla $(0,0,0)$).
      - **Desinkronisasi ReplayMod Timeline / Edit Screen (Gambar 1 vs Gambar 2):** Di halaman edit/replay viewer ReplayMod, `entity.tick()` tidak berjalan reguler dan false-positive `hasActiveEmoteAnimation` menyebabkan animasi prosedural dilewati, transformasi sendi diblokir, serta rotasi PoseStack horizontal pada `TakashaNpcRenderer.scale()` diabaikan. Kedua NPC pada Gambar 1 terdorong kembali ke posisi berdiri tegak alih-alih mengeksekusi emote dan pose masing-masing seperti pada gameplay normal di Gambar 2.
      - **Penyesuaian Pivot & Bending Kategori Duduk Lantai (Preset 9 "Duduk Sandar"):** Preset 9 sebelumnya dimasukkan ke dalam `isSitting` (kursi, $Y=8.5$). Padahal "Duduk Bersandar" adalah pose bersandar di lantai/tanah. Selain itu, tekukan lutut Bendable Cuboids `1.35f` ($\approx 77^\circ$) menekuk betis ke bawah menembus tanah.
    - **Solusi Arsitektur Baku v2.0.1+26.2:**
      - **Perbaikan Deteksi Emote Akurat (Zero False-Positive):** `EmotecraftCompat.hasActiveEmoteAnimation(state)` diperbaiki secara ketat: hanya mengembalikan `true` apabila method `isActive()` atau `isAnimationActive()` pada animation manager secara eksplisit mengembalikan `Boolean.TRUE`. Menghapus `return true;` prematur dan mengembalikan `false` secara default saat tidak ada animasi yang sedang dimainkan.
      - **Unconditional Baseline Posing untuk Seluruh Preset 1–32:** Hapus gembok `if (!emoteActive && !proceduralHandled)` pada `TakashaNpcModel.java`. Rotasi sendi Euler (`headPose`, `bodyPose`, `leftArmPose`, `rightArmPose`, `leftLegPose`, `rightLegPose`) dari `SynchedEntityData` serta deformasi sendi `applyBendableCuboidsPoses(preset, emoteId)` **SELALU diaplikasikan secara mutlak** untuk setiap preset (1..32).
      - **Sistem Layering Animasi & Emote:** Animasi prosedural native dan animasi Emotecraft diterapkan sebagai *overlay* di atas pose dasar yang sudah terbentuk, bukan mematikan fondasi pose NPC.
      - **Koreksi Pivot & Bending Preset 9:**
        - Pindahkan Preset 9 ke `isFloorSitting` (`head.y = 11.5F, body.y = 11.5F, legs.y = 20.5F, legs.z = -2.5F`), menjamin paha dan pantat menempel presisi di atas blok tanah/rumput.
        - Ubah tekukan lutut Bendable Cuboids untuk Preset 9 menjadi `0.05f` agar kedua kaki berselonjor santai di atas tanah tanpa menembus ke dalam tanah, dan rilekskan tangan menjadi `-0.4f`.
      - **Continuous Replay Timeline Animation:** `npcState.animTime` menginterpolasi waktu dinamis secara kontinu (`System.currentTimeMillis() % 1000000L / 50.0f + partialTick`) saat ReplayMod di-pause atau di-scrub, menjamin animasi emote tetap hidup dan terputar di halaman edit ReplayMod.
      - **Rotasi PoseStack Horizontal Konsisten:** Memastikan preset tidur 1–7 dan emote rebahan/KO pada preset 0 diputar mendatar horizontal ($X=\pm 90^\circ, Z=\pm 90^\circ$) pada `TakashaNpcRenderer.scale()`.
    - **Kepatuhan Penamaan, SemVer & Deployment (PRD Bagian 2.5, 8 & 10):**
      - Diklasifikasikan sebagai **PATCH release (v2.0.1)** sesuai SemVer 2.0.0 karena memulihkan fungsionalitas yang ada (bug fix) tanpa memecah API atau data serialisasi.
      - Target penamaan kompilasi lokal: `Hasil mod/Takasha-2.0.1+26.2.jar` (OpenJDK 25 LTS, bytecode major 69.0).
      - Build mod-only tanpa menyentuh Resource Pack atau folder launcher eksternal.
28. **Artikulasi Layer Luar (Outer-Skin) Pose Preset & Studio Rekonfigurasi Timeline ReplayMod / Flashback (v2.0.2+26.2 PATCH):**
    - **Akar Masalah Kegagalan Pose Preset (Gambar 1 - "Duduk Kursi Tetap Berdiri Kaku"):**
      - **Desinkronisasi Layer Kulit Terluar (*Outer Skin Layers*):** Model humanoid Minecraft memisahkan anggota tubuh menjadi kubus inti (*inner limb*: `head`, `body`, `leftArm`, `rightArm`, `leftLeg`, `rightLeg`) dan lapisan pakaian/jaket terluar (*outer layer*: `hat`, `jacket`, `leftSleeve`, `rightSleeve`, `leftPants`, `rightPants`). Pada implementasi sebelumnya, `TakashaNpcModel` memanggil `resetOuterLayerPose()` yang mengatur seluruh rotasi lapisan luar kembali ke $(0, 0, 0)$ dan tidak menyalin transformasi posisi, rotasi, maupun skala secara komprehensif. Karena skin pemain kustom umumnya meletakkan tekstur pakaian (celana, jaket, rambut 3D) pada layer luar, ketika layer luar tetap berdiri tegak $(0, 0, 0)$ sedangkan kubus dalam tersembunyi di bawahnya, NPC tampak secara visual 100% berdiri tegak kaku tanpa merespons preset (seperti terlihat pada Gambar 1: pemain memilih Preset 8 Duduk Kursi namun model tetap berdiri mematung).
      - **Bypass Restorasi NBT Preset Saat World/Chunk Load:** Pada `TakashaNpcEntity.readAdditionalSaveData()`, pemanggilan `applyPosePreset(preset)` dibatasi oleh `if (!hasSavedHead)`. Ketika chunk atau world disimpan dan dimuat ulang, bendera rotasi kepala yang tersimpan menyebabkan pemanggilan `applyPosePreset()` dilewati, sehingga data rotasi sendi tidak diperbarui secara konsisten.
    - **Solusi Penyelarasan Artikulasi Layer Luar & Restorasi NBT Baku:**
      - **Sinkronisasi Total `copyModelPartPose()`:** Menghapus seluruh panggilan `resetOuterLayerPose()`. Menambahkan utility method `copyModelPartPose(source, target)` yang menyalin posisi (`x, y, z`), sudut rotasi Euler (`xRot, yRot, zRot`), dan skala (`xScale, yScale, zScale`) dari kubus inti ke lapisan luar yang bersesuaian (`head -> hat`, `body -> jacket`, `leftArm -> leftSleeve`, `rightArm -> rightSleeve`, `leftLeg -> leftPants`, `rightLeg -> rightPants`).
      - **Bending Simultan Bendable Cuboids:** Penyelarasan dilakukan sebelum dan sesudah kalkulasi rotasi prosedural / emote, memastikan deformasi tekukan sendi Bendable Cuboids melipat lapisan celana dan jaket secara simultan bersama anggota tubuh inti tanpa tembus atau tertinggal.
      - **Enforcement Mutlak NBT Preset:** Pada `TakashaNpcEntity.readAdditionalSaveData()`, jika tag NBT `PosePreset > 0`, mod secara tanpa syarat (*unconditional*) memanggil `applyPosePreset(preset)` untuk menjamin postur mannequin selalu pulih sempurna setiap kali entitas dimuat ke dalam memori.
    - **Studio Rekonfigurasi Timeline ReplayMod & Flashback (Gambar 2):**
      - **Kebutuhan Pengguna:** Pada proses pembuatan video sinematik atau pengeditan timeline ReplayMod / Flashback, pemain sering kali perlu mengubah emote atau mengganti preset pose NPC langsung dari timeline tanpa harus merekam ulang rekaman gameplay dari awal.
      - **Manajer Override Runtime (`ReplayPoseOverrideManager`):** Sistem *client-side runtime override* berbasis UUID yang memetakan pose preset (1..32) atau emote Emotecraft kustom ke NPC target secara instan. Nilai override disimpan secara otomatis ke disk (`.minecraft/sakura_replay_overrides/active_replay_overrides.json`) sehingga tetap aktif selama sesi pengeditan dan rendering video ReplayMod.
      - **Integrasi Pipeline Rendering Client (`TakashaNpcRenderer`):** Pada method `extractRenderState()`, renderer memeriksa `ReplayPoseOverrideManager.getOverride(entity.getUUID())`. Jika override aktif, parameter `posePreset`, `customEmoteId`, dan `emotePlayMode` ditimpa secara dinamis, memungkinkan perubahan pose dan animasi tampil instan pada timeline ReplayMod saat scrubbing maupun saat pengeksporan video final.
      - **Antarmuka Grafis Sinematik (`TakashaReplayOverrideScreen`):**
        - Pratinjau langsung 3D NPC (*Live 3D Entity Preview*) menggunakan `InventoryScreen.extractEntityInInventoryFollowsMouse`.
        - Pengatur Siklus Preset (1 s/d 32) dengan tombol `[◀]` dan `[▶]` yang menampilkan nama bilingual preset secara realtime.
        - Dialog Modal Pencarian Emotecraft: Daftar emote lengkap dengan kotak input pencarian, ikon emote resmi 12x12 piksel, scrollbar halus, dan seleksi instan.
        - Pengatur Mode Pemutaran Emote: Tombol toggle tiga status (`🔁 Loop`, `⏸ Tahan / Freeze`, `▶ Sekali / Play Once`).
        - Pemilih Multi-NPC: Tombol peralihan antar NPC Takasha yang terdeteksi di sekitar pemain.
        - Tombol Aksi: `[✔ Terapkan ke Replay]` (menerapkan override dan menyimpan ke JSON) serta `[🔄 Reset Rekaman]` (menghapus override dan memulihkan data asli rekaman).
      - **Aksesibilitas & Integrasi Menu Entitas:**
        - **Keybind Klien Global:** Tombol pintas default `P` (Kategori: `key.categories.sakura_weapons`) untuk membuka Studio Override kapan pun pemain berada di dalam replay viewer atau dunia.
        - **Injeksi Menu Entitas Flashback / ReplayMod:** Melalui Fabric `ScreenEvents.AFTER_INIT`, mod mendeteksi terbukanya layar menu entitas Flashback (layar berjudul `Entity: <uuid>`), mengekstrak UUID NPC Takasha yang diklik, dan secara otomatis menyisipkan tombol `[🎭 Atur Emote & Pose Sakura]` di atas tombol navigasi Flashback.
    - **Kepatuhan Penamaan, SemVer & Deployment (PRD Bagian 2.5, 8 & 10):**
      - Diklasifikasikan sebagai **PATCH release (v2.0.2)** sesuai SemVer 2.0.0 karena menyempurnakan perbaikan pose dan menambahkan kompatibilitas ReplayMod tanpa mengubah protokol network atau memecah format serialisasi.
      - Target penamaan kompilasi lokal: `Hasil mod/Takasha-2.0.2+26.2.jar` (OpenJDK 25 LTS, bytecode major 69.0).
      - Build mod-only tanpa menyentuh Resource Pack (`Hasil RP/`) atau folder eksternal.
29. **Integrasi Antarmuka ImGui Flashback pada Popup Entitas (v2.0.3+26.2 PATCH):**
    - **Akar Masalah (Antarmuka Tidak Muncul di Flashback):** Flashback 0.43.4 merender seluruh editornya (timeline, panel Visuals, popup `Entity: <uuid>`) menggunakan Dear ImGui (`imgui.moulberry90.ImGui`) melalui `ReplayUI`, bukan `net.minecraft.client.gui.screens.Screen`. Hook `ScreenEvents.AFTER_INIT` pada v2.0.2 tidak pernah terpanggil sehingga tombol Sakura tidak muncul pada popup entitas.
    - **Solusi:**
      - `SelectedEntityPopupMixin` (`@Inject` pada `TAIL` method `SelectedEntityPopup.render(Entity, EditorState)`) menambahkan bagian `Sakura: Pose & Emote` di bawah `Equipment` / `Model Parts` khusus untuk `TakashaNpcEntity`.
      - `FlashbackSakuraPanel` merender kontrol ImGui: siklus pose preset `<` / `>` (0..32), tombol `Bebas (0)`, status & penghapusan emote, toggle mode (`Loop` / `Tahan` / `Sekali`), `Reset Override`, dan `Buka Studio Sakura (Lengkap)` yang menutup popup lalu membuka `TakashaReplayOverrideScreen` (via `Minecraft.execute`). Setiap perubahan langsung ditulis ke `ReplayPoseOverrideManager` sehingga NPC di viewport berubah seketika.
      - Teks ImGui disanitasi ke ASCII karena font Flashback tidak memuat glyph emoji.
    - **Keamanan Kompatibilitas:** Flashback ditambahkan sebagai `compileOnly` (tidak dibundel). `SakuraMixinPlugin` (`IMixinConfigPlugin`) hanya menerapkan mixin di paket `.flashback.` bila `FabricLoader.isModLoaded("flashback")`; tanpa Flashback mod berjalan normal. Injeksi memakai `require = 0` dan dibungkus try/catch agar tidak merusak UI Flashback.
    - **Kepatuhan SemVer & Deployment:** PATCH release **v2.0.3**; output `Hasil mod/Takasha-2.0.3+26.2.jar` (OpenJDK 25 LTS, major 69.0), mod-only.
30. **Resolusi Fatal Crash 26.2 (NoClassDefFoundError net/minecraft/class_2378), Migrasi Loom Unobfuscated Environment & Rilis Takasha v2.1.0+26.2:**
    - **Akar Masalah Fatal Client Crash (`NoClassDefFoundError: net/minecraft/class_2378`):**
      - Pada Minecraft Java 26.2, Mojang secara resmi menghapus lapisan runtime obfuscation, mendistribusikan kode biner game menggunakan nama kanonis resmi Mojang (*unobfuscated runtime environment*).
      - Fabric Loom 1.17 secara otomatis mendeteksi status ini dan mengaktifkan bendera internal `disableObfuscation = true`. Dalam mode ini, pemanggilan `mappings loom.officialMojangMappings()` memicu exception fatal Gradle `Cannot use Mojang mappings in a non-obfuscated environment`, serta konfigurasi `modImplementation` dan task `remapJar` ditiadakan karena proses remapping bytecode tidak lagi diperlukan.
      - Jika file JAR atau referensi dependensi memuat jejak mapping Fabric Intermediary lama (`net/minecraft/class_*`), JVM saat runtime 26.2 akan melempar fatal exception `java.lang.NoClassDefFoundError: net/minecraft/class_2378 at net.sakura.weapons.SakuraWeaponsMod.onInitialize(SakuraWeaponsMod.java:18)`.
    - **Penyelarasan Build System & Gradle Pipeline (`build.gradle` & `gradle.properties`):**
      - Menghapus deklarasi `mappings` dari `build.gradle` dan mengonfigurasi dependensi menggunakan standard Gradle configurations:
        ```groovy
        dependencies {
            minecraft "com.mojang:minecraft:${project.minecraft_version}"
            implementation "net.fabricmc:fabric-loader:${project.loader_version}"
            implementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_version}"
            compileOnly files("${rootDir}/../referensi-rp/Flashback-0.43.4-for-MC26.2.jar")
        }
        ```
      - Mengatur target rilis Java 25 LTS (`options.release = 25`, `sourceCompatibility = 25`, `targetCompatibility = 25`, bytecode version 69.0).
      - Mengonfigurasi otomatisasi task `build` untuk menyalin file JAR bersih langsung ke direktori distribusi `Hasil mod/Takasha-2.1.0+26.2.jar`.
    - **Refactoring & Modernisasi API Minecraft 26.2 & Fabric API 0.161.0+26.2:**
      1. **Creative Mode Tab API:** Memperbarui `ModItemGroups.java` dari API usang `fabric-item-group-api-v1` (`FabricItemGroup.builder()`) ke API baru 26.2 `fabric-creative-tab-api-v1` (`FabricCreativeModeTab.builder()`), memulihkan registrasi tab kustom Sakura Arsenal tanpa mapping intermediary.
      2. **Perisai (Shield) Data Component:** Mengadaptasi konstruktor komponen pertahanan `SakuraShieldItem.java` ke arsitektur `delayedComponent` 26.2 menggunakan provider registry lookup `BlocksAttacks(..., Optional.of(provider.getOrThrow(DamageTypeTags.BYPASSES_SHIELD)), ...)`.
      3. **Key Mapping API:** Memigrasikan `SakuraWeaponsClient.java` dari paket usang `net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper` ke `net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(...)`, serta kategori tombol ke Java Record `KeyMapping.Category.MISC`.
      4. **Konstanta Tipe Entitas:** Menyelaraskan referensi entitas dari `EntityType.PLAYER` dan `EntityType.ARMOR_STAND` ke registry holder resmi Mojang 26.2 `EntityTypes.PLAYER` dan `EntityTypes.ARMOR_STAND`.
      5. **Screen Widgets & UI Navigation:** Mengganti method `Screens.getButtons(...)` ke `Screens.getWidgets(...)` pada injeksi GUI Anvil, serta mengalihkan navigasi pembukaan layar client ke `client.gui.setScreen(...)`.
      6. **Layer & Feature Renderer Registration:** Mengganti callback `LivingEntityFeatureRendererRegistrationCallback` ke callback resmi 26.2 `LivingEntityRenderLayerRegistrationCallback.EVENT.register(...)` dengan pendaftaran layer via `helper.register(...)`.
      7. **Modernisasi GUI Rendering Engine (`GuiGraphicsExtractor`):** Merombak total `AnvilSideListWidget.java` dari model lama `renderWidget(GuiGraphics, ...)` menjadi `extractWidgetRenderState(GuiGraphicsExtractor, ...)`. Seluruh operasi rendering GUI (border, container background, teks tab, clipping scissor, icon item `fakeItem()`, dan tooltip `setComponentTooltipForNextFrame()`) diselaraskan 100% ke arsitektur rendering terkini Minecraft 26.2.
      8. **Eliminasi Kode Usang:** Menghapus file `SakuraNetworking.java` yang usang dan redundant, menyatukan seluruh penanganan network packet modern ke `ModNetworkMessages.java` (`PayloadTypeRegistry.serverboundPlay()` & `clientboundPlay()`).
    - **Verifikasi Kualitas Bytecode & Validasi Nol-Intermediary:**
      - Kompilasi JDK 25 LTS sukses penuh (`BUILD SUCCESSFUL in 27s` / `build 32s`).
      - Analisis `javap -v` mengonfirmasi class format versi `69.0` (Java 25).
      - Pemindaian biner menyeluruh pada seluruh class di dalam file JAR memastikan **0 referensi** terhadap `class_` atau `net/minecraft/class_*` (100% Mojang official unobfuscated names).
    - **Kepatuhan SemVer & Deployment (PRD Bagian 2.5, 8 & 10):**
      - Diklasifikasikan sebagai **MINOR release (v2.1.0)** sesuai SemVer 2.0.0 karena memadukan 4 set lengkap senjata (Sakura, Pink Legacy, Valentine, Dragon Mecha Overlord), sistem mannequin NPC modern, dukungan Flashback ReplayMod, dan kompatibilitas penuh Minecraft 26.2.
      - Output kompilasi final tersimpan di: `Hasil mod/Takasha-2.1.0+26.2.jar` (1.76 MB).
      - Menjaga keutuhan Resource Pack (`Hasil RP/`) tanpa perubahan apa pun.
31. **Overhaul Fisika Dorongan & Tabrakan NPC Manekin (Takasha Mannequin Push Physics Overhaul) & Rilis Takasha v2.1.1+26.2 PATCH:**
    - **Akar Masalah Fisika Dorongan Tidak Berfungsi di 26.2:**
      1. **Bypass Eksekusi `travel()` Sisi Server (`isEffectiveAi() == false`):**
         - Pada `LivingEntity.aiStep()`, Minecraft memverifikasi `if (this.canSimulateMovement() && this.isEffectiveAi()) { this.travel(travelVector); }`.
         - Karena `TakashaNpcEntity` sebelumnya mengembalikan nilai keras `false` pada `isEffectiveAi()`, method `travel()` sama sekali tidak pernah dipanggil di server.
         - Akibatnya, impuls kecepatan horizontal (`deltaMovement`) yang ditambahkan saat pemain menyentuh atau menabrak NPC tidak pernah diproses menjadi perubahan posisi nyata `(X, Y, Z)` via `this.move(MoverType.SELF, this.getDeltaMovement())`. Entitas tetap terpaku di tempat.
      2. **Hilangnya Override `canBeCollidedWith(Entity)`:**
         - Logika tabrakan `Player.canCollideWith(npc)` memanggil `npc.canBeCollidedWith(player)`.
         - Default vanilla `Entity.canBeCollidedWith()` adalah `false`. Tanpa meng-override method ini, kalkulasi raycast dan move clipping pemain memperlakukan manekin seperti udara kosong sehingga pemain dapat menembus manekin.
      3. **Pipeline Dorongan Standar Terputus (`push(Entity)` & `doPush(Entity)`):**
         - Minecraft menjalankan tabrakan entitas terutama via siklus `pushEntities() -> doPush(other) -> other.push(this)`.
         - Mod sebelumnya hanya menyediakan `playerTouch(Player)`, yang tidak terpicu secara andal oleh siklus dorongan entitas vanilla dan tidak mengaitkan perizinan `NpcPushPermission` serta pengganda kekuatan `DATA_PUSH_STRENGTH`.
      4. **Nilai Awal Terkunci & NPC Tanpa Pemilik:**
         - NPC baru di-spawn dengan `DATA_PUSHABLE = false` dan `DATA_PUSH_PERMISSION = 0` (`OWNER_ONLY`).
         - Saat di-spawn via wand/telur tanpa pemilik langsung (`ownerUUID.isEmpty()`), evaluasi `npc.isOwner(player)` bernilai `false` bagi seluruh pemain reguler non-OP, mengunci manekin secara permanen dari dorongan.
      5. **Gravitasi Terkunci (`noGravity = true`):**
         - Gravitasi tidak ditolak secara dinamis saat mode dorongan aktif, menyebabkan manekin melayang dan tidak mengalami friksi lantai normal jika didorong.
    - **Solusi Komprehensif & Implementasi Kode:**
      1. **Sinkronisasi AI Simulasi Gerakan (`TakashaNpcEntity.java`):**
         - Meng-override `isEffectiveAi()` untuk mengembalikan `isPushableMode() && super.isEffectiveAi()`.
         - Saat mode dorongan aktif (`pushable == true`), server mengeksekusi `travel(travelVector)` -> `travelInAir()` yang memanggil `move(MoverType.SELF, deltaMovement)`, memperhitungkan friksi blok lantai, serta penurunan kecepatan bertahap (*smooth deceleration*).
         - Saat mode dorongan nonaktif (`pushable == false`), `isEffectiveAi()` bernilai `false`, menjamin 0 pemborosan CPU server dan posisi manekin terkunci kokoh (*rock-solid immobility*).
      2. **Bounding Box Collision Solid:**
         - Meng-override `canBeCollidedWith(Entity other)` mengembalikan `this.isPushable()`, memungkinkan pemain menabrak dan berinteraksi secara fisik dengan bounding box NPC.
      3. **Pipeline Dorongan Terintegrasi Izin & Kekuatan:**
         - Meng-override `push(Entity entity)`: Menghitung vektor tolakan horizontal `(dx, dz)` yang dinormalisasi, dikalikan dengan faktor kekuatan slider GUI `getPushStrength()` (`DATA_PUSH_STRENGTH`), dan menerapkan `this.hurtMarked = true` agar paket pembaruan kecepatan langsung dikirim ke client tracker.
         - Meng-override `doPush(Entity other)` dan `pushEntities()`: Menjaga konsistensi dorongan bilateral antar entitas.
         - Meng-override `push(double x, double y, double z)`: Memastikan impuls eksternal hanya diterima jika `isPushable()` bernilai `true`.
      4. **Manajemen Dinamis Gravitasi & Nilai Default Ramah Pengguna:**
         - Mengubah default nilai awal di `defineSynchedData()` dan `readAdditionalSaveData()`: `DATA_PUSHABLE = true` (Bisa Didorong secara *out-of-the-box*), `DATA_PUSH_PERMISSION = 2` (`EVERYONE`), dan `DATA_PUSH_STRENGTH = 0.35f`.
         - Pada `setPushableMode(boolean pushable)` dan `onSyncedDataUpdated()`: Saat `pushable == true`, `setNoGravity(false)` diterapkan agar NPC tunduk pada hukum gravitasi dan friksi tanah. Saat `pushable == false`, `setNoGravity(true)` dan `setDeltaMovement(Vec3.ZERO)` diterapkan seketika.
         - Pada `NpcPushPermission.java`: Menambahkan pengecekan `if (npc.getOwnerUUID().isEmpty()) return true;`, memastikan NPC manekin yang belum diklaim dapat didorong bebas oleh siapa saja.
         - Pada `TakashaNpcScreen.java`: Menyelaraskan field default GUI `pushable = true` dan `pushPermission = EVERYONE`.
      5. **Reposisi Presisi Origin (`snapTo`):**
         - Memperbarui `resetToOriginPosition()` menggunakan `this.snapTo(this.originX, this.originY, this.originZ, this.originYaw, 0.0f)` dan `setDeltaMovement(Vec3.ZERO)` untuk pemulihan koordinat origin instan tanpa *lerp jitter*.
    - **Kepatuhan SemVer & Deployment (PRD Bagian 2.5, 8 & 10):**
      - Diklasifikasikan sebagai **PATCH release (v2.1.1)** sesuai SemVer 2.0.0 karena menyempurnakan dan menuntaskan perbaikan bug fungsionalitas fisika dorongan NPC yang telah ada tanpa mengubah protokol network atau memecah format serialisasi.
      - Output penamaan kompilasi lokal: `Hasil mod/Takasha-2.1.1+26.2.jar` (OpenJDK 25 LTS, bytecode major 69.0).
      - Build mod-only tanpa menyentuh Resource Pack (`Hasil RP/`).
32. **Standarisasi Arsitektur Pemisahan Source Code Multi-Platform (Minecraft 1.21.11 & 26.2) (v2.2.0 ARSITEKTUR):**
    - **Latar Belakang & Urgensi Pemisahan Source Code:**
      - Munculnya kebutuhan untuk merilis Takasha secara simultan pada dua ekosistem yang terpisah jauh: cabang LTS Minecraft 1.21.11 (ekosistem modding yang matang dan stabil) serta cabang produksi modern Minecraft 26.2 (fitur Mojang kanonis terbaru).
      - Menggabungkan kode kedua versi ke dalam mono-source tree tunggal terbukti mustahil dan berbahaya karena benturan inkompatibilitas fatal pada level compiler, bytecode JVM, dan Minecraft/Fabric API:
        1. **Benturan Bytecode JVM:** Minecraft 1.21.11 mewajibkan Java 21 LTS (`java-runtime-delta`, bytecode versi 65.0), sedangkan Minecraft 26.2 mewajibkan Java 25 LTS (`java-runtime-epsilon`, bytecode versi 69.0). Executable Java 21 akan langsung crash dengan `UnsupportedClassVersionError: 69.0` jika memuat class Java 25.
        2. **Benturan Loom Toolchain & Obfuscation:** Fabric Loom untuk 1.21.11 menggunakan mode obfuscation dengan Fabric Intermediary mappings (`net.fabricmc.fabric-loom-remap` / `mappings loom.officialMojangMappings()`), sedangkan Fabric Loom untuk 26.2 beroperasi dalam lingkungan unobfuscated Mojang official names murni (pemanggilan `loom.officialMojangMappings()` di 26.2 memicu fatal build error: `Cannot use Mojang mappings in a non-obfuscated environment`).
        3. **Divergensi API Signifikan:**
           - Nama kelas inti Mojang: `net.minecraft.resources.ResourceLocation` (1.21.11) vs `net.minecraft.resources.Identifier` (26.2).
           - Pipeline rendering GUI: `renderWidget(GuiGraphics, int, int, float)` (1.21.11) vs `extractWidgetRenderState(GuiGraphicsExtractor, ...)` (26.2).
           - Registrasi Creative Tab: `FabricItemGroup.builder()` (1.21.11) vs `FabricCreativeModeTab.builder()` (26.2).
           - Pendaftaran Render Layer: `LivingEntityFeatureRendererRegistrationCallback` (1.21.11) vs `LivingEntityRenderLayerRegistrationCallback` (26.2).
           - Entitas & Holders: `EntityType<T>` (1.21.11) vs `EntityTypes<T>` / `Holder<EntityType<?>>` (26.2).
    - **Strategi & Standarisasi Desain Arsitektur Baru:**
      - **Isolasi Penuh Source Code Java:**
        - Subproject Minecraft 1.21.11 diisolasi ke direktori `versions/1.21.11/` (`sakura-weapons-1.21.11`), dikompilasi dengan toolchain JDK 21 LTS dan target bytecode 65.0.
        - Subproject Minecraft 26.2 diisolasi ke direktori `versions/26.2/` (`sakura-weapons-26.2`), dikompilasi dengan toolchain JDK 25 LTS dan target bytecode 69.0.
      - **Shared Assets (Single Source of Truth):**
        - Seluruh aset Blockbench JSON (`models/item/`), tekstur PNG (`textures/item/`), definisi suara (`sounds.json`), file terjemahan (`lang/en_us.json`), dan metadata resource pack (`pack.mcmeta`) dipusatkan di shared directory (`shared-resources/` atau task Gradle `processResources`) sehingga 0 duplikasi biner aset terjadi antara kedua versi.
      - **Pipeline Kompilasi Mandiri & Paralel:**
        - `./gradlew :versions:1.21.11:build` menghasilkan `Hasil mod/Takasha-{VERSION}+1.21.11.jar`.
        - `./gradlew :versions:26.2:build` menghasilkan `Hasil mod/Takasha-{VERSION}+26.2.jar`.
        - `./gradlew buildAll` membangun kedua target secara bersamaan.
    - **Kepatuhan SemVer & Integritas Dokumen:**
      - Diklasifikasikan sebagai **MINOR release (v2.2.0 ARSITEKTUR)** sesuai SemVer 2.0.0 karena memperkenalkan perubahan arsitektur multi-platform struktural yang substansial dan membuka dukungan platform baru (Minecraft 1.21.11) secara formal di samping versi 26.2.
      - Seluruh bagian PRD (Bagian 1, 2.5, 5.2, 7, 8, 9, 10, 11, dan 12) telah diselaraskan secara komprehensif untuk menegakkan pemisahan source code, penamaan artefak paralel (`Takasha-{VERSION}+{MC_VERSION}.jar`), dan integritas deployment lokal.
33. **Penegakan Otomatis Semantic Versioning, Pemangkasan 4 Pose Bawaan NPC, Integrasi Logo & Deduplikasi Emotecraft GUI, Standarisasi Texture Set Loader ala ItemsAdder 4.0.18 Singleworld, dan Penyelarasan Armor Set Master Bahan Set/ untuk Minecraft 26.2 (v2.3.0+26.2 MINOR):**
    - **Akar Masalah SemVer Toolchain & Solusi Single Source of Truth:**
      - Subproyek `sakura-weapons/versions/26.2/build.gradle` sebelumnya tidak membaca file root `VERSION`, sehingga perubahan versi di `VERSION` tidak berpengaruh ke kompilasi subproyek ("semantic version tidak berjalan").
      - Mengonfigurasi pembacaan file `VERSION` secara otomatis di Gradle: `def versionFile = file("${rootDir}/../VERSION"); def globalModVersion = versionFile.exists() ? versionFile.text.trim() : project.findProperty("mod_version")`.
      - Menaikkan versi resmi dari `2.2.0` ke **`2.3.0`** (bukan patch `2.2.1` karena mencakup perombakan besar sistem pose, standarisasi loader texture set ala ItemsAdder 4.0.18, penambahan materi zirah modern, dan pemulihan aset master) sesuai kaidah SemVer 2.0.0.
      - Menghasilkan biner rilis tunggal: `Hasil mod/Takasha-2.3.0+26.2.jar`.
    - **Pemangkasan 28 Pose Warisan & Pembatasan 4 Pose Fundamental Kanonis:**
      - Memangkas tuntas 28 pose bawaan statis yang berlebihan (7 varian rebahan, 7 pose duduk/seiza/meditasi, 10 pose tempur, 8 pose gestur).
      - Menyederhanakan katalog preset NPC menjadi hanya 4 pose fundamental kanonis:
        - `0: Berdiri Biasa (Default Stand)` — Postur tegak natural bebas kaku.
        - `1: Duduk (Sitting)` — Duduk bersandar di kursi ($Y=8.5$) atau lantai ($Y=11.5$).
        - `2: Tidur (Lying / Sleep)` — Rebahan kasur horizontal ($X = \pm 90^\circ, Z = \pm 90^\circ, Y+0.5625$).
        - `3: Jongkok (Crouching / Sneaking)` — Membungkuk siaga / jongkok natural.
      - Membersihkan metode usang pada `TakashaNpcModel.java` (`applyCombatArmPose`, `applyGestureArmPose`, dll.) dan menyelaraskan selector GUI di `TakashaNpcScreen` dengan fallback aman `preset = 0` untuk nilai lama $> 3$.
    - **Penyempurnaan Integrasi Emotecraft GUI (Anti-Duplikasi List & Logo Emote):**
      - Menghilangkan duplikasi nama emote pada dropdown menu GUI dengan menyematkan filter `Set<String> seenKeys` pada method `addEmoteDedup` di `EmotecraftCompat.java`.
      - Mengintegrasikan pemuatan dan rendering icon logo resmi Emotecraft (`SAKURA_EMOTE_LOGO`) pada header dan item dropdown menu GUI.
    - **Standarisasi Texture Set Loader ala ItemsAdder 4.0.18 (Singleworld Autonomy):**
      - Mengadopsi arsitektur pemuatan texture set dan model dari ItemsAdder 4.0.18 (`referensi-rp/ItemsAdder_4.0.18`).
      - Menjamin seluruh tekstur zirah, model 3D, dan animasi berjalan 100% otonom di mode Singleworld tanpa memerlukan server Spigot/Paper atau plugin Bukkit ItemsAdder eksternal.
      - Menghapus pemblokiran buta terhadap `CustomModelData` (`if (stack.has(DataComponents.CUSTOM_MODEL_DATA)) return false;`) pada `DragonMechaOverlordArmorUtil`, `PinkLegacyArmorUtil`, dan `ValentineArmorUtil`. Memprioritaskan native mod items di baris pertama, sehingga pemain di Singleworld yang menamai item via Anvil maupun pemain di server dengan CMD dapat merender zirah secara presisi tanpa terblokir.
    - **Penyelarasan Seluruh Armor Set Master Bahan Set/ (Khusus 26.2):**
      - **Valentine:** Merestorasi tekstur `humanoid/valentine.png` dari master `Bahan Set/` (sepatu pink `#F7A2D5` utuh, mengeliminasi 576 pixel transparan/bolong di $Y=40$).
      - **Pink Legacy:** Merestorasi tekstur Layer 1 & 2 dari master `Bahan Set/` (mengeliminasi 64 px dan 560 px yang terpotong).
      - **Dragon Mecha Overlord:** Menyinkronkan menyeluruh folder aset `assets/dragon_mecha_overlord` ke `sakura-resourcepack/` (25 model item 3D, zirah layer 1 & 2, tekstur, icon).
      - **Klarifikasi Sakura Set:** Mengonfirmasi bahwa Set #1 Sakura murni hanya memiliki topi kosmetik (`sakura_hat`) dan sayap (`sakura_wings`), tidak memiliki zirah tubuh.
      - **Modern Armor Materials:** Menerapkan `SakuraArmorMaterials.java` (Netherite-boosted untuk Dragon Mecha Overlord: armor 20, toughness 3.0, knockback resistance 0.1; Diamond-tier untuk Pink Legacy & Valentine: armor 20, toughness 2.0).
      - Mendaftarkan komponen zirah `humanoidArmor()` pada `DragonMechaOverlordItems.java`, `ValentineItems.java`, dan `ModItems.java`.
    - **Kepatuhan Penamaan, SemVer & Deployment (PRD Bagian 2.5, 8 & 10):**
      - Diklasifikasikan sebagai **MINOR release (v2.3.0)** sesuai SemVer 2.0.0.
      - Target kompilasi tunggal: `Hasil mod/Takasha-2.3.0+26.2.jar` (OpenJDK 25 LTS, bytecode major 69.0).
      - Mod-only build tanpa menyentuh cabang 1.21.11, tanpa menyentuh Resource Pack zip, dan tanpa instalasi otomatis ke launcher eksternal.
34. **Auto-SellGUI Farming Automation, Pengaturan Bentuk Laporan Fortune Panen di Mod Menu, Sistem Auto Fishing & Auto Kill Mob (Porting Kaizen + Konfigurasi Vanilla), Sistem Auto Bow (Auto Aim + Free Movement), Solusi Anti-Freeze Dropdown Emote (3.027 File Profile Emotes), dan Adopsi ItemsAdder untuk Minecraft 26.2 (v2.4.0+26.2 MINOR):**
    - **Otomatisasi Penjualan Panen Auto-SellGUI (Client-Side Farming Automation Murni):**
      - **Pemicu (Farming Trigger):** Mendeteksi ketika pemain memanen tanaman matang (`CropBlock` dengan `isMaxAge`, `StemGrownBlock` seperti Melon/Labu, `NetherWartBlock` age 3, `CocoaBlock` age 2, `SweetBerryBushBlock` age >= 2, `SugarCaneBlock`, `CactusBlock`, `BambooBlock`, `KelpBlock`).
      - **Pengecekan Kepenuhan (Fullness Guard):** Memeriksa 36 slot inventaris utama. Jika tidak ada slot kosong sama sekali atau slot yang ada tidak dapat menampung tanaman panen, status pemicu menjadi aktif.
      - **Eksekusi Perintah `/sellgui`:** Mengirim command `/sellgui` melalui `client.player.connection.sendCommand("sellgui")` dengan perlindungan cooldown anti-spam (default 100 ticks / 5 detik).
      - **Otomatisasi Transfer Slot Aman Anti-Cheat:** Mendeteksi pembukaan `AbstractContainerScreen` bertajuk SellGUI, memindahkan seluruh tumpukan hasil panen dari inventaris pemain ke kontainer secara bertahap (1-2 tick per stack via `handleInventoryMouseClick` Quick Move / Shift-Click) untuk mencegah deteksi anticheat (GrimAC, NoCheatPlus, Matrix).
      - **Perlindungan Item Berharga:** Mengisolasi pemindahan hanya pada whitelist hasil panen (Wheat, Carrot, Potato, Beetroot, Nether Wart, Cocoa, Berries, Melons, Pumpkins, Cane, Cactus). Tidak pernah memindahkan senjata, armor, cangkul (hoe), bibit, atau item inventaris lainnya.
      - **Auto-Close:** Menutup kontainer otomatis (`player.closeContainer()`) setelah seluruh hasil panen masuk. *Bebas dari laporan penjualan keuangan.*
      - **Kontrol Pengguna:** Keybind toggle `F6` (kategori Takasha), perintah `/takasha autosell <toggle|status>`, dan konfigurasi tersimpan di `.minecraft/config/takasha_farming.json`.
    - **Sistem Auto Fishing (Dual-Mode: Server Kaizen & Vanilla System):**
      - **Adopsi Arsitektur dari `D:\laragon\www\autofish-kaizen`:** Membawa teknologi autofish battle-tested ke subproyek Takasha 26.2.
      - **Mode Spesifik Server Kaizen:**
        - **Auto-Capcay 1.0 (Anti-Captcha Chest GUI Solver):** Mendeteksi notifikasi chat server Kaizen yang memicu `/capcay` (regex: `(?i).*(\/capcay|ketik /capcay|capcay).*`), menerapkan penundaan natural manusiawi (1.200 ms), mengeksekusi `/capcay`, memindai GUI chest container, mengklasifikasikan item minoritas via `ItemClassifier`, melakukan klik slot aman dengan humanized jitter, dan otomatis me-resume memancing.
        - **Rod Slip Recovery & Disarm Protection:** Mendeteksi pesan rod slip di server Kaizen, otomatis mengambil kembali atau menukar joran cadangan dari inventaris, dan menyelamatkan hook jika terlempar ke tanah.
        - **Multiplayer Motion Spike Detection:** Mendeteksi gigitan ikan di server multiplayer via deteksi lonjakan vektor gerak vertikal bobber (`FishMonitorMPMotion`).
      - **Mode Sistem Vanilla:**
        - Deteksi gigitan berbasis suara percikan air vanilla (`entity.fishing_bobber.splash`) dan ambang batas getaran bobber tanpa overhead sistem Capcay.
      - **Fitur Auto-Discard & Checklist Item Simpan Hasil Mancing (Catch Filter & Keep Whitelist):**
        - Pemain dapat menentukan item hasil pancingan apa saja yang **INGIN DISIMPAN** di dalam inventaris melalui checklist interaktif di `TakashaFishingSubScreen`:
          - `[ ] Ikan Mentah (Cod, Salmon, Tropical Fish, Pufferfish)` -> Jika tidak dicentang, langsung dibuang otomatis saat didapat.
          - `[x] Buku Enchant (Enchanted Books)`
          - `[x] Treasure Berharga (Nautilus Shell, Saddle, Name Tag, Fishing Rod, Bow)`
          - `[ ] Sampah / Junk (Leather Boots, Rotten Flesh, Sticks, Strings, Bowls)`
        - **Mekanisme Auto-Drop:** Setiap kali joran menarik item masuk ke inventaris, item yang tidak dicentang dalam Keep Whitelist langsung otomatis dibuang ke luar (`player.drop(slot, true)`).
        - **Penanganan Inventaris Penuh:** Jika seluruh slot inventaris telah terisi penuh oleh item yang disimpan, item baru yang terpancing secara alami tidak akan bisa dipungut atau tersimpan (sesuai mekanika vanilla).
      - **Fitur Perlindungan Joran & Karakter:**
        - **Rod Break Protection & Auto-Switch:** Otomatis mengganti joran jika durabilitas <= 10, atau menghentikan memancing jika tidak ada joran cadangan.
        - **Integrated Auto-Eat:** Otomatis memakan makanan di hotbar jika tingkat kelaparan <= 14 atau darah rendah.
        - **Persistent Casting & Face Water Alignment:** Membidik otomatis ke blok air target dan melempar ulang hook jika terlepas.
      - **Sub-Screen Khusus:** Diatur melalui `TakashaFishingSubScreen` (toggle Kaizen/Vanilla, checklist item simpan, threshold durabilitas, keybind pintasan).
    - **Sistem Auto Kill Mob (Anti-Aura Kill / Frontal Cone Only + Pemilihan Mob Luas):**
      - **Jaminan Pergerakan Karakter: 100% Bebas Digerakkan oleh Pemain:**
        - **Karakter Masih Bisa Digerakkan Bebas:** Mod **TIDAK MENGUNCI** pergerakan pemain. Pemain memiliki kendali bebas 100% atas tombol `WASD`, sprint, jump, dan sneak saat sistem Auto Kill aktif menyerang mob.
        - **Smart Anti-AFK Yielding (Mode Kaizen):** Gerakan mikro Anti-AFK kiri-kanan **HANYA BERJALAN** saat pemain sedang diam (idle). Jika pemain menekan tombol gerak (`WASD`), sistem Anti-AFK otomatis mengalah/jeda seketika (*instant yield*), menjamin kontrol manual pemain tidak pernah terganggu atau tersendat.
        - Di Mode Vanilla: Gerakan Anti-AFK dinonaktifkan sepenuhnya.
      - **Pencegahan KillAura (Strict Anti-Aura Policy):**
        - **DILARANG KERAS 360° KillAura:** Mod TIDAK BOLEH menyerang mob di belakang atau di samping pemain secara instan.
        - **Frontal View Cone Only:** Hanya mengeksekusi serangan jika mob berada tepat di depan pandangan karakter pemain (FOV Cone $\le 35^\circ$ atau dot product $\vec{v} \cdot \vec{d} \ge 0.82$).
        - **Line of Sight Raycast:** Menjamin mob tidak terhalang oleh blok solid penuh, namun tetap mendukung celah grinder half-slab / trapdoor.
        - **Jarak Melee Aman:** Batas jangkauan 3.5 blok (konfigurabel 2.5 - 4.0 blok) yang sesuai dengan batas jangkauan serang vanilla.
        - **100% Attack Strength Synchronizer:** Hanya mengayunkan senjata ketika cooldown serangan penuh (`player.getAttackStrengthScale(0.5f) >= 0.95f`), memaksimalkan critical damage dan lolos anticheat.
        - **Weapon Switch & Durability Guard:** Otomatis memegang pedang/kapak terkuat dan melindungi senjata dari kehancuran.
      - **Pemilihan Mob yang Dieksekusi (Selectable Mob Whitelist):**
        - Pemain dapat memilih jenis mob apa saja yang akan dieksekusi secara spesifik di layar GUI `TakashaMobKillSubScreen`:
          - **Mob Ternak/Pasif:** Sapi (Cow), Domba (Sheep), Babi (Pig), Ayam (Chicken), Kelinci (Rabbit), Kuda (Horse), Cumi-cumi (Squid/Glow Squid), dll.
          - **Mob Monster/Hostile:** Zombie, Skeleton, Creeper, Spider, Cave Spider, Slime, Magma Cube, Blaze, Enderman, Witch, Drowned, Husk, Stray, Phantom, Piglin, Zombified Piglin, Wither Skeleton, Ghast, Pillager, Ravager, Warden, dll.
          - **Pilihan Preset:** `[ Hanya Monster ]`, `[ Hanya Hewan Ternak ]`, `[ Semua Mob ]`, atau `[ Kustom Whitelist ]`.
    - **Sistem Auto Bow (Auto Aim Panah + Free Character Movement):**
      - **Tembak Otomatis (Auto Draw & Release):**
        - Ketika pemain memegang busur (Bow/Crossbow), sistem otomatis menarik tali busur (pull charge) hingga kekuatan penuh (20 ticks / 1.0 charge) dan melepaskan panah tepat pada target.
      - **Auto Aim dengan Kompensasi Balistik (Ballistic Trajectory Compensation):**
        - Menghitung orientasi yaw dan pitch ke pusat hitbox target mob.
        - Menerapkan rumus balistik busur Minecraft (kecepatan awal panah $v \approx 3.0$ blok/tick, gravitasi $g \approx 0.05$ blok/tick$^2$) dengan mengangkat sudut pitch secara proporsional sesuai jarak target, menjamin akurasi panah pada jarak menengah dan jauh.
      - **Karakter Dapat Digerakkan Bebas oleh Pemain (100% Free Movement):**
        - Sistem **TIDAK MENGUNCI** pergerakan pemain. Pemain tetap memiliki kendali penuh 100% atas tombol `WASD`, sprint, jump, dan strafe saat membidik dan menembak.
      - **Pemilihan Target Mob Busur:**
        - Pemain dapat memilih target spesifik yang boleh ditembak busur di layar `TakashaAutoBowSubScreen` (misal memprioritaskan Creeper, Skeleton, Ghast, atau Blaze).
        - Batas jangkauan panah dapat diatur (default 24 blok, konfigurabel 8 - 48 blok).
        - Raycast jalur terbang panah memastikan tidak membentur tembok solid.
    - **Unified Native Master Dashboard & Sub-Screen Architecture (1 Keybind non-Weapon Hub):**
      - **Konsep Terpadu 1 Keybind:** Menghilangkan keharusan menghafal banyak tombol acak dengan menyediakan **1 Keybind Utama (Default: `K`, `key.sakura_weapons.dashboard`)** di in-game yang membuka layar native terpusat `TakashaDashboardScreen`.
      - **Pemisahan Fitur Non-Weapon (Strict Policy):** Dashboard ini mengonsolidasikan seluruh fitur mod di luar weapon set (Auto Farm, Auto Fishing, Auto Kill Mob, Auto Bow, Fortune Reshaper, NPC & Replay Studio, Nametag, Emote Cache). Seluruh senjata & armor tetap berada di Creative Tab / mekanisme in-game normal.
      - **Struktur Kartu Modul di Dashboard:**
        1. **Auto Farm & SellGUI:** Status aktif/nonaktif, pintasan tombol saat ini, dan tombol `[ ⚙ Konfigurasi Auto Farm ]` menuju sub-halaman.
        2. **Auto Fishing (Kaizen & Vanilla):** Status memancing, mode aktif (Kaizen/Vanilla), status Capcay, dan tombol `[ ⚙ Konfigurasi Auto Fish ]`.
        3. **Auto Kill Mob (Frontal):** Status aktif, daftar mob terpilih, dan tombol `[ ⚙ Konfigurasi Auto Kill ]`.
        4. **Auto Bow (Auto Aim):** Status busur, target terpilih, dan tombol `[ ⚙ Konfigurasi Auto Bow ]`.
        5. **Laporan Fortune & Server Skill Reshaper:** Pilihan bentuk tampilan aktif (`ACTIONBAR`, `CHAT`, `TITLE`, `OFF`, `PASSTHROUGH`), tombol konfigurasi.
        6. **NPC & Replay Studio:** Tombol cepat peluncuran studio dan pengaturannya.
        7. **Nametag Visibility:** Status visibilitas nametag dan tombol konfigurasi.
        8. **Emotes & Profile Sync:** Indikator jumlah emote termuat (3.027 emotes) dan tombol reload cache.
      - **Expandable Sub-Page Navigation (`TakashaSubScreen`):**
        - Mengklik modul di Dashboard melakukan transisi mulus ke sub-halaman khusus fitur tersebut (`TakashaFarmingSubScreen`, `TakashaFishingSubScreen`, `TakashaMobKillSubScreen`, `TakashaAutoBowSubScreen`, `TakashaFortuneSubScreen`).
        - Memuat kontrol konfigurasi lengkap (toggle, pilihan mob, opsi bentuk laporan, delay tick).
        - Dilengkapi tombol navigasi `[ ◄ Kembali ke Dashboard ]`.
      - **In-Screen Specific Keybind Rebinder (`TakashaKeybindWidget`):**
        - Di setiap sub-halaman, pemain dapat langsung mengatur keybind pintasan khusus untuk fitur tersebut:
        - Pemain mengklik tombol `Shortcut Langsung: [ F... ]`, status berubah menjadi `> Tekan Tombol <`.
        - Menekan tombol keyboard apapun langsung meng-update `KeyMapping` vanilla tanpa harus mencari di menu Controls Minecraft yang berisi ratusan keybind mod lain.
        - Menekan `Esc` dapat membatalkan atau menyetel shortcut ke `NONE`.
      - **Koneksi Terpadu Mod Menu & Command:**
        - Tombol "Config" di Mod Menu (`TakashaModMenuApi`) langsung membuka `TakashaDashboardScreen` ini.
        - Command in-game `/takasha` atau `/takasha menu` membuka layar yang sama.
    - **Pengaturan & Lokasi Bentuk Laporan Fortune Panen (Vanilla, AuraSkills, AdvancedEnchantments di Sub-Page Dashboard):**
      - **Identifikasi Lokasi Asli 3 Sumber Laporan Fortune:**
        1. **Vanilla Minecraft (Fortune / Keberuntungan):** Vanilla tidak memiliki laporan teks/chat/action bar. Vanilla hanya melipatgandakan jumlah drop item secara diam-diam melalui Loot Table server (`minecraft:loot_table`). Teks laporan yang muncul di layar pemain 100% berasal dari plugin server di bawah ini.
        2. **AuraSkills (Plugin Server RPG/Skills):** Notifikasi XP farming dan kemampuan panen ganda (*Triple Harvest / Bountiful Harvest*). Lokasi asli konfigurasi di sisi server: `plugins/AuraSkills/config.yml` (bagian `action_bar:`) dan file pesan `plugins/AuraSkills/messages/messages_en.yml` (bagian `skills.farming` & `abilities.farming`). Perintah toggle bawaan server: `/skills toggle` atau `/auraskills actionbar`.
        3. **AdvancedEnchantments (AE - Plugin Server Custom Enchantments):** Menangani custom enchant cangkul seperti *Harvester*, *CropMultiplier*, *Fortune IV-X*. Lokasi asli konfigurasi di sisi server: `plugins/AdvancedEnchantments/enchantments.yml` (pada baris `effects: - MESSAGE:` atau `ACTION_BAR:`) dan `messages.yml`.
      - **Akar Masalah Mengapa Tidak Ketemu di Mod Menu:**
        1. **Server Plugin vs Client Mod:** AuraSkills dan AdvancedEnchantments adalah plugin server Spigot/Paper yang berjalan di host server multiplayer (`Noesantara.id`/`kaizenmc.id`), bukan mod client Fabric. Mod Menu di Minecraft HANYA mendeteksi mod `.jar` yang terpasang di folder `.minecraft/mods` client Anda, sehingga plugin server tidak akan pernah muncul di Mod Menu.
        2. **Mod Takasha Belum Terdaftar:** Mod Takasha sebelumnya belum mendaftarkan entrypoint `"modmenu"` di `fabric.mod.json`, sehingga tombol "Config" di Mod Menu untuk Takasha belum aktif.
      - **Lokasi Pengaturan Baru di Mod Takasha (Client-Side Reshaper):**
        1. **Di Dashboard / Mod Menu (GUI):** Tekan keybind `K` atau buka **Mod Menu -> Takasha -> Config** -> Klik **Auto Farm & Fortune** (membuka `TakashaFarmingSubScreen`).
        2. **Di Perintah Chat In-Game:** `/takasha fortune <actionbar|chat|title|off|passthrough>` atau `/takasha config`.
        3. **Di File Konfigurasi Client (Disk):** `.minecraft/config/takasha_farming.json` (parameter `"fortuneReportStyle"`).
      - **Pilihan Bentuk Laporan yang Disediakan Mod Takasha:**
        - **`ACTIONBAR` (Default Rekomendasi):** Merapikan pesan panen dari AuraSkills/AE/Fortune menjadi 1 baris bersih di atas hotbar (misal `§a✔ Panen: +4 Wortel §7(AuraSkills / Fortune III)`).
        - **`CHAT`:** Memindahkan laporan ke chat box agar tidak menutupi indikator item hotbar.
        - **`TITLE` / Subtitle:** Menampilkan teks singkat melayang di tengah layar bawah selama 1 detik.
        - **`OFF / MUTE`:** Memblokir/menyembunyikan 100% spam laporan panen dari server (layar hening dan bersih seperti vanilla).
        - **`PASSTHROUGH`:** Membiarkan teks tampil apa adanya sesuai bawaan server tanpa modifikasi.
      - **Mekanisme Pencegatan Paket Jaringan (Client-Side Interceptor):**
        - Memasang mixin client pada `ClientPacketListener` untuk paket `ClientboundSetActionBarTextPacket` dan `ClientboundSystemChatPacket`. Saat mendeteksi teks panen/fortune dari server, mod Takasha me-route ulang teks tersebut sesuai opsi yang dipilih pemain di Mod Menu / Dashboard.
    - **Solusi Tuntas Eliminasi Freeze / Lag Spike Dropdown Emote (Skalabilitas 3.000+ File):**
      - **Akar Masalah:** Folder profil Minecraft pengguna (`.../ModrinthApp/profiles/Record/emotes`) memuat **3.027 file**. Pemanggilan refleksi `getMethod("getString")` berulang kali dan pembacaan `Files.walk` sinkron di render thread saat dropdown dibuka menyebabkan Minecraft *freeze* selama beberapa detik.
      - **Arsitektur Non-Blocking Zero-Lag:**
        - **Startup Pre-heating:** Memindahkan seluruh pemindaian in-memory Emotecraft dan disk I/O ke background worker thread via `CompletableFuture.runAsync` saat mod client diinisialisasi (`TakashaClientMod.onInitializeClient()`).
        - **O(1) Snapshot UI Read:** Method `EmotecraftCompat.getDetectedEmotes()` hanya mengembalikan referensi `volatile List<EmoteEntry>` yang sudah tersortir dan immutable (akses <0.01 ms).
        - **Refleksi Ter-cache:** Meng-cache objek `Method` secara statis, meniadakan lookup dinamis di dalam loop iterasi holder.
    - **Pemuatan Emote & Icon Dinamis Langsung dari Folder Profil (`Record/emotes`):**
      - **Resolusi Path Otomatis:** Menggunakan `Minecraft.getInstance().gameDirectory.toPath().resolve("emotes")` untuk mengakses langsung direktori `Record/emotes` pengguna.
      - **Pemasangan Icon PNG Pendamping:** Mendeteksi file `<baseName>.png` yang berdampingan dengan `<baseName>.emotecraft` atau `<baseName>.json`.
      - **Lazy Dynamic Texture Loading (LRU Cache):** Menggunakan `DynamicTexture` via `NativeImage.read()` yang dimuat secara on-demand hanya untuk item yang sedang tampil di layar dropdown (maksimal 64 texture aktif di VRAM) untuk mencegah Out-Of-Memory dan GPU hitching.
    - **Perbaikan Rendering Icon/Logo Emotecraft (Blit UV Bounds Sampling Fix):**
      - Memperbaiki bug sampling UV pada `TakashaNpcScreen.java` dan `TakashaReplayOverrideScreen.java` di mana pemanggilan `extractor.blit(icon, iconX, iconY, iconX + 12, iconY + 12, 0.0f, 0.0f, 1.0f, 1.0f)` memetakan `u0=0, u1=0, v0=1, v1=1` (rentang lebar 0 px) menjadi `(0.0f, 1.0f, 0.0f, 1.0f)` (`u0=0, u1=1, v0=0, v1=1`), sehingga icon 12x12 dirender utuh 100%.
      - Membundel 11 aset icon PNG resmi Emotecraft (`waving.png`, `clap.png`, `crying.png`, `point.png`, `here.png`, `palm.png`, `backflip.png`, `roblox_potion_dance.png`, `kazotsky_kick.png`, `twerk.png`, `club_penguin_dance.png`) ke direktori `sakura-weapons/shared-resources/assets/emotecraft/emotes/`.
    - **Adopsi Arsitektur Standar ItemsAdder 4.0.18 (Singleworld Autonomy):**
      - Mengadopsi struktur ItemsAdder secara identik: zirah tubuh (Chestplate, Leggings, Boots) berbasis 2-layer equipment vanilla 26.2 (`equipment/<id>.json`), dan ornamen 3D (Hat/Helmet mecha dan Sayap) dirender dengan `SakuraHatFeatureRenderer` dan `SakuraWingsFeatureRenderer`.
      - Menjamin seluruh tampilan zirah mcmodels (#15599 Dragon Mecha Overlord, #9695 Pink Legacy, #10170 Valentines) berjalan 100% otonom di mode Singleworld tanpa memerlukan server Bukkit/Paper.
    - **Kepatuhan SemVer 2.0.0 & Lingkup Rilis:**
      - Diklasifikasikan sebagai **MAJOR release (v3.0.0)** sesuai SemVer 2.0.0 karena memperkenalkan arsitektur dual-mod modular (`Takasha` Core Weapons dan `Takasha-RPG` Singleworld Engine).
      - Target kompilasi ganda:
        - `Hasil mod/Takasha-3.0.0+26.2.jar` (Mod Utama Senjata, CIT, Replay Studio, Utilitas)
        - `Hasil mod/Takasha-RPG-1.1.0+26.2.jar` (Mod Ekosistem RPG Mandiri, Maid Companion, Shop GUI 19 Kategori [1.532 Item], Economy & Dungeon Arena)


35. **Pembersihan Total Modul Touhou Little Maid, Penyusutan Biner Mod (-91%), dan Restrukturisasi Antarmuka Master Dashboard (v2.6.0+26.2 MINOR):**
    - **Latar Belakang & Eliminasi Modul Touhou Little Maid:**
      - Sesuai arahan pengguna ("di mod takasha, yang bukan RPG kan ada mod touhou maid, hapus bagian itu dong"), modul Touhou Little Maid yang sebelumnya diuji coba di Takasha Core (sakura-weapons) dihapus secara tuntas (*clean elimination*) tanpa sisa residu kode maupun aset.
      - **Penghapusan 26 Kelas Java Inti:**
        - Entitas & Renderer: `TakashaMaidEntity.java`, `TakashaMaidRenderer.java`, `TakashaMaidModelWrapper.java`, `TakashaMaidRenderState.java`.
        - Antarmuka GUI: `TakashaMaidSubScreen.java`.
        - Jaringan / Network Packets: `UpdateMaidTaskPayload.java`, `UpdateMaidModelPayload.java`, `OpenMaidInventoryPayload.java`.
        - Registry Model: `MaidModelInfo.java`, `MaidModelRegistry.java`.
        - Parser Bedrock JSON (16 kelas di `net.sakura.weapons.client.render.bedrock`): `BedrockGeometryModel.java`, `BedrockBone.java`, `BedrockCube.java`, `BedrockAnimation.java`, `BedrockAnimationChannel.java`, `BedrockKeyframe.java`, `BedrockModelParser.java`, `BedrockAnimationParser.java`, `BedrockTextureInfo.java`, `BedrockMaterial.java`, `BedrockPart.java`, `BedrockPose.java`, `BedrockRenderLayer.java`, `BedrockResourcePack.java`, `BedrockSound.java`, `BedrockVertex.java`.
      - **Pembersihan Registry & Client Lifecycle:**
        - `ModEntities.java`: Menghapus registrasi `TAKASHA_MAID_KEY`, `TAKASHA_MAID`, dan registrasi atribut entity.
        - `ModItems.java`: Menghapus pendaftaran `TAKASHA_MAID_SPAWN_EGG`.
        - `ModItemGroups.java`: Menghapus entri spawn egg dari creative tab Arsenal.
        - `SakuraWeaponsClient.java`: Menghapus inisialisasi `MaidModelRegistry.initialize()` dan registrasi `TakashaMaidRenderer`.
        - `ModNetworkMessages.java`: Menghapus registrasi 3 payload maid dan serverbound packet receiver.
        - `assets/sakura_weapons/lang/en_us.json` & `id_id.json`: Menghapus translation key `entity.sakura_weapons.takasha_maid` dan `item.sakura_weapons.takasha_maid_spawn_egg`.
        - `takasha-rpg/src/main/resources/fabric.mod.json` & `TakashaAoeHelper.java`: Membersihkan string deskripsi dan filter entitas dari referensi maid.
    - **Penyusutan Biner Mod & Pembersihan Aset Raksasa (Asset Cleanup):**
      - Menghapus folder `sakura-weapons/shared-resources/assets/touhou_little_maid/` yang memuat 1.586 berkas aset (model Bedrock, animasi JSON, suara maid, tekstur PNG) berukuran ~40,5 MB.
      - Menghapus definisi model item `takasha_maid_spawn_egg.json`.
      - Ukuran biner JAR Takasha Core menyusut drastis dari **~20,7 MB** menjadi hanya **~1,8 MB** (penurunan ukuran sebesar **~91%**), menghasilkan mod yang jauh lebih ringan, cepat dimuat (*fast boot*), dan hemat memori JVM.
    - **Restrukturisasi & Simetri Master Dashboard Keybind `K` (`TakashaDashboardScreen`):**
      - Menghapus Tombol Row 5: `maidGalleryBtn` (`[ 👗 Galeri Model Maid ]`) dan `maidFolderBtn` (`[ 📁 Buka Folder Model ]`).
      - Menghapus Card 9 (frame preview model maid) dari siklus rendering UI.
      - Menyelaraskan tata letak kartu menjadi tepat 4 baris simetris berisi 8 modul inti (Auto Farm, Auto Fish, Auto Kill, Auto Bow, Fortune Reshaper, Replay Studio, Dynamic Nametag, Emote Cache).
      - Menyesuaikan tinggi panel dialog (`panelH`) dari 310 piksel menjadi **260 piksel** dengan posisi vertikal seimbang `topY = (screenHeight - 260) / 2`, menghilangkan ruang kosong di bawah tombol.
      - Memperbarui subtitle antarmuka menjadi: `v2.6.0 (Master Utility)`.
    - **Kepatuhan Semantic Versioning (SemVer 2.0.0):**
      - `sakura-weapons`: Ditingkatkan dari `v2.5.0` ke **`v2.6.0`** (MINOR release dengan breaking deprecation internal akibat penghapusan subsistem Touhou Maid).
      - `takasha-rpg`: Tetap stabil pada `v1.7.1` (100% isolasi RPG).
36. **Modular Multi-Engine Architecture, Server Safe Mode & Comprehensive SubScreen Value Controls (v2.7.0+26.2 & v2.5.0+1.21.11 MINOR):**
    - **Diferensiasi Mode Mesin (Kaizen vs Vanilla):**
      - **Auto Kill:** Pilihan mesin `KAIZEN` (Anti-AFK controller, raycast Line of Sight, FOV kerucut frontal $\le 35^\circ$, sinkronisasi cooldown serangan $\ge 95\%$) vs `VANILLA` (jangkauan Euclidean murni, radius $360^\circ$, mode serangan Cooldown Sync / Turbo / Hyper Spam).
      - **Auto Fish:** Pilihan mesin `KAIZEN` (analisis vektor `BobberMotionSpike`, ambang batas terkalibrasi, solver Capcay 1.0, rod slip recovery) vs `VANILLA` (deteksi paket suara cipratan air vanilla `SplashSoundPacket`, jeda lempar presisi tinggi tanpa delay buatan).
    - **Server Safe Mode (Proteksi Otomatis Lingkungan Multiplayer):**
      - Deteksi otomatis koneksi server multiplayer publik (`!mc.isLocalServer()`).
      - Menonaktifkan secara selektif fitur sensitif seperti Replay Studio (`F7`), floating ImGui window, dan pemindaian direktori emote berulang untuk mencegah banned anti-cheat dan packet flood.
      - Menyediakan toggle manual `Server Safe: ON/OFF` di Master Dashboard (`K`) untuk server LAN/private teman.
    - **Konfigurabilitas Penuh 100% Nilai Manual (Cycle Buttons):**
      - Tombol siklus nilai pada sub-layar GUI: radius horizontal/vertikal farm, jeda aksi, batch per tick, auto-sell toggle, jangkauan serang melee, target whitelist, dan opsi busur.
    - **Kepatuhan Semantic Versioning (SemVer 2.0.0):**
      - `sakura-weapons/versions/26.2`: Versi `v2.7.0+26.2`.
      - `sakura-weapons/versions/1.21.11`: Versi `v2.5.0+1.21.11`.
      - Output JAR: `Hasil mod/Takasha-2.7.0+26.2.jar` dan `Hasil mod/Takasha-2.5.0+1.21.11.jar`.

37. **Crop Plot Memory Map, Zero-Hole Guarantee, In-Tick Instant Replant & Resilient Retry Engine (v2.8.0+26.2 & v2.6.0+1.21.11 MINOR):**
    - **Akar Masalah Hilangnya Memori Lahan & Petak Kebun Bolong:**
      - Pemain mendapati Auto-Farm meninggalkan petak tanah bolong saat memanen cepat ("seperti memorinya hilang").
      - Investigasi menemukan antrean replant lama langsung dihapus permanen (*silent memory eviction*) saat benih belum masuk tas akibat delay magnet vanilla drop item (~500ms).
      - Tidak adanya peta memori spasial menyebabkan tanaman salah tanam atau gagal ditanam kembali pada substrat khusus (Nether Wart di Soul Sand & Cocoa Beans di Jungle Log).
      - Ketimpangan throughput antara kecepatan memanen (cepat) dan menanam (lambat/dibatasi delay tick) memicu defisit replant saat pemain bergerak atau berlari.
    - **Arsitektur Crop Plot Memory Map (Sesi Aktif In-Memory):**
      - Menyimpan koordinat `BlockPos` ke dalam `CropPlotInfo` in-memory saat tanaman dipanen atau terdeteksi dalam jangkauan aktif.
      - Mendukung pemetaan jenis tanaman spesifik: Gandum (`WHEAT`), Wortel (`CARROTS`), Kentang (`POTATOES`), Bit (`BEETROOTS`), Nether Wart (`NETHER_WART`), dan Biji Kakao (`COCOA`).
      - *Multi-Substrate Targeted Healing:* Memulihkan lahan kosong berdasarkan substrat (`SOUL_SAND` $\rightarrow$ Nether Wart, `FARMLAND` $\rightarrow$ Benih spesifik sesuai riwayat memori, `JUNGLE_LOG` $\rightarrow$ Cocoa Beans).
      - *Zero Residual Memory Leak:* Sesuai preferensi pengguna, memori plot disimpan secara eksklusif selama sesi aktif (`config.autoFarmEnabled == true`) dan dibersihkan total (`activePlots.clear()`, `pendingReplants.clear()`) saat fitur dimatikan atau saat berpindah dunia/server.
    - **Sinkronisasi Replant Mengimbangi Kecepatan Panen (Zero-Hole Guarantee):**
      - *In-Tick Instant Harvest-Replant Pairing:* Mengeksekusi penanaman kembali di tick dan thread yang sama persis setelah paket hancur blok (`destroyBlock` $\rightarrow$ langsung `useItemOn`) dengan jeda 0ms jika benih tersedia di tangan kiri (offhand) atau tas pemain.
      - *Symmetric Batch Throughput:* Throughput replant ditingkatkan setara kecepatan panen hingga 12 aksi per tick (`farmActionsPerTick = 12`) dengan default jeda replant 0 ms (`farmReplantDelayMs = 0`).
      - *Dynamic Flow-Control Backpressure:* Mencegah defisit replant saat berlari; jika terdeteksi $\ge 2$ petak tanah bolong dalam jarak dekat pemain ($\le 4.2$m), sistem menahan pemanenan tanaman baru untuk memprioritaskan penanaman kembali hingga kebun utuh.
      - *Sticky Offhand Caching:* Mempertahankan tumpukan benih tanaman aktif di slot offhand selama sesi bertani, mengeliminasi spam paket swap bolak-balik di setiap tick yang sebelumnya memicu desinkronisasi inventaris server.
    - **Resilient Drop-Delay Backoff Retry & Post-Planting Verification Loop:**
      - *Drop-Delay Backoff Retry:* Tugas replant tidak lagi dihapus jika benih belum masuk tas; sistem melakukan retry adaptif hingga 25 siklus ($\times 150$ms) memberi waktu toleransi penuh bagi magnet pengambilan benih vanilla.
      - *Post-Planting Verification Loop:* Memverifikasi status blok 250ms setelah paket penanaman dikirim ke server. Jika penanaman ditolak diam-diam oleh server, petak otomatis dijadwalkan ulang untuk ditanam kembali.
      - *Strict Survival Reach:* Semua interaksi penanaman dibatasi ketat $\le 4.2$ blok (`distSqr <= 17.64`) untuk menjamin 100% paket diterima oleh server multiplayer tanpa penolakan reach.
    - **Integrasi Antarmuka GUI & Konfigurasi:**
      - Menambahkan parameter konfigurasi di `TakashaMasterConfig`: `cropMemoryEnabled`, `replantPacingEnabled`, `maxReplantRetries`, `farmActionsPerTick = 12`, `farmReplantDelayMs = 0`.
      - Menambahkan tombol kontrol di `TakashaFarmingSubScreen`: `[ Memori Lahan: ON/OFF ]` dan `[ Pacing Replant: ON/OFF ]` dengan penyesuaian tinggi dialog panel menjadi 320px.
    - **Kepatuhan Semantic Versioning (SemVer 2.0.0) & Verifikasi Build Dual-Platform:**
      - `sakura-weapons/versions/26.2`: Ditingkatkan ke **v2.8.0+26.2** (Minor feature release).
      - `sakura-weapons/versions/1.21.11`: Ditingkatkan ke **v2.6.0+1.21.11** (Minor feature release).
      - Hasil kompilasi dan packaging diverifikasi 100% sukses:
        - `Hasil mod/Takasha-2.8.0+26.2.jar` (Mojang Modern 26.2, OpenJDK 25 LTS).
        - `Hasil mod/Takasha-2.6.0+1.21.11.jar` (Fabric Loom Intermediary Remapped 1.21.11, OpenJDK 21 LTS).

---

## 2. Riwayat Pembaruan Takasha-RPG Engine (takasha-rpg)

### 14.16 Riwayat Pembaruan Takasha-RPG v1.1.1 (Patch Release)
*Tanggal Rilis: 2026-10-18*

1. **Penyelarasan Tata Letak Menu Utama Pasar (Kolom 4 Center Axis):**
   - Menghapus pergeseran 1 blok ke kanan pada menu utama pasar (`TakashaShopMenu`).
   - Sumbu tengah belah ketupat kini simetris 100% pada Kolom 4 (slot 0-indexed: `4`, `13`, `22`, `31`, `40`, `49`):
     - Baris 0: `RotatingShop` (Slot 4)
     - Baris 1: `Ores` (Slot 13)
     - Baris 2: `Enchanting` (Slot 22)
     - Baris 3: `Spawners` (Slot 31)
     - Baris 4: `Potions` (Slot 40)
     - Baris 5: `/sellgui` Chest (Slot 49)
     - Kolom 0 dan 8 dilapisi kaca hitam (`black_stained_glass_pane`) sebagai bingkai simetris.

2. **Pembersihan Kategori (17 Kategori Aktif, 1.540 Item Total):**
   - Menghapus berkas dan registrasi kategori `NBT items` dan `commands` dari katalog toko.
   - Total katalog kini berisi 17 kategori aktif dengan total 1.540 item perdagangan terintegrasi (termasuk item khusus mod Takasha RPG).

3. **In-Place Container Updates (Eliminasi Bug Auto-Center Kursor):**
   - Menghilangkan pembukaan ulang container (`player.openMenu`) saat menekan tombol kuantitas (`+1`, `-1`, `+16`, `-16`, `+32`, `-32`), tombol preset (`1x`, `16x`, `64x`), tombol beralih mode beli/jual, maupun transaksi beli/jual pada `ShopTransactionMenu`.
   - Mengadopsi pembaruan in-place (`container.setItem(...)` dan `player.containerMenu.broadcastChanges()`) pada `ShopTransactionMenu`, `ShopCategoryMenu` (paginasi & quick-sell), `TakashaShopMenu`, dan `RotatingShopMenu`.
   - Kursor mouse pemain kini tetap diam pada posisi klik tanpa terlempar ke tengah layar monitor.

4. **Isolasi Total Singleplayer & Perbaikan Inversi Actionbar HUD / Scoreboard:**
   - Memperbaiki bug di mana status Health & Mana bar muncul di server publik tetapi tidak muncul di singleplayer saat level EXP = 0:
     - Mengubah *rendering anchor* dari `VanillaHudElements.EXPERIENCE_LEVEL` ke `VanillaHudElements.CHAT` (yang selalu dipanggil setiap render frame).
   - Menambahkan guard isolasi ketat `Minecraft.getInstance().hasSingleplayerServer()` pada Actionbar HUD, Scoreboard, Network Receiver (`RpgClientSyncPayload`), Tombol Grimoire di `InventoryScreen`, dan Keybind `O`.
   - Mod Takasha-RPG kini 100% pasif (tidak aktif) ketika pemain terhubung ke server multiplayer eksternal, dan 100% aktif serta stabil di dunia singleplayer.

5. **Kebijakan Semantic Versioning (SemVer):**
   - `takasha-rpg`: Ditingkatkan dari `v1.1.0` ke `v1.1.1` (Patch version: penataan GUI, bugfix kursor & HUD, isolasi singleplayer).
   - `sakura-weapons` (Takasha Core Weapons): Tetap terkunci pada versi `v2.5.0` tanpa perubahan versi.

---

### 14.17 Riwayat Pembaruan Takasha-RPG v1.1.2 (Patch Release)
*Tanggal Rilis: 2026-10-18*

1. **Resolusi Placeholder & Hitung Mundur Dinamis Magic Shop (`%nextRotation%`):**
   - Mengatasi bug teks mentah `%nextRotation%` pada lore ikon Magic Shop / RotatingShop (`ShopCategory.createIconStack`).
   - `TakashaDailyShopManager` kini menyediakan `getRemainingTimeString()` dengan pemformatan waktu dinamis (`%02dh %02dm %02ds` atau `%02dm %02ds`) dan auto-inisialisasi timer jika belum berputar.
   - Lore ikon Magic Shop di menu utama `/shop` dan header jam di `/rotatingshop` kini menampilkan hitung mundur waktu rotasi yang selalu akurat dan terbarui secara realtime.

2. **Kamus Metadata & Tooltip Detail 42 Enchantment (`EnchantmentDetailHelper`):**
   - Mengimplementasikan kamus metadata lengkap untuk seluruh 42 tipe sihir vanilla Bukkit / Minecraft 26.2 yang terdaftar di `Enchanting.yml` (seperti `WATER_WORKER`, `DAMAGE_ALL`, `ARROW_DAMAGE`, `BREACH`, `DENSITY`, `WIND_BURST`, `MENDING`, `SILK_TOUCH`, dll).
   - Menghasilkan blok lore tooltip terstruktur dan kaya visual pada katalog `/shop enchanting` maupun kalkulator transaksi:
     - Header nama sihir bilingual dengan angka Romawi (`§d§lAqua Affinity I`, `§c§lSharpness V`, dsb.).
     - Target perlengkapan yang kompatibel (`§7Kompatibel: §eHelm`, `§7Kompatibel: §ePedang & Kapak`, dsb.).
     - Rincian penjelasan efek gameplay mendalam dalam Bahasa Indonesia (`§7Efek: Menghilangkan penalti keterlambatan menambang saat berada di dalam air...`).
     - Tier maksimal (`§8Tier Maksimal: I`, `§8Tier Maksimal: V`).
   - Menyematkan data enchantment nyata (`DataComponents.STORED_ENCHANTMENTS` dengan `RegistryAccess`) pada buku sihir saat dibeli oleh pemain, sehingga buku dapat langsung digunakan pada Anvil (Landasan) vanilla untuk memperkuat perlengkapan pemain.

3. **Kebijakan Semantic Versioning (SemVer):**
   - `takasha-rpg`: Ditingkatkan dari `v1.1.1` ke `v1.1.2` (Patch version: perbaikan placeholder countdown toko sihir & penambahan kamus tooltip enchant).
   - `sakura-weapons` (Takasha Core Weapons): Tetap terkunci pada versi `v2.5.0` tanpa perubahan versi.

---

### 14.18 Riwayat Pembaruan Takasha-RPG v1.2.0 (Minor Feature Release — 100% GUI Parity & Natural Acquisition)
*Tanggal Rilis: 2026-10-19*

1. **Paritas Penuh GUI AdvancedEnchantments (100% Identik Sumber Bukkit/Spigot):**
   - **Enchanter Menu (`/enchanter`):**
     - Ukuran 18 slot (`&7Server Enchanter`), dibingkai abu-abu (`gray_stained_glass_pane`).
     - Menjual 9 Tiered Mystery Books (Simple s.d. Mastery) murni menggunakan EXP points vanilla (tanpa biaya uang fiat/koin).
     - Slot 9: Landasan Anvil mengarah langsung ke menu Tinkerer (`/tinker`).
     - Slot 17: Bingkai Portal End mengarah langsung ke menu Alchemist (`/alchemist`).
     - Membuka Confirmation Inventory 9-slot (`&aConfirmation Inventory`) saat buku diklik: slot 0-3 kaca hijau (Confirm Purchase), slot 4 preview buku misteri, slot 5-8 kaca merah (Cancel Purchase).
     - Menghitung dan memotong poin EXP riil pemain dengan kurva kuadratik vanilla Minecraft tanpa bug pembulatan.
   - **Tinkerer Menu (`/tinkerer` / `/tinker`):**
     - Ukuran 54 slot (`You                   Tinkerer`, `MenuType.GENERIC_9x6`).
     - Kolom pemisah (Splitter) pada slot `4`, `13`, `22`, `31`, `40`, `49` berlapis kaca abu-abu & pembatas merah.
     - 23 slot sisi kiri untuk deposit item pemain; 23 slot sisi kanan untuk penawaran otomatis Tinkerer (1-to-1 matching).
     - Slot 0: Tombol Status & Accept Trade (Merah saat kosong/belum siap -> Kaca hijau saat siap konfirmasi). Slot 8: Indikator informasi Tinkerer.
     - Menukar buku sihir khusus (`TakashaCustomBookItem`) menjadi Mystery Dust, Magic Dust, atau Secret Dust; mendaur ulang senjata/armor bertuah menjadi Botol Pengalaman (`EXPERIENCE_BOTTLE`).
     - Fitur keamanan auto-refund: seluruh item di sisi deposit dikembalikan otomatis ke inventori pemain jika menu ditutup.
   - **Alchemist Menu (`/alchemist`):**
     - Ukuran 27 slot (`&8Alchemist`, `MenuType.GENERIC_9x3`), dibingkai kaca hitam (`black_stained_glass_pane`).
     - Slot 3: Slot Input 1, Slot 5: Slot Input 2, Slot 13: Slot Preview Hasil Fusi, Slot 22: Tombol Transaksi & Penjelasan Biaya EXP.
     - Resep Fusi Alchemist autentik:
       - 2x Buku sihir khusus bertingkat sama -> Tingkat sihir meningkat (+1 level) dengan biaya EXP.
       - 2x Magic Dust bertingkat sama -> Magic Dust dengan persentase keberhasilan lebih tinggi.
       - 2x Slot Increaser Tier I -> Slot Increaser Tier II.
       - 2x Slot Increaser Tier II -> Slot Increaser Tier III.
       - 2x White Scroll -> Holy Scroll (Sihir Proteksi Tertinggi).
     - Mengembalikan seluruh input dengan aman ke inventori pemain jika GUI ditutup sebelum transaksi fusi dieksekusi.

2. **Perdagangan Penduduk Desa & Pedagang Kelana (Authentic Villager & Wandering Trader Trades):**
   - Mengintegrasikan Mixin server-side `VillagerMixin` dan `WanderingTraderMixin` pada `updateTrades` untuk menyuntikkan perdagangan sesuai `lootConfiguration.yml`:
     - **Librarian (Pustakawan L1-L5):** Menjual Secret Dust, Magic Dust, Buku Sihir Misteri/Terungkap (Simple s.d. Mastery), White Scroll, Transmute Scroll, Black Scroll, Randomization Scroll, Holy Scroll, dan Slot Increaser I.
     - **Cleric (Klerus L1-L5):** Menjual Magic Dust, Secret Dust, Soul Gem, Holy Scroll, dan Master Slot Increaser (Tier II).
     - **Armorer (Pandai Armor L5):** Menjual Slot Increaser I dan Armor Enchantment Orb.
     - **Weaponsmith (Pandai Senjata L5):** Menjual Stat Tracker (Mobs Killed), Stat Tracker (Player Kills), dan Weapon Enchantment Orb.
     - **Toolsmith (Pandai Alat L5):** Menjual Stat Tracker (Blocks Mined), Stat Tracker (Fish Caught), dan Tool Enchantment Orb.
     - **Wandering Trader (Pedagang Kelana):** Menjual gulungan langka eksotis (Transmute Scroll, Randomization Scroll, White Scroll, Holy Scroll, dan Soul Gem).
   - Dilengkapi pencegah duplikasi perdagangan (`hasOffer`) agar penawaran tidak menumpuk saat level penduduk desa bertambah.

3. **Injeksi Loot Dungeon & Struktur Dunia (`TakashaWorldLootModifier`):**
   - Menyuntikkan seluruh item AdvancedEnchantments dan RPG ke dalam peti-peti reruntuhan alam liar, kuil, dan dungeon vanilla:
     - **Utility Pool (Peti Dungeon Umum):** Koin RPG, Mystery Dust, Magic Dust, Secret Dust, White Scroll, Transmute Scroll, Randomization Scroll, Slot Increaser I, Buku Custom Simple & Unique, Stat Trackers.
     - **High-Tier Pool (Peti Reruntuhan Langka):** Takasha Gem, Black Scroll, Slot Increaser II, Soul Gem, Buku Custom Elite, Ultimate, Legendary, Fabled, Orbs Senjata/Armor/Alat, dan Buku Pembuka Fasilitas Portabel.
     - **Mythic Pool (Ancient City, End City, Nether Bridge, Bastion):** Holy Scroll, Slot Increaser III, Buku Custom Heroic, Soul, Mastery, serta 5 Senjata Kosmik Legendaris (`Dimensional Traveller`, `Koth Sword`, `Koth Axe`, `Phantom Scythe`, `Supreme Weapon`).

4. **Kebijakan Semantic Versioning (SemVer):**
   - `takasha-rpg`: Ditingkatkan dari `v1.1.2` ke `v1.2.0` (Minor release: penambahan sistem Enchanter, Tinkerer, Alchemist GUI 100% paritas sumber, Villager Trades, dan World Loot modifier).
   - `sakura-weapons` (Takasha Core Weapons): Tetap terkunci pada versi `v2.5.0` tanpa perubahan versi.
   - Dokumen PRD: Ditingkatkan dari `v3.1.2` ke `v3.2.0`.

---

### 14.19 Riwayat Pembaruan Takasha-RPG v1.3.0 (Minor Feature Release — Drag-and-Drop Slot Enchanting, Full Indonesian Localization, & Combat Training Dummy)
*Tanggal Rilis: 2026-10-19*

1. **Sistem Drag-and-Drop Enchanting Inventori Otentik (Bukkit/Spigot Parity):**
   - Mengimplementasikan sistem pengaplikasian enchantment dengan cara mengambil buku sihir khusus (`TakashaCustomBookItem`) menggunakan kursor mouse dan menjatuhkannya (klik kiri/kanan) langsung ke atas slot zirah, senjata, panah, atau alat kerja pemain.
   - Perhitungan persentase keberhasilan (*Success Rate*) dan kegagalan/penghancuran (*Destroy Rate*):
     - **Success:** Menambahkan sihir ke perlengkapan, memotong 1 kuota slot enchantment, memainkan suara denting level-up, dan memunculkan partikel sihir hijau.
     - **Fail (Safe):** Buku lenyap tanpa merusak perlengkapan, memunculkan partikel asap abu-abu.
     - **Destroy:** Jika dilindungi *White Scroll*, White Scroll dikonsumsi untuk menyelamatkan item. Jika tidak terlindungi, item pecah/hancur.
   - Dukungan Drag-and-Drop untuk item utility: *Magic Dust* (menaikkan rate sukses buku), *White Scroll* (proteksi kehancuran), *Black Scroll* (mengekstrak sihir acak menjadi buku), *Transmute Scroll* (merombak rate buku), *Holy Scroll* (proteksi 100% dari kematian), dan *Slot Increaser* (menambah kuota slot maksimal zirah/senjata).

2. **Lokalisasi Penuh Bahasa Indonesia (Full Indonesian Localization):**
   - Seluruh nama tingkatan buku (Sederhana, Unik, Elit, Tertinggi, Legendaris, Fabled, Heroik, Jiwa, Penguasa).
   - Seluruh lore deskripsi, efek proc, peringatan kuota slot penuh, instruksi drag-and-drop, serta antarmuka GUI kini menggunakan Bahasa Indonesia yang elegan, alami, dan bebas istilah asing kaku.

3. **Entitas Combat Training Dummy (`DamageDummyEntity`):**
   - Entitas boneka latihan berbasis ArmorStand dengan HP tak terbatas (`HP: ∞`, 1000/1000 internal).
   - Kalkulasi DPS real-time dengan sliding window 5 detik (100 ticks).
   - Melacak metrik pertempuran: Last Damage, Peak Hit Record, Total Damage, dan Hit Count.
   - Mendukung penuh seluruh status efek vanilla dan status efek khusus AdvancedEnchantments (Bleed, Frozen, Burn, Shock, Silence, Disarm, Guard Shield, Fracture, Snowblind).
   - Interaksi ramah pengguna: Shift+Klik Kanan dengan Susu untuk menetralkan efek, Shift+Klik Kanan tangan kosong untuk mereset statistik, Klik Kanan tangan kosong untuk mencetak laporan rinci ke chat.
   - Perintah administratif: `/dummy spawn`, `/dummy remove`, `/dummy reset`, `/dummy clean`, `/dummy report`, `/dummy equip <armor_type>`.

4. **Kebijakan Semantic Versioning (SemVer):**
   - `takasha-rpg`: Ditingkatkan dari `v1.2.0` ke `v1.3.0` (Minor release: Drag-and-Drop Enchanting, Full Indonesian Localization, Combat Dummy Entity).
   - `sakura-weapons`: Tetap terkunci pada versi `v2.5.0`.
   - Dokumen PRD: Ditingkatkan dari `v3.2.0` ke `v3.3.0`.

---

### 14.20 Riwayat Pembaruan Takasha-RPG v1.3.1 (Patch Release — Zero-Overlap Single Billboard Guarantee & Spatial Radial Indicator Arc)
*Tanggal Rilis: 2026-10-19*

1. **Garansi Billboard Tunggal Tanpa Overlap (*Single Billboard Guarantee & Real-time Orphan Purge*):**
   - **Persistensi NBT UUID:** Menyimpan `overheadDisplayUuid` ke NBT (`output.putString("OverheadDisplayUUID", ...)`) dan memuatnya kembali saat chunk/world reload (`input.getStringOr("OverheadDisplayUUID", "")`). Mencegah terciptanya entitas `Display.TextDisplay` baru ganda di posisi yang sama saat relog/reload dunia.
   - **Pembersihan Entitas Yatim Piatu Real-Time (`purgeOrphanDisplays`):** Setiap siklus tick overhead, memindai seluruh entitas `TextDisplay` dalam radius 2.5 blok. Seluruh entitas liar/bekas lama yang tidak lagi terikat atau menempati koordinat kepala dummy otomatis di-discard seketika, mengeliminasi efek *double vision / text ghosting*.
   - **Penandaan Tag Unik:** Menyematkan tag `takasha_dummy_overhead` dan `takasha_dummy_overhead_<dummyUUID>` pada billboard overhead untuk identifikasi deterministik.
   - **Pembersihan Total pada Perintah:** Perintah `/dummy remove` dan `/dummy clear` kini secara otomatis memindai dan menghapus seluruh `TextDisplay` dalam radius 6 blok dari pemain.

2. **Pemisahan Spasial Indikator Damage Melayang (*Spatial Radial Arc Drift & DoT Separation*):**
   - **Dispersi Horizontal Menyamping:** Angka damage tidak lagi di-spawn di tengah badan dummy menuju ke atas billboard. Indikator di-spawn menyamping secara radial (radius $0.75 \pm 0.15$ blok dari pusat dummy).
   - **Trayektori Lengkung Keluar (*Outward Arc Drift*):** Angka melayang bergerak ke arah luar secara horizontal ($dx = \pm 0.018$, $dz = \pm 0.018$) dengan kenaikan vertikal landai ($dy = +0.015$) selama 20 tick, sehingga ketinggian maksimalnya ($Y \le 1.8$) tidak akan pernah menyentuh maupun menutupi kotak billboard overhead ($Y = 2.35$).
   - **Format Khusus Status Damage over Time (DoT):** Serangan DoT (Poison, Wither, Bleed, Burn) di-spawn pada elevasi lebih rendah ($Y = 1.1 - 1.3$) dengan visual ringkas berikon status (`§2-%.1f ☠`, `§8-%.1f ☣`, `§4-%.1f 🩸`, `§6-%.1f 🔥`), terpisah secara visual dari damage serangan utama pemain.
   - **Batas Maksimal Indikator Aktif (Cap):** Membatasi maksimal 6 indikator aktif secara simultan. Jika terjadi serangan beruntun bertubi-tubi (seperti Crossbow Multishot / Poison ticks), indikator tertua otomatis di-discard agar pandangan pemain tetap jernih.

3. **Pemberantasan Tabrakan Header dengan HUD Mod Klien:**
   - Menghapus custom name vanilla pada ArmorStand dummy (`this.setCustomName(null)`), sehingga mod display eksternal seperti Jade, WTHIT, ToroHealth, dan Neat tidak lagi merender bar duplikat tepat di atas billboard kustom mod.

4. **Kebijakan Semantic Versioning (SemVer):**
   - `takasha-rpg`: Ditingkatkan dari `v1.3.0` ke `v1.3.1` (Patch release: perbaikan kritis duplikasi TextDisplay dan penyempurnaan trayektori spasial floating indicators).
   - `sakura-weapons`: Tetap terkunci baku pada `v2.5.0` (zero-touch).
   - Dokumen PRD: Ditingkatkan dari `v3.3.0` ke `v3.3.1`.

---

### 14.21 Riwayat Pembaruan Takasha-RPG v1.4.0 (Minor Feature Release — 18 Tier-Distinct HD Book Textures & Mystery/Revealed Decoupling)
*Tanggal Rilis: 2026-10-19*

1. **Migrasi 18 Tekstur HD 32x32 Otentik Sumber (`referensi-rp\RPG\Item-custom`):**
   - Mengganti seluruh tekstur buku sihir per-tier yang sebelumnya identik/sama rupa dengan 18 tekstur HD 32x32 otentik:
     - **9 Mystery Books (Kitab Tertutup/Terselubung Misteri):**
       - Tier 1 Simple: `fb_71.png` (Kitab cokelat berpita pengikat)
       - Tier 2 Unique: `fb_73.png` (Kitab oranye/amber permata)
       - Tier 3 Elite: `fb_190.png` (Kitab safir mistis gelap berpola runik)
       - Tier 4 Ultimate: `fb_192.png` (Kitab zamrud bersudut emas)
       - Tier 5 Legendary: `fb_74.png` (Kitab rubi emas mewah kekaisaran)
       - Tier 6 Fabled: `fb_138.png` (Kitab ungu magis berpendar)
       - Tier 7 Heroic: `fb_136.png` (Kitab perak kebiruan bercahaya sakral)
       - Tier 8 Soul: `fb_172.png` (Kitab sculk/jiwa kegelapan mistis)
       - Tier 9 Mastery: `fb_137.png` (Kitab emas kosmik penguasa sihir tertinggi)
     - **9 Custom Enchanted Books (Gulungan & Kitab Terbuka Bercahaya):**
       - Tier 1 Simple: `paper1.png` (Perkamen gulungan antik sederhana)
       - Tier 2 Unique: `fb_191.png` (Kitab terbuka bercahaya amber)
       - Tier 3 Elite: `fb_schematic_61.png` (Skematik gulungan berbingkai safir)
       - Tier 4 Ultimate: `fb_schematic_58.png` (Skematik gulungan berbingkai zamrud)
       - Tier 5 Legendary: `fb_schematic_70.png` (Skematik gulungan emas rubi legendaris)
       - Tier 6 Fabled: `fb_schematic_55.png` (Skematik gulungan ungu mistis fabled)
       - Tier 7 Heroic: `fb_schematic_64.png` (Skematik gulungan perak bercahaya terang)
       - Tier 8 Soul: `fb_schematic_59.png` (Skematik gulungan jiwa sculk berpendar)
       - Tier 9 Mastery: `fb_schematic_68.png` (Skematik gulungan emas agung penguasa tertinggi)

2. **Pemisahan Kelas & Entitas Item (Decoupled Mystery vs Revealed Books):**
   - **`TakashaMysteryBookItem` (`mystery_book_<tier>`):** Kelas item independen untuk buku misteri yang belum terungkap.
     - **Mekanik Buka Segel (Right-Click Reveal):** Klik kanan memegang buku misteri akan mengundi 1 custom enchantment secara acak sesuai tier, memotong 1 buku, membunyikan denting `PLAYER_LEVELUP`, dan memunculkan partikel `ENCHANT` serta `TOTEM_OF_UNDYING`.
     - **Integrasi Enchanter:** Menu Enchanter (`/enchanter`) slot 13 confirmation preview dan serah-terima pembelian kini menerbitkan item `ModRpgItems.getMysteryBookForTier(tier)`.
     - **Daur Ulang Tinkerer:** Menu Tinkerer (`/tinkerer`) menerima baik `TakashaMysteryBookItem` maupun `TakashaCustomBookItem` untuk didaur ulang menjadi Magic Dust / Secret Dust.
   - **`TakashaCustomBookItem` (`custom_book_<tier>`):** Kelas item untuk buku sihir yang sudah teridentifikasi dengan persentase *Success Rate* & *Destroy Rate* untuk sistem Drag-and-Drop enchantment.

3. **Integrasi Creative Tab & Perintah Administratif:**
   - **Tab Utilitas Kreatif (`ModRpgItemGroups.RPG_UTILITY_TAB`):** Menampilkan ke-9 Mystery Book tepat di samping ke-9 Custom Enchant Book.
   - **Perintah `/ae give` & Auto-Completion:** Mendukung kata kunci item `mystery_simple` s.d. `mystery_mastery` serta `/ae giverandombook` yang kini memberikan `TakashaMysteryBookItem` otentik.

4. **Kebijakan Semantic Versioning (SemVer):**
   - `takasha-rpg`: Ditingkatkan dari `v1.3.1` ke `v1.4.0` (Minor release: 18 tekstur HD 32x32 per-tier, pemisahan item Mystery Book dan Custom Enchant Book, integrasi penuh GUI Enchanter & Tinkerer).
   - `sakura-weapons`: Tetap terkunci baku pada `v2.5.0` (zero-touch).
   - Dokumen PRD: Ditingkatkan dari `v3.3.1` ke `v3.4.0`.

---

### 14.22 Riwayat Pembaruan Takasha-RPG v1.5.0 (Minor Feature Release — Full 336 Custom Enchantments Combat Engine Dispatcher & Radial AOE Burn Immolation)
*Tanggal Rilis: 2026-10-19*

1. **Audit Total 336 Custom Enchantments (182 AE Otentik + 154 Custom RPG):**
   - Melakukan audit mendalam dan sistematis terhadap seluruh 336 custom enchantment yang terdaftar pada ekosistem Takasha RPG berdasarkan konfigurasi otentik `referensi-rp\RPG\AdvancedEnchantments-9.24.15\enchantments.yml`.
   - Mengatasi akar masalah *silent drop* di mana sihir seperti `immolation` sebelumnya tidak berfungsi karena filter string pencarian kata kunci (`id.contains("burn") || id.contains("ignit")`) melewatkan nama `immolation` dan tidak memicu query spasial radial.

2. **Penyempurnaan Mekanik Radial AOE & Partikel Cincin Horisontal (`TakashaAoeHelper`):**
   - Membuat kelas utilitas spasial modular `TakashaAoeHelper.java` untuk kalkulasi jarak Euclidean murni dan AABB terhadap target musuh valid (`Monster`, `Enemy`, dan `DamageDummyEntity`), dengan proteksi ketat anti-friendly fire terhadap pemain, hewan peliharaan jinak (`TamableAnimal.isTame()`), dan Touhou Little Maid (`takasha_maid`).
   - Algoritma pemancar cincin partikel horisontal matematis parametrik ($X = \cos(\theta) \cdot r$, $Z = \sin(\theta) \cdot r$) untuk rendering visual lingkaran partikel sempurna di atas tanah.
   - **Mekanik Immolation:** Mengaktifkan radius bakar 2 / 4 / 6 blok sesuai level sihir, menerapkan sisa api `40 + lvl * 20` ticks pada target primer dan seluruh monster/dummy di sekelilingnya, serta memancarkan cincin partikel ganda lava dan percikan api horisontal (`FALLING_LAVA` + `LAVA`).
   - **Mekanik AOE Lainnya:**
     - `inflame`: Penebaran api instan melingkar radius $3 + \text{lvl}$ blok.
     - `cleave` / `carve` / `mightycleave`: Sweep damage radial $1.5 \times \text{lvl}$ ke seluruh entitas di sekitarnya disertai status `fracture` dan partikel sapuan pedang `SWEEP_ATTACK`.
     - `deranged`: Kejutan petir kosmik radial radius $4 + \text{lvl}$ blok.
     - `killaura`: Tebasan otomatis kontinu terhadap musuh sekitar.
     - `ambit`: Serangan tebasan radial melingkar beruntun.
     - `contagion`: Ledakan racun beracun menyebar ke sekeliling victim saat terpukul.

3. **Mekanik Mob-Specific Slayers, Melee Debuffs & Status DoTs:**
   - **Multiplikator Kerusakan Slayer Khusus:**
     - `bonecrusher`: $+20\%/\text{lvl}$ kerusakan terhadap Skeleton, Stray, Bogged.
     - `deathpunch`: $+20\%/\text{lvl}$ kerusakan terhadap Zombie, Husk, Drowned.
     - `enderslayer` / `enderbane` / `endmaster`: $+25\%/\text{lvl}$ kerusakan terhadap Enderman, Shulker, Ender Dragon.
     - `blazereaper` / `netherslayer` / `netherling`: $+25\%/\text{lvl}$ kerusakan terhadap monster dimensi Nether (Blaze, Magma Cube, Ghast, Piglin, Wither, Hoglin).
     - `beastslayer` / `hunter`: $+20\%/\text{lvl}$ kerusakan terhadap Arthropoda & Beasts (Spider, Silverfish, Wolf, Ravager, Bee).
     - `cubism`: $+30\%/\text{lvl}$ kerusakan terhadap Slime dan Magma Cube.
     - `brightness`: $+25\%/\text{lvl}$ kerusakan terhadap Warden dan area berlevel cahaya rendah ($\le 4$).
     - `soulless`: $+15\%/\text{lvl}$ kerusakan terhadap seluruh monster musuh (Hostile Mobs).
   - **Melee Debuffs & Status DoTs:**
     - `frost`: Slowness II + partikel Snowflake beku.
     - `bleed`: Damage DoT darah bertubi-tubi + partikel Crimson Heart.
     - `shock`: Slowness + lonjakan petir elektrik `ELECTRIC_SPARK` + audio guntur.
     - `silence`: Kutukan sihir `WITCH` yang membungkam musuh.
     - `disarm`: Mining Fatigue III + debuff penyerangan.
     - `fracture` / `acid` / `corrosion`: Peremukan pertahanan armor dengan partikel Crit.
     - `flashbang`: Kebutaan instan (Blindness) + Slowness + ledakan kembang api Firework.
     - `confuse`: Mual disorientasi (Nausea).
     - `diminish` / `dominate`: Efek pelemahan fisik (Weakness).
     - `stun` / `cripple` / `trap`: Pembekuan gerakan ekstrem (Slowness IV).
     - `famine` / `starvation`: Efek lapar mendalam (Hunger).
   - **Combat Procs & Criticals:**
     - `lifesteal`: Pemulihan HP penyerang proporsional dari $5\% \times \text{lvl}$ damage serangan.
     - `berserk` / `enrage` / `shura`: Memicu buff Strength saat HP penyerang sekarat ($< 50\%$).
     - `critical` & `execute`: Serangan critical dahsyat $+15\% \times \text{lvl}$, bertambah kuat jika HP musuh $< 35\%$.
     - `allure` / `magnet`: Menarik musuh mendekat ke arah penyerang.
     - `forcefield` / `repel` / `obliterate`: Mementalkan musuh menjauh dari penyerang.
     - `explosive` / `frenzy`: Ledakan area splash kerusakan api dan dentuman dinamit.

4. **Mekanik Pertahanan Zirah (Armor Defense Triggers):**
   - `creeperarmor`: Pembatalan 100% damage ledakan Creeper dan ledakan TNT/kristal.
   - `arrowdeflect` / `arrowbreak`: Peluang $20\% \times \text{lvl}$ menepis proyektil panah sepenuhnya dengan suara perisai dan partikel salju.
   - `dodge`: Peluang $8\% \times \text{lvl}$ menepis serangan fisik sepenuhnya dengan suara kelelawar dan partikel poof.
   - `jellylegs` / `feather_step`: Pembatalan 100% kerusakan jatuh (*fall damage*).
   - `plummet`: Membatalkan fall damage dan mengubah benturan menjadi gelombang gempa AoE radius $3 + \text{lvl}$ blok ke monster sekitar.
   - `shockwave`: Peluang $25\% \times \text{lvl}$ memancarkan gelombang angin tolak radial 4 blok.
   - `smokebomb`: Tirai asap tebal dan kebutaan lawan saat HP $< 50\%$.
   - `reflect` / `rebounding` / `cactus`: Memantulkan $8\% \times \text{lvl}$ damage masuk kembali ke penyerang.
   - `guard` / `safeguard` / `ward`: Perlindungan perisai Absorption otomatis.
   - `escape` / `getaway`: Lonjakan Speed II dan Regeneration saat HP sekarat ($< 25\%$).
   - Cleansing: `clarity` (menghapus Blindness), `lucid` (menghapus Poison & Darkness), `metaphysical` & `blessed` (menghapus Slowness, Nausea, Weakness).

5. **Mekanik On-Kill Triggers:**
   - `decapitation`: Peluang $15\% \times \text{lvl}$ memenggal dan menjatuhkan kepala mob (Zombie Head, Skeleton Skull, Wither Skeleton Skull, Creeper Head, Piglin Head, Dragon Head, Player Head).
   - `devour`: Memulihkan food & saturation pemain seketika saat membunuh musuh.
   - `rebound`: Pemulihan instan $+2.0 \text{ HP} \times \text{lvl}$ saat menumbangkan target.
   - `inquisitive` / `master_inquisitive`: Bonus orb experience melimpah $+6 \text{ EXP} \times \text{lvl}$ per kill.

6. **Paritas Penuh Boneka Latihan (`DamageDummyEntity` DoT Burn Integration):**
   - Menambahkan penanganan mandiri status `burnTicks` pada siklus tick `DamageDummyEntity`. Saat dummy terbakar oleh `immolation` atau status api lainnya, dummy secara otomatis menerima 1.0 HP DoT damage setiap 20 tick, tercatat ke dalam sliding window DPS 5 detik, memperbarui metrik billboard overhead, serta memunculkan floating damage indicator khusus `§6-1.0 🔥`.

7. **Kebijakan Semantic Versioning (SemVer):**
   - `takasha-rpg`: Ditingkatkan dari `v1.4.0` ke `v1.5.0` (Minor release: Full 336 Custom Enchantments Combat Engine Dispatcher, Radial AOE Immolation Burn Ring, Mob Slayers, Melee Debuffs/DoTs & Status Procs, Armor Defense Reflector/Dodge/Deflect, On-Kill Decapitation & Devour, Dummy DoT Damage Parity).
   - `sakura-weapons`: Tetap terkunci baku pada `v2.5.0` (zero-touch).
   - Dokumen PRD: Ditingkatkan dari `v3.4.0` ke `v3.5.0`.

---

### 14.23 Riwayat Pembaruan Takasha-RPG v1.6.0 - v1.6.2 (Combat Tuning, Leaderboard Podium & In-Place Container Reuse)
*Tanggal Rilis: 2026-10-20*

1. **Top-10 Leaderboard Podium AuraSkills (`AuraLeaderboardMenu`):**
   - Menghadirkan menu papan peringkat 54-slot kanonikal AuraSkills untuk 11 skill RPG dan Power level.
   - Penataan podium terdistribusi: Rank #1 di posisi puncak bermahkota emas (Slot 13), Rank #2 & #3 di sayap podium (Slot 21 dan 23), serta Rank #4–#10 di baris meja peringkat (Slot 28–34).
   - Tombol kembali (Slot 45) terintegrasi mulus dengan `AuraSkillsMenu`.
2. **Protokol Strict Zero Cursor Displacement:**
   - Navigasi bolak-balik antara `/skills`, `/stats`, dan `/leaderboard` memanfaatkan *In-Place Container Reuse* pada kontainer `ReadOnlyChestMenu` 54-slot.
   - Mengeliminasi sentakan atau pemindahan kursor mouse ke tengah layar monitor pemain saat berpindah antarmuka.
3. **Penyempurnaan Target Dummy Tempur v1:**
   - Pembuatan entitas latihan tempur mandiri `DamageDummyEntity` dengan pencatatan hit sliding window 5 detik dan kalkulasi DPS akurat.

---

### 14.24 Riwayat Pembaruan Takasha-RPG v1.7.0 (Minor Feature Release — AuraSkills Sub-GUI Parity, Disaster Recovery & Soft Cap Enchants)
*Tanggal Rilis: 2026-10-24*

1. **Sub-GUI Interaktif AuraSkills 54-Slot (`AuraSourcesMenu` & `AuraAbilitiesMenu`):**
   - **`AuraSourcesMenu`:** Menampilkan katalog lengkap sumber perolehan XP asli (Mining, Foraging, Farming, Fishing, Combat, dll.) dalam grid 28 slot (slot 10–16, 19–25, 28–34, 37–43), dilengkapi sistem paginasi halaman dan pengurutan dinamis Hopper (Default, XP Tertinggi, A–Z).
   - **`AuraAbilitiesMenu`:** Menampilkan seluruh kemampuan (5 pasif + 1 mana ability aktif) per skill dengan indikator visual status terbuka (`Items.DYE.lightBlue()`) atau terkunci (`Items.DYE.gray()`).
   - **Navigasi Kanonikal:** Alur navigasi `/skills` -> Level Progression -> Sources / Abilities menggunakan kontainer 54-slot dengan *In-Place Container Reuse* (Zero Cursor Displacement).
2. **Disaster Recovery Snapshot & Auto-Cleanup Sisa Barrier (`DungeonArenaEngine`):**
   - Dual-file session snapshot atomik (`dungeon_active_session.json` dan `.backup.json`) di direktori save world (`takasha_rpg/`) dengan `StandardCopyOption.REPLACE_EXISTING`.
   - Fallback perimeter cleanup saat server boot: mendeteksi dan membersihkan blok barrier atau tinted glass sisa dari sesi lama/force crash yang belum sempat dipulihkan.
3. **Penyeimbangan RPG Balance: Diminishing Returns / Soft Cap Slot Enchant > 10 (`TakashaEnchantCombatEngine`):**
   - Menerapkan kurva soft cap $\text{Multiplier} = \frac{10.0 + \sqrt{N - 10}}{N}$ saat jumlah custom enchantment $N > 10$.
   - Mencegah ledakan eksponensial kerusakan dan risiko integer overflow pada perlengkapan dengan slot tinggi (15–25 slot).
4. **Catatan Keputusan Arsitektur: Eksklusi Multi-Profile RPG:**
   - Fitur multi-profile ditiadakan secara permanen karena mod berfokus pada singleplayer otentik 1 profil per dunia savegame.

---

### 14.25 Riwayat Pembaruan Takasha-RPG v1.7.1 (Patch Release — Eliminasi Double Nametag & Sinkronisasi Multi-Source DoT Display)
*Tanggal Rilis: 2026-10-24*

1. **Eliminasi Total Bug Visual Double Nametag (`DamageDummyEntity`):**
   - **Fixed:** Menonaktifkan native nametag vanilla ArmorStand (`setCustomNameVisible(false)`) secara persisten di konstruktor dan siklus pembaruan display, melenyapkan teks ganda yang saling menabrak di atas kepala dummy.
   - **Changed:** Menetapkan `Display.TextDisplay` billboard sebagai satu-satunya *Single Source of Truth* visual di atas kepala dummy.
   - **Changed:** Menaikkan titik jangkar spasial TextDisplay dari $Y + 2.35$ menjadi **$Y + 2.50$** dari kaki ArmorStand untuk memberikan ruang bebas (*clearance*) tanpa terhalang helm zirah maupun partikel kobaran api.
2. **Sinkronisasi Multi-Source Durasi Status Efek (DoT, Buff & Debuff):**
   - **Fixed:** Mengintegrasikan sisa durasi pembakaran api Vanilla (`getRemainingFireTicks()`) ke dalam siklus DoT `burnTicks`, sehingga dummy yang dibakar oleh Flint & Steel, lava, panah Flame, atau pedang Fire Aspect seketika menampilkan `§6🔥 Burn (m:ss)` dan menerima kalkulasi DoT serta floating indicator damage.
   - **Fixed:** Mengintegrasikan deteksi pembekuan Vanilla (`getTicksFrozen()`) ke dalam `freezeTicks` -> `§b❄ Frozen (m:ss)`.
   - **Added:** Visualisasi durasi real-time menit:detik `(%d:%02d)` untuk seluruh status ramuan Vanilla (`MobEffectInstance`), Custom AE (Bleed, Shock, Silence, Disarm, Fracture, Snowblind), dan AuraSkills.
   - **Changed:** Fallback status netral bersih: `§7[Status: Netral / Tidak Ada Efek]` saat tidak ada efek aktif sama sekali.
3. **Penyempurnaan Kebersihan Display & Chat Report:**
   - Logika `purgeOrphanDisplays` menyelaraskan ambang batas pembersihan dengan elevasi $Y + 2.50$ dan re-binding UUID deterministik.
   - Inspeksi klik kanan mencetak ringkasan sisa durasi status efek detik riil ke chat pemain.
4. **Kebijakan Semantic Versioning (SemVer):**
   - `takasha-rpg`: Ditingkatkan dari `v1.7.0` ke `v1.7.1` (Patch release: visual polish & state synchronization bugfix).
   - `sakura-weapons`: Tetap terkunci baku pada `v2.5.0` (100% Zero-Touch).
   - Dokumen PRD: Diselaraskan ke `v4.0.0` (Sub-bab 17.13 & 18.1 dimutakhirkan ke arsitektur Single-Layer Billboard).

---

### 14.26 Riwayat Pembaruan Takasha-RPG v1.8.0 (Minor Feature Release — World Events Engine, Grimoire Questing & 16 Sovereignty Titles)
*Tanggal Rilis: 2026-10-24*

1. **Sistem Takasha World Events (Acak & Berkala):**
   - **4 Acak (10–15 Menit Interval, Durasi 5–8 Menit):**
     - `BLOOD_MOON`: Langit berdarah, bonus XP +50%, koin +100%, monster lebih agresif.
     - `GOBLIN_HUNT`: Munculnya Golden Goblin pencuri emas di sekitar pemain dengan drop koin dan emas.
     - `STARFALL`: Hujan bintang kosmik yang menjatuhkan Magic Dust berkualitas tinggi di permukaan daratan.
     - `RIFT_INCURSION`: Anomali celah dimensi jurang dengan partikel portal dan gelombang mob abyssal.
   - **4 Berkala (Setiap 45–60 Menit Sekali):**
     - `DOUBLE_XP`: Peningkatan perolehan AuraSkills XP 2.0x lipat ke seluruh aksi pemain.
     - `MYTHIC_MERCHANT`: Kemunculan Wandering Trader mistis pembawa dagangan langka (White Scroll, Nightmare Keystone, Magic Dust +50%).
     - `DUNGEON_RUSH`: Penggandaan loot drop Dungeon Arena 2.0x lipat dan cooldown keystone dihilangkan.
     - `HARVEST_FESTIVAL`: Penggandaan hasil panen pertanian dan foraging 2.0x lipat.
   - **Zero World Griefing:** Tidak merusak blok buatan pemain atau medan overworld saat event berlangsung.
   - **ServerBossEvent & Soundscape:** Boss bar dinamis real-time dengan penghitung mundur menit:detik dan suara atmosferik saat event dimulai atau berakhir.
   - **Command Interface:** `/rpg event [status|start <id>|stop]` dan alias survival mandiri `/event` / `/events`.

2. **Perombakan Grimoire of Realms (`GrimoireScreen.java`):**
   - Menghadirkan navigasi 5 Tab terstruktur: `[⚔ AuraSkills]`, `[📜 Quests]`, `[👑 Titles]`, `[🌍 Events & Arena]`, `[🏠 Homes]`.
   - **Visual Progress Bar:** Render bar kemajuan dual-color ASCII `[████████░░]` pada ringkasan keahlian dan target quest.
   - **Papan Quest Interaktif:** Menampilkan 3 kategori (Kemajuan Bab 1-4, Misi Harian/Mingguan, 16 Legendaris) dengan tombol interaktif `[Klaim Hadiah]` via network payload `ClaimQuestC2SPayload`.
   - **Katalog Gelar & Trait:** Menampilkan 16 Gelar Kedaulatan, status unlock dinamis, rincian trait pasif, dan tombol `[Pakai Gelar]` / `[Lepas Gelar]` via `EquipTitleC2SPayload`.
   - **Zero Cursor Displacement & Paging:** Navigasi tab dan paginasi kartu tidak mereset posisi kursor mouse pemain.

3. **16 Title Traits Sovereignty Engine (`TakashaTitleManager.java`):**
   - Mengimplementasikan seluruh 16 Gelar Kedaulatan resmi (G-01 s/d G-16) dengan passive trait dan trigger combat unik:
     - G-01: Adrenaline Surge (Speed II + Resistance I saat darah kritis).
     - G-02: Void Anchor (Pencegahan jatuh ke kehampaan & Slow Falling darurat).
     - G-03: Lightning Galvanization (Kebal sambaran petir + Haste II & Strength I).
     - G-04: Phantom Riposte (Peluang 25% memantulkan proyektil panah).
     - G-05: Blood Frenzy (Regenerasi dan kekuatan saat killstreak malam/darah).
     - G-06: Abyssal Vortex (Pusaran jurang penarik monster saat menambang dalam).
     - G-07 s/d G-16: Penguatan armor rating, critical rate, diskon dust anvil, arrow pull, hingga Divine Sovereign (+20% All RPG Stats).

4. **Auto-Tracking Global Listener:**
   - Pencatatan otomatis statistik pemain dan progres quest di:
     - `AuraSkillsEngine`: Farming, mining deepslate, pembunuhan mob malam/nether/boss, dan Sakura/Takasha weapon kills.
     - `TakashaCustomBookItem`: Penggunaan Magic Dust pada anvil sihir.
     - `DungeonArenaEngine`: Gelombang arena dibersihkan dan dungeon selesai.
     - `ShopTransactionMenu`: Pembelian transaksi toko harian.

5. **Kepatuhan Arsitektur & SemVer:**
   - Mod `takasha-rpg` ditingkatkan ke **v1.8.0** (Minor feature release: World Events + Grimoire Quests & Sovereignty).
   - Mod `sakura-weapons` tetap terkunci pada **v2.6.0** (100% Zero-Touch).

---

## 15. Riwayat Pembaruan Takasha Core (sakura-weapons) v2.7.0+26.2 & v2.5.0+1.21.11 (Minor Feature Release — Server Safe Mode, Kaizen vs Vanilla Engine Specialization & Full Manual Value Control)
*Tanggal Rilis: 2026-10-26*

1. **Proteksi Server Safe Mode (`serverSafeMode`):**
   - **Otomatisasi Deteksi Multiplayer:** Saat pemain terhubung ke server multiplayer (`!mc.isLocalServer()`), fitur kreatif/developer yang berpotensi memicu kecurigaan admin atau desinkronisasi server secara otomatis dinonaktifkan:
     - Pintasan Replay Studio (F7) diblokir dan menampilkan overlay warning `§c[Server Safe] Replay Studio dinonaktifkan di server multiplayer!`.
     - Tombol Replay Studio dan Tombol Segarkan Emote di Master Dashboard di-disable (abu-abu/inactive) dengan tooltip pengaman.
     - `TakashaReplayOverrideScreen.init()` dan `TakashaStudioWindow.render()` memeriksa status multiplayer dan menutup layar secara instan.
   - **Kontrol Manual:** Disediakan tombol toggle `Server Safe: ON/OFF` di Dashboard untuk pemain yang ingin mengabaikan proteksi server secara manual jika diizinkan server lokal/private.

2. **Spesialisasi Kaizen vs Vanilla Engine:**
   - **Auto Kill Mob:**
     - **Mode KAIZEN:** Mengaktifkan osilasi cerdas anti-AFK (`TakashaMobKillAntiAfk`) dengan proteksi 100% free movement pemain, raycast Line-Of-Sight presisi tinggi yang mendukung celah half-slab mob grinder, sinkronisasi jeda cooldown senjata ($\ge 95\%$), serta kerucut pandang ketat anti-aura ($\le 35^\circ$).
     - **Mode VANILLA:** Pure stationary attack tanpa pergerakan otomatis (nol transmisi paket WASD/kamera), pemeriksaan jarak langsung tanpa overhead raycast rumit, kecepatan serangan unthrottled (Turbo $\ge 70\%$ atau Hyper Spam 0ms per tick), dan sudut serang bebas hingga 360°.
     - **Ekspansi Parameter Manual 100%:**
       - Jangkauan serangan terjauh (*Reach*): Siklus 2.5m, 3.0m, 3.5m, 4.0m, 4.5m, 5.0m, hingga 6.0m.
       - Prioritas sasaran (*Kill Target Priority*): Terdekat (*Nearest*), HP Terendah (*Lowest Health*), HP Tertinggi (*Highest Health*), atau Titik Tengah Crosshair (*FOV Center*).
       - Kecepatan serangan (*Attack Rate*): Cooldown Sync ($\ge 95\%$), Turbo ($\ge 70\%$), atau Hyper Spam (0ms / tiap tick).
       - Kerucut pandang (*FOV Cone*): 20°, 35°, 60°, 90°, 180°, atau 360° bebas.
       - Smart Critical: Serang hanya saat jatuh dari lompatan untuk memicu 150% critical damage.
       - PVP Players & Hostile Only toggles.
   - **Auto Fishing:**
     - **Mode KAIZEN:** Pelacakan paket lonjakan pergerakan pelampung (`ClientboundSetEntityMotionPacket`) dengan sub-tick reaction time, integrasi otomatis Auto-Capcay 1.0 (solver minoritas frekuensi item), penanganan lepas joran (*Rod Slip Recovery*), dan jitter penarikan acak manusiawi ($\pm 100$ms).
     - **Mode VANILLA:** Deteksi suara percikan air pelampung vanilla (`ClientboundSoundPacket` `entity.fishing_bobber.splash`), 0% beban CPU (Capcay dan Rod Slip di-bypass), jeda recast deterministik tanpa jitter.
     - **Ekspansi Parameter Manual 100%:**
       - Jeda tarik kail (*Reel-In Delay*): 0ms (instan), 50ms, 80ms, 150ms, 300ms.
       - Jeda lempar ulang (*Recast Delay*): 250ms, 500ms, 1000ms, 1500ms.
       - Sensitivitas gigitan (*Bite Sensitivity*): Ultra (-0.03), High (-0.06), Normal (-0.10).
       - Auto Rod Swap: Otomatis berpindah/menukar ke joran baru dari inventory saat sisa durability joran aktif $\le 3$.
       - Sound Alert: Efek suara lonceng *ding* saat berhasil menarik tangkapan.
       - Keep Catch Whitelist: Checklist item tangkapan yang dipertahankan di inventory, item lain dibuang otomatis.

3. **Perluasan Kecepatan & Jangkauan Tertinggi Auto Farm:**
   - Jangkauan horizontal default 6 blok, vertikal 2 blok (dapat disiklus 1–6H dan 1–3V).
   - Delay aksi 0ms dan kemampuan batch aksi hingga 12 eksekusi per tick.
   - Delay penanaman ulang instan 0ms (dapat disiklus 0ms, 50ms, 120ms, 250ms).
   - Tanam Farmland Kosong: Otomatis menanam benih gandum, wortel, kentang, atau bit pada blok `Blocks.FARMLAND` yang belum ditanami.
   - Otomatis eksekusi `/sellgui` dan cadangan benih minimal (8, 16, 32, 64 biji).

4. **Kepatuhan Arsitektur & SemVer 2.0.0:**
   - `versions/26.2`: Ditingkatkan ke **v2.7.0+26.2** (Minor feature release).
   - `versions/1.21.11`: Ditingkatkan ke **v2.5.0+1.21.11** (Minor feature release).
   - Root `VERSION`: `2.7.0`.
   - File konfigurasi persisten `takasha_master_features.json` menyimpan seluruh parameter di atas secara lengkap.

---

## 16. Riwayat Pembaruan Takasha Ecosystem v2.8.0+26.2 & v2.6.0+1.21.11 (Major Ecosystem Milestone — Auto-Farm Crop Plot Memory, Standalone Takasha-RPG Singleworld Engine, Touhou Maid Companion Extraction & YSM Integration, Damage Dummy DoT Tracking, and Web Companion Dashboard)
*Tanggal Rilis: 2026-10-27*

1. **Optimalisasi Menyeluruh Auto-Farm: Crop Plot Memory & Resilient Replant Engine (`TakashaFarmHandler`):**
   - **Penyelesaian Isu Lahan Bolong (*Zero-Hole Guarantee*):** Mengatasi keluhan pemain mengenai memori lahan yang hilang saat farming berkecepatan tinggi, memastikan seluruh petak tanaman yang dipanen langsung ditanami kembali secara instan dan presisi tanpa menyisakan tanah kosong.
   - **Crop Plot Memory Map (In-Memory Sesi Aktif):**
     - Model data `CropPlotInfo` & `PendingReplant` menyimpan koordinat `BlockPos`, jenis benih tanaman asli (`CropType`: Gandum, Wortel, Kentang, Beetroot, Nether Wart, Cocoa Beans), dan timestamp pencatatan.
     - Memori kebun beroperasi murni in-memory selama fitur aktif (`config.autoFarmEnabled == true`). Begitu fitur dimatikan atau pemain keluar dunia, memori dibersihkan total (`activePlots.clear()`) untuk mencegah memory leak (*Zero residual memory leak*).
   - **Multi-Substrate Targeted Healing:**
     - `Blocks.SOUL_SAND` $\rightarrow$ Prioritas penanaman kembali bibit `NETHER_WART`.
     - `Blocks.FARMLAND` $\rightarrow$ Benih spesifik dari memori lahan (kebun wortel tetap ditanami wortel, kebun kentang tetap ditanami kentang).
     - `BlockState.is(BlockTags.JUNGLE_LOGS)` $\rightarrow$ Penanaman kembali biji kakao (`COCOA`).
   - **In-Tick Instant Harvest-Replant Pairing (0ms Delay):**
     - Paket panen `destroyBlock(...)` dan paket penanaman kembali `useItemOn(...)` dieksekusi secara instan berpasangan dalam tick yang sama jika benih tersedia di offhand atau tas pemain.
     - Menghilangkan jeda antar-tick dan menjaga rasio 1 panen : 1 tanam secara sempurna bahkan saat pemain bergerak cepat atau sprint.
   - **Symmetric Batch Throughput (Hingga 12 Aksi/Tick):**
     - Parameter `farmActionsPerTick` disetel default ke `12` aksi/tick dan `farmReplantDelayMs` disetel default ke `0` ms (Instan), mengimbangi 100% throughput pemanenan.
   - **Dynamic Flow-Control Backpressure:**
     - Jika terdeteksi $\ge 2$ petak tanah kosong di sekitar pemain ($\le 4.2$m), sistem secara otomatis menahan pemanenan tanaman baru (`harvestSuppressed = true`) untuk memfokuskan aksi tick pada penanaman petak bolong hingga lahan pulih sepenuhnya.
   - **Sticky Offhand Caching:**
     - Tumpukan benih tanaman aktif dipertahankan di tangan kiri (offhand) selama proses farming tanpa melakukan swap balik ke item lama di setiap tick, mengeliminasi spamming paket `SWAP` inventaris yang memicu lag server.
   - **Drop-Delay Resilient Backoff Retry:**
     - Antrean replant tidak dihapus saat benih belum masuk tas akibat delay magnet vanilla item drop (~500ms). Tugas replant melakukan backoff retry hingga 25 siklus ($\times 150$ms) dan baru diabaikan jika inventaris pemain benar-benar kehabisan benih.
   - **Post-Planting Verification Loop (250ms):**
     - 250ms setelah paket penanaman dikirim ke server, status blok diverifikasi ulang. Jika paket ditolak diam-diam oleh server multiplayer karena jitter lag, koordinat otomatis dijadwalkan ulang untuk ditanam kembali.
   - **Strict Survival Reach ($\le 4.2$m):**
     - Semua interaksi penanaman dibatasi ketat $\le 4.2$ blok (`distSqr <= 17.64`), menjamin 100% paket diterima server multiplayer tanpa penolakan jangkauan survival.
   - **Kontrol GUI Baru di `TakashaFarmingSubScreen`:**
     - Tombol toggle `[ Memori Lahan: ON/OFF ]`
     - Tombol toggle `[ Pacing Replant: ON/OFF ]`
     - Dialog panel diperluas menjadi 320px untuk tata letak tombol yang rapi dan simetris.

2. **Peluncuran Penuh Standalone Takasha-RPG Singleworld Engine (`takasha-rpg` v1.8.0 / v2.8.0):**
   - **100% Paritas Fitur AdvancedEnchantments (Bebas Spigot/Paper):**
     - **6 Tier Kelangkaan Terstandarisasi:** `SIMPLE` (Putih), `UNIQUE` (Hijau), `ELITE` (Biru), `ULTIMATE` (Kuning Emas), `LEGENDARY` (Oranye), `SOUL` (Merah Kirmizi).
     - **Mesin Tempur Lanjutan (`TakashaEnchantCombatEngine`):** Mengimplementasikan proc effect tempur: *Lifesteal* (sedot HP musuh), *Bleed* (efek pendarahan DoT berkala), *Shockwave* (gelombang hentakan AoE), *Freeze* (pembekuan gerak musuh), *Execute* (damage mematikan pada HP kritis), *Double Strike* (serangan ganda instan), *Vampire*, *Blindness*, *Poison*, *Divine Light*.
     - **Mesin Utilitas Lanjutan (`TakashaEnchantUtilityEngine`):** *Vein Miner* (penambangan urat bijih 3D DFS sekaligus), *Auto Smelt* (peleburan instan saat menambang), *Telepathy* (drop item langsung masuk ke tas), *Magnet* (penarik drop item dalam radius luas), *Experience Boost*, *Trench* (penggalian area 3x3), *Oxygenate*, *Gears*, *Springs*.
     - **Item Pendukung Lengkap:**
       - *Magic Dust*: Meningkatkan persentase keberhasilan (*Success Rate*) buku sihir (1% s.d. 100%).
       - *White Scroll*: Melindungi peralatan dari kehancuran jika proses enchant di Anvil gagal (*Destroy Rate*).
       - *Black Scroll*: Mencabut enchant kustom acak dari perlengkapan dan mengubahnya kembali menjadi buku sihir 100% aman.
       - *Transmutation Orb*: Menambah slot maksimal custom enchant pada senjata/zirah.
       - *Mystery Books*: Buku misteri bersegel yang dapat dibuka dengan klik kanan untuk mendapatkan enchant kustom acak sesuai tier.
     - **Menu Khusus Paritas Spigot:**
       - `/enchanter` — Toko 18-slot pembelian buku misteri menggunakan EXP Points murni dengan dialog konfirmasi 9-slot.
       - `/tinkerer` atau `/tinker` — Antarmuka 54-slot untuk mendaur ulang buku dan zirah menjadi Secret Dust.
       - `/alchemist` — Antarmuka 27-slot untuk fusi dan upgrade dua buku bertingkat sama.
     - **Tekstur Buku Kustom Unik per Tier:** Tiap tier buku sihir memiliki tekstur Blockbench resolusi tinggi yang khas dan mudah dibedakan di inventori.
   - **Dungeon Rift Arena Engine (`DungeonArenaEngine`):**
     - 5 Tier Dungeon Keystones (Tier 1: Ruin, Tier 2: Crypt, Tier 3: Abyss, Tier 4: Void, Tier 5: Nightmare).
     - Instanced Wave Combat: Gelombang monster bertahap dengan AI hunter agresif (`DungeonMobHunterGoal`), fase pertarungan bos dengan mekanika serangan spesial, batas waktu countdown, fail-safe permadeath, loot chests interaktif, dan drop material crafting eksklusif.
   - **Damage Test Dummy & Status SubGUI DoT Tracker:**
     - Entitas boneka latih tempur (`DamageDummyEntity`) dengan armor stand kustom.
     - Floating combat damage numbers (Kuning = Normal, Merah = Critical, Ungu = DoT, Hijau = Heal).
     - Status SubGUI DoT Tracker: Visualisasi real-time damage per second (DPS), durasi status efek aktif, dan log riwayat serangan.
   - **AuraSkills Engine & Atribut Skala Nyata (`AuraStatModifierEngine`):**
     - 7 Cabang Keahlian: `Combat`, `Mining`, `Foraging`, `Farming`, `Fishing`, `Archery`, `Defense`.
     - Kalkulasi stat nyata: Strength (damage melee), Health (hati ekstra), Wisdom (mana & EXP), Toughness (reduksi damage), Luck (loot ganda), Agility (kecepatan).
     - HUD Actionbar Real-Time bilingual, Level-Up Fanfare sound & visual, dan Grimoire Skill Screen.
   - **Grimoire of Realms, Quests & 16 Gelar Kedaulatan:**
     - 16 Gelar Kedaulatan (G-01 s/d G-16) dengan passive combat traits permanen (*Adrenaline Surge*, *Void Anchor*, *Lightning Galvanization*, *Phantom Riposte*, hingga *Divine Sovereign*).
     - Questline 4 Bab Cerita, Misi Harian/Mingguan berhadiah currency/xp, dan Misi Legendaris.
     - Sistem Home teleportasi survival.
   - **Ekonomi Dinamis & Toko Harian:**
     - Dynamic Pricing Engine: Fluktuasi harga beli/jual berdasarkan volume transaksi.
     - Daily Rotating Shop dengan reset otomatis setiap 24 jam game.
     - Perintah `/sellgui` instan untuk penjualan massal hasil pertanian & tambang.
   - **Event Dunia Berkala:** 4 event global (`DOUBLE_XP`, `MYTHIC_MERCHANT`, `DUNGEON_RUSH`, `HARVEST_FESTIVAL`) dengan BossBar countdown real-time dan zero world griefing.

3. **Pemisahan Mandiri Touhou Little Maid Companion & Integrasi YSM (`touhou-little-maid` v2.5.0 / v2.8.0):**
   - **Pemisahan Subproyek:** Mengekstrak seluruh logika dan aset Touhou Little Maid dari `sakura-weapons` ke subproyek mandiri terdedikasi `touhou-little-maid/`.
   - **Universal Bedrock Geometry & Animation Engine:** Mendukung format Bedrock `1.10.0` dan `1.12.0` (Array UV & Per-Face UV box) dengan rendering langsung via native `SubmitNodeCollector.submitCustomGeometry` Mojang 26.2.
   - **Yes Steve Model (YSM) Compatibility (`YsmCompat`):**
     - Model Switch GUI dengan ikon gaun (`👗`) di antarmuka utama maid.
     - Sinkronisasi network roaming variables anim (`speed`, `is_sitting`, `is_sleeping`, `is_holding_item`).
     - Fallback otomatis ke katalog model Bedrock internal jika YSM tidak terpasang.
   - **Kecerdasan Pendamping Maid AI:**
     - Penjinakan menggunakan kue/makanan manis, mode Duduk/Ikuti toggle.
     - Inventori terpasang 36 slot untuk perlengkapan dan hasil kerja maid.
     - AI Auto-Farm (panen & tanam ulang mandiri) dan AI Auto-Fish (mancing otomatis).
     - Maid Revive: Mengorbankan Totem of Undying di inventori maid untuk menyelamatkan pemilik saat sekarat.
   - **Interaksi Kasih Sayang & Ciuman Maid (`KissMaidHandler`):**
     - Shift + Klik Kanan tangan kosong memancarkan partikel hati dan suara kucing serta menambah favorability.
     - 5x interaksi berturut-turut memberikan buff `Maid's Prayer` yang meningkatkan damage dan pertahanan pemilik serta maid.
   - **SubScreen GUI 3D Model Selector:** Preview 3D interaktif yang dapat diputar 360°, galeri 6 per halaman, tombol buka folder model (`.minecraft/maid_models/`), dan instant hot-reload tanpa restart game.

4. **Peluncuran Takasha RPG Web Companion (`takasha-rpg-web`):**
   - Aplikasi web interaktif lokal berbasis Node.js / HTML5 (port `http://localhost:5173/`).
   - Penampil katalog visual 40+ Custom Enchantments, panduan tier Dungeon Rift Keystone, dan simulasi kalkulator stat AuraSkills.

5. **Kepatuhan Arsitektur & SemVer 2.0.0:**
   - Root `VERSION`: **`2.8.0`** (Single Source of Truth).
   - `sakura-weapons`: `versions/26.2` ditingkatkan ke **v2.8.0+26.2**, `versions/1.21.11` ke **v2.6.0+1.21.11**.
   - `takasha-rpg`: Ditingkatkan ke **v1.8.0+26.2**.
   - `touhou-little-maid`: Ditingkatkan ke **v0.8.2+26.2**.

---

## 2. Riwayat Pembaruan Takasha-RPG Singleworld Engine & Samus Parity

38. **Zero-Persistence Transient VFX, Fastutil Crash Immunity, Restorasi Animasi Blazestaff & Orbital Stacks (v2.8.8 / Takasha-RPG v1.8.8+26.2):**
    - **Zero-Persistence Transient VFX Engine:**
      - Dibuat EntityMixin.java yang mencegat metode Entity.save dan mengembalikan nilai false untuk seluruh entitas Display.ItemDisplay bertag takasha_temp_vfx. Seluruh visual effect sementara (meteor, retakan tanah lava, ledakan, proyektil, dan angka damage indikator) kini 100% dicegah agar tidak pernah disimpan ke file chunk region .mca.
    - **Pembersihan Otomatis Ghost Model saat Masuk World (Dynamic Orphan Purge):**
      - Didaftarkan event listener ServerEntityEvents.ENTITY_LOAD pada TakashaModelEngine. Setiap entitas Display.ItemDisplay yang dimuat dari chunk penyimpanan lama akan langsung dideteksi: jika bertag takasha_temp_vfx atau memegang item model ber-namespace takasha_rpg dengan awalan vfx_ atau model awakened_pyromancer yang tidak dikelola aktif oleh TakashaModelEngine, entitas tersebut seketika dieksekusi display.discard(). World kini 100% bersih tanpa sisa model serangan yang membeku saat pemain masuk world/relog.
    - **Hardening Anti-Crash Fastutil (Int2ObjectLinkedOpenHashMap.fixPointers Index -1):**
      - Seluruh operasi pembuangan entitas (display.discard()) di ModelBoneInstance.java kini dijadwalkan secara aman pada antrean thread server utama (server.execute(...)) dengan pengecekan re-entrancy !display.isRemoved(), meniadakan crash fatal ArrayIndexOutOfBoundsException: Index -1 pada saat perombakan chunk atau pembersihan entitas masal.
    - **Restorasi Animasi Held Weapon Blazestaff (20 fps Otentik):**
      - Menghapus file atlas ganda yang rusak (assets/minecraft/atlases/items.json, assets/takasha_rpg/atlases/items.json, dan assets/takasha_rpg/atlases/blocks.json) yang sebelumnya memicu peringatan duplicate sprite dan menonaktifkan ticker animasi sprite pada blocks.png.
      - Memulihkan konfigurasi otentik Samus pada flame_debuff.png.mcmeta dan flame_debuff_e.png.mcmeta menjadi {"animation": {"frametime": 1}} tanpa interpolasi buram. Lidah api di ujung tongkat Blazestaff kini berkobar hidup dan tajam pada 20 fps.
    - **Perbaikan Timer Passive & Heat Mastery 100% Uptime:**
      - Memperbaiki konversi timer kemampuan di MMOInteractionHandler.java dari hitungan detik mentah menjadi tick kemampuan otentik (10 tick = 500ms). Aura Heat_Mastery_AURA (durasi 20 tick = 1.0 detik) kini terus diperbarui setiap 0.5 detik tanpa ada jeda mati.
    - **Dukungan UUID Fallback pada BlockbenchParser untuk Bola Api Orbital:**
      - Menambahkan penanganan fallback UUID jika grup bone pada file .bbmodel memiliki nama kosong (name: ""). Hal ini memungkinkan seluruh 6 bone bola api pada heat_stack_vfx.bbmodel diparsing secara utuh tanpa saling menimpa, sehingga formasi orbital stack 1 s.d. 3 tampil sempurna di sekeliling pemain.
    - **Kepatuhan Versi & Deployment:**
      - Root VERSION ditingkatkan ke 2.8.8.
      - takasha-rpg ditingkatkan ke v1.8.8+26.2.
      - Biner Takasha-RPG-1.8.8+26.2.jar (9.441.627 bytes) berhasil dideploy ke Hasil mod/ dan folder launcher profil Modrinth kaizenmc.id/mods/.

39. **Pemusatan BossBar XP AuraSkills, Aksesibilitas Universal Senjata Samus di Mode Kreatif, dan Patch Mixin ValueOutput (v2.8.9 / Takasha-RPG v1.8.9+26.2):**
    - **Pemusatan Laporan XP Terpadu (BossBar Only):**
      - Mengeliminasi pengiriman teks pesan overlay Actionbar `[AuraSkills] +X XP (Lvl Y - Z/Req)` dari `TakashaRpgManager.giveSkillXp()`.
      - Meniadakan tabrakan visual teks di atas hotbar dengan HUD bar HP & Mana (`TakashaActionbarHud`). Seluruh progres XP pemain kini divisualisasikan secara elegan dan terpusat melalui BossBar dinamis di bagian atas layar (`TakashaXpBossbarManager`).
    - **Aksesibilitas Universal Senjata Samus (Khusus Mode Kreatif):**
      - **Restriksi Mode Kreatif Dipertahankan Penuh:** Seluruh perintah pengambilan senjata Samus/MMO (`/samus`, `/rpg samus`, `/mmo get`, `/mmo give`, klik pada `/mmo list`) diverifikasi secara ketat menggunakan `player.isCreative()`. Pemain dalam mode Survival yang mencoba mengeksekusi perintah ditolak dengan pesan peringatan resmi: `§c[Takasha-MMO] Perintah senjata Samus/MMO hanya dapat digunakan dalam Mode Kreatif!`.
      - **Resolusi Sintaks Brigadier:** Menghilangkan syntax exception pada parser argumen akibat karakter colon (`:`) pada auto-complete (`awakened_pyromancer:awakened_pyromancer_blazestaff`).
      - **Suite Perintah Cepat & Interaktif:**
        - Perintah root `/samus` & `/samus give [item]` langsung memberikan Blazestaff jika pemain dalam Mode Kreatif.
        - Sub-perintah `/rpg samus` di bawah `/rpg`.
        - Perintah `/mmo get <item_id>` dan `/mmo give <item_or_player> [item_id]` dengan dukungan alias singkat (`blazestaff`, `samus`, `awakened_pyromancer_blazestaff`).
        - Daftar interaktif pada `/mmo list` dengan komponen teks yang dapat diklik (`[AMBIL]`) untuk memicu `/mmo get <id>` secara instan.
      - **Pendaftaran ke Creative Mode Tab:** Mendaftarkan Blazestaff (`MMOItemManager.generateItemStack("awakened_pyromancer_blazestaff")`) ke dalam Tab Mode Kreatif `RPG_UTILITY_TAB` di `ModRpgItemGroups.java` sehingga pemain dalam Mode Kreatif dapat mengambilnya langsung dari inventori Creative.
    - **Resolusi Crash Transformasi Mixin Minecraft 26.2 (`EntityMixin`):**
      - Memperbaiki descriptor metode `@Inject(method = "save")` dari `(CompoundTag, CallbackInfoReturnable)` menjadi `(ValueOutput, CallbackInfoReturnable)` serta menambahkan proteksi pada `shouldBeSaved()` untuk entitas bertag `takasha_temp_vfx`.
    - **Kepatuhan Versi & Deployment:**
      - Root VERSION ditingkatkan ke `2.8.9`.
      - takasha-rpg ditingkatkan ke `v1.8.9+26.2`.
      - Biner `Takasha-RPG-1.8.9+26.2.jar` berhasil dikompilasi ke `Hasil mod/` dan dideploy langsung ke folder launcher profil Modrinth `kaizenmc.id/mods/`.

40. **Pembersihan Total Overlay XP Actionbar, Imunitas Crash Basic Attack Spam, dan Restorasi Animasi Blazestaff (v2.8.10 / Takasha-RPG v1.8.10+26.2):**
    - **Pemusatan Total Laporan XP AuraSkills (Eksklusif BossBar Atas Layar):**
      - Menghapus tuntas pengiriman teks overlay actionbar pada saat kenaikan level (`player.sendOverlayMessage`) di `TakashaRpgManager.java`. Area di atas bar darah dan Mana (`TakashaActionbarHud`) kini 100% steril dan bersih dari tumpukan pesan actionbar vanilla.
      - Seluruh visualisasi progres XP dan kenaikan level difokuskan secara elegan dan konsisten pada BossBar dinamis di atas layar (`TakashaXpBossbarManager`), sedangkan perayaan level-up dikirimkan melalui Title tengah layar dan notifikasi chat breakdown.
    - **Imunitas Crash Basic Attack Spam (Netty vs Server Thread Safety):**
      - Memperbaiki race condition `ConcurrentModificationException` (`ArrayList$Itr.checkForComodification` di `EntitySection.lithium$collectPushableEntities`) saat pemain melakukan spam klik kiri / basic attack menggunakan senjata Samus (`Awakened Pyromancer Blazestaff`).
      - Pada `ServerGamePacketListenerImplMixin.java`, injeksi paket `ServerboundSwingPacket` dipindahkan dari `@At("HEAD")` ke `@At("TAIL")` dan dieksekusi secara aman di Server Main Thread (`MinecraftServer.execute(...)`). Hal ini menjamin penambahan entitas model/proyektil Display tidak pernah berbenturan dengan loop tick pergerakan dan dorongan entitas Lithium.
    - **Restorasi Animasi Sprite Api Blazestaff di Tangan (Atlas Stitching & Frame Blending):**
      - Menambahkan atlas texture `items.json` dan `blocks.json` di `assets/minecraft/atlases/` dan `assets/takasha_rpg/atlases/` sehingga sprite animasi `flame_debuff.png` didaftarkan dan di-tick secara berkelanjutan oleh renderer Minecraft 26.2.
      - Mengonfigurasi `flame_debuff.png.mcmeta` dan `flame_debuff_e.png.mcmeta` dengan `frametime: 1` dan `interpolate: true` di seluruh direktori tekstur, menghasilkan kobaran lidah api yang menyala hidup dan bergerak halus pada 20 FPS saat senjata dipegang di tangan pemain.
    - **Aksesibilitas Perintah Senjata Samus Tetap Khusus Mode Kreatif:**
      - Menambahkan rute `/takasha samus` serta `/ae give <player> samus` / `blazestaff` / `awakened_pyromancer_blazestaff` untuk mempermudah pemain kreatif mengambil senjata.
      - Restriksi `player.isCreative()` tetap ditegakkan dengan ketat; pemain survival yang mencoba mengambil senjata ditolak dengan aman.
    - **Kepatuhan Versi & Deployment:**
      - Root VERSION ditingkatkan ke `2.8.10`.
      - takasha-rpg ditingkatkan ke `v1.8.10+26.2`.
      - Biner `Takasha-RPG-1.8.10+26.2.jar` dikompilasi ke `Hasil mod/` dan dideploy langsung ke folder launcher profil Modrinth `kaizenmc.id/mods/`.

41. **Arsitektur Mandiri Komprehensif, Grammar-Aware MythicMobs Parser, Ballistics & Projectile Trajectory Fix, Full 8-Targeter Suite, dan Totem/Potion/Throw Mechanics (v2.8.11 / Takasha-RPG v1.8.11+26.2):**
    - **Grammar-Aware MythicMobs Parser (Bracket-Depth Tracking):**
      - Memperbaiki kegagalan regex targeter saat membaca parameter yang memiliki targeter bersarang, seperti `skill{s=Pyro_Combo_ASH_Missiles;origin=@SelfEyeLocation{fo=-0.5;yo=-0.2}} @EntitiesInCone{a=25;r=17}`.
      - Parser kini menggunakan tokenizer kedalaman kurung kurawal `{...}` untuk mengisolasi parameter mekanik dengan tepat, sehingga parameter `origin` tetap utuh dan targeter luar (`@EntitiesInCone`, `@forward`, `@eir`) diekstrak tanpa terpotong atau korup.
    - **Paritas Penuh Sumber Samus (Reverse-Engineering Analysis):**
      - **Mekanik Totem (`ActiveTotem.java` & `TotemMechanic.java`):** Menyediakan ground totem marker dinamis yang mengeksekusi subskill secara berkala (`onTick` / `oT`) dan saat kadaluarsa (`onExpire` / `oE`), mengembalikan jejak api dan magma eruption pada kemampuan `Magma_Dash`.
      - **Mekanik Potion (`PotionMechanic.java`):** Mengaplikasikan status efek slow/slowness selama pengisian daya sihir pamungkas (`Infernal_Judgement` dan `Firestorm_Volley`).
      - **Mekanik Throw (`ThrowMechanic.java`):** Menghempas musuh ke udara (`velocityY=5`) dan membantingnya kembali ke tanah (`velocityY=-8`) pada kombo `Volcanic_Spear`.
      - **Mekanik Directional Velocity (`DirectionalVelocityMechanic.java`):** Mendorong pemain/target secara terarah berdasarkan yaw bidikan.
      - **Suite Lengkap 8 Targeter MythicMobs:** Mendukung `@self`, `@origin`, `@selfeyelocation`, `@targetedtarget` / `@target`, `@entitiesincone`, `@entitiesnearorigin`, `@forward`, dan `@eir` (`@entitiesinradius`).
    - **Koreksi Konteks Target & Ballistik Homing:**
      - Menginisialisasi konteks target awal sebagai `null` (bukan pemain) saat menembakkan skill ke udara tanpa mob.
      - Memproteksi logika homing misil (`inertia > 0`) di `ActiveProjectile.java` agar hanya mengejar musuh yang hidup (`target != caster && target.isAlive()`).
      - Menyediakan fallback target titik depan (15 blok di depan mata pemain) saat kerucut bidik `@EntitiesInCone` kosong, sehingga misil Pyro Combo dan meteor meluncur lurus ke depan, bukan berbalik menghantam kepala atau kaki pemain.
    - **Penyampaian Damage & Restorasi Variabel Konteks:**
      - Mengalirkan variabel kemampuan (`<skill.damage>`, `burnduration`, `burndamage`) ke dalam proyektil sehingga saat proyektil bentur/meledak, damage dihitung dan diaplikasikan secara penuh melalui `CombatMitigationEngine.executeDamage`.
      - Mengembalikan `InteractionResult.PASS` pada `AttackEntityCallback` sehingga tebasan pedang jarak dekat (melee) tetap memberikan damage fisik bersamaan dengan peluncuran proyektil combo.
    - **Pembersihan Notifikasi Cooldown (Action Bar Throttled):**
      - Mengalihkan pesan peringatan cooldown dari riwayat chat ke Action Bar (`player.sendSystemMessage(msg, true)`) dengan perlindungan rate-limiting 400ms untuk mencegah banjir paket dan spam obrolan.
    - **Kepatuhan Versi & Deployment:**
      - Root VERSION ditingkatkan ke `2.8.11`.
      - takasha-rpg ditingkatkan ke `v1.8.11+26.2`.
      - Biner `Takasha-RPG-1.8.11+26.2.jar` (9.456.728 bytes) berhasil dikompilasi ke `Hasil mod/` dan dideploy langsung ke folder launcher profil Modrinth `kaizenmc.id/mods/`.

42. **VFX Parity Repair, Minecraft Atlas Registration, Missing Impact Texture/Font Alignment, TextDisplay Projectile Engine, dan State Normalization (v2.8.12 / Takasha-RPG v1.8.12+26.2):**
    - **Akar Masalah Kubus Hitam-Ungu (Missing Textures Atlas Resolver):**
      - Pada Minecraft 26.2 / 1.21.4+, atlas jahitan tekstur (`TextureAtlas.LOCATION_BLOCKS`) hanya membaca file konfigurasi standar di `assets/minecraft/atlases/blocks.json` dan `items.json`, serta mengabaikan folder atlas di namespace custom (`takasha_rpg/atlases/`).
      - Sebelumnya, `blocks.json` dan `items.json` hanya mendaftarkan `awakened_pyromancer_texture`. Folder `vfx` dan `awakened_assassin_texture` tidak terdaftar, menyebabkan Minecraft gagal memasukkan seluruh sprite efek ke dalam sheet atlas dan menghasilkan fallback missing texture (kubus/persegi hitam-ungu raksasa) pada setiap tebasan dan proyektil.
      - **Solusi:** Menambahkan direktori `vfx` (`source: "vfx"`, `prefix: "vfx/"`) dan `awakened_assassin_texture` ke dalam `assets/minecraft/atlases/blocks.json` dan `items.json`, serta menghapus folder redundant `assets/takasha_rpg/atlases/`.
    - **Restorasi Asset Font & Tekstur Impak Samus:**
      - Menyalin aset font asli `assets/minecraft/font/default.json` dari `Raw Resource Pack` referensi samus2002.
      - Menyalin seluruh file tekstur bitmap `vfx_dusk_impact_1.png` hingga `vfx_dusk_impact_7.png` ke dalam direktori `assets/minecraft/textures/awakened_assassin_texture/` dan `assets/takasha_rpg/textures/awakened_assassin_texture/`.
      - Pemetaan Unicode `\u1780` s/d `\u1786` kini terdaftar dan di-render secara akurat oleh Font Renderer Minecraft pada tinggi 180 dan ascent 85.
    - **Engine Proyektil TextDisplay & Animasi Impak (`TransientTextDisplay.java`):**
      - Menganalisis definisi `VFX_SAMUS_Impact` di `RPG_Class_Essentials_Mobs.yml` yang bertipe `text_display` (bukan model 3D Blockbench).
      - Menghilangkan warning `Attempted to spawn unknown static model: samus_impact`.
      - Mengimplementasikan `TransientTextDisplay.java` dan accessor invoker `TextDisplayAccessor.java` untuk mengendalikan `setText` dan `setBackgroundColor(0)` (latar transparan).
      - Mengintegrasikan entitas `TextDisplay` dengan billboard `CENTER` dan brightness luminous `(15, 15)` ke dalam `MythicMobManager`, `ProjectileMechanic`, `ActiveProjectile`, dan `SummonMechanic` yang otomatis memutar siklus 7 frame `\u1780`–`\u1786` setiap tick dan membersihkan diri secara otomatis (zero persistence).
    - **Normalisasi Pencocokan State Animasi (`ActiveModelInstance.java`):**
      - Menambahkan resolver cerdas `resolveAnimationName` pada `ActiveModelInstance.playAnimation` untuk menangani variasi penamaan state dari file mob/skill MythicMobs (misal: `slash1` -> `slash_normal1`, `pierce` -> `pierce_normal`).
    - **Audit Kelengkapan Bone Model Presisi:**
      - Melakukan audit menyeluruh terhadap seluruh 40 berkas `.bbmodel` sumber asli. Seluruh 495 visual bone yang memiliki geometri mesh kubus terkonfirmasi memiliki definisi item JSON di `assets/takasha_rpg/items/` dan model JSON di `assets/takasha_rpg/models/vfx/` dengan 2.052 referensi tekstur valid di disk.
    - **Kepatuhan Versi & Deployment:**
      - Root VERSION dinaikkan ke `2.8.12`.
      - Mod `takasha-rpg` dinaikkan ke `v1.8.12+26.2`.
      - Biner `Takasha-RPG-1.8.12+26.2.jar` (9.473.743 bytes) berhasil dikompilasi dan dideploy ke launcher profil Modrinth `kaizenmc.id/mods/` menggantikan versi 1.8.11.

---

## 17. Riwayat Pembaruan Takasha Core v2.8.13+26.2 & v2.6.0+1.21.11 (Universal Auto Drop Engine, Mode Kaizen Camera Auto-Fix, Auto Kill Strafe Expansion & Hardened Auto Eat)

43. **Mode Kaizen Camera Auto-Fix & Recovery, Auto Kill Strafe Expansion, Universal Auto Eat Hardening, dan Scalable Multi-Criteria Auto Drop Engine (v2.8.13+26.2 & v2.6.0+1.21.11 MINOR/FEATURE):**
    - **Mode Kaizen Camera Auto-Fix & Smooth Heading Recovery:**
      - **Akar Masalah:** Saat modul `Auto Fish` dan `Auto Kill` berjalan di server multiplayer mode `KAIZEN`, kamera pemain rentan melenceng dari titik fokus (blok air pemancingan atau bukaan bilik mob grinder) akibat dorongan fisik mob, knockback, pergerakan karakter, maupun intervensi input mouse manual. Pada versi sebelumnya, kamera yang melenceng tidak kembali ke posisi awal, mengakibatkan bot melempar joran ke darat atau menebaskan senjata ke dinding kosong.
      - **Pemulihan Hadap Kamera Otomatis (`TakashaFishingHandler.java`):** Saat mode KAIZEN aktif dan pelampung joran aktif, sistem mencatat baseline pitch & yaw awal. Jika deviasi sudut hadap melampaui ambang batas (> 5.0°), sistem secara halus menginterpolasi rotasi kamera pemain (~2.5°/tick) kembali ke titik tengah air secara bertahap dan natural tanpa sentakan sudut instan (*anti-teleport snap*).
      - **Penguncian Kamera Mob Grinder (`TakashaMobKillHandler.java`):** Ditambahkan opsi konfigurasi `kaizenCameraFix` (ON/OFF). Ketika mode KAIZEN aktif, sistem mengunci orientasi hadap awal pemain ke bukaan mob grinder dan memulihkan sudut bidikan secara halus ketika terjadi drift atau rotasi abnormal.
    - **Mode Kaizen Auto Kill Player Strafe Expansion (Pergerakan Kanan-Kiri Dinamis):**
      - **Otomasi Gerak Kanan-Kiri:** Menambahkan subsistem pergerakan karakter dinamis untuk mencegah deteksi AFK pasif server dan menyapu mob di grinder yang bergeser ke sudut samping:
        - `kaizenStrafeEnabled` (ON/OFF): Toggle aktivasi fitur strafe.
        - `kaizenStrafeDurationMs` (100 ms s.d. 500 ms, step 50 ms): Mengatur durasi penekanan tombol gerak per langkah.
        - `kaizenStrafeIntervalMs` (0 ms / Kontinu s.d. 5000 ms, step 500 ms): Mengatur jeda waktu hening antar siklus langkah.
        - `kaizenStrafePattern`: 4 pola gerak yang dapat dipilih (`ALTERNATING` / Kanan-Kiri Bergantian, `LEFT_ONLY` / Kiri Saja, `RIGHT_ONLY` / Kanan Saja, `RANDOM_JITTER` / Langkah Acak Mikroskopis).
        - `kaizenAutoSneak` (ON/OFF): Otomatis menahan tombol jongkok (`Sneak`) selama strafe berlangsung untuk menjamin pemain tidak tergelincir jatuh dari tepi blok/slab grinder.
        - `kaizenCameraFix` (ON/OFF): Memulihkan sudut pandang ke garis bidik grinder secara berkelanjutan.
      - **GUI Eksklusif Mode KAIZEN:** Seluruh opsi konfigurasi strafe ini diatur secara kondisional pada `TakashaMobKillSubScreen.java` dan **hanya muncul (dirender & interaktif)** ketika `killEngineMode == EngineMode.KAIZEN`. Jika mode dialihkan ke `VANILLA`, kontrol strafe disembunyikan secara otomatis untuk menjaga kebersihan visual antarmuka.
      - **Dispatcher Input Anti-AFK:** `TakashaMobKillAntiAfk.java` direkayasa ulang untuk mengendalikan `client.options.keyLeft`, `keyRight`, dan `keyShift` secara aman dan presisi dengan pelepasan state tombol terjamin saat otomasi dinonaktifkan.
    - **Universal Auto Eat Engine Hardening & Mutex Isolation:**
      - **Penegakan Otomatis (Auto-Enforcement):** Modul auto eat kini dijamin otomatis aktif dan memprioritaskan makanan saat salah satu mode otomasi (`Auto Fish`, `Auto Kill`, `Auto Farm`) sedang berjalan (`isAnyAutoActive()`), mencegah pemain kelaparan dan mati saat ditinggal AFK.
      - **Startup Grace Window (500 ms):** Diberikan jeda toleransi 500ms saat startup/transisi aksi sebelum memulai konsumsi makanan untuk mencegah tabrakan eksekusi dengan inisiasi ayunan senjata atau lemparan joran.
      - **Mutex Guard dengan Rod Slip (`RodSlipHandler.java`):** Ditambahkan pengecekan status `TakashaAutoEatHandler.isEating()`. Saat proses makan sedang berlangsung, solver Rod Slip ditangguhkan sementara agar paket klik kanan makan tidak terinterupsi oleh interaksi joran pancing.
      - **Restorasi Slot Hotbar & Hand State:** Setelah proses makan selesai, item di tangan dan slot hotbar asal dikembalikan secara instan dengan verifikasi status paket, mencegah ghost item dan desinkronisasi inventaris klien-server.
    - **Universal Auto Drop Inventory System (Scalable Multi-Criteria Engine):**
      - **Pemisahan 3 Kategori Independen:**
        1. `MINING`: Khusus membuang hasil galian tambang yang memenuhi inventori (cobblestone, dirt, andesite, diorite, granite, gravel, tuff, deepslate, dll).
        2. `FISHING`: Khusus membuang sampah hasil memancing (leather boots, bowl, rotten flesh, string, water bottle, stick, dll).
        3. `MOB_KILL`: Khusus membuang drop mob yang tidak diinginkan (rotten flesh, bone, spider eye, poisonous potato, glass bottle, dll).
      - **Scalable Multi-Criteria Matching (`DropFilterEntry.java`):**
        - Mendukung penyaringan fleksibel untuk item Minecraft vanilla, item mod, serta item custom dari server (ItemsAdder, Oraxen, Takasha-RPG):
          - **Item ID:** Berdasarkan identifier namespace (misal: `minecraft:cobblestone`, `custom_mod:item_id` dengan fallback otomatis `minecraft:`).
          - **CustomModelData:** Sintaks `#cmd` (misal: `#10042` atau `paper#10042`) untuk menyaring item server yang dibedakan lewat angka CustomModelData.
          - **Display Name Match:** Sintaks `name:"..."` (misal: `name:"Batu Biasa"` atau `name:"Ikan Busuk"`) untuk mencocokkan nama kustom item tanpa terikat pada ID item dasarnya.
      - **Quick-Add dari Item yang Dipegang:** Tombol "Tambah Item Tangan" membaca item yang sedang dipegang di main-hand pemain beserta CustomModelData dan Custom Name-nya secara otomatis dan langsung memasukkannya ke daftar filter kategori aktif.
      - **Safety Net Proteksi Item Berharga (Valuables Protection):**
        - Sistem secara ketat menolak membuang item ber-enchantment (`isEnchanted()`) dan item tier berharga (Netherite, Diamond, Golden Apple, Elytra, Totem of Undying, Shulker Box, dll) untuk mencegah kerugian fatal pemain akibat kesalahan konfigurasi filter.
      - **Verifikasi Status `/drop` Kaizen Server:**
        - Pada server KaizenMC, drop item diproteksi oleh toggle perintah `/drop`.
        - `TakashaClientPacketListenerMixin.java` memantau pesan chat server. Jika server merespons bahwa fitur drop terkunci, klien secara otomatis memverifikasi atau mengeksekusi `/drop` untuk membuka proteksi sebelum siklus pembuangan item dijalankan.
      - **GUI SubScreen Terpadu (`TakashaAutoDropSubScreen.java`):**
        - Desain panel 420x300 piksel dengan 3 tab navigasi (`[Mining]`, `[Fishing]`, `[Mob Kill]`), input text field responsif, tombol Hapus filter `[X]`, tombol toggle ON/OFF per kategori, dan tombol Quick-Add Item Tangan.
        - Akses instan disematkan langsung pada Dashboard Utama (`K`), `TakashaFishingSubScreen`, dan `TakashaMobKillSubScreen`.
    - **Kepatuhan Multi-Version & Deployment:**
      - Teruji dan terkompilasi bersih 100% pada kedua platform:
        - `sakura-weapons/versions/26.2`: Minecraft 26.2 Unobfuscated Mojang Mappings, Java 25 LTS.
        - `sakura-weapons/versions/1.21.11`: Minecraft 1.21.11 Intermediary Mojang Mappings, Java 21 LTS.
      - Biner terbitan berhasil dibangun ke `Hasil mod/Takasha-2.8.13+26.2.jar` dan `Hasil mod/Takasha-2.6.0+1.21.11.jar`.

44. **Resolusi Loop Command /drop Anti-Kick Server Kaizen, Mesin Auto Drop Pintar Menyisakan 1 Slot Makanan (Smart Food Reserve), dan Status Guard Unsupported (v2.8.14+26.2 & v2.6.1+1.21.11 PATCH/FEATURE):**
    - **Resolusi Loop Command /drop & Pencegahan Kick Server:**
      - **Akar Masalah:** Pesan server Kaizen `| No Drop dimatikan.` (drop aktif/diizinkan) dan `| No Drop dinyalakan` (drop dicegah/terkunci) tidak cocok dengan parser kata kunci lama (`diaktifkan`/`aktif`). Akibatnya status drop tidak pernah dikenali sebagai aktif dan klien mengirimkan `/drop` berulang-ulang setiap 2 detik hingga pemain ter-kick karena command spam. Selain itu, pada sub-server/lobby seperti Tycoon yang tidak mendukung perintah `/drop`, sistem membanjiri chat dengan `Command invalid atau tidak ada permission.`.
      - **Parser Pesan Chat Server Presisi:** Mengidentifikasi secara akurat string `no drop dimatikan` (aktif) dan `no drop dinyalakan` (terkunci), serta mendeteksi pesan `Command invalid atau tidak ada permission.` untuk menetapkan status `UNSUPPORTED`.
      - **Anti-Spam & Rate-Limit Backoff:** Membatasi percobaan pengiriman `/drop` maksimal 2 kali dengan jeda minimum 3000ms dan backoff 30 detik jika server tidak merespons, mencegah risiko kick 100%. Jika status `ACTIVE` atau `UNSUPPORTED`, perintah `/drop` sama sekali tidak akan dikirim ulang.
      - **Pemicu Berbasis Kebutuhan Riil (Demand-Driven):** Perintah `/drop` kini HANYA dikirimkan jika inventori pemain benar-benar memiliki item valid yang siap dibuang, bukan secara buta di awal tick saat tas masih bersih.
    - **Mesin Auto Drop Pintar Menyisakan 1 Slot Makanan (Smart Food Reserve):**
      - **Proteksi Kelaparan Auto Eat:** Menambahkan opsi `autoDropSmartFoodReserve` (default: ON).
      - **Reservasi 1 Slot Makanan:** Secara cerdas mendeteksi item bertipe makanan (`DataComponents.FOOD` / `isFood()`). Sistem menjamin tas pemain selalu **menyisakan minimal 1 slot makanan di inventori** (berapapun jumlah isinya $\ge 1$, misal 12 Roti, 5 Daging Rebus, atau 1 Sup Jamur unstackable) sebagai cadangan makanan `AutoEat`. Hanya slot makanan tambahan/berlebih yang dibuang jika terdaftar di filter drop.
      - **Kontrol GUI SubScreen:** Menyediakan tombol interaktif `[Sisakan 1 Slot Makanan: ON/OFF]` pada `TakashaAutoDropSubScreen`.
    - **Kepatuhan SemVer & Multi-Version:**
      - Root VERSION: `2.8.14`.
      - Cabang modern `versions/26.2` ditingkatkan ke **`v2.8.14+26.2`**.
      - Cabang LTS `versions/1.21.11` ditingkatkan ke **`v2.6.1+1.21.11`**.
      - Biner terbitan dikompilasi ke `Hasil mod/Takasha-2.8.14+26.2.jar` dan `Hasil mod/Takasha-2.6.1+1.21.11.jar`.

45. **Purge Senjata Samus Awakened Pyromancer, Transformasi Arsitektur RPG Items, GUI Suite 54-Slot Eksklusif Mode Kreatif, dan Integrasi Hadiah Dungeon (v2.9.0 / takasha-rpg v1.9.0):**
    - **Pembersihan Total Senjata Samus:**
      - Menghapus seluruh aset visual, audio, model 3D Blockbench, tekstur, file definisi item `awakened_pyromancer_blazestaff`, dan pack `awakened_pyromancer`.
      - Menghapus perintah `/samus`, sub-command `/rpg samus`, serta seluruh hardcoding nama skill Samus di `MMOInteractionHandler.java`, `MMOCommand.java`, `TakashaRpgCommands.java`, `TakashaPackManager.java`, dan `ModRpgItemGroups.java`.
    - **Arsitektur NBT & Format Lore RPG Items Presisi:**
      - Mengadopsi NBT ganda kompatibel standar RPG (tag kunci tipe item, item ID, tier, autosmelt, bouncing crack, pickaxe power, max durability, dan breakability) berdampingan dengan NBT terstruktur `TakashaMMOItem`.
      - Format Lore otentik terstruktur mengikuti tata letak standar RPG: Header kategori `#item-type#`, stat bar pembatas, persyaratan AuraSkills/Level, atribut fisik & mining (`Pickaxe Power`, `Mining Speed`), mekanika khusus (`Auto-Smelt`, `Bouncing Crack`), durabilitas kustom, dan footer tier kelangkaan.
    - **GUI Suite 54-Slot Bebas Flicker (ReadOnlyChestMenu):**
      - `MMOTypeBrowserMenu`: Menampilkan kategori item (TOOL, SWORD, AXE, ARMOR, BOW, CROSSBOW, CONSUMABLE, GEM_STONE, ACCESSORY, CATALYST, ALL) beserta penghitung item terdaftar.
      - `MMOItemBrowserMenu`: Menampilkan preview item dalam grid 21 slot (Left-click ambil 1x, Shift-left-click ambil 64x, Right-click buka editor).
      - `MMOItemEditionMenu`: Live item preview di Slot 4, opsi pengubahan nama, lore, material, model, tier, tool passives, serta durability & stats secara real-time.
      - Primary command `/mi` dan alias `/mmo` (`/mi browse`, `/mi edit <type> <id>`, `/mi create <type> <id>`, `/mi get`, `/mi give`, `/mi list`, `/mi reload`) diproteksi khusus **Mode Kreatif**.
    - **Ekosistem & Integrasi Gameplay:**
      - Perkakas RPG terintegrasi dengan pemecahan blok di `TakashaEnchantUtilityEngine.java`: `autosmelt` melebur ore langsung ke ingot + partikel lava, `bouncing-crack` memecah hingga 8 blok sejenis di radius 3x3x3.
      - Pengecekan syarat level AuraSkills (`required-mining`, `required-farming`, `required-woodcutting`) dengan efek suara `VILLAGER_NO` jika pemain belum memenuhi syarat.
      - Paket konfigurasi default 15 item RPG (`tool.yml` dengan 8 perkakas, `axe.yml` dengan 4 kapak, `sword.yml` dengan 3 pedang).
      - Integrasi perolehan item di survival secara murni melalui **Hadiah Dungeon** (`DungeonRewardAccumulator.java`): Wave 3 (Common/Uncommon), Wave 4 (Rare), Wave 5 (Epic/Mythical/Awakened).
    - **Kepatuhan SemVer & Metadata:**
      - Root VERSION: `2.9.0`.
      - Mod `takasha-rpg`: `v1.9.0+26.2`.

46. **Resolusi Konfigurasi /drop Gatekeeper Mod Takasha, Paritas Penuh 35 Modul Arsitektur RPG Items, dan Peningkatan SemVer (v2.9.1 / Takasha-RPG v1.9.1 / Takasha v2.8.15 & v2.6.2):**
    - **Resolusi Konfigurasi /drop Gatekeeper Mod Takasha (sakura-weapons):**
      - **Akar Masalah:** Logika `TakashaAutoDropHandler.java` sebelumnya otomatis mengirimkan perintah `/drop` ke server jika `isKaizen` bernilai `true` (`fishingMode == KAIZEN || mobKillMode == KAIZEN`). Hal ini membingungkan pengguna karena tidak tersedia tombol atau konfigurasi eksplisit untuk menonaktifkan fitur tersebut, dan tombol pada GUI hanya bertuliskan `"Uji /drop Server"`.
      - **Toggle Eksplisit `serverDropGatekeeperEnabled`:** Menambahkan opsi konfigurasi `public boolean serverDropGatekeeperEnabled = false;` di `TakashaMasterConfig.java` (default: FALSE agar aman dan tidak memicu pengiriman perintah yang tidak diinginkan).
      - **Kontrol GUI Ramah Pengguna:** Mengubah tombol di `TakashaAutoDropSubScreen.java` menjadi tombol toggle interaktif `Gatekeeper /drop: [AKTIF / NONAKTIF]` lengkap dengan tooltip penjelasan transparan: *"Jika NONAKTIF, mod sama sekali tidak akan mendeteksi atau mengirimkan perintah /drop ke server. Item akan dibuang secara lokal tanpa verifikasi server."*
      - **Bypass Total Saat Dinonaktifkan:** Pada `TakashaAutoDropHandler.java`, jika opsi ini `false`, seluruh pemeriksaan dan pengiriman perintah `/drop` langsung dilewati, memungkinkan proses pembuangan item berjalan murni secara lokal tanpa interaksi server.
      - **Dukungan Multi-Version:** Diterapkan secara sinkron pada cabang modern `sakura-weapons/versions/26.2` dan cabang LTS `sakura-weapons/versions/1.21.11`.
    - **Paritas Penuh 35 Modul Arsitektur RPG Items (takasha-rpg):**
      - **Audit Forensik Bytecode:** Memetakan seluruh 35 modul fungsional sistem item RPG ke dalam implementasi Fabric 26.2 native.
      - **Commands & Revision-ID Parity:** Command tree `/mi` lengkap (`create`, `edit`, `copy`, `delete`, `browse`, `give`, `giveall`, `take`, `drop`, `reload`, `update`, `stations`, `revid`) dan penanganan auto-update template item di inventori berbasis NBT revision-id.
      - **Suite GUI 54-Slot & Asinkronus Chat Edition:**
        - `TypeBrowser`: 21 slot kategori aktif (`10-16`, `19-25`, `28-34`) dengan hitungan item dan link ke `ItemBrowser`.
        - `ItemBrowser`: Navigasi halaman Slot 18 & 26, tombol Delete Mode Slot 47, tombol Back Slot 49, dan tombol Create New Slot 51 yang memicu prompt pembuatan ID item baru.
        - `ItemEdition`: Slot 2 `CHEST` ("&a➔ Get the Item! ➔" dengan klik kiri claim dan klik kanan reroll), Slot 4 Live Item Preview (`getCachedItem()`), Slot 6 `BARRIER` ("&a➔ Back"), Slot 27 & 35 Paginasi, serta 21 slot stat dengan placeholder `GRAY_STAINED_GLASS_PANE` ("&c- No Item Stat -").
        - `MMOChatEditionEngine`: Mengintersepsi event chat server, menutup GUI, mengirim strip kuning `&e&m-----------------------------------------------------`, title layar `See chat.`, serta menangani pembatalan dengan mengetik `cancel`.
      - **Data, Stats & Ekosistem Lanjutan:**
        - NBT dual-layer Spigot + CompoundTag, roll modifiers dinamis, 122 stat classes, dan custom durability.
        - Generator lore modular sesuai `lore-format.yml`, Crafting Stations (`/mi stations open`), resep kustom, Tier kelangkaan, Gem Sockets, Item Sets (2/4/6 potong), Soulbound Levels 1-3, dan partikel Lootsplosion.
        - Kompatibilitas level profesi AuraSkills dan custom enchantment AdvancedEnchantments.
    - **Kepatuhan SemVer & Matriks Versi Rilis:**
      - Root VERSION: **`2.9.1`**.
      - Mod `sakura-weapons/versions/26.2`: **`v2.9.1+26.2`**.
      - Mod `sakura-weapons/versions/1.21.11`: **`v2.6.2+1.21.11`**.
      - Mod `takasha-rpg`: **`v1.9.1+26.2`**.

47. **Paritas Faktual Sistem RPG Items (Detail per GUI Tiap Command & Dynamic Follow-up State), Mesin Persistensi Disk YAML, dan Optimasi Antigravity Anti-100% CPU (v2.9.2 / Takasha-RPG v1.9.2+26.2 / Takasha v2.9.2+26.2):**
    - **Optimasi Workspace Antigravity IDE (Anti-100% CPU):**
      - Mengonfigurasi `.vscode/settings.json` dengan `files.watcherExclude`, `search.exclude`, dan `java.import.exclusions` terhadap folder `referensi-rp/**` dan `Hasil mod/**`.
      - Mematikan auto-indexing berlebih dengan `java.autobuild.enabled: false`, `java.maxConcurrentBuilds: 1`, serta menyematkan argumen JVM `-XX:+UseSerialGC -Xmx768M -XX:MaxMetaspaceSize=256M`. Beban CPU host saat membuka workspace langsung turun di bawah 10%.
    - **Mesin Persistensi Disk YAML (`MMOItemDiskManager`):**
      - Mengimplementasikan serialisasi dan deserialisasi format YAML otentik item RPG ke direktori fisik `config/takasha/rpg_items/item/<TYPE>.yml`.
      - Perintah `/mi create`, `/mi copy`, dan `/mi delete` (maupun Delete Mode di GUI) terhubung langsung dengan file disk fisik secara real-time.
      - `/mi revid` menyimpan kenaikan revision ID template ke disk, dan `/mi reload` memuat ulang seluruh pack bawaan jar dan konfigurasi disk kustom.
    - **Rekonstruksi Penuh GUI Suite Tiap Command:**
      - `MMOTypeBrowserMenu`: 40 item types resmi sistem RPG (23 Root Types + 17 Sub-Types + ALL) tanpa kategori artifisial, lengkap dengan paginasi 2 halaman (Slot 18 & 26), Slot 49 Pack Info, dan auto-fill `GRAY_STAINED_GLASS_PANE` (`- No Type -`).
      - `MMOItemBrowserMenu`: Paginasi multi-halaman Slot 18 & 26, auto-fill `GRAY_STAINED_GLASS_PANE` (`- No Item -`), Slot 47 Delete Mode Toggle dengan penyesuaian judul dinamis (`Item Explorer: <TYPE>` vs `Delete Mode: <TYPE>`), Slot 49 Barrier Back, dan Slot 51 Anvil New Item chat edition.
      - `MMOItemEditionMenu`: Paginasi multi-halaman Slot 27 & 35 dengan pemfilteran stat dinamis per tipe item (`ModifierSource`), format lore tombol terstandarisasi (`Current Value: ...`, `➔ Left click to change`, `➔ Right click to reset`), live preview Slot 4 (`getCachedItem()`), Slot 2 Chest claim/reroll, Slot 6 Barrier back, dan zero-navigation loss (membuka kembali halaman yang sama persis setelah chat input).
      - Sub-Menu Suite: `MMOAbilityEditionMenu`, `MMOEnchantEditionMenu`, dan `MMOGemSocketMenu` (pengaturan warna dan jenis soket permata).
      - `CraftingStationMenu`: 2-tier GUI untuk `/mi stations open <station_id>` (Tier 1: 54 slots station view dengan antrian perakitan slots `[38..42]`; Tier 2: 45 slots preview dengan matriks 3x3 bahan, slot output 24, dan tombol konfirmasi emerald slot 34).
    - **Kepatuhan SemVer & Matriks Versi Rilis:**
      - Root VERSION: **`2.9.2`**.
      - Mod `sakura-weapons/versions/26.2`: **`v2.9.2+26.2`**.
      - Mod `sakura-weapons/versions/1.21.11`: **`v2.6.2+1.21.11`**.
      - Mod `takasha-rpg`: **`v1.9.2+26.2`**.

48. **Paritas Faktual GUI RPG Items (Audit Forensik 186 Stat, Eliminasi Polusi Border Kaca & Judul Tertinggal), Porting 216 Item Bawaan (34 File YAML), dan Matriks Pengujian Stat 7-Layer (v2.9.3 / Takasha-RPG v1.9.3+26.2):**
    - **Audit Forensik & Eliminasi Anomali Gambar 1 (MMOItemEditionMenu):**
      - **Packet Judul Layar:** Memperbaiki bug packet di mana `ReadOnlyChestMenu` me-reuse layar tanpa mengirimkan `ClientboundOpenScreenPacket` baru. Kini judul layar diperbarui secara mulus dan konsisten:
        - `Type Browser` pada `/mi browse`.
        - `Item Explorer: <TYPE>` pada `/mi browse <type>`.
        - `Item Edition: <ITEM_ID>` pada `/mi edit <type> <id>` atau klik kanan item.
      - **Eliminasi Polusi Kaca Abu-Abu (Glass Border Removal):** Menghapus loop pengisian 54-slot kaca abu-abu. Seluruh slot non-grid (Slot 0, 1, 3, 5, 7, 8, 9..17, 18, 26, 36, 44, 45..53) kini **AIR MURNI (`ItemStack.EMPTY`)** sehingga tekstur GUI chest vanilla transparan alami terlihat alami dan transparan. Placeholder `§c- No Item Stat -` HANYA berada pada slot kosong di dalam 21 grid stat (`[19..25, 28..34, 37..43]`).
      - **Bilah Kontrol Item Explorer Presisi:** Slot 47 `CAULDRON` (`Delete Item`), Slot 49 `ARROW` (`Back`), Slot 51 `WRITABLE_BOOK` (`Create New`), dan Slot 45 `HOPPER` (Download Resourcepack khusus block).
    - **Katalog 186 Stat Bytecode & Ikon Vanilla Otentik (`MMOStatRegistry`):**
      - Memetakan 186 stat distinct lengkap dengan ikon material vanilla otentik (`IRON_SWORD` untuk Attack Damage, `LIGHT_GRAY_DYE` untuk Attack Speed, `BLAZE_POWDER` untuk Abilities, `SLIME_BALL` untuk Elements, `ANVIL` untuk Unbreakable, `DIAMOND` untuk Tier, `FLINT` untuk Upgrade, `COAL` untuk Autosmelt, `COBBLESTONE_WALL` untuk Bouncing Crack, `PAPER` untuk Can-Identify/Can-Deconstruct).
      - Paginasi multi-halaman dinamis berbasis kompatibilitas tipe item: Halaman 1 (21 stat) memuat Slot 35 (`Next Page ►`), Halaman 2 memuat Slot 27 (`◄ Previous Page`).
    - **Porting Lengkap 216 Item Bawaan RPG (34 File YAML):**
      - Seluruh 34 file konfigurasi bawaan (`default/item/*.yml`) diekstrak utuh ke `takasha/rpg_packs/default_items/item/`.
      - Mendaftarkan auto-load pack `default_items` pada `TakashaPackManager.java` sehingga seluruh 216 item langsung aktif di `MMOItemManager`.
      - Menyertakan fungsionalitas operasional item: Set Zirah Dragon (kebal api, nafas naga), Set Steel (knockback resistance), Set Spellcaster (mana pool & spell vampirism), Falcon Blade (Life Ender), Soulstealer (Vampirism lifesteal), Amethyst Sword (Grand Heal), Frozen Blade (Frozen Aura), Autosmelt Pickaxe, Crack Pickaxe (bouncing crack 3x3x3), Scroll of Identification (drag-to-identify), Item Crusher (drag-to-deconstruct), dan socketing Gem Stones.
    - **Matriks Pengujian Stat 7-Layer (7-Layer Verification Matrix):**
      - Mengadopsi matriks pengujian komprehensif: Layer 1 (`/mi stats` character sheet), Layer 2 (Combat Training Dummy `DamageDummyEntity` + Action Bar feedback), Layer 3 (PoE mitigation & fall/fire damage), Layer 4 (Autosmelt & Bouncing Crack mining), Layer 5 (Drag-and-drop inventory interaction), Layer 6 (Threshold 4/4 set bonus), dan Layer 7 (Automated headless unit tests via Gradle).
    - **Kepatuhan SemVer & Matriks Versi Rilis:**
      - Root VERSION: **`2.9.3`**.
      - Mod `sakura-weapons/versions/26.2`: **`v2.9.3+26.2`**.
      - Mod `sakura-weapons/versions/1.21.11`: **`v2.6.2+1.21.11`**.
      - Mod `takasha-rpg`: **`v1.9.3+26.2`**.

49. **Paritas Chat Edition & Stat Edition Input, Zero-Stat Pure Item Creation Contract, dan GUI Dinamis Manajemen Hadiah Dungeon (v2.9.4 / Takasha-RPG v1.9.4+26.2):**
    - **Paritas Input Chat Edition (`MMOChatEditionEngine.java` & `MMOItemEditionMenu.java`):**
      - Mengadopsi arsitektur input stat dan penanganan sesi input pemain:
        - Banner dekoratif atas tunggal coret kuning `§e§m-----------------------------------------------------` tanpa garis bawah.
        - Prefix resmi RPG: `§8[§eTakasha-RPG§8] §7`.
        - Animasi screen title: Judul `§6§lItem Edition` dan subtitle `See chat.` (FadeIn: 10, Stay: 40, FadeOut: 10).
        - Baris instruksi pembatalan resmi: `§8[§eTakasha-RPG§8] §7Type 'cancel' to abort editing.`.
        - Mesin toleransi kesalahan (Fault-Tolerant Session): Jika input teks tidak valid, sesi chat TIDAK diputus; pemain dapat langsung mengetik ulang input yang benar di chat.
        - Pembatalan senyap (Silent Cancel): Mengetik `cancel` akan menutup sesi dan membuka kembali inventaris tanpa pesan error spam.
    - **Interaksi Stat Edition Terstandarisasi:**
      - Klik Kiri (`PICKUP_ALL`): Membuka sesi prompt chat dengan instruksi dan contoh konkret (bukan menambah nilai di tempat).
      - Klik Kanan (`PICKUP_HALF`): Menghapus/meniadakan nilai stat (`null`), mengirim notifikasi `Successfully removed <StatName>.` dan mengembalikan tampilan nilai ke `§a---` (numerik) atau `§cNone` (teks/tier).
      - Stat Boolean (Unbreakable, Will Break): Klik kiri men-toggle status `true` / `null`, Klik kanan membuka prompt peluang persentase.
    - **Zero-Stat Pure Item Creation Contract:**
      - Mengadopsi alur pembuatan item murni:
      - Pembuatan item baru melalui perintah `/mi create <TYPE> <ID>` maupun tombol GUI `[➕ Create New]` HANYA menyetel `base.material`. Seluruh stat dasar (`attack-damage`, `attack-speed`, `tier`, `lore`, `display-name`) bernilai `null` (kosong).
      - Tampilan lore stat kosong di GUI: numerik menampilkan `§7Current Value: §a---`, sedangkan teks dan tier menampilkan `§7Current Value: §cNone`.
      - Pemetaan material default 40 tipe item RPG sesuai standar otentik (SWORD -> IRON_SWORD, DAGGER -> STONE_SWORD, SPEAR -> TRIDENT, dll.).
    - **Antarmuka Interaktif Manajemen Hadiah Dungeon (`DungeonRewardManagerMenu.java` & `DungeonRewardConfigManager.java`):**
      - Antarmuka GUI 54 slot yang dapat diakses oleh OP/Gamemaster via `/dungeon rewards [tier] [wave]` dan alias `/dungeon loot`:
        - Tab Tier (Row 0): Common (x1), Nightmare (x2), Sakura Boss (x4).
        - Tab Wave (Row 1): Wave 1 s.d. Wave 5 Final Boss.
        - Grid Drop (Row 2..4): 27 slot loot dengan indikator peluang drop (%), rentang kuantitas, status multiplier scaling, dan resource ID.
        - Kontrol & Aksi (Row 5): Tambah item baru (dari mainhand atau ketik resource ID/MMOItem di chat), Halaman Sebelum/Berikutnya, Reset Wave ke Default, dan Ringkasan Drop ke chat.
        - Simulasi Roll 1x (`[🎲 Simulasi Roll]` di Slot 6): Mengundi loot secara instan dan mencetak daftar drop ke chat.
        - Penyimpanan Persisten JSON: Seluruh konfigurasi disimpan ke `config/takasha/dungeon_rewards.json` dan didukung hot-reload via `/dungeon reload`.
    - **Kepatuhan SemVer & Matriks Versi Rilis:**
      - Root VERSION: **`2.9.4`**.
      - Mod `sakura-weapons/versions/26.2`: **`v2.9.4+26.2`**.
      - Mod `takasha-rpg`: **`v1.9.4+26.2`**.

50. **Paritas Sub-Menu GUI Stat (Elements, Abilities, Enchants, Gem Sockets, Upgrade, Lore, Tier), Anti-Cursor Reset In-Place Screen Update, Restrukturisasi 3-Dungeon Selector, Resolusi Custom Model Data Hadiah Dungeon, dan Automated Combat Realization (v2.9.5 / Takasha-RPG v1.9.5+26.2):**
    - **Sub-Menu GUI Stat Komposit ("Kaya Element") Terpadu:**
      - Menghapus total notifikasi dead-end `"Stat dikonfigurasi melalui config!"`.
      - `MMOElementsEditionMenu`: Antarmuka sub-menu 27-slot mengelola 6 Elemen RPG (Fire, Ice, Lightning, Earth, Light, Darkness) untuk Damage dan Defense dengan dukungan input prompt chat dan pembersihan klik kanan.
      - `MMOTierEditionMenu`: Sub-menu pemilih tingkatan rarity (Common s.d. Mythic).
      - `MMOLoreEditionMenu`: Sub-menu editor baris deskripsi lore visual (tambah baris via chat, hapus baris tertentu, bersihkan seluruh lore).
      - Penyambungan sub-menu eksisting (`MMOAbilityEditionMenu`, `MMOEnchantEditionMenu`, `MMOGemSocketMenu`) secara langsung dari grid stat utama.
    - **Anti-Cursor Reset In-Place Screen Update (`ReadOnlyChestMenu.openOrUpdate`):**
      - Mencegah pergeseran kursor mouse ke tengah layar saat pergantian halaman dan navigasi sub-menu berukuran baris sama dengan memperbarui slot kontainer secara in-place dan memanggil `broadcastChanges()`, meniadakan pengiriman ulang `ClientboundOpenScreenPacket`.
    - **Format Input DoubleStat Otentik Presisi:**
      - Mengirimkan tepat 3 baris instruksi format + 1 baris cancel prompt sesuai standar DoubleStat:
        - Format 1: Angka desimal/tunggal (`25.0`).
        - Format 2: Formula 6-parameter `{Base} {Scaling} {Spread} {MaxSpread} {HardMin} {HardMax}`.
        - Format 3: Rentang acak seragam `{Min} -> {Max}`.
    - **Restrukturisasi 2-Tahap Hadiah Dungeon & Resolusi Model 3D:**
      - `DungeonTierSelectorMenu`: Menu pertama saat menjalankan `/dungeon rewards` menampilkan 3 pilihan dungeon (`Common`, `Nightmare`, `Sakura Boss`).
      - Di dalam `DungeonRewardManagerMenu`, slot 45 dialokasikan untuk tombol `[◀ Kembali ke Pemilih Dungeon]`.
      - Mengatasi masalah Barrier Block pada item default melalui aliasing namespace `takasha:` ke `takasha_rpg:`.
      - Menambahkan preservasi `CUSTOM_MODEL_DATA` dan `ITEM_MODEL` pada `DungeonLootEntry` sehingga item kustom yang ditambahkan dari mainhand mempertahankan model 3D dan tekstur aslinya.
    - **Combat Realization & Automated Verification:**
      - Pendaftaran `Attributes.ATTACK_DAMAGE` dan `Attributes.ATTACK_SPEED` pada `MMOItemManager.generateItemStack` melalui `DataComponents.ATTRIBUTE_MODIFIERS`.
      - Hook `MMOCombatEngine` pada `ServerLivingEntityEvents.ALLOW_DAMAGE` untuk kalkulasi `CRITICAL_STRIKE`, `FIRE_DAMAGE`, `ICE_DAMAGE`, `LIFESTEAL`, `PVE_DAMAGE`, `UNDEAD_DAMAGE`, dan `DEFENSE`.
      - Penyusunan matriks verifikasi terotomatisasi per-stat per-tipe pada `task.md`.
    - **Kepatuhan SemVer & Matriks Versi Rilis:**
      - Root VERSION: **`2.9.5`**.
      - Mod `sakura-weapons`: Tetap stabil pada `v1.6.2` / `v2.9.4` (tanpa perubahan).
      - Mod `takasha-rpg`: **`v1.9.5+26.2`**.

51. **Penghapusan Subproyek Touhou Little Maid, Pembersihan Git Dump, Paritas Penuh 67 Stat RPG Termasuk GEM_STONE, dan Integrasi Stat Fisik Vanilla (v2.9.5):**
    - **Penghapusan Tuntas Subproyek Touhou Little Maid:**
      - Menghapus direktori subproyek `touhou-little-maid/` beserta seluruh modul Bedrock rendering, AI pendamping, dan aset modelnya.
      - Memperbarui `sakura-weapons/settings.gradle` dan `sakura-weapons/build.gradle` (meniadakan target `touhou-little-maid` dari `buildAll` & `cleanAll`).
      - Menyinkronkan `README.md` dengan meniadakan seluruh bab, diagram Mermaid, tabel tombol pintas, instruksi build, dan daftar rilis yang merujuk pada Touhou Little Maid.
    - **Pembersihan Crash Dump & Git Hygiene:**
      - Menambahkan filter `hs_err_pid*.log`, `replay_pid*.log`, dan direktori `dev/` pada `.gitignore`.
      - Menghapus ~48 berkas crash dump JVM lokal yang tidak sengaja terbuat selama pengujian intensif.
    - **Paritas Penuh 67 Stat RPG & Tipe GEM_STONE:**
      - Penambahan tipe item `GEM_STONE` ke dalam `ItemType.java` dengan pemetaan material default `EMERALD`.
      - Pemetaan lengkap 67 stat RPG (DoubleStat, BooleanStat, CompositeStat) dengan formula parser dan validasi chat input.
      - Implementasi engine kode warna universal `TakashaColorParser` mendukung format hex `&#RRGGBB`, `§x§r§r§g§g§b§b`, dan legacy `&a`-`&f`.
    - **Integrasi Penuh Stat Fisik Vanilla (`TakashaStatIntegrationHandler`):**
      - Pendaftaran attribute modifiers vanilla (`Attributes.MAX_HEALTH`, `ARMOR`, `ARMOR_TOUGHNESS`, `KNOCKBACK_RESISTANCE`, `MOVEMENT_SPEED`, `MINING_EFFICIENCY`, `BLOCK_INTERACTION_RANGE`, `ENTITY_INTERACTION_RANGE`) ke dalam `DataComponents.ATTRIBUTE_MODIFIERS`.
      - Menggunakan `AttributeModifiersComponent.Display.hidden()` untuk menyembunyikan tooltip atribut vanilla bawaan sehingga lore kustom RPG terbebas dari duplikasi tampilan visual.
    - **Verifikasi Pengujian Terotomatisasi:**
      - 7 suite pengujian unit dan integrasi lulus 100% (983 assertions) mencakup `TakashaStatIntegrationAutoTest`, `MultiTypeStatAuditAutoTest`, `UniversalColorParserAutoTest`, `StatFormulaEvaluationAutoTest`, `CombatEngineDamageResolutionAutoTest`, `DungeonRewardSimulationAutoTest`, dan `ItemTemplateSerializationAutoTest`.
    - **Kepatuhan SemVer & Matriks Versi Rilis:**
      - Root VERSION: **`2.9.5`**.
      - Mod `sakura-weapons`: **`v2.9.5+26.2`** & **`v2.6.2+1.21.11`**.
      - Mod `takasha-rpg`: **`v1.9.5+26.2`**.
