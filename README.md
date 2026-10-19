# IslandDesk

Desktop Dynamic Island untuk Linux: panel kecil di tengah atas layar yang
membesar saat dibutuhkan. Dibangun dengan Kotlin Multiplatform + Compose
Desktop dan Material 3 Expressive.

Status: **Fase 1** (kerangka visual dan window). Belum siap dipakai harian.

## Fitur saat ini

- Window transparan, tanpa dekorasi, selalu di atas, tanpa merebut fokus.
- Tiga level tampilan: collapsed (jam + baterai), peek (jam + modul aktif),
  expanded (tab modul + konten).
- Hover intent 180 ms, perluasan peek ke expanded 600 ms, delay collapse
  350 ms, tutup dengan Esc atau klik.
- Modul contoh: Media, Clipboard, Shelf, Quick Controls.
- Tema Material 3 Expressive dengan font Google Sans Flex.

Lihat `docs/design-notes.md` untuk keputusan desain dan
`docs/platform-notes.md` untuk perilaku platform yang sudah diuji.

## Kebutuhan

- JDK 21
- Linux dengan X11 atau Wayland (XWayland)
- Gradle wrapper sudah disertakan

## Menjalankan

```bash
./gradlew :app:run
```

Perintah berguna:

```bash
./gradlew :core:jvmTest                 # unit test state machine
./gradlew :ui:compileKotlinJvm          # kompilasi UI
./gradlew :app:packageDeb               # paket .deb (belum final)
```

Variabel lingkungan:

| Variabel | Nilai | Keterangan |
| --- | --- | --- |
| `ISLANDDESK_RENDER_API` | `SOFTWARE`, `OPENGL` | Paksa backend renderer. Default: SOFTWARE di Wayland, OPENGL di X11. |
| `ISLANDDESK_DEMO` | `cycle` | Driver sementara untuk menguji transisi tanpa pointer. |

### Catatan renderer

Di Wayland (XWayland), backend OpenGL Skiko menggantung pada window
transparan. Aplikasi otomatis memakai renderer software di sesi Wayland.
Detail dan bukti ada di `docs/platform-notes.md`.

## Struktur modul

| Modul | Isi |
| --- | --- |
| `core` | State machine island, kontrak `IslandModule`, registry. Murni Kotlin, tanpa UI. |
| `platform` | `expect/actual`: sesi, geometri layar, jam, kontrol window. |
| `ui` | Tema Expressive, bentuk island, konten collapsed/peek/expanded, modul demo. |
| `app` | Entry point, window desktop, driver demo. |

## Lisensi dan aset

- Font **Google Sans Flex** dipakai dengan lisensi SIL Open Font License 1.1
  (`docs/licenses/google_sans_flex_OFL.txt`).
- PixelPlayer hanya dipelajari sebagai referensi desain. Kodenya proprietary,
  jadi tidak ada kode atau aset yang disalin.
