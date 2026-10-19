# IslandDesk — Catatan Desain (Fase 0)

Status: **draft untuk persetujuan**
Tanggal: 2026-10-19
Referensi yang dipelajari: `~/Code/cloned/PixelPlayer` @ `63cb59b9` (2026-09-17)
Target kerja: Zorin OS (Linux, GNOME), arsitektur siap multiplatform.

---

## 1. Ringkasan eksekutif

1. PixelPlayer memakai bahasa visual **Material 3 Expressive secara selektif**, bukan theme expressive penuh: wrapper-nya masih `MaterialTheme`, tetapi banyak komponen dan motion memakai API expressive (`LoadingIndicator`, `LinearWavyProgressIndicator`, `MotionScheme.expressive()`, tipografi `*Emphasized`).
2. Ciri paling kuat: **satu permukaan besar dengan sudut sangat bulat (50–60 dp)**, kontrol yang dikelompokkan dalam baris seimbang, dan aksen warna yang mengikuti artwork.
3. Tipografi memakai **Google Sans Flex varian rounded** dengan skala besar dan tebal; angka/judul menjadi fokus, teks pendukung kecil dan tenang.
4. Motion memakai **spring expressive** untuk perubahan bentuk/ukuran, ditambah tween pendek untuk fade. Ada catatan performa penting: spring expressive default bisa terlalu lambat/memantul untuk permukaan besar, sehingga tim menggantinya dengan spring no-bounce di beberapa tempat.
5. Shape tidak statis: sudut beranimasi (mis. chip 50 dp ↔ 6 dp), tombol play/pause berubah dari 60 dp ke 26 dp, dan segmen tab berubah pill ↔ rounded-rect. Ini pola yang cocok untuk morph collapsed → peek → expanded.
6. Layout player vertikal: artwork → metadata + progres (gap 4 dp) → kontrol; padding horizontal 24 dp. Layout horizontal: dua kolom 50/50 dengan gap 9 dp.
7. Grup kontrol transport: tinggi 80 dp, tiga tombol dengan bobot sama; tombol yang ditekan membesar (weight 1.1) dan yang lain mengecil (0.65), gap 6 dp.
8. Baris toggle sekunder: wadah pill 60 dp, padding dalam 6 dp, tiga segmen sama besar; tinggi 66–86 dp.
9. Untuk island desktop, angka-angka Android ini harus **diperkecil dan dipadatkan**; target klik minimal 40–44 dp, bukan 48 dp, karena pointer lebih presisi dan panel lebih sempit.
10. Lisensi PixelPlayer **proprietary**: studi pribadi diperbolehkan, tetapi redistribusi karya turunan dilarang. Kita tidak menyalin kode, font, drawable, string, atau aset apa pun; yang diambil hanya prinsip layout dan rasa visual, lalu ditulis ulang.

---

## 2. Lisensi dan batasan hukum

`PixelPlayer/LICENSE` menyatakan:

- Lisensi **Proprietary**, copyright Theo Vilardo (2026).
- Diizinkan: mempelajari, meninjau, dan menggunakan software untuk **personal, non-commercial**.
- Dilarang: redistribusi software atau **derivative work**, dalam bentuk biner maupun source, tanpa izin tertulis.
- Dilarang: penggunaan komersial, termasuk menjual/menerbitkan di platform apa pun.

Implikasi untuk IslandDesk:

- Kita **tidak menyalin source code** PixelPlayer, termasuk potongan composable, helper, atau konstanta.
- Kita **tidak menyalin aset**: font Google Sans Flex, ikon, drawable, string, screenshot, atau file konfigurasi.
- Kita **tidak memakai** library pihak ketiga yang hanya ada karena dipakai PixelPlayer (mis. `racra.compose.smooth_corner_rect_library`) tanpa memeriksa lisensinya sendiri.
- Yang dipakai adalah **observasi tingkat tinggi**: proporsi, hierarki, pengelompokan kontrol, penggunaan shape dan motion. Implementasi IslandDesk ditulis dari nol dengan token dan komponen sendiri.
- Catatan ini bukan nasihat hukum. Jika IslandDesk akan didistribusikan, sebaiknya lakukan review lisensi terpisah.

---

## 3. Verifikasi toolchain dan API (bukan asumsi)

Proyek saat ini:

| Item | Versi |
|---|---|
| Kotlin | 2.4.21 |
| Compose Multiplatform | 1.13.0-alpha02 |
| Compose Material 3 | 1.13.0-alpha02 |
| Gradle | 9.8.1 |
| Target saat ini | JVM desktop |

Verifikasi dilakukan langsung terhadap artifact yang terpasang di Gradle cache
(`material3-desktop-1.13.0-alpha02.jar`), bukan dari dokumentasi online:

| API | Status | Bukti |
|---|---|---|
| `MaterialExpressiveTheme(colorScheme, motionScheme, shapes, typography, content)` | Tersedia | `javap androidx.compose.material3.MaterialThemeKt` |
| `MotionScheme.expressive()` / `standard()` | Tersedia | `MotionScheme$Companion` |
| `MotionScheme.defaultSpatialSpec/fastSpatialSpec/slowSpatialSpec` | Tersedia | `MotionScheme` |
| `MotionScheme.defaultEffectsSpec/fastEffectsSpec/slowEffectsSpec` | Tersedia | `MotionScheme` |
| `Typography.*Emphasized` (15 varian) | Tersedia | `Typography` |
| `Shapes` dengan `extraSmall … extraExtraLarge`, `largeIncreased`, `extraLargeIncreased` | Tersedia | `Shapes` |
| `LoadingIndicator` | Tersedia | `LoadingIndicatorKt` |
| `LinearWavyProgressIndicator`, `CircularWavyProgressIndicator` | Tersedia | `WavyProgressIndicatorKt` |
| `WavyProgressIndicatorDefaults` | Tersedia | `WavyProgressIndicatorDefaults` |
| `ButtonGroup`, `ToggleButton`, `SplitButton` | Tersedia | class terkait di jar |
| `FloatingToolbar` (horizontal/vertical) | Tersedia | `FloatingToolbarKt` |
| `MaterialShapes` (circle, cookie, clover, dll.) | Tersedia | `MaterialShapes$Companion` |
| `androidx.graphics.shapes` | Tersedia sebagai dependency runtime | POM material3 → `graphics-shapes:1.1.0` |

Catatan risiko:

- Compose Multiplatform dan Material 3 di proyek ini masih **alpha** (`1.13.0-alpha02`). API dapat berubah. Setiap pemakaian API expressive akan diberi opt-in `OptIn(ExperimentalMaterial3ExpressiveApi::class)` dan diverifikasi saat kompilasi.
- `MaterialExpressiveTheme` sudah ada, tetapi **default parameter** (mis. apakah motionScheme otomatis `MotionScheme.expressive()`) harus diverifikasi saat implementasi Fase 1, bukan diasumsikan.
- `MaterialShapes` membutuhkan `androidx.graphics.shapes`; sudah tersedia transitif, tetapi tetap akan dicek di classpath desktop.

---

## 4. Temuan dari PixelPlayer

### 4.1 Struktur theme

File kunci: `app/src/main/java/com/theveloper/pixelplay/ui/theme/Theme.kt`.

- Wrapper memakai `MaterialTheme(colorScheme, typography, shapes)`, **bukan** `MaterialExpressiveTheme`.
- Skema warna:
  - `DarkColorScheme`: purple/pink/orange, background `#1E1234`, surface `#2A1F40`, onSurface `#E1BEE7`.
  - `LightColorScheme`: background `#F7F2FF`, surface `#FBF8FF`, primary `#6C4FF5`.
  - Di Android 12+, warna dinamis sistem dipakai lebih dulu; jika ada artwork, skema album menimpa.
- Tidak ada default gelap: `darkTheme = isSystemInDarkTheme()`.
- Ada `LocalPixelPlayDarkTheme` dan `LocalShowScrollbar` sebagai composition local.
- `PixelPlayStatusBarStyle` mengatur status/navigation bar Android secara transparan.

**Terjemahan:** IslandDesk akan memakai `MaterialExpressiveTheme` secara eksplisit, default gelap, dan tidak memakai dynamic color Android. Seed color dan skema tonal dihitung sendiri di desktop.

### 4.2 Warna

File kunci: `ui/theme/Color.kt`, `ui/theme/ColorRoles.kt`.

- Palet dasar kecil dan sengaja terbatas: purple, pink, orange, near-black, putih, light purple.
- `ColorRoles.kt` (728 baris) melakukan ekstraksi warna dari artwork:
  - resize gambar,
  - kuantisasi dan skoring warna,
  - penalti chroma berlebih,
  - fallback ke skema netral jika artwork hampir grayscale,
  - konversi ke `ColorScheme` lewat HCT/dynamic scheme.
- Pemakaian warna di UI:
  - play/pause: `primary`;
  - skip: `secondaryFixedDim`;
  - aksen lain: `tertiaryFixedDim`;
  - wadah tidak aktif: `onSurface.copy(alpha = 0.07f)`;
  - panel bawah: `surfaceContainerLowest.copy(alpha = 0.7f)`.
- Alpha sering dipakai untuk meredam permukaan, bukan menambah warna baru.

