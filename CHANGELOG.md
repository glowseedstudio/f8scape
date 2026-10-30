# Changelog

All notable changes to **F8scape** will be documented in this file.

## [0.1.0-milestone0] - 2026-10-30

### Added
- Milestone 0 diagnostic foundation for the standard Samsung Galaxy Z Fold 8 (passport form-factor).
- `HardwareProbe` collecting device identifiers, board, SDK version, and standard Fold 8 target verification.
- `ScreenStateRepository` & `CoverDisplayCoordinator` inspecting active physical/logical displays, resolution, refresh rates, and display ID rebinding.
- `SensorPostureSource` integrating public sensor checks (`TYPE_HINGE_ANGLE`, accelerometer, gravity) with `StablePoseDetector` for surface stillness vs hand movement.
- Deterministic `StandbyStateMachine` supporting states: `DISABLED`, `MONITORING`, `CANDIDATE_TENT`, `ACTIVATING`, `ACTIVE_DASHBOARD`, `SUSPENDED`, `EXITING`, `COOLDOWN`.
- One UI 9-inspired dark ambient Jetpack Compose theme (`F8scapeTheme`, pure OLED blacks, restrained typography).
- `DiagnosticsScreen` with live posture stream, display metrics, state machine indicators, and local redacted markdown exporter.
- `StandbyPreviewScreen` demonstrating ambient large clock, date, and native weather/battery card presentation.
- `SafeShizukuBridge` for optional, reversible shell checks without crashing when Shizuku is not running.
- Comprehensive unit tests for `StandbyStateMachine` and `StablePoseDetector`.
