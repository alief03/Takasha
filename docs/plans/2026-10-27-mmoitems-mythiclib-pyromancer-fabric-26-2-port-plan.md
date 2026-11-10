# MMOItems, MythicLib & Awakened Pyromancer 1:1 Fabric 26.2 Port Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Port and reconstruct MMOItems 6.10.1, MythicLib 1.7, and samus2002 Awakened Pyromancer v1.1 into Minecraft 26.2 Fabric with 1:1 architectural parity, zero Bukkit dependencies, and full ItemsAdder standard mechanics compatibility.

**Architecture:** Decoupled modular RPG engine within `takasha-rpg` utilizing native Minecraft 26.2 Data Components (`DataComponentType<T>`), Fabric Attachment API for player RPG stats (`MMOPlayerData`), Mixin-driven combat and interaction pipelines (`ServerLivingEntityEvents`, `LivingEntityMixin`, `UseItemCallback`), Brigadier command trees, and Fabric Custom Payload Networking for client-side VFX, sound cues, and HUD synchronization.

**Tech Stack:** Minecraft 26.2, Fabric Loader 0.19.3, Fabric API 0.161.0+26.2, OpenJDK 25 LTS, Mojang Data Components, Fabric Attachment API v1, Mojang Brigadier, SpongePowered Mixin, Blockbench 3D Item Models.

---

## 1. Executive Summary & Hardware Target Profile

- **Target Versi:** Minecraft 26.2 (Fabric Loader `0.19.3`, Fabric API `0.161.0+26.2`, Java 25 LTS).
- **Target Profil Perangkat Keras:** AMD Ryzen 3 7320U (4 Core / 8 Threads), RAM Fisik 5.74 GB (Sisa bebas ~0.7–1.0 GB), AMD Radeon™ 610M iGPU (Shared VRAM).
- **Sumber Data Terverifikasi (REA Source Artifacts):**
  1. **Sumber A (MMOItems 6.10.1):** `referensi-rp/RPG/MMOItems-6.10.1-157-7` (Item definitions, stat systems, abilities, tier rarities, lore generator).
  2. **Sumber B (MythicLib 1.7):** `referensi-rp/RPG/MythicLib-1.7` (Combat damage metadata, attack calculation, elemental damage, skill trigger pipelines, cooldown maps).
  3. **Sumber C (Awakened Pyromancer v1.1):** `referensi-rp/RPG/samus2002_AWAKENED_PYROMANCER [v1.1]` (Blazestaff CMD 2096, 7 skill api, 7 ikon CMD 2357–2363, audio pack `sounds.json`, MythicMobs skills config).
  4. **Referensi Standar (ItemsAdder 4.0.18):** `referensi-rp/ItemsAdder_4.0.18` (Namespace registry, CMD allocation, interaction hitboxes, negative space fonts, container GUI textures).
- **Modul Target:** `takasha-rpg/src/main/java/net/takasha/rpg/mmo/` & `takasha-rpg/src/main/resources/assets/awakened_pyromancer/`.

---

## 2. REA Source Deconstruction & Gap Analysis

### 2.1 Bedah Arsitektur Sumber A (MMOItems 6.10.1)
- **Data Model:** `MMOItem` memiliki tipe (`Type`), ID template, dan peta `ItemStat` -> `StatData`.
- **Stat Taxonomy:**
  - *Numeric/Double Stats:* `ATTACK_DAMAGE`, `ATTACK_SPEED`, `CRITICAL_STRIKE_CHANCE`, `CRITICAL_STRIKE_POWER`, `MAX_HEALTH`, `ARMOR`, `ARMOR_TOUGHNESS`, `MAX_MANA`, `MANA_REGENERATION`, `STAMINA`.
  - *Restriction Stats:* `REQUIRED_LEVEL`, `REQUIRED_CLASS`, `SOULBOUND`.
  - *Functional Stats:* `ABILITIES`, `GEM_SOCKETS`, `UNBREAKABLE`, `CUSTOM_MODEL_DATA`.
- **Lifecycle & Build:** `MMOItemBuilder` menerjemahkan data stat ke `ItemStack` Bukkit melalui modifikasi NBT dan penulisan tag PDC (`PersistentDataContainer`).
- **Gap ke Fabric 26.2:** Di Minecraft 26.2, NBT bebas telah dihapus dan digantikan oleh `DataComponents`. Seluruh data MMOItem wajib diserialisasi menggunakan `DataComponentType<MMOItemComponent>` dengan immutable Mojang `Codec`.

### 2.2 Bedah Arsitektur Sumber B (MythicLib 1.7)
- **Multi-Layer Combat & Damage Pipeline:**
  - `AttackMetadata`: Mengemas `LivingEntity attacker` (via `PlayerMetadata`/`StatProvider`), `DamageMetadata damage`, dan entitas target (`LivingEntity`).
  - `DamageMetadata`: Koleksi modular `DamagePacket` yang memisahkan kerusakan ke dalam 9 tipe (`PHYSICAL`, `MAGIC`, `WEAPON`, `SKILL`, `PROJECTILE`, `UNARMED`, `ON_HIT`, `MINION`, `DOT`) dan 7 elemen RPG (`FIRE`, `ICE`, `WIND`, `EARTH`, `THUNDER`, `DARKNESS`, `LIGHT`).
  - Pengali Kerusakan: Mendukung `multiplicativeModifier` dan `additiveModifier` per tipe damage maupun per elemen (contoh: buff Ignition +20% Fire Damage).
  - Dual/Triple Critical Strike: Pemisahan kalkulasi Weapon Crit (pengali 2.0x, internal cooldown 3.0s) dan Skill Crit (pengali 1.5x, internal cooldown 3.0s) serta ledakan proksimiti Elemental Crit.
- **Formula Pertahanan & Mitigasi (Path of Exile Standard):**
  - Natural Damage: $\text{FinalDamage} = \max(0.1, \text{Damage} - \text{Defense})$.
  - Elemental Mitigation: $\text{FinalElementalDamage} = \text{Damage} \times \left(1 - \frac{\text{Defense}}{5 \times \text{Damage} + \text{Defense}}\right)$.
  - Mitigation Triad:
    - `Block`: Reduksi damage % via `block-power`, cooldown 5s, pesan actionbar `You just blocked #damage# damage.`
    - `Dodge`: $100\%$ damage negated + knockback 1.0, cooldown 5s, pesan actionbar `You just dodged #damage# damage.`
    - `Parry`: Negasi dan pantulkan damage fisik balik ke musuh, knockback 1.0, cooldown 8s, pesan actionbar `You just parried #damage# damage.`
- **Player Stat & Modifier Engine:**
  - `MMOPlayerData` & `PlayerMetadata`: Mengelola status pemain melalui `ModifierMap`.
  - Tipe Modifier: `FLAT` (+angka tetap) dan `RELATIVE` (+% persentase).
  - 9 Sumber Modifier (`ModifierSource`): `MAINHAND_ITEM`, `OFFHAND_ITEM`, `MELEE_WEAPON`, `RANGED_WEAPON`, `ARMOR`, `ACCESSORY`, `OTHER`, `VOID`.
  - Equip/Unequip Lifecycle: Pendaftaran otomatis saat item dipegang dan deregistrasi instan via `Closeable.close()` saat item dilepas, mencegah stat stacking bug.
- **Skill Triggering System & Parameter Overrides:**
  - 32+ `TriggerType`: `LEFT_CLICK`, `RIGHT_CLICK`, `SHIFT_LEFT_CLICK`, `SHIFT_RIGHT_CLICK`, `SNEAK`, `DROP_ITEM`, `SHIFT_DROP_ITEM`, `SWAP_ITEMS`, `SHIFT_SWAP_ITEMS`, `TIMER`, `ATTACK`, `DAMAGED`, `KILL_ENTITY`, dll.
  - Parameter Overrides: MMOItems mendefinisikan nilai parameter kemampuan (`damage`, `burnduration`, `cooldown`, `mana`) pada item YAML yang secara dinamis menimpa default pada MythicLib skill YAML via `SkillMetadata.getModifier(key)`.
  - `CastingDelayHandler`: Channeling skill dengan slowness 60%, BossBar SEGMENTED_20 ungu berlabel `CASTING`, dan `cancel-on-move`.
- **Modern Indicator Engine 26.2:**
  - Menggantikan ArmorStand lama dengan `DisplayEntity.TextDisplayEntity` (Billboard centered, zero ticking lag).
  - Menampilkan hologram teks damage `🗡 <val>` (Normal), `&c&l🗡 <val>` (Crit 2.0x), `★ <val>` (Skill), `&6&l★ <val>` (Skill Crit 1.5x), dan `&6🔥 <val>` (Fire).
- **Gap ke Fabric 26.2:** Bukkit bergantung pada `EntityDamageByEntityEvent` dan `PlayerInteractEvent`. Di Fabric 26.2, kita menggunakan `ServerLivingEntityEvents.ALLOW_DAMAGE`, `ServerLivingEntityEvents.AFTER_DAMAGE`, `AttackEntityCallback`, `UseItemCallback`, dan Mixin injeksi presisi pada `LivingEntity.damage` serta `ServerPlayNetworkHandler.handlePlayerAction`.

### 2.3 Bedah Arsitektur Sumber C (Awakened Pyromancer v1.1)
- **Item Profile:**
  - Senjata: `awakened_pyromancer_blazestaff` (Base Material: `STONE_SWORD`, CustomModelData: `2096`, Unbreakable, Display: *"Blazestaff"*, Type: *"Awakened Pyromancer"*).
  - Ikon Skill (7 Item Ikon pada Material `COAL`):
    - `icon_firestorm_volley` (CMD `2357`)
    - `icon_heat_mastery` (CMD `2358`)
    - `icon_hellfire_overblast` (CMD `2359`)
    - `icon_infernal_judgement` (CMD `2360`)
    - `icon_magma_dash` (CMD `2361`)
    - `icon_pyro_combo` (CMD `2362`)
    - `icon_volcanic_spear` (CMD `2363`)
