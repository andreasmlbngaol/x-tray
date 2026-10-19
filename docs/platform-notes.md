# Catatan Platform — IslandDesk

Dokumen ini mencatat perilaku nyata window IslandDesk di lingkungan pengembangan,
bukan asumsi. Semua angka berasal dari log aplikasi dan alat X11.

Tanggal pengujian: 2026-10-19

## Lingkungan uji

| Komponen | Nilai |
| --- | --- |
| Distro / desktop | Zorin OS, GNOME |
| Sesi | Wayland (`XDG_SESSION_TYPE=wayland`, `WAYLAND_DISPLAY=wayland-0`) |
| AWT | XWayland (Java belum punya backend Wayland native) |
| JDK | Azul Zulu 21.0.11 |
| Compose Multiplatform | 1.13.0-alpha02 |
| Skiko | 0.153.0 |
| Layar | 1920x1080, area usable 1920x1024 (top bar 56 px) |

Log startup:

```
IslandDesk: session=wayland desktop=zorin:GNOME displayServer=wayland-0 wayland=true
```

## Window

Properti yang dipakai: `undecorated = true`, `transparent = true`,
`resizable = false`, `focusable = true`, `alwaysOnTop = true`,
`isAutoRequestFocus = false`.

Hasil inspeksi X11 saat collapsed:

| Alat | Hasil |
| --- | --- |
| `wmctrl -l` | window terdaftar dengan judul `IslandDesk` |
| `xwininfo` | `224x48+848+0`, Depth 32, Map State `IsViewable` |
| `xprop` | `_NET_WM_STATE_ABOVE`, `_NET_WM_WINDOW_TYPE_NORMAL` |

Posisi horizontal dihitung dari `DisplayInfo.topCenterPosition`, sehingga
window selalu di tengah atas layar utama. Saat state berubah, ukuran window
ikut berubah agar area tangkapan pointer tetap sekecil mungkin:

| State | Ukuran visual | Ukuran window (termasuk padding 12 dp) |
| --- | --- | --- |
| Collapsed | 200x36 | 224x48 |
| Peek | 420x140 | 444x152 |
| Expanded | 460x620 | 484x632 |

## Temuan penting: OpenGL + window transparan menggantung di XWayland

Gejala: window tampil, tetapi setelah frame pertama UI tidak pernah
diperbarui. Semua coroutine yang dijadwalkan di EDT berhenti berjalan.

Diagnosis dengan `jstack`:

```
"AWT-EventQueue-0" ... java.lang.Thread.State: RUNNABLE
    at org.jetbrains.skiko.renderer.LinuxOpenGLRendererKt.swapBuffers(Native Method)
    at org.jetbrains.skiko.renderer.LinuxOpenGLRendererKt.swapBuffers(LinuxOpenGLRenderer.kt:160)
    at org.jetbrains.skiko.renderer.LinuxOpenGLRenderer.swapBuffers$skiko(LinuxOpenGLRenderer.kt:45)
    at org.jetbrains.skiko.renderer.LinuxGLFrameBatch.drawAndPresent(LinuxOpenGLRenderer.kt:150)
    ...
    at java.awt.EventDispatchThread.run(EventDispatchThread.java:90)
```

Matriks uji:

| Window | Renderer | Hasil |
| --- | --- | --- |
| Transparan | OpenGL (default) | **macet** di `swapBuffers` |
| Transparan | Software (`SKIKO_RENDER_API=SOFTWARE`) | normal |
| Opaque | OpenGL | normal |

Kesimpulan: kombinasi window transparan dan backend OpenGL Skiko di XWayland
yang bermasalah, bukan kode island. Menonaktifkan vsync
(`-Dskiko.vsync.enabled=false` dan
`-Dskiko.rendering.linux.waitForFrameVsyncOnRedrawImmediately=false`) tidak
menolong.

Mitigasi yang dipakai (lihat `Main.kt`):

- Sesi Wayland -> `skiko.renderApi=SOFTWARE` otomatis.
- Sesi X11 native -> `OPENGL`.
- Override manual: `ISLANDDESK_RENDER_API=OPENGL|SOFTWARE`.

Konsekuensi: di Wayland rendering memakai CPU. Untuk permukaan sekecil ini
(224x48 saat collapsed) beban masih wajar, tetapi perlu diukur ulang saat
animasi panel penuh dan modul media aktif. Jika nanti pindah ke compositor
yang mendukung Wayland native (mis. melalui Java binding), jalur OpenGL bisa
dipakai kembali.

## Hover dan input pointer

- `xdotool mousemove` lewat XTest **tidak** diandalkan untuk memicu hover di
  permukaan XWayland; event dapat tidak sampai ke window Compose.
- Namun hover dengan kursor fisik **bekerja**: saat kursor berada di atas
  island, state berubah `Collapsed -> Peek` setelah hover intent 180 ms.
- Perilaku final: peek tidak pernah membuka expanded sendiri; klik yang
  membuka expanded. Kursor keluar dari peek menutup peek; expanded
  mengabaikan gerakan kursor dan hanya menutup lewat Esc, klik di luar
  (focus lost), atau perintah IPC.

