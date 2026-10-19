# IslandDesk

Dynamic Island untuk desktop Linux: panel kecil di tengah atas layar yang
membesar saat dibutuhkan. Dibangun dengan Kotlin Multiplatform + Compose
Desktop dan Material 3 Expressive.

## Fitur

- **Tiga level**: collapsed (jam, tanggal, kecepatan upload/download,
  baterai), peek (jam bertumpuk HH/MM/ss + tanggal, media dengan kontrol
  putar, baterai, notifikasi terbaru), expanded (rail modul + konten).
  Informasi paling penting ada di collapsed, detail bertambah di peek, dan
  semuanya di expanded.
- **Interaksi**: hover membuka peek, kursor keluar menutup peek, klik
  membuka expanded. Expanded hanya menutup lewat Esc, klik di luar, atau
  pindah window — bukan saat kursor keluar.
- **Media (MPRIS)** lewat `busctl`: judul, artis, album, progres, dan
  kontrol putar/jeda/berikutnya/sebelumnya — tersedia juga di peek supaya
  tidak perlu membuka expanded.
- **Clipboard**: riwayat teks di memori, klik untuk menyalin ulang, plus
  switch "Recording" untuk menjeda/melanjutkan penangkapan.
- **Shelf**: tarik file ke island, lihat thumbnail gambar, lalu tarik lagi
  keluar ke file manager (drag-out XDND) atau buka/hapus dari panel.
- **Fokus**: durasi bisa diketik (jam/menit/detik, tanpa batas 12 jam),
  hitung mundur menampilkan detik dan jam selesai; tampil besar di peek
  selama sesi berjalan.
- **Kontrol sistem**: slider volume dan kecerahan, mute menyatu dengan
  volume, toggle mic/Wi-Fi/Bluetooth/DND memakai warna sebagai status
  (tanpa label "On/Off").
- **Power menu** di header expanded: suspend, reboot, shutdown — dengan
  konfirmasi.
- **Notifikasi** lewat IPC (`islanddesk notify`): saat collapsed seluruh isi
  island diganti pesan notifikasi, saat peek muncul di baris bawah, dan saat
  expanded muncul sebagai banner di atas modul aktif.
- **Pengaturan** di dalam island: tema System/Light/Dark, lima aksen preset
  plus input HEX kustom, reduce motion, DND, autostart, shrink over
  maximized, monitor, jeda hover/tutup, About (versi, lisensi, GitHub), dan
  Danger zone untuk reset ke default.
- **Drag & drop** file ke island memakai drag-and-drop Compose native.
- **Window**: transparan, tanpa dekorasi, selalu di atas, tidak muncul di
  taskbar, click-through di luar bentuk island (XShape).
- **Dormant** saat ada window fullscreen atau maximized (bisa dimatikan di
  Settings): island mengecil jadi garis 120x6 dp di tengah atas, hover
  membuka peek, klik membuka expanded — jadi tidak menutupi tab browser tapi
  tetap bisa dipanggil. Sembunyikan total lewat tray atau `islanddesk hide`.
- **Single instance**, tray icon, autostart XDG, CLI/IPC lewat Unix socket.

## Kebutuhan

- JDK 21
- Linux X11 atau Wayland (XWayland)
- Opsional, sesuai fitur yang dipakai:
  - `busctl` (systemd) untuk MPRIS
  - `wpctl` (PipeWire) untuk volume/mic
  - `nmcli` untuk Wi-Fi
  - `bluetoothctl` untuk Bluetooth
  - `brightnessctl` untuk kecerahan

Fitur yang tool-nya tidak tersedia otomatis disembunyikan; aplikasi tetap
jalan.

## Menjalankan

```bash
./gradlew :app:run
```

Test:

```bash
./gradlew :core:jvmTest :platform:jvmTest
```

Paket Debian:

```bash
./gradlew :app:packageDeb
sudo apt install app/build/compose/binaries/main/deb/islanddesk_0.1.0_amd64.deb
islanddesk
```

