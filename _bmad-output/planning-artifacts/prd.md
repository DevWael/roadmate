---
stepsCompleted: [step-01-init, step-02-discovery, step-02b-vision, step-02c-executive-summary, step-03-success, step-04-journeys, step-05-domain, step-06-innovation, step-07-project-type, step-08-scoping, step-09-functional, step-10-nonfunctional, step-11-polish]
releaseMode: phased
inputDocuments: [roadmate.md]
workflowType: 'prd'
documentCounts:
  briefs: 0
  research: 0
  projectDocs: 1
  projectContext: 0
classification:
  projectType: mobile_app
  domain: automotive
  complexity: high
  projectContext: brownfield
---

# Product Requirements Document - RoadMate

**Author:** Ahmad
**Date:** 2026-05-08

## Executive Summary

RoadMate is an open source Android application that transforms a car's aftermarket head unit from a passive media player into an active vehicle management system. Running as a background foreground service, it silently tracks mileage via GPS, manages maintenance schedules, records trips, estimates fuel consumption, and monitors document expiry — all with zero cloud dependency. A companion phone app syncs automatically over the existing Bluetooth Classic link (RFCOMM SPP) that's already established for calls and music, delivering local notifications for upcoming maintenance and expiring documents without requiring any internet connection.

The system targets car owners who want data-informed vehicle ownership without behavioral overhead. The head unit app auto-starts on boot, auto-detects trips through a GPS-based state machine, and auto-syncs to the phone — the driver does nothing differently. V1 ships with GPS-based tracking and manual odometer entry, with OBD-II integration designed at the interface level (`OBDProvider`) for seamless V2 plug-in.

Built as a multi-module Kotlin project (Jetpack Compose, Room, Hilt), the architecture splits into `app-headunit` (GPS tracker + dashboard + BT sync server), `app-phone` (companion viewer + notifications + BT sync client), and `core` (shared data layer, business logic, OBD interfaces). Default configuration targets a Mitsubishi Lancer EX 2015 with Egypt's 10,000 km service cycle, with full multi-vehicle and custom schedule support.

### What Makes This Special

- **Zero-friction by design** — No setup, no accounts, no manual trip logging. Boot → track → sync happens automatically via services the car already runs (GPS, Bluetooth).
- **Privacy as architecture** — All data stays on-device. Room databases on head unit and phone, Bluetooth delta sync, local notifications via WorkManager. No server, no API keys, no data leaving the user's control.
- **Untapped Bluetooth channel** — The phone-to-head-unit BT link exists for audio and calls. RoadMate repurposes it as a data sync channel using RFCOMM SPP, eliminating the need for WiFi hotspots, internet, or any additional pairing.
- **Proactive maintenance** — Transforms reactive car ownership ("something sounds wrong") into predictive ownership with km-based and time-based maintenance prediction, progress bars, and estimated service dates calculated from actual daily driving averages.
- **Open source, zero cost** — No subscriptions, no vendor lock-in, community-extensible.

## Project Classification

| Attribute | Value |
|---|---|
| **Project Type** | Mobile App — Dual native Android (head unit + phone companion), multi-module Kotlin |
| **Domain** | Automotive — Vehicle maintenance, trip tracking, GPS telematics |
| **Complexity** | High — Real-time GPS processing, Bluetooth RFCOMM sync protocol, foreground service lifecycle, power loss data integrity, trip detection state machine, OBD-II interface design |
| **Project Context** | Brownfield — Comprehensive design document with architecture, data models, 8 feature modules, sync protocol, and technical hardening fully specified |

## Success Criteria

### User Success

- **Zero-touch daily operation** — After initial vehicle profile setup, the driver never interacts with the head unit app during normal driving. Trips are detected, recorded, and synced without any user action.
- **Maintenance awareness** — The driver always knows what's coming next: which service, how many km remain, and an estimated date — visible at a glance on the dashboard card.
- **No missed renewals** — Insurance, license, and registration expiry dates generate phone notifications 30 days before expiry. No more surprise fines.
- **Accurate trip history** — Every drive is recorded with distance, duration, speed stats, and route. False trip triggers from GPS drift in garages/parking are rejected by the 3-gate validation system.
- **Fuel cost visibility** — Actual L/100km calculated from fill-up logs, compared against estimated consumption. Cost-per-km trends over time give the driver a real picture of ownership cost.

### Business Success

