# Story 3.7: Maintenance Service History View

Status: ready-for-dev

## Story

As a driver,
I want to see the full service history for each maintenance item,
so that I can track past services, costs, and plan future maintenance.

## Acceptance Criteria

1. **Detail screen** — Item name, GaugeArc Large, predicted next service, interval config, scrollable MaintenanceRecord list.

2. **Record display** — Each: date, ODO at service, cost (if any), location (if any), notes (if any). Most recent first.

3. **Empty history** — "No service records yet. Mark as done after your next service."

4. **Query performance** — Data returns within 200ms.

5. **Total spent** — Sum of all non-null costs at top of history list.

6. **Mark as done action** — Primary button triggers Story 3.2 ModalBottomSheet.

## Tasks / Subtasks

- [ ] Task 1: Detail screen (AC: #1, #3)
  - [ ] Create `MaintenanceDetailScreen.kt` (shared via core or per-app)
  - [ ] GaugeArc Large variant at top
  - [ ] Predicted next service date from prediction engine
  - [ ] Interval display (km / months)

- [ ] Task 2: History list (AC: #2, #5)
  - [ ] Query MaintenanceRecords by scheduleId, sorted by datePerformed DESC
  - [ ] Display date, ODO, cost, location, notes
  - [ ] Total spent summary at top

- [ ] Task 3: Mark as done (AC: #6)
  - [ ] Primary action button → launch MaintenanceCompletionSheet from Story 3.2

## Dev Notes

### Total spent calculation:
```kotlin
val totalSpent = records.mapNotNull { it.cost }.sum()
```

### References

- [Source: epics.md#Story 3.7] — Acceptance criteria

## Dev Agent Record

### Agent Model Used
### Debug Log References
### Completion Notes List
### File List
