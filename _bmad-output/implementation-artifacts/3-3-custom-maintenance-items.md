# Story 3.3: Custom Maintenance Items & Interval Configuration

Status: ready-for-dev

## Story

As a driver,
I want to add my own maintenance items and configure service intervals,
so that I can track services specific to my vehicle beyond the pre-built template.

## Acceptance Criteria

1. **Add custom item** — FAB "+" → ModalBottomSheet: name (required), interval km (optional), interval months (optional), last service date (default today), last service km (default current ODO).

2. **Create entity** — At least name + one interval → `MaintenanceSchedule` with `isCustom = true`. Appears immediately at 0%.

3. **Dual interval** — Both km and month: higher percentage triggers color. Alert text shows closer trigger.

4. **Empty state** — Wrench icon + "No maintenance items. Add your first service item." + primary "Add" button.

5. **Edit intervals** — Modify `intervalKm`/`intervalMonths` on existing items. Progress recalculates immediately.

6. **Delete** — Destructive confirmation dialog → delete MaintenanceSchedule + all MaintenanceRecords. Snackbar "Item deleted".

## Tasks / Subtasks

- [ ] Task 1: Add item sheet (AC: #1, #2)
  - [ ] Create `core/ui/components/AddMaintenanceSheet.kt`
  - [ ] Form: name, interval km, interval months, last service date, last service km
  - [ ] Validation: name required + at least one interval

- [ ] Task 2: Edit intervals (AC: #5)
  - [ ] Create edit mode in bottom sheet (pre-populate existing values)
  - [ ] Update schedule via repository, recalculate progress

- [ ] Task 3: Delete with confirmation (AC: #6)
  - [ ] AlertDialog with destructive action
  - [ ] Cascade delete: schedule + all records
  - [ ] Success Snackbar

- [ ] Task 4: Empty state (AC: #4)
  - [ ] Wrench icon composable
  - [ ] "Add" button triggering bottom sheet

## Dev Notes

### Architecture Compliance

**Custom vs template items:** `isCustom = true` for user-created items. Template items have `isCustom = false`. Both use the same `MaintenanceSchedule` entity.

**Cascade delete:** Room doesn't cascade by default. Delete MaintenanceRecords WHERE scheduleId = X, then delete MaintenanceSchedule. Both in `@Transaction`.

### References

- [Source: architecture.md#Maintenance Domain] — Schedule entity, custom items
- [Source: epics.md#Story 3.3] — Acceptance criteria

## Dev Agent Record

### Agent Model Used
### Debug Log References
### Completion Notes List
### File List