- **Open source adoption** — As a personal/community project, success is measured by utility, not revenue:
  - Functional V1 deployed on the developer's own head unit and phone as daily driver
  - GitHub stars and community forks indicating interest
  - At least one community-contributed maintenance template for a different vehicle
- **Personal reliability** — The app runs stably for 30+ consecutive days without crashes, data loss, or sync failures on the developer's own vehicle setup.

### Technical Success

- **Foreground service stability** — Service survives across reboots, screen-off states, and extended driving sessions (4+ hours) without being killed by Android.
- **Power loss resilience** — Zero data loss beyond the documented thresholds (10s of route points, 30s of trip summary) on abrupt power cut, verified by crash recovery journal.
- **Bluetooth sync reliability** — Delta sync completes within 5 seconds for typical payloads (daily driving data). Conflict resolution (last-write-wins) produces no data corruption.
- **GPS accuracy** — Trip detection does not false-trigger in garages/parking (3-gate validation: accuracy <25m, sustained speed 30s, displacement >50m). Route recording reflects actual driven path.
- **OBD-II interface readiness** — `OBDProvider` interface compiles and `MockOBDProvider` stubs return null gracefully, proving the V2 upgrade path works at the contract level.

### Measurable Outcomes

| Metric | Target |
|---|---|
| Trip false-positive rate (indoor/garage) | <1% |
| Sync completion time (daily data) | <5 seconds |
| Service uptime without restart | 30+ days |
| Power loss data recovery rate | >99% of trip data |
| Boot-to-tracking-active time | <15 seconds |
| Maintenance prediction accuracy | ±500 km of actual service date |

## Product Scope

### MVP — Minimum Viable Product

**Head Unit App:**
- Foreground service with auto-start on boot
- GPS-based trip detection (state machine: IDLE → DRIVING → STOPPING)
- GPS drift prevention (3-gate validation)
- Power loss data integrity (WAL + flush + crash journal)
- Manual odometer entry
- Dashboard: current ODO, next maintenance, active alerts, daily/weekly/monthly km
- Maintenance manager: template schedules (Lancer EX 2015 default), mark-as-done, history, prediction
- Trip list with distance, duration, speed stats
- Bluetooth RFCOMM SPP sync server

**Phone App:**
- Bluetooth RFCOMM sync client (auto-sync on BT connect, event-driven, periodic 15min)
- Companion dashboard (read-only mirror of vehicle state)
- Local notifications via WorkManager (maintenance due within 500km, document expiry within 30 days)

**Core:**
- Room database with all entities (Vehicle, MaintenanceSchedule, MaintenanceRecord, Trip, TripPoint, FuelLog, Document, PreTripChecklist)
- Delta sync protocol (length-prefixed JSON, SYNC_STATUS/PULL/PUSH/ACK)
- Multi-vehicle support
- `OBDProvider` interface + `MockOBDProvider` stub

### Growth Features (Post-MVP)

- Trip route display on OpenStreetMap (osmdroid)
- Trip merge/split
- Share route via Google Maps URL (WhatsApp/Telegram)
- Fuel tracker with actual vs estimated consumption comparison
- Pre-trip checklists (daily + long trip defaults, custom checklists)
- Documents wallet with photo storage
- Statistics & reports (monthly/yearly summaries, driving patterns, cost trends)
- CSV/PDF export
- Weekly summary notifications

### Vision (Future — V2)

- OBD-II Bluetooth ELM327 integration (`BluetoothOBDProvider` implementing existing interface)
- Real-time OBD data: live odometer, RPM, fuel rate, coolant temp, battery voltage, DTC codes
- Google Drive backup
- Kalman filter for smoother route recording
- Community maintenance templates (crowdsourced per vehicle model)
- Night driving percentage analysis
- City vs highway driving classification from speed profiles

## User Journeys

### Journey 1: Karim's Daily Commute (Primary User — Success Path)

**Karim**, 32, drives a Mitsubishi Lancer EX 2015 in Cairo. He commutes 40 km daily and has no idea when his last oil change was. His car makes a new noise every few months, and he always wonders if he should’ve serviced it sooner.

**Opening Scene:** Karim installs RoadMate on his aftermarket Android head unit and pairs his Pixel phone via Bluetooth (already paired for music). He enters his current odometer reading — 87,400 km — and selects "Mitsubishi Lancer EX 2015" from the template list. Setup done in under 2 minutes.