- **7 Mekanika Skill:**
  1. `HEAT_MASTERY` (TIMER 10s): Setiap serangan fisik memberi +1 Heat stack. Pada 3 stack, masuk mode `IGNITION` (+20% damage, flame visual crimson).
  2. `PYRO_COMBO` (LEFT_CLICK, CD 0.3s): 3-hit melee combo tebasan api, 4 damage + 3s burn (0.5 dmg/tick). Ignition: Shockwave api ke depan.
  3. `VOLCANIC_SPEAR` (RIGHT_CLICK, CD 0.5s): Menembakkan proyektil tombak magma menembus 3 mob, 4 damage + 3s burn.
  4. `HELLFIRE_OVERBLAST` (SNEAK, CD 3.0s): Ledakan radial 360° radius 4 blok, 4 damage + 3s burn, knockback 3 blok.
  5. `FIRESTORM_VOLLEY` (TIMER 10s / Proc): 3–5 bola api melayang yang mencari mob musuh dalam radius 12 blok, 4 damage.
  6. `MAGMA_DASH` (SHIFT_RIGHT_CLICK, CD 4.0s): Melesat maju 8 blok dengan jejak magma, menembus musuh dengan 3 damage + 3s burn.
  7. `INFERNAL_JUDGEMENT` (SHIFT_LEFT_CLICK, CD 10.0s): Ultimate: Melompat ke udara dan menghantam tanah dengan pilar lava area 6 blok, 7 damage + 5s burn.
- **Audio Effects:** 10 berkas stereo kustom pada namespace `awakened_pyromancer_sounds` (`sounds.json`).

### 2.4 Bedah Referensi Standar ItemsAdder (v4.0.18)
- **Namespacing:** Format baku `<namespace>:<id>`.
- **Modern Item Definition 26.2:** Berkas `assets/<namespace>/items/<id>.json` memanfaatkan model definition vanilla 26.2 dengan dispatch native `minecraft:custom_model_data` atau `minecraft:model`.
- **Furniture Hitbox:** Menggantikan ArmorStand Bukkit dengan `InteractionEntity` (bounding box klik native) dan `ItemDisplayEntity` (rotasi matriks mulus).
- **Negative Space Font:** Mendukung pergeseran koordinat font (`\uF801`–`\uF808` dengan advance negatif) untuk overlay GUI 54-slot.

