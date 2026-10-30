# F8scape — Physical Device Test Plan

This document details the scenario test matrix for verifying F8scape on the physical standard Galaxy Z Fold 8.

---

## 1. Posture & Placement Matrix

| Test ID | Scenario | Expected Outcome | Pass/Fail |
|---|---|---|---|
| **POS-01** | Device closed → opened to ~75° tent upright on desk | Enters `CANDIDATE_TENT`, counts dwell ~1s, activates cover dashboard | [ ] |
| **POS-02** | Device opened to ~75° tent while held in shaky hand | Stability score < 0.70; remains in `MONITORING`, does not activate | [ ] |
| **POS-03** | Device in active tent dashboard → picked up by user | Acceleration spike triggers exit to `COOLDOWN`; dashboard dismisses | [ ] |
| **POS-04** | Device in active tent dashboard → unfolded flat on desk | Posture becomes `FLAT`; dashboard dismisses cleanly | [ ] |
| **POS-05** | Device placed flat on back or face down | Posture rejected; no activation | [ ] |

---

## 2. Display & Power Matrix

| Test ID | Scenario | Expected Outcome | Pass/Fail |
|---|---|---|---|
| **DISP-01** | One UI shuts off cover panel | F8scape ceases rendering immediately; no battery drain | [ ] |
| **DISP-02** | Fast open/close flapping | Cooldown timer prevents screen bouncing/flapping | [ ] |
| **DISP-03** | Device plugged into charger | Respects "Only while charging" trigger policy if enabled | [ ] |
| **DISP-04** | Battery Saver enabled | Switches to Eco mode; disables non-essential animations | [ ] |

---

## 3. Security & Keyguard Matrix

| Test ID | Scenario | Expected Outcome | Pass/Fail |
|---|---|---|---|
| **SEC-01** | Tent placed while phone locked (PIN/Fingerprint) | Never bypasses keyguard; respects secure window flags | [ ] |
| **SEC-02** | Incoming phone call arrives during Standby | System call UI takes priority; dashboard suspends cleanly | [ ] |