**Rising Action:** Monday morning, Karim starts his car. The head unit boots, RoadMate’s foreground service starts automatically, and the persistent notification reads "RoadMate tracking active." He plays his podcast and drives. Behind the scenes, GPS detects sustained movement (>5 km/h for 30 seconds, displacement >50 meters) and begins recording the trip. His phone auto-connects via Bluetooth, triggering a delta sync — yesterday’s trip data flows to the phone in under 5 seconds.

**Climax:** Two weeks in, Karim’s phone buzzes: "Oil Change due in 480 km — estimated May 22." He glances at the head unit dashboard during a red light: the oil change progress bar is at 95%, showing exactly 480 km remaining. He schedules a service appointment for Saturday. After the service, he taps "Mark as Done" on the maintenance screen, enters the cost (850 EGP), and the cycle resets.

**Resolution:** Three months later, Karim hasn’t missed a single service interval. His trip history shows he drives 42 km/day on average. The maintenance predictor uses this to nail service dates within a week. He never thinks about car maintenance — it just happens on time. His car runs smoother, and he has a cost history showing he spent 3,200 EGP on maintenance this quarter.

---

### Journey 2: Karim's Road Trip to Hurghada (Primary User — Edge Case / Long Trip)

**Opening Scene:** Karim plans a 450 km drive from Cairo to Hurghada for a weekend trip. Before leaving, the head unit shows an alert: "Tire Rotation due in 200 km." He decides to do it before the trip.

**Rising Action:** He logs the tire rotation as done. The long trip triggers continuous GPS recording across 5+ hours. The head unit handles a 30-minute fuel stop — GPS speed drops below 2 km/h, the STOPPING state timer begins, but Karim restarts within 20 minutes, so the trip continues as one segment. Midway through a mountain pass, the head unit briefly loses GPS signal (tunnel). The GAP_CHECK state activates, but the gap is only 45 seconds — the trip resumes seamlessly.

**Climax:** After arriving, Karim logs a fuel fill-up on his phone: 38 liters, 12.50 EGP/liter, full tank. RoadMate calculates his actual highway consumption: 7.1 L/100km — better than the 8.0 L/100km mixed estimate. He taps on the trip and sees the full route on the OpenStreetMap view. He generates a Google Maps URL of his route and shares it in the family WhatsApp group.

**Resolution:** The trip summary shows 462 km, 5h 12m driving time, max speed 128 km/h, average 89 km/h. His fuel cost for the trip: 475 EGP. He now has real data for planning future trips — he knows exactly what the drive costs.

---

### Journey 3: Karim's Garage Phantom (Primary User — GPS Drift Edge Case)

**Opening Scene:** Karim parks in an underground garage at work. Raw GPS jumps show 8 km/h of phantom movement.

**Rising Action:** Gate 1 (accuracy check): `Location.getAccuracy()` returns 65 meters — far above the 25m threshold. **Rejected.** Even if accuracy somehow passes, Gate 2 (sustained speed) requires 3 consecutive readings above 5 km/h over 30 seconds — sporadic drift rarely sustains. Gate 3 (displacement) checks actual distance from the last parking point — phantom GPS movement oscillates around the same spot, never exceeding 50 meters of real displacement.

**Climax:** No false trip is created. The state machine stays in IDLE. The odometer doesn’t drift.

**Resolution:** At 5 PM, Karim starts driving out. Real movement passes all three gates within 35 seconds, and a genuine trip begins recording. Zero phantom trips in his history.

---

### Journey 4: Power Loss Recovery (Technical Edge Case)

**Opening Scene:** Karim is driving on the Ring Road when his head unit suddenly loses power — loose wiring. Mid-trip, everything cuts.

**Rising Action:** The foreground service was flushing TripPoints to Room every 10 seconds (WAL mode). The crash recovery journal in DataStore was updating trip summary (distance, ODO, duration) every 30 seconds.

**Climax:** Karim fixes the wiring and the head unit reboots. RoadMate’s boot receiver triggers the service. On startup, the service finds a Trip with no `endTime` — an interrupted trip. It recovers the route from the last flushed TripPoints (lost: at most 10 seconds of GPS track). It recovers distance and ODO from the DataStore crash journal (lost: at most 30 seconds of summary data). The trip is marked `status = INTERRUPTED` with all recovered data preserved.

**Resolution:** Karim’s trip history shows the interrupted trip with 98%+ of the data intact. No phantom ODO increase. No database corruption. The next trip starts clean.

---

### Journey 5: Multi-Vehicle Owner (Secondary User — Fleet/Family)