Implikasi untuk pengujian otomatis: jangan bergantung pada injeksi pointer
X11. Gunakan `ISLANDDESK_DEMO=cycle` (driver sementara) atau nanti IPC
`islanddesk toggle` untuk menguji transisi.

## Screenshot

Tidak ada metode screenshot yang berhasil di sesi ini:

| Metode | Hasil |
| --- | --- |
| `import -window root` (ImageMagick) | `Resource temporarily unavailable` |
| `org.gnome.Shell.Screenshot` via DBus | `AccessDenied` |
| `xwd` | `BadMatch` |
| `ffmpeg -f x11grab` | file terbentuk, isi hitam (kompositor Wayland tidak menggambar ke root X11) |

Verifikasi visual saat ini mengandalkan log geometri dan pemeriksaan manual
oleh pengguna. Untuk CI/regresi visual nanti, pertimbangkan render offscreen
Compose (mis. `runDesktopComposeUiTest`) yang tidak membutuhkan compositor.

## Click-through

Diterapkan dengan XShape (detail di bagian berikut). Padding transparan dan
area di luar bentuk island tidak lagi menangkap klik. Di Wayland native,
XShape hanya berlaku pada permukaan XWayland; perlu strategi terpisah bila
kelak ada backend Wayland native.

## Fokus dan fullscreen

- `isAutoRequestFocus = false` mencegah window merebut fokus saat muncul.
- Klik di luar island terdeteksi lewat `WindowFocusListener`: saat window
  kehilangan fokus dan state sedang `Expanded`, island langsung mengecil.
- Auto-hide memakai polling 1 detik: `_NET_ACTIVE_WINDOW` di root, lalu
  cek `_NET_WM_STATE_FULLSCREEN` atau pasangan
  `_NET_WM_STATE_MAXIMIZED_VERT` + `_NET_WM_STATE_MAXIMIZED_HORZ` pada
  window aktif. Window maximized menaruh tab browser tepat di bawah panel,
  jadi island ikut menyingkir. Polling dipilih karena listener X11 event
  membutuhkan event loop sendiri; bebannya satu query ringan per detik.
- Window aktif diabaikan bila `_NET_WM_PID` sama dengan PID IslandDesk,
  supaya mengklik island tidak pernah menyembunyikannya.
- Visibilitas diterapkan dari loop polling 300 ms, bukan dari
  `LaunchedEffect` yang di-key pada state. Saat window disembunyikan
  (`isVisible = false`) Compose berhenti meminta frame, sehingga efek
  berbasis state tidak dijalankan lagi dan `islanddesk show` tidak akan
  pernah mengembalikan window. Loop biasa tetap berjalan karena hanya
  memakai `delay` pada dispatcher Swing.
- DND diterapkan sebagai preferensi: hover tidak membuka panel dan
  `islanddesk notify` dijawab `suppressed`.
- Multi-monitor: `DisplayInfo.screens()` membaca semua `ScreenDevice`;
  `monitorIndex` di pengaturan menentukan layar tujuan. Posisi dihitung dari
  usable bounds monitor tersebut.

## Click-through dengan XShape

`X11Window.applyInputRegion()` memanggil `XShapeCombineRectangles` dengan
`ShapeInput` (2), `ShapeSet` (0). Region dibentuk dari 24 strip horizontal
yang mengikuti radius bawah island; area di luar region meneruskan klik ke
window di bawahnya.

Catatan:

- Window dicari lewat `_NET_CLIENT_LIST` + `_NET_WM_PID`, dengan fallback
  ke `WM_NAME` = `IslandDesk`. Pendekatan ini menghindari refleksi ke
  `sun.awt` yang diblokir module system Java 21.
- Region diperbarui saat state berubah (ukuran target), bukan setiap frame
  animasi. Selama ~300 ms transisi, area input masih memakai ukuran lama.
- Padding transparan 12 dp di kiri/kanan dan bawah tidak lagi menangkap klik.
- XShape hanya berlaku pada X11/XWayland. Wayland native memerlukan
  protokol input region sendiri.

## EWMH

Setelah window tampil, aplikasi menulis:

- `_NET_WM_WINDOW_TYPE` = `_NET_WM_WINDOW_TYPE_UTILITY`
- `_NET_WM_STATE` = ABOVE, SKIP_TASKBAR, SKIP_PAGER, STICKY

GNOME menerima hint ini; window tidak muncul di alt-tab/taskbar. Beberapa
window manager hanya membaca `_NET_WM_STATE` saat map, jadi aplikasi
mengulang penulisan 6 kali dengan jeda 250 ms setelah window dibuat.

## IPC dan single instance

- Socket: `$XDG_RUNTIME_DIR/islanddesk.sock` (fallback ke temp dir bila
  variabel tidak ada).
- Protokol: satu baris perintah, satu baris balasan.
- Lock: `FileChannel.tryLock()` pada `~/.config/islanddesk/instance.lock`.
  Proses kedua meneruskan `show` lalu keluar.

