---
title: Privacy policy
permalink: /privacy
---

# Privacy policy

**Joule Arc** · Last updated: 2026-11-08

## Who is responsible

Joule Arc is developed and maintained independently by **rilclesaraiva**.
Contact: [saraivarilcle@gmail.com](mailto:saraivarilcle@gmail.com).

## Short version

Joule Arc works on your device. The app does not declare the internet permission, has no
analytics, advertising or tracking libraries, and no account. The developer does not receive
any data from the app.

## What the app reads, and why

| Data | Why | Where it goes |
|------|-----|---------------|
| Battery readings: level, current, voltage, temperature, status, power source, charge counter, condition, technology, and when the device reports them the cycle count and what the charger offers | Show live values, ratings and estimates of speed, time and capacity | Shown on screen and in the notification, tile and widget. Not stored, except as summarized in the history below |
| Charging session history: start time, duration, battery level at start and end, average and peak current, peak power, maximum temperature, power source | Show the history and compare what charges faster | Stored in a file in the app's private storage on your device (`sessions.json`). Not sent anywhere |
| Battery level history: time, battery percentage, whether a charger was connected and whether the screen was on, one entry when the level or charger state changes or every 10 minutes, for the last 14 days | Draw the battery level and daily usage charts, compute consumption with the screen on and off, and split the drained percentage between apps | Stored in a file in the app's private storage on your device (`levels.json`). Older entries are deleted automatically. Not sent anywhere |
| Design capacity: read from the system power profile when available, or typed by you | Estimate battery health and capacity lost to wear | The typed value is stored on your device in the app's private storage. Not sent anywhere |
| Settings and recorded values: reminders and their levels, theme, temperature unit, dual-cell option, highest and lowest current recorded | Remember your choices | Stored on your device in the app's private storage. Not sent anywhere |
| Device facts: Android version, manufacturer and model | Show them on the home screen and fill in a problem report | Shown on screen. Only included in an email if you choose "Report a problem" and send the draft yourself |
| App usage events (when each app is in the foreground or runs a foreground service), only if you grant the special "Usage access" permission | Rank which apps use the most battery (an estimate) | Read on demand, turned into totals per app and shown on screen. Not stored and not sent anywhere |

## Permissions

- **Notifications:** show the persistent monitoring notification and, if you turn them on, the charge and low battery reminders.
- **Foreground service:** keep the monitor running.
- **Receive boot completed:** restart the monitor after the device reboots.
- **Usage access (special permission):** read app usage time for the ranking. You grant it in system settings and can revoke it at any time.

## Problem reports

"Report a problem" opens your own email app with a draft addressed to the developer, including the app version, Android version, manufacturer and model. Nothing is sent unless you send that email. If you do, the developer receives what you sent and uses it only to answer and fix the problem.

## Sharing, retention and deletion

The app does not share data with third parties. The session history, the battery level history and your settings stay on your device until
you clear the app's storage in Android settings or uninstall the app, which deletes it.
Automatic Android backup of the app's data is turned off.

## Links that leave the app

The links in the app to the project repository, this policy and the terms open in your browser.
Those sites (GitHub) have their own privacy policies.

## Your rights

Since the developer does not collect or receive personal data from the app, there is no
developer-held data to access, correct or delete. If you have questions about this policy
(including under the Brazilian LGPD or the European GDPR), contact the address above.

## Changes

If the app starts collecting or sharing anything new, this policy is updated in the same
release and the date above changes.
