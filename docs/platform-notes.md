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
| Peek | 380x100 | 404x112 |
| Expanded | 440x600 | 464x612 |

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
  island, state berubah `Collapsed -> Peek` setelah hover intent 180 ms
  (terlihat di log `state=Peek`).
- Klik juga diterima window (window fokus, `focusable = true`).

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

Belum diterapkan. Saat ini seluruh persegi window menangkap klik, termasuk
padding transparan 12 dp dan area di sekitar bentuk island. Artinya klik pada
area tersebut tidak tembus ke aplikasi di bawahnya.

Rencana Fase 2: bentuk ulang input region dengan XShape lewat JNA
(`XShapeCombineRectangles`) mengikuti `IslandShape`, sehingga hanya bentuk
yang terlihat yang menerima input. Di Wayland native, XShape hanya berlaku
pada permukaan XWayland; perlu strategi terpisah bila kelak ada backend
Wayland native.

## Fokus dan fullscreen

- `isAutoRequestFocus = false` mencegah window merebut fokus saat muncul.
- Deteksi window fullscreen (auto-hide) belum diimplementasikan; perlu
  membaca `_NET_ACTIVE_WINDOW` / `_NET_WM_STATE_FULLSCREEN` lewat X11.
- DND (do-not-disturb) dan multi-monitor belum diimplementasikan; saat ini
  island selalu di monitor utama.

## Perintah verifikasi yang dipakai

```bash
./gradlew :core:jvmTest
./gradlew :app:run
ISLANDDESK_DEMO=cycle ./gradlew :app:run
ISLANDDESK_RENDER_API=OPENGL ./gradlew :app:run
wmctrl -l
xwininfo -id <window-id>
xprop -id <window-id>
jstack <pid>
```

## Status Fase 1

Terpenuhi:

- Window transparan, undecorated, always-on-top di tengah atas.
- Tiga level (collapsed/peek/expanded) dengan ukuran dan radius berbeda.
- Transisi berjalan mulus dan tidak memblokir event loop (dengan renderer
  software di Wayland).
- Hover intent, delay collapse, Esc, dan klik bekerja.
- Unit test state machine lulus (7 test).

Belum:

- Click-through di luar bentuk island (Fase 2).
- Auto-hide fullscreen, DND, multi-monitor (Fase 2+).
- Pengukuran beban CPU saat idle dan saat animasi.
- Uji di sesi X11 native (tidak tersedia di mesin ini).
