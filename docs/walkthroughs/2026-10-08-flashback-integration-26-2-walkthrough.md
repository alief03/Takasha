# Walkthrough: Integrasi Mendalam Flashback Mod (MC 26.2)

Dukungan integrasi Flashback Mod (`Flashback-0.43.4-for-MC26.2`) telah selesai diimplementasikan khusus untuk versi **Minecraft 26.2** (`versions/26.2`). Modul **1.21.11** tetap 100% bersih tanpa perubahan.

---

## 🌸 Ringkasan Fitur yang Diimplementasikan

### 1. Dear ImGui Floating Studio (`TakashaStudioWindow`)
Jendela ImGui mengambang khusus di dalam overlay editor Flashback dengan 4 tab komprehensif:
- **Tab 1: Radar NPC di Sekitar**: Mendeteksi seluruh `TakashaNpcEntity` dalam radius 64 blok, menampilkan nama, UUID, jarak, tombol **Arahkan Kamera (Look At)**, dan tombol **Pilih**.
- **Tab 2: Katalog Pose & Emote**:
  - Filter kategori pose: *Semua*, *Combat*, *Casual*, *Valentine*, dan *Dragon Mecha*.
  - Pemilih Emote Emotecraft interaktif dengan **kotak pencarian teks langsung (Search Bar)**.
  - Slider rotasi sendi (Kepala, Torso, Lengan Kiri/Kanan, Kaki Kiri/Kanan) dalam rentang -180° hingga +180°.
  - Tombol mode putar emote (*Loop*, *Tahan / Freeze*, *Sekali / Once*).
- **Tab 3: Kosmetik & Sayap**:
  - Pengatur kecepatan animasi kepakan sayap (`0.0x` - `3.0x`).
  - Toggle **Bekukan Animasi Sayap (Freeze)** untuk pose sinematik statis.
  - Toggle **Partikel Kelopak Sakura**.
- **Tab 4: Timeline Keyframing**:
  - Menampilkan tick replay aktif secara real-time.
  - Tombol **Simpan Keyframe di Tick Ini** untuk merekam pose berbeda pada detik/tick replay yang berbeda.
  - Daftar keyframe aktif dengan tombol hapus.

### 2. Integrasi Menu Bar Flashback (`MainMenuBarMixin`)
- Menambahkan menu dropdown **🌸 Sakura** langsung di Main Menu Bar atas Flashback.
- Pilihan menu:
  - **Studio Sakura (F8)**: Membuka/menutup `TakashaStudioWindow`.
  - **Reset Semua Pose Override**: Mengembalikan semua NPC ke rekaman asli.

### 3. Integrasi Klik Kanan Entitas Flashback (`SelectedEntityPopupMixin` & `FlashbackSakuraPanel`)
- Panel ImGui langsung muncul saat entitas `TakashaNpcEntity` diklik di editor Flashback.
- **Entitas Pemain (`Player` / `AbstractClientPlayer`) tetap vanilla 100% murni** tanpa gangguan.

### 4. Sinkronisasi Sub-Tick Sayap & Animasi Prosedural (`SakuraWingsFeatureRenderer` & `TakashaNpcRenderer`)
- Animasi kepakan sayap menggunakan `FlashbackCompatHelper.getPartialReplayTick()` sehingga saat export slow-motion (misal 0.1x - 0.5x speed) kepakan tetap mulus tanpa patah-patah.
- Waktu animasi model NPC (`npcState.animTime`) dikunci ke sub-tick replay saat sesi Flashback aktif.

### 5. Failsafe & Modularitas Bersih
- Semua pemanggilan Flashback dibungkus `SakuraMixinPlugin` yang mendeteksi ketersediaan mod `flashback` lewat `FabricLoader`.
- Jika Flashback tidak terpasang, mod tetap berjalan normal tanpa crash (Zero Crash Guarantee).

---

## 🛠️ File yang Dibuat & Dimodifikasi

| Komponen | File Path | Aksi |
|---|---|---|
| Helper Flashback | [FlashbackCompatHelper.java](file:///d:/Mod%20Minecraft/weapon%20set/sakura/sakura-weapons/versions/26.2/src/main/java/net/sakura/weapons/compat/flashback/FlashbackCompatHelper.java) | Dibuat |
| Replay Studio ImGui | [TakashaStudioWindow.java](file:///d:/Mod%20Minecraft/weapon%20set/sakura/sakura-weapons/versions/26.2/src/main/java/net/sakura/weapons/compat/flashback/TakashaStudioWindow.java) | Dibuat |
| Panel Entitas Popup | [FlashbackSakuraPanel.java](file:///d:/Mod%20Minecraft/weapon%20set/sakura/sakura-weapons/versions/26.2/src/main/java/net/sakura/weapons/compat/flashback/FlashbackSakuraPanel.java) | Dimodifikasi |
| Menu Bar Mixin | [MainMenuBarMixin.java](file:///d:/Mod%20Minecraft/weapon%20set/sakura/sakura-weapons/versions/26.2/src/main/java/net/sakura/weapons/mixin/flashback/MainMenuBarMixin.java) | Dibuat |
| Overlay Render Mixin | [ReplayUIMixin.java](file:///d:/Mod%20Minecraft/weapon%20set/sakura/sakura-weapons/versions/26.2/src/main/java/net/sakura/weapons/mixin/flashback/ReplayUIMixin.java) | Dibuat |
| Konfigurasi Mixin | [sakura_weapons.mixins.json](file:///d:/Mod%20Minecraft/weapon%20set/sakura/sakura-weapons/versions/26.2/src/main/resources/sakura_weapons.mixins.json) | Dimodifikasi |
| Replay Override Manager | [ReplayPoseOverrideManager.java](file:///d:/Mod%20Minecraft/weapon%20set/sakura/sakura-weapons/versions/26.2/src/main/java/net/sakura/weapons/client/replay/ReplayPoseOverrideManager.java) | Dimodifikasi |
| Renderer Sayap Sakura | [SakuraWingsFeatureRenderer.java](file:///d:/Mod%20Minecraft/weapon%20set/sakura/sakura-weapons/versions/26.2/src/main/java/net/sakura/weapons/client/render/SakuraWingsFeatureRenderer.java) | Dimodifikasi |
| Renderer Entitas NPC | [TakashaNpcRenderer.java](file:///d:/Mod%20Minecraft/weapon%20set/sakura/sakura-weapons/versions/26.2/src/main/java/net/sakura/weapons/client/render/entity/TakashaNpcRenderer.java) | Dimodifikasi |

---

## 📦 Hasil Verifikasi & Build

Perintah `gradlew buildAll` sukses dikompilasi tanpa galat:
- **`Takasha-2.2.0+1.21.11.jar`** (1,605,753 bytes) di folder `Hasil mod`
- **`Takasha-2.2.0+26.2.jar`** (1,784,686 bytes) di folder `Hasil mod`
