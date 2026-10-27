# 🌐 Takasha-RPG Web Companion

[![Runtime](https://img.shields.io/badge/Runtime-Node.js%20(Zero%20Dependencies)-brightgreen.svg)](https://nodejs.org/)
[![Port](https://img.shields.io/badge/Port-5173-blue.svg)](#)
[![Stack](https://img.shields.io/badge/Stack-HTML5%20%7C%20CSS3%20%7C%20Vanilla%20JS-orange.svg)](#)

Aplikasi web pendamping resmi (*Official Web Companion*) untuk **Takasha-RPG**. Berfungsi sebagai database interaktif, perencana build karakter (*stat planner*), katalog lengkap custom enchantment 6-tier, dan panduan taktik dungeon rift.

---

## 📑 Daftar Isi
- [Fitur Utama](#-fitur-utama)
- [Cara Menjalankan](#-cara-menjalankan)
- [Struktur File](#-struktur-file)
- [Integrasi Data](#-integrasi-data)

---

## 🌟 Fitur Utama

1. **Katalog Interaktif 6-Tier Custom Enchantments**:
   - Menelusuri seluruh custom enchantment dari tier Simple hingga Fabled.
   - Pencarian instan dan filter berdasarkan target perlengkapan (Sword, Bow, Armor, Tool).
   - Penjelasan detail mengenai efek proc, cooldown, damage scaling, dan tingkat keberhasilan (*success/destroy rate*).

2. **AuraSkills Build & Stat Calculator**:
   - Simulasi alokasi poin skill untuk 7 tipe skill AuraSkills (Farming, Mining, Combat, Foraging, Fishing, Agility, Alchemy).
   - Penghitungan otomatis atribut karakter: Maksimum HP, Mana Pool, Strength, Toughness, Ferocity, dan Luck.

3. **Dungeon Rift Compendium**:
   - Panduan taktik untuk 5 tingkatan Dungeon Arena (Coal, Iron, Gold, Diamond, Netherite).
   - Rincian gelombang musuh (*wave survival*), kemampuan boss, dan tabel reward material.

4. **Desain Modern & Responsif**:
   - Antarmuka bertema gelap (*dark mode*) dengan sentuhan visual glassmorphism dan tipografi modern.
   - Ringan, cepat, dan dapat diakses dari browser desktop maupun mobile.

---

## 🚀 Cara Menjalankan

Web companion ini menggunakan server HTTP native Node.js tanpa memerlukan dependensi eksternal (`npm install` tidak diperlukan!).

### Menjalankan Server:
```powershell
# Masuk ke direktori web companion
cd "d:/Mod Minecraft/weapon set/sakura/takasha-rpg-web"

# Jalankan server
node server.js
```

Setelah server aktif, buka peramban web (*browser*) Anda dan kunjungi:
👉 **`http://localhost:5173/`**

---

## 📁 Struktur File

```
takasha-rpg-web/
├── server.js        # Server HTTP native Node.js (Port 5173, MIME types handler)
├── index.html       # Struktur HTML5 utama aplikasi companion
├── style.css        # Desain CSS3 kustom dengan sistem warna glassmorphism
├── app.js           # Logika interaktif frontend (filtering, search, kalkulator)
└── data/            # JSON dataset (daftar enchants, skill XP curve, dungeon loot)
```

---

## 🔗 Integrasi Data

Dataset JSON pada folder `data/` disinkronkan langsung dengan kode sumber Java `takasha-rpg`, memastikan seluruh statistik dan nilai multiplier yang ditampilkan pada web 100% akurat dengan gameplay Minecraft in-game.
