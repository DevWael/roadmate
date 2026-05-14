# Story 7.3: Trip Route Sharing

Status: review

## Story

As a driver,
I want to share a specific trip route with someone,
so that I can show them where I drove or save interesting routes.

## Acceptance Criteria

1. **Share route** — TripDetail → "Share Route" → OpenStreetMap URL: `https://www.openstreetmap.org/directions?route=[lat1,lng1;lat2,lng2;...]` with max 25 sampled waypoints.

2. **Intelligent sampling** — 500+ TripPoints → start + end + 23 evenly distributed intermediate points. Route shape preserved.

3. **Share intent** — Android share sheet with text: "My trip on [date]: [distance] km, [duration] — [URL]".

4. **Small trips** — <25 TripPoints → all included without sampling.

5. **Interrupted trips** — Only available TripPoints used. Gaps = straight lines on map (acceptable).

## Tasks / Subtasks

- [x] Task 1: Route URL generator (AC: #1, #2, #4)
  - [x] Create `core/util/RouteShareGenerator.kt`
  - [x] Sample TripPoints: start, end, 23 intermediate (evenly spaced by index)
  - [x] Build OSM URL with lat/lng pairs

- [x] Task 2: Share intent (AC: #3)
  - [x] Format share text with date, distance, duration, URL
  - [x] Launch `Intent.ACTION_SEND` with `text/plain`

- [x] Task 3: Edge cases (AC: #4, #5)
  - [x] Handle small trips (no sampling needed)
  - [x] Handle interrupted trips (use available points)

## Dev Notes

### Sampling algorithm:
```kotlin
fun sampleWaypoints(points: List<TripPoint>, maxPoints: Int = 25): List<TripPoint> {
    if (points.size <= maxPoints) return points
    val step = (points.size - 2).toDouble() / (maxPoints - 2)
    return buildList {
        add(points.first())
        for (i in 1 until maxPoints - 1) {
            add(points[(i * step).roundToInt()])
        }
        add(points.last())
    }
}
```

### References

- [Source: epics.md#Story 7.3] — Acceptance criteria

## Dev Agent Record

### Agent Model Used
GLM-5.1
### Debug Log References
No issues encountered during implementation.
### Completion Notes List
- Created `RouteShareGenerator.kt` as a stateless object utility matching project patterns (like CsvExporter, HaversineCalculator)
- Implemented `sampleWaypoints()` with exact algorithm from Dev Notes - start + end + 23 evenly distributed intermediate points for trips with >25 points
- Implemented `generateOsmUrl()` building OSM directions URL with semicolon-separated lat,lng pairs
- Implemented `formatShareText()` with format: "My trip on [date]: [distance] km, [duration] — [URL]"
- Implemented `generateRouteShare()` combining sampling + URL + text formatting
- Updated `TripDetailUiState` to include `tripPoints` list for share generation
- Added `generateShareText()` method to `TripDetailViewModel` that returns null when <2 points
- Added "Share Route" OutlinedButton with Share icon to `TripDetailScreen` below Route Summary
- Share button launches `Intent.ACTION_SEND` with `text/plain` via Android share sheet
- Edge cases handled: small trips (≤25 points no sampling), interrupted trips (uses available points as-is), trips with <2 points (share button hidden via routeSummary null check)
- All 18 new tests pass (RouteShareGeneratorTest + TripDetailViewModelTest share tests)
- Pre-existing test failures in MaintenancePredictionEngineTest (2) and FuelLogViewModelTest (1) are unrelated to this story
### File List

New files:
- core/src/main/kotlin/com/roadmate/core/util/RouteShareGenerator.kt
- core/src/test/kotlin/com/roadmate/core/util/RouteShareGeneratorTest.kt

Modified files:
- core/src/main/kotlin/com/roadmate/core/util/ (no changes to existing files)
- app-phone/src/main/kotlin/com/roadmate/phone/ui/trips/TripDetailViewModel.kt
- app-phone/src/main/kotlin/com/roadmate/phone/ui/trips/TripScreens.kt
- app-phone/src/test/kotlin/com/roadmate/phone/ui/trips/TripDetailViewModelTest.kt

### Review Findings
- [x] [Review][Patch] Duplicate hiltViewModel() in TripDetailContent — creates second ViewModel instance; share may operate on unloaded data [TripScreens.kt:L267]
- [x] [Review][Patch] Fully-qualified imports in TripScreens.kt — inconsistent with project import style [TripScreens.kt:L262-264]

## Change Log

- 2026-05-14: Implemented trip route sharing feature (Story 7.3) - RouteShareGenerator utility, TripDetailViewModel share support, TripDetailScreen Share Route button
- 2026-05-14: Code review patches applied (duplicate ViewModel, import style)
