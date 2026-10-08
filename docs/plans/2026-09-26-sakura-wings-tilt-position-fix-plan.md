# Planning Perbaikan Posisi & Kemiringan Sakura Wings (CEM Elytra)

> **Ringkasan:** Berdasarkan analisis screenshot in-game terbaru (`media_1790407459297.png`), masalah tinggi (melayang di langit) dan pemisahan ranting sudah teratasi dengan baik. Namun, muncul dua masalah baru:
> 1. **Model miring secara diagonal sebesar ~15° (tilted)**.
> 2. **Pusat model bergeser ke kiri punggung player (~8.5 hingga 10 pixel Minecraft dari tulang belakang/spine)**.

---

## 1. Analisis Root Cause (Penyebab Masalah)

### A. Mengapa Model Miring ~15°?
1. Pada Minecraft vanilla, class `ElytraModel.setupAnim()` menerapkan pose istirahat (resting pose) saat player berdiri:
   - `xRot` (Pitch): `+0.2618 rad` (+15.0°)
   - `zRot` (Roll): `-0.2618 rad` (-15.0°)
   - `yRot` (Yaw): `0.0 rad`
2. Pada implementasi sebelumnya, kita memasang script animasi CEM:
   ```json
   "animations": [
     {
       "left_wing.rx": "0",
       "left_wing.ry": "0",
       "left_wing.rz": "0"
     }
   ]
   ```
3. **Mengapa animasi CEM tersebut tidak berpengaruh?**
   - Mod **Entity Model Features (EMF)** memperlakukan `elytra.jem` sebagai layer equipment khusus (bukan mob standar).
   - Minecraft `ElytraFeatureRenderer` mengeksekusi rotasi vanilla `setupAnim()` langsung pada `left_wing`, dan EMF tidak menimpa rotasi part akar ini dengan animasi `"0"`.
   - Akibatnya, `left_wing` tetap berotasi roll `-15°` dan pitch `+15°`, menyebabkan seluruh pohon Sakura miring secara diagonal sebesar tepat 15.00°.

### B. Mengapa Pusat Model Bergeser ke Kiri (~8.5 pixel)?
1. Pivot rotasi `left_wing` pada vanilla Minecraft berada di koordinat `[5.0, 0.0, 0.0]`.
2. Pada implementasi sebelumnya, submodel dipasang dengan translasi:
   `translate: [-5.0, -6.0, 3.5]`
3. Ketika parent `left_wing` berotasi roll `-15°` dan pitch `+15°` mengitari pivot `(5.0, 0.0, 0.0)`, vektor `[-5.0, -6.0, 3.5]` ikut terayun (*swing arc*):
   $$\vec{P}_{world} = \begin{bmatrix} 5.0 \\ 0.0 \\ 0.0 \end{bmatrix} + R_z(-15^\circ) R_x(15^\circ) \begin{bmatrix} -5.0 \\ -6.0 \\ 3.5 \end{bmatrix} = \begin{bmatrix} -1.56 \\ -5.18 \\ 1.83 \end{bmatrix}$$
4. Karena seluruh pohon berotasi 15° dari titik tersebut, pusat emblem Sakura terlempar ke arah kiri tubuh player (tampak di belakang bahu kiri pada screenshot).

---

## 2. Solusi Matematika (Hierarchical Kinematic Inversion)

Untuk meniadakan kemiringan 15° dan pergeseran koordinat tanpa bergantung pada engine animasi EMF, kita menerapkan **Hierarchical Kinematic Counter-Rotation**:

### A. Pembatalan Rotasi (Counter-Rotation)
Diberikan matriks rotasi parent bawaan vanilla:
$$R_{parent} = R_z(-15^\circ) \cdot R_x(+15^\circ)$$

