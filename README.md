<p align="center">
  <h1 align="center">🚗 RoadMate</h1>
  <p align="center">
    <strong>Open Source Vehicle Maintenance & Trip Tracker for Android</strong>
  </p>
  <p align="center">
    Runs silently on your car's head unit. Tracks mileage via GPS. Manages maintenance schedules.<br/>
    Records trips. Syncs to your phone over Bluetooth. Zero cloud dependency.
  </p>
  <p align="center">
    <a href="#features">Features</a> •
    <a href="#architecture">Architecture</a> •
    <a href="#getting-started">Getting Started</a> •
    <a href="#tech-stack">Tech Stack</a> •
    <a href="#project-structure">Project Structure</a> •
    <a href="#roadmap">Roadmap</a> •
    <a href="#contributing">Contributing</a> •
    <a href="#license">License</a>
  </p>
</p>

---

## What is RoadMate?

RoadMate transforms your aftermarket Android car head unit from a passive media player into an **active vehicle management system**. It runs as a background service that auto-starts on boot, auto-detects trips through GPS, manages your maintenance schedule, and auto-syncs everything to your phone — **you do nothing differently**.

### Why RoadMate?

| Problem | RoadMate Solution |
|---|---|
| "When was my last oil change?" | Maintenance tracking with km-based progress bars and predicted service dates |
| "How many km did I drive this month?" | Automatic GPS trip recording — zero manual logging |
| "My insurance expired and I didn't notice" | Local phone notifications 30 days before document expiry |
| "I don't trust cloud apps with my location data" | **Zero network permissions.** All data stays on your devices. Period. |
| "I need WiFi/internet to sync" | Syncs over the Bluetooth link your phone already uses for calls and music |

### What Makes This Different

- **🔇 Zero-friction** — No accounts, no setup ritual, no manual trip logging. Boot → track → sync happens automatically.
- **🔒 Privacy as architecture** — No `INTERNET` permission requested. Data _can't_ leak because the app physically cannot make network requests.
- **📡 Bluetooth data channel** — Repurposes the existing phone-to-head-unit BT Classic link (RFCOMM SPP) for data sync. No WiFi hotspots, no internet.
- **🔮 Predictive maintenance** — Calculates _when_ your next service is due based on your actual daily driving average. Not just km remaining — an estimated calendar date.
- **🆓 Fully open source** — No subscriptions, no ads, no vendor lock-in.

---

## Features

### 📊 Dashboard
- Current odometer reading (manual + GPS-accumulated)
- Next maintenance due with progress bar and estimated date
- Active alerts (overdue maintenance, expiring documents)
- Quick stats: today's km, this week, this month

### 🔧 Maintenance Manager
- Visual progress bars showing percentage to next service
- Pre-built maintenance templates per vehicle (10,000 km Egypt service cycle default)
- Custom maintenance items with configurable km/month intervals
- "Mark as Done" with cost, location, and notes
- Full service history per item
- **Predictive scheduling** — estimated date based on your avg daily km

### 🛣️ Trip Recorder
- Fully automatic trip detection via GPS state machine
- GPS drift prevention with 3-gate validation (no phantom trips in garages)
- Trip list with distance, duration, avg/max speed, estimated fuel
- Route recording with OpenStreetMap display
- Trip merge/split capability
- Share routes via Google Maps URL

### ⛽ Fuel Tracker
- Log fill-ups: liters, price/liter, odometer, station
- Actual L/100km calculation between fill-ups
- Actual vs estimated consumption comparison
- Cost trends and cost-per-km analysis

### 📋 Pre-Trip Checklists
- Default daily and long-trip checklists
- Custom checklists with completion history

### 📄 Documents Wallet
- Insurance, license, registration expiry tracking
- Configurable reminder notifications (X days before expiry)

### 🚙 Multi-Vehicle Support
- Independent maintenance schedules, trips, and fuel logs per vehicle
- Quick-switch between vehicle profiles

### 📱 Phone Companion App
- Synced dashboard showing all vehicle data
- Local notifications for upcoming maintenance and expiring documents
- No internet required — everything over Bluetooth

---

## Architecture

RoadMate is a **multi-module Kotlin project** with two Android apps sharing a common core:

```
┌─────────────────────────────────────────┐
│            Car Head Unit                │
│                                         │
│  ┌─────────────────────────────────┐    │
│  │  RoadMate Foreground Service    │    │
│  │  ├── GPS Tracker                │    │
│  │  ├── Trip State Machine         │    │
│  │  └── BT RFCOMM Sync Server     │    │
│  └─────────────────────────────────┘    │
│  ┌──────────┐  ┌───────────────────┐    │
│  │ Room DB  │  │ Dashboard UI      │    │
│  │ (Source  │  │ (Jetpack Compose) │    │
│  │ of Truth)│  │                   │    │
│  └──────────┘  └───────────────────┘    │
└──────────────────┬──────────────────────┘
                   │ Bluetooth RFCOMM (SPP)
                   │ Delta Sync
┌──────────────────┴──────────────────────┐
│              Phone                      │
│                                         │
│  ┌──────────┐  ┌───────────────────┐    │
│  │ Room DB  │  │ Companion UI      │    │
│  │ (Synced  │  │ (Jetpack Compose) │    │
│  │  Copy)   │  │                   │    │
│  └──────────┘  └───────────────────┘    │
│  ┌─────────────────────────────────┐    │
│  │ WorkManager                     │    │
│  │ └── Local Notification Engine   │    │
│  └─────────────────────────────────┘    │
└─────────────────────────────────────────┘
```

### Sync Protocol

- **Transport:** Bluetooth Classic RFCOMM (SPP) — reuses the existing audio/calls BT link
- **Strategy:** Bidirectional delta sync — only records modified since last sync
- **Format:** Length-prefixed JSON frames (`SYNC_STATUS`, `SYNC_PULL`, `SYNC_PUSH`, `SYNC_ACK`)
- **Conflict resolution:** Last-write-wins
- **Triggers:** On BT connect, on events (trip end, maintenance logged), every 15 min, manual

### Data Integrity

| Protection Layer | Strategy | Max Data Loss |
|---|---|---|
| Room WAL mode | Write-ahead logging for crash resilience | Minimal |
| TripPoint flush | Batch GPS points, flush to Room every 10s | 10 seconds |
| Crash recovery journal | Trip summary to DataStore every 30s | 30 seconds |

On boot after power loss: detect interrupted trips → recover from TripPoints + crash journal → mark as `INTERRUPTED` with 98%+ data preserved.

### GPS Drift Prevention

Three gates must pass before a trip triggers — eliminates phantom trips from indoor GPS drift:

| Gate | Check | Threshold |
|---|---|---|
| Accuracy | `Location.getAccuracy()` | < 25m |
| Sustained Speed | Consecutive readings > 5 km/h | 3 readings (~30s) |
| Displacement | Distance from last parking point | > 50 meters |

---

## Tech Stack

| Technology | Purpose |
|---|---|
| **Kotlin** | Language (min API 29 / Android 10) |
| **Jetpack Compose** | UI framework with Material 3 |
| **Room** | Local database (WAL mode) |
| **Hilt** | Dependency injection |
| **Android Bluetooth API** | RFCOMM sync (server on head unit, client on phone) |
| **Google Location Services** | GPS via FusedLocationProvider |
| **osmdroid** | OpenStreetMap display (fully open source) |
| **WorkManager** | Background notification scheduling (phone) |
| **DataStore** | Preferences, settings, crash recovery journal |
| **Kotlin Serialization** | JSON for Bluetooth sync protocol |

---

## Project Structure

```
roadmate/
├── app-headunit/           # Head unit application
│   └── src/main/
│       ├── service/        # GPS foreground service
│       ├── ui/             # Compose screens (dashboard, trips, maintenance)
│       ├── sync/           # Bluetooth RFCOMM sync server
│       └── boot/           # Boot receiver (auto-start)
│
├── app-phone/              # Phone companion application
│   └── src/main/
│       ├── ui/             # Compose screens (companion viewer)
│       ├── sync/           # Bluetooth RFCOMM sync client
│       ├── worker/         # WorkManager notification jobs
│       └── backup/         # Google Drive backup (V2)
│
├── core/                   # Shared module
│   ├── database/           # Room entities, DAOs, migrations
│   ├── model/              # Domain models
│   ├── repository/         # Data repositories
│   ├── obd/                # OBD-II interfaces (V1 design, V2 impl)
│   ├── templates/          # Car maintenance templates (JSON)
│   └── util/               # Distance calc, fuel estimation, speed analysis
│
├── docs/                   # Documentation
├── _bmad-output/           # Planning & implementation artifacts
│   ├── planning-artifacts/ # PRD, architecture, UX specs, epics
│   └── implementation-artifacts/ # Story specs, sprint tracking
│
├── build.gradle.kts
└── settings.gradle.kts
```

---

## Getting Started

### Prerequisites

- **Android Studio** Hedgehog (2023.1.1) or later
- **JDK 17**
- **Android SDK** with API 29+ installed
- A physical Android head unit for full testing (emulator works for UI development)
- A phone + head unit pair for Bluetooth sync testing

### Build & Run

```bash
# Clone the repository
git clone https://github.com/YOUR_USERNAME/roadmate.git
cd roadmate

# Build the project
./gradlew build

# Install head unit app
./gradlew :app-headunit:installDebug

# Install phone app
./gradlew :app-phone:installDebug
```

### First Run

