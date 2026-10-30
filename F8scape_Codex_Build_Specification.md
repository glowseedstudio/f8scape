# F8scape — Complete Codex Build Specification

**Version:** 1.0 product brief · **Prepared:** 30 October 2026  
**Product:** F8scape (pronounced “Fatescape”)  
**First hardware target:** **standard Samsung Galaxy Z Fold 8**, Android 17 / One UI 9 as installed on the test device; verify the exact model, build and displays at runtime. **Do not substitute the Fold 8 Ultra's measurements for the standard Fold 8.**  
**Platform:** Native Android only · **Distribution:** GitHub source + signed sideloadable APK releases; no Google Play dependency.  
**Identity:** Fully standalone, **no Vantafyn integration**, no Samsung/Apple branding or copied assets, no “Glow” or Glowseed branding in the app name/visual identity.

> **Instructions to the implementing AI (Codex):** Treat this file as the product and engineering source of truth. Produce a complete, maintainable Kotlin/Jetpack Compose Android project, not just mockups. Work incrementally, with compilable milestones, explicit diagnostics and a hardware-test matrix. Begin by implementing and shipping a diagnostic spike for hinge state, display switching and background launch behavior on the *standard* Fold 8. Only claim a mode works after a physical device test. Continue through the roadmap without inventing privileged API access or bypassing Android security boundaries. Prioritize automatic, reliable cover-screen activation; beautiful UI is the second gate. Document anything that genuinely requires a device test or optional Shizuku authorization. Do not silently substitute manual activation for the core automation requirement.

---

## 1. Product vision and non-negotiables

F8scape turns the Fold 8's **outer/cover screen** into a calm, dark, premium ambient dashboard when the phone is **propped in tent position**. It should feel like an overlooked native feature of One UI 9 without imitating copyrighted design assets. Users should place the phone in tent position and have the dashboard appear **automatically**, without launching the app each time; exiting tent mode should remove the dashboard and restore normal behavior.

Priorities, in order:

1. **Automatic activation and dismissal** in the intended tent posture, with no routine tapping. Do not misrepresent Android background-launch or display-switching restrictions.
2. **Cover screen only for v1.** No inner-screen dashboard in the initial release. An inner-screen/tabletop mode is an explicitly deferred enhancement.
3. **Native-feeling One UI 9-inspired quality:** restrained surfaces, thoughtful rounded corners, balanced spacing, excellent typography, consistent interactions, 60/120 Hz-friendly transitions when visible. No Samsung names, logos, borrowed assets, or exact copies of proprietary UI.
4. **Low-power ambient design:** dim OLED-friendly presentation, largely black surfaces, minimal idle work, optional ambient animations, automatic eco settings, burn-in mitigation.
5. **Context-aware media display:** use active Android media sessions, show artwork and controls when music is playing, return to default layout when playback ends.
6. **Customizable but curated dashboard:** a snap-to-grid editor for native cards, not an unrestricted launcher. No page-indicator dots or clutter.
7. **Offline-first and private:** zero ads, telemetry, analytics SDKs, trackers or account requirement. Network only for explicitly enabled features (e.g., optional weather/update checks).
8. Fully independent product and codebase from Vantafyn and all previous apps.

**Success scene:** User is listening to music, sets the Fold 8 in a stable tent pose with cover panel facing them, the cover display shows the dark F8scape dashboard, subdued artwork gently animates if enabled, system media controls appear, and it dismisses cleanly on pickup/unfold/close. When no media is playing, the user's chosen clock/weather/calendar/battery layout appears instead.

---

## 2. Critical engineering reality: do not build on assumptions

### 2.1 Hinge and posture

Public-facing Samsung Fold hinge readings have been reported by third-party developers as effectively **0° / 90° / 180° stops**, not smooth continuous degrees. Samsung's private sensors (described as `folding_angle` and `lid_angle_fusion`) are not ordinary app-facing sensors; they cannot be treated as publicly accessible merely by requesting an Android runtime permission. A developer project documents obtaining **selected sensor HAL log entries with an expressly authorized Shizuku shell helper**. This is a device/build-dependent technique, not an Android SDK guarantee.

AndroidX WindowManager `FoldingFeature` exposes `FLAT` and `HALF_OPENED` geometry for the **active application window**, not a universal always-on sensor that magically tracks the hinge while any app is in the background. It also does not guarantee a distinct `TENT` state. A developer-reported hidden device-state stream may include `CLOSED`, `TENT`, `HALF_OPENED`, `OPENED` on some Galaxy builds; its reachability, mapping and stability must be proven on the target handset. Do not blindly ship reflection into hidden APIs.

