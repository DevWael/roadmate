---
stepsCompleted: [1, 2, 3, 4, 5, 6, 7, 8]
inputDocuments: [roadmate.md, prd.md, ux-design-specification.md]
workflowType: 'architecture'
project_name: 'RoadMate'
user_name: 'Ahmad'
date: '2026-05-08'
lastStep: 8
status: 'complete'
completedAt: '2026-05-08'
---

# Architecture Decision Document

_This document builds collaboratively through step-by-step discovery. Sections are appended as we work through each architectural decision together._

## Project Context Analysis

### Requirements Overview

**Functional Requirements:**

50 FRs across 10 capability areas. The heaviest clusters are Trip Tracking (FR7-14, 8 requirements) and Data Synchronization (FR30-35, 6 requirements) — both involving real-time or near-real-time system behavior with no user interaction. Maintenance Management (FR15-21, 7 requirements) is the primary user-facing active interaction surface. Vehicle Management, Fuel Tracking, Document Management, Data Integrity, Background Service, Notifications, and Statistics round out the set.

The architectural implication: ~60% of FRs describe automated system behavior (tracking, syncing, recovering, notifying). Only ~40% involve direct user interaction. The architecture must be service-first, not UI-first.

**Non-Functional Requirements:**

27 NFRs organized into Performance (8), Reliability & Data Integrity (7), Privacy & Security (5), Integration (4), and Resource Efficiency (3).

Architecturally critical NFRs:
- **NFR1:** State machine evaluation within 100ms per location update — tight real-time constraint
- **NFR5:** Delta sync under 5 seconds for daily data — defines RFCOMM payload budgets
- **NFR9-10:** Max 10s/30s data loss on power cut — drives flush interval and journal frequency
- **NFR13:** 30+ day continuous service operation — no memory leaks, no ANR
- **NFR16:** Zero network permissions — enforced at manifest level
- **NFR21:** RFCOMM must not interfere with A2DP/HFP audio — separate UUID channel required
- **NFR25:** GPS polling at 3s intervals during tracking, zero when IDLE — battery/CPU budget
- **NFR26:** <50MB database for 1 year — constrains TripPoint storage density

**UX Architectural Implications:**

The UX spec defines 9 custom Compose components (`GaugeArc`, `DrivingHUD`, `ParkedDashboard`, `VehicleHeroCard`, `AttentionBand`, `TripLiveIndicator`, `ContextAwareLayout`, `ProgressRing`, `StatusChip`) shared across platforms via a `:core:ui` module. The context-aware head unit layout (driving ↔ parked) depends on real-time driving state from the service layer — this requires a reactive state flow from service → ViewModel → Composable.

Split-screen support on head unit (3 layout breakpoints: ≥960dp, 480-959dp, ≤479dp) adds adaptive layout complexity.

**Scale & Complexity:**

- Primary domain: Native Android dual-app mobile system
- Complexity level: High
- Estimated architectural components: ~15 major components (foreground service, GPS tracker, trip state machine, crash recovery, BT sync server, BT sync client, Room database layer, repository layer, sync protocol handler, notification scheduler, head unit UI layer, phone UI layer, vehicle management, OBD interface abstraction, DataStore preferences)

### Technical Constraints & Dependencies

| Constraint | Impact |
|---|---|
| Min API 29 (Android 10) | All Bluetooth and location APIs available. No backcompat concerns. |
| No Google Play Services guarantee (head units) | Must fallback from `FusedLocationProviderClient` to `LocationManager`. Affects GPS acquisition strategy. |
| No INTERNET permission | No crash reporting, no analytics, no remote config, no cloud sync. All diagnostics must be local. |
| Head unit abrupt power loss | Every write path must assume interruption. No graceful shutdown guaranteed. |
| Single RFCOMM connection | Only one phone syncs at a time. Multi-user = sequential, not concurrent. |
| BT Classic, not BLE | Stream socket with ~2-3 Mbps throughput. Good for delta sync payloads, but connection management differs from BLE. |
| Landscape-only head unit | Fixed orientation. No portrait adaptation needed for head unit. |
| Kotlin + Jetpack Compose + Hilt + Room | Stack is decided. Architecture must work within these frameworks' patterns (ViewModel, Repository, DAO, @Inject). |

### Cross-Cutting Concerns Identified

1. **Driving State Propagation** — The trip detection state machine's output (IDLE/DRIVING/STOPPING/GAP_CHECK) drives UI mode switching, sync event triggers, GPS polling intervals, and notification thresholds. It's the central nervous system of the app.

2. **Data Integrity Under Power Loss** — Affects Room configuration (WAL mode), TripPoint write strategy (batched flush), crash recovery journal (DataStore), and boot recovery sequence. Cuts across service, database, and preferences layers.

3. **Vehicle Scoping** — Every entity (Trip, MaintenanceSchedule, MaintenanceRecord, FuelLog, Document, PreTripChecklist) is keyed by `vehicleId`. Every query, every UI screen, every sync payload must be vehicle-aware. The "active vehicle" concept flows from settings through repository to UI.

4. **Bluetooth Connection Lifecycle** — Connection state (connected/disconnected/connecting/failed) affects sync availability, status chip display, event-driven sync triggers, and periodic sync scheduling. Must be observable from multiple consumers.

5. **Offline-First Data Flow** — No network ever means no remote error reporting, no feature flags, no A/B testing, no remote configuration. Every diagnostic, every configuration, every fallback is local. Map tiles must be pre-cached.

6. **Shared Core Module** — Business logic, data models, Room database, repositories, and OBD interfaces must be identical between both apps. Changes to `core` affect both `app-headunit` and `app-phone`. Module boundary discipline is critical.

## Starter Template Evaluation

### Primary Technology Domain

