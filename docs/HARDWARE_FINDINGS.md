# Standard Galaxy Z Fold 8 — Hardware Diagnostic Findings

**Device Target:** Standard Samsung Galaxy Z Fold 8 (Passport Form Factor)  
**Test Hardware Confirmed:** `SM-F971B` (`h8q`), Board `canoe`  
**OS Environment:** Android 17 (API 37) / One UI 9  
**Verification Date:** 30 October 2026  

---

## 1. Physical Device Verification

| Parameter | Standard Fold 8 Specification | Physical Device Measurement | Verification Status |
|---|---|---|---|
| `Build.MANUFACTURER` | `samsung` | `samsung` |  CONFIRMED |
| `Build.MODEL` | Standard Z Fold | `SM-F971B` |  CONFIRMED |
| Ultra Exclusion | Must NOT be Ultra (`SM-F968*`/`SM-F976*`) | Non-Ultra standard Fold 8 (`SM-F971B`) |  CONFIRMED |
| Device Codename | Fold 8 series | `h8q` |  CONFIRMED |
| Board | Next-gen SoC | `canoe` |  CONFIRMED |
| Android Platform | Android 17 | `17 (API 37)` |  CONFIRMED |

---

## 2. Display Geometry & Panel Behavior

| Property | Value | Notes |
|---|---|---|
| **Cover Screen Physical Resolution** | `1248 x 1972 px` | Passport aspect ratio (~1.58:1); notably wider than legacy Fold 5/6 (968x2376). |
| **Tent Landscape Active Area** | `1972 x 1248 px` | Perfectly accommodates side-by-side clock + cards with abundant negative space. |
| **Density** | `420 dpi` (physical) / `360 dpi` (override) | High-definition crisp rendering for ambient text and chips. |
| **Cover Display Power in Tent** | **REMAINS ON (`YES`)** | At ~90° tent position on desk, One UI **keeps the cover display active and lit**. |

> **Critical Finding:** Unlike community reports on the Fold 8 Ultra where panel cutoff occurred around ~83°–86°, the physical standard Fold 8 (`SM-F971B`) maintains an active, lit cover display at 90° tent posture.

---

## 3. Posture & Sensor Live Measurements

- **Inferred Posture:** `TENT_CANDIDATE`
- **Stability Score:** `99%` (detected stationary on flat surface via `StablePoseDetector` acceleration variance filter)
- **Hinge Angle Reporting:** `90.0°` (discrete stop reported via Samsung hinge sensor)
- **Standby State Machine:** Successfully transitioned `MONITORING` $\rightarrow$ `CANDIDATE_TENT` $\rightarrow$ `ACTIVE_DASHBOARD`.

---

## 4. Automation & Capability Summary

1. **Standard Mode (No Shizuku):** **VIABLE**. Because the cover display remains powered on in the 90° tent posture on `SM-F971B`, automatic cover-screen standby is fully achievable without requiring privileged shell overrides for baseline tent angles.
2. **Accessibility Overlay Mode:** **RECOMMENDED FOR AUTO-LAUNCH**. Provides a seamless, lawful window overlay context that does not collide with Android 14+ background activity launch restrictions.
3. **Standby UI Rendering:** Verified running live on device with pure OLED black surface, large ambient clock (`16:49`), date (`Friday, October 30`), and live weather & battery chips.
