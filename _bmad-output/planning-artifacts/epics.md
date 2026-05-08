---
stepsCompleted: [step-01-validate-prerequisites, step-02-design-epics, step-03-create-stories, step-04-final-validation]
inputDocuments: [prd.md, architecture.md, ux-design-specification.md]
---

# RoadMate - Epic Breakdown

## Overview

This document provides the complete epic and story breakdown for RoadMate, decomposing the requirements from the PRD, UX Design, and Architecture requirements into implementable stories.

## Requirements Inventory

### Functional Requirements

FR1: Driver can create a vehicle profile with make, model, year, engine type, engine size, fuel type, plate number, and optional VIN
FR2: Driver can set and update the current odometer reading for a vehicle
FR3: Driver can configure city and highway fuel consumption estimates for a vehicle
FR4: Driver can manage multiple vehicle profiles and switch between them
FR5: Driver can select a pre-built maintenance template when creating a vehicle profile
FR6: Driver can set the odometer unit (km or miles) per vehicle
FR7: System can automatically detect trip start when the vehicle begins sustained movement
FR8: System can automatically detect trip end when the vehicle remains stationary for a configurable timeout period
FR9: System can record trip data including start/end time, distance, duration, max speed, average speed, and estimated fuel consumption
FR10: System can record GPS route points (latitude, longitude, speed, altitude, timestamp) throughout a trip
FR11: System can reject false trip triggers caused by GPS drift when the vehicle is stationary
FR12: System can handle GPS signal gaps and determine whether to resume or end a trip
FR13: Driver can view a list of recorded trips with summary statistics
FR14: System can accumulate GPS-based distance and update the odometer accordingly
FR15: Driver can view all scheduled maintenance items with visual progress indicators showing percentage to next service
FR16: Driver can mark a maintenance item as completed with date, odometer reading, optional cost, location, and notes
FR17: Driver can view the full service history for each maintenance item
FR18: System can predict the estimated date for next maintenance based on average daily driving distance
FR19: Driver can add custom maintenance items beyond the pre-built template
FR20: System can alert the driver when maintenance is due within a configurable km threshold
FR21: Driver can configure maintenance intervals by km and/or months for each item
FR22: Driver can log fuel fill-ups with date, odometer, liters, price per liter, total cost, full tank indicator, and station
FR23: System can calculate actual fuel consumption (L/100km) between full-tank fill-ups
FR24: System can compare actual fuel consumption against estimated consumption
FR25: Driver can view fuel cost trends over time (weekly, monthly, yearly)
FR26: System can calculate cost per km from fuel log data
FR27: Driver can store vehicle document records (insurance, license, registration) with expiry dates
FR28: System can alert the driver when a document is expiring within a configurable number of days
FR29: Driver can set custom reminder lead times for each document type
FR30: System can automatically sync data between head unit and phone over Bluetooth when a connection is established
FR31: System can perform delta sync, transferring only records modified since the last successful sync
FR32: System can resolve sync conflicts using last-write-wins based on record timestamps
FR33: System can sync on event triggers (trip end, maintenance logged, fuel entry added)
FR34: System can sync periodically while Bluetooth is connected
FR35: Driver can manually trigger a sync from either device
FR36: System can recover trip data after abrupt power loss with minimal data loss
FR37: System can detect and recover interrupted trips on startup
FR38: System can maintain database integrity through write-ahead logging
FR39: System can persist critical trip state to a crash recovery journal independent of the main database
FR40: System can start automatically when the head unit boots
FR41: System can run continuously as a background service without being killed by the operating system
FR42: System can display a persistent notification indicating active tracking status
FR43: System can operate without any network connectivity
FR44: System can generate local notifications when maintenance is due within a threshold
FR45: System can generate local notifications when vehicle documents are approaching expiry
FR46: System can check notification thresholds periodically without requiring user interaction
FR47: Driver can view driving summaries by day, week, month, and year (Growth)
FR48: Driver can view total distance, fuel cost, and maintenance cost over a period (Growth)
FR49: Driver can export vehicle data to CSV or PDF (Growth)
FR50: Driver can share a recorded trip route via a generated map URL (Growth)

### NonFunctional Requirements

NFR1: Trip detection state machine must evaluate GPS data and transition states within 100ms per location update
NFR2: Head unit dashboard must render and display within 2 seconds of app launch
NFR3: Foreground service must reach GPS-tracking-ready state within 15 seconds of boot
NFR4: TripPoint GPS data must be flushed to Room DB within 10 seconds of capture
NFR5: Delta sync of a typical day's data (50 trip points, 1 trip, 0-2 maintenance/fuel records) must complete within 5 seconds over RFCOMM
NFR6: Room DB queries for dashboard data (current ODO, next maintenance, recent trips) must return within 200ms
NFR7: UI frame rendering must maintain 60fps on head unit hardware during normal dashboard operation
NFR8: Memory usage of the foreground service must remain under 100MB during continuous GPS tracking
NFR9: Maximum GPS track data loss after abrupt power cut must not exceed 10 seconds of recording
NFR10: Maximum trip summary data loss after abrupt power cut must not exceed 30 seconds of accumulated values
NFR11: Database must maintain ACID properties through WAL mode under all power-loss scenarios
NFR12: Crash recovery journal must restore trip state within 5 seconds of reboot
NFR13: Foreground service must maintain continuous operation for 30+ days without memory leaks or ANR events
NFR14: GPS drift rejection must produce zero false trips across 30 days of real-world usage including underground parking
NFR15: Interrupted sync operations must be safely resumable without data corruption or duplication
NFR16: Application must request zero network-related permissions (no INTERNET, no ACCESS_NETWORK_STATE)
NFR17: All user data must reside exclusively on-device (head unit and/or phone local storage)
NFR18: No telemetry, analytics, or crash reporting data may be transmitted to external services
NFR19: Bluetooth RFCOMM connections must use a unique application-specific UUID to prevent unauthorized pairing
NFR20: Room database must not be exported to external storage without explicit user action
NFR21: Bluetooth RFCOMM sync must operate without interfering with concurrent audio streaming (A2DP/HFP profiles)
NFR22: OBDProvider interface must support addition of new data sources (V2 Bluetooth ELM327) without modifying existing trip tracking or maintenance logic
NFR23: OpenStreetMap tile loading via osmdroid must support offline pre-cached tiles when no network is available
NFR24: App must function correctly on head units both with and without Google Play Services installed
NFR25: GPS location polling interval must balance accuracy vs CPU impact — 3-second intervals during active tracking, no polling when IDLE
NFR26: Room database size for 1 year of typical usage (15,000 km, 700 trips, 50 fuel logs, 20 maintenance records) must remain under 50MB
NFR27: Crash recovery journal (DataStore) must remain under 1KB per active trip

### Additional Requirements

- **Starter/Build System Setup**: Project must be initialized as multi-module Kotlin Gradle project with convention plugins (AndroidApplicationConventionPlugin, AndroidLibraryConventionPlugin, AndroidComposeConventionPlugin, AndroidHiltConventionPlugin, AndroidRoomConventionPlugin) and Version Catalog (libs.versions.toml) following the Now In Android pattern
- **Module Structure**: Three app modules (`:app-headunit`, `:app-phone`, `:core`) plus `:build-logic` included build with strict dependency rules: core never depends on app modules, app modules never depend on each other
- **Technology Stack Versions**: Kotlin 2.3.21, AGP 9.2.1, Compose BOM 2026.04.01, Room 2.8.4 (KSP), Hilt 2.59.2, WorkManager 2.11.2, DataStore 1.2.1, Kotlin Serialization 1.11.0, Play Services Location 21.3.0
- **Data Architecture**: Room entities serve as domain models with UUID string primary keys, `lastModified` epoch millis on every entity, `vehicleId` foreign key on all entities. Sync uses dedicated `@Serializable` DTO classes with extension function mappers
- **Database Configuration**: WAL journal mode, exportSchema = true for auto-migration, single RoadMateDatabase in `:core:database`, DAOs per entity group (VehicleDao, TripDao, MaintenanceDao, FuelDao, DocumentDao)
- **State Management**: Hilt singleton StateFlow pattern for DrivingStateManager, BluetoothStateManager, LocationStateManager — private MutableStateFlow, public read-only StateFlow
- **Service Architecture**: Foreground service (RoadMateService) with auto-start via BOOT_COMPLETED receiver, GPS tracker with 3s/0s interval switching based on driving state, RFCOMM SPP server for sync
- **Sync Protocol**: Entity-level delta sync with length-prefixed JSON over RFCOMM, SYNC_STATUS/PULL/PUSH/ACK messages, TripPoint batching at 100 per message, UUID-based idempotency
- **Navigation**: Jetpack Navigation Compose with @Serializable type-safe routes for phone app, no navigation graph for head unit (single ContextAwareLayout screen)
- **Error Handling**: Timber for logging, Result<T> from repositories, sealed UiState<T> (Loading/Success/Error) for every screen, no LiveData anywhere
- **GPS Location Provider**: FusedLocationProviderClient with fallback to LocationManager for head units without Google Play Services
- **OBD Interface**: OBDProvider interface + MockOBDProvider stub in `:core:obd` for V2 upgrade path
- **Testing Framework**: JUnit 5 for unit tests, Compose UI testing for UI, Espresso for instrumented, Turbine for Flow testing. Test fakes as Fake{Class}, fixtures as {Entity}Fixtures
- **Crash Recovery**: WAL mode + 10s TripPoint flush interval + 30s DataStore crash journal + PowerReceiver for graceful shutdown + BootReceiver for recovery on restart
- **ProGuard/R8 Rules**: Needed for release builds to keep Room entities, Kotlin Serialization, and Hilt-generated code (define in first build story)
- **AndroidManifest Permissions**: Full permission list (BLUETOOTH_CONNECT, BLUETOOTH_SCAN, ACCESS_FINE_LOCATION, ACCESS_BACKGROUND_LOCATION, FOREGROUND_SERVICE, FOREGROUND_SERVICE_LOCATION, RECEIVE_BOOT_COMPLETED, POST_NOTIFICATIONS, WAKE_LOCK) to be declared per module

### UX Design Requirements

