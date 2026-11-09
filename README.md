# Joule Arc

Smart battery and charging monitor for Android. It keeps watching the charging system, so it can tell you how fast your device is charging and, over time, when and why it charges faster or slower.

## Features

**Available now (early development)**
- One home screen with the main readings; history, consumption, battery use by app and settings open as separate pages.
- Live current on a gauge centered on zero (negative = out of the battery), updated twice per second while the app is open, with % per hour, time to full or time left, and the highest and lowest current recorded.
- Amber warning when a charger is connected but the battery is not charging or is losing charge.
- Voltage, temperature, level, power, status, power source, health, technology and cycle count, each with a plain-language rating and a detail sheet that explains it.
- Battery capacity: charge now, full charge today (estimated), lost to wear and design capacity, with estimated health.
- Charging session history with current and level curves, and which power source and time of day charge fastest and slowest.
- Consumption with the screen on and off, estimated battery life, and which time of day drains the most.
- Battery use by app: 24-hour level chart, 14-day usage chart and a per-app list with an estimated percentage (Android does not expose per-app mAh to regular apps).
- Charge and low battery reminders.
- Quick Settings tile, home screen widget and a persistent notification.
- Material 3 Expressive interface with dynamic color, light and dark themes, English and Portuguese.

## Privacy and permissions

- **Notifications** (Android 13+): the persistent monitoring notification and, if you turn them on, the charge and low battery reminders.
- **Foreground service**: keeps the monitor running. Sampling is slower when the device is not charging, to save battery.
- **Start at boot**: restarts the monitor after the device reboots.
- **Usage access** (special permission, granted by you in system settings): used to measure how long apps run in the foreground and background for the battery use by app page. The app only asks for it there. The data is read on demand and not stored.

The charging session history, the 14-day battery level history and your settings are stored only on your device, in the app's private storage. The app does not declare the internet permission. "Report a problem" opens your own email app with a draft; nothing is sent unless you send it.

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
