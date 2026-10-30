# Standard Galaxy Z Fold 8 — Hardware Diagnostic Findings

**Device Target:** Standard Samsung Galaxy Z Fold 8 (Passport Form Factor)  
**Document Status:** Initial Milestone 0 Scaffold (Awaiting Physical Device Log Execution)  
**Baseline OS:** Android 17 / One UI 9  

---

## 1. Physical Device Verification

| Parameter | Standard Fold 8 Target | Test Device Measurement | Status |
|---|---|---|---|
| `Build.MANUFACTURER` | `samsung` | *(Pending test device run)* | ⏳ Pending |
| `Build.MODEL` | Standard Z Fold (e.g., `SM-F966*`) | *(Pending)* | ⏳ Pending |
| Ultra Exclusion | **Must NOT be Ultra** (`SM-F968*`) | *(Pending)* | ⏳ Pending |
| Form Factor | Passport (wider cover display) | *(Pending)* | ⏳ Pending |
| Android Version | Android 17 (API 35/36) | *(Pending)* | ⏳ Pending |

---

## 2. Hinge Sensor & Discrete Stops

- **Public Hinge Sensor (`Sensor.TYPE_HINGE_ANGLE`):**  
  *Finding:* Pending verification. On Samsung devices, third-party tests often observe discrete reports (0°, 90°, 180°) rather than continuous single-degree steps.
- **Private Sensor HALs (`folding_angle`, `lid_angle_fusion`):**  
  *Finding:* Not accessible via standard Android runtime permissions.
- **AndroidX WindowManager `FoldingFeature`:**  
  *Finding:* Accurately delivers `FLAT` and `HALF_OPENED` while the activity window is active.

---

## 3. Display Switching & Panel Control

- **Cover Screen Resolution & Density:** *(To be recorded from Diagnostics report)*
- **Display ID Rebinding:** Does logical display ID change when folding from flat to tent? *(To be recorded)*
- **Cover Display Power Cutoff Angle:**  
  *Note:* Galaxy Fold 8 Ultra community tests reported a switch around 83–86°. The standard Fold 8 passport form factor must be empirically tested.

---

## 4. Automatic Activation Feasibility

| Mode | Feasibility | Requirements / Notes |
|---|---|---|
| **Standard Mode (No Shizuku)** | *Candidate* | Requires cover panel to remain lit in desired tent angle and valid activity/overlay permission. |
| **Accessibility Overlay Mode** | *Candidate* | Requires user-granted Accessibility Service; avoids background `startActivity` block. |
| **Enhanced Mode (Shizuku)** | *Fallback* | Used only if One UI shuts off cover panel before tent angle is reached. |
| **Lock-Screen Automatic Launch** | *Stretch Goal* | Subject to keyguard BAL restrictions. Secure PIN/Biometrics must remain guarded. |

---

*(Run F8scape on the target Fold 8, tap "Copy Report" in the Diagnostics screen, and paste the output here to update this record).*
