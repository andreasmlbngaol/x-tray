<div align="center">

# X-Tray

**A Dynamic Island for the Linux desktop.**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.21-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.13-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://www.jetbrains.com/compose-multiplatform/)
[![Material 3 Expressive](https://img.shields.io/badge/Material%203-Expressive-6750A4?style=for-the-badge&logo=materialdesign&logoColor=white)](https://m3.material.io/)
[![Linux](https://img.shields.io/badge/Linux-X11%20%7C%20Wayland-FCC624?style=for-the-badge&logo=linux&logoColor=black)](https://kernel.org/)
[![Release](https://img.shields.io/github/v/release/andreasmlbngaol/x-tray?style=for-the-badge&logo=github&color=6E40C9)](https://github.com/andreasmlbngaol/x-tray/releases)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-D22128?style=for-the-badge&logo=apache&logoColor=white)](./LICENSE)

<img src="docs/images/hero.png" alt="X-Tray closed, docked at the top centre of the screen" width="760">

</div>

X-Tray is a single panel that sits at the top centre of your screen. At rest it
is only 300 px wide: the time, the date, network speed, and the battery. When
your pointer gets close it grows to show what is playing and the latest
notification, and when you click it, it opens fully with quick controls, a
calendar, the notification history, the clipboard, time tools, the shelf, and
settings.

No window to keep around, no extra taskbar icon. Everything you usually look
for in five different places lives in one panel you can summon from the top
edge.

## What it does

- **Glanceable at rest.** Time, date, upload and download speed, battery.
- **Music without switching apps.** Title, artist, and album show up in peek,
  with play, previous, and next buttons.
- **One place for notifications.** X-Tray takes over desktop notifications, so
  the stock popups stop and everything lands in the island.
- **Volume and brightness.** Two sliders, plus mic, Wi-Fi, Bluetooth, do not
  disturb, dark mode, and airplane.
- **Clipboard history.** Every copied text stays in memory while the app runs;
  click one to copy it again.
- **A shelf for files.** Drag files onto the island, see thumbnails for images
  and videos, open or delete them on hover, and drag them back out.
- **Time tools.** A stopwatch, a countdown timer, and a focus session that
  counts down in peek and notifies you when it ends.
- **Calendar and notification history.** A month grid you can jump through by
  month or year, and every notification you received, grouped per app.
- **Out of the way when you want.** Right click shrinks it to a 120x6 hairline
  at the top edge; hovering brings it back.

## How you use it

<img src="docs/images/states.png" alt="Peek and the notification card" width="900">

| You do | What happens |
|---|---|
| Leave it alone | Small bar: time, date, network speed, battery |
| Move the pointer over it | Peek: stacked clock, music, latest notification |
| Click | Expanded: every module |
| Right click | Collapses to a hairline at the top edge |
| Drag a file onto it | The shelf opens and takes the file |
| Esc or click outside | Back to the small bar |

Peek closes by itself when the pointer leaves. Expanded only closes on Esc, a
click outside, or when you switch windows.

## The expanded panel

<img src="docs/images/expanded.png" alt="X-Tray expanded on the Controls tab" width="420">

The rail on the left has seven tabs, and **Controls** is the one that opens by
default:

- **Controls** — volume, brightness, and a 4×2 quick-settings grid (mic,
  Wi-Fi, Bluetooth, airplane, do not disturb, dark mode, mute, and Settings).
  Hold a tile to open the matching desktop settings page; the full music player
  sits right under the grid.
- **Calendar** — a month grid with today marked. Pick the month and the year
  from their own menus, or step a month at a time. Clicking the date in the
  header opens the same tab.
- **Notifications** — the history of everything the island received, grouped
  per app. Expand a group to read all of its messages, swipe a message away,
  swipe the group header to drop the sender, or clear the lot.
- **Clipboard** — text history, click to copy again, clear all at once.
- **Time** — a stopwatch, a countdown timer, and a focus session with a
  duration you can type. All three keep running when the panel closes.
- **Shelf** — the files you dragged onto the island, with clear all.
- **Settings** — theme, accent colour, reduce motion, autostart, hover timing,
  notification takeover, and reset.

The header above holds the clock, the date, network speed, a power menu, and the
close button.

## Notifications

While X-Tray runs, it is the desktop notification receiver. When the bar is
small, a notification appears as a compact card that hugs its content (max 560 dp wide)
with the sender's real app icon,
app name, title, body, and a close button. The card still appears while the
island is hidden or dormant; only **Do not disturb** suppresses it. Clicking the
card asks the sender to open the matching content (for example the chat behind a
ZapZap message) through its default D-Bus action, falling back to launching the
app's desktop entry. In peek it shows on the bottom row; in expanded it becomes a
banner above the active module.

Every message is also kept in the **Notifications** tab, grouped per app, so a
sender that posts often cannot flood the list. A horizontal swipe removes one
message, swiping the group header removes every message from that sender, and
**Clear all** empties the history. Clicking a message opens the app that sent it.

Because X-Tray replaces the stock popups, it also plays the desktop notification
chime itself, through libcanberra so it follows the sound theme you already use
(with `paplay`/`pw-play` and a plain beep as fallbacks). **Do not disturb**
silences it.

If you would rather keep the stock popups, turn off **Replace system
notifications** in Settings. When X-Tray exits, the desktop gets notifications
back on its own.

## Install

### One command (Linux)

The installer resolves the latest release, downloads the `.deb` to a temporary
file, installs it with `apt`, and deletes the file again:

```bash
curl -fsSL https://raw.githubusercontent.com/andreasmlbngaol/x-tray/master/scripts/install.sh | bash
```

Pass `--version` to pin a release (the pipe form needs `bash -s --` to forward
arguments):

```bash
./scripts/install.sh --version v1.3.0
curl -fsSL https://raw.githubusercontent.com/andreasmlbngaol/x-tray/master/scripts/install.sh | bash -s -- --version v1.3.3
```

The script needs `curl`, `dpkg` and `apt-get` (Debian, Ubuntu, Zorin) and only
publishes `amd64` packages today. Nothing is left in `/tmp` afterwards.

### From a release (Linux)

Prefer to do it by hand? Download the latest `.deb` from
[Releases](https://github.com/andreasmlbngaol/x-tray/releases), then:

```bash
sudo apt install ./x-tray_1.3.3_amd64.deb
```

Start **X-Tray** from your app menu. The package bundles its own Java runtime,
so you do not need a JDK.

### From source

You need JDK 21.

```bash
./gradlew :app:run
```

Build the package yourself:

```bash
./gradlew :app:packageReleaseDeb
```

## Quick commands

While the app is running, commands go to the same instance over a Unix socket:

```bash
x-tray toggle        # open or close the expanded panel
x-tray show          # bring the bar back
x-tray hide          # shrink to the hairline
x-tray notify "Hi"   # send a notification into the island
x-tray dnd on        # do not disturb
x-tray activity start 25 "Write the report"
x-tray quit
```

Running `x-tray` again with no arguments does not open a second window; it just
brings the running one forward.

## Settings worth knowing

| Setting | What it does |
|---|---|
| Theme | Follow the system, always light, or always dark |
| Accent colour | Five presets or your own HEX code |
| Reduce motion | Turns animations off |
| Autostart | Start X-Tray when you log in |
| Hide over maximized | Shrinks to the hairline while a maximized window is focused |
| Replace system notifications | Takes over desktop notifications |
| Hover and close delay | How quickly peek opens and closes |

## Notes

- Works on X11 and on Wayland through XWayland.
- Notification takeover relies on the GNOME notification helper. On other
  desktops X-Tray leaves the original notifications alone.
- The tray icon needs the AppIndicator extension on GNOME.
- Clipboard and shelf are kept in memory only, for as long as the app runs.

## License

X-Tray is licensed under the **Apache License 2.0**. See [LICENSE](./LICENSE).

The **Google Sans Flex** font is used under the SIL Open Font License 1.1
(`docs/licenses/google_sans_flex_OFL.txt`). PixelPlayer was studied as a design
reference only; none of its code or assets were copied.

Copyright 2026 Andreas M Lbn Gaol