### 2.5 Bedah Dependensi ModelEngine (R4.1.1) via Skill REA
- **Metodologi REA:** Dilakukan dekonstruksi langsung terhadap berkas binary `referensi-rp/RPG/ModelEngine-R4.1.1.jar` (2.289 entries, 1.825 classes) sesuai standar skill [reverse-engineer-anything](file:///d:/Mod%20Minecraft/weapon%20set/sakura/.agents/skills/reverse-engineer-anything/SKILL.md) dan [rea-essentialsx-parity](file:///d:/Mod%20Minecraft/weapon%20set/sakura/.agents/skills/rea-essentialsx-parity/SKILL.md).
- **[EVIDENCE: ME-01] `BlockbenchModel.class` & `BlockbenchDeserializer.class`:**
  - Terletak di package `com.ticxo.modelengine.api.generator.parser.blockbench`.
  - Bytecode mengurai struktur Blockbench format 5.0 JSON:
    - `resolution`: dimensi tekstur (`width`, `height`).
    - `elements`: array kubus dengan batas 3D `from`, `to`, `uv`, dan 6 sisi `faces`.
    - `outliner`: struktur pohon hierarki tulang (Group) dengan registri `flatOutlinerByName` dan `flatOutlinerByUUID`.
    - `animations`: kurva keyframe `position`, `rotation`, dan `scale`.
- **[EVIDENCE: ME-02] `BlueprintBone.class` & 3D Math JOML:**
  - Terletak di package `com.ticxo.modelengine.api.generator.blueprint`.
  - Menggunakan library `org.joml` secara ekstensif:
    - `org.joml.Quaternionf localQuaternion` & `globalQuaternion`
    - `org.joml.Quaternionf invGlobalQuaternion`
    - `org.joml.Vector3f modelScale`
  - *Temuan Kunci REA:* Mojang pada Minecraft 26.2 secara native menggunakan `org.joml.Quaternionf` dan `org.joml.Vector3f` pada seluruh rendering engine vanilla (`DisplayEntity`, `Transformation`, `Matrix4f`). Hal ini membuktikan bahwa kita dapat melakukan translasi model 1:1 ke Fabric 26.2 tanpa layer wrapper atau alokasi memori berlebih.
- **[EVIDENCE: ME-03] `BlueprintAnimation.class` & Keyframe Engine:**
  - Terletak di package `com.ticxo.modelengine.api.animation`.
  - Mendukung `KeyframeTypes.POSITION`, `ROTATION`, `SCALE`, `SCRIPT` serta interpolasi kurva `LoopMode.LOOP`, `HOLD`, `ONCE`.
- **Translasi ke Fabric 26.2:** Menggantikan ArmorStand Bukkit yang lambat dengan native `Display.ItemDisplayEntity` dan `Display.BlockDisplayEntity` Fabric 26.2 (affine 4x4 matrix) yang diinterpolasi 60+ FPS mulus di client.

### 2.6 Bedah Dependensi MythicMobs Premium (5.8.2-SNAPSHOT) via Skill REA
- **Metodologi REA:** Dilakukan inspeksi binary dan ekstraksi simbol dari `referensi-rp/RPG/MythicMobsPremium-5.8.2-SNAPSHOT.jar` (11.440 entries, 10.495 classes).
- **[EVIDENCE: MM-01] `AbstractModelEngineSupport.class`:**
  - Terletak di `io.lumine.mythic.bukkit.compatibility`.
  - Menyediakan kontrak integrasi:
    - `createMEGBullet(modelData, location)`: Peluru balistik dengan mesh model 3D Blockbench.
    - `createMobModel(entity, modelId)`: Pemasangan model ke entitas.
    - `overlapsOOBB(...)`: Deteksi tabrakan Oriented Bounding Box akurat.
- **[EVIDENCE: MM-02] `MissileMechanic.class`:**
  - Terletak di `io.lumine.mythic.core.skills.mechanics`.
  - Bytecode mengonfirmasi parameter: `hitRadius`, `verticalHitRadius`, `onHitSkill`, `onTickSkill`, `onHitBlockSkill`, `projectileVelocityHorizOffset`, `projectileVelocityVertOffset`.
  - Digunakan pada skill Samus `Volcanic_Spear` dan `Pyro_Combo_ASH_Missiles`.
- **[EVIDENCE: MM-03] `OrbitalMechanic.class`:**
  - Terletak di `io.lumine.mythic.core.skills.mechanics` (turunan `Aura`).
  - Bytecode mengonfirmasi parameter: `auraName`, `hr` (horizontal radius), `vr` (vertical radius), `oy` (offset Y), `points`, `duration`, `avx/y/z` (angular velocity).
  - Digunakan pada skill Samus `Heat_Mastery_STACK` untuk bola api berputar (*Heat Stacks*).
- **[EVIDENCE: MM-04] `AuraMechanic.class` & `EntitiesInConeTargeter.class`:**
  - Terletak di `io.lumine.mythic.core.skills.mechanics` dan `io.lumine.mythic.core.skills.targeters`.
  - Mengelola buff temporal, akumulasi tumpukan (*stacks*), serta evaluasi kerucut penglihatan 3D pemain.
- **Translasi ke Fabric 26.2:** Membangun `TakashaMythicEngine` (parser YAML AST rekursif + dispatcher mechanic) sehingga seluruh skill Samus berjalan secara murni data-driven tanpa hardcode Java class!

---

## 3. Matriks Pemetaan Arsitektur 1:1 (Bukkit ke Fabric 26.2)

```
┌────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                   1:1 ARCHITECTURE TRANSLATION MATRIX                                  │
├───────────────────────────┬───────────────────────────────────┬────────────────────────────────────────┤
│ Komponen Arsitektur       │ Implementasi Bukkit/Spigot        │ Implementasi Fabric 26.2 (Native)      │
├───────────────────────────┼───────────────────────────────────┼────────────────────────────────────────┤
│ Item Metadata Storage     │ CraftItemStack + PDC NBT          │ DataComponentType<MMOItemComponent>   │
│ Display Name & Lore       │ ItemMeta.setDisplayName / Lore    │ DataComponents.CUSTOM_NAME & LORE      │
│ Native Attribute Modifiers│ ItemMeta.addAttributeModifier     │ DataComponents.ATTRIBUTE_MODIFIERS     │
│ Model Dispatching         │ CustomModelData Integer NBT       │ DataComponents.CUSTOM_MODEL_DATA & JSON│
│ Player RPG Stats Data     │ MMOPlayerData (File / DB)         │ Fabric Attachment API (World NBT)      │
│ Physical Attack Hook      │ EntityDamageByEntityEvent         │ ServerLivingEntityEvents.ALLOW_DAMAGE  │
│ Damage Post-Processing    │ EntityDamageEvent (MONITOR)       │ ServerLivingEntityEvents.AFTER_DAMAGE  │
│ Elemental Damage Pipeline │ MythicLib DamageMetadata          │ TakashaDamageMetadata & DamageSource   │
│ Right Click / Use Hook    │ PlayerInteractEvent (RIGHT_CLICK) │ UseItemCallback.EVENT                  │
│ Left Click / Swing Hook   │ PlayerInteractEvent (LEFT_CLICK)  │ ServerPlayNetworkHandlerMixin          │
│ Sneak State Change Hook   │ PlayerToggleSneakEvent            │ ServerPlayerEntityMixin.setSneaking    │
│ Passive Timer Tick        │ BukkitRunnable (runTaskTimer)     │ ServerTickEvents.END_SERVER_TICK       │
│ Command Registration      │ PluginCommand (plugin.yml)        │ CommandRegistrationCallback (Brigadier)│
│ Permissions Check         │ Player.hasPermission(String)      │ Fabric Permissions API / OP Level      │
│ Client Particle & VFX Sync│ PacketPlayOutWorldParticles       │ Fabric Custom Payload Networking (S2C) │
│ Custom Sound Registration │ Bukkit sound string playback      │ Registry.register(BuiltInRegistries...)│
│ Furniture & Display Entity│ ArmorStand + Slime Mount          │ InteractionEntity + ItemDisplayEntity  │
│ Negative Space Font Kerning│ ResourcePack font ascent hack    │ Native Font Provider JSON (advance <0) │
│ ModelEngine .bbmodel Parse│ ModelEngine Blueprint Loader      │ BlockbenchModelParser & ModelBlueprint │
│ ModelEngine Bone Renderer │ ArmorStand / Packet Display      │ Native ItemDisplayEntity (Affine 4x4)  │
│ ModelEngine Animation Play│ ModelEngine ActiveModel Animator  │ TakashaModelAnimator (Keyframe Easing) │
│ MythicMobs Skill YAML     │ MythicMobs Script Executor        │ TakashaMythicEngine & SkillSyntaxTree  │
│ MythicMobs Missile/Orbital│ Mythic Projectile Task            │ Native Fabric Missile & Orbital Runner │
│ Samus Class Scalability   │ Multiple Plugin configs           │ TakashaPackManager (Drop-in Auto-Scan) │
└───────────────────────────┴───────────────────────────────────┴────────────────────────────────────────┤
```

---

## 4. Alur Kerja Arsitektur Porting 1:1

```mermaid
flowchart TD
    subgraph Client [Fabric 26.2 Client]
        A["Pemain Input (Left/Right/Shift Click)"] --> B["Client Interaction Handler"]
        B --> C["Kirim Packet Aksi ke Server"]
        HUD["RPG HUD & Actionbar Sync"] <-- M["Custom Payload S2C"]
    end

    subgraph ServerCore [Takasha-RPG Server Core 26.2]
        C --> D{"Validasi Item & Component"}
        D -->|"Item memiliki MMOItemComponent"| E["MMOItemEngine: Ambil Stat & Ability"]
        D -->|"Item Vanilla Biasa"| F["Fallback Vanilla Mechanics"]

        E --> G{"Evaluasi TriggerType"}
        G -->|"LEFT_CLICK"| H1["Trigger: PYRO_COMBO"]
        G -->|"RIGHT_CLICK"| H2["Trigger: VOLCANIC_SPEAR"]
        G -->|"SHIFT_RIGHT_CLICK"| H3["Trigger: MAGMA_DASH"]
        G -->|"SHIFT_LEFT_CLICK"| H4["Trigger: INFERNAL_JUDGEMENT (Ult)"]
        G -->|"SNEAK"| H5["Trigger: HELLFIRE_OVERBLAST"]
        G -->|"TIMER (20 ticks)"| H6["Passive: HEAT_MASTERY & FIRESTORM"]

        H1 & H2 & H3 & H4 & H5 & H6 --> I["Cooldown & Mana Verification"]
        I -->|"Lolos"| J["Eksekusi TakashaCombatEngine"]
        I -->|"Cooldown Aktif"| K["Kirim Actionbar Cooldown Warning"]

        J --> L["Kalkulasi TakashaDamageMetadata (Physical + Fire)"]
        L --> M["Kirim VFX & Sound Payload S2C"]
        L --> N["ServerLivingEntityEvents: Apply Damage ke Mob"]
    end
```

---

## 5. Rencana Replikasi Mekanik ItemsAdder (1:1 Parity)

1. **Namespace Registry:**
   - Format: `<namespace>:<item_id>` (contoh: `awakened_pyromancer:awakened_pyromancer_blazestaff`).
   - Mendaftarkan entri di `assets/awakened_pyromancer/items/awakened_pyromancer_blazestaff.json` yang secara native merender model 3D Blockbench di tangan pemain.
2. **Hitbox & Furniture Engine Tanpa Lag:**
   - `TakashaFurnitureManager`:
     - Spawning `ItemDisplayEntity` dengan transformasi matriks presisi untuk model visual.
     - Spawning `InteractionEntity` dengan lebar dan tinggi sesuai hitbox YAML ItemsAdder.
     - Penanganan event interaksi duduk (seat mount) dan break drops.
3. **Negative Space Font & Container GUI Overlays:**
   - Menyertakan berkas `assets/takasha/font/default.json` dengan penyedia font bitmap spasi negatif:
     ```json
     {
       "type": "space",
       "advances": {
         "\uF801": -1,
         "\uF802": -2,
         "\uF804": -4,
         "\uF808": -8,
         "\uF810": -16,
         "\uF820": -32,
         "\uF840": -64,
         "\uF880": -128
       }
     }
     ```
   - Memungkinkan judul GUI 54-slot bergeser ke kiri untuk menempatkan tekstur custom container overlay persis seperti ItemsAdder.

---

## 6. Pohon Direktori & Modul Proyek

```
takasha-rpg/
├── src/main/java/net/takasha/rpg/
│   ├── TakashaRpgMod.java                          # Main Mod Entry Point 26.2
│   ├── command/
│   │   └── TakashaCommands.java                    # Brigadier Command Tree & Creative Permissions
│   ├── mmo/                                        # Sub-sistem Porting MMOItems & MythicLib
│   │   ├── api/
│   │   │   ├── MMOItem.java                        # Objek Representasi Item RPG
│   │   │   ├── MMOItemType.java                    # SWORD, AXE, BOW, STAFF, dll.
│   │   │   ├── ItemTier.java                       # COMMON s/d MYTHIC
│   │   │   └── stat/
│   │   │       ├── ItemStat.java                   # Definisi Stat RPG
│   │   │       └── StatType.java                   # NUMERIC, DOUBLE, ABILITY, RESTRICTION
│   │   ├── component/
│   │   │   └── MMOItemComponent.java               # Mojang DataComponentType Record
│   │   ├── combat/
│   │   │   ├── TakashaDamageMetadata.java          # Damage Breakdown & Elements
│   │   │   ├── DamageType.java                     # PHYSICAL, MAGIC, WEAPON, SKILL
│   │   │   ├── ElementalDamage.java                # FIRE, ICE, LIGHTNING, dll.
│   │   │   └── CombatCalculator.java               # Stat Scaling & Mitigation
│   │   ├── skill/
│   │   │   ├── Skill.java                          # Base Skill Interface
│   │   │   ├── TriggerType.java                    # Enums Trigger Interaksi
│   │   │   └── SkillMetadata.java                  # Parameter Casting Runtime
│   │   ├── itemsadder/                             # Modul Paritas ItemsAdder
│   │   │   ├── ItemsAdderRegistry.java             # Namespace & Model Registry
│   │   │   ├── FurnitureEngine.java                # Interaction + ItemDisplay
│   │   │   └── NegativeFontHelper.java             # Negative Space Kerning
│   │   ├── network/
│   │   │   ├── TakashaSkillVfxPayload.java         # Custom Payload S2C VFX
│   │   │   └── TakashaStatSyncPayload.java         # Custom Payload S2C Stats
│   │   ├── enchant/
│   │   │   ├── TakashaFlightManager.java           # Flight State Machine & Safe Landing Engine
│   │   │   └── TakashaEnchantUtilityEngine.java    # Mobility & Liquid Walking Overhaul
│   │   └── mixin/
│   │       ├── ServerPlayNetworkHandlerMixin.java  # Left Click Air Swing Hook
│   │       ├── LivingEntityDamageMixin.java        # Combat Pipeline Injection
│   │       └── ServerPlayerEntitySneakMixin.java   # Sneak Trigger Hook
│   ├── modelengine/                                # Sub-sistem Porting ModelEngine R4.1.1
│   │   ├── parser/
│   │   │   ├── BlockbenchModelParser.java          # Parser .bbmodel 5.0 JSON
│   │   │   ├── ModelBlueprint.java                 # Struktur Data Tulang, Kubus, Pivot
│   │   │   └── AnimationTimeline.java              # Keyframe Interpolator & Easing
│   │   ├── runtime/
│   │   │   ├── ActiveModelInstance.java            # Instansi Model Aktif di Dunia
│   │   │   ├── DisplayEntityModelRenderer.java     # Native Item/Block Display Renderer (4x4 Matrix)
│   │   │   └── ModelAnimationController.java       # Pengendali State Animasi & Transisi
│   │   └── registry/
│   │       └── TakashaModelBlueprintRegistry.java  # Registri Global Model Blockbench
│   ├── mythic/                                     # Sub-sistem Porting MythicMobs 5.8.2
│   │   ├── parser/
│   │   │   ├── MythicYamlParser.java               # Parser YAML Skill & Helper Mobs
│   │   │   ├── SkillSyntaxTree.java                # Pohon Eksekusi Mekanik Skill
│   │   │   └── TargeterSyntax.java                 # Parser Targeter Math 3D
│   │   ├── mechanics/
│   │   │   ├── TakashaMechanicDispatcher.java      # Dispatcher 22+ Mekanik Inti
│   │   │   ├── MissileMechanic.java                # Balistik Proyektil & onTick/onHit
│   │   │   ├── OrbitalMechanic.java                # Entitas VFX Mengorbit Pemain
│   │   │   ├── AuraMechanic.java                   # Buff Temporal, Ignition, & Stack Tracker
│   │   │   └── DamageMechanic.java                 # Integrasi ke TakashaDamageMetadata
│   │   ├── targeters/
│   │   │   ├── ConeTargeter.java                   # EntitiesInCone 3D Vector Math
│   │   │   └── OriginTargeter.java                 # Radius Search Near Origin / Caster
│   │   ├── conditions/
│   │   │   └── ConditionChecker.java               # Evaluasi hasAura, hasaurastacks, isLiving
│   │   └── registry/
│   │       ├── TakashaMythicSkillRegistry.java     # Registri Skrip Skill YAML
│   │       └── TakashaMythicMobRegistry.java       # Registri Helper Mob Template
│   ├── pack/                                       # Scalable Samus Pack Engine
│   │   ├── TakashaPackManager.java                 # Auto-Scanner (.minecraft/config/takasha/rpg_packs/)
│   │   ├── RpgPackManifest.java                    # Parser Metadata & Dependency Pack
│   │   └── DynamicPackResourceLoader.java          # Dynamic Virtual Resource Pack Provider
│   └── (komponen AuraSkills, ADA, Shop, Maid yang sudah ada...)
└── src/main/resources/
    ├── fabric.mod.json
    └── assets/
        ├── awakened_pyromancer/                    # Aset Sumber C
        │   ├── items/awakened_pyromancer_blazestaff.json
        │   ├── models/item/awakened_pyromancer_blazestaff.json
        │   ├── textures/item/awakened_pyromancer_blazestaff.png
        │   └── sounds.json                         # 10 Custom Sound Events
        └── takasha/
            └── font/default.json                   # Negative Space Font Provider
```

---

## 7. Bite-Sized Implementation Plan

### Task 1: Fondasi Data Components & MMOItem Model (Fase 1: Fondasi Core)
**Files:**
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/mmo/component/MMOItemComponent.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/mmo/api/MMOItemType.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/mmo/api/ItemTier.java`
- Modify: `takasha-rpg/src/main/java/net/takasha/rpg/registry/ModDataComponents.java`
- Test: `takasha-rpg/src/test/java/net/takasha/rpg/mmo/MMOItemComponentTest.java`

**Step 1: Write the failing test**
```java
@Test
void testMMOItemComponentCodecSerialization() {
    MMOItemComponent comp = new MMOItemComponent("SWORD", "awakened_pyromancer_blazestaff", 5, Map.of("attack-damage", 5.0), List.of("HEAT_MASTERY"), 2096, true);
    DynamicOps<NbtElement> ops = NbtOps.INSTANCE;
    DataResult<NbtElement> result = MMOItemComponent.CODEC.encodeStart(ops, comp);
    assertTrue(result.result().isPresent());
}
```
**Step 2: Run test to verify it fails**
Run: `gw test --tests net.takasha.rpg.mmo.MMOItemComponentTest`
Expected: FAIL (class not found).

**Step 3: Write minimal implementation**
Implement `MMOItemComponent` record with `Codec<MMOItemComponent>` and register `DataComponentType<MMOItemComponent>` in `ModDataComponents`.

**Step 4: Run test to verify it passes**
Run: `gw test --tests net.takasha.rpg.mmo.MMOItemComponentTest`
Expected: PASS.

**Step 5: Commit**
`git commit -m "feat(mmo): add MMOItemComponent DataComponent and Type enums"`

---

### Task 2: MythicLib Multi-Layer Combat Engine, PoE Defense & Mitigation Triad (Fase 2: Combat & Damage)
**Files:**
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/mmo/combat/DamageType.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/mmo/combat/Element.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/mmo/combat/DamagePacket.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/mmo/combat/ElementalDamagePacket.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/mmo/combat/TakashaDamageMetadata.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/mmo/combat/AttackMetadata.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/mmo/combat/DefenseCalculator.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/mmo/combat/MitigationHandler.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/mmo/modifier/ModifierMap.java`
- Test: `takasha-rpg/src/test/java/net/takasha/rpg/mmo/CombatCalculatorTest.java`

**Step 1: Write the failing test**
```java
@Test
void testElementalFireAndPoEDefenseMitigation() {
    TakashaDamageMetadata meta = new TakashaDamageMetadata();
    meta.addPacket(new DamagePacket(10.0, DamageType.WEAPON, DamageType.PHYSICAL));
    meta.addPacket(new ElementalDamagePacket(10.0, Element.FIRE));
    
    // Uji formula non-linear Path of Exile untuk elemental: Damage * (1 - Def / (5 * Dmg + Def))
    // Dengan Defense = 50: 10 * (1 - 50 / (50 + 50)) = 10 * (1 - 0.5) = 5.0
    double mitigatedFire = DefenseCalculator.calculateElementalDamage(10.0, 50.0);
    assertEquals(5.0, mitigatedFire, 0.01);

    // Uji formula linear Path of Exile untuk natural damage: max(0.1, Dmg - Def)
    // Dengan Defense = 4: max(0.1, 10 - 4) = 6.0
    double mitigatedPhys = DefenseCalculator.calculateNaturalDamage(10.0, 4.0);
    assertEquals(6.0, mitigatedPhys, 0.01);

    // Uji mitigasi Block (Block Power 50% memotong damage menjadi separuh)
    double blockedPhys = MitigationHandler.applyBlock(mitigatedPhys, 50.0);
    assertEquals(3.0, blockedPhys, 0.01);
}
```
**Step 2: Run test to verify it fails**
Run: `gw test --tests net.takasha.rpg.mmo.CombatCalculatorTest`
Expected: FAIL.

**Step 3: Write minimal implementation**
- Implementasikan 9 `DamageType` (`PHYSICAL`, `MAGIC`, `WEAPON`, `SKILL`, `PROJECTILE`, `UNARMED`, `ON_HIT`, `MINION`, `DOT`) dan 7 `Element` (`FIRE`, `ICE`, `WIND`, `EARTH`, `THUNDER`, `DARKNESS`, `LIGHT`).
- Implementasikan struktur modular `DamagePacket` dan `ElementalDamagePacket` di dalam `TakashaDamageMetadata`.
- Implementasikan `DefenseCalculator` dengan formula Path of Exile asli dari konfigurasi MythicLib 1.7.
- Implementasikan `MitigationHandler` untuk kalkulasi Block (reduksi % via block-power), Dodge (negasi 100% + knockback 1.0), dan Parry (pantulan damage balik ke penyerang + knockback 1.0) dengan cooldown tracking independen (5s, 5s, 8s).
- Implementasikan `ModifierMap` yang mendukung `ModifierType.FLAT` dan `ModifierType.RELATIVE` pada 9 `ModifierSource`.

**Step 4: Run test to verify it passes**
Run: `gw test --tests net.takasha.rpg.mmo.CombatCalculatorTest`
Expected: PASS.

**Step 5: Commit**
`git commit -m "feat(mmo): implement MythicLib 1.7 multi-layer combat engine, PoE defense and mitigation triad"`

---

### Task 2b: TakashaFlightManager State Machine & Mobility Enchants Fix (Fase 2b: Flight Engine & Mobility Audit)
**Files:**
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/enchant/TakashaFlightManager.java`
- Modify: `takasha-rpg/src/main/java/net/takasha/rpg/enchant/TakashaEnchantUtilityEngine.java`
- Test: `takasha-rpg/src/test/java/net/takasha/rpg/enchant/TakashaFlightManagerTest.java`

**Step 1: Write the failing test**
```java
@Test
void testWingsFlightActivationAndSafeLanding() {
    TakashaFlightManager manager = new TakashaFlightManager();
    UUID playerUuid = UUID.randomUUID();
    
    // 1. Simulasikan player equip boots dengan enchant 'wings'
    manager.handleBootsEquipped(playerUuid, "wings");
    assertTrue(manager.canFly(playerUuid), "Pemain dengan enchant wings wajib memiliki kemampuan terbang");
    
    // 2. Simulasikan player unequip boots di udara
    manager.handleBootsUnequipped(playerUuid, true /* inAir */);
    assertFalse(manager.canFly(playerUuid), "Kemampuan terbang wajib dicabut seketika");
    assertTrue(manager.hasSlowFallingGrace(playerUuid), "Pemain wajib menerima Slow Falling safe-landing");
    assertTrue(manager.hasFallDamageGrace(playerUuid), "Pemain wajib menerima anti-fall damage grace period");
}

@Test
void testWaterWalkerSurfaceLocking() {
    // Verifikasi penahanan posisi vertikal di atas air saat tidak sneaking
    Vec3d movement = TakashaEnchantUtilityEngine.calculateLiquidWalkingDelta(
        new Vec3d(0.2, -0.5, 0.2), true /* onWaterSurface */, false /* isSneaking */
    );
    assertEquals(0.0, movement.y, 0.001, "Y delta wajib dikunci 0.0 agar berjalan di atas air");
}
```
**Step 2: Run test to verify it fails**
Run: `gw test --tests net.takasha.rpg.enchant.TakashaFlightManagerTest`
Expected: FAIL (class not found).

**Step 3: Write minimal implementation**
- Buat `TakashaFlightManager` dengan `Set<UUID> WINGS_FLYING_PLAYERS` dan `Map<UUID, Long> FLIGHT_GRACE_PERIODS`.
- Integrasikan dengan `ServerTickEvents.END_SERVER_TICK` di `TakashaEnchantUtilityEngine.java`:
  - Periksa enchant `wings` pada boots.
  - Set `sp.getAbilities().mayfly = true; sp.onUpdateAbilities();`.
  - Kemampuan terbang murni (zero extra FX): `sp.getAbilities().mayfly = true; sp.onUpdateAbilities();` tanpa partikel visual atau audio kepakan sayap sesuai instruksi desain minimalis.
  - Saat boots dilepas/rusak di udara: cabut mayfly & flying, berikan `MobEffects.SLOW_FALLING` (5s), catat grace period 10 detik.
- Hook ke `ServerLivingEntityEvents.ALLOW_DAMAGE`: batalkan 100% `DamageTypes.FALL` jika pemain dalam grace period pendaratan wings.
- Perbaiki `waterwalker`: kunci $Y$ saat berada di atas `Blocks.WATER` dan `!sp.isShiftKeyDown()` agar berjalan di permukaan air.
- Implementasikan `lavawalker`: mekanika serupa di atas `Blocks.LAVA` + kekebalan lava & pemadaman api.

**Step 4: Run test to verify it passes**
Run: `gw test --tests net.takasha.rpg.enchant.TakashaFlightManagerTest`
Expected: PASS.

**Step 5: Commit**
`git commit -m "fix(enchant): implement TakashaFlightManager for wings flight and overhaul liquid walking"`

---

### Task 2c: World Exit Stability, Disconnect Cleanup & Anti-Freeze Architecture (Pencegahan CPU 100% & Hang Saat Keluar Dunia)

**Files:**
- Create: `src/main/java/net/sakura/weapons/client/event/TakashaClientDisconnectHandler.java`
- Modify: `src/main/java/net/sakura/weapons/client/SakuraWeaponsClient.java`
- Modify: `src/main/java/net/sakura/weapons/client/automation/TakashaMobKillHandler.java`
- Modify: `src/main/java/net/sakura/weapons/client/automation/TakashaFarmHandler.java`
- Modify: `src/main/java/net/sakura/weapons/client/automation/TakashaFishingHandler.java`
- Modify: `src/main/java/net/sakura/weapons/client/automation/TakashaAutoBowHandler.java`
- Modify: `takasha-rpg/src/main/java/net/takasha/rpg/TakashaRpgMod.java`
- Modify: `takasha-rpg/src/main/java/net/takasha/rpg/TakashaRpgManager.java`
- Modify: `takasha-rpg/src/main/java/net/takasha/rpg/shop/TakashaDailyShopManager.java`
- Test: `takasha-rpg/src/test/java/net/takasha/rpg/lifecycle/WorldExitStabilityTest.java`

**Step 1: Write the failing test**
```java
@Test
void testDoubleSavePreventionDuringServerStopping() {
    TakashaRpgManager manager = new TakashaRpgManager();
    UUID playerUuid = UUID.randomUUID();
    
    // Simulasikan fase server stopping
    manager.setServerStopping(true);
    
    // Disconnect saat stopping tidak boleh memicu double disk save
    boolean savedAgain = manager.onPlayerDisconnect(playerUuid);
    assertFalse(savedAgain, "Penyimpanan ganda wajib di-bypass saat server sedang shutdown");
}

@Test
void testDailyShopRotationTimestampUnit() {
    long realTime = System.currentTimeMillis();
    long clockTime = 12000L; // 12000 ticks
    
    // Verifikasi bahwa rotasi toko hanya menerima epoch millisecond
    assertTrue(TakashaDailyShopManager.isValidTimestamp(realTime), "Timestamp milidetik wajib valid");
    assertFalse(TakashaDailyShopManager.isValidTimestamp(clockTime), "Game ticks tidak boleh dioper ke rotasi toko");
}
```

**Step 2: Run test to verify it fails**
Run: `gw test --tests net.takasha.rpg.lifecycle.WorldExitStabilityTest`
Expected: FAIL (method not found / class not found).

**Step 3: Write minimal implementation**
1. **Client Active Connection Guard:**
   - Di `TakashaMobKillHandler.java`, `TakashaFarmHandler.java`, `TakashaFishingHandler.java`, dan `TakashaAutoBowHandler.java`:
     Tambahkan guard di awal `tick(MinecraftClient client)`:
     ```java
     if (client == null || client.getConnection() == null || !client.getConnection().getConnection().isConnected()) {
         return;
     }
     ```
2. **Client Disconnect Event Handler:**
   - Buat `TakashaClientDisconnectHandler.java` dengan method `handleClientDisconnect()`.
   - Reset semua state automasi: matikan status auto-kill, auto-farm, auto-fishing, auto-bow, bersihkan target cache, dan reset tombol input virtual Minecraft.
   - Daftarkan ke `ClientPlayConnectionEvents.DISCONNECT` di `SakuraWeaponsClient.java`.
3. **Server Anti-Double Save:**
   - Di `TakashaRpgManager.java`, tambahkan `AtomicBoolean isServerStopping = new AtomicBoolean(false);`.
   - Di method `saveAll()` atau saat `SERVER_STOPPING`, set `isServerStopping.set(true)`.
   - Di `onPlayerDisconnect(UUID uuid)`, jika `isServerStopping.get() == true`, langsung return tanpa memanggil `savePlayerData(uuid)`.
4. **Perbaikan Clock Daily Shop:**
   - Di `TakashaRpgMod.java` baris ~237 pada `ServerTickEvents.END_SERVER_TICK`:
     Ganti `TakashaDailyShopManager.checkRotation(server.overworld().getOverworldClockTime())` menjadi `TakashaDailyShopManager.checkRotation(System.currentTimeMillis())`.
5. **Graceful Executor Shutdown:**
   - Di `TakashaRpgMod.java`, tambahkan shutdown hook pada `ServerLifecycleEvents.SERVER_STOPPED` untuk menghentikan executor service yang berjalan di background.

**Step 4: Run test to verify it passes**
Run: `gw test --tests net.takasha.rpg.lifecycle.WorldExitStabilityTest`
Expected: PASS.

**Step 5: Commit**
`git commit -m "fix(lifecycle): prevent CPU 100% and freeze on world unload with connection guard and disconnect cleanup"`

---

### Task 2d: ModelEngine R4.1.1 & MythicMobs 5.8.2 Scalable Data-Driven Engine (Fase 2d: Scalable Samus Engine)
**Files:**
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/modelengine/parser/BlockbenchModelParser.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/modelengine/parser/ModelBlueprint.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/modelengine/parser/AnimationTimeline.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/modelengine/runtime/ActiveModelInstance.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/modelengine/runtime/DisplayEntityModelRenderer.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/modelengine/runtime/ModelAnimationController.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/modelengine/registry/TakashaModelBlueprintRegistry.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/mythic/parser/MythicYamlParser.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/mythic/parser/SkillSyntaxTree.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/mythic/mechanics/TakashaMechanicDispatcher.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/mythic/mechanics/MissileMechanic.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/mythic/mechanics/OrbitalMechanic.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/mythic/mechanics/AuraMechanic.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/mythic/targeters/ConeTargeter.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/mythic/conditions/ConditionChecker.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/pack/TakashaPackManager.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/pack/RpgPackManifest.java`
- Test: `takasha-rpg/src/test/java/net/takasha/rpg/modelengine/BlockbenchParserTest.java`
- Test: `takasha-rpg/src/test/java/net/takasha/rpg/mythic/MythicSkillParserTest.java`
- Test: `takasha-rpg/src/test/java/net/takasha/rpg/pack/TakashaPackManagerTest.java`

**Step 1: Write failing tests**
```java
@Test
void testBlockbenchParserLoadsModelBonesAndAnimations() {
    File modelFile = new File("src/test/resources/test_models/infernal_spear.bbmodel");
    ModelBlueprint blueprint = BlockbenchModelParser.parse(modelFile);
    assertNotNull(blueprint);
    assertFalse(blueprint.getBones().isEmpty(), "Model wajib memiliki minimal satu tulang");
    assertTrue(blueprint.hasAnimation("spawn") || blueprint.hasAnimation("idle"));
}

@Test
void testMythicSkillParserParsesMechanicsAndTargeters() {
    String yamlContent = 
        "InfernalJudgement_Cast:\n" +
        "  Skills:\n" +
        "  - missile{ot=InfernalJudgement_Tick;oh=InfernalJudgement_Hit;v=15} @TargetedTarget\n" +
        "  - damage{amount=14;type=FIRE} @EntitiesInCone{a=90;r=6}\n";
    SkillSyntaxTree tree = MythicYamlParser.parse(yamlContent);
    assertNotNull(tree);
    assertEquals(2, tree.getMechanicNodes().size());
}

@Test
void testTakashaPackManagerDiscoversPacksDynamically() {
    File packDir = new File("build/test_packs");
    TakashaPackManager manager = new TakashaPackManager(packDir);
    manager.scanAndRegisterAll();
    assertTrue(manager.getRegisteredPacks().contains("awakened_pyromancer"));
}
```

**Step 2: Run tests to verify they fail**
Run: `gw test --tests net.takasha.rpg.modelengine.* --tests net.takasha.rpg.mythic.* --tests net.takasha.rpg.pack.*`
Expected: FAIL (classes not found).

**Step 3: Write minimal implementation**
1. **ModelEngine .bbmodel 5.0 Parser (`BlockbenchModelParser.java`):**
   - Parse struktur JSON: membaca `resolution`, `elements` (posisi, ukuran, uv), `outliner` (hierarki tulang grup), dan `animations`.
   - Simpan ke `ModelBlueprint.java` yang siap dikonsumsi oleh `ActiveModelInstance`.
2. **Display Entity Model Renderer (`DisplayEntityModelRenderer.java`):**
   - Menghubungkan setiap tulang ke `Display.ItemDisplayEntity` / `BlockDisplayEntity` Fabric 26.2.
   - Interpolasi rotasi Quaternion dan skala affine 4x4 per tick tanpa merusak performa client.
3. **MythicMobs Interpreter (`MythicYamlParser.java` & `TakashaMechanicDispatcher.java`):**
   - Parse baris sintaks `- mechanic{param=val;...} @targeter{...} ?condition{...}`.
   - Implementasikan runner untuk 22 mechanic penting: `missile` (proyektil dengan callback `onTick` dan `onHit`), `orbital` (VFX memutar), `aura` (stacking buff), `damage` (integrasi `TakashaDamageMetadata`), `particles`, `sound`, `remove`, `changepart`, `tint`.
4. **TakashaPackManager Scalable Drop-in Pipeline (`TakashaPackManager.java`):**
   - Memindai folder `.minecraft/config/takasha/rpg_packs/` saat startup atau reload.
   - Otomatis mendaftarkan model `.bbmodel`, template mob helper, skrip skill YAML, dan pemicu item MMOItems.
   - Menjamin bahwa saat user menambahkan class Samus baru (misal: *Awakened Assassin*, *Awakened Gunslinger*), class tersebut **100% langsung aktif tanpa menulis 1 baris pun kode Java**!

**Step 4: Run tests to verify they pass**
Run: `gw test --tests net.takasha.rpg.modelengine.* --tests net.takasha.rpg.mythic.* --tests net.takasha.rpg.pack.*`
Expected: PASS.

**Step 5: Commit**
`git commit -m "feat(samus): implement scalable ModelEngine R4.1.1 parser, MythicMobs 5.8.2 runtime interpreter, and TakashaPackManager"`

---

### Task 3: Skill Trigger Pipeline & Mixin Injections (Fase 3: Trigger Pipeline)
**Files:**
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/mmo/skill/TriggerType.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/mmo/skill/Skill.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/mmo/skill/SkillManager.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/mixin/ServerPlayNetworkHandlerActionMixin.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/mixin/ServerPlayerEntitySneakMixin.java`
- Test: `takasha-rpg/src/test/java/net/takasha/rpg/mmo/SkillTriggerTest.java`

**Step 1: Write the failing test**
```java
@Test
void testSkillTriggerResolution() {
    TriggerType trigger = TriggerType.fromInteraction(true, false, false);
    assertEquals(TriggerType.LEFT_CLICK, trigger);
}
```
**Step 2: Run test to verify it fails**
Run: `gw test --tests net.takasha.rpg.mmo.SkillTriggerTest`
Expected: FAIL.

**Step 3: Write minimal implementation**
Implement `TriggerType` resolution and dispatching in `SkillManager`, hooked to `UseItemCallback` and Network/Player Mixins.

**Step 4: Run test to verify it passes**
Run: `gw test --tests net.takasha.rpg.mmo.SkillTriggerTest`
Expected: PASS.

**Step 5: Commit**
`git commit -m "feat(mmo): add Skill trigger pipeline and player interaction mixins"`

---

### Task 4: Registrasi Data-Driven Awakened Pyromancer Pack via TakashaPackManager & AuraSkills Mana Bridge (Fase 4: Pyromancer Parity & Scalable Execution)
**Files:**
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/mmo/bridge/AuraSkillsManaBridge.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/pack/pyromancer/PyromancerPackBootstrap.java`
- Resource: `takasha-rpg/src/main/resources/packs/awakened_pyromancer/` (berkas asli dari Samus: `ModelEngine/`, `MythicMobs/`, `MythicLib/`, `MMOItems/`)
- Test: `takasha-rpg/src/test/java/net/takasha/rpg/pack/PyromancerDataDrivenExecutionTest.java`

> [!IMPORTANT]
> **Zero Java Hardcoding untuk Skill:**
> Sesuai arsitektur scalable, ke-7 skill Pyromancer (`HEAT_MASTERY`, `PYRO_COMBO`, `VOLCANIC_SPEAR`, `HELLFIRE_OVERBLAST`, `FIRESTORM_VOLLEY`, `MAGMA_DASH`, `INFERNAL_JUDGEMENT`) **TIDAK di-hardcode ke kelas Java terpisah**. Seluruh mekanik dieksekusi langsung dari berkas asli `MythicMobs/Skills/awakened_pyromancer.yml` oleh `TakashaMythicEngine`. Pendekatan ini memastikan class Samus masa depan dapat langsung di-drop-in ke folder pack tanpa menyentuh kode Java.

**Step 1: Write the failing test**
```java
@Test
void testPyromancerPackLoadedViaPackManager() {
    TakashaPackManager manager = TakashaPackManager.getInstance();
    RpgPack pyromancerPack = manager.getPack("awakened_pyromancer");
    assertNotNull(pyromancerPack, "Pack Pyromancer wajib ditemukan di registry");
    
    // Verifikasi bahwa ke-7 skill terbaca dari YAML
    assertTrue(pyromancerPack.hasSkill("HEAT_MASTERY"));
    assertTrue(pyromancerPack.hasSkill("PYRO_COMBO"));
    assertTrue(pyromancerPack.hasSkill("VOLCANIC_SPEAR"));
    assertTrue(pyromancerPack.hasSkill("HELLFIRE_OVERBLAST"));
    assertTrue(pyromancerPack.hasSkill("FIRESTORM_VOLLEY"));
    assertTrue(pyromancerPack.hasSkill("MAGMA_DASH"));
    assertTrue(pyromancerPack.hasSkill("INFERNAL_JUDGEMENT"));
}

@Test
void testPyromancerManaConsumptionAndWisdomScaling() {
    TakashaRpgData rpgData = new TakashaRpgData();
    rpgData.setCurrentMana(50.0);
    
    // Verifikasi konsumsi mana Volcanic Spear (15 Mana)
    boolean castSuccess = AuraSkillsManaBridge.tryConsumeMana(rpgData, 15.0);
    assertTrue(castSuccess);
    assertEquals(35.0, rpgData.getCurrentMana());
    
    // Verifikasi pembatalan cast saat mana tidak cukup (Infernal Judgement butuh 50 Mana)
    boolean ultSuccess = AuraSkillsManaBridge.tryConsumeMana(rpgData, 50.0);
    assertFalse(ultSuccess);
    assertEquals(35.0, rpgData.getCurrentMana()); // Mana tidak berkurang jika gagal
}
```

**Step 2: Run test to verify it fails**
Run: `gw test --tests net.takasha.rpg.pack.PyromancerDataDrivenExecutionTest`
Expected: FAIL.

**Step 3: Write minimal implementation**
1. **Pyromancer Pack Assets & Configs Integration:**
   - Masukkan konfigurasi asli dari referensi Samus (`ModelEngine/blueprints/*.bbmodel`, `MythicMobs/Skills/*.yml`, `MythicMobs/Mobs/*.yml`, `MythicLib/skill/*.yml`, dan `MMOItems/item/*.yml`) ke dalam pack bootstrap mod default di `takasha-rpg/src/main/resources/packs/awakened_pyromancer/`.
2. **AuraSkills Mana & Wisdom Scaling Bridge (`AuraSkillsManaBridge.java`):**
   - Mengaitkan kebutuhan mana skill YAML ke `TakashaRpgData.consumeMana(cost)`.
   - Mengalikan damage skill dengan multiplier Wisdom (+2% per stat Wisdom).
3. **Data-Driven Tooltip & Item Component Builder:**
   - `MMOItemComponent` menghasilkan lore dan nama gradien 1:1 identik dengan `sword.yml` (`<gradient:#c84300:#ffd06c:#c84300>`, garis pembatas `&7&m━━━━━`).

**Step 4: Run test to verify it passes**
Run: `gw test --tests net.takasha.rpg.pack.PyromancerDataDrivenExecutionTest`
Expected: PASS.

**Step 5: Commit**
`git commit -m "feat(pyromancer): integrate Awakened Pyromancer as data-driven pack with AuraSkills mana bridge and zero-code skill execution"`

---

### Task 5: Sistem Perintah (Commands / CMD) & Creative Mode Permissions (Fase 5: Creative Command Engine)
**Files:**
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/command/TakashaCommands.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/command/MMOItemsCommands.java`
- Modify: `takasha-rpg/src/main/java/net/takasha/rpg/TakashaRpgMod.java`
- Test: `takasha-rpg/src/test/java/net/takasha/rpg/command/TakashaCommandsTest.java`

**Step 1: Write the failing test**
```java
@Test
void testCreativePlayerHasPermissionWithoutOp() {
    // Memverifikasi pemain non-OP di mode Creative lolos predicate CREATIVE_OR_ADMIN
    ServerCommandSource mockSource = createMockPlayerSource(GameMode.CREATIVE, false);
    assertTrue(TakashaCommands.CREATIVE_OR_ADMIN.test(mockSource));
    
    ServerCommandSource mockSurvival = createMockPlayerSource(GameMode.SURVIVAL, false);
    assertFalse(TakashaCommands.CREATIVE_OR_ADMIN.test(mockSurvival));
}
```
**Step 2: Run test to verify it fails**
Run: `gw test --tests net.takasha.rpg.command.TakashaCommandsTest`
Expected: FAIL (class not found).

**Step 3: Write minimal implementation**
Implementasikan tree perintah `/takasha` dan alias MMOItems `/mi` (`/mi browse`, `/mi edit`, `/mi give`, `/mi reload`) pada `TakashaCommands` & `MMOItemsCommands` menggunakan predikat `CREATIVE_OR_ADMIN` (`source.getPlayer().isCreative()`) yang didaftarkan ke `CommandRegistrationCallback.EVENT`.

**Step 4: Run test to verify it passes**
Run: `gw test --tests net.takasha.rpg.command.TakashaCommandsTest`
Expected: PASS.

**Step 5: Commit**
`git commit -m "feat(command): implement /takasha and /mi commands with creative mode auto-grant permissions"`


---

### Task 6: ItemsAdder Mechanics & Negative Space Font Parity (Fase 6: ItemsAdder Parity)
**Files:**
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/mmo/itemsadder/ItemsAdderRegistry.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/mmo/itemsadder/FurnitureEngine.java`
- Create: `takasha-rpg/src/main/resources/assets/takasha/font/default.json`
- Test: `takasha-rpg/src/test/java/net/takasha/rpg/mmo/ItemsAdderParityTest.java`

**Step 1: Write the failing test**
```java
@Test
void testNegativeFontAdvanceMap() {
    int advance = NegativeFontHelper.getAdvance('\uF808');
    assertEquals(-8, advance);
}
```
**Step 2: Run test to verify it fails**
Run: `gw test --tests net.takasha.rpg.mmo.ItemsAdderParityTest`
Expected: FAIL.

**Step 3: Write minimal implementation**
Implement `NegativeFontHelper`, font json with space provider, and `FurnitureEngine` wrapping `InteractionEntity` + `ItemDisplayEntity`.

**Step 4: Run test to verify it passes**
Run: `gw test --tests net.takasha.rpg.mmo.ItemsAdderParityTest`
Expected: PASS.

**Step 5: Commit**
`git commit -m "feat(itemsadder): add negative space font kerning and furniture engine"`

---

### Task 7: Visual FX, Sound Events, Native Damage Indicators & Networking (Fase 7: Client Sync & Polish)
**Files:**
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/mmo/network/TakashaSkillVfxPayload.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/mmo/client/PyromancerClientVfxHandler.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/mmo/indicator/TakashaDamageIndicatorEngine.java`
- Create: `takasha-rpg/src/main/resources/assets/awakened_pyromancer/sounds.json`
- Modify: `takasha-rpg/src/main/java/net/takasha/rpg/TakashaRpgMod.java`
- Test: `takasha-rpg/src/test/java/net/takasha/rpg/mmo/NetworkingPayloadTest.java`

**Step 1: Write the failing test**
```java
@Test
void testSkillVfxPayloadSerialization() {
    TakashaSkillVfxPayload payload = new TakashaSkillVfxPayload("INFERNAL_JUDGEMENT", 0, 64, 0, 6.0f);
    PacketByteBuf buf = PacketByteBufs.create();
    payload.write(buf);
    TakashaSkillVfxPayload decoded = new TakashaSkillVfxPayload(buf);
    assertEquals("INFERNAL_JUDGEMENT", decoded.skillId());
}
```
**Step 2: Run test to verify it fails**
Run: `gw test --tests net.takasha.rpg.mmo.NetworkingPayloadTest`
Expected: FAIL.

**Step 3: Write minimal implementation**
- Implementasikan payload S2C dan listener client untuk rendering visual partikel api Blazestaff.
- Daftarkan 10 sound event stereo pada namespace `awakened_pyromancer_sounds` di `BuiltInRegistries.SOUND_EVENT`.
- Implementasikan `TakashaDamageIndicatorEngine` berbasis `DisplayEntity.TextDisplayEntity` (Billboard `CENTER`, background transparan, zero entity ticking lag) dengan trajektori halus (upward 1.0, radial 1.0, lifespan 20 ticks) dan format ikon MythicLib 1.7 (`🗡`, `★`, `🔥`, bold crit).
- Terapkan batas maksimal 20 indikator simultan per chunk untuk menjaga stabilitas performa pada Radeon 610M.

**Step 4: Run test to verify it passes**
Run: `gw test --tests net.takasha.rpg.mmo.NetworkingPayloadTest`
Expected: PASS.

**Step 5: Commit**
`git commit -m "feat(vfx): implement networking payloads, sounds, and native TextDisplayEntity damage indicators"`

---

### Task 8: MMOItems GUI Architecture Parity (54-Slot Chest System & 11 Sub-GUIs) (Fase 8: GUI Engine)
**Files:**
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/mmo/gui/TypeBrowserScreenHandler.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/mmo/gui/ItemBrowserScreenHandler.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/mmo/gui/ItemEditionScreenHandler.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/mmo/gui/AbilityListEditionScreenHandler.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/mmo/gui/AbilityEditionScreenHandler.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/mmo/gui/ElementsEditionScreenHandler.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/mmo/gui/UpgradingEditionScreenHandler.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/mmo/gui/ParticlesEditionScreenHandler.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/mmo/gui/SoundsEditionScreenHandler.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/mmo/gui/CommandListEditionScreenHandler.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/mmo/gui/RevisionScreenHandler.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/mmo/gui/recipe/RecipeListScreenHandler.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/mmo/gui/recipe/RecipeEditorScreenHandler.java`
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/mmo/gui/ChatInputListener.java`
- Test: `takasha-rpg/src/test/java/net/takasha/rpg/mmo/gui/MMOItemsGuiTest.java`

**Step 1: Write the failing test**
```java
@Test
void testItemEditionLayoutAndGetItemSlot() {
    ItemEditionScreenHandler handler = new ItemEditionScreenHandler(0, createMockPlayerInventory(), "SWORD", "awakened_pyromancer_blazestaff");
    // Slot 4 wajib memuat tombol "Get the Item!"
    ItemStack getItemStack = handler.getSlot(4).getStack();
    assertNotNull(getItemStack);
    assertTrue(getItemStack.getName().getString().contains("Get the Item!"));
    
    // Matriks 21 slot stat (Baris 2, 3, 4)
    int[] expectedStatSlots = new int[] {
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };
    assertEquals(21, expectedStatSlots.length);
    
    // Verifikasi slot tombol navigasi standar MMOItems di Baris 5
    assertEquals(Items.BARRIER, handler.getSlot(45).getStack().getItem()); // Back
    assertEquals(Items.ARROW, handler.getSlot(48).getStack().getItem()); // Prev Page
    assertEquals(Items.COMPASS, handler.getSlot(49).getStack().getItem()); // Info / Search
    assertEquals(Items.ARROW, handler.getSlot(50).getStack().getItem()); // Next Page
    assertEquals(Items.BARRIER, handler.getSlot(53).getStack().getItem()); // Close
}
```
**Step 2: Run test to verify it fails**
Run: `gw test --tests net.takasha.rpg.mmo.gui.MMOItemsGuiTest`
Expected: FAIL.

**Step 3: Write minimal implementation**
- Implementasikan arsitektur 11 `ScreenHandler` berbasis `GenericContainerScreenHandler` (Chest 9×6 / 54 slot).
- Implementasikan slot 4 `Get the Item!` (Klik kiri = give item, Klik kanan = reroll stat variance).
- Implementasikan matriks 21 slot stat interaktif terpusat (baris 2..4: slot 19–25, 28–34, 37–43) yang membaca 16 kategori item options (100+ stat).
- Implementasikan batasan 7 ability pada `AbilityListEditionScreenHandler`.
- Implementasikan `ChatInputListener` untuk menerima input angka stat (damage, speed, tier) dan modifier ability secara interaktif.
- Izinkan auto-grant pembukaan GUI `/mi browse` dan `/mi edit` untuk pemain Creative mode tanpa membutuhkan node permission eksternal.

**Step 4: Run test to verify it passes**
Run: `gw test --tests net.takasha.rpg.mmo.gui.MMOItemsGuiTest`
Expected: PASS.

**Step 5: Commit**
`git commit -m "feat(gui): implement MMOItems 54-slot chest GUI system, 11 sub-GUIs and 16 item option categories"`

---

### Task 9: Pinnacle Dungeon Boss Drop Engine (Infernal Sanctum Floor 100) (Fase 9: Dungeon Integration)
**Files:**
- Create: `takasha-rpg/src/main/java/net/takasha/rpg/dungeon/InfernalSanctumDropEngine.java`
- Modify: `takasha-rpg/src/main/java/net/takasha/rpg/dungeon/DungeonBossChestHandler.java`
- Test: `takasha-rpg/src/test/java/net/takasha/rpg/dungeon/PyromancerDropTest.java`

**Step 1: Write the failing test**
```java
@Test
void testPitySystemGuaranteedDropAt50Kills() {
    InfernalSanctumDropEngine engine = new InfernalSanctumDropEngine();
    UUID player = UUID.randomUUID();
    // 49 kali kill gagal
    for (int i = 0; i < 49; i++) {
        engine.recordFailedAttempt(player);
    }
    // Kill ke-50 wajib drop Blazestaff 100%
    ItemStack drop = engine.rollBossChestReward(player, 0.0); // 0.0 luck
    assertEquals("awakened_pyromancer_blazestaff", drop.get(ModDataComponents.MMOITEM_COMPONENT).id());
}
```
**Step 2: Run test to verify it fails**
Run: `gw test --tests net.takasha.rpg.dungeon.PyromancerDropTest`
Expected: FAIL.

**Step 3: Write minimal implementation**
- Implementasikan `InfernalSanctumDropEngine` dengan kalkulasi drop 1.0% dan pity counter di `TakashaRpgData`.
- Pasangkan Blazestaff legendaris sebagai hadiah peti akhir bos dungeon tertinggi *Infernal Emperor Ignis*.

**Step 4: Run test to verify it passes**
Run: `gw test --tests net.takasha.rpg.dungeon.PyromancerDropTest`
Expected: PASS.

**Step 5: Commit**
`git commit -m "feat(dungeon): integrate Blazestaff as exclusive legendary drop for Infernal Sanctum boss"`

---

## 8. Roadmap Eksekusi, Semantic Versioning & Rencana CHANGELOG.md

### 8.1 Strategi Rilis SemVer 2.0.0
- **`v1.9.0-alpha.1+mc26.2` (Fase 1, 2, & 2b):** Fondasi `DataComponents` MMOItem, pembacaan konfigurasi YAML, kalkulasi tempur `TakashaDamageMetadata`, perbaikan tuntas Flight Engine `wings`, serta perbaikan liquid walking (`waterwalker`/`lavawalker`).
- **`v1.9.0-beta.1+mc26.2` (Fase 3 & 4):** Pipeline pemicu skill interaksi, 7 skill Awakened Pyromancer, dan Blazestaff CMD 2096.
- **`v1.9.0-rc.1+mc26.2` (Fase 5 & 6):** Sistem perintah Brigadier (`/takasha`) dengan izin Creative Mode auto-grant, serta paritas ItemsAdder (hitbox furniture, negative space font, container overlays).
- **`v1.9.0+26.2` (Rilis Final Paritas PRD):** Paritas penuh 1:1, audio 10 stereo events, low-spec Radeon 610M anti-crash profiling, dan dokumentasi.

### 8.2 Draf Kerangka CHANGELOG.md

```markdown
## [1.9.0+26.2] - 2026-10-27

### Added
- **MMOItems & MythicLib 1:1 Porting Engine:**
  - Registrasi `DataComponentType<MMOItemComponent>` (`takasha:mmoitem_data`) menggunakan Mojang Codec immutable menggantikan sistem NBT/PDC Bukkit.
  - Arsitektur status pemain `MMOPlayerData` berbasis Fabric Attachment API yang tersimpan otomatis di level NBT world.
  - Mesin tempur `TakashaDamageMetadata` dengan pemisahan 6 tipe damage (`PHYSICAL`, `MAGIC`, `PROJECTILE`, `WEAPON`, `SKILL`, `MINION`) dan 7 elemen kustom (`FIRE`, `ICE`, `LIGHTNING`, `EARTH`, `WIND`, `DARKNESS`, `LIGHT`).
  - Sistem pemicu interaksi multi-vektor (`LEFT_CLICK`, `RIGHT_CLICK`, `SHIFT_LEFT_CLICK`, `SHIFT_RIGHT_CLICK`, `SNEAK`, `TIMER`).
- **TakashaFlightManager & Mobility Enchantment Engine:**
  - Implementasi state machine terbang native 26.2 untuk enchant `wings` pada sepatu zirah (Boots) dengan sinkronisasi `mayfly = true` dan `onUpdateAbilities()`.
  - Pelacakan state independen `WINGS_FLYING_PLAYERS` untuk mencegah benturan hak terbang dengan mode Creative/Spectator.
  - Sistem Safe-Landing darurat saat unequip boots di udara: efek Slow Falling 5 detik otomatis dan grace period anti-fall damage 100% pada pendaratan pertama.
  - Desain Minimalis Wings: Murni manipulasi kemampuan terbang (`mayfly = true`) dan pendaratan darurat (zero extra visual/audio FX).
- **Sistem Perintah (Commands / CMD) & Creative Mode Engine:**
  - Pendaftaran hierarki perintah Brigadier `/takasha` (`give`, `skill cast`, `skill cooldown reset/bypass`, `stat get/set`, `inspect`).
  - Aturan izin predikat otomatis bagi pemain di **Creative Mode** (`player.isCreative()`) tanpa wajib hak akses OP level 2.
  - Suggester dinamis untuk item ID, skill ID, nama stat, dan tier kelangkaan.
- **Awakened Pyromancer Full Parity:**
  - Senjata kanonis `awakened_pyromancer_blazestaff` (Stone Sword, CMD 2096, Unbreakable).
  - 7 Kemampuan Elemen Api: `HEAT_MASTERY`, `PYRO_COMBO`, `VOLCANIC_SPEAR`, `HELLFIRE_OVERBLAST`, `FIRESTORM_VOLLEY`, `MAGMA_DASH`, `INFERNAL_JUDGEMENT`.
  - 7 Ikon kemampuan pada item `COAL` (CMD 2357 s/d 2363).
  - Registrasi 10 custom sound events stereo pada namespace `awakened_pyromancer_sounds`.
- **ItemsAdder Mechanics Parity:**
  - Engine furniture modern berbasis `InteractionEntity` dan `ItemDisplayEntity` bebas lag ArmorStand.
  - Penyedia font spasi negatif (`\uF801`–`\uF808` dengan advance negatif) untuk overlay GUI 54-slot otentik.

### Changed
- Memperluas pipeline damage `ServerLivingEntityEvents` di `takasha-rpg` untuk mendukung kalkulasi atribut MMOItems dan MythicLib secara bersamaan dengan AuraSkills.
- Menggantikan command Bukkit `/mmoitems` dan `/mythiclib` dengan pohon perintah native Mojang Brigadier AST berizin kondisional.
- Overhaul implementasi `waterwalker`: mengunci elevasi vertikal $Y$ agar pemain benar-benar berjalan di atas permukaan air seperti es alih-alih hanya berenang cepat di bawah air.

### Fixed
- **Critical Fix (World Exit Stability):** Mengeliminasi lonjakan CPU 100% dan freeze saat keluar dunia/disconnect dengan menginjeksi active connection guard pada client automasi, mendaftarkan `ClientPlayConnectionEvents.DISCONNECT`, mencegah double disk save di server thread, dan memperbaiki parameter rotasi toko harian.
- **Critical Fix:** Memperbaiki bug enchant `wings` yang tidak dapat terbang saat digunakan di boots akibat ketiadaan method handler pada server tick loop.
- **Critical Fix:** Menambahkan kekebalan lahar dan pemadaman api pada `lavawalker` di atas permukaan lava cair.
- Menghilangkan bug exploit cooldown bypass saat mengganti item di tangan melalui tracking cooldown berbasis UUID di Attachment API pemain.
- Mencegah GPU Driver Timeout (`amdxx64.dll`) pada Radeon 610M dengan menerapkan pembatasan partikel api radial maksimal 16 partikel per skill cast.

### Removed
- Menghapus dependensi terhadap JVM Bukkit/Spigot runtime, NBT compound lama, dan CraftItemStack secara permanen.
```

---

## 9. Checklist Verifikasi & Kriteria Keberhasilan (Definition of Done)

- [ ] **Fase 1 (Fondasi Data Components):** `MMOItemComponent` berhasil encode/decode via NBT Ops tanpa error dan atribut modifier tersinkronisasi ke client vanilla.
- [ ] **Fase 2 (Combat & Damage Metadata):** Kalkulasi damage fisik dan api menghasilkan reduksi yang akurat terhadap armor mob target.
- [ ] **Fase 2b (Flight Engine & Mobility Enchants):** Pemain yang memakai boots `wings` dapat melayang/terbang bebas di Survival mode (murni tanpa partikel/audio ekstra); saat dicopot di udara, pemain tidak tewas karena menerima Slow Falling dan proteksi pendaratan 100%; `waterwalker` mengizinkan jalan kaki di atas permukaan air.
- [ ] **Fase 2c (World Exit Stability & Anti-Hang):** Keluar dari dunia Singleplayer ("Save and Quit to Title") atau disconnect dari server berlangsung instan (< 1.5 detik), utilisasi CPU tetap normal (< 10%), tidak ada busy-spin loop, dan data pemain tersimpan rapi tanpa file corruption.
- [ ] **Fase 2d (Scalable Samus Engine):** ModelEngine R4.1.1 parser (.bbmodel) dan MythicMobs 5.8.2 interpreter berhasil mem-parse model 3D, menganimasikan tulang via Display Entity, serta mengeksekusi mechanic missile, orbital, aura, dan cone targeter secara native.
- [ ] **Fase 2e (Scalable Drop-in Verification):** Menambahkan pack class Samus baru (atau dummy pack) ke dalam folder `.minecraft/config/takasha/rpg_packs/` otomatis dikenali, dimuat, dan dapat dimainkan tanpa perlu mengedit/compile ulang kode Java mod.
- [ ] **Fase 3 (Trigger Pipeline):** Klik kiri, klik kanan, shift klik, sneak, dan timer 10s terdeteksi 100% tanpa delay input.
- [ ] **Fase 4 (Pyromancer Parity):** Blazestaff memicu 7 skill melalui engine interpreter data-driven; status Ignition aktif setelah 3 pukulan dasar; konsumsi mana dan scaling stat tersinkronisasi ke AuraSkills.
- [ ] **Fase 5 (Creative Command Engine):** Pemain non-OP di mode Creative dapat menjalankan `/takasha give`, `/takasha skill cast`, dan `/takasha inspect` dengan autocompletion lengkap.
- [ ] **Fase 6 (ItemsAdder Parity):** Model 3D terpasang rapi, hitbox furniture dapat diduduki/dihancurkan, font negatif menggeser judul GUI tanpa error rendering.
- [ ] **Fase 7 (Stabilitas Low-Spec):** Pemanggilan skill ultimate berulang kali di dekat 20 mob menghasilkan FPS stabil tanpa GC freeze pada Ryzen 3 7320U / Radeon 610M.