**Opening Scene:** Karim’s wife Nadia drives a Hyundai Tucson. They share the same head unit (family car has its own). Karim adds a second vehicle profile: "Hyundai Tucson 2020, 2.0L Gas, 45,200 km."

**Rising Action:** He sets up a custom maintenance schedule for the Tucson — different intervals than the Lancer. Each vehicle has its own trips, fuel logs, maintenance schedules, and document expiry dates. The head unit dashboard has a quick-switch between vehicles. When Nadia drives, she switches to the Tucson profile before starting.

**Climax:** Both vehicles’ data syncs to Karim’s phone. He sees a unified view: the Lancer needs brake pad inspection in 3,000 km; the Tucson’s insurance expires in 22 days. One phone, two vehicles, complete visibility.

**Resolution:** Karim manages both family vehicles from a single phone. Each has independent tracking, independent maintenance predictions, and independent cost histories.

---

### Journey Requirements Summary

| Journey | Capabilities Revealed |
|---|---|
| **Daily Commute** | Auto-start service, trip detection, BT sync, maintenance tracking, progress bars, prediction, mark-as-done, dashboard summary |
| **Road Trip** | Long trip handling, stop/resume detection, fuel logging, consumption calculation, route display, trip sharing, GPS gap recovery |
| **Garage Phantom** | 3-gate GPS validation, drift rejection, accuracy/speed/displacement checks |
| **Power Loss** | WAL journaling, TripPoint flushing, DataStore crash journal, boot recovery, interrupted trip handling |
| **Multi-Vehicle** | Vehicle profiles, quick-switch, independent schedules/trips/logs, unified phone view |

## Domain-Specific Requirements

### Android Automotive Platform Constraints

- **Head unit limitations** — Aftermarket Android head units run standard Android (not Android Automotive OS). No access to Vehicle HAL or Car API. All vehicle data comes from GPS and manual entry (V1) or OBD-II (V2).
- **No battery optimization** — Head units run on car power with no Doze mode or battery saving. Foreground services run without the restrictions phone apps face. However, **abrupt power loss is the norm**, not the exception.
- **Screen form factor** — Head unit displays are typically 7-10 inches, landscape orientation, viewed at arm's length while driving. UI must be glanceable — large text, high contrast, no fine interaction required while moving.
- **Boot behavior** — Head unit boots every time the car starts. App must auto-start via `BOOT_COMPLETED` receiver and reach tracking-ready state within 15 seconds.

### GPS & Location Accuracy

- **Urban canyon effects** — Tall buildings in Cairo create GPS multipath. Accuracy can degrade to 30-50m in dense areas. The 25m accuracy gate handles this by rejecting unreliable fixes for trip triggering.
- **Tunnel/underground gaps** — GPS signal loss in tunnels, parking garages, and underpasses. GAP_CHECK state handles resumption vs. trip boundary decisions.
- **Speed calculation** — GPS-derived speed is accurate at >20 km/h but noisy at low speeds. The 5 km/h threshold for trip detection accounts for this noise floor.
- **Distance accumulation** — GPS-calculated distance over-counts on curved roads and under-counts on straight roads. Acceptable for maintenance prediction (±2-3% error), but manual ODO entry remains the ground truth.

### Bluetooth RFCOMM Constraints

- **Single RFCOMM connection** — Only one phone can sync at a time. Multi-user scenarios (Karim and Nadia) require sequential syncing.
- **BT Classic vs BLE** — RFCOMM SPP chosen over BLE because BLE has a 512-byte MTU limit per characteristic and is designed for low-bandwidth sensor data. RFCOMM provides a reliable stream socket with ~2-3 Mbps throughput, better suited for delta sync payloads.
- **Connection lifecycle** — BT connection may drop during sync (phone out of range, BT restart). Sync protocol must be idempotent — interrupted syncs retry from the last acknowledged point.
- **Android BT permission model** — Android 12+ requires `BLUETOOTH_CONNECT` and `BLUETOOTH_SCAN` runtime permissions. The app must handle permission denial gracefully.

### Data Integrity & Privacy

- **No cloud = no backup (V1)** — If the head unit's storage fails, data is lost unless the phone has a recent sync. Users should be informed of this limitation. Google Drive backup deferred to V2.
- **Local-only data** — No network permissions requested. No analytics, no telemetry, no crash reporting to external services. Privacy is enforced at the permission level, not just policy.
- **Sync conflict resolution** — Last-write-wins is simple but can lose data if both devices edit the same record offline. Acceptable for V1 given single-user scenarios. V2 could add conflict UI for multi-user households.

