# Changelog

## [Unreleased]

## [0.1.0] - 2026-11-09
First public version.

### Added
- Persistent foreground-service notification that keeps monitoring the charging system, restarted after reboot. Its title is the state ("Charging", "Discharging"...), with current, level and temperature below. Sampling: every 500 ms with the app open, every 2 s while charging with the screen off, every 30 s otherwise, and at once on any battery event.
- Single home screen with the main readings, and separate pages for charging history, consumption, usage by app and settings.
- Current gauge centered on zero (negative = out of the battery, positive = into it), with % per hour, time to full or time left, and the highest and lowest current recorded (with reset). The gauge keeps its scale: it grows at once when needed and shrinks only after the value stays low for a while.
- Amber state and warning when a charger is connected but the battery is not charging or is losing charge (weak or faulty charger or cable, charging limit or protection). It waits 45 seconds after the reading turns odd ("Checking charge"), and the current direction is learned per device from the sign it reports.
- Readings grid with small gauges and plain-language ratings (Normal, Warm, Good, Fast, Heavy use...): voltage, temperature, level, power, status, power source, health, technology and cycle count. Tap any reading for a detail sheet with a colored range, lowest and highest since the app was opened, what the charger offers (when the system reports it) and what the value means. The texts fit the situation: with no charger connected they no longer tell you to unplug or keep charging. Charging power from 65 W up is shown in blue (above the ideal range, not harmful).
- Battery capacity card: charge now, full charge today (estimated from the charge counter, sampled only when the level changes and smoothed), capacity lost to wear and device capacity, with estimated health.
- Device capacity detected by the app from the rated "battery.capacity" of the system power profile (the adjusted value some phones report is ignored), then from the framework's XML resource and the kernel node when readable; the value typed in Settings always wins. The Device row says where the detected value came from.
- Device section: one panel that opens a single sheet with model, manufacturer, Android version, API level, security update, device code and device capacity.
- Charging session history (stored only on the device), a comparison of the fastest and slowest power source and time of day, and a page for each session with its current and level curves.
- Consumption page: battery drain with the screen on and off (percent and estimated mAh per hour), estimated battery life at those rates, and the times of day that drain the most and the least.
- Usage by app page, inspired by Android's own: hourly battery level for 24 hours, daily usage for 14 days, last charge to 100% and a per-app list with icons and an estimated percentage (Android does not let regular apps read per-app mAh).
  - Tap a bar to see only that hour (24 hours) or that day (14 days, which also shows the hours of the chosen day); tap it again to go back to the overall view. Holding a bar shows its time.
  - System apps are hidden by default; the three-dot menu shows them. Percentages still count them, so the shown apps may add up to less than the total.
  - Tapping an app opens its page: total time, active and background time and the estimated share of the battery of each, drawn as a bar filled by battery percentage.
  - The list keeps its scroll position when you open an app and come back.
- Settings switch for the persistent notification, on by default. Turned off, the notification disappears from the shade and the monitor runs only while the app is open: history, reminders, the tile and the widget stop updating in the background (the reminders already sent stay). Turning it on again brings it back at once.
- Charge reminder (unplug at a chosen level) and low battery reminder, off by default.
- Settings: theme (system, light, dark, AMOLED), °C or °F, reminders, app protection (battery restrictions), device capacity, dual-cell battery, predictive back, update alerts and a manual check, reset recorded values, report a problem by email, source code, and About (developer, project, legal pages, license).
- New version notice: the app asks GitHub for the latest release (kept with its ETag), compares it number by number with the installed version and, if newer, shows a full-screen notice with the release notes. "Not now" lasts until the app is opened again; the bell snoozes for a week or a month or skips that version. Updates are never installed by the app: "Download" opens the browser. The developer's GitHub picture is shown in About, saved on the device and refreshed at most once a week. The app declares the internet permission only for these two requests.
- Quick Settings tile and home screen widget with the live current, status, level and power.
- Predictive back: the current page slides with your finger over the page below, always to the right; the on-screen back button plays the same slide-out. On by default, with a switch in Settings.
- Material 3 Expressive theme with dynamic color, light, dark and AMOLED (pure black surfaces, with selected items a little lighter so they stand out). The app's animations run at the app's own speed, regardless of the system animation scale. Pages draw under the system bars, titles scroll with the content and only a round back button stays fixed.
- Languages: English, Spanish and Portuguese (Brazil). The language follows Android: choose it in the system settings for the app (Android 13+) or the system language.
- Original adaptive launcher icon (the electrical battery symbol) with a monochrome layer, also shown in About.
- Privacy policy and terms in `docs/` (GitHub Pages), linked in the README and in the app.
- Project structure (`core` / `data` / `features`), CI workflow and R8 release build without obfuscation.

### Known issues
- Not tested on a device yet beyond the first screens; the new home, sheets, reminders, tile and widget are untested.
- What the charger offers, the device capacity from the system and the cycle count depend on the device and may not appear.
- The per-app list for a single hour can show less time than the real one for apps that were already open when the hour began.
