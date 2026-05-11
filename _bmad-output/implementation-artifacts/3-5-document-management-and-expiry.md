# Story 3.5: Document Management & Expiry Tracking

Status: done

## Story

As a driver,
I want to store my vehicle documents with expiry dates and get reminders before they expire,
so that I never drive with expired insurance, license, or registration.

## Acceptance Criteria

1. **Add document** — FAB "+" → ModalBottomSheet: type (dropdown: Insurance, License, Registration, Other), name (required), expiry date (DatePicker, required), reminder days before (number, default 30), notes (optional).

2. **Document list** — Sorted by expiry soonest first. Cards: type icon, name, expiry date, days until expiry. Expiring within reminder window = tertiary. Expired = error color.

3. **Expired display** — "Expired [N days] ago" in error color.

4. **Edit document** — Update expiry date and all fields. Reminder recalculates.

5. **Custom reminder lead time** — Per-document `reminderDaysBefore` (e.g., 14 for license).

## Tasks / Subtasks

- [x] Task 1: Add document sheet (AC: #1)
  - [x] Create `core/ui/components/AddDocumentSheet.kt`
  - [x] DocumentType dropdown, name, DatePicker, reminder days, notes
  - [x] Validation: name and expiry required

- [x] Task 2: Document list screen (AC: #2, #3)
  - [x] Create document list composable (shared between HU and phone)
  - [x] Sort by expiry date ascending
  - [x] Color coding: normal, warning (tertiary), expired (error)

- [x] Task 3: Edit document (AC: #4, #5)
  - [x] Reuse bottom sheet in edit mode
  - [x] Pre-populate all fields

## Dev Notes

### Architecture Compliance

**Document type icons:** Use Material Icons — `Shield` for Insurance, `Badge` for License, `Description` for Registration, `Article` for Other.

**Days until expiry calculation:**
```kotlin
val daysUntilExpiry = ChronoUnit.DAYS.between(LocalDate.now(), Instant.ofEpochMilli(document.expiryDate).atZone(ZoneId.systemDefault()).toLocalDate())
```

### References

- [Source: architecture.md#Document Domain] — Document entity
- [Source: epics.md#Story 3.5] — Acceptance criteria

## Dev Agent Record

### Agent Model Used
glm-5.1

### Debug Log References
- Pre-existing test failures in MaintenancePredictionEngine (daily average calculation) — unrelated to this story

### Completion Notes List
- Created AddDocumentFormState data class with validation, toDocument/fromDocument conversion methods following existing AddMaintenanceFormState pattern
- Created AddDocumentSheet composable with ModalBottomSheet containing DocumentType dropdown, name field, DatePickerField, reminder days field, and optional notes — follows AddMaintenanceSheet conventions exactly
- Created DocumentList composable with shared DocumentCard showing type icon, name, expiry date, and days-until-expiry with color coding (error for expired, tertiary for warning, onSurfaceVariant for normal)
- Edit mode supported via AddDocumentFormState.fromDocument() which pre-populates all fields, then the same AddDocumentSheet is reused with pre-filled state
- Days-until-expiry uses ChronoUnit.DAYS.between as specified in Dev Notes
- All acceptance criteria satisfied: add document (#1), document list with sorting and color coding (#2, #3), edit document (#4), custom reminder lead time (#5)
- All new tests pass (AddDocumentFormStateTest: 15 tests, DocumentListTest: 13 tests)
- No regressions introduced (pre-existing MaintenancePredictionEngine failures unchanged)

### File List
- core/src/main/kotlin/com/roadmate/core/ui/components/AddDocumentFormState.kt (new)
- core/src/main/kotlin/com/roadmate/core/ui/components/AddDocumentSheet.kt (new)
- core/src/main/kotlin/com/roadmate/core/ui/components/DocumentList.kt (new)
- core/src/test/kotlin/com/roadmate/core/ui/components/AddDocumentFormStateTest.kt (new)
- core/src/test/kotlin/com/roadmate/core/ui/components/DocumentListTest.kt (new)

### Review Findings

- [x] [Review][Patch] `isSaveEnabled` doesn't enforce `validate()` was called — name check alone is insufficient [AddDocumentFormState.kt:15-16]
- [x] [Review][Patch] `reminderDaysBefore = "0"` rejected as invalid — prevents disabling reminders [AddDocumentFormState.kt:26]
- [x] [Review][Patch] Flaky tests near midnight — `daysUntilExpiry` tests use `System.currentTimeMillis()` arithmetic instead of fixed LocalDate [DocumentListTest.kt:20-38]
- [x] [Review][Patch] `DocumentCard` missing 76dp minimum touch target height for automotive displays [DocumentList.kt:92-139]
- [x] [Review][Patch] `toDocument()` allows whitespace-only name to produce blank Document name [AddDocumentFormState.kt:40]
- [x] [Review][Patch] `documentExpiryState(0, 0)` returns WARNING when 0 reminder days means "no reminder" [DocumentList.kt:62-66]
- [x] [Review][Defer] `expiryDate` defaults to `System.currentTimeMillis()` — new document saves as "expiring today" [AddDocumentFormState.kt:10] — deferred, pre-existing pattern from AddMaintenanceFormState
- [x] [Review][Defer] No input length limits on name/notes fields [AddDocumentSheet.kt:89-143] — deferred, project-wide concern
- [x] [Review][Defer] `daysUntilExpiry` timezone sensitivity with `ZoneId.systemDefault()` [DocumentList.kt:42-46] — deferred, systemic date calculation issue
