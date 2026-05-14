# Story 7.2: Data Export (CSV/PDF)

Status: review

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

- [x] Task 1: Export screen (AC: #1)
  - [x] Create `app-phone/ui/settings/ExportScreen.kt`
  - [x] Format selector, scope selector, date range picker

- [x] Task 2: CSV generator (AC: #2, #3, #4)
  - [x] Create `core/util/CsvExporter.kt`
  - [x] Generate CSV files for each scope
  - [x] Write to app's cache directory

- [x] Task 3: PDF generator (AC: #5)
  - [x] Create `core/util/PdfExporter.kt`
  - [x] Use Android `PdfDocument` API
  - [x] Table layout with headers

- [x] Task 4: Share intent (AC: #6)
  - [x] Use `FileProvider` for content URI
  - [x] `Intent.ACTION_SEND` with file MIME type

- [x] Task 5: Background execution (AC: #7)
  - [x] Run export on `Dispatchers.IO`
  - [x] Show progress indicator

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
glm-5.1

### Debug Log References
- Pre-existing test failures in MaintenancePredictionEngineTest (2) and FuelLogViewModelTest (1) — unrelated to this story
- All CsvExporterTest cases pass (12/12)

### Completion Notes List
- Implemented CsvExporter as pure utility object with exportTrips/exportFuelLogs/exportMaintenance methods, date range filtering, and file naming
- Implemented PdfExporter using Android PdfDocument API with paginated table layout, vehicle name header, export date, and auto-pagination
- Created ExportViewModel with Hilt injection, background export on Dispatchers.IO, date range support, and ZIP bundling for "All" scope
- Created ExportScreen with format/scope/date selectors, progress indicator, success state with share button, and error handling
- Added FileProvider configuration (file_paths.xml + AndroidManifest provider declaration)
- Added ExportData route and navigation from VehicleHubScreen top bar (file download icon)
- Share intent uses FileProvider URI with FLAG_GRANT_READ_URI_PERMISSION

### File List
- core/src/main/kotlin/com/roadmate/core/util/CsvExporter.kt (new)
- core/src/main/kotlin/com/roadmate/core/util/PdfExporter.kt (new)
- core/src/test/kotlin/com/roadmate/core/util/CsvExporterTest.kt (new)
- app-phone/src/main/kotlin/com/roadmate/phone/ui/settings/ExportScreen.kt (new)
- app-phone/src/main/kotlin/com/roadmate/phone/ui/settings/ExportViewModel.kt (new)
- app-phone/src/main/res/xml/file_paths.xml (new)
- app-phone/src/main/AndroidManifest.xml (modified — added FileProvider)
- app-phone/src/main/kotlin/com/roadmate/phone/navigation/Routes.kt (modified — added ExportData route)
- app-phone/src/main/kotlin/com/roadmate/phone/navigation/RoadMateNavHost.kt (modified — added ExportData composable)
- app-phone/src/main/kotlin/com/roadmate/phone/ui/hub/VehicleHubScreen.kt (modified — added export button in top bar)

### Review Findings
- [x] [Review][Patch] SimpleDateFormat thread-safety — static instances not thread-safe on IO dispatcher [CsvExporter.kt:L15-17, PdfExporter.kt:L19-21]
- [x] [Review][Patch] CSV injection — fields with commas/quotes/newlines break CSV parsing [CsvExporter.kt:L38-46,L68-74,L99-101]
- [x] [Review][Patch] PdfDocument resource leak on exception — document not closed if FileOutputStream fails [PdfExporter.kt:L251-265]
- [x] [Review][Patch] CSV Fuel Consumption column always empty — AC#3 specifies Consumption data [CsvExporter.kt:L73]
- [x] [Review][Patch] CSV date filter inconsistency — uses `<=` for toMs while StatisticsCalculator uses `<` [CsvExporter.kt:L30,L61,L90]
- [x] [Review][Patch] Date picker pre-selects today when null — may unintentionally narrow export scope [ExportScreen.kt:L257,L271]
- [x] [Review][Defer] ExportViewModel.export() takes Context parameter — violates ViewModel architecture; needs @ApplicationContext injection [ExportViewModel.kt:L97]
- [x] [Review][Defer] Cache files not cleaned after zip — individual files remain in cache directory [ExportViewModel.kt:L222-237]

## Change Log
- 2026-05-14: Implemented data export feature (Story 7.2)
- 2026-05-14: Code review patches applied (thread-safety, CSV injection, PDF leak, consumption column, date filters, date picker)