### Risk Mitigations

| Risk | Mitigation |
|---|---|
| Head unit runs out of storage | Monitor Room DB size, warn at 80% capacity, offer export/purge of old trips |
| GPS hardware failure on head unit | Detect prolonged GPS unavailability (>24h), alert user, continue maintenance tracking without trips |
| Bluetooth pairing lost | Detect failed connection attempts, notify user to re-pair, queue sync data for next successful connection |
| ODO drift from GPS inaccuracy | Display GPS-accumulated vs manual ODO separately, let user reconcile periodically |
| Room DB corruption from power loss | WAL mode + crash recovery journal, periodic DB integrity check on boot |

## Innovation & Novel Patterns

### Detected Innovation Areas

**1. Bluetooth Audio Link as Data Channel**
The phone is already BT-paired to the head unit for calls and music. RoadMate treats this existing RFCOMM-capable link as a bidirectional data sync channel. This eliminates the need for WiFi hotspots, internet connectivity, or any additional pairing. The innovation is in recognizing that a connection already exists and repurposing it — zero new hardware, zero new setup.

**2. Head Unit as Background Vehicle Intelligence**
Aftermarket Android head units are treated as media players and navigation screens. RoadMate redefines the head unit’s role — it becomes a persistent, always-on vehicle management system running a foreground service. The head unit’s constant power supply (no Doze mode, no battery optimization) becomes an advantage rather than a limitation.

**3. Privacy-by-Architecture (Not Policy)**
Most vehicle tracking apps collect data server-side. RoadMate enforces privacy at the Android permission level — no INTERNET permission requested, period. Data can’t leak because the app physically cannot make network requests. This is architecturally different from “we promise not to share your data.”

### Market Context & Competitive Landscape

| Competitor | Approach | RoadMate Difference |
|---|---|---|
| **Drivvo** | Cloud-based, requires account, ad-supported | Local-only, no account, no ads, open source |
| **Fuelio** | Phone-only, manual trip logging | Auto-detection via head unit GPS, zero manual effort |
| **Car Scanner (OBD)** | Requires OBD-II dongle from day 1 | GPS-first with OBD as V2 upgrade path |
| **Google Maps Timeline** | Requires Google account, cloud-stored | Fully local, no Google dependency |

### Validation Approach

- **BT sync validation** — Test RFCOMM data transfer alongside active audio streaming (music + sync simultaneously) to confirm no interference
- **Head unit stability** — Run foreground service for 30+ consecutive days on real hardware to validate no memory leaks or ANR issues
- **Privacy claim verification** — Audit APK permissions to confirm zero network-related permissions

### Risk Mitigation

| Innovation Risk | Fallback |
|---|---|
| RFCOMM conflicts with audio streaming | Use separate RFCOMM channel UUID; test concurrent audio + data transfer |
| Head unit Android version too old for required APIs | Min API 29 (Android 10) provides all needed Bluetooth and location APIs |
| Users don’t understand “no cloud” value proposition | In-app explainer showing exactly what data exists and where it lives |

## Mobile App Specific Requirements

### Project-Type Overview

