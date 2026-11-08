# Changelog

## [Unreleased]
### Added
- Project structure (`core` / `data` / `features`), CI workflow and R8 release build without obfuscation.
- Original adaptive launcher icon with a monochrome layer.
- Persistent foreground-service notification that keeps monitoring the charging system.
- Live readings: charging current, battery level, voltage, temperature and charge status.
- Material 3 Expressive theme with dynamic color.
- Usage access permission flow (groundwork for the foreground/background app ranking).
- Power (W) reading, shown with a sign (positive while a charger is connected, negative on battery).
- Charging estimate: % per hour at the current speed and the time and clock hour when the battery is full (based on the charge counter; charging slows near 100%, so real time is usually longer).
- Charging session history tab, stored only on the device: duration, level range, average and peak current, peak power, max temperature and average % per hour.

- Current is now shown negative while running on the battery (and in the notification, tile and widget).
- "Fastest charging" insights in the History tab: compares your sessions by power source and by time of day.
- Health tab: state of health and cycle count (Android 14+, when the device reports them), estimated capacity, condition and technology.
- Apps tab: ranking of foreground and background usage over 24 hours or 7 days (an estimate based on usage time, not exact mAh). Uses the special usage access permission.
- Quick Settings tile and home screen widget with the live current, status, level and power.
- Privacy policy and terms in `docs/` (for GitHub Pages), linked in the README and in the app.

### Fixed
- The status could lag by up to 30 s after plugging or unplugging the charger (it showed "Discharging" while charging). The monitor now reacts instantly to battery broadcasts and samples every 500 ms while the app is visible (2 s while charging with the screen off).

### Known issues
- Not built or tested on a device yet.
- The tile, widget, health, apps and insights views are new and untested on a device.
- Charging session charts are not implemented yet.
