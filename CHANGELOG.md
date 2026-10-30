# Changelog

All notable changes to **F8scape** are documented in this file.

## [1.0.0] - 2026-10-30

### Added
- **App Experience & First-Open Setup:**
  - Bespoke 2-step **First-Open Setup** screen guiding users through the required Accessibility Service and Appear on Top permissions.
  - **App Home Hub** with live status indicator, quick-access optional module cards (Music & Media Controls, Calendar Events, Weather & Battery with `°C`/`°F`/System Default picker, Active Clock & Calendar Styles), and a floating dark bottom navigation rail (`Home`, `Settings`, `About`).
  - **About Screen** featuring the custom F8scape emblem, version badge, and plain-English overview.
  - Moved **Hardware & Posture Diagnostics** into Settings for testing and troubleshooting.
- **Four Swipeable StandBy Views:**
  - **Split Dashboard:** Custom Clock on the left + smart right stack (Media player when active, or Calendar + Weather + Battery cards).
  - **Full-Screen Clock:** Edge-to-edge display with 6 styles (*Default*, *Fluid Gradient*, *Split Flip*, *Neon Outline*, *Analog Chrono*, and *Retro LED* with `AM`/`PM` hardware indicators), 8 curated accent colours, *Highlights* vs *Full Colour* modes, and automatic 12h/24h system format detection.
  - **Full-Screen Calendar:** 5 layouts (*Default*, *Agenda Split*, *Minimal Dot*, *Editorial Bold*, *Bento Boxed*), 8 accent colours, *Highlights* vs *Full Colour* modes, and lockscreen privacy masking.
  - **Full-Screen Media Player:** Album art backdrop, animated progress scrubber, and large transport controls.
- **Touch-and-Hold Live Studios:** Long-press the Clock or Calendar directly on the StandBy screen to customize styles, colours, and highlight modes in real time.
- **Battery, Thermal & Resilience Guardrails:**
  - `PowerProfileController` with **Ambient** and **Eco** profiles, plus automatic Eco switchover on Android Battery Saver or thermal throttling.
  - Configurable **Auto-Off Screen Timeout** (`Never`, `15 min`, `30 min`, `60 min`).
  - **Suppress on Phone Calls** guardrail to immediately yield the screen during incoming or active calls.
  - `BootRecoveryReceiver` to cleanly restore state after device reboots or app updates.
- **Security & Release Hardening:**
  - Enabled R8 code minification (`isMinifyEnabled = true`) and resource shrinking (`isShrinkResources = true`) with tailored ProGuard rules.
  - Added strict `network_security_config.xml` blocking cleartext traffic and whitelisting only `api.open-meteo.com` and `geocoding-api.open-meteo.com` over HTTPS.
  - Zero reflection, zero dynamic code loading, non-exported receivers, zero-snooping accessibility service configuration, and tapjacking protection (`filterTouchesWhenObscured = true`).
