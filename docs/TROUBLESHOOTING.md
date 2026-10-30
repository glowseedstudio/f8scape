# F8scape — Troubleshooting Guide

## 1. Cover Screen Turns Off in Tent Angle
- **Issue:** On some Galaxy Fold builds, One UI automatically switches panel power from the outer cover screen to the inner folding display when opened past a certain angle.
- **Diagnostics Check:** Open F8scape Diagnostics, prop the phone in tent, and observe the "Cover Screen Active" field.
- **Resolution:** If One UI cuts power to the cover screen before your desired tent angle:
  1. Try a slightly narrower tent angle (e.g., 65°–75° instead of 85°).
  2. If supported, check the optional Shizuku Enhanced Mode in Settings to inspect device-state overrides.

## 2. Background Launch Restrictions (Android 14+)
- **Issue:** Android limits background activity launches from background services.
- **Diagnostics Check:** Check "Accessibility Service" in the Diagnostics screen.
- **Resolution:** Enable the F8scape Accessibility Service in Android Settings → Accessibility → Installed Apps. This provides a lawful window overlay context for automatic cover display without violating platform background launch restrictions.

## 3. Restricted Settings on Sideloaded APKs
- **Issue:** On Android 13+, sideloaded APKs may display "Restricted setting" when trying to enable Accessibility or Notification Listener permissions.
- **Resolution:**
  1. Open Android Settings → Apps → F8scape.
  2. Tap the three dots in the top-right corner.
  3. Select **Allow restricted settings**.
  4. Authenticate with your device PIN or fingerprint, then return to Accessibility settings to enable F8scape.
