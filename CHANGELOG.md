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

### Fixed
- The status could lag by up to 30 s after plugging or unplugging the charger (it showed "Discharging" while charging). The monitor now reacts instantly to battery broadcasts and samples every 500 ms while the app is visible (2 s while charging with the screen off).

### Known issues
- Not built or tested on a device yet.
- Charging history, fastest-charging windows, battery health estimate and the app ranking are not implemented yet.