### 2.2 The biggest unknown: cover display while part-folded

Galaxy devices may switch the active panel from cover to inner as the hinge opens. A **Galaxy Z Fold 8 Ultra** community measurement reports a swap around 83–86°; **that is not a verified threshold for the standard Fold 8**. If One UI turns off the cover screen in the user's preferred tent angle range, no ordinary app activity can render on the powered-off panel. The Shizuku-assisted device-state base-override technique described by a community fork may help keep or switch the cover display on in some circumstances, but it can be fragile or unavailable. This is an **experimental opt-in**, not a silently required background hack.

### 2.3 Background launches and lock screen

Recent Android versions restrict background `startActivity` calls. An accessibility service may be able to host an accessibility overlay when enabled, or a visible/otherwise permitted app may be allowed to launch UI, but **never assume arbitrary activity launching will work from every background state**. `setShowWhenLocked()` and `setTurnScreenOn()` only govern activity behavior *once the activity can lawfully be shown*; they do not grant unrestricted background launch or permanent Always On Display replacement.

**Locked/screen-off automatic behavior is a stretch acceptance target subject to verification**, not a guaranteed v1 feature. Display that fact honestly in onboarding and README. Never bypass the device's secure keyguard. Protect calendar/notification details while locked.

### 2.4 Activation definition

**MVP success is not “opening F8scape manually.”** MVP must demonstrate automatic cover-screen entry and exit for at least the supported unlocked/cover-active tent configuration on the physical standard Fold 8. If blocked by OS panel control, build a transparent experimental Shizuku path and safe fallback, report the blocker and capture reproducible device logs. Do not label a manual-only prototype “feature complete.”

---

## 3. First task for Codex: a diagnostic APK before the pretty app

Implement a clean, installable **Diagnostics** screen within the project; allow it to be disabled/hidden for normal users after testing. Logs must be **local and user-exported only** (a “Copy diagnostic report” button, no automatic transmission). Never record unrelated logcat data.

Track with timestamps and document:

- `Build.MANUFACTURER`, `MODEL`, `DEVICE`, SDK version, One UI/build fingerprint (avoid unnecessary identifiers), UI density, refresh rate, screen dimensions and orientation.
- `DisplayManager` listing, IDs/flags, which logical display hosts F8scape, whether default display ID rebinds during fold transitions; never assume one physical screen = permanent display ID.
- `SensorManager` public sensor list for hinge/fold/orientation; sensor names, vendor, range and resolution, plus **only** sampled hinge/accelerometer/gravity values with timestamps.
- WindowManager `WindowInfoTracker` / `FoldingFeature` values while app foregrounded: `FLAT` or `HALF_OPENED`, orientation, hinge bounds.
- Whether a public device-state callback is accessible with the installed SDK; whether any candidate `TENT` state can be observed using supported APIs. If exploring hidden APIs, mark this as experimental and do not make it a mandatory feature.
- Acceleration/gravity posture inference to distinguish stable surface placement from hand movement, with false-positive testing.
- State when screen turns on/off, device locks/unlocks, app foreground/background, battery saver/charging status.
- Whether system panel changes from cover to inner at given physical hinge positions, and whether those events reach service observers in the background.
- Whether an enabled `AccessibilityService` can add/remove its own cover-screen overlay after an eligible posture change; whether the window permits direct touch media controls; whether the UI appears after screen-off/locked transitions.
- If explicitly opted in, Shizuku connectivity, authorization state, candidate sensor state/log adapter and supported/unavailable display state commands. **No automatic changes to device-state overrides in the diagnostic build** without an explicit reversible test action.

**Diagnostic acceptance report:** Document reliable triggers; panel-switch threshold/range; successful auto-entry contexts; failed contexts; Shizuku necessity; locked-screen limitations; battery/perf observations. Use the result to select the supported activation implementation, rather than trying to make the UI guess.

---

## 4. Architecture and Android stack

Use Kotlin, Jetpack Compose, Material 3 foundations (customized), Kotlin Coroutines + `StateFlow`, lifecycle-aware collection, navigation, DataStore Preferences (or Proto DataStore for structured settings), and optional Room only if needed for complex widget layouts/history. Follow modern Android Gradle Plugin and library versions that are actually available/compatible when Codex builds. Use minSdk appropriate to target Fold 8; target recent Android SDK without cheating by lowering target to evade restrictions. Prefer a small single `:app` module with clear packages until modularization creates real value.