Native Android dual-app system (Kotlin + Jetpack Compose), multi-module Gradle project. Not a web/CLI/cross-platform project — no starter template CLI applies. Project is initialized via Android Studio with manual multi-module configuration following the Now In Android convention plugin pattern.

### Technology Stack — Verified Current Versions

| Dependency | Version | Status |
|---|---|---|
| Kotlin | 2.3.21 | ✅ Latest stable |
| Android Gradle Plugin | 9.2.1 | ✅ Latest stable |
| Compose BOM | 2026.04.01 (Compose 1.11.0) | ✅ Latest stable |
| Room | 2.8.4 (KSP) | ✅ Latest stable (Room 3.0 is alpha — skip) |
| Hilt (Dagger) | 2.59.2 | ✅ Latest stable |
| WorkManager | 2.11.2 | ✅ Latest stable |
| DataStore Preferences | 1.2.1 | ✅ Latest stable |
| Kotlin Serialization | 1.11.0 | ✅ Latest stable |
| Play Services Location | 21.3.0 | ✅ Latest stable |
| osmdroid | 6.1.20 | ⚠️ Archived — no updates since Aug 2024 |
| MapLibre Native Android | 13.1.0 | ✅ Recommended alternative for Growth phase maps |

### Maps Library Decision

**osmdroid (6.1.20)** — Originally specified in product brief. Open source, OpenStreetMap, offline tile caching. However, the project is **archived on GitHub** with no maintenance. No Compose integration. View-based only (requires `AndroidView` wrapper in Compose).

**MapLibre Native Android (13.1.0)** — Actively maintained, OpenStreetMap compatible, vector tiles, Compose extension available (`maplibre-compose`), no API key required for OSM tile servers. Same privacy guarantees as osmdroid.

**Recommendation:** Use MapLibre for the Growth phase map features. Maps are not MVP scope, so this decision can be revisited, but osmdroid should be considered a risk given its archived status.

### Build System Architecture

**Approach:** Convention Plugins + Version Catalog (Now In Android pattern)

**Rationale:** With 3+ modules sharing build configuration (minSdk, compileSdk, Compose compiler, Hilt processor, Room KSP), duplicating `build.gradle.kts` logic is error-prone. Convention plugins encapsulate shared config once.

**Structure:**

```
build-logic/
├── convention/
│   └── src/main/kotlin/
│       ├── AndroidApplicationConventionPlugin.kt
│       ├── AndroidLibraryConventionPlugin.kt
│       ├── AndroidComposeConventionPlugin.kt
│       └── AndroidHiltConventionPlugin.kt
gradle/
└── libs.versions.toml
```

**Initialization Command:**

```bash
# No CLI starter — Android Studio "Empty Compose Activity" project
# then manually configure multi-module structure:
# 1. Create project with :app-headunit module
# 2. Add :app-phone module (Android Application)
# 3. Add :core module (Android Library)
# 4. Add build-logic included build
# 5. Configure libs.versions.toml with verified versions above
```

**Architectural Decisions Provided by Build System:**

**Language & Runtime:**
- Kotlin 2.3.21, targeting JVM 17
- KSP for annotation processing (Room, Hilt)
- Compose compiler integrated via Kotlin plugin (no separate compiler version since Kotlin 2.0+)

**Code Organization:**
- `:app-headunit` — Android Application, landscape-only, foreground service
- `:app-phone` — Android Application, portrait-primary, WorkManager
- `:core` — Android Library, shared Room DB, repositories, models, business logic, OBD interfaces
- `:build-logic` — Included build, convention plugins for shared Gradle config

**Testing Framework:**
- JUnit 5 for unit tests
- Compose UI testing (`createComposeRule`) for UI tests
- Espresso for instrumented tests
- Turbine for Flow testing

**Development Experience:**
- Gradle Version Catalog for single-source dependency management
- Convention plugins for zero-duplication build config
- Compose previews for UI development

**Note:** Project initialization using this structure should be the first implementation story.

## Core Architectural Decisions

### Decision Priority Analysis

**Critical Decisions (Block Implementation):**
1. Data modeling approach (Room entities as domain models + sync DTOs)
2. Service-to-UI state propagation (Hilt singleton StateFlow)
3. Sync granularity (entity-level delta sync)
4. Navigation architecture (Jetpack Navigation Compose)

**Important Decisions (Shape Architecture):**
5. Error handling pattern (sealed UiState + Timber)
6. Database migration strategy (auto-migration default)

**Deferred Decisions (Post-MVP):**
- Google Drive backup architecture (V2)
- OBD-II Bluetooth ELM327 communication protocol (V2 — interface designed now)
- Community maintenance template distribution format (V2)
- Kalman filter integration point for GPS smoothing (V2)

### Data Architecture

**Decision: Room entities serve as domain models. Sync uses dedicated DTOs.**

| Aspect | Choice | Rationale |
|---|---|---|
| Domain models | Room `@Entity` classes used throughout | Single-developer project; data shape matches storage shape. Avoids mapping boilerplate across 8 entity types. |
| Sync payloads | Dedicated `@Serializable` DTO classes | Decouples sync wire format from Room annotations. Allows sync protocol evolution without DB migration. |
| Entity-to-DTO mapping | Extension functions in `:core` | `Trip.toSyncDto()` / `TripSyncDto.toEntity()` — simple, discoverable, testable. |
| Vehicle scoping | `vehicleId` foreign key on all entities | Every query, every sync payload filtered by vehicle. Active vehicle stored in DataStore. |
| UUID strategy | `UUID.randomUUID().toString()` as primary key | Enables conflict-free ID generation on both devices. No auto-increment — no ID collision during sync. |
| Timestamps | `lastModified: Long` (epoch millis) on every entity | Drives delta sync and last-write-wins conflict resolution. Updated on every write. |

