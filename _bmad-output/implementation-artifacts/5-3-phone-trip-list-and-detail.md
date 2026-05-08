# Story 5.3: Phone Trip List & Trip Detail

Status: ready-for-dev

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

- [ ] Task 1: TripList screen (AC: #1, #2, #3, #6)
  - [ ] Create `app-phone/ui/trips/TripListScreen.kt`
  - [ ] Trip cards with metadata
  - [ ] Empty state
  - [ ] Navigate to TripDetail on tap

- [ ] Task 2: TripDetail screen (AC: #4, #5)
  - [ ] Create `app-phone/ui/trips/TripDetailScreen.kt`
  - [ ] Full trip stats display
  - [ ] Route summary with start/end coordinates

## Dev Notes

### Architecture Compliance

**Trip card format:** Use `DateTimeFormatter` for date, `String.format("%d:%02d", hours, minutes)` for duration.

**Coordinate formatting:** `"%.4f°N, %.4f°E".format(lat, lng)` as offline-friendly text.

### References

- [Source: ux-design-specification.md#Trip Cards] — Card layout
- [Source: epics.md#Story 5.3] — Acceptance criteria

## Dev Agent Record

### Agent Model Used
### Debug Log References
### Completion Notes List
### File List
