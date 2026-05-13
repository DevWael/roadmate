# Story 5.3: Phone Trip List & Trip Detail

Status: done

## Story

As a driver,
I want to see my trip history on my phone with details for each trip,
so that I can review my driving patterns and journey details.

## Acceptance Criteria

1. **Trip list** — Scrollable cards for active vehicle: date, distance (km), duration (h:mm), avg speed. Most recent first.

2. **Interrupted label** — "⚡ Interrupted" in tertiary color.

3. **Empty state** — Car icon + "No trips recorded yet. Drive to start tracking."

4. **Trip detail** — Date/time range, total distance, duration, avg speed, max speed, estimated fuel. Loads within 200ms.

5. **Route summary** — Start/end coordinates as text (lat/lng offline).

6. **60fps scrolling** — Standard LazyColumn performance.

## Tasks / Subtasks

- [x] Task 1: TripList screen (AC: #1, #2, #3, #6)
  - [x] Create `app-phone/ui/trips/TripListScreen.kt`
  - [x] Trip cards with metadata
  - [x] Empty state
  - [x] Navigate to TripDetail on tap

- [x] Task 2: TripDetail screen (AC: #4, #5)
  - [x] Create `app-phone/ui/trips/TripDetailScreen.kt`
  - [x] Full trip stats display
  - [x] Route summary with start/end coordinates

## Dev Notes

### Architecture Compliance

**Trip card format:** Use `DateTimeFormatter` for date, `String.format("%d:%02d", hours, minutes)` for duration.

**Coordinate formatting:** `"%.4f°N, %.4f°E".format(lat, lng)` as offline-friendly text.

### References

- [Source: ux-design-specification.md#Trip Cards] — Card layout
- [Source: epics.md#Story 5.3] — Acceptance criteria

## Dev Agent Record

### Agent Model Used
glm-5.1

### Debug Log References
- Initial build failed due to missing `Box` import — fixed by adding the import.
- Pre-existing core module test failures (MaintenancePredictionEngineTest) confirmed unrelated.

### Completion Notes List
- Implemented TripListViewModel with ActiveVehicleRepository + TripRepository reactive flow
- Implemented TripDetailViewModel with trip + trip points combine flow, builds RouteSummary from first/last TripPoint
- TripListScreen: LazyColumn with trip cards showing date, distance, duration (h:mm), avg speed; ⚡ Interrupted label in RoadMateTertiary; empty state with DirectionsCar icon; navigates to TripDetail on tap
- TripDetailScreen: RoadMateScaffold with sections for date/time range, full stats (distance, duration, avg speed, max speed, est. fuel), route summary with lat/lng coordinate text
- Duration formatted as `%d:%02d` per Dev Notes spec
- Coordinates formatted as `%.4f°N/S, %.4f°E/W` per Dev Notes spec (offline-friendly)
- All app-phone tests pass (TripListViewModelTest, TripDetailViewModelTest, TripFormattingTest + existing tests)

### File List
- `app-phone/src/main/kotlin/com/roadmate/phone/ui/trips/TripListViewModel.kt` (new)
- `app-phone/src/main/kotlin/com/roadmate/phone/ui/trips/TripDetailViewModel.kt` (new)
- `app-phone/src/main/kotlin/com/roadmate/phone/ui/trips/TripScreens.kt` (modified)
- `app-phone/src/test/kotlin/com/roadmate/phone/ui/trips/TripListViewModelTest.kt` (new)
- `app-phone/src/test/kotlin/com/roadmate/phone/ui/trips/TripDetailViewModelTest.kt` (new)
- `app-phone/src/test/kotlin/com/roadmate/phone/ui/trips/TripFormattingTest.kt` (new)

## Change Log
- 2026-05-13: Implemented TripList and TripDetail screens with ViewModels, formatting utilities, and comprehensive tests

### Review Findings

- [x] [Review][Patch] FQN usage instead of imports for RoadMateScaffold and LaunchedEffect [TripScreens.kt:206,212,225,257]
- [x] [Review][Patch] loadTrip() coroutine leak — previous combine collector not cancelled on re-invocation [TripDetailViewModel.kt:37-54]
- [x] [Review][Patch] Indentation error on verticalArrangement parameter [TripScreens.kt:298]
- [x] [Review][Patch] SimpleDateFormat used instead of DateTimeFormatter per Dev Notes spec [TripScreens.kt:360-371]
- [x] [Review][Patch] String.format locale sensitivity — decimal separator varies by locale [TripScreens.kt:164,174,300-304]
- [x] [Review][Patch] No negative duration guard — negative durationMs produces garbage output [TripScreens.kt:374-377]
- [x] [Review][Defer] Duplicate FakeTripDao implementations across test files [TripListViewModelTest.kt, TripDetailViewModelTest.kt] — deferred, pre-existing test pattern
- [x] [Review][Defer] No testTag/contentDescription on TripCard for accessibility [TripScreens.kt:141] — deferred, accessibility pass planned