Kita menyusun submodel bersarang (*nested hierarchy*) di mana setiap level membatalkan satu sumbu rotasi secara independen:
1. **Node 1 (Pitch Cancellation):** `rotate: [-15.0, 0.0, 0.0]` ($R_x(-15^\circ)$)
2. **Node 2 (Roll Cancellation):** `rotate: [0.0, 0.0, 15.0]` ($R_z(+15^\circ)$)
3. **Node 3 (Orientation):** `rotate: [0.0, -90.0, 0.0]` ($R_y(-90^\circ)$ agar sayap melebar rata di punggung)

Hasil perkalian matriks total di world space:
$$R_{total} = R_{parent} \cdot (R_x(-15^\circ) \cdot R_z(+15^\circ) \cdot R_y(-90^\circ))$$
$$= [R_z(-15^\circ) R_x(15^\circ)] \cdot [R_x(-15^\circ) R_z(15^\circ) R_y(-90^\circ)] = R_y(-90^\circ)$$
- **Pitch akhir:** $0.00^\circ$ (tegak lurus)
- **Roll akhir:** $0.00^\circ$ (horizontal sempurna, kemiringan 0°)
- **Yaw akhir:** $-90.00^\circ$ (menghadap ke belakang)

### B. Kompensasi Posisi (Translation Offset)
Kita menginginkan pusat batang sayap berada tepat di tengah punggung:
$$\vec{P}_{desired} = \begin{bmatrix} 0.0 \\ -6.0 \\ 3.5 \end{bmatrix}$$

Dengan pivot parent $\vec{Pivot} = [5.0, 0.0, 0.0]$, translasi $\vec{T}_1$ yang harus diisikan pada node terluar adalah:
$$\vec{T}_1 = (R_{parent})^T \cdot (\vec{P}_{desired} - \vec{Pivot})$$
$$\vec{T}_1 = \begin{bmatrix} -3.2767 \\ -5.9422 \\ 5.2157 \end{bmatrix}$$

**Verifikasi Koordinat Ujung Sayap:**
- Batang tengah: $[X = 0.0, Y = -6.0, Z = 3.5]$ (tepat di tulang belakang)
- Ujung sayap kiri: $[X = -15.0, Y = -6.0, Z = 3.5]$
- Ujung sayap kanan: $[X = +15.0, Y = -6.0, Z = 3.5]$
- Tinggi $Y$ kedua ujung sayap sama persis (simetris horizontal sempurna)!

---

## 3. Rencana Eksekusi Langkah-demi-Langkah

### Task 1: Update Generator `build_sakura_wings_etf_emf.py`
- Ubah struktur `left_wing_submodels` menjadi hierarki 3 lapis pembatal rotasi untuk:
  - `sakura_wings_static`
  - `sakura_wings_anim1`
  - `sakura_wings_anim2`
- Terapkan parameter translasi kompensasi $\vec{T}_1 = [-3.2767, -5.9422, 5.2157]$.
- Pastikan texture dan frame texture mapping pada animasi 01 dan 02 tetap terikat dengan benar ke child submodels.

### Task 2: Eksekusi Build & Regenerasi JEM
- Jalankan `python build_sakura_wings_etf_emf.py`.
- Verifikasi file yang dihasilkan di:
  `sakura-resourcepack/assets/minecraft/optifine/cem/elytra.jem`
- Cek struktur JSON untuk memastikan tidak ada syntax error atau property yang hilang.

### Task 3: Verifikasi Konsistensi CIT & Resource Pack
- Jalankan `python validate_cit_and_items.py`.
- Pastikan 32 item CIT di OptiFine dan CIT Resewn tetap 100% sinkron dan lulus validasi tanpa error.

### Task 4: Bump Version & Packaging
- Bump versi dari `1.2.1` ke `1.2.2` di:
  - `VERSION`
  - `PRD.md`
- Jalankan `python build.py` untuk mengompilasi `Takasha-1.2.2.zip`.
- Konfirmasikan siap diuji oleh pengguna di Minecraft dengan EMF/ETF.
