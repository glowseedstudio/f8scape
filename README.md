# F8scape

> **Ambient Cover-Screen Standby for the standard Samsung Galaxy Z Fold 8.**  
> Pronounced *“Fatescape”*.

F8scape transforms the outer cover display of your Galaxy Z Fold 8 into a calm, dark, premium ambient dashboard when the phone is propped in **tent position**.

---

## 🎯 Target Hardware Scope

- **Primary Target:** Physical **standard Samsung Galaxy Z Fold 8** (the wider passport form-factor, **not** the Fold 8 Ultra).
- **OS Environment:** Android 17 / One UI 9.
- **Form Factor Principle:** Designed explicitly around the cover screen dimensions and aspect ratio of the standard Fold 8.

---

## ✨ Features (Milestone 0 Foundation)

- **Hardware & Sensor Probe:** Real-time diagnostics for `Build` identifiers, physical and logical `DisplayManager` transitions, and `SensorManager` capabilities.
- **Hinge & Stability Detection:** Pure deterministic state machine with continuous accelerometer/gravity stability calculation to distinguish resting tent placement from hand movement.
- **One UI 9 Aesthetic:** Subdued dark palette (pure OLED blacks, subtle elevation, restrained rounded corners, legible typography).
- **Zero Routine Tapping:** Built from the ground up for automatic cover screen entry upon a stable tent dwell (~1 second) and instant dismissal on pickup or fold changes.
- **Local Diagnostics:** In-memory diagnostic logs with full redaction and a one-click "Copy Report" button.
- **Privacy & Offline First:** Zero trackers, no Firebase, no ads, no telemetry, no cloud sync, no required accounts.

---

## 📱 Hardware Diagnostics Spike (Milestone 0)

Before assuming non-public Samsung sensor APIs or panel control behaviors, F8scape ships an on-device **Diagnostics** screen that tests:

1. Device hardware identification and confirmation of the standard Fold 8 passport form factor.
2. Active logical display mapping and whether default display ID rebinds during fold transitions.
3. Live hinge state reporting and stability confidence.
4. Capability matrix for accessibility overlay services and optional Shizuku shell hooks.

---

## 🏗️ Architecture & Stack

- **UI:** Jetpack Compose + Material 3 foundations (customized dark ambient theme).
- **Language & Runtime:** Kotlin Coroutines + `StateFlow`.
- **Fold Detection:** AndroidX Window `WindowInfoTracker` + Hardware `SensorManager` stability filtering.
- **State Machine:** Single deterministic `StandbyStateMachine` (`DISABLED`, `MONITORING`, `CANDIDATE_TENT`, `ACTIVATING`, `ACTIVE_DASHBOARD`, `SUSPENDED`, `EXITING`, `COOLDOWN`).
- **Distribution:** GitHub signed sideloadable APK; no Google Play dependency.

---

## 🚀 Building & Testing

```bash
# Ensure Java 17 is active
export JAVA_HOME=/path/to/java-17

# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test
```

---

## 📄 Privacy & Licensing

- See [PRIVACY.md](PRIVACY.md) for permission rationale and offline-first commitments.
- F8scape is an independent open-source project. Not affiliated with Samsung, Apple, or Meta.
