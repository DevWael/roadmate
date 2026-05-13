# Story 5.2: VehicleHub — Hero Card & Dashboard Layout

Status: done

## Story

As a driver,
I want a single-scroll dashboard on my phone showing my vehicle's key information,
so that I can see everything important about my car in one glance.

## Acceptance Criteria

1. **LazyColumn layout** — VehicleHeroCard → AttentionBand (if alerts) → Maintenance summary → Recent trips → Fuel log summary.

2. **VehicleHeroCard** — Vehicle silhouette icon (64dp, onSurfaceVariant), vehicle name (headlineMedium), ODO (displayLarge 36sp Bold), sync status (labelSmall). Background surfaceVariant (#1A1A1A), 4dp corners.

3. **Sync status display** — <5min: "Last synced: just now" default color. >1h: "[X] hours ago" default. Never: "Not yet synced" tertiary (#FFB74D).

4. **AttentionBand** — Up to 2 strips + "+N more". Tap navigates. Swipe defers.

5. **Pull-to-refresh** — Triggers manual BT sync (Story 4.4). Shows indicator until complete/fail.

6. **Query performance** — All dashboard data within 200ms.

## Tasks / Subtasks

- [x] Task 1: VehicleHeroCard (AC: #2, #3)
  - [x] Create `app-phone/ui/hub/VehicleHeroCard.kt`
  - [x] Vehicle icon, name, ODO, sync status
  - [x] Sync time ago logic

- [x] Task 2: VehicleHub screen (AC: #1)
  - [x] Create `app-phone/ui/hub/VehicleHubScreen.kt` with LazyColumn
  - [x] Compose sections: hero, attention, maintenance, trips, fuel

- [x] Task 3: Pull-to-refresh (AC: #5)
  - [x] Use M3 `pullToRefreshState`
  - [x] Wire to SyncTriggerManager.triggerManualSync()

- [x] Task 4: Section cards (AC: #1)
  - [x] Maintenance summary card with top 3 GaugeArc Compact
  - [x] Recent trips card with last 3 trips
  - [x] Fuel log summary with this month's stats

### Review Findings

- [x] [Review][Patch] `formatSyncStatus()` uses `System.currentTimeMillis()` — untestable, negative time diff unguarded [VehicleHeroCard.kt:86-88]
- [x] [Review][Patch] Fuel log month boundary uses `LocalDate.now()` with `ZoneOffset.UTC` — timezone mismatch for non-UTC users [VehicleHubViewModel.kt:196-199]
- [x] [Review][Patch] Duplicate `remainingKm()` computation per schedule in `buildUiState()` — compute once and reuse [VehicleHubViewModel.kt:152-189]
- [x] [Review][Patch] Init race: `loadSyncTimestamp()` and `loadData()` both launch concurrently in `init{}` — 0L flicker on first emission [VehicleHubViewModel.kt:85-89]
- [x] [Review][Patch] `FakePreferencesDataStore` is public instead of `private` — visibility leak in test file [VehicleHubViewModelTest.kt:504]
- [x] [Review][Defer] "See all" clickable Row lacks accessibility `contentDescription` [VehicleHubScreen.kt:316] — deferred, pre-existing pattern

## Dev Notes

### Architecture Compliance

**Screen/Content split** applies to VehicleHubScreen. ViewModel collects all data Flows.

**Section cards are tap-navigable.** Each section card has "See all →" that navigates to the full list screen.

### References

- [Source: ux-design-specification.md#VehicleHub] — Tesla-style layout
- [Source: ux-design-specification.md#VehicleHeroCard] — Card specs
- [Source: epics.md#Story 5.2] — Acceptance criteria

## Dev Agent Record

### Agent Model Used
GLM-5.1

### Debug Log References
- Fixed smart-cast issue with `schedule.intervalKm` in ViewModel by introducing local val
- Added `@Inject constructor()` to `MessageSerializer` and created `SyncModule` to provide `Clock.SYSTEM` for Hilt DI resolution
- Fixed deprecation warnings: replaced `confirmValueChange` in `SwipeToDismissBox` with `LaunchedEffect` observation pattern

### Completion Notes List
- Implemented VehicleHubViewModel with reactive Flow-based data composition combining vehicle, schedules, trips, fuel logs, and sync timestamp
- Created VehicleHeroCard with vehicle icon (64dp DirectionsCar), name, ODO (36sp Bold), and sync status with time-ago logic
- Built full VehicleHubScreen with LazyColumn layout: HeroCard → AttentionBand → MaintenanceSummary → RecentTrips → FuelSummary
- AttentionBand shows up to 2 attention strips with SwipeToDismiss for deferring, plus "+N more" overflow text
- Pull-to-refresh wired via M3 PullToRefreshBox to SyncTriggerManager.triggerManualSync()
- Maintenance summary section uses GaugeArc Compact for top 3 items sorted by percentage
- Recent trips section shows last 3 completed trips with date, distance, and duration
- Fuel summary section shows this month's total liters, cost, and fill-up count
- Each section card has "See all →" navigation to respective list screens
- Updated RoadMateNavHost with new navigation callbacks (onTripClick, onMaintenanceClick)
- Added `testImplementation(datastore-preferences)` to app-phone for test DataStore support
- 22 unit tests all passing: ViewModel tests (initial load, attention items, maintenance summaries, recent trips, fuel summary) and HeroCard utility tests (formatOdometer, formatSyncStatus, syncStatusColor)

### File List
- `app-phone/src/main/kotlin/com/roadmate/phone/ui/hub/VehicleHubViewModel.kt` (new)
- `app-phone/src/main/kotlin/com/roadmate/phone/ui/hub/VehicleHeroCard.kt` (new)
- `app-phone/src/main/kotlin/com/roadmate/phone/ui/hub/VehicleHubScreen.kt` (modified)
- `app-phone/src/main/kotlin/com/roadmate/phone/navigation/RoadMateNavHost.kt` (modified)
- `app-phone/build.gradle.kts` (modified — added test datastore dep)
- `core/src/main/kotlin/com/roadmate/core/sync/protocol/MessageSerializer.kt` (modified — added @Inject)
- `core/src/main/kotlin/com/roadmate/core/di/SyncModule.kt` (new — provides Clock)
- `app-phone/src/test/kotlin/com/roadmate/phone/ui/hub/VehicleHubViewModelTest.kt` (new)
- `app-phone/src/test/kotlin/com/roadmate/phone/ui/hub/VehicleHeroCardTest.kt` (new)

## Change Log
- 2026-05-13: Implemented full VehicleHub dashboard with HeroCard, AttentionBand, section cards, and pull-to-refresh. All 4 tasks completed, 22 tests passing.
