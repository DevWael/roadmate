# Story 7.3: Trip Route Sharing

Status: ready-for-dev

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

- [ ] Task 1: Route URL generator (AC: #1, #2, #4)
  - [ ] Create `core/util/RouteShareGenerator.kt`
  - [ ] Sample TripPoints: start, end, 23 intermediate (evenly spaced by index)
  - [ ] Build OSM URL with lat/lng pairs

- [ ] Task 2: Share intent (AC: #3)
  - [ ] Format share text with date, distance, duration, URL
  - [ ] Launch `Intent.ACTION_SEND` with `text/plain`

- [ ] Task 3: Edge cases (AC: #4, #5)
  - [ ] Handle small trips (no sampling needed)
  - [ ] Handle interrupted trips (use available points)

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
### Debug Log References
### Completion Notes List
### File List
