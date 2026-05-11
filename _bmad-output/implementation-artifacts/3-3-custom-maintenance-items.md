# Story 3.3: Custom Maintenance Items & Interval Configuration

Status: done

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

- [x] Task 1: Add item sheet (AC: #1, #2)
  - [x] Create `core/ui/components/AddMaintenanceSheet.kt`
  - [x] Form: name, interval km, interval months, last service date, last service km
  - [x] Validation: name required + at least one interval

- [x] Task 2: Edit intervals (AC: #5)
  - [x] Create edit mode in bottom sheet (pre-populate existing values)
  - [x] Update schedule via repository, recalculate progress

- [x] Task 3: Delete with confirmation (AC: #6)
  - [x] AlertDialog with destructive action
  - [x] Cascade delete: schedule + all records
  - [x] Success Snackbar

- [x] Task 4: Empty state (AC: #4)
  - [x] Wrench icon composable
  - [x] "Add" button triggering bottom sheet

## Dev Notes

### Architecture Compliance

**Custom vs template items:** `isCustom = true` for user-created items. Template items have `isCustom = false`. Both use the same `MaintenanceSchedule` entity.

**Cascade delete:** Room doesn't cascade by default. Delete MaintenanceRecords WHERE scheduleId = X, then delete MaintenanceSchedule. Both in `@Transaction`.

### References

- [Source: architecture.md#Maintenance Domain] — Schedule entity, custom items
- [Source: epics.md#Story 3.3] — Acceptance criteria

## Dev Agent Record

### Agent Model Used
GLM-5.1

### Debug Log References
- Fixed pre-existing syntax error in `MaintenanceCompletionSheetTest.kt` (missing closing brace for inner class)
- Added `deleteRecordsByScheduleId` abstract method to all test fake DAOs (FakeMainMaintenanceDao, CompletionFakeMaintenanceDao, FakeMaintenanceDao in VehicleSetupViewModelTest)

### Completion Notes List
- Created `AddMaintenanceSheet.kt` - ModalBottomSheet composable for add/edit custom maintenance items with form validation
- Created `AddMaintenanceFormState.kt` - Form state data class with validation (name required, at least one interval), toSchedule() for entity creation, fromSchedule() for edit pre-population
- Created `MaintenanceItemViewModel.kt` - HiltViewModel managing add/edit/delete lifecycle with repository
- Created `DeleteConfirmationDialog.kt` - AlertDialog with destructive action styling for delete confirmation
- Created `MaintenanceEmptyState.kt` - Empty state composable with wrench icon and "Add" button
- Added `deleteRecordsByScheduleId()` and `deleteScheduleWithRecords()` to MaintenanceDao as @Transaction
- Added `deleteScheduleWithRecords()` to MaintenanceRepository
- All acceptance criteria satisfied: add custom item (AC#1), create entity with isCustom=true (AC#2), dual interval handled by existing calculator (AC#3), empty state (AC#4), edit intervals (AC#5), delete with cascade (AC#6)

### File List
- core/src/main/kotlin/com/roadmate/core/database/dao/MaintenanceDao.kt (modified)
- core/src/main/kotlin/com/roadmate/core/repository/MaintenanceRepository.kt (modified)
- core/src/main/kotlin/com/roadmate/core/ui/components/AddMaintenanceSheet.kt (new)
- core/src/main/kotlin/com/roadmate/core/ui/components/DeleteConfirmationDialog.kt (new)
- core/src/main/kotlin/com/roadmate/core/ui/components/MaintenanceEmptyState.kt (new)
- core/src/test/kotlin/com/roadmate/core/repository/MaintenanceRepositoryTest.kt (modified)
- core/src/test/kotlin/com/roadmate/core/ui/components/MaintenanceCompletionSheetTest.kt (fixed)
- app-headunit/src/main/kotlin/com/roadmate/headunit/viewmodel/AddMaintenanceFormState.kt (new)
- app-headunit/src/main/kotlin/com/roadmate/headunit/viewmodel/MaintenanceItemViewModel.kt (new)
- app-headunit/src/test/kotlin/com/roadmate/headunit/viewmodel/AddMaintenanceFormStateTest.kt (new)
- app-headunit/src/test/kotlin/com/roadmate/headunit/viewmodel/MaintenanceItemViewModelTest.kt (new)
- app-headunit/src/test/kotlin/com/roadmate/headunit/MainViewModelTest.kt (modified)
- app-headunit/src/test/kotlin/com/roadmate/headunit/viewmodel/MaintenanceCompletionViewModelTest.kt (modified)
- app-headunit/src/test/kotlin/com/roadmate/headunit/viewmodel/VehicleSetupViewModelTest.kt (modified)

## Change Log
- 2026-05-12: Story 3.3 implementation complete - custom maintenance items, interval configuration, edit, delete with cascade, empty state
- 2026-05-12: Adversarial code review — 2 decision-needed, 6 patch, 0 defer, 1 dismissed
- 2026-05-12: All review findings resolved — patches applied

### Review Findings

- [x] [Review][Decision] `toSchedule()` hardcodes `isCustom = true` — resolved: added `isCustom` field to form state, preserved from original schedule during edits
- [x] [Review][Decision] "Item deleted" snackbar missing — resolved: added `MaintenanceItemEvent` sealed interface with `Channel`-based one-shot events
- [x] [Review][Patch] `DatePickerField` duplicated — resolved: extracted to shared `DatePickerField.kt` with `label` parameter
- [x] [Review][Patch] `RowOfTwo` uses inline fully-qualified Row — resolved: replaced with proper `Row` import
- [x] [Review][Patch] `fromSchedule()` truncates fractional km — resolved: preserves fractional values, clean integers for whole numbers
- [x] [Review][Patch] Delete dialog buttons use 48dp touch targets — resolved: updated to 76dp
- [x] [Review][Patch] Missing `save()` and `deleteSchedule()` test coverage — resolved: added full test suite with dispatcher setup and Turbine event verification
- [x] [Review][Patch] `deleteSchedule` lacks re-entrancy guard — resolved: added `isDeleting` flag mirroring `isSaving` pattern