1. Install `app-headunit` on your car's Android head unit
2. Install `app-phone` on your phone
3. Pair the devices via Bluetooth (if not already paired for audio)
4. Launch RoadMate on the head unit → set up your vehicle profile and current odometer
5. The service auto-starts on every boot — drive and forget

---

## Default Vehicle Template

Pre-configured for **Mitsubishi Lancer EX 2015 1.6L** (Egypt 10,000 km service cycle):

| Item | Interval (km) | Interval (months) |
|---|---|---|
| Engine Oil + Filter | 10,000 | 6 |
| Air Filter | 20,000 | 12 |
| AC/Cabin Filter | 20,000 | 12 |
| Spark Plugs | 40,000 | 24 |
| Brake Pads (inspect) | 20,000 | 12 |
| Brake Fluid | 40,000 | 24 |
| CVT Transmission Fluid | 40,000 | 24 |
| Coolant | 40,000 | 24 |
| Power Steering Fluid | 60,000 | 36 |
| Timing Chain (inspect) | 100,000 | — |
| Tire Rotation | 10,000 | 6 |
| Battery (check) | 20,000 | 12 |
| Serpentine Belt | 60,000 | 36 |
| Fuel Filter | 40,000 | 24 |
| Wiper Blades | — | 6 |

> Custom templates can be created for any vehicle. Community-contributed templates are welcome.

---

## Roadmap

### ✅ V1 — MVP (Current)

- [x] Project architecture & planning
- [ ] Foreground service with auto-start on boot
- [ ] GPS-based trip detection (state machine)
- [ ] 3-gate GPS drift prevention
- [ ] Power loss data integrity (WAL + flush + crash journal)
- [ ] Manual odometer entry
- [ ] Dashboard with maintenance progress & predictions
- [ ] Maintenance manager with templates
- [ ] Bluetooth RFCOMM delta sync
- [ ] Phone companion with local notifications
- [ ] Multi-vehicle support
- [ ] Fuel logging & consumption calculation

### 🔜 V1 Growth

- [ ] Trip route display on OpenStreetMap
- [ ] Trip merge/split
- [ ] Route sharing via Google Maps URL
- [ ] Pre-trip checklists
- [ ] Documents wallet with photo storage
- [ ] Statistics & reports (monthly/yearly)
- [ ] CSV/PDF export

### 🔮 V2 — Vision

- [ ] OBD-II Bluetooth ELM327 integration
- [ ] Real-time OBD data (live ODO, RPM, fuel rate, coolant temp, DTC codes)
- [ ] Google Drive backup
- [ ] Kalman filter for smoother route recording
- [ ] Community maintenance templates
- [ ] Driving pattern analysis (city vs highway, night driving)

---

## Permissions

RoadMate requests only what it needs — and explicitly avoids network permissions:

| Permission | App | Purpose |
|---|---|---|
| `ACCESS_FINE_LOCATION` | Head Unit | GPS trip tracking |
| `ACCESS_BACKGROUND_LOCATION` | Head Unit | Continuous GPS in foreground service |
| `FOREGROUND_SERVICE` | Head Unit | Persistent tracking service |
| `RECEIVE_BOOT_COMPLETED` | Head Unit | Auto-start on boot |
| `BLUETOOTH_CONNECT` | Both | RFCOMM data sync |
| `POST_NOTIFICATIONS` | Phone | Local maintenance/expiry alerts |

**Not requested:** `INTERNET`, `ACCESS_NETWORK_STATE`, `READ_CONTACTS`, `CAMERA`

---

## Contributing

Contributions are welcome! Here's how to help:

1. **Fork** the repository
2. **Create a feature branch** (`git checkout -b feature/add-toyota-template`)
3. **Commit** your changes (`git commit -m 'Add Toyota Corolla 2020 maintenance template'`)
4. **Push** to the branch (`git push origin feature/add-toyota-template`)
5. **Open a Pull Request**

### Good first contributions

- 🚙 **Maintenance templates** — Add templates for your car model
- 🌍 **Localization** — Translate the app to your language
- 📝 **Documentation** — Improve setup guides for specific head unit models
- 🐛 **Bug reports** — Test on your hardware and report issues

---

## Competitors

| App | Approach | RoadMate Difference |
|---|---|---|
| **Drivvo** | Cloud-based, requires account, ads | Local-only, no account, no ads |
| **Fuelio** | Phone-only, manual trip logging | Auto-detection via head unit GPS |
| **Car Scanner** | Requires OBD-II dongle from day 1 | GPS-first, OBD as upgrade path |
| **Google Maps Timeline** | Requires Google account, cloud-stored | Fully local, no Google dependency |

---

## License

This project is open source. See [LICENSE](LICENSE) for details.

---

<p align="center">
  Built with ☕ and 🔧 for car enthusiasts who want data-informed vehicle ownership<br/>without giving up their privacy.
</p>