Diuji langsung di sesi ini:

```text
$ islanddesk state      -> Collapsed
$ islanddesk toggle     -> ok
$ islanddesk state      -> Expanded
$ islanddesk notify ... -> ok
$ islanddesk quit       -> ok
```

## Drag and drop

Shelf memakai `Modifier.dragAndDropTarget` dari Compose
(`androidx.compose.foundation.draganddrop`), bukan `java.awt.dnd.DropTarget`.
AWT `DropTarget` pada ComposeWindow bertabrakan dengan drag manager Compose,
sehingga drop tidak pernah sampai ke listener. Dengan modifier Compose,
`XdndAware` dipasang otomatis dan `DragData.FilesList` memberi path file.

Verifikasi: `xprop` menunjukkan `XdndAware(ATOM) = BITMAP` pada window.

Drag keluar (shelf → file manager) memakai `Modifier.dragAndDropSource`
dengan `DragAndDropTransferData` yang membungkus `java.awt.datatransfer.Transferable`
berisi `DataFlavor.javaFileListFlavor`. API ini hanya ada di source set
desktop, jadi dibungkus `expect fun Modifier.fileDragSource(path)` di
commonMain dan diimplementasikan di `ui/src/jvmMain`.

## Tema

`themeMode` di `settings.json` bernilai `System`, `Light`, atau `Dark`.
`System` memakai `isSystemInDarkTheme()` dari Compose Desktop.

`accentColor` memilih salah satu dari lima keluarga aksen (Navy, Violet,
Emerald, Amber, Rose). Alih-alih menyimpan 10 skema warna manual,
`ui/theme/Color.kt` menyimpan satu hue + bobot saturasi per aksen lalu
membangun seluruh peran tonal dengan `Color.hsl`. Hasilnya semua aksen
punya hubungan kontras yang sama; Navy adalah default dan paling dekat
dengan permukaan netral biru gelap.

Island memakai gradien tonal tipis agar tidak terlihat seperti kotak datar.

## Kecepatan jaringan, volume, dan power

- Kecepatan up/down dibaca dari `/sys/class/net/*/statistics/rx_bytes` dan
  `tx_bytes` (kecuali `lo`), lalu dihitung selisihnya per detik. Sampel
  pertama selalu 0 karena belum ada pembanding. Tidak perlu root.
- Volume memakai `wpctl set-volume @DEFAULT_AUDIO_SINK@ N%`; kecerahan
  memakai `brightnessctl set N%`. Keduanya dipanggil hanya saat slider
  dilepas (`onValueChangeFinished`), bukan setiap frame.
- Power menu memakai `systemctl poweroff|reboot|suspend`. Konfirmasi
  ditampilkan lebih dulu karena aksi ini tidak bisa dibatalkan.

## Tray dan autostart

- Tray memakai `java.awt.SystemTray` dengan menu Buka/Ciutkan/DND/Keluar.
  Di GNOME tray hanya terlihat bila ekstensi AppIndicator aktif; kegagalan
  tidak membuat aplikasi berhenti.
- Autostart menulis `~/.config/autostart/islanddesk.desktop`. `Exec`
  memakai `ISLANDDESK_EXEC`, lalu `/opt/islanddesk/bin/islanddesk`, lalu
  perintah proses saat ini.

## Logging

Log ditulis ke stdout dan ke `~/.local/state/islanddesk/islanddesk.log`.
Berkas dipotong (ditimpa kosong) saat melewati 1 MB. `ISLANDDESK_DEBUG=1`
menambahkan log debug dan stack trace.

## Perintah verifikasi yang dipakai

```bash
./gradlew :core:jvmTest :platform:jvmTest
./gradlew :app:run
ISLANDDESK_DEMO=cycle ./gradlew :app:run
ISLANDDESK_RENDER_API=OPENGL ./gradlew :app:run
./gradlew :app:run --args='state'
./gradlew :app:packageDeb
wmctrl -l
xwininfo -id <window-id>
xprop -id <window-id>
jstack <pid>
```

## Status implementasi

Sudah berjalan:

- Window transparan, undecorated, always-on-top, utility/skip-taskbar.
- Tiga level dengan morphing shape dan spring motion.
- Hover intent, delay collapse, Esc, klik, DND.
- Click-through XShape di luar bentuk island.
- MPRIS lewat `busctl`, clipboard AWT, shelf drag & drop, timer fokus,
  kontrol sistem, pengaturan JSON.
- IPC Unix socket + CLI + single instance + tray + autostart.
- Auto-hide fullscreen, pemilihan monitor.
- Unit test: state machine, settings, clipboard, shelf, activity, IPC, parser
  MPRIS.

Belum / perlu uji tambahan:

- Uji di sesi X11 native (mesin ini Wayland).
- Uji drag & drop dari aplikasi Wayland native ke XWayland.
- Pengukuran beban CPU idle dan saat animasi.
- Notifikasi sistem dari aplikasi lain (butuh menjadi notification daemon).
- Persistensi clipboard/shelf bila diinginkan.
