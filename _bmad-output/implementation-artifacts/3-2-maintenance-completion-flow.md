# Story 3.2: Maintenance Completion Flow

Status: ready-for-dev

## Story

As a driver,
I want to mark a maintenance item as done and record the service details,
so that the progress resets and my service history is kept up to date.

## Acceptance Criteria

1. **ModalBottomSheet** — Slides up with pre-filled: today's date (DatePicker), current vehicle ODO. Optional: cost, location, notes. Dark styling `surfaceContainer` (#121212).

2. **Save action** — Creates `MaintenanceRecord`, updates `MaintenanceSchedule` (lastServiceKm, lastServiceDate), GaugeArc animates to 0%.

3. **Undo** — Success Snackbar "Service recorded" 4 seconds with "Undo". Undo deletes record and reverts schedule.

4. **ODO validation** — Red border if entered ODO < vehicle current ODO. Save disabled.

5. **Required fields** — Save disabled until date and ODO valid.

6. **Dismiss** — Swipe down dismisses without saving (implicit cancel).

## Tasks / Subtasks

- [ ] Task 1: Bottom sheet UI (AC: #1, #5)
  - [ ] Create `core/ui/components/MaintenanceCompletionSheet.kt`
  - [ ] Pre-fill date and ODO from vehicle state
  - [ ] Cost, location, notes as optional fields
  - [ ] Dark styling with surfaceContainer background

- [ ] Task 2: Save logic (AC: #2)
  - [ ] Create MaintenanceRecord via repository
  - [ ] Update MaintenanceSchedule.lastServiceKm and lastServiceDate
  - [ ] Both in single transaction

- [ ] Task 3: Undo (AC: #3)
  - [ ] Show Snackbar with Undo action
  - [ ] On Undo: delete record, revert schedule to previous values
  - [ ] Store previous values in ViewModel before save

- [ ] Task 4: Validation (AC: #4)
  - [ ] Inline ODO validation against vehicle's current odometerKm
  - [ ] Red border + helper text for invalid input

## Dev Notes

### Architecture Compliance

**ModalBottomSheet must use M3:**
```kotlin
ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surfaceContainer
) { ... }
```

**Undo pattern:** Store previous `lastServiceKm` and `lastServiceDate` in ViewModel state before writing. On undo, restore both + delete record in single transaction.

### References

- [Source: ux-design-specification.md#Bottom Sheets] — Modal pattern
- [Source: architecture.md#Implementation Patterns] — Transaction pattern
- [Source: epics.md#Story 3.2] — Acceptance criteria

## Dev Agent Record

### Agent Model Used
### Debug Log References
### Completion Notes List
### File List
