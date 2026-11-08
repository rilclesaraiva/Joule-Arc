# Joule Arc

Smart battery and charging monitor for Android. It keeps watching the charging system, so it can tell you how fast your device is charging and, over time, when and why it charges faster or slower.

## Features

**Available now (early development)**
- Persistent notification while monitoring, with current, level and temperature.
- Live charging current, battery level, voltage, temperature, power and charge status, updated twice per second while the app is open.
- Charging estimate: percent per hour and when the battery should be full.
- Charging session history, and a comparison of what charges fastest (by power source and time of day).
- Battery health: estimated capacity, estimated health against the rated capacity you enter, and the cycle count when your device reports it (Android 14+).
- Battery usage screen: hourly battery level for 24 hours and daily usage for 14 days, with the last charge to 100%, and a per-app ranking with icons (an estimate: the drained percentage is split between apps by foreground and background time, since Android does not expose per-app mAh to regular apps).
- Swipe between the tabs; the tab buttons are pills.
- Quick Settings tile and home screen widget.
- Material 3 Expressive interface with dynamic color.

**Planned**
- Charts for charging sessions.

## Privacy and permissions

- **Notifications** (Android 13+): required to show the persistent monitoring notification.
- **Foreground service**: keeps the monitor running. Sampling is slower when the device is not charging, to save battery.
- **Start at boot**: restarts the monitor after the device reboots.
- **Usage access** (special permission, granted by you in system settings): used to measure how long apps run in the foreground and background for the Apps tab. The app only asks for it when you open that tab. The data is read on demand and not stored.

The charging session history and the 14-day battery level history are stored only on your device, in the app's private storage. The app does not declare the internet permission.

- [Privacy policy](https://rilclesaraiva.github.io/Joule-Arc/privacy) · [Terms](https://rilclesaraiva.github.io/Joule-Arc/terms) (source in [`docs/`](docs))

## 🤖 Built with AI ("vibe coding")

In the spirit of transparency: **Joule Arc is developed with heavy use of AI.**
Most of the code is written by [Claude](https://claude.ai) (Anthropic) under the
direction of the human maintainer, a workflow often called *vibe coding*.
We take care that it doesn't turn into AI slop:

- **The maintainer decides, the AI executes.** Every feature starts from a real
  need, and every build is tested on real Android devices before the next round.
- **Written rules the AI has to follow.** [`CLAUDE.md`](CLAUDE.md) sets the
  architecture, privacy limits and process, and is updated with every decision.
- **Tests and automated builds.** Every change goes through GitHub Actions,
  and a failing test means no build.
- **No release without approval.** Nothing is published without the
  maintainer's explicit approval.
- **Honest changelogs.** [`CHANGELOG.md`](CHANGELOG.md) says what changed,
  including what is still broken.

If AI-assisted code is a concern for you, now you know, and you are welcome to
read every line. Found something careless? Please [open an issue](../../issues).

## Installation

No public release yet. Signed builds will be attached to the [Releases](../../releases) page.

## License

[GPL-3.0](LICENSE)

## ☕ Support the project

Joule Arc is free and maintained independently. If it's useful to you, you can
support its development with a coffee:

[![Buy Me a Coffee](https://img.shields.io/badge/Buy%20Me%20a%20Coffee-support-FFDD00?logo=buymeacoffee&logoColor=black)](https://buymeacoffee.com/saraivarilcle)

**Contact:** [saraivarilcle@gmail.com](mailto:saraivarilcle@gmail.com)
