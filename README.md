<div align="center">

# X-Tray

**Dynamic Island untuk desktop Linux.**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.21-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.13-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://www.jetbrains.com/compose-multiplatform/)
[![Material 3 Expressive](https://img.shields.io/badge/Material%203-Expressive-6750A4?style=for-the-badge&logo=materialdesign&logoColor=white)](https://m3.material.io/)
[![Linux](https://img.shields.io/badge/Linux-X11%20%7C%20Wayland-FCC624?style=for-the-badge&logo=linux&logoColor=black)](https://kernel.org/)
[![Release](https://img.shields.io/github/v/release/andreasmlbngaol/x-tray?style=for-the-badge&logo=github&color=6E40C9)](https://github.com/andreasmlbngaol/x-tray/releases)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-D22128?style=for-the-badge&logo=apache&logoColor=white)](./LICENSE)

<img src="docs/images/hero.png" alt="X-Tray dalam mode closed di tengah atas layar" width="760">

</div>

X-Tray adalah satu panel kecil yang menempel di tengah atas layar. Saat tidak
dipakai ia cuma selebar 300 px: jam, tanggal, kecepatan internet, dan baterai.
Begitu kursor mendekat ia membesar untuk menampilkan musik dan notifikasi, dan
kalau diklik ia terbuka penuh berisi kontrol cepat, clipboard, timer fokus,
shelf, dan pengaturan.

Tidak ada window yang perlu dibuka, tidak ada ikon tambahan di taskbar. Semua
yang biasanya tersebar di beberapa tempat diringkas jadi satu panel yang bisa
dipanggil kapan saja.

## Yang bisa kamu lakukan

- **Lihat sekilas tanpa klik.** Jam, tanggal, kecepatan upload/download, dan
  baterai selalu ada di bar kecil itu.
- **Dengar musik tanpa pindah aplikasi.** Judul, artis, dan album muncul di
  peek; tombol putar, sebelumnya, dan berikutnya ada di sana.
- **Baca notifikasi di satu tempat.** X-Tray mengambil alih notifikasi desktop,
  jadi popup bawaan berhenti dan semua notifikasi masuk ke island.
- **Atur volume dan kecerahan.** Dua slider, plus tombol mic, Wi-Fi, Bluetooth,
  DND, mode gelap, dan airplane.
- **Salin ulang teks lama.** Riwayat clipboard tersimpan selama aplikasi
  berjalan; klik satu item untuk menyalinnya lagi.
- **Simpan file sementara.** Tarik file ke island, lihat thumbnail gambar dan
  video, buka atau hapus saat hover, dan tarik keluar lagi ke file manager.
- **Fokus tanpa gangguan.** Timer dengan durasi bebas, hitung mundur besar di
  peek, dan notifikasi saat sesi selesai.
- **Sembunyikan saat tidak perlu.** Klik kanan membuatnya jadi garis tipis 120x6
  di tepi atas; hover membukanya lagi.

## Tiga cara memakainya

<img src="docs/images/states.png" alt="Mode peek dan kartu notifikasi" width="900">

| Aksi | Yang terjadi |
|---|---|
| Diamkan | Bar kecil: jam, tanggal, kecepatan internet, baterai |
| Arahkan kursor | Peek: jam bertumpuk, musik, notifikasi terbaru |
| Klik | Expanded: semua modul terbuka |
| Klik kanan | Menjadi garis tipis di tepi atas |
| Tarik file | Shelf terbuka dan file masuk ke sana |
| Esc / klik di luar | Menutup kembali ke bar kecil |

Peek menutup sendiri saat kursor pergi. Expanded hanya menutup kalau kamu
menekan Esc, mengklik di luar, atau berpindah window.

## Expanded

<img src="docs/images/expanded.png" alt="X-Tray expanded dengan tab Controls" width="420">

Rail di kiri berisi lima tab, dan **Controls** adalah tab pertama yang terbuka:

- **Controls** — volume, kecerahan, mic, Wi-Fi, Bluetooth, DND, mode gelap,
  airplane, dan pemutar musik lengkap di bawah tombol-tombol itu.
- **Focus** — timer fokus dengan durasi yang bisa diketik.
- **Clipboard** — riwayat teks, klik untuk menyalin ulang.
- **Shelf** — file yang kamu tarik ke island.
- **Settings** — tema, warna aksen, reduce motion, autostart, perilaku hover,
  pengambilalihan notifikasi, dan reset.

Header di atas berisi jam, tanggal, kecepatan jaringan, tombol power, dan
tombol tutup.

## Notifikasi

Selama X-Tray berjalan, ia menjadi penerima notifikasi desktop. Saat bar dalam
keadaan kecil, notifikasi masuk sebagai kartu 384x100 berisi nama aplikasi,
judul, isi, dan tombol tutup. Saat peek, notifikasi muncul di baris bawah. Saat
expanded, ia jadi banner di atas modul.

Kalau kamu lebih suka popup bawaan desktop, matikan **Replace system
notifications** di Settings. Saat X-Tray keluar, notifikasi desktop kembali
normal dengan sendirinya.

## Pasang

### Dari rilis

Unduh `.deb` terbaru dari [Releases](https://github.com/andreasmlbngaol/x-tray/releases),
lalu:

```bash
sudo apt install ./x-tray_1.0.1_amd64.deb
```

Setelah itu jalankan **X-Tray** dari menu aplikasi. Paket ini sudah membawa
runtime Java-nya sendiri, jadi tidak perlu memasang JDK.

### Dari source

Butuh JDK 21.

```bash
./gradlew :app:run
```

Membuat paket sendiri:

```bash
./gradlew :app:packageReleaseDeb
```

## Perintah cepat

Saat aplikasi berjalan, perintah dikirim ke instance yang sama lewat socket:

```bash
x-tray toggle        # buka/tutup expanded
x-tray show          # tampilkan bar
x-tray hide          # jadi garis tipis
x-tray notify "Halo" # kirim notifikasi ke island
x-tray dnd on        # jangan ganggu
x-tray activity start 25 "Tulis laporan"
x-tray quit
```

Menjalankan `x-tray` lagi tanpa argumen tidak membuka window baru; ia hanya
memunculkan instance yang sudah ada.

## Pengaturan yang sering dipakai

| Pengaturan | Gunanya |
|---|---|
| Tema | Ikut sistem, selalu terang, atau selalu gelap |
| Warna aksen | Lima preset atau kode HEX sendiri |
| Reduce motion | Mematikan animasi kalau terasa mengganggu |
| Autostart | Menjalankan X-Tray saat login |
| Hide over maximized | Menyusut jadi garis saat ada window maximized |
| Replace system notifications | Mengambil alih notifikasi desktop |
| Jeda hover / tutup | Seberapa cepat peek terbuka dan menutup |

## Catatan

- Berjalan di X11 dan di Wayland lewat XWayland.
- Pengambilalihan notifikasi memakai helper notifikasi GNOME. Di desktop lain
  X-Tray membiarkan notifikasi asli tetap hidup.
- Ikon tray di GNOME perlu ekstensi AppIndicator.
- Clipboard dan shelf hanya tersimpan selama aplikasi berjalan.

## Lisensi

Kode X-Tray memakai **Apache License 2.0**. Lihat [LICENSE](./LICENSE).

Font **Google Sans Flex** dipakai dengan lisensi SIL Open Font License 1.1
(`docs/licenses/google_sans_flex_OFL.txt`). PixelPlayer hanya dipelajari
sebagai referensi desain; tidak ada kode atau asetnya yang disalin.

Copyright 2026 Andreas M Lbn Gaol