**Terjemahan:** IslandDesk memakai tonal palette dari satu seed, ditambah opsi aksen artwork yang hanya menyentuh `primary/secondary/tertiary`, bukan surface hitam island. Alpha untuk permukaan sekunder harus dijaga agar kontras teks tetap ≥ 4.5:1.

### 4.3 Tipografi

File kunci: `ui/theme/Type.kt`.

- Font utama: **Google Sans Flex variable** dengan sumbu `ROND = 100` (rounded). Ini font proprietary; tidak boleh disalin.
- Skala `Typography`:
  - display: 48 / 36 / 30 sp, bold/normal;
  - headline: 32 / 28 / 24 sp, semibold;
  - title: 22 / 18 / 14 sp, normal/medium;
  - body: 16 / 14 / 12 sp;
  - label: 16 / 14 / 11 sp, medium.
- Letter spacing hampir 0; line height 1.16–1.33×.
- Ada `ExpTitleTypography` terpisah memakai Montserrat dengan `TextGeometricTransform(scaleX = 1.5f)` untuk judul pemasaran.
- Varian `*Emphasized` dipakai secukupnya: `titleLargeEmphasized` (6×), `bodyLargeEmphasized` (3×), `labelLargeEmphasized` (1×).

**Terjemahan:** IslandDesk tidak memakai Google Sans. Untuk Fase 1 gunakan font sistem desktop (fallback stack) dengan **tabular numerals** untuk jam. Jika ingin karakter rounded, bundel font OFL (mis. Figtree/Manrope/Inter) setelah disetujui. Skala island jauh lebih kecil: jam 18–30 sp, judul modul 14–16 sp, body 12–13 sp, caption 10–11 sp.

### 4.4 Shape

File kunci: `ui/theme/Shape.kt` dan pemakaian di komponen.

- `Shapes` dasar: small 8 dp, medium 16 dp, large 24 dp.
- Namun UI nyata memakai bahasa yang jauh lebih bulat:
  - pill / circle: 50–60 dp, bahkan `RoundedCornerShape(999.dp)`;
  - chip asimetris: `topStart=50, topEnd=6, bottomStart=50, bottomEnd=6` (dan varian cerminnya);
  - kartu/panel: 18–24 dp;
  - tombol play/pause: sudut 60 dp saat playing, 26 dp saat paused;
  - `AbsoluteSmoothCornerShape` (library `racra`) untuk squircle halus dengan smoothness 60%.
- Sudut beranimasi memakai `animateDpAsState` dengan spring, bukan swap shape instan.

**Terjemahan:** IslandDesk memakai satu `IslandShape` kustom yang bisa diinterpolasi (corner radius + concave fillet), bukan dua komponen berbeda. Untuk Fase 1, interpolasi `RoundedCornerShape` sudah cukup; fillet cekung di tepi atas dibuat dengan `GenericShape`/Path. `MaterialShapes` disiapkan untuk aksen ikon/badge, bukan untuk badan panel.

### 4.5 Motion

File kunci: `AnimatedPlaybackControls.kt`, `FullPlayerContent.kt`, `UnifiedPlayerSheetV2.kt`, `WavySliderExpressive.kt`.

- `MotionScheme.expressive()` dipakai sebagai sumber spec:
  - `fastSpatialSpec<Float>()` untuk animasi tekan tombol;
  - `defaultSpatialSpec<Dp>()` untuk morph corner radius play/pause;
  - `fastEffectsSpec()` untuk crossfade ikon play/pause.
- Spring manual juga dipakai:
  - `spring(dampingRatio = MediumBouncy, stiffness = Low)` untuk chip cast yang melebar;
  - `animateContentSize` dengan spring yang sama.
- Tween pendek 120–400 ms untuk fade/slide, `FastOutSlowInEasing`.
- Ada **pelajaran performa** di `AlbumCarouselSelection.kt:71–78`: spring expressive default dianggap terlalu lambat dan memantul untuk scroll carousel besar, lalu diganti spring no-bounce yang lebih cepat. Artinya: token expressive bagus untuk perubahan kecil, tetapi permukaan besar perlu spring yang lebih terkontrol.
- `WavySliderExpressive` mengisolasi progres visual dari recomposition: nilai diperbarui di frame clock dan dibaca di draw lambda, bukan state yang memicu recomposition tiap frame.

**Terjemahan:** Gunakan `MotionScheme.expressive()` sebagai default, tetapi:
- bounds panel (width/height/corner) memakai satu spec spatial yang sama di seluruh app;
- fade konten memakai effects spec;
- untuk expand besar, pilih `fastSpatialSpec` atau override spring tunggal yang terdokumentasi, bukan angka acak per komponen;
- progress media di-update di draw phase agar idle/render tetap ringan.

### 4.6 Layout dan pengelompokan komponen