**Database Configuration:**
- `JournalMode.WRITE_AHEAD_LOGGING` — explicit WAL for power loss resilience
- `exportSchema = true` — enables auto-migration between versions
- Single `RoadMateDatabase` class in `:core:database`
- DAOs per entity group: `VehicleDao`, `TripDao`, `MaintenanceDao`, `FuelDao`, `DocumentDao`

**Migration Strategy:**
- Auto-migration as default (Room 2.8.x)
- Manual `Migration` classes only for column renames, data transforms, or complex schema changes
- Schema exported to `schemas/` directory for version tracking

### State Management & Service Communication

**Decision: Hilt singleton `StateFlow` for driving state propagation.**

| Component | Pattern | Detail |
|---|---|---|
| Driving state source | `DrivingStateManager` (`@Singleton`) | Owns `MutableStateFlow<DrivingState>`. Injected into foreground service and ViewModels. |
| State values | `sealed interface DrivingState` | `Idle`, `Driving(tripId, distance, duration)`, `Stopping(timeSinceStop)`, `GapCheck(gapDuration)` |
| Service → UI flow | `StateFlow.collectAsState()` in Compose | ViewModels expose `StateFlow<DrivingState>` to Composables. No binding, no broadcasts. |
| BT connection state | `BluetoothStateManager` (`@Singleton`) | `StateFlow<BtConnectionState>` — `Connected`, `Disconnected`, `Connecting`, `SyncInProgress`, `SyncFailed` |
| GPS state | `LocationStateManager` (`@Singleton`) | `StateFlow<GpsState>` — `Acquired(accuracy)`, `Acquiring`, `Unavailable` |

**ViewModel Pattern:**
- One ViewModel per screen (not per feature)
- ViewModels inject repositories + state managers via Hilt
- UI state exposed as `StateFlow<ScreenUiState>` combining data + system state
- No `LiveData` — `StateFlow` throughout for consistency

### Sync Protocol Architecture

**Decision: Entity-level delta sync with UUID + lastModified.**

| Aspect | Choice | Rationale |
|---|---|---|
| Sync granularity | Per-entity record | Avoids transferring unchanged records. Critical for TripPoint volumes. |
| Sync trigger | On BT connect + event-driven + 15-min periodic + manual | Multiple triggers ensure freshness without polling overhead. |
| Conflict resolution | Last-write-wins by `lastModified` | Simple, deterministic. Acceptable for single-user V1. |
| Payload format | Length-prefixed JSON over RFCOMM | `[4 bytes: length][JSON payload]`. Kotlin Serialization for encode/decode. |
| Sync messages | `SYNC_STATUS`, `SYNC_PULL`, `SYNC_PUSH`, `SYNC_ACK` | Request/response pairs. Each message self-contained. |
| Idempotency | UUID-based — re-sending a record with same UUID overwrites | Safe to retry on connection drop. No duplicate records. |
| TripPoint batching | Sync TripPoints in batches of 100 per message | Prevents oversized payloads for long trips (1000+ points). |

**Head Unit (Server) Role:**
- Registers RFCOMM SPP with fixed UUID inside `RoadMateService`
- Accepts incoming connection from bonded phone
- Source of truth for trips, GPS data, odometer

**Phone (Client) Role:**
- Connects to bonded head unit UUID on BT proximity
- Source of truth for maintenance mark-as-done, fuel logs (user-entered data)
- Pulls trip/GPS data from head unit, pushes user-entered data

### Navigation Architecture

**Decision: Jetpack Navigation Compose with type-safe routes.**

| Aspect | Choice |
|---|---|
| Library | `androidx.navigation:navigation-compose` (from Compose BOM) |
| Route definition | `@Serializable` data classes/objects (Kotlin 2.0+ pattern) |
| NavHost | Single `NavHost` per app — no nested navigation graphs |
| Deep links | Notification tap → `PendingIntent` with navigation deep link to specific screen |
| Arguments | Type-safe via `@Serializable` route data classes (e.g., `MaintenanceDetail(scheduleId: String)`) |

**Phone App Navigation Graph:**
```
VehicleHub (start) → MaintenanceList → MaintenanceDetail → MarkAsDone (bottom sheet)
VehicleHub → TripList → TripDetail
VehicleHub → FuelLogList → FuelLogEntry (bottom sheet)
VehicleHub → DocumentList → DocumentDetail
VehicleHub → Settings
```

**Head Unit Navigation:**
- No navigation graph — single screen with `ContextAwareLayout`
- Driving/parked mode switching handled by `DrivingStateManager` flow, not navigation

### Error Handling & Logging

**Decision: Timber for logging + sealed `UiState<T>` for UI error handling.**

| Layer | Pattern | Detail |
|---|---|---|
| Logging | Timber | Debug tree in debug builds. No-op tree (or file tree) in release. |
| Repository → ViewModel | `kotlin.Result<T>` | Repository functions return `Result<T>`. ViewModel maps to `UiState`. |
| ViewModel → UI | `sealed interface UiState<T>` | `Loading`, `Success(data: T)`, `Error(message: String)` |
| Service errors | Timber + `StateFlow` update | GPS/BT failures update state managers, logged via Timber. No UI interruption in driving mode. |
| Sync errors | Silent retry + status update | `BtConnectionState.SyncFailed` shown via `StatusChip`. No snackbar/dialog while driving. |
| Form validation | Inline on `OutlinedTextField` | Red border + helper text. Save button disabled until valid. No toast/snackbar. |

### Decision Impact Analysis

**Implementation Sequence:**
1. `:core:database` — Room entities, DAOs, database (foundation for everything)
2. `:core:model` — Sync DTOs, state sealed interfaces
3. `:core` repositories — Data access with `Result<T>` returns
4. State managers (`DrivingStateManager`, `BluetoothStateManager`, `LocationStateManager`)
5. `RoadMateService` (foreground service) — GPS + trip state machine + BT server
6. Head unit UI (DrivingHUD + ParkedDashboard)
7. Phone UI (VehicleHub + navigation graph + detail screens)
8. Sync protocol handler
9. WorkManager notification scheduler

