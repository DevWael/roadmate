# Story 2.5: Odometer Auto-Update & Trip List (Head Unit)

Status: ready-for-dev

## Story

As a driver,
I want the odometer to update automatically after each trip and to see a list of my past trips,
so that I always know my current mileage and can review my driving history.

## Acceptance Criteria

1. **Odometer auto-update** — On trip completion (COMPLETED/INTERRUPTED), vehicle `odometerKm += trip.distanceKm`, `lastModified` updated.

2. **Trip list** — Parked mode: cards with date, distance (km), duration (h:mm), avg speed (km/h). Most recent first. Interrupted trips show "⚡ Interrupted" label.

3. **Empty state** — Car icon + "No trips recorded yet. Drive to start tracking." in `onSurfaceVariant`.

4. **Live ODO update** — When trip completes while dashboard visible, ODO updates in-place without full refresh. Latest trip appears at top.

5. **60fps scrolling** — Trip list scrolling at 60fps with locale-formatted numbers.

## Tasks / Subtasks

- [ ] Task 1: Odometer update (AC: #1)
  - [ ] In trip finalization logic: update Vehicle.odometerKm via VehicleRepository
  - [ ] Update lastModified timestamp

- [ ] Task 2: Trip list UI (AC: #2, #3, #5)
  - [ ] Create `app-headunit/ui/parked/TripListSection.kt` composable
  - [ ] Trip cards with date, distance, duration, avg speed
  - [ ] "⚡ Interrupted" label for interrupted trips
  - [ ] Empty state composable
  - [ ] Use `LazyColumn` for 60fps scroll performance
  - [ ] Locale number formatting

- [ ] Task 3: Live update (AC: #4)
  - [ ] Dashboard observes `TripRepository.getTripsForVehicle()` Flow
  - [ ] ODO observes `VehicleRepository.getVehicle()` Flow
  - [ ] Updates propagate reactively without manual refresh

## Dev Notes

### Architecture Compliance

**Locale formatting:** Use `NumberFormat.getNumberInstance(Locale.getDefault()).format(value)` for ODO display.

**Trip card is read-only on head unit.** No tap actions — just display. Phone app has detail views.

### References

- [Source: architecture.md#Dashboard Architecture] — Parked mode panels
- [Source: ux-design-specification.md#Trip Cards] — Card layout
- [Source: epics.md#Story 2.5] — Acceptance criteria

## Dev Agent Record

### Agent Model Used
### Debug Log References
### Completion Notes List
### File List
