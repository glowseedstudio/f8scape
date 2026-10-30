# F8scape

**A quiet, dark StandBy display built specifically for the Samsung Galaxy Z Fold 8.**  
*(Pronounced “Fatescape”)*

Prop your Galaxy Z Fold 8 on a desk or bedside table in **tent mode**, and F8scape turns the outer cover screen into a clean, glanceable ambient display. Pick the phone up, fold it closed, or open it flat, and it gets out of your way immediately.

No accounts. No subscriptions. No analytics or telemetry. Just a fast, native utility designed around the Fold 8's passport cover display (`1248 × 1972`).

---

## ✨ What It Does

### 1. Automatic Tent Mode Detection
F8scape combines the Fold 8's hardware hinge sensor (`55°–135°`) with accelerometer stillness checks. It waits until the phone is genuinely resting on a flat surface in tent posture before fading in—so it won't accidentally trigger while you're holding or adjusting the phone.

### 2. Four Swipeable StandBy Views
Swipe horizontally across the cover screen to move between four dedicated views (the bottom page indicator automatically fades out after 10 seconds of idle time so the screen stays completely clean):

- **Split Dashboard:** Your everyday desk view. Shows your customized Clock on the left and a smart stack on the right (automatically switching to Media Controls when music is playing, or your Calendar, Weather, and Battery cards).
- **Full-Screen Clock:** A large, room-legible timepiece that respects your phone's 12-hour or 24-hour time format. Choose from **6 styles**:
  - **Default** — Clean, balanced typographic display
  - **Fluid Gradient** — Soft multi-tone gradient numerals
  - **Split Flip** — Classic mechanical split-flap cards
  - **Neon Outline** — Spaced geometric stencil tubes with subtle glow
  - **Analog Chrono** — Oversized watch dial with sweeping hands (`Highlights` or `Full Colour`)
  - **Retro LED** — 7-segment digital matrix with unlit ghost segments and hardware `AM`/`PM` indicators
- **Full-Screen Calendar:** Full-month and upcoming schedule view with **5 layouts** (*Default*, *Agenda Split*, *Minimal Dot*, *Editorial Bold*, and *Bento Boxed*), 8 accent colours, and `Highlights` vs `Full Colour` modes.
- **Full-Screen Media Player:** Appears automatically when music or podcasts are playing, complete with album art, an animated progress scrubber, big tap targets for play/pause and track skipping, and a **top-edge left/right swipe gesture** to smoothly adjust music volume.

### 3. Touch-and-Hold Live Studio & Edge Gestures
You don't need to unfold the phone to tweak how StandBy looks or behaves:
- **Long-press the Clock** (in Split Dashboard or Full-Screen Clock) to open the **Clock Studio** and live-preview styles, accent colours, and highlight modes.
- **Long-press the Calendar** to open the **Calendar Studio**.
- **Swipe up/down along the left screen edge** on any StandBy view to adjust numeral and widget brightness on the fly.
- **Swipe left/right along the top edge of the Music screen** to adjust media playback volume with a sleek pill indicator.
- **Tap the top-right gear icon** on the StandBy screen to adjust wallpaper visibility, subtle motion, and brightness sliders.

### 4. Built for OLED Longevity & Battery Life
- **Burn-In Protection:** Periodic subtle pixel-shifting keeps static pixels from fatiguing the cover OLED panel during long desk or overnight sessions.
- **Ambient vs. Eco Power Profiles:** Switch manually or let F8scape automatically drop into **Eco Mode** (pure black OLED background, zero background motion, reduced sensor polling) when Android Battery Saver turns on or the device warms up.
- **Auto-Off Timer:** Optionally turn the StandBy display off after `15`, `30`, or `60` minutes in tent mode, or leave it on `Never` while docked.
- **Phone Call Guardrail:** Automatically hides the StandBy overlay the moment a phone call rings or connects so you always have immediate access to the native dialer.

---

## 🔐 Permissions & Why We Need Them