RoadMate is a **native Android** dual-app system built in **Kotlin + Jetpack Compose**. It targets two distinct Android form factors: aftermarket car head units (7-10" landscape) and standard phones. Both apps share a `core` module for data models, database, repositories, and business logic. This is not a cross-platform project — Android-only by design, since head units run Android and the primary developer's phone is a Pixel.

### Platform Requirements

| Requirement | Specification |
|---|---|
| **Min API** | 29 (Android 10) |
| **Target API** | Latest stable |
| **Language** | Kotlin |
| **UI Framework** | Jetpack Compose + Material 3 |
| **DI** | Hilt |
| **Database** | Room (WAL mode) |
| **Preferences** | DataStore |
| **Serialization** | Kotlin Serialization (JSON) |
| **Maps** | osmdroid (OpenStreetMap) |
| **Background work** | WorkManager (phone), Foreground Service (head unit) |

**Head Unit Specifics:**
- Landscape-only layout
- No Google Play Services dependency for core features (head units often lack them)
- `FusedLocationProviderClient` for GPS with fallback to `LocationManager` if Play Services unavailable
- Auto-start via `BOOT_COMPLETED` BroadcastReceiver
- Single persistent foreground notification

**Phone Specifics:**
- Portrait + landscape support
- Standard Material 3 navigation patterns
- WorkManager for periodic notification checks (every 12 hours)

### Device Permissions

| Permission | Purpose | Required |
|---|---|---|
| `ACCESS_FINE_LOCATION` | GPS trip tracking | Yes (head unit), No (phone) |
| `ACCESS_BACKGROUND_LOCATION` | Continuous GPS in foreground service | Yes (head unit only) |
| `FOREGROUND_SERVICE` | Persistent tracking service | Yes (head unit only) |
| `FOREGROUND_SERVICE_LOCATION` | Location-type foreground service | Yes (head unit only) |
| `RECEIVE_BOOT_COMPLETED` | Auto-start on boot | Yes (head unit only) |
| `BLUETOOTH_CONNECT` | RFCOMM sync | Yes (both) |
| `BLUETOOTH_SCAN` | Device discovery (initial setup only) | Yes (phone only) |
| `POST_NOTIFICATIONS` | Local maintenance/expiry alerts | Yes (phone only) |
| `WAKE_LOCK` | Keep service alive during sync | Yes (both) |

**Not requested:** `INTERNET`, `ACCESS_NETWORK_STATE`, `READ_CONTACTS`, `CAMERA` (except optional document photo in V2).

### Offline Mode

RoadMate is **offline-first by architecture** — there is no online mode. The entire app operates without any network connectivity:

- Room database is the local source of truth on each device
- Bluetooth sync is the only data transfer mechanism
- OpenStreetMap tiles can be pre-cached via osmdroid's tile download manager for offline map display
- No API calls, no cloud sync, no remote configuration

### Push / Notification Strategy

**No push notifications.** All notifications are **local**, generated by WorkManager:

- WorkManager periodic job runs every 12 hours on the phone
- Checks synced local data against thresholds:
  - Maintenance due within 500 km — notification
  - Document expiring within 30 days — notification
  - Weekly driving summary ready — notification
- Uses `NotificationCompat` with channels for each notification type
- No FCM, no server, no internet required

### Store Compliance

- **Open source distribution** — Primary distribution via GitHub releases (APK/AAB), F-Droid
- **Google Play** — Optional future listing. Would require:
  - Privacy policy (straightforward: "no data collected, no data transmitted")
  - Background location permission justification
  - Foreground service type declaration in manifest
- **No in-app purchases, no ads, no analytics SDKs**

### Implementation Considerations

- **Multi-module Gradle project** — `app-headunit`, `app-phone`, `core` modules with shared dependencies
- **Head unit testing** — No Android emulator accurately simulates head unit behavior. Testing requires real hardware (aftermarket Android head unit).
- **Bluetooth testing** — RFCOMM testing requires two physical devices. Cannot be unit-tested in isolation — needs instrumented tests with mocked BluetoothSocket or real device pairing.
- **GPS simulation** — Android emulator supports mock locations for trip detection testing, but real-world validation (drift, tunnels, urban canyons) requires on-road testing.

## Project Scoping & Phased Development

### MVP Strategy & Philosophy

**MVP Approach:** Problem-Solving MVP — deliver the core loop (track → alert → maintain) that makes vehicle ownership proactive. The MVP must work as a daily driver on real hardware from day one.

**Resource Requirements:** Solo developer (Ahmad), targeting personal use first. No external dependencies, no cloud infrastructure to maintain, no ops overhead.

### MVP Feature Set (Phase 1 — V1)

**Core User Journeys Supported:**
- ✅ Daily Commute (Journey 1) — full support
- ✅ Road Trip (Journey 2) — trip recording, fuel logging (route display is Growth)
- ✅ Garage Phantom (Journey 3) — full 3-gate GPS drift prevention
- ✅ Power Loss Recovery (Journey 4) — full crash recovery
- ✅ Multi-Vehicle (Journey 5) — full support

**Must-Have Capabilities:**

| Component | Must-Have Features |
|---|---|
| **Head Unit Service** | Foreground service, auto-start, GPS tracking, trip detection state machine, 3-gate drift prevention, power loss recovery (WAL + flush + crash journal) |
| **Head Unit UI** | Dashboard (ODO, next maintenance, alerts, km stats), maintenance manager (templates, mark-as-done, history, prediction), trip list (distance, duration, speed) |
| **Phone App** | BT sync client, companion dashboard, local notifications (maintenance + document expiry) |
| **Core** | Room DB (all entities), delta sync protocol (RFCOMM SPP), multi-vehicle, `OBDProvider` interface + `MockOBDProvider` |
| **Data Entry** | Manual odometer, vehicle profile setup, maintenance template selection, fuel log entry |

### Post-MVP Features (Phase 2 — Growth)

- Trip route display on OpenStreetMap (osmdroid)
- Trip merge/split
- Share route via Google Maps URL
- Fuel tracker with actual vs estimated consumption comparison
- Pre-trip checklists (daily + long trip defaults, custom)
- Documents wallet with photo storage
- Statistics & reports (monthly/yearly summaries, driving patterns, cost trends)
- CSV/PDF export
- Weekly summary notifications

### Phase 3 — Vision (V2)

- OBD-II Bluetooth ELM327 integration (`BluetoothOBDProvider`)
- Real-time OBD data (live odometer, RPM, fuel rate, coolant temp, battery voltage, DTC codes)
- Google Drive backup
- Kalman filter for smoother route recording
- Community maintenance templates
- Night driving / city vs highway analysis

### Risk Mitigation Strategy

**Technical Risks:**
- Most challenging: Bluetooth RFCOMM stability alongside audio streaming — mitigate with separate UUID channel and concurrent testing early
- Riskiest assumption: GPS 3-gate validation rejects all drift scenarios — validate on real hardware in parking garages before committing to the design

**Market Risks:**
- Primary risk is irrelevant for a personal-use open source project — the "market" is Ahmad's own Lancer EX
- Secondary risk: community adoption depends on documentation quality and ease of adapting to other vehicles

**Resource Risks:**
- Solo developer — if time is limited, Growth features can ship incrementally without blocking MVP utility
- No infrastructure costs (no servers, no cloud) — resource risk is purely developer time

## Functional Requirements

### Vehicle Management

- **FR1:** Driver can create a vehicle profile with make, model, year, engine type, engine size, fuel type, plate number, and optional VIN
- **FR2:** Driver can set and update the current odometer reading for a vehicle
- **FR3:** Driver can configure city and highway fuel consumption estimates for a vehicle
- **FR4:** Driver can manage multiple vehicle profiles and switch between them
- **FR5:** Driver can select a pre-built maintenance template when creating a vehicle profile
- **FR6:** Driver can set the odometer unit (km or miles) per vehicle

### Trip Tracking

- **FR7:** System can automatically detect trip start when the vehicle begins sustained movement
- **FR8:** System can automatically detect trip end when the vehicle remains stationary for a configurable timeout period
- **FR9:** System can record trip data including start/end time, distance, duration, max speed, average speed, and estimated fuel consumption
- **FR10:** System can record GPS route points (latitude, longitude, speed, altitude, timestamp) throughout a trip
- **FR11:** System can reject false trip triggers caused by GPS drift when the vehicle is stationary
- **FR12:** System can handle GPS signal gaps and determine whether to resume or end a trip
- **FR13:** Driver can view a list of recorded trips with summary statistics
- **FR14:** System can accumulate GPS-based distance and update the odometer accordingly

### Maintenance Management

- **FR15:** Driver can view all scheduled maintenance items with visual progress indicators showing percentage to next service
- **FR16:** Driver can mark a maintenance item as completed with date, odometer reading, optional cost, location, and notes
- **FR17:** Driver can view the full service history for each maintenance item
- **FR18:** System can predict the estimated date for next maintenance based on average daily driving distance
- **FR19:** Driver can add custom maintenance items beyond the pre-built template
- **FR20:** System can alert the driver when maintenance is due within a configurable km threshold
- **FR21:** Driver can configure maintenance intervals by km and/or months for each item

### Fuel Tracking

- **FR22:** Driver can log fuel fill-ups with date, odometer, liters, price per liter, total cost, full tank indicator, and station
- **FR23:** System can calculate actual fuel consumption (L/100km) between full-tank fill-ups
- **FR24:** System can compare actual fuel consumption against estimated consumption
- **FR25:** Driver can view fuel cost trends over time (weekly, monthly, yearly)
- **FR26:** System can calculate cost per km from fuel log data

### Document Management

- **FR27:** Driver can store vehicle document records (insurance, license, registration) with expiry dates
- **FR28:** System can alert the driver when a document is expiring within a configurable number of days
- **FR29:** Driver can set custom reminder lead times for each document type

### Data Synchronization

- **FR30:** System can automatically sync data between head unit and phone over Bluetooth when a connection is established
- **FR31:** System can perform delta sync, transferring only records modified since the last successful sync
- **FR32:** System can resolve sync conflicts using last-write-wins based on record timestamps
- **FR33:** System can sync on event triggers (trip end, maintenance logged, fuel entry added)
- **FR34:** System can sync periodically while Bluetooth is connected
- **FR35:** Driver can manually trigger a sync from either device

### Data Integrity & Recovery

- **FR36:** System can recover trip data after abrupt power loss with minimal data loss
- **FR37:** System can detect and recover interrupted trips on startup
- **FR38:** System can maintain database integrity through write-ahead logging
- **FR39:** System can persist critical trip state to a crash recovery journal independent of the main database

### Background Service & Lifecycle

- **FR40:** System can start automatically when the head unit boots
- **FR41:** System can run continuously as a background service without being killed by the operating system
- **FR42:** System can display a persistent notification indicating active tracking status
- **FR43:** System can operate without any network connectivity

### Notifications (Phone)

- **FR44:** System can generate local notifications when maintenance is due within a threshold
- **FR45:** System can generate local notifications when vehicle documents are approaching expiry
- **FR46:** System can check notification thresholds periodically without requiring user interaction

### Statistics & Reporting (Growth)

- **FR47:** Driver can view driving summaries by day, week, month, and year
- **FR48:** Driver can view total distance, fuel cost, and maintenance cost over a period
- **FR49:** Driver can export vehicle data to CSV or PDF
- **FR50:** Driver can share a recorded trip route via a generated map URL

## Non-Functional Requirements

### Performance

- **NFR1:** Trip detection state machine must evaluate GPS data and transition states within 100ms per location update
- **NFR2:** Head unit dashboard must render and display within 2 seconds of app launch
- **NFR3:** Foreground service must reach GPS-tracking-ready state within 15 seconds of boot
- **NFR4:** TripPoint GPS data must be flushed to Room DB within 10 seconds of capture
- **NFR5:** Delta sync of a typical day's data (50 trip points, 1 trip, 0-2 maintenance/fuel records) must complete within 5 seconds over RFCOMM
- **NFR6:** Room DB queries for dashboard data (current ODO, next maintenance, recent trips) must return within 200ms
- **NFR7:** UI frame rendering must maintain 60fps on head unit hardware during normal dashboard operation
- **NFR8:** Memory usage of the foreground service must remain under 100MB during continuous GPS tracking

### Reliability & Data Integrity

- **NFR9:** Maximum GPS track data loss after abrupt power cut must not exceed 10 seconds of recording
- **NFR10:** Maximum trip summary data loss after abrupt power cut must not exceed 30 seconds of accumulated values
- **NFR11:** Database must maintain ACID properties through WAL mode under all power-loss scenarios
- **NFR12:** Crash recovery journal must restore trip state within 5 seconds of reboot
- **NFR13:** Foreground service must maintain continuous operation for 30+ days without memory leaks or ANR events
- **NFR14:** GPS drift rejection must produce zero false trips across 30 days of real-world usage including underground parking
- **NFR15:** Interrupted sync operations must be safely resumable without data corruption or duplication

### Privacy & Security

- **NFR16:** Application must request zero network-related permissions (no INTERNET, no ACCESS_NETWORK_STATE)
- **NFR17:** All user data must reside exclusively on-device (head unit and/or phone local storage)
- **NFR18:** No telemetry, analytics, or crash reporting data may be transmitted to external services
- **NFR19:** Bluetooth RFCOMM connections must use a unique application-specific UUID to prevent unauthorized pairing
- **NFR20:** Room database must not be exported to external storage without explicit user action

### Integration

- **NFR21:** Bluetooth RFCOMM sync must operate without interfering with concurrent audio streaming (A2DP/HFP profiles)
- **NFR22:** `OBDProvider` interface must support addition of new data sources (V2 Bluetooth ELM327) without modifying existing trip tracking or maintenance logic
- **NFR23:** OpenStreetMap tile loading via osmdroid must support offline pre-cached tiles when no network is available
- **NFR24:** App must function correctly on head units both with and without Google Play Services installed

### Resource Efficiency

- **NFR25:** GPS location polling interval must balance accuracy vs CPU impact — 3-second intervals during active tracking, no polling when IDLE
- **NFR26:** Room database size for 1 year of typical usage (15,000 km, 700 trips, 50 fuel logs, 20 maintenance records) must remain under 50MB
- **NFR27:** Crash recovery journal (DataStore) must remain under 1KB per active trip
