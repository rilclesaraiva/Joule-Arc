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
| Battery readings: level, current, voltage, temperature, status, power source, charge counter, health and cycle count when the device reports them | Show live values, estimate charging speed and battery capacity | Shown on screen and in the notification, tile and widget. Not stored, except as summarized in the history below |
| Charging session history: start time, duration, battery level at start and end, average and peak current, peak power, maximum temperature, power source | Show the history and compare what charges faster | Stored in a file in the app's private storage on your device (`sessions.json`). Not sent anywhere |
| App usage time (foreground time and foreground-service time per app), only if you grant the special "Usage access" permission | Rank which apps use the most battery (an estimate) | Read on demand and shown on screen. Not stored and not sent anywhere |

## Permissions

- **Notifications:** show the persistent monitoring notification.
- **Foreground service:** keep the monitor running.
- **Receive boot completed:** restart the monitor after the device reboots.
- **Usage access (special permission):** read app usage time for the ranking. You grant it in system settings and can revoke it at any time.

## Sharing, retention and deletion

The app does not share data with third parties. The session history stays on your device until
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