F8scape asks for the absolute minimum permissions needed to function. When you first open the app, a 2-step setup guides you through the two required permissions; everything else is optional and can be toggled on or off from the Home screen.

### Required to Run
| Permission | Why F8scape Needs It | Privacy Guarantee |
| :--- | :--- | :--- |
| **Accessibility Service** | Allows F8scape to keep its lightweight hinge-angle listener alive in the background and display the StandBy screen reliably on the outer cover display when tented. | Configured with `canRetrieveWindowContent="false"` and `accessibilityEventTypes=""`. F8scape **cannot** read screen text, notifications, passwords, or taps from other apps. |
| **Appear on Top** (`SYSTEM_ALERT_WINDOW`) | Lets F8scape draw the dark StandBy dashboard over the cover screen as soon as the phone settles into tent mode. | Used strictly to render the local StandBy overlay window. |

### Optional Features (Enabled from the App Home Screen)
| Permission | Why F8scape Needs It | Privacy Guarantee |
| :--- | :--- | :--- |
| **Notification Access** *(Optional)* | Detects active media sessions (Spotify, YouTube Music, Apple Music, Pocket Casts, etc.) so you can see album art and control playback in tent mode. | Reads only `MediaSession` metadata (`MediaController`). Personal notifications and messages are never read or stored. |
| **Read Calendar** *(Optional)* | Shows dots on days with events and lists your next upcoming appointment on the Calendar card. Includes a **Mask When Locked** option in Settings. | Queried locally on-device via Android's `CalendarContract`. Calendar data never leaves your phone. |
| **Internet** *(Weather Only)* | Fetches current temperature and weather conditions for your chosen city (supporting Phone Default, `°C`, or `°F`). | Locked via Android's `network_security_config.xml` exclusively to `https://api.open-meteo.com` and `https://geocoding-api.open-meteo.com` (free, open-source weather API with no API keys or tracking). |
| **Run at Startup** (`RECEIVE_BOOT_COMPLETED`) | Restores the background tent-posture watcher after you restart your phone or install an app update. | Internal receiver (`exported="false"`). |

---

## 🛡️ Security by Design

- **Zero Trackers or Ads:** No Firebase, no Google Play Services analytics, no crash-reporting SDKs, and no third-party telemetry libraries.
- **Strict HTTPS Whitelisting:** Cleartext HTTP traffic is blocked at the OS manifest level (`usesCleartextTraffic="false"`), and outbound connections are restricted solely to Open-Meteo over TLS.
- **Zero Dynamic Code Execution:** No `WebView`, no JavaScript bridges, no shell execution, and no runtime reflection.
- **Tapjacking Protection:** The configuration app enforces `filterTouchesWhenObscured = true` so malicious overlays cannot trick you into tapping settings.
- **No Cloud Backups:** `android:allowBackup="false"` ensures your local preferences stay strictly on your device.

---

## 📦 Installation

1. Download the latest `F8scape-v1.0.0.apk` from the [**Releases**](../../releases) page on your Galaxy Z Fold 8.
2. Open the APK to install it.
3. Launch **F8scape** and complete the quick 2-step setup (**Accessibility Service** and **Appear on Top**).
4. *(Optional)* Enable **Music & Media Controls**, **Calendar Events**, or customize your **Weather City & Unit (`°C` / `°F`)** right from the Home tab.
5. Fold your phone into **tent position** (`~90°`) and set it down on your desk.

---

## 🛠️ Building from Source

Requirements: **JDK 17** and the **Android SDK (API 35+)**.

```bash
# Run unit tests
./gradlew test

# Build minified & resource-shrunk Release APK
./gradlew assembleRelease
```

The compiled release APK will be generated at `app/build/outputs/apk/release/app-release.apk`.

---

## 📄 License & Disclaimer

See [PRIVACY.md](PRIVACY.md) for the full breakdown of local data handling.  
F8scape is an independent project created for Galaxy Z Fold 8 owners and is not affiliated with or endorsed by Samsung Electronics.
