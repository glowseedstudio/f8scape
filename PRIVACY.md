# Privacy Policy — F8scape

**Last Updated:** October 2026

F8scape is built on uncompromising privacy principles.

## 1. Zero Tracking, Telemetry, or Analytics
- **No SDKs:** F8scape contains zero analytics SDKs (no Firebase, no Mixpanel, no Google Play Services dependencies).
- **No Ads:** F8scape is completely free of advertisements.
- **No Accounts:** No account, registration, or sign-in is ever required.
- **No Cloud Synchronization:** Your preferences and configurations are stored purely locally on your device via Android Jetpack DataStore.

## 2. Permissions & Data Access Rationale
Every permission requested by F8scape serves an explicit, user-facing function:

| Permission | Purpose | Data Handling |
|---|---|---|
| `WAKE_LOCK` | Keeps the cover screen illuminated while the ambient dashboard is actively displayed. | Internal window flag; no data recorded. |
| `SYSTEM_ALERT_WINDOW` | Optional: allows testing cover screen window overlay capabilities. | Local UI presentation only. |
| `AccessibilityService` (Optional) | Optional: detects window changes and manages the cover screen standby overlay without manual launching. | F8scape **never** retrieves window text, passwords, or keystrokes (`canRetrieveWindowContent="false"`). |
| `NotificationListenerService` (Future Milestone) | Detects active media playback sessions to show song title, artist, and transport controls. | Only standard `MediaController` session metadata is read. Notifications are never stored or transmitted. |
| `READ_CALENDAR` (Future Milestone) | Optional next-event card on the dashboard. | Read locally via CalendarProvider; hidden on lockscreen unless permitted. Never leaves device. |

## 3. Diagnostic Logs
- Diagnostic logs (`F8Log`) are kept strictly **in-memory** in a rolling buffer.
- Logs are never transmitted across the network.
- Diagnostic export strings redact potential PII (emails, IP/MAC addresses).