File kunci: `presentation/components/player/FullPlayerContent.kt`.

**Portrait (vertikal):**

```
Column(fillMaxSize, padding horizontal 24dp, SpaceAround)
├─ Album cover (carousel)
├─ Column(gap 4dp)
│  ├─ Metadata row (min 70dp)
│  └─ Progress section (min 70dp)
└─ Controls section (182dp)
   ├─ Transport row (80dp)
   └─ Secondary toggle row (66–86dp)
```

**Landscape:**

```
Row(fillMaxSize, padding horizontal 24dp, CenterVertically)
├─ Album cover (weight 1)
├─ Spacer 9dp
└─ Column(weight 1, SpaceEvenly)
   ├─ Metadata
   ├─ Progress
   └─ Controls
```

**Top bar player:**

- `TopAppBar` transparan.
- Tombol collapse: lingkaran 42 dp, di dalam area 56×42 dp, padding tepi 18/14 dp.
- Chip aksi: tinggi 42 dp, lebar 50 dp (ikon) atau hingga 190 dp saat label muncul.
- Jarak antar aksi 6 dp.

**Metadata row:**

- Tinggi minimum 70 dp.
- Kiri: judul + artis (weight 1).
- Kanan: chip lirik/queue 42–50 dp, atau `FilledIconButton` 48 dp.
- Gap 12 dp; padding kanan 8 dp untuk indikator loading.
- `LoadingIndicator` 28 dp di dalam lingkaran 48 dp saat buffering.

**Progress:**

- Tinggi minimum 70 dp.
- Slider wavy: stroke 5 dp, thumb radius 8 dp, tinggi thumb saat interaksi 24 dp, padding vertikal 8 dp.
- Time labels di bawah, `SpaceBetween`, padding horizontal mengikuti track inset.
- Wave hanya beranimasi saat playing dan panel terlihat; saat pause amplitude → 0.

**Transport:**

- Tinggi 80 dp; tiga tombol `weight` sama, gap 6 dp.
- Saat ditekan: tombol aktif weight 1.1, lainnya 0.65; release delay 220 ms.
- Play/pause morph corner 60 ↔ 26 dp; ikon crossfade.

**Secondary toggle row:**

- Wadah `AbsoluteSmoothCornerShape(60.dp)`, padding 6 dp.
- Tiga segmen sama besar, gap 6 dp, tinggi total 66–86 dp.
- Ini pada dasarnya **segmented button group** dengan container dan item terpisah.

**Player sheet:**

- Surface full-width, shape/shadow dinamis, konten digeser dengan translasi.
- Ada drag handle dan padding bawah dinamis.
- Konten muncul bertahap berdasarkan `expansionFraction` (threshold 0.08–0.42), dengan placeholder saat transisi.

**Pelajaran untuk island:** hierarki yang sama bisa dipakai — header tipis, satu area konten, satu baris kontrol. Yang berbeda: island tidak punya ruang untuk artwork besar dan tidak boleh memakai padding 24 dp di collapsed.

### 4.7 Inventaris spacing, ukuran, dan radius

Dari pemakaian nyata di file-file di atas:

| Kategori | Nilai yang muncul |
|---|---|
| Gap kecil | 4, 6, 8 dp |
| Gap sedang | 12, 14, 16, 18 dp |
| Padding tepi | 18, 22, 24, 26 dp |
| Tinggi kontrol | 42, 48, 66, 70, 80, 86, 182 dp |
| Ukuran ikon | 16, 18, 24, 28, 32, 36 dp |
| Radius | 6, 8, 10, 16, 18, 24, 26, 50, 60, 999 dp |
| Touch target Android | 42–48 dp |

Untuk desktop, nilai 42–48 dp dipertahankan sebagai **target klik**, tetapi tinggi visual bisa lebih kecil (mis. 36 dp) dengan area hit yang diperluas.

### 4.8 Komponen expressive yang benar-benar dipakai

| Komponen | Pemakaian di PixelPlayer | Relevansi untuk IslandDesk |
|---|---|---|
| `LoadingIndicator` | 35 kemunculan; indikator buffering/loading | Loading modul, sinkronisasi, download |
| `LinearWavyProgressIndicator` | 8 kemunculan; progres media | Progres media dan download |
| `CircularWavyProgressIndicator` | 4 kemunculan | Timer/pomodoro ringkas |
| `MotionScheme.expressive()` | 8 kemunculan | Semua transisi island |
| Tipografi `*Emphasized` | title/body/label | Jam besar dan angka status |
| `ButtonGroup` (API M3) | tersedia, tetapi PixelPlayer memakai segmented control kustom | Tab modul dan quick actions |
| `SplitButton` (API M3) | tersedia, tidak dipakai | Aksi "gabung rapat" dengan menu |
| `FloatingToolbar` (API M3) | tersedia, PixelPlayer memakai toolbar kustom | Aksi kontekstual shelf/clipboard |
| `ToggleButton` | tersedia | Mic/Wi-Fi/Bluetooth |
| `MaterialShapes` | tersedia | Badge/ikon state, bukan panel |