**Cross-Component Dependencies:**
- State managers are injected everywhere → must be implemented early as interfaces with test fakes
- Room entities define the sync DTO shape → entity design must be finalized before sync protocol work
- Navigation deep links depend on route definitions → routes defined with screen stubs first
- Convention plugins must be set up before any module can build → true first story

## Implementation Patterns & Consistency Rules

### Pattern Categories Defined

**Critical Conflict Points Identified:** 12 areas where AI agents could make different choices, grouped into naming, structure, format, communication, and process.

### Naming Patterns

**Database Naming:**

| Element | Convention | Example |
|---|---|---|
| Table names | `snake_case`, plural | `trips`, `trip_points`, `maintenance_schedules` |
| Column names | `snake_case` | `vehicle_id`, `last_modified`, `start_time` |
| Primary keys | `id` (String UUID) | `@PrimaryKey val id: String = UUID.randomUUID().toString()` |
| Foreign keys | `{entity}_id` | `vehicle_id`, `trip_id` |
| Index names | `idx_{table}_{columns}` | `idx_trips_vehicle_id`, `idx_trip_points_trip_id` |
| Entity class names | PascalCase, singular | `Trip`, `TripPoint`, `MaintenanceSchedule` |

**Code Naming:**

| Element | Convention | Example |
|---|---|---|
| Kotlin files | PascalCase matching class | `TripRepository.kt`, `DrivingStateManager.kt` |
| Packages | lowercase, dot-separated | `com.roadmate.core.database`, `com.roadmate.headunit.ui` |
| Functions | camelCase, verb-first | `getActiveVehicle()`, `startTracking()`, `syncTrips()` |
| Variables/properties | camelCase | `activeVehicle`, `lastSyncTime`, `tripPoints` |
| Constants | SCREAMING_SNAKE_CASE | `GPS_INTERVAL_MS`, `RFCOMM_UUID`, `MAX_TRIP_POINTS_PER_BATCH` |
| Composable functions | PascalCase | `DrivingHUD()`, `GaugeArc()`, `VehicleHeroCard()` |
| ViewModel classes | `{Screen}ViewModel` | `VehicleHubViewModel`, `MaintenanceDetailViewModel` |
| Repository classes | `{Entity}Repository` | `TripRepository`, `MaintenanceRepository` |
| DAO interfaces | `{Entity}Dao` | `TripDao`, `VehicleDao` |
| State managers | `{Domain}StateManager` | `DrivingStateManager`, `BluetoothStateManager` |
| Hilt modules | `{Scope}Module` | `DatabaseModule`, `RepositoryModule`, `ServiceModule` |
| Navigation routes | PascalCase object/data class | `object VehicleHub`, `data class TripDetail(val tripId: String)` |

**Sync DTO Naming:**

| Element | Convention | Example |
|---|---|---|
| DTO classes | `{Entity}SyncDto` | `TripSyncDto`, `MaintenanceScheduleSyncDto` |
| Mapper extensions | `to{Target}()` | `Trip.toSyncDto()`, `TripSyncDto.toEntity()` |
| Sync message types | SCREAMING_SNAKE_CASE enum | `SYNC_STATUS`, `SYNC_PULL`, `SYNC_PUSH`, `SYNC_ACK` |

### Structure Patterns

**Module Organization:**

```
roadmate/
├── build-logic/
│   └── convention/src/main/kotlin/
├── app-headunit/
│   └── src/main/kotlin/com/roadmate/headunit/
│       ├── di/                  # Hilt modules specific to head unit
│       ├── service/             # RoadMateService, GPS tracker
│       ├── ui/                  # Composables, ViewModels
│       │   ├── driving/         # DrivingHUD and related
│       │   ├── parked/          # ParkedDashboard and related
│       │   └── theme/           # Head unit theme overrides
│       └── receiver/            # BootReceiver, PowerReceiver
├── app-phone/
│   └── src/main/kotlin/com/roadmate/phone/
│       ├── di/
│       ├── navigation/          # NavHost, route definitions
│       ├── ui/
│       │   ├── hub/             # VehicleHub screen
│       │   ├── maintenance/     # Maintenance list + detail
│       │   ├── trips/           # Trip list + detail
│       │   ├── fuel/            # Fuel log list + entry
│       │   ├── documents/       # Document list + detail
│       │   ├── settings/        # Settings screen
│       │   ├── components/      # Phone-specific shared components
│       │   └── theme/           # Phone theme overrides
│       └── worker/              # WorkManager notification workers
├── core/
│   └── src/main/kotlin/com/roadmate/core/
│       ├── database/            # RoadMateDatabase, DAOs, entities
│       ├── model/               # Sync DTOs, sealed interfaces, enums
│       ├── repository/          # Repository implementations
│       ├── sync/                # Sync protocol handler, BT manager
│       ├── location/            # GPS provider abstraction
│       ├── obd/                 # OBDProvider interface + MockOBDProvider
│       ├── state/               # DrivingStateManager, BluetoothStateManager, etc.
│       ├── ui/                  # Shared Composables (GaugeArc, StatusChip, etc.)
│       │   ├── components/
│       │   └── theme/           # Shared M3 theme, colors, typography
│       └── util/                # Extension functions, constants
└── gradle/
    └── libs.versions.toml
```

**Test Organization:**