UX-DR1: Implement shared RoadMateTheme with custom dark-only M3 ColorScheme — surface (#0A0A0A), surfaceVariant (#1A1A1A), surfaceContainer (#121212), surfaceBright (#242424), primary (#4FC3F7), secondary (#80CBC4), tertiary (#FFB74D), error (#EF5350), success (#66BB6A), onSurface (#E8E8E8), onSurfaceVariant (#9E9E9E), outline (#4A4A4A)
UX-DR2: Implement dual typography scales — head unit scale (displayLarge 48sp, headlineMedium 28sp, titleLarge 22sp, bodyLarge 18sp, labelLarge 16sp) and phone scale (displayLarge 36sp, headlineMedium 24sp, titleMedium 16sp, bodyLarge 16sp, bodyMedium 14sp, labelSmall 11sp) using Inter font with tabular figures for numeric displays
UX-DR3: Implement spacing system with 4dp base unit (space-xs 4dp through space-3xl 32dp), head unit layout grid (12-column, 24dp margins, 16dp gutters), and phone layout grid (4-column, 16dp margins, 8dp gutters)
UX-DR4: Build custom GaugeArc composable — circular progress arc with state-based coloring (teal 0-74%, amber 75-94%, red 95-100%), 600ms fill/reset animation, Large (160dp) and Compact (48dp) variants, accessibility contentDescription, shared via :core:ui module
UX-DR5: Build custom DrivingHUD composable — full-screen minimal HUD for driving mode with ODO displayLarge left-center, trip distance in teal below, recording indicator dot top-center, time top-right, conditional alert strip at bottom edge (amber/red), zero interactive elements
UX-DR6: Build custom ParkedDashboard composable — three-panel landscape layout (left: ODO + status chips + vehicle name, center: recent trips scrollable list max 5, right: maintenance GaugeArc stack of 3), with empty state handling for fresh installs
UX-DR7: Build custom VehicleHeroCard composable — vehicle silhouette icon (64dp) + vehicle name (headlineMedium) + ODO (displayLarge) + sync status label, surfaceVariant background, 4dp radius, with synced/stale/never-synced states
UX-DR8: Build custom AttentionBand composable — full-width 48dp alert strip with amber (warning) or red (critical) background, dark text, tappable (deep-link to detail), swipe-to-dismiss, stacks vertically with max 2 visible + "+N more" label
UX-DR9: Build custom TripLiveIndicator composable — 12dp pulsing green circle (scale 1.0→1.3→1.0 over 1500ms loop) with optional "Recording" label text
UX-DR10: Build custom StatusChip composable — based on M3 AssistChip with icon (24dp) + label, four variants (tracking active/paused, synced, sync failed) with appropriate colors
UX-DR11: Build custom ProgressRing composable — compact 36dp inline circle progress with 4dp stroke, same color logic as GaugeArc but no center text, maintenance type icon below (16dp)
UX-DR12: Build ContextAwareLayout composable — layout container that switches children between DrivingHUD and ParkedDashboard based on DrivingState flow from service layer
UX-DR13: Implement AdaptiveDashboard with BoxWithConstraints for head unit split-screen support — 3 layout breakpoints: full (≥960dp three-panel), compact (480-959dp two-panel), minimal (≤479dp single-column), maintaining 76dp minimum touch targets at all widths
UX-DR14: Implement phone VehicleHub as Tesla-style single-scroll LazyColumn — VehicleHeroCard at top, AttentionBand for alerts, then card stack for maintenance (with ProgressRing inline), recent trips, and fuel log. No tab bar, no bottom navigation
UX-DR15: Implement ModalBottomSheet pattern for all write operations on phone — mark-as-done form, fuel entry form, ODO edit — with pre-filled fields (today's date, current ODO), minimal required fields, inline real-time validation, dark custom styling
UX-DR16: Implement motion and animation system — dashboard mode transition 400ms FastOutSlowIn, progress arc fill/reset 600ms LinearOutSlowIn, trip live indicator pulse 1500ms loop, card expand 250ms, sync pulse 800ms, snackbar enter/exit 200ms. Respect AccessibilityManager.isReduceMotionEnabled
UX-DR17: Implement empty state patterns for all list screens — icon + descriptive message + CTA button where actionable (trips, maintenance, fuel log, phone hub never-synced, parked dashboard fresh install), never show empty whitespace
UX-DR18: Implement loading states as shimmer placeholders (surfaceVariant → surfaceBright oscillation 200-400ms) for card refresh after sync. No spinners, no "Loading..." text — data is always local
UX-DR19: Implement feedback patterns — success Snackbar (4s, green icon, auto-dismiss, undo if reversible), system error Snackbar (6s, red icon, retry action), inline field errors (red border + helper text), StatusChip for persistent system state. Never stack snackbars. No snackbars/toasts on head unit while driving
UX-DR20: Implement accessibility to WCAG 2.1 AA — Compose semantics with contentDescription on all custom components (GaugeArc, DrivingHUD, StatusChip, AttentionBand, VehicleHeroCard, trip/fuel cards), support system font scaling (sp units), reduced motion support, D-pad/rotary focus traversal for head unit parked mode with 1dp primary focus ring
UX-DR21: Implement action hierarchy — primary (filled button, primary color, max 1 per screen), secondary (outlined), tertiary (text-only), destructive (text-only error color behind confirmation), FAB (filled, single "add" action per list screen). Head unit has NO buttons in driving mode, 76dp minimum in parked mode
UX-DR22: Implement notification deep-link navigation — notification tap opens specific detail screen (maintenance item, document) via PendingIntent with Jetpack Navigation deep links
UX-DR23: Implement vehicle switcher — tappable vehicle name in phone top bar → dropdown/sheet with vehicle list. Head unit header includes quick-switch control
UX-DR24: Implement custom vehicle silhouette icon, gauge arc icon, BT sync icon, and trip route icon using Material Symbols Outlined style — 24dp (phone) / 32dp (head unit), with state-based coloring (default onSurfaceVariant, active primary, warning tertiary, error error)
UX-DR25: Implement phone screen structure using Scaffold + TopAppBar for detail screens with back arrow navigation, pull-to-refresh on VehicleHub to trigger manual BT sync, maximum navigation depth of 3 levels (hub → detail → action sheet)

### FR Coverage Map

FR1: Epic 1 - Vehicle profile creation
FR2: Epic 1 - Odometer entry and update
FR3: Epic 1 - Fuel consumption estimates configuration
FR4: Epic 1 / Epic 6 - Multi-vehicle management and switching
FR5: Epic 1 - Maintenance template selection
FR6: Epic 1 - Odometer unit setting (km/miles)
FR7: Epic 2 - Automatic trip start detection
FR8: Epic 2 - Automatic trip end detection
FR9: Epic 2 - Trip data recording (time, distance, duration, speed, fuel estimate)
FR10: Epic 2 - GPS route point recording
FR11: Epic 2 - GPS drift false trip rejection
FR12: Epic 2 - GPS signal gap handling
FR13: Epic 2 / Epic 5 - Trip list with summary stats (head unit / phone)
FR14: Epic 2 - GPS distance accumulation and ODO update
FR15: Epic 3 / Epic 5 - Maintenance items with progress indicators (head unit / phone)
FR16: Epic 3 - Mark maintenance as completed
FR17: Epic 3 - Maintenance service history view
FR18: Epic 3 - Maintenance date prediction from daily driving average
FR19: Epic 3 - Custom maintenance item creation
FR20: Epic 3 - Maintenance due alert at configurable km threshold
FR21: Epic 3 - Configurable maintenance intervals (km and/or months)
FR22: Epic 5 - Fuel fill-up logging
FR23: Epic 5 - Actual fuel consumption calculation
FR24: Epic 5 - Actual vs estimated consumption comparison
FR25: Epic 5 - Fuel cost trends over time
FR26: Epic 5 - Cost per km calculation
FR27: Epic 3 - Vehicle document storage with expiry dates
FR28: Epic 3 - Document expiry alerts
FR29: Epic 3 - Custom reminder lead times for document types
FR30: Epic 4 - Automatic BT sync on connection
FR31: Epic 4 - Delta sync (modified records only)
FR32: Epic 4 - Last-write-wins conflict resolution
FR33: Epic 4 - Event-driven sync triggers
FR34: Epic 4 - Periodic sync while BT connected
FR35: Epic 4 - Manual sync trigger from either device
FR36: Epic 2 - Trip data recovery after power loss
FR37: Epic 2 - Interrupted trip detection and recovery on startup
FR38: Epic 2 - Database integrity via write-ahead logging
FR39: Epic 2 - Crash recovery journal independent of main database
FR40: Epic 1 - Auto-start on head unit boot
FR41: Epic 1 - Continuous background service operation
FR42: Epic 1 - Persistent notification for tracking status
FR43: Epic 1 - Operation without network connectivity
FR44: Epic 3 - Local notification for maintenance due
FR45: Epic 3 - Local notification for document expiry
FR46: Epic 3 - Periodic threshold checking without user interaction
FR47: Epic 7 - Driving summaries by day/week/month/year (Growth)
FR48: Epic 7 - Total distance, fuel cost, maintenance cost over period (Growth)
FR49: Epic 7 - CSV/PDF export (Growth)
FR50: Epic 7 - Share trip route via map URL (Growth)

## Epic List

### Epic 1: Project Foundation & Vehicle Management
The driver can create and manage vehicle profiles, and the head unit foreground service starts automatically on boot with a persistent tracking notification. The system is alive — ready to track. Includes full project build system setup (convention plugins, version catalog, multi-module structure), Room database with all entities and DAOs, shared design system/theme, and OBDProvider interface stub.
**FRs covered:** FR1, FR2, FR3, FR4, FR5, FR6, FR40, FR41, FR42, FR43

### Epic 2: Trip Tracking & Data Integrity
Trips are automatically detected, recorded, and survive power loss. The driver sees trip history with accurate distance, duration, and speed stats. GPS drift in garages produces zero false trips. The odometer updates automatically. The head unit driving mode (DrivingHUD) provides a glanceable HUD with live trip indicator. The driving ↔ parked mode transition works via ContextAwareLayout.
**FRs covered:** FR7, FR8, FR9, FR10, FR11, FR12, FR13, FR14, FR36, FR37, FR38, FR39

### Epic 3: Maintenance & Document Management with Notifications
The driver sees visual progress toward next service via gauge arcs, receives phone notifications before maintenance is due or documents expire, can mark maintenance as done (closing the predict → alert → act → reset loop), and manages vehicle documents with expiry tracking. Includes WorkManager notification scheduling and notification deep-link navigation.
**FRs covered:** FR15, FR16, FR17, FR18, FR19, FR20, FR21, FR27, FR28, FR29, FR44, FR45, FR46

### Epic 4: Bluetooth Sync
Data flows automatically between head unit and phone over the existing Bluetooth RFCOMM link. The phone shows fresh vehicle data without user action. Sync works alongside music streaming, handles interruptions gracefully via idempotent protocol, and supports event-driven, periodic, and manual sync triggers.
**FRs covered:** FR30, FR31, FR32, FR33, FR34, FR35

### Epic 5: Phone Companion App
The driver has a full vehicle management hub on their phone — Tesla-style single-scroll layout with vehicle hero card, attention band alerts, maintenance overview with mini gauges, trip list, fuel log with consumption calculations, document list, and vehicle switching. All write operations use bottom sheets with pre-filled fields.
**FRs covered:** FR13, FR15, FR22, FR23, FR24, FR25, FR26

### Epic 6: Head Unit Dashboard Polish
The head unit parked dashboard is a full three-panel information display with recent trips, maintenance gauges, and vehicle status. Split-screen mode works alongside navigation apps via AdaptiveDashboard with 3 layout breakpoints. The driving ↔ parked transition animation is smooth. Head unit vehicle switcher is available in parked mode.
**FRs covered:** FR4, FR13, FR15

### Epic 7: Statistics, Export & Growth Features
The driver can view driving summaries by time period, cost trends, export vehicle data to CSV/PDF, and share trip routes via map URL. Weekly summary notifications keep the driver informed.
**FRs covered:** FR47, FR48, FR49, FR50

## Epic 1: Project Foundation & Vehicle Management

The driver can create and manage vehicle profiles, and the head unit foreground service starts automatically on boot with a persistent tracking notification. The system is alive — ready to track. Includes full project build system setup (convention plugins, version catalog, multi-module structure), Room database with all entities and DAOs, shared design system/theme, and OBDProvider interface stub.

### Story 1.1: Project Scaffold & Build System

As a developer,
I want a multi-module Kotlin Gradle project with convention plugins and version catalog,
So that all modules share consistent build configuration and I can begin implementing features across `:app-headunit`, `:app-phone`, and `:core`.

**Acceptance Criteria:**

**Given** the project is initialized from scratch
**When** I open it in Android Studio and trigger a Gradle sync
**Then** the project syncs successfully with modules `:app-headunit`, `:app-phone`, `:core`, and `:build-logic`
**And** `settings.gradle.kts` includes all four modules
**And** `gradle/libs.versions.toml` contains all verified dependency versions (Kotlin 2.3.21, AGP 9.2.1, Compose BOM 2026.04.01, Room 2.8.4, Hilt 2.59.2, WorkManager 2.11.2, DataStore 1.2.1, Kotlin Serialization 1.11.0, Play Services Location 21.3.0)

**Given** convention plugins exist in `build-logic/convention/`
**When** any module applies `AndroidApplicationConventionPlugin`, `AndroidLibraryConventionPlugin`, `AndroidComposeConventionPlugin`, `AndroidHiltConventionPlugin`, or `AndroidRoomConventionPlugin`
**Then** shared build config (minSdk 29, compileSdk latest, JVM 17, Compose compiler, KSP) is applied without duplication

**Given** the module dependency graph
**When** I inspect `build.gradle.kts` for each module
**Then** `:app-headunit` depends on `:core`, `:app-phone` depends on `:core`, `:core` has no project dependencies, and app modules do not depend on each other

**Given** the head unit app module
**When** I check the AndroidManifest
**Then** it declares zero network-related permissions (no INTERNET, no ACCESS_NETWORK_STATE) and sets `android:screenOrientation="landscape"`

**Given** the project compiles
**When** I run `./gradlew assembleDebug`
**Then** all three modules build successfully with zero errors

### Story 1.2: Core Data Layer — Vehicle & Maintenance Entities

As a developer,
I want Room database entities, DAOs, and repositories for Vehicle and Maintenance domain,
So that vehicle profiles and maintenance schedules can be persisted and queried.

**Acceptance Criteria:**

**Given** the `:core:database` package exists
**When** `RoadMateDatabase` is initialized
**Then** it uses `JournalMode.WRITE_AHEAD_LOGGING`, `exportSchema = true`, and includes all entity classes

**Given** the `Vehicle` entity
**When** I inspect its definition
**Then** it has fields: `id` (String UUID PK), `name`, `make`, `model`, `year`, `engineType`, `engineSize`, `fuelType`, `plateNumber`, `vin` (nullable), `odometerKm` (Double), `odometerUnit` (km/miles enum), `cityConsumption` (Double), `highwayConsumption` (Double), `lastModified` (Long epoch millis)
**And** the primary key uses `UUID.randomUUID().toString()` as default

**Given** the `MaintenanceSchedule` entity
**When** I inspect its definition
**Then** it has fields: `id` (String UUID PK), `vehicleId` (FK), `name`, `intervalKm` (Int nullable), `intervalMonths` (Int nullable), `lastServiceKm` (Double), `lastServiceDate` (Long), `isCustom` (Boolean), `lastModified` (Long)
**And** it has an index on `vehicleId`

**Given** the `MaintenanceRecord` entity
**When** I inspect its definition
**Then** it has fields: `id` (String UUID PK), `scheduleId` (FK), `vehicleId` (FK), `datePerformed` (Long), `odometerKm` (Double), `cost` (Double nullable), `location` (String nullable), `notes` (String nullable), `lastModified` (Long)

**Given** `VehicleDao` and `MaintenanceDao` exist
**When** I inspect their methods
**Then** all query methods take `vehicleId` as parameter (never unscoped), reactive queries return `Flow<T>`, and write operations use `@Upsert`

**Given** `VehicleRepository` and `MaintenanceRepository` exist
**When** I call write operations
**Then** they return `suspend Result<Unit>` using `runCatching { }`
**And** reactive queries return `Flow<T>` delegating to DAO

**Given** a pre-built maintenance template for Mitsubishi Lancer EX 2015
**When** it is loaded during vehicle profile creation with this template selected
**Then** it creates MaintenanceSchedule records for: Oil Change (10,000 km / 6 months), Oil Filter (10,000 km / 6 months), Air Filter (20,000 km / 12 months), Brake Pads (40,000 km / 24 months), Tire Rotation (10,000 km / 6 months), Coolant (40,000 km / 24 months), Spark Plugs (30,000 km), Transmission Fluid (60,000 km), Brake Fluid (40,000 km / 24 months)

### Story 1.3: Core Data Layer — Trip, Fuel & Document Entities

As a developer,
I want Room entities, DAOs, and repositories for Trip, Fuel, and Document domains plus the OBDProvider interface,
So that all data models are ready for trip tracking, fuel logging, document management, and future OBD-II integration.

**Acceptance Criteria:**

**Given** the `Trip` entity
**When** I inspect its definition
**Then** it has fields: `id` (String UUID PK), `vehicleId` (FK), `startTime` (Long), `endTime` (Long nullable), `distanceKm` (Double), `durationMs` (Long), `maxSpeedKmh` (Double), `avgSpeedKmh` (Double), `estimatedFuelL` (Double), `startOdometerKm` (Double), `endOdometerKm` (Double), `status` (enum: ACTIVE, COMPLETED, INTERRUPTED), `lastModified` (Long)

**Given** the `TripPoint` entity
**When** I inspect its definition
**Then** it has fields: `id` (String UUID PK), `tripId` (FK), `latitude` (Double), `longitude` (Double), `speedKmh` (Double), `altitude` (Double), `accuracy` (Float), `timestamp` (Long), `lastModified` (Long)
**And** it has an index on `tripId`

**Given** the `FuelLog` entity
**When** I inspect its definition
**Then** it has fields: `id` (String UUID PK), `vehicleId` (FK), `date` (Long), `odometerKm` (Double), `liters` (Double), `pricePerLiter` (Double), `totalCost` (Double), `isFullTank` (Boolean), `station` (String nullable), `lastModified` (Long)

**Given** the `Document` entity
**When** I inspect its definition
**Then** it has fields: `id` (String UUID PK), `vehicleId` (FK), `type` (enum: INSURANCE, LICENSE, REGISTRATION, OTHER), `name` (String), `expiryDate` (Long), `reminderDaysBefore` (Int, default 30), `notes` (String nullable), `lastModified` (Long)

**Given** `TripDao`, `FuelDao`, `DocumentDao` exist
**When** I inspect their methods
**Then** all queries are scoped by `vehicleId` or parent FK, reactive queries return `Flow<T>`, and writes use `@Upsert`

**Given** `TripRepository`, `FuelRepository`, `DocumentRepository` exist
**When** I call their methods
**Then** they follow the same `Result<T>` / `Flow<T>` pattern as VehicleRepository

**Given** the `OBDProvider` interface in `:core:obd`
**When** I inspect it
**Then** it defines methods for reading odometer, RPM, fuel rate, coolant temp, battery voltage, and DTC codes — all returning nullable types
**And** `MockOBDProvider` implements the interface with all methods returning null

**Given** sync DTO classes exist in `:core:model:sync`
**When** I inspect them
**Then** `TripSyncDto`, `TripPointSyncDto`, `MaintenanceScheduleSyncDto`, `MaintenanceRecordSyncDto`, `FuelLogSyncDto`, `DocumentSyncDto`, `VehicleSyncDto` are all `@Serializable` with camelCase fields
**And** extension functions `toSyncDto()` and `toEntity()` exist for each pair

### Story 1.4: Shared Design System & Theme

As a developer,
I want a shared RoadMate design system with dark-only theme, typography, spacing, and icon conventions,
So that all UI components across both apps have a consistent automotive look and feel.

**Acceptance Criteria:**

**Given** `RoadMateTheme` composable in `:core:ui:theme`
**When** applied to any screen
**Then** it provides a dark-only M3 `ColorScheme` with exact tokens: surface (#0A0A0A), surfaceVariant (#1A1A1A), surfaceContainer (#121212), primary (#4FC3F7), secondary (#80CBC4), tertiary (#FFB74D), error (#EF5350), onSurface (#E8E8E8), onSurfaceVariant (#9E9E9E), outline (#4A4A4A)
**And** no dynamic color / Material You wallpaper theming is applied

**Given** typography is configured
**When** I inspect the head unit typography scale
**Then** displayLarge is 48sp Bold, headlineMedium is 28sp SemiBold, titleLarge is 22sp Medium, bodyLarge is 18sp Regular, labelLarge is 16sp Medium using Inter font family

**Given** typography is configured
**When** I inspect the phone typography scale
**Then** displayLarge is 36sp Bold, headlineMedium is 24sp SemiBold, titleMedium is 16sp Medium, bodyLarge is 16sp Regular, bodyMedium is 14sp Regular, labelSmall is 11sp Medium using Inter font family

**Given** spacing tokens in `:core:ui:theme`
**When** I use them in composables
**Then** `space-xs` = 4dp, `space-sm` = 8dp, `space-md` = 12dp, `space-lg` = 16dp, `space-xl` = 20dp, `space-2xl` = 24dp, `space-3xl` = 32dp are available as `RoadMateSpacing` object

**Given** shapes are configured
**When** I inspect the shape theme
**Then** card corners use 4dp radius (angular automotive style, not M3 default 12dp)

**Given** `UiState<T>` sealed interface in `:core:model`
**When** I inspect it
**Then** it defines `Loading`, `Success<T>(val data: T)`, and `Error(val message: String)` subtypes

**Given** `DrivingState`, `BtConnectionState`, `GpsState` sealed interfaces in `:core:model`
**When** I inspect them
**Then** `DrivingState` has `Idle`, `Driving(tripId, distance, duration)`, `Stopping(timeSinceStop)`, `GapCheck(gapDuration)` subtypes
**And** `BtConnectionState` has `Connected`, `Disconnected`, `Connecting`, `SyncInProgress`, `SyncFailed` subtypes
**And** `GpsState` has `Acquired(accuracy)`, `Acquiring`, `Unavailable` subtypes

### Story 1.5: Head Unit Foreground Service & Boot Receiver

As a driver,
I want the head unit to automatically start tracking when my car boots,
So that I never have to manually launch the app or worry about it running.

**Acceptance Criteria:**

**Given** the head unit has just booted (car ignition ON)
**When** `ACTION_BOOT_COMPLETED` is broadcast
**Then** `BootReceiver` starts `RoadMateService` as a foreground service within 5 seconds

**Given** `RoadMateService` is starting
**When** the service initializes
**Then** it displays a persistent notification with channel "RoadMate Tracking" and content "RoadMate tracking active"
**And** the notification cannot be dismissed by the user (ongoing)
**And** the service is started with `startForeground()` using `FOREGROUND_SERVICE_LOCATION` type

**Given** `DrivingStateManager`, `BluetoothStateManager`, and `LocationStateManager` are created
**When** injected via Hilt `@Singleton`
**Then** each exposes a public `StateFlow` (read-only) and private `MutableStateFlow`
**And** the initial states are `DrivingState.Idle`, `BtConnectionState.Disconnected`, and `GpsState.Acquiring`

**Given** the service is running
**When** I check its memory usage during idle (no active trip)
**Then** it remains under 50MB

**Given** the service is running without network permissions
**When** I inspect the running app via `adb shell dumpsys package`
**Then** zero network-related permissions are granted

**Given** the head unit is about to shut down
**When** `ACTION_SHUTDOWN` is broadcast
**Then** `PowerReceiver` triggers any pending data flush before the system shuts down

**Given** the `LocationProvider` interface in `:core:location`
**When** Google Play Services is available
**Then** `FusedLocationProvider` is used for GPS acquisition
**When** Google Play Services is NOT available
**Then** `PlatformLocationProvider` (LocationManager fallback) is used instead

### Story 1.6: Vehicle Profile Management UI (Head Unit)

As a driver,
I want to create and manage vehicle profiles on my head unit,
So that I can set up my car's details and maintenance schedule before I start driving.

**Acceptance Criteria:**

**Given** the driver is on the vehicle management screen (parked mode)
**When** they create a new vehicle
**Then** a form with number-only and selection inputs is shown: vehicle name (text), make (text), model (text), year (number), engine type (dropdown), engine size (number), fuel type (dropdown), plate number (text), odometer (number), odometer unit (km/miles toggle)
**And** all touch targets are minimum 76dp (arm's-length automotive standard)

**Given** the driver is creating a vehicle
**When** they reach the maintenance template step
**Then** they can select "Mitsubishi Lancer EX 2015" from available templates or choose "Custom (no template)"
**And** selecting a template pre-populates MaintenanceSchedule records via the repository

**Given** the driver enters the current odometer reading
**When** they input a value (e.g., 87,400)
**Then** the value is stored as the vehicle's `odometerKm` and displayed with locale number formatting on the dashboard

**Given** the driver has multiple vehicles
**When** they use the vehicle switcher
**Then** a list of all vehicle profiles is shown and selecting one updates the active vehicle in DataStore
**And** the dashboard immediately reflects the selected vehicle's data

**Given** the driver edits fuel consumption estimates
**When** they set city and highway L/100km values
**Then** these are persisted to the Vehicle entity and used for estimated fuel calculations on trips

**Given** a vehicle profile exists
**When** the driver views the dashboard shell
**Then** the vehicle name, current ODO (displayLarge), and tracking status (StatusChip) are visible
**And** the dashboard renders within 2 seconds of navigation

### Story 1.7: First-Time Setup Flow (Head Unit)

As a first-time driver,
I want a minimal setup experience that gets tracking running in under 2 minutes,
So that I start getting value immediately without a lengthy configuration process.

**Acceptance Criteria:**

**Given** the app is launched for the first time (no vehicles in database)
**When** the app opens
**Then** a single-screen welcome flow is displayed with: vehicle name input, current odometer input (number keyboard), and a "Start Tracking" primary button
**And** no onboarding tour, no feature walkthrough, no account creation is shown

**Given** the driver enters a vehicle name and odometer reading
**When** they tap "Start Tracking"
**Then** a Vehicle entity is created with the provided name and ODO
**And** the foreground service starts (if not already running)
**And** GPS acquisition begins
**And** the dashboard is displayed showing the vehicle name and ODO within 15 seconds of the tap
**And** the persistent notification reads "RoadMate tracking active"

**Given** the driver has already completed setup (at least one vehicle exists)
**When** the app launches on subsequent boots
**Then** the welcome flow is skipped entirely and the dashboard is shown directly with the last active vehicle

**Given** the setup flow is displayed
**When** I measure the total fields required
**Then** only 2 inputs are required (vehicle name, ODO) and 1 tap (Start Tracking)
**And** the flow can be completed in under 60 seconds

**Given** the setup flow completes
**When** no maintenance template was selected
**Then** the vehicle is created with zero maintenance schedules (the driver can add them later)

## Epic 2: Trip Tracking & Data Integrity

Trips are automatically detected, recorded, and survive power loss. The driver sees trip history with accurate distance, duration, and speed stats. GPS drift in garages produces zero false trips. The odometer updates automatically. The head unit driving mode (DrivingHUD) provides a glanceable HUD with live trip indicator. The driving ↔ parked mode transition works via ContextAwareLayout.

### Story 2.1: GPS Tracker & Location Pipeline

As a developer,
I want a GPS location pipeline that captures position data at configurable intervals with speed and accuracy metadata,
So that the trip detection system and trip recorder have reliable location data to work with.

**Acceptance Criteria:**

**Given** the foreground service is running and GPS permissions are granted
**When** `GpsTracker` is started
**Then** it requests location updates at 3-second intervals during active tracking
**And** each location update contains latitude, longitude, speed (m/s converted to km/h), altitude, accuracy (meters), and timestamp

**Given** the driving state is `Idle`
**When** GpsTracker evaluates the polling interval
**Then** it uses 0-second interval (no active polling) to conserve CPU
**And** it relies on passive location updates or significant motion detection to wake

**Given** the driving state transitions to `Driving`
**When** GpsTracker evaluates the polling interval
**Then** it switches to 3-second active polling interval within 100ms of state change

**Given** a location update arrives
**When** the accuracy is worse than 50 meters
**Then** the update is tagged with `isLowAccuracy = true` but still passed to downstream consumers
**And** downstream consumers (trip detector, trip recorder) decide how to handle it

**Given** `FusedLocationProvider` fails (no Play Services)
**When** `PlatformLocationProvider` fallback is used
**Then** location updates are still delivered at the requested interval using `LocationManager.GPS_PROVIDER`
**And** the switch is transparent to GpsTracker consumers

**Given** GpsTracker is running for 24+ hours
**When** I check for memory leaks
**Then** no location callback references are leaked and memory remains stable

### Story 2.2: Trip Detection State Machine

As a driver,
I want the system to automatically detect when I start and stop driving,
So that trips are recorded without me pressing any buttons.

**Acceptance Criteria:**

**Given** the vehicle is stationary (state: `Idle`)
**When** GPS reports speed above 8 km/h for 3 consecutive readings (9 seconds of sustained movement)
**Then** the state transitions to `Driving` and a new Trip entity is created with status `ACTIVE`

**Given** the vehicle is moving (state: `Driving`)
**When** GPS reports speed below 3 km/h for the configurable stop timeout (default: 120 seconds)
**Then** the state transitions to `Stopping` and a countdown begins

**Given** the vehicle is in `Stopping` state
**When** the full stop timeout elapses without speed exceeding 8 km/h
**Then** the state transitions to `Idle`, the active Trip is finalized with status `COMPLETED`, and `endTime` is set to when the vehicle first stopped (not when the timeout elapsed)

**Given** the vehicle is in `Stopping` state
**When** speed exceeds 8 km/h before the timeout elapses
**Then** the state transitions back to `Driving` and the existing trip continues (no new trip created)

**Given** the vehicle is parked in an underground garage
**When** GPS drift causes occasional position changes with speed < 5 km/h and accuracy > 30 meters
**Then** the state machine remains in `Idle` and no false trip is created
**And** zero false trips are produced across 30 days of real-world usage including parking scenarios

**Given** the state machine processes a location update
**When** I measure the evaluation time
**Then** the state transition decision completes within 100ms per location update

**Given** the state machine transitions
**When** `DrivingStateManager.drivingState` is updated
**Then** all collectors of the `StateFlow` receive the new state within one frame (16ms)

### Story 2.3: Trip Recording & TripPoint Persistence

As a driver,
I want each trip to record my route, distance, speed, and duration automatically,
So that I have an accurate log of every journey.

**Acceptance Criteria:**

**Given** the state machine enters `Driving` and a Trip is created
**When** location updates arrive during the trip
**Then** `TripPoint` entities are created with tripId, lat, lng, speed, altitude, accuracy, and timestamp
**And** TripPoints are buffered in memory and flushed to Room every 10 seconds

**Given** a trip is active
**When** each TripPoint is recorded
**Then** the trip's accumulated `distanceKm` is updated using the Haversine formula between consecutive valid points
**And** `durationMs` is updated from startTime to current timestamp
**And** `maxSpeedKmh` is updated if the current speed exceeds the stored maximum
**And** `avgSpeedKmh` is recalculated as `distanceKm / (durationMs / 3600000)`

**Given** a trip is active and the vehicle has city/highway consumption estimates
**When** the trip distance increases
**Then** `estimatedFuelL` is calculated as `distanceKm * (cityConsumption / 100)` (city estimate used as default)

**Given** TripPoints are buffered in memory
**When** the 10-second flush timer fires
**Then** all buffered TripPoints are written to Room in a single transaction
**And** the Trip entity's summary fields (distance, duration, max/avg speed, fuel estimate) are updated in the same transaction
**And** the flush completes without blocking the UI thread

**Given** a TripPoint has accuracy worse than 50 meters
**When** the distance calculator evaluates it
**Then** the point is excluded from distance calculation but still persisted for route completeness

**Given** the trip is finalized (state → Idle)
**When** the Trip entity is completed
**Then** `endTime` is set, `status` changes to `COMPLETED`, `endOdometerKm` is set to `startOdometerKm + distanceKm`
**And** any remaining buffered TripPoints are flushed immediately

### Story 2.4: Crash Recovery Journal & Power Loss Protection

As a driver,
I want my trip data to survive sudden power cuts (car ignition off),
So that I never lose more than a few seconds of driving data.

**Acceptance Criteria:**

**Given** a trip is active
**When** the crash recovery journal writes
**Then** DataStore persists the current trip state every 30 seconds: `tripId`, `vehicleId`, `currentDistanceKm`, `currentDurationMs`, `lastKnownOdometerKm`, `lastFlushTimestamp`, `status = ACTIVE`
**And** the journal file remains under 1KB

**Given** the head unit loses power abruptly (car ignition off, no graceful shutdown)
**When** the system reboots and `BootReceiver` triggers
**Then** `CrashRecoveryManager` reads the DataStore journal
**And** if a trip with `status = ACTIVE` is found in the journal, it checks the Room database

**Given** crash recovery finds an active trip in the journal
**When** Room has the Trip entity with status `ACTIVE`
**Then** the Trip is finalized using journal data: `endTime = lastFlushTimestamp`, `status = INTERRUPTED`, distance/duration from journal values
**And** the vehicle's odometer is updated to `lastKnownOdometerKm`
**And** the journal is cleared after successful recovery

**Given** crash recovery completes
**When** I measure the recovery time
**Then** full state restoration completes within 5 seconds of reboot

**Given** an abrupt power cut occurs
**When** TripPoints were buffered but not flushed
**Then** maximum data loss is 10 seconds of GPS points (one flush interval)
**And** maximum trip summary data loss is 30 seconds (one journal write interval)

**Given** `PowerReceiver` detects `ACTION_SHUTDOWN`
**When** a trip is active
**Then** all buffered TripPoints are flushed to Room immediately
**And** the crash recovery journal is updated with current trip state
**And** the Trip is finalized gracefully with status `COMPLETED`

**Given** Room is configured with WAL journal mode
**When** a write transaction is interrupted by power loss
**Then** the database recovers to the last consistent state on next open without corruption

### Story 2.5: Odometer Auto-Update & Trip List (Head Unit)

As a driver,
I want the odometer to update automatically after each trip and to see a list of my past trips,
So that I always know my current mileage and can review my driving history.

**Acceptance Criteria:**

**Given** a trip is completed (status: `COMPLETED` or `INTERRUPTED`)
**When** the trip is finalized
**Then** the vehicle's `odometerKm` is updated by adding the trip's `distanceKm`
**And** the vehicle's `lastModified` timestamp is updated

**Given** the driver is in parked mode on the head unit
**When** they view the trip list section
**Then** completed trips are shown as cards with: date, distance (km), duration (h:mm), average speed (km/h)
**And** trips are sorted by most recent first
**And** interrupted trips show an "⚡ Interrupted" label

**Given** no trips have been recorded yet
**When** the trip list is displayed
**Then** an empty state is shown: car icon + "No trips recorded yet. Drive to start tracking." in `onSurfaceVariant` color

**Given** the dashboard displays the vehicle ODO
**When** a trip completes while the dashboard is visible
**Then** the ODO number updates in place without a full screen refresh
**And** the latest trip appears at the top of the trip list

**Given** the driver has many trips
**When** the trip list is scrolled
**Then** scrolling maintains 60fps on head unit hardware
**And** trip cards use locale formatting for numbers (e.g., 1,234.5 km)

### Story 2.6: DrivingHUD & ContextAwareLayout

As a driver,
I want a minimal heads-up display while driving and an automatic switch between driving and parked views,
So that I can glance at essential information without distraction.

**Acceptance Criteria:**

**Given** the driving state is `Driving`
**When** `ContextAwareLayout` evaluates the current state
**Then** it displays `DrivingHUD` as the full-screen content

**Given** `DrivingHUD` is displayed
**When** I inspect the layout
**Then** the ODO number is shown in `displayLarge` (48sp Bold) left-center
**And** the trip distance is shown in `titleLarge` (22sp) teal (#80CBC4) below the ODO
**And** `TripLiveIndicator` (12dp pulsing green circle, 1.0→1.3→1.0 scale over 1500ms) is shown top-center
**And** current time is shown in `titleLarge` top-right
**And** zero interactive elements (no buttons, no tappable areas) are present

**Given** the driving state transitions from `Idle` to `Driving`
**When** `ContextAwareLayout` switches views
**Then** the transition animation uses `FastOutSlowIn` over 400ms
**And** if `AccessibilityManager.isReduceMotionEnabled` is true, the switch is instant (no animation)

**Given** GPS is acquiring signal (state: `GpsState.Acquiring`)
**When** `DrivingHUD` is displayed
**Then** `TripLiveIndicator` shows a gray static circle (not pulsing) instead of green

**Given** a maintenance item is critical (≥95% used)
**When** `DrivingHUD` is displayed
**Then** an alert strip appears at the bottom edge with red (#EF5350) background and single-line text describing the item
**And** the strip is full width, not interactive

**Given** no alerts exist
**When** `DrivingHUD` is displayed
**Then** the alert strip is hidden (no reserved space)

**Given** the driving state transitions from `Driving` to `Idle`
**When** `ContextAwareLayout` switches views
**Then** it displays the parked view (dashboard shell from Story 1.6) with the same 400ms transition

### Story 2.7: GPS Gap Handling & Edge Cases

As a driver,
I want the system to handle tunnels, garages, and GPS signal gaps intelligently,
So that my trip isn't incorrectly split or ended when I temporarily lose signal.

**Acceptance Criteria:**

**Given** a trip is active (state: `Driving`)
**When** GPS updates stop arriving for more than 30 seconds
**Then** the state transitions to `GapCheck(gapDuration)` and the trip continues (not ended)
**And** trip duration continues accumulating but distance does not

**Given** the state is `GapCheck`
**When** GPS signal returns within 5 minutes with speed above 8 km/h
**Then** the state transitions back to `Driving` and the trip resumes normally
**And** the distance between the last valid point before the gap and the first valid point after is calculated and added (if the straight-line distance is plausible given elapsed time and reasonable speed)

**Given** the state is `GapCheck`
**When** GPS signal returns within 5 minutes with speed below 3 km/h
**Then** the state transitions to `Stopping` (the vehicle may have parked during the gap)

**Given** the state is `GapCheck`
**When** the gap exceeds 5 minutes without any GPS signal
**Then** the trip is ended with the last valid GPS point as the endpoint
**And** the Trip status is set to `INTERRUPTED` with the reason "GPS signal lost"

**Given** GPS signal returns after a gap
**When** the straight-line distance between last-valid and first-returned points implies speed > 200 km/h (implausible teleport)
**Then** the gap distance is discarded (not added to trip distance)
**And** the points are still persisted for route display but flagged as gap-boundary points

**Given** the vehicle exits an underground parking garage
**When** GPS gradually improves from low accuracy (>50m) to high accuracy (<10m)
**Then** trip recording resumes using only points with accuracy better than 50m for distance calculation
**And** no false distance spike is recorded from the initial inaccurate readings

## Epic 3: Maintenance & Document Management with Notifications

The driver sees visual progress toward next service via gauge arcs, receives phone notifications before maintenance is due or documents expire, can mark maintenance as done (closing the predict → alert → act → reset loop), and manages vehicle documents with expiry tracking. Includes WorkManager notification scheduling and notification deep-link navigation.

### Story 3.1: Maintenance Progress Calculation & GaugeArc Component

As a driver,
I want to see a visual gauge showing how close each maintenance item is to being due,
So that I can tell at a glance which services need attention.

**Acceptance Criteria:**

**Given** a MaintenanceSchedule has `intervalKm = 10000` and the vehicle has driven 7500 km since `lastServiceKm`
**When** the progress is calculated
**Then** the percentage is 75% (7500 / 10000)
**And** if `intervalMonths` is also set, the higher of the two percentages is used

**Given** the `GaugeArc` composable (Large variant, 160dp diameter)
**When** rendered with a percentage between 0-74%
**Then** the arc is colored `secondary` (#80CBC4) with the percentage number as center text

**Given** the `GaugeArc` composable
**When** rendered with a percentage between 75-94%
**Then** the arc is colored `tertiary` (#FFB74D) with the percentage as center text

**Given** the `GaugeArc` composable
**When** rendered with a percentage between 95-100%
**Then** the arc is colored `error` (#EF5350) with the percentage as center text
**And** a subtle pulse animation loops (1.5s duration)

**Given** the `GaugeArc` composable
**When** a maintenance item is reset (completed)
**Then** the arc animates from the previous percentage to 0% over 600ms using `LinearOutSlowIn`

**Given** `GaugeArc` Compact variant (48dp diameter)
**When** rendered
**Then** it displays the same color logic as Large but without center text

**Given** any `GaugeArc` is rendered
**When** TalkBack is active
**Then** the contentDescription reads: "[Item name]: [X]% complete, [Y km] remaining until due"

**Given** `AccessibilityManager.isReduceMotionEnabled` is true
**When** `GaugeArc` renders
**Then** all animations are disabled — arc shows end state instantly, no pulse on critical items

### Story 3.2: Maintenance Completion Flow

As a driver,
I want to mark a maintenance item as done and record the service details,
So that the progress resets and my service history is kept up to date.

**Acceptance Criteria:**

**Given** the driver taps a maintenance item to mark as complete
**When** the action triggers
**Then** a `ModalBottomSheet` slides up with pre-filled fields: today's date (DatePicker), current vehicle ODO (number input)
**And** optional fields: cost (number input), location (text input), notes (text input)
**And** the bottom sheet uses dark styling consistent with `surfaceContainer` (#121212)

**Given** the driver fills required fields (date, ODO)
**When** they tap the "Save" primary button
**Then** a `MaintenanceRecord` entity is created with the entered values
**And** the parent `MaintenanceSchedule` is updated: `lastServiceKm = enteredODO`, `lastServiceDate = enteredDate`
**And** the `GaugeArc` animates from the old percentage to 0%
**And** a success Snackbar shows "Service recorded" for 4 seconds with an "Undo" action

**Given** the driver taps "Undo" on the success Snackbar
**When** within the 4-second window
**Then** the MaintenanceRecord is deleted and the MaintenanceSchedule reverts to its previous values

**Given** the driver enters an ODO value lower than the vehicle's current ODO
**When** inline validation runs
**Then** the ODO field shows a red border with helper text: "Odometer must be greater than or equal to last recorded value"
**And** the Save button remains disabled

**Given** the required fields are empty
**When** the form is displayed
**Then** the Save button is disabled until date and ODO are valid

**Given** the bottom sheet is open
**When** the driver swipes down
**Then** the sheet dismisses without saving (implicit cancel, no explicit Cancel button)

### Story 3.3: Custom Maintenance Items & Interval Configuration

As a driver,
I want to add my own maintenance items and configure service intervals,
So that I can track services specific to my vehicle beyond the pre-built template.

**Acceptance Criteria:**

**Given** the driver is on the maintenance list screen
**When** they tap the FAB ("+") button
**Then** a `ModalBottomSheet` appears with fields: item name (text, required), interval in km (number, optional), interval in months (number, optional), last service date (DatePicker, defaults to today), last service km (number, defaults to current ODO)

**Given** the driver creates a custom maintenance item
**When** they provide at least a name and one interval (km or months)
**Then** a `MaintenanceSchedule` entity is created with `isCustom = true`
**And** it appears in the maintenance list immediately with a fresh GaugeArc at 0%

**Given** the driver creates an item with both km and month intervals
**When** progress is calculated
**Then** the higher percentage of the two triggers the display color
**And** the alert text mentions the trigger that's closer to due

**Given** no maintenance items exist for the vehicle
**When** the maintenance list is displayed
**Then** an empty state shows: wrench icon + "No maintenance items. Add your first service item." + primary "Add" button

**Given** an existing maintenance item (template or custom)
**When** the driver wants to edit intervals
**Then** they can modify `intervalKm` and `intervalMonths` via an edit action
**And** progress recalculates immediately with the new intervals

**Given** the driver deletes a custom maintenance item
**When** they confirm via a destructive confirmation dialog
**Then** the MaintenanceSchedule and all associated MaintenanceRecords are deleted
**And** a success Snackbar confirms "Item deleted"

### Story 3.4: Maintenance Prediction & Alerting Logic

As a driver,
I want the system to predict when maintenance will be due and alert me proactively,
So that I can plan service visits before items become overdue.

**Acceptance Criteria:**

**Given** a vehicle has recorded trips over the past 30 days
**When** the average daily distance is calculated
**Then** it uses `totalDistanceLast30Days / 30` as the daily average
**And** if fewer than 7 days of data exist, it falls back to a configurable default (50 km/day)

**Given** a MaintenanceSchedule with `intervalKm = 10000` and 2500 km remaining
**When** the daily average is 80 km/day
**Then** the predicted date is `today + (2500 / 80)` = ~31 days from now

**Given** a MaintenanceSchedule has both km and month intervals
**When** prediction is calculated
**Then** the earlier of the two predicted dates is used as the estimated due date

**Given** a maintenance item is within the configurable km threshold (default: 500 km)
**When** the alert check runs
**Then** an `AttentionBand` appears on the head unit parked dashboard (amber for warning, red for critical)
**And** the band text reads: "[Item name] due in [X km]"
**And** tapping the band navigates to the maintenance detail screen

**Given** multiple alerts exist simultaneously
**When** the `AttentionBand` stack is rendered
**Then** maximum 2 bands are visible with a "+N more" label for additional alerts
**And** the most critical (highest percentage) items are shown first

**Given** an `AttentionBand` is displayed
**When** the driver swipes to dismiss
**Then** the alert is deferred (hidden from view) but not resolved
**And** it reappears on the next app launch or sync

**Given** a maintenance item reaches 100%
**When** the item is still not marked as complete
**Then** it continues showing at 100% with the critical (red) color and pulse animation
**And** the text changes to "[Item name] overdue by [X km]"

### Story 3.5: Document Management & Expiry Tracking

As a driver,
I want to store my vehicle documents with expiry dates and get reminders before they expire,
So that I never drive with expired insurance, license, or registration.

**Acceptance Criteria:**

**Given** the driver navigates to the document management screen
**When** they tap the FAB ("+") to add a document
**Then** a `ModalBottomSheet` appears with: document type (dropdown: Insurance, License, Registration, Other), name (text, required), expiry date (DatePicker, required), reminder days before (number, default 30), notes (optional)

**Given** the driver creates a document
**When** they save with valid required fields
**Then** a `Document` entity is created and appears in the document list
**And** documents are sorted by expiry date (soonest first)

**Given** a document has an expiry date in the future
**When** the document list is displayed
**Then** each document card shows: type icon, name, expiry date, and days until expiry
**And** documents expiring within the reminder window are highlighted with `tertiary` color
**And** expired documents are highlighted with `error` color

**Given** a document's expiry date has passed
**When** the document list is displayed
**Then** the card shows "Expired [N days] ago" in `error` color

**Given** the driver wants to update a document (e.g., renewed insurance)
**When** they edit the document
**Then** they can update the expiry date and all other fields
**And** the reminder schedule recalculates based on the new expiry date

**Given** the driver wants to set different reminder lead times
**When** they set `reminderDaysBefore` to 14 for their license
**Then** the notification for that document fires 14 days before expiry (not the default 30)

### Story 3.6: Notification System (WorkManager)

As a driver,
I want to receive phone notifications when maintenance is due or documents are expiring,
So that I'm alerted even when I'm not actively using the app.

**Acceptance Criteria:**

**Given** the phone app is installed
**When** it initializes
**Then** two notification channels are created: "Maintenance Alerts" (high importance) and "Document Reminders" (default importance)

**Given** a `PeriodicWorkRequest` is enqueued
**When** it executes (every 12 hours, flex 6 hours)
**Then** `NotificationCheckWorker` queries all MaintenanceSchedules and Documents for the active vehicle

**Given** a maintenance item is within the alert threshold (configurable, default 500 km)
**When** the notification worker evaluates it
**Then** a notification is posted: title "[Vehicle name] - Maintenance Due", body "[Item name] is due in [X km]"
**And** the notification has a `PendingIntent` that deep-links to the maintenance detail screen via Jetpack Navigation

**Given** a document is within its `reminderDaysBefore` window
**When** the notification worker evaluates it
**Then** a notification is posted: title "[Vehicle name] - Document Expiring", body "[Document name] expires in [X days]"
**And** the notification has a `PendingIntent` that deep-links to the document detail screen

**Given** the notification worker has already posted a notification for an item
**When** the same item is still within threshold on the next check
**Then** the notification is not duplicated (use consistent notification IDs per schedule/document)

**Given** the phone app requests `POST_NOTIFICATIONS` permission
**When** the user denies permission
**Then** notifications are not posted but all other functionality works normally
**And** no crash or error occurs

**Given** the worker runs
**When** no items are within their thresholds
**Then** no notifications are posted and the worker completes successfully

### Story 3.7: Maintenance Service History View

As a driver,
I want to see the full service history for each maintenance item,
So that I can track past services, costs, and plan future maintenance.

**Acceptance Criteria:**

**Given** the driver taps a maintenance item
**When** the detail screen opens
**Then** it shows: item name, current progress (GaugeArc Large variant), predicted next service date, interval configuration (km/months), and a scrollable list of all `MaintenanceRecord` entries

**Given** service history records exist
**When** they are displayed
**Then** each record shows: date performed, ODO at service, cost (if recorded), location (if recorded), and notes (if recorded)
**And** records are sorted by most recent first

**Given** a maintenance item has no service history
**When** the history section is displayed
**Then** it shows: "No service records yet. Mark as done after your next service."

**Given** the driver views the detail screen
**When** the data loads
**Then** the query returns within 200ms (Room query performance requirement)

**Given** multiple service records with costs exist
**When** the detail screen is displayed
**Then** a "Total spent" summary is shown at the top of the history list (sum of all non-null costs)

**Given** the driver is on the maintenance detail screen
**When** they tap the primary action button
**Then** the mark-as-done flow from Story 3.2 is triggered (ModalBottomSheet)

## Epic 4: Bluetooth Sync

Data flows automatically between head unit and phone over the existing Bluetooth RFCOMM link. The phone shows fresh vehicle data without user action. Sync works alongside music streaming, handles interruptions gracefully via idempotent protocol, and supports event-driven, periodic, and manual sync triggers.

### Story 4.1: RFCOMM Server & Client Connection

As a developer,
I want a reliable Bluetooth RFCOMM connection between the head unit and phone,
So that a communication channel exists for syncing data between both devices.

**Acceptance Criteria:**

**Given** the head unit app is running
**When** `BluetoothSyncServer` starts
**Then** it opens an RFCOMM SPP server socket using a unique application-specific UUID
**And** it listens for incoming connections on a background thread (not main thread)
**And** the server is started as part of `RoadMateService` lifecycle

**Given** the phone app is running and Bluetooth is enabled
**When** `BluetoothSyncClient` scans for the paired head unit
**Then** it identifies the head unit from the bonded devices list by matching the app-specific UUID
**And** it connects to the RFCOMM server socket

**Given** an RFCOMM connection is established
**When** `BluetoothStateManager` is updated
**Then** the state transitions to `BtConnectionState.Connected`
**And** both devices can read and write to the RFCOMM `InputStream`/`OutputStream`

**Given** the RFCOMM connection drops unexpectedly (device out of range, BT toggled off)
**When** the disconnection is detected
**Then** `BluetoothStateManager` transitions to `BtConnectionState.Disconnected`
**And** the client attempts automatic reconnection with exponential backoff (2s, 4s, 8s, max 30s)
**And** no crash or ANR occurs

**Given** the phone is connected to the head unit for audio (A2DP profile) and/or calls (HFP profile)
**When** the RFCOMM SPP connection is established simultaneously
**Then** audio streaming and phone calls are not interrupted or degraded
**And** RFCOMM data transfer operates on its own channel independent of media profiles

**Given** `BLUETOOTH_CONNECT` and `BLUETOOTH_SCAN` permissions are required
**When** the phone app requests them at runtime
**Then** a rationale explaining "Connect to your head unit for data sync" is shown
**And** if permissions are denied, sync functionality is disabled but all other features work normally

**Given** multiple devices are bonded
**When** the client needs to identify the head unit
**Then** it filters bonded devices by the app-specific UUID service record
**And** if no matching device is found, `BluetoothStateManager` stays `Disconnected` with no error toast

### Story 4.2: Delta Sync Protocol

As a developer,
I want an efficient delta sync protocol that transfers only modified records,
So that sync completes quickly and minimizes data transfer over the Bluetooth link.

**Acceptance Criteria:**

**Given** an RFCOMM connection is established
**When** sync begins
**Then** the initiator sends a `SYNC_STATUS` message containing `lastSyncTimestamp` (epoch millis) for each entity type (Vehicle, Trip, TripPoint, MaintenanceSchedule, MaintenanceRecord, FuelLog, Document)

**Given** the receiver gets a `SYNC_STATUS` message
**When** it processes the timestamps
**Then** it queries Room for all entities with `lastModified > receivedTimestamp` per entity type
**And** it sends `PUSH` messages containing the modified entities as `@Serializable` DTOs

**Given** a `PUSH` message is sent
**When** the message is formatted
**Then** it uses length-prefixed JSON: 4-byte big-endian length header followed by UTF-8 JSON payload
**And** the message includes `entityType` (string), `data` (array of DTOs), and `messageId` (UUID)

**Given** TripPoints need to be synced
**When** more than 100 TripPoints are pending
**Then** they are batched into messages of 100 TripPoints each
**And** each batch has its own `messageId` for individual acknowledgment

**Given** the receiver processes a `PUSH` message
**When** entities are written to the local Room database
**Then** an `ACK` message is sent back with the `messageId` confirming successful receipt
**And** entities are upserted using `@Upsert` (insert or update by UUID primary key)

**Given** a sync session completes with all ACKs received
**When** both sides have exchanged all modified entities
**Then** `lastSyncTimestamp` is updated to `System.currentTimeMillis()` on both devices
**And** `BluetoothStateManager` transitions from `SyncInProgress` to `Connected`

**Given** a typical day's data (50 TripPoints, 1 Trip, 0-2 maintenance/fuel records)
**When** delta sync runs
**Then** the entire sync completes within 5 seconds over RFCOMM

### Story 4.3: Conflict Resolution & Idempotency

As a developer,
I want sync to handle conflicts and retries without corrupting or duplicating data,
So that both devices maintain consistent and accurate records.

**Acceptance Criteria:**

**Given** both devices have modified the same entity (same UUID) since the last sync
**When** the receiver processes the incoming `PUSH` with a conflicting entity
**Then** the entity with the higher `lastModified` timestamp wins (last-write-wins)
**And** the losing version is silently overwritten

**Given** a `PUSH` message is received with a UUID that already exists locally
**When** the local entity has a higher `lastModified` than the incoming entity
**Then** the incoming entity is discarded (local version preserved)

**Given** a `PUSH` message is received with a UUID that already exists locally
**When** the local entity has a lower `lastModified` than the incoming entity
**Then** the incoming entity overwrites the local version via `@Upsert`

**Given** the sync connection drops mid-transfer
**When** a `PUSH` message was sent but no `ACK` was received
**Then** the unacknowledged message is retransmitted on the next sync session
**And** the receiver handles the duplicate `PUSH` idempotently (UUID-based upsert produces same result)

**Given** a `PUSH` message is received twice (duplicate due to retry)
**When** the receiver processes it
**Then** the result is identical to processing it once (idempotent upsert by UUID)
**And** no duplicate entities are created

**Given** sync is interrupted after partial completion
**When** the next sync session begins
**Then** it uses the stored `lastSyncTimestamp` which was NOT updated (no ACK for incomplete sync)
**And** all unsynced entities from the interrupted session are re-evaluated

### Story 4.4: Event & Periodic Sync Triggers

As a driver,
I want data to sync automatically when something happens or on a regular schedule,
So that my phone always has the latest vehicle data without me thinking about it.

**Acceptance Criteria:**

**Given** a trip ends (status changes to `COMPLETED` or `INTERRUPTED`)
**When** the Bluetooth connection is active
**Then** a sync is triggered within 5 seconds of trip completion
**And** the completed trip and its TripPoints are included in the delta

**Given** a maintenance item is marked as done
**When** the Bluetooth connection is active
**Then** a sync is triggered within 5 seconds
**And** the new MaintenanceRecord and updated MaintenanceSchedule are included

**Given** a fuel entry is added on the phone
**When** the Bluetooth connection is active
**Then** a sync is triggered within 5 seconds
**And** the new FuelLog is pushed to the head unit

**Given** the Bluetooth connection is active and no event-driven sync has occurred
**When** the periodic timer fires (every 15 minutes)
**Then** a delta sync runs to catch any missed changes

**Given** a sync is already in progress
**When** another sync trigger fires (event or periodic)
**Then** the new trigger is queued and processed after the current sync completes
**And** no concurrent sync operations run simultaneously

**Given** the driver pulls down on the phone VehicleHub (pull-to-refresh gesture)
**When** the Bluetooth connection is active
**Then** a manual sync is triggered immediately
**And** the pull-to-refresh indicator shows until sync completes

**Given** the Bluetooth connection is not active
**When** any sync trigger fires
**Then** the trigger is silently ignored (no error, no retry)
**And** sync will happen naturally when the connection is re-established

### Story 4.5: Sync UI — StatusChip & Shimmer Refresh

As a driver,
I want to see the sync status at a glance and have the UI refresh smoothly after sync,
So that I know my data is current and trust the system is working.

**Acceptance Criteria:**

**Given** the Bluetooth connection is active and idle
**When** `StatusChip` for sync is displayed
**Then** it shows BT icon + "Synced [X] ago" in `primary` color
**And** the time ago updates in real-time (e.g., "just now" → "2m ago" → "5m ago")

**Given** a sync is in progress
**When** `BluetoothStateManager.state` is `SyncInProgress`
**Then** `StatusChip` shows BT icon + "Syncing..." with a subtle pulse animation (800ms loop)
**And** `BtConnectionState` is reflected within one frame (16ms) on all collectors

**Given** a sync fails
**When** `BluetoothStateManager.state` is `SyncFailed`
**Then** `StatusChip` shows BT icon + "!" + "Sync failed" in `error` color
**And** the chip reverts to the last successful sync time after 10 seconds

**Given** Bluetooth is disconnected
**When** `StatusChip` for sync is displayed
**Then** it shows BT icon (outlined) + "Not connected" in `onSurfaceVariant` color

**Given** a sync completes and new data was received
**When** the dashboard cards refresh with updated data
**Then** cards show a shimmer skeleton placeholder (`surfaceVariant` → `surfaceBright` oscillation) for 200-400ms
**And** then populate with the new data

**Given** a sync completes with no new data
**When** the dashboard is displayed
**Then** no shimmer is shown and cards remain unchanged
**And** only the StatusChip timestamp updates

**Given** the head unit is in driving mode
**When** a sync completes
**Then** no snackbar, toast, or modal is shown — only the `StatusChip` updates silently

## Epic 5: Phone Companion App

The driver has a full vehicle management hub on their phone — Tesla-style single-scroll layout with vehicle hero card, attention band alerts, maintenance overview with mini gauges, trip list, fuel log with consumption calculations, document list, and vehicle switching. All write operations use bottom sheets with pre-filled fields.

### Story 5.1: Phone App Shell & Navigation Graph

As a developer,
I want the phone app's navigation structure and screen shell,
So that all phone screens have consistent navigation, top bar, and deep-link support.

**Acceptance Criteria:**

**Given** the phone app launches
**When** the main activity initializes
**Then** it uses `RoadMateTheme` (phone typography scale) and sets up a `NavHost` with Jetpack Navigation Compose

**Given** `@Serializable` route classes exist
**When** I inspect them
**Then** routes include: `VehicleHub`, `TripList`, `TripDetail(tripId: String)`, `MaintenanceList`, `MaintenanceDetail(scheduleId: String)`, `FuelLog`, `DocumentList`, `DocumentDetail(documentId: String)`, `VehicleManagement`

**Given** any detail screen is displayed
**When** the screen renders
**Then** it uses `Scaffold` with `TopAppBar` showing a back arrow that navigates up
**And** the back arrow responds to both tap and system back gesture

**Given** a notification deep-link targets a specific screen (e.g., `MaintenanceDetail`)
**When** the notification `PendingIntent` launches the app
**Then** it navigates directly to the target screen with the correct ID argument
**And** pressing back from the deep-linked screen returns to VehicleHub (not the system home)

**Given** the phone app has no vehicles in the database
**When** the app launches
**Then** it shows: BT icon + "Connect to your head unit to sync vehicle data." as the VehicleHub empty state
**And** no navigation options are available until data is synced

**Given** the maximum navigation depth is 3 levels
**When** I trace any user journey
**Then** the deepest path is: VehicleHub → Detail Screen → ModalBottomSheet (action)
**And** no nested drawers, tabs within tabs, or deeper navigation stacks exist

### Story 5.2: VehicleHub — Hero Card & Dashboard Layout

As a driver,
I want a single-scroll dashboard on my phone showing my vehicle's key information,
So that I can see everything important about my car in one glance.

**Acceptance Criteria:**

**Given** the phone app has synced vehicle data
**When** VehicleHub screen renders
**Then** it displays a `LazyColumn` with the following items in order: VehicleHeroCard, AttentionBand (if alerts exist), Maintenance summary card, Recent trips card, Fuel log summary card

**Given** `VehicleHeroCard` is displayed
**When** rendered
**Then** it shows: vehicle silhouette icon (64dp, centered, `onSurfaceVariant`), vehicle name (`headlineMedium`), ODO number (`displayLarge` 36sp Bold), sync status label (`labelSmall`)
**And** background is `surfaceVariant` (#1A1A1A) with 4dp corner radius

**Given** the vehicle synced recently (< 5 minutes)
**When** `VehicleHeroCard` sync status is displayed
**Then** it shows "Last synced: just now" in default `onSurfaceVariant` color

**Given** the vehicle synced > 1 hour ago
**When** `VehicleHeroCard` sync status is displayed
**Then** it shows "Last synced: [X] hours ago" in `onSurfaceVariant` color

**Given** the vehicle has never synced
**When** `VehicleHeroCard` sync status is displayed
**Then** it shows "Not yet synced" in `tertiary` (#FFB74D) color

**Given** maintenance or document alerts exist
**When** `AttentionBand` is rendered below the hero card
**Then** it shows up to 2 alert strips (amber for warning, red for critical) with "+N more" for additional
**And** tapping a band navigates to the relevant detail screen
**And** swiping dismisses the band (deferred, not resolved)

**Given** the driver pulls down on the VehicleHub
**When** the pull-to-refresh gesture triggers
**Then** a manual Bluetooth sync is initiated (from Story 4.4)
**And** the refresh indicator shows until sync completes or fails

**Given** VehicleHub loads data
**When** Room queries execute
**Then** all dashboard data (ODO, next maintenance, recent trips) returns within 200ms

### Story 5.3: Phone Trip List & Trip Detail

As a driver,
I want to see my trip history on my phone with details for each trip,
So that I can review my driving patterns and journey details.

**Acceptance Criteria:**

**Given** the driver taps the "Recent Trips" card on VehicleHub
**When** the TripList screen opens
**Then** it shows a scrollable list of trip cards for the active vehicle
**And** each card displays: date, distance (km), duration (h:mm), average speed (km/h)
**And** trips are sorted by most recent first

**Given** interrupted trips exist
**When** the trip list is displayed
**Then** interrupted trips show an "⚡ Interrupted" label in `tertiary` color

**Given** no trips exist for the active vehicle
**When** the trip list is displayed
**Then** an empty state shows: car icon + "No trips recorded yet. Drive to start tracking." in `onSurfaceVariant`

**Given** the driver taps a trip card
**When** the TripDetail screen opens
**Then** it shows: date and time range, total distance, duration, average speed, max speed, estimated fuel consumed
**And** the data loads within 200ms

**Given** the trip has recorded TripPoints
**When** the detail screen is displayed
**Then** a route summary section shows start and end coordinates formatted as readable location text (or lat/lng if geocoding is unavailable offline)

**Given** the trip list has many entries
**When** the driver scrolls
**Then** the list scrolls smoothly at 60fps using standard `LazyColumn` performance

### Story 5.4: Fuel Log — Entry, Calculation & Trends

As a driver,
I want to log fuel fill-ups and see consumption and cost trends,
So that I can track my fuel efficiency and spending over time.

**Acceptance Criteria:**

**Given** the driver taps the FAB ("+") on the FuelLog screen
**When** the `ModalBottomSheet` opens
**Then** it shows pre-filled fields: date (today), odometer (current vehicle ODO)
**And** additional fields: liters (number, required), price per liter (number, required), total cost (auto-calculated from liters × price), full tank toggle (boolean, default true), station name (text, optional)

**Given** the driver enters liters and price per liter
**When** the values change
**Then** total cost is auto-calculated and displayed in real-time: `liters × pricePerLiter`

**Given** the driver saves a fuel entry with `isFullTank = true`
**When** a previous full-tank entry exists
**Then** actual consumption is calculated: `totalLitersBetweenFills / distanceBetweenFills × 100` = L/100km
**And** the calculated consumption is displayed on the fuel log entry

**Given** actual consumption is calculated
**When** the vehicle has city consumption estimates configured
**Then** the entry shows "Actual: X.X L/100km vs Estimated: Y.Y L/100km"
**And** if actual > estimated by more than 20%, the actual value is shown in `tertiary` (amber)

**Given** the driver views the FuelLog screen
**When** entries exist
**Then** entries are sorted by most recent first
**And** each card shows: date, liters, total cost, consumption (if calculable), station name

**Given** the driver wants to see fuel cost trends
**When** they view the FuelLog summary section at the top of the screen
**Then** it shows: total fuel cost (this month), average L/100km (from full-tank calculations), average cost per km

**Given** no fuel entries exist
**When** the FuelLog screen is displayed
**Then** an empty state shows: fuel pump icon + "No fuel entries yet. Log your first fill-up." + primary "Add" button

**Given** the driver enters an ODO value lower than the previous fuel entry's ODO
**When** inline validation runs
**Then** the ODO field shows a red border with helper text: "Odometer must be higher than last entry ([X] km)"
**And** the Save button remains disabled

### Story 5.5: Phone Maintenance & Document Views

As a driver,
I want to manage maintenance items and documents on my phone,
So that I can track service schedules and document expiry from anywhere.

**Acceptance Criteria:**

**Given** the driver taps the maintenance summary card on VehicleHub
**When** the MaintenanceList screen opens
**Then** it shows all maintenance items for the active vehicle
**And** each item shows: name, `ProgressRing` (36dp compact inline, same color logic as GaugeArc), predicted next service date
**And** items are sorted by percentage (highest/most urgent first)

**Given** the driver taps a maintenance item
**When** the MaintenanceDetail screen opens
**Then** it shows the full detail view from Story 3.7 (GaugeArc Large, service history, mark-as-done action)

**Given** the driver taps the FAB ("+") on MaintenanceList
**When** the add maintenance flow triggers
**Then** it uses the same ModalBottomSheet as Story 3.3 (custom item creation)

**Given** the driver taps mark-as-done on a maintenance item
**When** the completion flow triggers
**Then** it uses the same ModalBottomSheet as Story 3.2 (pre-filled date/ODO, optional cost/location/notes)

**Given** the driver navigates to the DocumentList screen
**When** documents exist
**Then** they are displayed as cards sorted by expiry date (soonest first)
**And** each card shows type icon, name, expiry date, days until expiry with color coding (tertiary for soon, error for expired)

**Given** the driver taps a document card
**When** the DocumentDetail screen opens
**Then** it shows all document fields and allows editing via a secondary "Edit" button

**Given** the driver taps the FAB ("+") on DocumentList
**When** the add document flow triggers
**Then** it uses the same ModalBottomSheet as Story 3.5

### Story 5.6: Vehicle Switcher & Multi-Vehicle Support

As a driver with multiple vehicles,
I want to switch between vehicles on my phone,
So that I can view and manage data for each car separately.

**Acceptance Criteria:**

**Given** the phone has synced data for multiple vehicles
**When** the driver taps the vehicle name in the TopAppBar
**Then** a dropdown menu or ModalBottomSheet appears showing all vehicle names with their current ODO

**Given** the vehicle list is displayed
**When** the driver selects a different vehicle
**Then** the active vehicle ID is persisted to DataStore
**And** VehicleHub immediately refreshes with the selected vehicle's data
**And** all child screens (trips, maintenance, fuel, documents) are scoped to the new vehicle

**Given** the active vehicle is changed
**When** the VehicleHeroCard re-renders
**Then** it shows the new vehicle's name, ODO, and sync status
**And** the transition happens within 200ms (data is local)

**Given** only one vehicle exists
**When** the driver taps the vehicle name
**Then** the vehicle list still shows with the single entry (no special casing)
**And** the driver can still navigate to vehicle management to add another

**Given** the phone app launches
**When** DataStore has a persisted active vehicle ID
**Then** that vehicle is loaded immediately without prompting the driver to select

**Given** the active vehicle is deleted on the head unit and synced
**When** the phone app detects the vehicle no longer exists
**Then** it falls back to the first available vehicle
**And** if no vehicles exist, the VehicleHub empty state is shown

## Epic 6: Head Unit Dashboard Polish

The head unit parked dashboard is a full three-panel information display with recent trips, maintenance gauges, and vehicle status. Split-screen mode works alongside navigation apps via AdaptiveDashboard with 3 layout breakpoints. The driving ↔ parked transition animation is smooth. Head unit vehicle switcher is available in parked mode.

### Story 6.1: ParkedDashboard — Three-Panel Layout

As a driver,
I want a rich information dashboard when my car is parked,
So that I can review trips, check maintenance status, and see vehicle info at a glance on the head unit.

**Acceptance Criteria:**

**Given** the driving state is `Idle` (vehicle parked)
**When** the parked dashboard is displayed
**Then** it renders a three-panel landscape layout using a 12-column grid with 24dp margins and 16dp gutters

**Given** the left panel (4 columns)
**When** it renders
**Then** it shows: vehicle ODO number (`displayLarge`), `StatusChip` for tracking (active/paused), `StatusChip` for sync (synced/disconnected), and vehicle name (`headlineMedium`)
**And** all elements use `onSurface` or `onSurfaceVariant` colors on `surface` background

**Given** the center panel (4 columns)
**When** it renders
**Then** it shows: "Recent Trips" header (`titleLarge`), and a vertically scrollable list of trip cards (maximum 5 visible)
**And** each trip card shows: date, distance (km), duration (h:mm)
**And** the list scrolls with 60fps

**Given** the right panel (4 columns)
**When** it renders
**Then** it shows: "Maintenance" header (`titleLarge`), and a vertical stack of 3 `GaugeArc` Compact items (the 3 most urgent maintenance items by percentage)
**And** each GaugeArc shows the item name below in `labelLarge`

**Given** the vehicle has no trips yet
**When** the center panel renders
**Then** it shows "No trips yet" in `onSurfaceVariant` text (no icon, text-only for head unit)

**Given** the vehicle has no maintenance items configured
**When** the right panel renders
**Then** it shows "No maintenance items configured" in `onSurfaceVariant` text

**Given** the parked dashboard renders
**When** I measure load time
**Then** all three panels render within 2 seconds of state transition from driving to parked
**And** Room queries for dashboard data complete within 200ms

### Story 6.2: AdaptiveDashboard & Split-Screen Support

As a driver,
I want the head unit dashboard to adapt when running side-by-side with navigation apps,
So that I can see my vehicle info alongside Google Maps without either app breaking.

**Acceptance Criteria:**

**Given** the head unit runs RoadMate in full-screen mode (available width ≥ 960dp)
**When** `AdaptiveDashboard` evaluates the available width via `BoxWithConstraints`
**Then** it renders the full three-panel `ParkedDashboard` layout (driving mode shows full `DrivingHUD`)

**Given** Android split-screen is activated with half-screen (available width 480-959dp)
**When** `AdaptiveDashboard` evaluates the width
**Then** parked mode shows a two-panel layout: left (ODO + status chips), right (maintenance GaugeArc stack). Trips panel is hidden.
**And** driving mode shows compact HUD: ODO + trip stacked vertically, recording dot, no time display. Alert strip stays.

**Given** Android split-screen with narrow mode (available width ≤ 479dp)
**When** `AdaptiveDashboard` evaluates the width
**Then** parked mode shows a single column: ODO card, then maintenance list as vertical scroll
**And** driving mode shows minimal HUD: ODO number only + recording dot. Alert strip as icon-only (no text).

**Given** any width breakpoint
**When** touch targets are rendered in parked mode
**Then** all interactive elements maintain 76dp minimum touch target size
**And** fewer items are shown rather than smaller items

**Given** the dashboard is in split-screen
**When** the ODO number is displayed
**Then** it is always visible regardless of width (the non-negotiable element)

**Given** the dashboard is in split-screen driving mode
**When** `TripLiveIndicator` is displayed
**Then** the recording indicator is always visible regardless of width

**Given** the alert strip degrades across breakpoints
**When** width decreases
**Then** the strip shows: full text (≥960dp) → icon + abbreviated text (480-959dp) → colored dot only (≤479dp)

**Given** the app enters or exits multi-window mode
**When** `isInMultiWindowMode` changes
**Then** the layout adapts immediately without requiring a restart or re-navigation

### Story 6.3: Head Unit Vehicle Switcher & D-pad Navigation

As a driver,
I want to switch vehicles and navigate the dashboard with hardware controls on my head unit,
So that I can manage multiple vehicles and access information even with a rotary controller or D-pad.

**Acceptance Criteria:**

**Given** the head unit is in parked mode
**When** the driver taps the vehicle name in the dashboard header
**Then** a large-touch-target vehicle list appears showing all vehicle names and ODO values
**And** each list item has a minimum 76dp touch target

**Given** the vehicle list is displayed
**When** the driver selects a different vehicle
**Then** the active vehicle is updated in DataStore
**And** the parked dashboard refreshes immediately with the new vehicle's data (trips, maintenance, status)
**And** the driving HUD will use the new vehicle's ODO on the next trip

**Given** the head unit has a rotary controller or D-pad
**When** the driver uses directional navigation in parked mode
**Then** a 1dp `primary` (#4FC3F7) focus ring is drawn around the currently focused element
**And** focus cycles through dashboard panels: left panel → center panel → right panel

**Given** D-pad focus is on a trip card in the center panel
**When** the driver presses Enter/Select
**Then** it expands the trip card to show full details (distance, duration, avg speed, date) inline
**And** pressing Enter/Select again collapses it

**Given** D-pad focus is on a GaugeArc in the right panel
**When** the driver presses Enter/Select
**Then** it shows the maintenance item name, percentage, and predicted next service date in an expanded view

**Given** the head unit is in driving mode
**When** the driver uses D-pad or rotary controls
**Then** no focus ring is shown and no navigation occurs (zero interaction in driving mode)

**Given** `Modifier.focusOrder` is configured
**When** Tab (keyboard) or D-pad navigates
**Then** the traversal follows a logical left → center → right panel order
**And** within each panel, focus moves top to bottom

## Epic 7: Statistics, Export & Growth Features

The driver can view driving summaries by time period, cost trends, export vehicle data to CSV/PDF, and share trip routes via map URL. Weekly summary notifications keep the driver informed.

### Story 7.1: Driving Summaries & Statistics

As a driver,
I want to see summaries of my driving, fuel, and maintenance costs over time,
So that I can understand my vehicle usage patterns and total cost of ownership.

**Acceptance Criteria:**

**Given** the driver navigates to the Statistics screen on the phone
**When** the screen loads
**Then** it shows a period selector (Day, Week, Month, Year) with Month as the default

**Given** a period is selected
**When** the statistics are calculated
**Then** the screen displays: total distance driven (km), total number of trips, average trip distance, total driving time, total fuel cost, total maintenance cost, cost per km (fuel + maintenance)

**Given** the driver selects "Month" and the current month
**When** the statistics render
**Then** all values are calculated from trips, fuel logs, and maintenance records within the selected calendar month
**And** the query completes within 200ms

**Given** the driver selects "Year"
**When** the statistics render
**Then** a month-by-month breakdown is shown as a simple list: each month with distance, fuel cost, and maintenance cost
**And** a running total is shown at the top

**Given** no trips or records exist for the selected period
**When** the statistics screen renders
**Then** all values show 0 or "—" with a message: "No data for this period"

**Given** the driver views weekly statistics
**When** the current week and previous week have data
**Then** a comparison shows: distance change (↑12% or ↓5%), fuel cost change, helping the driver see trends

### Story 7.2: Data Export (CSV/PDF)

As a driver,
I want to export my vehicle data to CSV or PDF,
So that I can keep records outside the app or share them with my mechanic.

**Acceptance Criteria:**

**Given** the driver navigates to export options (Settings or Statistics screen)
**When** they select "Export Data"
**Then** they can choose: export format (CSV or PDF), data scope (Trips, Fuel Log, Maintenance History, or All), and date range (optional, defaults to all time)

**Given** the driver selects CSV export for Trips
**When** the export generates
**Then** a CSV file is created with columns: Date, Start Time, End Time, Distance (km), Duration, Avg Speed (km/h), Max Speed (km/h), Estimated Fuel (L), Status
**And** one row per trip, sorted by date ascending
**And** the file is named `roadmate_trips_[vehicle-name]_[date].csv`

**Given** the driver selects CSV export for Fuel Log
**When** the export generates
**Then** columns include: Date, Odometer (km), Liters, Price/L, Total Cost, Full Tank, Station, Consumption (L/100km)

**Given** the driver selects CSV export for Maintenance History
**When** the export generates
**Then** columns include: Item Name, Date Performed, Odometer (km), Cost, Location, Notes

**Given** the driver selects PDF export
**When** the export generates
**Then** a formatted PDF is created with the vehicle name as header, export date, and the selected data in readable table format

**Given** the export file is generated
**When** the driver taps "Share"
**Then** the Android share sheet opens with the file attached, allowing the driver to send via any sharing app (email, messaging, cloud storage)

**Given** the vehicle has 1 year of data (~700 trips, 50 fuel logs, 20 maintenance records)
**When** a full CSV export runs
**Then** it completes within 5 seconds without blocking the UI

### Story 7.3: Trip Route Sharing

As a driver,
I want to share a specific trip route with someone,
So that I can show them where I drove or save interesting routes.

**Acceptance Criteria:**

**Given** the driver is on a TripDetail screen with TripPoints available
**When** they tap "Share Route"
**Then** a shareable link is generated using OpenStreetMap URL format: `https://www.openstreetmap.org/directions?route=[lat1,lng1;lat2,lng2;...]` with waypoints sampled from TripPoints (max 25 points to fit URL limits)

**Given** the trip has many TripPoints (e.g., 500+)
**When** the route URL is generated
**Then** TripPoints are intelligently sampled: start point, end point, and up to 23 evenly distributed intermediate points
**And** the route shape is preserved at a high level despite the sampling

**Given** the route URL is generated
**When** the driver taps "Share"
**Then** the Android share sheet opens with the URL as text content
**And** the share text includes: "My trip on [date]: [distance] km, [duration] — [URL]"

**Given** a trip has fewer than 25 TripPoints
**When** the route URL is generated
**Then** all TripPoints are included without sampling

**Given** a trip has status `INTERRUPTED` with a GPS gap
**When** the route URL is generated
**Then** only the available TripPoints are used (gaps result in straight-line segments on the map, which is acceptable)
