# Privacy Policy — F8scape

**Last Updated:** October 2026

F8scape is built on strict local-only privacy principles.

## 1. Zero Tracking, Telemetry, or Analytics
- **No SDKs:** F8scape contains zero analytics SDKs (no Firebase, no Mixpanel, no Google Play Services dependencies).
- **No Ads:** F8scape is completely free of advertisements.
- **No Accounts:** No account, registration, or sign-in is ever required.
- **No Cloud Synchronization or Backups:** Your preferences and configurations are stored purely locally on your device via Android Jetpack DataStore (`android:allowBackup="false"`).

## 2. Permissions & Data Access Rationale
Every permission requested by F8scape serves an explicit, user-facing function:

| Permission | Purpose | Data Handling |
|---|---|---|
| `AccessibilityService` *(Required)* | Keeps the hinge-angle posture watcher active and renders the cover-screen StandBy overlay when the phone settles into tent mode. | F8scape **never** retrieves window text, notifications, passwords, or keystrokes (`canRetrieveWindowContent="false"`, `accessibilityEventTypes=""`). |
| `SYSTEM_ALERT_WINDOW` *(Required)* | Allows F8scape to draw the StandBy overlay over the cover screen in tent position. | Local UI presentation only. |
| `NotificationListenerService` *(Optional)* | Detects active media playback sessions to show song title, artist, album art, and transport controls. | Only standard `MediaController` session metadata is read. Personal notifications are never read, stored, or transmitted. |
| `READ_CALENDAR` *(Optional)* | Populates event dots and the next upcoming event on the Calendar card. | Read locally via `CalendarContract`; supports lockscreen privacy masking. Never leaves your device. |
| `INTERNET` *(Weather Only)* | Fetches current weather conditions for your configured city. | Restricted via `network_security_config.xml` exclusively to `https://api.open-meteo.com` and `https://geocoding-api.open-meteo.com` over TLS. |
| `RECEIVE_BOOT_COMPLETED` | Restores the standby posture watcher after a device reboot or app update. | Internal non-exported receiver (`android:exported="false"`). |

## 3. Diagnostic Logs
- Hardware & posture diagnostic logs (`F8Log`) are kept strictly **in-memory** in a bounded rolling buffer.
- Logs are never transmitted across the network.
- Diagnostic export strings automatically redact potential PII (emails, IP/MAC addresses).