Catatan: angka "35/8/4" adalah jumlah kemunculan teks API di source, bukan jumlah instance runtime.

#### 4.8.1 Implementasi aktual di IslandDesk

| API | Lokasi | Alasan |
|---|---|---|
| `MaterialExpressiveTheme` + `MotionScheme.expressive()` | `theme/Theme.kt` | Sumber semua spec motion dan token expressive |
| `ToggleButton` + `ToggleButtonSize.Medium` | `ControlsModule` | Tile quick settings; kontainer bermorfosis saat ditekan dan berubah ke `primaryContainer` saat aktif |
| `ButtonGroup` + `customItem` + `Modifier.animateWidth` | `MediaModule` | Tombol previous/next saling menekan (squish) saat salah satu ditekan |
| `IconButtonShapes` (pill → 14 dp) | `MediaModule` play/pause | Morph bentuk saat ditekan, bukan sekadar ripple |
| `MaterialShapes` + `RoundedPolygon.toShape()` | `Expressive.kt` → empty state Media/Shelf | Siluet organik sebagai latar ikon, menghindari rounded-rect biasa |
| `LinearWavyProgressIndicator` | Peek, kartu media, focus timer | Progres media dan sesi fokus |
| Tipografi `*Emphasized` | Jam peek, judul media, nilai tile, angka fokus | Hierarki angka/teks utama |
| `FilterChip` | `SettingsModule` tema System/Light/Dark | Pemilihan eksklusif ringkas |
| `animateColorAsState` + `slowSpatialSpec` | Rail modul | Seleksi rail beranimasi, ikon membesar 1.08× |

Prinsipnya: expressive dipakai pada **titik interaksi** (tekan, pilih, buka), bukan sebagai dekorasi di seluruh permukaan. Permukaan besar tetap memakai spring terkontrol agar tidak memantul berlebihan.

### 4.9 Pola yang layak diadopsi dan yang perlu dihindari

**Diadopsi:**

- Satu permukaan yang bermorfosis, bukan dua kartu yang bertukar.
- Sudut besar dan konsisten; pill untuk kontrol, rounded-rect untuk konten.
- Hierarki tenang: satu fokus besar (artwork/jam), teks pendukung kecil.
- Pengelompokan kontrol dalam baris dengan bobot sama dan gap 6–8 dp.
- Warna aksen dari artwork, tetapi surface tetap netral/gelap.
- Progress wavy yang hanya beranimasi saat relevan.
- Placeholder dan threshold bertahap saat panel membuka.

**Dihindari:**

- API Android-only: `dynamicDarkColorScheme`, `GoogleFont.Provider`, `LocalHapticFeedback`, Palette Android.
- Ukuran 48 dp dan padding 24 dp di collapsed; terlalu besar untuk island.
- Spring bouncy pada permukaan besar; bisa terasa lambat dan memicu frame mahal.
- Alpha bertumpuk yang menurunkan kontras teks.
- Shadow besar di atas window transparan; di X11 bisa mahal dan terlihat kotor.

---

## 5. Terjemahan ke IslandDesk

### 5.1 Token desain awal

**Spacing (grid 4 dp):**

| Token | Nilai | Pemakaian |
|---|---|---|
| `space2` | 2 dp | hairline, separator |
| `space4` | 4 dp | gap ikon-teks |
| `space6` | 6 dp | gap antar tombol |
| `space8` | 8 dp | gap kontrol |
| `space12` | 12 dp | padding dalam kartu kecil |
| `space16` | 16 dp | padding panel peek |
| `space20` | 20 dp | padding panel expanded |
| `space24` | 24 dp | padding layar settings |

**Radius:**

| Token | Nilai | Pemakaian |
|---|---|---|
| `radiusSmall` | 10 dp | chip kecil, thumbnail |
| `radiusMedium` | 16 dp | kartu modul |
| `radiusLarge` | 24 dp | panel peek |
| `radiusXLarge` | 32 dp | panel expanded |
| `radiusPill` | 999 dp | pill, tombol bulat |

**Ukuran panel (draft):**