| Test type | Location | Convention |
|---|---|---|
| Unit tests | `src/test/kotlin/...` (mirrors main) | `{Class}Test.kt` — e.g., `TripRepositoryTest.kt` |
| Instrumented tests | `src/androidTest/kotlin/...` | `{Class}InstrumentedTest.kt` |
| Compose UI tests | `src/androidTest/kotlin/.../ui/` | `{Screen}ScreenTest.kt` |
| Test fakes | `src/test/kotlin/.../fake/` | `Fake{Interface}.kt` — e.g., `FakeTripRepository.kt` |
| Test fixtures | `src/test/kotlin/.../fixture/` | `{Entity}Fixtures.kt` — factory functions for test data |

### Format Patterns

**Sync Payload JSON (Kotlin Serialization):**

```kotlin
// All JSON fields use camelCase (Kotlin Serialization default)
@Serializable
data class TripSyncDto(
    val id: String,
    val vehicleId: String,
    val startTime: Long,        // epoch millis
    val endTime: Long?,
    val distanceKm: Double,
    val lastModified: Long
)
```

| Format element | Convention |
|---|---|
| JSON field naming | camelCase (Kotlin Serialization default) |
| Timestamps | `Long` epoch milliseconds |
| Distances | `Double` in kilometers |
| Volumes | `Double` in liters |
| Currency | `Double` (user's local currency, no conversion) |
| Nullable fields | Kotlin `?` type → JSON `null` |
| UUIDs | `String` (not typed UUID) |

**Date/Time Display (UI):**

| Context | Format | Example |
|---|---|---|
| Trip date | `d MMM yyyy` | `8 May 2026` |
| Trip time | `HH:mm` | `14:30` |
| Relative time | `"X min ago"`, `"X hours ago"` | `"2 min ago"` |
| Duration | `Xh Ym` | `1h 23m` |
| Odometer | Formatted with locale grouping | `45,230 km` |

### Communication Patterns

**StateFlow Patterns:**

```kotlin
// ALWAYS: Private mutable, public read-only
class DrivingStateManager @Inject constructor() {
    private val _state = MutableStateFlow<DrivingState>(DrivingState.Idle)
    val state: StateFlow<DrivingState> = _state.asStateFlow()

    fun updateState(newState: DrivingState) {
        _state.value = newState
    }
}
```

| Pattern | Rule |
|---|---|
| Mutable state | `_` prefix, private |
| Public state | No prefix, `StateFlow` (not `MutableStateFlow`) |
| State collection in ViewModel | `.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initial)` |
| State collection in Compose | `.collectAsState()` |
| Event emission | One-shot events via `Channel` → `receiveAsFlow()`, not `SharedFlow` |

**UiState Pattern:**

```kotlin
// ALWAYS: Use this exact sealed interface for every screen
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
```

### Process Patterns

**Repository Pattern:**

```kotlin
// ALWAYS: Repositories return Result<T> for fallible operations
class TripRepository @Inject constructor(
    private val tripDao: TripDao
) {
    fun getTripsForVehicle(vehicleId: String): Flow<List<Trip>> =
        tripDao.getTripsForVehicle(vehicleId) // Flow for reactive queries

    suspend fun saveTrip(trip: Trip): Result<Unit> = runCatching {
        tripDao.upsert(trip)
    }
}
```

| Pattern | Rule |
|---|---|
| Reactive queries | Return `Flow<T>` (Room's reactive queries) |
| One-shot writes | Return `suspend Result<T>` |
| Error propagation | `runCatching { }` in repositories, `.fold()` or `.getOrElse()` in ViewModels |
| Vehicle scoping | Every query takes `vehicleId` parameter — never rely on "current vehicle" inside DAO |

**Compose Screen Pattern:**

```kotlin
// ALWAYS: Screen composable takes ViewModel, internal composable takes state
@Composable
fun MaintenanceListScreen(
    viewModel: MaintenanceListViewModel = hiltViewModel(),
    onNavigateToDetail: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    MaintenanceListContent(uiState = uiState, onItemClick = onNavigateToDetail)
}

@Composable
internal fun MaintenanceListContent(
    uiState: UiState<List<MaintenanceSchedule>>,
    onItemClick: (String) -> Unit
) {
    when (uiState) {
        is UiState.Loading -> { /* shimmer */ }
        is UiState.Success -> { /* content */ }
        is UiState.Error -> { /* error state */ }
    }
}
```

| Pattern | Rule |
|---|---|
| Screen composable | Injects ViewModel, collects state, delegates to content composable |
| Content composable | `internal`, takes state + callbacks, no ViewModel reference. Enables preview + testing. |
| Preview | On content composable with sample data |
| Navigation callbacks | Lambda parameters (`onNavigateToDetail`), not `NavController` reference |

**Hilt Module Pattern:**

```kotlin
// ALWAYS: One module per concern, installed in SingletonComponent for app-wide
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RoadMateDatabase =
        Room.databaseBuilder(context, RoadMateDatabase::class.java, "roadmate.db")
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .build()

    @Provides
    fun provideTripDao(database: RoadMateDatabase): TripDao = database.tripDao()
}
```

### Enforcement Guidelines

**All AI Agents MUST:**

1. Follow the naming conventions table exactly — no exceptions for "just this one class"
2. Place files in the module/package structure defined above — don't create new top-level packages without updating architecture
3. Use `UiState<T>` sealed interface for every screen — no custom loading/error patterns
4. Return `Result<T>` from repositories — no throwing exceptions through the ViewModel layer
5. Use `StateFlow` for state, `Channel` for one-shot events — no `LiveData`, no `SharedFlow` for events
6. Scope every data query by `vehicleId` — no "get all trips" without vehicle filter
7. Use the Screen/Content composable split — no ViewModel references in `@Preview`-compatible composables
8. Name tests `{Class}Test.kt` and fakes `Fake{Class}.kt` — consistent discoverability

**Anti-Patterns (NEVER do these):**

| ❌ Anti-Pattern | ✅ Correct Pattern |
|---|---|
| `class tripRepository` | `class TripRepository` |
| `fun MaintenanceList()` with ViewModel inside | Split into `MaintenanceListScreen()` + `MaintenanceListContent()` |
| `LiveData<List<Trip>>` | `StateFlow<UiState<List<Trip>>>` |
| `throw Exception("failed")` in repository | `return Result.failure(e)` |
| `tripDao.getAllTrips()` | `tripDao.getTripsForVehicle(vehicleId)` |
| Constants in companion object | Top-level `const val` in `Constants.kt` or relevant file |
| `MutableStateFlow` exposed publicly | Private `_state` + public `state: StateFlow` |

## Project Structure & Boundaries

### Complete Project Directory Structure

**Root Configuration:**

```
roadmate/
├── README.md
├── settings.gradle.kts              # includes build-logic, :core, :app-headunit, :app-phone
├── build.gradle.kts                 # root: apply false for all plugins
├── gradle.properties                # org.gradle.jvmargs, android.useAndroidX=true
├── .gitignore
├── gradle/
│   ├── libs.versions.toml           # all dependency versions
│   └── wrapper/gradle-wrapper.properties
└── build-logic/
    ├── settings.gradle.kts
    └── convention/
        ├── build.gradle.kts
        └── src/main/kotlin/
            ├── AndroidApplicationConventionPlugin.kt
            ├── AndroidLibraryConventionPlugin.kt
            ├── AndroidComposeConventionPlugin.kt
            ├── AndroidHiltConventionPlugin.kt
            └── AndroidRoomConventionPlugin.kt
```

**Core Module — Shared Foundation:**

```
core/src/main/kotlin/com/roadmate/core/
├── database/
│   ├── RoadMateDatabase.kt
│   ├── entity/          # Vehicle, Trip, TripPoint, MaintenanceSchedule,
│   │                    # MaintenanceRecord, FuelLog, Document, PreTripChecklist
│   ├── dao/             # VehicleDao, TripDao, MaintenanceDao, FuelDao, DocumentDao
│   └── converter/       # Converters.kt (Room TypeConverters)
├── model/
│   ├── DrivingState.kt, BtConnectionState.kt, GpsState.kt, UiState.kt
│   ├── SyncMessage.kt   # sealed interface for protocol message types
│   └── sync/            # TripSyncDto, MaintenanceScheduleSyncDto, FuelLogSyncDto, etc.
├── repository/          # VehicleRepository, TripRepository, MaintenanceRepository,
│                        # FuelRepository, DocumentRepository
├── sync/
│   ├── SyncProtocolHandler.kt    # frame encode/decode, message routing
│   ├── SyncEngine.kt             # delta calculation, batch orchestration
│   └── BluetoothSyncManager.kt   # RFCOMM socket management
├── location/
│   ├── LocationProvider.kt        # interface
│   ├── FusedLocationProvider.kt   # Google Play Services impl
│   └── PlatformLocationProvider.kt # LocationManager fallback
├── obd/
│   ├── OBDProvider.kt             # interface (V2 ready)
│   └── MockOBDProvider.kt         # GPS-fallback stub for V1
├── state/
│   ├── DrivingStateManager.kt
│   ├── BluetoothStateManager.kt
│   └── LocationStateManager.kt
├── ui/
│   ├── components/      # GaugeArc, StatusChip, TripLiveIndicator, ProgressRing
│   └── theme/           # RoadMateTheme.kt, Color.kt, Type.kt, Spacing.kt
└── util/                # Constants.kt, DateFormatters.kt, Extensions.kt
```

**Head Unit App:**

```
app-headunit/src/main/kotlin/com/roadmate/headunit/
├── RoadMateApplication.kt          # @HiltAndroidApp
├── di/                              # ServiceModule.kt, LocationModule.kt
├── service/
│   ├── RoadMateService.kt          # Foreground service: GPS + BT server + state machine
│   ├── TripDetector.kt             # State machine: IDLE→DRIVING→STOPPING→GAP_CHECK
│   ├── GpsTracker.kt               # Location callback → TripPoint write + flush
│   ├── CrashRecoveryJournal.kt     # DataStore-backed journal (30s interval)
│   └── RfcommServer.kt             # SPP accept loop, delegates to SyncProtocolHandler
├── ui/
│   ├── driving/
│   │   ├── DrivingHudScreen.kt     # Screen + Content split
│   │   └── DrivingHudViewModel.kt
│   ├── parked/
│   │   ├── ParkedDashboardScreen.kt
│   │   ├── ParkedDashboardViewModel.kt
│   │   └── AdaptiveDashboard.kt    # BoxWithConstraints 3-tier layout
│   └── theme/                       # HeadUnitTheme.kt (dark-only overrides)
└── receiver/
    ├── BootReceiver.kt              # ACTION_BOOT_COMPLETED → start service
    └── PowerReceiver.kt             # ACTION_SHUTDOWN → flush + journal
```

**Phone App:**

```
app-phone/src/main/kotlin/com/roadmate/phone/
├── RoadMatePhoneApplication.kt     # @HiltAndroidApp
├── di/                              # WorkerModule.kt, SyncModule.kt
├── navigation/
│   ├── RoadMateNavHost.kt          # Single NavHost, all routes
│   └── Routes.kt                   # @Serializable route definitions
├── ui/
│   ├── hub/                         # VehicleHubScreen, VehicleHubViewModel
│   ├── maintenance/                 # List + Detail screens and ViewModels
│   ├── trips/                       # List + Detail screens and ViewModels
│   ├── fuel/                        # FuelLogListScreen, FuelLogEntrySheet
│   ├── documents/                   # List + Detail screens and ViewModels
│   ├── settings/                    # SettingsScreen, SettingsViewModel
│   ├── components/                  # AttentionBand, VehicleHeroCard
│   └── theme/                       # PhoneTheme.kt
└── worker/
    ├── MaintenanceNotificationWorker.kt
    └── SyncWorker.kt
```

### Architectural Boundaries

**Module Dependency Rules:**

```
app-headunit ──→ core
app-phone    ──→ core
core         ──→ (no project dependencies, only external libs)
build-logic  ──→ (standalone, no project dependencies)
```

- `:core` NEVER depends on `:app-headunit` or `:app-phone`
- App modules NEVER depend on each other
- Shared UI components live in `:core:ui`, NOT duplicated between apps

**Layer Boundaries within Core:**

| Boundary | Rule |
|---|---|
| DAOs | Only accessed by repositories — never by ViewModels or services directly |
| Repositories | Only accessed by ViewModels and sync engine |
| State managers | Accessed by services (write) and ViewModels (read) |
| Sync engine | Only accessed by `BluetoothSyncManager` — never by UI layer |
| Location providers | Only accessed by `GpsTracker` in head unit service |

### Requirements to Structure Mapping

| PRD Category | Primary Location | Secondary |
|---|---|---|
| FR1-6: Vehicle Management | `core/database/entity/Vehicle.kt`, `core/repository/VehicleRepository.kt` | `phone/ui/hub/`, `phone/ui/settings/` |
| FR7-14: Trip Tracking | `headunit/service/TripDetector.kt`, `headunit/service/GpsTracker.kt` | `core/database/entity/Trip.kt`, `core/database/entity/TripPoint.kt` |
| FR15-21: Maintenance | `core/repository/MaintenanceRepository.kt` | `phone/ui/maintenance/`, `core/ui/components/GaugeArc.kt` |
| FR22-25: Fuel Tracking | `core/repository/FuelRepository.kt` | `phone/ui/fuel/` |
| FR26-29: Documents | `core/repository/DocumentRepository.kt` | `phone/ui/documents/` |
| FR30-35: Data Sync | `core/sync/SyncEngine.kt`, `core/sync/BluetoothSyncManager.kt` | `headunit/service/RfcommServer.kt`, `phone/worker/SyncWorker.kt` |
| FR36-39: Data Integrity | `headunit/service/CrashRecoveryJournal.kt`, `core/database/RoadMateDatabase.kt` | `headunit/receiver/PowerReceiver.kt` |
| FR40-43: Background Service | `headunit/service/RoadMateService.kt` | `headunit/receiver/BootReceiver.kt` |
| FR44-47: Notifications | `phone/worker/MaintenanceNotificationWorker.kt` | — |
| FR48-50: Statistics | `core/repository/TripRepository.kt` (aggregate queries) | `phone/ui/hub/VehicleHubViewModel.kt` |

### Cross-Cutting Concerns Mapping

| Concern | Files Involved |
|---|---|
| Driving state | `core/state/DrivingStateManager.kt` → `headunit/service/TripDetector.kt` (writes), all ViewModels (reads) |
| Vehicle scoping | Every DAO method, every repository method, every ViewModel query |
| Power loss resilience | `core/database/RoadMateDatabase.kt` (WAL), `headunit/service/GpsTracker.kt` (flush), `headunit/service/CrashRecoveryJournal.kt` |
| BT connection | `core/state/BluetoothStateManager.kt` → `core/ui/components/StatusChip.kt`, `core/sync/BluetoothSyncManager.kt` |
| Theme | `core/ui/theme/` (shared base) → `headunit/ui/theme/` (dark landscape) + `phone/ui/theme/` (dark portrait) |

### Data Flow

```
GPS Hardware
    │
    ▼
GpsTracker (location callback)
    │
    ├──→ TripDetector (state machine evaluation, <100ms)
    │       │
    │       ├──→ DrivingStateManager.updateState()
    │       │       │
    │       │       └──→ UI layer (collectAsState)
    │       │
    │       └──→ TripRepository.saveTripPoint() (batched flush every 10s)
    │               │
    │               └──→ TripDao.upsert() → Room DB (WAL)
    │
    └──→ CrashRecoveryJournal.checkpoint() (every 30s)
            │
            └──→ DataStore write

BT Phone Connect
    │
    ▼
RfcommServer (accept connection)
    │
    └──→ SyncProtocolHandler (parse frames)
            │
            ├──→ SyncEngine.handlePull() → query repos → build DTOs → send
            └──→ SyncEngine.handlePush() → parse DTOs → upsert via repos
```

## Architecture Validation Results

### Coherence Validation ✅

**Decision Compatibility:**
- Kotlin 2.3.21 + AGP 9.2.1 + Compose BOM 2026.04.01 — fully compatible. KSP works with both Room 2.8.4 and Hilt 2.59.2.
- Room WAL mode + DataStore crash journal + foreground service — no conflicts. WAL is the recommended journal mode for Room with concurrent readers.
- RFCOMM SPP + A2DP/HFP coexistence — separate UUIDs, separate RFCOMM channels. A2DP uses L2CAP, not RFCOMM.
- Hilt `@Singleton` state managers + foreground service — works because both the service and activities share the same `SingletonComponent` scope.
- No contradictory decisions found.

**Pattern Consistency:**
- Screen/Content composable split aligns with `UiState<T>` sealed interface.
- Repository `Result<T>` flows cleanly to ViewModel `UiState` mapping via `.fold()`.
- `StateFlow` throughout (no LiveData) — consistent reactive pattern from service → state manager → ViewModel → Compose.
- Naming conventions internally consistent — PascalCase classes, camelCase functions, snake_case DB columns.

**Structure Alignment:**
- Module dependency graph enforced by Gradle module structure.
- Shared UI in `:core:ui` prevents duplication.
- Convention plugins centralize build config.

### Requirements Coverage Validation ✅

**Functional Requirements (50 FRs):**

| FR Range | Category | Architecture Support | Status |
|---|---|---|---|
| FR1-6 | Vehicle Management | Room entities + VehicleRepository + DataStore (active vehicle) | ✅ |
| FR7-14 | Trip Tracking | TripDetector state machine + GpsTracker + TripPoint batching | ✅ |
| FR15-21 | Maintenance | MaintenanceRepository + GaugeArc UI + notification worker | ✅ |
| FR22-25 | Fuel Tracking | FuelRepository + FuelLogEntrySheet (bottom sheet pattern) | ✅ |
| FR26-29 | Documents | DocumentRepository + phone UI screens | ✅ |
| FR30-35 | Data Sync | SyncEngine + SyncProtocolHandler + RFCOMM + DTOs | ✅ |
| FR36-39 | Data Integrity | WAL + 10s flush + 30s journal + PowerReceiver + BootReceiver | ✅ |
| FR40-43 | Background Service | RoadMateService foreground service + BootReceiver auto-start | ✅ |
| FR44-47 | Notifications | MaintenanceNotificationWorker (WorkManager periodic) | ✅ |
| FR48-50 | Statistics | Aggregate DAO queries + VehicleHubViewModel | ✅ |

**Non-Functional Requirements (27 NFRs):**

| NFR | Requirement | Architecture Support | Status |
|---|---|---|---|
| NFR1 | State machine <100ms | TripDetector runs in-process on location callback thread | ✅ |
| NFR5 | Delta sync <5s | Entity-level sync + TripPoint batching (100/msg) | ✅ |
| NFR9-10 | Max 10s/30s data loss | GPS flush interval 10s + journal interval 30s | ✅ |
| NFR13 | 30+ day service uptime | Foreground service + no memory leaks via Hilt scoping | ✅ |
| NFR16-18 | Zero network | No INTERNET permission in manifest | ✅ |
| NFR21 | BT coexistence | Separate RFCOMM UUID, not A2DP channel | ✅ |
| NFR25 | GPS 3s active/0 idle | GpsTracker adjusts interval based on DrivingState | ✅ |
| NFR26 | <50MB DB/year | TripPoint density manageable with 3s interval + cleanup | ✅ |

### Implementation Readiness Validation ✅

**Decision Completeness:**
- All critical technology versions verified via web search ✅
- Code examples provided for every pattern ✅
- Anti-patterns documented with correct alternatives ✅
- Enforcement guidelines explicit (8 mandatory rules) ✅

**Structure Completeness:**
- Every source file named and placed in module tree ✅
- Test organization defined (unit, instrumented, UI, fakes, fixtures) ✅
- Build-logic convention plugins enumerated ✅
- Data flow diagram shows complete GPS → DB → sync path ✅

**Pattern Completeness:**
- All 12 conflict points identified and resolved ✅
- Naming, structure, format, communication, and process patterns defined ✅
- Date/time display formats specified for UI consistency ✅
- Sync payload format fully specified ✅

### Gap Analysis Results

**Critical Gaps:** None

**Important Gaps (non-blocking, addressable in implementation stories):**
1. **ProGuard/R8 rules** — Needed for release builds to keep Room entities, Kotlin Serialization, and Hilt-generated code. Define in first build story.
2. **AndroidManifest permissions** — Exact `<uses-permission>` list (BLUETOOTH_CONNECT, BLUETOOTH_SCAN, ACCESS_FINE_LOCATION, FOREGROUND_SERVICE, RECEIVE_BOOT_COMPLETED) not enumerated. Address in implementation stories.
3. **Version code/name strategy** — Semantic versioning recommended but not critical for architecture.

**Nice-to-Have Gaps:**
1. CI/CD pipeline — No CI defined. Add GitHub Actions later.
2. ktlint/detekt configuration — Code style enforcement tooling not specified.
3. Compose preview strategy — Sample data objects for preview not defined.

### Architecture Completeness Checklist

**Requirements Analysis**
- [x] Project context thoroughly analyzed
- [x] Scale and complexity assessed
- [x] Technical constraints identified
- [x] Cross-cutting concerns mapped

**Architectural Decisions**
- [x] Critical decisions documented with versions
- [x] Technology stack fully specified
- [x] Integration patterns defined
- [x] Performance considerations addressed

**Implementation Patterns**
- [x] Naming conventions established
- [x] Structure patterns defined
- [x] Communication patterns specified
- [x] Process patterns documented

**Project Structure**
- [x] Complete directory structure defined
- [x] Component boundaries established
- [x] Integration points mapped
- [x] Requirements to structure mapping complete

### Architecture Readiness Assessment

**Overall Status:** READY FOR IMPLEMENTATION

**Confidence Level:** High

**Key Strengths:**
- Zero ambiguity on technology stack — every dependency has a verified version
- Service-first architecture matches the 60% automated behavior requirement profile
- Power loss resilience designed at every layer (WAL + flush + journal)
- Complete module boundary rules prevent accidental cross-contamination
- Code pattern examples give AI agents copy-paste starting points

**Areas for Future Enhancement:**
- ProGuard/R8 rules (first release story)
- CI/CD pipeline (post-MVP)
- ktlint/detekt integration (post-MVP)
- Compose preview data objects (per-screen implementation)

### Implementation Handoff

**AI Agent Guidelines:**
- Follow all architectural decisions exactly as documented
- Use implementation patterns consistently across all components
- Respect project structure and module boundaries
- Refer to this document for all architectural questions
- When in doubt, check the anti-patterns table

**First Implementation Priority:**
1. Create project with convention plugins + version catalog
2. Implement `:core:database` entities and DAOs
3. Implement `:core:model` sealed interfaces and sync DTOs
4. Implement repositories with `Result<T>` pattern
5. Implement state managers with `StateFlow` pattern