Recommended structure:

```text
app/src/main/java/<package>/
  app/                 Application, entry Activity, navigation, theme
  onboarding/          Device compatibility, access/setup wizard
  diagnostics/         Hardware probe + local report
  fold/                PostureEvent, SensorPostureSource, FoldStateRepository
  automation/          StandbyStateMachine, TriggerPolicy, StablePoseDetector
  display/             ScreenStateRepository, CoverDisplayCoordinator
  service/             Optional user-enabled AccessibilityService/overlay host
  shizuku/             OptionalShellBridge, bounded capability checks, reset
  dashboard/           StandbyScreen, LayoutEngine, grid editor, presets
  cards/               Clock, Date, Weather, Calendar, Battery, Media
  media/               NotificationListenerService, MediaSessionRepository
  ambient/             ArtworkProcessor, KenBurnsEffect, AmbientProfile
  power/               BrightnessController, IdlePolicy, BurnInProtection
  settings/            Preferences, profiles, card visibility, permissions
  data/                DataStore models, layout persistence
  util/                Time, compatibility, sealed results, logger
app/src/test/           State machine, policy, layout and model tests
app/src/androidTest/    Compose interactions and device behavior tests
.github/workflows/      CI for build/lint/test, release workflow
```

Core patterns:

- A **single immutable `StandbyUiState`** drives Compose.
- Fold posture, display state, media activity, power, lock state, and user preferences are independent inputs into a **single deterministic `StandbyStateMachine`**.
- Keep platform and Shizuku operations behind interfaces; use dependency injection manually or Hilt if warranted, and inject fakes in tests.
- All observers are registered/unregistered correctly on lifecycle transitions; no spin loops/polling while inactive.
- `DashboardActivity` or overlay host contains Compose; `SettingsActivity`/main screen contains onboarding, live preview and controls.
- Access to system services and sensitive permissions must fail gracefully with explanatory UI. No invisible escalation of access.

### 4.1 Activation source abstraction

Create a `PostureSource` abstraction with progressively available implementations:

- `PublicWindowPostureSource`: AndroidX WindowManager and ordinary sensors, strongest in foreground.
- `SystemEventPostureSource`: OS events/device states *only where publicly accessible and verified*; may require pairing with accessibility service to remain alive.
- `AccessibilityPostureSource`: explicitly enabled service observing suitable state and managing accessibility overlay; do not assume access to a finer hinge angle than public sensors provide.
- `OptionalShizukuPostureSource`: separately consented, transparent shell bridge for narrowly scoped hinge state/cover-display behavior **only if diagnostic evidence says necessary and it works on the standard Fold 8**.

Do not represent `HALF_OPENED` as equivalent to *tent upright on a table*. Combine observed hinge/posture state with gravity/accelerometer **and a stability dwell time**, charging policy, orientation, and whether cover screen is actually active. Use debouncing and hysteresis rather than perfect angle cutoffs.

### 4.2 State machine

Suggested states:

```text
DISABLED
MONITORING
CANDIDATE_TENT (collect stable evidence for 800–1500 ms)
ACTIVATING
ACTIVE_DASHBOARD
SUSPENDED (power/lock/display restriction or permission loss)
EXITING
COOLDOWN (avoid bouncing in/out)
```

Inputs include fold/posture confidence, physical stability, cover visible/on, charging status, device interactive/locked, playback state, foreground apps/critical system UI when observable, battery saver, overrides and user enable/disable. Side effects are isolated, idempotent and cancellable. A stale delayed event must not revive an overlay after the user closes it. On service crash, restart and reboot, clean up overlays and recover any experimental display override safely.

