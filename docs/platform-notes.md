# Catatan Platform — X-Tray

Dokumen ini mencatat perilaku nyata window X-Tray di lingkungan pengembangan,
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
X-Tray: session=wayland desktop=zorin:GNOME displayServer=wayland-0 wayland=true
```

## Window

Properti yang dipakai: `undecorated = true`, `transparent = true`,
`resizable = false`, `focusable = true`, `alwaysOnTop = true`,
`isAutoRequestFocus = false`.

Hasil inspeksi X11 saat collapsed:

| Alat | Hasil |
| --- | --- |
| `wmctrl -l` | window terdaftar dengan judul `X-Tray` |
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
- Override manual: `XTRAY_RENDER_API=OPENGL|SOFTWARE`.

Konsekuensi: di Wayland rendering memakai CPU. Untuk permukaan sekecil ini
(224x48 saat collapsed) beban masih wajar, tetapi perlu diukur ulang saat
animasi panel penuh dan modul media aktif. Jika nanti pindah ke compositor
yang mendukung Wayland native (mis. melalui Java binding), jalur OpenGL bisa
dipakai kembali.

## Hover dan input pointer

Hover tidak boleh bergantung pada satu sumber saja:

- `MouseInfo`/XQueryPointer **membeku** selama pointer berada di atas surface
  Wayland native. Kalau hover hanya dibaca dari posisi itu, peek tidak pernah
  menutup karena posisi terakhir yang terlihat masih di dalam island.
- Event enter/exit AWT tetap datang dari compositor saat pointer pindah ke
  surface native, jadi event itu yang menjadi sumber utama.
- Polling posisi tetap dipakai sebagai cadangan, tetapi hanya bereaksi kalau
  posisi pointer benar-benar berubah. Posisi yang membeku tidak boleh
  mengunci hover.

Bukti: pada build 1.0.3, log debug `hover=` tidak pernah muncul selama lima
siklus hover masuk/keluar, artinya seluruh perubahan hover berasal dari event
AWT, dan setiap keluar kembali ke 324x50.

- `xdotool mousemove` lewat XTest **tidak** diandalkan untuk memicu hover di
  permukaan XWayland; event dapat tidak sampai ke window Compose.
- Namun hover dengan kursor fisik **bekerja**: saat kursor berada di atas
  island, state berubah `Collapsed -> Peek` setelah hover intent 180 ms.
- Perilaku final: peek tidak pernah membuka expanded sendiri; klik yang
  membuka expanded. Kursor keluar dari peek menutup peek; expanded
  mengabaikan gerakan kursor dan hanya menutup lewat Esc, klik di luar
  (focus lost), atau perintah IPC.

Implikasi untuk pengujian otomatis: jangan bergantung pada injeksi pointer
X11. Gunakan `XTRAY_DEMO=cycle` (driver sementara) atau nanti IPC
`x-tray toggle` untuk menguji transisi.

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
- Deteksi "tertutup" memakai polling 1 detik: `_NET_ACTIVE_WINDOW` di root,
  lalu cek `_NET_WM_STATE_FULLSCREEN` atau pasangan
  `_NET_WM_STATE_MAXIMIZED_VERT` + `_NET_WM_STATE_MAXIMIZED_HORZ` pada
  window aktif. Window maximized menaruh tab browser tepat di bawah panel.
- Saat tertutup island **tidak disembunyikan**, melainkan mengecil ke bentuk
  dormant 120x6 dp. Menyembunyikan total akan membuat island tidak pernah
  tampil, karena hampir semua window di desktop Linux dibuka maximized.
  Bentuk dormant tetap menerima hover (peek) dan klik (expanded), dan input
  region XShape ikut mengecil sehingga sekitarnya click-through.
- Polling dipilih karena listener X11 event membutuhkan event loop sendiri;
  bebannya satu query ringan per detik.
- Window aktif diabaikan bila `_NET_WM_PID` sama dengan PID X-Tray,
  supaya mengklik island tidak pernah menyembunyikannya.
- Window tidak pernah di-unmap. "Sembunyikan" berarti state dormant yang
  sama dengan kasus maximized (garis 120x6), jadi hover tetap diterima dan
  peek bisa dibuka lagi. Pendekatan `isVisible = false` dibuang: saat window
  tidak dipetakan Compose berhenti meminta frame, sehingga tidak ada cara
  mengembalikannya lewat hover.
- DND diterapkan sebagai preferensi: hover tidak membuka panel dan
  `x-tray notify` dijawab `suppressed`.
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
  ke `WM_NAME` = `X-Tray`. Pendekatan ini menghindari refleksi ke
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

- Socket: `$XDG_RUNTIME_DIR/x-tray.sock` (fallback ke temp dir bila
  variabel tidak ada).
- Protokol: satu baris perintah, satu baris balasan.
- Lock: `FileChannel.tryLock()` pada `~/.config/x-tray/instance.lock`.
  Proses kedua meneruskan `show` lalu keluar.

Diuji langsung di sesi ini:

```text
$ x-tray state      -> Collapsed
$ x-tray toggle     -> ok
$ x-tray state      -> Expanded
$ x-tray notify ... -> ok
$ x-tray quit       -> ok
```

## Drag and drop

### Notifikasi sistem (`org.freedesktop.Notifications`)

X-Tray mengekspor objek D-Bus di `/org/freedesktop/Notifications` lewat
`dbus-java` (core + transport native unixsocket, tanpa JNI tambahan) dan
meminta nama `org.freedesktop.Notifications`.

GNOME memegang nama itu di proses helper kecil
(`/usr/bin/gjs -m /usr/share/gnome-shell/org.gnome.Shell.Notifications`) yang
**tidak** mengizinkan penggantian (`RequestName` dengan REPLACE_EXISTING
dijawab `EXISTS`). Karena itu alurnya:

1. Coba `RequestName`. Kalau berhasil, selesai.
2. Kalau gagal, baca pemilik nama lewat `GetConnectionUnixProcessID` dan
   periksa `/proc/<pid>/cmdline`. Hanya kalau itu helper GNOME, proses
   dihentikan (SIGTERM) lalu nama diminta ulang (maks 3 detik).
3. Pemilik lain (mis. dunst) dibiarkan hidup: X-Tray melaporkan gagal dan
   notifikasi asli tetap berjalan.

Saat berhenti, nama dilepas lalu `org.gnome.Shell.Notifications` di-`Ping`.
Itu memicu aktivasi D-Bus yang menjalankan helper lagi, dan helper baru
langsung meminta kembali `org.freedesktop.Notifications`. Restart ini sudah
diuji: setelah `x-tray quit`, `busctl --user status` menunjukkan proses
`gjs` memegang nama itu lagi.

Antarmuka yang diekspor: `Notify`, `CloseNotification`, `GetCapabilities`,
`GetServerInformation` (4 out-arg lewat `DBusSerializable`), plus sinyal
`NotificationClosed` dan `ActionInvoked`. Kapabilitas yang diiklankan hanya
`body` dan `body-markup`.

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
- Volume memakai `wpctl set-volume @DEFAULT_AUDIO_SINK@ N%`. PipeWire
  mengizinkan nilai di atas 100%; slider memakai rentang 0–150% karena itu
  batas aman sebelum distorsi parah pada kebanyakan sink.
- Kecerahan memakai `brightnessctl` bila terpasang. Bila tidak (mis. Zorin
  tanpa brightnessctl), aplikasi memakai GNOME:
  `org.gnome.SettingsDaemon.Power.Screen.Brightness` lewat `busctl --user
  get-property/set-property`. Ini tidak butuh root maupun paket tambahan,
  tetapi hanya ada saat sesi GNOME berjalan.
- Keduanya dipanggil hanya saat slider dilepas (`onValueChangeFinished`),
  bukan setiap frame.
- Power menu memakai `systemctl poweroff|reboot|suspend`. Konfirmasi
  ditampilkan lebih dulu karena aksi ini tidak bisa dibatalkan.

## Tray dan autostart

- Tray memakai `java.awt.SystemTray` dengan menu Buka/Ciutkan/DND/Keluar.
  Di GNOME tray hanya terlihat bila ekstensi AppIndicator aktif; kegagalan
  tidak membuat aplikasi berhenti.
- Autostart menulis `~/.config/autostart/x-tray.desktop`. `Exec`
  memakai `XTRAY_EXEC`, lalu `/opt/x-tray/bin/x-tray`, lalu
  perintah proses saat ini.

## Logging

Log ditulis ke stdout dan ke `~/.local/state/x-tray/x-tray.log`.
Berkas dipotong (ditimpa kosong) saat melewati 1 MB. `XTRAY_DEBUG=1`
menambahkan log debug dan stack trace.

## Rilis, minifikasi, dan konsumsi daya

- `packageReleaseDeb` menjalankan ProGuard 7.8.0 (shrink + optimize, tanpa
  obfuscate). Aturan ada di `app/proguard-rules.pro`; yang harus dijaga:
  JNA (interface dan struktur), dbus-java (interface beranotasi,
  `DBusSerializable`, `DBusSignal`), serta provider `ServiceLoader`
  (`org.slf4j.nop`, `org.freedesktop.dbus.spi.transport.ITransportProvider`).
- Runtime image jpackage tidak memuat `jdk.security.auth`, padahal SASL
  dbus-java memakai `com.sun.security.auth.module.UnixSystem`. Tanpa
  `modules("jdk.security.auth", "java.security.sasl")` koneksi bus gagal
  dengan `NoClassDefFoundError` dan daemon notifikasi tidak pernah jalan —
  build tetap sukses, jadi ini hanya terlihat saat aplikasi dijalankan.
- Ukuran `.deb`: 90 MB tanpa minify, 43 MB pada build release.
- Terukur pada sesi ini (Wayland, renderer SOFTWARE, collapsed, tanpa
  notifikasi): sekitar 3% CPU satu inti, RSS ~259 MB, 57 thread.

## Perintah verifikasi yang dipakai

```bash
./gradlew :core:jvmTest :platform:jvmTest
./gradlew :app:run
XTRAY_DEMO=cycle ./gradlew :app:run
XTRAY_RENDER_API=OPENGL ./gradlew :app:run
./gradlew :app:run --args='state'
./gradlew :app:packageReleaseDeb
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
- Persistensi clipboard/shelf bila diinginkan.
