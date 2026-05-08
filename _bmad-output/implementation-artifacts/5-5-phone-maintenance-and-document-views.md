# Story 5.5: Phone Maintenance & Document Views

Status: ready-for-dev

## Story

As a driver,
I want to manage maintenance items and documents on my phone,
so that I can track service schedules and document expiry from anywhere.

## Acceptance Criteria

1. **MaintenanceList** — All items for active vehicle. Each: name, ProgressRing (36dp compact, same color logic), predicted next service. Sorted by urgency.

2. **MaintenanceDetail** — Full detail from Story 3.7 (GaugeArc Large, history, mark-as-done).

3. **Add maintenance** — FAB "+" → same ModalBottomSheet as Story 3.3.

4. **Mark as done** — Same ModalBottomSheet as Story 3.2.

5. **DocumentList** — Cards sorted by expiry soonest. Type icon, name, expiry, days until. Color coding.

6. **DocumentDetail** — All fields + "Edit" secondary button.

7. **Add document** — FAB "+" → same ModalBottomSheet as Story 3.5.

## Tasks / Subtasks

- [ ] Task 1: MaintenanceList screen (AC: #1, #3)
  - [ ] Create `app-phone/ui/maintenance/MaintenanceListScreen.kt`
  - [ ] ProgressRing (36dp) per item
  - [ ] FAB for add

- [ ] Task 2: MaintenanceDetail screen (AC: #2, #4)
  - [ ] Create `app-phone/ui/maintenance/MaintenanceDetailScreen.kt`
  - [ ] Reuse GaugeArc, history list, completion sheet from core

- [ ] Task 3: DocumentList screen (AC: #5, #7)
  - [ ] Create `app-phone/ui/documents/DocumentListScreen.kt`
  - [ ] Color-coded expiry cards
  - [ ] FAB for add

- [ ] Task 4: DocumentDetail screen (AC: #6)
  - [ ] Create `app-phone/ui/documents/DocumentDetailScreen.kt`
  - [ ] Edit button → reuse AddDocumentSheet in edit mode

## Dev Notes

### Architecture Compliance

**Component reuse is critical.** GaugeArc, MaintenanceCompletionSheet, AddMaintenanceSheet, AddDocumentSheet — all live in `core/ui/components/`. Phone screens just compose them.

**ProgressRing (36dp)** is a variant of GaugeArc Compact. May need a `Mini` variant or use Compact at 36dp.

### References

- [Source: epics.md#Story 5.5] — Acceptance criteria
- Stories 3.2, 3.3, 3.5, 3.7 for shared components

## Dev Agent Record

### Agent Model Used
### Debug Log References
### Completion Notes List
### File List
