# Changelog

## [Unreleased]
### Changed
- Uso por app hides system apps by default (built-in apps that were never updated, and background parts with no launcher icon). The three-dot button at the top right opens a small menu over it to show or hide them. The percentages still count them, so the shown apps may add up to less than the total.
- The on-screen back button now plays the same slide-out as the back gesture.
- Device capacity detection has one more source: the system power profile read as the framework's XML resource through public calls, tried before the kernel node.
- Tapping an app in "Uso por app" opens its own page: total time, active and background time and the estimated battery share of each, with the 24 hours / 14 days switch.
- The Device section is now one tappable panel that opens a single sheet (model, manufacturer, Android version, API level, security update, device code, device capacity).
- The full-charge estimate no longer flickers: it is sampled only when the battery level changes and smoothed slowly.
- Predictive back always slides the page out to the right, back to where it came from, whichever edge you swipe from.
- The persistent notification shows the state ("Charging", "Discharging"...) as its title instead of the app name, with current, level and temperature below.
- Reading details now fit the situation: with no charger connected, the voltage, temperature and level texts no longer tell you to unplug or keep charging, and the power note explains discharge. Portuguese uses "Tensão" everywhere (no more "Voltagem").
- Predictive back now works like a page stack: the current page slides with your finger over the page below (with a shadow and rounded corners), and the swipe finishes without a second animation.
- "Design capacity" is now called "Device capacity" (Settings, capacity card and detail texts).
- Charging power: very fast and ultra fast (65 W and up) are now blue instead of green, matching the other readings, where blue means above the ideal range but not harmful.

### Added
- New app icon: the electrical battery symbol (alternating short and long plates, with minus and plus), as an adaptive icon with a monochrome layer, also shown in About.
- Predictive back gesture: while you swipe back, the page follows your finger (shrinks and rounds its corners). On by default; Settings has a switch. The switch controls the in-app animation; the system's own back-to-home preview follows Android.
- Project structure (`core` / `data` / `features`), CI workflow and R8 release build without obfuscation.
- Original adaptive launcher icon with a monochrome layer.
- Persistent foreground-service notification that keeps monitoring the charging system, restarted after reboot.
- Single home screen with the main readings, and separate pages for charging history, consumption, battery use by app and settings.
- Current gauge centered on zero (negative = out of the battery, positive = into it), with % per hour, time to full or time left, and the highest and lowest current recorded (with reset).
- Amber state and warning when a charger is connected but the battery is not charging or is losing charge (weak or faulty charger or cable, charging limit or protection). The current direction is learned per device from the sign it reports.
- Battery capacity card: charge now, full charge today (estimated), lost to wear and device capacity, with estimated health. The device capacity comes from the system power profile when available, or from the value you type in Settings.
- Readings grid with small gauges and plain-language ratings (for example Normal, Warm, Good, Fast, Heavy use): voltage, temperature, level, power, status, power source, health, technology and cycle count. Tap any reading for a detail sheet with a colored range, lowest and highest since the app was opened, what the charger offers (when the system reports it) and what the value means.
- Device section: Android version, manufacturer and model.
- Charging session history (stored only on the device), a comparison of the fastest and slowest power source and time of day, and a page for each session with its current and level curves.
- Consumption page: battery drain with the screen on and off (percent and estimated mAh per hour), estimated battery life at those rates, and the times of day that drain the most and the least.
- Battery use by app page inspired by Android's own: hourly level for 24 hours, daily usage for 14 days, last charge to 100%, and a per-app list with icons and an estimated percentage (Android does not let regular apps read per-app mAh).
- Charge reminder (unplug at a chosen level) and low battery reminder, off by default.
- Settings: reminders, app protection (battery restrictions), device capacity, dual-cell battery, °C or °F, reset recorded values, theme (system, light, dark), language (Android 13+), report a problem by email, source code.
- Quick Settings tile and home screen widget with the live current, status, level and power.
- Material 3 Expressive theme with dynamic color. The app's animations run at the app's own speed, regardless of the system animation scale.
- Privacy policy and terms in `docs/` (GitHub Pages), linked in the README and in the app.

### Added
- New version notice: the app asks GitHub for the latest release (kept with its ETag, so an unchanged answer costs nothing), compares it number by number with the installed version and, if newer, shows a full-screen notice with the release notes. "Not now" lasts until the app is opened again, the bell snoozes for a week or a month or skips that version, and Settings has a manual check and the alert options. It checks on every cold start and at most every 10 minutes when coming back. Updates are never installed by the app: "Download" opens the browser.
- The developer's GitHub picture in About, saved on the device and refreshed at most once a week (every 10 minutes while there is none).
- The app now declares the internet permission, used only for these two requests. Privacy policy and README updated.

### Fixed
- Pages now draw under the status bar instead of stopping below it; page titles sit to the side of the back button, so the button never covers them.
- Text on the home and pages was too dark in the dark and AMOLED themes (the pages lost their content color when the top bars were removed).

### Changed
- Reading details now fit the situation: with no charger connected, the voltage, temperature and level texts no longer tell you to unplug or keep charging, and the power note explains discharge. Portuguese uses "Tensão" everywhere (no more "Voltagem").
- The settings button on the home is now a floating round button, like the back buttons.
- "Battery use by app" is now "Usage by app", and page titles keep the same margin on both sides.
- No fixed top bars: page titles scroll with the content, and the home title and settings button sit at the start of the home. Only a round, slightly transparent back button stays over inner pages.
- Settings are now plain ordered lists with icons (no categories); About is the only heading and opens a page with the developer, project links, legal pages, license and an invitation to the repository. The home footer is gone.
- Reset buttons need two quick taps; the first tap shows a small hint.
- "In the battery now" shows whole mAh (for example 1.509).
- Settings has a fourth theme, AMOLED (pure black surfaces).
- The home no longer shifts while numbers change: tabular digits, one-line subtitles on the main card.
- The home remembers its scroll position when you come back from a page.
- The current gauge keeps its scale: it grows at once when needed but shrinks only after the value stays low for a while.
- The colored top bar is gone; the battery state now tints the main card and shows as a scalloped Expressive badge.
- The amber warning waits 45 seconds after the reading turns odd (new "Checking charge" state), so a charger that is still negotiating is not reported as a problem.
- The device capacity is detected by the app (system power profile, then kernel node when readable); the value typed in Settings still wins.

### Known issues
- Not tested on a device yet beyond the first screens; the new home, sheets, reminders, tile and widget are untested.
- What the charger offers, the device capacity from the system and the cycle count depend on the device and may not appear.