| State | Lebar | Tinggi | Padding | Radius | Catatan |
|---|---|---|---|---|---|
| Collapsed | 244 dp | 38 dp | 12 dp horizontal | 19 dp | jam + tanggal + speed + baterai |
| Peek | 420 dp | 140 dp | 16 dp | 34 dp | jam HH/MM/ss + tanggal, media + kontrol, baterai, notifikasi/fokus |
| Expanded | 460 dp | 620 dp | 16 dp | 34 dp | rail modul + konten (tanpa footer) |
| Tab menempel | sama | sama | sama | atas 0, bawah 32 dp, fillet 10 dp | default layar tanpa notch |

Angka ini adalah titik awal, bukan final. Akan divalidasi di Fase 1 pada layar 1080p/1440p dan skala fractional GNOME.

### 5.2 Tiga level informasi

**Collapsed — 38 dp, glance < 1 detik:**

```
┌──────────────────────────────────────────┐
│ 09:41  Sen, 19 Okt │  ↓1.2M ↑240K ▮ 82% │
└──────────────────────────────────────────┘
```

- Satu informasi dominan: jam, dengan tanggal di sebelahnya.
- Kecepatan jaringan hanya muncul saat ada trafik; idle = ruang kembali ke baterai.
- Baterai sebagai ikon + persen; lebar mengikuti konten, tidak pernah dipecah ke dua baris.
- Tidak ada tab, tidak ada teks sekunder.
- Kiri dan kanan dipisah `SpaceBetween` supaya ruang kosong berada di tengah, bukan menumpuk di kanan.

**Peek — 140 dp:**

```
┌────────────────────────────────────────────┐
│ 09        │ ♪ Title — Artist    ▮ 82%     │
│ 41        │ ────────────────────          │
│ ss        │ 01:12 / 04:20   ⏮ ▶ ⏭        │
│ Sen, 19   │ [ notifikasi terbaru ]        │
└────────────────────────────────────────────┘
```

- Kiri: jam bertumpuk HH/MM/ss + tanggal (tanggal wajib ada, sama seperti collapsed).
- Kanan atas: artwork, judul, artis, DND, baterai.
- Kanan tengah: progress wavy + posisi/durasi.
- Kanan bawah: kontrol transport mini (prev/play-pause/next) supaya tidak perlu membuka expanded.
- Baris paling bawah dipakai notifikasi terbaru, atau pengingat fokus.
- Saat sesi fokus aktif, seluruh kolom kanan diganti hero fokus: judul, hitung mundur 34 sp dengan detik, jam selesai, dan progress — fokus adalah informasi utama pada momen itu.
- Tetap satu kolom tanpa scroll.

**Expanded — panel penuh:**

```
┌────────────────────────────────────────────┐
│ 09:41   [Media] [Clipboard] [Shelf] …  ⚙  │  header + tab
├────────────────────────────────────────────┤
│                                            │
│  Konten modul aktif                        │
│                                            │
├────────────────────────────────────────────┤
│  ⌘K cari   •   DND   •   pengaturan         │  footer
└────────────────────────────────────────────┘
```

- Header 44 dp: jam/status + tab strip + tombol settings.
- Tab strip: 3–5 modul, icon + label, tinggi 40–44 dp; selected pill.
- Konten: satu area scroll, padding 16–20 dp.
- Footer 36–40 dp: quick actions, hint keyboard.
- Esc menutup; Tab/panah/Enter navigasi.

### 5.3 Shape dan morph

- Satu `IslandShape` dengan parameter:
  - `topRadius` (0 untuk tab menempel),
  - `bottomRadius` (18/28/32),
  - `filletRadius` (0–12 dp, cekung di kedua sisi atas),
  - `width`, `height`.
- Transisi collapsed → peek → expanded menginterpolasi parameter ini, bukan mengganti composable.
- Fillet cekung diimplementasikan dengan `GenericShape` dan quadratic/cubic curve. `RoundedCornerShape` tidak bisa menghasilkan cekungan.
- Untuk Fase 1, cukup `RoundedCornerShape` yang dianimasikan; fillet ditambahkan setelah bentuk dasar stabil.
- Inner elements mengikuti radius panel: `panelRadius - padding`, agar tidak ada sudut yang "keluar" dari panel.

### 5.4 Tipografi untuk island

| Peran | Gaya | Ukuran |
|---|---|---|
| Jam collapsed | `titleMediumEmphasized` / tabular | 18 sp / 22 line |
| Jam peek | `displaySmallEmphasized` | 30 sp / 34 line |
| Tanggal peek | `labelMedium` | 11 sp |
| Judul modul | `titleMedium` | 15–16 sp |
| Body modul | `bodyMedium` | 13 sp |
| Metadata | `bodySmall` | 11–12 sp |
| Caption/chip | `labelSmall` | 10–11 sp |
| Angka besar (timer) | `displayMediumEmphasized` | 36–40 sp |

Aturan:

- Gunakan `FontFeatureSettings("tnum")` atau font dengan tabular numerals agar jam tidak "bergeser".
- Maksimal dua ukuran teks menonjol per state.
- Hindari teks < 10 sp.
- Line height minimal 1.2× untuk teks kecil.

### 5.5 Warna

- Default gelap: surface island mendekati hitam netral dengan sedikit tint biru, agar menyatu dengan bezel.
- Aksen default **Navy** (bukan ungu baseline M3). Pengguna bisa memilih lima keluarga aksen di Settings: Navy, Violet, Emerald, Amber, Rose.
- Implementasi: `ui/theme/Color.kt` menyimpan `AccentSpec(hue, saturation)` per aksen lalu menghasilkan seluruh peran tonal lewat `Color.hsl`. Tidak ada 10 skema hardcode; satu rumus tonal menjaga kontras antar aksen tetap konsisten.
  - Dark: `primary` = hsl(hue, s×0.55, 0.78), `primaryContainer` = hsl(hue, s×0.70, 0.32), surface = hsl(hue, 0.16, 0.05).
  - Light: `primary` = hsl(hue, s×0.75, 0.34), `primaryContainer` = hsl(hue, s×0.70, 0.90), surface = hsl(hue, 0.24, 0.985).
  - `tertiary` memakai hue komplementer (+150°) agar tetap ada aksen sekunder tanpa menambah input pengguna.
- Swatch di Settings memakai `accentSwatch()`, versi pekat dari aksen yang sama.
- Aksen artwork opsional: ekstrak 1 warna dominan, batasi chroma, terapkan ke `primary`/`secondary`; jangan ubah surface.
- Kontras: teks utama ≥ 4.5:1, teks sekunder ≥ 3:1 untuk ukuran besar; ikon status ≥ 3:1.
- Tema terang opsional; jangan menjadi default.

### 5.6 Motion

| Transisi | Spec | Perkiraan terasa |
|---|---|---|
| Collapsed → Peek | `fastSpatialSpec` / spring terkontrol | 180–240 ms |
| Peek → Expanded | `defaultSpatialSpec` | 240–300 ms |
| Expanded → Peek/Collapsed | `fastSpatialSpec` | 200–260 ms |
| Fade konten | `fastEffectsSpec` | 120–180 ms |
| Tekan tombol | `fastSpatialSpec` | 120–180 ms |
| Morph corner | `defaultSpatialSpec` | 200–280 ms |
| Notifikasi auto-expand | spatial + delay 2.5 s | 250 ms + tahan |

Aturan:

- Hover intent: mulai expand setelah kursor diam **180 ms** (default; dapat diatur 150–250 ms).
- Collapse delay: **350 ms** setelah kursor keluar (dapat diatur 300–500 ms).
- Reduce motion: snap ke state akhir, fade maksimal 80 ms; wave progress dimatikan.
- Jangan animasikan tinggi dan lebar dengan dua spec berbeda; satu spec untuk bounds.
- Update progress di draw phase, bukan recomposition per frame.

### 5.7 Pola layout per modul

**Media (expanded):**

```
Row(gap 12dp)
├─ Artwork 64dp, radius 14dp
├─ Column(weight 1)
│  ├─ Title 14sp, 1 line
│  ├─ Artist 11sp, 1 line
│  └─ Wavy slider + time labels
└─ Column
   ├─ Play/pause 44dp
   └─ Next 36dp
```

**Clipboard:**

- Search field 40 dp di atas.
- Filter chips 32 dp (Semua/Teks/Gambar/Tautan/Kode).
- List item 60–64 dp: preview 2 baris, chip tipe, tombol pin/hapus.
- Aksi massal di footer: hapus semua, jeda rekam.

**Shelf:**

- Grid 3 kolom, tile 112×84 dp, radius 16 dp.
- Thumbnail mengisi tile, nama file 10–11 sp di bawah.
- Drag-over: panel membesar + border aksen 2 dp.
- Aksi per item: context menu (buka, salin path, file manager, kompres, resize, hapus).

**Quick controls:**

- Grid 3×2, tile tinggi 72–80 dp.
- Ikon 20 dp + label 11 sp + nilai (mis. 45%).
- Slider volume/brightness di bawah grid, tinggi 40 dp.
- Toggle Wi-Fi/Bluetooth memakai `ToggleButton` atau tile dengan state jelas.

**Settings:**

- Panel 520×600 dp; sidebar kategori 140 dp + konten.
- Baris setting 48–56 dp, judul 13 sp, deskripsi 11 sp, kontrol di kanan.
- Semua kontrol ≥ 40 dp; slider memiliki nilai numerik.

### 5.8 Aksesibilitas dan keyboard

