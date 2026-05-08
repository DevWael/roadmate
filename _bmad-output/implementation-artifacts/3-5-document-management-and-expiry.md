# Story 3.5: Document Management & Expiry Tracking

Status: ready-for-dev

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

- [ ] Task 1: Add document sheet (AC: #1)
  - [ ] Create `core/ui/components/AddDocumentSheet.kt`
  - [ ] DocumentType dropdown, name, DatePicker, reminder days, notes
  - [ ] Validation: name and expiry required

- [ ] Task 2: Document list screen (AC: #2, #3)
  - [ ] Create document list composable (shared between HU and phone)
  - [ ] Sort by expiry date ascending
  - [ ] Color coding: normal, warning (tertiary), expired (error)

- [ ] Task 3: Edit document (AC: #4, #5)
  - [ ] Reuse bottom sheet in edit mode
  - [ ] Pre-populate all fields

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
### Debug Log References
### Completion Notes List
### File List
