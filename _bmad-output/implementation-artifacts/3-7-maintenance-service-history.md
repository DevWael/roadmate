# Story 3.7: Maintenance Service History View

Status: done

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

- [x] Task 1: Detail screen (AC: #1, #3)
  - [x] Create `MaintenanceDetailScreen.kt` (shared via core or per-app)
  - [x] GaugeArc Large variant at top
  - [x] Predicted next service date from prediction engine
  - [x] Interval display (km / months)

- [x] Task 2: History list (AC: #2, #5)
  - [x] Query MaintenanceRecords by scheduleId, sorted by datePerformed DESC
  - [x] Display date, ODO, cost, location, notes
  - [x] Total spent summary at top

- [x] Task 3: Mark as done (AC: #6)
  - [x] Primary action button → launch MaintenanceCompletionSheet from Story 3.2

## Dev Notes

### Total spent calculation:
```kotlin
val totalSpent = records.mapNotNull { it.cost }.sum()
```

### References

- [Source: epics.md#Story 3.7] — Acceptance criteria

## Dev Agent Record

### Agent Model Used
GLM-5.1

### Debug Log References
None

### Completion Notes List
- Created `MaintenanceDetailViewModel` with reactive Flow-based data loading using `flatMapLatest` + `combine` for schedule, vehicle, and records
- ViewModel calculates remaining km, percentage, total spent, and predicted next service date
- Created `MaintenanceDetailScreen` composable with Scaffold, TopAppBar with back navigation, GaugeArc Large, interval info, prediction card, total spent card, record list, empty state, and mark-as-done button
- Records displayed with date, odometer, cost, location, and notes
- Empty state shows "No service records yet. Mark as done after your next service."
- Mark as done button integrates with `MaintenanceCompletionSheet` via `MaintenanceCompletionSheetState` data class
- Total spent shown as formatted currency at top of history list

### File List
- `app-phone/src/main/kotlin/com/roadmate/phone/ui/maintenance/MaintenanceDetailScreen.kt` (new)
- `app-phone/src/main/kotlin/com/roadmate/phone/ui/maintenance/MaintenanceDetailViewModel.kt` (new)
- `app-phone/src/test/kotlin/com/roadmate/phone/ui/maintenance/MaintenanceDetailViewModelTest.kt` (new)

### Review Findings
- [x] [Review][Patch] Hardcoded USD currency in formatCurrency [MaintenanceDetailScreen.kt:456]
- [x] [Review][Patch] Records not sorted by datePerformed DESC — violates AC#2 [MaintenanceDetailViewModel.kt:54]
- [x] [Review][Patch] Multiple loadSchedule() calls create parallel collectors [MaintenanceDetailViewModel.kt:46]

## Change Log
- 2026-05-12: Implemented maintenance detail screen with history list, GaugeArc, prediction, total spent, and mark-as-done integration
