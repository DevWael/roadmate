# Story 7.2: Data Export (CSV/PDF)

Status: ready-for-dev

## Story

As a driver,
I want to export my vehicle data to CSV or PDF,
so that I can keep records outside the app or share them with my mechanic.

## Acceptance Criteria

1. **Export options** — Format (CSV/PDF), scope (Trips/Fuel/Maintenance/All), date range (optional, default all time).

2. **CSV Trips** — Columns: Date, Start Time, End Time, Distance, Duration, Avg Speed, Max Speed, Est Fuel, Status. File: `roadmate_trips_[vehicle]_[date].csv`.

3. **CSV Fuel** — Columns: Date, ODO, Liters, Price/L, Total Cost, Full Tank, Station, Consumption.

4. **CSV Maintenance** — Columns: Item Name, Date, ODO, Cost, Location, Notes.

5. **PDF export** — Vehicle name header, export date, selected data in table format.

6. **Share** — Android share sheet with file attached.

7. **Performance** — 1 year data (~700 trips, 50 fuel, 20 maintenance) exports within 5 seconds, no UI blocking.

## Tasks / Subtasks

- [ ] Task 1: Export screen (AC: #1)
  - [ ] Create `app-phone/ui/settings/ExportScreen.kt`
  - [ ] Format selector, scope selector, date range picker

- [ ] Task 2: CSV generator (AC: #2, #3, #4)
  - [ ] Create `core/util/CsvExporter.kt`
  - [ ] Generate CSV files for each scope
  - [ ] Write to app's cache directory

- [ ] Task 3: PDF generator (AC: #5)
  - [ ] Create `core/util/PdfExporter.kt`
  - [ ] Use Android `PdfDocument` API
  - [ ] Table layout with headers

- [ ] Task 4: Share intent (AC: #6)
  - [ ] Use `FileProvider` for content URI
  - [ ] `Intent.ACTION_SEND` with file MIME type

- [ ] Task 5: Background execution (AC: #7)
  - [ ] Run export on `Dispatchers.IO`
  - [ ] Show progress indicator

## Dev Notes

### Architecture Compliance

**FileProvider for sharing:**
```xml
<provider android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false" android:grantUriPermissions="true">
    <meta-data android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

**CSV uses standard library** — No external dependency. `StringBuilder` or `BufferedWriter`.

### References

- [Source: epics.md#Story 7.2] — Acceptance criteria

## Dev Agent Record

### Agent Model Used
### Debug Log References
### Completion Notes List
### File List
