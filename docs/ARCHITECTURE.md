# F8scape Architecture

This document outlines the architecture, component interaction, and design patterns powering F8scape.

---

## 1. Core Principles

1. **Deterministic Single Source of Truth:** All posture signals, power states, and user policies feed into a single `StandbyStateMachine`. UI observes an immutable `StandbyUiState`.
2. **Standard Fold 8 Hardware Targeting:** Specific attention to the passport outer screen aspect ratio; we never substitute Ultra measurements or assumptions.
3. **Layered Decoupling:** Hardware sensors, window metrics, and accessibility layers are abstracted behind `PostureSource` and `ScreenStateRepository` interfaces, allowing effortless fake injection in tests.
4. **Reversible & Safe:** No persistent hidden system mutations. Optional Shizuku shell overrides (if used in later milestones) are bounded, monitored, and restored immediately on exit.

---

## 2. Component Diagram

```
                 [ Hardware Sensors ]               [ WindowManager / FoldingFeature ]
                          │                                         │
                          ▼                                         ▼
               [ SensorPostureSource ]                    [ WindowPostureSource ]
                          │                                         │
                          └─────────────────┬───────────────────────┘
                                            ▼
                                   [ PostureData Stream ]
                                            │
                                            ▼
                                 [ StablePoseDetector ]
                                            │ (confidence > 0.70 & tent angle)
                                            ▼
                                 [ StandbyStateMachine ] ◄─── [ TriggerPolicy ]
                                            │
                       ┌────────────────────┴────────────────────┐
                       ▼                                         ▼
                [ State: ACTIVATING ]                     [ State: EXITING ]
                       │                                         │
                       ▼                                         ▼
               [ Cover Screen UI ]                        [ Cooldown & Cleanup ]
           (Activity / Overlay Host)
```

---

## 3. Posture & Stability Detection

A folded device propped in tent position is distinguished by:
1. **Hinge Opening:** ~60°–120° range. On devices where continuous hinge angles are not publicly exposed, public `FoldingFeature` (`HALF_OPENED`) or sensor discrete states are queried.
2. **Inverted-V Gravity Vector:** Gravity is distributed across the Y and Z axes (neither lying flat on back with $Z \approx 9.8\text{ m/s}^2$, nor purely vertical).
3. **Stationary Dwell:** Acceleration variance calculated over a sliding window must be below $0.35\text{ m/s}^2$ for 800–1500 ms before triggering `ACTIVATING`. This rejects hand handling and walking tremors.

---

## 4. Display Coordination

Galaxy Fold devices switch logical display IDs or turn off panel power when folding/unfolding. `ScreenStateRepository` and `CoverDisplayCoordinator`:
- Inspect active display resolutions at runtime.
- Check whether the outer cover display is currently lit (`Display.STATE_ON`).
- If One UI turns off the cover display in tent mode, F8scape immediately ceases drawing to prevent battery drain on an inactive panel.