**Exit conditions:** fold fully closed or opened; device picked up or moved beyond stability thresholds; incompatible panel state; power button/lock policy; user dismissal; permission revoked; calls or other critical system UI (respect users' task priority); inactivity timeout. Provide an opt-in grace window so one accidental nudge does not cause flicker.

**Manual controls exist for testing and fallback**, but are never presented as a substitute for planned automatic activation.

---

## 5. Experience and visual design

### 5.1 Design language

Appearance is **One UI 9-compatible in feel, uniquely F8scape in identity**. Use slightly rounded cards, responsive touch targets, layered elevation only where useful, low-contrast ambient borders, elegant text hierarchy and restrained glass/frost. Use system font stack or licensed/open fonts, not extracted Samsung fonts. Respect display cutouts, corner radii, system bars and landscape/portrait physical placement. Design for the **actual cover-screen bounds** read at runtime, rather than hardcoding screenshots of a Fold 8 Ultra.

Visual rules:

- Dark-first palette: nearly black surfaces, muted neutrals, limited accents derived from media artwork or user selection.
- No neon, aggressive gradients, garish glow, cyan/magenta pairing, excessive blur, animated decorative borders, UI clutter or page dots.
- Ambient animations feel slow and subtle; respect Android animator scale / Reduce Motion where accessible and offer an in-app disable switch.
- Controls are legible in a dim room with adequate contrast, even when screen brightness is low.
- Press states, icon sizing, slider behavior and dialogs feel cohesive with Android/One UI patterns, but avoid proprietary copies.
- Offline app icon and wordmark original; package/repo name `f8scape` (confirm name/trademark separately before public release).

### 5.2 StandBy layouts

Default when **nothing is playing**: visually large clock/date plus compact native cards (weather, battery and calendar). Maintain ample negative space. Use landscape layout when it suits the actual tent orientation, but test whether the Fold 8's cover screen will allow/preserve that rotation. Fallback gracefully to portrait without clipping.

Presets:

- **Minimal** — large clock, date and little else.
- **Dashboard** — clock alongside weather, upcoming event, battery.
- **Media** — large artwork and compact music transport controls, clock remains visible.

Layout composition must adapt to available width and height, font scale, orientation and accessibility settings. Prefer true composables and responsive dimensions, not pixel-perfect image overlays.

### 5.3 Snap-to-grid editor

A user enters the editor with a deliberate long press or a separate Settings > Customize action (not accidental touches in bedside mode). Native cards can be added, removed, moved/reordered and switched among predefined size spans. Layout engine prevents overlap and out-of-bounds coordinates; grid guides appear **only in edit mode**, with a clear Done/Reset option and undo for changes. Save and restore each preset arrangement locally. Include preview/editor in standard app mode so editing does not require physical tent placement.

Keep v1's cards **native** to F8scape for consistent design, permissions and power. Other apps' Android widgets via `AppWidgetHost` are optional **post-v1** and require user-authorized widget binding; external widget styling cannot be guaranteed.

### 5.4 Built-in cards and data contracts

| Card | Default capability | Data/access requirement |
|---|---|---|
| Clock & date | 12/24h system preference, timezone, multiple restrained faces, optional seconds (off by default) | No permission; efficient minute-tick scheduling |
| Weather | Current conditions, temperature, icon, short text, optional forecast | Opt-in network/location or manual city; cache; offline stale indication |
| Battery | Phone percent, charging state, time remaining **only if reliable** | BatteryManager / sticky battery broadcast; no constant polling |
| Calendar | Next event with privacy hide while locked | Optional READ_CALENDAR runtime permission and local CalendarContract; no account access |
| Media | Artwork, title, artist, playback position/transport | User-enabled NotificationListenerService, compatible active media sessions |
| Blank/Spacer | Structured negative space for user customization | None |

Do not add notifications, phone messages or personal content as default cards; any future access to them must be opt-in with a privacy explanation.

---

## 6. Music experience and media controls

When compatible audio is playing, resolve active sessions via `MediaSessionManager.getActiveSessions()` using a **user-enabled `NotificationListenerService`**. It is not correct to assume a normal app can hold the signature-level `MEDIA_CONTENT_CONTROL` permission. Listen for metadata/playback updates; support multiple sessions with a stable policy (playback-active > last user-selected > last active), and let the user choose another when needed.

Show app-provided media metadata/artwork (if present), song/artist, play/pause, previous/next and seek **only if supported by that session's available actions**. Use the `MediaController.TransportControls` and session playback state. Adapt when artwork is missing or an app declines controls; do not claim a third-party system control skin is being embedded. The visual language may *feel* like One UI media controls, but all Compose media control UI and assets belong to F8scape. Correctly handle Spotify, local media apps and Vantafyn media sessions if they publish standard Android sessions, without a direct Vantafyn integration.

**Automatic layout:** media-active → tasteful media-biased layout (user may opt out); media stopped/idle → transition back to chosen default grid. Avoid jumping if a music app briefly buffers; debounce state changes. If media is paused, choose a user-configurable short grace period before returning to dashboard.

Privacy: do not store playback titles/history or send session data off-device. While locked, default to hiding sensitive metadata/artwork unless the user explicitly chooses otherwise; never show secure app content via screenshots.

---

## 7. Album art ambient backgrounds: static or Ken Burns

The user specifically wants a **Ken Burns-style animated background** during music, but **off by choice**, without high battery cost.

- On media artwork change, asynchronously decode/downsample artwork **once** to a bounded surface size; cancel stale requests and release superseded bitmaps.
- Generate a blurred/dimmed image where feasible once, not a live CPU blur each frame. Compose graphics transforms or Android RenderEffect/GPU-backed render as appropriate; no extra video player, audio visualization capture or continuous screenshot service.
- Set image fill, substantial dark scrim and muted colors to protect readability.
- Motion: **60–90 seconds** for a slow modest scale (e.g. ~1.00→1.05) and gentle translation. Continuous but visually almost imperceptible; avoid flashing, large parallax and jarring direction reversal.
- Settings: Off/Static/Animated (default can be Static pending battery measurements), motion intensity Low/Normal, blur, darkness, palette follow-art toggle.
- If the user selects **Static**, there must be **no animation clock** ticking for that layer. Prefer no Ken Burns on battery saver, low battery, thermal stress, reduced motion, screen off, nonvisible display or eco mode.
- Do not claim GPU work is zero cost; battery test on real hardware.
- Separate optional very subtle background treatment for no-media dashboard; default plain/dark.

---

## 8. Battery, OLED and durability

Create two straightforward power profiles:

- **Ambient:** dim screen; user-optional Ken Burns; limited accent use; clock refresh only when needed.
- **Eco:** static/dark background, disabled nonessential animations, conservative refresh, optional screen timeout, fewer network updates.

Default behavior should be conservative. If Android Battery Saver activates, apply Eco while active unless user explicitly requests otherwise where safe. Provide a brightness setting that controls the **F8scape window's own brightness** when supported (`WindowManager.LayoutParams.screenBrightness`), restoring normal system behavior on exit. Do **not** silently change global brightness or disable system protection. Do not fake system-level Always On Display. Keep screen awake with `FLAG_KEEP_SCREEN_ON` only while a valid, user-configured active dashboard is visible, and respect battery/timeout policy.

OLED measures:

- Predominantly true/near-black areas, no high-brightness full-screen whites, soft accents.
- Dim clock and controls, optional micro-offset of text/card positions at a slow interval without breaking alignment. Movement reduces but **cannot eliminate** burn-in risk.
- Allow auto-off after 15/30/60 min, on battery but not while charging, and overnight schedule options.
- No per-frame Compose state updates for idle cards; clock tick on minute boundary, seconds off by default.
- Lifecycle releases image animations, jobs, sensor listeners, location/weather refresh, media callbacks and overlays when inactive.
- Avoid wake locks and always-running foreground services unless justified by an explicit Android-approved use case; do not misuse FGS types.
- Battery measurements must compare F8scape on/off at identical brightness/charging/network states, and record actual measured drain rather than claiming a fixed percentage improvement.

---

## 9. Automatic cover-screen operation: supported modes

**Primary feature:** user enables Auto StandBy once; F8scape monitors the supported fold/tent signals. Hold the phone stable in a tent for a short dwell (default ~1 second), then show the dashboard on the **lit cover panel**. Exiting the posture dismisses it.

Implement modes as *capability-based*, not marketing labels:

**A. Standard mode (no Shizuku):** Public Android fold/orientation/window sources and, *only when specifically enabled and useful*, an accessibility service or accessible overlay. No privileged screen override. Should work wherever reliable cover-visible tent indication and permitted display window are empirically available. If the OS powers off the cover display, show an explicit capability limitation; don't claim automatic cover-screen activation in that posture.

**B. Enhanced mode (optional Shizuku):** After explaining why needed and getting authorization, investigate a minimal shell-side bridge for the specific posture/cover-screen need. Avoid collecting full system logs; parse only sensor-relevant tags if necessary. Any device state override must be bounded, reversible, never silently applied on boot, and tracked so it can be reset after failure, service stop, or lost connectivity. Include a visible **Restore default display behavior** action. Do not perform persistent settings writes or force unsafe states. The user must be able to use the app without Shizuku to the extent the public capabilities permit.

**C. Manual preview:** A normal activity, test overlay and home shortcut for preview/troubleshooting, clearly distinguished from auto.

User-configurable activation policies: Automatic on stable tent (default goal), only while charging (optional), battery threshold, screen-on only if locked behavior unsupported, auto-dim amount, activation dwell, suppress on calls, quick disable.

**Safety checks:** If cover turns off, immediately hide and stop drawing; don't keep inaccessible media controls active on an off panel. When the device is secure/locked, do not manipulate screen overrides based on information that bypasses keyguard. Never intercept lockscreen credentials or tap through other apps. Respect secure windows and system UI priority.

---

## 10. Permissions, onboarding and privacy

Onboarding should use plain-language steps and request only the access needed for selected features. The app must **function as a manual dark clock preview with no special permissions** even before automation setup; however onboarding makes clear that automatic mode is the product's main feature and identifies missing capabilities.

Suggested sequence:

1. Welcome: “F8scape — your Fold 8, at rest.” Preview minimal/dashboard/media.
2. Compatibility check: fold hardware and standard Fold 8 identity, OS, display capabilities. Nonmatching devices may try preview, but are not declared supported.
3. Automatic activation test: live posture diagnostics, guided tent demo, screen-selection results.
4. Only if required, explain/enable Accessibility service; be transparent about overlay control and its scope. Do not request accessibility solely to make a background activity run if another supported option exists. Allow revoke easily.
5. Only if chosen, explain/authorize Shizuku enhanced mode; warn that authorization/service may need restarting and that features depend on firmware.
6. Only when user enables media card: notification-listener access for active media sessions; clarify this permission grants broad notification access even though F8scape will only use media-session data.
7. Only when enabling calendar: READ_CALENDAR, with no permission needed otherwise.
8. Only when enabling weather: manual city or optional fine/coarse location permission; support no-location option. Weather source/API key choice must be documented; cache and let user disable network weather completely.
9. Configure brightness/profile/timeout and run live preview.

Do not request `QUERY_ALL_PACKAGES`, screen recording, SMS, contacts, root or broad storage permission without a clear, reviewed requirement. No analytics, telemetry, ad IDs, crash upload, remote account or Google SDK. No cloud sync. User settings stay local (DataStore), media artwork only caches transiently, logs redact unrelated/personal content. If internet exists, explain exactly what requests leave the device. Provide an in-app privacy explanation and README.

Some sideloaded Android packages can face restricted-settings safeguards for Accessibility or Notification Listener privileges. Provide clear lawful, user-driven troubleshooting guidance but never disable security settings programmatically.

---

## 11. Settings structure

Top-level sections, styled as first-class One UI-feeling settings (not copied screenshots):

- **StandBy:** Auto StandBy on/off; current capability status; tent dwell; charging-only; battery threshold; pickup exit; timeout; lock-screen behavior status.
- **Dashboard:** Minimal/Dashboard/Media presets; Customize; clocks; cards; date/time preferences; landscape layout preference.
- **Media:** Enable session controls; prefer media layout; source selection; transition back after pause; artwork privacy.
- **Appearance:** Dark palette; ambient accent choice; density/size; smooth but restrained transitions; respect reduce motion.
- **Backgrounds:** Off/Static/Animated Ken Burns; motion speed/intensity; artwork blur; darkness; adaptive coloring.
- **Power:** Ambient/Eco; brightness; battery-saver behavior; burn-in movement; auto-off.
- **Integrations:** Optional weather city/location; calendar; notification-listener status; Shizuku if supported.
- **Device & Diagnostics:** Screen info; available posture states; current service health; test auto StandBy; export redacted report; restore display defaults.
- **About:** F8scape build number, open-source licenses, GitHub repo URL, optional manual check for GitHub Releases updates, privacy policy, no tracking.

Persist settings atomically; restore after process death. Defaults need to be safe and sensible with minimal setup.

---

## 12. Implementation roadmap and deliverables

**Milestone 0 — Device viability / diagnostic build (REQUIRED FIRST)**

- Create Android project, CI skeleton and on-device Diagnostics screen.
- Verify posture signals, standard Fold 8 display behavior, background/overlay behavior, screen-off/lock limitations.
- Produce `docs/HARDWARE_FINDINGS.md` with results and a yes/no for each automatic mode.
- **Gate:** do not promise full automation until evidence supports it. If standard mode fails, investigate Shizuku with a separate consented prototype and reversible controls.

**Milestone 1 — Core automation and UI shell**

- State machine, observation repositories, safe accessibility overlay/activity strategy chosen from tested capabilities.
- Auto appear/dismiss in supported unlocked tent scenario, debouncing, pickup handling, manual preview.
- Minimal clock dashboard, dim OLED mode, basic setup, local settings and service-status diagnostics.
- **Gate:** hardware demonstration of auto entry/exit without touching icon.

**Milestone 2 — Premium customizable dashboard**

- One UI-inspired bespoke Compose styling; responsive cover display; clocks/date/battery.
- Preset layouts, native snap-to-grid add/move/resize, saved layout, polished editor, reduced-motion handling.
- Weather/manual city integration and optional calendar (only with permission).

**Milestone 3 — Media and ambient animation**

- Opt-in notification listener, MediaSessionManager integration, metadata/artwork/playback, transport actions, media layouts.
- Static album art plus optional prepared/dimmed/blurred Ken Burns GPU transforms; transition logic.
- Empty/error/media-session multiple-player handling.

**Milestone 4 — Battery, resilience and enhanced mode**

- Ambient/Eco profiles; auto-off, safe brightness, burn-in mitigation, thermal/battery saver handling.
- Shizuku enhanced path **only if needed and verified**; precise recovery/reset semantics.
- Boot/service restart, interruption, crash, permissions revocation handling; no orphaned overlay/override.

**Milestone 5 — QA and GitHub release**

- Unit and Compose tests; physical Fold 8 scenario matrix; battery/performance checks; accessibility review.
- Complete README, setup guide, privacy policy, license, code of conduct optional, `CHANGELOG.md`, GitHub Actions building test APK and tagged release artifact.
- Release an installable, correctly signed APK, its SHA-256 checksum, source and tag. Protect signing key; use GitHub Actions secrets if automating signed release. No Play publishing requirement.

**Post-v1 (not blockers for initial release):** inner-screen/tabletop layout, external `AppWidgetHost` widgets after permission/security review, more clock faces/themes, richer weather, optional explicit opt-in GitHub releases updater, compatibility with other foldables after testing.

---

## 13. Definition of done and test matrix

### Functional acceptance

- [ ] Physical **standard Galaxy Z Fold 8** confirmed in diagnostics; no extrapolation from an Ultra variant.
- [ ] On supported setup, user tents phone and **cover dashboard appears automatically** after stable dwell; no manual launch required.
- [ ] Dashboard exits cleanly when phone picked up, closed or unfolded; no stuck overlays or jitter.
- [ ] Unsupported states show honest warning and a manual preview, never silently malfunction or drain battery.
- [ ] Cover screen remains the target; inner screen never inadvertently gets an obscuring dashboard.
- [ ] Default dashboard is elegant and useful without network or special permissions.
- [ ] Native clock, date, battery and structured grid work; presets, long-press edit, move/resize/remove persist.
- [ ] Music session opt-in shows correct metadata, supports available controls, absent artwork fallback; stops return to normal layout.
- [ ] Ken Burns animation is gentle and *fully stops* when disabled or in Eco mode.
- [ ] All media/weather/calendar permissions can be refused or revoked; app degrades safely.
- [ ] Dimness, screen timeout, battery saver, orientation, static burn-in offset, and animation settings behave as documented.
- [ ] No tracking, Firebase, ads, unnecessary data collection or remote account.
- [ ] Settings survive process death/reboot. Experimental shell override is always cleaned/reset on relevant exits.
- [ ] Build, lint and unit tests pass; debug and GitHub release APK install successfully.

### Test matrix (record pass/fail with exact build)

- Cover screen unlocked + app foreground / background / process not resident.
- Closed → slightly tented → stable tent → half-open → unfolded → reverse.
- Different hinge positions and hinge speeds; debounce against casual hand opening.
- Tent placed upright, landscape tent, resting face down, held in hand; false-positive rejection.
- Charging vs battery, battery saver, low battery, thermal warning, screen timeout.
- Screen locked/unlocked, display sleeping/woken, secure PIN, face unlock; no keyguard bypass.
- Incoming call, alarm, immersive video, lockscreen, keyboard; system UI priority.
- Media sources: active music, pause, resume, seek, track change, no artwork, multiple sessions, app killed.
- Accessibility/notification/calendar permission revoked mid-session; Shizuku unavailable or stopped mid-override.
- Orientation rotation, One UI display size/font scaling, dark mode, reduced motion, 60/120 Hz refresh.
- 30–60 minutes screen-on idle drain per Eco/Ambient under matched conditions; compare baseline; note measurement methodology.
- Force-stop/reboot/stale callbacks/rapid repeated opening and closing; no phantom activation after user disables auto.

**Quality criteria:** No obvious dropped frames, accidental taps, blocking transitions, unnecessary network polling, hot device, lingering bright screen or irrecoverable system display state. Set measurable performance goals after profiling real hardware; avoid fake benchmarks.

---

## 14. Required files to commit to repository

```text
README.md                        — what F8scape is, hardware scope, setup, screenshots, known limits
LICENSE                          — project owner chooses compatible open-source license
PRIVACY.md                       — no tracking; every requested permission explained
CHANGELOG.md
CONTRIBUTING.md                   — optional but useful for GitHub-only releases
docs/ARCHITECTURE.md              — sources, state machine, media and overlay decisions
docs/HARDWARE_FINDINGS.md         — real standard Fold 8 measurements + test conclusions
docs/TEST_PLAN.md                 — complete physical matrix
docs/TROUBLESHOOTING.md           — accessibility, Shizuku, cover-screen limitation
app/...                           — runnable Kotlin/Compose source
.github/workflows/android-ci.yml  — compile/lint/test/release CI
```

### Repository documentation cautions

State that F8scape is **independent**, not affiliated with Samsung, Apple or Meta. Project name and trademarks require independent clearance before wider use; initial web-name checks are not legal assurance. If adapting code from another open-source project, check license and attribution before copying any implementation. For Duo Open references, prefer concepts and cite links rather than bringing in screenshot/shader or shell logic without understanding security/licensing.

---

## 15. References for Codex to verify against current SDK

These are implementation/research references, **not proof that a particular Fold 8 exposes the same behavior**:

1. Android official — AndroidX WindowManager foldables: https://developer.android.com/codelabs/android-window-manager-dual-screen-foldables
2. Android official — `FoldingFeature`: https://developer.android.com/reference/androidx/window/layout/FoldingFeature
3. Android official — Background activity launch/security: https://developer.android.com/guide/components/activities/secure-bal
4. Android official — Activity `setShowWhenLocked` / `setTurnScreenOn`: https://developer.android.com/reference/android/app/Activity
5. Android official — Accessibility overlay background: https://codelabs.developers.google.com/codelabs/developing-android-a11y-service
6. Android official — `MediaSessionManager`: https://developer.android.com/reference/android/media/session/MediaSessionManager
7. Android official — `AppWidgetHost`: https://developer.android.com/develop/ui/views/appwidgets/host
8. Android official — Keep screen on, power guidance: https://developer.android.com/develop/background-work/background-tasks/awake/screen-on
9. Community research — original Duo Open: https://github.com/marcoazeem/duo-open
10. Community fork with Galaxy findings — Duo Open for Galaxy: https://github.com/OrangeHubX/duo-open-for-galaxy (measurements on the **Z Fold 8 Ultra** as of October 2026; not validated for standard Fold 8).

**Before implementing Shizuku or any hidden/privileged system workaround:** independently inspect source, license, device/build compatibility, exactly which permission calls are made, security effects, and recovery path. Do not automatically ingest broad logcat or retain personal data.

---

## 16. First Codex task to execute verbatim

> Build the native Android project described in `F8scape_Codex_Build_Specification.md`. Read the entire specification before starting. Start with Milestone 0: produce a compilable diagnostic APK and working Compose onboarding/preview, capable of logging fold/posture/display transitions on the **standard Galaxy Z Fold 8**, as well as capability-tested automatic overlay/launch behavior, without assuming Shizuku or privileged access. Implement proper architecture, state models, DataStore, safe permission explanations and local exportable diagnostic report. Add tests and build instructions. Report exact files created, build result, device tests still required and any Android platform blockers. Then continue the remaining milestones in order, keeping each one compiling and tested. The defining requirement is automatic activation on the **outer screen in tent posture**; preserve that requirement, disclose hardware-dependent limitations, and never call a manual-only preview a completed StandBy app. Design should feel like a missing One UI 9 feature, original and understated, with no Apple/Samsung/Glowseed branding, plus native grid customization, media-aware cards, optional Ken Burns artwork, and stringent battery/privacy controls.

**End of specification.**