## CLI / IPC

Saat aplikasi berjalan, perintah dikirim lewat Unix socket
(`$XDG_RUNTIME_DIR/islanddesk.sock`):

```bash
islanddesk toggle
islanddesk expand
islanddesk collapse
islanddesk show
islanddesk hide
islanddesk state
islanddesk quit
islanddesk notify "Build selesai"
islanddesk activity start 25 "Tulis laporan"
islanddesk activity stop
islanddesk dnd on
islanddesk dnd off
```

Menjalankan aplikasi kedua tanpa argumen akan meneruskan `show` ke instance
yang sudah ada, bukan membuat window baru.

## Variabel lingkungan

| Variabel | Nilai | Keterangan |
| --- | --- | --- |
| `ISLANDDESK_RENDER_API` | `SOFTWARE`, `OPENGL` | Paksa backend renderer. Default: SOFTWARE di Wayland, OPENGL di X11. |
| `ISLANDDESK_DEMO` | `cycle` | Driver sementara untuk menguji transisi tanpa pointer. |
| `ISLANDDESK_DEBUG` | `1` | Log debug dan stack trace. |
| `ISLANDDESK_EXEC` | path | Perintah yang ditulis ke entri autostart. |
| `ISLANDDESK_MODULE` | id modul | Modul yang aktif saat island pertama dibuka. |
| `ISLANDDESK_SHELF_SEED` | path1,path2 | Isi shelf saat start, untuk menguji drag-out. |

## Penyimpanan dan privasi

| Data | Lokasi | Catatan |
| --- | --- | --- |
| Pengaturan | `~/.config/islanddesk/settings.json` | JSON, bisa diedit manual |
| Log | `~/.local/state/islanddesk/islanddesk.log` | Dipotong saat melewati 1 MB |
| Socket IPC | `$XDG_RUNTIME_DIR/islanddesk.sock` | Hanya untuk user yang sama |
| Lock instance | `~/.config/islanddesk/instance.lock` | Advisory file lock |
| Clipboard | memori proses | Tidak ditulis ke disk |
| Shelf | memori proses | Hilang saat keluar |

Tidak ada telemetri dan tidak ada koneksi jaringan.

## Struktur modul

| Modul | Isi |
| --- | --- |
| `core` | State machine, kontrak modul, settings, clipboard/shelf/activity logic, parser IPC. Murni Kotlin + Compose runtime, tanpa sistem. |
| `platform` | `expect/actual`: sesi, layar, jam, window, MPRIS, clipboard, baterai, kontrol sistem, autostart, IPC, X11/JNA. |
| `ui` | Tema Expressive, bentuk island, konten tiga level, modul-modul. |
| `app` | Entry point, wiring window, tray, drop target, CLI, polling. |

## Catatan platform

Di Wayland (XWayland), backend OpenGL Skiko menggantung pada window
transparan, sehingga aplikasi otomatis memakai renderer software. Detail,
bukti `jstack`, dan keterbatasan lain ada di `docs/platform-notes.md`.
Keputusan desain ada di `docs/design-notes.md`.

## Batasan yang diketahui

- Wayland native belum didukung; AWT berjalan lewat XWayland.
- Click-through memakai XShape, jadi hanya berlaku di X11/XWayland.
- Notifikasi sistem (dari aplikasi lain) belum dibaca; notifikasi island
  berasal dari IPC.
- Tray icon di GNOME butuh ekstensi AppIndicator.
- Clipboard dan shelf belum persisten.
- Timer fokus berhenti saat aplikasi ditutup.

## Lisensi dan aset

- Font **Google Sans Flex** dipakai dengan lisensi SIL Open Font License 1.1
  (`docs/licenses/google_sans_flex_OFL.txt`).
- PixelPlayer hanya dipelajari sebagai referensi desain. Kodenya proprietary,
  jadi tidak ada kode atau aset yang disalin.
