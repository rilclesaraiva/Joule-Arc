---
title: Privacy policy
permalink: /privacy
---

# Privacy policy

**Joule Arc** · Last updated: 2026-11-09

## Who is responsible

Joule Arc is developed and maintained independently by **rilclesaraiva**.
Contact: [saraivarilcle@gmail.com](mailto:saraivarilcle@gmail.com).

## Short version

Joule Arc works on your device. The only thing it does over the internet is ask GitHub two
things: whether a newer version was published, and the developer's profile picture. It sends
nothing about you or your battery. The app has no analytics, advertising or tracking libraries,
and no account. The developer does not receive any data from the app.

## What the app reads, and why

| Data | Why | Where it goes |
|------|-----|---------------|
| Battery readings: level, current, voltage, temperature, status, power source, charge counter, condition, technology, and when the device reports them the cycle count and what the charger offers | Show live values, ratings and estimates of speed, time and capacity | Shown on screen and in the notification, tile and widget. Not stored, except as summarized in the history below |
| Charging session history: start time, duration, battery level at start and end, average and peak current, peak power, maximum temperature, power source | Show the history and compare what charges faster | Stored in a file in the app's private storage on your device (`sessions.json`). Not sent anywhere |
| Battery level history: time, battery percentage, whether a charger was connected and whether the screen was on, one entry when the level or charger state changes or every 10 minutes, for the last 14 days | Draw the battery level and daily usage charts, compute consumption with the screen on and off, and split the drained percentage between apps | Stored in a file in the app's private storage on your device (`levels.json`). Older entries are deleted automatically. Not sent anywhere |
| Device capacity: read from the system (power profile, system resources or a kernel file, when readable) when available, or typed by you | Estimate battery health and capacity lost to wear | The typed value is stored on your device in the app's private storage. Not sent anywhere |
| Settings and recorded values: reminders and their levels, theme, temperature unit, dual-cell option, predictive back option, persistent notification option, highest and lowest current recorded | Remember your choices | Stored on your device in the app's private storage. Not sent anywhere |
| Device facts: Android version, API level, security update date, manufacturer, model and device code | Show them in the Device sheet, and fill in a problem report | Shown on screen. Only the app version, Android version, manufacturer and model are included in an email, and only if you choose "Report a problem" and send the draft yourself |
| Export file: only when you choose "Export data" in Settings. It holds the device details (manufacturer, brand, model, build, Android version, security update), the battery readings and level history (including the periods with no readings and whether the app is exempt from battery restrictions), your charging sessions, the consumption rates and, if you granted Usage access, the time each app was used (with the app names and package names) | Let you analyze a battery drain yourself | Written once, to the place you pick in Android's file picker. Joule Arc does not send it anywhere and does not keep a copy. If you share the file, you decide with whom: it shows which apps you use and when |
| Update check: the app asks `api.github.com` for the newest public release of the project (version, notes, download link) | Tell you when a newer version exists | The request carries no data about you. It identifies the app and its version (`JouleArc/<version>`), as every request does. GitHub, like any website, sees the request and your IP address. The last answer, its ETag, the time of the check and your choices (snooze, skipped version) are stored on your device |
| Developer picture: the app downloads `github.com/rilclesaraiva.png` the first time you open the About page, and looks for a new one at most once a week | Show the developer's photo in the About page | Same as above: no data about you is sent. The picture is stored in the app's private storage |
| App usage events (when each app is in the foreground or runs a foreground service) and the names and icons of the apps that have a launcher icon, only if you grant the special "Usage access" permission | Rank which apps use the most battery (an estimate) | Read on demand, turned into totals per app and shown on screen. Not stored and not sent anywhere |

## Permissions

- **Internet:** only for the two requests to GitHub described above. The app never sends your battery readings, history or settings.
- **Notifications:** show the persistent monitoring notification and, if you turn them on, the charge and low battery reminders.
- **Foreground service:** keep the monitor running in the background while the persistent notification is on. With it off in Settings, the monitor only runs while the app is open.
- **Receive boot completed:** restart the monitor after the device reboots.
- **Usage access (special permission):** read app usage time for the ranking. You grant it in system settings and can revoke it at any time.

## Problem reports

"Report a problem" opens your own email app with a draft addressed to the developer, including the app version, Android version, manufacturer and model. Nothing is sent unless you send that email. If you do, the developer receives what you sent and uses it only to answer and fix the problem.

## Sharing, retention and deletion

The app does not share data with third parties. The session history, the battery level history and your settings stay on your device until
you clear the app's storage in Android settings or uninstall the app, which deletes it. This includes the saved answer of the update check and the developer picture.
Automatic Android backup of the app's data is turned off.

## Links that leave the app

The links in the app to the project repository, the developer's GitHub profile, the changelog, the license, this policy and the terms open in your browser.
So does "Download" in the new version notice (it opens the release page on GitHub): the app never downloads or installs an update by
itself. Those sites (GitHub) have their own privacy policies.

## Your rights

Since the developer does not collect or receive personal data from the app, there is no
developer-held data to access, correct or delete. If you have questions about this policy
(including under the Brazilian LGPD or the European GDPR), contact the address above.

## Changes

If the app starts collecting or sharing anything new, this policy is updated in the same
release and the date above changes.