- Fokus terlihat: ring 2 dp `primary`, offset 2 dp.
- Semua aksi dapat dijangkau Tab; panah untuk tab strip; Enter/Space aktivasi; Esc tutup.
- Tooltip untuk ikon tanpa label.
- Tidak ada informasi yang hanya disampaikan lewat warna.
- Reduce motion, skala teks, dan kontras harus dihormati.
- Notifikasi tidak boleh merebut fokus keyboard; gunakan window non-activating saat collapsed.

---

## 6. Keputusan yang perlu persetujuan sebelum Fase 1

1. **Nama dan package.** Tetap `IslandDesk`? Ganti package `id.andreasmlbngaol.d_util` sekarang atau nanti? Rename setelah Fase 1 lebih mahal.
2. **Ukuran panel.** Final: 220×38 / 420×140 / 460×620 (disesuaikan setelah revisi pengguna).
3. **Gaya default.** "Tab menempel" untuk layar tanpa notch, sesuai brief. Konfirmasi bahwa ini default.
4. **Font.** Font sistem + tabular numerals untuk Fase 1, atau langsung bundel font OFL rounded (mis. Figtree/Manrope). Jangan memakai Google Sans dari PixelPlayer.
5. **Seed color default.** Baseline M3 `#6750A4`, atau warna lain (mis. indigo kebiruan `#5B5BD6`).
6. **Cakupan Fase 1.** Hanya window + tiga state + konten dummy + tema, atau sekalian kerangka `IslandModule` dan settings sederhana? Rekomendasi: kerangka modul minimal, agar Fase 3 tidak refactor besar.
7. **Arsitektur window.** Konfirmasi pendekatan JNA untuk input region di Fase 2 (bukan Fase 1), dan bahwa Fase 1 hanya menguji transparansi/posisi/always-on-top.

---

## 7. Rencana Fase 1 (setelah catatan ini disetujui)

1. Pisahkan modul Gradle: `core`, `ui`, `platform`, `modules/*`, `app` (desktop). Untuk Fase 1, minimal `core` + `ui` + `app`.
2. Buat `IslandDeskTheme` dengan `MaterialExpressiveTheme`, dark scheme dari seed, typography expressive, shapes.
3. Buat window desktop: `undecorated`, `transparent`, `alwaysOnTop`, posisi tengah atas, tanpa taskbar (eksperimen).
4. Implementasikan state machine `Collapsed/Peek/Expanded` + hover intent + collapse delay + Esc/klik luar.
5. Implementasikan `IslandShape` dan animasi bounds; konten dummy per state.
6. Uji di X11 dan Wayland (XWayland) di Zorin; catat di `docs/platform-notes.md`.
7. Ukur idle CPU dan frame time; pastikan tidak ada polling.
8. Commit kecil: theme, window, state machine, shape, dokumentasi.

Kriteria penerimaan Fase 1:

- Island tampil di tengah atas, transparan, dan terasa seperti satu bentuk yang membesar.
- Hover singkat → peek; hover lama/klik → expanded; keluar → collapse dengan delay.
- Klik di area transparan tidak terhalang (atau diketahui keterbatasannya dan dicatat).
- Build dan test lulus.
- Catatan X11 vs Wayland lengkap.

---

## 8. Metode dan bukti

- Eksplorasi read-only terhadap `~/Code/cloned/PixelPlayer` (tidak ada file referensi yang diubah).
- File yang dibaca langsung:
  - `ui/theme/Theme.kt`, `Color.kt`, `Type.kt`, `Shape.kt`, `ColorRoles.kt`
  - `presentation/components/player/FullPlayerContent.kt`
  - `presentation/components/player/AnimatedPlaybackControls.kt`
  - `presentation/components/WavySliderExpressive.kt`
  - `presentation/components/UnifiedPlayerSheetV2.kt`
  - `presentation/screens/CreatePlaylistScreen.kt`
- Verifikasi API dilakukan dengan `unzip -l` dan `javap` pada artifact Gradle yang terpasang, bukan dari dokumentasi atau ingatan.
- Tidak ada kode PixelPlayer yang disalin ke IslandDesk.

---

## 9. Catatan terbuka

- `MaterialExpressiveTheme` masih alpha; perlu smoke test kompilasi sebelum membangun banyak UI di atasnya.
- Perilaku transparansi dan always-on-top di Wayland GNOME perlu diuji nyata; jangan diasumsikan bekerja seperti X11.
- `WavyProgressIndicatorDefaults` dan parameter `LinearWavyProgressIndicator` harus dicek signature-nya saat implementasi (alpha API).
- Untuk artwork accent, pilihan library ekstraksi warna desktop belum diputuskan; kandidat: `material-color-utilities` (Apache 2.0) atau implementasi kuantisasi sendiri.
